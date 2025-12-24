//package com.noteability.mynote
//
//import androidx.arch.core.executor.testing.InstantTaskExecutorRule
//import androidx.room.Room
//import androidx.test.core.app.ApplicationProvider
//import androidx.test.ext.junit.runners.AndroidJUnit4
//import com.noteability.mynote.data.database.AppDatabase
//import com.noteability.mynote.data.database.entity.Note
//import com.noteability.mynote.data.database.entity.Tag
//import com.noteability.mynote.data.database.entity.User
//import com.noteability.mynote.data.repository.impl.NoteRepositoryImpl
//import com.noteability.mynote.data.repository.impl.TagRepositoryImpl
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.flow.first
//import kotlinx.coroutines.runBlocking
//import org.junit.After
//import org.junit.Assert.*
//import org.junit.Before
//import org.junit.Rule
//import org.junit.Test
//import org.junit.runner.RunWith
//import java.io.IOException
//
//@RunWith(AndroidJUnit4::class)
//class UserIsolationTest {
//
//    @get:Rule
//    val instantTaskExecutorRule = InstantTaskExecutorRule()
//
//    private lateinit var database: AppDatabase
//    private lateinit var noteRepository: NoteRepositoryImpl
//    private lateinit var tagRepository: TagRepositoryImpl
//
//    // 测试用户ID
//    private val user1Id = 1L
//    private val user2Id = 2L
//
//    @Before
//    fun setUp() {
//        // 使用内存数据库进行测试
//        database = Room.inMemoryDatabaseBuilder(
//            ApplicationProvider.getApplicationContext(),
//            AppDatabase::class.java
//        ).allowMainThreadQueries().build()
//
//        // 初始化仓库
//        noteRepository = NoteRepositoryImpl(database.noteDao(), database.tagDao(), database.userDao())
//        tagRepository = TagRepositoryImpl(database.tagDao(), database.noteDao())
//
//        // 设置用户ID
//        noteRepository.setLoggedInUserId(user1Id)
//        tagRepository.setLoggedInUserId(user1Id)
//
//        // 插入测试用户
//        runBlocking {
//            database.userDao().insert(User(user1Id, "user1@example.com", "user1", "password"))
//            database.userDao().insert(User(user2Id, "user2@example.com", "user2", "password"))
//        }
//    }
//
//    @After
//    @Throws(IOException::class)
//    fun tearDown() {
//        database.close()
//    }
//
//    @Test
//    fun testTagUserIsolation() = runBlocking {
//        // 用户1创建标签
//        tagRepository.setLoggedInUserId(user1Id)
//        val user1Tag = Tag(0, "用户1的标签", user1Id)
//        tagRepository.saveTag(user1Tag)
//
//        // 用户2创建标签
//        tagRepository.setLoggedInUserId(user2Id)
//        val user2Tag = Tag(0, "用户2的标签", user2Id)
//        tagRepository.saveTag(user2Tag)
//
//        // 验证用户1只能看到自己的标签
//        tagRepository.setLoggedInUserId(user1Id)
//        val user1Tags = tagRepository.getAllTags().first()
//        assertEquals(1, user1Tags.size)
//        assertEquals("用户1的标签", user1Tags[0].name)
//
//        // 验证用户2只能看到自己的标签
//        tagRepository.setLoggedInUserId(user2Id)
//        val user2Tags = tagRepository.getAllTags().first()
//        assertEquals(1, user2Tags.size)
//        assertEquals("用户2的标签", user2Tags[0].name)
//    }
//
//    @Test
//    fun testNoteUserIsolation() = runBlocking {
//        // 用户1创建标签
//        tagRepository.setLoggedInUserId(user1Id)
//        val user1Tag = tagRepository.saveTag(Tag(0, "用户1的标签", user1Id))
//
//        // 用户2创建标签
//        tagRepository.setLoggedInUserId(user2Id)
//        val user2Tag = tagRepository.saveTag(Tag(0, "用户2的标签", user2Id))
//
//        // 用户1创建笔记
//        noteRepository.setLoggedInUserId(user1Id)
//        val user1Note = Note(0, "用户1的笔记", "用户1的内容", user1Tag.tagId, user1Id)
//        noteRepository.saveNote(user1Note)
//
//        // 用户2创建笔记
//        noteRepository.setLoggedInUserId(user2Id)
//        val user2Note = Note(0, "用户2的笔记", "用户2的内容", user2Tag.tagId, user2Id)
//        noteRepository.saveNote(user2Note)
//
//        // 验证用户1只能看到自己的笔记
//        noteRepository.setLoggedInUserId(user1Id)
//        val user1Notes = noteRepository.getAllNotes().first()
//        assertEquals(1, user1Notes.size)
//        assertEquals("用户1的笔记", user1Notes[0].title)
//
//        // 验证用户2只能看到自己的笔记
//        noteRepository.setLoggedInUserId(user2Id)
//        val user2Notes = noteRepository.getAllNotes().first()
//        assertEquals(1, user2Notes.size)
//        assertEquals("用户2的笔记", user2Notes[0].title)
//    }
//
//    @Test
//    fun testNoteTagConsistency() = runBlocking {
//        // 设置用户1
//        noteRepository.setLoggedInUserId(user1Id)
//        tagRepository.setLoggedInUserId(user1Id)
//
//        // 创建标签和笔记
//        val tag = tagRepository.saveTag(Tag(0, "测试标签", user1Id))
//        val note = noteRepository.saveNote(Note(0, "测试笔记", "测试内容", tag.tagId, user1Id))
//
//        // 通过标签ID获取笔记
//        val notesByTag = noteRepository.getNotesByTagId(tag.tagId).first()
//        assertEquals(1, notesByTag.size)
//        assertEquals(note.noteId, notesByTag[0].noteId)
//
//        // 切换到用户2
//        noteRepository.setLoggedInUserId(user2Id)
//        tagRepository.setLoggedInUserId(user2Id)
//
//        // 验证用户2看不到用户1的标签和笔记
//        val user2Tags = tagRepository.getAllTags().first()
//        val user2Notes = noteRepository.getAllNotes().first()
//        val user2NotesByTag = noteRepository.getNotesByTagId(tag.tagId).first()
//
//        assertEquals(0, user2Tags.size)
//        assertEquals(0, user2Notes.size)
//        assertEquals(0, user2NotesByTag.size)
//    }
//}