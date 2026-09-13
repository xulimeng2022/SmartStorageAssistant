# Coordinator State

- Role: 主控（Coordinator）
- Branch: codex/integration

## Current Task

- ID / Title: T-024 Coordinator 会话与 Worker Thread 生命周期规则落地
- Status: COMPLETED / LOCAL_VERIFIED
- Risk: Low
- Goal: 固化 Coordinator 对话生命周期与 Worker Thread REUSE-first 规则，替换「新 Task ID → 新建线程」旧默认。
- Allowed Scope: 全局 `multi-codex-coordinator` Skill、`docs/coordination/**`、`docs/project/**` 协调与任务文档。
- Acceptance: Skill 与 WORKFLOW 口径一致；8 处旧默认替换；UI / AI / Data Role State 建立 Worker Thread Registry；App 业务代码 0 变更。
- Task Card: `docs/project/tasks/T-024-Coordinator会话与Worker线程生命周期规则.md`

## Previous Tasks

- T-023 Archify 与 Obsidian 1.1.0→当前知识同步：COMPLETED / LOCAL_VERIFIED；两张 Archify 图与 Obsidian 6 个笔记/入口同步，App 代码 0 变更。
- T-022 UI 1.2.0 App 图标安全区返修：用户于 2026-09-13 确认本轮完成；最终 Release APK SHA-256 `1C080AEC567814A314868DBD44F8B3A1CDE0CA42BFBEE326E3A41917B7C2A854`。
- BRIDGE-PLAN-001：COMPLETED / LOCAL_VERIFIED。
- BRIDGE-PLAN-TEST-001：内容和证据保留，暂不执行 A-F Gate 测试。

## Chat Handoff

- 会话级交接（切换 Coordinator 主对话时使用）：当前阶段、已完成、进行中、待办、已确认重要决策、已废弃方案、已知问题、工作树状态、限制与禁止事项、下一步建议、必读事实源。
- 已确认决策：Coordinator Chat 生命周期与 Worker Thread 路由是两套不同机制；主对话只生成 Chat Handoff 并建议用户手动新开，不自动创建；Worker Thread 默认 REUSE，NEW 需举证。
- 已废弃方案：`新 Task ID → 新建线程` 的旧默认（替换清单见 T-024 任务卡）。
- 必读事实源：`AGENTS.md`、`docs/coordination/WORKFLOW.md`（§3 / §6 / §16）、`docs/coordination/OWNERSHIP.md`、`docs/project/PROJECT_STATUS.md`、本文件。
- 下一步建议：无新任务时保持 IDLE；进入下一阶段前按 WORKFLOW §16 判断是继续当前对话还是生成 Chat Handoff。

## 投递日志

- T-024 由 Coordinator Owned Local 执行：无永久角色 Worker Thread 投递；全局 Skill 写入为一次性用户授权操作（仓库外，不入 Git）。
- Worker Thread 复用 / 新建的判定与证据记录在各 Formal Task Card（唯一任务级真相源）；`UI.md` / `AI.md` / `Data.md` 的 Worker Thread Registry 只记录线程长期状态。
- 最近一次永久工作树投递：T-014 UI / T-015 AI / T-016 Data（历史记录，详见各 Task Card）。

## Progress

- Completed: 全局 Skill 与 WORKFLOW 固化对话生命周期、REUSE-first 路由、Hot / Warm / Cold、Thread Decision 单真相源、BLOCKED 与 Block Reason 判断；UI / AI / Data Role State 建立 Worker Thread Registry；RECOVERY、PROJECT_STATUS、04 清单与 tasks/README 同步。
- External Pending: 爱发电主页认证尚未通过；用户完成确认之后新增的网盘入口排序/「国内网络优先」小字和 GitHub README 用户化待办仍按未完成记录。

## Working Tree

- State: T-024 规则与状态文档已写入；T-023 未提交成果与 T-024 改动按两个 commit 分开提交；其它既有未提交修改保留。
- Pre-existing Dirty Files: `AGENTS.md`、`README.md`、`RELEASE_GUIDE.md`、`docs/project/08-开发环境.md`、`docs/project/09-发布与版本.md` 等在本任务前已存在修改，按文件级增量处理，不整体暂存。

## Validation

- 一致性搜索：`新 Task ID` / `新建线程` / `新任务线程` / `创建新线程` 在 Skill 与 WORKFLOW 中 0 命中（历史任务卡 T-012 保留当时的完成记录，不作为现行规则）。
- 口径检查：Thread Decision 单真相源、HOT 自动降级、BLOCKED ≠ NEW 三条硬规则在 Skill §4 / §10 与 WORKFLOW §3 / §6 / §16 一致。
- Skill hash：`SKILL.md` SHA-256 `C71D737FFBC05481E49A6DCF709ACA1D339F0C09D26BD25F41E31A55A5BFD745`，与 RECOVERY.md 记录一致。
- App 业务代码 / Gradle / 数据库 / 版本号：0 变更。Debug / Release 构建：NOT RUN（本任务无代码变更）。
- Push：按 T-024 授权 push `codex/integration`；不含 Tag 与 Release。

## Blockers

- 无阻塞。

## Known Issues

- Tag `v1.2.0` 指向 `f5aa898`，其 App tree 与最终 APK 源码 tree `d68f09c` 不同；T-023 只记录，不修复。
- Worker Thread Registry 初始条目 Health 均为 `UNKNOWN`，threadId 来自历史 Formal Task Card，待对应角色下次 Resume / Handoff 核实。

## Next Step

- 等待用户审核 T-024 结果与下一阶段任务；继续工作前先按 WORKFLOW §16 判断当前对话是否继续。