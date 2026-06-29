# 每日开发计划

## 执行规则

- 每天只完成当天任务。
- 不提前实现后续天数的功能。
- 每次改动必须保持项目能启动或能通过当前阶段的验证。
- 后端接口统一返回格式。
- 代码命名清晰。
- 不生成伪代码。
- 不留下空实现。
- 每天任务完成后更新 README 和本文档。
- 每天任务完成后运行构建或测试。
- 每天任务完成后提交一个清晰的 commit。
- 如果当前仓库连接 GitHub，提交后创建 Pull Request。

## 15 天计划

| 天数 | 任务 | 状态 |
| --- | --- | --- |
| Day 1 | 初始化项目结构、README、ROADMAP、AGENTS.md | 已完成 |
| Day 2 | 搭建 Spring Boot 后端基础框架 | 已完成 |
| Day 3 | 设计 MySQL 表结构和 init.sql | 已完成 |
| Day 4 | 实现用户注册、登录、JWT | 已完成 |
| Day 5 | 实现角色权限控制 | 已完成 |
| Day 6 | 实现岗位模块后端接口 | 已完成 |
| Day 7 | 实现简历模块后端接口 | 已完成 |
| Day 8 | 实现投递模块后端接口 | 已完成 |
| Day 9 | 实现 AI 匹配模块 | 已完成 |
| Day 10 | 接入 Redis 缓存 | 已完成 |
| Day 11 | 搭建 Vue3 前端基础框架 | 已完成 |
| Day 12 | 实现登录、注册、路由守卫 | 已完成 |
| Day 13 | 实现学生端页面 | 已完成 |
| Day 14 | 实现 HR 端页面 | 已完成 |
| Day 15 | 实现管理员看板、统计图表、完善 README 和部署文档 | 未开始 |

## Day 1 记录

### 完成了什么

- 初始化 Git 仓库。
- 创建基础目录：`backend/`、`frontend/`、`docs/`。
- 创建项目说明文件：`README.md`。
- 创建项目路线图：`docs/ROADMAP.md`。
- 创建每日开发计划：`docs/DAILY_TASKS.md`。
- 创建 Codex 工作规范：`AGENTS.md`。
- 创建数据库占位文件：`docs/init.sql`。

### 修改了哪些文件

- `README.md`
- `docs/ROADMAP.md`
- `docs/DAILY_TASKS.md`
- `AGENTS.md`
- `backend/.gitkeep`
- `frontend/.gitkeep`
- `docs/init.sql`

### 如何运行

Day 1 尚未创建可运行应用。当前阶段只需确认仓库结构完整。

### 如何测试

```powershell
Test-Path README.md
Test-Path AGENTS.md
Test-Path docs/ROADMAP.md
Test-Path docs/DAILY_TASKS.md
Test-Path docs/init.sql
Test-Path backend/.gitkeep
Test-Path frontend/.gitkeep
```

### 下一天该做什么

Day 2：搭建 Spring Boot 后端基础框架，并保证后端项目可以构建或启动。

## Day 2 记录

### 完成了什么

- 在 `backend/` 下创建 Maven 工程。
- 引入 Spring Boot 3 Web、Validation 和 Test 基础依赖。
- 创建后端启动类：`AiJobPlatformApplication`。
- 创建基础配置文件：`application.yml`，设置应用名称和默认端口。
- 创建最小测试，验证应用入口类可加载。
- 未实现注册登录、业务接口、数据库连接或前端功能。

### 修改了哪些文件

- `backend/pom.xml`
- `backend/src/main/java/com/example/aijobs/AiJobPlatformApplication.java`
- `backend/src/main/resources/application.yml`
- `backend/src/test/java/com/example/aijobs/AiJobPlatformApplicationTests.java`
- `.gitignore`
- `README.md`
- `docs/DAILY_TASKS.md`

### 如何运行

```powershell
cd backend
mvn spring-boot:run
```

启动后默认监听 `http://localhost:8080`。Day 2 尚未提供业务接口。

### 如何测试

```powershell
cd backend
mvn test
```

或执行完整打包构建：

```powershell
cd backend
mvn package
```

### 下一天该做什么

Day 3：设计 MySQL 表结构并更新 `docs/init.sql`，不提前实现后端业务接口。

## Day 3 记录

### 完成了什么

