/**
 * Client-side mirror of the backend's structural safety floor (`safety.py`).
 *
 * The backend's before-model callback is the authority; this mirror gives the
 * same guarantee instantly in the browser — before any text leaves the page,
 * and even fully offline. Keep the patterns in sync with
 * `judgemycal-agent/judgemycal/safety.py` (and the Android SafetyGuard).
 */

const DISTRESS: RegExp[] = [
  /\bstarv(e|ing)\b/i,
  /haven'?t eaten/i,
  /stop(ped)? eating/i,
  /skip(ping)? meals/i,
  /\bnot eating\b/i,
  /barely eat/i,
  /punish myself/i,
  /purge/i,
  /purging/i,
  /throw(ing)? up/i,
  /ma(?:k|d)e myself sick/i,
  /vomit/i,
  /hate my body/i,
  /hate how i look/i,
  /i'?m disgusting/i,
  /i'?m so fat/i,
  /worthless/i,
  /hurt myself/i,
  /want to disappear/i,
  /don'?t want to be here/i,
  /end it all/i,
  /no reason to live/i,
];

const BANNED = ["disgusting", "lazy", "failure", "greedy", "worthless"];

export const SUPPORT_MESSAGE =
  "I'm going to step out of character for a moment, because what you said matters more " +
  "than any number. You deserve real support from someone who can genuinely help — " +
  "please consider reaching out to a doctor, a mental-health professional, or someone " +
  "you trust. If you might be in immediate danger, contact your local emergency services " +
  "right away. I can't be that support on my own, but I didn't want to just carry on as " +
  "if you hadn't said it.";

export function isDistress(text: string | null | undefined): boolean {
  if (!text) return false;
  return DISTRESS.some((p) => p.test(text));
}

/** Neutralise any punitive term in a drafted reply (defence in depth). */
export function governText(text: string): string {
  return BANNED.reduce(
    (acc, term) => acc.replace(new RegExp(`\\b${term}\\b`, "gi"), "—"),
    text,
  );
}
