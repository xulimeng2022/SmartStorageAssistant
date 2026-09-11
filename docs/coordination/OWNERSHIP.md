# Multi-Codex Ownership

> 本文件是智能收纳助手模块 Ownership 的唯一真相源。
> 只回答“谁默认拥有、谁默认可写”，不记录项目状态、任务、Handoff、风险处置方案或完整 Workflow。

## Core Rules

- 长期角色统一为：主控（Coordinator）、UI、Data、AI。
- `主控` 是中文正式称呼，`Coordinator` 是英文技术名称；`codex/integration` 是主控分支，`integration` 只表示职责、流程或分支概念，不作为角色名称。
- 共同依赖不等于共同 Ownership。一个长期受管文件或范围只有一个 Long-term Owner 和一个默认 Writer。
- 其他模块可以读取、消费公开 contract、提出 Cross-module Request，但默认不得直接修改。
- Ownership 按实际职责、架构边界、行为维护责任和唯一 Writer 适配性判断，不按目录名机械判断。
- Ownership 与 Coupling Hotspot 分离：存在跨模块耦合不改变 Owner，也不产生多 Owner 或 Ownership Pending。

## Single Writer Principle

- 每个长期受管文件或范围必须有明确的 Long-term Owner。
- 每个文件或范围只有一个 Long-term Owner 和默认 Writer；同一文件或范围在同一任务 / 集成窗口内只能有一个实际 Writer。
- 主控可以针对明确的跨模块任务临时指定非 Owner 作为 Single Writer，但必须限定任务、文件/范围和原因；临时授权不会改变该文件的 Long-term Owner。
- AI 可以调用 Data 提供的公开 contract，但默认无权修改 Room schema、Entity、DAO、Migration 或 Data Repository implementation。
- 其他模块需要修改非本人 Ownership 的文件时，必须先提出 Cross-module Request，由主控确认范围和唯一 Writer。

## Ownership 判断原则

1. 先判断文件维护的行为、架构边界和长期唯一 Writer，而不是先看物理目录。
2. `data/remote/llm/**` 和 `data/remote/vision/**` 虽位于 `data/` 下，仍归 AI。
3. `DatabaseModule` 虽位于 `di/` 下，仍归 Data；`di/**` 不整体归主控。
4. Gradle 文件不整体归主控；App/root 集成构建配置归主控，模块内部专属配置按实际职责判断。
5. `res/values*/**` 不整体归 UI；localization strings、Theme、UI colors、Presentation-specific resources 和已确认的品牌视觉资源按当前职责归 UI，未来 App-level / Integration-level resource 重新判定。
6. 无法通过现有职责边界静默归类的范围，由主控显式裁定，不写入多个 Owner。

## Role Ownership

### 主控（Coordinator）

- `SmartStorageApp.kt`
- `RemoteModule.kt`
- `RepositoryModule.kt`
- 真正跨模块的 DI 装配
- `AndroidManifest.xml`
- `res/xml/file_paths.xml`
- App/root 级集成与构建配置
- 中央协调文档、项目状态文档和跨模块公共规则（不含角色专属动态状态文件）

### UI

- `presentation/**`
- Compose Screen / Component
- ViewModel
- UI State / Event
- Navigation
- Theme
- `UiMessage`
- `MainActivity.kt`
- localization strings
- UI colors
- Presentation-specific resources
- Launcher / 品牌视觉资源

### Data

- Room、Entity、DAO、Migration
- `AppDatabase`
- `DatabaseModule`
- `data/local/**`
- ImageStorage
- prefs
- backup persistence
- Repository implementation
- Mapper
- Domain Repository contract
- 通用 Data / Domain contract
- `image_ai_indices` schema / entity / table

### AI

- `data/remote/llm/**`
- `data/remote/vision/**`
- AI Provider / Transport
- AI Prompt / Parser
- Vision
- AI-only 类型
- AI-only tests
- `ImageIndexingCoordinator.kt`

### 未来角色状态文件（当前未创建）

- `docs/coordination/Coordinator.md` → 主控（Coordinator）
- `docs/coordination/Data.md` → Data
- `docs/coordination/AI.md` → AI
- `docs/coordination/UI.md` → UI

`docs/coordination/**` 不整体归主控；中央协调文档与跨模块公共规则归主控，角色专属动态状态文件归对应角色。

## Ownership Map

> `其他消费者` 仅表示实际代码、contract、调用或集成依赖方。Agent 可以只读查看其他模块文件，但只读查看不构成消费者。纯模块内部范围填写 `—` 和 `否`。

