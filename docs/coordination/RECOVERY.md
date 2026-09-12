# Multi-Codex Recovery Runbook

> Candidate only。本文件定义恢复协议，不代表已经完成 Remote 保护、Git Bundle、Team Migration 或 Disaster Simulation。
> 本文不是第二份 Workflow。正常新 Chat 继续使用 [WORKFLOW.md](WORKFLOW.md) 的 Lightweight Resume；只有普通 Resume 无法完成时，才使用本 Runbook。

## 1. 目的与边界

本 Runbook 回答“东西损坏或环境迁移后如何恢复”，适用于：

- 旧 Chat 不可访问
- 旧 Team / Project 不可访问
- 本机 Repository 或 main Worktree 损坏
- Branch 存在但 Worktree 丢失
- 需要迁移到新机器或新 Codex 环境
- 需要重新连接 Obsidian、Archify 或模型 Provider

不适用于：

- 日常任务分发
- 普通 Chat 续接
- 业务功能实现
- Room Migration 修复
- Release 或版本管理

权威源链接：

- 项目规则：[AGENTS.md](../../AGENTS.md)
- 文件 Ownership：[OWNERSHIP.md](OWNERSHIP.md)
- 协作流程：[WORKFLOW.md](WORKFLOW.md)
- 当前项目状态：[PROJECT_STATUS.md](../project/PROJECT_STATUS.md)
- 正式任务系统：[04-任务与验收清单.md](../project/04-任务与验收清单.md)
- 角色恢复快照：[Coordinator.md](Coordinator.md)、[Data.md](Data.md)、[AI.md](AI.md)、[UI.md](UI.md)

## 2. 核心恢复原则

```text
Repository / Git = 最小生存层
Archify          = 可选架构增强
Obsidian         = 可选长期知识增强
Chat             = 可丢弃缓存
```

信息优先级：

```text
Code / Git
↓
Repository Project State
↓
Formal Task / Role State / Handoff
↓
Archify
↓
Obsidian
↓
Chat
```

Task 与 Role State 不机械比较高低：

- 有 Current Formal Task 时，Task Card / 任务系统是任务生命周期权威源。
- Role State 是当前 Worktree / Chat 的恢复快照。
- 两者冲突时结合 Git、Working Tree、Task Card 和 Handoff 判断，不盲目覆盖。

## 3. 动态恢复目标

恢复时不得永久假设某个 Commit 就是 Current Baseline。必须先执行：

```text
需要恢复
↓
识别可验证 Recovery Source
↓
读取 Git refs / Project State / Task / Handoff
↓
确定目标恢复 Commit
↓
验证 Commit identity
↓
恢复
```

T-010 启动时的历史保护检查点：

```text
Initial Protected Baseline:
c2faa5fe0fbb97233df2095e5e5a405bafa81017
```

该值只用于识别 Phase A 的审计起点，不代表未来 `main` 永久固定在此时点。

## 4. Recovery Sources

### Remote Repository

- 定位：Git remote。
- 优点：异机、标准 Git 恢复、clone 简单。
- 风险：依赖网络与认证；repository 可见性可能受限制。
- 注意：`refs/remotes/*` 可能过期，不能未经 fetch 就当作远端真相。

### Git Bundle

- 定位：与网络无关的离线保护渠道。
- 用途：U 盘、云盘、第二块磁盘或用户指定的安全位置。
- 建议命名：`smart-storage-backup-YYYYMMDD-HHmm.bundle`。
- 备份输入优先来自本地 branches、tags、当前 stable main、四个长期角色分支、Release Branch 和关键 Release Tags。
- 不把 remote-tracking refs 当作远端真相。

未来创建 Bundle 后必须执行：

```text
create
↓
git bundle verify
↓
temporary clone
↓
verify refs
↓
verify expected commit
```

只有完整验证通过，才能称为有效备份。当前没有创建 Bundle。

### Full Git Clone / Backup

- 定位：包含完整 `.git` object store 的独立 Repository 副本。
- 适合作为 Bundle 和 Remote 之外的额外保护。
- 不要求通过 Worktree 形式保存。

## 5. Worktree 的灾备事实

当前 Coordinator、Data、AI、UI Worktree 为 linked Worktree，其 `.git` 文件依赖共享 Git object store。

```text
linked Worktree ≠ Full Git Backup
```

