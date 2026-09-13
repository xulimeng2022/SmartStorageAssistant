# T-012 Coordinator 跨永久工作树任务分发优先级

## Multi-Codex Metadata

- Task ID: T-012
- Owner: 主控（Coordinator）
- Risk: Medium
- Source Branch: codex/integration
- Commit / Commit Range: T-012 closure commit；精确 hash 为收口提交后的当前 Git HEAD
- Contract Change: 仅 Task Card / 调度协议；无 Android public API、schema 或 DI contract 变化
- Cross-module Request: 无；真实投递任务均为只读验证
- Execution Order: 根因与备份 → 规则修复 → 静态验证 → UI / AI / Data 真实只读投递 → 结果核验 → T-012 收口
- Handoff Target: Coordinator Integration Gate

## Task Boundary

- 所属模块：Coordination
- 目标行为：Coordinator 对永久角色模块任务默认先路由到既有永久工作树，只在有明确授权与证据时本地执行或 fallback。
- 本任务不做：业务代码、Room/schema、UI、AI 实现、CC Switch、DeepSeek、call_id、版本号、依赖、Worktree 重建或无关配置修改。
- 前置依赖：全局 `multi-codex-coordinator` Skill 完成时间戳备份；项目角色与 Ownership 真相源已存在。

## Acceptance Criteria

- [x] 全局 Skill 及 references 不硬编码本项目角色名、projectId、worktree 路径、branch 或 threadId。
- [x] 项目 AGENTS / WORKFLOW 明确 `Permanent Worktree > Coordinator Owned Local > Temporary Subagent`。
- [x] 同一 Task ID 续投原线程，新任务在既有角色 Project 本地环境新建线程，不创建新 Worktree。
- [x] 模块 Ownership 写操作必须先获得临时 Single Writer；Temporary Subagent 同样受约束。
- [x] 永久工作树失败时记录 `PERMANENT_WORKTREE_FALLBACK`，不静默降级。
- [x] UI、AI、Data 三个真实只读任务分别返回 threadId、实际 Project、worktree root、branch、验证前后 `git status --short`。
- [x] 三个验证线程的 cwd 不是 Coordinator 的 `3aad` 工作树，且没有使用 `spawn_agent` 结果冒充。
- [x] 最终只提交 T-012 范围内项目文件。

## TDD / Verification Record

本任务以规则一致性与真实跨工作树集成验证为主，不包含适合先失败后实现的产品行为测试。

### Static Validation

| 验证项 | 结果 | 证据 |
| --- | --- | --- |
| 全局 Skill 项目无关 | PASS | 未命中项目名、角色名、projectId、具体路径或 branch；仅保留通用 `threadId` 字段 |
| 路由优先级一致 | PASS | AGENTS / WORKFLOW / Skill 均定义三级优先级 |
| Single Writer / fallback 闭环 | PASS | 模块写操作的本地与 Subagent fallback 均受 Single Writer 约束；fallback marker 已定义 |
| Diff / 空白检查 | PASS | `git diff --check` |
| 全局 Skill 备份 | PASS | `C:\Users\XU\.codex\skills\multi-codex-coordinator.backup-20260913-113100`，4 个文件哈希比对一致 |

### Permanent Worktree Dispatch Evidence

| Target Role | Actual Project | threadId | `git rev-parse --show-toplevel` | `git branch --show-current` | `git status --short` before | `git status --short` after | Result |
| --- | --- | --- | --- | --- | --- | --- | --- |
| UI | 智能收纳助手 - UI | `01a098d3-5139-7b20-aaed-79804837f7b1` | `C:/Users/XU/.codex/worktrees/f98f/智能收纳助手` | `codex/ui` | 空 | 空 | PASS；读取 `HomeScreen.kt`，未启动子 Agent |
| AI | 智能收纳助手 - AI | `01a098d3-59b0-7e52-ae3a-d5905baa9a35` | `C:/Users/XU/.codex/worktrees/ae14/智能收纳助手` | `codex/ai` | 空 | 空 | PASS；读取 `VisionAnalyzer.kt`，未启动子 Agent |
| Data | 智能收纳助手 - Data | `01a098d3-5f7f-7752-8045-4f0fe6cc9d8f` | `C:/Users/XU/.codex/worktrees/95b0/智能收纳助手` | `codex/data` | 空 | 空 | PASS；读取 `AppDatabase.kt`，未启动子 Agent |

独立核验：

- `read_thread` 返回的 `cwd` 分别为 f98f、ae14、95b0 永久工作树。
- 三个 threadId 均由目标 Project 的 `create_thread` 直接返回，未调用 `spawn_agent`。
- Coordinator 当前 `3aad` 的 branch 为 `codex/integration`，与三个目标线程均不同。
- 投递后 Coordinator 独立执行 `git status --short`，三个角色工作树均为空。

## Implementation Log

- 2026-09-13：确认根因，完成全局 Skill 时间戳备份，建立 T-012 并完成项目级规则。
- 2026-09-13：将全局 Skill 及 references 改为项目无关的永久工作树优先机制。
- 2026-09-13：完成静态检查与 UI / AI / Data 三个真实永久工作树只读投递，全部 PASS。

## Fallback Record

- PERMANENT_WORKTREE_FALLBACK: 未触发。

## Completion Conclusion

- Status: DONE
- Deliverable: AGENTS / WORKFLOW / 全局 Skill / references / T-012 证据
- Long-term facts to backwrite: 已完成
- Follow-up: 无
