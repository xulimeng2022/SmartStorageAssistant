# 智能收纳助手

一款用于记录和管理家中物品存放位置的 Android 应用：物品清单 + 照片附件 + AI 智能解析（支持批量识别）+ 语义搜索 + 回收站 + 数据备份恢复，功能完整。

## 功能特性

### 物品管理
- 首页实时展示物品清单（Room + Flow，按最后修改时间倒序），区分「从未添加」与「搜索无结果」两种空状态
- 添加 / 编辑物品表单：物品名（必填）、存放地点、备注，编辑页基于工作副本，未保存退出不落库
- 重复物品检测：保存时按名称查重（忽略大小写），弹窗选择「更新旧记录 / 新建记录 / 取消」
- 物品详情页：照片横向滚动 + 全屏预览、继续加图、编辑、删除
- 删除走软删除：移入回收站（数据与照片保留），首页 Snackbar 支持一键撤销
- 未保存修改拦截：编辑页 / 设置页有未保存修改时，切换底部 Tab 先弹确认框

### 搜索
- 关键词模糊搜索：名称 / 地点 / 备注逐字模糊匹配（去掉空格后按字符顺序命中，如输入「红色 口红」或「红色口红」都能找到「红色的口红」，英文忽略大小写，并保留反向包含）
- AI 语义搜索：把整句自然语言（可键盘语音输入）解析为名称 / 地点 / 描述三字段，按三字段精确筛选
- 键盘语音搜索：点 ? 聚焦搜索框并调起输入法语音
- 首次聚焦搜索框弹出「搜索小贴士」，可勾选「不再提示」，设置页可重新开启

### AI 智能解析（大模型）
- 添加页：用手机键盘语音录入口语描述，点「智能解析」由大模型自动提取并填入物品名 / 存放地点 / 备注
- 批量识别：一次输入多条描述（用逗号、句号或分号分隔），自动拆分并弹出批量确认框；重复物品逐条询问「更新旧记录 / 新建记录 / 跳过」
- 免费模式：内置硅基流动（SiliconFlow）Qwen2.5-7B-Instruct，开箱即用、无需任何配置
- 自定义模式：支持 DeepSeek、通义千问、OpenAI、智谱AI、Moonshot、百川、MiniMax、Anthropic、Google 九种预设 + 自定义，可配置 Base URL / 模型版本 / API Key（任意 OpenAI 兼容接口）
- API Key 使用 EncryptedSharedPreferences（AES-256-GCM）加密存储，各预设独立保存，支持显示 / 隐藏
- 免费模式超时 / 解析失败时自动降级：本地按标点分割 + 正则解析，不阻塞手动填写

### 照片附件
- 最多 9 张：相册多选 / 拍照（FileProvider + 相机运行时权限）
- 自动压缩（1080px 宽 / JPEG 80%）、按 EXIF 方向修正，保存在应用私有目录 `filesDir/item_images/`
- 详情页大图预览；永久删除 / 清空回收站时同步清理照片文件

### 回收站
- 软删除：仅写入删除时间戳，数据与照片保留，可一键恢复
- 永久删除 / 清空回收站：物理删除记录并清理关联照片文件（需二次确认）

### 设置 / 关于 / 捐赠
- 设置页：AI 智能解析配置（免费 / 自定义模式、预设切换、模型版本下拉、API Key 显隐）、主题模式（跟随系统 / 浅色 / 深色，全局即时生效）、全局文字颜色（默认 / 纯色 / 渐变 + 预设 + HSV 色环，首页 / 详情 / 输入框实时生效）、数据备份与恢复（导出 ZIP / 导入覆盖或合并）、搜索小贴士开关、回收站入口
- 关于页：版本号、版权信息、联系方式（QQ / 微信点击复制，GitHub / 个人网站点击弹出应用选择器）
- 捐赠页：0.66 / 2 / 5 / 20 / 66 / 99 元档位，支付宝 / 微信收款码，可保存到系统相册

### 首次启动引导页
- 首次启动全屏展示 3 页引导（左右滑动 + 跳过），最后一页点击「开始使用」进入主界面，完成后不再出现

### 交互细节
- 自定义底部导航（固定高度，规避部分机型 M3 NavigationBar 高度测量异常把内容区挤为 0）
- 主 Tab 左右滑动切换（HorizontalPager）：首页 / 添加 / 设置三页可左右滑动，底部导航与滑动双向同步
- GitHub Star 提醒：历史累计添加数达到 5 / 15 / 30 件时弹出提醒，支持「稍后提醒」（24 小时后再次弹出）
- Emoji 彩蛋：主要按钮点击弹出随机 Emoji 飘动动画（最多 3 个并发，动画结束自动回收）
- 立体按压按钮动效（缩放 + 阴影变化）

## 技术栈

