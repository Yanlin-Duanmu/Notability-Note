package com.noteability.mynote.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.noteability.mynote.data.dao.NoteDao
import com.noteability.mynote.data.entity.Note
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class NoteListViewModel(private val noteDao: NoteDao, private val userId: Long) : ViewModel() {

    private val _notes = MutableStateFlow<List<Note>>(emptyList())
    val notes: StateFlow<List<Note>> get() = _notes

    fun loadNotes() {
        viewModelScope.launch {
            _notes.value = noteDao.getNotesByUserId(userId)
        }
    }

    fun addNote(title: String, content: String) {
        viewModelScope.launch {
            val note = Note(userId = userId, title = title, content = content)
            noteDao.insertNote(note)
            loadNotes()
        }
    }

    fun updateNote(note: Note) {
        viewModelScope.launch {
            noteDao.updateNote(note)
            loadNotes()
        }
    }

    fun deleteNote(note: Note) {
        viewModelScope.launch {
            noteDao.deleteNote(note)
            loadNotes()
        }
    }

    fun searchNotes(query: String) {
        viewModelScope.launch {
            _notes.value = noteDao.searchNotes(userId, query)
        }
    }
}
