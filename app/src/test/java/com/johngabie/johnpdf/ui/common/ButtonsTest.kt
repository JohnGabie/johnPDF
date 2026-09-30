package com.johngabie.johnpdf.ui.common

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.johngabie.johnpdf.ui.theme.JohnPdfTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ButtonsTest {
    @get:Rule val rule = createComposeRule()

    @Test fun primary_button_is_clickable_and_at_least_56dp_tall() {
        var clicked = false
        rule.setContent { JohnPdfTheme { PrimaryButton("Allow access", onClick = { clicked = true }) } }
        rule.onNodeWithText("Allow access").assertHeightIsAtLeast(56.dp).performClick()
        assertTrue(clicked)
    }

    @Test fun secondary_button_default_height_is_56dp() {
        rule.setContent { JohnPdfTheme { SecondaryButton("Open", onClick = {}) } }
        rule.onNodeWithText("Open").assertHeightIsAtLeast(56.dp)
    }

    @Test fun secondary_button_accepts_48dp_height_override() {
        rule.setContent { JohnPdfTheme { SecondaryButton("Open", onClick = {}, height = 48.dp) } }
        rule.onNodeWithText("Open").assertHeightIsAtLeast(48.dp)
    }

    @Test fun disabled_primary_button_stays_visible_and_not_clickable() {
        rule.setContent { JohnPdfTheme { PrimaryButton("Remove", onClick = {}, enabled = false) } }
        rule.onNodeWithText("Remove").assertIsDisplayed().assertIsNotEnabled()
    }

    @Test fun only_one_primary_button_tagged_per_surface() {
        rule.setContent { JohnPdfTheme { PrimaryButton("Remove", onClick = {}) } }
        rule.onAllNodesWithTag("primary_button").assertCountEquals(1)
    }
}