- 语言：Kotlin（100%）
- UI：Jetpack Compose（Material 3，Compose BOM 2024.12.01）+ Material Icons Extended
- 架构：MVVM + Clean Architecture（UseCase + Repository 模式）
- 本地数据库：Room（SQLite，当前版本 v4）
- 依赖注入：Hilt（含 Hilt Navigation Compose）
- 异步：Kotlin Coroutines + Flow
- 网络：OkHttp（大模型 HTTP 请求，7 秒超时，超时/失败自动降级本地解析）
- 配置存储：DataStore Preferences（AI 配置 / 引导页 / 外观 / Star 提醒）+ EncryptedSharedPreferences（API Key 加密）
- 图片加载：Coil
- 页面路由：自定义 `sealed class Screen` + HorizontalPager（主 Tab 滑动切换）+ 子页面覆盖层（未使用 NavHost）

## 包结构

```
com.example.smartstorage
├── data                        # 数据层：Room / 图片存储 / 偏好 / 远程 LLM / 备份 / 仓库实现 / 映射
│   ├── local
│   │   ├── entity              # ItemEntity（物品表）
│   │   ├── dao                 # ItemDao（含软删除 / 搜索 / 回收站 / 备份查询）
│   │   ├── converters          # List<String> 与 JSON 数组互转（多图）
│   │   ├── image               # ImageStorage（照片压缩 / EXIF 修正 / 删除）
│   │   ├── prefs               # SettingsRepository（AI 配置）+ AppPreferencesRepository（外观）+ OnboardingRepository（引导）+ ThemeRepository（主题/文字颜色）+ StarMilestoneRepository（Star 提醒）
│   │   ├── backup              # BackupRepository（数据导出 ZIP / 导入覆盖或合并）
│   │   └── AppDatabase         # Room 数据库（v1→v4 显式迁移）
│   ├── mapper                  # 实体与领域模型互转
│   ├── remote.llm              # LlmClient（OpenAI 兼容接口调用 / JSON 容错解析 / 批量解析 / 本地降级）
│   └── repository              # ItemRepositoryImpl
├── domain                      # 领域层：模型 / 仓库接口 / 用例
│   ├── model                   # Item
│   ├── repository              # ItemRepository 接口
│   └── usecase                 # 增删改查 / 搜索 / 回收站 / 观察等用例
├── di                          # Hilt 模块（DatabaseModule / RemoteModule / RepositoryModule）
└── presentation                # 表现层
    ├── MainScreen.kt           # HorizontalPager 主 Tab 滑动 + 子页面覆盖层
    ├── navigation              # Screen 路由定义
    ├── onboarding              # 首次启动引导页（3 页滑动 + 跳过）
    ├── home                    # 首页（清单 / 搜索 / AI 语义搜索）
    ├── add                     # 添加 / 编辑（语音描述 + AI 解析 + 批量识别 + 多图）
    ├── detail                  # 物品详情（大图预览 / 照片管理）
    ├── trash                   # 回收站（恢复 / 永久删除 / 清空）
    ├── settings                # 设置（AI 配置 / 主题模式 / 文字颜色 / 数据备份恢复）
    ├── about                   # 关于（版本 / 联系方式）
    ├── donate                  # 捐赠（多档位收款码）
    ├── common                  # 通用组件（AnimatedButton / EmojiEffect / 对话框 / LinkOpener）
    └── theme                   # Material 3 主题 + TextColorStyles（全局文字颜色渲染）
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

- 免费模式：内置硅基流动免费模型，开箱即用；API Key 在构建时从根目录 `local.properties` 的 `SILICONFLOW_API_KEY` 注入（未配置也能正常编译，App 内会提示切换到自定义模式）。支持单条与批量解析，多条描述用逗号、句号或分号分隔。
- 自定义模式：在设置页选择预设（或自定义）→ 填 Base URL / 模型版本 / API Key → 保存；API Key 加密存储，切换预设时各预设配置相互独立。
- 预设的 Base URL 与可选模型版本在代码 `SettingsRepository` 的 `LlmPreset` 中维护，可按实际可用的模型名自行修改。

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

## 签名与发布（Release APK）

- 签名配置在 `app/build.gradle.kts` 的 `release` 构建类型（已开启 R8 混淆与资源压缩），密钥信息不写死在代码里，从根目录 `local.properties` 读取（该文件已被 `.gitignore` 忽略，不会提交到 GitHub）：
  - `KEYSTORE_FILE`：密钥库文件路径，如 `D\:\\keystore\\smartstorage.jks`
  - `KEYSTORE_PASSWORD`：密钥库密码
  - `KEY_ALIAS`：密钥别名，如 `smartstorage`
  - `KEY_PASSWORD`：密钥密码
- 首次发版需用 `keytool` 生成正式密钥库（`.jks`），密钥文件与密码请妥善保管并备份，**严禁提交到 GitHub**。
- 配置好 `local.properties` 后执行 `./gradlew :app:assembleRelease`，签名 APK 产物位于 `app/build/outputs/apk/release/app-release.apk`。
- 完整的签名、构建与 GitHub Releases 发布流程见 `RELEASE_GUIDE.md`。

## 版本

- 当前版本：1.0.1（versionCode 2）
- minSdk 26 / targetSdk 35 / compileSdk 35，Java 17，AGP 8.7.3，Kotlin 2.0.21