- 创建 MySQL 8 数据库 `ai_job_platform` 的可重复执行初始化脚本。
- 设计用户、角色、用户角色、简历、岗位、投递和 AI 匹配结果共 7 张核心表。
- 为用户登录标识、角色编码、重复投递和重复匹配设置唯一约束。
- 为岗位列表、投递列表、用户角色和匹配分数等常用查询设置索引。
- 设置外键、状态检查、薪资范围和匹配分数检查约束。
- 初始化学生、HR、管理员三个基础角色。
- 未接入 MyBatis-Plus、MySQL 数据源、注册登录或业务接口。

### 修改了哪些文件

- `docs/init.sql`
- `README.md`
- `docs/DAILY_TASKS.md`

### 如何运行

后端仍可按 Day 2 的方式启动：

```powershell
cd backend
mvn spring-boot:run
```

使用 MySQL 8 初始化数据库：

```powershell
mysql -u root -p -e "source docs/init.sql"
```

脚本会删除并重建其管理的 7 张表，仅适合本地初始化或可重置环境。

### 如何测试

验证后端现有测试和构建：

```powershell
cd backend
mvn test
mvn package
```

如果本机已安装并启动 MySQL 8，可执行初始化脚本后检查表结构：

```powershell
mysql -u root -p -e "source docs/init.sql"
mysql -u root -p -e "USE ai_job_platform; SHOW TABLES;"
```

### 下一天该做什么

Day 4：实现用户注册、登录和 JWT，不提前实现角色权限控制或其他业务模块。

## Day 4 记录

### 完成了什么

- 接入 MyBatis-Plus 和 MySQL 数据源，创建用户、角色、用户角色实体及 Mapper。
- 实现 `POST /api/auth/register`，支持学生和 HR 注册，禁止管理员自助注册。
- 注册时校验用户名、邮箱和手机号冲突，使用 BCrypt 保存密码哈希，并在事务中关联基础角色。
- 实现 `POST /api/auth/login`，支持用户名或邮箱登录，校验密码和用户状态。
- 实现 JWT 签发和校验，令牌包含用户 ID、用户名、签发时间和过期时间。
- 添加统一响应格式、参数校验及注册登录相关异常处理。
- 未实现 JWT 请求过滤、角色权限控制或其他业务模块。

### 修改了哪些文件

- `backend/pom.xml`
- `backend/src/main/resources/application.yml`
- `backend/src/main/java/com/example/aijobs/auth/`
- `backend/src/main/java/com/example/aijobs/common/`
- `backend/src/test/java/com/example/aijobs/auth/`
- `README.md`
- `docs/DAILY_TASKS.md`

### 如何运行

先初始化数据库，然后通过环境变量提供本地数据库密码和至少 32 字节的 JWT 密钥：

```powershell
mysql -u root -p -e "source docs/init.sql"
cd backend
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "你的本地数据库密码"
$env:JWT_SECRET = "至少32字节的自定义密钥"
mvn spring-boot:run
```

启动后可调用 `POST /api/auth/register` 和 `POST /api/auth/login`。

### 如何测试

```powershell
cd backend
mvn test
mvn package
```

本次还使用隔离数据库 `ai_job_platform_day4_validation` 验证了真实注册和登录链路、BCrypt 密码哈希、学生角色关联及 JWT 返回；验证完成后已删除隔离数据库。

### 下一天该做什么

Day 5：实现 JWT 请求认证和基于角色的权限控制，不提前实现岗位、简历或投递模块。

## Day 5 记录

### 完成了什么

- 接入 Spring Security，配置无状态请求认证并关闭不适用于 REST API 的会话和 CSRF 机制。
- 实现 Bearer JWT 请求过滤器，校验令牌签名和有效期，并从数据库加载用户状态及当前角色。
- 将数据库角色映射为 Spring Security 权限，按 `STUDENT`、`HR`、`ADMIN` 隔离接口访问。
- 新增学生、HR、管理员三个最小权限验证接口，用于验证合法访问和跨角色拒绝。
- 对缺少/无效令牌返回统一 `401` 响应，对角色不匹配返回统一 `403` 响应。
- 新增请求过滤和角色授权测试；未实现岗位、简历、投递或其他后续业务模块。

### 修改了哪些文件

- `backend/pom.xml`
- `backend/src/main/java/com/example/aijobs/auth/`
- `backend/src/test/java/com/example/aijobs/auth/`
- `README.md`
- `docs/DAILY_TASKS.md`

### 如何运行

