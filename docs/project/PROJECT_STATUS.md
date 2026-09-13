# 智能收纳助手 · 项目状态

> 本文件只记录会随时间变化的当前事实：版本、阶段、测试与构建状态、lint、待用户验收项、已知问题。
> 写入规则：只有主控 Codex 与 Release 流程可以更新；模块 / Review / 测试 Agent 只读，发现过期信息报告主控。
> 不承载任务管理（任务卡见 `04-任务与验收清单.md` 与 `tasks/`），不写临时日志与命令输出。

- 最近更新：2026-09-13（T-011 完成 1.2.0 历史问题与 A1–A8 代码收口；候选包待真机验收）

## 当前版本与阶段

- 当前版本：`1.2.0`（versionCode 6，见 `app/build.gradle.kts` 当前实际配置）。
- 当前阶段：v1.2.0 候选包准备阶段，尚未正式发布；T-011 已进入 `codex/integration`，待真机覆盖升级验收。
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
- T-011 历史问题与 A1–A8 收口：`IMPLEMENTED / 待真机`。

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
- Single Writer / Cross-module Request / Lightweight Resume / Handoff / Experiment isolation：`DEFINED`。
- T-010 Recovery / Backup / Team Migration：`DONE / 已验收 @ codex/integration`。
- Full Machine Recovery：`NOT VERIFIED`；Legacy dirty recovery：`PENDING SEPARATE DECISION`。
- Legacy Worktrees `10ba` / `baee`：保持隔离，未修改 dirty 数据。

## 测试与构建状态

- 本轮自动验证：78 个 JVM 单元测试通过；Debug 与未签名 Release 构建通过；v4→v5→v6 迁移 SQL 以隔离 SQLite 数据库验证通过。
- 本轮代码包含 Room v6/索引生命周期、导入事务、照片索引、合并、语音纠错、邮箱、检查更新和完全删除数据。
- Android Room 仪器测试、真机覆盖升级、真实网络/视觉 API 与完整清理流程尚未执行，不代表已通过真机验收。

## Lint

- 仍有既有 Manifest 相机硬件声明错误，未处理。

## 待用户验收

- v1.1.0 覆盖安装候选包的数据保留、单图索引创建/删除、批量合并、语音例子、完全删除数据、检查更新、邮箱、三语言和深色模式仍待真机验收。

## 已知问题

- 百川最新模型 ID 无法从官方 API 文档零猜测确认，列表中明确标注「待官方确认」。
- 已修复·数据安全：`MIGRATION_4_5` 已注册，新增 `MIGRATION_5_6` 并移除破坏性回退；仍需真机覆盖升级最终确认。
- 已修复·代码质量：`ItemRepositoryImpl` 的未使用 `ImageIndexingCoordinator` 注入已移除。