# AI 智能求职招聘平台

这是一个长期迭代的前后端分离项目，面向学生、HR 和管理员三类角色。

## 当前进度

- 当前阶段：Day 36（2026-10-13），完成招聘助手真实 HTTP/JWT/聊天服务到 DeepSeek 的合成数据联调；三次模型调用均获得完整文本，10 个错误请求在调用模型前被拒绝。226 项常规测试、独立真实检查、后端打包和前端构建通过；数据层为合成替身，人工质量仍待复核。
- 已完成：Spring Boot 基础框架、MySQL 核心表、注册登录、JWT 请求认证、角色权限、岗位、简历、投递、AI 匹配后端模块、Redis 岗位缓存、Vue 3 前端基础框架、前端登录注册和路由守卫、学生端岗位/简历/投递/AI 匹配页面、HR 端岗位/投递/AI 匹配页面、管理员看板和统计图表、可解释 AI 匹配评分、简历智能优化建议、岗位 JD 智能解析与优化建议、HR 候选人推荐排序与风险摘要、AI 面试题生成与评分维度、管理端 AI 运营洞察、HR 投递跟进建议、学生端 AI 求职行动计划、HR 候选人沟通话术草稿
- Day 25 新增：中英文技能别名归一化、去重、按岗位所需技能覆盖率评分和完整技能缺口展示。
- Day 26 新增：文本型 PDF / UTF-8 TXT 上传提取、可编辑预览、确认创建本人草稿、导入文本保存与技能匹配接入。
- Day 27 新增：独立 DeepSeek 文本适配器，默认禁用，支持环境变量配置、完整请求超时、响应限额和明确降级状态。此阶段没有对外对话接口或页面入口，既有规则评分与招聘状态仍独立运行。配置及内部契约见 [适配器说明](docs/DEEPSEEK_ADAPTER.md)。
- Day 28 新增：学生端和 HR 端服务端招聘助手对话接口，支持会话归属隔离、历史长度限制、服务端角色提示、本人/所属岗位招聘上下文和只读匹配分引用。默认 DeepSeek 关闭或异常时返回明确降级说明，不修改匹配分、投递状态或招聘决定。
- Day 29 新增：学生“求职助手”和 HR“招聘助手”选项卡，支持上下文选择、当前会话消息、只读规则分、模型辅助建议与降级说明；HR 投递状态变更增加人工确认，助手入口只跳转复核，不自动更新状态。
- Day 30 新增：管理员固定合成评估接口，对比规则基线与规则加模型辅助，记录规则分、技能缺口、模型状态、耗时、token 用量、贡献检查和越界风险；默认关闭 DeepSeek 时只验证降级链路，不声称真实模型增益。
- Day 31 补充验收：业务匹配和评估共用 `SkillCoverageService`；返回固定合成输入，测试学生/HR 入口及保存结果与评估一致；运行模式仅标记文本生成/降级，不将模拟响应称为真实调用。
- Day 32 补充验收：评估报告增加固定场景版本、场景集合哈希、单场景输入哈希、规则证据哈希和模型提示词哈希，便于核对两次执行是否使用同一合成输入与规则口径；这些哈希不证明真实模型调用或招聘效果。
- Day 33 补充验收：报告及各场景明确保持待人工复核，提供按生成/降级、缺口有无区分的核验清单和 [人工记录模板](docs/EVALUATION_MANUAL_REVIEW.md)。模型声称已通过不会改变复核状态或规则分；实际人工质量结论仍待审查。
- Day 34 补充验收：使用隔离库 `ai_job_platform_day34_20261010` 验证 `docs/init.sql`、Day 26 迁移幂等性，以及真实后端 HTTP 注册、登录、HR 发岗发布、学生简历 `sourceText` 保存、投递和规则匹配落库。该库仅含合成数据，不代表生产数据库或真实招聘效果。
- 2026-10-10 用户追加回归：修复登录角色导航与过期登录态、薪资空值、空表单未处理异常、助手上下文复用和历史输入超限；后端 225 项测试、前端构建及 32 项合成接口浏览器检查通过。本次真实 MySQL 连接缺少密码，未复测真实数据库或调用 DeepSeek，详细结论见 `docs/verification/REVIEW_2026-10-10.md`。
- 2026-10-11 用户追加界面优化：统一设计变量与共享样式，压缩标题区；学生/HR 工作台分组导航（手机端单行横向滚动）、规则匹配分/模型辅助建议/人工决定分开标注，HR 投递表格固定“处理”列、状态下拉不再显示原始枚举，中文文件选择按钮。前端构建与 77 项合成接口浏览器检查（1440/1024/768/390）通过；未做真实后端联调，详见 `docs/verification/UI_POLISH_2026-10-11.md`。
- 2026-10-01 补充：统一登录、注册和三类工作台的青绿色主题、页头、面板与手机布局；改善岗位薪资、中文用工类型和选中状态，替换登录页过时的开发说明。
- 外部岗位接入：已接入 Jobicy 公开远程岗位，提供匿名浏览、关键词/地区筛选、分页、详情和原站申请入口。BOSS直聘、智联和前程无忧仍缺少本次可确认的直接可用岗位读取接口；来源依据和实现边界见 [接入说明](docs/EXTERNAL_JOB_DATA.md)。
- Day 35 补充验收：复用 3 个固定合成场景，两轮真实 DeepSeek 调用均为 HTTP 200。512 token 默认预算均截断，2048 token 测试覆盖值获得完整文本；规则分 66.67 / 75.00 / 100.00 及输入、规则、提示词哈希保持不变。应用默认配置未改，原文仅本地保存，见 [验收记录](docs/verification/DAY35_2026-10-12.md)。
- Day 36 补充验收：临时本地 HTTP 服务执行学生首轮/续聊、HR 所属投递对话，真实 JWT 校验与模型调用通过；已有 75.00 分、SUBMITTED 状态和合成实体保持不变，全部 Mapper 操作只读。记录见 [当天验收](docs/verification/DAY36_2026-10-13.md)，数据层为固定合成替身，不是 MySQL 或浏览器端到端。
- 既定 Day 25–30 小模块已实现；Day 31–36 补规则基线、可复现记录、人工复核材料、隔离 MySQL HTTP、真实模型合成比较和助手 HTTP 联调证据。实际人工质量、可量化模型增益、生产/既有数据库验收和真实招聘效果仍未验证，不能写成已通过实验或生产结论。

