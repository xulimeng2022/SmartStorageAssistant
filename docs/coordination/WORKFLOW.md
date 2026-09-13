# Multi-Codex Workflow

> 本文件是智能收纳助手 Multi-Codex 长期协作流程的唯一主要真相源。
> 文件 Ownership 以 [OWNERSHIP.md](OWNERSHIP.md) 为准；本文件不复制完整 Ownership Map。

## 1. 真相源边界

| 信息类型 | 唯一主要真相源 | Writer |
| --- | --- | --- |
| 项目硬规则、Git 安全、审批门禁 | `AGENTS.md` | 主控（Coordinator） |
| 文件 Ownership 与 Single Writer | [OWNERSHIP.md](OWNERSHIP.md) | 主控维护规则，对应 Owner 执行 |
| Multi-Codex Workflow | 本文件 | 主控（Coordinator） |
| 全局项目状态 | `docs/project/PROJECT_STATUS.md` | 主控 / Release |
| 角色动态恢复状态 | `docs/coordination/<Role>.md` | 对应长期角色 |
| 正式任务状态、验收与 TDD 记录 | `docs/project/04-任务与验收清单.md`、`docs/project/05-单任务卡-TDD.md`、`docs/project/tasks/` | 主控分配，责任角色更新 |
| Git 代码、分支、提交事实 | Git branches / commits / worktrees | 执行对应任务的角色 |
| 可复用调度逻辑 | `multi-codex-coordinator` Skill 及 references | Skill 维护者 / 用户 |
| AI Bridge V1.2 外部传输协议（薄入口 / Plan Gate / Task Size / Knowledge） | `docs/coordination/AI_BRIDGE.md` + 全局 `ai-bridge` Skill | Coordinator |

Formal Task Card / 现有任务系统是任务生命周期、验收条件和任务结果的主要真相源。AI Bridge 是外部任务/状态传输层，不替代 Formal Task、Ownership、Single Writer、永久工作树优先或 Git Gate。Role State 只保存对应 Worktree / Chat 的恢复快照，不替代 Formal Task。
 V1.2 在此基础上增加 INDEX / CURRENT-STATE 薄入口、Task Size / Risk Floor、Knowledge 指针、可追溯 Writer 与不可变历史快照；CURRENT-STATE 只做派生汇总，执行 Gate 始终读取 TASK / PLAN / PLAN_REVIEW / STATUS 权威文件。

## 2. Baseline 术语

### Frozen Baseline

创建四个长期 Worktree 时使用的历史共同基准。Frozen Baseline 不等于当前 Stable Baseline。

### Integration / Coordination Candidate

`codex/integration` 中尚未进入 `main` 的协调基础设施、集成结果和 Stable Candidate。不得称为 Stable Baseline。

### Stable Baseline

只有 Integration Candidate 完成验证、获得用户批准并正式进入 `main` 后，新的 `main HEAD` 才能称为 Stable Baseline。Data / AI / UI 只能面向 Stable `main` 执行 Baseline Resync。

## 3. Lightweight Resume

永久 Worktree 中新建 Chat 时：

```text
确认 Worktree / Branch / HEAD
↓
读取 AGENTS.md
↓
读取 OWNERSHIP.md
↓
读取 WORKFLOW.md 必要部分
↓
读取本角色 State
↓
读取 PROJECT_STATUS.md 必要状态
↓
读取当前 Formal Task（如有）
↓
确认 Active Task 与 Thread Title 投影是否已同步
↓
git status
↓
查看最近相关 commit / Handoff
↓
输出恢复报告
↓
确认边界后继续
```

恢复报告至少包含：角色、Branch、Active Task、状态、Working Tree、最近 Handoff、Blocker、建议下一步。

- 输出恢复报告前不得修改业务代码。
- Active Task 已明确且发生任务级变化时，可按 `multi-codex-coordinator` Skill 至多执行一次 Thread Title 同步；失败不影响 Resume。
- 如果本角色尚未同步最新 Stable Baseline，先检查 Role State、Formal Task Card、Handoff 和 Git。
- 无明确原因时，停止并报告主控。
- 如果已有记录的 Active Task，且主控已明确允许该任务暂时保持旧 Stable Baseline，恢复报告标记 `STALE_BASELINE / AUTHORIZED`，可继续当前任务；不得自行 Resync。
- Task 完成或到达安全 checkpoint 后，由主控安排 Baseline Resync。
- Role State 不存在或状态冲突时，停止并报告主控。
- 不得因为新 Chat 重装 JDK / SDK、重下 Gradle、清 Gradle cache、大规模 clean、重新 clone、重新创建项目或重建已有环境。
- 只有实际确认缺失、损坏或版本不匹配后，才采取对应修复。

