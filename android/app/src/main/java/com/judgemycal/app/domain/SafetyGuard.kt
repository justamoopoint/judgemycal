package com.judgemycal.app.domain

/**
 * Client-side mirror of the backend's structural safety floor (`safety.py`).
 *
 * The backend's before-model callback is the authority — no persona can be
 * consulted on a distress turn, period. This mirror exists so the same
 * guarantee holds instantly on-device: before any text leaves the phone, and
 * even fully offline in fallback mode. Keep the patterns in sync with
 * `judgemycal-agent/judgemycal/safety.py`.
 */
object SafetyGuard {

    private val DISTRESS = listOf(
        "\\bstarv(e|ing)\\b", "haven'?t eaten", "stop(ped)? eating", "skip(ping)? meals",
        "\\bnot eating\\b", "barely eat", "punish myself",
        "purge", "purging", "throw(ing)? up", "ma(?:k|d)e myself sick", "vomit",
        "hate my body", "hate how i look", "i'?m disgusting", "i'?m so fat", "worthless",
        "hurt myself", "want to disappear", "don'?t want to be here", "end it all",
        "no reason to live",
    ).map { Regex(it, RegexOption.IGNORE_CASE) }

    private val BANNED = listOf("disgusting", "lazy", "failure", "greedy", "worthless")

    const val SUPPORT_MESSAGE =
        "I'm going to step out of character for a moment, because what you said matters more " +
            "than any number. You deserve real support from someone who can genuinely help — " +
            "please consider reaching out to a doctor, a mental-health professional, or someone " +
            "you trust. If you might be in immediate danger, contact your local emergency services " +
            "right away. I can't be that support on my own, but I didn't want to just carry on as " +
            "if you hadn't said it."

    fun isDistress(text: String?): Boolean =
        !text.isNullOrEmpty() && DISTRESS.any { it.containsMatchIn(text) }

    /** Neutralise any punitive term in a drafted reply (defence in depth). */
    fun governText(text: String): String =
        BANNED.fold(text) { acc, term ->
            acc.replace(Regex("\\b$term\\b", RegexOption.IGNORE_CASE), "—")
        }
}
