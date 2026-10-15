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

此检查仅发送固定的合成助教技能问题，限制 128 个输出 token，不读取真实简历，也不输出密钥或生成正文。它会消耗少量 API 额度。条件不满足时的跳过不能记为真实调用通过；Day 27 未做此调用。

## Day 35 固定合成受控比较真实检查

`ControlledComparisonLiveIT` 复用现有评估服务的 3 个合成场景：先关闭模型获取规则基线，再经真实适配器向官方地址发出 3 次请求。不会启动 Spring Boot HTTP 服务或连接业务数据库，不是页面/数据库端到端验收。默认测试集合不运行该检查；应用的 `DEEPSEEK_ENABLED=false` 默认值不变。

在环境中已有密钥且明确选择真实调用时运行（会消耗 API 额度）：

```powershell
$previousLiveTest = $env:DEEPSEEK_LIVE_TEST
$env:DEEPSEEK_LIVE_TEST = "true"
try {
    mvn -f backend/pom.xml "-Dtest=ControlledComparisonLiveIT" test
    if ($LASTEXITCODE -ne 0) { throw "真实合成比较检查失败；请查看安全状态及本地证据。" }
} finally {
    if ($null -eq $previousLiveTest) {
        Remove-Item Env:DEEPSEEK_LIVE_TEST -ErrorAction SilentlyContinue
    } else {
        $env:DEEPSEEK_LIVE_TEST = $previousLiveTest
    }
}
```

该测试使用 `DEEPSEEK_MODEL` 或默认 `deepseek-flash`，连接超时为 3 秒；`DEEPSEEK_REQUEST_TIMEOUT` 默认 `PT15S`、`DEEPSEEK_MAX_TOKENS` 默认 `512`，沿用适配器对 60 秒和 4096 token 的上限检查，不重试。可仅在测试进程中覆盖这些值，复核截断或超时，运行后恢复原值。它核对基线与模型运行的输入、规则、提示词哈希及分数/技能证据，并保留 HTTP 状态、实际模型名、结束原因、截断标记和 token 用量。通过表示真实调用及规则隔离检查通过，不表示输出完整、质量通过或模型带来实际招聘增益。

完整生成文本及 JSON 仅保存在已忽略的 `backend/target/live-evaluation/`，使用唯一文件名避免覆盖；断言前保存证据，因此失败运行也可审查。日志只输出证据路径和安全的状态摘要。按 [人工复核流程](EVALUATION_MANUAL_REVIEW.md) 在个人目录备份，勿将原始证据、密钥或请求授权头加入 Git。运行时刻、证据摘要和未验证范围见 [Day 35 记录](verification/DAY35_2026-10-12.md)。

## Day 36 招聘助手真实 HTTP 检查

`RecruitmentChatHttpTests` 在 `127.0.0.1` 随机端口启动独立 Spring 测试上下文，使用生产安全过滤链、JWT、控制器、聊天服务和明确禁用的生产适配器。固定合成 Mapper 替身提供用户角色、岗位、简历、投递和已保存匹配记录；排除数据库/MyBatis/Redis 自动配置。常规 `mvn test` 会执行这一离线 HTTP 检查，不访问官方模型。

`RecruitmentChatLiveIT` 复用同一 HTTP 流程，只有显式选中测试且 `ASSISTANT_LIVE_TEST=true` 时运行。测试配置启用真实适配器，固定 2048 token / 30 秒请求超时；生产默认关闭、512 token / 15 秒不变。最多允许 3 次适配器调用，分别为学生首轮、学生续聊和 HR 所属投递；失败不自动重试。合成数据层不能算真实数据库端到端。

仅在运行环境已有 `DEEPSEEK_API_KEY` 时执行以下命令，会消耗 API 额度：