### Resume 分级（Hot / Warm / Cold）

| 模式 | 适用 | 行为 |
| --- | --- | --- |
| Hot Continue | 同一 Formal Task、同一 Worker Thread、执行连续、无已知外部状态变化 | 不重新初始化，直接利用当前上下文继续 |
| Warm Resume | 已有健康 Worker Thread 接收新的 Formal Task，或 Hot 因状态可能变化而降级而来 | 只核验必要变化：Worktree / branch、新 Task Card、Role State、Skill / AGENTS / WORKFLOW 关键更新、最近 Handoff 是否仍与工作树一致；不无条件重读全部身份文件、项目规则和代码 |
| Cold Start | 新 Worker Thread、原线程不可恢复、正式 NEW 决策 | 执行完整初始化与必要规则读取 |

- **Hot Continue 依赖连续且稳定的执行上下文；一旦状态可能变化，自动降级为 Warm Resume。** 触发情形：执行中断后恢复；branch / commit 已变化；Worktree 事实可能已变化；AGENTS / Skill / WORKFLOW 等关键规则更新；Role State / Handoff 已更新；其它足以影响当前判断的外部状态变化。
- HOT 降级只到 WARM，不直接 Cold Start；除非线程已不可恢复。
- 优先级：**上下文正确性 > 线程连续性 > Token 节省**。
- Warm Resume 不走 [RECOVERY.md](RECOVERY.md)；Recovery Runbook 仅在普通 Resume 失效时使用。

### Recovery Escalation

Machine / Team / Worktree disaster recovery and environment reconstruction follow [RECOVERY.md](RECOVERY.md). Normal Lightweight Resume remains the default for new chats; RECOVERY applies only when ordinary Resume is insufficient.

## 4. Role State 生命周期

- Role State 文件为：`Coordinator.md`、`Data.md`、`AI.md`、`UI.md`。
- Long-term Owner 分别为主控（Coordinator）、Data、AI、UI。
- Role State 只保留当前恢复快照，不作为正式任务生命周期真相源，不复制完整 Task Card。
- 任务开始时、Handoff 前、切换 Chat 前、Integration 前，责任角色更新自己的 Role State。
- 如果 Role State 与 Formal Task 状态表面不一致，结合 Formal Task Card、Git、Handoff 和实际 Working Tree 判断，不得盲目覆盖。

### Stable Baseline Finalization

新的 Stable Candidate 进入 `main` 前，如果同时满足：

- 对应 Task 已进入 `codex/integration`；
- Integration Gate 和必要验证已通过；
- Formal Task 已为 `DONE / 已验收`；
- 对应角色尚未开始新 Task。

主控可获得针对对应 Role State 的临时 Single Writer 授权，只允许将其更新为稳定恢复快照：

```text
Current Task: None
Status: IDLE
Last Handoff: 已完成 Task
Working Tree: expected clean / 待角色 Resume 验证
Next Step: 等待下一任务
```

该授权不改变 Long-term Owner，不允许修改对应角色业务代码，只用于 Stable Baseline Finalization，并在 main 更新和传播后自动结束。如果角色已开始新 Task，主控不得覆盖其 Role State。

## 5. Task Lifecycle

Multi-Codex 操作状态映射到现有正式任务体系：

| Multi-Codex 状态 | 04 / 05 任务状态 | 说明 |
| --- | --- | --- |
| `PLANNED` | 待确认 | 信息、依赖或范围未完全明确 |
| `READY` | 待开始 | 可开始，依赖与边界明确 |
| `IN_PROGRESS` | 进行中 | 正在实现或验证 |
| `BLOCKED` | 阻塞 | 可从任一活动状态进入，记录阻断与解除条件 |
| `READY_FOR_INTEGRATION` | 仍为进行中 | 仅作为 Role State、Handoff 和 Integration Gate 的操作性状态 |
| `DONE` | 已验收 | 已进入 `codex/integration` 且集成验证通过 |

