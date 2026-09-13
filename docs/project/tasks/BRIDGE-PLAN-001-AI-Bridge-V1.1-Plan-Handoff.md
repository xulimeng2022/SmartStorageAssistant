# BRIDGE-PLAN-001 AI Bridge V1.1 Plan Handoff

## Formal Task Metadata

- Task ID: `BRIDGE-PLAN-001`
- Owner: 主控（Coordinator）
- Risk: Medium
- Source Branch: `codex/integration`
- Contract Change: 全局 `ai-bridge` Skill 与项目 AI Bridge 指针增加 PLAN/PLAN_REVIEW、Plan Revision 绑定与执行 Review Gate；不改 App contract
- Cross-module Request: UI / AI / Data 均不直接写 Bridge；继续通过 Coordinator Handoff
- Execution Order: 发布 PLAN Rev2 → ChatGPT 审核 `APPROVED + AUTHORIZED` → TASK 切换 BRIDGE-PLAN-001 → 用户显式执行 → Skill / protocol / templates → Bridge README / 项目指针 / AGENTS / WORKFLOW → Formal Task 与状态 → 本地验证 → 终态
- Handoff Target: 主控（Coordinator）

## Task Boundary

- 所属模块：Coordination。
- 目标行为：实现 Plan Handoff，使 Codex 计划经 PLAN.md 交给 ChatGPT 审核，并由 Revision 绑定的 Review Gate 控制执行。
- 允许修改：全局 `ai-bridge` Skill、Google Drive Bridge README、`docs/coordination/AI_BRIDGE.md`、`AGENTS.md` / `docs/coordination/WORKFLOW.md` 最小规则、本任务卡与 `BRIDGE-PLAN-TEST-001` 任务卡、`04-任务与验收清单.md`、`tasks/README.md`、`Coordinator.md`。
- 禁止修改：App 源码、UI、数据库、Gradle、版本号、CC Switch、DeepSeek、`call_id` 修复。
- 禁止创建：MCP、Relay、HTTP Server、WebSocket、daemon、watcher、polling。

## Acceptance Criteria

- [x] TASK / PLAN / PLAN_REVIEW / 显式执行命令的 Task ID 一致。
- [x] Plan Revision 2 审核为 `APPROVED + AUTHORIZED`，批准只绑定 Revision 2。
- [x] 全局 Skill 增加 PLAN / PLAN_REVIEW 模板与规划、发布、读取审核、Gate 控制面。
- [x] protocol 增加 7 文件 Single Writer、Plan Revision 绑定、执行 Gate、bootstrap 保护与旧字段策略。
- [x] Google Drive README 更新为 7 文件 Bridge。
- [x] 项目指针、AGENTS、WORKFLOW 完成最小规则更新。
- [x] 现有 `PLAN.md` Revision 2 与 `PLAN_REVIEW.md` 未被实施步骤覆盖。
- [x] App 业务代码变更为 0。
- [x] 未 Push、未 Tag、未 Release。

## Final State

- Status: `COMPLETED`
- Plan Revision: `2`（`APPROVED` + `AUTHORIZED`）
- Global Skill: `C:\Users\XU\.codex\skills\ai-bridge`
- Repository Pointer: `docs/coordination/AI_BRIDGE.md`
- App / CC Switch / DeepSeek / Database / Gradle / Version: unchanged.
- Next: ChatGPT/User 将 `TASK.md` 切换为 `BRIDGE-PLAN-TEST-001` 后执行 A-F Gate 测试。

## Rollback

- 删除 Google Drive 项目目录中的 `PLAN.md` 与 `PLAN_REVIEW.md`。
- 回滚全局 `ai-bridge` Skill 的 V1.1 修改。
- 回滚项目指针、AGENTS / WORKFLOW 最小规则与本任务卡。
- 保留 V1 五文件继续工作。
- 不涉及 App、CC Switch、DeepSeek、数据库、Gradle 或版本号回滚。

## Implementation Log

- 2026-09-13：发布 PLAN Revision 1；ChatGPT 审核为 `CHANGES_REQUESTED`。
- 2026-09-13：发布 PLAN Revision 2，纳入 bootstrap 保护、单活跃 TASK 顺序与旧 Plan Requirement 策略；ChatGPT 审核为 `APPROVED + AUTHORIZED`。
- 2026-09-13：Coordinator 收到显式 `执行 AI Bridge BRIDGE-PLAN-001`，执行 Gate 通过。
- 2026-09-13：完成 Skill / protocol / templates、Bridge README、项目指针、AGENTS / WORKFLOW 最小规则与任务登记更新。
- 2026-09-13：本地静态验证、V1 回归检查与未覆盖保护检查通过；App 业务代码 0 变更。

## Fallback Record

- `PERMANENT_WORKTREE_FALLBACK`: 不适用；本任务为 Coordinator-owned infrastructure。
