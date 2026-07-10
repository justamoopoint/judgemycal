#!/usr/bin/env bash
# Post-deploy smoke tests for the JudgeMyCal backend (runnable RUNBOOK §6).
#
# Usage:
#   URL=https://judgemycal-agent-xxxx.run.app \
#   WEB_API_KEY=<Firebase Web API key, from project settings> \
#     ./infra/smoke-test.sh
#
# WEB_API_KEY is used only to mint a throwaway anonymous user via the Firebase
# Auth REST API (the same thing the apps do). It is a public client
# identifier, not a secret.
set -euo pipefail

: "${URL:?set URL (Cloud Run service url)}"
: "${WEB_API_KEY:?set WEB_API_KEY (Firebase Web API key)}"

PASS=0; FAIL=0
check() { # check <name> <expected> <actual>
  if [[ "$2" == "$3" ]]; then
    echo "  ok   $1"; PASS=$((PASS+1))
  else
    echo "  FAIL $1 (expected: $2, got: $3)"; FAIL=$((FAIL+1))
  fi
}

echo "==> 1. Health probe (open)"
check "/health returns 200" 200 \
  "$(curl -s -o /dev/null -w '%{http_code}' "$URL/health")"

echo "==> 2. Auth wall (everything else closed)"
check "/list-apps without token is 401" 401 \
  "$(curl -s -o /dev/null -w '%{http_code}' "$URL/list-apps")"
check "/run without token is 401" 401 \
  "$(curl -s -o /dev/null -w '%{http_code}' -X POST -H 'Content-Type: application/json' -d '{}' "$URL/run")"

echo "==> 3. Anonymous sign-in (Firebase Auth REST, like the apps do)"
AUTH_JSON=$(curl -s -X POST \
  "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=${WEB_API_KEY}" \
  -H "Content-Type: application/json" -d '{"returnSecureToken":true}')
TOKEN=$(python3 -c "import json,sys; print(json.load(sys.stdin)['idToken'])" <<<"$AUTH_JSON")
UID=$(python3 -c "import json,sys; print(json.load(sys.stdin)['localId'])" <<<"$AUTH_JSON")
echo "  ok   signed in anonymously as $UID"

echo "==> 4. Session binding"
SESSION_JSON=$(curl -s -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  "$URL/apps/judgemycal/users/$UID/sessions" -d '{"state": {"persona": "buddy"}}')
SESSION=$(python3 -c "import json,sys; print(json.load(sys.stdin)['id'])" <<<"$SESSION_JSON")
echo "  ok   created session $SESSION"
check "another user's sessions are 403" 403 \
  "$(curl -s -o /dev/null -w '%{http_code}' -H "Authorization: Bearer $TOKEN" "$URL/apps/judgemycal/users/someone-else/sessions")"
check "spoofed userId in /run body is 403" 403 \
  "$(curl -s -o /dev/null -w '%{http_code}' -X POST -H "Authorization: Bearer $TOKEN" -H 'Content-Type: application/json' \
     -d "{\"appName\":\"judgemycal\",\"userId\":\"someone-else\",\"sessionId\":\"$SESSION\",\"newMessage\":{\"role\":\"user\",\"parts\":[{\"text\":\"hi\"}]}}" \
     "$URL/run")"

echo "==> 5. Real agent turn + MCP nutrition lookup (proves the stdio subprocess works in the container)"
RUN_OUT=$(curl -s --max-time 120 -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" "$URL/run" \
  -d "{\"appName\":\"judgemycal\",\"userId\":\"$UID\",\"sessionId\":\"$SESSION\",
       \"newMessage\":{\"role\":\"user\",\"parts\":[{\"text\":\"how many calories in 100g of white rice?\"}]}}")
if grep -q "130" <<<"$RUN_OUT"; then
  echo "  ok   reply contains the verified value (130 kcal/100g white rice)"; PASS=$((PASS+1))
else
  echo "  FAIL nutrition value not found in reply — check MCP subprocess logs"; FAIL=$((FAIL+1))
  echo "  reply head: $(head -c 300 <<<"$RUN_OUT")"
fi

echo "==> 6. Safety floor end-to-end (break-character on distress)"
SAFETY_OUT=$(curl -s --max-time 120 -X POST -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" "$URL/run" \
  -d "{\"appName\":\"judgemycal\",\"userId\":\"$UID\",\"sessionId\":\"$SESSION\",
       \"newMessage\":{\"role\":\"user\",\"parts\":[{\"text\":\"I feel like I should just stop eating\"}]}}")
if grep -q "step out of character" <<<"$SAFETY_OUT"; then
  echo "  ok   support message returned; the model was never consulted"; PASS=$((PASS+1))
else
  echo "  FAIL support message missing — the safety floor did NOT trigger"; FAIL=$((FAIL+1))
fi

echo "==> 7. Session persistence across instances"
echo "     (manual: bump the service — gcloud run services update judgemycal-agent"
echo "      --set-env-vars DEPLOY_BUMP=\$(date +%s) — then GET the session again:)"
echo "     curl -H \"Authorization: Bearer \$TOKEN\" $URL/apps/judgemycal/users/$UID/sessions/$SESSION"

echo
echo "Result: $PASS passed, $FAIL failed"
exit $((FAIL > 0 ? 1 : 0))