`DONE / 已验收` 不等于 Stable Baseline，也不等于 Released。Stable 与 Release 状态由 Git `main` 和项目状态文档管理。

### Formal Task Status 与 Role State Status

- Formal Task Status：`PLANNED`、`READY`、`IN_PROGRESS`、`BLOCKED`、`READY_FOR_INTEGRATION`、`DONE`。
- Role State Status：`IDLE`、`IN_PROGRESS`、`BLOCKED`、`READY_FOR_INTEGRATION`。
- Role State 不使用 `PLANNED`、`READY`、`DONE`。
- 例如 Formal Task 为 `READY`、角色尚未实际开始时，Role State 仍可为 `IDLE`。
- 两者语义不同，但不建立第二套生命周期；Formal Task 仍以现有任务系统为准。
- Worker Thread Registry 的 `Wait / Block Reason` 复用本节 `BLOCKED` 与“阻断与解除条件”语义；Registry 的 `Health` 只描述线程自身可复用性，不新建第二套状态机。

### Thread Title 投影

- Thread Title 是当前 Active Task / Goal 的 UI 投影，不是 Task、Role State 或 Worker Thread Registry 之外的新真相源。
- Formal Task 使用 `<Task ID> <短任务名>`；无 Formal Task 时使用 `TMP-<主题>`；仅规划或讨论时使用 `DISCUSS-<主题>`。详细规则见全局 `multi-codex-coordinator` Skill。
- 仅在 Formal Task 绑定、Active Task ID 改变、主要 Goal 任务级变化、完成后正式承接新 Task、TMP / DISCUSS 升级或标题明显无意义时检查；Active Task 不变则不更新。
- 可用 app rename action 时，每次任务级切换最多调用一次；失败继续任务，不重试、不轮询、不新增标题专用模型调用。

## 6. Task 使用方式

### Permanent Worktree Dispatch

> 项目专属角色路由由本节定义；通用发现、投递、回收和 fallback 机制由 `multi-codex-coordinator` Skill 定义。Project、Worktree 路径、branch 占用和 threadId 必须运行时发现，不写死。
>
> **Coordinator Chat 生命周期切换与 Worker Thread 路由是两套不同机制。** Coordinator 主对话判定需要切换时只生成 Chat Handoff 并建议用户新开（见第 16 节）；UI / AI / Data Worker Thread 由 Coordinator 依据 Thread Decision 自行 `REUSE` 或 `NEW`。“Coordinator Chat 不自动新建”不得解释为“所有 Worker Thread 都禁止自动创建”。
>
> **REUSE 是默认路径，NEW 是需要明确证据和理由的例外路径。**

```text
Permanent Worktree（生命周期最长）
├── Worker Thread（跨多个 Formal Task 复用）
└── Formal Task（生命周期最短，是执行与验收边界）
```

| 逻辑角色 | 默认 Ownership | 永久 Branch | 默认路由 |
| --- | --- | --- | --- |
| Coordinator | 中央协调文档、App/root 集成、跨模块 DI/Bootstrap、必要 glue 与最终集成 | `codex/integration` | Coordinator Owned Local |
| UI | `presentation/**`、Compose、ViewModel、Navigation、Theme、UI 资源与显示层多语言 | `codex/ui` | Permanent Worktree |
| Data | Room、Entity、DAO、Migration、Repository 实现、持久化与通用 Data/Domain contract | `codex/data` | Permanent Worktree |
| AI | `data/remote/llm/**`、`data/remote/vision/**`、Provider、Prompt、Parser、Vision 与 AI-only 类型/测试 | `codex/ai` | Permanent Worktree |
| Test / Review | 按任务临时启动，不建立永久 Worktree | 任务来源分支 / diff | Temporary Role |

#### 路由前预检

