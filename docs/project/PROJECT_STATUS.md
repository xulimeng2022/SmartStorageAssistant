# 智能收纳助手 · 项目状态

> 本文件只记录会随时间变化的当前事实：版本、阶段、测试与构建状态、lint、待用户验收项、已知问题。
> 写入规则：只有主控 Codex 与 Release 流程可以更新；模块 / Review / 测试 Agent 只读，发现过期信息报告主控。
> 不承载任务管理（任务卡见 `04-任务与验收清单.md` 与 `tasks/`），不写临时日志与命令输出。

- 最近更新：2026-09-12（完成 Baseline Resync、四角色 Resume 验证与 Role State Finalization；T-009 Final Readiness checkpoint 定义 Multi-Codex Overall Readiness = READY）

## 当前版本与阶段

- 当前版本：`1.2.0`（versionCode 6，见 `app/build.gradle.kts` 当前实际配置）。
- 当前阶段：v1.2.0 开发 / Debug 准备阶段，尚未正式发布；Coordination Infrastructure、Baseline Resync、四角色 Lightweight Resume 与 Role State reconciliation 均已完成，T-009 Final Readiness checkpoint 作为最终治理基准。
- Phase 3A — Coordination Infrastructure：`COMPLETE`。
- Phase 3B — Global Status Reconciliation：`COMPLETE`。
- Phase 3C — AGENTS Workflow Entry：`COMPLETE`。
- Coordination Finalization：`COMPLETE`。
- Stable Candidate Review：`COMPLETE / PASS`。
- Stable main propagation：`COMPLETE / PASS`。
- v1.2.0 Preservation：`PASS`。
- Data / AI / UI Baseline Resync：`COMPLETE / PASS`。
- Baseline Resync：`COMPLETE / PASS`。
- Central Rule Git Propagation：`COMPLETE / PASS`。
- Lightweight Resume Validation：`COMPLETE / PASS`。
- Role State Finalization：`COMPLETE IN T-009 FINAL CHECKPOINT`。

## Baseline 状态

- Frozen Baseline：`ee95f93b82e454ecebc3463e25fce51a9ec4569c`。这是四个长期 Worktree 创建时的历史共同基准。
- Integration / Coordination：已审核 Coordination Candidate 已成功 fast-forward 进入 main；后续分支精确 HEAD / ahead / behind 以 Git 为准。
- Stable Baseline：`main` 已包含通过审核的 Coordination Infrastructure，并成为当前新的 Stable Baseline；精确 HEAD 以 Git 为准。

## Multi-Codex 协调状态

- 永久角色 Worktree：主控（Coordinator）、Data、AI、UI 均已建立；Test / Review 仍按任务临时启动，不建立永久 Worktree。
- 长期分支：主控 `codex/integration`、Data `codex/data`、AI `codex/ai`、UI `codex/ui`。
- Ownership：已固化并进入 Stable main，唯一真相源为 [OWNERSHIP](../coordination/OWNERSHIP.md)；Remaining Pending Ownership 为 `None`。
- Workflow：已固化并进入 Stable main，唯一主要真相源为 [WORKFLOW](../coordination/WORKFLOW.md)。
- Role State infrastructure：已建立并进入 Stable main；Coordinator / Data / AI / UI 恢复快照机制已可用。
- Single Writer：`DEFINED`。
- Cross-module Request：`DEFINED`。
- Lightweight Resume：`DEFINED`。
- Handoff：`DEFINED`。
- Experiment isolation：`DEFINED`。
- Baseline Resync：`COMPLETE / PASS`。
- Central Rule Git Propagation：`COMPLETE / PASS`。
- Central Rule State Reconciliation：`COMPLETE IN T-009 FINAL CHECKPOINT`。
- Coordinator Recovery：`VALIDATED`。
- Data / AI / UI Recovery：`VALIDATED`。
- Multi-Codex Overall Readiness：`READY`；T-009 Final Readiness checkpoint 是该状态的治理基准，具体分支是否已应用 checkpoint 由 Git 事实决定。
- Legacy Worktrees `10ba` / `baee`：已隔离保留，等待后续单独审计。

## 测试与构建状态

- 最近已有自动验证基线：71 个 JVM 单元测试通过；Debug 与 Release 构建通过；Release APK 通过 v2 + v3 签名校验。
- 最近一次完整自动验证记录：2026-09-11（T-008，见 `07-变更与交付确认.md`）。
- 上述结果仅代表已有自动化 / 构建验证基线，不代表 v1.2.0 已完成本轮正式 Debug、真机验收或正式发布。
- 当前 Coordination Infrastructure 变更只涉及文档，尚未重新运行完整项目构建。

## Lint

- 仍有既有 Manifest 相机硬件声明错误，未处理。

## 待用户验收

- 多语言、真实视觉 API、历史索引进度与 Top-5 流程仍待用户在手机验收。

## 已知问题

- 百川最新模型 ID 无法从官方 API 文档零猜测确认，列表中明确标注「待官方确认」。
- 待确认·数据安全风险：Room v5 的 `MIGRATION_4_5` 已在 `AppDatabase.kt` 声明，但未注册到 `DatabaseModule.addMigrations(...)`；同时启用了 `fallbackToDestructiveMigration()`。真实 v4→v5 升级可能触发破坏性重建，存在本地数据丢失风险。当前仅登记，不创建正式任务、不修复；未来如立项按 High Risk / Data Safety 处理，要求 Data Owner、Migration Test、Test、Review 与主控 Gate。
- 待确认·代码质量问题：`ItemRepositoryImpl` 注入了未使用的 `ImageIndexingCoordinator`；当前仅登记，不修改，不创建修复任务。
