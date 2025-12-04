package com.noteability.mynote

import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.noteability.mynote.databinding.ActivityMainBinding
import com.noteability.mynote.di.ServiceLocator
import com.noteability.mynote.ui.adapter.NoteAdapter
import com.noteability.mynote.ui.viewmodel.NotesViewModel
import com.noteability.mynote.ui.viewmodel.TagsViewModel
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var toggle: ActionBarDrawerToggle
    private lateinit var noteAdapter: NoteAdapter
    private var allTagView: TextView? = null
    private val tagViews = mutableMapOf<Long, TextView>()

    // ViewModel实例
    private lateinit var viewModel: NotesViewModel
    private lateinit var tagsViewModel: TagsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 检查用户登录状态
        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val loggedInUserId = sharedPreferences.getLong("logged_in_user_id", 0L)

        // 如果用户未登录，跳转到登录页面
        if (loggedInUserId <= 0) {
            startActivity(
                Intent(
                    this,
                    com.noteability.mynote.ui.activity.LoginActivity::class.java
                )
            )
            finish()
            return
        }

        // 初始化ViewModels
        initViewModels(loggedInUserId)

        setupToolbarAndDrawer()

        // 初始化RecyclerView
        setupRecyclerView()
        setupSwipeToDelete()

        // 加载标签
        loadTags()

        // 设置搜索监听器
        setupSearchListener()

        // 设置按钮点击事件
        setupButtonListeners()

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

        setupOnBackPressed()
    }

    private fun initViewModels(loggedInUserId: Long) {
        // 从ServiceLocator获取仓库
        val noteRepository = ServiceLocator.provideNoteRepository()
        val tagRepository = ServiceLocator.provideTagRepository()

        // 初始化ViewModels
        viewModel = NotesViewModel(noteRepository)
        tagsViewModel = TagsViewModel(tagRepository)

        // 设置当前登录用户ID
        viewModel.setLoggedInUserId(loggedInUserId)
        tagsViewModel.setLoggedInUserId(loggedInUserId)
    }

    private fun setupToolbarAndDrawer() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,    // DrawerLayout 实例
            binding.toolbar,         // Toolbar 实例
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )

        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        setupNavigationDrawerListener()
    }

    private fun setupNavigationDrawerListener() {
        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_notes -> {
                    if (currentSelectedTagId > 0) {
                        currentSelectedTagId = 0L
                        viewModel.loadNotes()
                        showToast("显示所有笔记")
                        updateTagSelectionState()
                    }
                    // true
                }

                R.id.nav_tags -> {
                    val intent = Intent(this, TagManagementActivity::class.java)
                    startActivity(intent)
                    // true
                }

                R.id.nav_settings -> {
                    showToast("跳转到设置页面")
                    // true
                }
            }

            binding.drawerLayout.closeDrawer(GravityCompat.START)

            true
        }
    }

    private fun setupRecyclerView() {
        val tagNameMap = mutableMapOf<Long, String>()

        // 先创建适配器，初始为空的标签映射
        noteAdapter = NoteAdapter(emptyList(), onNoteClick = { note ->
            // 处理笔记点击事件，跳转到笔记编辑页面
            val intent = Intent(this, NoteEditActivity::class.java)
            intent.putExtra("noteId", note.noteId)
            startActivity(intent)
        }, tagNameMap = tagNameMap)

        // 然后再设置标签数据收集
        lifecycleScope.launch {
            tagsViewModel.tags.collect { tagsList ->
                tagNameMap.clear()
                tagsList.forEach { tag ->
                    tagNameMap[tag.tagId] = tag.name
                }
                // 更新适配器中的标签映射
                noteAdapter.updateTagNameMap(tagNameMap)
            }
        }

        binding.notesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.notesRecyclerView.adapter = noteAdapter
    }

    private fun loadTags() {
        lifecycleScope.launch {
            tagsViewModel.tags.collect { tags ->
                // CLear views
                binding.tagsContainer.removeAllViews()
                tagViews.clear()

                // General function
                fun addTagView(id: Long, name: String) {
                    val tagView = layoutInflater.inflate(R.layout.item_tag, binding.tagsContainer, false) as TextView

                    tagView.text = name
                    tagView.setOnClickListener {
                        binding.searchEditText.text.clear()

                        if (currentSelectedTagId == id && id != 0L) {
                            currentSelectedTagId = 0L
                            viewModel.loadNotes()
                        } else {
                            currentSelectedTagId = id
                            if (id == 0L) viewModel.loadNotes() else viewModel.loadNotesByTag(id)
                        }
                        updateTagSelectionState()
                    }

                    // Add to binding
                    binding.tagsContainer.addView(tagView)
                    if (id == 0L) allTagView = tagView else tagViews[id] = tagView
                }

                addTagView(0L, "全部")
                tags.forEach { tag -> addTagView(tag.tagId, tag.name) }

                // Refresh state
                updateTagSelectionState()
            }
        }
    }

    private fun updateTagSelectionState() {
        // 重置所有标签的样式
        (tagViews.values + listOfNotNull(allTagView)).forEach { view ->
            view.setBackgroundResource(R.drawable.tag_unselected_background)
            view.setTextColor(ContextCompat.getColor(this, R.color.tag_unselected_text_color))
        }

        // 设置选中标签的样式
        val selectedView =
            if (currentSelectedTagId == 0L) allTagView else tagViews[currentSelectedTagId]
        selectedView?.setBackgroundResource(R.drawable.tag_selected_background)
        selectedView?.setTextColor(Color.WHITE)
    }

    private fun setupSearchListener() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
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
        binding.addNoteFab.setOnClickListener {
            // 跳转到新建笔记页面
            val intent = Intent(this, NoteEditActivity::class.java)
            // 如果当前处于标签筛选模式，传递标签ID给编辑页面
            if (currentSelectedTagId > 0) {
                intent.putExtra("preSelectedTagId", currentSelectedTagId)
            }
            startActivity(intent)
        }

        binding.errorStateView.setOnClickListener {
            // 重试加载
            viewModel.loadNotes()
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            // 观察笔记列表
            viewModel.notes.collect { notes ->
                noteAdapter.updateNotes(notes)
                // 检查搜索框内容，以决定“空状态”的提示文本
                val isSearching = binding.searchEditText.text.isNotEmpty()
                updateUIState(notes.isEmpty(), isSearching)
            }
        }

        lifecycleScope.launch {
            // 观察加载状态
            viewModel.isLoading.collect { isLoading ->
                binding.loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
            }
        }

        lifecycleScope.launch {
            // 观察错误状态
            viewModel.error.collect { errorMessage ->
                binding.errorStateView.text = errorMessage ?: getString(R.string.loading_failed)
                binding.errorStateView.visibility =
                    if (errorMessage != null) View.VISIBLE else View.GONE
            }
        }
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

                Snackbar.make(binding.coordinatorLayout, "笔记已删除", Snackbar.LENGTH_LONG)
                    .setAnchorView(binding.addNoteFab)
                    .setAction("撤销") {
                        viewModel.saveNote(noteToDelete)
                    }
                    .show()
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val icon = ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_delete)
                val background = ColorDrawable(Color.RED)

                // Set the background color
                background.setBounds(
                    itemView.right + dX.toInt(),
                    itemView.top,
                    itemView.right,
                    itemView.bottom
                )
                background.draw(c)

                // Set the icon
                val iconMargin = (itemView.height - icon!!.intrinsicHeight) / 2
                val iconTop = itemView.top + (itemView.height - icon.intrinsicHeight) / 2
                val iconBottom = iconTop + icon.intrinsicHeight
                val iconLeft = itemView.right - iconMargin - icon.intrinsicWidth
                val iconRight = itemView.right - iconMargin

                icon.setBounds(iconLeft, iconTop, iconRight, iconBottom)
                icon.draw(c)

                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }
        }

        val itemTouchHelper = ItemTouchHelper(itemTouchHelperCallback)
        itemTouchHelper.attachToRecyclerView(binding.notesRecyclerView)
    }

    private fun updateUIState(isEmptyList: Boolean, isSearching: Boolean) {
        if (isEmptyList && !viewModel.isLoading.value) {
            binding.emptyStateView.visibility = View.VISIBLE
            binding.notesRecyclerView.visibility = View.GONE
            // 根据是否在搜索中，显示不同的提示文本
            if (isSearching) {
                binding.emptyStateView.text = "没有找到相关内容" // R.string.no_search_results_found
            } else {
                binding.emptyStateView.text = "还没有笔记，快来添加吧" // R.string.no_notes_yet
            }
        } else {
            binding.emptyStateView.visibility = View.GONE
            binding.notesRecyclerView.visibility = View.VISIBLE
        }
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun setupOnBackPressed() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
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
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onResume() {
        super.onResume()
        // 从其他Activity返回时，根据当前选中的标签或搜索框状态重新加载笔记
        val searchQuery = binding.searchEditText.text.toString()
        if (searchQuery.isNotEmpty()) {
            viewModel.searchNotes(searchQuery)
        } else if (currentSelectedTagId > 0) {
            viewModel.loadNotesByTag(currentSelectedTagId)
        } else {
            viewModel.loadNotes()
        }
    }
}