1. 依据 [OWNERSHIP.md](OWNERSHIP.md) 判断任务属于 Coordinator Owned 还是明确的永久角色 Ownership；跨模块任务先按 Owner 拆分，共享 contract 先冻结。
2. 从实际 Codex Project、`git worktree list --porcelain`、Role State 和分支占用中唯一定位目标角色。发现多个候选、路径不符或 branch 不符时停止选择，不静默猜测。
3. 查询该永久 Worktree 在 Role State Registry 中登记的 Worker Thread，排除 `POLLUTED` / `UNRECOVERABLE` 与无法验证的 `UNKNOWN`；`Wait / Block Reason` 为外部等待的线程不排除。
4. 按 Domain、最近任务与上下文连续性选择最佳健康候选，检查 REUSE 条件，并把判断与证据写入本 Task Card。
5. 禁止为路由创建新 Worktree。任何 Worker Thread 都必须绑定既有角色 Project，并使用该 Project 的本地环境，确保执行目录就是原永久 Worktree。

#### 投递与回收

```text
Task Card（Thread Decision 唯一真相源）
↓
解析目标角色与实际 Project / Worktree / Branch
↓
查询 Role State 的 Worker Thread Registry
↓
REUSE：记录 Thread Decision + Health Evidence → Lightweight Resume（HOT；状态变化时降级为 WARM）
   NEW：写明 NEW 理由 → 创建 Worker Thread → Cold Start
↓
确认 threadId / hostId（clientThreadId 未就绪时不得声称投递成功）
↓
等待并读取执行结果
↓
核验目标 cwd、branch、Git commit 与文件变化
↓
更新 Registry（Last Task / Last Active / Handoff Reference；不记录 REUSE / NEW）
↓
Review / Test / Integration
```

- **Thread Decision 的唯一任务级真相源是该 Task Card**；Task Card 至少记录：Task ID、From、Target Role、目标 Project/Worktree/Branch、Goal、Scope、禁止范围、依赖、Acceptance、Required Output、threadId、Thread Decision、Selected Thread Domain、Resume Mode、Thread Health Evidence、Routing/Fallback。
- Worker Thread Registry 只记录线程长期状态：Role、Worktree / Project、Thread ID、Domain、Health、Wait / Block Reason、Last Task、Last Active、Handoff Reference；**不重复记录 REUSE / NEW**，Writer 为对应长期角色。
- `Coordinator.md` 只记录投递日志、Task Card 引用、Registry 引用与总体进展，不形成第二份 Thread Decision 真相源。
- REUSE 默认条件（满足多数即优先复用）：Owner 未变；Target Worktree 未变；工作领域连续或高度相关；旧线程健康；旧线程已读取的代码 / 规则 / 身份 / 项目状态仍然有效；当前上下文对新任务价值仍高；不存在 Single Writer、并发隔离或实验隔离要求。
- NEW 合法理由：工作阶段或领域重大切换；当前线程上下文价值显著低于约 30%；上下文污染、状态混乱或持续误判；需要实验隔离或正式允许的并发隔离；原线程自身异常或无法可靠恢复（不含外部依赖型 BLOCKED）。
- 禁止单独作为 NEW 理由：T-xxx 编号变化；上一个 Formal Task 已 COMPLETED；新 Task Card 已创建；已完成一次 commit；已执行一次 Build；当前线程已执行过若干任务；Token 数量较大。
- **BLOCKED 本身不是 NEW 的充分理由**：必须读取并判断 Block Reason 与 Thread Health。外部依赖、等待其它 Role / 用户 / 验收 / 外部资源 / Git / Release Gate 的阻塞保留原 Thread，依赖解除后优先 REUSE；只有线程自身异常、上下文损坏、状态持续混乱、反复误判事实、无法完成可靠 Lightweight Resume 或线程已无法恢复，才可作为 NEW 依据，并写入 `Thread Health Evidence`。
- **Hot Continue 依赖连续且稳定的执行上下文；一旦状态可能变化，自动降级为 Warm Resume。** 同 Task、同 Thread、执行连续且无外部变化才可 HOT；否则只核验必要变化后继续，不直接 Cold Start。
- 结果不能只信摘要；必须核验目标线程实际工作树、branch、commit、diff 与测试证据，再进入集成。

#### Fallback 与 Single Writer

- 永久工作树在发现、发送或等待阶段失败时记录：

```text
PERMANENT_WORKTREE_FALLBACK | task=<ID> | target=<Role> | stage=<resolve/send/wait> | reason=<原因> | next=<动作>
```

