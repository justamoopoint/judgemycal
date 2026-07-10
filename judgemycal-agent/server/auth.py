"""Firebase Admin verification helpers.

firebase_admin is imported lazily so the test suite (which injects fake
verifiers) and local no-auth dev mode never need Google credentials.
"""
from __future__ import annotations


class AuthError(Exception):
    """Raised when a token is missing, malformed, expired, or revoked."""


def init_firebase() -> None:
    """Initialise the default Firebase Admin app (ADC on Cloud Run)."""
    import firebase_admin

    if not firebase_admin._apps:
        firebase_admin.initialize_app()


def verify_id_token(token: str) -> str:
    """Verify a Firebase ID token and return the caller's UID."""
    from firebase_admin import auth as fb_auth

    try:
        decoded = fb_auth.verify_id_token(token)
    except Exception as exc:
        raise AuthError(f"invalid ID token: {exc.__class__.__name__}") from exc
    uid = decoded.get("uid")
    if not uid:
        raise AuthError("ID token has no uid")
    return uid


def verify_app_check_token(token: str) -> None:
    """Verify a Firebase App Check token (raises AuthError on failure)."""
    from firebase_admin import app_check

    try:
        app_check.verify_token(token)
    except Exception as exc:
        raise AuthError(f"invalid App Check token: {exc.__class__.__name__}") from exc
