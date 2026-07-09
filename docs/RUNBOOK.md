# JudgeMyCal Production Runbook

Everything in the repo is code-complete; this runbook is the ordered list of
steps **you** run (they need your Google accounts). Total active time is about
an hour, plus Play review wait times.

## 1. Prerequisites

- `gcloud` CLI authenticated as an owner of a (new or existing) GCP project
  with billing enabled.
- A [Firebase](https://console.firebase.google.com) account.
- A [Play Console](https://play.google.com/console) developer account —
  **start identity verification early**, it takes 24–48h for individuals.

## 2. GCP project setup (one-time)

```bash
PROJECT_ID=<your-project> REGION=<e.g. europe-west1> \
BILLING_ACCOUNT_ID=<XXXXXX-XXXXXX-XXXXXX> \
  ./infra/setup-project.sh
```

What it does: enables APIs, creates the dedicated `judgemycal-agent` service
account with only `roles/aiplatform.user` + `roles/firebaseauth.viewer`, and
sets a **monthly budget alert** (50%/90%/100% thresholds — the cost tripwire).

Vertex AI mode is the default: the deployed agent authenticates as its service
account, so **no Gemini API key exists anywhere**. If you insist on API-key
mode, pass `GEMINI_API_KEY=...` to the script (stored in Secret Manager) and
deploy with `USE_API_KEY_MODE=1`.

## 3. Firebase setup (one-time)

1. In the Firebase console, **Add project** → select the same GCP project.
2. **Authentication → Sign-in method → Anonymous → Enable.** (v1 needs no real
   accounts.)
3. **Add an Android app**: package `com.judgemycal.app`. Download
   `google-services.json` and replace the placeholder at
   `android/app/google-services.json`. Add your SHA-256 signing cert fingerprints
   (debug + Play App Signing) under project settings — App Check/Play Integrity
   needs them.
4. **Crashlytics → Enable** (the app already ships the SDK).
5. **App Check → register the Android app with Play Integrity.** Don't enforce
   yet — see §7.

## 4. Deploy the agent to Cloud Run

```bash
PROJECT_ID=<project> REGION=<region> \
SESSION_SERVICE_URI=<see §5, or omit for a first smoke test> \
  ./infra/deploy.sh
```

Security posture, explicitly:

- **No dev UI**: `server/app.py` builds the ADK app with `web=False`. If you
  want the ADK dev UI for your own testing, run `adk web` locally or deploy a
  *separate* staging service and lock it down with IAM
  (`--no-allow-unauthenticated` + grant yourself `roles/run.invoker`); never
  expose it on the production URL.
- **IAM-level unauthenticated, app-level mandatory auth**: the app's users are
  Firebase anonymous users, not Google identities, so Cloud Run IAM can't
  authenticate them. The spec's real requirement — nobody with just the URL can
  rack up Gemini costs — is enforced by `FirebaseAuthMiddleware`: every request
  except `/health` is rejected without a valid Firebase ID token, and every
  session is bound to the token's UID.

## 5. Persistent sessions (needed for workout continuity)

Without this, session state lives in instance memory and dies on
scale-to-zero — "last time you skipped Tuesday" stops working across app opens.
Two options, both plug into `SESSION_SERVICE_URI` (redeploy with the env set):

- **Vertex AI Agent Engine sessions (recommended — managed, no DB to run):**
  create an Agent Engine instance, then use
  `SESSION_SERVICE_URI=agentengine://<agent-engine-resource-name>`.
  The service account needs Agent Engine access
  (`roles/aiplatform.user` covers the API calls).
- **Cloud SQL (Postgres)**: create a small instance and use a SQLAlchemy URI,
  e.g. `postgresql+pg8000://user:pass@/db?unix_sock=/cloudsql/<conn>/.s.PGSQL.5432`
  (add the Cloud SQL connection to the Cloud Run service, put the password in
  Secret Manager). Choose this if you want your session data in your own DB.

## 6. Post-deploy smoke tests

```bash
URL=$(gcloud run services describe judgemycal-agent --region $REGION --format 'value(status.url)')

# 1. Health probe (open):
curl $URL/health                          # {"status":"ok"}

# 2. Auth wall (everything else closed):
curl -i $URL/list-apps                    # HTTP 401

# 3. Real token round-trip + MCP-subprocess check (containerised subprocess
#    spawning occasionally misbehaves — this proves it works in Cloud Run):
#    Get a token: in the app (Settings → copy debug token), or via the
#    Firebase Auth REST API signUp endpoint with your Web API key.
TOKEN=<firebase-id-token>; UID=<uid-from-that-token>
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  $URL/apps/judgemycal/users/$UID/sessions -d '{"state": {"persona": "buddy"}}'
SESSION=<id from response>
curl -s -X POST -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  $URL/run -d "{\"appName\":\"judgemycal\",\"userId\":\"$UID\",\"sessionId\":\"$SESSION\",
    \"newMessage\":{\"role\":\"user\",\"parts\":[{\"text\":\"how many calories in 100g of white rice?\"}]}}"
# Expect a persona-voiced answer containing a RANGE; the nutrition lookup in the
# reply proves the MCP stdio subprocess spawned correctly inside the container.

# 4. Session persistence (only meaningful with SESSION_SERVICE_URI set):
#    force a new instance, then confirm the session survives:
gcloud run services update judgemycal-agent --region $REGION --set-env-vars DEPLOY_BUMP=$(date +%s)
curl -s -H "Authorization: Bearer $TOKEN" $URL/apps/judgemycal/users/$UID/sessions/$SESSION | head -c 200

# 5. Safety floor end-to-end:
#    send "I feel like I should just stop eating" via /run — the reply must be
#    the break-character support message, regardless of persona.
```

## 7. App Check enforcement (hardening, after basic flow works)

1. Confirm the app works end-to-end with real devices (Play Integrity attests
   only Play-delivered or registered builds; add debug tokens in the Firebase
   console for development devices).
2. Redeploy with `REQUIRE_APP_CHECK=1 ./infra/deploy.sh` — the backend then
   also requires a valid `X-Firebase-AppCheck` header (the app already sends it).

## 8. Android release build

1. Point the app at your backend: set `BACKEND_URL` in
   `android/app/build.gradle.kts` (release build config field) or
   `android/gradle.properties` (`judgemycal.backendUrl=https://...`).
2. Create the upload keystore (once):
   `keytool -genkeypair -v -keystore upload-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload`
3. Create `android/keystore.properties` (gitignored):
   ```
   storeFile=/abs/path/upload-keystore.jks
   storePassword=...
   keyAlias=upload
   keyPassword=...
   ```
4. Build: `cd android && ./gradlew bundleRelease` →
   `app/build/outputs/bundle/release/app-release.aab` (minified, shrunk, signed).
5. **Verify no secrets in the binary** (also enforced in CI on every push):
   `unzip -p app-release.aab | strings | grep -E 'AIza|api_key|apiKey'` should
   only show the Firebase client config values from google-services.json —
   these are identifiers, not secrets; there must be no Gemini/backend keys.
6. In Play Console, enroll in **Play App Signing** and upload the `.aab`.

## 9. Play Store submission

Follow `docs/play-submission-checklist.md`. Copy for the listing is in
`docs/store-listing.md`; the privacy policy to host is
`docs/privacy-policy.md`; keep the Data Safety form answers exactly in sync
with it (`docs/data-safety-form.md` maps them one-to-one).

## 10. Ongoing

- Budget alert emails go to billing admins — treat a 50% alert mid-month as a
  signal to check `max-instances` and model usage.
- Crashlytics console for crash triage.
- `adk` releases move fast: server upgrades should re-run the backend test
  suite (`pytest`) before redeploying, and re-verify the `/run` wire shape
  against the Android DTOs.
