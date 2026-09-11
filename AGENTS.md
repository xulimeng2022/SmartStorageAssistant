# 项目指令

## 知识库与必读顺序

- 项目「是什么」（版本/需求/架构/模块/环境/发布）一律以 `docs/project/` 知识库为准，代码与配置是最高优先级事实来源。
- 新会话 / 新 Agent 开始任务前按顺序读取（按需，不必读完所有文档）：
  1. 本文件（协作规则）
  2. `docs/project/00-项目索引.md`（项目一句话、模块导航、文档入口）
  3. `docs/project/01-需求与范围确认.md`（做什么/不做什么/如何验收）
  4. `docs/project/02-系统架构.md`（分层与模块边界）
  5. `docs/project/modules/Mxx-*.md`（当前任务对应模块：负责/不负责/代码位置）
  6. `docs/project/04-任务与验收清单.md` 与 `docs/project/tasks/`（当前任务卡，如有）
  7. 需要当前版本 / 测试 / lint / 待验收状态时：`docs/project/PROJECT_STATUS.md`
- `docs/reference/ai-project-template/` 是原始通用模板，只作结构/方法论参考，**不是项目事实**，禁止改动。

## 协作规则（强制）

- 需求不明确 → 不猜，先列「待确认」；事实先从代码/配置验证再记录。
- 一个任务只改变一个可以说明的主要可观察行为；Bug 先建立最小复现。
- 适合自动化测试的行为优先测试先行；测试 seam 已由任务卡或主控明确测试目标、模块与验收标准时，直接执行，不重复请求确认。
- 如果测试 seam 会影响模块接口、架构或其他模块，模块 Agent 先报告主控 Codex，不直接跨模块决定。
- 主控 Codex 在已有需求、项目规则和用户授权范围内能够明确判断时，可以直接决策并继续推进。
- 只有涉及需求变化、重大架构取舍、不可逆风险、明显扩大任务范围，或超出已有用户授权时，才由主控向用户确认。
- 纯文案、纯样式、文档、配置、格式调整，以及无法合理自动化测试的任务，不强制 TDD。
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

## 当前状态入口

- 当前版本、开发阶段、测试与构建状态、lint 现状、待用户验收项与已知问题：统一见 `docs/project/PROJECT_STATUS.md`。
- 该文件由主控 Codex / Release 流程维护，其余 Agent 只读；不要在 AGENTS.md 内嵌会过期的数字。

## 文档同步规则（强制）

- 修改实际用法、接口、架构、模块边界、构建方式或长期约定后，必须检查相关项目文档是否需要同步。
- 模块 Agent 只负责更新自己任务与职责范围内的模块文档和任务记录。
- 如果改动需要更新 `README.md`、`AGENTS.md`、`PROJECT_STATUS.md`、项目索引或其他中央文档，模块 Agent 不直接修改，应向主控 Codex 报告需要同步的内容。
- `AGENTS.md`、`PROJECT_STATUS.md` 以及跨模块公共规则 / 状态原则上由主控 Codex 统一维护。
- 主控完成集成后，负责检查并同步受影响的 README、AGENTS 与 `docs/project/` 中央文档。
- 更新完成后，应明确说明同步了哪些文档以及原因。

## 版本号规则（强制）

- 日常开发（新增功能、Bug 修复、重构、UI 与资源、配置、依赖变更）不得自动修改 `versionCode` / `versionName`；避免多 Worktree 同时改 `app/build.gradle.kts` 产生无意义冲突。
- 只有 Release 阶段或用户明确要求时才更新版本号；发版流程与版本历史见 `docs/project/09-发布与版本.md`。
- 只有主控 Codex 与 Release 流程负责版本号；模块 Agent、Review Agent、测试 Agent 一律不得改动。

## Multi-Codex 协作原则

> 中央规则只定义长期角色、权限和协作边界；详细流程放在 WORKFLOW.md、Skill 和 Task Card 中。

### Coordination Entry

- 长期角色：主控（Coordinator）、Data、AI、UI；Test / Review 按任务临时启动，不建立长期 State。
- 开始工作前按 `WORKFLOW.md` 的 Lightweight Resume 按需读取对应真相源：
  - 项目硬规则 / Git 安全 / 用户审批门禁：`AGENTS.md`
  - Ownership：`docs/coordination/OWNERSHIP.md`
  - Workflow：`docs/coordination/WORKFLOW.md`
  - 当前项目状态：`docs/project/PROJECT_STATUS.md`
  - 角色恢复快照：主控（Coordinator）→ `docs/coordination/Coordinator.md`，Data → `docs/coordination/Data.md`，AI → `docs/coordination/AI.md`，UI → `docs/coordination/UI.md`
  - 正式任务状态、验收与 TDD 记录：`docs/project/04-任务与验收清单.md`、`docs/project/05-单任务卡-TDD.md` 与 `docs/project/tasks/`
  - Git HEAD / branch / commit / worktree：Git
- 各真相源职责分离，不要求每次全文读取 WORKFLOW。
- 同一事实在不同真相源冲突时，不自行猜测、覆盖或静默选择；停止相关修改并报告主控（Coordinator）。

### 当前阶段角色工作区

- 当前阶段的长期角色工作区为主控（Coordinator）、Data、AI、UI。Test 与 Review 当前按任务临时启动。
- 后续可根据项目架构和规模经正式决策调整角色结构，不得由 Agent 擅自新增、删除或改变长期角色职责。
- 永久 Worktree 是角色工作空间，不是一个功能一个 Worktree。当前四个长期角色从同一冻结基线创建，UI / Data / AI 不构成父子分支关系。
- Worktree ID、路径等运行时动态发现，不写死；Test / Review 当前不建立永久 Worktree。

### 主控（Coordinator）

- Coordinator 是可由新聊天接力的长期角色，不以某一个聊天窗口作为唯一状态源；状态从 Skill、项目文档、Git 状态、Task Card 与 handoff 恢复。
- 使用独立永久 Worktree，负责拆任务、划定边界、接收模块 commit / handoff、安排 Review / Test、处理冲突与最终 Integration。
- 可以读取其他角色结果，但默认不直接承担所有业务模块开发。
- 在项目规则规定的授权和验收条件满足后，可以执行 merge；push、正式 release、正式版本号变更仍需明确授权。
- 维护 `docs/project/PROJECT_STATUS.md`，处理跨 Worktree 状态与规则同步。

### 模块 Agent（Data / AI / UI）

- 对自己 Ownership 范围内的文件具有正常任务写权限。
- 对其他模块默认只读，不得擅自跨模块修改。
- Coordinator 或正式 Task Card 可临时授予指定文件、共享 contract 或明确范围的写权限；授权必须说明原因和当前写入者。
- 共享文件遵循 Single Writer Principle。同一时刻只允许一个明确 Agent 拥有写权限；多个角色需要修改时，由 Coordinator 重新划分所有权、安排串行，或先完成 contract 修改再通知依赖方。
- 可在授权范围内修改文件和运行测试、检查 diff，并在任务满足验收条件后创建自己的任务 commit，输出 commit hash 与 handoff。
- 默认不得 merge 到其他长期分支、push、release、修改正式版本号、删除其他 Worktree 或操作其他 Agent 的分支。
- 发现跨模块问题时报告 Coordinator，不擅自扩大职责。

### Review / Test

- Review 默认只读，检查缺陷、规格符合性、回归风险与测试覆盖，输出可执行 findings。
- Test 按 Task Card 验收标准设计测试、复现问题、运行验证并输出证据。
- Test / Review 当前按任务临时启动，不建立永久 Worktree；除用户或 Coordinator 明确授权外，不修改业务实现。
