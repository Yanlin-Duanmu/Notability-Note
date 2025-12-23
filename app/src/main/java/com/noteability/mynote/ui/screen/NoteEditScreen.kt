package com.noteability.mynote.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddLink
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.ExperimentalMaterial3Api
import com.noteability.mynote.data.entity.Tag
import com.noteability.mynote.ui.component.AiSummaryPanel
import com.noteability.mynote.ui.component.VditorController
import com.noteability.mynote.ui.component.VditorWebView
import com.noteability.mynote.ui.viewmodel.NoteEditUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    uiState: NoteEditUiState,
    onTitleChange: (String) -> Unit,
    onContentChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onTagClick: () -> Unit,
    onTagSelected: (Tag) -> Unit,
    onAiSummaryClick: () -> Unit = {},
    onAiSummaryClose: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showImageDialog by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var vditorController by remember { mutableStateOf<VditorController?>(null) }

    // Image insert dialog
    if (showImageDialog) {
        InsertImageDialog(
            onDismiss = { showImageDialog = false },
            onConfirm = { url, desc ->
                vditorController?.insertImage(url, desc)
                showImageDialog = false
            }
        )
    }

    // Link insert dialog
    if (showLinkDialog) {
        InsertLinkDialog(
            onDismiss = { showLinkDialog = false },
            onConfirm = { url, text ->
                vditorController?.insertLink(url, text)
                showLinkDialog = false
            }
        )
    }

    if (showTagDialog) {
        TagSelectionDialog(
            tags = uiState.allTags,
            onDismissRequest = { showTagDialog = false },
            onTagSelected = {
                onTagSelected(it)
                showTagDialog = false
            }
        )
    }

    Scaffold(
        topBar = {
            TopBar(
                title = uiState.title,
                tagName = uiState.currentTag?.name,
                onTitleChange = onTitleChange,
                onBackClick = onBackClick,
                onMoreClick = { showMenu = true },
                onTagClick = { showTagDialog = true },
                menu = {
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("保存") },
                            onClick = {
                                showMenu = false
                                onSaveClick()
                            },
                            leadingIcon = { Icon(Icons.Outlined.Save, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("删除") },
                            onClick = {
                                showMenu = false
                                onDeleteClick()
                            },
                            leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null) }
                        )
                    }
                }
            )
        },
        bottomBar = {
            BottomFormattingBar(
                onBoldClick = { vditorController?.formatBold() },
                onItalicClick = { vditorController?.formatItalic() },
                onListClick = { vditorController?.formatList() },
                onQuoteClick = { vditorController?.formatQuote() },
                onCodeClick = { vditorController?.formatCode() },
                onImageClick = { showImageDialog = true },
                onLinkClick = { showLinkDialog = true },
                onAiSummaryClick = onAiSummaryClick,
                onAiStyleClick = { /* TODO: AI Style */ },
                modifier = Modifier
                    .windowInsetsPadding(
                        WindowInsets.ime.union(WindowInsets.navigationBars)
                            .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                    )
                    .padding(bottom = 8.dp, start = 8.dp, end = 8.dp)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            VditorWebView(
                content = uiState.content,
                onContentChange = onContentChange,
                onControllerReady = { vditorController = it },
                modifier = Modifier.fillMaxSize()
            )

            // AI Summary panel overlay
            AiSummaryPanel(
                isVisible = uiState.isAiSummaryVisible,
                content = uiState.aiSummaryContent,
                isGenerating = uiState.isAiGenerating,
                onClose = onAiSummaryClose,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            if (uiState.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background.copy(alpha = 0.5f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun TagSelectionDialog(
    tags: List<Tag>,
    onDismissRequest: () -> Unit,
    onTagSelected: (Tag) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("选择标签") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                tags.forEach { tag ->
                    TextButton(
                        onClick = { onTagSelected(tag) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = tag.name,
                            modifier = Modifier.fillMaxWidth(),
                            style = TextStyle(fontSize = 16.sp)
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("取消")
            }
        }
    )
}

@Composable
fun TopBar(
    title: String,
    tagName: String?,
    onTitleChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onMoreClick: () -> Unit,
    onTagClick: () -> Unit,
    menu: @Composable () -> Unit = {}
) {
    Column(modifier = Modifier.statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                BasicTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    textStyle = TextStyle(
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        if (title.isEmpty()) {
                            Text(
                                text = "无标题",
                                style = TextStyle(
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                        innerTextField()
                    }
                )

                Spacer(modifier = Modifier.height(4.dp))
                TagChip(tagName = tagName ?: "未分类", onClick = onTagClick)
            }

            Box {
                IconButton(onClick = onMoreClick) {
                    Icon(
                        imageVector = Icons.Outlined.MoreVert,
                        contentDescription = "More",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                menu()
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
}

@Composable
fun InsertImageDialog(
    onDismiss: () -> Unit,
    onConfirm: (url: String, description: String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("插入图片") },
        text = {
            Column {
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("图片 URL") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("图片描述（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(url, description) },
                enabled = url.isNotBlank()
            ) {
                Text("插入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun InsertLinkDialog(
    onDismiss: () -> Unit,
    onConfirm: (url: String, text: String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var text by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("插入链接") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("显示文本") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("链接 URL") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(url, text) },
                enabled = url.isNotBlank()
            ) {
                Text("插入")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}

@Composable
fun BottomFormattingBar(
    onBoldClick: () -> Unit,
    onItalicClick: () -> Unit,
    onListClick: () -> Unit,
    onQuoteClick: () -> Unit,
    onCodeClick: () -> Unit,
    onImageClick: () -> Unit,
    onLinkClick: () -> Unit,
    onAiSummaryClick: () -> Unit,
    onAiStyleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.8f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(8.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Standard Formatting Icons
            FormattingIconButton(
                icon = Icons.Outlined.FormatBold,
                contentDescription = "Bold",
                onClick = onBoldClick
            )
            FormattingIconButton(
                icon = Icons.Outlined.FormatItalic,
                contentDescription = "Italic",
                onClick = onItalicClick
            )
            FormattingIconButton(
                icon = Icons.Outlined.FormatListBulleted,
                contentDescription = "List",
                onClick = onListClick
            )
            FormattingIconButton(
                icon = Icons.Outlined.FormatQuote,
                contentDescription = "Quote",
                onClick = onQuoteClick
            )
            FormattingIconButton(
                icon = Icons.Outlined.DataObject,
                contentDescription = "Code",
                onClick = onCodeClick
            )

            VerticalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Insertion Icons
            FormattingIconButton(
                icon = Icons.Outlined.AddPhotoAlternate,
                contentDescription = "Image",
                onClick = onImageClick
            )
            FormattingIconButton(
                icon = Icons.Outlined.AddLink,
                contentDescription = "Link",
                onClick = onLinkClick
            )

            VerticalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // AI Feature Buttons
            AiFeatureButton(
                icon = Icons.Outlined.AutoAwesome,
                contentDescription = "AI Summary",
                onClick = onAiSummaryClick
            )
            AiFeatureButton(
                icon = Icons.Outlined.Brush,
                contentDescription = "AI Style",
                onClick = onAiStyleClick
            )
        }
    }
}

@Composable
fun FormattingIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit = {}
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(40.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun TagChip(
    tagName: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = tagName,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun AiFeatureButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit = {}
) {
    Box(
        modifier = Modifier
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun NoteEditScreenPreview() {
    MaterialTheme {
        NoteEditScreen(
            uiState = NoteEditUiState(
                title = "Preview Title",
                content = "Preview Content",
                currentTag = Tag(1, 1, "Design System", 0)
            ),
            onTitleChange = {},
            onContentChange = {},
            onBackClick = {},
            onSaveClick = {},
            onDeleteClick = {},
            onTagClick = {},
            onTagSelected = {}
        )
    }
}