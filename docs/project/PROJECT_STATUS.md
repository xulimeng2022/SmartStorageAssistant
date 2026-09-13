# 智能收纳助手 · 项目状态

> 本文件只记录会随时间变化的当前事实：版本、阶段、测试与构建状态、lint、待用户验收项、已知问题。
> 写入规则：只有主控 Codex 与 Release 流程可以更新；模块 / Review / 测试 Agent 只读，发现过期信息报告主控。
> 不承载任务管理（任务卡见 `04-任务与验收清单.md` 与 `tasks/`），不写临时日志与命令输出。

- 最近更新：2026-09-13（T-022 真机图标安全区返修已集成并重建正式签名 Release；GitHub Release / Tag 未执行，真机视觉待复测）

## 当前版本与阶段

- 当前版本：`1.2.0`（versionCode 6，见 `app/build.gradle.kts` 当前实际配置）。
- 当前阶段：v1.2.0 最终本地发布包已按 T-022 重建、R8 混淆、v2+v3 正式签名并归档；尚未 Push / Tag / GitHub Release，真机视觉待复测。
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
- T-011 历史问题与 A1–A8 收口：`IMPLEMENTED / 待真机`。
- T-013 RC 专项回归核验与缺陷收口：`DONE / INTEGRATED / READY_FOR_USER_DEVICE_ACCEPTANCE`；4 个 BLOCKER 已闭环。
- T-019 校园邮箱与设置布局：`DONE / INTEGRATED / DEVICE_VISUAL_PENDING`。
- T-020 合并未分配照片：`DONE / INTEGRATED / DEVICE_PERSISTENCE_PENDING`。
- T-021 UI 1.2.0 最终 App 图标替换：`SUPERSEDED_BY_T-022`。
- T-022 App 图标安全区返修：`FINAL_PACKAGE_BUILT / DEVICE_VISUAL_PENDING`。

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
- T-010 Recovery / Backup / Team Migration：`DONE / 已验收 @ codex/integration`。
- Full Machine Recovery：`NOT VERIFIED`；Legacy dirty recovery：`PENDING SEPARATE DECISION`。
- Legacy Worktrees `10ba` / `baee`：保持隔离，未修改 dirty 数据。

## 测试与构建状态

- RC2 集成后自动验证：22 个测试套件 / 107 个 JVM 单元测试通过；Debug 与 Release/R8 构建通过；v4→v5→v6 迁移 SQL 以隔离 SQLite 数据库验证通过。
- 本轮代码包含 Room v6/索引生命周期、导入事务、照片索引、合并、语音纠错、邮箱、检查更新、完全删除数据，以及 T-013 的数据安全/索引修复和 T-019/T-020 的邮箱标签、设置布局、合并未分配照片修复。
- T-021 图标验证：`:app:processDebugResources`、`:app:assembleDebug`、`:app:assembleRelease` 在 ASCII 临时目录 PASS；107 JVM tests PASS；APK 内 adaptive XML 与 nodpi PNG 引用检查 PASS；lint 仅有既有 Manifest 相机硬件声明 error。
- T-022 安全区验证：foreground 缩放至原视觉约 85%，圆形 / 圆角矩形 / Squircle 预演星光与立方体完整；`:app:assembleDebug` 与 `:app:assembleRelease` PASS。
- 最终本地发布包：`app/release/智能收纳助手-1.2.0.apk`，版本 `1.2.0/6`，SHA-256 `3FAC4FEC1F3291161E53EEDE619276E499137A8873C80B9138A8A039C64CCD5E`，v2+v3 正式签名通过，证书 SHA-256 `0511094a1de87d24d3ed3dc3fd6d731f8158e40170ce85255ccecce74282525c`；mapping 归档于 `release/v1.2.0/mapping.txt`。
- Android Room 仪器测试、真机覆盖升级、真实网络/视觉 API 与完整清理流程尚未执行，不代表已通过真机验收；T-013 三方 findings 中仍有少量 IMPORTANT/OPTIONAL（含批量合并中文标点）留在后续体验项。

- 旧 RC2：`app/release/智能收纳助手-1.2.0-rc2.apk`，SHA-256 `11A2F25E08F2950DD48CEBBFB02B8B30B4DC16766FADA8F3372B1EBB7DCF2BF7`；不包含 T-021 最终图标，保留仅作历史对照。
- 最终 APK 与旧 RC2 使用同一正式证书；由于 versionCode 同为 6 且签名一致，可作为同一测试/发布轨道的覆盖安装包。
- 旧候选 `app/release/智能收纳助手-1.2.0-rc.apk` 保留仅作对照，不再作为当前真机候选。

## Lint

- 仍有既有 Manifest 相机硬件声明 error，共 114 warnings；T-021 未新增图标资源 error。

## 待用户验收

- v1.1.0 覆盖安装候选包的数据保留、单图索引创建/删除、批量合并、语音例子、完全删除数据、检查更新、邮箱、三语言和深色模式仍待真机验收。
- T-013 修复后的设置页 Dialog、索引动作语义、关闭图片理解后停止后续上传、导入取消边界和清空失败反馈需在真机复验。
- T-019 的当前手机宽度、320dp、1.5 倍字体、三语和深色模式布局需在真机复验；只打开删除确认入口，不执行真实清空。
- T-020 的“合并→取消→再打开”和“合并选图→保存→关闭并重启→详情打开照片”需在真机复验。
- T-021 的桌面 Launcher、圆形 / Squircle mask、Android 12+ Splash、设置应用信息、最近任务和 App 内旧 Logo 需在新构建真机复验。

## 已知问题

- 百川最新模型 ID 无法从官方 API 文档零猜测确认，列表中明确标注「待官方确认」。
- 已修复·数据安全：`MIGRATION_4_5` 已注册，新增 `MIGRATION_5_6` 并移除破坏性回退；仍需真机覆盖升级最终确认。
- 已修复·代码质量：`ItemRepositoryImpl` 的未使用 `ImageIndexingCoordinator` 注入已移除。
