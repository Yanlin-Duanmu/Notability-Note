import java.util.Properties

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
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

        buildConfigField(
            "String",
            "OPENAI_API_KEY",
            "\"${localProperties.getProperty("OPENAI_API_KEY") ?: ""}\""
        )
        buildConfigField("String", "OPENAI_BASE_URL", "\"${localProperties.getProperty("OPENAI_BASE_URL") ?: "https://apis.iflow.cn/"}\"")
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }
}

dependencies {
    // ========== 核心 UI ==========
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    // RecyclerView —— 用于笔记列表
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // ========== Lifecycle / MVVM ==========
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    // ========== Room（数据库）==========
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    //========== Paging 3 ==========
    implementation(libs.room.paging)

    implementation("androidx.paging:paging-runtime-ktx:3.2.1")

    // ========== Kotlin 协程 ==========
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // ========== Gson（JSON序列化/反序列化）==========
    implementation("com.google.code.gson:gson:2.11.0")

    // ========== 单元测试 ==========
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    // For OpenAI Compatible Request Demo
    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.activity.compose)

    implementation("com.google.code.gson:gson:2.10.1")
    // Markwon 核心库
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:editor:4.6.2")
    implementation("io.noties.markwon:html:4.6.2")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    // 图片
    implementation("io.noties.markwon:image-glide:4.6.2")
    implementation("com.github.bumptech.glide:glide:4.16.0")

    implementation(libs.androidx.material.icons.extended)

}


//implementation("androidx.room:room-runtime:2.6.0")
//kapt("androidx.room:room-compiler:2.6.0")  // 使用 kapt 而不是 annotationProcessor
//implementation("androidx.room:room-ktx:2.6.0")

//Kotlin 协程 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")