先初始化数据库并配置数据库连接和 JWT 密钥：

```powershell
mysql -u root -p -e "source docs/init.sql"
cd backend
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "你的本地数据库密码"
$env:JWT_SECRET = "至少32字节的自定义密钥"
mvn spring-boot:run
```

登录后将 `accessToken` 作为 Bearer 令牌访问对应角色接口：

```powershell
$token = "登录响应中的 accessToken"
Invoke-RestMethod http://localhost:8080/api/access/student `
  -Headers @{ Authorization = "Bearer $token" }
```

### 如何测试

```powershell
cd backend
mvn test
mvn package
```

测试覆盖有效和无效 JWT、数据库角色加载、匿名请求拒绝、同角色访问成功以及跨角色访问被拒绝。

### 下一天该做什么

Day 6：实现岗位模块后端接口，不提前实现简历、投递或后续模块。

## Day 6 记录

### 完成了什么

- 新增岗位实体和 MyBatis-Plus Mapper，对接 `job_posting` 表。
- 新增公开岗位列表和详情接口，仅展示 `PUBLISHED` 岗位，支持关键词、城市和用工类型筛选。
- 新增 HR 岗位管理接口，支持创建草稿、查看本人岗位、编辑岗位和更新岗位状态。
- 写操作限制为 HR 角色，并校验岗位归属、用工类型、状态和薪资区间。
- 发布岗位时记录首次发布时间；未实现简历、投递、AI 匹配、Redis 或前端功能。
- 新增岗位服务和权限测试。

### 修改了哪些文件

- `backend/src/main/java/com/example/aijobs/auth/SecurityConfiguration.java`
- `backend/src/main/java/com/example/aijobs/job/`
- `backend/src/test/java/com/example/aijobs/job/`
- `README.md`
- `docs/DAILY_TASKS.md`

### 如何运行

```powershell
mysql -u root -p -e "source docs/init.sql"
cd backend
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "你的本地数据库密码"
$env:JWT_SECRET = "至少32字节的自定义密钥"
mvn spring-boot:run
```

公开访问 `GET /api/jobs` 和 `GET /api/jobs/{id}`；HR 登录后使用 Bearer JWT 访问 `/api/hr/jobs`。

### 如何测试

```powershell
cd backend
mvn test
mvn package
```

本次运行环境无法连接 Maven Central，且没有可用的本地 Maven 依赖缓存，因此上述 Maven 命令在父 POM 解析阶段被环境阻止。已使用上次构建产物中包含的依赖，对全部 32 个主代码源文件执行独立 `javac` 编译检查并通过。

### 下一天该做什么

Day 7：实现简历模块后端接口，不提前实现投递、AI 匹配或后续模块。

## Day 7 记录

### 完成了什么

- 新增简历实体和 MyBatis-Plus Mapper，对接 `resume` 表。
- 新增学生简历管理接口，支持创建草稿、查看本人简历列表和详情、编辑简历以及更新简历状态。
- 所有简历接口仅允许 `STUDENT` 角色访问，并校验简历归属，禁止跨学生读取或修改。
- 校验简历标题必填且最长 100 字符，状态仅允许 `DRAFT` 和 `PUBLISHED`。
- 新增简历服务和接口权限测试，并修复既有岗位测试在新版 MyBatis-Plus 方法重载下的类型歧义和事务测试隔离。
- 未实现投递、HR 查看简历、AI 匹配、Redis 或前端功能。

### 修改了哪些文件

- `backend/src/main/java/com/example/aijobs/auth/SecurityConfiguration.java`
- `backend/src/main/java/com/example/aijobs/resume/`
- `backend/src/test/java/com/example/aijobs/resume/`
- `backend/src/test/java/com/example/aijobs/job/JobAuthorizationTests.java`
- `backend/src/test/java/com/example/aijobs/job/JobServiceTests.java`
- `README.md`
- `docs/DAILY_TASKS.md`

### 如何运行

```powershell
mysql -u root -p -e "source docs/init.sql"
cd backend
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "你的本地数据库密码"
$env:JWT_SECRET = "至少32字节的自定义密钥"
mvn spring-boot:run
```

学生登录后，将 `accessToken` 作为 Bearer 令牌访问 `/api/student/resumes`。

### 如何测试

```powershell
cd backend
mvn test
mvn package
```

本次 `mvn test` 的 25 个测试全部通过，`mvn package` 成功生成可执行 JAR。

### 下一天该做什么

Day 8：实现投递模块后端接口，不提前实现 AI 匹配、Redis 或后续模块。

## Day 8 记录

### 完成了什么

- 新增投递实体和 MyBatis-Plus Mapper，对接 `job_application` 表。
- 新增学生投递接口，支持查看本人投递列表、使用本人已发布简历投递已发布岗位，以及撤回本人投递。
- 投递创建时校验岗位必须已发布、简历必须属于当前学生且已发布，并禁止同一学生重复投递同一岗位。
- 新增 HR 投递管理接口，支持查看本人岗位收到的投递，并将投递状态更新为 `REVIEWING`、`INTERVIEW`、`OFFERED` 或 `REJECTED`。
- 所有投递管理接口按 `STUDENT` 和 `HR` 角色隔离，并校验学生投递归属和 HR 岗位归属。
- 新增投递服务和接口权限测试；未实现 AI 匹配、Redis、前端或后续模块。

### 修改了哪些文件

- `backend/src/main/java/com/example/aijobs/auth/SecurityConfiguration.java`
- `backend/src/main/java/com/example/aijobs/application/`
- `backend/src/test/java/com/example/aijobs/application/`
- `README.md`
- `docs/DAILY_TASKS.md`

### 如何运行

```powershell
mysql -u root -p -e "source docs/init.sql"
cd backend
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "你的本地数据库密码"
$env:JWT_SECRET = "至少32字节的自定义密钥"
mvn spring-boot:run
```

学生登录后，将 `accessToken` 作为 Bearer 令牌访问 `/api/student/applications`；HR 登录后访问 `/api/hr/applications`。

### 如何测试

```powershell
cd backend
mvn test
mvn package
```

本次 `mvn test` 的 37 个测试全部通过，覆盖投递创建、重复投递拦截、简历发布状态校验、学生撤回、HR 岗位归属校验、投递状态更新以及 STUDENT/HR/匿名权限隔离。

### 下一天该做什么

Day 9：实现 AI 匹配模块，不提前实现 Redis、前端或后续模块。

## Day 9 记录

### 完成了什么

- 新增 AI 匹配结果实体和 MyBatis-Plus Mapper，对接 `ai_match_result` 表。
- 新增本地关键词匹配服务，基于简历技能、经历与岗位要求生成 0 到 100 的匹配分数、分析文本和模型名称 `local-keyword-match-v1`。
- 新增学生 AI 匹配接口，支持查看本人简历的匹配结果，以及使用本人已发布简历匹配已发布岗位。
- 新增 HR AI 匹配接口，支持查看本人岗位的匹配结果，以及为已投递到本人岗位的已发布简历生成匹配结果。
- 匹配结果按简历和岗位唯一保存；重复生成同一简历和岗位的匹配时更新既有结果。
- 所有 AI 匹配接口按 `STUDENT` 和 `HR` 角色隔离，并校验学生简历归属、HR 岗位归属和 HR 可匹配范围。
- 新增 AI 匹配服务和接口权限测试；未实现 Redis、外部 AI 服务、前端或后续模块。

### 修改了哪些文件

- `backend/src/main/java/com/example/aijobs/auth/SecurityConfiguration.java`
- `backend/src/main/java/com/example/aijobs/match/`
- `backend/src/test/java/com/example/aijobs/match/`
- `README.md`
- `docs/DAILY_TASKS.md`

### 如何运行

```powershell
mysql -u root -p -e "source docs/init.sql"
cd backend
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "你的本地数据库密码"
$env:JWT_SECRET = "至少32字节的自定义密钥"
mvn spring-boot:run
```

学生登录后，将 `accessToken` 作为 Bearer 令牌访问 `/api/student/matches`；HR 登录后访问 `/api/hr/matches`。

### 如何测试

```powershell
cd backend
mvn test
mvn package
```

本次 `mvn test` 的 48 个测试全部通过，覆盖学生生成匹配、已有结果更新、学生简历归属校验、HR 岗位归属校验、HR 仅能匹配已投递到本人岗位的简历，以及 STUDENT/HR/匿名权限隔离。

### 下一天该做什么

Day 10：接入 Redis 缓存，不提前实现前端或后续模块。

## Day 10 记录

### 完成了什么

- 引入 Spring Boot Redis 依赖，配置 Redis 连接环境变量和岗位缓存 TTL。
- 新增 `JobCacheService`，使用 Redis 缓存公开岗位列表和公开岗位详情。
- 公开岗位列表和详情查询会优先读取缓存，未命中时查询 MySQL 并写入缓存。
- Redis 读取、写入或清理失败时自动回退，不影响 MySQL 查询和岗位写操作。
- HR 创建、编辑或更新岗位状态后清理公开岗位列表缓存，并清理对应岗位详情缓存。
- 新增岗位缓存路径测试；未实现前端、登录注册页面或后续业务模块。

### 修改了哪些文件

- `backend/pom.xml`
- `backend/src/main/resources/application.yml`
- `backend/src/main/java/com/example/aijobs/job/JobCacheService.java`
- `backend/src/main/java/com/example/aijobs/job/JobService.java`
- `backend/src/main/java/com/example/aijobs/auth/SecurityConfiguration.java`
- `backend/src/test/java/com/example/aijobs/job/JobServiceTests.java`
- `README.md`
- `docs/DAILY_TASKS.md`

### 如何运行

先初始化 MySQL，并按需启动本地 Redis：

```powershell
mysql -u root -p -e "source docs/init.sql"
redis-server
cd backend
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "你的本地数据库密码"
$env:JWT_SECRET = "至少32字节的自定义密钥"
$env:REDIS_HOST = "localhost"
$env:REDIS_PORT = "6379"
mvn spring-boot:run
```

可选配置：`REDIS_PASSWORD`、`REDIS_DATABASE`、`JOB_CACHE_TTL`。Redis 未启动时，岗位接口会回退到 MySQL 查询。

### 如何测试

```powershell
cd backend
mvn test
mvn package
```

本次 `mvn test` 的 49 个测试全部通过，覆盖公开岗位列表缓存命中、缓存写入、岗位变更清理缓存，以及既有认证、岗位、简历、投递和 AI 匹配流程。

### 下一天该做什么

Day 11：搭建 Vue3 前端基础框架，不提前实现登录注册或业务页面。

## Day 11 记录

### 完成了什么

- 在 `frontend/` 下创建 Vue 3 + Vite 基础工程。
- 新增前端入口文件、根组件和全局样式，提供可启动的项目应用壳。
- 配置 Vite 开发服务器，默认监听 `5173` 端口，并将 `/api` 代理到后端 `http://localhost:8080`。
- 使用 pnpm 管理前端依赖并生成 `pnpm-lock.yaml`。
- 更新 `.gitignore`，忽略前端 `node_modules/` 和 `dist/` 生成目录。
- 未接入 Element Plus、Axios、Pinia、Vue Router，未实现登录注册、路由守卫或业务页面。

