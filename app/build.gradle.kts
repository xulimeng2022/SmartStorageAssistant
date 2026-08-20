// app 模块构建脚本
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

// 读取 local.properties 中的免费模式 API Key（未配置则为空串，可正常编译）
val localProperties = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localProperties.load(localPropsFile.inputStream())
}
val siliconflowApiKey: String = localProperties.getProperty("SILICONFLOW_API_KEY", "")

android {
    namespace = "com.example.smartstorage"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.smartstorage"
        minSdk = 26
        targetSdk = 35
        versionCode = 52
        versionName = "2.8.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // 默认大模型 Key（可留空，在 App 设置页可随时修改；也可配置其他任意 OpenAI 兼容大模型）
        buildConfigField("String", "DEEPSEEK_API_KEY", "\"\"")

        // 免费模式（硅基流动）Key：从 local.properties 的 SILICONFLOW_API_KEY 注入，未配置为空串
        buildConfigField(
            "String",
            "SILICONFLOW_API_KEY",
            "\"${siliconflowApiKey.replace("\"", "\\\"")}\"",
        )
    }

    buildTypes {
        release {
            // 初始版本暂不启用代码混淆，后续可再开启
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
        compose = true
        buildConfig = true
    }
}

dependencies {
    // AndroidX 基础
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Jetpack Compose（由 BOM 统一管理版本）
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)

    // Hilt 依赖注入
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    // Room 本地数据库
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Kotlin 协程
    implementation(libs.kotlinx.coroutines.android)

    // 网络请求（大模型 HTTP）
    implementation(libs.okhttp)

    // 数据存储：DataStore + 加密存储（API Key）
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.security.crypto)

    // 图片加载（Coil）
    implementation(libs.coil.compose)


    // 测试
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}