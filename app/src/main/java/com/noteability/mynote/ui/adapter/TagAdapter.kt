package com.noteability.mynote.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.noteability.mynote.R
import com.noteability.mynote.data.entity.Tag

/**
 * 标签适配器，用于在标签管理页面中显示标签列表
 */
class TagAdapter(
    private val tags: List<Tag>,
    private val noteCounts: Map<Long, Int>,
    private val onTagClick: (Tag) -> Unit
) : RecyclerView.Adapter<TagAdapter.TagViewHolder>() {

    /**
     * 标签颜色列表，用于为不同标签显示不同颜色
     */
    private val tagColors = listOf(
        R.color.tag_color_1,
        R.color.tag_color_4,
        R.color.tag_color_5,
        R.color.tag_color_3,
        R.color.tag_color_6,
        R.color.tag_color_8,
        R.color.tag_color_2
    )

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TagViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_tag_management, parent, false)
        return TagViewHolder(view)
    }

    override fun onBindViewHolder(holder: TagViewHolder, position: Int) {
        val tag = tags[position]
        val noteCount = noteCounts[tag.tagId] ?: 0

        // 设置标签信息
        holder.tagNameTextView.text = tag.name
        holder.noteCountTextView.text = "${noteCount}篇笔记"

        // 设置标签颜色（基于标签ID的哈希值来选择颜色，确保一致性）
        val colorIndex = Math.abs(tag.tagId.toInt()) % tagColors.size
        holder.tagColorIndicator.setBackgroundResource(tagColors[colorIndex])

        // 设置点击事件
        holder.itemView.setOnClickListener {
            onTagClick(tag)
        }

        // 由于布局中没有moreButton，暂时移除相关代码
    }

    override fun getItemCount(): Int = tags.size

    /**
     * 标签视图持有者
     */
    class TagViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tagColorIndicator: View = itemView.findViewById(R.id.tagColorDot)
        val tagNameTextView: TextView = itemView.findViewById(R.id.tagName)
        val noteCountTextView: TextView = itemView.findViewById(R.id.noteCount)
    }
}