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
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
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
import kotlinx.coroutines.flow.collectLatest // [新增] 用于收集最新的 Paging 数据
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var noteAdapter: NoteAdapter
    private lateinit var searchSuggestionAdapter: SearchSuggestionAdapter
    private var allTagView: TextView? = null
    private val tagViews = mutableMapOf<Long, TextView>()

    // ViewModel实例
    private lateinit var viewModel: NotesViewModel
    private lateinit var tagsViewModel: TagsViewModel

    // 存储当前选中的标签ID，默认为0表示未筛选标签
    private var currentSelectedTagId: Long = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)



        // 检查用户登录状态
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val loggedInUserId = sharedPreferences.getLong("logged_in_user_id", 0L)

        if (loggedInUserId <= 0) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        initViewModels(loggedInUserId)
        setupToolbarAndDrawer()

        // 初始化RecyclerView
        setupRecyclerView()

        // 加载状态监听 (替代原来的 updateUIState)
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

        // 1. Adapter 初始化
        noteAdapter = NoteAdapter(onNoteClick = { note ->
            val intent = Intent(this, NoteEditActivity::class.java)
            intent.putExtra("noteId", note.noteId)
            startActivity(intent)
        }, tagNameMap = tagNameMap)

        // 搜索建议 Adapter
        searchSuggestionAdapter = SearchSuggestionAdapter(
            onSuggestionClick = { query ->
                binding.searchEditText.setText(query)
                binding.searchEditText.setSelection(query.length)
                binding.searchSuggestionsRecyclerview.visibility = View.GONE
            },
            onSuggestionDelete = { query ->
                viewModel.deleteSearchFromHistory(query)
            }
        )

        lifecycleScope.launch {
            tagsViewModel.tags.collect { tagsList ->
                tagNameMap.clear()
                tagsList.forEach { tag -> tagNameMap[tag.tagId] = tag.name }
                noteAdapter.updateTagNameMap(tagNameMap)
            }
        }

        val layoutManager = LinearLayoutManager(this)
        binding.notesRecyclerView.layoutManager = layoutManager
        binding.notesRecyclerView.adapter = noteAdapter
        binding.searchSuggestionsRecyclerview.adapter = searchSuggestionAdapter

        binding.notesRecyclerView.itemAnimator = null
    }


    //监听 Paging 的加载状态，控制 Loading 和 空视图
    private fun setupLoadStateListener() {
        noteAdapter.addLoadStateListener { loadState ->
            val isListEmpty = loadState.refresh is LoadState.NotLoading && noteAdapter.itemCount == 0
            val isRefresh = loadState.refresh is LoadState.Loading

            binding.loadingIndicator.visibility = if (isRefresh) View.VISIBLE else View.GONE


            if (loadState.refresh is LoadState.NotLoading && noteAdapter.itemCount > 0) {
                // 使用 post 确保在 UI 绘制完成后执行
                binding.notesRecyclerView.post {
                    (binding.notesRecyclerView.layoutManager as LinearLayoutManager)
                        .scrollToPositionWithOffset(0, 0)
                }
            }

            if (isListEmpty) {
                binding.emptyStateView.visibility = View.VISIBLE
                binding.notesRecyclerView.visibility = View.GONE
                val isSearching = binding.searchEditText.text.isNotEmpty()
                binding.emptyStateView.text = if (isSearching) "没有找到相关内容" else "还没有笔记，快来添加吧"
            } else {
                binding.emptyStateView.visibility = View.GONE
                binding.notesRecyclerView.visibility = View.VISIBLE
            }
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

    private fun setupSearchListener() {
        lifecycleScope.launch {
            viewModel.suggestions.collect { suggestionList ->
                searchSuggestionAdapter.submitList(suggestionList)
                if (binding.searchEditText.hasFocus() && suggestionList.isNotEmpty()) {
                    binding.searchSuggestionsRecyclerview.visibility = View.VISIBLE
                } else {
                    binding.searchSuggestionsRecyclerview.visibility = View.GONE
                }
            }
        }
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                viewModel.searchNotes(query, currentSelectedTagId)
                viewModel.loadSuggestions(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
        binding.searchEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                viewModel.loadSuggestions(binding.searchEditText.text.toString())
            } else {
                binding.searchSuggestionsRecyclerview.visibility = View.GONE
                val query = binding.searchEditText.text.toString()
                if (query.isNotBlank()) viewModel.saveSearchToHistory(query)
            }
        }
    }

    private fun setupButtonListeners() {
        binding.addNoteFab.setOnClickListener {
            val intent = Intent(this, NoteEditActivity::class.java)
            if (currentSelectedTagId > 0) intent.putExtra("preSelectedTagId", currentSelectedTagId)
            startActivity(intent)
        }
        binding.errorStateView.setOnClickListener {
            // [修改] Paging 3 重试机制
            noteAdapter.retry()
        }
    }

    private fun observeViewModel() {

        lifecycleScope.launch {
            viewModel.notesPagingFlow.collectLatest { pagingData ->
                noteAdapter.submitData(lifecycle, pagingData)
            }
        }

        // 移除了 viewModel.isLoading 的观察，改用 setupLoadStateListener

        lifecycleScope.launch {
            viewModel.error.collect { errorMessage ->
                binding.errorStateView.text = errorMessage ?: getString(R.string.loading_failed)
                // 这里只处理业务逻辑错误，Paging 的加载错误由 loadStateListener 处理
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
                val position = viewHolder.bindingAdapterPosition // 推荐使用 bindingAdapterPosition

                // [修改] 使用 getNoteAtPosition (PagingAdapter 的方法)
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
                // ... (保持原有绘图代码不变)
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

    //移除了原来的 updateUIState 方法，被 setupLoadStateListener 替代

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