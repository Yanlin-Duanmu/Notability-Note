package com.noteability.mynote.ui.activity
import android.text.Editable
import android.text.TextWatcher
import androidx.core.view.isVisible
import kotlinx.coroutines.flow.combine
import android.widget.TextView
import android.app.AlertDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.noteability.mynote.R
import com.noteability.mynote.data.entity.Note
import com.noteability.mynote.data.entity.Tag
import com.noteability.mynote.data.repository.impl.NoteRepositoryImpl
import com.noteability.mynote.data.repository.impl.TagRepositoryImpl
import com.noteability.mynote.databinding.ActivityNoteEditBinding
import com.noteability.mynote.di.ServiceLocator
import com.noteability.mynote.ui.aiDemo.AiDemoViewModel
import com.noteability.mynote.ui.viewmodel.NoteDetailViewModel
import com.noteability.mynote.ui.viewmodel.TagsViewModel
import com.noteability.mynote.utils.MarkdownUtils
import io.noties.markwon.Markwon
import kotlinx.coroutines.launch
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter

class NoteEditActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNoteEditBinding
    private val aiViewModel: AiDemoViewModel by viewModels()

    private var lastSummary = ""
    private var lastTags = listOf<String>()

    // 新增：预览相关组件
    private lateinit var markwon: Markwon

    private var noteId: Long? = null
    private var currentTag: Tag? = null
    private var isBold = false
    private var isItalic = false
    private var isUnderline = false
    private var isPreviewMode = false

    // 真实标签数据列表
    private val realTags = mutableListOf<Tag>()

    // TagRepository实例
    private lateinit var tagRepository: TagRepositoryImpl
    private lateinit var contentWatcher: TextWatcher
    // 创建NoteDetailViewModelFactory
    private class NoteDetailViewModelFactory(private val applicationContext: Context) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NoteDetailViewModel::class.java)) {
                val repository = NoteRepositoryImpl(applicationContext)
                val viewModel = NoteDetailViewModel(repository)
                return viewModel as? T
                    ?: throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    // 创建TagsViewModelFactory
    private class TagsViewModelFactory(private val applicationContext: Context) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TagsViewModel::class.java)) {
                val repository = TagRepositoryImpl(applicationContext)
                val viewModel = TagsViewModel(repository)
                return viewModel as? T
                    ?: throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    // 使用viewModels委托和自定义Factory来获取NoteDetailViewModel实例
    private val noteDetailViewModel: NoteDetailViewModel by viewModels {
        NoteDetailViewModelFactory(applicationContext)
    }

    // 使用viewModels委托和自定义Factory来获取TagsViewModel实例
    private val tagsViewModel: TagsViewModel by viewModels {
        TagsViewModelFactory(applicationContext)
    }

    // 当前登录用户ID
    private var loggedInUserId: Long = 1L


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNoteEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 从SharedPreferences获取当前登录用户ID
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        loggedInUserId = sharedPreferences.getLong("logged_in_user_id", 1L)

        // 初始化 Markdown 和预览组件
        markwon = MarkdownUtils.createMarkwon()

        // 设置ServiceLocator上下文
        ServiceLocator.setContext(applicationContext)

        // 初始化TagRepository
        tagRepository = TagRepositoryImpl(applicationContext)

        // 初始化ViewModels并设置用户ID
        initViewModels()

        // 设置工具栏
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "编辑笔记"

        // 加载标签数据
        loadTags()

        // 使用OnBackPressedDispatcher注册返回按钮处理器
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackPress()
            }
        })

        // 获取传递过来的noteId
        noteId = intent.getLongExtra("noteId", -1)

        // 设置标签切换点击事件
        binding.changeTagButton.setOnClickListener { showTagSelectionDialog() }

        // 设置预览按钮点击
        binding.previewButton.setOnClickListener { togglePreviewMode() }

        // 监听标签数据变化
        observeTags()

        // 设置格式化按钮点击事件
        setupFormattingButtons()

        // 设置文本变化监听器
        setupTextWatchers()
        setupInPageSearchListeners()
        // 观察ViewModel中的数据变化
        observeViewModel()

        // 如果是编辑现有笔记，则加载笔记内容
        if (noteId != null && noteId != -1L) {
            noteDetailViewModel.loadNote(noteId!!)
        } else {
            // 新建笔记，先设置默认标签名称
            binding.tagTextView.text = "未选择标签"

            // 检查是否有预选中的标签ID
            val preSelectedTagId = intent.getLongExtra("preSelectedTagId", 0L)
            if (preSelectedTagId > 0) {
                handlePreSelectedTag(preSelectedTagId)
            }
        }

        setupAiListeners()
        observeAiState()
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.note_edit_menu, menu)

        // 1. 获取一个深色（例如黑色）
        val iconColor = ContextCompat.getColor(this, android.R.color.black)

        // 2. 遍历菜单中的每一个item
        if (menu != null) {
            for (i in 0 until menu.size()) {
                val menuItem = menu.getItem(i)
                val icon = menuItem.icon
                if (icon != null) {
                    // 3. 为图标设置颜色过滤器，强制其变为我们想要的颜色
                    // 这样做不会影响返回箭头，只影响菜单中的图标
                    icon.colorFilter = PorterDuffColorFilter(iconColor, PorterDuff.Mode.SRC_IN)
                }
            }
        }
        return true
    }




    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                handleBackPress()
                return true}

            R.id.action_save -> {
                saveNote()
                return true
            }

            R.id.action_share -> {
                shareNote()
                return true
            }

            R.id.action_delete -> {
                showDeleteConfirmationDialog()
                return true
            }
            // +++ 新增下面这个 when 分支 +++
            R.id.action_search_in_page -> {
                noteDetailViewModel.toggleSearchView()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }


    private fun observeViewModel() {
        // 观察笔记数据变化
        lifecycleScope.launch {
            noteDetailViewModel.note.collect { note ->
                note?.let {
                    binding.titleEditText.setText(it.title)
                    //直接显示 Markdown 文本，不再使用 StyleManager
                    noteDetailViewModel.onNoteContentChanged(it.content) // <-- 替换为这一行


                    // 设置标签
                    if (realTags.isNotEmpty()) {
                        currentTag = realTags.find { tag -> tag.tagId == it.tagId }
                        if (currentTag == null && realTags.isNotEmpty()) {
                            currentTag = realTags[0]
                        }
                        binding.tagTextView.text = currentTag?.name
                    } else {
                        val noteTagId = it.tagId
                        binding.tagTextView.text = "加载中..."

                        lifecycleScope.launch {
                            tagRepository.getAllTags().collect { tags ->
                                if (tags.isNotEmpty() && currentTag == null) {
                                    currentTag = tags.find { tag -> tag.tagId == noteTagId }
                                    if (currentTag == null) {
                                        currentTag = tags[0]
                                    }
                                    binding.tagTextView.text = currentTag?.name
                                }
                            }
                        }
                    }
                }
            }
        }

        // +++ 恢复对 loadingIndicator 和 errorTextView 的控制 +++
        // 观察加载状态
        lifecycleScope.launch {
            noteDetailViewModel.isLoading.collect { isLoading ->
                binding.loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
                if (isLoading) {
                    binding.errorTextView.visibility = View.GONE
                }
            }
        }

        // 观察错误状态
        lifecycleScope.launch {
            noteDetailViewModel.error.collect { error ->
                if (error != null) {
                    binding.errorTextView.text = error
                    binding.errorTextView.visibility = View.VISIBLE
                } else {
                    binding.errorTextView.visibility = View.GONE
                }
            }
        }
        // +++ 恢复结束 +++

        // 观察保存状态
        lifecycleScope.launch {
            noteDetailViewModel.isSaved.collect { isSaved ->
                if (isSaved) {
                    showToast("笔记已保存")
                    noteDetailViewModel.resetSaveState()
                    finish()
                }
            }
        }
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                noteDetailViewModel.highlightedContent.collect { highlightedText ->
                    // 暂时移除监听器，防止在程序设置文本时触发死循环
                    if (::contentWatcher.isInitialized) { // 确保 watcher 已初始化
                        binding.contentEditText.removeTextChangedListener(contentWatcher)
                    }

                    val selectionStart = binding.contentEditText.selectionStart
                    val selectionEnd = binding.contentEditText.selectionEnd
                    binding.contentEditText.setText(highlightedText, TextView.BufferType.SPANNABLE)

                    // 尝试恢复光标位置
                    try {
                        if (selectionStart <= highlightedText.length && selectionEnd <= highlightedText.length) {
                            binding.contentEditText.setSelection(selectionStart, selectionEnd)
                        }
                    } catch (e: Exception) {
                        // 如果失败，将光标移到末尾
                        binding.contentEditText.setSelection(highlightedText.length)
                    }

                    // 重新添加监听器
                    if (::contentWatcher.isInitialized) {
                        binding.contentEditText.addTextChangedListener(contentWatcher)
                    }
                }
            }
        }