| 文件/范围 | Long-term Owner | 其他消费者 | Shared Touchpoint | 默认 Writer | 修改规则 | 架构备注 |
| --- | --- | --- | --- | --- | --- | --- |
| `presentation/**`、Compose、ViewModel、Navigation、Theme、`UiMessage`、`MainActivity.kt` | UI | — | 否 | UI | UI 默认唯一 Writer | ViewModel 直接依赖下层时仍归 UI，记录为 Coupling Hotspot |
| localization strings、UI colors、Presentation-specific resources、Launcher / 品牌视觉资源 | UI | — | 否 | UI | UI 默认唯一 Writer | 不据此推断所有 App-level 资源均归 UI |
| `data/remote/llm/**`、`data/remote/vision/**` | AI | UI / Data 消费 contract | 是 | AI | AI 默认唯一 Writer | 物理路径位于 `data/` 不改变 AI Ownership |
| AI Provider、Transport、Prompt、Parser、Vision、AI-only 类型与测试 | AI | Data 持久化结果，UI 展示 | 是 | AI | AI 默认唯一 Writer | 纯 AI 行为与类型归 AI |
| Room、Entity、DAO、Migration、`AppDatabase` | Data | — | 否 | Data | Data 默认唯一 Writer | schema、迁移和持久化 contract 不得由 AI / UI 直接修改 |
| `DatabaseModule.kt` | Data | — | 否 | Data | Data 默认唯一 Writer | 数据库构建和迁移注册属于 Data，不因位于 `di/` 归主控 |
| `data/local/**`、ImageStorage、prefs、backup persistence | Data | UI / AI 调用 | 是 | Data | Data 默认唯一 Writer | 图片文件、偏好和备份持久化统一归 Data |
| `data/repository/**`、Mapper、Repository implementation | Data | UI / AI 消费 | 是 | Data | Data 默认唯一 Writer | 仓库实现和映射属于数据边界 |
| `domain/repository/**`、通用 Data / Domain contract、通用 UseCase | Data | UI / AI 消费 | 是 | Data | Data 默认唯一 Writer | Repository contract 和通用领域 contract 由 Data 维护 |
| `ImageAiIndexRepository.kt` | Data | AI 写分析状态，UI 读进度 | 是 | Data | AI 通过公开方法调用；改 contract 先提 Cross-module Request | 持久化、状态迁移和 JSON 编解码归 Data |
| `VisualSearchEngine.kt` | Data | UI 当前直接调用 | 是 | Data | Data 默认唯一 Writer | 纯本地索引检索与排序，无网络职责 |
| `ImageUnderstandingRepository.kt` | Data | AI 读写状态，UI 展示 | 是 | Data | Data 默认唯一 Writer；AI 改语义先提 Cross-module Request | 当前主要是持久化状态与隐私开关 |
| `SettingsRepository.kt` | Data | AI 读取配置，UI 编辑配置 | 是 | Data | Data 默认唯一 Writer | 加密、DataStore 和旧配置迁移归 Data |
| `ItemRepositoryImpl.kt` | Data | UI 经 UseCase 消费 | 是 | Data | Data 默认唯一 Writer | 不因调用方需要而转移 Owner |
| `BackupRepository.kt` | Data | UI 触发备份恢复 | 是 | Data | Data 默认唯一 Writer | AI 索引数据可被备份，但 schema 仍由 Data 管理 |
| `ImageIndexingCoordinator.kt` | AI | UI / 主控 | 是 | AI | AI 默认唯一 Writer；改 Data contract 先提 Cross-module Request | AI-owned Coupling Hotspot；Data 侧 `ItemRepositoryImpl` 仍有未使用注入，待后续清理 |
| `SmartStorageApp.kt` | 主控（Coordinator） | — | 否 | 主控（Coordinator） | 主控默认唯一 Writer | App bootstrap 与跨模块启动入口 |
| `RemoteModule.kt`、`RepositoryModule.kt`、跨层 DI 装配 | 主控（Coordinator） | AI / Data / UI 消费 | 是 | 主控（Coordinator） | 主控默认唯一 Writer | 跨层基础设施和接口绑定装配 |
| `AndroidManifest.xml`、`res/xml/file_paths.xml` | 主控（Coordinator） | UI / Data（FileProvider 文件路径集成） | 是 | 主控（Coordinator） | 主控默认唯一 Writer | App-level integration contract |
| App/root 级集成构建配置、中央协调与项目状态文档 | 主控（Coordinator） | — | 否 | 主控（Coordinator） | 主控默认唯一 Writer | 模块内部专属配置仍按模块职责判断 |

Remaining Pending Ownership: None.

数据库迁移与安全变更由 Data 负责；风险等级、测试方案、任务编号和修复计划不记录在本文件。

## Coupling Hotspots

| 文件/范围 | Owner | 当前耦合 | 后续方向 |
| --- | --- | --- | --- |
| `HomeViewModel.kt` | UI | 直接依赖 LLM、Vision、视觉索引仓库、图片理解状态和本地视觉搜索 | 后续通过 UI-facing facade / UseCase 收口 |
| `SettingsViewModel.kt` | UI | 直接依赖 AI 配置、备份、视觉状态、索引协调器和索引仓库 | 后续按配置、视觉索引、备份用例边界收口 |
| `AddItemViewModel.kt` | UI | 直接依赖 LLM 和 ImageStorage | 后续通过 UseCase 或 UI-facing service 隔离 |
| `ItemDetailViewModel.kt` | UI | 直接依赖 ImageStorage | 后续增加照片操作 UseCase |
| `ImageIndexingCoordinator.kt` | AI | 调用 Data-owned 索引仓库和状态仓库 | AI 通过稳定 Data contract 工作，跨边界修改先请求主控 |
| `ImageAiIndexRepository.kt` | Data | AI / UI / Backup 消费持久化实体 | 后续评估内部 entity 与外部 DTO 分层 |
| `BackupRepository.kt` | Data | 直接处理 AI 索引 DAO / entity | 后续定义备份 DTO，降低 Room entity 耦合 |
| `VisualSearchEngine.kt` | Data | UI 当前直接调用 Data object | 后续通过 Data facade 或 UseCase 暴露 |

Coupling Hotspot 只记录耦合与后续方向，不改变 Owner，也不等于 Ownership Pending。

## Cross-module Change Rule

```text
模块发现需要修改其他 Owner 的文件
↓
提出 Cross-module Request
↓
主控确认文件、范围、原因和唯一 Writer
↓
由文件 Owner 或主控明确指定的 Single Writer 修改
```

完整 Cross-module Request 模板和流程由 Phase 3 的 `WORKFLOW.md` 定义，本文件不复制完整 Workflow。