### 修改了哪些文件

- `.gitignore`
- `frontend/package.json`
- `frontend/pnpm-lock.yaml`
- `frontend/pnpm-workspace.yaml`
- `frontend/index.html`
- `frontend/vite.config.js`
- `frontend/src/main.js`
- `frontend/src/App.vue`
- `frontend/src/styles.css`
- `README.md`
- `docs/DAILY_TASKS.md`

### 如何运行

```powershell
cd frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm install
pnpm dev
```

启动后默认访问 `http://localhost:5173`。如需联调后端，先按后端运行说明启动 Spring Boot 服务，前端 `/api` 请求会代理到 `http://localhost:8080`。

### 如何测试

```powershell
cd frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm build
```

本次 `pnpm build` 已通过，验证 Vue 3 + Vite 基础工程可构建。

### 下一天该做什么

Day 12：实现登录、注册和路由守卫，不提前实现学生端、HR 端或管理员业务页面。

## Day 12 记录

### 完成了什么

- 接入 Element Plus、Axios、Pinia 和 Vue Router。
- 新增登录页，调用 `POST /api/auth/login`，保存后端返回的 Bearer JWT 和过期时间。
- 新增注册页，调用 `POST /api/auth/register`，仅支持 `STUDENT` 和 `HR` 自助注册。
- 新增 Axios 请求封装，自动为已登录请求注入 `Authorization: Bearer <token>`。
- 新增 Pinia 认证状态管理，支持登录态持久化和退出登录。
- 新增 `/app` 受保护路由，用于验证路由守卫和登录态，不包含学生端、HR 端或管理员业务功能。
- 未实现学生端岗位、简历、投递页面；未实现 HR 端页面；未实现管理员看板或统计图表。

