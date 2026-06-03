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
package com.google.jetpackcamera.data.media.testing

import android.net.Uri
import com.google.jetpackcamera.data.media.ImageTextRecognitionRepository
import com.google.jetpackcamera.data.media.ImageTextRecognitionState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeImageTextRecognitionRepository : ImageTextRecognitionRepository {
    private val states =
        MutableStateFlow<Map<String, ImageTextRecognitionState>>(emptyMap())

    val startRecognitionCalls = mutableListOf<Uri>()
    val copiedRecognitions = mutableListOf<Pair<Uri, Uri>>()
    val clearedRecognitions = mutableListOf<Uri>()

    var startRecognitionHandler: suspend (Uri) -> ImageTextRecognitionState? = { null }

    override fun observe(uri: Uri): Flow<ImageTextRecognitionState> =
        states.map { it[uri.toString()] ?: ImageTextRecognitionState.Idle }

    override suspend fun startRecognition(uri: Uri) {
        startRecognitionCalls += uri
        startRecognitionHandler(uri)?.let { state ->
            emitState(uri, state)
        }
    }

    override suspend fun copyRecognition(sourceUri: Uri, destinationUri: Uri) {
        copiedRecognitions += sourceUri to destinationUri
        states.value[sourceUri.toString()]?.let { state ->
            emitState(destinationUri, state)
        }
    }

    override suspend fun clearRecognition(uri: Uri) {
        clearedRecognitions += uri
        states.value = states.value - uri.toString()
    }

    fun emitState(uri: Uri, state: ImageTextRecognitionState) {
        states.value = states.value + (uri.toString() to state)
    }
}
