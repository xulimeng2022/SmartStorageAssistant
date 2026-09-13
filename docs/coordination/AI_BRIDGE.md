# AI Bridge

> 项目指针。全局规则见 `ai-bridge` Skill；AI Bridge 是传输层，Formal Task、Git、Ownership 与 WORKFLOW 仍是生命周期事实源。

## Protocol Version

`V1.2`

## Project Slug

`github.com__xulimeng2022__smartstorageassistant`

## Transport

Google Drive

## Google Drive Sync Root

`G:\我的云端硬盘`

## Local Bridge Root

`G:\我的云端硬盘\AI-Bridge`

## Project Bridge Directory

`G:\我的云端硬盘\AI-Bridge\projects\github.com__xulimeng2022__smartstorageassistant`

## V1.2 入口

- 全局入口：`G:\我的云端硬盘\AI-Bridge\INDEX.md`
- 项目当前快照：`G:\我的云端硬盘\AI-Bridge\projects\github.com__xulimeng2022__smartstorageassistant\CURRENT-STATE.md`
- 活动文件：`TASK.md`、`PLAN.md`、`PLAN_REVIEW.md`、`STATUS.md`、`REPORT.md`、`DECISIONS.md`、`BLOCKERS.md`
- 历史快照：项目目录下 `history/<task-id>/`

新会话按需读取：

```text
INDEX.md
→ CURRENT-STATE.md
→ TASK.md
→ 当前阶段的 PLAN.md / PLAN_REVIEW.md / STATUS.md / REPORT.md
→ 必要的 Formal Task、Role State 与精确 Knowledge 笔记
```

`CURRENT-STATE.md` 只是派生汇总，不是执行授权或 Gate 真相源。Revision、SHA 或状态不一致时，先标记 Stale，再重新读取权威活动文件、重新执行 Gate，最后重新生成快照；Stale 不能独立决定 `APPROVED`、`REJECTED` 或 `AUTHORIZED`。

## Commands

```text
为 AI Bridge <Task ID> 制定实施计划，暂不执行
发布 AI Bridge <Task ID> 计划
读取 AI Bridge <Task ID> 计划审核
执行 AI Bridge <Task ID>
重试 AI Bridge <Task ID>
```

`同步并执行最新 AI Bridge 任务` 为只读命令，只报告当前 Task ID，不执行。

## Task Size and Risk Floor

`TASK.md` 使用：

- `Task Size`: `SMALL | MEDIUM | LARGE`
- `Risk`: `LOW | MEDIUM | HIGH`
- `Plan Requirement`: `REQUIRED | NOT_REQUIRED_SMALL | SKIP_EXPLICIT`

工作流/规则、架构、数据库/Migration、共享 Contract/Schema、多 Worker、Release/Version、安全或高回滚风险属于 Hard LARGE Trigger。命中后 `Minimum Task Size=LARGE` 且 `Plan Requirement=REQUIRED`，不能只修改 Size 字段降级。

## Plan Gate

对于 `Plan Requirement=REQUIRED`：

```text
TASK.md Task ID = PLAN.md Task ID = PLAN_REVIEW.md Task ID = explicit execution Task ID
PLAN_REVIEW.md Reviewed Plan Revision = PLAN.md Plan Revision
PLAN_REVIEW.md Result = APPROVED
PLAN_REVIEW.md Execution Authorization = AUTHORIZED
```

`Decision Source` 必须是 ChatGPT/User。Coordinator 不得自批准；只有用户在当前会话明确要求代写并给出具体决定时，才可按 `File Writer=Coordinator (delegated)` 忠实持久化。`APPROVED` 不会自动执行，仍须显式执行命令。

## Knowledge

- PLAN 使用 `Knowledge Used` 记录路径、Section 和 Purpose；未使用时写 `Knowledge Used: none`。
- REPORT 使用 `Knowledge` 记录 `updated | unchanged | none | pending`、Archive 和 Source Task ID。
- UI / AI / Data Worker 只提交 `Knowledge Candidate`，不直接写 `D:\Knowledge`；Coordinator 按 `knowledge-base` Skill 统一去重和归档。
- Bridge 不保存完整 Knowledge 正文。

## Ownership

- `TASK.md`: ChatGPT / User
- `PLAN.md`: Coordinator
- `PLAN_REVIEW.md`: ChatGPT / User；Coordinator 默认只读
- `STATUS.md` / `REPORT.md` / `DECISIONS.md` / `BLOCKERS.md`: Coordinator
- `INDEX.md` / `CURRENT-STATE.md` / `history/**`: Coordinator
- UI / AI / Data 永久工作树不直接写共享 Bridge 文件

## Bootstrap Evidence

- Bootstrap ID: `_bootstrap_7f3c9a`
- Unique local candidate found: `G:\我的云端硬盘\AI-Bridge\_bootstrap_7f3c9a`
- Bootstrap directory was empty and removed after path confirmation.

## Compatibility

- V1.0 / V1.1 文件继续可读，不因升级而重写。
- 旧任务保留原 Plan Revision 与审核绑定。
- `BRIDGE-PLAN-TEST-001` 继续保持未执行，不因 V1.2 落地而标记完成。