## 技术栈

界面运行命令沿用下文的前端启动方式；本次不需要数据库升级。`cd frontend; pnpm build` 可验证前端构建。设计稿、桌面/手机截图和浏览器验证边界见 [界面验收记录](docs/ui/VERIFICATION.md)。截图使用隔离演示数据，不代表真实岗位接入或真实 MySQL 联调结果。

- 后端：Spring Boot 3、Maven、MyBatis-Plus、MySQL 8、Spring Security、JWT、Redis
- 前端规划：Vue 3、Vite、Element Plus、Axios、Pinia、Vue Router、ECharts

## 项目结构

```text
backend/
  pom.xml
  src/main/java/com/example/aijobs/
    auth/                  注册、登录、JWT 和角色权限
    common/                统一响应和异常处理
    job/                   岗位查询、HR 岗位管理与 JD 智能解析
    job/JobCacheService    Redis 岗位列表和详情缓存
    externaljobs/          Jobicy 公开岗位读取、缓存与只读查询
    deepseek/              默认禁用的外部文本适配器，无业务数据读取/写入
    resume/                学生简历维护与简历智能优化建议
    application/           学生投递、HR 投递管理与候选人推荐
    admin/                 管理员看板统计与 AI 运营洞察
    match/                 AI 简历岗位匹配
docs/
  DAILY_TASKS.md
  ROADMAP.md
  init.sql
frontend/
  package.json
  index.html
  vite.config.js
  src/
    main.js
    App.vue
    api/http.js
    api/hr.js
    router/
    stores/
    views/
      AdminDashboardView.vue
      HrDashboardView.vue
      StudentDashboardView.vue
    styles.css
```

## 运行方式

全新、可重置的 MySQL 8 数据库才执行 `docs/init.sql`（会删除旧表）。已有数据库请先备份，并在启动 Day 26 后端前执行下面的增量脚本；它只添加可空的 `resume.source_text`，可以重复执行，不删除旧数据：

```powershell
mysql -u root -p ai_job_platform -e "source docs/migrations/day26-resume-source-text.sql"
```

全新环境初始化后，配置数据库连接与至少 32 字节的 JWT 密钥：

```powershell
mysql -u root -p -e "source docs/init.sql"
cd backend
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "你的本地数据库密码"
$env:JWT_SECRET = "至少32字节的自定义密钥"
mvn spring-boot:run
```

可选环境变量：`DB_URL`、`JWT_EXPIRATION`（默认 `PT2H`）、`REDIS_HOST`（默认 `localhost`）、`REDIS_PORT`（默认 `6379`）、`REDIS_PASSWORD`、`REDIS_DATABASE`（默认 `0`）、`JOB_CACHE_TTL`（默认 `PT10M`）、`EXTERNAL_JOBS_ENABLED`（默认 `true`，设为 `false` 禁用 Jobicy）。服务默认地址为 `http://localhost:8080`。
招聘助手可选环境变量：`ASSISTANT_MAX_HISTORY_MESSAGES`（默认 `8`，服务端最多带入最近 20 条历史）；DeepSeek 相关变量见 [适配器说明](docs/DEEPSEEK_ADAPTER.md)，默认关闭。

Redis 用于缓存公开岗位列表和公开岗位详情。Redis 未启动时，接口会回退到 MySQL 查询，不影响基础功能；HR 创建、编辑或更新岗位状态后会清理公开岗位缓存。

前端基础工程运行方式：

```powershell
cd frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm install
pnpm dev
```

