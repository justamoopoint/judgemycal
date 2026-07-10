# JudgeMyCal

A personality-led AI health companion: photo calorie tracking with **honest
confidence bands**, goal-aligned shopping swaps, adaptive workouts — voiced by
a persona you choose (**Buddy** by default; Auntie and Coach opt-in), behind a
**safety floor no persona can override**.

## Repository layout

| Directory | What it is |
| --- | --- |
| [`judgemycal-agent/`](judgemycal-agent/) | The brain: multi-agent system on Google ADK (orchestrator + cv_calorie / health_shopping / workout_coach), MCP nutrition server, structural safety floor, and the production FastAPI server (`server/`) that fronts it with Firebase auth on Cloud Run |
| [`android/`](android/) | The Android client: Kotlin + Jetpack Compose, SDK 35, all three capabilities against the hosted backend with a graceful on-device offline fallback |
| [`webapp/`](webapp/) | The web client: React + Vite + TypeScript on Firebase Hosting — same capabilities, same wire contract, same in-browser offline fallback |
| [`infra/`](infra/) | GCP setup + Cloud Run deploy scripts (dedicated minimal-permission service account, Vertex AI mode — no API key exists anywhere, budget alert) |
| [`docs/`](docs/) | [RUNBOOK](docs/RUNBOOK.md) (deploy + release, step by step), privacy policy, Play Data Safety answers, store listing copy, submission checklist |

## Architecture

```
Android app (Compose, SDK 35)      Web app (React + Vite, Firebase Hosting)
   │  Firebase anonymous auth → ID token (+ App Check); CORS for the browser
   ▼
Cloud Run: server/ = ADK FastAPI app + FirebaseAuthMiddleware
   │  sessions bound to Firebase UID; persona in session state
   ▼
judgemycal (orchestrator, LlmAgent — Gemini via Vertex AI)
   ├─ cv_calorie       photo → honest range estimate; MCP nutrition tools
   ├─ health_shopping  neutral swaps; never purchases
   └─ workout_coach    adaptive sessions; respects rest
        ▲                        ▲
        │ persona voice (state)  │ MCP: verified nutrition DB (stdio subprocess)
        └─ SAFETY FLOOR: before/after-model callbacks on EVERY agent ─┘
```

Two invariants hold end to end, and are enforced by tests on both sides:

1. **Every estimate is a range, never a fake-precise number** — the band comes
   from per-item portion confidence, and the meal's confidence is its *lowest*
   item's, never an average.
2. **The safety floor is structural.** A distress signal short-circuits before
   any model (or persona) is consulted: on the backend via
   `before_model_callback`, on the device via `SafetyGuard` before a request
   even leaves the phone. Covered by pytest and an instrumented Compose test.

The app never talks to a model provider directly — only to our backend — so no
model API key can exist in the binary; CI decompiles each release build to
prove it.

## Getting started

- **Run the agent locally** — see [`judgemycal-agent/README.md`](judgemycal-agent/README.md)
  (`demo_offline.py` needs no key; `adk run judgemycal` for the live agent).
- **Backend tests** — `cd judgemycal-agent && pip install -r requirements-dev.txt && pytest`.
- **Web** — `cd webapp && npm ci && npm run dev` (offline demo mode with no
  config; see `webapp/.env.example` to point it at a backend).
- **Android** — open `android/` in Android Studio, or `./gradlew build`.
  Set `judgemycal.backendUrl` in `android/gradle.properties` to your deployed
  backend; empty means offline demo mode.
- **Deploy + ship** — follow [`docs/RUNBOOK.md`](docs/RUNBOOK.md) end to end,
  then [`docs/play-submission-checklist.md`](docs/play-submission-checklist.md).

## CI

Every push/PR runs: backend pytest · Android build + unit tests + lint ·
R8-minified release bundle with a decompiled secret scan · instrumented
emulator tests · web vitest + build + Playwright e2e (both platforms cover
the capture→estimate→log flow and the safety-floor break-character path).
