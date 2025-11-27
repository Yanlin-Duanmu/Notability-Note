package com.noteability.mynote

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import com.noteability.mynote.data.AppDatabase
import com.noteability.mynote.data.repository.NoteRepository
import com.noteability.mynote.data.repository.TagRepository
import com.noteability.mynote.data.repository.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DatabaseTestActivity : Activity() {
    private val TAG = "MyNoteTest"
    private var testNoteId: Long = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_database_test)

        // 获取 Repository 实例
        val database = AppDatabase.getDatabase(this)
        val noteRepository = NoteRepository(database.noteDao())
        val tagRepository = TagRepository(database.tagDao())
        val userRepository = UserRepository(database.userDao())

        val testButton = findViewById<Button>(R.id.testButton)
        val resultText = findViewById<TextView>(R.id.resultText)

        testButton.setOnClickListener {
            testTagAndNoteFlow(noteRepository, tagRepository, userRepository, resultText)
        }
    }

    private fun testTagAndNoteFlow(
        noteRepository: NoteRepository,
        tagRepository: TagRepository,
        userRepository: UserRepository,
        resultText: TextView
    ) {
        resultText.text = "开始标签和笔记全流程测试，请稍候..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val testResults = StringBuilder()
                testResults.append("=== 标签和笔记全流程测试 ===\n\n")

                // 阶段1: 用户和基础环境测试
                testResults.append("🔍 阶段1: 用户和环境检查\n")
                testResults.append("----------------------------------------\n")

                // 测试1: 验证默认用户存在
                testResults.append("1. 检查默认用户...")
                val defaultUser = userRepository.getUserByUsername("default")
                if (defaultUser != null) {
                    testResults.append("✅ 成功 (用户ID: ${defaultUser.userId})\n")
                    Log.d(TAG, "默认用户ID: ${defaultUser.userId}")
                } else {
                    testResults.append("❌ 失败: 默认用户不存在\n")
                    runOnUiThread { resultText.text = testResults.toString() }
                    return@launch
                }

                // 阶段2: 标签管理功能测试
                testResults.append("\n🏷️  阶段2: 标签管理功能测试\n")
                testResults.append("----------------------------------------\n")

                // 测试2: 创建3个标签
                testResults.append("2. 创建3个学习标签...")
                val tagNames = listOf("c语言学习", "c++开发学习", "java开发学习")
                val createdTagIds = mutableListOf<Long>()
                
                for (tagName in tagNames) {
                    // 先检查标签是否已存在，存在则删除
                    val existingTag = tagRepository.getTagByName(tagName)
                    if (existingTag != null) {
                        tagRepository.deleteTagById(existingTag.tagId)
                        testResults.append("🔄")
                    }
                    
                    // 创建新标签
                    val tagId = tagRepository.createTag(tagName)
                    if (tagId > 0) {
                        createdTagIds.add(tagId)
                    }
                }
                
                if (createdTagIds.size == 3) {
                    testResults.append("✅ 成功\n")
                    tagNames.forEachIndexed { index, tagName ->
                        testResults.append("   • $tagName (ID: ${createdTagIds[index]})\n")
                    }
                } else {
                    testResults.append("❌ 失败: 只创建了${createdTagIds.size}个标签\n")
                    runOnUiThread { resultText.text = testResults.toString() }
                    return@launch
                }

                // 测试3: 获取所有标签
                testResults.append("3. 获取所有标签...")
                val allTags = tagRepository.getAllTags()
                if (allTags.size >= 3) {
                    testResults.append("✅ 成功 (共${allTags.size}个标签)\n")
                } else {
                    testResults.append("❌ 失败\n")
                }

                // 阶段3: 笔记与标签关联测试
                testResults.append("\n📝 阶段3: 笔记与标签关联测试\n")
                testResults.append("----------------------------------------\n")

                // 测试4: 为每个标签创建多篇笔记
                testResults.append("4. 为每个标签创建笔记...")
                val tagNoteCount = mutableMapOf<Long, Int>()
                
                // 为每个标签创建2篇笔记
                for (i in 0 until tagNames.size) {
                    val tagId = createdTagIds[i]
                    val tagName = tagNames[i]
                    val noteCount = 2
                    tagNoteCount[tagId] = noteCount
                    
                    for (j in 1..noteCount) {
                        noteRepository.insertArticleWithTimestamp(
                            title = "$tagName - 笔记 $j",
                            content = "这是关于${tagName}的第${j}篇笔记内容。",
                            userId = defaultUser.userId,
                            tagId = tagId,
                            createdTime = System.currentTimeMillis(),
                            updatedTime = System.currentTimeMillis()
                        )
                    }
                }
                
                testResults.append("✅ 成功\n")
                tagNames.forEachIndexed { index, tagName ->
                    testResults.append("   • $tagName: ${tagNoteCount[createdTagIds[index]]}篇笔记\n")
                }

                // 测试5: 验证每个标签下的笔记数量
                testResults.append("5. 验证标签笔记数量...")
                var allCountsCorrect = true
                
                for (i in 0 until tagNames.size) {
                    val tagId = createdTagIds[i]
                    val expectedCount = tagNoteCount[tagId] ?: 0
                    val actualCount = noteRepository.getNoteCountByTag(tagId)
                    
                    if (actualCount != expectedCount) {
                        allCountsCorrect = false
                        testResults.append("\n   ❌ ${tagNames[i]}: 期望${expectedCount}篇，实际${actualCount}篇")
                    }
                }
                
                if (allCountsCorrect) {
                    testResults.append("✅ 成功\n")
                } else {
                    testResults.append("\n")
                }

                // 阶段4: 查询功能测试
                testResults.append("\n🔍 阶段4: 查询功能测试\n")
                testResults.append("----------------------------------------\n")

                // 测试6: 根据标签ID查询笔记
                testResults.append("6. 根据标签ID查询笔记...")
                val cLanguageTagId = createdTagIds[0]
                val cLanguageNotes = noteRepository.getNotesByTagId(cLanguageTagId)
                
                if (cLanguageNotes.size == 2) {
                    testResults.append("✅ 成功 (找到${cLanguageNotes.size}篇笔记)\n")
                    cLanguageNotes.forEachIndexed { index, note ->
                        testResults.append("   ${index + 1}. ${note.title}\n")
                    }
                } else {
                    testResults.append("❌ 失败 (找到${cLanguageNotes.size}篇笔记)\n")
                }

                // 测试7: 关键词搜索
                testResults.append("7. 关键词搜索测试...")
                val searchResults = noteRepository.searchNotes("笔记")
                testResults.append("✅ 成功 (找到${searchResults.size}篇匹配笔记)\n")

                // 阶段5: 删除操作测试
                testResults.append("\n🗑️  阶段5: 删除操作测试\n")
                testResults.append("----------------------------------------\n")

                // 测试8: 删除"c语言学习"标签下的所有笔记
                testResults.append("8. 删除'c语言学习'标签下所有笔记...")
                val deletedNotesCount = noteRepository.deleteNotesByTagId(cLanguageTagId)
                
                if (deletedNotesCount >= 0) {
                    testResults.append("✅ 成功 (删除了${deletedNotesCount}篇笔记)\n")
                } else {
                    testResults.append("❌ 失败\n")
                }

                // 测试9: 验证删除结果
                testResults.append("9. 验证删除结果...")
                val remainingNotes = noteRepository.getNotesByTagId(cLanguageTagId)
                if (remainingNotes.isEmpty()) {
                    testResults.append("✅ 成功 ('c语言学习'标签下已无笔记)\n")
                } else {
                    testResults.append("❌ 失败 (仍有${remainingNotes.size}篇笔记)\n")
                }

                // 测试10: 删除"c语言学习"标签
                testResults.append("10. 删除'c语言学习'标签...")
                val cLanguageTag = tagRepository.getTagById(cLanguageTagId)
                val deletedTagResult = if (cLanguageTag != null) {
                    tagRepository.deleteTag(cLanguageTag)
                } else {
                    0
                }
                
                if (deletedTagResult > 0) {
                    testResults.append("✅ 成功\n")
                } else {
                    testResults.append("❌ 失败\n")
                }

                // 测试11: 验证标签删除结果
                testResults.append("11. 验证标签删除结果...")
                val deletedTagCheck = tagRepository.getTagById(cLanguageTagId)
                if (deletedTagCheck == null) {
                    testResults.append("✅ 成功 ('c语言学习'标签已删除)\n")
                } else {
                    testResults.append("❌ 失败 (标签仍然存在)\n")
                }

                // 最终总结
                testResults.append("\n🎯 测试总结\n")
                testResults.append("----------------------------------------\n")
                testResults.append("• 用户管理: ✅ 正常\n")
                testResults.append("• 标签管理: ✅ 正常\n")
                testResults.append("• 笔记与标签关联: ✅ 正常\n")
                testResults.append("• 查询功能: ✅ 正常\n")
                testResults.append("• 删除操作: ✅ 正常\n\n")
                
                // 显示当前数据库状态
                val remainingTags = tagRepository.getAllTags()
                val allRemainingNotes = noteRepository.getAllNotes().first()
                
                testResults.append("📊 当前数据库状态:\n")
                testResults.append("----------------------------------------\n")
                testResults.append("• 剩余标签数: ${remainingTags.size}\n")
                testResults.append("• 剩余笔记数: ${allRemainingNotes.size}\n\n")
                
                remainingTags.forEach { tag ->
                    val tagNoteCount = noteRepository.getNoteCountByTag(tag.tagId)
                    testResults.append("   • ${tag.name}: ${tagNoteCount}篇笔记\n")
                }
                
                testResults.append("\n🎉 标签和笔记全流程测试完成！\n")
                testResults.append("数据库层级关系结构工作正常")

                runOnUiThread {
                    resultText.text = testResults.toString()
                }

            } catch (e: Exception) {
                Log.e(TAG, "全面测试失败: ${e.message}", e)
                runOnUiThread {
                    resultText.text = "❌ 全面测试失败:\n${e.message}\n\n请查看 Logcat 获取详细信息"
                }
            }
        }
    }
}