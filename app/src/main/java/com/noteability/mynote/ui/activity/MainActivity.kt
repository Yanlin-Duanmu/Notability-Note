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
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog // [新增] 弹窗需要
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu // [新增] 菜单需要
import androidx.core.content.ContextCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.paging.LoadState
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.noteability.mynote.R
import com.noteability.mynote.data.dao.SortOrder
import com.noteability.mynote.data.entity.Note
import com.noteability.mynote.databinding.ActivityMainBinding
import com.noteability.mynote.di.ServiceLocator
import com.noteability.mynote.ui.adapter.NoteAdapter
import com.noteability.mynote.ui.adapter.SearchSuggestion
import com.noteability.mynote.ui.adapter.SearchSuggestionAdapter
import com.noteability.mynote.ui.adapter.SearchSuggestionType
import com.noteability.mynote.ui.viewmodel.NotesViewModel
import com.noteability.mynote.ui.viewmodel.TagsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
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

    // 标记当前是否处于批量选择模式
    private var isSelectionMode = false

    //标记是否需要滚到顶部
    private var needScrollToTop = false

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
        setupClearSearchListeners()
        val selectedTagId = intent.getLongExtra("selectedTagId", 0L)
        if (selectedTagId > 0) {
            currentSelectedTagId = selectedTagId
            viewModel.loadNotesByTag(selectedTagId)
        }

        setupOnBackPressed()

//        injectFakeData()
    }


    // 临时代码：批量插入 10,000 条数据用于压力测试
