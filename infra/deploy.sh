#!/usr/bin/env bash
# Deploy the JudgeMyCal agent API to Cloud Run.
#
# Usage:
#   PROJECT_ID=my-project REGION=europe-west1 ./infra/deploy.sh
#
# Optional:
#   SERVICE_NAME=judgemycal-agent
#   SESSION_SERVICE_URI=agentengine://<resource>   persistent sessions (recommended;
#                                                  see docs/RUNBOOK.md §5) or a
#                                                  SQLAlchemy URI (Cloud SQL).
#   REQUIRE_APP_CHECK=1        enforce Firebase App Check (turn on after the
#                              basic token flow works end-to-end)
#   USE_API_KEY_MODE=1         use the Secret Manager key instead of Vertex AI
#   MODEL=gemini-2.0-flash     override JUDGEMYCAL_MODEL
#
# Security posture (deliberate, see docs/RUNBOOK.md §4):
#   * No ADK dev UI in the image (server/app.py passes web=False).
#   * IAM allows unauthenticated *invocation* because callers are Firebase
#     (anonymous) users, not Google identities — but the app rejects every
#     request without a valid Firebase ID token. Nobody can spend Gemini
#     quota with just the URL.
set -euo pipefail

: "${PROJECT_ID:?set PROJECT_ID}"
: "${REGION:?set REGION}"

SERVICE_NAME="${SERVICE_NAME:-judgemycal-agent}"
SA_EMAIL="judgemycal-agent@${PROJECT_ID}.iam.gserviceaccount.com"
AGENT_DIR="$(cd "$(dirname "$0")/../judgemycal-agent" && pwd)"

ENV_VARS="GOOGLE_CLOUD_PROJECT=${PROJECT_ID},GOOGLE_CLOUD_LOCATION=${REGION}"
ENV_VARS+=",JUDGEMYCAL_MODEL=${MODEL:-gemini-2.0-flash}"

SECRET_ARGS=()
if [[ "${USE_API_KEY_MODE:-0}" == "1" ]]; then
  # Key comes from Secret Manager at runtime — never baked into the image.
  ENV_VARS+=",GOOGLE_GENAI_USE_VERTEXAI=FALSE"
  SECRET_ARGS+=(--set-secrets "GOOGLE_API_KEY=gemini-api-key:latest")
else
  # Recommended: Vertex AI mode. Credentials are the runtime service account;
  # no API key exists anywhere.
  ENV_VARS+=",GOOGLE_GENAI_USE_VERTEXAI=TRUE"
fi

if [[ -n "${SESSION_SERVICE_URI:-}" ]]; then
  ENV_VARS+=",SESSION_SERVICE_URI=${SESSION_SERVICE_URI}"
else
  echo "!! SESSION_SERVICE_URI not set — sessions are IN-MEMORY and will be lost"
  echo "   on scale-to-zero/restart (workout continuity breaks). Fine for a smoke"
  echo "   test; set it for real use. See docs/RUNBOOK.md §5."
fi

if [[ "${REQUIRE_APP_CHECK:-0}" == "1" ]]; then
  ENV_VARS+=",REQUIRE_APP_CHECK=1"
fi

# Browser origins for the web app (e.g. https://<project>.web.app). Uses ^;^
# as the gcloud delimiter since origin lists are comma-separated themselves.
if [[ -n "${ALLOWED_ORIGINS:-}" ]]; then
  ENV_VARS="^;^${ENV_VARS//,/;};ALLOWED_ORIGINS=${ALLOWED_ORIGINS}"
fi

echo "==> Deploying ${SERVICE_NAME} to ${REGION} (source: ${AGENT_DIR})"
gcloud run deploy "$SERVICE_NAME" \
  --project "$PROJECT_ID" \
  --region "$REGION" \
  --source "$AGENT_DIR" \
  --service-account "$SA_EMAIL" \
  --allow-unauthenticated \
  --set-env-vars "$ENV_VARS" \
  "${SECRET_ARGS[@]}" \
  --memory 1Gi \
  --cpu 1 \
  --min-instances 0 \
  --max-instances 3 \
  --timeout 300

URL="$(gcloud run services describe "$SERVICE_NAME" --project "$PROJECT_ID" \
        --region "$REGION" --format 'value(status.url)')"
echo
echo "Deployed: $URL"
echo "Smoke test:"
echo "  curl ${URL}/health                     # expect {\"status\":\"ok\"}"
echo "  curl ${URL}/list-apps                  # expect 401 (auth wall works)"
echo "Full post-deploy checks: docs/RUNBOOK.md §6."
