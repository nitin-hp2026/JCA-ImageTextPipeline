/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.jetpackcamera.feature.postcapture.ui

import android.graphics.Bitmap
import android.graphics.Rect
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.jetpackcamera.ui.uistate.postcapture.ImageTextUiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PostCaptureScreenComponentsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun imageWithRecognizedText_tapShowsCopyAction() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        composeTestRule.setContent {
            MaterialTheme {
                ImageFromBitmap(
                    modifier = Modifier.size(200.dp),
                    bitmap = bitmap,
                    imageTextUiState = ImageTextUiState.Ready(
                        fullText = "Detected text",
                        imageWidth = 100,
                        imageHeight = 100,
                        words = listOf(
                            ImageTextUiState.Word(
                                text = "Detected",
                                boundingBox = Rect(0, 0, 45, 100),
                                lineIndex = 0,
                                wordIndexInLine = 0
                            ),
                            ImageTextUiState.Word(
                                text = "text",
                                boundingBox = Rect(50, 0, 100, 100),
                                lineIndex = 0,
                                wordIndexInLine = 1
                            )
                        )
                    )
                )
            }
        }

        composeTestRule.onNodeWithTag(VIEWER_POST_CAPTURE_TEXT_OVERLAY)
            .performTouchInput { click(center) }

        assertEquals(
            1,
            composeTestRule
                .onAllNodesWithTag(BUTTON_POST_CAPTURE_COPY_TEXT)
                .fetchSemanticsNodes().size
        )
    }
}