前端开发服务器默认地址为 `http://localhost:5173`，并将 `/api` 代理到 `http://localhost:8080`。Day 24 已接入学生端、HR 端和管理员端工作台：学生端 `/app` 提供公开岗位筛选和详情、学生简历草稿创建/编辑/发布、学生投递和撤回、学生 AI 匹配生成和可解释结果查看、按目标岗位生成简历优化建议，以及按单个投递生成 AI 求职行动计划；HR 端 `/hr` 提供岗位草稿创建/编辑/发布/关闭、按岗位查看投递、更新投递状态、为已投递简历生成 AI 匹配、查看可解释岗位匹配结果、生成本地规则 JD 解析与优化建议、查看候选人推荐排序和风险摘要、为单个投递生成 AI 面试题与评分维度、生成单个投递的 AI 跟进建议，以及生成候选人沟通话术草稿；管理员端 `/admin` 提供平台用户、岗位、简历、投递和 AI 匹配统计图表，并展示本地规则 AI 运营洞察。

## 外部岗位（Jobicy）

无需登录访问 `http://localhost:5173/external-jobs`，或从登录/注册页点击“先逛逛 Jobicy 外部岗位”。学生工作台新增“外部岗位”选项卡，申请按钮跳转 Jobicy 原站。

- `GET /api/external-jobs?keyword=&location=&page=1&pageSize=20`：公开只读接口，统一响应。关键词匹配标题、公司、描述和标签；地区匹配原始工作范围。页码从 1 开始，每页 1–50 条，筛选词最多 100 字符。
- 后端固定读取 Jobicy 公共接口，无需 API Key。首次访问触发读取，各筛选共用最多 200 条的最新快照；按需每小时最多尝试一次，刷新按钮也使用此限制。筛选总数是当前快照中的匹配数，不是 Jobicy 全站总数。
- 来源仅包含最近 7 天发布的岗位，约延迟 3 小时；以海外英文远程岗位为主，仍可能限制工作国家/地区。保留币种与薪资周期，缺失字段不推测。
- 请求失败时提示错误；有不超过 24 小时的旧快照时明确标为暂存数据，超过此期限返回 503。页面显示真实成功同步时间，岗位是否仍开放以原站为准。
- 本模块无需新增表或数据库升级，不向 Jobicy 发送简历、令牌或用户资料，不创建本平台投递/匹配记录。描述清理为纯文本，申请链接校验 HTTPS 和来源域名。
- 缓存与限频在单个后端进程内生效，重启会清空；多实例部署前需统一缓存与请求限制。

## CV 文件导入（Day 26）

学生工作台 `/app` → “我的简历” → 选择文件 → 核对并编辑提取文本 → “确认创建草稿”。预览和取消均不创建简历；确认只创建当前学生的新 DRAFT，不覆盖旧简历，也不会自动发布。草稿可在原有编辑表单的“导入简历文本”栏继续修改，发布后参与技能覆盖率计算。

