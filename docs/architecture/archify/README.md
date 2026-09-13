# 智能收纳助手 · Archify 架构产物

> 本目录保存可由 Archify 重新生成的结构化架构图，不替代源码与 Git 事实。

## 当前基线

- 运行时图源码基线：`codex/integration@5e417bc` 的 App tree；与 T-022 最终打包提交 `7973391:app` 相同。
- 发布版本：`1.2.0 / versionCode 6`。
- 协作图文档基线：`codex/integration@5e417bc`。
- 已知发布溯源差异：远端 Tag `v1.2.0` 指向 `f5aa898`，其 App tree 与最终 APK 源码 tree（`7973391:app`）不同。本目录只记录差异，不修改 Tag。

## 图表

### 当前运行时架构

- JSON：`runtime/smart-storage-assistant-runtime-architecture.architecture.json`
- HTML：`runtime/smart-storage-assistant-runtime-architecture.html`
- Specification SHA-256：`52689d6fcc7eb4b477d971fafa64a3626f65bb33cf72a3bab864efdc35406991`
- Artifact SHA-256：`34f02a0400d45cd22fd996882725392e3490914e5ab95fef2220617a226cc11b`
- Showcase validate：9/9 checks，0 errors，0 warnings
- Visual-check：PASS（1440×900、1600×1000、1920×1080、2048×1320；浅色/深色）

### 开发协作与交付体系

- JSON：`collaboration/smart-storage-assistant-collaboration-delivery.architecture.json`
- HTML：`collaboration/smart-storage-assistant-collaboration-delivery.html`
- Specification SHA-256：`568b9fa0a11e48f31b8f3db68cb3cc5ad74be9d8fb3ad05821b373e25dd80251`
- Artifact SHA-256：`c504c357c3ee1bafa66b6db49e8cafbff1e1238685d46ffb56aa82aeb7c60e29`
- Showcase validate：9/9 checks，0 errors，0 warnings
- Visual-check：PASS（1440×900、1600×1000、1920×1080、2048×1320；浅色/深色）

## 重新生成

在 Archify Skill 目录执行，传入本仓库根目录：

```powershell
$repo = 'C:\Users\XU\.codex\worktrees\3aad\智能收纳助手'
node bin/archify.mjs validate architecture "$repo\docs\architecture\archify\runtime\smart-storage-assistant-runtime-architecture.architecture.json" --quality showcase --repo-root $repo --json
node bin/archify.mjs deliver architecture "$repo\docs\architecture\archify\runtime\smart-storage-assistant-runtime-architecture.architecture.json" "$repo\docs\architecture\archify\runtime\smart-storage-assistant-runtime-architecture.html" --quality showcase --repo-root $repo --json
```

`deliver` 成功后才能对精确 HTML 执行 `visual-check`。仓库证据校验需要可访问包含原点的 Git 仓库，并且当前环境应允许 Git 处理该工作树。

## 边界

- App 内 AI 调用与开发链路的 CC Switch / DeepSeek 是两条不同链路。
- 代码类 `ImageIndexingCoordinator` 与开发角色 Coordinator 不是同一对象。
- Archify 图是生成增强，不是发布包、任务状态或数据迁移的事实源。