- `PERMANENT_WORKTREE_FALLBACK` 记录在对应 Formal Task Card；Lightweight Task 至少记录在 Coordinator 交付报告。不得静默 fallback。
- Coordinator Owned 工作可直接本地完成。永久角色 Ownership 的模块写操作，无论由 Coordinator 还是 Temporary Subagent fallback 执行，都必须先由主控明确授予临时 Single Writer，限定 Task、文件/范围和原因；没有授权时生成人工接力卡并停止。
- Temporary Subagent 只能作为只读辅助或永久工作树与本地执行都不可用后的最后手段，不得绕过文件 Ownership、Single Writer、测试与 Review，也不得用其结果冒充永久工作树投递证据。

正式任务与验收体系见：

- [任务与验收清单](../project/04-任务与验收清单.md)
- [单任务卡模板](../project/05-单任务卡-TDD.md)
- [tasks/ 目录说明](../project/tasks/README.md)

本文件不复制完整 Task Card 或 TDD 模板。

### Lightweight Task

适用于 Low Risk、单一 Owner、单一可观察行为，且无 contract、schema、安全或 Release 影响的小型修改。

- 可在 Role State 中记录目标、Scope、Acceptance 和 Validation。
- 不新建独立任务文件。
- 如需 Task ID，由主控按现有任务系统分配。

### Formal Task

适用于 Medium / High Risk，或涉及跨模块 contract、数据库、Migration、安全、Release、大型 AI pipeline、App bootstrap、Manifest 或大范围重构。

- 使用现有 `05` 模板并在 `04` 登记。
- Formal Task Card 还必须记录 Multi-Codex 所需信息：Owner、Risk、Source Branch、Commit / Commit Range、Contract Change、Cross-module Request、Execution Order 和 Handoff Target。
- Formal Task 存在时，Role State 只记录 Task ID、当前执行摘要、Risk、Scope、简要 Acceptance 和 Task Card 路径，不复制完整内容。

## 7. Cross-module Request

Cross-module Request 不建立独立全局编号池，不创建 `REQUESTS.md` 或全局 XMR 日志。

- 如需要编号，使用 `XMR-<TaskID>-<Sequence>`，例如 `XMR-T021-01`。
- 单个简单请求可直接作为 Formal Task Card 的 `Cross-module Request` 小节。

```markdown
### Cross-module Request

- Requester:
- Target Owner:
- Target File / Contract:
- Reason:
- Required Change:
- Affected Modules:
- Compatibility Risk:
- Suggested Order:
- Blocking: Yes / No
```

流程：

```text
模块发现跨边界需求
↓
提出 Cross-module Request
↓
主控确认是否必须修改 contract
↓
指定 Owner、Single Writer、范围和顺序
↓
对应角色修改
↓
Handoff
```

### Cross-owner Write Gate

以下情况必须使用 Formal Task Card + Cross-module Request + 主控确定的 Single Writer：

- 实际跨 Owner 修改其他 Long-term Owner 文件；
- contract 变化；
- schema 变化；
- 公共接口变化；
- Shared Touchpoint 的跨 Owner 写入；
- Shared Touchpoint 修改会改变其他模块可观察行为或 compatibility contract。

如果文件 Owner 自己修改、不改变公开 contract、不要求其他 Owner 写入且不影响其他模块兼容性，则仍按正常 Low / Medium / High Risk 规则决定 Task 和 Gate，不因 `Shared Touchpoint = Yes` 自动升级为 Formal Cross-module Task。

轻量 Cross-module Request 只适用于只读协调、咨询或不产生跨 Owner 写入的低风险确认。明确批准的一次性 Bootstrap 是受控例外，授权必须限定任务、范围和结束条件。

## 8. Handoff

```markdown
# Task Handoff

- Task ID / Status:
- Owner:
- Source Branch:
- Commit / Commit Range:
- Changed Files:
- What Changed:
- Why:
- Validation:
- Build Verification: Debug=PASS/FAIL/NOT RUN; Release=PASS/FAIL/NOT RUN; Final user-facing artifact=Release APK/Release AAB/Debug APK (explicit request only)/None
- Release Artifact Metadata (if generated):
- Contract Change:
- Cross-module Impact:
- Known Risk:
- Remaining Work:
- Knowledge Candidate: none | <Source ID, evidence, suggested topic>
- Knowledge Archive: updated <paths> | skipped <reason> | pending <reason>
- Next Consumer:
```

