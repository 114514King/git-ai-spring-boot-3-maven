# AI 智能求职招聘平台

这是一个长期迭代的前后端分离项目，面向学生、HR 和管理员三类角色。

## 当前进度

- 当前阶段：Day 12
- 已完成：Spring Boot 基础框架、MySQL 核心表、注册登录、JWT 请求认证、角色权限、岗位、简历、投递、AI 匹配后端模块、Redis 岗位缓存、Vue 3 前端基础框架、前端登录注册和路由守卫
- 尚未开始：学生端、HR 端和管理员业务页面

## 技术栈

- 后端：Spring Boot 3、Maven、MyBatis-Plus、MySQL 8、Spring Security、JWT、Redis
- 前端规划：Vue 3、Vite、Element Plus、Axios、Pinia、Vue Router、ECharts

## 项目结构

```text
backend/
  pom.xml
  src/main/java/com/example/aijobs/
    auth/                  注册、登录、JWT 和角色权限
    common/                统一响应和异常处理
    job/                   岗位查询与 HR 岗位管理
    job/JobCacheService    Redis 岗位列表和详情缓存
    resume/                学生简历维护
    application/           学生投递与 HR 投递管理
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
    router/
    stores/
    views/
    styles.css
```

## 运行方式

先用 MySQL 8 执行初始化脚本，再配置数据库连接与至少 32 字节的 JWT 密钥：

```powershell
mysql -u root -p -e "source docs/init.sql"
cd backend
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "你的本地数据库密码"
$env:JWT_SECRET = "至少32字节的自定义密钥"
mvn spring-boot:run
```

可选环境变量：`DB_URL`、`JWT_EXPIRATION`（默认 `PT2H`）、`REDIS_HOST`（默认 `localhost`）、`REDIS_PORT`（默认 `6379`）、`REDIS_PASSWORD`、`REDIS_DATABASE`（默认 `0`）、`JOB_CACHE_TTL`（默认 `PT10M`）。服务默认地址为 `http://localhost:8080`。

Redis 用于缓存公开岗位列表和公开岗位详情。Redis 未启动时，接口会回退到 MySQL 查询，不影响基础功能；HR 创建、编辑或更新岗位状态后会清理公开岗位缓存。

前端基础工程运行方式：

```powershell
cd frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm install
pnpm dev
```

前端开发服务器默认地址为 `http://localhost:5173`，并将 `/api` 代理到 `http://localhost:8080`。Day 12 已接入 Element Plus、Axios、Pinia 和 Vue Router，提供登录、注册、令牌保存、Bearer 请求头注入、受保护路由和退出登录；尚未实现学生端、HR 端或管理员业务页面。

## 认证接口

- `POST /api/auth/register`：注册 `STUDENT` 或 `HR`
- `POST /api/auth/login`：使用用户名或邮箱登录并获取 Bearer JWT
- `GET /api/access/student`：仅 `STUDENT`
- `GET /api/access/hr`：仅 `HR`
- `GET /api/access/admin`：仅 `ADMIN`

## 岗位接口

公开接口只返回 `PUBLISHED` 岗位：

- `GET /api/jobs?keyword=&city=&employmentType=`：按关键词、城市、用工类型筛选
- `GET /api/jobs/{id}`：查询已发布岗位详情

以下接口需要 HR 的 Bearer JWT，且只能管理自己的岗位：

- `GET /api/hr/jobs`：查询当前 HR 的全部岗位
- `POST /api/hr/jobs`：创建 `DRAFT` 草稿
- `PUT /api/hr/jobs/{id}`：编辑岗位内容
- `PATCH /api/hr/jobs/{id}/status`：将状态设为 `DRAFT`、`PUBLISHED` 或 `CLOSED`

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

## 投递接口

以下接口需要学生的 Bearer JWT，且只能管理自己的投递：

- `GET /api/student/applications`：查询当前学生的投递列表
- `POST /api/student/applications`：使用本人已发布简历投递已发布岗位，禁止重复投递同一岗位
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
- `PATCH /api/hr/applications/{id}/status`：将投递状态更新为 `REVIEWING`、`INTERVIEW`、`OFFERED` 或 `REJECTED`

## AI 匹配接口

AI 匹配使用本地关键词规则生成分数和分析文本，并将结果保存到 `ai_match_result` 表；不会调用外部 AI 服务。

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

## 测试方式

```powershell
cd backend
mvn test
mvn package
cd ../frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm build
```

当前后端共 49 个测试，Day 10 新增岗位缓存路径测试，覆盖公开岗位列表缓存命中、缓存写入以及 HR 岗位变更后的缓存清理；既有认证、岗位、简历、投递和 AI 匹配测试继续通过。Day 12 前端执行 `pnpm build` 通过，验证登录注册页面、Pinia 认证状态、Axios 请求封装和路由守卫可构建。

## 本次修改文件

- `frontend/package.json`
- `frontend/pnpm-lock.yaml`
- `frontend/src/main.js`
- `frontend/src/App.vue`
- `frontend/src/api/http.js`
- `frontend/src/router/index.js`
- `frontend/src/stores/auth.js`
- `frontend/src/views/LoginView.vue`
- `frontend/src/views/RegisterView.vue`
- `frontend/src/views/ProtectedHomeView.vue`
- `frontend/src/styles.css`
- `README.md`
- `docs/DAILY_TASKS.md`

## 下一步

Day 13：实现学生端页面，不提前实现 HR 端、管理员看板或统计图表。
