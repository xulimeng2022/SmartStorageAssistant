# ==================== 智能收纳助手 ProGuard / R8 混淆规则 ====================
# 对应技术栈：Kotlin + Jetpack Compose + Room + Hilt + OkHttp + DataStore
#            + security-crypto + Coil + Coroutines + org.json（JDK 内置）
# 说明：Room / Hilt / OkHttp / Coil / DataStore / security-crypto / Compose
#       等官方库自带 consumer rules，R8 会自动应用；以下为兜底与自定义类规则。

# ==================== 通用规则 ====================

# 保留所有 Activity / Service / BroadcastReceiver / ContentProvider
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# 保留自定义 View（含构造方法）
-keep public class * extends android.view.View {
    public <init>(android.content.Context);
    public <init>(android.content.Context, android.util.AttributeSet);
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# 保留所有 Enum
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留 Parcelable 实现类
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# 保留 R 文件资源 ID（Compose 资源引用）
-keepclassmembers class **.R$* {
    public static <fields>;
}

# 保留自定义 Application 类（Hilt 入口，Manifest 引用）
-keep class * extends android.app.Application

# 保留 Native 方法名
-keepclasseswithmembernames class * {
    native <methods>;
}

# 保留 Kotlin 反射与元数据（协程 / 序列化可能用到）
-keep class kotlin.reflect.** { *; }
-keepclassmembers class kotlin.Metadata {
    public *;
}

# ==================== Room 数据库 ====================

# 保留 RoomDatabase 子类
-keep class * extends androidx.room.RoomDatabase

# 保留实体类（Room 生成代码引用字段与类名）
-keep class com.example.smartstorage.data.local.entity.** { *; }

# 保留 TypeConverter（方法名被 Room 运行时调用）
-keep class com.example.smartstorage.data.local.converters.** { *; }

# 保留 DAO 接口
-keep interface com.example.smartstorage.data.local.dao.** { *; }

-dontwarn androidx.room.**

# ==================== Hilt 依赖注入 ====================

# 保留所有 ViewModel（Hilt 生成工厂引用构造器，混淆后会导致注入失败）
-keep class * extends androidx.lifecycle.ViewModel { *; }

# 保留 Hilt 内部类与注解
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-dontwarn dagger.hilt.**

# ==================== 项目数据模型 ====================
# 备份 / 设置模型通过 org.json 手动序列化 + DataStore 读写，保守保留

-keep class com.example.smartstorage.data.local.prefs.** { *; }
-keep class com.example.smartstorage.data.local.backup.** { *; }

# ==================== 网络（OkHttp）====================
# OkHttp 官方 consumer rules 已自动应用，这里仅关闭警告
-dontwarn okhttp3.**
-dontwarn okio.**

# ==================== 图片加载（Coil）====================
-dontwarn coil.**

# ==================== DataStore / security-crypto ====================
-dontwarn androidx.datastore.**
-dontwarn androidx.security.crypto.**

# ==================== Jetpack Compose ====================
# Compose 官方 consumer rules 已处理，这里仅关闭警告
-dontwarn androidx.compose.**
-dontwarn androidx.compose.ui.tooling.**

# ==================== Kotlin 协程 ====================
-dontwarn kotlinx.coroutines.**

# ==================== 其他（消除无关警告，提升构建稳定性）====================
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
-dontwarn javax.lang.model.**

# security-crypto 依赖的 Tink 引用了编译期注解（运行时不打包），忽略缺失类警告
-dontwarn com.google.errorprone.annotations.**
