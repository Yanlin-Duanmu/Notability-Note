//package com.noteability.mynote.ui.adapter
//
//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.paging.PagingDataAdapter
//import androidx.recyclerview.widget.DiffUtil
//import androidx.recyclerview.widget.RecyclerView
//import com.noteability.mynote.R
//import com.noteability.mynote.data.entity.Note
//import java.text.SimpleDateFormat
//import java.util.*
//
//
//class NoteAdapter(
//    private val onNoteClick: (Note) -> Unit,
//    private var tagNameMap: Map<Long, String> = emptyMap()
//) : PagingDataAdapter<Note, NoteAdapter.NoteViewHolder>(DIFF_CALLBACK) {
//
//    private val dateFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())
//
//    /**
//     * 更新标签映射数据
//     */
//    fun updateTagNameMap(newTagNameMap: Map<Long, String>) {
//        this.tagNameMap = newTagNameMap
//        notifyDataSetChanged()
//    }
//
//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_note, parent, false)
//        return NoteViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
//        val note = getItem(position)
//        if (note != null) {
//            holder.bind(note)
//        }
//    }
//
//
//    fun getNoteAtPosition(position: Int): Note? {
//        return getItem(position)
//    }
//
//
//
//    /**
//     * 获取标签名称
//     */
//    private fun getTagName(tagId: Long): String {
//        return tagNameMap[tagId] ?: "未分类"
//    }
//
//    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        private val noteTitle: TextView = itemView.findViewById(R.id.noteTitle)
//        private val noteContentPreview: TextView = itemView.findViewById(R.id.noteContentPreview)
//        private val noteUpdateTime: TextView = itemView.findViewById(R.id.noteUpdateTime)
//        private val tagsContainer: ViewGroup = itemView.findViewById(R.id.noteTagsContainer)
//
//        fun bind(note: Note) {
//            noteTitle.text = note.title
//            noteContentPreview.text = note.content
//            noteUpdateTime.text = dateFormat.format(Date(note.updatedAt))
//
//            // 清除现有标签并添加新标签
//            tagsContainer.removeAllViews()
//            if (note.tagId > 0) {
//                val tagView = LayoutInflater.from(tagsContainer.context)
//                    .inflate(R.layout.item_tag, tagsContainer, false) as TextView
//                // 使用模拟的标签名称
//                tagView.text = getTagName(note.tagId)
//                tagsContainer.addView(tagView)
//            }
//
//            itemView.setOnClickListener {
//                onNoteClick(note)
//            }
//        }
//    }
//
//    // DiffUtil 回调
//    companion object {
//        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Note>() {
//            // 判断是否是同一个 Item（通常比较 ID）
//            override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
//                return oldItem.noteId == newItem.noteId
//            }
//
//            // 判断内容是否完全一致（用于决定是否刷新 UI 显示）
//            override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
//                return oldItem == newItem
//            }
//        }
//    }
//}
package com.noteability.mynote.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox // [新增]
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

    // [新增] 标记当前是否处于批量选择模式
    var isSelectionMode: Boolean = false
        private set

    // [新增] 存储被选中的笔记 ID (使用 Set 防止重复，使用 ID 保证准确性)
    private val selectedNoteIds = HashSet<Long>()

    // [新增] 回调：当选中数量发生变化时通知外部（用于更新标题 "已选 X 项"）
    var onSelectionCountChanged: ((Int) -> Unit)? = null

    /**
     * 更新标签映射数据
     */
    fun updateTagNameMap(newTagNameMap: Map<Long, String>) {
        this.tagNameMap = newTagNameMap
        notifyDataSetChanged()
    }

    // [新增] 进入或退出选择模式
    fun setSelectionMode(enable: Boolean) {
        if (isSelectionMode != enable) {
            isSelectionMode = enable
            if (!enable) {
                selectedNoteIds.clear() // 退出模式时清空选中
                onSelectionCountChanged?.invoke(0)
            }
            notifyDataSetChanged() // 刷新所有 Item 以显示/隐藏 CheckBox
        }
    }

    // [新增] 获取当前选中的所有笔记 ID (给 Activity 删除用)
    fun getSelectedNoteIds(): List<Long> {
        return selectedNoteIds.toList()
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
        // [新增] 绑定 CheckBox
        private val noteCheckBox: CheckBox = itemView.findViewById(R.id.noteCheckBox)

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

            // [修改] 核心交互逻辑
            if (isSelectionMode) {
                // 1. 如果是选择模式，显示 CheckBox
                noteCheckBox.visibility = View.VISIBLE
                // 2. 根据 ID 是否在 Set 中，设置选中状态
                noteCheckBox.isChecked = selectedNoteIds.contains(note.noteId)

                // 3. 点击整个 Item 触发选中/取消
                itemView.setOnClickListener {
                    if (selectedNoteIds.contains(note.noteId)) {
                        selectedNoteIds.remove(note.noteId)
                        noteCheckBox.isChecked = false
                    } else {
                        selectedNoteIds.add(note.noteId)
                        noteCheckBox.isChecked = true
                    }
                    // 通知 Activity 更新标题数量
                    onSelectionCountChanged?.invoke(selectedNoteIds.size)
                }
            } else {
                // 1. 如果是普通模式，隐藏 CheckBox
                noteCheckBox.visibility = View.GONE
                // 2. 恢复普通的点击跳转逻辑
                itemView.setOnClickListener {
                    onNoteClick(note)
                }
            }
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Note>() {
            override fun areItemsTheSame(oldItem: Note, newItem: Note): Boolean {
                return oldItem.noteId == newItem.noteId
            }

            override fun areContentsTheSame(oldItem: Note, newItem: Note): Boolean {
                return oldItem == newItem
            }
        }
    }
}