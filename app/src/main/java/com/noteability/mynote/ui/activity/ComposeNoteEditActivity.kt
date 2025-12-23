package com.noteability.mynote.ui.activity

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.noteability.mynote.data.repository.impl.NoteRepositoryImpl
import com.noteability.mynote.data.repository.impl.TagRepositoryImpl
import com.noteability.mynote.ui.screen.NoteEditScreen
import com.noteability.mynote.ui.theme.MyNoteTheme
import com.noteability.mynote.ui.viewmodel.ComposeNoteEditViewModel
import kotlinx.coroutines.launch

class ComposeNoteEditActivity : ComponentActivity() {

    private class ViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ComposeNoteEditViewModel::class.java)) {
                return ComposeNoteEditViewModel(
                    NoteRepositoryImpl(context),
                    TagRepositoryImpl(context)
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    private val viewModel: ComposeNoteEditViewModel by viewModels { ViewModelFactory(this) }

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
                    onBackClick = { handleBackPress(uiState.title, uiState.content) },
                    onSaveClick = viewModel::saveNote,
                    onDeleteClick = { showDeleteDialog() },
                    onTagClick = { /* TODO: tag selection dialog */ },
                    onTagSelected = viewModel::updateTag,
                    onAiSummaryClick = viewModel::triggerAiSummary,
                    onAiSummaryClose = viewModel::closeAiSummary
                )
            }
        }
    }

    private fun handleBackPress(title: String, content: String) {
        if (title.isNotEmpty() || content.isNotEmpty()) {
             AlertDialog.Builder(this)
                .setTitle("保存笔记")
                .setMessage("是否保存当前笔记？")
                .setPositiveButton("保存") { _, _ ->
                    viewModel.saveNote()
                }
                .setNegativeButton("不保存") { _, _ ->
                    finish()
                }
                .setNeutralButton("取消", null)
                .show()
        } else {
            finish()
        }
    }
    
    private fun showDeleteDialog() {
        AlertDialog.Builder(this)
            .setTitle("删除笔记")
            .setMessage("确定要删除这篇笔记吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteNote()
            }
            .setNegativeButton("取消", null)
            .show()
    }
}