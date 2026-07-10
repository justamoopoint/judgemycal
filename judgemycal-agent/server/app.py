"""Production entry point: the ADK API server behind Firebase auth.

Runs the exact FastAPI app `adk api_server` serves (same session + /run
wire contract the Android client is built against), wrapped with
FirebaseAuthMiddleware. Configuration is all env-driven:

  AGENTS_DIR            directory whose subdirs are agent apps (default: repo root)
  SESSION_SERVICE_URI   persistent sessions, e.g. `agentengine://<resource>` or a
                        SQLAlchemy DB URI; unset = in-memory (dev only — state
                        dies with the instance, which breaks workout continuity)
  ALLOWED_ORIGINS       comma-separated browser origins for CORS (web app);
                        unset = no CORS headers (native clients don't need them)
  REQUIRE_APP_CHECK=1   also require a valid X-Firebase-AppCheck header
  DISABLE_AUTH=1        local development ONLY: skip Firebase entirely
  PORT                  bind port (Cloud Run sets this; default 8080)

Model credentials come from the runtime service account when
GOOGLE_GENAI_USE_VERTEXAI=TRUE — the recommended production mode; no API key
exists anywhere. See infra/ and docs/RUNBOOK.md for deployment.
"""
from __future__ import annotations

import logging
import os
from typing import Callable, Optional

from google.adk.cli.fast_api import get_fast_api_app

from . import auth
from .middleware import FirebaseAuthMiddleware

logger = logging.getLogger(__name__)

_REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def create_app(
    agents_dir: Optional[str] = None,
    verify_token: Optional[Callable[[str], str]] = None,
    verify_app_check: Optional[Callable[[str], None]] = None,
):
    """Build the app. `verify_*` overrides exist for tests; production uses Firebase."""
    # Browser clients (the web app) need CORS; native clients don't. Origins are
    # an explicit allowlist — unset means no CORS headers at all.
    allowed_origins = [
        o.strip() for o in os.environ.get("ALLOWED_ORIGINS", "").split(",") if o.strip()
    ]

    app = get_fast_api_app(
        agents_dir=agents_dir or os.environ.get("AGENTS_DIR", _REPO_ROOT),
        web=False,  # never ship the dev UI on the production service
        session_service_uri=os.environ.get("SESSION_SERVICE_URI") or None,
        allow_origins=allowed_origins or None,
    )

    if os.environ.get("DISABLE_AUTH") == "1":
        logger.warning("DISABLE_AUTH=1 — serving WITHOUT authentication (dev only)")
        return app

    if verify_token is None:
        auth.init_firebase()
        verify_token = auth.verify_id_token
        verify_app_check = auth.verify_app_check_token

    app.add_middleware(
        FirebaseAuthMiddleware,
        verify_token=verify_token,
        verify_app_check=verify_app_check,
        require_app_check=os.environ.get("REQUIRE_APP_CHECK") == "1",
    )
    return app


# Serve with:  uvicorn server.app:create_app --factory  (see Dockerfile)
if __name__ == "__main__":
    import uvicorn

    uvicorn.run(create_app(), host="0.0.0.0", port=int(os.environ.get("PORT", "8080")))