### 修改了哪些文件

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

### 如何运行

先按后端运行说明启动 Spring Boot 服务，再启动前端：

```powershell
cd frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm install
pnpm dev
```

启动后访问 `http://localhost:5173`。登录和注册请求会通过 Vite `/api` 代理转发到 `http://localhost:8080`。

### 如何测试

```powershell
cd frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm build
```

本次 `pnpm build` 已通过，验证登录注册页面、Pinia 认证状态、Axios 请求封装和路由守卫可构建。构建过程中出现第三方依赖注释和 chunk 体积警告，不影响构建结果。

### 下一天该做什么

Day 13：实现学生端页面，不提前实现 HR 端、管理员看板或统计图表。

## Day 13 记录

### 完成了什么

- 新增学生端 API 封装，统一调用公开岗位、学生简历、学生投递和学生 AI 匹配接口。
- 将受保护路由 `/app` 切换为学生端工作台页面。
- 新增学生端岗位浏览页面，支持公开岗位关键词、城市和用工类型筛选，查看岗位详情，并从已发布简历发起投递或 AI 匹配。
- 新增学生端简历维护页面，支持创建草稿、编辑简历、发布简历和设回草稿。
- 新增学生端投递记录页面，支持查看本人投递状态并撤回本人投递。
- 新增学生端 AI 匹配页面，支持选择公开岗位和已发布简历生成匹配结果，并查看历史匹配分数和分析文本。
- 扩展前端样式，提供学生工作台、统计条、列表、详情、表单和响应式布局。
- 未实现 HR 端页面、管理员看板、统计图表或新的后端接口。

