package com.noteability.mynote.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.noteability.mynote.ui.theme.MyNoteTheme

// Dropdown with save/delete actions
@Composable
fun StyledMoreMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onSaveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = DpOffset(x = 0.dp, y = 4.dp),
        shape = RoundedCornerShape(16.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        shadowElevation = 12.dp,
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .width(180.dp)
                .padding(vertical = 4.dp)
        ) {
            StyledMenuItem(
                icon = Icons.Outlined.Save,
                text = "保存",
                onClick = {
                    onDismissRequest()
                    onSaveClick()
                }
            )

            StyledMenuItem(
                icon = Icons.Outlined.Delete,
                text = "删除",
                onClick = {
                    onDismissRequest()
                    onDeleteClick()
                },
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

// Menu item row with icon
@Composable
fun StyledMenuItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge.copy(
                color = tint,
                fontWeight = FontWeight.Medium
            )
        )
    }
}

// region Previews

@Preview(showBackground = true, name = "More Menu")
@Composable
private fun MoreMenuPreview() {
    MyNoteTheme {
        Surface(
            modifier = Modifier.padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 12.dp,
            border = BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        ) {
            Column(
                modifier = Modifier
                    .width(180.dp)
                    .padding(vertical = 4.dp)
            ) {
                StyledMenuItem(
                    icon = Icons.Outlined.Save,
                    text = "保存",
                    onClick = {}
                )
                StyledMenuItem(
                    icon = Icons.Outlined.Delete,
                    text = "删除",
                    onClick = {},
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// endregion