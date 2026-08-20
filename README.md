# 智能收纳助手

一款用于记录和管理家中物品存放位置的 Android 应用：物品清单 + 照片附件 + AI 智能解析 + 语义搜索 + 回收站，功能完整。

## 功能特性

### 物品管理
- 首页实时展示物品清单（Room + Flow，按最后修改时间倒序），区分「从未添加」与「搜索无结果」两种空状态
- 添加 / 编辑物品表单：物品名（必填）、存放地点、备注，编辑页基于工作副本，未保存退出不落库
- 重复物品检测：保存时按名称查重（忽略大小写），弹窗选择「更新旧记录 / 新建记录 / 取消」
- 物品详情页：照片横向滚动 + 全屏预览、继续加图、编辑、删除
- 删除走软删除：移入回收站（数据与照片保留），首页 Snackbar 支持一键撤销
- 未保存修改拦截：编辑页 / 设置页有未保存修改时，切换底部 Tab 先弹确认框

### 搜索
- 关键词模糊搜索：按名称 / 地点 / 备注双向包含匹配（存储值包含关键词，或关键词包含存储值，如「车里」与「车里面」互相命中）
- AI 语义搜索：把整句自然语言（可键盘语音输入）解析为名称 / 地点 / 描述三字段，按三字段精确筛选
- 键盘语音搜索：点 🎤 聚焦搜索框并调起输入法语音
- 首次聚焦搜索框弹出「搜索小贴士」，可勾选「不再提示」，设置页可重新开启

### AI 智能解析（大模型）
- 添加页：用手机键盘语音录入口语描述，点「智能解析」由大模型自动提取并填入物品名 / 存放地点 / 备注
- 免费模式：内置硅基流动（SiliconFlow）Qwen2.5-7B-Instruct，开箱即用、无需任何配置
- 自定义模式：支持 DeepSeek、通义千问、OpenAI、智谱AI、Moonshot、百川、MiniMax、Anthropic、Google 九种预设 + 自定义，可配置 Base URL / 模型版本 / API Key（任意 OpenAI 兼容接口）
- API Key 使用 EncryptedSharedPreferences（AES-256-GCM）加密存储，各预设独立保存，支持显示 / 隐藏
- 免费模式超时弹窗引导切换到自定义模式；解析失败自动降级（提示 + 原文搜索），不阻塞手动填写

### 照片附件
- 最多 9 张：相册多选 / 拍照（FileProvider + 相机运行时权限）
- 自动压缩（1080px 宽 / JPEG 80%）、按 EXIF 方向修正，保存在应用私有目录 `filesDir/item_images/`
- 详情页大图预览；永久删除 / 清空回收站时同步清理照片文件

### 回收站
- 软删除：仅写入删除时间戳，数据与照片保留，可一键恢复
- 永久删除 / 清空回收站：物理删除记录并清理关联照片文件（需二次确认）

### 设置 / 关于 / 捐赠
- 设置页：AI 智能解析配置（免费 / 自定义模式、预设切换、模型版本下拉、API Key 显隐）、输入文字颜色、搜索小贴士开关、回收站入口
- 关于页：版本号、版权信息、联系方式（QQ / 微信 / GitHub / 个人网站，支持跳转 / 复制）
- 捐赠页：0.66 / 2 / 5 / 20 / 66 / 99 元档位，支付宝 / 微信收款码，可保存到系统相册

### 交互细节
- 自定义底部导航（固定高度，规避部分机型 M3 NavigationBar 高度测量异常把内容区挤为 0）
- Emoji 彩蛋：主要按钮点击弹出随机 Emoji 飘动动画（最多 3 个并发，动画结束自动回收）
- 立体按压按钮动效（缩放 + 阴影变化）

## 技术栈

- 语言：Kotlin（100%）
- UI：Jetpack Compose（Material 3，Compose BOM 2024.12.01）+ Material Icons Extended
- 架构：MVVM + Clean Architecture（UseCase + Repository 模式）
- 本地数据库：Room（SQLite，当前版本 v4）
- 依赖注入：Hilt（含 Hilt Navigation Compose）
- 异步：Kotlin Coroutines + Flow
- 网络：OkHttp（大模型 HTTP 请求，7 秒超时用于免费模式超时引导）
- 配置存储：DataStore Preferences（AI 配置）+ EncryptedSharedPreferences（API Key 加密）
- 图片加载：Coil
- 页面路由：自定义 `sealed class Screen` + 状态切换（底部导航为自实现，未使用 NavHost）

## 包结构