如果主 Repository 的完整 `.git` 丢失，四个 linked Worktree 不能视为四份完整 Repository Backup。真正 Recovery Source 必须来自：

- Remote Repository
- Git Bundle
- Full Git Clone / Backup

Git 无法恢复只存在于未提交 Working Tree 中的修改。可能依赖编辑器 Local History、文件系统 Backup、系统快照或用户手工副本。

正常 checkpoint 仍遵循：

```text
test
↓
state / task update
↓
commit
↓
handoff
```

但 Chat 结束不等于必须 Commit。上下文满且 Task 未完成时，允许更新 Role State、保留 dirty Worktree 并在同一 Worktree 创建新 Chat Resume。

## 6. Repository 与 Branch 恢复

1. 从 Remote、Bundle 或 Full Clone 获取 Repository。
2. 验证 Repository identity 和目标 Commit。
3. 检查目标 Branch refs 是否完整。
4. 检查 AGENTS、Ownership、Workflow、Project State 和 Task System 是否存在。
5. 在工作区干净的目标路径创建或重建 Worktree。
6. 对每个 Branch 验证 Branch、HEAD、status 和 Role State。
7. 执行 Coordinator Resume；再按需恢复 Data / AI / UI。

如果目标 Branch 不存在：

- 停止自动恢复。
- 报告缺失 ref、可能的 Recovery Source 和最近已知 Commit。
- 不得猜测重建历史。

## 7. Worktree Reconstruction Protocol

仅在 Branch ref 已存在、Worktree 丢失且用户已授权时执行：

```text
verify refs
↓
verify worktree registry
↓
verify destination
↓
create / restore
↓
verify branch
↓
verify HEAD
↓
verify status
↓
Lightweight Resume
```

要求：

- 确认没有重复 Worktree registration。
- 目标路径必须安全，且不得覆盖已有目录。
- 先验证远端、Bundle 或完整 clone 中的 ref。
- 异常立即停止并报告。

禁止把以下动作作为默认恢复手段：

- `reset --hard`
- 覆盖已有目录
- 删除未知 dirty Worktree
- 自动 `worktree prune` 后直接重建
- 自动 rebase
- 自动 force
- history rewrite

## 8. Coordinator Recovery

旧 Coordinator Chat 永久不可访问时：

1. 确认 Repository、Branch 和当前 HEAD。
2. 读取 `AGENTS.md`。
3. 读取 `OWNERSHIP.md`。
4. 读取 `WORKFLOW.md` 的必要章节。
5. 读取 `PROJECT_STATUS.md`。
6. 读取 `Coordinator.md`。
7. 读取 Current Formal Task；无任务时确认 IDLE。
8. 读取最近 Handoff 和近期 Commit。
9. 检查 Git status、风险和下一步。
10. 输出恢复报告。

恢复报告至少包含：

- Role
- Branch / HEAD
- Active Task / Status
- Working Tree
- Recent Handoff
- Blocker
- Risk
- Suggested Next Step

不得依赖旧 Chat 才能继续。

## 9. Module Recovery

Data / AI / UI 分别执行：

```text
Role
↓
Branch / HEAD
↓
AGENTS
↓
Ownership relevant section
↓
Workflow Resume section
↓
own Role State
↓
Current Task
↓
relevant code
↓
Recovery Report
```

模块 Agent 不默认读取整个项目，也不默认加载完整 Archify 或完整 Obsidian。

## 10. Team A → Team B Migration

```text
New Team
↓
Restore / Clone Repository
↓
Verify target recovery commit
↓
Restore environment
↓
Restore Secrets separately
↓
Rebuild Worktrees
↓
Coordinator Resume
↓
Data / AI / UI Resume
↓
Reconnect / regenerate Archify
↓
Reconnect Obsidian
↓
Health Check when available
↓
Continue
```

旧 Chat、旧 Project 和旧 Agent Session 不是恢复依赖。

## 11. Environment Recovery Inventory

以下清单基于 2026-09-12 的本机只读检查。恢复时必须重新验证实际版本；无法可靠确认的字段标记为 `VERIFY AT MIGRATION`，但保留验证位置和步骤。

