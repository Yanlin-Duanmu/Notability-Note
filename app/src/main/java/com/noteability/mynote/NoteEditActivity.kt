package com.noteability.mynote

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.SpannableString
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.text.style.UnderlineSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.content.Context
import com.noteability.mynote.data.entity.Note
import com.noteability.mynote.data.entity.Tag
import com.noteability.mynote.data.repository.impl.NoteRepositoryImpl
import com.noteability.mynote.data.repository.impl.TagRepositoryImpl
import com.noteability.mynote.ui.viewmodel.NoteDetailViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    private var noteId: Long? = null
    private var currentTag: Tag? = null
    private var isBold = false
    private var isItalic = false
    private var isUnderline = false
    
    // 真实标签数据列表
    private val realTags = mutableListOf<Tag>()
    
    // TagRepository实例
    private lateinit var tagRepository: TagRepositoryImpl
    
    // 创建ViewModelFactory
    private class NoteDetailViewModelFactory(private val applicationContext: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NoteDetailViewModel::class.java)) {
                // 创建带Context的NoteRepositoryImpl实例
                val repository = NoteRepositoryImpl(applicationContext)
                val viewModel = NoteDetailViewModel(repository)
                return viewModel as? T ?: throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
    
    // 使用viewModels委托和自定义Factory来获取NoteDetailViewModel实例
    private val noteDetailViewModel: NoteDetailViewModel by viewModels {
        NoteDetailViewModelFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_note_edit)

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
        
        // 初始化TagRepository
        tagRepository = TagRepositoryImpl(applicationContext)
        
        // 加载真实标签数据
        loadRealTags()

        // 设置工具栏
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "编辑笔记"
        
        // 使用OnBackPressedDispatcher注册返回按钮处理器（现代方式）
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // 处理返回按钮点击事件
                handleBackPress()
            }
        })

        // 获取传递过来的noteId
        noteId = intent.getLongExtra("noteId", -1)

        // 设置标签切换点击事件
        changeTagButton.setOnClickListener {
            showTagSelectionDialog()
        }

        // 设置富文本编辑按钮点击事件
        setupFormattingButtons()

        // 设置文本变化监听器
        setupTextWatchers()
        
        // 观察ViewModel中的数据变化
        observeViewModel()
        
        // 如果是编辑现有笔记，则加载笔记内容
        if (noteId != null && noteId != -1L) {
            noteDetailViewModel.loadNote(noteId!!)
        } else {
            // 新建笔记，先设置默认标签名称，标签数据加载完成后会更新
            tagTextView.text = "未选择标签"
            
            // 检查是否有预选中的标签ID
            val preSelectedTagId = intent.getLongExtra("preSelectedTagId", 0L)
            if (preSelectedTagId > 0) {
                // 预选中的标签ID会在loadRealTags中处理
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
                // 处理返回按钮点击事件
                handleBackPress()
                return true
            }
            R.id.action_save -> {
                // 保存笔记，不立即finish，让保存操作完成后再关闭
                saveNote()
                return true
            }
            R.id.action_share -> {
                // 分享笔记
                shareNote()
                return true
            }
            R.id.action_delete -> {
                // 删除笔记
                showDeleteConfirmationDialog()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    // 移除传统的onBackPressed()重写，使用OnBackPressedDispatcher

    private fun observeViewModel() {
        // 观察笔记数据变化
        lifecycleScope.launch {
            noteDetailViewModel.note.collect { note ->
                note?.let {
                    titleEditText.setText(it.title)
                    contentEditText.setText(it.content)
                    
                    // 设置标签
                    if (realTags.isNotEmpty()) {
                        // 如果标签列表已经加载完成，直接查找对应的标签
                        currentTag = realTags.find { tag -> tag.tagId == it.tagId }
                        if (currentTag == null && realTags.isNotEmpty()) {
                            currentTag = realTags[0]
                        }
                        tagTextView.text = currentTag?.name
                    } else {
                        // 如果标签列表还未加载完成，先设置标签ID，等待标签加载完成后再更新显示
                        val noteTagId = it.tagId
                        tagTextView.text = "加载中..."
                        
                        // 当标签加载完成后，会在loadRealTags方法中自动更新显示
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
                    // 保存成功后再关闭Activity
                    finish()
                }
            }
        }
    }

    private fun saveNote() {
        val title = titleEditText.text.toString().trim()
        val content = contentEditText.text.toString().trim()

        if (title.isNotEmpty() || content.isNotEmpty()) {
            val note = if (noteId != null && noteId != -1L) {
                // 更新现有笔记
                Note(
                    noteId = noteId!!,
                    userId = 1,
                    tagId = currentTag?.tagId ?: 1,
                    title = title,
                    content = content,
                    updatedAt = System.currentTimeMillis()
                )
            } else {
                // 创建新笔记
                Note(
                    noteId = 0,
                    userId = 1,
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
                    // 使用ViewModel删除笔记
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
        // 重置ViewModel状态
        noteDetailViewModel.resetSaveState()
    }

    private fun loadRealTags() {
        lifecycleScope.launch {
            tagRepository.getAllTags().collect { tags ->
                // 更新真实标签列表
                realTags.clear()
                realTags.addAll(tags)
                
                // 如果当前没有选中的标签但列表不为空，设置第一个标签为默认标签
                if (currentTag == null && realTags.isNotEmpty()) {
                    val preSelectedTagId = intent.getLongExtra("preSelectedTagId", 0L)
                    if (preSelectedTagId > 0) {
                        // 优先使用预选中的标签
                        currentTag = realTags.find { it.tagId == preSelectedTagId }
                    }
                    
                    // 如果没有找到预选中的标签或没有预选中的标签，使用第一个标签
                    if (currentTag == null) {
                        currentTag = realTags[0]
                    }
                    
                    // 更新标签显示
                    tagTextView.text = currentTag?.name
                }
            }
        }
    }
    
    private fun handlePreSelectedTag(preSelectedTagId: Long) {
        // 如果标签列表已经加载完成，立即处理预选中的标签
        if (realTags.isNotEmpty()) {
            currentTag = realTags.find { it.tagId == preSelectedTagId }
            if (currentTag == null && realTags.isNotEmpty()) {
                currentTag = realTags[0]
            }
            tagTextView.text = currentTag?.name
        }
        // 否则等待标签加载完成后处理
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
                // 更新当前选中的标签
                currentTag = realTags[which]
                tagTextView.text = currentTag?.name
            }
            .setNegativeButton("取消") { dialog, _ ->
                // 取消按钮会自动关闭对话框
            }
            .show()
    }

    private fun setupFormattingButtons() {
        boldButton.setOnClickListener {
            toggleBold()
        }

        italicButton.setOnClickListener {
            toggleItalic()
        }

        underlineButton.setOnClickListener {
            toggleUnderline()
        }

        bulletListButton.setOnClickListener {
            insertBulletList()
        }

        numberListButton.setOnClickListener {
            insertNumberedList()
        }
    }

    private fun toggleBold() {
        isBold = !isBold
        boldButton.setColorFilter(if (isBold) getColor(R.color.purple_500) else getColor(R.color.gray_700))
        applyTextStyle(android.graphics.Typeface.BOLD)
    }

    private fun toggleItalic() {
        isItalic = !isItalic
        italicButton.setColorFilter(if (isItalic) getColor(R.color.purple_500) else getColor(R.color.gray_700))
        applyTextStyle(android.graphics.Typeface.ITALIC)
    }

    private fun toggleUnderline() {
        isUnderline = !isUnderline
        underlineButton.setColorFilter(if (isUnderline) getColor(R.color.purple_500) else getColor(R.color.gray_700))
        applyUnderline()
    }

    private fun applyTextStyle(style: Int) {
        val start = contentEditText.selectionStart
        val end = contentEditText.selectionEnd
        val editable = contentEditText.text

        if (start < 0 || end <= start) return

        val spannable = SpannableString(editable)
        val spans = spannable.getSpans(start, end, StyleSpan::class.java)

        if (spans.isEmpty()) {
            // 没有应用过样式，添加新样式
            spannable.setSpan(StyleSpan(style), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        } else {
            // 已经应用过样式，检查是否需要移除
            for (span in spans) {
                if (span.style == style) {
                    spannable.removeSpan(span)
                }
            }
        }

        contentEditText.setText(spannable)
        contentEditText.setSelection(end)
    }

    private fun applyUnderline() {
        val start = contentEditText.selectionStart
        val end = contentEditText.selectionEnd
        val editable = contentEditText.text

        if (start < 0 || end <= start) return

        val spannable = SpannableString(editable)
        val spans = spannable.getSpans(start, end, UnderlineSpan::class.java)

        if (spans.isEmpty()) {
            // 没有应用过下划线，添加下划线
            spannable.setSpan(UnderlineSpan(), start, end, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        } else {
            // 已经应用过下划线，移除下划线
            for (span in spans) {
                spannable.removeSpan(span)
            }
        }

        contentEditText.setText(spannable)
        contentEditText.setSelection(end)
    }

    private fun insertBulletList() {
        val cursorPosition = contentEditText.selectionStart
        val text = contentEditText.text
        val textBeforeCursor = text.substring(0, cursorPosition)
        val textAfterCursor = text.substring(cursorPosition)

        // 插入项目符号
        contentEditText.setText("$textBeforeCursor• $textAfterCursor")
        contentEditText.setSelection(cursorPosition + 2)
    }

    private fun insertNumberedList() {
        val cursorPosition = contentEditText.selectionStart
        val text = contentEditText.text
        val textBeforeCursor = text.substring(0, cursorPosition)
        val textAfterCursor = text.substring(cursorPosition)

        // 简单实现，插入 "1. " 作为编号
        contentEditText.setText("${textBeforeCursor}1. $textAfterCursor")
        contentEditText.setSelection(cursorPosition + 3)
    }

    private fun setupTextWatchers() {
        // 可以在这里添加文本变化监听器，用于自动保存或其他功能
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
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }
}