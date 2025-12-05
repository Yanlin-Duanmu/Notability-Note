package com.noteability.mynote.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.noteability.mynote.R
import com.noteability.mynote.data.entity.Note
import java.text.SimpleDateFormat
import java.util.*

class NoteAdapter(
    private var notes: List<Note>,
    private val onNoteClick: (Note) -> Unit,
    // 标签映射，从外部传入
    private var tagNameMap: Map<Long, String> = emptyMap()
) : RecyclerView.Adapter<NoteAdapter.NoteViewHolder>() {

    private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
    
    /**
     * 更新标签映射数据
     */
    fun updateTagNameMap(newTagNameMap: Map<Long, String>) {
        this.tagNameMap = newTagNameMap
        // 通知数据集变化以更新所有视图
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        val note = notes[position]
        holder.bind(note)
    }

    override fun getItemCount(): Int = notes.size
    
    /**
     * 更新数据集合并通知适配器
     */
    fun updateNotes(newNotes: List<Note>) {
        this.notes = newNotes
        notifyDataSetChanged()
    }
    
    /**
     * 删除单个笔记
     */
    fun removeNote(noteId: Long) {
        val index = notes.indexOfFirst { it.noteId == noteId }
        if (index != -1) {
            notes = notes.toMutableList().apply { removeAt(index) }
            notifyItemRemoved(index)
        }
    }
    
    /**
     * 获取标签名称
     */
    private fun getTagName(tagId: Long): String {
        return tagNameMap[tagId] ?: "未分类"
    }

    fun getNoteAt(position: Int): Note {
        return notes[position]
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
}