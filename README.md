# 智能收纳助手

> 语音记录物品存放位置，再也不用担心找不到东西了。

智能收纳助手是一款基于 Android 的轻量级收纳管理工具。用户可以通过文字或键盘语音记录物品名称、存放地点和备注，也可以让 AI 从照片中建立可搜索的视觉索引，帮助找到“只记得长什么样、但没写进名称”的物品。

当前版本：**1.2.0（versionCode 6）**

## ✨ 主要功能

- 🎙️ **语音录入**：使用键盘语音说出物品和存放位置，再由 AI 提取结构化信息。
- 🤖 **AI 智能解析**：支持单条和批量识别；模型失败时保留原文并支持本地规则降级。
- 📦 **批量录入**：一次描述多个物品，逐条编辑、分配照片并确认保存。
- 🖼️ **多图附件**：每件物品最多 9 张照片，保存时压缩为 1080px 宽、JPEG 80%，并修正 EXIF 方向。
- 🔍 **搜索**：支持逐字模糊搜索、AI 三字段语义筛选、语音搜索和普通结果排序。
- 👁️ **AI 视觉找物**：照片分析一次后保存本地结构化索引，搜索时优先本地检索，不重复分析全部照片。
- 🧾 **历史照片索引**：可为历史正常物品建立索引，支持进度、暂停、继续、取消、失败重试和重启恢复。
- ✅ **Top-5 视觉复核**：本地搜索完成后，用户可手动点击“AI 图片复核”，最多上传 5 个候选照片做二次判断。
- 🔒 **隐私开关**：AI 图片理解默认关闭；首次开启前显示图片上传说明并探测当前模型能力。
- 🗑️ **回收站**：删除物品先软删除，可恢复；永久删除时同步清理照片和视觉索引。
- 🌙 **深色模式**：跟随系统或手动切换，采用固定微信式深灰层次。
- 🌐 **多语言**：简体中文、繁體中文、English，支持跟随系统和手动切换；Toast/Snackbar 等临时提示与界面同语言，切换后立即生效。
- 💾 **备份恢复**：导出 ZIP，支持覆盖/合并导入；备份格式 1.1 可包含正常物品的视觉索引。

## 👁️ AI 图片理解与视觉找物

1. 默认关闭。首次开启会显示隐私说明，并用应用内生成的合成测试图探测当前模型是否支持 OpenAI 兼容图片输入。
2. 探测成功后，新照片在物品保存成功后进入后台索引队列；视觉分析失败不会导致物品或照片保存失败。
3. 历史索引只处理正常物品，软删除物品保留已有索引；永久删除会清理索引。
4. 搜索先查原有名称/地点/备注和本地视觉索引。只有用户手动点击“AI 图片复核”时，才上传得分靠前的 Top-5 候选照片。
5. 视觉结果与用户填写的数据分离，AI 不会覆盖用户保存的名称、地点或备注。
6. 关闭 AI 图片理解后，App 不再发送新的视觉请求；历史索引默认保留，可另行清除。

当前视觉实现仅支持 OpenAI 兼容的 `chat/completions` 图片格式，不包含向量数据库、CLIP、整个手机相册扫描或 Bounding Box。

## 🔑 API 配置

- 自定义模式可配置 OpenAI 兼容的 Base URL、模型名称和 API Key。
- DeepSeek 推荐模型为 `deepseek-flash`（官方 V4.1 Flash，原生支持图片理解）；已保存的旧别名 `deepseek-v4-flash` / `deepseek-v4-flash-vision-exp` 继续按原配置兼容探测，不会删除或改写既有 Key、Base URL 与自定义模型。
- 免费模式当前使用硅基流动 `Qwen/Qwen3.5-4B`，该模型在官方模型广场标价为 0，并支持图像输入和 JSON Mode。
- 免费模式仍需要有效的硅基流动 API Key。项目从根目录 `local.properties` 的 `SILICONFLOW_API_KEY` 经 BuildConfig 注入，该文件不进入 Git。
- 各预设 API Key 使用加密存储；日志和错误提示不会输出完整 Key。

## 🛠️ 构建

环境要求：

- Android SDK 35
- Java 17
- Android Gradle Plugin 8.7.3
- Kotlin 2.0.21

命令：

```powershell
.\gradlew.bat :app:testDebugUnitTest
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:assembleRelease
```

产物：

- Debug：`app\build\outputs\apk\debug\app-debug.apk`
- Release：`app\build\outputs\apk\release\app-release.apk`

> 当前仓库路径包含中文。若当前 Windows/Gradle 环境直接运行单元测试出现 classpath 编码导致的 `ClassNotFoundException`，可复制到纯 ASCII 临时目录后执行；不要给 Gradle 全局添加 `-Dfile.encoding=UTF-8`。

## ✅ 当前验证状态

- JVM 单元测试：71 个通过（包含视觉探测分类矩阵、能力指纹、错误消息映射与本地化回归用例）。
- Debug 构建：通过。
- Release 构建：通过，包括 R8 和资源压缩；本轮交付 APK 已用 v2 + v3 签名方案校验通过。
- Android lint：仍有 1 个既有 Manifest 相机硬件声明错误和若干 warning，尚未在本轮处理。
- 真机验收：待执行，重点验证三语言、模型探测、历史索引控制、照片搜索、Top-5 复核和 Android 升级迁移。
- 百川官方 API 文档暂未零猜测确认到最新模型 ID，界面保留“模型列表待官方确认”提示。

## 📥 下载

正式版本请前往 [Releases](https://github.com/xulimeng2022/SmartStorageAssistant/releases) 下载。

## 📄 许可证

本项目采用 MIT License 开源协议。

## 📧 联系方式

- 作者：徐力萌
- QQ：2913895771
- 微信：xulimeng2021
- GitHub：[xulimeng2022](https://github.com/xulimeng2022)
- 个人网站：[xulimeng2026.netlify.app](https://xulimeng2026.netlify.app)

## 📝 更新记录

- **v1.2.0（2026-09-11，versionCode 6）**：修复语言切换后 Toast/Snackbar 等临时提示仍停留在旧语言的问题（临时消息改为“类型化 + 显示层解析”）；修复 DeepSeek V4.1 Flash（`deepseek-flash`）视觉探测误报失败的问题（网络调用移入 IO 线程、探测结果细分为鉴权/额度/限流/网络/超时/服务端/请求参数/模型/响应解析等，负结果不再持久化）；Release APK 启用 v2 + v3 签名。
- **v1.2.0（2026-09-11，versionCode 5）**：完成三语言本地化收尾；新增 AI 图片理解隐私开关、合成图能力探测、Room v5 图片视觉索引、历史照片索引、暂停/继续/重试、照片内容搜索和 Top-5 手动复核；备份升级到 1.1；模型目录更新到 2026-09-11；免费模型升级为 Qwen3.5-4B。
- **v1.1.0（2026-09-08，versionCode 4）**：新增三语言切换、微信式深色主题、AI 解析失败两出口、批量照片归属修复、全屏照片缩放预览和 LLM 错误分类。
- **v1.0.1（versionCode 2）**：逐字模糊搜索、多图附件、全屏照片预览、AI 批量识别与可编辑确认。

---

如果你觉得这个项目对你有帮助，欢迎给个 Star ⭐。
