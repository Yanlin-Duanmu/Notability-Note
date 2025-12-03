package com.noteability.mynote

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.progressindicator.CircularProgressIndicator
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import android.content.Context
import android.app.AlertDialog
import com.noteability.mynote.data.entity.Tag
import com.noteability.mynote.data.repository.impl.TagRepositoryImpl
import com.noteability.mynote.ui.viewmodel.TagsViewModel

class TagManagementActivity : AppCompatActivity() {

    private lateinit var tagsRecyclerView: RecyclerView
    private lateinit var searchEditText: EditText
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var tagAdapter: TagAdapter
    private lateinit var addTagButton: FloatingActionButton
    private lateinit var loadingIndicator: CircularProgressIndicator
    private lateinit var errorTextView: TextView
    private lateinit var emptyStateTextView: TextView

    // 标签颜色
    private val tagColors = arrayListOf(
        R.color.tag_color_1,
        R.color.tag_color_2,
        R.color.tag_color_3,
        R.color.tag_color_4,
        R.color.tag_color_5
    )
    
    // 为TagsViewModel创建工厂类
    private class TagsViewModelFactory(private val applicationContext: Context) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TagsViewModel::class.java)) {
                // 创建带Context的TagRepositoryImpl实例
                val repository = TagRepositoryImpl(applicationContext)
                val viewModel = TagsViewModel(repository)
                return viewModel as? T ?: throw IllegalArgumentException("Cannot create ViewModel for class: $modelClass")
            }
            throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
        }
    }
    
    // 使用自定义工厂类获取TagsViewModel实例
    private val tagsViewModel: TagsViewModel by viewModels { TagsViewModelFactory(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tag_management)

        // 初始化界面组件
        tagsRecyclerView = findViewById(R.id.tagsRecyclerView)
        searchEditText = findViewById(R.id.searchEditText)
        bottomNavigationView = findViewById(R.id.bottomNavigationView)
        addTagButton = findViewById(R.id.addTagButton)
        loadingIndicator = findViewById(R.id.loadingIndicator)
        errorTextView = findViewById(R.id.errorTextView)
        emptyStateTextView = findViewById(R.id.emptyStateTextView)

        // 初始化RecyclerView
        setupRecyclerView()

        // 设置搜索监听器
        setupSearchListener()

        // 设置按钮点击事件
        setupButtonListeners()

        // 设置底部导航栏
        setupBottomNavigation()
        
        // 获取当前登录用户ID并设置到ViewModel
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val loggedInUserId = sharedPreferences.getLong("logged_in_user_id", 0L)
        if (loggedInUserId > 0) {
            tagsViewModel.setLoggedInUserId(loggedInUserId)
        }
        
        // 观察ViewModel中的数据变化
        observeViewModel()
    }

    private fun setupRecyclerView() {
        tagAdapter = TagAdapter(emptyList(), tagColors) { tag ->
            // 处理标签点击事件
            val intent = Intent(this, MainActivity::class.java)
            intent.putExtra("selectedTagId", tag.tagId)
            startActivity(intent)
            finish() // 完成后返回主页面
        }

        tagsRecyclerView.layoutManager = LinearLayoutManager(this)
        tagsRecyclerView.adapter = tagAdapter
    }
    
    private fun observeViewModel() {
        // 创建NoteRepository实例来获取笔记数量
        val noteRepository = com.noteability.mynote.data.repository.impl.NoteRepositoryImpl(applicationContext)
        // 确保设置正确的用户ID到NoteRepository
        val sharedPreferences = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val loggedInUserId = sharedPreferences.getLong("logged_in_user_id", 0L)
        if (loggedInUserId > 0) {
            noteRepository.updateCurrentUserId(loggedInUserId)
        }
        
        // 观察标签列表变化
        lifecycleScope.launch {
            tagsViewModel.tags.collect { tags ->
                // 为每个标签更新真实的笔记数量
                lifecycleScope.launch(Dispatchers.IO) {
                    val updatedTags = tags.map { tag ->
                        // 获取该标签下的真实笔记数量
                        val realNoteCount = noteRepository.getNoteCountByTag(tag.tagId)
                        // 创建一个更新了笔记数量的新标签对象
                        tag.copy(noteCount = realNoteCount)
                    }
                    
                    // 在主线程更新UI
                    launch(Dispatchers.Main) {
                        // 更新适配器数据
                        tagAdapter.updateTags(updatedTags)
                        
                        // 显示或隐藏空状态
                        if (updatedTags.isEmpty()) {
                            emptyStateTextView.visibility = View.VISIBLE
                            tagsRecyclerView.visibility = View.GONE
                        } else {
                            emptyStateTextView.visibility = View.GONE
                            tagsRecyclerView.visibility = View.VISIBLE
                        }
                    }
                }
            }
        }
        
        // 观察加载状态
        lifecycleScope.launch {
            tagsViewModel.isLoading.collect { isLoading ->
                loadingIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
                tagsRecyclerView.visibility = if (isLoading) View.GONE else View.VISIBLE
            }
        }
        
        // 观察错误状态
        lifecycleScope.launch {
            tagsViewModel.error.collect { errorMessage ->
                if (errorMessage != null) {
                    errorTextView.text = errorMessage
                    errorTextView.visibility = View.VISIBLE
                } else {
                    errorTextView.visibility = View.GONE
                }
            }
        }
    }

    private fun setupSearchListener() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // 处理搜索逻辑
                filterTagsBySearch(s.toString())
            }

            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupButtonListeners() {
        addTagButton.setOnClickListener {
            // 这里可以实现新增标签的对话框
            showAddTagDialog()
        }
        
        errorTextView.setOnClickListener {
            // 点击错误提示重试加载
            tagsViewModel.loadTags()
        }
    }
    
    private fun showAddTagDialog() {
        // 使用AlertDialog让用户输入标签名称
        val builder = AlertDialog.Builder(this)
        builder.setTitle("新建标签")
        
        // 创建一个输入框
        val input = EditText(this)
        input.hint = "请输入标签名称"
        builder.setView(input)
        
        // 设置确定按钮
        builder.setPositiveButton("确定") { dialog, which ->
            val tagName = input.text.toString().trim()
            if (tagName.isNotEmpty()) {
                tagsViewModel.createTag(tagName)
                showToast("标签 '$tagName' 创建成功")
            } else {
                showToast("标签名称不能为空")
            }
        }
        
        // 设置取消按钮
        builder.setNegativeButton("取消") { dialog, which ->
            dialog.dismiss()
        }
        
        // 显示对话框
        builder.show()
    }

    private fun setupBottomNavigation() {
        // 设置当前选中的导航项为标签
        bottomNavigationView.selectedItemId = R.id.nav_tags
        
        bottomNavigationView.setOnNavigationItemSelectedListener {
            when (it.itemId) {
                R.id.nav_notes -> {
                    // 跳转到笔记页面
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                    true
                }
                R.id.nav_tags -> {
                    // 已经在标签页面，无需操作
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

    private fun filterTagsBySearch(query: String) {
        // 使用ViewModel进行搜索
        tagsViewModel.searchTags(query)
    }

    private fun showToast(message: String) {
        android.widget.Toast.makeText(this, message, android.widget.Toast.LENGTH_SHORT).show()
    }

    // 标签适配器
    inner class TagAdapter(
        private var tags: List<Tag>,
        private val colors: ArrayList<Int>,
        private val onTagClickListener: (Tag) -> Unit
    ) : RecyclerView.Adapter<TagAdapter.TagViewHolder>() {

        fun updateTags(newTags: List<Tag>) {
            this.tags = newTags
            notifyDataSetChanged()
        }

        inner class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val tagColorDot: View = itemView.findViewById(R.id.tagColorDot)
            val tagName: TextView = itemView.findViewById(R.id.tagName)
            val noteCount: TextView = itemView.findViewById(R.id.noteCount)

            fun bind(tag: Tag, position: Int) {
                // 使用位置取模来循环使用颜色
                val colorIndex = position % colors.size
                tagColorDot.setBackgroundColor(ContextCompat.getColor(itemView.context, colors[colorIndex]))
                tagName.text = tag.name
                noteCount.text = "${tag.noteCount}篇笔记"

                itemView.setOnClickListener {
                    onTagClickListener(tag)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
            val view = layoutInflater.inflate(R.layout.item_tag_management, parent, false)
            return TagViewHolder(view)
        }

        override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
            holder.bind(tags[position], position)
        }

        override fun getItemCount(): Int = tags.size
    }
}