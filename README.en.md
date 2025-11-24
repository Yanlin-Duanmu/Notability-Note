This project is an Android application written in Kotlin, primarily implementing a note management feature that supports user registration, login, CRUD operations on notes, and search functionality. Below is an overview of the main features and structure:

---

## 📝 Project Overview

**MyNote** is a simple note management application supporting the following features:

- User registration and login (passwords stored with encryption)
- Create, edit, and delete notes
- Note search functionality
- Tag management (expandable in the future)

The project uses the **Room persistence library** for local database management, follows the **MVVM architecture**, and leverages **Kotlin coroutines** for asynchronous operations.

---

## 🧱 Technical Architecture

- **Data Layer**: Uses Room database with three entity classes: `User`, `Note`, and `Tag`, accessed via DAO interfaces.
- **Business Logic Layer**: Encapsulates data operations through the `Repository` class.
- **ViewModel Layer**: Uses `ViewModel` to manage UI-related data and prevent memory leaks.
- **UI Layer**: Implements view interactions using `Activity` and View Binding.

---

## 📁 Main Modules Overview

### Database Module

- `AppDatabase.kt`: Abstract Room database class defining the database schema.
- `User.kt`, `Note.kt`, `Tag.kt`: Database entity classes.
- `UserDao.kt`, `NoteDao.kt`, `TagDao.kt`: DAO interfaces for database operations.

### User Module

- `UserRepository.kt`: Encapsulates user data operations.
- `AuthViewModel.kt`: Handles user login and registration logic.
- `LoginActivity.kt`: Login interface.
- `SecurityUtils.kt`: Utility class for password encryption.

### Note Module

- `NoteListViewModel.kt`: Manages note list data.
- `MainActivity.kt`: Main interface displaying the note list.

---

## 🧪 Testing Module

- `ExampleUnitTest.kt`: Local unit tests.
- `ExampleInstrumentedTest.kt`: Android instrumentation tests.

---

## 🚀 Quick Start

### Environment Setup

- Android Studio (recommended latest stable version)
- JDK 11+
- Android SDK (API 21+)

### Build and Run

1. Clone the project:
   ```bash
   git clone https://gitee.com/xueyuTYH/my-note.git
   ```

2. Open the project in Android Studio and sync Gradle.
3. Connect an Android device or start an emulator.
4. Click the Run button to launch the app.

---

## 📌 Usage Instructions

- Register an account upon first use.
- After logging in, you can view, add, edit, and delete notes.
- Search notes by keyword in the title.

---

## 🤝 Contribution Guidelines

Pull requests and issues are welcome. Please ensure consistent code style and include necessary comments and test cases.

---

## 📄 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.