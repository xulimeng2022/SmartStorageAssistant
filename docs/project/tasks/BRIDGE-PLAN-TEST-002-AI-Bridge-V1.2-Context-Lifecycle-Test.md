# BRIDGE-PLAN-TEST-002 AI Bridge V1.2 Context Lifecycle Test

## Formal Task Metadata

- Task ID: `BRIDGE-PLAN-TEST-002`
- Owner: 主控（Coordinator）
- Task Size: `MEDIUM`
- Risk: `MEDIUM`
- Plan Requirement: `REQUIRED`
- Source Branch: `codex/integration`
- Contract Change: 无；仅验证 AI Bridge V1.2 的薄入口、Task Size、Plan Gate、CURRENT-STATE Stale、Knowledge 和归档规则
- Cross-module Request: 无；不修改 App 业务代码
- Execution Order: `BRIDGE-PLAN-002` 终态且证据归档 -> ChatGPT/User 切换活动任务 -> 制定并审核测试计划 -> 显式执行 -> A-J 测试 -> 证据回写本任务卡
- Handoff Target: 主控（Coordinator）

## Task Boundary

- 所属模块：Coordination。
- 目标行为：验证 V1.2 在不破坏 V1.1 的前提下可恢复、可审核、可拒绝 stale、可安全归档。
- 允许修改：测试临时 Bridge 副本、本任务卡、`04-任务与验收清单.md` 的测试状态；不得修改 App 业务代码。
- 禁止修改：App 源码、UI、数据库、Gradle、版本号、CC Switch、DeepSeek、V1.1 历史任务证据、`BRIDGE-PLAN-TEST-001`。
- 禁止创建：MCP、Relay、HTTP Server、WebSocket、daemon、watcher、polling。

## Test Cases

| 测试 | 前置 | 动作 | 预期 |
| --- | --- | --- | --- |
| A Task Size / Risk Floor | 创建 Small / Medium / Large 临时任务 | 应用 Hard LARGE Trigger | 命中时 Minimum Size=LARGE，Plan Requirement=REQUIRED |
| B Small 同会话授权 | Small 简短计划 | 同一会话显式执行指令 | 同时作为计划和执行授权；不创建 PLAN 文件 |
| C Small 新会话恢复 | 无权威用户决定 | 新会话尝试执行 | 不继承隐式授权，重新要求显式命令 |
| D Plan Mode 边界 | Plan Mode | 制定和讨论计划 | 不写 Bridge；发布命令后才写 PLAN |
| E Plan Gate | Pending / Changes Requested / Rejected | 尝试执行 | 全部拒绝执行 |
| F Stale Review | Revision 1 审核、Revision 2 计划 | 尝试执行 | 拒绝并报告 Revision 不匹配 |
| G CURRENT-STATE Stale | 故意写入错误 SHA | 读取 CURRENT-STATE | 标记 Stale，重读权威文件并重跑 Gate；不能独立批准/拒绝 |
| H Writer 边界 | Coordinator 无用户委托 | 尝试写 APPROVED | 拒绝；Decision Source 必须是 ChatGPT/User |
| I Knowledge 指针 | 有 Worker Candidate | Coordinator 归档 | Bridge 只保存 Path / Section / Source；不复制正文 |
| J Archive | 发布和终态任务 | 检查 history | PLAN、Review、terminal 快照存在且只追加、哈希可验证 |
| K Compatibility | 读取 V1.1 任务 | 按 V1.2 接管 | V1.1 文件和旧审核保持可读，不重写历史 |

## Acceptance Criteria

- [x] A-K PASS 或明确记录 NOT RUN 原因。A/B/H/I 为 reused evidence；C/D/E/F/G/J/K 为新增最小验证。
- [x] 不修改 App 业务代码、数据库、Gradle、版本号。
- [x] 不 Push / Merge / Tag / Release。
- [x] 测试证据和 external verification boundary 已回写本任务卡。
- [x] `BRIDGE-PLAN-TEST-001` 保持未执行。

## Final State

- Status: `COMPLETED / LOCAL_VERIFIED / EXTERNAL_PENDING`
- Prerequisite: MET；BRIDGE-PLAN-002 已归档，ChatGPT/User 已切换任务并完成 Plan Revision 1 审核。
- Note: S1-S3 最小验收 PASS；其余 A-K 按新增验证或既有证据对账；外部 ChatGPT/User 读取仍为 PENDING。


## Verification Evidence

- S1：真实 TEST-002 发布、Revision 1 审核 APPROVED + AUTHORIZED、显式执行 Gate PASS。
- S2：Task ID mismatch、stale Plan Revision、PENDING、CHANGES_REQUESTED、REJECTED 共 5 个负例全部拒绝执行且夹具哈希不变。
- S3：真实 CURRENT-STATE Stale 被识别并重读权威文件；Small 新会话无 Decision Evidence 时拒绝继承授权。
- A/B/H/I：复用 BRIDGE-PLAN-002 归档审核与 REPORT 的既有 PASS 证据。
- K：最小只读检查 T-023 V1.1 快照，Task ID、Revision、Approved、Authorized、STATUS / REPORT 字段可读且前后 SHA-256 不变。
- Evidence Root：.codex-tmp\BRIDGE-PLAN-TEST-002\evidence\。
- PLAN Revision 1 SHA-256：`BDB47EF3E0661DC2E6F17835E5D190B4378F744EDAD0BA17AB9E790D25A8BCB7`。
- PLAN_REVIEW Revision 1 APPROVED SHA-256：`7A9D8C90E8470387CB477B58081C40ADC6465DBF3E88971D3862564B8BB1A00D`。
- S1-S3 结果 SHA-256：`547312C9EE69AEFC30932BF99FA1495E8FF02F39D96490E0EBF2DBEA925BB904`。
- A-K 结果 SHA-256：`4A296F78D992EFD722459879B49EE28F6708197C71DC9EBCA71756B9C62C465D`。
- Phase B：Google Drive 外部 ChatGPT/User 读取未由 Coordinator 冒充，状态为 PENDING。

## Rollback

- 恢复测试临时 Bridge 副本。
- 回滚本任务卡与 `04-任务与验收清单.md` 的测试登记。
- 不涉及 App、数据库、Gradle、版本号或 Release。

## Implementation Log

- 2026-09-13：任务卡创建；等待 BRIDGE-PLAN-002 终态与独立审核。
- 2026-09-13：完成 S1-S3 最小验收与 A-K 证据对账；本地验证 PASS，Phase B 外部读取待 ChatGPT/User 确认。

## Fallback Record

- `PERMANENT_WORKTREE_FALLBACK`: 不适用；本任务为 Coordinator-owned infrastructure。