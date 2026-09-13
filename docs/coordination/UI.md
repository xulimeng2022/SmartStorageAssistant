# UI State

- Role: UI
- Branch: codex/ui

## Current Task

- ID / Title: T-020 UI 1.2.0 真机反馈修复：合并预览包含未分配照片
- Status: COMPLETE
- Risk: High
- Goal: 批量合并候选包含本次添加会话中尚未分配给任何草稿的照片，同时排除仅属于未选中草稿的照片。
- Allowed Scope: `presentation/add/**`、UI tests、M09 与本角色 State
- Acceptance: 候选/默认选择/路径去重七项规则测试通过；相关 JVM 测试与 Debug 构建通过；真机保存重开另行验收。
- Task Card: docs/project/tasks/T-020-UI-1.2.0真机反馈修复-合并未分配照片.md

## Progress

- Completed: 新增 `BatchMergePhotoPolicy` 纯函数；`startBatchMerge()` 分别注入候选与默认选中照片；8 个目标 JVM 测试通过；T-019/T-020 已集成到 `codex/integration`。
- In Progress: None

## Working Tree

- State: Clean after docs-only Handoff commit
- Changed: None

## Validation

- Baseline: TDD 红灯已复现（策略实现前目标测试 unresolved reference）
- JVM: 107 tests PASS；其中 `BatchMergePhotoPolicyTest` 8 tests PASS
- Debug Build: PASS（`:app:assembleDebug`，app-debug.apk）
- Android instrumentation / real device: NOT VERIFIED；需复测“打开合并→取消→再打开”和“合并选图→保存→重启→详情打开照片”

## Last Handoff

- Task / Source Branch: T-020 / codex/ui
- Commits: implementation `7fff55e`; integration `cfe5a5d`; docs Handoff `1d9a7f4`; integration docs `45fe428`
- Summary: 合并候选按选中草稿精确照片并集加当前照片池未分配照片生成，按绝对路径去重；默认仅选来源照片，仅属于未选中草稿的照片排除，候选不受 9 张截断。
- Known limitation: 真机取消重开与保存重启后的照片可打开性未验证。

## Blockers

- None

## Next Step

- 已集成并生成 RC2；等待用户真机复测布局与合并持久性流程。
