package com.noteability.mynote.ui.activity

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.geometry.isEmpty
import androidx.compose.ui.semantics.text
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.noteability.mynote.R
import com.noteability.mynote.databinding.ActivityMainBinding
import com.noteability.mynote.di.ServiceLocator
import com.noteability.mynote.ui.adapter.NoteAdapter
import com.noteability.mynote.ui.adapter.SearchSuggestionAdapter
import com.noteability.mynote.ui.viewmodel.NotesViewModel
import com.noteability.mynote.ui.viewmodel.TagsViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var searchSuggestionAdapter: SearchSuggestionAdapter
    private var allTagView: TextView? = null
    private val tagViews = mutableMapOf<Long, TextView>()

    private lateinit var viewModel: NotesViewModel
    private lateinit var tagsViewModel: TagsViewModel

    private var currentSelectedTagId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val loggedInUserId = sharedPreferences.getLong("logged_in_user_id", 0L)

        if (loggedInUserId <= 0) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        initViewModels(loggedInUserId)
        setupToolbarAndDrawer()
        setupRecyclerView()
        setupLoadStateListener()
        setupSwipeToDelete()
        loadTags()
        setupSearchListener()
        setupButtonListeners()
        observeViewModel()

        val selectedTagId = intent.getLongExtra("selectedTagId", 0L)
        if (selectedTagId > 0) {
            currentSelectedTagId = selectedTagId
            viewModel.loadNotesByTag(selectedTagId)
        }

        setupOnBackPressed()
    }


    private fun initViewModels(loggedInUserId: Long) {
        val noteRepository = ServiceLocator.provideNoteRepository()
        val tagRepository = ServiceLocator.provideTagRepository()

        viewModel = NotesViewModel(noteRepository, ServiceLocator.provideSearchHistoryManager(this))
        tagsViewModel = TagsViewModel(tagRepository)
        viewModel.setLoggedInUserId(loggedInUserId)
        tagsViewModel.setLoggedInUserId(loggedInUserId)
    }

    private fun setupToolbarAndDrawer() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()
        binding.navigationView.setCheckedItem(R.id.nav_notes)
        setupNavigationDrawerListener()
    }

    private fun setupNavigationDrawerListener() {
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_notes -> {
                    currentSelectedTagId = 0L
                    updateTagSelectionState()
                    showToast("显示所有笔记")
                    val query = binding.searchEditText.text.toString()
                    if (query.isNotEmpty()) {
                        viewModel.searchNotes(query, 0L)
                    } else {
                        viewModel.loadNotes()
                    }
                }
                R.id.nav_tags -> {
                    startActivity(Intent(this, TagManagementActivity::class.java))
                }
                R.id.nav_settings -> {
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }


    private fun setupRecyclerView() {
        val tagNameMap = mutableMapOf<Long, String>()

        noteAdapter = NoteAdapter(onNoteClick = { note ->
            val intent = Intent(this, NoteEditActivity::class.java)
            intent.putExtra("noteId", note.noteId)
            startActivity(intent)
        }, tagNameMap = tagNameMap)

        searchSuggestionAdapter = SearchSuggestionAdapter(
            onSuggestionClick = { suggestionText ->
                val originalQuery = binding.searchEditText.text.toString()
                if (originalQuery.isNotBlank()) {
                    viewModel.saveSearchToHistory(originalQuery)
                }
                binding.searchEditText.setText(suggestionText)
                binding.searchEditText.setSelection(suggestionText.length)
                binding.searchEditText.clearFocus()
            },
            onSuggestionDelete = { query ->
                viewModel.deleteSearchFromHistory(query)
                // 注意：这里没有调用 updateSearchHistoryView()，因为 onSuggestionDelete
                // 属于智能推荐列表，而非历史记录框的删除按钮。
            }
        )

        lifecycleScope.launch {
            tagsViewModel.tags.collect { tagsList ->
                tagNameMap.clear()
                tagsList.forEach { tag -> tagNameMap[tag.tagId] = tag.name }
                noteAdapter.updateTagNameMap(tagNameMap)
            }
        }

        binding.notesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.notesRecyclerView.adapter = noteAdapter

        binding.searchSuggestionsRecyclerview.layoutManager = LinearLayoutManager(this)
        binding.searchSuggestionsRecyclerview.adapter = searchSuggestionAdapter

        binding.notesRecyclerView.itemAnimator = null
    }

    private fun setupLoadStateListener() {
        noteAdapter.addLoadStateListener { loadState ->
            // 只有当搜索框没有焦点时，才让 Paging 控制 UI，防止与建议/历史浮层冲突
            if (!binding.searchEditText.hasFocus()) {
                val isListEmpty = loadState.refresh is LoadState.NotLoading && noteAdapter.itemCount == 0
                val isRefresh = loadState.refresh is LoadState.Loading

                binding.loadingIndicator.visibility = if (isRefresh) View.VISIBLE else View.GONE
                binding.emptyStateView.visibility = if (isListEmpty) View.VISIBLE else View.GONE

                if (isListEmpty) {
                    val isSearching = binding.searchEditText.text.isNotEmpty()
                    binding.emptyStateView.text = if (isSearching) "没有找到相关内容" else "还没有笔记，快来添加吧"
                }
            }
        }
    }

    private fun setupSearchListener() {
        // 1. 监听文本变化，只用于切换“历史记录”和“智能推荐”的UI状态
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                viewModel.searchNotes(query, currentSelectedTagId) // 主列表实时搜索

                if (query.isBlank()) {
                    // 输入为空: 显示历史记录视图, 隐藏智能推荐视图
                    binding.searchSuggestionsRecyclerview.visibility = View.GONE
                    updateSearchHistoryView()
                } else {
                    // 【核心修复】有输入: 隐藏历史记录视图, 并立即尝试显示智能推荐列表
                    binding.searchHistoryContainer.visibility = View.GONE

                    // 主动将会被建议列表设置为可见，即使此时它可能还是空的。
                    // 这样可以确保它已经准备好接收即将到来的数据。
                    binding.searchSuggestionsRecyclerview.visibility = View.VISIBLE

                    viewModel.loadSuggestions(query) // 在后台加载数据填充它
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 2. 监听焦点变化，用于控制浮层的整体显示与隐藏
        binding.searchEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // 获得焦点时，如果输入框为空，就显示历史记录
                if (binding.searchEditText.text.isBlank()) {
                    updateSearchHistoryView()
                } else {
                    // 如果获得焦点时已经有内容，也确保建议列表是可见的
                    binding.searchSuggestionsRecyclerview.visibility = View.VISIBLE
                    viewModel.loadSuggestions(binding.searchEditText.text.toString())
                }
            } else {
                // 失去焦点时，隐藏所有浮层
                binding.searchHistoryContainer.visibility = View.GONE
                binding.searchSuggestionsRecyclerview.visibility = View.GONE
            }
        }

        // 3. 监听软键盘的“搜索”或“完成”按钮，用于保存历史
        binding.searchEditText.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val query = v.text.toString()
                if (query.isNotBlank()) {
                    viewModel.saveSearchToHistory(query)
                }
                binding.searchEditText.clearFocus() // 隐藏键盘和浮层
                true
            } else {
                false
            }
        }

        // 4. 观察智能推荐的数据，只用于更新智能推荐UI
        lifecycleScope.launch {
            viewModel.suggestions.collect { suggestionList ->
                searchSuggestionAdapter.submitList(suggestionList)
                // 【优化】当数据加载回来后，如果列表是空的，我们再把它隐藏掉
                if (binding.searchEditText.hasFocus() && binding.searchEditText.text.isNotBlank()) {
                    if (suggestionList.isEmpty()) {
                        binding.searchSuggestionsRecyclerview.visibility = View.GONE
                    }
                }
            }
        }

    }



    // 辅助函数，用于显示和填充新的历史记录框
    private fun updateSearchHistoryView() {
        // getSearchHistory() 现在返回的是一个有序列表，最新的在最前面
        val historyList = viewModel.getSearchHistory().take(3)
        val historyContainer = binding.searchHistoryContainer

        historyContainer.removeAllViews()

        if (historyList.isNotEmpty() && binding.searchEditText.text.isBlank()) {
            historyContainer.visibility = View.VISIBLE
            for (historyText in historyList) {
                // ... 动态创建和添加 chipView 的逻辑保持不变 ...
                val chipView = layoutInflater.inflate(R.layout.item_history_box, historyContainer, false)
                val historyTextView = chipView.findViewById<TextView>(R.id.history_text)
                val deleteButton = chipView.findViewById<ImageView>(R.id.button_delete_history_item)

                historyTextView.text = historyText
                chipView.setOnClickListener {
                    binding.searchEditText.setText(historyText)
                    binding.searchEditText.setSelection(historyText.length)
                }
                deleteButton.setOnClickListener {
                    viewModel.deleteSearchFromHistory(historyText)
                    updateSearchHistoryView() // 删除后立即刷新
                }
                historyContainer.addView(chipView)
            }
        } else {
            historyContainer.visibility = View.GONE
        }
    }

    private fun loadTags() {
        lifecycleScope.launch {
            tagsViewModel.tags.collect { tags ->
                binding.tagsContainer.removeAllViews()
                tagViews.clear()

                fun addTagView(id: Long, name: String) {
                    val tagView = layoutInflater.inflate(R.layout.item_tag, binding.tagsContainer, false) as TextView
                    tagView.text = name
                    tagView.setOnClickListener {
                        val newTagId = if (currentSelectedTagId == id && id != 0L) 0L else id
                        currentSelectedTagId = newTagId
                        updateTagSelectionState()
                        val query = binding.searchEditText.text.toString()
                        if (query.isNotEmpty()) {
                            viewModel.searchNotes(query, currentSelectedTagId)
                        } else {
                            if (currentSelectedTagId == 0L) viewModel.loadNotes() else viewModel.loadNotesByTag(currentSelectedTagId)
                        }
                    }
                    binding.tagsContainer.addView(tagView)
                    if (id == 0L) allTagView = tagView else tagViews[id] = tagView
                }

                addTagView(0L, "全部")
                tags.forEach { tag -> addTagView(tag.tagId, tag.name) }
                updateTagSelectionState()
            }
        }
    }

    private fun updateTagSelectionState() {
        (tagViews.values + listOfNotNull(allTagView)).forEach { view ->
            val defaultBgColor = ContextCompat.getColor(view.context, R.color.filter_bar_tag_bg_default)
            val defaultTextColor = ContextCompat.getColor(view.context, R.color.filter_bar_tag_text_default)
            view.backgroundTintList = ColorStateList.valueOf(defaultBgColor)
            view.setTextColor(defaultTextColor)
            view.setTypeface(null, Typeface.NORMAL)
        }
        val selectedView = if (currentSelectedTagId == 0L) allTagView else tagViews[currentSelectedTagId]
        selectedView?.let { view ->
            val selectedBgColor = ContextCompat.getColor(view.context, R.color.filter_bar_tag_bg_selected)
            val selectedTextColor = ContextCompat.getColor(view.context, R.color.filter_bar_tag_text_selected)
            view.backgroundTintList = ColorStateList.valueOf(selectedBgColor)
            view.setTextColor(selectedTextColor)
            view.setTypeface(view.typeface, Typeface.BOLD)
        }
    }

    private fun setupButtonListeners() {
        binding.addNoteFab.setOnClickListener {
            val intent = Intent(this, NoteEditActivity::class.java)
            if (currentSelectedTagId > 0) intent.putExtra("preSelectedTagId", currentSelectedTagId)
            startActivity(intent)
        }
        binding.errorStateView.setOnClickListener {
            noteAdapter.retry()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.notesPagingFlow.collectLatest { pagingData ->
                noteAdapter.submitData(lifecycle, pagingData)
            }
        }

        lifecycleScope.launch {
            viewModel.error.collect { errorMessage ->
                binding.errorStateView.text = errorMessage ?: getString(R.string.loading_failed)
                if (errorMessage != null) {
                    binding.errorStateView.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            override fun onMove(r: RecyclerView, v: RecyclerView.ViewHolder, t: RecyclerView.ViewHolder): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                val noteToDelete = noteAdapter.getNoteAtPosition(position)

                noteToDelete?.let { note ->
                    viewModel.deleteNote(note.noteId)
                    Snackbar.make(binding.coordinatorLayout, "笔记已删除", Snackbar.LENGTH_LONG)
                        .setAnchorView(binding.addNoteFab)
                        .setAction("撤销") { viewModel.saveNote(note) }
                        .show()
                }
            }

            override fun onChildDraw(c: Canvas, r: RecyclerView, v: RecyclerView.ViewHolder, dX: Float, dY: Float, action: Int, active: Boolean) {
                val itemView = v.itemView
                val icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_delete)
                val background = ColorDrawable(Color.RED)
                background.setBounds(itemView.right + dX.toInt(), itemView.top, itemView.right, itemView.bottom)
                background.draw(c)
                val iconMargin = (itemView.height - icon!!.intrinsicHeight) / 2
                val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
                val iconBottom = iconTop + icon.intrinsicHeight
                val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
                val iconRight = itemView.right - iconMargin
                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                icon.draw(c)
                super.onChildDraw(c, r, v, dX, dY, action, active)
            }
        }
        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.notesRecyclerView)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun setupOnBackPressed() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }
}