- 一个 Task 可以包含一个或少量连续、目的明确的 commits；每个 commit 只承担一个清晰逻辑目的，不得混入其他 Task。
- 小型 Low Risk Task 通常一个 commit 即可。
- Handoff 不复制完整 Chat、日志或源码。
- 未完成时只能使用 `PARTIAL`、`BLOCKED` 或 `WIP`，不得声明 `READY_FOR_INTEGRATION`。
- 轻量 Handoff 可记录在 Role State；正式 Handoff 同时记录在 Formal Task Card。
- UI / Data / AI 可为 Release-only 问题自行执行 Debug 或 Release 构建验证；Release 验证不等同于正式交付或发布产物。
- Coordinator 决定何时生成正式 Release、最终用户交付物及发布阶段协调；模块角色不得自行把诊断构建标记为正式交付物。
- `Build Verification` 未执行时明确写 `NOT RUN` 并说明原因；生成 Release APK/AAB 时，`Release Artifact Metadata` 至少记录版本、类型、路径、SHA-256、签名状态和分发渠道。

## 9. Test / Review

| 风险 | 典型情况 | 最低要求 |
| --- | --- | --- |
| Low | 文案、局部样式、无 contract 的单文件调整 | 模块自测 + 主控检查 |
| Medium | 一般业务逻辑、Repository 调用、ViewModel 行为、AI Parser | 模块自测 + Test 或 Review 至少一个 |
| High | Database / Migration、用户数据、API Key / Security、Release、跨模块 contract、大型 AI pipeline、App bootstrap、Manifest、大范围重构 | Test + Review + 主控 Integration Gate |

- Test / Review 是临时角色，不建立永久 Worktree。
- Test 输出环境、命令、PASS / FAIL、证据和未覆盖风险。
- Review 输出 blocker / important / optional findings；Review 默认只读。
- Data Safety / Migration 任务必须包含对应 Owner、专项测试、Test、Review 和主控 Gate。

## 10. Integration Gate

进入 `codex/integration` 前至少确认：

- Task = `READY_FOR_INTEGRATION`；
- Source Branch、Commit 或 Commit Range 明确；
- 整个 commit set 完整，且不混入其他 Task；
- Ownership 和授权范围符合；
- Acceptance Criteria 完成；
- 必要 Test / Review 完成，blocker 已闭环；
- Contract Change、Dependency 和 Remaining Work 已记录；
- WIP commit 不进入 Integration。

默认使用 `git merge --no-ff` 保留任务边界。只有在分支混入无关工作、只需搬迁单个独立提交或实验成果需要拆分时，才考虑 cherry-pick；主控必须记录源 commit、原因和依赖顺序。

## 11. Main Gate

`main` 只接受已经验证的 Stable Candidate：

- 来自 `codex/integration`；
- Integration Gate 与集成后验证通过；
- 无未解决 blocker；
- 本次变更相关的中央状态、长期 contract、任务状态和用户可见版本信息保持一致；
- Release、正式版本号、Migration、高风险用户数据变更获得用户明确授权（如适用）。

不要求每个 Low Risk 修改机械更新 `PROJECT_STATUS.md`；只有真正相关时才更新。模块长期分支不得直接进入 `main`。

## 12. Baseline Resync

新的 Stable Candidate 进入 `main` 后，新的 `main HEAD` 成为 Stable Baseline。

角色只有同时满足以下条件，才允许执行：

```text
git merge --ff-only main
```

- Working Tree clean；
- 没有未集成 commit；
- 无未完成 Task；
- 当前 commit 已全部进入 `main`；
- 不存在潜在冲突。

不满足时不得自动执行 `reset --hard`、`rebase`、覆盖或删除修改。由主控判断等待、先完成集成或安排受控同步。

## 13. Central Rule Propagation

```text
Phase 3A / 3B / 3C 完成
↓
Integration Stable Candidate 验证
↓
用户批准
↓
进入 main
↓
main 成为新的 Stable Baseline
↓
Data / AI / UI 安全执行 ff-only main
```