```powershell
$previousAssistantLive = $env:ASSISTANT_LIVE_TEST
$previousEvaluationSource = $env:EVALUATION_SOURCE_COMMIT
try {
    $env:ASSISTANT_LIVE_TEST = "true"
    $env:EVALUATION_SOURCE_COMMIT = git rev-parse HEAD
    mvn -f backend/pom.xml "-Dtest=RecruitmentChatLiveIT" test
    if ($LASTEXITCODE -ne 0) { throw "招聘助手真实检查失败，请查看本地证据；不要自动重试。" }
} finally {
    if ($null -eq $previousAssistantLive) { Remove-Item Env:ASSISTANT_LIVE_TEST -ErrorAction SilentlyContinue }
    else { $env:ASSISTANT_LIVE_TEST = $previousAssistantLive }
    if ($null -eq $previousEvaluationSource) { Remove-Item Env:EVALUATION_SOURCE_COMMIT -ErrorAction SilentlyContinue }
    else { $env:EVALUATION_SOURCE_COMMIT = $previousEvaluationSource }
}
```

检查匿名/越权/消息限制/会话上下文错误在模型调用前被拒绝、续聊实际带入上一轮问答、学生与 HR 使用服务端角色提示，以及所有 Mapper 调用只读、合成实体不变。JWT 在测试中通过生产 `JwtService` 签发；不将其解释为已测登录注册 HTTP。

完整合成消息、实际模型返回、安全 HTTP 响应和测试实体快照保存到已忽略的 `backend/target/live-assistant/`，文件名唯一；在实时调用结果断言前保存，HTTP 流程断言失败时也尝试留证。保存前拒绝包含运行密钥或测试 JWT 的文件。请在个人目录备份后保留原文供审查，不将原始证据提交 Git。源提交字段应与当次代码快照一起核对；未提交测试需另记测试文件哈希。

记录及未验证范围见 [Day 36 验收记录](verification/DAY36_2026-10-13.md)。真实响应须为 HTTP 200、`GENERATED`、`stop`、非空且用量有效才通过；通过不表示模型建议正确、实际人工审查完成或产生招聘增益。

## Day 37 隔离 MySQL 与助手同链检查

`RecruitmentChatMysqlLiveIT` 使用完整生产应用、真实 MySQL/MyBatis、HTTP 注册登录/JWT、岗位/简历/投递/规则匹配接口及官方适配器。只有明确选择 `-Dtest=RecruitmentChatMysqlLiveIT` 且 `ASSISTANT_MYSQL_LIVE_TEST=true` 时执行，普通测试不访问外部模型。运行条件与环境恢复命令见 [Day 37 记录](verification/DAY37_2026-10-15.md)。

数据库必须是 `127.0.0.1:3306` 上全新初始化的 `ai_job_platform_day37_<数字>` 专用库；检查在写数据前拒绝非空业务表。提供环境变量 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD` 和 `DEEPSEEK_API_KEY`，不将实际凭证写入代码。测试使用固定合成文本，经真实规则服务计算并保存匹配分，然后核对学生首轮、续聊和 HR 所属投递的只读上下文。适配器观测只记录真实调用，不替换响应；上限 3 次、不自动重试。

临时 HTTP 服务仅监听回环地址；Redis 指向测试独占、非 Redis 的本地端口，触发既有缓存异常降级，避免触碰共享缓存，因此该检查不验证正常 Redis 服务。测试专用模型配置为 2048 token / 30 秒，生产默认值不变。

原始合成输出仅保留在忽略目录 `backend/target/live-assistant-mysql/` 并备份至个人证据目录；登录令牌与凭证不得持久化。Git 仅保存技术摘要和证据哈希。失败不清空数据库或覆盖旧证据；修正后若需要重跑，应使用另一个新的隔离库并单独记录原因。通过不代表人工质量审查或真实招聘效果通过。

## 回滚

回滚本模块的代码与配置即可移除适配器，没有数据库数据或规则评分结果需要迁移。已有简历原文和 Day 26 增量列保留。
