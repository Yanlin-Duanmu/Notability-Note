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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditor
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
import com.mohamedrejeb.richeditor.ui.material3.RichTextEditorDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import com.noteability.mynote.data.entity.Tag
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
    onTagSelected: (Tag) -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    
    // Initialize rich text state
    val richTextState = rememberRichTextState()

    // Load initial markdown content when data loading is finished
    var isInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isLoading) {
        if (!uiState.isLoading && !isInitialized) {
            richTextState.setMarkdown(uiState.content)
            isInitialized = true
        }
    }

    // Sync rich text changes back to ViewModel as markdown
    LaunchedEffect(richTextState.annotatedString) {
        if (isInitialized) {
            onContentChange(richTextState.toMarkdown())
        }
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
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                RichTextEditor(
                    state = richTextState,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 24.sp
                    ),
                    colors = RichTextEditorDefaults.richTextEditorColors(
                        cursorColor = MaterialTheme.colorScheme.primary,
                        focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                        containerColor = androidx.compose.ui.graphics.Color.Transparent
                    )
                )
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
fun BottomFormattingBar(modifier: Modifier = Modifier) {
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
            FormattingIconButton(icon = Icons.Outlined.FormatBold, contentDescription = "Bold")
            FormattingIconButton(icon = Icons.Outlined.FormatItalic, contentDescription = "Italic")
            FormattingIconButton(icon = Icons.Outlined.FormatListBulleted, contentDescription = "List")
            FormattingIconButton(icon = Icons.Outlined.FormatQuote, contentDescription = "Quote")
            FormattingIconButton(icon = Icons.Outlined.DataObject, contentDescription = "Code")

            VerticalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // Insertion Icons
            FormattingIconButton(icon = Icons.Outlined.AddPhotoAlternate, contentDescription = "Image")
            FormattingIconButton(icon = Icons.Outlined.AddLink, contentDescription = "Link")

            VerticalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .padding(horizontal = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )

            // AI Feature Buttons
            AiFeatureButton(icon = Icons.Outlined.AutoAwesome, contentDescription = "AI Summary")
            AiFeatureButton(icon = Icons.Outlined.Brush, contentDescription = "AI Style")
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