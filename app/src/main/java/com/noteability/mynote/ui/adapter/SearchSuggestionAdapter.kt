// C:/Users/USTC/Desktop/project/app/src/main/java/com/noteability/mynote/ui/adapter/SearchSuggestionAdapter.kt

package com.noteability.mynote.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.noteability.mynote.R
import com.noteability.mynote.databinding.ItemSearchSuggestionBinding
import com.noteability.mynote.databinding.ItemNoMatchBinding // 【新增】导入新的 Binding

// 【新增】定义两种视图类型
private const val VIEW_TYPE_SUGGESTION = 1
private const val VIEW_TYPE_NO_MATCH = 2

class SearchSuggestionAdapter(
    private val onSuggestionClick: (String) -> Unit,
    private val onSuggestionDelete: (String) -> Unit
) : ListAdapter<SearchSuggestion, RecyclerView.ViewHolder>(SuggestionDiffCallback()) { // 【修改】ViewHolder 类型为通用类型

    // 【新增】重写 getItemViewType 方法
    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).type == SearchSuggestionType.NO_MATCH) {
            VIEW_TYPE_NO_MATCH
        } else {
            VIEW_TYPE_SUGGESTION
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder { // 【修改】返回值类型
        val inflater = LayoutInflater.from(parent.context)
        // 【新增】根据视图类型加载不同的布局
        return when (viewType) {
            VIEW_TYPE_NO_MATCH -> {
                val binding = ItemNoMatchBinding.inflate(inflater, parent, false)
                NoMatchViewHolder(binding)
            }
            else -> { // VIEW_TYPE_SUGGESTION
                val binding = ItemSearchSuggestionBinding.inflate(inflater, parent, false)
                SuggestionViewHolder(binding)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) { // 【修改】holder 类型
        // 【新增】根据 ViewHolder 的类型进行绑定
        when (holder) {
            is SuggestionViewHolder -> holder.bind(getItem(position))
            is NoMatchViewHolder -> holder.bind() // 无匹配项不需要数据
        }
    }

    // 【内部类 SuggestionViewHolder 保持不变】
    inner class SuggestionViewHolder(private val binding: ItemSearchSuggestionBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(suggestion: SearchSuggestion) {
            binding.suggestionTextView.text = suggestion.text
            when (suggestion.type) {
                SearchSuggestionType.HISTORY -> {
                    binding.imageSuggestionIcon.setImageResource(R.drawable.ic_history)
                    binding.buttonDelete.visibility = View.VISIBLE
                }
                SearchSuggestionType.SUGGESTION -> {
                    binding.imageSuggestionIcon.setImageResource(R.drawable.ic_search)
                    binding.buttonDelete.visibility = View.GONE
                }
                else -> { /* For NO_MATCH or others, hide everything */ }
            }
            binding.root.setOnClickListener {
                onSuggestionClick(suggestion.text)
            }
            binding.buttonDelete.setOnClickListener {
                onSuggestionDelete(suggestion.text)
            }
        }
    }

    // 【新增】为“无匹配”提示创建的 ViewHolder
    inner class NoMatchViewHolder(binding: ItemNoMatchBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind() {
            // 这个 ViewHolder 非常简单，它的布局文件里已经写好了“无匹配”
            // 我们还可以在这里设置它不可点击
            itemView.isClickable = false
        }
    }

    // 【DiffCallback 保持不变】
    class SuggestionDiffCallback : DiffUtil.ItemCallback<SearchSuggestion>() {
        override fun areItemsTheSame(oldItem: SearchSuggestion, newItem: SearchSuggestion): Boolean {
            return oldItem.text == newItem.text && oldItem.type == newItem.type
        }

        override fun areContentsTheSame(oldItem: SearchSuggestion, newItem: SearchSuggestion): Boolean {
            return oldItem == newItem
        }
    }
}
