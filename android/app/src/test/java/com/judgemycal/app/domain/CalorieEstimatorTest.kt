package com.judgemycal.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalorieEstimatorTest {

    private val estimates = (0L until 25L).map { CalorieEstimator.estimate(seed = it) }

    @Test
    fun `every estimate is a range never a point`() {
        estimates.forEach { est ->
            assertTrue(est.totalLow < est.totalKcal)
            assertTrue(est.totalKcal < est.totalHigh)
        }
    }

    @Test
    fun `totals are item sums`() {
        estimates.forEach { est ->
            assertEquals(est.items.sumOf { it.kcal }, est.totalKcal)
            assertEquals(est.items.sumOf { it.kcalLow }, est.totalLow)
            assertEquals(est.items.sumOf { it.kcalHigh }, est.totalHigh)
        }
    }

    @Test
    fun `overall confidence is the lowest item's, never an average`() {
        estimates.forEach { est ->
            val worst = est.items.maxOf { it.confidence.ordinal }
            assertEquals(worst, est.overallConfidence.ordinal)
        }
    }

    @Test
    fun `low-confidence meals name their shakiest item`() {
        estimates.filter { it.overallConfidence == Confidence.LOW }.forEach { est ->
            val lowNames = est.items.filter { it.confidence == Confidence.LOW }.map { it.name }
            assertTrue(est.lowestConfidenceItem in lowNames)
        }
    }

    @Test
    fun `fallback estimates are flagged as such`() {
        estimates.forEach { assertTrue(it.fromFallback) }
    }

    @Test
    fun `portion correction recomputes the corrected item and total honestly`() {
        val est = estimates.first()
        val target = est.items.first()
        val corrected = CalorieEstimator.withPortion(est, target.name, target.grams * 2)

        val newItem = corrected.items.first { it.name == target.name }
        assertEquals(target.grams * 2, newItem.grams)
        // The user confirmed the portion: confidence becomes HIGH, band tightens.
        assertEquals(Confidence.HIGH, newItem.confidence)
        assertTrue(newItem.kcal > target.kcal)
        // Totals still sum items, still a range.
        assertEquals(corrected.items.sumOf { it.kcal }, corrected.totalKcal)
        assertTrue(corrected.totalLow < corrected.totalKcal)
        assertTrue(corrected.totalKcal < corrected.totalHigh)
    }
}
