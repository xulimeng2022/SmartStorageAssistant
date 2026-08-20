# 智能收纳助手

一款用于记录和管理家中物品存放位置的 Android 应用（初始脚手架）。

## 技术栈

- 语言：Kotlin（100%）
- UI：Jetpack Compose（Material 3）
- 架构：MVVM + Clean Architecture（UseCase + Repository 模式）
- 本地数据库：Room（SQLite）
- 依赖注入：Hilt
- 异步：Kotlin Coroutines + Flow

## 包结构

```
com.example.smartstorage
├── data                    # 数据层：Room 实体 / DAO / 数据库 / 仓库实现 / 映射
│   ├── local
│   │   ├── entity          # ItemEntity（物品清单表）
│   │   ├── dao             # ItemDao
│   │   └── AppDatabase
│   ├── mapper              # 实体与领域模型互转
│   └── repository          # ItemRepositoryImpl
├── domain                  # 领域层：模型 / 仓库接口 / 用例
│   ├── model               # Item
│   ├── repository          # ItemRepository 接口
│   └── usecase             # Observe/Add/Update/Delete 用例
├── di                      # Hilt 模块（DatabaseModule / RepositoryModule）
└── presentation            # 表现层：导航 / 页面 / ViewModel / 主题
    ├── navigation          # 路由与导航图
    ├── home                # 首页（物品清单）
    ├── add                 # 添加物品
    ├── settings            # 设置（占位）
    └── theme               # Material 3 主题
```

## 功能

- 底部导航栏：首页（清单）｜添加（中间悬浮按钮）｜设置
- 首页：实时展示物品清单（Room + Flow），空状态引导；DAO 支持按名称/地点模糊搜索
- 添加：物品表单（物品名必填、存放地点、备注），支持语音录入（SpeechRecognizer 实时中间结果），保存后写入本地数据库
- 语音输入：大圆形麦克风按钮，点击/按住说话，松开或再次点击停止，识别文本实时填入物品名
- 权限：Android 6.0+ 运行时动态申请 RECORD_AUDIO，INTERNET 在 Manifest 声明
- 设置：占位页面

## 物品清单表字段

| 字段 | 说明 |
| ---- | ---- |
| id | 唯一 ID（自增主键） |
| name | 物品名称 |
| location | 存放地点 |
| description | 详细描述 |
| created_at | 创建时间（毫秒时间戳） |
| updated_at | 最后修改时间（毫秒时间戳） |

## 运行方式

1. 使用 Android Studio（Ladybug 或更新版本）打开项目根目录。
2. 等待 Gradle 同步完成（首次会自动下载依赖，需要联网）。
3. 如未自动生成 `local.properties`，请确认 `SDK Location` 指向本机 Android SDK。
4. 选择 `app` 运行配置，连接设备或模拟器后点击运行。

> 也可在命令行执行 `./gradlew :app:assembleDebug` 构建。