// 观察搜索栏的可见性
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                noteDetailViewModel.isSearchViewVisible.collect { isVisible ->
                    binding.searchBarContainer.isVisible = isVisible
                    if (isVisible) {
                        binding.searchInPageEditText.requestFocus()
                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.showSoftInput(binding.searchInPageEditText, InputMethodManager.SHOW_IMPLICIT)
                    } else {
                        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        imm.hideSoftInputFromWindow(binding.searchInPageEditText.windowToken, 0)
                    }
                }
            }
        }

// 观察并更新搜索匹配计数
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    noteDetailViewModel.currentMatchIndex,
                    noteDetailViewModel.matchRanges,
                    noteDetailViewModel.searchQueryInNote
                ) { index, ranges, query ->
                    if (query.isBlank()) " " else if (ranges.isEmpty()) "0/0" else "${index + 1}/${ranges.size}"
                }.collect { countText ->
                    binding.searchMatchCount.text = countText
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                noteDetailViewModel.currentMatchIndex.collect { index ->
                    // 只有在有有效匹配项时才执行
                    if (index != -1) {
                        scrollToMatch(index)
                    }
                }
            }
        }
    }
    private fun scrollToMatch(matchIndex: Int) {
        // 确保 EditText 有布局并且 ViewModel 中有匹配范围
        val layout = binding.contentEditText.layout ?: return
        val ranges = noteDetailViewModel.matchRanges.value
        if (matchIndex < 0 || matchIndex >= ranges.size) {
            return
        }

        // 1. 获取当前匹配项在文本中的起始位置
        val currentRange = ranges[matchIndex]
        val startOffset = currentRange.first

        // 2. 计算该位置所在的行号
        val line = layout.getLineForOffset(startOffset)

        // 3. 计算该行的顶部Y坐标 (相对于EditText的顶部)
        val lineTop = layout.getLineTop(line)

        // 4. 获取EditText在NestedScrollView中的相对Y坐标
        val editTextTop = binding.contentEditText.top

        // 5. 计算最终需要滚动的Y坐标
        val scrollY = editTextTop + lineTop

        // 6. 命令 NestedScrollView 滚动
        binding.nestedScrollViewId.smoothScrollTo(0, scrollY)
    }


    private fun saveNote() {
        val title = binding.titleEditText.text.toString().trim()
        val content = binding.contentEditText.text.toString().trim()

        // 添加标题验证
        if (title.isEmpty()) {
            Toast.makeText(this, "标题为空，请添加一个标题！", Toast.LENGTH_SHORT).show()
            return
        }

        if (title.isNotEmpty() || content.isNotEmpty()) {
            lifecycleScope.launch {
                var tagIdToUse = currentTag?.tagId

                // 如果没有选择标签，尝试获取或创建"未分类"标签
                if (tagIdToUse == null) {
                    // 尝试获取"未分类"标签
                    var uncategorizedTag = tagRepository.getTagByName(loggedInUserId, "未分类")

                    // 如果"未分类"标签不存在，创建一个新的
                    if (uncategorizedTag == null) {
                        val newTag = Tag(
                            tagId = 0,
                            userId = loggedInUserId,
                            name = "未分类",
                            noteCount = 0
                        )
                        tagRepository.saveTag(newTag)
                        uncategorizedTag = tagRepository.getTagByName(loggedInUserId, "未分类")
                    }

                    tagIdToUse = uncategorizedTag?.tagId ?: 1
                }

                val note = if (noteId != null && noteId != -1L) {
                    // 更新现有笔记
                    Note(
                        noteId = noteId!!,
                        userId = loggedInUserId,
                        tagId = tagIdToUse,
                        title = title,
                        content = content,
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    // 创建新笔记
                    Note(
                        noteId = 0,
                        userId = loggedInUserId,
                        tagId = tagIdToUse,
                        title = title,
                        content = content,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                }

                // 使用ViewModel保存笔记
                noteDetailViewModel.saveNote(note)
            }
        }
    }

    private fun shareNote() {
        val title = binding.titleEditText.text.toString().trim()
        val content = binding.contentEditText.text.toString().trim()

        if (title.isEmpty() && content.isEmpty()) {
            showToast("没有可分享的内容")
            return
        }

        val shareContent = if (title.isNotEmpty()) "$title\n\n$content" else content
        val shareIntent = Intent(Intent.ACTION_SEND)
        shareIntent.type = "text/plain"
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareContent)
        startActivity(Intent.createChooser(shareIntent, "分享笔记"))
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("删除笔记")
            .setMessage("确定要删除这篇笔记吗？")
            .setPositiveButton("删除") { dialog, which ->
                if (noteId != null && noteId != -1L) {
                    noteDetailViewModel.deleteNote(noteId!!)
                    showToast("笔记已删除")
                }
                finish()
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        noteDetailViewModel.resetSaveState()
    }

    private fun initViewModels() {
        noteDetailViewModel.setLoggedInUserId(loggedInUserId)
        tagsViewModel.setLoggedInUserId(loggedInUserId)
    }

    private fun loadTags() {
        tagsViewModel.loadTags()
    }

    private fun observeTags() {
        lifecycleScope.launch {
            try {
                tagsViewModel.tags.collect { tags ->
                    realTags.clear()
                    realTags.addAll(tags)

                    if (currentTag == null && realTags.isNotEmpty()) {
                        val preSelectedTagId = intent.getLongExtra("preSelectedTagId", 0L)
                        if (preSelectedTagId > 0) {
                            currentTag = realTags.find { it.tagId == preSelectedTagId }
                        }

                        if (currentTag == null) {
                            // 当没有预选中的标签时，优先选择"未归档"标签
                            currentTag = realTags.find { it.name == "未归档" } ?: realTags[0]
                        }

                        binding.tagTextView.text = currentTag?.name
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun handlePreSelectedTag(preSelectedTagId: Long) {
        if (realTags.isNotEmpty()) {
            currentTag = realTags.find { it.tagId == preSelectedTagId }
            if (currentTag == null && realTags.isNotEmpty()) {
                // 当找不到预选中的标签时，优先选择"未归档"标签
                currentTag = realTags.find { it.name == "未归档" } ?: realTags[0]
            }
            binding.tagTextView.text = currentTag?.name
        }
    }

    private fun showTagSelectionDialog() {
        if (realTags.isEmpty()) {
            showToast("没有可用的标签")
            return
        }

        val tagNames = realTags.map { it.name }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("选择标签")
            .setItems(tagNames) { dialog, which ->
                currentTag = realTags[which]
                binding.tagTextView.text = currentTag?.name
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun setupFormattingButtons() {
        binding.boldButton.setOnClickListener {
            MarkdownUtils.insertMarkdownFormat(binding.contentEditText, "bold")
            isBold = !isBold
            binding.boldButton.setColorFilter(
                if (isBold) getColor(R.color.brand_primary) else getColor(
                    R.color.text_gray
                )
            )
        }

        binding.italicButton.setOnClickListener {
            MarkdownUtils.insertMarkdownFormat(binding.contentEditText, "italic")
            isItalic = !isItalic
            binding.italicButton.setColorFilter(
                if (isItalic) getColor(R.color.brand_primary) else getColor(
                    R.color.text_gray
                )
            )
        }

        binding.underlineButton.setOnClickListener {
            MarkdownUtils.insertMarkdownFormat(binding.contentEditText, "underline")
            isUnderline = !isUnderline
            binding.underlineButton.setColorFilter(
                if (isUnderline) getColor(R.color.brand_primary) else getColor(
                    R.color.text_gray
                )
            )
        }

        binding.bulletListButton.setOnClickListener {
            MarkdownUtils.insertMarkdownFormat(binding.contentEditText, "bullet")
        }

        binding.numberListButton.setOnClickListener {
            MarkdownUtils.insertMarkdownFormat(binding.contentEditText, "numbered")
        }
    }


    private fun togglePreviewMode() {
        isPreviewMode = !isPreviewMode

        if (isPreviewMode) {
            // 切换到预览模式
            binding.contentEditText.visibility = View.GONE
            binding.previewTextView.visibility = View.VISIBLE

            val markdownText = binding.contentEditText.text.toString()
            MarkdownUtils.renderMarkdown(binding.previewTextView, markdownText, markwon)

            // 按钮文字改成“编辑”
            binding.previewButton.text = "编辑"

            // 隐藏键盘
            binding.contentEditText.clearFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.contentEditText.windowToken, 0)

        } else {
            // 切换到编辑模式
            binding.contentEditText.visibility = View.VISIBLE
            binding.previewTextView.visibility = View.GONE

            // 按钮文字改成“预览”
            binding.previewButton.text = "预览"

            // 显示键盘
            binding.contentEditText.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(binding.contentEditText, InputMethodManager.SHOW_IMPLICIT)
        }
    }


    private fun setupTextWatchers() {
        contentWatcher = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                // 当用户在 EditText 中输入时，将纯文本内容同步到 ViewModel
                // 注意：这里传递 s.toString() 是为了获取无样式的纯文本
                noteDetailViewModel.onNoteContentChanged(s.toString())
            }
        }
        binding.contentEditText.addTextChangedListener(contentWatcher)
    }

    private fun setupInPageSearchListeners() {
        // 监听搜索输入框的文本变化
        binding.searchInPageEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                noteDetailViewModel.onSearchQueryChanged(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // "上一个" 按钮
        binding.searchPreviousButton.setOnClickListener {
            noteDetailViewModel.previousMatch()
        }

        // "下一个" 按钮
        binding.searchNextButton.setOnClickListener {
            noteDetailViewModel.nextMatch()
        }

        // "关闭" 按钮
        binding.searchCloseButton.setOnClickListener {
            noteDetailViewModel.toggleSearchView()
        }
    }

    private fun handleBackPress() {
        val hasChanges = binding.titleEditText.text.toString()
            .isNotEmpty() || binding.contentEditText.text.toString().isNotEmpty()
        if (hasChanges) {
            AlertDialog.Builder(this)
                .setTitle("保存笔记")
                .setMessage("是否保存当前笔记？")
                .setPositiveButton("保存") { dialog, which ->
                    saveNote()
                    finish()
                }
                .setNegativeButton("不保存") { dialog, which ->
                    finish()
                }
                .setNeutralButton("取消") { dialog, which ->
                    dialog.dismiss()
                }
                .show()
        } else {
            finish()
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun setupAiListeners() {
        // AI Summary
        binding.aiSummaryButton.setOnClickListener {
            val text = binding.contentEditText.text.toString()
            if (validateInput(text)) {
                aiViewModel.onSourceTextChanged(text)
                aiViewModel.fetchSummary()
            }
        }

        // AI Tagging
        binding.aiTagButton.setOnClickListener {
            val text = binding.contentEditText.text.toString()
            val currentTags = binding.tagTextView.text.toString()
            if (validateInput(text)) {
                aiViewModel.onSourceTextChanged(text)
                aiViewModel.onTagsInputChanged(currentTags)
                aiViewModel.fetchTags()
            }
        }
    }

    private fun validateInput(text: String): Boolean {
        if (text.isBlank()) {
            Toast.makeText(this, "请先输入笔记内容", Toast.LENGTH_SHORT).show()
            return false
        }
        return true
    }

    private fun observeAiState() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                aiViewModel.uiState.collect { state ->
                    // Handle loading
                    updateLoadingState(state.isLoading)

                    // Handle error
                    state.error?.let {
                        Toast.makeText(this@NoteEditActivity, it, Toast.LENGTH_SHORT).show()
                    }

                    // Summary dialog
                    if (state.summaryResult.isNotEmpty() && state.summaryResult != lastSummary) {
                        lastSummary = state.summaryResult
                        showSummaryDialog(state.summaryResult)
                    }

                    // Tag dialog
                    if (state.tagResult.isNotEmpty() && state.tagResult != lastTags) {
                        lastTags = state.tagResult
                        showTagDialog(state.tagResult)
                    }
                }
            }
        }
    }

    private fun updateLoadingState(isLoading: Boolean) {
        binding.aiProgressBar.apply {
            if (isLoading) {
                isIndeterminate = true
                show()
            } else {
                hide()
            }
        }

        with(binding.aiSummaryButton) {
            isEnabled = !isLoading
            alpha = if (isLoading) 0.5f else 1.0f
        }

        with(binding.aiTagButton) {
            isEnabled = !isLoading
            alpha = if (isLoading) 0.5f else 1.0f
        }
    }

    private fun showSummaryDialog(summary: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("AI 摘要生成完毕")
            .setMessage(summary)
            .setPositiveButton("复制到剪贴板") { _, _ ->
                copyToClipboard("AI Summary", summary)
            }
            .setNegativeButton("关闭", null)
            .show()
    }

    private fun showTagDialog(tags: List<String>) {
        val tagString = tags.joinToString(", ")
        MaterialAlertDialogBuilder(this)
            .setTitle("AI 推荐标签")
            .setMessage("为您找到以下标签：\n$tagString")
            .setPositiveButton("复制到剪贴板") { _, _ ->
                copyToClipboard("AI Tag", tagString)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun copyToClipboard(label: String, text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "已复制", Toast.LENGTH_SHORT).show()
    }
}