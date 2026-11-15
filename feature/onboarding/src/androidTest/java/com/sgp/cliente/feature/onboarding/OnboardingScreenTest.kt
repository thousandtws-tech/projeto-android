package com.sgp.cliente.feature.onboarding

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.sgp.cliente.core.designsystem.theme.SgpTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for [OnboardingContent]. The composable is driven directly
 * with callbacks so no ViewModel/Hilt graph is required.
 */
class OnboardingScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun setOnboardingContent(
        onFinish: () -> Unit = {},
        initialPage: Int = 0,
    ) {
        composeRule.setContent {
            SgpTheme {
                OnboardingContent(
                    brandName = "SGP Cliente",
                    onFinish = onFinish,
                    initialPage = initialPage,
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun firstPageTitleIsDisplayed() {
        setOnboardingContent()

        composeRule.onNodeWithText("Sua conexão.\nSeu controle.").assertIsDisplayed()
        composeRule
            .onNodeWithText("Sua internet, suas faturas e seu atendimento. Tudo em um só lugar.")
            .assertIsDisplayed()
    }

    @Test
    fun continueButtonAdvancesToSecondPage() {
        setOnboardingContent()

        composeRule.onNodeWithTag(OnboardingTestTags.CtaButton).performClick()
        composeRule.waitForIdle()

        composeRule
            .onNodeWithText("Menos burocracia.\nMais tempo pra você.")
            .assertIsDisplayed()
    }

    @Test
    fun dotsReflectCurrentPage() {
        setOnboardingContent()

        // HorizontalPager prefetches adjacent pages, so dots of off-screen
        // pages exist in the tree — assert on the displayed one only.
        composeRule.onNodeWithContentDescription("Passo 1 de 3").assertIsDisplayed()

        composeRule.onNodeWithTag(OnboardingTestTags.CtaButton).performClick()
        composeRule.waitForIdle()

        composeRule.onNodeWithContentDescription("Passo 2 de 3").assertIsDisplayed()
    }

    @Test
    fun lastPageShowsAccessAccountCta() {
        setOnboardingContent()

        composeRule.onNodeWithTag(OnboardingTestTags.CtaButton).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(OnboardingTestTags.CtaButton).performClick()
        composeRule.waitForIdle()

        composeRule
            .onNodeWithText("Perto de você.\nSempre que precisar.")
            .assertIsDisplayed()
        composeRule.onNodeWithText("Acessar minha conta").assertIsDisplayed()
    }

    @Test
    fun accessAccountOnLastPageInvokesOnFinish() {
        var finished = false
        setOnboardingContent(onFinish = { finished = true }, initialPage = 2)

        composeRule.onNodeWithText("Acessar minha conta").performClick()

        assertTrue(finished)
    }

    @Test
    fun skipButtonInvokesOnFinish() {
        var finished = false
        setOnboardingContent(onFinish = { finished = true })

        composeRule.onNodeWithTag(OnboardingTestTags.SkipButton).performClick()

        assertTrue(finished)
    }

    @Test
    fun alreadySubscriberButtonInvokesOnFinish() {
        var finished = false
        setOnboardingContent(onFinish = { finished = true })

        composeRule.onNodeWithTag(OnboardingTestTags.SubscriberButton).performClick()

        assertTrue(finished)
    }
}
