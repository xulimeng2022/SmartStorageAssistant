# 智能收纳助手 · 项目状态

> 本文件只记录会随时间变化的当前事实：版本、阶段、测试与构建状态、lint、待用户验收项、已知问题。
> 写入规则：只有主控 Codex 与 Release 流程可以更新；模块 / Review / 测试 Agent 只读，发现过期信息报告主控。
> 不承载任务管理（任务卡见 `04-任务与验收清单.md` 与 `tasks/`），不写临时日志与命令输出。

- 最近更新：2026-09-13（v1.2.0 GitHub Release 与 Tag 已正式发布；用户确认此前本轮事项完成；T-024 Coordinator 会话与 Worker Thread 生命周期规则落地；AI Bridge V1.2 与 BRIDGE-PLAN-003 Dynamic Thread Naming / Workflow Cost Guardrails 已外部验收；爱发电主页认证、网盘排序/小字与 README 用户化待办仍未完成；Tag provenance 差异已记录）

## 当前版本与阶段

- 当前版本：`1.2.0`（versionCode 6，见 `app/build.gradle.kts` 当前实际配置）。
- 当前阶段：v1.2.0 已按 T-022 重建、R8 混淆、v2+v3 正式签名并发布到 GitHub；Tag `v1.2.0` 指向 `f5aa8988e45c291fe8bf6ff9f4b7f17178153ec5`，其 App tree 与最终 APK 源码 tree 不同。用户于 2026-09-13 确认此前本轮事项完成；该确认不覆盖其后新增的两项待办。
- Phase 3A — Coordination Infrastructure：`COMPLETE`。
- Phase 3B — Global Status Reconciliation：`COMPLETE`。
- Phase 3C — AGENTS Workflow Entry：`COMPLETE`。
- Coordination Finalization：`COMPLETE`。
- Stable Candidate Review：`COMPLETE / PASS`。
- Stable main propagation：`COMPLETE / PASS`。
- v1.2.0 Preservation：`PASS`。
- Data / AI / UI Baseline Resync：`COMPLETE / PASS`。
- Baseline Resync：`COMPLETE / PASS`。
- Central Rule Git Propagation：`COMPLETE / PASS`。
- Lightweight Resume Validation：`COMPLETE / PASS`。
- Role State Finalization：`COMPLETE IN T-009 FINAL CHECKPOINT`。
- T-011 历史问题与 A1–A8 收口：`USER_CONFIRMED_COMPLETE / 技术原始证据未归档`。
- T-013 RC 专项回归核验与缺陷收口：`USER_CONFIRMED_COMPLETE / 技术原始证据未归档`；4 个 BLOCKER 已闭环。
- T-019 校园邮箱与设置布局：`USER_CONFIRMED_COMPLETE / 原真机视觉证据未归档`。
- T-020 合并未分配照片：`USER_CONFIRMED_COMPLETE / 原真机持久性证据未归档`。
- T-021 UI 1.2.0 最终 App 图标替换：`SUPERSEDED_BY_T-022`。
- T-022 App 图标安全区返修：`FINAL_PACKAGE_BUILT / USER_CONFIRMED_COMPLETE / 原真机视觉证据未归档`。

## Baseline 状态

- Frozen Baseline：`ee95f93b82e454ecebc3463e25fce51a9ec4569c`。这是四个长期 Worktree 创建时的历史共同基准。
- Integration / Coordination：已审核 Coordination Candidate 已成功 fast-forward 进入 main；后续分支精确 HEAD / ahead / behind 以 Git 为准。
- Stable Baseline：`main` 已包含通过审核的 Coordination Infrastructure，并成为当前新的 Stable Baseline；精确 HEAD 以 Git 为准。

## Multi-Codex 协调状态

- 永久角色 Worktree：主控（Coordinator）、Data、AI、UI 均已建立；Test / Review 仍按任务临时启动，不建立永久 Worktree。
- 长期分支：主控 `codex/integration`、Data `codex/data`、AI `codex/ai`、UI `codex/ui`。
- Ownership：已固化并进入 Stable main，唯一真相源为 [OWNERSHIP](../coordination/OWNERSHIP.md)；Remaining Pending Ownership 为 `None`。
- Workflow：已固化并进入 Stable main，唯一主要真相源为 [WORKFLOW](../coordination/WORKFLOW.md)。
- Role State infrastructure：已建立并进入 Stable main；Coordinator / Data / AI / UI 恢复快照机制已可用。
- Single Writer / Cross-module Request / Lightweight Resume / Handoff / Experiment isolation：`DEFINED`。
- 对话生命周期 / Chat Handoff：`DEFINED（T-024）`；Coordinator 主对话不自动创建，只生成 Chat Handoff 并建议用户手动新开。
- Worker Thread Reuse + Registry + REUSE-first Routing：`DEFINED（T-024）`；UI / AI / Data 默认复用健康 Worker Thread，NEW 需举证，Registry 只记录线程长期状态。
- AI Bridge V1.2：`COMPLETED / EXTERNAL_VERIFIED`；INDEX / CURRENT-STATE、Task Size / Risk Floor、Knowledge 指针、Writer 分离与 history 归档已落地；ChatGPT Connector 外部读取 PASS。
- Dynamic Thread Naming / Workflow Cost Guardrails：`COMPLETED / EXTERNAL_VERIFIED（BRIDGE-PLAN-003）`；Thread Title 只投影 Active Task；无 watcher / polling / 标题专用模型；5 个明确映射线程已纠偏。
- T-010 Recovery / Backup / Team Migration：`DONE / 已验收 @ codex/integration`。
- Full Machine Recovery：`NOT VERIFIED`；Legacy dirty recovery：`PENDING SEPARATE DECISION`。
- Legacy Worktrees `10ba` / `baee`：保持隔离，未修改 dirty 数据。

