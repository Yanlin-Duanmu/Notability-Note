package com.noteability.mynote.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddLink
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.noteability.mynote.ui.theme.MyNoteTheme

// Image insertion with URL input and local picker
@Composable
fun StyledInsertImageDialog(
    onDismiss: () -> Unit,
    onConfirm: (url: String, description: String) -> Unit,
    onPickLocalImage: (() -> Unit)? = null
) {
    var url by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    StyledDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp)) {
            DialogHeader(
                icon = Icons.Outlined.AddPhotoAlternate,
                title = "插入图片"
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Local picker option
            if (onPickLocalImage != null) {
                LocalPickerButton(onClick = onPickLocalImage)
                Spacer(modifier = Modifier.height(16.dp))
                OrDivider()
                Spacer(modifier = Modifier.height(16.dp))
            }

            StyledInputField(
                value = url,
                onValueChange = { url = it },
                label = "图片链接",
                placeholder = "https://example.com/image.png"
            )

            Spacer(modifier = Modifier.height(16.dp))

            StyledInputField(
                value = description,
                onValueChange = { description = it },
                label = "图片描述（可选）",
                placeholder = "描述图片内容"
            )

            Spacer(modifier = Modifier.height(28.dp))

            DialogButtonRow(
                onCancel = onDismiss,
                onConfirm = { onConfirm(url, description) },
                confirmText = "插入",
                confirmEnabled = url.isNotBlank()
            )
        }
    }
}

// Hyperlink insertion with URL and display text
@Composable
fun StyledInsertLinkDialog(
    onDismiss: () -> Unit,
    onConfirm: (url: String, text: String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }

    StyledDialog(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(24.dp)) {
            DialogHeader(
                icon = Icons.Outlined.AddLink,
                title = "插入链接"
            )

            Spacer(modifier = Modifier.height(24.dp))

            StyledInputField(
                value = text,
                onValueChange = { text = it },
                label = "显示文本",
                placeholder = "链接的显示文字"
            )

            Spacer(modifier = Modifier.height(16.dp))

            StyledInputField(
                value = url,
                onValueChange = { url = it },
                label = "链接地址",
                placeholder = "https://example.com"
            )

            Spacer(modifier = Modifier.height(28.dp))

            DialogButtonRow(
                onCancel = onDismiss,
                onConfirm = { onConfirm(url, text) },
                confirmText = "插入",
                confirmEnabled = url.isNotBlank()
            )
        }
    }
}

// Header with icon and title
@Composable
private fun DialogHeader(
    icon: ImageVector,
    title: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

// Local image picker button
@Composable
private fun LocalPickerButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Outlined.Folder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "从相册选择",
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

// Divider with "or" text
@Composable
private fun OrDivider() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Text(
            text = "或",
            style = MaterialTheme.typography.bodySmall.copy(
                color = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

// Cancel/Confirm button row
@Composable
private fun DialogButtonRow(
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
    confirmText: String,
    confirmEnabled: Boolean = true
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StyledButton(
            text = "取消",
            onClick = onCancel,
            isPrimary = false,
            modifier = Modifier.weight(1f)
        )
        StyledButton(
            text = confirmText,
            onClick = onConfirm,
            enabled = confirmEnabled,
            modifier = Modifier.weight(1f)
        )
    }
}

// region Previews

@Composable
private fun DialogPreviewWrapper(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 16.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        content()
    }
}

@Preview(showBackground = true, name = "Insert Image Dialog")
@Composable
private fun InsertImageDialogPreview() {
    MyNoteTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            DialogPreviewWrapper {
                InsertImageDialogContent()
            }
        }
    }
}

@Composable
private fun InsertImageDialogContent() {
    Column(modifier = Modifier.padding(24.dp)) {
        DialogHeader(
            icon = Icons.Outlined.AddPhotoAlternate,
            title = "插入图片"
        )
        Spacer(modifier = Modifier.height(24.dp))
        StyledInputField(
            value = "",
            onValueChange = {},
            label = "图片链接",
            placeholder = "https://example.com/image.png"
        )
        Spacer(modifier = Modifier.height(16.dp))
        StyledInputField(
            value = "",
            onValueChange = {},
            label = "图片描述（可选）",
            placeholder = "描述图片内容"
        )
        Spacer(modifier = Modifier.height(28.dp))
        DialogButtonRow(
            onCancel = {},
            onConfirm = {},
            confirmText = "插入",
            confirmEnabled = false
        )
    }
}

@Preview(showBackground = true, name = "Insert Link Dialog")
@Composable
private fun InsertLinkDialogPreview() {
    MyNoteTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            DialogPreviewWrapper {
                InsertLinkDialogContent()
            }
        }
    }
}

@Composable
private fun InsertLinkDialogContent() {
    Column(modifier = Modifier.padding(24.dp)) {
        DialogHeader(
            icon = Icons.Outlined.AddLink,
            title = "插入链接"
        )
        Spacer(modifier = Modifier.height(24.dp))
        StyledInputField(
            value = "Kotlin 官方文档",
            onValueChange = {},
            label = "显示文本",
            placeholder = "链接的显示文字"
        )
        Spacer(modifier = Modifier.height(16.dp))
        StyledInputField(
            value = "https://kotlinlang.org",
            onValueChange = {},
            label = "链接地址",
            placeholder = "https://example.com"
        )
        Spacer(modifier = Modifier.height(28.dp))
        DialogButtonRow(
            onCancel = {},
            onConfirm = {},
            confirmText = "插入",
            confirmEnabled = true
        )
    }
}

// endregion