//    private fun injectFakeData() {
//
//        val sharedPreferences = getSharedPreferences("user_prefs", MODE_PRIVATE)
//        val currentUserId = sharedPreferences.getLong("logged_in_user_id", 0L)
//        lifecycleScope.launch(Dispatchers.IO) {
//            val fakeNotes = mutableListOf<Note>()
//            for (i in 1..10000) {
//                fakeNotes.add(
//                    Note(
//                        userId = currentUserId,
//                        title = "压力测试笔记 $i",
//                        content = "这是第 $i 条测试数据，用来测试 Paging 3 在海量数据下的内存表现和加载速度..." +
//                                "为了增加内存占用，我可以把这段文字写得长一点...".repeat(10), // 重复几次增加体积
//                        updatedAt = System.currentTimeMillis() - i * 1000,
//                        tagId = 1L // 假设 0 是默认标签
//                    )
//                )
//                // 每 500 条插一次，防止一次性把内存撑爆
//                if (fakeNotes.size >= 500) {
//                    // 注意：你需要确保 NoteDao 有一个 @Insert insertAll(notes: List<Note>) 方法
//                    // 如果没有，去 Dao 里加一个 @Insert suspend fun insertAll(notes: List<Note>)
//                    try {
//                        val repository = ServiceLocator.provideNoteRepository()
//                        // 这里可能需要你临时在 Repository 开一个后门方法调用 Dao 的 insertAll
//                        // 或者直接用 dao.insertAll(fakeNotes)
//                        // 简单起见，你可以循环调用 saveNote (虽然慢点，但能用)
//                        fakeNotes.forEach { repository.saveNote(it) }
//                    } catch (e: Exception) { e.printStackTrace() }
//                    fakeNotes.clear()
//                    Log.d("Test", "已插入 $i 条数据")
//                }
//            }
//        }
//    }




    override fun onResume() {
        super.onResume()
        // 不再根据搜索框状态自动清除搜索，避免影响用户正常搜索结果
        // 用户可以通过点击"全部"或其他方式手动清除搜索
    }
    private fun setupClearSearchListeners() {
        // 我们不再使用 setOnClickListener，因为它会被全屏的 RecyclerView 拦截。
        // binding.coordinatorLayout.setOnClickListener { ... } // -> 移除这部分

        // 给根布局设置一个 Touch 监听器，这是更底层的事件。
        binding.root.setOnTouchListener { view, event ->
            // 当用户手指按下时 (MotionEvent.ACTION_DOWN)
            if (event.action == android.view.MotionEvent.ACTION_DOWN) {
                // 检查当前是否有焦点的是 EditText
                if (currentFocus is android.widget.EditText) {
                    // 如果是，则清除它的焦点
                    currentFocus?.clearFocus()
                } else {
                    // 如果当前焦点不在 EditText 上（说明已经处于搜索结果展示状态），
                    // 并且 ViewModel 中仍有搜索词，则清除搜索。
                    if (viewModel.searchQuery.value.isNotEmpty()) {
                        viewModel.clearSearch()
                        // 返回 true 表示我们已经处理了这个触摸事件，防止它继续传递。
                        return@setOnTouchListener true
                    }
                }
            }
            // 返回 false 表示我们不消费这个事件，让其他视图（如滑动 RecyclerView）可以正常响应。
            return@setOnTouchListener false
        }
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

        // [修改] NoteAdapter 初始化
        noteAdapter = NoteAdapter(onNoteClick = { note ->
            // 如果在选择模式下，Adapter 内部会处理选中逻辑，不会触发这个回调
            // 只有在非选择模式下，点击才会跳转
            val intent = Intent(this, NoteEditActivity::class.java)
            intent.putExtra("noteId", note.noteId)
            startActivity(intent)
        }, tagNameMap = tagNameMap)

        // [新增] 监听选中数量变化，更新标题
        noteAdapter.onSelectionCountChanged = { count ->
            if (isSelectionMode) {
                try {
                    binding.tvPageTitle.text = "已选择 $count 项"
                } catch (e: Exception) {
                    supportActionBar?.title = "已选择 $count 项"
                }
            }
        }

        searchSuggestionAdapter = SearchSuggestionAdapter(
            onSuggestionClick = { suggestion ->
                // 1. 保存触发建议列表的 *原始输入* 到历史记录 (此逻辑保持不变，是正确的)
                val originalQuery = binding.searchEditText.text.toString().trim()
                if (originalQuery.isNotBlank()) {
                    viewModel.saveSearchToHistory(originalQuery)
                }

                // 2. 【核心修改】使用建议的文本，在当前页面执行搜索
                val exactTitle = suggestion.text
                viewModel.searchNotesByExactTitle(exactTitle, currentSelectedTagId)


                // 3. 【优化体验】清除焦点，隐藏键盘和浮层
                binding.searchEditText.clearFocus()

                // 4. 【重要】不要将建议文本放回搜索框
                binding.searchEditText.setText("") // 确保搜索框在搜索后清空
            },
            onSuggestionDelete = { query ->
                viewModel.deleteSearchFromHistory(query)
                // 删除后刷新建议列表
                viewModel.loadSuggestions(binding.searchEditText.text.toString())
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

            // 判断条件：1. 刷新操作已完成 (NotLoading)  2. 刚才按了排序 (needScrollToTop)
            if (loadState.source.refresh is LoadState.NotLoading && needScrollToTop) {
                binding.notesRecyclerView.scrollToPosition(0) // 强制滚到第一行
                needScrollToTop = false // 重置标志位，防止下次普通刷新也乱滚
            }


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
                if (binding.searchEditText.hasFocus()) { // 只在有焦点时处理
                    if (query.isBlank()) {
                        // 【修改】输入为空时，只隐藏建议列表，不再主动显示历史记录
                        binding.searchSuggestionsRecyclerview.visibility = View.GONE
                    } else {
                        // 有输入时，隐藏历史记录，显示建议列表
                        binding.searchHistoryContainer.visibility = View.GONE
                        binding.searchSuggestionsRecyclerview.visibility = View.VISIBLE
                        viewModel.loadSuggestions(query)
                    }
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 2. 监听焦点变化，用于控制浮层的整体显示与隐藏
        binding.searchEditText.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                // 获得焦点时，根据输入框内容决定显示历史记录还是智能推荐
                if (binding.searchEditText.text.isBlank()) {
                    updateSearchHistoryView()
                } else {
                    binding.searchSuggestionsRecyclerview.visibility = View.VISIBLE
                    viewModel.loadSuggestions(binding.searchEditText.text.toString())
                }
            } else {
                // 失去焦点时，隐藏所有浮层（历史记录和智能推荐）
                binding.searchHistoryContainer.visibility = View.GONE
                binding.searchSuggestionsRecyclerview.visibility = View.GONE
                val query = binding.searchEditText.text.toString()
            }
        }

        // 3. 监听软键盘的“搜索”按钮
        binding.searchEditText.setOnEditorActionListener { v, actionId, _ ->
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH) {
                val query = v.text.toString()
                if (query.isNotBlank()) {
                    // 逻辑保持不变
                    // 【优化】当用户按键盘搜索时，也直接执行搜索
                    viewModel.saveSearchToHistory(query)
                    viewModel.searchNotes(query, currentSelectedTagId)
                    binding.searchEditText.clearFocus() // 清除焦点以隐藏键盘和浮层
                }
                true
            } else {
                false
            }
        }

        // 4. 观察智能推荐的数据
        lifecycleScope.launch {
            viewModel.suggestions.collect { suggestionList ->
                // 【修改这里的逻辑】
                // 只有当搜索框有焦点并且内容不为空时，才处理建议列表
                if (binding.searchEditText.hasFocus() && binding.searchEditText.text.isNotBlank()) {
                    if (suggestionList.isEmpty()) {
                        // 如果返回的列表为空，则构建一个只包含“无匹配”项的列表
                        val noMatchList = listOf(
                            SearchSuggestion(
                                "no_match_placeholder",
                                SearchSuggestionType.NO_MATCH
                            )
                        )
                        searchSuggestionAdapter.submitList(noMatchList)
                        binding.searchSuggestionsRecyclerview.visibility = View.VISIBLE
                    } else {
                        // 如果有结果，正常提交列表
                        searchSuggestionAdapter.submitList(suggestionList)
                        binding.searchSuggestionsRecyclerview.visibility = View.VISIBLE
                    }
                }
                // 如果不满足条件（如失去焦点），焦点监听器会负责隐藏列表，这里不需要额外处理
            }
        }
    }


    // 辅助函数，用于显示和填充新的历史记录框
    private fun updateSearchHistoryView() {
        val historyList = viewModel.getSearchHistory().take(3) // 最多取3条
        val historyContainer = binding.searchHistoryFlowLayout // 这是我们刚刚在XML里修改的LinearLayout

        // 1. 先清空所有旧的视图
        historyContainer.removeAllViews()

        if (historyList.isNotEmpty()) {
            // 2. 如果有历史记录，则显示整个历史记录区域
            binding.searchHistoryContainer.visibility = View.VISIBLE
            // 同时确保建议列表是隐藏的
            binding.searchSuggestionsRecyclerview.visibility = View.GONE

            // 3. 动态添加历史记录项
            historyList.forEach { historyText ->
                // 加载你的 item_history_box.xml 布局
                val historyBoxView = layoutInflater.inflate(R.layout.item_history_box, historyContainer, false)

                // 获取布局中的 TextView 和 ImageView
                val textView = historyBoxView.findViewById<TextView>(R.id.history_text)
                val deleteButton = historyBoxView.findViewById<ImageView>(R.id.button_delete_history_item)

                // 设置文本
                textView.text = historyText

                // 设置整个卡片的点击事件 -> 执行搜索
                historyBoxView.setOnClickListener {
                    viewModel.searchNotes(historyText, currentSelectedTagId)
                    binding.searchEditText.clearFocus() // 清除焦点，隐藏浮层
                    binding.searchEditText.setText("") // 搜索完成后清空搜索框
                }

                // 设置删除按钮的点击事件 -> 删除此条历史
                deleteButton.setOnClickListener {
                    viewModel.deleteSearchFromHistory(historyText)
                    updateSearchHistoryView() // 刷新历史记录视图
                }

                // 将创建好的视图添加到 LinearLayout 中
                historyContainer.addView(historyBoxView)
            }
            // 设置“清空所有历史”按钮的点击事件
            binding.buttonClearHistory.setOnClickListener {
                viewModel.clearSearchHistory()
                updateSearchHistoryView() // 刷新视图
            }

        } else {
            // 4. 如果没有历史记录，隐藏整个区域
            binding.searchHistoryContainer.visibility = View.GONE
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

        // [新增] 右上角菜单按钮点击事件
        // 请确保 xml 里右上角那个四方块按钮的 ID 是 btnMore
        try {
            binding.btnMore.setOnClickListener { view ->
                showMoreMenu(view)
            }
        } catch (e: Exception) {
            // 如果 ID 不对，请检查 activity_main.xml
        }

        //底部批量删除按钮点击事件
        try {
            binding.batchDeleteBar.setOnClickListener { performBatchDelete() }
            binding.btnBatchDelete.setOnClickListener { performBatchDelete() }

            val btnCancel = binding.btnCancelBatch
            btnCancel.setOnClickListener { exitSelectionMode() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    //  显示右上角弹出菜单
    private fun showMoreMenu(view: View) {
        val popup = PopupMenu(this, view)
        popup.menu.add(0, 1, 0, "批量删除")
        popup.menu.add(0, 2, 0, "排序方式") // ID 为 3

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                1 -> {
                    enterSelectionMode()
                    true
                }
                2 -> {
                    // 点击排序，调用弹窗方法
                    showSortDialog()
                    true
                }
                else -> false
            }
        }
        popup.show()
    }

    // [新增] 显示排序单选对话框
    private fun showSortDialog() {
        // 1. 定义显示给用户的选项 (顺序必须和下面的枚举对应)
        val options = arrayOf("编辑时间 (默认)", "创建时间", "标题")

        // 2. 定义对应的枚举值
        val sortOrders = arrayOf(
            SortOrder.EDIT_TIME_DESC,   // 对应 "编辑时间"
            SortOrder.CREATE_TIME_ASC,  // 对应 "创建时间"
            SortOrder.TITLE_ASC         // 对应 "标题"
        )

        // 3. 获取当前 ViewModel 里选中的排序，用于回显 (让弹窗知道哪个该打钩)
        val currentSort = viewModel.currentSortOrder
        val currentIdx = sortOrders.indexOf(currentSort)

        // 4. 创建并显示弹窗
        AlertDialog.Builder(this)
            .setTitle("排序方式")
            .setSingleChoiceItems(options, currentIdx) { dialog, which ->
                // 用户点击了第 'which' 项
                val selectedSort = sortOrders[which]

                // 通知 ViewModel 更新排序
                viewModel.updateSortOrder(selectedSort)
                //更新数据之后滚回顶部
                needScrollToTop = true

                // 提示用户并关闭弹窗
                showToast("已按 ${options[which]} 排序")
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 进入批量选择模式
    private fun enterSelectionMode() {
        isSelectionMode = true
        noteAdapter.setSelectionMode(true)

        // UI 变化：隐藏 FAB，显示删除栏，锁住侧边栏
        binding.addNoteFab.hide()
        binding.batchDeleteBar.visibility = View.VISIBLE
        binding.drawerLayout.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        // 隐藏搜索框，显示“已选X项”标题
        binding.searchEditText.visibility = View.GONE
        binding.tvPageTitle.visibility = View.VISIBLE
        binding.tvPageTitle.text = "已选择 0 项"
    }

    // [新增] 退出批量选择模式
    private fun exitSelectionMode() {
        isSelectionMode = false
        noteAdapter.setSelectionMode(false)

        // UI 恢复
        binding.addNoteFab.show()
        binding.batchDeleteBar.visibility = View.GONE
        binding.drawerLayout.setDrawerLockMode(androidx.drawerlayout.widget.DrawerLayout.LOCK_MODE_UNLOCKED)

        // 恢复搜索框和标题
        binding.searchEditText.visibility = View.VISIBLE
        binding.tvPageTitle.text = "我的笔记" // 恢复你的默认标题
    }

    //  执行批量删除
    private fun performBatchDelete() {
        val selectedIds = noteAdapter.getSelectedNoteIds()
        if (selectedIds.isEmpty()) {
            showToast("请先选择要删除的笔记")
            return
        }
        //删除之前备份数据用于撤销
        val notesBackup = noteAdapter.getSelectedNotes()

        AlertDialog.Builder(this)
            .setTitle("确认删除")
            .setMessage("确定要删除选中的 ${selectedIds.size} 条笔记吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteNotes(selectedIds)
                showToast("已删除 ${selectedIds.size} 条笔记")
                exitSelectionMode() // 删除后退出选择模式

                // 显示Snackbar提供撤销功能

                Snackbar.make(binding.root, "已删除 ${selectedIds.size} 条笔记", Snackbar.LENGTH_LONG)
                    .setAnchorView(binding.addNoteFab) // 防止挡住悬浮按钮 (如果有的话)
                    .setAction("撤销") {
                        // 5. 如果用户点了撤销，就把刚才备份的 notesBackup 存回去
                        viewModel.restoreNotes(notesBackup)
                        showToast("已恢复")
                    }
                    .show()
            }
            .setNegativeButton("取消", null)
            .show()
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
                // [修改] 如果处于批量选择模式，禁用滑动删除，防止冲突
                if (isSelectionMode) {
                    noteAdapter.notifyItemChanged(viewHolder.bindingAdapterPosition) // 恢复滑动的 Item
                    return
                }

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
                // [修改] 批量模式下不绘制红色背景
                if (isSelectionMode) {
                    super.onChildDraw(c, r, v, dX, dY, action, active)
                    return
                }
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
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    // 优先处理批量模式退出
                    isSelectionMode -> {
                        exitSelectionMode()
                    }

                    // 1. 如果侧边栏开着，先关侧边栏
                    binding.drawerLayout.isDrawerOpen(GravityCompat.START) -> {
                        binding.drawerLayout.closeDrawer(GravityCompat.START)
                    }

                    // 2. 如果搜索框有焦点，先清除焦点
                    binding.searchEditText.hasFocus() -> {
                        binding.searchEditText.clearFocus()
                    }

                    // 3. 如果当前处于搜索结果状态，则清除搜索状态
                    viewModel.searchQuery.value.isNotEmpty() -> {
                        viewModel.clearSearch()
                    }

                    // 4. 否则，执行默认的返回操作（退出应用）
                    else -> {
                        // 这里使用标准的退回逻辑，比直接 finish() 更安全
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        })
    }
}
