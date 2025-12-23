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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddLink
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Brush
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.noteability.mynote.data.entity.Tag
import com.noteability.mynote.ui.component.AiSummaryPanel
import com.noteability.mynote.ui.component.DeleteConfirmationDialog
import com.noteability.mynote.ui.component.SaveConfirmationDialog
import com.noteability.mynote.ui.component.StyledAiTagSelectionDialog
import com.noteability.mynote.ui.component.StyledInsertImageDialog
import com.noteability.mynote.ui.component.StyledInsertLinkDialog
import com.noteability.mynote.ui.component.StyledMoreMenu
import com.noteability.mynote.ui.component.StyledTagSelectionDialog
import com.noteability.mynote.ui.component.VditorController
import com.noteability.mynote.ui.component.VditorWebView
import com.noteability.mynote.ui.viewmodel.NoteEditUiState

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
    onAiSummaryClose: () -> Unit = {},
    onAiTaggingClick: () -> Unit = {},
    onAiTagSelected: (String) -> Unit = {},
    onAiTagsDialogClose: () -> Unit = {},
    showSaveDialog: Boolean = false,
    onSaveDialogDismiss: () -> Unit = {},
    onSaveDialogSave: () -> Unit = {},
    onSaveDialogDiscard: () -> Unit = {},
    showDeleteDialog: Boolean = false,
    onDeleteDialogDismiss: () -> Unit = {},
    onDeleteDialogConfirm: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var showImageDialog by remember { mutableStateOf(false) }
    var showLinkDialog by remember { mutableStateOf(false) }
    var vditorController by remember { mutableStateOf<VditorController?>(null) }

    // Save confirmation dialog
    if (showSaveDialog) {
        SaveConfirmationDialog(
            onDismiss = onSaveDialogDismiss,
            onSave = onSaveDialogSave,
            onDiscard = onSaveDialogDiscard
        )
    }

    // Delete confirmation dialog
    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            onDismiss = onDeleteDialogDismiss,
            onConfirm = onDeleteDialogConfirm
        )
    }

    // Image insert dialog
    if (showImageDialog) {
        StyledInsertImageDialog(
            onDismiss = { showImageDialog = false },
            onConfirm = { url, desc ->
                vditorController?.insertImage(url, desc)
                showImageDialog = false
            }
        )
    }

    // Link insert dialog
    if (showLinkDialog) {
        StyledInsertLinkDialog(
            onDismiss = { showLinkDialog = false },
            onConfirm = { url, text ->
                vditorController?.insertLink(url, text)
                showLinkDialog = false
            }
        )
    }

    // Tag selection dialog
    if (showTagDialog) {
        StyledTagSelectionDialog(
            title = "选择标签",
            tags = uiState.allTags,
            onDismissRequest = { showTagDialog = false },
            onTagSelected = {
                onTagSelected(it)
                showTagDialog = false
            }
        )
    }

    // AI tag selection dialog
    if (uiState.showAiTagsDialog) {
        StyledAiTagSelectionDialog(
            tags = uiState.suggestedTags,
            onDismissRequest = onAiTagsDialogClose,
            onTagSelected = onAiTagSelected
        )
    }

    Scaffold(
        topBar = {
            TopBar(
                title = uiState.title,
                tagName = uiState.currentTag?.name,
                onTitleChange = onTitleChange,
                onBackClick = onBackClick,
                onMoreClick = { showMenu = !showMenu },
                onTagClick = { showTagDialog = true },
                showMenu = showMenu,
                onMenuDismiss = { showMenu = false },
                onSaveClick = onSaveClick,
                onDeleteClick = onDeleteClick
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
                onAiStyleClick = onAiTaggingClick,
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
private fun TopBar(
    title: String,
    tagName: String?,
    onTitleChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onMoreClick: () -> Unit,
    onTagClick: () -> Unit,
    showMenu: Boolean,
    onMenuDismiss: () -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit
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
                
                // Styled dropdown menu
                StyledMoreMenu(
                    expanded = showMenu,
                    onDismissRequest = onMenuDismiss,
                    onSaveClick = onSaveClick,
                    onDeleteClick = onDeleteClick
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    }
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
                contentDescription = "AI 摘要",
                onClick = onAiSummaryClick
            )
            AiFeatureButton(
                icon = Icons.Outlined.Brush,
                contentDescription = "AI 打标",
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