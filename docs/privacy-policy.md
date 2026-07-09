# JudgeMyCal Privacy Policy

_Last updated: 2026-07-09_

> **Hosting note (remove this block before publishing):** Google Play requires
> this policy at a stable public URL. Easiest path: enable GitHub Pages on this
> repository and link to this page, or paste it into any static host. The URL
> goes in Play Console → Store presence → Privacy policy, and must stay live.

JudgeMyCal ("the app") is a wellness companion that helps you log meals,
consider food swaps, and plan workouts, with an AI companion persona. This
policy describes exactly what the app collects and what happens to it.

## What we collect

- **Meal photos** you choose to take or pick. Photos are sent to our backend
  service to produce a calorie estimate and are not used for anything else.
- **Free-text notes and inputs** you type: meal notes, shopping basket items,
  workout goals and equipment.
- **Your activity in the app**: logged meals (name and calorie range), workout
  sessions you generate, and your chosen companion persona.
- **An anonymous account identifier.** The app signs you in anonymously with
  Firebase Authentication; we do not ask for your name, email address, or phone
  number, and we do not know who you are.
- **Crash data** via Firebase Crashlytics (device model, OS version, stack
  traces) to fix bugs.

## What we do NOT collect

- No name, email, phone number, contacts, or precise location.
- No advertising identifiers; the app contains no ads and no ad SDKs.
- No health records; nothing is read from or written to Health Connect or any
  other health platform.

## How your data is processed

Photos, notes, and inputs are sent over TLS to our backend service (hosted on
Google Cloud Run) where an AI agent — built on Google's AI models (Gemini via
Google Cloud Vertex AI) — produces estimates, swap suggestions, and workout
plans. Your conversation state (persona choice, meal log summary, recent
workouts) is stored server-side against your anonymous account id so the
companion can remember context between sessions.

Your meal log and workout history are also stored locally on your device.

## Safety

If your messages suggest distress around food, body image, or self-harm, the
app and backend are designed to stop the companion persona and show a
supportive message encouraging you to seek real help. This detection runs
automatically as part of processing your inputs; it is a safety feature, not
profiling, and it is never used for any other purpose.

## Sharing

We do not sell your data and we do not share it with third parties for
marketing. Data is processed only by our service providers as needed to run
the app: Google Cloud (backend hosting and AI model processing) and Firebase
(authentication, crash reporting, app integrity). Each acts as a processor
under their standard terms.

## Retention & deletion

- Local data (meal log, workout history, persona) can be removed at any time by
  clearing the app's storage or uninstalling.
- Server-side session data is retained only to provide continuity and can be
  deleted on request.
- Crash data is retained per Firebase Crashlytics' standard retention (90 days).

To request deletion of your server-side data, contact us with your request at
the support email listed on the app's Play Store page. Because your account is
anonymous, deletion requests are handled by clearing the sessions associated
with your app installation.

## Children

JudgeMyCal is not directed at children under 13, and calorie tracking is not
appropriate for young children. Per our content rating, the app is intended
for teens and adults.

## Not medical advice

JudgeMyCal is a wellness companion, not a medical or diagnostic tool. Calorie
figures are honest estimates with explicit uncertainty ranges, and nothing in
the app constitutes medical, nutritional, or psychological advice.

## Changes

We will update this page and its "last updated" date when this policy changes
materially.
