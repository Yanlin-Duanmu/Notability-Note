# MyNote 数据库接口文档

## 1. 数据库概述

MyNote 应用使用 Room 数据库实现本地数据持久化。数据库名称为 `mynote_database`，当前版本为 **4**。

### 1.1 主要功能
- 用户账户管理
- 标签分类管理
- 笔记创建与管理（按标签分类）
- 富文本笔记支持
- 搜索功能

## 2. 表结构设计

### 2.1 users 表

**描述**：存储用户账户信息

| 字段名 | 数据类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `userId` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | 用户ID |
| `username` | `TEXT` | `NOT NULL` | 用户名 |
| `passwordHash` | `TEXT` | `NOT NULL` | 密码哈希值 |
| `email` | `TEXT` | `NULL` | 电子邮件（可选） |

### 2.2 tags 表

**描述**：存储用户创建的标签信息

| 字段名 | 数据类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `tagId` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | 标签ID |
| `userId` | `INTEGER` | `FOREIGN KEY REFERENCES users(userId) ON DELETE CASCADE NOT NULL` | 所属用户ID |
| `name` | `TEXT` | `NOT NULL` | 标签名称 |
| `noteCount` | `INTEGER` | `DEFAULT 0` | 该标签下的笔记数量

### 2.3 notes 表

**描述**：存储用户创建的笔记信息，按标签分类

| 字段名 | 数据类型 | 约束 | 描述 |
| :--- | :--- | :--- | :--- |
| `noteId` | `INTEGER` | `PRIMARY KEY AUTOINCREMENT` | 笔记ID |
| `userId` | `INTEGER` | `FOREIGN KEY REFERENCES users(userId) ON DELETE CASCADE NOT NULL` | 所属用户ID |
| `tagId` | `INTEGER` | `FOREIGN KEY REFERENCES tags(tagId) ON DELETE CASCADE NOT NULL` | 所属标签ID |
| `title` | `TEXT` | `NOT NULL` | 笔记标题 |
| `content` | `TEXT` | `NOT NULL` | 笔记内容 |
| `createdAt` | `INTEGER` | `DEFAULT CURRENT_TIMESTAMP NOT NULL` | 创建时间戳 |
| `updatedAt` | `INTEGER` | `DEFAULT CURRENT_TIMESTAMP NOT NULL` | 更新时间戳 |

## 3. DAO 接口

### 3.1 UserDao

**描述**：用户数据访问接口

```kotlin
@Dao
interface UserDao {
    @Insert
    suspend fun insertUser(user: User): Long

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE userId = :id LIMIT 1")
    suspend fun getUserById(id: Long): User?
}
```

**方法说明**：
| 方法名 | 参数 | 返回值 | 描述 |
| :--- | :--- | :--- | :--- |
| `insertUser` | `user: User` | `Long` | 插入用户并返回新用户ID |
| `getUserByUsername` | `username: String` | `User?` | 根据用户名查找用户 |
| `getUserById` | `id: Long` | `User?` | 根据ID查找用户 |

### 3.2 TagDao

**描述**：标签数据访问接口

```kotlin
@Dao
interface TagDao {
    @Insert
    suspend fun insertTag(tag: Tag): Long

    @Update
    suspend fun updateTag(tag: Tag): Int

    @Delete
    suspend fun deleteTag(tag: Tag): Int

    @Query("SELECT * FROM tags WHERE userId = :userId ORDER BY name ASC")
    suspend fun getTagsByUserId(userId: Long): List<Tag>

    @Query("SELECT * FROM tags WHERE userId = :userId AND tagId = :tagId LIMIT 1")
    suspend fun getTagById(userId: Long, tagId: Long): Tag?

    @Query("SELECT * FROM tags WHERE userId = :userId AND name = :tagName LIMIT 1")
    suspend fun getTagByName(userId: Long, tagName: String): Tag?
    
    @Query("SELECT * FROM tags WHERE userId = :userId AND name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun getTagsByNameContaining(userId: Long, query: String): List<Tag>

    @Query("DELETE FROM tags WHERE userId = :userId AND tagId = :tagId")
    suspend fun deleteTagById(userId: Long, tagId: Long): Int
}
```

