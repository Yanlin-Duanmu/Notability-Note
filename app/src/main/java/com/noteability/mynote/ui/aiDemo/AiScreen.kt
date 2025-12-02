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

            // Section 2: Existing tags input
            Text("现有标签库", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = uiState.existingTags,
                onValueChange = viewModel::onTagsInputChanged,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("科技, Kotlin, 旅游") },
                singleLine = true
            )

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { viewModel.fetchSummary() },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading
                ) {
                    Text("生成摘要")
                }

                Button(
                    onClick = { viewModel.fetchTags() },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isLoading
                ) {
                    Text("智能打标")
                }
            }

            // Loading indicator and error message
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
            }

            uiState.error?.let { err ->
                Text(text = err, color = MaterialTheme.colorScheme.error)
            }

            // Section 3: Summary result display
            if (uiState.summaryResult.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("摘要结果：", style = MaterialTheme.typography.labelLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(uiState.summaryResult)
                    }
                }
            }

            // Section 4: Tag display
            if (uiState.tagResult.isNotEmpty()) {
                Text("匹配标签：", style = MaterialTheme.typography.titleMedium)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    uiState.tagResult.forEach { tag ->
                        SuggestionChip(
                            onClick = { /* Click event */ },
                            label = { Text(tag) }
                        )
                    }
                }
            }
        }
    }
}
