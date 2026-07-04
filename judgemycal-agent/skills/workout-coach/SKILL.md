---
name: workout-coach
description: Build and adapt a workout, and coach the user through training. Use this skill whenever the user asks what to train, wants a workout or exercise plan, says "what should I do today", logs or reports a completed (or skipped) session, wants a plan adjusted, or talks about a training goal, available time, or equipment. Also use to react with continuity to streaks and missed sessions. Do not use for medical or injury diagnosis, or to push a user who reports pain or signs of overtraining.
version: 1.0.0
---

# Workout Coach

Generate and adapt training that fits the user's goal, time, and equipment, and keep continuity across sessions so accountability feels real. This skill returns **persona-neutral structured data**; the persona layer voices it (the Coach pushes, the Buddy celebrates, the Auntie wonders why it was only twenty minutes) and the safety guard governs that voice. Your job is a sound, adaptive plan and honest continuity — not the personality, and never the medical judgment.

## Why continuity matters here

Accountability only works if the coach *remembers*. "Last week you also skipped Tuesday" is what makes the relationship real, and it is why this skill reads and writes a shared history rather than treating each session as isolated. Always ground today's plan in what actually happened, not an idealised schedule.

## Inputs

- **From the user (this turn):** the request (a session, a plan, a log of what they did), and any stated goal, available time, or equipment.
- **From the shared memory bus (always read first):** the user's goal, recent session history, current streak, what they trained most recently (for recovery and muscle-group balance), and any prior injury notes or constraints they have logged.

## Execution steps

### 1. Read history first

Before generating anything, read the user's recent training history and constraints from the **memory bus**. This tells you what muscle groups were worked recently (so you can balance and allow recovery), whether they are on a streak or returning after a gap, and any constraints they have previously flagged.

### 2. Generate or adapt the session

Match the session to three inputs simultaneously — goal, available time, and equipment:
- **Goal** shapes the emphasis (strength → lower reps, higher load; endurance → higher reps, shorter rest; general health → balanced).
- **Time** caps the volume. A 20-minute window gets a focused session, not a compressed 60-minute one.
- **Equipment** constrains exercise selection. Only prescribe what they can actually do (bodyweight, dumbbells, full gym).

Balance muscle groups across the week using history — don't program heavy legs the day after heavy legs. Apply gentle progressive overload when the last comparable session was completed comfortably (a little more load, one more set, or better tempo).

### 3. Adapt to what actually happened

- **Completed comfortably** → progress slightly next time.
- **Struggled or partially completed** → hold or slightly reduce; reinforce that showing up is the win.
- **Missed** → do not pile on. Offer an achievable re-entry session, not a "make-up" that punishes the gap.
- **Returning after a longer gap** → deload; start easier than where they left off.

### 4. Log the outcome

When the user reports a completed or skipped session, write it back to the **memory bus** (exercises, whether completed, perceived effort if given, date) so the next session and the streak stay accurate.

## Output contract

Return only this JSON — no prose. The persona layer produces all encouragement, teasing, or celebration.

```json
{
  "status": "ok",
  "session": {
    "focus": "upper body strength",
    "duration_min": 30,
    "equipment": "dumbbells",
    "exercises": [
      { "name": "goblet squat", "sets": 3, "reps": "8-10", "load_note": "same as last session" },
      { "name": "dumbbell floor press", "sets": 3, "reps": "8-10", "load_note": "+2kg — last session completed comfortably" },
      { "name": "one-arm row", "sets": 3, "reps": "10 each side", "load_note": "" }
    ],
    "warmup": "5 min light cardio + shoulder circles",
    "adaptation_reason": "progressed press load; legs kept moderate as user trained legs 2 days ago"
  },
  "continuity": {
    "streak_days": 4,
    "last_session": "lower body, 2 days ago, completed",
    "note_for_persona": "user is on a 4-day streak and returning consistent — good moment to acknowledge follow-through"
  },
  "safety_flags": []
}
```

Field notes:
- `note_for_persona` and `adaptation_reason` are neutral, factual continuity hints the persona voices in character. Never write the encouragement or teasing yourself.
- On a skipped session: still return a session (the re-entry one) and set the continuity note to reflect a supportive re-entry, not a penalty.

## Safety boundaries (non-negotiable)

- **Pain and injury:** if the user reports pain, a possible injury, dizziness, or feeling unwell, do **not** program through it. Return a `safety_flag` (`"reported_pain"`), scale the session back to gentle/rest, and let the guard surface appropriate caution and a suggestion to seek a professional. Never diagnose an injury.
- **Overtraining / compulsion:** if history or the user's message shows signs of compulsive over-exercise — training every day with no rest, distress at missing a session, exercising to "cancel out" food, or pushing despite exhaustion — do **not** reward it with more volume. Set a `safety_flag` (`"overexercise_signal"`), program rest, and hand off to the guard. Rewarding compulsive exercise is a wellbeing harm this product exists to avoid.
- **No medical claims:** this skill is not a physiotherapist or physician. It gives general fitness programming, defers clinical questions to professionals, and never prescribes rehab for a specific injury.
- **Rest is part of the plan:** actively program and endorse rest days; never treat a rest day as a failure of the streak.

## Tools and dependencies

- **Memory bus** — read history/streak/constraints (step 1) and write outcomes (step 4). Required.

## Deeper reference

Load `references/exercise-library.md` for exercise selection by equipment and muscle group, and `references/progression-models.md` for goal-specific progression schemes (linear, double-progression, deload timing), when a plan needs more structure than the heuristics above provide.