**方法说明**：
| 方法名 | 参数 | 返回值 | 描述 |
| :--- | :--- | :--- | :--- |
| `insertTag` | `tag: Tag` | `Long` | 插入标签并返回新标签ID |
| `updateTag` | `tag: Tag` | `Int` | 更新标签信息，返回受影响的行数 |
| `deleteTag` | `tag: Tag` | `Int` | 删除标签，返回受影响的行数 |
| `getTagsByUserId` | `userId: Long` | `List<Tag>` | 获取用户的所有标签（按名称排序） |
| `getTagById` | `userId: Long, tagId: Long` | `Tag?` | 根据ID查找标签 |
| `getTagByName` | `userId: Long, tagName: String` | `Tag?` | 根据名称查找标签 |
| `getTagsByNameContaining` | `userId: Long, query: String` | `List<Tag>` | 根据名称模糊搜索标签，返回符合条件的标签列表 |
| `deleteTagById` | `userId: Long, tagId: Long` | `Int` | 根据ID删除标签，返回受影响的行数 |

### 3.3 NoteDao

**描述**：笔记数据访问接口

```kotlin
@Dao
interface NoteDao {
    // 基础 CRUD 操作
    @Insert
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note): Int

    @Delete
    suspend fun deleteNote(note: Note): Int

    // 查询操作
    @Query("SELECT * FROM notes WHERE userId = :userId ORDER BY updatedAt DESC")
    suspend fun getNotesByUserId(userId: Long): List<Note>

    @Query("SELECT * FROM notes WHERE userId = :userId AND tagId = :tagId ORDER BY updatedAt DESC")
    suspend fun getNotesByTagId(userId: Long, tagId: Long): List<Note>

    @Query("SELECT * FROM notes WHERE noteId = :id")
    suspend fun getNoteById(id: Long): Note?

    // 搜索功能
    @Query("SELECT * FROM notes WHERE userId = :userId AND title LIKE '%' || :query || '%' ORDER BY updatedAt DESC")
    suspend fun searchNotes(userId: Long, query: String): List<Note>

    @Query("SELECT * FROM notes WHERE userId = :userId AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%') ORDER BY updatedAt DESC")
    suspend fun searchNotesByTitleOrContent(userId: Long, query: String): List<Note>

    // 为富文本模块提供的专用方法
    @Insert
    suspend fun insertArticle(note: Note): Long

    @Query("UPDATE notes SET title = :newTitle, content = :newContent, updatedAt = :updatedTime WHERE noteId = :id")
    suspend fun updateArticle(id: Long, newTitle: String, newContent: String, updatedTime: Long): Int

    // 便捷方法 - 自动处理时间戳的插入和更新
    @Query("INSERT INTO notes (title, content, userId, tagId, createdAt, updatedAt) VALUES (:title, :content, :userId, :tagId, :createdTime, :updatedTime)")
    suspend fun insertArticleWithTimestamp(title: String, content: String, userId: Long, tagId: Long, createdTime: Long, updatedTime: Long): Long

    @Query("UPDATE notes SET title = :newTitle, content = :newContent, updatedAt = :updatedTime WHERE noteId = :id")
    suspend fun updateArticleWithTimestamp(id: Long, newTitle: String, newContent: String, updatedTime: Long): Int

    @Query("UPDATE notes SET tagId = :newTagId WHERE noteId = :id")
    suspend fun updateNoteTag(id: Long, newTagId: Long): Int

    // 统计相关
    @Query("SELECT COUNT(*) FROM notes WHERE userId = :userId")
    suspend fun getNoteCountByUser(userId: Long): Int

    @Query("SELECT COUNT(*) FROM notes WHERE userId = :userId AND tagId = :tagId")
    suspend fun getNoteCountByTag(userId: Long, tagId: Long): Int

    // 批量操作
    @Query("DELETE FROM notes WHERE userId = :userId")
    suspend fun deleteAllNotesByUser(userId: Long): Int

    @Query("DELETE FROM notes WHERE userId = :userId AND tagId = :tagId")
    suspend fun deleteNotesByTagId(userId: Long, tagId: Long): Int

    @Query("DELETE FROM notes WHERE noteId = :id")
    suspend fun deleteNoteById(id: Long): Int
}
```

