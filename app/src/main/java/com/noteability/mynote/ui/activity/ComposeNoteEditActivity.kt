package com.noteability.mynote.ui.activity

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.noteability.mynote.data.repository.impl.NoteRepositoryImpl
import com.noteability.mynote.data.repository.impl.TagRepositoryImpl
import com.noteability.mynote.ui.screen.NoteEditScreen
import com.noteability.mynote.ui.theme.MyNoteTheme
import com.noteability.mynote.ui.viewmodel.ComposeNoteEditViewModel

class ComposeNoteEditActivity : ComponentActivity() {

    private class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ComposeNoteEditViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ComposeNoteEditViewModel(
                    NoteRepositoryImpl(context),
                    TagRepositoryImpl(context)
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    private val viewModel: ComposeNoteEditViewModel by viewModels { ViewModelFactory(this) }

    private val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.processLocalImage(this, it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val loggedInUserId = sharedPreferences.getLong("logged_in_user_id", 1L)
        viewModel.setLoggedInUserId(loggedInUserId)

        val noteId = intent.getLongExtra("noteId", -1L)
        if (savedInstanceState == null) {
            viewModel.loadData(noteId)
        }

        setContent {
            MyNoteTheme {
                val uiState by viewModel.uiState.collectAsState()
                
                // Dialog states managed in Compose
                var showSaveDialog by remember { mutableStateOf(false) }
                var showDeleteDialog by remember { mutableStateOf(false) }

                // Handle one-shot events
                if (uiState.isSaved) {
                    Toast.makeText(this, "笔记已保存", Toast.LENGTH_SHORT).show()
                    viewModel.resetSaveState()
                    finish()
                }
                
                if (uiState.isDeleted) {
                    Toast.makeText(this, "笔记已删除", Toast.LENGTH_SHORT).show()
                    finish()
                }
                
                uiState.error?.let {
                    Toast.makeText(this, it, Toast.LENGTH_SHORT).show()
                    viewModel.clearError()
                }

                NoteEditScreen(
                    uiState = uiState,
                    onTitleChange = viewModel::updateTitle,
                    onContentChange = viewModel::updateContent,
                    onBackClick = {
                        // Show save dialog if content exists
                        if (uiState.title.isNotEmpty() || uiState.content.isNotEmpty()) {
                            showSaveDialog = true
                        } else {
                            finish()
                        }
                    },
                    onSaveClick = viewModel::saveNote,
                    onDeleteClick = { showDeleteDialog = true },
                    onTagClick = { },
                    onTagSelected = viewModel::updateTag,
                    onAiSummaryClick = viewModel::triggerAiSummary,
                    onAiSummaryClose = viewModel::closeAiSummary,
                    onAiTaggingClick = viewModel::triggerAiTagging,
                    onAiTagSelected = viewModel::applyAiTag,
                    onAiTagsDialogClose = viewModel::closeAiTagsDialog,
                    // Compose-managed dialog states
                    showSaveDialog = showSaveDialog,
                    onSaveDialogDismiss = { showSaveDialog = false },
                    onSaveDialogSave = {
                        showSaveDialog = false
                        viewModel.saveNote()
                    },
                    onSaveDialogDiscard = {
                        showSaveDialog = false
                        finish()
                    },
                    showDeleteDialog = showDeleteDialog,
                    onDeleteDialogDismiss = { showDeleteDialog = false },
                    onDeleteDialogConfirm = {
                        showDeleteDialog = false
                        viewModel.deleteNote()
                    },
                    onPickLocalImage = {
                        pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onInsertLocalImage = { _, _ ->
                        viewModel.clearPendingLocalImage()
                    }
                )
            }
        }
    }
}