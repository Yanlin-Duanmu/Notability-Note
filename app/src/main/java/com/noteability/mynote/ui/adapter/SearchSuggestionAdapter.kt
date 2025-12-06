package com.noteability.mynote.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.semantics.text
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.noteability.mynote.R
import com.noteability.mynote.databinding.ItemSearchSuggestionBinding

class SearchSuggestionAdapter(
    private val onSuggestionClick: (String) -> Unit,
    private val onSuggestionDelete: (String) -> Unit // 新增的回调，用于删除历史
) : ListAdapter<SearchSuggestion, SearchSuggestionAdapter.SuggestionViewHolder>(SuggestionDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SuggestionViewHolder {
        val binding = ItemSearchSuggestionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SuggestionViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SuggestionViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class SuggestionViewHolder(private val binding: ItemSearchSuggestionBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(suggestion: SearchSuggestion) {
            binding.suggestionTextView.text = suggestion.text

            // 根据类型设置图标和删除按钮的可见性
            when (suggestion.type) {
                SearchSuggestionType.HISTORY -> {
                    binding.imageSuggestionIcon.setImageResource(R.drawable.ic_history)
                    binding.buttonDelete.visibility = View.VISIBLE
                }
                SearchSuggestionType.SUGGESTION -> {
                    binding.imageSuggestionIcon.setImageResource(R.drawable.ic_search)
                    binding.buttonDelete.visibility = View.GONE
                }
            }

            // 整个项目行的点击事件
            binding.root.setOnClickListener {
                onSuggestionClick(suggestion.text)
            }

            // 删除按钮的点击事件
            binding.buttonDelete.setOnClickListener {
                onSuggestionDelete(suggestion.text)
            }
        }
    }

    class SuggestionDiffCallback : DiffUtil.ItemCallback<SearchSuggestion>() {
        override fun areItemsTheSame(oldItem: SearchSuggestion, newItem: SearchSuggestion): Boolean {
            return oldItem.text == newItem.text && oldItem.type == newItem.type
        }

        override fun areContentsTheSame(oldItem: SearchSuggestion, newItem: SearchSuggestion): Boolean {
            return oldItem == newItem
        }
    }
}
