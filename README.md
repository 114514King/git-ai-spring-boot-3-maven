# AI 智能求职招聘平台

这是一个长期迭代的前后端分离项目，面向学生、HR 和管理员三类角色。

## 当前进度

- 当前阶段：Day 29（2026-10-05），学生/HR 助手页面和投递状态人工确认已实现，标准后端测试、打包、前端构建及合成接口浏览器验证通过。
- 已完成：Spring Boot 基础框架、MySQL 核心表、注册登录、JWT 请求认证、角色权限、岗位、简历、投递、AI 匹配后端模块、Redis 岗位缓存、Vue 3 前端基础框架、前端登录注册和路由守卫、学生端岗位/简历/投递/AI 匹配页面、HR 端岗位/投递/AI 匹配页面、管理员看板和统计图表、可解释 AI 匹配评分、简历智能优化建议、岗位 JD 智能解析与优化建议、HR 候选人推荐排序与风险摘要、AI 面试题生成与评分维度、管理端 AI 运营洞察、HR 投递跟进建议、学生端 AI 求职行动计划、HR 候选人沟通话术草稿
- Day 25 新增：中英文技能别名归一化、去重、按岗位所需技能覆盖率评分和完整技能缺口展示。
- Day 26 新增：文本型 PDF / UTF-8 TXT 上传提取、可编辑预览、确认创建本人草稿、导入文本保存与技能匹配接入。
- Day 27 新增：独立 DeepSeek 文本适配器，默认禁用，支持环境变量配置、完整请求超时、响应限额和明确降级状态。此阶段没有对外对话接口或页面入口，既有规则评分与招聘状态仍独立运行。配置及内部契约见 [适配器说明](docs/DEEPSEEK_ADAPTER.md)。
- Day 28 新增：学生端和 HR 端服务端招聘助手对话接口，支持会话归属隔离、历史长度限制、服务端角色提示、本人/所属岗位招聘上下文和只读匹配分引用。默认 DeepSeek 关闭或异常时返回明确降级说明，不修改匹配分、投递状态或招聘决定。
- Day 29 新增：学生“求职助手”和 HR“招聘助手”选项卡，支持上下文选择、当前会话消息、只读规则分、模型辅助建议与降级说明；HR 投递状态变更增加人工确认，助手入口只跳转复核，不自动更新状态。
- 2026-10-01 补充：统一登录、注册和三类工作台的青绿色主题、页头、面板与手机布局；改善岗位薪资、中文用工类型和选中状态，替换登录页过时的开发说明。
- 外部岗位接入：已接入 Jobicy 公开远程岗位，提供匿名浏览、关键词/地区筛选、分页、详情和原站申请入口。BOSS直聘、智联和前程无忧仍缺少本次可确认的直接可用岗位读取接口；来源依据和实现边界见 [接入说明](docs/EXTERNAL_JOB_DATA.md)。
- 尚未开始：Day 30 的规则/LLM 对照评估。适配器、对话接口和助手页面存在不代表真实模型调用已经通过。

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

### 助手页面与人工复核（Day 29）

学生登录后在 `/app` 的“求职助手”选择通用、岗位与本人简历或本人投递；HR 在 `/hr` 的“招聘助手”选择通用、本人岗位或所属岗位投递。切换上下文或点击“新对话”清空当前消息与草稿，后续请求不带旧会话 ID。当前页最多展示最近 24 条消息；服务端按既有历史配置带入上下文。聊天仅保存在当前页面内存，刷新、退出或切换角色后不恢复旧消息，也不写入浏览器存储。

单条消息最多 1000 个 UTF-16 字符单位，等待回复时禁止重复发送或切换上下文。助手请求前端超时为 90 秒，高于后端默认的 15 秒完整请求超时；HTTP/网络失败保留输入，已有会话遇到 403/404 提示新建对话，不自动重试。回复使用 Vue 纯文本插值，独立展示只读规则分、来源、模型辅助建议或降级说明，不执行模型输出中的 HTML/脚本。

HR 的“人工复核投递”按钮只跳转投递管理。选择新状态后，必须在确认弹窗中核对投递 ID、原状态和目标状态；取消或关闭不发送写请求，只有“确认更新”调用既有状态接口。无需数据库升级，运行方式沿用上文；回滚本次提交可恢复 Day 28 页面行为。

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

Jobicy 真实联调另行验证：启动原有 Spring Boot 服务和 Vite，通过匿名接口实际读取 200 条岗位，页面验证关键词/地区筛选、分页、详情切换、空结果和原站链接；桌面与 390×844 手机布局正常，未使用模拟岗位。这条公开查询链路不访问 MySQL，也未测试外站申请提交或登录后的其他业务。

内置浏览器在 `http://127.0.0.1:5180` 联调真实 Spring Boot 登录、上传、保存与匹配接口（后端 `127.0.0.1:18080`，Mapper/缓存隔离为内存测试替身）。验证 TXT 预览取消后无记录、两页 PDF 提取后可修改并创建本人草稿、再次编辑保留文本、发布后获得 2/3 = 66.67 分；损坏 PDF 正确显示 422，绕过前端上传超限文件时真实 HTTP 返回统一 413。桌面与 390×844 窄屏显示正常，无运行错误或相关控制台警告。测试只用合成文件，未连接真实 MySQL、未在已有数据库执行迁移、未调用外部 AI，不等同于生产数据库全链路验收。

## 本次修改文件

- `frontend/src/api/assistant.js`：复用统一 HTTP 客户端调用学生/HR 助手接口。
- `frontend/src/components/RecruitmentAssistantPanel.vue`：上下文、当前会话、只读分数、降级与错误展示。
- `frontend/src/views/StudentDashboardView.vue`、`frontend/src/views/HrDashboardView.vue`：角色入口、HR 人工复核跳转及状态更新确认。
- `README.md`、`docs/DAILY_TASKS.md`、`docs/ROADMAP.md` 和当天验证记录。

## 下一步

下一模块为 Day 30：系统验证与固定输入下的规则/LLM 受控比较。完整验收目标见 `docs/ROADMAP.md`；计划项不能写成已完成能力，团队领导与协作经历只能依据真实参与记录说明。