### 修改了哪些文件

- `frontend/src/api/student.js`
- `frontend/src/router/index.js`
- `frontend/src/views/StudentDashboardView.vue`
- `frontend/src/styles.css`
- `README.md`
- `docs/DAILY_TASKS.md`

### 如何运行

先按后端运行说明启动 Spring Boot 服务，再启动前端：

```powershell
cd frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm install
pnpm dev
```

启动后访问 `http://localhost:5173`，登录学生账号后进入 `/app` 使用学生端工作台。岗位、简历、投递和匹配请求会通过 Vite `/api` 代理转发到 `http://localhost:8080`。

### 如何测试

```powershell
cd frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm build
```

本次 `pnpm build` 已通过，验证学生端岗位浏览、简历维护、投递记录和 AI 匹配页面可构建。构建过程中出现第三方依赖注释和 chunk 体积警告，不影响构建结果。

### 下一天该做什么

Day 14：实现 HR 端页面，不提前实现管理员看板或统计图表。

## Day 14 记录

### 完成了什么

- 新增 HR 端 API 封装，统一调用 HR 岗位管理、HR 投递管理和 HR AI 匹配接口。
- 新增受保护路由 `/hr`，用于访问 HR 招聘工作台。
- 新增 HR 端岗位管理页面，支持创建岗位草稿、编辑岗位、发布岗位、设回草稿和关闭岗位。
- 新增 HR 端投递管理页面，支持按本人岗位筛选投递，并将投递状态更新为筛选中、面试、已录用或未通过。
- 新增 HR 端 AI 匹配页面，支持为已投递到本人岗位的简历生成匹配结果，并查看本人岗位范围内的历史匹配分数和分析文本。
- 复用现有 Element Plus、Axios、Pinia、Vue Router 和工作台样式，不新增后端接口。
- 未实现管理员看板、统计图表、部署文档或新的后端业务模块。

### 修改了哪些文件

- `frontend/src/api/hr.js`
- `frontend/src/router/index.js`
- `frontend/src/views/HrDashboardView.vue`
- `README.md`
- `docs/DAILY_TASKS.md`

### 如何运行

先按后端运行说明启动 Spring Boot 服务，再启动前端：

```powershell
cd frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm install
pnpm dev
```

启动后访问 `http://localhost:5173`，登录 HR 账号后访问 `/hr` 使用 HR 招聘工作台。岗位、投递和匹配请求会通过 Vite `/api` 代理转发到 `http://localhost:8080`。

### 如何测试

```powershell
cd frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm build
```

本次 `pnpm build` 已通过，验证 HR 端岗位管理、投递管理和 AI 匹配页面可构建。构建过程中出现第三方依赖注释和 chunk 体积警告，不影响构建结果。

### 下一天该做什么

Day 15：实现管理员看板、统计图表、完善 README 和部署文档。
