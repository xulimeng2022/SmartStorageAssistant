# BRIDGE-INIT-001 AI Bridge V1 Google Drive

## Formal Task Metadata

- Task ID: `BRIDGE-INIT-001`
- Owner: 主控（Coordinator）
- Risk: Medium
- Source Branch: `codex/integration`
- Contract Change: 新增全局 `ai-bridge` Skill；项目仓库后续增加 AI Bridge 指针与最小规则链接，不改 App contract
- Cross-module Request: UI / AI / Data 均不直接写 Bridge；继续通过 Coordinator Handoff
- Execution Order: Google Drive 安装与登录 → bootstrap 唯一路径发现 → 全局 Skill 验证 → Google Drive Bridge 文件 → 仓库指针与规则 → BRIDGE-TEST-001 → Git 验证
- Handoff Target: 主控（Coordinator）

## Task Boundary

- 所属模块：Coordination。
- 目标行为：建立 Google Drive Markdown 文件通信层，使 ChatGPT/用户写 TASK，Coordinator 写 STATUS/REPORT/DECISIONS/BLOCKERS。
- 允许修改：全局 `ai-bridge` Skill、Google Drive 桥文件、AI Bridge 项目指针、`AGENTS.md` / `docs/coordination/WORKFLOW.md` 最小链接、本任务卡与任务清单。
- 禁止修改：App 源码、UI、数据库、Gradle、版本号、CC Switch、DeepSeek 配置、call_id 修复。
- 禁止创建：MCP、Relay、HTTP Server、WebSocket、daemon、watcher、polling。

## Acceptance Criteria

- [ ] Google Drive for Desktop 已安装并完成登录，使用 ChatGPT Connector 可访问的账号。
- [ ] 唯一 `_bootstrap_*` 目录在本地命中且只命中一次。
- [ ] 确认实际 Google Drive 本地根目录，未猜盘符或 Mirror / Stream 路径。
- [ ] 建立 `AI-Bridge/projects/github.com__xulimeng2022__smartstorageassistant/` 和五个普通 Markdown 文件。
- [ ] 全局 `ai-bridge` Skill 通过实际可用的验证；验证器不可用时完成并记录静态验证限制。
- [ ] 建立 `docs/coordination/AI_BRIDGE.md` 和最小 AGENTS / WORKFLOW 入口。
- [ ] `BRIDGE-TEST-001` Phase A 通过：本地读写、状态转换、REPORT、BLOCKERS、Git 验证。
- [ ] 明确标记 Phase B 的 ChatGPT Connector 云端可见性等待外部用户验证。
- [ ] 去重、错误 ID、项目隔离测试通过。
- [ ] 不 Push、不 Tag、不 Release、不修改 App / CC Switch / DeepSeek。

## Current State

- Status: `BLOCKED`
- Google Drive for Desktop: 未安装完成。首次 winget 下载在已达约 118.5 MB 时按下载监控规则停止；winget 进程已退出。
- Bootstrap: 未提供 `_bootstrap_<随机短 ID>`，无法确认本地同步根目录。
- Global Skill: 已创建 `C:\Users\XU\.codex\skills\ai-bridge`，包含 `SKILL.md`、`references/protocol.md`、`agents/openai.yaml` 和五个 templates。
- Skill Validator: `quick_validate.py` 首次运行因 Python 环境缺少 `PyYAML` 未执行成功；未安装新依赖，待静态/可用环境验证。
- Project Pointer: 尚未创建，避免写入未确认的 Google Drive 路径。
- Runtime Bridge Files: 尚未创建。
- App / CC Switch / DeepSeek: 未修改。

## Next Required User Actions

1. 决定是否允许重新尝试 Google Drive 下载，或由用户使用官方安装包完成安装。
2. 安装后由用户登录 Google Drive，不猜测账号或绕过认证。
3. 在当前 ChatGPT 中通过 Google Drive Connector 创建唯一：
   `AI-Bridge/_bootstrap_<随机短 ID>/`
4. bootstrap 同步到本机后继续路径发现和后续初始化。

## Rollback

- 删除全局 `C:\Users\XU\.codex\skills\ai-bridge`。
- 删除 Google Drive 中已创建的当前项目 Bridge 目录。
- 回滚本任务卡、任务清单条目以及后续 `AI_BRIDGE.md` / AGENTS / WORKFLOW 最小链接。
- 不涉及 App、CC Switch 或 DeepSeek 回滚。
