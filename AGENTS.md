# 项目指令

## 项目概览
- Android 应用「智能收纳助手」：记录家中物品存放位置，支持物品清单 + 照片 + AI 解析 + 语义搜索 + 回收站 + 备份恢复。
- 当前版本：1.1.0（versionCode 4）

## 技术栈
- 语言：Kotlin（100%）
- UI：Jetpack Compose（Material 3，Compose BOM 2024.12.01）+ Material Icons Extended
- 架构：MVVM + Clean Architecture（UseCase + Repository）
- 数据库：Room（SQLite，当前版本 v4，显式迁移，失败兜底破坏性重建）
- DI：Hilt（含 Hilt Navigation Compose）
- 异步：Kotlin Coroutines + Flow
- 网络：OkHttp（LLM HTTP 请求，7 秒超时，超时/失败自动降级本地解析）
- 配置：DataStore Preferences（AI 配置/引导页/外观/Star 提醒）+ EncryptedSharedPreferences（API Key，AES-256-GCM）
- 图片：Coil
- 路由：自定义 `sealed class Screen` + HorizontalPager（主 Tab 滑动）+ 子页面覆盖层（未使用 NavHost）

## 构建环境
- minSdk 26 / targetSdk 35 / compileSdk 35
- Java 17，AGP 8.7.3，Kotlin 2.0.21
- 构建命令：`./gradlew :app:assembleDebug`
- 使用 Android Studio（Ladybug 或更新）打开；首次同步需联网
- 根目录 `local.properties` 需指向本机 SDK；免费模式 AI Key 通过 `SILICONFLOW_API_KEY=你的Key` 注入（未配置也能编译，App 内提示切换自定义模式）
- Release 签名：`app/build.gradle.kts` 的 release 构建类型从 `local.properties` 读取密钥（KEYSTORE_FILE / KEYSTORE_PASSWORD / KEY_ALIAS / KEY_PASSWORD），未配置则产出未签名 APK；密钥严禁提交到 GitHub，完整流程见 `RELEASE_GUIDE.md`
- 构建 Release APK：`./gradlew :app:assembleRelease`，产物 `app/build/outputs/apk/release/app-release.apk`

## 包结构
```bash
com.example.smartstorage
├── data                        # 数据层：Room / 图片 / 偏好 / 远程 LLM / 备份 / 仓库实现 / 映射
│   ├── local
│   │   ├── entity              # ItemEntity（物品表）
│   │   ├── dao                 # ItemDao（软删除 / 搜索 / 回收站 / 备份查询）
│   │   ├── converters          # List<String> 与 JSON 数组互转（多图）
│   │   ├── image               # ImageStorage（压缩 / EXIF 修正 / 删除）
│   │   ├── prefs               # SettingsRepository + AppPreferencesRepository + OnboardingRepository + ThemeRepository + StarMilestoneRepository
│   │   ├── backup              # BackupRepository（导出 ZIP / 导入覆盖或合并）
│   │   └── AppDatabase         # Room 数据库（v1→v4 显式迁移）
│   ├── mapper                  # 实体与领域模型互转
│   ├── remote.llm              # LlmClient（OpenAI 兼容调用 / JSON 容错解析 / 批量解析 / 本地降级）
│   └── repository              # ItemRepositoryImpl
├── domain                      # 领域层：模型 / 仓库接口 / 用例
│   ├── model                   # Item
│   ├── repository              # ItemRepository 接口
│   └── usecase                 # 增删改查 / 搜索 / 回收站 / 观察
├── di                          # Hilt 模块（DatabaseModule / RemoteModule / RepositoryModule）
└── presentation                # 表现层
    ├── MainScreen.kt           # HorizontalPager 主 Tab + 子页面覆盖层
    ├── navigation              # Screen 路由
    ├── onboarding              # 首次启动引导页（3 页滑动 + 跳过）
    ├── home                    # 首页（清单 / 搜索 / AI 语义搜索）
    ├── add                     # 添加 / 编辑（语音 + AI 解析 + 批量识别 + 多图）
    ├── detail                  # 物品详情（大图预览 / 照片管理）
    ├── trash                   # 回收站（恢复 / 永久删除 / 清空）
    ├── settings                # 设置（AI 配置 / 主题 / 文字颜色 / 备份恢复）
    ├── about                   # 关于（版本 / 联系方式）
    ├── donate                  # 捐赠（多档位收款码）
    ├── common                  # 通用组件（AnimatedButton / EmojiEffect / 对话框 / LinkOpener）
    └── theme                   # Material 3 主题 + TextColorStyles
```

## 数据模型（items 表）
| 字段 | 说明 |
| ---- | ---- |
| id | 唯一 ID（自增主键） |
| name | 物品名称 |
| location | 存放地点 |
| description | 详细描述 |
| image_path | 照片绝对路径列表（JSON 数组字符串，可为空） |
| created_at | 创建时间（毫秒时间戳） |
| updated_at | 最后修改时间（毫秒时间戳） |
| deleted_at | 删除时间（毫秒时间戳）；null=正常，非 null=回收站 |

