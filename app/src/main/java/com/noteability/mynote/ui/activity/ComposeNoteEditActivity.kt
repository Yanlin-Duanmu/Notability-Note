package com.noteability.mynote.ui.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.noteability.mynote.ui.screen.NoteEditScreen
import com.noteability.mynote.ui.theme.MyNoteTheme

class ComposeNoteEditActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge() // Draw behind system bars for immersive UI
        
        setContent {
            MyNoteTheme {
                NoteEditScreen(
                    onBackClick = { finish() },
                    onMoreClick = { /* TODO: Implement more menu */ }
                )
            }
        }
    }
}