| Asset | Required / Class | Current Location | Source / Version | Restore | Verify | Secret |
| --- | --- | --- | --- | --- | --- | --- |
| Project AGENTS | Yes / Tracked | Repository 根目录 | Git recovery commit；当前 checkpoint blob `251be91ab40eedad02f0b6bfe8a60358d954c3a5` | 从目标恢复 Commit checkout | `git hash-object AGENTS.md` 与目标 Commit 一致 | No |
| Global AGENTS | Yes / Local-only | `C:\Users\XU\.codex\AGENTS.md` | 无 canonical remote；audit SHA-256 `AD7DE0CEE83881614CD019DFF87D1D207E586D59C5F70366F154F791D3B1C86E` | 从安全配置备份恢复；无备份时按规则类别重建 | 文件存在且项目必需规则仍由 Project AGENTS 覆盖 | No |
| Codex Config | Yes / Local-only | `C:\Users\XU\.codex\config.toml` | Host-managed；audit SHA-256 `21C406C390EC04928A8EB28804BCDFCAF5E79754963AD3A3C9BFF64E9CB56FDF` | 从脱敏配置模板或安全备份重建 provider、sandbox、plugin、MCP 入口 | `codex --version`；配置可解析；必要入口可用 | Yes, auth references |
| Skills | `multi-codex-coordinator` Yes；`archify` Optional / Local-only | `C:\Users\XU\.codex\skills\multi-codex-coordinator`、`...\skills\archify` | Source `VERIFY AT MIGRATION`；当前安装为本地 Skill 包；Archify metadata `2.17` / package `2.17.0-dev.1`；协调 Skill 无版本号，`SKILL.md` SHA-256 `9CB848D934C5D171450591363B0735B7B1BA069ED0663FE08259722DC913F90B`；CC Switch skill sync `auto / cc_switch` | 从安全工作区备份恢复最小 Skill；无备份时 Archify 可按来源重装，协调 Skill 标记待重建 | Skill 名称存在；Archify `doctor` 通过；协调 Skill hash 与基线一致 | No |
| Plugins | Minimum Required: None / Optional host plugins | `C:\Users\XU\.codex\config.toml`、plugin cache | 仅用于现有 Codex 工作流对齐，不是 Repository 恢复硬依赖 | 缺失时继续 Repository Recovery；需完整环境时按下方清单重新安装 | 插件入口可用且不影响核心恢复 | No |
| MCP | Optional capability enhancement / Local-only | `config.toml` 的 `[mcp_servers.node_repl]`；当前 command path 为 `C:\Users\XU\AppData\Local\OpenAI\Codex\runtimes\cua_node\e7fe122ad3cbcd58\bin\node_repl.exe` | `node_repl`；运行时版本 `VERIFY AT MIGRATION` | 重建 config entry 或安装 Codex 对应运行时；路径变化时重新解析当前 runtime | Node REPL 启动；工具入口可调用 | No |
| CC Switch | Yes for current provider route / Local-only sensitive | `C:\Users\XU\.cc-switch` | App：`C:\Users\XU\AppData\Local\Programs\CC Switch\cc-switch.exe`；App version `VERIFY AT MIGRATION`；DB/settings 为本地状态 | 安装同版本 App 或从加密备份恢复 settings / DB；不复制 logs/cache | 进程运行；`enableLocalProxy=true`；Provider 可选中 | Yes |
| Model Provider | Yes for current Codex model / Local-only | Codex Config + CC Switch | Codex provider `custom`；model `deepseek-flash`；base URL `http://127.0.0.1:15721/v1`；wire API `responses`；catalog `C:\Users\XU\.codex\cc-switch-deepseek-vision-model-catalog.json`；CC Switch provider ID `9aab64d1-10be-48c9-a61b-467e32cefc69` | 按下方 Model Provider / CC Switch 顺序恢复 | 非破坏性最小模型/能力检查；确认模型路由 | Yes, credential only |
| Obsidian | Optional / Local-only | `D:\Knowledge` → `D:\Study` | Vault 本地目录；Agent Client `0.12.1` | 恢复或重新连接 Vault；按需安装 Agent Client | Vault 可打开；只读搜索成功；不影响 Repository | No |
| Archify | Optional / Generated enhancement | `C:\Users\XU\.codex\skills\archify`；生成产物在 Codex work output | Skill `2.17`；当前 v1.2.0 spec base `d92d8123dde9510607858a00faf7950081ccf4f9` | 从 Skill 备份恢复或重装；必要时从 Code 重新生成 | Archify `doctor`；spec / artifact 可读 | No |
| local.properties | Yes for Android build / Local-only Never Commit | 当前 audit 仅 main Worktree 存在 | 无版本控制；字段：`sdk.dir`、`SILICONFLOW_API_KEY`、`KEYSTORE_FILE`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD` | 按新机 Android SDK 和签名环境重建；Secret 从安全存储重注入 | Gradle 可解析；只运行允许的构建/测试验证 | Yes |
| Secrets | Yes / Secret | `local.properties`、Codex auth、`.sandbox-secrets`、CC Switch DB | 无 Repository Source；来自用户 Secret Store | `REINJECT FROM SECRET STORE`；不得从 Git / Obsidian / Archify 恢复 | 认证成功；模型可用；签名构建仅在明确授权时验证 | Yes |

### Minimum Skills

- **Required:** `multi-codex-coordinator`
  - Purpose: Multi-Codex 调度、任务拆分、Handoff 与 Integration 流程入口。
  - Source: `VERIFY AT MIGRATION`；当前机器上的 Skill 实际位于其安装目录，CC Switch settings 显示 `skillSyncMethod=auto`、`skillStorageLocation=cc_switch`。
  - Version: 无版本号；以 `SKILL.md` SHA-256 标识。
  - Restore: 从安全工作区备份恢复；若来源缺失，标记 `VERIFY AT MIGRATION` 并重建。
  - Verify: Skill 可被当前 Codex 发现，且 hash 与审计基线一致。
- **Optional but expected:** `archify`
  - Purpose: 架构认知增强。
  - Source: 本地安装的 Archify Skill 包。
  - Version: `2.17`（package `2.17.0-dev.1`）。
  - Restore: 从 Skill backup 恢复或按支持的重装流程安装。
  - Verify: `node bin/archify.mjs doctor --json` 返回成功。
- Unknown fields: Archify 与协调 Skill 的 canonical install source 均为 `VERIFY AT MIGRATION`；恢复时检查 `SKILL.md`、Skill hash 和 CC Switch 的 skill storage 状态，不得猜测。

### Minimum Plugin Set

**Minimum Required Plugin Set: None。**

Plugins 对 Repository Recovery 没有硬依赖。以下仅为当前环境对齐清单，缺失时不得阻塞 Repository / Git 恢复：

- `codex-app-tools@openai-bundled`：`0.1.3`
- `browser@openai-bundled`：`26.903.71938`
- `unified-computer-use@openai-bundled`：`26.903.71938`
- `chrome@openai-bundled`：`26.903.71938`
- `computer-use@openai-bundled`：`26.903.71938`
- `visualize@openai-bundled`：`1.0.32`
- `documents`、`pdf`、`spreadsheets`、`presentations`、`template-creator@openai-primary-runtime`：`26.909.12148`

- Source: `openai-bundled` 与 `openai-primary-runtime` marketplaces。
- Restore: 使用 Codex plugin 安装/同步机制，不手工复制 cache 作为权威安装。
- Verify: 插件名称出现在有效配置中，且所需工具可调用。
- Unknown fields: 任一新版本不可确认时写 `VERIFY AT MIGRATION`。

### MCP

- `node_repl` 是 Optional capability enhancement，不是 Recovery hard dependency。
- Restore: 重建 `[mcp_servers.node_repl]` 配置及对应 runtime。
- Verify: 工具可启动并执行一个只读 Node 命令。
- Unknown fields: 精确 runtime version 使用 `VERIFY AT MIGRATION`。

### Model Provider / CC Switch Restore Sequence

1. Install 或 restore CC Switch 当前 App。
2. Restore 非 Secret settings 与所需 DB；不要复制日志和缓存。
3. Confirm non-secret fields：`enableLocalProxy=true`、`currentProviderCodex=9aab64d1-10be-48c9-a61b-467e32cefc69`。
4. 从 Secret Store 重新注入 provider credential；不得写入 Repository。
5. Start CC Switch。
6. Verify 本地 provider endpoint `http://127.0.0.1:15721/v1` 与 routing；若无法监听则停止并报告。
7. Restore Codex custom provider reference：`model_provider = "custom"`、`model = "deepseek-flash"`、`wire_api = "responses"`、model catalog path。
8. Start Codex。
9. Verify selected model 与 provider。
10. Run non-destructive capability check；不得把一次成功调用扩大成 Release 或数据操作授权。

