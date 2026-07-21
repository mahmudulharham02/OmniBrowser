package com.example

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.ui.components.SearchBar
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SearchBarTest {

    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun typing_into_search_bar_works() {
        var submittedUrl: String? = null
        composeTestRule.setContent {
            MyApplicationTheme {
                SearchBar(
                    searchEngineUrl = "https://google.com/search?q=",
                    onSubmit = { submittedUrl = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("search_input")
            .performTextInput("github.com")

        composeTestRule.onNodeWithTag("search_input")
            .assert(hasText("github.com"))
    }

    @Test
    fun pressing_search_key_submits() {
        var submittedUrl: String? = null
        composeTestRule.setContent {
            MyApplicationTheme {
                SearchBar(
                    searchEngineUrl = "https://google.com/search?q=",
                    onSubmit = { submittedUrl = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("search_input")
            .performTextInput("github.com")

        composeTestRule.onNodeWithTag("search_input")
            .performImeAction()

        assertEquals("https://github.com", submittedUrl)
    }

    @Test
    fun go_button_submits() {
        var submittedUrl: String? = null
        composeTestRule.setContent {
            MyApplicationTheme {
                SearchBar(
                    searchEngineUrl = "https://google.com/search?q=",
                    onSubmit = { submittedUrl = it }
                )
            }
        }

        composeTestRule.onNodeWithTag("search_input")
            .performTextInput("github.com")

        composeTestRule.onNodeWithTag("search_go_button")
            .performClick()

        assertEquals("https://github.com", submittedUrl)
    }

    @Test
    fun search_to_browser_navigation_works() {
        var submittedUrl: String? = null
        composeTestRule.setContent {
            MyApplicationTheme {
                HomeScreen(
                    totalAds = 12,
                    totalTrackers = 34,
                    totalPopups = 5,
                    searchEngineUrl = "https://google.com/search?q=",
                    onSearch = { submittedUrl = it },
                    onNavigateTo = { submittedUrl = it }
                )
            }
        }

        // 1. Start on home screen
        composeTestRule.onNodeWithTag("home_search_input").assertExists()

        // 2. Type a search
        composeTestRule.onNodeWithTag("home_search_input").performTextInput("github")

        // 3. Submit via IME action
        composeTestRule.onNodeWithTag("home_search_input").performImeAction()

        // 4. Verify search submit actually triggered the onSearch callback
        assertEquals("https://google.com/search?q=github", submittedUrl)
    }
}
