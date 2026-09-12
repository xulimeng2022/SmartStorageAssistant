# T-010 Recovery / Backup / Team Migration

> Formal Task complete。Candidate Review、Focused Re-review、Independent Test、Independent Review 与 Coordinator Integration Gate 均已通过。
> Current Phase: DONE / COMPLETE。

## Formal Task Metadata

- Task ID: T-010
- Title: Recovery / Backup / Team Migration
- Type: Formal Task
- Owner: 主控（Coordinator）
- Risk: High
- Source Branch: `codex/integration`
- Commit / Commit Range: T-010 finalization checkpoint; exact hash is current Git HEAD after closure commit
- Status: `DONE`
- Current Phase: DONE / COMPLETE
- Initial Protected Baseline: `c2faa5fe0fbb97233df2095e5e5a405bafa81017`
- Baseline Semantics: T-010 启动时的保护检查点，不是永久 Current Baseline
- Contract Change: No
- Cross-module Request: None
- Execution Order: Candidate Review / Formal Task establishment → Initial Git baseline protection gate → Git Bundle authorization → Bundle creation + verify → temporary clone recovery validation → Remote truth verification gate → user-authorized fetch if approved → evaluate remote state → user-authorized push if needed → Recovery / Team Migration simulation → Test → Review → Coordinator Integration Gate → Stable main propagation → final lifecycle closure
- Handoff Target: Coordinator Integration Gate（Test / Review 后交回主控集成）
- Test Required: Yes
- Review Required: Yes
- User Gate Required: Yes
- Business code changes: Prohibited
- Runtime changes: None
- Build Runtime changes: None
- Release / Version changes: None

### Formal Lifecycle

当前状态：`DONE`。

- Candidate Review: PASS
- Focused Re-review: PASS
- Implementation: COMPLETE / INTEGRATED IN CODEX/INTEGRATION
- External Actions: REMOTE PUSH + TEAM MIGRATION SIMULATION COMPLETE; NO FURTHER EXTERNAL ACTION AUTHORIZED
- Git Bundle Recovery Source: VALIDATED
- Remote Recovery Model B: VERIFIED
- Team Migration Simulation: PASS
- Independent Test: PASS
- Independent Review: PASS
- Coordinator Integration Gate: PASS

All lifecycle gates through Coordinator Integration Gate 已通过；T-010 已满足 DONE 条件。

## 任务边界

- 所属范围：Multi-Codex Coordination / Recovery / Backup / Team Migration
- 目标行为：建立最小灾备与跨 Team 恢复能力，使旧 Chat、Team、Worktree 或本机环境失效后，可以通过 Repository、Git、Recovery Runbook 和可安全恢复的环境配置重新恢复 Coordinator、Data、AI、UI。
- 本任务不做：Health Check、Archify Freshness、Obsidian Knowledge Promotion、Room Migration 修复、Ownership 修改、四树重构、业务开发、Release 或版本号修改。
- 前置依赖：Phase A Planning Review PASS；Git / Worktree 基线已确认；T-009 保持 DONE。

## Scope

### A. Repository Recovery

- 识别可验证 Recovery Source。
- 读取 Git refs、Project State、Task、Role State 和 Handoff 后确定目标恢复 Commit。
- 恢复 Repository、Branch、refs 与 Worktree。
- 验证项目 AGENTS、Workflow、Ownership 和 Role State。

### B. Coordinator Recovery

- 旧 Coordinator Chat 丢失后，仅依赖 Repository 和 Git 恢复角色、边界、当前任务与下一步。
- 输出标准 Recovery Report，不把旧 Chat 作为恢复依赖。

### C. Module Recovery

- Data / AI / UI 可分别通过 Branch、Role State、Current Task、Ownership、相关代码和 Git Resume。
- 不要求模块 Agent 默认读取整个 Repository。

### D. Team Migration

- 覆盖 Team A → Team B 场景。
- 环境恢复范围包括 Global AGENTS、Codex Config、Skills、Plugins、MCP、CC Switch、Model Provider、Obsidian、Archify 和 Secret placeholders。
- 只提供恢复协议，不在本 Candidate 阶段执行迁移。

### E. Disaster Recovery 基础验证

- 未来只在临时目录执行 Repository restore、expected HEAD、Branch refs、Worktree reconstruction、AGENTS、Role State 和 Coordinator Resume 验证。
- 不得使用正式 Worktree 做破坏测试。

## Out of Scope / Known Existing Risk

### 明确排除

- Room v5 Migration 修复
- DatabaseModule 修改
- destructive fallback 修改
- Project Health Check
- Archify Freshness 实现或 refresh
- Obsidian Knowledge Promotion 或写入
- Ownership 修改
- 四树重构
- Business Code、Release、Version 修改

### Known Existing Risk

- `AppDatabase.kt` 当前 version 为 5。
- `MIGRATION_4_5` 已定义，但 `DatabaseModule.kt` 未注册。
- 当前仍启用了 destructive fallback。
- `M01-本地数据与仓库.md` 与 `02-系统架构.md` 对 Room v5 的描述可能失真。
- 该风险仅记录，不在本任务中修复，未来应进入独立 High Risk / Data Safety Task。