- Current architecture: Codex 通过 custom provider 引用本机 CC Switch proxy；CC Switch 负责 DeepSeek-compatible provider routing。
- Non-secret configuration: provider identifier `custom`、CC Switch ID `9aab64d1-10be-48c9-a61b-467e32cefc69`、model `deepseek-flash`、base URL `http://127.0.0.1:15721/v1`、wire API `responses`、model catalog `C:\Users\XU\.codex\cc-switch-deepseek-vision-model-catalog.json`。
- Secret reinjection: `Credential: REINJECT FROM SECRET STORE`。
- Unknown fields: CC Switch App version、CC Switch installer source、proxy listener 当前状态、model catalog 版本均标记 `VERIFY AT MIGRATION`。

### Global AGENTS 恢复策略

- 记录规则类别、必要性和重建方式。
- 区分 Project Rule 与个人 Global Rule。
- 项目继续运行必须依赖的规则应保留在 tracked Project AGENTS。
- 纯个人工作习惯保留在 Global。
- 不把 Global AGENTS 全文复制进 Repository。

### Obsidian

- 逻辑路径：`D:\Knowledge`
- 实际目标：`D:\Study`
- 恢复后先验证 junction、Vault 内容和 Obsidian 再连接。
- Obsidian 不可用不阻塞 Repository 开发。