**方法说明**：
| 方法名 | 参数 | 返回值 | 描述 |
| :--- | :--- | :--- | :--- |
| `insertNote` | `note: Note` | `Long` | 插入笔记并返回新笔记ID |
| `updateNote` | `note: Note` | `Int` | 更新笔记信息，返回受影响的行数 |
| `deleteNote` | `note: Note` | `Int` | 删除笔记，返回受影响的行数 |
| `getNotesByUserId` | `userId: Long` | `List<Note>` | 获取用户的所有笔记 |
| `getNotesByTagId` | `userId: Long, tagId: Long` | `List<Note>` | 获取指定标签下的所有笔记 |
| `getNoteById` | `id: Long` | `Note?` | 根据ID查找笔记 |
| `searchNotes` | `userId: Long, query: String` | `List<Note>` | 根据标题搜索笔记 |
| `searchNotesByTitleOrContent` | `userId: Long, query: String` | `List<Note>` | 根据标题或内容搜索笔记 |
| `insertArticle` | `note: Note` | `Long` | 插入富文本笔记 |
| `updateArticle` | `id: Long, newTitle: String, newContent: String, updatedTime: Long` | `Int` | 更新富文本笔记，返回受影响的行数 |
| `insertArticleWithTimestamp` | `title: String, content: String, userId: Long, tagId: Long, createdTime: Long, updatedTime: Long` | `Long` | 插入带时间戳的笔记 |
| `updateArticleWithTimestamp` | `id: Long, newTitle: String, newContent: String, updatedTime: Long` | `Int` | 更新带时间戳的笔记，返回受影响的行数 |
| `updateNoteTag` | `id: Long, newTagId: Long` | `Int` | 更新笔记所属标签，返回受影响的行数 |
| `getNoteCountByUser` | `userId: Long` | `Int` | 获取用户笔记总数 |
| `getNoteCountByTag` | `userId: Long, tagId: Long` | `Int` | 获取指定标签下的笔记数量 |
| `deleteAllNotesByUser` | `userId: Long` | `Int` | 删除用户的所有笔记，返回受影响的行数 |
| `deleteNotesByTagId` | `userId: Long, tagId: Long` | `Int` | 删除指定标签下的所有笔记，返回受影响的行数 |
| `deleteNoteById` | `id: Long` | `Int` | 根据ID删除笔记，返回受影响的行数 |

## 4. 数据库配置

### 4.1 AppDatabase

```kotlin
@Database(
    entities = [User::class, Note::class, Tag::class],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mynote_database"
                )
                // 添加破坏性迁移，仅用于开发环境
                // 生产环境应实现具体的Migration类
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
```

## 5. 使用示例

### 5.1 初始化数据库

```kotlin
// 在Application类或Activity中初始化
val db = AppDatabase.getDatabase(context)
val userDao = db.userDao()
val tagDao = db.tagDao()
val noteDao = db.noteDao()
```

### 5.2 用户管理

```kotlin
// 插入新用户
val newUserId = userDao.insertUser(User(username = "testuser", passwordHash = "hashed_password"))

// 根据用户名查询用户
val user = userDao.getUserByUsername("testuser")
```

### 5.3 标签管理

```kotlin
// 为用户创建标签
val javaDevTagId = tagDao.insertTag(Tag(userId = newUserId, name = "Java开发"))
val cppDevTagId = tagDao.insertTag(Tag(userId = newUserId, name = "C++开发"))

// 获取用户的所有标签
val userTags = tagDao.getTagsByUserId(newUserId)

// 根据名称查找标签
val javaDevTag = tagDao.getTagByName(newUserId, "Java开发")
```

