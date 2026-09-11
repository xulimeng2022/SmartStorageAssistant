# 项目指令

## 知识库与必读顺序

- 项目「是什么」（版本/需求/架构/模块/环境/发布）一律以 `docs/project/` 知识库为准，代码与配置是最高优先级事实来源。
- 新会话 / 新 Agent 开始任务前按顺序读取（按需，不必读完所有文档）：
  1. 本文件（协作规则）
  2. `docs/project/00-项目索引.md`（项目一句话、当前版本 1.2.0(5)、模块导航）
  3. `docs/project/01-需求与范围确认.md`（做什么/不做什么/如何验收）
  4. `docs/project/02-系统架构.md`（分层与模块边界）
  5. `docs/project/modules/Mxx-*.md`（当前任务对应模块：负责/不负责/代码位置）
  6. `docs/project/04-任务与验收清单.md` 与 `docs/project/tasks/`（当前任务卡，如有）
- `docs/reference/ai-project-template/` 是原始通用模板，只作结构/方法论参考，**不是项目事实**，禁止改动。

## 协作规则（强制）

- 需求不明确 → 不猜，先列「待确认」；事实先从代码/配置验证再记录。
- 一个任务只改变一个可以说明的主要可观察行为；Bug 先建立最小复现。
- 适合自动化测试的行为 → 先写失败测试（TDD 缝合点先与用户确认）；再最小实现，不顺手重构无关代码。
- 改完运行受影响测试（`.\gradlew.bat :app:testDebugUnitTest`）。当前路径含中文；若本机直接运行出现 classpath 编码导致的 `ClassNotFoundException`，复制到 ASCII 临时目录后运行，不要给 Gradle 增加 `-Dfile.encoding=UTF-8`。
- 长期稳定事实（模块边界/接口/状态/数据模型/架构决策/环境/发布规则）→ 回写 `docs/project/` 对应文档；临时日志、命令输出、真实数据、截图只留任务卡/Git，不写入长期文档。
- 敏感信息（API Key、签名密码、token、`local.properties` 值、真实账号）禁止出现在任何文档、日志或提交中。
- 任务完成后用一两句中文说明改了什么、为什么；并同步更新 README/AGENTS/知识库中受影响内容（见「文档同步规则」）。

## 不可破坏的行为约束（速查，细节见模块文档）

- 删除一律软删除（只写 `deleted_at`，数据与照片保留）；永久删除/清空才物理删除并清理照片，且需二次确认 → `modules/M01`、`M10`
- 首页列表按 `updated_at` 倒序；区分「从未添加」与「搜索无结果」→ `M08`
- 保存前按名称查重（忽略大小写）弹「更新旧记录/新建/取消」；批量逐条询问 → `M09`
- 编辑页基于工作副本，未保存退出不落库；编辑/设置页有未保存修改时切 Tab 先弹确认 → `M09`/`M07`
- 搜索：关键词逐字模糊（去空白按字符序匹配，覆盖名称/地点/备注，忽略 ASCII 大小写，保留反向包含）；AI 语义搜索把整句解析为名称/地点/描述三字段精确筛选 → `M01`/`M08`
- 照片最多 9 张，压缩（1080px 宽 / JPEG 80%）+ EXIF 修正，存 `filesDir/item_images/` → `M04`
- AI 解析失败两出口（去配置 API / 稍后）的完整语义与降级规则 → `modules/M03` 第 9 节
- 各预设 API Key 独立加密存储，支持显示/隐藏；免费模式 Key 仅从 `local.properties` 经 BuildConfig 注入 → `M06`
- 底部导航自定义实现（固定高度）+ 主 Tab 左右滑动双向同步；子页为覆盖层 → `M07`
- 多语言只本地化显示层；深色为固定微信式深灰色板（关闭动态取色）→ `M11`
- 临时 UI 消息（Toast / Snackbar / 弹窗提示）一律走“类型化 + 显示层解析”：业务层只产出资源 ID 与参数（`presentation/common/UiMessage.kt`），禁止在 ViewModel 或数据层用 `ApplicationContext.getString()` 提前拼好文案缓存 → `M03`/`M11`
- AI 图片理解默认关闭且必须显式同意；能力探测使用本地合成图，网络/鉴权/额度错误不得误判为模型不支持 → `M03`/`M11`
- 视觉能力探测/分析/复核的网络调用必须在 IO 线程执行；能力缓存只持久化 `SUPPORTED`，负结果仅在本次会话展示，用户主动开启时始终重新真实探测 → `M03`/`M06`
- 视觉功能只支持 OpenAI 兼容图片接口；免费模式当前为 `Qwen/Qwen3.5-4B`，仍需 `SILICONFLOW_API_KEY` → `M03`/`M06`
- 图片视觉索引是独立派生数据：`PENDING / PROCESSING / SUCCESS / FAILED / OUTDATED`；软删除保留，永久删除清理，失败不回滚物品或照片 → `M01`/`M04`
- 搜索先走本地索引，只有用户手动点击“AI 图片复核”才上传 Top-5；任何视觉失败必须退回本地结果 → `M08`
- 备份格式为 1.1，可携带正常物品的 `SUCCESS` 索引；旧 1.0 继续可导入 → `M05`
- Star 提醒：累计添加 5/15/30 弹窗，稍后 24h 重弹 → `M06`
- Emoji 彩蛋：主要按钮点击随机飘动（最多 3 并发，结束回收）→ `M12`

