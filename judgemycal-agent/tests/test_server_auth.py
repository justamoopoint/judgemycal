"""Auth middleware contract: no token, no service; and a token for user A
can never touch user B's sessions — by path or by /run body."""
import os

import pytest
from fastapi.testclient import TestClient

from server.app import create_app
from server.auth import AuthError

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))

VALID_TOKEN = "valid-token"
UID = "firebase-uid-123"


def _verify(token: str) -> str:
    if token == VALID_TOKEN:
        return UID
    raise AuthError("invalid ID token")


def _verify_app_check(token: str) -> None:
    if token != "valid-app-check":
        raise AuthError("invalid App Check token")


@pytest.fixture(scope="module")
def client():
    app = create_app(agents_dir=REPO_ROOT, verify_token=_verify,
                     verify_app_check=_verify_app_check)
    with TestClient(app) as c:
        yield c


def _auth(**extra):
    return {"Authorization": f"Bearer {VALID_TOKEN}", **extra}


def test_health_is_open_for_probes(client):
    assert client.get("/health").status_code == 200


def test_everything_else_requires_a_token(client):
    assert client.get("/list-apps").status_code == 401
    assert client.post("/run", json={}).status_code == 401
    assert client.get(f"/apps/judgemycal/users/{UID}/sessions").status_code == 401


def test_garbage_token_rejected(client):
    r = client.get("/list-apps", headers={"Authorization": "Bearer nope"})
    assert r.status_code == 401


def test_valid_token_reaches_the_api(client):
    r = client.get("/list-apps", headers=_auth())
    assert r.status_code == 200
    assert "judgemycal" in r.json()


def test_path_user_binding_blocks_other_users(client):
    r = client.get("/apps/judgemycal/users/someone-else/sessions", headers=_auth())
    assert r.status_code == 403


def test_own_sessions_accessible_and_persona_state_settable(client):
    r = client.post(
        f"/apps/judgemycal/users/{UID}/sessions",
        headers=_auth(), json={"state": {"persona": "buddy"}},
    )
    assert r.status_code == 200
    session = r.json()
    assert session["state"]["persona"] == "buddy"

    r = client.get(f"/apps/judgemycal/users/{UID}/sessions", headers=_auth())
    assert r.status_code == 200
    assert any(s["id"] == session["id"] for s in r.json())


def test_run_body_user_binding_blocks_spoofed_userid(client):
    r = client.post("/run", headers=_auth(), json={
        "appName": "judgemycal", "userId": "someone-else",
        "sessionId": "x", "newMessage": {"role": "user", "parts": [{"text": "hi"}]},
    })
    assert r.status_code == 403


def test_run_with_own_userid_passes_auth(client):
    # Nonexistent session -> ADK 404: proves the middleware let it through
    # without needing a live model call.
    r = client.post("/run", headers=_auth(), json={
        "appName": "judgemycal", "userId": UID,
        "sessionId": "no-such-session",
        "newMessage": {"role": "user", "parts": [{"text": "hi"}]},
    })
    assert r.status_code == 404


def test_cors_allows_configured_web_origin_and_preflight(monkeypatch):
    monkeypatch.setenv("ALLOWED_ORIGINS", "https://judgemycal.web.app")
    app = create_app(agents_dir=REPO_ROOT, verify_token=_verify,
                     verify_app_check=_verify_app_check)
    with TestClient(app) as c:
        # Preflight needs no token (it can't carry one, per the CORS spec).
        r = c.options(
            "/run",
            headers={
                "Origin": "https://judgemycal.web.app",
                "Access-Control-Request-Method": "POST",
                "Access-Control-Request-Headers": "authorization,content-type",
            },
        )
        assert r.status_code == 200
        assert r.headers["access-control-allow-origin"] == "https://judgemycal.web.app"

        # The actual request still requires a token even from an allowed origin.
        r = c.post("/run", headers={"Origin": "https://judgemycal.web.app"}, json={})
        assert r.status_code == 401

        # Unknown origins get no CORS grant.
        r = c.options(
            "/run",
            headers={
                "Origin": "https://evil.example",
                "Access-Control-Request-Method": "POST",
            },
        )
        assert "access-control-allow-origin" not in r.headers


def test_no_cors_headers_by_default(client):
    r = client.get("/health", headers={"Origin": "https://judgemycal.web.app"})
    assert "access-control-allow-origin" not in r.headers


def test_app_check_enforced_when_required(monkeypatch):
    monkeypatch.setenv("REQUIRE_APP_CHECK", "1")
    app = create_app(agents_dir=REPO_ROOT, verify_token=_verify,
                     verify_app_check=_verify_app_check)
    with TestClient(app) as c:
        assert c.get("/list-apps", headers=_auth()).status_code == 401
        assert c.get("/list-apps", headers=_auth(
            **{"X-Firebase-AppCheck": "bad"})).status_code == 401
        assert c.get("/list-apps", headers=_auth(
            **{"X-Firebase-AppCheck": "valid-app-check"})).status_code == 200
        assert c.get("/health").status_code == 200
