package com.noteability.mynote

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.activity.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView

import com.google.android.material.progressindicator.CircularProgressIndicator
import androidx.lifecycle.ViewModelProvider
import com.noteability.mynote.data.repository.impl.NoteRepositoryImpl
import com.noteability.mynote.data.repository.impl.TagRepositoryImpl
import com.noteability.mynote.ui.adapter.NoteAdapter
import com.noteability.mynote.ui.viewmodel.NotesViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var notesRecyclerView: RecyclerView
    private lateinit var searchEditText: EditText
    private lateinit var tagsContainer: LinearLayout
    private lateinit var addNoteButton: androidx.appcompat.widget.AppCompatButton
    // private lateinit var filterButton: ImageView // filterButton 已被移除
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var loadingIndicator: CircularProgressIndicator
    private lateinit var emptyStateView: TextView
    private lateinit var errorStateView: TextView

    // 为NotesViewModel创建工厂类
    private class NotesViewModelFactory(private val applicationContext: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(NotesViewModel::class.java)) {
                // 创建带Context的NoteRepositoryImpl实例
                val repository = NoteRepositoryImpl(applicationContext)
                val viewModel = NotesViewModel(repository)
                return viewModel as? T ?: throw IllegalArgumentException("Cannot create ViewModel for class: $modelClass")
            }
            throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
        }
    }

    // 使用自定义工厂类获取NotesViewModel实例
    private val viewModel: NotesViewModel by viewModels { NotesViewModelFactory(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 初始化界面组件
        notesRecyclerView = findViewById(R.id.notesRecyclerView)
        searchEditText = findViewById(R.id.searchEditText)
        tagsContainer = findViewById(R.id.tagsContainer)
        addNoteButton = findViewById(R.id.addNoteButton)
        // filterButton = findViewById(R.id.filterButton) // filterButton 已被移除
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        emptyStateView = findViewById(R.id.emptyStateView)
        errorStateView = findViewById(R.id.errorStateView)

        // 初始化RecyclerView
        setupRecyclerView()

        // 加载标签
        loadTags()

        // 设置搜索监听器
        setupSearchListener()

        // 设置按钮点击事件
        setupButtonListeners()

        // 设置底部导航栏监听器
        setupBottomNavigationListener()

        // 观察ViewModel数据
        observeViewModel()

        // 检查是否有从标签管理页面传来的选择标签
        val selectedTagId = intent.getLongExtra("selectedTagId", 0L)
        if (selectedTagId > 0) {
            // 保存当前选中的标签ID
            currentSelectedTagId = selectedTagId
            // 加载该标签下的笔记
            viewModel.loadNotesByTag(selectedTagId)
        }
    }

    private fun setupRecyclerView() {
        // 初始化TagRepository获取标签数据
        val tagRepository = TagRepositoryImpl(applicationContext)
        val tagNameMap = mutableMapOf<Long, String>()

        // 立即加载标签数据
        lifecycleScope.launch {
            tagRepository.getAllTags().collect { tagsList ->
                tagNameMap.clear()
                tagsList.forEach { tag ->
                    tagNameMap[tag.tagId] = tag.name
                }
                // 更新适配器中的标签映射
                noteAdapter.updateTagNameMap(tagNameMap)
            }
        }

        // 创建适配器，初始为空的标签映射
        noteAdapter = NoteAdapter(emptyList(), onNoteClick = { note ->
            // 处理笔记点击事件，跳转到笔记编辑页面
            val intent = Intent(this, NoteEditActivity::class.java)
            intent.putExtra("noteId", note.noteId)
            startActivity(intent)
        }, tagNameMap = tagNameMap)

        notesRecyclerView.layoutManager = LinearLayoutManager(this)
        notesRecyclerView.adapter = noteAdapter
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false // 我们不关心拖动操作
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val noteToDelete = noteAdapter.getNoteAt(position)
                viewModel.deleteNote(noteToDelete.noteId)

                Snackbar.make(notesRecyclerView, "笔记已删除", Snackbar.LENGTH_LONG)
                    .setAction("撤销") { 
                        viewModel.saveNote(noteToDelete)
                    }
                    .show()
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(notesRecyclerView)
    }

    private fun loadTags() {
        // 清除现有的标签按钮
        tagsContainer.removeAllViews()

        // 初始化TagRepository获取真实的标签数据
        val tagRepository = TagRepositoryImpl(applicationContext)

        lifecycleScope.launch {
            tagRepository.getAllTags().collect { tags ->
                // 清除现有的标签按钮
                tagsContainer.removeAllViews()

                // 动态添加 "全部" 标签
                val allTagView = LayoutInflater.from(this@MainActivity)
                    .inflate(R.layout.item_tag, tagsContainer, false) as TextView
                allTagView.text = "全部"
                allTagView.setOnClickListener {
                    // 清空搜索框
                    searchEditText.text.clear()
                    // 加载所有笔记
                    currentSelectedTagId = 0L
                    viewModel.loadNotes()
                }
                tagsContainer.addView(allTagView)

                // 动态添加其他标签按钮
                for (tag in tags) {
                    val tagView = LayoutInflater.from(this@MainActivity)
                        .inflate(R.layout.item_tag, tagsContainer, false) as TextView
                    tagView.text = tag.name
                    tagView.setOnClickListener {
                        // 清空搜索框
                        searchEditText.text.clear()
                        
                        // 如果点击的标签就是当前选中的标签，则取消选中并显示所有笔记
                        if (currentSelectedTagId == tag.tagId) {
                            currentSelectedTagId = 0L
                            viewModel.loadNotes()
                        } else {
                            // 否则，切换到新点击的标签
                            currentSelectedTagId = tag.tagId
                            viewModel.loadNotesByTag(tag.tagId)
                        }
                    }
                    tagsContainer.addView(tagView)
                    tagViews[tag.tagId] = tagView
                }
                updateTagSelectionState() // 初始化状态
            }
        }
    }

    private fun updateTagSelectionState() {
        // 重置所有标签的样式
        (tagViews.values + allTagView).forEach { view ->
            view?.setBackgroundResource(R.drawable.tag_unselected_background)
            view?.setTextColor(ContextCompat.getColor(this, R.color.tag_unselected_text_color))
        }

        // 设置选中标签的样式
        val selectedView = if (currentSelectedTagId == 0L) allTagView else tagViews[currentSelectedTagId]
        selectedView?.setBackgroundResource(R.drawable.tag_selected_background)
        selectedView?.setTextColor(Color.WHITE)
    }

    private fun setupSearchListener() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                viewModel.searchNotes(query)
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // 存储当前选中的标签ID，默认为0表示未筛选标签
    private var currentSelectedTagId: Long = 0L

    private fun setupButtonListeners() {
        addNoteButton.setOnClickListener {
            // 跳转到新建笔记页面
            val intent = Intent(this, NoteEditActivity::class.java)
            // 如果当前处于标签筛选模式，传递标签ID给编辑页面
            if (currentSelectedTagId > 0) {
                intent.putExtra("preSelectedTagId", currentSelectedTagId)
            }
            startActivity(intent)
        }

        // filterButton的点击监听器已被移除

        errorStateView.setOnClickListener {
            // 重试加载
            viewModel.loadNotes()
        }
    }

    private fun setupBottomNavigationListener() {
        bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_notes -> {
                    // 检查是否当前处于特定标签筛选模式
                    if (currentSelectedTagId > 0) {
                        // 如果是，重置标签筛选，加载所有笔记
                        currentSelectedTagId = 0L
                        viewModel.loadNotes()
                        showToast("显示所有笔记")
                    }
                    // 无论是否处于筛选模式，都返回true
                    true
                }
                R.id.nav_tags -> {
                    // 跳转到标签管理页面
                    val intent = Intent(this, TagManagementActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_settings -> {
                    // 跳转到设置页面
                    showToast("跳转到设置页面")
                    true
                }
                else -> false
            }
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            // 观察笔记列表
            viewModel.notes.collect { notes ->
                noteAdapter.updateNotes(notes)
                // 检查搜索框内容，以决定“空状态”的提示文本
                val isSearching = searchEditText.text.isNotEmpty()
                updateUIState(notes.isEmpty(), isSearching)
            }
        }

        lifecycleScope.launch {
            // 观察加载状态
            viewModel.isLoading.collect { isLoading ->
                loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            // 观察错误状态
            viewModel.error.collect { errorMessage ->
                errorStateView.text = errorMessage ?: getString(R.string.loading_failed)
                errorStateView.visibility = if (errorMessage != null) View.VISIBLE else View.GONE
            }
        }
    }

    private fun updateUIState(isEmptyList: Boolean, isSearching: Boolean) {
        if (isEmptyList && !viewModel.isLoading.value) {
            emptyStateView.visibility = View.VISIBLE
            notesRecyclerView.visibility = View.GONE
            // 根据是否在搜索中，显示不同的提示文本
            if (isSearching) {
                emptyStateView.text = "没有找到相关内容" // R.string.no_search_results_found
            } else {
                emptyStateView.text = "还没有笔记，快来添加吧" // R.string.no_notes_yet
            }
        } else {
            emptyStateView.visibility = View.GONE
            notesRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        // 从其他Activity返回时，根据当前选中的标签或搜索框状态重新加载笔记
        val searchQuery = searchEditText.text.toString()
        if (searchQuery.isNotEmpty()) {
            viewModel.searchNotes(searchQuery)
        } else if (currentSelectedTagId > 0) {
            viewModel.loadNotesByTag(currentSelectedTagId)
        } else {
            viewModel.loadNotes()
        }
    }
}
