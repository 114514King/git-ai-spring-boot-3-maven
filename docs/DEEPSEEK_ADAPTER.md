# Day 27 DeepSeek 文本适配器

执行日期：2026-10-02（北京时间）。实现仅限独立适配器，当前验证与 Git 交付结果见 [当天记录](verification/DAY27_2026-10-02.md)。

## 接口依据与范围

按执行日的官方 [Chat Completions 文档](https://api-docs.deepseek.com/api/create-chat-completion/)、[接口示例](https://api-docs.deepseek.com/) 和 [思考模式说明](https://api-docs.deepseek.com/guides/thinking_mode/) 构造请求。生产地址固定为 `https://api.deepseek.com/chat/completions`，使用 Bearer 密钥和 JSON POST。

默认模型为 `deepseek-flash`。官方 [更新日志](https://api-docs.deepseek.com/updates/) 记录旧 `deepseek-chat` / `deepseek-reasoner` 名称停止服务的计划及当前 Flash 模型名称，因此没有照搬早期模型名。模型别名可能更新，返回值记录上游实际 `model`，不能把别名当作固定模型版本的实验依据。

本模块没有 REST 控制器、会话存储、数据库 Mapper 或前端入口。只有内部 Java 调用 `DeepSeekChatClient.generate(List<DeepSeekMessage>)`；服务端角色提示、招聘数据访问与历史归属隔离将在 Day 28 实现。现有规则评分、缺口说明、投递状态接口不调用适配器，模型文本不会作为分数或自动招聘动作。

## 配置与运行

沿用 README 的 MySQL/JWT 与启动方式，无新增依赖或数据库迁移。

| 环境变量 | 默认值 | 含义与范围 |
| --- | --- | --- |
| `DEEPSEEK_ENABLED` | `false` | 必须显式开启才可能发请求 |
| `DEEPSEEK_API_KEY` | 空 | 只从运行环境提供，不写入源码、命令行参数或提交 |
| `DEEPSEEK_MODEL` | `deepseek-flash` | 可配置的模型标识，1–100 个受限字符 |
| `DEEPSEEK_CONNECT_TIMEOUT` | `PT3S` | 连接超时，1 ms–60 s |
| `DEEPSEEK_REQUEST_TIMEOUT` | `PT15S` | 整个响应体完成的截止时间，1 ms–60 s |
| `DEEPSEEK_MAX_TOKENS` | `512` | 输出 token 上限，1–4096 |

配置通过 `app.deepseek` 绑定。非法超时、token 上限、模型标识或带控制字符的密钥在初始化时报错；配置对象的 `toString()` 隐去密钥。默认关闭时不会发 HTTP 请求，仅设置密钥也不会开启模型。

当前只有适配器，没有可供浏览器直接调用的模型接口。开启开关不会使既有招聘页面开始发送简历；后续接入仍须按最小必要数据原则并经服务端范围检查。

## 内部请求与响应契约

消息仅支持 `system`、`user`、`assistant` 三种文本角色，内容非空，所有消息总计最多 24,000 个 Java UTF-16 字符单位。这是传输层约束，不代表完成了用户权限或会话历史限制；调用方不得把客户端角色或任意他人招聘数据直接传入。

请求字段为 `model`、`messages`、`stream=false`、`thinking={"type":"disabled"}`、`max_tokens`。不携带 tools、业务身份、JWT 或自动动作。禁止重定向，不自动重试，避免把密钥发送至其他地址或重复产生费用。

响应最多 1 MiB，包含上游等待期间的空白；整个请求截止时间也覆盖这种保活空白。只接受 HTTP 200 的 JSON、非空 assistant 文本及 `stop` / `length` 结束原因。拒绝工具调用、异常结构、尾随 JSON、负值/溢出/不一致的 token 用量。返回 `content`、上游实际 `model`、`finishReason`、可选 `usage` 和 HTTP 状态。

| `status` | 结果含义 |
| --- | --- |
| `GENERATED` | 已收到有效辅助文本；`truncated()` 在 `length` 时为 true |
| `DISABLED` | 未开启，不发送请求 |
| `MISSING_API_KEY` | 开启但没有密钥，不发送请求 |
| `TIMEOUT` | 连接/完整响应超过截止时间，取消请求 |
| `UPSTREAM_ERROR` | HTTP 非 200，记录状态码 |
| `NETWORK_ERROR` | 连接或传输失败 |
| `INVALID_RESPONSE` | 非 JSON、无有效文本、响应超限或结构不合约 |
| `INTERRUPTED` | 调用线程被中断，取消请求并保留中断标志 |

降级结果的 `content` 为空，`usage` 和 `finishReason` 为 null，不包含密钥、原始请求、异常信息或上游错误体。它明确表示未生成文本，不伪装成真实模型输出。未来对话服务应显示可用性状态和已有规则结果；适配器本身不会重算或覆盖规则分。

生成文本仍可能包含错误、提示注入响应或不当建议，传输成功不等于内容正确。Day 28–30 将补齐服务端上下文、展示隔离及固定输入比较，本模块不声称已验证这些能力。

## 复核方法

常规验证：

```powershell
mvn -f backend/pom.xml test
mvn -f backend/pom.xml package -DskipTests
pnpm --dir frontend build
```

`DeepSeekChatClientTests` 使用 `127.0.0.1` 的随机端口模拟上游、合成消息及虚假测试密钥，检查真实 HTTP 请求契约、完整响应超时、异常降级和响应限制。`DeepSeekConfigurationTests` 检查 Spring 配置绑定与安全默认值。不会向外部 API 发送测试数据，也不依赖真实数据库。

真实调用检查单独保存在 `DeepSeekLiveIT`，默认不进入常规 `*Tests` 测试集合，并额外要求 `DEEPSEEK_LIVE_TEST=true`。只在安全会话中已设置 `DEEPSEEK_API_KEY`、具备官方接口网络访问且明确选择真实验证后运行：

```powershell
$env:DEEPSEEK_LIVE_TEST = "true"
try {
    mvn -f backend/pom.xml "-Dtest=DeepSeekLiveIT" test
} finally {
    Remove-Item Env:DEEPSEEK_LIVE_TEST -ErrorAction SilentlyContinue
}
```

此检查仅发送固定的合成助教技能问题，限制 128 个输出 token，不读取真实简历，也不输出密钥或生成正文。它会消耗少量 API 额度。条件不满足时的跳过不能记为真实调用通过；当前执行未做此调用。

## 回滚

回滚本模块的代码与配置即可移除适配器，没有数据库数据或规则评分结果需要迁移。已有简历原文和 Day 26 增量列保留。
