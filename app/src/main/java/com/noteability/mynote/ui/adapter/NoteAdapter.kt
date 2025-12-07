package com.noteability.mynote.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.noteability.mynote.R
import com.noteability.mynote.data.entity.Note
import java.text.SimpleDateFormat
import java.util.*


class NoteAdapter(
    private val onNoteClick: (Note) -> Unit,
    private var tagNameMap: Map<Long, String> = emptyMap()
) : PagingDataAdapter<Note, NoteAdapter.NoteViewHolder>(DIFF_CALLBACK) {

    private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

    /**
     * 更新标签映射数据
     */
    fun updateTagNameMap(newTagNameMap: Map<Long, String>) {
        this.tagNameMap = newTagNameMap
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = getItem(position)
        if (note != null) {
            holder.bind(note)
        }
    }


    fun getNoteAtPosition(position: Int): Note? {
        return getItem(position)
    }



    /**
     * 获取标签名称
     */
    private fun getTagName(tagId: Long): String {
        return tagNameMap[tagId] ?: "未分类"
    }

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val noteTitle: TextView = itemView.findViewById(R.id.noteTitle)
        private val noteContentPreview: TextView = itemView.findViewById(R.id.noteContentPreview)
        private val noteUpdateTime: TextView = itemView.findViewById(R.id.noteUpdateTime)
        private val tagsContainer: ViewGroup = itemView.findViewById(R.id.noteTagsContainer)

        fun bind(note: Note) {
            noteTitle.text = note.title
            noteContentPreview.text = note.content
            noteUpdateTime.text = dateFormat.format(Date(note.updatedAt))

            // 清除现有标签并添加新标签
            tagsContainer.removeAllViews()
            if (note.tagId > 0) {
                val tagView = LayoutInflater.from(tagsContainer.context)
                    .inflate(R.layout.item_tag, tagsContainer, false) as TextView
                // 使用模拟的标签名称
                tagView.text = getTagName(note.tagId)
                tagsContainer.addView(tagView)
            }

            itemView.setOnClickListener {
                onNoteClick(note)
            }
        }
    }

    // DiffUtil 回调
    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Note>() {
            // 判断是否是同一个 Item（通常比较 ID）
            override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
                return oldItem.noteId == newItem.noteId
            }

            // 判断内容是否完全一致（用于决定是否刷新 UI 显示）
            override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
                return oldItem == newItem
            }
        }
    }
}
