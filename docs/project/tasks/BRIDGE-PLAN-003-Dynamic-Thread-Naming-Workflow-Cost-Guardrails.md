# BRIDGE-PLAN-003 Dynamic Thread Naming + Workflow Cost Guardrails

## Formal Task Metadata

- Task ID: `BRIDGE-PLAN-003`
- Owner: 主控（Coordinator）
- Task Size: `LARGE`
- Risk: `MEDIUM`
- Plan Requirement: `REQUIRED`
- Source Branch: `codex/integration`
- Contract Change: 无 App / Database / Gradle / 版本号变化；仅新增 Multi-Codex 的 Thread Title 投影规则与工作流成本护栏
- Cross-module Request: 无；UI / AI / Data 不写全局规则
- Execution Order: 用户审核计划 -> 发布 Bridge PLAN / PLAN_REVIEW -> 全局 Skill / AGENTS -> 项目 WORKFLOW / AGENTS -> 任务记录 -> 当前可见线程纠偏 -> 静态验证 -> Bridge 终态
- Handoff Target: 主控（Coordinator）

## Task Boundary

- 所属模块：Coordination。
- 目标行为：让 Thread Title 稳定投影当前 Active Task，并让后续工作流升级显式考虑时间、性能、Token 与复杂度。
- 允许修改：全局 `multi-codex-coordinator` Skill 及 reference、全局 `AGENTS.md`、项目 `AGENTS.md`、`docs/coordination/WORKFLOW.md`、本任务卡、`docs/project/04-任务与验收清单.md`、`docs/coordination/Coordinator.md`、`docs/project/PROJECT_STATUS.md`、Bridge `TASK / PLAN / PLAN_REVIEW / STATUS / REPORT / CURRENT-STATE`。
- 允许执行：使用 Codex App `set_thread_title` 修正当前项目可见且能直接映射的 Thread Title。
- 禁止修改：App 源码、数据库、Gradle、版本号、CC Switch、DeepSeek 配置、签名、Codex 私有数据库或私有状态接口。
- 禁止创建：watcher、polling、daemon、后台服务、标题专用模型调用、第二套 Task Registry / Active Task / Coordinator State。

## Acceptance Criteria

- [x] 全局 `AGENTS.md` 只保留极短 Thread Naming 与 Workflow Cost Guardrails 入口。
- [x] `multi-codex-coordinator` Skill 明确 Thread Title 只是 Active Task 的 UI 投影，不是真相源。
- [x] 完整 reference 覆盖标题来源优先级、7 类触发事件、普通消息/内部步骤禁止触发、失败降级、线程清理边界和成本检查。
- [x] 项目 `AGENTS.md` 与 `WORKFLOW.md` 只增加短指针和任务生命周期接入，不复制完整规则。
- [x] Task Card、Role State、Worker Thread Registry 不新增标题字段或第二套状态。
- [x] 普通消息不触发重命名、不读取完整上下文、不新增模型请求、不轮询。
- [x] 任务级切换最多调用一次 `set_thread_title`；失败不阻塞任务、不重试。
- [x] 当前项目可见且能直接映射的 Thread Title 完成一次性纠偏；无法映射的历史线程保持原状。
- [x] App 业务代码、数据库、Gradle、版本号与发布配置：0 变更。

## Verification

- 静态检查：Skill / reference / 全局与项目 AGENTS / WORKFLOW 规则一致；无第二真相源。
- 场景检查：Formal 保持不变、普通追问不触发、内部调试不触发、T-027→T-028 仅改名一次、TMP→Formal 改名一次、失败继续执行、占位标题纠正。
- 能力检查：当前线程执行一次 `set_thread_title`，再通过 `list_threads` 核验。
- 安全边界：无 Codex 私有 DB、无高风险脚本、无 watcher / polling / daemon。
- 仓库检查：`git diff --check`、UTF-8 无 BOM、链接与路径存在。
- App 验证：NOT RUN（无 App 变更）。

- PASS：`git diff --check`；新增/修改文件均为 UTF-8 无 BOM。
- PASS：完整 reference 存在且 Skill 引用有效；全局 AGENTS 仅增加短入口。
- PASS：当前项目 5 个可直接映射的线程标题已通过 `set_thread_title` 更新并由 `list_threads` 回读；其他项目或无法映射线程跳过。
- PASS：App 源码、Database、Gradle、版本号、CC Switch、DeepSeek、签名均 0 变更。
- PASS：Knowledge 已语义去重并更新 `D:\Knowledge\AI-Workflow\Multi-Codex 协作.md`。
- NOT RUN：Gradle / App 构建与测试（本任务无 App 变更）。

## Rollback

- 通过逐文件逆向本次新增段落恢复全局 Skill / AGENTS、项目 WORKFLOW / AGENTS 与任务记录；Bridge 旧活动文件已存于 `history/BRIDGE-PLAN-TEST-002/`。
- 删除新增 reference / Task Card 前先确认没有后续引用；不重写任何 history。
- 不执行 reset、clean、force 或工作区整体覆盖。

## Implementation Log

- 2026-09-13：用户以 `PLEASE IMPLEMENT THIS PLAN` 批准并授权 Plan Revision 1。
- 2026-09-13：完成全局 Skill / reference、全局与项目 AGENTS、项目 WORKFLOW、任务记录和 Bridge 活动文件同步。
- 2026-09-13：完成当前项目明确映射线程的一次性标题纠偏与静态验证。

## Final State

- Status: `COMPLETED / EXTERNAL_VERIFIED`
- Plan Revision: `1`
- External Verification: `EXTERNAL_VERIFIED`（ChatGPT Google Drive 外部读取，2026-09-13）
- App / Database / Gradle / Version / CC Switch / DeepSeek: unchanged.