```
com.example.smartstorage
├── data                        # 数据层：Room / 图片存储 / 偏好 / 远程 LLM / 仓库实现 / 映射
│   ├── local
│   │   ├── entity              # ItemEntity（物品表）
│   │   ├── dao                 # ItemDao（含软删除 / 搜索 / 回收站查询）
│   │   ├── converters          # List<String> 与 JSON 数组互转（多图）
│   │   ├── image               # ImageStorage（照片压缩 / EXIF 修正 / 删除）
│   │   ├── prefs               # SettingsRepository（AI 配置）+ AppPreferencesRepository（外观）
│   │   └── AppDatabase         # Room 数据库（v1→v4 显式迁移）
│   ├── mapper                  # 实体与领域模型互转
│   ├── remote.llm              # LlmClient（OpenAI 兼容接口调用 / JSON 容错解析）
│   └── repository              # ItemRepositoryImpl
├── domain                      # 领域层：模型 / 仓库接口 / 用例
│   ├── model                   # Item
│   ├── repository              # ItemRepository 接口
│   └── usecase                 # 增删改查 / 搜索 / 回收站 / 观察等用例
├── di                          # Hilt 模块（DatabaseModule / RemoteModule / RepositoryModule）
└── presentation                # 表现层
    ├── MainScreen.kt           # 底部导航 + 页面容器（自定义路由切换）
    ├── navigation              # Screen 路由定义
    ├── home                    # 首页（清单 / 搜索 / AI 语义搜索）
    ├── add                     # 添加 / 编辑（语音描述 + AI 解析 + 多图）
    ├── detail                  # 物品详情（大图预览 / 照片管理）
    ├── trash                   # 回收站（恢复 / 永久删除 / 清空）
    ├── settings                # 设置（AI 配置 / 外观）
    ├── about                   # 关于（版本 / 联系方式）
    ├── donate                  # 捐赠（多档位收款码）
    ├── common                  # 通用组件（AnimatedButton / EmojiEffect / 对话框）
    └── theme                   # Material 3 主题
```

## 数据模型（items 表）

| 字段 | 说明 |
| ---- | ---- |
| id | 唯一 ID（自增主键） |
| name | 物品名称 |
| location | 存放地点 |
| description | 详细描述 |
| image_path | 照片附件绝对路径列表（JSON 数组字符串，可为空） |
| created_at | 创建时间（毫秒时间戳） |
| updated_at | 最后修改时间（毫秒时间戳） |
| deleted_at | 删除时间（毫秒时间戳）；null 表示正常，非 null 表示在回收站 |

数据库当前版本 v4，通过显式迁移升级（v1→v2 加 `image_path` 列、v2→v3 加 `deleted_at` 列、v3→v4 单图转多图 JSON 数组），迁移失败时兜底破坏性重建。

## AI 配置说明

- 免费模式：内置硅基流动免费模型，开箱即用；API Key 在构建时从根目录 `local.properties` 的 `SILICONFLOW_API_KEY` 注入（未配置也能正常编译，App 内会提示切换到自定义模式）。
- 自定义模式：在设置页选择预设（或自定义）→ 填 Base URL / 模型版本 / API Key → 保存；API Key 加密存储，切换预设时各预设配置相互独立。

## 权限

| 权限 | 用途 |
| ---- | ---- |
| INTERNET | 大模型 HTTP 请求（普通权限，声明即可） |
| CAMERA | 拍照（运行时动态申请） |
| WRITE_EXTERNAL_STORAGE / READ_EXTERNAL_STORAGE | 保存收款码到相册（仅 Android 12 及以下需要，`maxSdkVersion=32`） |
| RECORD_AUDIO | Manifest 中保留声明（当前语音输入走手机键盘自带语音，代码未主动申请） |

## 运行方式

1. 使用 Android Studio（Ladybug 或更新版本）打开项目根目录。
2. 等待 Gradle 同步完成（首次会自动下载依赖，需要联网）。
3. 如未自动生成 `local.properties`，请确认 `SDK Location` 指向本机 Android SDK；如需免费模式 AI 解析，可在其中追加 `SILICONFLOW_API_KEY=你的Key`。
4. 选择 `app` 运行配置，连接设备或模拟器后点击运行。

> 也可在命令行执行 `./gradlew :app:assembleDebug` 构建。

## 版本

- 当前版本：2.7.8（versionCode 51）
- minSdk 26 / targetSdk 35 / compileSdk 35，Java 17，AGP 8.7.3，Kotlin 2.0.21