### 5.4 笔记管理（按标签分类）

```kotlin
// 在Java开发标签下创建笔记
val javaNoteId = noteDao.insertNote(Note(
    userId = newUserId,
    tagId = javaDevTagId,
    title = "Java基础",
    content = "这是Java基础笔记内容"
))

val javaWebNoteId = noteDao.insertNote(Note(
    userId = newUserId,
    tagId = javaDevTagId,
    title = "JavaWeb",
    content = "这是JavaWeb笔记内容"
))

// 在C++开发标签下创建笔记
val cppNoteId = noteDao.insertNote(Note(
    userId = newUserId,
    tagId = cppDevTagId,
    title = "C++基础",
    content = "这是C++基础笔记内容"
))

// 获取指定标签下的所有笔记
val javaNotes = noteDao.getNotesByTagId(newUserId, javaDevTagId)
val cppNotes = noteDao.getNotesByTagId(newUserId, cppDevTagId)

// 更新笔记所属标签（并检查是否成功）
val updateResult = noteDao.updateNoteTag(javaWebNoteId, cppDevTagId)
if (updateResult > 0) {
    // 更新成功
    println("笔记标签更新成功")
} else {
    // 更新失败
    println("笔记标签更新失败，可能笔记不存在")
}

// 删除指定标签下的所有笔记（并检查是否成功）
val deleteResult = noteDao.deleteNotesByTagId(newUserId, javaDevTagId)
println("成功删除了 $deleteResult 条笔记")

// 修改标签名称（并检查是否成功）
val tagToUpdate = tagDao.getTagByName(newUserId, "Java开发")
tagToUpdate?.let {
    val updatedTag = it.copy(name = "Java技术")
    val tagUpdateResult = tagDao.updateTag(updatedTag)
    if (tagUpdateResult > 0) {
        println("标签名称更新成功")
    }
}
```

### 5.5 笔记搜索

```kotlin
// 搜索笔记
val searchResults = noteDao.searchNotesByTitleOrContent(newUserId, "Java")
```

## 6. 注意事项

1. **返回值说明**：
   - 更新方法（如`updateNote`、`updateTag`）返回受影响的行数，可以通过检查返回值是否大于0来确认操作是否成功
   - 删除方法（如`deleteNote`、`deleteTag`）返回受影响的行数，可以通过检查返回值是否大于0来确认操作是否成功
   - 插入方法返回新记录的ID值

2. **外键约束**：
   - 删除用户时，会级联删除该用户的所有标签和笔记
   - 删除标签时，会级联删除该标签下的所有笔记

3. **事务处理**：
   - 复杂操作应使用事务确保数据一致性
   - Room提供`@Transaction`注解简化事务管理

4. **性能优化**：
   - 查询时添加适当的索引（如按userId和tagId）
   - 使用分页查询处理大量数据
   - 避免在主线程执行数据库操作

5. **版本管理**：
   - 当前使用`fallbackToDestructiveMigration()`进行开发环境测试
   - 生产环境应实现具体的`Migration`类处理数据库迁移

## 7. 版本历史

| 版本 | 变更内容 | 日期 |
| :--- | :--- | :--- |
| 4 | 1. 在Tag表中新增noteCount字段，用于存储标签下的笔记数量<br>2. 在TagDao中新增getTagsByNameContaining方法，支持标签名称模糊搜索 | 2025-11-27 |
| 3 | 更新所有更新和删除方法，使其返回受影响的行数，便于检查操作是否成功 | 2025-11-27 |
| 2 | 1. 在Note表中添加tagId字段和外键关系<br>2. 增强TagDao功能（添加更新、删除、查询方法）<br>3. 增强NoteDao功能（添加按标签查询、更新标签等方法） | 2025-11-27 |
| 1 | 初始版本，包含用户、笔记和标签表基础功能 | 2025-11-25 |