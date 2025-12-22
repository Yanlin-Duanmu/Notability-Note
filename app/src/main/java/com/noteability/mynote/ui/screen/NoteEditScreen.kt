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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.outlined.FormatBold
import androidx.compose.material.icons.outlined.FormatItalic
import androidx.compose.material.icons.outlined.FormatListBulleted
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.material.icons.outlined.MoreVert
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NoteEditScreen(
    initialTitle: String = "UI/UX Design Principles",
    initialContent: String = "In user interface (UI) and user experience (UX) design, several core principles guide the creation of effective and enjoyable digital products. Understanding these is key to building intuitive applications.\n\n" +
            "1. Clarity and Simplicity\n" +
            "The design should be clear and easy to understand. Avoid unnecessary elements that could confuse the user. Every element should have a purpose.\n\n" +
            "2. Consistency\n" +
            "Maintain consistency in design elements and terminology throughout the app. This helps users learn the interface faster and reduces confusion. For example, a button for saving should always look and behave the same way.\n\n" +
            "3. Feedback\n" +
            "Provide immediate and clear feedback for every user action. This can be a visual cue, a sound, or a vibration. This reassures the user that the system has received their input.",
    onBackClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var content by remember { mutableStateOf(initialContent) }

    Scaffold(
        topBar = {
            TopBar(
                title = title,
                onTitleChange = { title = it },
                onBackClick = onBackClick,
                onMoreClick = onMoreClick
            )
        },
        bottomBar = {
            // Handle IME and navigation bar insets
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp)
            ) {
                // Placeholder for rich text editing (e.g. Markwon)
                BasicTextField(
                    value = content,
                    onValueChange = { content = it },
                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 24.sp
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary)
                )
            }
        }
    }
}

@Composable
fun TopBar(
    title: String,
    onTitleChange: (String) -> Unit,
    onBackClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Column(modifier = Modifier.statusBarsPadding()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            BasicTextField(
                value = title,
                onValueChange = onTitleChange,
                textStyle = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    if (title.isEmpty()) {
                        Text(
                            text = "Untitled Note",
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

            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Outlined.MoreVert,
                    contentDescription = "More",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
            horizontalArrangement = Arrangement.spacedBy(4.dp),
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
        modifier = Modifier.size(32.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .clickable(onClick = onClick)
            .padding(8.dp)
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
        NoteEditScreen(onBackClick = {}, onMoreClick = {})
    }
}