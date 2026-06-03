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
package com.google.jetpackcamera.ui.uistate.postcapture

import android.graphics.Rect

sealed interface ImageTextUiState {
    data object Idle : ImageTextUiState

    data object Running : ImageTextUiState

    data object Empty : ImageTextUiState

    data object Error : ImageTextUiState

    data class Ready(
        val fullText: String,
        val imageWidth: Int,
        val imageHeight: Int,
        val words: List<Word>
    ) : ImageTextUiState

    data class Word(
        val text: String,
        val boundingBox: Rect,
        val lineIndex: Int,
        val wordIndexInLine: Int
    )

    companion object
}
