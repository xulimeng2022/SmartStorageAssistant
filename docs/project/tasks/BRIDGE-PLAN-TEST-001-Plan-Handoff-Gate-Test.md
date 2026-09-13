# BRIDGE-PLAN-TEST-001 Plan Handoff Gate Test

## Formal Task Metadata

- Task ID: `BRIDGE-PLAN-TEST-001`
- Owner: 主控（Coordinator）
- Risk: Low
- Source Branch: `codex/integration`
- Contract Change: 无；仅验证已实现的 Plan Handoff Gate
- Cross-module Request: 无；不修改 App 业务代码
- Execution Order: `BRIDGE-PLAN-001` 终态且证据归档 → ChatGPT/User 切换桥 TASK.md → Test A-F → 证据回写本任务卡
- Handoff Target: 主控（Coordinator）

## Task Boundary

- 所属模块：Coordination。
- 目标行为：验证 `TASK → PLAN → PLAN_REVIEW → APPROVED → EXECUTION GATE`，包括 PENDING、CHANGES_REQUESTED、REJECTED、stale APPROVED 和 matching APPROVED。
- 允许修改：Bridge `TASK.md`（ChatGPT/User）、`PLAN.md`（Coordinator）、`PLAN_REVIEW.md`（ChatGPT/User）、本任务卡与 04 状态登记。
- 禁止修改：App 源码、UI、数据库、Gradle、版本号、CC Switch、DeepSeek、`call_id` 修复。
- 禁止创建：MCP、Relay、HTTP Server、WebSocket、daemon、watcher、polling。

## Test Cases

| 测试 | 前置 | 动作 | 预期 |
| --- | --- | --- | --- |
| A 计划发布 | TASK `BRIDGE-PLAN-TEST-001`，`Plan Requirement=REQUIRED` | 制定计划但不执行，发布 `PLAN.md` | Task ID 正确，`Plan Revision=1`，`Status=READY_FOR_REVIEW`，业务代码 0 变更 |
| B 未审核禁止执行 | `PLAN_REVIEW=PENDING`，`Reviewed Plan Revision=NONE`，`Execution Authorization=NOT_AUTHORIZED` | 执行 `执行 AI Bridge BRIDGE-PLAN-TEST-001` | 拒绝执行，不修改 App 业务代码 |
| C 修改请求 | ChatGPT/User 写 `Result=CHANGES_REQUESTED`、`Reviewed Plan Revision=1` | Coordinator 读取审核，不执行，按 Required Changes 重新规划 | 不执行；修订后发布 `Plan Revision=2` |
| D 旧批准失效 | PLAN Revision=2；ChatGPT/User 保留或写入 `Reviewed Plan Revision=1`、`Result=APPROVED` | 再次尝试执行 | 拒绝，并报告审核针对 Revision 1、最新计划为 Revision 2 |
| E 正确批准 | ChatGPT/User 写 `Reviewed Plan Revision=2`、`Result=APPROVED`、`Execution Authorization=AUTHORIZED` | 仅检查执行 Gate | Gate 通过；测试任务不执行 |
| F Plan Mode 边界 | Plan Mode 只读 | 只生成、讨论、确认计划 | 不写任何 Bridge 文件；退出 Plan Mode 后再执行发布命令 |

## Acceptance Criteria

- [ ] Test A PASS。
- [ ] Test B PASS。
- [ ] Test C PASS，并产生 Revision 2。
- [ ] Test D stale review 被拒绝。
- [ ] Test E matching review 通过 Gate，但不执行。
- [ ] Test F Plan Mode 边界未被绕过。
- [ ] App 业务代码变更 0。
- [ ] 未 Push / Tag / Release。
- [ ] 测试证据已回写本任务卡；`BRIDGE-PLAN-001` Formal Task 只在证据归档时更新。

## Final State

- Status: `NOT_STARTED`
- Prerequisite: `BRIDGE-PLAN-001` 已完成并归档；ChatGPT/User 将 `TASK.md` 切换为 `BRIDGE-PLAN-TEST-001`。
- Note: 本任务只验证 Gate，不执行 App 业务变更。

## Rollback

- 恢复测试任务的 `TASK.md` / `PLAN.md` / `PLAN_REVIEW.md` 模板或删除测试内容。
- 回滚本任务卡与 `04-任务与验收清单.md` 的测试登记。
- 不涉及 App、CC Switch、DeepSeek、数据库、Gradle 或版本号。

## Implementation Log

- 2026-09-13：任务卡创建；等待 `BRIDGE-PLAN-001` 终态后执行 A-F。

## Fallback Record

- `PERMANENT_WORKTREE_FALLBACK`: 不适用；本任务为 Coordinator-owned infrastructure。
