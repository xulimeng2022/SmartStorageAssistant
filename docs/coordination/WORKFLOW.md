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

Formal Task Card / 现有任务系统是任务生命周期、验收条件和任务结果的主要真相源。Role State 只保存对应 Worktree / Chat 的恢复快照，不替代 Formal Task。

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
- 如果本角色尚未同步最新 Stable Baseline，先检查 Role State、Formal Task Card、Handoff 和 Git。
- 无明确原因时，停止并报告主控。
- 如果已有记录的 Active Task，且主控已明确允许该任务暂时保持旧 Stable Baseline，恢复报告标记 `STALE_BASELINE / AUTHORIZED`，可继续当前任务；不得自行 Resync。
- Task 完成或到达安全 checkpoint 后，由主控安排 Baseline Resync。
- Role State 不存在或状态冲突时，停止并报告主控。
- 不得因为新 Chat 重装 JDK / SDK、重下 Gradle、清 Gradle cache、大规模 clean、重新 clone、重新创建项目或重建已有环境。
- 只有实际确认缺失、损坏或版本不匹配后，才采取对应修复。

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

## 6. Task 使用方式

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
- Contract Change:
- Cross-module Impact:
- Known Risk:
- Remaining Work:
- Next Consumer:
```

- 一个 Task 可以包含一个或少量连续、目的明确的 commits；每个 commit 只承担一个清晰逻辑目的，不得混入其他 Task。
- 小型 Low Risk Task 通常一个 commit 即可。
- Handoff 不复制完整 Chat、日志或源码。
- 未完成时只能使用 `PARTIAL`、`BLOCKED` 或 `WIP`，不得声明 `READY_FOR_INTEGRATION`。
- 轻量 Handoff 可记录在 Role State；正式 Handoff 同时记录在 Formal Task Card。

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

Chat 结束不等于必须 commit。WIP checkpoint 是例外，不是默认流程。