package com.mbougar.swapware.ui.auth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.mbougar.swapware.MainActivity
import com.mbougar.swapware.data.repository.AuthRepository
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

@HiltAndroidTest
class AuthFlowTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Inject
    lateinit var authRepository: AuthRepository

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun loginScreen_successfulLogin_navigatesToHomeScreen() {
        composeTestRule.onNodeWithText("SwapWare Login").assertIsDisplayed()

        composeTestRule.onNodeWithText("Email").performTextInput("test@test.com")
        composeTestRule.onNodeWithText("Password").performTextInput("password")

        composeTestRule.onNodeWithText("Login").performClick()

        composeTestRule.waitUntil(timeoutMillis = 5000) {
            composeTestRule.onAllNodesWithText("SwapWare").fetchSemanticsNodes().isNotEmpty()
        }
        composeTestRule.onNodeWithText("SwapWare").assertIsDisplayed()
    }
}
