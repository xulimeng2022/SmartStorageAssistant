# M03 AI 解析与远程 LLM

## 1. 职责与边界

### 模块职责
- 调用任意 OpenAI 兼容 chat/completions，把口述文本解析为结构化物品字段（单条/批量）。
- HTTP 错误分类（类型化、脱敏）、有界重试、超时标识。
- 模型不可用时本地规则降级（LocalDescriptionParser）+ 统一清洗（ParsedItemSanitizer）。
- 免费模式（硅基流动 Qwen2.5-7B-Instruct）与自定义模式配置读取（经 SettingsRepository，见 M06）。

### 不负责什么
- 不保存解析结果（入库在 M09/M02 流程）；不上传照片；不实现非 OpenAI 兼容协议。

### 上下游关系
| 方向 | 模块 | 交换内容 | 约定 |
| --- | --- | --- | --- |
| 输入 | M09 添加页 / M08 首页 | 文本 | userText 仅为口述文本 |
| 输出 | 上层 ViewModel | ParsedItem / ParseItemsResult / SearchParseOutcome | 失败显式告知，不静默假装成功 |
| 依赖 | M06 SettingsRepository、OkHttp（di/RemoteModule） | baseUrl/model/apiKey | 7s 连接 / 25s 读 / 7s 写 |

## 2. 功能清单
| 编号 | 功能 | 说明 |
| --- | --- | --- |
| M03-01 | parseItem | 单条：JSON 对象解析 + 容错 + 清洗；失败返回 null |
| M03-02 | parseItems | 批量：JSON 数组解析；模型失败/空回退本地规则，返回 warning 与来源 |
| M03-03 | 语义搜索解析 | 首页把整句转为关键词或三字段（SearchParseOutcome） |
| M03-04 | 错误分类 | LlmErrorClassifier：MISSING_CONFIG/AUTH/QUOTA/RATE_LIMIT/MODEL_OR_REQUEST/SERVER/NETWORK/UNKNOWN |
| M03-05 | 重试 | 可重试错误（限流/服务端/网络）最多 2 次，退避 300ms |
| M03-06 | 本地降级 | LocalDescriptionParser：句读切分 + 位置动词切地点 + 枚举拆分（保守，防误拆品牌） |
| M03-07 | 清洗 | ParsedItemSanitizer：去首尾引号标点、剥离尾部连接词、丢弃空名称 |

## 3. 核心流程
1. ViewModel 调 LlmClient → transport.postChatCompletion（OkHttpLlmTransport 拼接 /chat/completions，兼容根地址/完整地址）。
2. 非 2xx → LlmErrorClassifier.exceptionFor 分类；网络/超时上抛；免费模式超时抛 LlmTimeoutException（UI 弹超时引导）。
3. 成功 → parseJsonContent/parseJsonArray（去 ```json 标记、补 `}`、JSONObject→正则兜底）→ ParsedItemSanitizer 清洗。
4. 批量在模型不可用/空结果时走 LocalDescriptionParser，source=LOCAL_FALLBACK + warning 提示用户当前为本地识别。

## 4. 数据与接口
| 名称 | 调用方 | 输入 | 成功输出 | 失败输出 |
| --- | --- | --- | --- | --- |
| LlmClient.parseItem | AddItemViewModel | text | ParsedItem? | null（上层降级） |
| LlmClient.parseItems | AddItemViewModel | text | ParseItemsResult(items, source, warning) | - |
| LlmTransport.postChatCompletion | LlmClient | baseUrl/model/apiKey/systemPrompt/userText/maxTokens | content | LlmRequestException |
| FreeModel | - | BuildConfig.SILICONFLOW_API_KEY | 免费模式常量 | - |

批量输出上限 1200 tokens；单条提示词要求严格 JSON。

## 5. 状态、错误与排查
| 状态/错误 | 触发 | 用户可见结果 | 系统行为 | 优先排查 |
| --- | --- | --- | --- | --- |
| MISSING_CONFIG | 免费无 Key/自定义不完整 | 去配置引导 | 归类后提示 | SettingsRepository/FreeModel |
| AUTH/QUOTA/RATE_LIMIT | 服务商返回 | 对应中文提示 | 限流等可重试 | LlmErrorClassifier |
| NETWORK/SERVER | 网络/5xx | 提示可重试 | 自动重试一次后降级 | OkHttp 超时配置 |
| LlmTimeoutException | 免费模式超时 | 「AI 解析未成功」对话框 | UI 两出口 | RemoteModule 超时 |
| 本地降级 | 模型失败/空 | 提示“本地识别” | LocalDescriptionParser | 规则文件 |

> 日志只记录阶段/耗时/错误类型，不记录 Key 与用户原文；错误消息经 summarizeBody 截断脱敏。

## 6. 测试与验收
| 场景 | 层级 | 说明 |
| --- | --- | --- |
| 错误分类 | 单元 | LlmErrorClassifierTest（状态码→kind、可重试、脱敏摘要） |
| 本地规则 | 单元 | LocalDescriptionParserTest（句读/共享地点/枚举/品牌防误拆） |
| 清洗 | 单元 | ParsedItemSanitizerTest（引号/尾部连接词/空名称丢弃） |
| 传输故障注入 | 单元 | OkHttpLlmTransport 可被 LlmTransport 假实现替换（见接口注释） |

## 7. 实现定位
- 代码：`app/src/main/java/com/example/smartstorage/data/remote/llm/`、`di/RemoteModule.kt`
- 测试：`app/src/test/java/com/example/smartstorage/data/remote/llm/`（3 个测试文件）
- 相关 UI 出口：`presentation/common/AiParseFailedDialog.kt`（M12）

## 8. 长期决策与待办
### 稳定决策
| 日期 | 决策 | 原因 | 影响 |
| --- | --- | --- | --- |
| v1.1.0 | 错误分类 + 有界重试 + 两出口引导 | 让用户可行动、不静默降级 | LlmErrorClassifier/LlmTransport |
| v1.1.0 | 本地降级与模型结果统一过 Sanitizer | 行为一致 | ParsedItemSanitizer |

### 待办 / 待确认
- [ ] 各预设模型版本随服务商变动需人工核对（见 LlmPreset，M06）

## 9. AI 解析失败与降级：完整行为约束（速查）

> 来源：原 AGENTS.md（v1.1.0）功能实现约束，已核实与代码一致。改 AI 相关行为时不得破坏以下语义。

- 超时与降级判定：读取超时 25s / 连接超时 7s（见 di/RemoteModule）；模型或请求最终失败（超时、配置缺失、鉴权/额度/限流、网络/服务端、空结果等）统一由 UI 弹「AI 解析未成功（去配置 API / 稍后）」两出口。
- 「稍后」= 若尚未降级则自动本地规则降级，已降级则保留结果继续填写；返回/点击外部等同「稍后」；同一页面会话内选过一次「稍后」后不再重复弹框。
- 「去配置 API」：从添加/编辑页进入时保留草稿（表单/照片/批量/降级结果）并可返回继续；自动切到自定义模式并定位 AI 配置卡；不自动启用空配置、不自动重跑模型、不自动保存。
- 降级只影响当前任务，不持久修改 AI 模式（改模式须走设置页）。
- 免费模式界面不再展示构建时 SILICONFLOW_API_KEY 的开发说明。
- 批量识别逐条询问重名决策；模型结果与本地降级结果统一过 ParsedItemSanitizer。