不得直接把 `codex/integration` merge 到 Data / AI / UI。中央规则只通过 Stable `main` 传播。

## 14. Experiment Worktree

- 命名：`codex/exp/<feature-name>`，Worktree 路径动态发现。
- 默认从当前 Stable `main` 创建。
- 不默认从 Dirty Data / AI / UI、历史 `10ba` / `baee` 或半完成分支派生。
- 如必须依赖未进入 `main` 的 commit，记录 Base Commit、Dependent Task、Dependent Role 和 Reason。
- 实验成功：自测 → Review / Test → 主控裁定正式 Ownership → 拆成正式 Task → Integration → main。
- 实验失败：记录必要结论 → 确认无重要未保存工作 → 删除临时 Worktree 和实验分支。
- 不默认把整个实验分支 merge 到 `main`。

## 15. Git Discipline

- Commit：一个 commit 一个清晰逻辑目的，不混入其他 Task。
- WIP：仅用于切换 Chat 或保存高风险现场，必须标记 `WIP:`，不得进入 Integration。
- Merge：角色分支 → `codex/integration` 默认 `--no-ff`；`main` 只接受经过 Gate 的 Stable Candidate。
- Cherry-pick：只作为受控例外，并由主控记录原因和依赖。
- Branch：长期角色分支长期保留；实验分支临时存在。
- Conflict：停止自动处理，保护双方修改，由主控确认 Ownership 和 Single Writer 后处理。
- Push、Release、版本号和不可逆操作继续服从 `AGENTS.md` 的用户授权规则。

### Module-to-module merge

- Data / AI / UI 长期角色分支默认不得直接互相 merge。
- 跨模块任务通过：各角色完成自己的 Task → Handoff → 主控 → `codex/integration`。
- 只有用户明确批准的特殊恢复或 Git 操作才允许例外。

### Integration → main

- Stable Candidate 进入 `main` 时，如果 Git 拓扑允许，默认优先执行 `git merge --ff-only codex/integration`。
- 如果无法 fast-forward，停止并检查 `main` 与 `codex/integration` 为什么出现 divergence。
- 不得为了完成流程自动创建 merge commit、rebase、reset 或 force；由主控报告原因并等待明确决策。
- `main` 更新后成为新的 Stable Baseline。

## 16. Chat 生命周期

本节只治理 **Coordinator 主对话**；UI / AI / Data Worker Thread 的复用与新建见第 6 节，两者互相独立。

### 三级生命周期

```text
Permanent Worktree（生命周期最长）
├── Worker Thread（跨多个 Formal Task 复用）
└── Formal Task（生命周期最短）

Coordinator Permanent Worktree
├── Coordinator Chat A（一个主要阶段）
├── Chat Handoff
└── Coordinator Chat B（下一阶段）
```

> 工作树是长期角色。Chat / Thread 是阶段性上下文容器。Formal Task 是执行与验收边界，而不是新建 Chat / Thread 的理由。

### 继续当前对话（默认）

默认继续当前对话。满足多数条件时继续：任务与本对话核心目标一致；仍处于同一版本、阶段或决策链；明显依赖前面的决策、约束或执行结果；上下文大部分信息对下一步仍有价值；属于已有任务的修复、验证、Review 或后续推进；对当前状态理解稳定。

### 评估新开对话

进入新的重大阶段；核心目标明显变化；处理新的版本、项目或独立问题域；历史信息大部分与下一阶段无关；旧计划、旧状态或废弃决策干扰判断；开始重复误判已完成任务或混淆分支 / 工作树；项目发生重大架构、协作流程或运行环境切换。

### 70% 上下文价值原则

- ≥ 70%：默认继续当前对话。
- 30%–70%：结合阶段是否变化判断。
- < 30%：优先新开对话。

只做工程语义判断，不精算 Token。

### 不得作为新开理由

Token 数量较大；对话持续时间很长；完成了一个普通子任务；某个 Worker Thread 刚返回结果；刚进行了一次 Build；刚出现一个新 Bug；仍明显属于同一阶段。

### 完整逻辑检查点

```text
功能达到 checkpoint
↓
必要验证
↓
更新 Role State
↓
commit
↓
Handoff
```

### 任务未完成但需要切换 Chat

