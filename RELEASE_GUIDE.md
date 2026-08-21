# 智能收纳助手 · 发布与上架操作指引

本文档面向零基础新手，说明如何为 App 配置正式签名密钥、构建并发布 Release APK 到 GitHub Releases。

## 一、当前发布配置说明

- 已开启 **R8 官方混淆** 与 **资源压缩**（`app/build.gradle.kts` 的 `release` 构建类型）。
- 混淆规则位于 `app/proguard-rules.pro`，覆盖项目技术栈（Compose / Room / Hilt / OkHttp / DataStore / security-crypto / Coil / Coroutines）。
- 签名配置**不写死在代码里**，从根目录 `local.properties` 读取（该文件已被 `.gitignore` 忽略，不会提交到 GitHub）：
  - `KEYSTORE_FILE`：密钥库文件路径
  - `KEYSTORE_PASSWORD`：密钥库密码
  - `KEY_ALIAS`：密钥别名
  - `KEY_PASSWORD`：密钥密码
- 当前 `local.properties` 中尚未配置签名密钥，请按下面的步骤填入你自己的正式密钥（只填在本地，不会提交到 GitHub）。

## 二、生成正式签名密钥（只需一次）

1. 打开命令行（Windows 建议用 PowerShell）。
2. 执行以下命令（把 `D:\keystore\smartstorage.jks` 换成你希望的保存路径，密码换成你自己的强密码）：

```powershell
keytool -genkeypair -v `
  -keystore "D:\keystore\smartstorage.jks" `
  -alias smartstorage `
  -keyalg RSA -keysize 2048 -validity 25 `
  -storepass "你的密钥库密码" `
  -keypass "你的密钥密码" `
  -dname "CN=Xu Limeng, OU=SmartStorage, O=SmartStorage, L=Shenzhen, ST=Guangdong, C=CN"
```

3. 如果提示 `keytool` 不是命令，请先配置 JDK：把 `C:\Users\XU\.jdks\jbr-21.0.11\bin` 加入系统 PATH，或直接使用该目录下的 `keytool.exe` 完整路径。
4. **重要**：
   - 密钥文件和密码请务必妥善保管、备份，**丢失后无法再对已发布的 App 进行升级签名**。
   - **绝对不要把 `.jks` 文件提交到 GitHub**，也不要把密码写进代码。

## 三、配置签名并构建 Release APK

1. 用记事本打开项目根目录的 `local.properties`，追加（替换为你的真实路径与密码）：

```properties
KEYSTORE_FILE=D\:\\keystore\\smartstorage.jks
KEYSTORE_PASSWORD=你的密钥库密码
KEY_ALIAS=smartstorage
KEY_PASSWORD=你的密钥密码
```

2. 在项目根目录执行：

```powershell
.\gradlew.bat :app:assembleRelease
```

3. 构建成功后的 APK 位于：`app\build\outputs\apk\release\app-release.apk`

4. （可选）校验签名：

```powershell
apksigner verify --print-certs app\build\outputs\apk\release\app-release.apk
```

5. 混淆映射文件（用于崩溃堆栈反混淆，务必随版本一起留存）：
   - `app\build\outputs\mapping\release\mapping.txt`

## 四、发布到 GitHub Releases

1. 打开仓库：`https://github.com/xulimeng2022/SmartStorageAssistant`
2. 进入 **Releases** → **Draft a new release**（或 Create a new release）。
3. 填写：
   - **Tag**：`v1.0.0`（可自行定义，如 `v1.0.0`）
   - **Title**：`智能收纳助手 v1.0.0`
   - **描述**：写更新日志，例如“首次发布：支持语音录入、AI 智能解析、物品照片、回收站、数据备份恢复等功能”。
4. 点击 **Attach binaries**，上传 `app-release.apk`。
5. 点击 **Publish release** 发布。
6. 用户即可在 Releases 页面下载，或通过直链下载：
   `https://github.com/xulimeng2022/SmartStorageAssistant/releases/download/v1.0.0/app-release.apk`

## 五、混淆后功能测试清单（发布前必测）

安装 Release APK 到手机后，逐项验证：

- [ ] App 正常启动，无崩溃 / 无白屏
- [ ] 首页物品列表显示正常（名称 / 地点 / 缩略图）
- [ ] 添加物品（名称 / 地点 / 备注）→ 保存成功，列表出现新物品
- [ ] 重复名称保存 → 弹出「物品已存在」选择框
- [ ] 编辑物品 → 保存后详情刷新
- [ ] 删除物品 → Snackbar「已移入回收站」+ 撤销
- [ ] 搜索：关键词搜索命中 / 无结果显示「未找到」
- [ ] AI 智能解析：口语描述解析（无 Key 时验证降级提示）
- [ ] 照片附件：相册选图 / 拍照 / 大图预览 / 删除单张
- [ ] 回收站：恢复 / 永久删除 / 清空
- [ ] 设置页：主题模式切换（浅色 / 深色 / 跟随系统）全局生效
- [ ] 设置页：文字颜色抽屉（默认 / 纯色 / 渐变 / 预设 / 色环）实时预览
- [ ] 设置页：数据备份导出 ZIP、导入（覆盖 / 合并）
- [ ] 主 Tab 左右滑动切换 + 底部选中同步
- [ ] 添加 5 件物品触发 GitHub Star 提醒（后续 15 / 30 件再次触发）
- [ ] 关于页：QQ / 微信复制、GitHub / 网站打开选择器
- [ ] 捐赠页：档位选择、收款码弹窗、保存到相册
- [ ] 深色模式下各页面显示正常

> 提示：如果混淆后出现崩溃，用 `mapping.txt` 反混淆崩溃堆栈定位问题类，再补充对应 `-keep` 规则。

## 六、版本号维护

- 版本名 / 版本号在 `app/build.gradle.kts` 的 `defaultConfig` 中：
  - `versionName`：展示给用户的版本号（如 `1.0.0`）
  - `versionCode`：自增整数，每次发版必须**大于上一版**（Android 用它判断升级）
- 每次发版前记得递增 `versionCode`。