## 验收示例

### Case A — Coordinator Chat Lost

- Given：旧 Coordinator Chat 不可访问，Repository 正常。
- When：新 Coordinator 按 RECOVERY 执行 Resume。
- Then：能识别 Role；确认 Branch / HEAD；读取 Project State / Task / Handoff；输出下一步；不依赖旧 Chat。

### Case B — Worktree Lost

- Given：Role Branch 仍存在，对应 Worktree 丢失。
- When：恢复者执行 Worktree Reconstruction Protocol。
- Then：Runbook 指导验证 ref / registry / destination；只在授权后重建；不使用 destructive reset；恢复后 Branch、HEAD 和 Role State 正确。

### Case C — Team Migration

- Given：旧 Team / Project / Chat 全不可用，但有有效 Recovery Source。
- When：按 Team A → Team B 恢复流程执行。
- Then：可恢复 Repository；可恢复四角色；可重新连接外部环境；不要求读取旧 Chat。

### Case D — Obsidian Unavailable

- Given：Repository 与 Git 已恢复，但 Obsidian 暂不可用。
- When：继续执行 Coordinator / Module Resume。
- Then：项目仍可继续开发；只损失长期知识增强；不把 Obsidian 变成 hard dependency。

### Case E — Archify Unavailable

- Given：Repository 与 Git 已恢复，但 Archify 暂不可用。
- When：继续恢复或开发。
- Then：Code / Git 仍为真相；架构图可稍后重新生成；不阻塞 Recovery。

上述示例是 Acceptance / planned validation，当前均未标记 PASS。

## TDD / 验证记录

### 1. 失败测试

- TDD: Not applicable to documentation changes.
- Protocol / Recovery validation replaces code-level TDD.
- 当前尚未执行恢复验证，因此没有失败测试或恢复测试 PASS。

### 2. 最小实现

- 拟新增：`docs/coordination/RECOVERY.md`
- 拟修改：`docs/coordination/WORKFLOW.md` 的最短 Recovery 入口
- 拟登记：`docs/project/04-任务与验收清单.md`、`docs/project/tasks/README.md`
- 不新增：第二套 Task System、Recovery Database、自动化平台或服务端组件

### 3. 当前验证结果

| 验证项 | 结果 | 证据 |
| --- | --- | --- |
| Candidate static review | PASS | 初始 Candidate Review 为 FAIL；Minimal Repair 后 Focused Re-review 为 PASS |
| Bundle Authorization | COMPLETE | User-approved T-010 Git Bundle Creation + Validation Phase |
| Git Bundle | PASS | Bundle 已创建并保留在 E 盘 |
| Bundle Verify | PASS | `git bundle verify` exit 0；complete history |
| Temporary Clone | PASS | 仅从 Bundle clone；fsck、refs、commits、project truth files 均通过 |
| Fetch | PASS | `git fetch origin --no-tags` exit 0；local branches preserved |
| Remote Truth | VERIFIED FOR FETCHED TIPS | FETCH_HEAD 确认 main / codex/release-v1.1.0 |
| Push | PASS | main / integration / data / ai / ui 已推送并逐一验证 |
| Team Migration Simulation | PASS | Remote-only clone、four role branches、four Worktrees、Coordinator/Module recovery 全部通过 |
| Independent Test | PASS | Initial Test FAIL（stale RECOVERY current-state mirror）；Minimal Repair `a2877f5`；Targeted Re-test PASS |
| Independent Review | PASS | Acceptance SATISFIED；Test Evidence ACCEPTED；Recovery Evidence SUFFICIENT；Truth Source / Lifecycle Consistency PASS；BLOCKER/IMPORTANT None |
| Coordinator Integration Gate | PASS | Candidate identity、scope isolation、main ancestry、truth source、recovery boundary、Secret boundary 全部通过 |

### 4. Bundle Recovery Evidence

- Path: `E:\SmartStorageAssistant\Backups\smart-storage-T010-76408b1-20260912-145033.bundle`
- Size: `5,425,079 bytes`
- SHA-256: `795161C9D25399D32670573AB41C7253B30974D16320F5B7A86F73AD9B56D366`
- Classification: `OFF-DEVICE / PRIVATE SOURCE ARCHIVE`
- History completeness: `COMPLETE`
- Branches: 8
- Tags: 3
- Missing refs: None
- Temporary clone cleanup: COMPLETE
- Bundle retained: Yes
- Legacy dirty data: `NOT PROTECTED BY GIT BUNDLE`；10ba `5 modified / 4 untracked`，baee `1 modified / 2 untracked`

### 5. Remote Recovery Evidence

- Remote Recovery Model: B
- GitHub main: AVAILABLE / VERIFIED → `c2faa5fe0fbb97233df2095e5e5a405bafa81017`
- GitHub Coordinator: AVAILABLE / VERIFIED → `2973e0a40ebb1723668ce3b9168c69fe55d3743a`
- GitHub Data: AVAILABLE / VERIFIED → `c2faa5fe0fbb97233df2095e5e5a405bafa81017`
- GitHub AI: AVAILABLE / VERIFIED → `c2faa5fe0fbb97233df2095e5e5a405bafa81017`
- GitHub UI: AVAILABLE / VERIFIED → `c2faa5fe0fbb97233df2095e5e5a405bafa81017`
- Post-push `ls-remote`: PASS
- Local refs preserved: Yes
- Working Tree preserved: Yes
- Force: No
### 6. Team Migration Simulation Evidence

