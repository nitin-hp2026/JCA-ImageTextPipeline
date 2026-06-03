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
package com.google.jetpackcamera.data.media

import android.graphics.Rect
import android.net.Uri
import kotlinx.coroutines.flow.Flow

interface ImageTextRecognitionRepository {
    fun observe(uri: Uri): Flow<ImageTextRecognitionState>

    suspend fun startRecognition(uri: Uri)

    suspend fun copyRecognition(sourceUri: Uri, destinationUri: Uri)

    suspend fun clearRecognition(uri: Uri)
}

sealed interface ImageTextRecognitionState {
    data object Idle : ImageTextRecognitionState

    data object Running : ImageTextRecognitionState

    data object Empty : ImageTextRecognitionState

    data object Error : ImageTextRecognitionState

    data class Ready(val result: RecognizedTextResult) : ImageTextRecognitionState
}

data class RecognizedTextResult(
    val fullText: String,
    val imageWidth: Int,
    val imageHeight: Int,
    val words: List<RecognizedTextWord>
)

data class RecognizedTextWord(
    val text: String,
    val boundingBox: Rect,
    val lineIndex: Int,
    val wordIndexInLine: Int
)
