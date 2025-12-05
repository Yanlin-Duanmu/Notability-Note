package com.noteability.mynote.ui.activity

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.noteability.mynote.R
import com.noteability.mynote.data.entity.Note
import com.noteability.mynote.data.entity.Tag
import com.noteability.mynote.data.repository.impl.NoteRepositoryImpl
import com.noteability.mynote.data.repository.impl.TagRepositoryImpl
import com.noteability.mynote.di.ServiceLocator
import com.noteability.mynote.ui.viewmodel.NoteDetailViewModel
import com.noteability.mynote.ui.viewmodel.TagsViewModel
import com.noteability.mynote.utils.MarkdownUtils
import io.noties.markwon.Markwon
import kotlinx.coroutines.launch

class NoteEditActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var tagTextView: TextView
    private lateinit var changeTagButton: ImageView
    private lateinit var titleEditText: EditText
    private lateinit var contentEditText: EditText
    private lateinit var boldButton: ImageButton
    private lateinit var italicButton: ImageButton
    private lateinit var underlineButton: ImageButton
    private lateinit var bulletListButton: ImageButton
    private lateinit var numberListButton: ImageButton
    private lateinit var loadingIndicator: ProgressBar
    private lateinit var errorTextView: TextView

    // 新增：预览相关组件
    private lateinit var previewButton: ImageButton
    private lateinit var previewTextView: TextView
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

    // 创建NoteDetailViewModelFactory
    private class NoteDetailViewModelFactory(private val applicationContext: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NoteDetailViewModel::class.java)) {
                val repository = NoteRepositoryImpl(applicationContext)
                val viewModel = NoteDetailViewModel(repository)
                return viewModel as? T ?: throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }

    // 创建TagsViewModelFactory
    private class TagsViewModelFactory(private val applicationContext: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TagsViewModel::class.java)) {
                val repository = TagRepositoryImpl(applicationContext)
                val viewModel = TagsViewModel(repository)
                return viewModel as? T ?: throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
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
        setContentView(R.layout.activity_note_edit)

        // 从SharedPreferences获取当前登录用户ID
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        loggedInUserId = sharedPreferences.getLong("logged_in_user_id", 1L)

        // 初始化界面组件
        toolbar = findViewById(R.id.toolbar)
        tagTextView = findViewById(R.id.tagTextView)
        changeTagButton = findViewById(R.id.changeTagButton)
        titleEditText = findViewById(R.id.titleEditText)
        contentEditText = findViewById(R.id.contentEditText)
        boldButton = findViewById(R.id.boldButton)
        italicButton = findViewById(R.id.italicButton)
        underlineButton = findViewById(R.id.underlineButton)
        bulletListButton = findViewById(R.id.bulletListButton)
        numberListButton = findViewById(R.id.numberListButton)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        errorTextView = findViewById(R.id.errorTextView)

        // 初始化 Markdown 和预览组件
        markwon = MarkdownUtils.createMarkwon()
        previewButton = findViewById(R.id.previewButton)
        previewTextView = findViewById(R.id.previewTextView)

        // 设置ServiceLocator上下文
        ServiceLocator.setContext(applicationContext)

        // 初始化TagRepository
        tagRepository = TagRepositoryImpl(applicationContext)

        // 初始化ViewModels并设置用户ID
        initViewModels()

        // 设置工具栏
        setSupportActionBar(toolbar)
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
        changeTagButton.setOnClickListener {
            showTagSelectionDialog()
        }

        // 设置预览按钮点击
        previewButton.setOnClickListener {
            togglePreviewMode()
        }

        // 监听标签数据变化
        observeTags()

        // 设置格式化按钮点击事件
        setupFormattingButtons()

        // 设置文本变化监听器
        setupTextWatchers()

        // 观察ViewModel中的数据变化
        observeViewModel()

        // 如果是编辑现有笔记，则加载笔记内容
        if (noteId != null && noteId != -1L) {
            noteDetailViewModel.loadNote(noteId!!)
        } else {
            // 新建笔记，先设置默认标签名称
            tagTextView.text = "未选择标签"

            // 检查是否有预选中的标签ID
            val preSelectedTagId = intent.getLongExtra("preSelectedTagId", 0L)
            if (preSelectedTagId > 0) {
                handlePreSelectedTag(preSelectedTagId)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.note_edit_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                handleBackPress()
                return true
            }
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
        }
        return super.onOptionsItemSelected(item)
    }
    private fun observeViewModel() {
        // 观察笔记数据变化
        lifecycleScope.launch {
            noteDetailViewModel.note.collect { note ->
                note?.let {
                    titleEditText.setText(it.title)
                    //直接显示 Markdown 文本，不再使用 StyleManager
                    contentEditText.setText(it.content)

                    // 设置标签
                    if (realTags.isNotEmpty()) {
                        currentTag = realTags.find { tag -> tag.tagId == it.tagId }
                        if (currentTag == null && realTags.isNotEmpty()) {
                            currentTag = realTags[0]
                        }
                        tagTextView.text = currentTag?.name
                    } else {
                        val noteTagId = it.tagId
                        tagTextView.text = "加载中..."

                        lifecycleScope.launch {
                            tagRepository.getAllTags().collect { tags ->
                                if (tags.isNotEmpty() && currentTag == null) {
                                    currentTag = tags.find { tag -> tag.tagId == noteTagId }
                                    if (currentTag == null) {
                                        currentTag = tags[0]
                                    }
                                    tagTextView.text = currentTag?.name
                                }
                            }
                        }
                    }
                }
            }
        }

        // 观察加载状态
        lifecycleScope.launch {
            noteDetailViewModel.isLoading.collect { isLoading ->
                loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
                if (isLoading) {
                    errorTextView.visibility = View.GONE
                }
            }
        }

        // 观察错误状态
        lifecycleScope.launch {
            noteDetailViewModel.error.collect { error ->
                if (error != null) {
                    errorTextView.text = error
                    errorTextView.visibility = View.VISIBLE
                } else {
                    errorTextView.visibility = View.GONE
                }
            }
        }

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
    }

    private fun saveNote() {
        val title = titleEditText.text.toString().trim()
        val content = contentEditText.text.toString().trim()

        // 添加标题验证
        if (title.isEmpty()) {
            Toast.makeText(this, "标题为空，请添加一个标题！", Toast.LENGTH_SHORT).show()
            return
        }

        if (title.isNotEmpty() || content.isNotEmpty()) {
            val note = if (noteId != null && noteId != -1L) {
                // 更新现有笔记
                Note(
                    noteId = noteId!!,
                    userId = loggedInUserId,
                    tagId = currentTag?.tagId ?: 1,
                    title = title,
                    content = content,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                // 创建新笔记
                Note(
                    noteId = 0,
                    userId = loggedInUserId,
                    tagId = currentTag?.tagId ?: 1,
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

    private fun shareNote() {
        val title = titleEditText.text.toString().trim()
        val content = contentEditText.text.toString().trim()

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
                            currentTag = realTags[0]
                        }

                        tagTextView.text = currentTag?.name
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
                currentTag = realTags[0]
            }
            tagTextView.text = currentTag?.name
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
                tagTextView.text = currentTag?.name
            }
            .setNegativeButton("取消") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun setupFormattingButtons() {
        boldButton.setOnClickListener {
            MarkdownUtils.insertMarkdownFormat(contentEditText, "bold")
            isBold = !isBold
            boldButton.setColorFilter(if (isBold) getColor(R.color.purple_500) else getColor(R.color.gray_700))
        }

        italicButton.setOnClickListener {
            MarkdownUtils.insertMarkdownFormat(contentEditText, "italic")
            isItalic = !isItalic
            italicButton.setColorFilter(if (isItalic) getColor(R.color.purple_500) else getColor(R.color.gray_700))
        }

        underlineButton.setOnClickListener {
            MarkdownUtils.insertMarkdownFormat(contentEditText, "underline")
            isUnderline = !isUnderline
            underlineButton.setColorFilter(if (isUnderline) getColor(R.color.purple_500) else getColor(R.color.gray_700))
        }

        bulletListButton.setOnClickListener {
            MarkdownUtils.insertMarkdownFormat(contentEditText, "bullet")
        }

        numberListButton.setOnClickListener {
            MarkdownUtils.insertMarkdownFormat(contentEditText, "numbered")
        }
    }



    private fun togglePreviewMode() {
        isPreviewMode = !isPreviewMode

        if (isPreviewMode) {
            // 切换到预览模式
            contentEditText.visibility = View.GONE
            previewTextView.visibility = View.VISIBLE

            // 渲染 Markdown
            val markdownText = contentEditText.text.toString()
            MarkdownUtils.renderMarkdown(previewTextView, markdownText, markwon)

            // 更新按钮颜色
            previewButton.setColorFilter(getColor(R.color.purple_500))

            // 隐藏键盘
            contentEditText.clearFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(contentEditText.windowToken, 0)
        } else {
            // 切换回编辑模式
            contentEditText.visibility = View.VISIBLE
            previewTextView.visibility = View.GONE

            // 恢复按钮颜色
            previewButton.setColorFilter(getColor(R.color.gray_700))

            // 显示键盘
            contentEditText.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(contentEditText, InputMethodManager.SHOW_IMPLICIT)
        }
    }

    private fun setupTextWatchers() {
    }

    private fun handleBackPress() {
        val hasChanges = titleEditText.text.toString().isNotEmpty() || contentEditText.text.toString().isNotEmpty()
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
}