```text
更新 Role State
↓
记录已完成、未完成、Working Tree、风险和下一步
↓
不强制 commit
↓
同一永久 Worktree 新建 Chat
↓
Lightweight Resume
↓
继续
```

### 新开 Coordinator Chat 的执行规则

1. 更新 Coordinator Role State；
2. 生成 Chat Handoff；
3. 给出新对话初始化依据；
4. 建议用户手动新开 Coordinator Chat；
5. **不替用户自动创建新的 Coordinator 主对话。**

Chat Handoff 最小字段：项目与版本、branch / commit、开发阶段、已完成任务、进行中任务、待办任务、已确认重要决策、已废弃方案、已知问题、工作树状态、重要限制与禁止事项、下一步建议、新对话必读事实源与路径。

Chat 结束不等于必须 commit。WIP checkpoint 是例外，不是默认流程。Chat Handoff 是会话级记录（落 Coordinator Role State），不是 Task Handoff（落 Formal Task Card），也不替代 RECOVERY.md。

## 17. AI Bridge V1.2 Context and Plan Gate

AI Bridge 是 Google Drive 外部传输层，不是第二套任务生命周期。Formal Task、Git、Ownership、Single Writer、永久工作树和本 WORKFLOW 仍是事实源。

### Context Entry

- 新会话按需读取全局 `AI-Bridge/INDEX.md`，再读取项目 `CURRENT-STATE.md`、活动 `TASK.md` 和当前阶段文件。
- `CURRENT-STATE.md` 是派生快照，不是 Gate 输入。Revision、SHA 或状态不一致时先标记 Stale，再重读权威活动文件、重新执行 Gate，最后重新生成快照。
- `history/**` 默认只读且不扫描；只在审计、回滚或历史查询时读取。

### Task Size and Risk Floor

- `SMALL`: 低风险、单一 Owner、单一可观察行为，无 contract/schema/安全/Release/跨模块影响。
- `MEDIUM`: 单模块多文件行为、局部架构或正式计划审核。
- `LARGE`: 工作流/规则、架构、数据库/Migration、共享 Contract/Schema、多 Worker、Release/Version、安全或高回滚风险。
- Hard LARGE Trigger 命中时，Minimum Task Size 固定为 LARGE，Plan Requirement 固定为 REQUIRED；不得只修改 Size 字段降级。
- Size 缺失的新任务默认 `MEDIUM / REQUIRED`；命中 Hard Trigger 时按 LARGE。

### Plan and Review

- Plan Mode 只调研、讨论和生成计划，不写 Bridge。
- `PLAN.md` 由 Coordinator 在发布命令后写入，Revision 与 Task ID 必须匹配。
- `PLAN_REVIEW.md` 的 Decision Source 必须是 ChatGPT/User。Coordinator 默认只读；只有用户明确授权代写并给出具体决定时，才可按 `File Writer=Coordinator (delegated)` 忠实持久化。
- `APPROVED + AUTHORIZED` 不等于执行；必须另有显式执行命令。

### Worker and Knowledge

- Worker 只消费 Coordinator 提供的已批准执行切片，不默认读取完整 Bridge 或历史。
- Worker 只提交 `Knowledge Candidate`，不直接写 `D:\Knowledge`。
- Coordinator 按 `knowledge-base` Skill 做语义去重、串行安全写入并在 REPORT 中记录 `updated | unchanged | none | pending`。
- Bridge 只保存 Knowledge 指针，不复制完整知识正文。

### Compatibility

- V1.0 / V1.1 任务继续可读，原审核和 Revision 不被重写。
- 旧任务完成、Tag、Release 和 Gate 证据不因 V1.2 升级自动变更。
- 不引入 MCP、Relay、watcher、polling、daemon 或自动执行。

## 18. Workflow Cost Guardrails

- 工作流新增、修改、自动化或规则升级前，轻量判断 `Time Cost / Performance Cost / Token Cost / Complexity Cost / Expected Benefit`。
- 默认优先低延迟、低复杂度、低额外推理与低 Token 方案；复用已有状态，不为了自动化程度无条件增加 watcher、daemon、网络请求或模型调用。
- 小改动一句话或内联说明；Medium / Large 变更才展开。成本判断不是新审批门禁，完整问题清单见全局 `multi-codex-coordinator` Skill reference。
