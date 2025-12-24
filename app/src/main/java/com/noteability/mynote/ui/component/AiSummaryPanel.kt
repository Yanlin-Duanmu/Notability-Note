package com.noteability.mynote.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val SWIPE_THRESHOLD = -60f

// Slide-in panel for AI-generated summary with swipe-to-dismiss
@Composable
fun AiSummaryPanel(
    isVisible: Boolean,
    content: String,
    isGenerating: Boolean,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = expandVertically(
            expandFrom = Alignment.Top,
            animationSpec = tween(durationMillis = 350)
        ),
        exit = shrinkVertically(
            shrinkTowards = Alignment.Top,
            animationSpec = tween(durationMillis = 250)
        ),
        modifier = modifier
    ) {
        AiSummaryContent(
            content = content,
            isGenerating = isGenerating,
            onClose = onClose
        )
    }
}

@Composable
private fun AiSummaryContent(
    content: String,
    isGenerating: Boolean,
    onClose: () -> Unit
) {
    val dragOffset = remember { mutableFloatStateOf(0f) }
    val scrollState = rememberScrollState()
    val maxHeight = (LocalConfiguration.current.screenHeightDp * 0.2f).dp

    // Auto-scroll to bottom when streaming content
    LaunchedEffect(content) {
        if (isGenerating) scrollState.animateScrollTo(scrollState.maxValue)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        shadowElevation = 8.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .swipeToDismiss(dragOffset, onClose)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp, max = maxHeight)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 20.dp)
            ) {
                if (content.isEmpty() && isGenerating) {
                    LoadingIndicator()
                } else {
                    SummaryText(content)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            DragHandle()
        }
    }
}

// Swipe-up gesture to dismiss panel
private fun Modifier.swipeToDismiss(
    dragOffset: MutableFloatState,
    onClose: () -> Unit
): Modifier = pointerInput(Unit) {
    detectVerticalDragGestures(
        onDragEnd = {
            if (dragOffset.floatValue < SWIPE_THRESHOLD) onClose()
            dragOffset.floatValue = 0f
        },
        onDragCancel = { dragOffset.floatValue = 0f },
        onVerticalDrag = { _, dragAmount -> dragOffset.floatValue += dragAmount }
    )
}

@Composable
private fun LoadingIndicator() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CircularProgressIndicator(
            modifier = Modifier.size(14.dp),
            strokeWidth = 2.dp,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "AI 正在思考...",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
    }
}

@Composable
private fun SummaryText(content: String) {
    Text(
        text = content,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontSize = 15.sp,
            lineHeight = 22.sp,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
        )
    )
}

@Composable
private fun DragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(32.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(1.5.dp))
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}
