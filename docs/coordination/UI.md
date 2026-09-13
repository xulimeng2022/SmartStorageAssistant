# UI State

- Role: UI
- Branch: codex/ui

## Current Task

- ID / Title: T-021 UI 1.2.0 最终 App 图标替换
- Status: COMPLETE / READY_FOR_INTEGRATION
- Risk: Medium
- Goal: 以用户提供的 1254×1254 PNG 为唯一视觉基准，替换 adaptive launcher / round icon 与 Android 12+ 默认 splash 的图标来源，并清理旧图标资源。
- Allowed Scope: Launcher / 品牌视觉资源、adaptive XML、M07、本角色 State
- Acceptance: 新 background / foreground 被 launcher 与 round 引用；旧 vector / color 无引用；圆形与圆角遮罩预演无主体裁切或双层圆角；资源处理、Debug / Release、107 JVM tests 通过；真机视觉待验。
- Task Card: docs/project/tasks/T-021-UI-1.2.0最终App图标替换.md

## Progress

- Completed: 生成 768×768 `ic_launcher_background_image.png` 与 `ic_launcher_foreground_image.png`；前景保留原图白色立方体与星光，背景为同源蓝紫渐变；旧 `ic_launcher_foreground.xml` 与旧背景 color 已删除；圆形 / 圆角遮罩预演通过。
- Previous: T-019/T-020 校园邮箱与设置布局、合并未分配照片已集成到 `codex/integration`，仍待设备视觉与持久性复测。
- In Progress: None

## Working Tree

- State: Clean after docs-only Handoff commit
- Changed: None

## Validation

- Source：1254×1254 PNG，SHA-256 `4D32F0B84D8963EDF121BDFB01EE20951A755E76AA21C65486C11F5CBDCA94BC`；未重新设计主体。
- Resource processing：`:app:processDebugResources` PASS。
- JVM：ASCII 临时目录中 107 tests PASS。
- Debug：ASCII 临时目录中 `:app:assembleDebug` PASS。
- Release / R8：ASCII 临时目录中 `:app:assembleRelease` PASS；为未签名 APK。
- Lint：仅既有 Manifest 相机硬件声明 error，共 114 warnings；无图标资源 error。
- APK 资源：Manifest 的 icon / roundIcon、两个 v26 adaptive XML、两个 nodpi PNG 均已打包。
- Android 真机：Launcher、圆形 / Squircle mask、Splash、最近任务、应用信息、App 内 Logo 均 NOT VERIFIED。

## Last Handoff

- Task / Source Branch: T-021 / codex/ui
- Commits: implementation `a378a32`; integration `2b4a616`
- Summary: 最终图标已替换为独立 background / foreground adaptive 资源，旧资源已清理，静态验证通过。
- Previous Handoff: T-020 / `7fff55e` / integration `cfe5a5d`；合并候选包含未分配照片，真机取消重开与保存重启待复测。
- Known limitation: Android 12+ 未声明独立 splash 资源，系统 splash 默认跟随 launcher icon；Launcher 缓存可能要求卸载重装后复查。

## Blockers

- 当前无设备连接，真机视觉与 T-019/T-020 持久性尚未验证。

## Next Step

- Coordinator 完成 T-021 中央任务登记与状态同步；用户在真机安装新构建并复测各 Launcher mask、Splash，以及既有 T-019/T-020 流程。