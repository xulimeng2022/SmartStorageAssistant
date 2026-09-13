# BRIDGE-PLAN-002 AI Bridge V1.2 Plan Mode Context Lifecycle

## Formal Task Metadata

- Task ID: `BRIDGE-PLAN-002`
- Owner: 主控（Coordinator）
- Task Size: `LARGE`
- Risk: `HIGH`
- Plan Requirement: `REQUIRED`
- Source Branch: `codex/integration`
- Contract Change: AI Bridge V1.2 外部传输协议；新增薄入口、Task Size / Risk Floor、Knowledge 指针、CURRENT-STATE 派生快照、历史快照和 Writer 边界；不改 App contract
- Cross-module Request: UI / AI / Data 均不直接写 Bridge；继续通过 Coordinator Handoff
- Execution Order: 发布 PLAN Revision 2 -> ChatGPT/User APPROVED + AUTHORIZED -> 用户显式执行 -> Skill / protocol / templates -> Bridge README / INDEX / CURRENT-STATE / history -> 仓库协调文档与任务登记 -> 本地验证 -> REPORT / STATUS 终态
- Handoff Target: 主控（Coordinator）

## Task Boundary

- 所属模块：Coordination。
- 目标行为：将 Codex Plan Mode 与 Google Drive AI Bridge 整合为 V1.2，并保持 Formal Task、Git、Ownership、Single Writer 和 V1.1 兼容。
- 允许修改：全局 `ai-bridge` Skill、Google Drive Bridge、`docs/coordination/AI_BRIDGE.md`、`docs/coordination/WORKFLOW.md`、`AGENTS.md` 最小入口、`docs/project/06-AI协作提示词.md`、本任务卡、`BRIDGE-PLAN-TEST-002` 任务卡、`04-任务与验收清单.md`、`docs/project/tasks/README.md`、`docs/coordination/Coordinator.md`。
- 禁止修改：App 源码、UI、数据库、Gradle、版本号、CC Switch、DeepSeek、现有 T-023 证据和 `BRIDGE-PLAN-TEST-001` 结论。
- 禁止创建：MCP、Relay、HTTP Server、WebSocket、daemon、watcher、polling、自动执行通道、新永久 Worktree。

## Acceptance Criteria

- [x] Bridge `TASK / PLAN / PLAN_REVIEW / STATUS` 的 Task ID 与执行命令一致。
- [x] Plan Revision 2 获得匹配的 `APPROVED + AUTHORIZED`，Decision Source 为 ChatGPT/User。
- [x] 全局 `ai-bridge` Skill、protocol 与模板升级为 V1.2。
- [x] Bridge 新增全局 `INDEX.md` 和项目级 `CURRENT-STATE.md`。
- [x] TASK.md 支持 `Task Size`、`Risk`、`Plan Requirement`；Hard LARGE Trigger 有 Risk Floor。
- [x] PLAN / REPORT 支持 `Knowledge Used` / `Knowledge`；Worker 只提交 `Knowledge Candidate`。
- [x] CURRENT-STATE 明确为派生快照，包含 Stale 刷新规则，不作为 Gate 输入。
- [x] PLAN_REVIEW 区分 Decision Source 与 File Writer，Coordinator 不自批准。
- [x] 计划、审核和终态写入 `history/<task-id>/`，旧修订不被覆盖。
- [x] 仓库协调文档和任务登记完成同步。
- [x] App 业务代码、数据库、Gradle、版本号：0 变更。
- [x] 外部 ChatGPT Connector 读取验证（2026-09-13 PASS）。
- [x] 未 Push / Merge / Tag / Release。

## Verification

- 本地静态校验：`ai-bridge` Skill 与 protocol 版本、模板字段、Bridge Task ID / Revision / Review、UTF-8 无 BOM、历史快照路径、仓库规则一致性。
- App 验证：NOT RUN（无 App 变更）。
- 外部云同步与 ChatGPT Connector：PASS（用户已实际读取入口、计划、审核和报告，关键状态一致）。
- `BRIDGE-PLAN-TEST-002`：创建待执行任务卡，不在此任务中执行。

- 本地静态检查：PASS；`git diff --check`：PASS；UTF-8 无 BOM：PASS；App 业务代码 diff：NONE。
- Knowledge：`AI Bridge.md` 与 `Multi-Codex 协作.md` 已通过 knowledge-base 脚本写入并回读。
- Bridge Gate：TASK / PLAN / PLAN_REVIEW Task ID 匹配；Plan Revision=2；APPROVED + AUTHORIZED；Decision Source=ChatGPT。

## Final State

- Status: `COMPLETED / EXTERNAL_VERIFIED`
- Plan Revision: `2`
- Report: `G:\我的云端硬盘\AI-Bridge\projects\github.com__xulimeng2022__smartstorageassistant\REPORT.md`
- App / Database / Gradle / Version / CC Switch / DeepSeek: unchanged.
- Next: ChatGPT/User 外部读取并执行 `BRIDGE-PLAN-TEST-002`。

## Rollback

- 使用 `.codex-tmp/BRIDGE-PLAN-002/pre-change-20260913/` 中的 Skill、协议、模板和仓库文档快照恢复。
- 删除本任务新增的 `INDEX.md`、`CURRENT-STATE.md` 和 `history/**` 前先确认没有后续修改。
- 保留 V1.0 / V1.1 七文件、T-023 证据和 `BRIDGE-PLAN-TEST-001`。
- 不使用 reset、clean、force 或整工作区覆盖。

## Implementation Log

- 2026-09-13：发布 BRIDGE-PLAN-002 / Plan Revision 2。
- 2026-09-13：执行 Gate 通过：Task ID、Revision、APPROVED、AUTHORIZED、Decision Source 全部匹配。
- 2026-09-13：完成全局 Skill / protocol / 模板、Bridge 薄入口、仓库协调文档和任务卡同步。

## Fallback Record

- `PERMANENT_WORKTREE_FALLBACK`: 不适用；本任务为 Coordinator-owned infrastructure。
