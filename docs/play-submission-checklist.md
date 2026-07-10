# Play Store Submission Checklist

Ordered; items marked ⏱ have external wait times — start them first.

1. ⏱ **Developer account + identity verification** — create the Play Console
   account and start identity verification immediately (24–48 h for
   individuals). D-U-N-S not needed for individual accounts.
2. **App creation** — package `com.judgemycal.app`, app name per
   `store-listing.md`, Health & Fitness category, free, no ads.
3. **Play App Signing** — accept enrolment when uploading the first `.aab`
   (RUNBOOK §8 builds it). Keep the upload keystore backed up; add both the
   upload and Play-signing SHA-256 fingerprints to Firebase (RUNBOOK §3.3).
4. ⏱ **Privacy policy** — host `privacy-policy.md` at a stable URL, set it in
   Store presence → Privacy policy.
5. **Data Safety form** — enter exactly the answers in `data-safety-form.md`.
   Re-check against the privacy policy before submitting; they must agree.
6. **Content rating questionnaire** — category "Utility/Productivity/Health";
   answer No to violence/sexuality/etc. Expect a Teen-or-lower rating. The
   app shows no user-generated content from other users.
7. **Health apps declaration** — Play's health-apps policy requires declaring
   health features: declare **wellness/fitness tracking** (food diary +
   workouts). The app is NOT a medical device and makes no clinical claims —
   the in-app disclaimer (settings/persona sheet) and store copy already say
   this; keep them consistent.
8. **Store listing** — copy + assets per `store-listing.md`. Screenshot the
   safety-floor support message as one of the screenshots; for
   eating-disorder-adjacent policy review, showing the protective design is an
   asset, not a risk.
9. **Target audience** — 13+ (calorie tracking is not for children; the
   listing and content rating should both reflect this).
10. **Release** — upload the signed `.aab` to **Internal testing** first, smoke
    test on a real device against the production backend (RUNBOOK §6 checks,
    plus airplane-mode mid-session to see the graceful offline fallback), then
    promote to Production review.
11. **Pre-launch report** — after upload, review Play's automated device test
    results; fix any crashes before promoting.
12. **Post-approval** — verify Crashlytics is receiving events from production
    builds; confirm the GCP budget alert is armed (RUNBOOK §10).

## Policy areas that get extra scrutiny for this app

- **Health misinformation / medical claims**: none made; estimates are ranges
  with explicit uncertainty. Keep it that way in future copy.
- **Eating-disorder adjacency**: the personas' teasing is aimed at meals, never
  the person; the safety floor breaks character on distress signals. If a
  reviewer questions the "judgy" framing, point to the non-overridable safety
  floor (structural, tested in CI) and the store copy's supportive tone.
- **Data**: photos leave the device — the privacy policy and Data Safety form
  both disclose this clearly and identically.