迁移：v1→v2 加 `image_path` 列、v2→v3 加 `deleted_at` 列、v3→v4 单图转多图 JSON 数组；迁移失败兜底破坏性重建。

## 功能实现约束
- 删除一律软删除（只写 `deleted_at`，数据与照片保留），永久删除/清空回收站才物理删除并清理照片，需二次确认。
- 首页列表按 `updated_at` 倒序；区分「从未添加」与「搜索无结果」两种空状态。
- 重复物品检测：保存时按名称查重（忽略大小写），弹窗「更新旧记录 / 新建记录 / 取消」；批量识别时逐条询问。
- 编辑页基于工作副本，未保存退出不落库；编辑页/设置页有未保存修改时切换 Tab 先弹确认。
- 搜索：关键词逐字模糊匹配（去掉空格后按字符顺序匹配，覆盖名称/地点/备注，英文忽略大小写，保留反向包含）；AI 语义搜索把整句解析为名称/地点/描述三字段精确筛选。
- 照片最多 9 张，自动压缩（1080px 宽 / JPEG 80%）+ EXIF 方向修正，存 `filesDir/item_images/`；FileProvider + 相机运行时权限。
- 免费模式 LLM 内置 SiliconFlow Qwen2.5-7B-Instruct；解析失败自动降级为本地按标点分割 + 正则解析，不阻塞手动填写。
- 自定义模式预设：DeepSeek、通义千问、OpenAI、智谱AI、Moonshot、百川、MiniMax、Anthropic、Google + 自定义（Base URL / 模型 / API Key，OpenAI 兼容）。预设 Base URL 与模型版本在 `SettingsRepository.LlmPreset` 维护。
- 各预设 API Key 独立加密存储，支持显示/隐藏。
- 底部导航为自定义实现（固定高度，规避 M3 NavigationBar 高度测量异常）。
- 主 Tab 左右滑动切换（首页/添加/设置），底部导航与滑动双向同步。
- 界面语言：支持跟随系统 / 简体中文 / 繁體中文 / English。默认资源 values/ 为简体（系统语言不受支持时的回退），并维护 values-zh-rTW、values-en；选择经 `AppLanguage`（SharedPreferences）持久化，在 `SmartStorageApp`/`MainActivity.attachBaseContext` 同步应用，设置页切换后 `recreate()` 立即生效；语言与深浅主题独立、互不覆盖。只本地化显示层，用户数据与稳定标识、JSON 键、Llm 提示词不翻译。
- 深色模式：关闭 Android 12+ 动态取色，使用固定语义色板（微信式深灰层次，见 `Color.kt`/`Theme.kt`），所有页面走 MaterialTheme.colorScheme。
- 免费模式超时：读取超时 25s / 连接超时 7s；超时弹窗提供「本地识别继续（稍后配置）→ 用本地规则解析并继续当前任务」「去配置 API」「取消」三出口；同一页面会话内选择一次本地降级后不再重复弹框；降级只影响当前任务，不持久修改 AI 模式（改模式走设置页）。
- Star 提醒：累计添加 5 / 15 / 30 件时弹窗，「稍后提醒」24 小时后重弹。
- Emoji 彩蛋：主要按钮点击随机 Emoji 飘动（最多 3 并发，结束自动回收）。

## 权限
| 权限 | 用途 |
| ---- | ---- |
| INTERNET | 大模型 HTTP 请求（普通权限） |
| CAMERA | 拍照（运行时动态申请） |
| WRITE_EXTERNAL_STORAGE / READ_EXTERNAL_STORAGE | 保存收款码到相册（仅 Android 12 及以下，`maxSdkVersion=32`） |
| RECORD_AUDIO | 仅 Manifest 保留声明（语音走手机键盘自带，代码未主动申请） |

## 文档同步规则（强制）
- 当我要求新增功能、修改架构、变更配置或重构代码后，**必须同步检查并更新** `README.md`（面向人类的使用说明）和 `AGENTS.md`（面向AI的项目指令）。
- 如果改动涉及启动命令、目录结构、环境变量或核心依赖，必须同步更新上述两个文件。
- 更新完成后，需明确告知我已同步更新了哪些文档内容。

## 版本号自动递增规则（强制）
- **触发条件**：每当我通过对话要求你完成以下任一操作**并实际修改了项目文件**后，必须自动递增版本号：
  - 新增功能/模块
  - 修复 Bug
  - 重构代码
  - 修改 UI/资源文件
  - 更改配置文件（如 `build.gradle.kts`、`gradle.properties` 等）
  - 添加/删除依赖
- **例外情况**：仅修改文档（如 `README.md`、`AGENTS.md`）或仅做代码注释调整时，**不触发**版本号更新。
- **具体操作**：
  - 修改 `app/build.gradle.kts` 或 `gradle.properties` 中的 `versionCode`（整数，每次 +1）。
  - 是否同时递增 `versionName`（语义化版本）由你决定，建议至少递增补丁号（如 1.0.0 → 1.0.1）以保持可读性。
- **反馈**：更新完成后，必须在对话中明确告知我**旧版本号**和**新版本号**，以便我确认。
