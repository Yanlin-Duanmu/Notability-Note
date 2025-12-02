package com.noteability.mynote.ui.aiDemo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.noteability.mynote.viewmodel.AiDemoViewModel

@OptIn(ExperimentalLayoutApi::class) // FlowRow requires opt-in
@Composable
fun AiDemoScreen(
    viewModel: AiDemoViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState() // Observe ui state as Compose State
    val scrollState = rememberScrollState() // Scroll state

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("AI Demo") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState), // Enable vertical scrolling
            verticalArrangement = Arrangement.spacedBy(16.dp) // Spacing between children
        ) {
            // Section 1: Source text input
            Text("文本内容", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = uiState.sourceText,
                onValueChange = viewModel::onSourceTextChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp), // Fixed height
                placeholder = { Text("需要分析的文字...") },
                supportingText = { Text("字数: ${uiState.sourceText.length}") }
            )


        }
    }
}