## 构建与发布速查（细节见 `docs/project/08-开发环境.md`、`09-发布与版本.md`）

- minSdk 26 / targetSdk 35 / compileSdk 35；Java 17；AGP 8.7.3 / Kotlin 2.0.21；WorkManager 2.10.5；AndroidX Hilt Work 1.2.0；单模块 `:app`。
- Debug：`.\gradlew.bat :app:assembleDebug`；Release：`.\gradlew.bat :app:assembleRelease`（产物 `app\build\outputs\apk\release\app-release.apk`）。
- `local.properties`（不入 Git）职责：SDK 路径、`SILICONFLOW_API_KEY`、Release 签名四键（未配签名则产出未签名 APK）；密钥严禁提交，流程见 `RELEASE_GUIDE.md`。
- 项目路径含中文：不要给 Gradle 添加 `-Dfile.encoding=UTF-8`（会破坏中文路径下单元测试），原因已记录在 `gradle.properties`。

## 当前项目状态

- 当前版本：`1.2.0 / versionCode 6`。
- Room：版本 5，新增 `image_ai_indices` 显式迁移。
- 自动化：71 个 JVM 单元测试通过，Debug 和 Release 构建通过；Release APK 以 v2 + v3 签名校验通过。
- Lint：仍有既有 Manifest 相机硬件声明错误，未在本轮处理。
- 真机：多语言、真实视觉 API、历史索引进度和 Top-5 流程仍待用户在手机验收。
- 百川最新模型 ID 无法从官方 API 文档零猜测确认，列表中明确标注“待官方确认”。

## 文档同步规则（强制）

- 当我要求新增功能、修改架构、变更配置或重构代码后，**必须同步检查并更新** `README.md`（面向人类的使用说明）、`AGENTS.md`（面向 AI 的协作规则）与 `docs/project/` 知识库（项目事实）。
- 如果改动涉及启动命令、目录结构、环境变量或核心依赖，必须同步更新上述文档。
- 更新完成后，需明确告知我已同步更新了哪些文档内容。

## 版本号自动递增规则（强制）

- **触发条件**：每当我通过对话要求你完成以下任一操作**并实际修改了项目文件**后，必须自动递增版本号：
  - 新增功能/模块
  - 修复 Bug
  - 重构代码
  - 修改 UI/资源文件
  - 更改配置文件（如 `build.gradle.kts`、`gradle.properties` 等）
  - 添加/删除依赖
- **例外情况**：仅修改文档（如 `README.md`、`AGENTS.md`、`docs/`）或仅做代码注释调整时，**不触发**版本号更新。
- **具体操作**：
  - 修改 `app/build.gradle.kts` 或 `gradle.properties` 中的 `versionCode`（整数，每次 +1）。
  - 是否同时递增 `versionName`（语义化版本）由你决定，建议至少递增补丁号（如 1.0.0 → 1.0.1）以保持可读性。
- **反馈**：更新完成后，必须在对话中明确告知我**旧版本号**和**新版本号**，以便我确认。