- `POST /api/student/resumes/import-preview`：学生 Bearer JWT + `multipart/form-data`，文件字段名为 `file`。返回统一响应，`data` 含 `filename`、`format`（PDF/TXT）、`pageCount`（TXT 为 0）、`text`、`warnings`。
- `POST /api/student/resumes`：确认后提交 JSON，例如 `{"title":"助教简历","sourceText":"English lesson planning"}`。原字段继续兼容，学生 ID 和 DRAFT 状态由服务端决定。
- `GET /api/student/resumes`、`GET /api/student/resumes/{id}` 和 `PUT /api/student/resumes/{id}` 支持 `sourceText`，遵守原有本人归属权限。旧客户端更新时省略或传 null 保留原文，显式空字符串清空原文。
- 限制：单文件 2 MiB，整个请求 3 MiB；PDF 最多 20 页；提取及确认文本最多 20,000 个 Java/JavaScript UTF-16 字符单位。只接受文本型 PDF 和严格 UTF-8 TXT（支持 BOM）；不支持 DOC/DOCX、OCR 或密码解密。
- 错误：空文件/缺少 file 返回 400，文件/页数/文本超限返回 413，不支持的扩展名返回 415，损坏、加密、无可提取文字或非 UTF-8 文件返回 422。扩展名、PDF 文件头和实际内容会校验，不信任上传 MIME。
- 原始文件只在请求内存中解析，不写入业务存储或数据库，不向第三方发送；只有用户确认的文本保存到本人简历。PDF 分栏/表格顺序、缺失字体映射可能影响提取，必须人工核对。
- 解析采用 [Apache PDFBox 3.0.8](https://pdfbox.apache.org/download.cgi)。此阶段的原文接入针对技能覆盖率服务，其他既有关键词建议模块仍主要使用结构化简历字段；用户可继续整理这些字段。

回滚时先回滚 Day 26 代码，保留可空的 `source_text` 列及已保存文本，旧代码会忽略该列；不要为了回滚删除用户原文。

## 认证接口

- `POST /api/auth/register`：注册 `STUDENT` 或 `HR`
- `POST /api/auth/login`：使用用户名或邮箱登录并获取 Bearer JWT
- 登录响应在原有 `tokenType`、`accessToken`、`expiresAt` 上增加数据库角色列表 `roles`，前端按 ADMIN、HR、STUDENT 的顺序选择对应工作台；实际接口权限仍由后端每次验证。此前未保存角色的登录态需要重新登录，过期或损坏的本地登录态会在路由跳转时清除。
- `GET /api/access/student`：仅 `STUDENT`
- `GET /api/access/hr`：仅 `HR`
- `GET /api/access/admin`：仅 `ADMIN`

## 招聘助手对话接口（Day 28）

以下接口已由 Day 29 学生/HR 助手页面接入。会话存放在当前后端进程内，重启后失效；不会新增数据库表，不保存原始文件，不自动改写匹配分、简历、岗位、投递状态或招聘决定。

- `POST /api/student/assistant/chat`：仅 `STUDENT`，可附加本人 `applicationId`，或本人 `resumeId` 与公开岗位 `jobId`。
- `POST /api/hr/assistant/chat`：仅 `HR`，可附加本人岗位 `jobId`，或本人岗位范围内的 `applicationId` 并读取对应候选人简历上下文。

请求示例：

```json
{
  "conversationId": "chat_可选续聊ID",
  "message": "请帮我解释这个技能缺口应该如何准备",
  "applicationId": 11
}
```

响应包含 `conversationId`、`assistantMessage`、`generated`、`modelStatus`、`contextSummary`、`historyMessageCount`、`readOnlyMatchScore`、`matchScoreSource`、`humanReviewRequired` 和 `warnings`。服务端固定写入角色提示，要求模型只基于授权上下文回答，忽略简历/岗位/用户文本中的指令注入；匹配分只读传入，生成文本不能改写分数、录用或拒绝候选人。DeepSeek 未启用、缺少密钥、超时或上游异常时会返回降级说明。真实模型调用需显式配置并使用合成数据验证，不能发送真实个人简历。

会话同时绑定服务端解析后的投递、岗位和简历对象；携带旧 `conversationId` 切换对象返回统一 HTTP 409，页面提示新建对话。历史按完整问答轮次传入，并在适配器 24,000 字符预算内裁减最旧轮次，返回明确 warning；当前问题、角色提示和授权上下文保留。

### 助手页面与人工复核（Day 29）

学生登录后在 `/app` 的“求职助手”选择通用、岗位与本人简历或本人投递；HR 在 `/hr` 的“招聘助手”选择通用、本人岗位或所属岗位投递。切换上下文或点击“新对话”清空当前消息与草稿，后续请求不带旧会话 ID。当前页最多展示最近 24 条消息；服务端按既有历史配置带入上下文。聊天仅保存在当前页面内存，刷新、退出或切换角色后不恢复旧消息，也不写入浏览器存储。

单条消息最多 1000 个 UTF-16 字符单位，等待回复时禁止重复发送或切换上下文。助手请求前端超时为 90 秒，高于后端默认的 15 秒完整请求超时；HTTP/网络失败保留输入，已有会话遇到 403/404 提示新建对话，不自动重试。回复使用 Vue 纯文本插值，独立展示只读规则分、来源、模型辅助建议或降级说明，不执行模型输出中的 HTML/脚本。

HR 的“人工复核投递”按钮只跳转投递管理。选择新状态后，必须在确认弹窗中核对投递 ID、原状态和目标状态；取消或关闭不发送写请求，只有“确认更新”调用既有状态接口。无需数据库升级，运行方式沿用上文；回滚本次提交可恢复 Day 28 页面行为。

## 受控评估接口（Day 30）

`GET /api/admin/evaluations/day30-controlled-comparison` 仅允许 `ADMIN` 访问。接口使用 3 组固定合成岗位/简历文本，通过与业务匹配共用的 `SkillCoverageService` 计算规则覆盖率，再以同一规则分、匹配技能和缺口构造模型辅助提示，形成规则基线与规则加模型辅助的对照报告。调用在 DeepSeek 启用时会触发 3 次适配器请求；常规测试使用模拟回复，Day 35 则直接调用同一服务和真实适配器进行独立集成检查，未复测管理员 HTTP 链路。

报告字段包括 `evaluationId`、`runMode`、`ruleModelName`、`dictionaryVersion`、生成/降级数量、每个场景的 `jobRequirements` / `resumeText` 合成输入、规则分、技能列表、缺口、规则说明、模型说明、模型状态、耗时、token 用量、贡献检查、风险标记和限制说明。`evaluationId` 沿用 Day 30 固定场景标识，并非唯一执行 ID。模型生成文本只用于人工复核材料，不写回匹配分、简历、岗位或投递状态。

Day 31 调整 `runMode` 契约，调用方应按以下值解析，停用旧的 `rule-plus-live-model` 值：

| `runMode` | 含义 |
| --- | --- |
| `rule-plus-model-output` | 所有场景返回辅助文本；模拟与真实响应都可能产生此值 |
| `rule-plus-partial-output` | 部分场景返回文本，其他场景降级 |
| `rule-plus-fallback` | 全部场景降级，没有辅助文本 |

这些值不证明真实 DeepSeek 调用。关键词命中只用于人工检查，不能评估建议真实性或模型实际增益；规则无缺口时，缺口覆盖检查标为不适用。

默认 `DEEPSEEK_ENABLED=false` 时，接口会返回 `rule-plus-fallback`，只证明降级路径、规则基线和评估记录结构可用；这不是 DeepSeek 真实效果评估。若要做真实模型验证，必须显式配置 DeepSeek 环境变量，并且只使用合成数据，随后人工审查报告中的越界风险、遗漏缺口、时延和 token 用量。小样本结果不能外推为真实招聘准确率、效率提升或自动决策依据。

Day 32 增加可复核元数据：`scenarioSetVersion` 标记固定合成场景版本，`scenarioSetHash` 由全部场景的 ID、角色、岗位要求和简历文本计算；每个结果含 `inputHash`、`ruleEvidenceHash` 和 `modelPromptHash`，分别对应单场景输入、规则分/技能证据和发送给模型的系统提示加用户提示。`reproducibilityNotes` 说明这些哈希适合核对输入与规则口径，`latencyMs`、模型文本和 token 用量可能随运行变化。哈希只证明同一合成输入和规则证据可复核，不证明真实 DeepSeek 调用、模型额外贡献或招聘效果。

Day 33 增加 `qualityReviewStatus` / `qualityReviewSummary`，每个场景增加 `manualReviewStatus` / `manualReviewChecklist`。状态始终为 `PENDING_HUMAN_REVIEW`，生成、降级、关键词命中和模型自称人工已通过都不会改变它。清单要求核对规则证据、缺口遗漏/误述、无依据经历、额外核验动作和招聘决定边界；无缺口时聚焦真实性与熟练度，未生成时不能评估模型额外贡献。按 [人工复核说明](docs/EVALUATION_MANUAL_REVIEW.md) 在独立记录中保留原始输出、执行来源、哈希和实际审查证据；本模块没有复核写接口或持久化，填写模板不会改变 API 状态。

## 岗位接口

公开接口只返回 `PUBLISHED` 岗位：

- `GET /api/jobs?keyword=&city=&employmentType=`：按关键词、城市、用工类型筛选
- `GET /api/jobs/{id}`：查询已发布岗位详情

以下接口需要 HR 的 Bearer JWT，且只能管理自己的岗位：

- `GET /api/hr/jobs`：查询当前 HR 的全部岗位
- `POST /api/hr/jobs`：创建 `DRAFT` 草稿
- `PUT /api/hr/jobs/{id}`：编辑岗位内容
- `PATCH /api/hr/jobs/{id}/status`：将状态设为 `DRAFT`、`PUBLISHED` 或 `CLOSED`
- `POST /api/hr/jobs/{id}/jd-analysis`：生成本人岗位的本地规则 JD 解析与优化建议

创建或编辑请求示例：

```json
{
  "title": "Java 开发工程师",
  "companyName": "示例科技",
  "city": "上海",
  "employmentType": "FULL_TIME",
  "salaryMin": 10000,
  "salaryMax": 15000,
  "description": "负责后端服务开发",
  "requirements": "熟悉 Spring Boot"
}
```

## 简历接口

以下接口需要学生的 Bearer JWT，且只能访问和修改自己的简历：

- `GET /api/student/resumes`：查询当前学生的简历列表
- `GET /api/student/resumes/{id}`：查询本人简历详情
- `POST /api/student/resumes`：创建 `DRAFT` 简历草稿
- `PUT /api/student/resumes/{id}`：编辑本人简历
- `PATCH /api/student/resumes/{id}/status`：将状态设为 `DRAFT` 或 `PUBLISHED`
- `POST /api/student/resumes/{id}/optimization`：根据目标岗位生成本地规则简历优化建议

创建或编辑请求示例：

```json
{
  "title": "Java 后端简历",
  "education": "计算机科学本科",
  "workExperience": "后端开发实习经历",
  "projectExperience": "AI 招聘平台项目",
  "skills": "Java, Spring Boot, MySQL",
  "selfEvaluation": "具备良好的工程实践能力"
}
```

JD 分析接口不调用外部 AI 服务，当前模型标识为 `local-jd-analyzer-v1`。返回内容包含 JD 摘要、关键技能、岗位亮点、信息缺口和优化建议；前端 HR 工作台 `/hr` 的“JD 分析”标签页可直接选择岗位生成结果。

简历优化建议请求示例：

```json
{
  "jobId": 1
}
```

该接口使用本地关键词规则，不调用外部 AI 服务，当前模型标识为 `local-resume-optimizer-v1`。返回内容包含总体摘要、已覆盖关键词、待补充关键词、内容建议和下一步动作。

## 投递接口

以下接口需要学生的 Bearer JWT，且只能管理自己的投递：

- `GET /api/student/applications`：查询当前学生的投递列表
- `POST /api/student/applications`：使用本人已发布简历投递已发布岗位，禁止重复投递同一岗位
- `POST /api/student/applications/{id}/action-plan`：为本人单个投递生成本地规则求职行动计划
- `PATCH /api/student/applications/{id}/withdraw`：撤回本人投递

投递请求示例：

```json
{
  "jobId": 1,
  "resumeId": 1
}
```

以下接口需要 HR 的 Bearer JWT，且只能查看和管理自己岗位的投递：

- `GET /api/hr/applications?jobId=`：查询当前 HR 岗位收到的投递，可按本人岗位 ID 筛选
- `GET /api/hr/applications/recommendations?jobId=`：生成本人岗位范围内的候选人推荐排序、风险摘要和建议动作
- `POST /api/hr/applications/{id}/interview-kit`：为本人岗位范围内的单个投递生成本地规则面试题、评分维度、风险关注和建议追问
- `POST /api/hr/applications/{id}/follow-up-advice`：为本人岗位范围内的单个投递生成本地规则跟进优先级、建议状态、风险提醒、建议动作和沟通提示
- `POST /api/hr/applications/{id}/communication-draft`：为本人岗位范围内的单个投递生成本地规则候选人沟通话术草稿
- `PATCH /api/hr/applications/{id}/status`：将投递状态更新为 `REVIEWING`、`INTERVIEW`、`OFFERED` 或 `REJECTED`

候选人推荐排序不调用外部 AI 服务，当前本地规则模型标识为 `local-candidate-ranker-v1`；如果同一简历和岗位已有 AI 匹配结果，会优先使用已有匹配分数作为排序依据，返回推荐分、分数来源、已匹配关键词、待核验关键词、推荐理由、风险摘要和建议动作。
AI 面试题生成不调用外部 AI 服务，当前本地规则模型标识为 `local-interview-kit-v1`；该接口只即时返回结果，不新增持久化表，不自动变更投递状态，已撤回投递会被拒绝生成。
学生端 AI 求职行动计划不调用外部 AI 服务，当前本地规则模型标识为 `local-student-action-plan-v1`；该接口基于本人投递状态、岗位要求、简历内容和已有 AI 匹配分即时生成准备清单、风险提醒和下一步动作，不新增持久化表，也不会自动修改投递状态。
AI 投递跟进建议不调用外部 AI 服务，当前本地规则模型标识为 `local-application-follow-up-v1`；该接口基于当前投递状态、岗位要求、简历内容和已有 AI 匹配分即时生成，不新增持久化表，也不会自动修改投递状态。
AI 候选人沟通话术草稿不调用外部 AI 服务，当前本地规则模型标识为 `local-candidate-communication-draft-v1`；该接口基于 HR 本人岗位范围内的单个投递、岗位要求、简历内容、投递状态和已有 AI 匹配分即时生成沟通场景、主题、开场说明、关键追问、风险提示和后续动作，不新增持久化表，也不会自动修改投递状态。

## AI 匹配接口

AI 匹配使用独立本地技能规则计算分数，再生成固定模板说明，并将结果保存到 `ai_match_result` 表；不会调用外部 AI 服务。当前模型标识为 `local-skill-coverage-v1`，词典版本为 `skill-dictionary-v1`。简历优化建议仍使用原有本地规则，模型标识为 `local-resume-optimizer-v1`。

Day 25 评分口径：

- 只从岗位 `requirements` 提取词典支持的技能，岗位标题、描述和用工类型不计入分母。
- 从简历 `education`、`workExperience`、`projectExperience`、`skills`、`selfEvaluation` 识别技能，Day 26 增加 `sourceText`；简历标题不作为技能证据，字段间不拼接成跨字段技能别名。
- NFKC 全角归一化、忽略大小写、合并空白、别名映射和去重；保留 `C++`、`C#`，不把 `JavaScript` 计为 `Java`，不把 `MySQL` 计为 `SQL`。
- 分数 = 已识别的岗位技能交集数量 / 已识别的岗位技能数量 × 100，四舍五入到两位小数。例：要求 `English, lesson planning, classroom management`，简历含 `英语、教学设计、备课`，去重后为 2/3，即 66.67 分，缺口为 `Classroom management`。
- 没有可识别的岗位技能返回统一 `422` 错误，且不覆盖已有结果；有岗位技能但简历没有识别结果则为 0 分。
- 保持现有响应字段：`analysis` 包含分子、分母、全部识别要求、公式与词典版本；`strengthSummary` 和 `gapSummary` 列出全部已匹配/未匹配技能，`actionSuggestions` 为规则建议。学生和 HR 页面沿用这些字段显示结果。

词典共 29 种技能，完整别名见 `backend/src/main/java/com/example/aijobs/match/SkillNormalizer.java`：

| 范围 | 标准技能 |
| --- | --- |
| 开发 | Java、JavaScript、TypeScript、Python、C++、C#、Spring Boot、MySQL、SQL、Redis、Git、Linux、Vue、React、Node.js、HTML、CSS |
| 办公 | Excel、PowerPoint |
| 教学与协作 | Lesson planning、Classroom management、Assessment、Tutoring、Communication、Teamwork、English、Mandarin、Safeguarding、First aid |

规则边界：未知技能不计入分母；当前只识别文本提及，不理解否定、熟练度、年限或“必需/优先”的语义区别。因此 HR 应把拟评分技能明确填写到岗位要求，并人工核对原始文本。缺口只表示未识别到提及，不表示候选人不具备；分数不代表录用概率，不能替代人工决定。

Day 25 评分算法兼容与回滚：该算法升级本身无数据库迁移、无外部依赖新增、无投递状态自动变更（Day 26 的文件解析依赖及新增列迁移见上文）。旧记录仍显示原模型和原分数，重新生成该简历/岗位匹配才升级为新版本。新旧版本不可直接横向比较；推荐、跟进等模块仍可能读取历史分数，使用前请统一重新生成。回滚 Day 25 提交可以恢复旧算法，但不会恢复已经覆盖的历史分数；如需保留对照快照，应在重新生成前导出原结果。

以下接口需要学生的 Bearer JWT：

- `GET /api/student/matches?resumeId=`：查询本人简历的匹配结果，可按本人简历 ID 筛选
- `POST /api/student/matches`：使用本人已发布简历匹配已发布岗位

以下接口需要 HR 的 Bearer JWT：

- `GET /api/hr/matches?jobId=`：查询本人岗位的匹配结果，可按本人岗位 ID 筛选
- `POST /api/hr/matches`：为已投递到本人岗位的已发布简历生成匹配结果

匹配请求示例：

```json
{
  "resumeId": 1,
  "jobId": 1
}
```

## 管理员看板接口

以下接口需要管理员的 Bearer JWT：

- `GET /api/admin/dashboard`：查询平台用户、岗位、简历、投递、AI 匹配统计和管理端 AI 运营洞察

管理端 AI 运营洞察不调用外部 AI 服务，当前本地规则模型标识为 `local-admin-ai-ops-v1`。该能力基于现有统计数据即时生成，不新增持久化表，返回匹配覆盖率、低分匹配数量、运营健康摘要、重点关注项、风险提醒和建议动作。

## 测试方式

```powershell
cd backend
mvn test
mvn package
cd ../frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm build
```

2026-10-01 的上一阶段记录：后端 `mvn test` 共 147 项通过（失败/错误/跳过均为 0），其中外部岗位新增 30 项覆盖字段解析、HTML 清理、来源链接、薪资/时区、缓存并发、失败恢复、搜索分页与匿名 HTTP 权限。测试使用模拟上游，不依赖外网；当时 `mvn package -DskipTests` 和前端 `pnpm build` 通过。该记录不作为 Day 27 的构建结果。

Day 27 收尾复核于 2026-10-03 完成：`mvn -f backend/pom.xml test` 通过 178 项，失败/错误/跳过均为 0，其中新增 31 项；`mvn -f backend/pom.xml package -DskipTests` 通过；`pnpm --dir frontend build` 通过，保留既有第三方注释和 chunk 体积警告。当前验证与交付状态见 [当天记录](docs/verification/DAY27_2026-10-02.md)。真实模型调用与实际 MySQL 端到端均未验证。前端本次没有修改，因此没有新增页面交互验收。

Day 28 于 2026-10-04 完成：`mvn -f backend/pom.xml test` 通过 185 项，失败/错误/跳过均为 0，其中新增 7 项招聘助手服务测试；`mvn -f backend/pom.xml package -DskipTests` 通过；`pnpm --dir frontend build` 通过，保留既有第三方注释和 chunk 体积警告。当前验证与交付状态见 [当天记录](docs/verification/DAY28_2026-10-04.md)。真实 DeepSeek 调用、真实 MySQL 端到端和浏览器页面交互未验证；本次无前端页面改动。

Day 29 于 2026-10-05 完成：标准 Maven 测试 185 项通过（0 失败/错误/跳过），后端打包和前端构建通过；内置浏览器验证桌面与 390×844 学生/HR 助手、续聊、上下文切换、24 条消息上限、输入限制、等待、失败、过期、纯文本回复及人工取消/确认。接口为独立合成替身，不能视为真实 Spring Boot 对话链路或真实 MySQL/DeepSeek 端到端验证。保留既有 HR 空薪资字段类型警告及构建警告，详见 [当天记录](docs/verification/DAY29_2026-10-05.md)。

Day 30 于 2026-10-06 完成：标准 Maven 测试 191 项通过（0 失败/错误/跳过），其中新增 6 项覆盖受控评估服务、默认降级、假模型生成、越界文本风险标记和 ADMIN 权限；后端打包和前端构建通过，前端仅保留既有第三方注释和 chunk 体积警告。当前验证与交付状态见 [当天记录](docs/verification/DAY30_2026-10-06.md)。真实 DeepSeek 调用、真实 MySQL 端到端和真实招聘效果未验证。

Day 31 于 2026-10-07 补规则基线验收：标准 Maven 测试 205 项通过（0 失败/错误/跳过），新增 9 项共享规则测试、3 项固定场景业务一致性测试、1 项部分降级测试和 1 项 HR 权限测试。管理员 MockMvc 检查改为真实控制器/评估/规则服务配合模拟模型和 mapper，验证新增输入字段及无业务数据访问。后端打包和前端构建通过，详见 [当天记录](docs/verification/DAY31_2026-10-07.md)。本次未改页面，无新增浏览器交互验收。

Day 32 于 2026-10-08 补可复现记录验收：标准 Maven 测试 206 项通过（0 失败/错误/跳过），新增 1 项哈希稳定性测试，并扩展管理员响应检查。后端打包和前端构建通过，详见 [当天记录](docs/verification/DAY32_2026-10-08.md)。本次未改页面，无新增浏览器交互验收，未调用真实 DeepSeek 或真实 MySQL。

Day 33 于 2026-10-09 补人工复核材料验收：目标评估测试 14 项、标准后端测试 208 项通过（0 失败/错误/跳过），后端打包和前端构建通过。新增 2 项覆盖模型伪称人工通过和无缺口场景，扩展部分生成、降级和管理员 JSON 契约检查，详见 [当天记录](docs/verification/DAY33_2026-10-09.md)。本次未调用真实 DeepSeek、真实 MySQL，未执行实际人工质量评估。

Day 34 于 2026-10-10 补隔离 MySQL 验收：本机 MySQL 8.0.46 的 `ai_job_platform_day34_20261010` 临时库成功执行初始化脚本和 Day 26 幂等迁移；真实 Spring Boot 后端连接该库后，通过 HTTP 完成学生/HR 注册登录、HR 岗位创建发布、学生简历创建发布、投递、匹配和 HR 投递查询，`ai_match_result` 落库分数为 75.00，详见 [当天记录](docs/verification/DAY34_2026-10-10.md)。本次只使用合成数据，未调用 DeepSeek，未验证生产库或真实招聘效果。

2026-10-11 界面优化：`node node_modules\vite\bin\vite.js build`（即 `pnpm build`）通过；最终构建产物在独立静态服务上用 Playwright + 合成 `/api` 拦截完成 77 项检查（岗位筛选与详情切换、简历导入预览/取消/确认新建、助手上下文切换与纯文本渲染、HR 状态确认/取消、四种宽度无横向滚动等），0 失败。验证脚本保存在仓库外，没有连接真实后端或数据库，详见 [记录](docs/verification/UI_POLISH_2026-10-11.md)。

Jobicy 真实联调另行验证：启动原有 Spring Boot 服务和 Vite，通过匿名接口实际读取 200 条岗位，页面验证关键词/地区筛选、分页、详情切换、空结果和原站链接；桌面与 390×844 手机布局正常，未使用模拟岗位。这条公开查询链路不访问 MySQL，也未测试外站申请提交或登录后的其他业务。

内置浏览器在 `http://127.0.0.1:5180` 联调真实 Spring Boot 登录、上传、保存与匹配接口（后端 `127.0.0.1:18080`，Mapper/缓存隔离为内存测试替身）。验证 TXT 预览取消后无记录、两页 PDF 提取后可修改并创建本人草稿、再次编辑保留文本、发布后获得 2/3 = 66.67 分；损坏 PDF 正确显示 422，绕过前端上传超限文件时真实 HTTP 返回统一 413。桌面与 390×844 窄屏显示正常，无运行错误或相关控制台警告。测试只用合成文件，未连接真实 MySQL、未在已有数据库执行迁移、未调用外部 AI，不等同于生产数据库全链路验收。

Day 35 于 2026-10-12 完成真实合成比较：`mvn -f backend/pom.xml test` 最终 225 项通过；显式选择 `ControlledComparisonLiveIT` 的两次执行各 1 项通过，共 6 次真实请求，第一轮 3 份截断、第二轮 3 份完整。后端打包、前端构建通过。启动方法见 [适配器说明](docs/DEEPSEEK_ADAPTER.md#day-35-固定合成受控比较真实检查)，结果及限制见 [当天记录](docs/verification/DAY35_2026-10-12.md)；常规测试不会调用真实模型。无页面改动，本次未做浏览器或真实数据库联调。

Day 36 于 2026-10-13 完成：常规 `mvn -f backend/pom.xml test` 226 项通过，0 失败/错误/跳过；新增离线 HTTP 检查含 13 个请求，独立 `RecruitmentChatLiveIT` 1 项通过（同样 13 个 HTTP 请求、仅 3 次真实模型调用）。后端打包、前端构建通过。真实模型返回均为 HTTP 200 / GENERATED / stop，原文仅本地保存，见 [当天记录](docs/verification/DAY36_2026-10-13.md)。真实 MySQL、Redis、登录注册 HTTP、浏览器和人工质量不属于本次验证。

## 本次修改文件

- 检查：`backend/src/test/java/com/example/aijobs/assistant/RecruitmentChatHttpHarness.java`、`RecruitmentChatHttpTests.java`、`RecruitmentChatLiveIT.java`；无生产源码、页面、依赖或数据库变更。
- 文档：README、DAILY_TASKS、ROADMAP、DEEPSEEK_ADAPTER、`docs/verification/DAY36_2026-10-13.md` 及对应 `.results.json`；完整模型原文不提交。

## 下一步

最终验收尚未全部完成。下一步优先由实际复核人按模板审查 Day 35 第二轮完整输出，判断建议是否相关、是否过度推断、相对规则基线是否有价值。Day 36 已补真实助手 HTTP 与模型链路，但数据库为替身；后续真实数据库助手联调须提供运行环境中的本地数据库凭证，浏览器到真实数据库/模型、生产/既有数据库升级和真实招聘效果仍未验证。不重复无目的调用或新增复核元数据。真实协作经历由用户提供，不能用开发补造。全部验收完成后才停用自动化，今天不继续开发其他模块。