- Remote Recovery Model B: PASS
- Team A → Team B Remote Clone: PASS
- Four Role Branch Recovery: PASS
- Four Worktree Reconstruction: PASS
- Coordinator Chat-loss Recovery: PASS
- Module Chat-loss Recovery: PASS
- AGENTS / Workflow / Ownership Recovery: PASS
- No Old Chat Dependency: PASS
- Archify Hard Dependency: NO
- Obsidian Hard Dependency: NO
- Full Machine Recovery: NOT VERIFIED
- Legacy dirty protection: PENDING SEPARATE DECISION
### 7. 未来验证方式

| 验证项 | 未来责任 | 证据要求 |
| --- | --- | --- |
| Repository restore | Test | 临时目录 clone / fetch 后提交身份一致 |
| Expected HEAD / refs | Test | `rev-parse` 与 Branch refs 对照 |
| Worktree reconstruction | Test | 临时路径创建并验证 Branch / HEAD / status |
| Coordinator Resume | Test | 无旧 Chat 条件下输出恢复报告 |
| Secret safety | Review | Repository diff 无 Secret 实际值 |
| Architecture preservation | Review | Ownership、Worktree、业务代码无变化 |

## 未来实施验收条件

- Recovery Runbook 存在，且覆盖 Repository、Coordinator、Module、Worktree、Team Migration 和环境恢复。
- Remote / Bundle / Full Clone 三类 Recovery Source 均有适用边界和验证方法。
- linked Worktree 不被描述为完整 Git backup。
- 记录 main `.git` 单点风险及至少一种安全备份策略。
- Secret boundary 清晰，Repository 不包含任何 Secret 实际值。
- 临时目录的独立灾备模拟通过，且不依赖旧 Chat。
- 不改变现有 Ownership、Workflow 主体、业务 Runtime 或版本号。
- v1.2.0 保持未发布。

## 实施日志（短期）

- 2026-09-12：Phase A Read-only Audit completed。
- 2026-09-12：T-010 Candidate created。
- 2026-09-12：第一次 Candidate Review executed；结果为 FAIL。
- 2026-09-12：执行 Minimal Repair，范围仅为 Formal Task structure 与 Environment Recovery Inventory。
- 2026-09-12：Focused Candidate Re-review executed；结果为 PASS。
- 2026-09-12：Candidate Commit Gate preflight PASS；状态转换为 READY / 待开始，实施尚未开始。
- 2026-09-12：Git Bundle Creation + Validation 获得用户明确授权；执行 lifecycle activation，状态转为 IN_PROGRESS。
- 2026-09-12：创建 off-device Git Bundle；`bundle verify` 与 `list-heads` PASS。
- 2026-09-12：仅从 Bundle 执行 temporary clone；refs、commits、project truth files 与恢复状态 PASS。
- 2026-09-12：删除临时 clone，保留 Bundle。
- 2026-09-12：Remote Truth Fetch 与 Public Remote Sanitization PASS。
- 2026-09-12：Remote Recovery Model B 建立；main、integration、data、ai、ui 逐一推送并 verified。
- 2026-09-12：仅从 GitHub Remote clone；four role branches / Worktrees / Coordinator / Module Recovery PASS。
- 2026-09-12：Team Migration Simulation 临时目录已删除；下一阶段独立 Test / Review。
- 2026-09-12：Independent Test PASS；Minimal Repair `a2877f5` 后 Targeted Re-test PASS。
- 2026-09-12：Independent Review PASS；进入 Coordinator Integration Gate。
- 2026-09-12：Coordinator Integration Gate PASS；scope isolation、main ancestry、truth source、recovery boundary 与 Secret boundary 全部通过。

## Safety Gates

以下动作均需要独立 User Gate；已完成的授权不会自动扩展到未来动作：

- `git fetch`
- 创建 Git Bundle
- 任意 `git push`
- 任何正式 Worktree reconstruct / remove / repair / relocate / reset / clean / force / delete / history rewrite

## 完成结论

- Status: `DONE`
- Current Phase: DONE / COMPLETE
- Formal Task: ESTABLISHED
- Recovery Runbook: ESTABLISHED / VALIDATED
- Git Bundle Recovery Source: VALIDATED
- Off-device Git Recovery: AVAILABLE
- Remote Recovery Model B: VERIFIED
- GitHub main / integration / data / ai / ui: AVAILABLE / VERIFIED
- Team Migration Simulation: PASS
- Full Machine Recovery: NOT VERIFIED
- External Actions: NO FURTHER EXTERNAL ACTION AUTHORIZED
- Independent Test: PASS
- Independent Review: PASS
- Coordinator Integration Gate: PASS
- T-010 已满足 DONE 条件；main propagation、remote final sync 与 module resync 不属于本 Gate。
