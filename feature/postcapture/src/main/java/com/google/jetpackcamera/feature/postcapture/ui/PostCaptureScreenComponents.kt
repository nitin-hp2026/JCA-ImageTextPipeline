/*
 * Copyright (C) 2025 The Android Open Source Project
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
import androidx.annotation.OptIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.ContentFrame
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import com.google.jetpackcamera.feature.postcapture.R
import com.google.jetpackcamera.ui.uistate.postcapture.ImageTextUiState
import com.google.jetpackcamera.ui.uistate.postcapture.MediaViewerUiState
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun MediaViewer(
    uiState: MediaViewerUiState,
    onLoadVideo: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        is MediaViewerUiState.Content.Image -> {
            ImageFromBitmap(
                modifier = modifier,
                bitmap = uiState.imageBitmap,
                imageTextUiState = uiState.imageTextUiState
            )
        }

        is MediaViewerUiState.Content.Video.Loading -> {
            Text(modifier = modifier, text = stringResource(R.string.loading_video_text))
        }

        is MediaViewerUiState.Content.Video.Ready -> {
            VideoPlayer(modifier = modifier, player = uiState.player)
            LaunchedEffect(Unit) {
                onLoadVideo()
            }
        }

        MediaViewerUiState.Loading -> {
            Text(modifier = modifier, text = stringResource(R.string.no_media_available))
        }

        MediaViewerUiState.Error -> {
            Text(modifier = modifier, text = stringResource(R.string.error_loading_media))
        }
    }
}

@Composable
fun ImageFromBitmap(
    modifier: Modifier,
    bitmap: Bitmap,
    imageTextUiState: ImageTextUiState = ImageTextUiState.Idle
) {
    val clipboardManager = LocalClipboardManager.current
    val selectionBoxState = remember(imageTextUiState) { mutableStateOf<SelectionBox?>(null) }
    val selectedWords =
        (imageTextUiState as? ImageTextUiState.Ready)
            ?.let { readyState ->
                selectionBoxState.value?.let { selection ->
                    readyState.words
                        .filter { word -> selection.intersects(word.boundingBox) }
                        .sortedWith(
                            compareBy(
                                ImageTextUiState.Word::lineIndex,
                                ImageTextUiState.Word::wordIndexInLine
                            )
                        )
                } ?: emptyList()
            } ?: emptyList()

    Box(modifier = modifier.testTag(VIEWER_POST_CAPTURE_IMAGE)) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = stringResource(R.string.post_capture_image_description),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize()
        )

        if (imageTextUiState is ImageTextUiState.Ready) {
            ImageTextSelectionOverlay(
                imageTextUiState = imageTextUiState,
                selectionBox = selectionBoxState.value,
                onSelectionBoxChanged = { selectionBoxState.value = it },
                modifier = Modifier.fillMaxSize()
            )

            val selectedText = buildSelectedText(selectedWords)
            if (selectedText.isNotBlank()) {
                SelectedImageTextCard(
                    text = selectedText,
                    onCopyClick = {
                        clipboardManager.setText(AnnotatedString(selectedText))
                    },
                    modifier = Modifier
                        .align(androidx.compose.ui.Alignment.BottomCenter)
                        .padding(24.dp)
                )
            }
        }
    }
}

@OptIn(UnstableApi::class)
@Composable
fun VideoPlayer(modifier: Modifier, player: Player?) {
    val presentationState = rememberPresentationState(player)
    ContentFrame(
        modifier = modifier
            .testTag(VIEWER_POST_CAPTURE_VIDEO)
            .resizeWithContentScale(
                ContentScale.Fit,
                presentationState.videoSizeDp
            ),
        player = player
    )
}

/**
 * A button to exit post capture.
 *
 * @param onExitPostCapture the action to be performed when the button is clicked.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExitPostCaptureButton(onExitPostCapture: () -> Unit, modifier: Modifier = Modifier) {
    PostCaptureIconButton(
        modifier = modifier
            .testTag(BUTTON_POST_CAPTURE_EXIT),
        onClick = onExitPostCapture
    ) {
        Icon(
            modifier = it,

            painter = painterResource(
                id = com.google.jetpackcamera.ui.components.capture.R.drawable.ic_close
            ),
            contentDescription = stringResource(R.string.button_exit_description)
        )
    }
}

@Composable
fun ShareCurrentMediaButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    PostCaptureIconButton(
        onClick = onClick,
        modifier = modifier
            .testTag(BUTTON_POST_CAPTURE_SHARE),
        enabled = enabled

    ) {
        Icon(
            modifier = it,

            painter = painterResource(id = R.drawable.ic_share),
            contentDescription = stringResource(R.string.button_share_media_description),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}

/**
 * A button to save the current media.
 *
 * @param onClick the action to be performed when the button is clicked.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SaveCurrentMediaButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    PostCaptureIconButton(
        modifier = modifier
            .testTag(BUTTON_POST_CAPTURE_SAVE),
        onClick = onClick
    ) {
        Icon(
            modifier = it,

            painter = painterResource(id = R.drawable.ic_save),
            contentDescription = stringResource(R.string.button_save_media_description)
        )
    }
}

@Composable
fun DeleteCurrentMediaButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    PostCaptureIconButton(
        modifier = modifier.testTag(BUTTON_POST_CAPTURE_DELETE),
        onClick = onClick,
        enabled = enabled
    ) {
        Icon(
            modifier = it,
            painter = painterResource(id = R.drawable.ic_delete),
            contentDescription = stringResource(
                R.string.button_delete_media_description
            )
        )
    }
}

@kotlin.OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun PostCaptureIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors? = null,
    content: @Composable (Modifier) -> Unit
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier
            .size(IconButtonDefaults.mediumContainerSize())
            .shadow(10.dp, CircleShape),
        colors = colors ?: IconButtonDefaults.iconButtonColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        content(Modifier.size(IconButtonDefaults.mediumIconSize))
    }
}

@Composable
private fun ImageTextSelectionOverlay(
    imageTextUiState: ImageTextUiState.Ready,
    selectionBox: SelectionBox?,
    onSelectionBoxChanged: (SelectionBox?) -> Unit,
    modifier: Modifier = Modifier
) {
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    val highlightFillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    val layout = remember(
        containerSize,
        imageTextUiState.imageWidth,
        imageTextUiState.imageHeight
    ) {
        calculateDisplayedImageLayout(
            containerSize = containerSize,
            imageWidth = imageTextUiState.imageWidth,
            imageHeight = imageTextUiState.imageHeight
        )
    }

    Box(
        modifier = modifier
            .onSizeChanged { containerSize = it }
            .pointerInput(imageTextUiState, layout) {
                detectTapGestures { tapOffset ->
                    val displayLayout = layout ?: return@detectTapGestures
                    imageTextUiState.words
                        .firstOrNull { word -> displayLayout.contains(word.boundingBox, tapOffset) }
                        ?.let { word ->
                            onSelectionBoxChanged(SelectionBox.fromRect(word.boundingBox))
                        } ?: onSelectionBoxChanged(null)
                }
            }
            .testTag(VIEWER_POST_CAPTURE_TEXT_OVERLAY)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val displayLayout = layout ?: return@Canvas
            val activeSelectionBox = selectionBox ?: return@Canvas
            val selectedWords = imageTextUiState.words.filter { word ->
                activeSelectionBox.intersects(word.boundingBox)
            }
            selectedWords.forEach { word ->
                val mappedRect = displayLayout.map(word.boundingBox)
                drawRect(
                    color = highlightFillColor,
                    topLeft = mappedRect.topLeft,
                    size = mappedRect.size
                )
            }
        }

        val displayLayout = layout
        val activeSelectionBox = selectionBox
        if (displayLayout != null && activeSelectionBox != null) {
            val mappedSelectionRect = displayLayout.map(activeSelectionBox)
            SelectionHandle(
                corner = SelectionCorner.TopLeft,
                position = mappedSelectionRect.topLeft,
                onDrag = { newPosition ->
                    onSelectionBoxChanged(
                        displayLayout.updateSelectionBox(
                            selectionBox = activeSelectionBox,
                            corner = SelectionCorner.TopLeft,
                            displayPosition = newPosition,
                            imageWidth = imageTextUiState.imageWidth,
                            imageHeight = imageTextUiState.imageHeight
                        )
                    )
                }
            )
            SelectionHandle(
                corner = SelectionCorner.TopRight,
                position = Offset(mappedSelectionRect.right, mappedSelectionRect.top),
                onDrag = { newPosition ->
                    onSelectionBoxChanged(
                        displayLayout.updateSelectionBox(
                            selectionBox = activeSelectionBox,
                            corner = SelectionCorner.TopRight,
                            displayPosition = newPosition,
                            imageWidth = imageTextUiState.imageWidth,
                            imageHeight = imageTextUiState.imageHeight
                        )
                    )
                }
            )
            SelectionHandle(
                corner = SelectionCorner.BottomLeft,
                position = Offset(mappedSelectionRect.left, mappedSelectionRect.bottom),
                onDrag = { newPosition ->
                    onSelectionBoxChanged(
                        displayLayout.updateSelectionBox(
                            selectionBox = activeSelectionBox,
                            corner = SelectionCorner.BottomLeft,
                            displayPosition = newPosition,
                            imageWidth = imageTextUiState.imageWidth,
                            imageHeight = imageTextUiState.imageHeight
                        )
                    )
                }
            )
            SelectionHandle(
                corner = SelectionCorner.BottomRight,
                position = mappedSelectionRect.bottomRight,
                onDrag = { newPosition ->
                    onSelectionBoxChanged(
                        displayLayout.updateSelectionBox(
                            selectionBox = activeSelectionBox,
                            corner = SelectionCorner.BottomRight,
                            displayPosition = newPosition,
                            imageWidth = imageTextUiState.imageWidth,
                            imageHeight = imageTextUiState.imageHeight
                        )
                    )
                }
            )
        }
    }
}

@Composable
private fun SelectedImageTextCard(
    text: String,
    onCopyClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.testTag(CARD_POST_CAPTURE_SELECTED_TEXT),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
        shape = RoundedCornerShape(20.dp),
        shadowElevation = 10.dp
    ) {
        Row(
            modifier = Modifier
                .background(Color.Transparent)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Box(modifier = Modifier.padding(end = 12.dp)) {
                SelectionContainer {
                    Text(
                        text = text,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            TextButton(
                onClick = onCopyClick,
                modifier = Modifier.testTag(BUTTON_POST_CAPTURE_COPY_TEXT)
            ) {
                Text(text = stringResource(R.string.button_copy_text_description))
            }
        }
    }
}

private data class DisplayedImageLayout(
    val scale: Float,
    val horizontalOffset: Float,
    val verticalOffset: Float
) {
    fun map(rect: Rect): androidx.compose.ui.geometry.Rect {
        val left = rect.left * scale + horizontalOffset
        val top = rect.top * scale + verticalOffset
        val right = rect.right * scale + horizontalOffset
        val bottom = rect.bottom * scale + verticalOffset
        return androidx.compose.ui.geometry.Rect(left, top, right, bottom)
    }

    fun map(selectionBox: SelectionBox): androidx.compose.ui.geometry.Rect =
        androidx.compose.ui.geometry.Rect(
            selectionBox.left * scale + horizontalOffset,
            selectionBox.top * scale + verticalOffset,
            selectionBox.right * scale + horizontalOffset,
            selectionBox.bottom * scale + verticalOffset
        )

    fun contains(rect: Rect, tapOffset: Offset): Boolean {
        val mappedRect = map(rect)
        return tapOffset.x in mappedRect.left..mappedRect.right &&
            tapOffset.y in mappedRect.top..mappedRect.bottom
    }

    fun toImageOffset(displayOffset: Offset): Offset = Offset(
        x = ((displayOffset.x - horizontalOffset) / scale).coerceAtLeast(0f),
        y = ((displayOffset.y - verticalOffset) / scale).coerceAtLeast(0f)
    )

    fun updateSelectionBox(
        selectionBox: SelectionBox,
        corner: SelectionCorner,
        displayPosition: Offset,
        imageWidth: Int,
        imageHeight: Int
    ): SelectionBox {
        val imageOffset = toImageOffset(displayPosition)
        return selectionBox
            .withCorner(corner, imageOffset)
            .clamp(imageWidth.toFloat(), imageHeight.toFloat())
    }
}

private fun calculateDisplayedImageLayout(
    containerSize: IntSize,
    imageWidth: Int,
    imageHeight: Int
): DisplayedImageLayout? {
    if (containerSize.width == 0 || containerSize.height == 0 || imageWidth == 0 || imageHeight == 0) {
        return null
    }

    val scale = min(
        containerSize.width.toFloat() / imageWidth.toFloat(),
        containerSize.height.toFloat() / imageHeight.toFloat()
    )
    val displayedWidth = imageWidth * scale
    val displayedHeight = imageHeight * scale

    return DisplayedImageLayout(
        scale = scale,
        horizontalOffset = (containerSize.width - displayedWidth) / 2f,
        verticalOffset = (containerSize.height - displayedHeight) / 2f
    )
}

private enum class SelectionCorner {
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight
}

private data class SelectionBox(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    fun intersects(rect: Rect): Boolean =
        left <= rect.right &&
            right >= rect.left &&
            top <= rect.bottom &&
            bottom >= rect.top

    fun withCorner(corner: SelectionCorner, offset: Offset): SelectionBox =
        when (corner) {
            SelectionCorner.TopLeft -> SelectionBox(offset.x, offset.y, right, bottom)
            SelectionCorner.TopRight -> SelectionBox(left, offset.y, offset.x, bottom)
            SelectionCorner.BottomLeft -> SelectionBox(offset.x, top, right, offset.y)
            SelectionCorner.BottomRight -> SelectionBox(left, top, offset.x, offset.y)
        }.normalized()

    fun clamp(maxWidth: Float, maxHeight: Float): SelectionBox =
        SelectionBox(
            left = left.coerceIn(0f, maxWidth),
            top = top.coerceIn(0f, maxHeight),
            right = right.coerceIn(0f, maxWidth),
            bottom = bottom.coerceIn(0f, maxHeight)
        ).normalized()

    fun normalized(): SelectionBox = SelectionBox(
        left = minOf(left, right),
        top = minOf(top, bottom),
        right = maxOf(left, right),
        bottom = maxOf(top, bottom)
    )

    companion object {
        fun fromRect(rect: Rect): SelectionBox = SelectionBox(
            left = rect.left.toFloat(),
            top = rect.top.toFloat(),
            right = rect.right.toFloat(),
            bottom = rect.bottom.toFloat()
        )
    }
}

@Composable
private fun SelectionHandle(
    corner: SelectionCorner,
    position: Offset,
    onDrag: (Offset) -> Unit
) {
    val handleTouchTarget = 28.dp
    val handleVisualSize = 20.dp
    val handleColor = MaterialTheme.colorScheme.primary
    var currentPosition by remember(position) { mutableStateOf(position) }

    Box(
        modifier = Modifier
            .offset {
                val touchTargetPx = handleTouchTarget.roundToPx()
                when (corner) {
                    SelectionCorner.TopLeft -> IntOffset(
                        x = currentPosition.x.roundToInt(),
                        y = currentPosition.y.roundToInt()
                    )
                    SelectionCorner.TopRight -> IntOffset(
                        x = (currentPosition.x - touchTargetPx).roundToInt(),
                        y = currentPosition.y.roundToInt()
                    )
                    SelectionCorner.BottomLeft -> IntOffset(
                        x = currentPosition.x.roundToInt(),
                        y = (currentPosition.y - touchTargetPx).roundToInt()
                    )
                    SelectionCorner.BottomRight -> IntOffset(
                        x = (currentPosition.x - touchTargetPx).roundToInt(),
                        y = (currentPosition.y - touchTargetPx).roundToInt()
                    )
                }
            }
            .size(handleTouchTarget)
            .pointerInput(corner) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    currentPosition += dragAmount
                    onDrag(currentPosition)
                }
            }
    ) {
        Canvas(
            modifier = Modifier
                .align(
                    when (corner) {
                        SelectionCorner.TopLeft -> androidx.compose.ui.Alignment.TopStart
                        SelectionCorner.TopRight -> androidx.compose.ui.Alignment.TopEnd
                        SelectionCorner.BottomLeft -> androidx.compose.ui.Alignment.BottomStart
                        SelectionCorner.BottomRight -> androidx.compose.ui.Alignment.BottomEnd
                    }
                )
                .size(handleVisualSize)
        ) {
            val strokeWidth = 3.dp.toPx()
            val inset = strokeWidth / 2f
            val cornerRadius = 6.dp.toPx()
            val maxX = size.width - inset
            val maxY = size.height - inset

            when (corner) {
                SelectionCorner.TopLeft -> {
                    drawLine(
                        color = handleColor,
                        start = Offset(cornerRadius + inset, inset),
                        end = Offset(maxX, inset),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = handleColor,
                        start = Offset(inset, cornerRadius + inset),
                        end = Offset(inset, maxY),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                    drawArc(
                        color = handleColor,
                        startAngle = 180f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = androidx.compose.ui.geometry.Size(
                            width = cornerRadius * 2,
                            height = cornerRadius * 2
                        ),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                SelectionCorner.TopRight -> {
                    drawLine(
                        color = handleColor,
                        start = Offset(inset, inset),
                        end = Offset(maxX - cornerRadius, inset),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = handleColor,
                        start = Offset(maxX, cornerRadius + inset),
                        end = Offset(maxX, maxY),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                    drawArc(
                        color = handleColor,
                        startAngle = 270f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(maxX - cornerRadius * 2, inset),
                        size = androidx.compose.ui.geometry.Size(
                            width = cornerRadius * 2,
                            height = cornerRadius * 2
                        ),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                SelectionCorner.BottomLeft -> {
                    drawLine(
                        color = handleColor,
                        start = Offset(cornerRadius + inset, maxY),
                        end = Offset(maxX, maxY),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = handleColor,
                        start = Offset(inset, inset),
                        end = Offset(inset, maxY - cornerRadius),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                    drawArc(
                        color = handleColor,
                        startAngle = 90f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(inset, maxY - cornerRadius * 2),
                        size = androidx.compose.ui.geometry.Size(
                            width = cornerRadius * 2,
                            height = cornerRadius * 2
                        ),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                SelectionCorner.BottomRight -> {
                    drawLine(
                        color = handleColor,
                        start = Offset(inset, maxY),
                        end = Offset(maxX - cornerRadius, maxY),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = handleColor,
                        start = Offset(maxX, inset),
                        end = Offset(maxX, maxY - cornerRadius),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round
                    )
                    drawArc(
                        color = handleColor,
                        startAngle = 0f,
                        sweepAngle = 90f,
                        useCenter = false,
                        topLeft = Offset(maxX - cornerRadius * 2, maxY - cornerRadius * 2),
                        size = androidx.compose.ui.geometry.Size(
                            width = cornerRadius * 2,
                            height = cornerRadius * 2
                        ),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
        }
    }
}

private fun buildSelectedText(words: List<ImageTextUiState.Word>): String =
    words
        .groupBy { it.lineIndex }
        .toSortedMap()
        .values
        .joinToString(separator = "\n") { wordsInLine ->
            wordsInLine
                .sortedBy { it.wordIndexInLine }
                .joinToString(separator = " ") { it.text }
        }
