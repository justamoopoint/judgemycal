# Play Console Data Safety Form — answers

Fill the Data Safety questionnaire with exactly these answers. They mirror
`privacy-policy.md` line for line — the #1 avoidable rejection is these two
disagreeing.

## Overview questions

| Question | Answer |
| --- | --- |
| Does your app collect or share any of the required user data types? | **Yes** |
| Is all of the user data collected by your app encrypted in transit? | **Yes** (TLS everywhere) |
| Do you provide a way for users to request that their data is deleted? | **Yes** (contact-based deletion; local data via uninstall) |

## Data types collected

### Photos and videos → Photos
- Collected: **Yes** · Shared: **No**
- Processed ephemerally: **Yes** (sent for estimation, not retained as media)
- Required or optional: **Optional** (demo mode and manual logging work without)
- Purpose: **App functionality**

### Health and fitness → Fitness info
- Collected: **Yes** (workout goals/sessions) · Shared: **No**
- Purpose: **App functionality**

### Health and fitness → Health info
- Collected: **Yes** (meal/calorie log — food diary data) · Shared: **No**
- Purpose: **App functionality**

### App activity → Other user-generated content
- Collected: **Yes** (meal notes, basket items) · Shared: **No**
- Purpose: **App functionality**

### App info and performance → Crash logs, Diagnostics
- Collected: **Yes** (Crashlytics) · Shared: **No**
- Purpose: **Analytics** (crash triage)

### Device or other IDs → Device or other IDs
- Collected: **Yes** (anonymous Firebase auth UID; App Check attestation) · Shared: **No**
- Purpose: **App functionality**, **Fraud prevention, security, and compliance**

## Explicitly NOT collected (answer "No" everywhere else)

Name, email, phone, address, precise/approximate location, contacts, financial
info, browsing history, search history outside the app, installed apps, SMS,
files, calendar, microphone/voice, music, purchase history, advertising ID.

## Notes for reviewers (if asked)

- AI processing happens on our own backend (Google Cloud Run + Vertex AI),
  acting as our processor — this is "collection", not "sharing", under Play's
  definitions.
- The app has no ads, no analytics SDKs beyond Crashlytics, and no data sale.
