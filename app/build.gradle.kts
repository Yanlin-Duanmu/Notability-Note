plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android") version "2.0.21"
    id("kotlin-kapt")
}

android {
    namespace = "com.noteability.mynote"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.noteability.mynote"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
//    composeOptions {
//        // 指定 Compose Compiler 版本与 Kotlin 2.0 兼容
//        kotlinCompilerExtensionVersion = "1.5.3"
//    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        compose = false
    }
}

dependencies {
    // ========== 核心 UI ==========
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    // RecyclerView —— 用于笔记列表
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // ========== Lifecycle / MVVM ==========
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    // ========== Room（数据库）==========
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")  // 添加的关键依赖

    // ========== Kotlin 协程 ==========
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // ========== 单元测试 ==========
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}


//implementation("androidx.room:room-runtime:2.6.0")
//kapt("androidx.room:room-compiler:2.6.0")  // 使用 kapt 而不是 annotationProcessor
//implementation("androidx.room:room-ktx:2.6.0")

//Kotlin 协程 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")