### Archify

- Archify 是可重新生成的 generated enhancement，不属于唯一真相。
- 已有 spec 可记录 base revision；当前已知 v1.2.0 spec 的 base revision 为 `d92d8123dde9510607858a00faf7950081ccf4f9`。
- Refresh、Freshness 自动化和 Architecture Review 属于后续阶段。
## 12. Secret Boundary

Knowledge Management ≠ Secret Management。

Repository、Recovery Runbook、Task Card、Git commit、Obsidian、Archify metadata 和报告不得包含：

- API Key
- Token
- Password
- Codex auth
- CC Switch credentials
- 私密认证信息

只允许记录占位符，例如：

```text
DEEPSEEK_API_KEY=<SECRET>
```

Secret 需要在新环境单独重新注入、重新认证或从加密存储恢复。

## 13. Dirty Worktree

- Git 无法恢复未提交修改。
- 恢复前不得覆盖、reset 或 clean dirty Worktree。
- 如果 dirty Worktree 仍存在，先复制必要现场并确认用户意图。
- 如果 dirty Worktree 已丢失，只能检查本地编辑器历史、文件系统 Backup、系统快照或用户副本。
- 不为了灾备强制把每个 Chat 转成 WIP commit。

## 14. Disaster Scenarios

| Scenario | Recovery Entry |
| --- | --- |
| Coordinator Chat 丢失 | 第 8 节 |
| Data / AI / UI Chat 丢失 | 第 9 节 |
| Worktree 被删 | 第 7 节 |
| main `.git` 损坏 | 第 4、5、6 节 |
| Dirty Worktree 损坏 | 第 13 节 |
| Team / Project 丢失 | 第 10、11 节 |
| CC Switch / Model Provider 丢失 | 第 11 节 |
| Obsidian 不可用 | 第 11 节 |
| Archify 不可用或过期 | 第 11 节 |

## 15. 未来验证程序

本 Runbook 尚未执行灾备模拟。未来授权后：

- 只在临时目录运行。
- 使用已验证的 Remote、Bundle 或 Full Clone。
- 验证 Repository restore、expected HEAD、Branch refs、Worktree reconstruction、AGENTS、Role State 和 Coordinator Resume。
- 不执行正式 Worktree 的 reset、clean、delete、force 或 history rewrite。
- 记录实际结果；未执行项目保持 `NOT RUN`，不得写成 PASS。
- 验证结束后只删除临时目录。

## 16. 停止条件

发现以下任一情况立即停止：

- Commit identity 不一致
- Branch ref 缺失
- Worktree registry 冲突
- 目标路径存在未知内容
- Repository 含 Secret
- 需要覆盖或删除 dirty Worktree
- 需要 force / reset / history rewrite
- 无法确认用户授权
- Recovery Source 未验证

## 17. 当前状态

- Recovery Runbook: `CANDIDATE`
- Remote truth: `NOT VERIFIED IN THIS PHASE`
- Fetch: `NOT PERFORMED`
- Push: `NOT PERFORMED`
- Bundle: `NOT CREATED`
- Disaster Simulation: `NOT RUN`
- Recovery capability: `PROTOCOL DEFINED, VALIDATION PENDING`