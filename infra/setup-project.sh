#!/usr/bin/env bash
# One-time GCP project setup for the JudgeMyCal agent backend.
# Run from a machine with `gcloud` authenticated as a project owner.
#
# Usage:
#   PROJECT_ID=my-project REGION=europe-west1 BILLING_ACCOUNT_ID=XXXXXX-XXXXXX-XXXXXX \
#     ./infra/setup-project.sh
#
# Optional:
#   BUDGET_AMOUNT=25            monthly budget alert threshold (default 25, in the
#                               billing account's currency)
#   GEMINI_API_KEY=...          ONLY if you choose API-key mode instead of the
#                               recommended Vertex AI mode; stored in Secret Manager,
#                               never in the image or plain env vars.
set -euo pipefail

: "${PROJECT_ID:?set PROJECT_ID}"
: "${REGION:?set REGION (e.g. europe-west1)}"

SA_NAME="judgemycal-agent"
SA_EMAIL="${SA_NAME}@${PROJECT_ID}.iam.gserviceaccount.com"

echo "==> Enabling required APIs"
gcloud services enable \
  run.googleapis.com \
  cloudbuild.googleapis.com \
  artifactregistry.googleapis.com \
  aiplatform.googleapis.com \
  secretmanager.googleapis.com \
  firebase.googleapis.com \
  --project "$PROJECT_ID"

echo "==> Creating dedicated minimal-permission service account: $SA_EMAIL"
if ! gcloud iam service-accounts describe "$SA_EMAIL" --project "$PROJECT_ID" >/dev/null 2>&1; then
  gcloud iam service-accounts create "$SA_NAME" \
    --display-name "JudgeMyCal agent (Cloud Run runtime)" \
    --project "$PROJECT_ID"
fi

# The only role the agent runtime actually needs: calling Gemini via Vertex AI.
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member "serviceAccount:${SA_EMAIL}" \
  --role "roles/aiplatform.user" \
  --condition=None >/dev/null
echo "    granted roles/aiplatform.user"

# Firebase Admin SDK needs to verify ID tokens / App Check tokens. Token
# verification itself is done with public certs (no role needed), but fetching
# project config requires this viewer-level role.
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member "serviceAccount:${SA_EMAIL}" \
  --role "roles/firebaseauth.viewer" \
  --condition=None >/dev/null
echo "    granted roles/firebaseauth.viewer"

# --- Optional: API-key mode ---------------------------------------------------
# Recommended mode is Vertex AI (no key at all). If you must use a Google AI
# Studio key, it lives in Secret Manager and is mounted by deploy.sh.
if [[ -n "${GEMINI_API_KEY:-}" ]]; then
  echo "==> Storing Gemini API key in Secret Manager (API-key mode)"
  if ! gcloud secrets describe gemini-api-key --project "$PROJECT_ID" >/dev/null 2>&1; then
    gcloud secrets create gemini-api-key --replication-policy automatic --project "$PROJECT_ID"
  fi
  printf '%s' "$GEMINI_API_KEY" | gcloud secrets versions add gemini-api-key \
    --data-file=- --project "$PROJECT_ID"
  gcloud secrets add-iam-policy-binding gemini-api-key \
    --member "serviceAccount:${SA_EMAIL}" \
    --role "roles/secretmanager.secretAccessor" \
    --project "$PROJECT_ID" >/dev/null
fi

# --- Budget alert (cost tripwire) ---------------------------------------------
if [[ -n "${BILLING_ACCOUNT_ID:-}" ]]; then
  AMOUNT="${BUDGET_AMOUNT:-25}"
  echo "==> Creating monthly budget alert (${AMOUNT}) on billing account"
  gcloud billing budgets create \
    --billing-account "$BILLING_ACCOUNT_ID" \
    --display-name "judgemycal-agent budget" \
    --budget-amount "$AMOUNT" \
    --threshold-rule percent=0.5 \
    --threshold-rule percent=0.9 \
    --threshold-rule percent=1.0 \
    --filter-projects "projects/${PROJECT_ID}" || \
    echo "    (budget may already exist — check: gcloud billing budgets list --billing-account $BILLING_ACCOUNT_ID)"
else
  echo "!! BILLING_ACCOUNT_ID not set — SKIPPING budget alert. Set it and re-run,"
  echo "   or create one in the console. A live backend needs a cost tripwire."
fi

echo
echo "Done. Next: ./infra/deploy.sh (see docs/RUNBOOK.md for the full sequence)."
