package com.judgemycal.app.domain

/**
 * On-device fallback for the health_shopping capability, ported from the
 * backend's `suggest_swaps`. Neutral, goal-aligned suggestions — nothing is
 * ever labelled "junk", "cheat", or "bad".
 */
object SwapEngine {

    private val SWAPS = mapOf(
        "white rice" to "brown rice or basmati — similar taste, steadier energy",
        "fries" to "roasted potato wedges — same comfort, less oil",
        "beef burger" to "a leaner turkey or grilled-chicken burger, if you fancy it",
        "naan" to "roti — lighter, still great with curry",
        "soda" to "sparkling water with lime",
    )

    fun suggestSwaps(items: List<String>): SwapResult {
        val out = items.mapNotNull { item ->
            val key = item.trim().lowercase()
            SWAPS.entries
                .firstOrNull { (k, _) -> k in key || key in k }
                ?.let { SwapSuggestion(item = item, swap = it.value) }
        }
        return SwapResult(
            swaps = out,
            note = "Suggestions only — your call, no pressure.",
            fromFallback = true,
        )
    }
}
