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

import android.content.Context
import android.graphics.Rect
import android.net.Uri
import com.google.jetpackcamera.core.common.IODispatcher
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@Singleton
class LocalImageTextRecognitionRepository @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IODispatcher private val ioDispatcher: CoroutineDispatcher
) : ImageTextRecognitionRepository {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    private val recognitionStates =
        MutableStateFlow<Map<String, ImageTextRecognitionState>>(emptyMap())

    override fun observe(uri: Uri): Flow<ImageTextRecognitionState> =
        recognitionStates
            .map { states -> states[uri.toString()] ?: ImageTextRecognitionState.Idle }
            .distinctUntilChanged()

    override suspend fun startRecognition(uri: Uri) {
        val key = uri.toString()
        when (recognitionStates.value[key]) {
            ImageTextRecognitionState.Empty,
            ImageTextRecognitionState.Running,
            is ImageTextRecognitionState.Ready -> return

            ImageTextRecognitionState.Error,
            ImageTextRecognitionState.Idle,
            null -> Unit
        }

        updateRecognitionState(uri, ImageTextRecognitionState.Running)

        val state = withContext(ioDispatcher) {
            runCatching {
                val inputImage = InputImage.fromFilePath(context, uri)
                val recognitionResult = recognizer.process(inputImage).await()
                val fullText = recognitionResult.text.trim()
                if (fullText.isBlank()) {
                    ImageTextRecognitionState.Empty
                } else {
                    val words = mutableListOf<RecognizedTextWord>()

                    // Try to extract individual word elements
                    recognitionResult.textBlocks.forEachIndexed { blockIndex, block ->
                        block.lines.forEachIndexed { lineIndex, line ->
                            line.elements.forEachIndexed { wordIndex, element ->
                                element.boundingBox?.let { boundingBox ->
                                    element.text.trim().takeIf { it.isNotBlank() }?.let { text ->
                                        words.add(
                                            RecognizedTextWord(
                                                text = text,
                                                boundingBox = Rect(boundingBox),
                                                lineIndex = blockIndex * 10_000 + lineIndex,
                                                wordIndexInLine = wordIndex
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Fallback: if no elements found, split block text
                    if (words.isEmpty()) {
                        recognitionResult.textBlocks.forEachIndexed { blockIndex, block ->
                            block.boundingBox?.let { boundingBox ->
                                block.text.trim().takeIf { it.isNotBlank() }?.let { text ->
                                    text.split(Regex("\\s+"))
                                        .filter { it.isNotBlank() }
                                        .forEachIndexed { wordIndex, word ->
                                            words.add(
                                                RecognizedTextWord(
                                                    text = word,
                                                    boundingBox = Rect(boundingBox),
                                                    lineIndex = blockIndex,
                                                    wordIndexInLine = wordIndex
                                                )
                                            )
                                        }
                                }
                            }
                        }
                    }

                    ImageTextRecognitionState.Ready(
                        RecognizedTextResult(
                            fullText = fullText,
                            imageWidth = inputImage.width,
                            imageHeight = inputImage.height,
                            words = words
                        )
                    )
                }
            }.getOrElse {
                ImageTextRecognitionState.Error
            }
        }

        updateRecognitionState(uri, state)
    }

    override suspend fun copyRecognition(sourceUri: Uri, destinationUri: Uri) {
        recognitionStates.value[sourceUri.toString()]?.let { state ->
            updateRecognitionState(destinationUri, state)
        }
    }

    override suspend fun clearRecognition(uri: Uri) {
        recognitionStates.value = recognitionStates.value - uri.toString()
    }

    private fun updateRecognitionState(uri: Uri, state: ImageTextRecognitionState) {
        recognitionStates.value = recognitionStates.value + (uri.toString() to state)
    }
}