## 测试与构建状态
- BRIDGE-PLAN-003：静态规则一致性、UTF-8 无 BOM、引用与 `git diff --check` PASS；App 业务代码 / Gradle / 数据库 / 版本号 0 变更；Debug / Release NOT RUN（无 App 变更）。
- BRIDGE-PLAN-002：AI Bridge V1.2 本地静态验证 PASS；App 业务代码 / Gradle / 数据库 / 版本号 0 变更；Debug / Release 构建 NOT RUN（本任务无 App 代码变更）；外部 ChatGPT Connector PENDING。

- T-023：Archify 运行时图与协作图 showcase validate / deliver / Edge visual-check PASS；Obsidian 6 个笔记与 6 个入口同步；App 业务代码、Gradle、版本号 0 变更。
- RC2 集成后自动验证：22 个测试套件 / 107 个 JVM 单元测试通过；Debug 与 Release/R8 构建通过；v4→v5→v6 迁移 SQL 以隔离 SQLite 数据库验证通过。
- 本轮代码包含 Room v6/索引生命周期、导入事务、照片索引、合并、语音纠错、邮箱、检查更新、完全删除数据，以及 T-013 的数据安全/索引修复和 T-019/T-020 的邮箱标签、设置布局、合并未分配照片修复。
- T-021 图标验证：`:app:processDebugResources`、`:app:assembleDebug`、`:app:assembleRelease` 在 ASCII 临时目录 PASS；107 JVM tests PASS；APK 内 adaptive XML 与 nodpi PNG 引用检查 PASS；lint 仅有既有 Manifest 相机硬件声明 error。
- T-022 安全区验证：foreground 缩放至原视觉约 85%，圆形 / 圆角矩形 / Squircle 预演星光与立方体完整；`:app:assembleDebug` 与 `:app:assembleRelease` PASS。
- 最终本地发布包：`app/release/智能收纳助手-1.2.0.apk`，版本 `1.2.0/6`，SHA-256 `1C080AEC567814A314868DBD44F8B3A1CDE0CA42BFBEE326E3A41917B7C2A854`，v2+v3 正式签名通过，证书 SHA-256 `0511094a1de87d24d3ed3dc3fd6d731f8158e40170ce85255ccecce74282525c`；mapping 归档于 `release/v1.2.0/mapping.txt`。
- GitHub Release：[智能收纳助手 v1.2.0](https://github.com/xulimeng2022/SmartStorageAssistant/releases/tag/v1.2.0)；资产 `app-release-1.2.0.apk`，大小 `4,175,289` bytes，SHA-256 `1C080AEC567814A314868DBD44F8B3A1CDE0CA42BFBEE326E3A41917B7C2A854`，v2+v3 正式签名通过。
- Android Room 仪器测试、真机覆盖升级、真实网络/视觉 API 与完整清理流程尚未执行，不代表已通过真机验收；T-013 三方 findings 中仍有少量 IMPORTANT/OPTIONAL（含批量合并中文标点）留在后续体验项。

- 旧 RC2：`app/release/智能收纳助手-1.2.0-rc2.apk`，SHA-256 `11A2F25E08F2950DD48CEBBFB02B8B30B4DC16766FADA8F3372B1EBB7DCF2BF7`；不包含 T-021 最终图标，保留仅作历史对照。
- 最终 APK 与旧 RC2 使用同一正式证书；由于 versionCode 同为 6 且签名一致，可作为同一测试/发布轨道的覆盖安装包。
- 旧候选 `app/release/智能收纳助手-1.2.0-rc.apk` 保留仅作对照，不再作为当前真机候选。

## Lint

- 仍有既有 Manifest 相机硬件声明 error，共 114 warnings；T-021 未新增图标资源 error。

## 当前验收状态与待办

- 用户于 2026-09-13 确认“除爱发电主页认证外，其余事项完成”。这是用户验收事实，不替代未归档的真机日志、真实网络/视觉 API 原始结果或仪器测试。
- 用户确认后新增两项待办，当前仍未完成：
  1. 相关页面及类似页面的两个网盘下载入口下方小字统一为“国内网络优先”，百度网盘排在夸克网盘之前。
  2. GitHub 仓库 README 调整为面向普通用户的介绍。
- 爱发电主页认证继续保持“尚未通过”，不推断提交、审核或拒绝阶段。
- 技术证据边界：v1.2.0 Release 自动构建、签名、迁移 SQL 与 JVM 测试有记录；Android 仪器测试、长期真机覆盖升级、真实网络/视觉 API 的完整原始结果未归档。

## 已知问题

- 百川最新模型 ID 无法从官方 API 文档零猜测确认，列表中明确标注「待官方确认」。
- 已修复·数据安全：`MIGRATION_4_5` 已注册，新增 `MIGRATION_5_6` 并移除破坏性回退；仍需真机覆盖升级最终确认。
- 已修复·代码质量：`ItemRepositoryImpl` 的未使用 `ImageIndexingCoordinator` 注入已移除。
- 发布溯源：Tag `v1.2.0` 的 App tree 与最终 APK 源码 tree 不同；本轮只记录，不改 Tag、不重发。
- 新增待办：用户确认后的网盘入口排序/“国内网络优先”小字与 GitHub README 用户化尚未完成。
- 外部事项：爱发电主页认证尚未通过。
