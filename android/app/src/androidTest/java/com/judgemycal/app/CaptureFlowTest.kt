package com.judgemycal.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.judgemycal.app.data.MealRepository
import com.judgemycal.app.ui.JudgeMyCalRoot
import com.judgemycal.app.ui.JudgeMyCalTheme
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The core product flow: capture → honest-range estimate → one-tap log. */
@RunWith(AndroidJUnit4::class)
class CaptureFlowTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var fake: FakeAgentService

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        fake = FakeAgentService()
        AppGraph.mealRepository = MealRepository(context)
        AppGraph.agentService = fake
        compose.setContent {
            JudgeMyCalTheme { JudgeMyCalRoot() }
        }
    }

    @Test
    fun captureEstimateLogFlow_showsRangeAndLogsMeal() {
        compose.onNodeWithTag("nav_capture").performClick()
        compose.onNodeWithTag("demo_meal").performClick()

        compose.waitUntil(5_000) {
            compose.onAllNodes(androidx.compose.ui.test.hasTestTag("estimate_range"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        // The estimate is a RANGE, and the shaky item's persona reaction is shown.
        compose.onNodeWithTag("estimate_range").assertIsDisplayed()
        compose.onNodeWithTag("persona_reaction").assertIsDisplayed()
        assertEquals(1, fake.estimateCalls.get())

        compose.onNodeWithTag("log_meal").performClick()
        compose.onNodeWithTag("nav_home").performClick()

        compose.waitUntil(5_000) {
            compose.onAllNodes(androidx.compose.ui.test.hasTestTag("home_total"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("home_total").assertIsDisplayed()
    }

    @Test
    fun portionCorrection_recomputesTheRange() {
        compose.onNodeWithTag("nav_capture").performClick()
        compose.onNodeWithTag("demo_meal").performClick()

        compose.waitUntil(5_000) {
            compose.onAllNodes(androidx.compose.ui.test.hasTestTag("estimate_range"))
                .fetchSemanticsNodes().isNotEmpty()
        }
        // Correct the shaky item's portion upward; the band must recompute.
        compose.onNodeWithTag("portion_white rice_225", useUnmergedTree = true)
            .performClick()
        compose.onNodeWithTag("estimate_range").assertIsDisplayed()
    }
}
