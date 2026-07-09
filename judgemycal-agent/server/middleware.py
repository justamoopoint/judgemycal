"""Pure-ASGI auth middleware for the ADK FastAPI server.

Every request (except the health probe) must carry a valid Firebase ID token.
On top of verification, the middleware *binds* the request to the token's UID:

  * path-addressed routes (`/apps/{app}/users/{user_id}/...`) must use the
    caller's own UID as `user_id`;
  * body-addressed routes (`POST /run`, `POST /run_sse`) must set `userId`
    to the caller's UID.

So a valid token for user A can never read or write user B's sessions.
Implemented as raw ASGI (not BaseHTTPMiddleware) so the /run body can be
buffered, inspected, and replayed without breaking SSE streaming responses.
"""
from __future__ import annotations

import json
import re
from typing import Awaitable, Callable, Iterable, Optional

from .auth import AuthError

_USER_PATH = re.compile(r"^/apps/[^/]+/users/([^/]+)(/|$)")
_BODY_BOUND_PATHS = frozenset({"/run", "/run_sse"})


async def _send_json(send, status: int, payload: dict) -> None:
    body = json.dumps(payload).encode()
    await send({
        "type": "http.response.start",
        "status": status,
        "headers": [
            (b"content-type", b"application/json"),
            (b"content-length", str(len(body)).encode()),
        ],
    })
    await send({"type": "http.response.body", "body": body})


class FirebaseAuthMiddleware:
    """ASGI middleware enforcing Firebase auth + user binding (+ App Check)."""

    def __init__(
        self,
        app,
        verify_token: Callable[[str], str],
        verify_app_check: Optional[Callable[[str], None]] = None,
        require_app_check: bool = False,
        exempt_paths: Iterable[str] = ("/health",),
    ) -> None:
        self.app = app
        self.verify_token = verify_token
        self.verify_app_check = verify_app_check
        self.require_app_check = require_app_check
        self.exempt_paths = frozenset(exempt_paths)

    async def __call__(self, scope, receive, send) -> None:
        if scope["type"] != "http" or scope["path"] in self.exempt_paths:
            await self.app(scope, receive, send)
            return

        headers = {k.decode("latin-1").lower(): v.decode("latin-1")
                   for k, v in scope.get("headers", [])}

        authorization = headers.get("authorization", "")
        if not authorization.startswith("Bearer "):
            await _send_json(send, 401, {"detail": "Missing bearer token"})
            return
        try:
            uid = self.verify_token(authorization[len("Bearer "):])
        except AuthError as exc:
            await _send_json(send, 401, {"detail": str(exc)})
            return

        if self.require_app_check:
            app_check_token = headers.get("x-firebase-appcheck", "")
            if not app_check_token:
                await _send_json(send, 401, {"detail": "Missing App Check token"})
                return
            try:
                self.verify_app_check(app_check_token)
            except AuthError as exc:
                await _send_json(send, 401, {"detail": str(exc)})
                return

        path_match = _USER_PATH.match(scope["path"])
        if path_match and path_match.group(1) != uid:
            await _send_json(send, 403, {"detail": "user_id does not match token"})
            return

        if scope["path"] in _BODY_BOUND_PATHS and scope.get("method") == "POST":
            body, replay_receive = await self._buffer_body(receive)
            try:
                payload = json.loads(body or b"{}")
                body_uid = payload.get("userId", payload.get("user_id"))
            except (json.JSONDecodeError, AttributeError):
                body_uid = None
            if body_uid != uid:
                await _send_json(send, 403, {"detail": "userId does not match token"})
                return
            await self.app(scope, replay_receive, send)
            return

        await self.app(scope, receive, send)

    @staticmethod
    async def _buffer_body(receive) -> tuple[bytes, Callable[[], Awaitable[dict]]]:
        """Drain the request body, returning it plus a receive that replays it."""
        chunks: list[bytes] = []
        while True:
            message = await receive()
            if message["type"] != "http.request":
                break
            chunks.append(message.get("body", b""))
            if not message.get("more_body", False):
                break
        body = b"".join(chunks)
        sent = False

        async def replay() -> dict:
            nonlocal sent
            if not sent:
                sent = True
                return {"type": "http.request", "body": body, "more_body": False}
            # Body already replayed: defer to the real channel so downstream
            # disconnect-watchers (e.g. ADK's /run abort logic) see the true
            # client state instead of a premature disconnect.
            return await receive()

        return body, replay
