# T-024 Coordinator 会话与 Worker Thread 生命周期规则落地

## Multi-Codex Metadata

- Task ID: T-024
- Owner: 主控（Coordinator）
- Risk: Low
- Source Branch: `codex/integration`
- Contract Change: 有（会话 / 线程生命周期与路由契约，见下方接口契约）
- Handoff Target: 无
- Routing Decision: COORDINATOR_OWNED_LOCAL
- Thread Decision: 不适用（本任务不投递永久角色 Worker Thread）
- Selected Thread Domain: 不适用
- Resume Mode: 不适用
- Thread Health Evidence: 不适用
- Fallback: 无

## Task Boundary

- 所属范围：全局 `multi-codex-coordinator` Skill（含 references）、`docs/coordination/**`、`docs/project/**` 协调与任务文档。
- 目标行为：把 Coordinator 对话生命周期与 Worker Thread REUSE-first 路由固化为可执行规则，并替换「新 Task ID → 新建线程」的旧默认。
- 禁止修改：App 业务逻辑、资源、Gradle、数据库、版本号、Release 产物、Tag、永久工作树结构与 `OWNERSHIP.md`。
- Single Writer：Registry 段 Writer 仍为对应长期角色；Coordinator 只读 Registry，只写 Task Card 与协调文档。

## Baseline

- 工程工作区：`codex/integration@5e417bc`。
- App 版本：`1.2.0 / versionCode 6`（本任务不改动）。
- 全局 Skill：`C:\Users\XU\.codex\skills\multi-codex-coordinator`（仓库外，不入 Git）。

## Deliverables

- 全局 Skill：`SKILL.md`（新增第 10 节对话生命周期、第 4 节 REUSE-first 线程策略）、`references/task-card.md`、`references/coordination-patterns.md`、`agents/openai.yaml`。
- 项目规则：`docs/coordination/WORKFLOW.md` §3 Resume 分级、§5 Registry 交叉引用、§6 REUSE-first 路由、§16 Chat 生命周期。
- Role State Registry：`docs/coordination/UI.md`、`AI.md`、`Data.md`。
- 状态与任务记录：`Coordinator.md`、`RECOVERY.md`、`PROJECT_STATUS.md`、`04-任务与验收清单.md`、`tasks/README.md`。

## 接口契约

- **Thread Decision 的唯一任务级真相源是 Formal Task Card**；Worker Thread Registry 只记录线程长期状态（Role、Worktree / Project、Thread ID、Domain、Health、Wait / Block Reason、Last Task、Last Active、Handoff Reference），不重复存储 REUSE / NEW。
- Task Card 字段：`Thread ID`、`Thread Decision: REUSE / NEW`、`Selected Thread Domain`、`Resume Mode: HOT / WARM / COLD`、`Thread Health Evidence`、NEW 理由。
- **Hot Continue 依赖连续且稳定的执行上下文；一旦状态可能变化，自动降级为 Warm Resume**（不直接 Cold Start）。
- **BLOCKED 本身不是 NEW 的充分理由**：必须判断 Block Reason 与 Thread Health；外部依赖型阻塞保留原线程，线程自身异常才可 NEW 并写入证据。
- Thread Health：`HEALTHY` / `POLLUTED` / `UNRECOVERABLE` / `UNKNOWN`，与任务级 `BLOCKED` 语义分离。
- Coordinator Chat 与 Worker Thread 生命周期互相独立：主对话不自动创建，Worker Thread 仍可依据正式 NEW 条件自动创建。

## 被本次用户指示替换的历史规则

| 位置 | 被替换的现行规则 |
| --- | --- |
| `WORKFLOW.md` L150 路由前预检③ | 不存在时创建新任务线程 |
| `WORKFLOW.md` L151 路由前预检④ | 新任务线程必须绑定既有角色 Project（措辞调整，语义保留） |
| `WORKFLOW.md` L160 投递流程图 | 复用同 Task ID threadId，否则在目标 Project 创建新任务线程 |
| `WORKFLOW.md` L172 投递与回收 | 同一 Task ID 的返修优先续投原线程；新 Task ID 在目标永久 Worktree 中新建线程 |
| `SKILL.md` L95 §4 投递流程图 | 同 Task ID 复用 threadId，否则在目标 Project 创建新任务线程 |
| `SKILL.md` L108–109 §4 线程策略 | 同一 Task ID 的返修优先续投原线程；新 Task ID 在目标角色既有 Project 中创建新任务线程 |
| `references/task-card.md` L87–88 §4 使用规则 | 同一 Task ID 返修优先续投原线程；新 Task ID 可在既有永久 Project 中创建新线程 |
| `references/coordination-patterns.md` L68 路由循环 | 同 Task ID 复用线程或新建任务线程 |

替换依据：用户 2026-09-13 指示「Permanent Worker Thread Reuse」，Formal Task 是任务生命周期边界而非 Chat / Thread 生命周期边界。历史任务卡 `T-012` 中的同类表述属当时完成记录，保留不改。

## Verification

- 残留搜索：`新 Task ID` / `新建线程` / `新任务线程` / `创建新线程` / `一 Task` / `一线程` 在全局 Skill 与 WORKFLOW 中 0 命中。
- 口径检查：Thread Decision 单真相源、HOT 自动降级、BLOCKED ≠ NEW 在 Skill §4 / §10 与 WORKFLOW §3 / §6 / §16 表述一致。
- 纸面语义走查：REUSE 默认、Task Completed / commit / Build 不触发 NEW、实验隔离 NEW、外部依赖 BLOCKED 保留原线程、线程不可恢复才 NEW、Coordinator Chat 与 Worker Thread 互不误伤。
- Skill hash：`SKILL.md` SHA-256 `C71D737FFBC05481E49A6DCF709ACA1D339F0C09D26BD25F41E31A55A5BFD745`，与 `RECOVERY.md` 记录一致。
- App 业务代码 / Gradle / 数据库 / 版本号变更：NONE。
- Debug / Release 构建：NOT RUN（无代码变更）。Tag / Release：NOT RUN。

## Known Issues

- Worker Thread Registry 初始条目 Health 均为 `UNKNOWN`，threadId 来源为历史 Formal Task Card，待对应角色下次 Resume / Handoff 核实更新。
- 全局 Skill 位于仓库外，不受 Git 版本控制；其一致性依赖 `RECOVERY.md` 记录的 SHA-256 基线。
- Tag `v1.2.0` 与最终 APK 源码 tree 不一致，属 T-023 记录项，本任务不处理。

## Rollback

- 仓库侧：按文件级 `git diff` 回退本任务涉及的协调文档；不得使用 `git reset --hard`、`git checkout .` 或 `git clean`。
- 全局 Skill：从 Git 外的 Skill 备份恢复，或用本任务前的 `SKILL.md` SHA-256 `C75F79D77E44578B69BBF5DCD651C1D60A9CACC22C79716D9CBB576EDCB895B9` 校验历史版本。
- 回退后需同步 `RECOVERY.md` 的 Skill hash 基线。

## Final Report

T-024 已把 Coordinator 对话生命周期与 Worker Thread REUSE-first 路由固化为全局 Skill + 项目 WORKFLOW 两层规则，替换 8 处「新 Task ID → 新建线程」旧默认，并在 UI / AI / Data Role State 建立轻量 Worker Thread Registry。App 业务代码、Gradle、数据库、版本号与发布物 0 变更。