"""Production API server: ADK's FastAPI app wrapped with Firebase auth.

The agent tree (judgemycal/) is untouched — this package only adds the
transport-layer concerns a hosted deployment needs: token verification,
user binding, optional App Check, and persistent sessions via env config.
"""
