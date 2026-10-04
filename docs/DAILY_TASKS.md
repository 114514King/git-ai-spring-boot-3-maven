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
| Day 15 | 实现管理员看板、统计图表、完善 README 和部署文档 | 已完成 |

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

## Day 15 记录

### 完成了什么

- 新增管理员端只读统计接口 `GET /api/admin/dashboard`，仅允许 `ADMIN` 角色访问。
- 新增管理员统计聚合服务，统计用户总数和角色分布、岗位状态、简历发布情况、投递状态、AI 匹配总数和平均分。
- 新增管理员统计服务单元测试，覆盖核心聚合结果。
- 前端新增管理员 API 封装和 `/admin` 路由。
- 前端新增管理员运营看板页面，使用 ECharts 展示用户角色分布、岗位状态、投递流转和 AI 匹配均分。
- 补充 README 和本文件，记录 Day 15 进度、运行方式、测试方式、修改文件和后续建议。

### 修改了哪些文件

- `backend/src/main/java/com/example/aijobs/admin/`
- `backend/src/main/java/com/example/aijobs/auth/SecurityConfiguration.java`
- `backend/src/test/java/com/example/aijobs/admin/AdminDashboardServiceTests.java`
- `frontend/package.json`
- `frontend/pnpm-lock.yaml`
- `frontend/src/api/admin.js`
- `frontend/src/router/index.js`
- `frontend/src/views/AdminDashboardView.vue`
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

启动后访问 `http://localhost:5173`，管理员登录后访问 `/admin` 查看平台运营看板。管理员统计请求会通过 Vite `/api` 代理转发到 `http://localhost:8080/api/admin/dashboard`。

### 如何测试

```powershell
cd backend
mvn test
mvn package
cd ../frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm build
```

本次 `mvn test` 和 `mvn package` 均通过，后端共 50 个测试；`pnpm build` 通过，验证管理员看板和统计图表可以生产构建。前端构建过程中出现第三方依赖注释和 chunk 体积警告，不影响构建结果。

### 下一天该做什么

15 天基础迭代计划已完成。后续建议进入联调、部署和体验优化阶段，例如补充真实管理员账号初始化说明、前端按角色自动跳转、生产环境配置和接口联调验收。

## Day 16 任务

### 当天任务边界

- 实现可解释 AI 匹配评分，只补充匹配优势、匹配缺口和建议动作。
- 继续使用本地关键词规则，不接入外部 AI 或模型 API。
- 后端匹配结果在原有分数和分析文本基础上，返回结构化解释字段。
- 前端学生端和 HR 端 AI 匹配结果展示新增解释信息。
- 不实现简历智能优化、JD 智能解析、HR 推荐排序、面试题生成或管理端 AI 运营洞察。

### 状态

已完成

### 完成了什么

- 将本地 AI 匹配模型标识升级为 `local-keyword-match-v2`。
- 在 `ai_match_result` 表中新增匹配优势、匹配缺口和建议动作字段。
- 后端匹配结果响应新增 `strengthSummary`、`gapSummary` 和 `actionSuggestions`。
- 匹配服务继续使用本地关键词规则，根据重合关键词、缺口关键词和分数区间生成解释文本。
- 学生端和 HR 端 AI 匹配结果卡片新增匹配优势、匹配缺口和建议动作展示。
- 未接入外部 AI 或模型 API，未实现简历智能优化、JD 智能解析、HR 推荐排序、面试题生成或管理端 AI 运营洞察。

### 修改了哪些文件

- `backend/src/main/java/com/example/aijobs/match/AiMatchService.java`
- `backend/src/main/java/com/example/aijobs/match/dto/MatchResponse.java`
- `backend/src/main/java/com/example/aijobs/match/entity/AiMatchResult.java`
- `backend/src/test/java/com/example/aijobs/match/AiMatchServiceTests.java`
- `backend/src/test/java/com/example/aijobs/match/AiMatchAuthorizationTests.java`
- `docs/init.sql`
- `frontend/src/views/StudentDashboardView.vue`
- `frontend/src/views/HrDashboardView.vue`
- `frontend/src/styles.css`
- `README.md`
- `docs/DAILY_TASKS.md`

### 如何运行

先按后端运行说明初始化 MySQL 并启动 Spring Boot 服务，再启动前端：

```powershell
mysql -u root -p -e "source docs/init.sql"
cd backend
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "你的本地数据库密码"
$env:JWT_SECRET = "至少32字节的自定义密钥"
mvn spring-boot:run
```

```powershell
cd frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm install
pnpm dev
```

启动后学生端访问 `/app`、HR 端访问 `/hr`，生成 AI 匹配后可查看分数、分析文本、匹配优势、匹配缺口和建议动作。

### 如何测试

```powershell
cd backend
mvn test
mvn package
cd ../frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm build
```

本次 `mvn test` 通过，后端共 50 个测试；`mvn package` 通过；`pnpm build` 通过。前端构建过程中出现第三方依赖注释和 chunk 体积警告，不影响构建结果。

### 下一天该做什么

Day 17：实现简历智能优化建议，根据目标岗位给出简历改进建议，并继续使用本地规则和可降级设计。

## Day 17 任务

### 当天任务边界

- 实现简历智能优化建议，只根据学生本人简历和已发布目标岗位生成改进建议。
- 继续使用本地关键词规则，不接入外部 AI 或模型 API。
- 后端新增学生端简历优化建议接口，返回总体摘要、已覆盖关键词、待补充关键词、内容建议和下一步动作。
- 前端学生端新增简历优化入口和建议结果展示。
- 不实现岗位 JD 智能解析、HR 候选人推荐排序、AI 面试题生成或管理端 AI 运营洞察。

### 状态

已完成

### 完成了什么

- 新增 `POST /api/student/resumes/{id}/optimization`，学生只能基于本人简历生成优化建议。
- 新增本地规则模型标识 `local-resume-optimizer-v1`，根据目标岗位关键词与简历内容生成可执行建议。
- 后端返回总体摘要、已覆盖关键词、待补充关键词、内容建议和下一步动作，不新增持久化表。
- 学生端 `/app` 新增“简历优化”标签页，可选择简历和目标岗位生成建议。
- 未接入外部 AI 或模型 API，未实现岗位 JD 智能解析、HR 推荐排序、面试题生成或管理端 AI 运营洞察。

### 修改了哪些文件

- `backend/src/main/java/com/example/aijobs/resume/ResumeService.java`
- `backend/src/main/java/com/example/aijobs/resume/ResumeController.java`
- `backend/src/main/java/com/example/aijobs/resume/dto/ResumeOptimizationRequest.java`
- `backend/src/main/java/com/example/aijobs/resume/dto/ResumeOptimizationResponse.java`
- `backend/src/test/java/com/example/aijobs/resume/ResumeServiceTests.java`
- `frontend/src/api/student.js`
- `frontend/src/views/StudentDashboardView.vue`
- `frontend/src/styles.css`
- `README.md`
- `docs/DAILY_TASKS.md`

### 如何运行

先按后端运行说明初始化 MySQL 并启动 Spring Boot 服务，再启动前端：

```powershell
mysql -u root -p -e "source docs/init.sql"
cd backend
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "你的本地数据库密码"
$env:JWT_SECRET = "至少32字节的自定义密钥"
mvn spring-boot:run
```

```powershell
cd frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm install
pnpm dev
```

启动后学生端访问 `/app`，进入“简历优化”标签页，选择简历和目标岗位后生成本地规则优化建议。

### 如何测试

```powershell
cd backend
mvn test
mvn package
cd ../frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm build
```

本次 `mvn test` 通过，后端共 53 个测试；`mvn package` 通过；`pnpm build` 通过。前端构建过程中仍出现第三方依赖注释和 chunk 体积警告，不影响构建结果。

### 下一天该做什么

Day 18：实现岗位 JD 智能解析与优化建议，继续使用本地规则和可降级设计。

## Day 18 任务

### 当天任务边界

- 实现岗位 JD 智能解析与优化建议，只基于 HR 本人岗位的标题、描述和要求生成本地规则分析。
- 继续使用本地关键词和文本完整度规则，不接入外部 AI 或模型 API。
- 后端新增 HR 端岗位 JD 分析接口，返回模型标识、JD 摘要、识别出的关键技能、岗位亮点、信息缺口和优化建议。
- 前端 HR 端新增 JD 分析入口和结果展示。
- 不实现 HR 候选人推荐排序、AI 面试题生成或管理端 AI 运营洞察。

### 状态

已完成

### 完成了什么

- 新增 `POST /api/hr/jobs/{id}/jd-analysis`，HR 只能分析本人岗位。
- 新增本地规则模型标识 `local-jd-analyzer-v1`，基于岗位标题、描述和要求生成 JD 摘要、关键技能、岗位亮点、信息缺口和优化建议。
- 后端继续使用本地关键词和文本完整度规则，不接入外部 AI 或模型 API，不新增持久化表。
- HR 端 `/hr` 新增“JD 分析”标签页，可选择本人岗位生成并查看分析结果。
- 未实现 HR 候选人推荐排序、AI 面试题生成或管理端 AI 运营洞察。

### 修改了哪些文件

- `backend/src/main/java/com/example/aijobs/job/JobService.java`
- `backend/src/main/java/com/example/aijobs/job/JobController.java`
- `backend/src/main/java/com/example/aijobs/job/dto/JobJdAnalysisResponse.java`
- `backend/src/test/java/com/example/aijobs/job/JobServiceTests.java`
- `backend/src/test/java/com/example/aijobs/job/JobAuthorizationTests.java`
- `frontend/src/api/hr.js`
- `frontend/src/views/HrDashboardView.vue`
- `README.md`
- `docs/DAILY_TASKS.md`

### 如何运行

先按后端运行说明初始化 MySQL 并启动 Spring Boot 服务，再启动前端：

```powershell
mysql -u root -p -e "source docs/init.sql"
cd backend
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "你的本地数据库密码"
$env:JWT_SECRET = "至少32字节的自定义密钥"
mvn spring-boot:run
```

```powershell
cd frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm install
pnpm dev
```

启动后 HR 端访问 `/hr`，进入“JD 分析”标签页，选择本人岗位后生成本地规则 JD 解析与优化建议。

### 如何测试

```powershell
cd backend
mvn test
mvn package
cd ../frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm build
```

本次 `mvn test` 通过，后端共 56 个测试；`mvn package` 通过；`pnpm build` 通过。前端构建过程中仍出现第三方依赖注释和 chunk 体积警告，不影响构建结果。

### 下一天该做什么

Day 19：实现 HR 候选人推荐排序与风险摘要，继续使用本地规则和可降级设计。

## Day 19 任务

### 当天任务边界

- 实现 HR 候选人推荐排序与风险摘要，只基于 HR 本人岗位收到的投递、简历内容、岗位要求和已有 AI 匹配结果生成。
- 继续使用本地关键词规则和已有匹配结果，不接入外部 AI 或模型 API。
- 后端新增 HR 端候选人推荐接口，返回推荐分、分数来源、已匹配关键词、待核验关键词、推荐理由、风险摘要和建议动作。
- 前端 HR 端新增候选人推荐入口和排序结果展示。
- 不实现 AI 面试题生成、评分维度或管理端 AI 运营洞察。

### 状态

已完成

### 完成了什么

- 新增 `GET /api/hr/applications/recommendations?jobId=`，HR 只能查看本人岗位范围内的候选人推荐。
- 新增本地规则模型标识 `local-candidate-ranker-v1`；如果已有同一简历和岗位的 AI 匹配结果，则优先使用已有匹配分数作为排序依据。
- 推荐结果排除已撤回投递，按推荐分降序返回推荐理由、风险摘要、建议动作、匹配关键词和待核验关键词。
- HR 端 `/hr` 新增“候选人推荐”标签页，可按岗位筛选并查看推荐排序与风险摘要。
- 未接入外部 AI 或模型 API，未实现 AI 面试题生成、评分维度或管理端 AI 运营洞察。

### 修改了哪些文件

- `backend/src/main/java/com/example/aijobs/application/JobApplicationService.java`
- `backend/src/main/java/com/example/aijobs/application/JobApplicationController.java`
- `backend/src/main/java/com/example/aijobs/application/dto/CandidateRecommendationResponse.java`
- `backend/src/test/java/com/example/aijobs/application/JobApplicationServiceTests.java`
- `backend/src/test/java/com/example/aijobs/application/JobApplicationAuthorizationTests.java`
- `frontend/src/api/hr.js`
- `frontend/src/views/HrDashboardView.vue`
- `frontend/src/styles.css`
- `README.md`
- `docs/DAILY_TASKS.md`

### 如何运行

先按后端运行说明初始化 MySQL 并启动 Spring Boot 服务，再启动前端：

```powershell
mysql -u root -p -e "source docs/init.sql"
cd backend
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "你的本地数据库密码"
$env:JWT_SECRET = "至少32字节的自定义密钥"
mvn spring-boot:run
```

```powershell
cd frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm install
pnpm dev
```

启动后 HR 端访问 `/hr`，进入“候选人推荐”标签页，选择本人岗位后查看本地规则推荐排序与风险摘要。

### 如何测试

```powershell
cd backend
mvn test
mvn package
cd ../frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm build
```

本次 `mvn test` 通过，后端共 60 个测试；`mvn package` 通过；`pnpm build` 通过。前端构建过程中仍出现第三方依赖注释和 chunk 体积警告，不影响构建结果。

### 下一天该做什么

Day 20：实现 AI 面试题生成与评分维度，继续使用本地规则和可降级设计。

## Day 20 任务

### 当天任务边界

- 实现 AI 面试题生成与评分维度，只基于 HR 本人岗位范围内的单个投递、岗位要求和候选人简历生成。
- 继续使用本地关键词和文本完整度规则，不接入外部 AI 或模型 API。
- 后端新增 HR 端面试题生成接口，返回模型标识、面试摘要、结构化面试题、评分维度、关注风险和建议追问。
- 前端 HR 端新增面试题生成入口和结果展示。
- 不实现管理端 AI 运营洞察，不新增面试记录持久化表，不自动变更投递状态。

### 状态
已完成

### 完成了什么

- 新增 `POST /api/hr/applications/{id}/interview-kit`，HR 只能为本人岗位范围内的单个投递生成面试题。
- 新增本地规则模型标识 `local-interview-kit-v1`，根据岗位标题、岗位要求、简历技能和项目经历生成面试摘要、结构化面试题、评分维度、风险关注和建议追问。
- 面试题生成不调用外部 AI 或模型 API，不新增持久化表，不自动变更投递状态；已撤回投递会被拒绝生成。
- HR 端 `/hr` 新增“AI 面试题”页签，可选择投递并查看面试题、评分维度、风险关注和追问建议。

### 修改了哪些文件

- `backend/src/main/java/com/example/aijobs/application/JobApplicationService.java`
- `backend/src/main/java/com/example/aijobs/application/JobApplicationController.java`
- `backend/src/main/java/com/example/aijobs/application/dto/InterviewKitResponse.java`
- `backend/src/test/java/com/example/aijobs/application/JobApplicationServiceTests.java`
- `backend/src/test/java/com/example/aijobs/application/JobApplicationAuthorizationTests.java`
- `frontend/src/api/hr.js`
- `frontend/src/views/HrDashboardView.vue`
- `frontend/src/styles.css`
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

```powershell
cd frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm install
pnpm dev
```

启动后 HR 端访问 `/hr`，进入“AI 面试题”页签，选择本人岗位范围内的投递后生成本地规则面试题。

### 如何测试

```powershell
cd backend
mvn test
mvn package
cd ../frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm build
```

### 下一天该做什么

Day 21：实现管理端 AI 运营洞察，继续使用本地规则和可降级设计，不接入外部 AI 或模型 API。

## Day 21 任务

### 当天任务边界

- 实现管理端 AI 运营洞察，只基于现有用户、岗位、简历、投递和 AI 匹配统计生成本地规则洞察。
- 继续复用现有管理员看板接口和页面，不新增持久化表，不接入外部 AI 或模型 API。
- 后端在管理员看板响应中新增 AI 运营洞察字段，返回模型标识、覆盖率摘要、重点关注项、风险提醒和建议动作。
- 前端管理员端 `/admin` 新增 AI 运营洞察展示区，帮助管理员识别匹配覆盖、低分匹配和投递流转风险。
- 不实现新的候选人推荐、面试题、简历优化、JD 分析或跨天功能。

### 状态

已完成

### 完成了什么

- 在管理员看板响应中新增 AI 运营洞察字段，模型标识为 `local-admin-ai-ops-v1`。
- 后端基于现有用户、岗位、简历、投递和 AI 匹配统计，使用本地规则生成匹配覆盖率、低分匹配数量、运营健康摘要、重点关注项、风险提醒和建议动作。
- 管理端 `/admin` 新增“AI 运营洞察”展示区，管理员可直接查看覆盖率、低分匹配、风险提醒和建议动作。
- 未接入外部 AI 或模型 API，未新增持久化表，未实现新的候选人推荐、面试题、简历优化或 JD 分析能力。

### 修改了哪些文件

- `backend/src/main/java/com/example/aijobs/admin/AdminDashboardService.java`
- `backend/src/main/java/com/example/aijobs/admin/dto/AdminDashboardResponse.java`
- `backend/src/main/java/com/example/aijobs/admin/dto/AdminAiOperationInsight.java`
- `backend/src/test/java/com/example/aijobs/admin/AdminDashboardServiceTests.java`
- `frontend/src/views/AdminDashboardView.vue`
- `frontend/src/styles.css`
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

```powershell
cd frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm install
pnpm dev
```

启动后管理员端访问 `/admin`，查看平台统计图表和本地规则 AI 运营洞察。

### 如何测试

```powershell
cd backend
mvn test
mvn package
cd ../frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm build
```

本次 `mvn test` 通过，后端共 65 个测试；`mvn package` 通过；`pnpm build` 通过。前端构建过程中仍出现第三方依赖注释和 chunk 体积警告，不影响构建结果。

### 下一天该做什么

Day 22：根据实际运营数据继续规划下一个 AI 能力升级小模块，保持本地规则、可测试和可降级设计。

## Day 22 任务

### 当天任务边界

- 实现 HR 投递跟进建议，只针对 HR 本人岗位范围内的单个已有投递生成下一步处理建议。
- 继续使用本地关键词、投递状态和已有 AI 匹配分规则，不接入外部 AI 或模型 API。
- 后端新增 HR 端投递跟进建议接口，返回模型标识、匹配分来源、跟进优先级、建议状态、风险提醒、建议动作和沟通提示。
- 前端 HR 端新增“跟进建议”页签，可选择投递生成并查看建议。
- 不新增持久化表，不自动修改投递状态，不实现新的候选人推荐、面试题、简历优化、JD 分析或管理端洞察能力。

### 状态

已完成

### 完成了什么

- 新增 `POST /api/hr/applications/{id}/follow-up-advice`，HR 只能为本人岗位范围内的投递生成跟进建议。
- 新增本地规则模型标识 `local-application-follow-up-v1`，基于投递状态、岗位要求、简历内容和已有 AI 匹配分生成优先级与建议状态。
- 跟进建议返回匹配分来源、已匹配关键词、待核验关键词、风险提醒、建议动作和沟通提示，不新增持久化表且不自动修改投递状态。
- HR 端 `/hr` 新增“跟进建议”页签，可选择投递并查看本地规则建议。
- 未接入外部 AI 或模型 API，未实现新的候选人推荐、面试题、简历优化、JD 分析或管理端洞察能力。

### 修改了哪些文件

- `backend/src/main/java/com/example/aijobs/application/JobApplicationService.java`
- `backend/src/main/java/com/example/aijobs/application/JobApplicationController.java`
- `backend/src/main/java/com/example/aijobs/application/dto/ApplicationFollowUpAdviceResponse.java`
- `backend/src/test/java/com/example/aijobs/application/JobApplicationServiceTests.java`
- `frontend/src/api/hr.js`
- `frontend/src/views/HrDashboardView.vue`
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

```powershell
cd frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm install
pnpm dev
```

启动后 HR 端访问 `/hr`，进入“跟进建议”页签，选择本人岗位范围内的投递后生成本地规则跟进建议。

### 如何测试

```powershell
cd backend
mvn test
mvn package
cd ../frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm build
```

本次 `mvn test` 通过，后端共 67 个测试；`mvn package` 通过；`pnpm build` 通过。前端构建过程中仍出现第三方依赖注释和 chunk 体积警告，不影响构建结果。

### 下一天该做什么

Day 23：继续规划下一个 AI 能力升级小模块，保持本地规则、可测试和可降级设计。

## Day 23 任务

### 当天任务边界

- 实现学生端 AI 求职行动计划，只针对学生本人单个已有投递生成下一步求职动作建议。
- 继续使用本地关键词、投递状态、岗位要求、简历内容和已有 AI 匹配分规则，不接入外部 AI 或模型 API。
- 后端新增学生端投递行动计划接口，返回模型标识、匹配分来源、行动优先级、状态摘要、准备清单、风险提醒和下一步动作。
- 前端学生端新增“行动计划”页签，可选择本人投递生成并查看建议。
- 不新增持久化表，不自动修改投递状态，不实现新的 HR 推荐、面试题、JD 分析、简历优化或管理端洞察能力。

### 状态

已完成

### 完成了什么

- 新增 `POST /api/student/applications/{id}/action-plan`，学生只能为本人单个投递生成求职行动计划。
- 新增本地规则模型标识 `local-student-action-plan-v1`，基于投递状态、岗位要求、简历内容和已有 AI 匹配分生成行动优先级与状态摘要。
- 行动计划返回匹配分来源、准备清单、风险提醒和下一步动作，不新增持久化表且不自动修改投递状态。
- 学生端 `/app` 新增“行动计划”页签，可选择本人投递并查看本地规则建议。
- 未接入外部 AI 或模型 API，未实现新的 HR 推荐、面试题、JD 分析、简历优化或管理端洞察能力。

### 修改了哪些文件

- `backend/src/main/java/com/example/aijobs/application/JobApplicationService.java`
- `backend/src/main/java/com/example/aijobs/application/JobApplicationController.java`
- `backend/src/main/java/com/example/aijobs/application/dto/StudentApplicationActionPlanResponse.java`
- `backend/src/test/java/com/example/aijobs/application/JobApplicationServiceTests.java`
- `backend/src/test/java/com/example/aijobs/application/JobApplicationAuthorizationTests.java`
- `frontend/src/api/student.js`
- `frontend/src/views/StudentDashboardView.vue`
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

```powershell
cd frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm install
pnpm dev
```

启动后学生端访问 `/app`，进入“行动计划”页签，选择本人投递后生成本地规则求职行动计划。

### 如何测试

```powershell
cd backend
mvn test
mvn package
cd ../frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm build
```

本次 `mvn test` 通过，后端共 71 个测试；`mvn package` 通过；`pnpm build` 通过。前端构建过程中仍出现第三方依赖注释和 chunk 体积警告，不影响构建结果。

### 下一天该做什么

Day 24：继续规划下一个 AI 能力升级小模块，保持本地规则、可测试和可降级设计。

## Day 24 任务

### 当天任务边界

- 实现 HR 候选人沟通话术草稿，只针对 HR 本人岗位范围内的单个已有投递生成。
- 继续使用本地关键词、投递状态、岗位要求、简历内容和已有 AI 匹配分规则，不接入外部 AI 或模型 API。
- 后端新增 HR 端候选人沟通草稿接口，返回模型标识、匹配分来源、沟通场景、主题、开场说明、关键追问、风险提示和后续动作。
- 前端 HR 端新增“沟通草稿”页签，可选择投递生成并查看本地规则话术。
- 不新增持久化表，不自动修改投递状态，不实现新的学生端行动计划、面试题、JD 分析、简历优化或管理端洞察能力。

### 状态

已完成

### 完成了什么

- 新增 `POST /api/hr/applications/{id}/communication-draft`，HR 只能为本人岗位范围内的单个投递生成候选人沟通话术草稿。
- 新增本地规则模型标识 `local-candidate-communication-draft-v1`，基于投递状态、岗位要求、简历内容和已有 AI 匹配分生成沟通场景、主题、开场说明、关键追问、风险提示和后续动作。
- 沟通话术不调用外部 AI 或模型 API，不新增持久化表，不自动修改投递状态。
- HR 端 `/hr` 新增“沟通草稿”页签，可选择投递生成并查看本地规则话术。
- 未实现新的学生端行动计划、面试题、JD 分析、简历优化或管理端洞察能力。

### 修改了哪些文件

- `backend/src/main/java/com/example/aijobs/application/JobApplicationService.java`
- `backend/src/main/java/com/example/aijobs/application/JobApplicationController.java`
- `backend/src/main/java/com/example/aijobs/application/dto/CandidateCommunicationDraftResponse.java`
- `backend/src/test/java/com/example/aijobs/application/JobApplicationServiceTests.java`
- `backend/src/test/java/com/example/aijobs/application/JobApplicationAuthorizationTests.java`
- `frontend/src/api/hr.js`
- `frontend/src/views/HrDashboardView.vue`
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

```powershell
cd frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm install
pnpm dev
```

启动后 HR 端访问 `/hr`，进入“沟通草稿”页签，选择本人岗位范围内的投递后生成本地规则候选人沟通话术。

### 如何测试

```powershell
cd backend
mvn test
mvn package
cd ../frontend
$env:Path = "C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\node\bin;C:\Users\HP\.cache\codex-runtimes\codex-primary-runtime\dependencies\bin;" + $env:Path
pnpm build
```

本次 `mvn test` 通过，后端共 75 个测试；`mvn package` 通过；`pnpm build` 通过。前端构建过程中仍出现第三方依赖注释和 chunk 体积警告，不影响构建结果。

### 下一天该做什么

Day 25：实现技能归一化与岗位所需技能覆盖率，建立后续 DeepSeek 辅助解释的独立规则基线。

## Day 25 任务（2026-09-30）

### 当天任务边界

- 以版本化技能词典归一化中英文别名、大小写、全角字符和重复技能，覆盖开发技能与助教常见技能。
- 只从岗位 requirements 字段识别所需技能；按“简历识别到的所需技能数 / 岗位识别到的所需技能总数 × 100”计算分数。
- 已匹配技能、缺口和分子/分母通过现有匹配响应及页面展示；未识别到岗位技能时明确拒绝评分，不伪造零分或满分。
- 保留学生简历归属、HR 岗位归属和投递权限校验；不新增数据库表，不自动变更投递状态。
- 更新测试、接口说明和长期验收路线；CV 文件提取、DeepSeek 调用、对话历史和对照评估留到后续天数。

### 状态

已完成

### 完成内容与验证

- 新增 29 种技能的 `SkillNormalizer`（`skill-dictionary-v1`），支持中英文别名、大小写/全角/空白归一化、重复项去重与英文词边界。
- 匹配算法升级为 `local-skill-coverage-v1`，分母仅来自岗位 requirements；例：English + lesson planning / 三种要求 = 66.67 分。
- 沿用现有响应字段展示分子/分母、全部已匹配技能、全部缺口和人工复核说明。没有可识别的岗位要求时返回 422，不保存或覆盖结果。
- 历史匹配记录保留旧版本，重新生成才升级；原有学生归属和 HR 投递权限逻辑保留。
- `mvn test`：98 项通过，0 失败、0 错误、0 跳过；`mvn package -DskipTests` 打包通过；`pnpm build` 通过（保留第三方注释和 chunk 体积警告）。
- 内置浏览器使用 loopback 地址与独立模拟 API，验证学生生成匹配及 HR 结果展示；确认 66.67 分、公式、技能缺口和人工复核说明。HR 原有薪资表单存在 Number 类型警告；未做 MySQL 端到端或真实 LLM 调用。
- 更新 README、路线图与本计划，将 CV 提取、DeepSeek、角色对话、人工复核与受控比较按后续天数安排。

### 修改文件

- 后端：`match/SkillNormalizer.java`、`match/AiMatchService.java`。
- 测试：`SkillNormalizerTests`、`AiMatchServiceTests`、`AiMatchAuthorizationTests`。
- 前端：`StudentDashboardView.vue`、`HrDashboardView.vue` 的匹配说明。
- 文档：`README.md`、`docs/DAILY_TASKS.md`、`docs/ROADMAP.md`。

### 运行与复核

沿用 README 中的 MySQL/JWT/Redis 配置与后端、前端启动方式，无需新增数据库迁移。学生或 HR 在“AI 匹配”页重新生成结果；接口路径与请求字段保持不变。完整词典、评分边界、历史兼容与回滚说明见 README 的“AI 匹配接口”。

下一天：Day 26，CV 文件文本提取与学生导入预览。

### 后续迭代顺序（仅规划，未实现）

| 天数 | 单日模块 | 验收重点 |
| --- | --- | --- |
| Day 26 | CV 文件文本提取与学生导入预览 | 限定格式/大小、异常文件、本人权限、提取文本可核对且经确认后保存 |
| Day 27 | DeepSeek Chat API 适配器 | 配置密钥、真实请求结构、超时/异常处理、禁用时降级，生成文本不能写入匹配分 |
| Day 28 | 角色与招聘上下文对话服务 | 服务端角色提示、本人/所属岗位上下文、会话隔离、历史长度限制、最小必要数据 |
| Day 29 | 前端助手与人工复核流程 | 学生/HR 对话入口、历史展示、规则分与生成解释区分、招聘决定仍由人工确认 |
| Day 30 | 系统验证与受控比较 | 固定简历/岗位与规则分，对比仅规则和规则加 LLM；记录解释正确性、幻觉、时延、成本及失败案例 |

具体当日边界在执行前细化。分工、接口协调、Git 合并和团队测试须记录真实参与者与实际过程，不补造历史经历。

## Day 26 任务（2026-10-01）

### 当天任务边界

- 学生本人上传文本型 PDF 或 UTF-8 TXT，返回可编辑文本预览；原始文件不持久化，不调用外部服务。
- 单文件最多 2 MiB，PDF 最多 20 页，提取文本最多 20,000 字符；拒绝空文件、损坏/加密 PDF、无可提取文字的扫描件和不支持的格式。
- 用户确认后通过既有学生简历接口创建本人 DRAFT；新增可选 sourceText 字段保存确认后的文本，提供非破坏性数据库升级脚本，并接入已有技能匹配。
- 前端提供导入、预览编辑、取消和确认创建流程；保留现有简历编辑与权限隔离。
- 更新文档、验证文件解析/接口权限/匹配和页面流程，提交并 push、创建 PR。Day 27 DeepSeek 及后续能力不在本次实现。

### 状态

已完成

### 完成内容与验证

- 新增 `POST /api/student/resumes/import-preview`，只允许 STUDENT 上传 PDF/TXT，返回可编辑文本及必要的 PDF 核对提示；预览服务无 Mapper、无持久化写入。
- 引入 PDFBox 3.0.8，校验大小、页数、文本长度、文件头、UTF-8 编码、空文件、加密及无文字 PDF；统一返回 400/413/415/422。
- 新增可选 `sourceText` DTO/实体字段及 `resume.source_text`；初始化脚本同步更新，另提供可重复运行且保留旧数据的增量升级脚本。
- 前端“我的简历”新增上传、预览修改、取消和确认草稿，旧编辑表单支持保存/清空导入原文。确认身份来自登录用户，状态固定 DRAFT。
- 已确认原文参与 Day 25 技能覆盖率并去重。无 OCR/DOCX/外部 API，没有实现 Day 27 功能。
- `mvn test`：117 项通过，0 失败/错误/跳过；`mvn package -DskipTests` 与 `pnpm build` 通过，前端保留既有构建警告。
- 桌面和 390×844 内置浏览器验证：TXT 取消不产生记录、真实两页 PDF 提取、修改后创建草稿、再编辑原文一致、发布后 66.67 分；损坏文件提示和预览清理正确，无运行错误。额外真实 HTTP 超限上传返回统一 413。
- 联调使用真实 Spring Boot 控制器/服务、JWT 和合成文件；数据库 Mapper 与缓存为内存替身。未执行实际 MySQL 迁移或真实数据库端到端测试，未发送真实简历或调用外部 AI。

### 修改文件与运行方式

- 后端：`resume/ResumeImportService`、`ResumeImportController`、`dto/ResumeImportPreviewResponse`，简历 DTO/实体/服务、`AiMatchService`、统一异常处理、Maven 与上传配置。
- 前端：`ResumeImportPanel.vue`、`StudentDashboardView.vue`、`api/student.js`。
- 测试：文件提取、简历权限/保存、匹配测试；文档：README、路线图、本计划、init.sql 及 `docs/migrations/day26-resume-source-text.sql`。
- 现有数据库先备份，在正确数据库运行增量脚本后再启动新后端；不得用 init.sql 升级已有数据。前后端运行命令沿用 README。回滚代码时保留 source_text 列与用户文本。

### 下一天

Day 27：DeepSeek Chat API 适配器及配置开关、超时/异常降级；尚未实现。

## Day 26 补充任务（2026-10-01）：界面美化与外部数据可行性

### 当天任务边界

根据用户新增请求，美化现有招聘系统界面；核查外部岗位数据接入方式，优先 BOSS直聘、智联招聘和前程无忧。本次只调整现有界面的样式、展示与文案并编写接入方案，不实现 Day 27–30 功能或外部同步模块。

### 状态

已完成

### 完成内容与验证

- 统一账号页和学生、HR、管理员工作台的青绿色主题、平台页头、面板、表单和手机布局；替换账号页早期开发说明。
- 岗位浏览突出薪资、中文化用工类型、完善选中状态和键盘焦点、增加筛选标签；保留真实数据、所有原有选项卡及业务操作。
- 修正卡片状态标签压缩显示，统一管理员图表配色；复用页头与账号介绍组件。
- `pnpm --dir frontend build` 通过，保留既有依赖注释和 chunk 体积提示；未改后端，未重复后端测试。
- 内置浏览器核查桌面/手机视口；隔离模拟 API 验证登录、筛选、详情选择、投递反馈、简历入口、HR/管理员布局、账号导航。未做真实 MySQL 或外部平台端到端联调。
- 保留参考稿、截图与详细验收边界于 `docs/ui/VERIFICATION.md`。临时预览服务和模拟代码交付前清理。
- 新增 `docs/EXTERNAL_JOB_DATA.md`：国内平台尚无本次可确认的直接可用全站岗位读取接口；记录官方授权、来源链接、文件导入、字段清理、去重、更新与原站申请的后续方案。没有新增外部岗位接口或占位 UI。

### 修改文件与运行方式

- 前端：`App.vue`、`main.js`、`theme.css`、`PlatformBrand.vue`、`AuthIntro.vue`、登录/注册/三类工作台视图。
- 文档：README、本计划、外部数据方案及界面验收/截图。
- 沿用 README 的前后端运行方式，无需新增数据库升级。回滚本次提交即可恢复旧界面。

### 下一步

按既有 Day 27–30 计划继续。外部岗位数据单独安排后续任务，先确定授权数据或实际平台接口，不能提前把方案写成已实现功能。

## Day 26 新增任务（2026-10-01）：接入可用外部岗位数据

### 当天任务边界

根据用户明确要求“查找可接入的平台数据并接入”，本次接入一个已核实可用的公开岗位源 Jobicy。增加服务端真实读取、缓存、筛选分页和异常处理，以及无需登录的外部岗位浏览页和学生端入口。保留来源与原站申请链接，不把外部岗位写入本平台 HR/投递/匹配记录，不实现 Day 27–30 能力。

### 状态

已完成

### 完成内容与验证

- 核查 Jobicy 官方接口、字段和展示使用条件，接入固定公开岗位源，无需 API Key。国内三大平台仍未找到本次可以确认的直接可用岗位读取接口，没有冒称已接入。
- 后端新增公开只读查询、纯文本描述、HTTPS 来源链接校验、币种/周期与时区处理、去重、共享快照筛选分页；按需每小时最多一次同步尝试，并发查询共用一次读取。失败保留不超过 24 小时的旧数据并标识状态，过旧返回统一 503。
- 前端新增 `/external-jobs` 匿名页面、登录/注册页入口和学生端选项卡；支持筛选、分页、详情、重试/空结果及同步状态。原站申请按钮不生成本平台投递，长描述可滚动并支持键盘焦点。
- 当前只读取最新最多 200 条，没有实现游标遍历；明确最近 7 天窗口、约 3 小时延迟、海外英文远程岗位及地区限制。缓存按进程生效，重启/多实例限制已记录。
- `mvn -f backend/pom.xml test`：147 项通过，0 失败/错误/跳过（新增 30 项）；`mvn -f backend/pom.xml package -DskipTests`、`pnpm --dir frontend build` 通过，保留前端既有依赖和体积提示。
- 使用完整 Spring Boot 应用、Vite 和真实 Jobicy 响应联调，实际读取 200 条；“Engineer + Poland”返回 4 条，分页、岗位详情切换、无匹配提示、公开入口和学生选项卡正确。匿名请求使用独立客户端，未向来源传送 JWT 或简历。
- 内置浏览器验证桌面 1280×720、手机 390×844；无页面空白/框架覆盖层/相关控制台错误、无横向溢出；手机选择岗位滚动到详情。未测试外站申请提交或真实账号/数据库业务；外部查询本身不依赖业务数据库。

### 修改文件与运行方式

- 后端：`externaljobs/` 客户端、服务、控制器和响应记录，公开 GET 权限、开关配置、Jsoup 依赖及解析/服务/HTTP 测试。
- 前端：`ExternalJobsPanel.vue`、`ExternalJobsView.vue`、`api/externalJobs.js`、路由、账号页入口和学生选项卡。
- 文档：README、本计划和 `EXTERNAL_JOB_DATA.md`。前后端沿用 README 运行命令；无需本次数据库升级。默认启用，可通过 `EXTERNAL_JOBS_ENABLED=false` 禁用；回滚本次提交即可移除接入模块。
- 协作：主任务负责后端实现、接口协调、真实联调和提交；并行子任务分别核查公开来源、实现前端、补充测试与只读审查。未补造历史开发或人工参与记录。

## Day 27 任务（2026-10-02）

### 当天任务边界

- 新增独立的 DeepSeek Chat Completions 后端适配器，支持启用开关、环境变量密钥、可配置模型和连接/整个请求超时。
- 按执行日官方接口构造非流式文本请求；旧 deepseek-chat 已退役，默认使用 deepseek-flash，禁用思考模式，限制输出 token。
- 禁用、缺少密钥、超时、HTTP 错误、网络异常或无效响应时返回明确降级状态，不暴露密钥、原始错误响应或请求文本。
- 只接受内部消息并返回辅助文本；不读取业务数据库，不修改匹配分或投递状态。对外对话接口、招聘上下文、会话归属/历史和前端助手留到 Day 28–29。
- 使用合成消息与本地模拟上游验证请求契约、异常降级及超时；运行后端测试/打包与前端构建，更新文档、提交、push 并创建以此前成果为基线的 PR。
- 缺少凭证时不调用真实模型，明确记录真实调用未验证及所需输入。

### 状态

已完成：2026-10-02 完成实现与独立后端测试；2026-10-03 补齐标准 Maven 测试、后端打包、前端构建、Git 提交、push 与 PR 收尾。

### 完成内容

- 接续执行前已有的 Day 27 工作分支与未提交改动，保留 CV、界面及 Jobicy 迭代成果；没有重置工作区或从旧 Day 25 分支重做。
- 新增独立 `DeepSeekChatClient`、消息/结果记录、环境变量属性和 Spring Bean；生产地址固定，默认关闭，支持配置模型、连接/完整请求截止时间和输出 token 上限。
- 非流式 JSON 请求关闭思考模式，不使用 tools、不跟随重定向、不自动重试。明确区分禁用、缺少密钥、超时、上游错误、网络失败、异常响应和中断，降级不返回敏感请求/错误体。
- 限制消息总文本 24,000 个 UTF-16 字符单位与响应体 1 MiB；保存有效输出的结束原因、实际模型和 token 用量。补齐尾随 JSON 与不一致用量的拒绝检查。
- 无业务数据库读写、无评分或投递状态修改、无对外对话接口；服务端角色/历史/招聘上下文与页面入口仍留给 Day 28–29。
- 新增明确选择后才能执行的 `DeepSeekLiveIT`，仅使用固定合成助教问题；本次没有执行真实模型调用，没有发送任何真实个人简历。

### 验证结果与限制

- 独立编译源码后通过 JUnit Platform 执行全部 `*Tests`：178 项通过，失败/错误/跳过为 0。新增 31 项：28 项客户端契约/异常检查与 3 项配置检查。
- 客户端模拟上游绑定 `127.0.0.1` 随机端口，验证真实 HTTP 请求、完整响应超时（含保活空白）、响应限额、HTTP 301/401/429/500/503、不跟随重定向、无自动重试及线程中断。
- 2026-10-03 收尾复核：`mvn -f backend/pom.xml test` 通过 178 项，失败/错误/跳过为 0；`mvn -f backend/pom.xml package -DskipTests` 通过并生成 Spring Boot jar；`pnpm --dir frontend build` 通过，保留既有第三方注释和 chunk 体积警告。
- 未执行真实 MySQL 迁移、数据库端到端、DeepSeek 真实 API 或规则/LLM 增益评估。当前已存在密钥环境配置也不等于真实 API 可用或验证通过；仍需网络访问与显式真实测试选择。
- 2026-10-03 已能访问 origin 并确认 `King/jobicy-external-jobs` 远程起点为 `f36c291`；Day 27 分支、提交、远程 SHA 和 PR 以实际 Git 操作结果为准，详见 [当天记录](verification/DAY27_2026-10-02.md)。

### 修改文件与运行方式

- 后端：`backend/src/main/java/com/example/aijobs/deepseek/` 的 5 个类与 `backend/src/main/resources/application.yml`。
- 测试：`DeepSeekChatClientTests`、`DeepSeekConfigurationTests`、默认不运行的 `DeepSeekLiveIT`。
- 文档：README、本计划、ROADMAP、`docs/DEEPSEEK_ADAPTER.md` 与当天验证记录。
- 沿用 README 的后端运行方式，无数据库升级或新增 Maven 依赖。所有开关、环境变量、请求/降级契约、复核命令和回滚范围见适配器说明。

### Git 交付与下一步

当前工作分支为 `King/day-27-deepseek-adapter`，起点和 PR 基线为已推送的 `King/jobicy-external-jobs` / `f36c291`。没有自动合并或强制推送。

2026-10-03 已补齐本模块的标准构建、提交、显式 push、本地/远程 SHA 核对和 PR 创建；Day 27 到此完成。下一次可推进 Day 28 的服务端角色、招聘上下文、会话归属及历史限制。

## Day 28 任务（2026-10-04）

### 当天任务边界

- 新增服务端招聘助手对话接口，仅面向已认证学生和 HR；不实现前端入口、页面历史展示或人工复核按钮。
- 服务端固定角色提示，不允许客户端传入 system prompt；限制会话归属、角色、历史长度和可读招聘数据范围。
- 学生只能读取本人投递/简历上下文和公开岗位；HR 只能读取本人岗位及其投递候选人上下文。
- 只把规则匹配分作为只读上下文传给模型；生成文本不能修改分数、简历、岗位、投递状态，不能自动录用或拒绝候选人。
- DeepSeek 默认关闭、缺少密钥或异常时返回明确降级说明；本次只用合成/模拟数据测试，不调用真实模型或发送真实个人简历。
- 更新 README、路线图、当日记录和验证记录；运行后端测试/打包与前端构建，提交、push 并创建以 Day 27 为基线的 PR。

### 状态

已完成：2026-10-04 完成实现、标准测试、后端打包和前端构建；Git 交付以实际提交、push 和 PR 结果为准。

### 完成内容

- 新增 `POST /api/student/assistant/chat` 和 `POST /api/hr/assistant/chat`，分别由 STUDENT、HR 角色访问。
- 新增进程内会话管理：新请求创建 `conversationId`，续聊必须匹配同一用户和同一角色；默认带入最近 8 条历史，服务端上限 20 条。
- 服务端拼接系统提示与授权上下文，明确上下文和用户消息均为不可信数据，要求模型忽略指令注入、不得泄露密钥或隐私。
- 学生端上下文支持本人 `applicationId`，或本人 `resumeId` 与公开 `jobId`；HR 端上下文支持本人 `jobId` 或本人岗位范围内 `applicationId`。
- 返回 `generated`、`modelStatus`、`contextSummary`、`historyMessageCount`、`readOnlyMatchScore`、`matchScoreSource`、`humanReviewRequired` 和 `warnings`，明确人工复核和只读分数边界。
- DeepSeek 未生成时返回降级说明；不新增数据库表，不修改既有业务实体或状态。

### 验证结果与限制

- `mvn -f backend/pom.xml test`：185 项通过，失败/错误/跳过均为 0；新增 7 项招聘助手服务测试。
- 新增测试覆盖：学生本人投递上下文、HR 本人岗位候选人上下文、匹配分只读传递、模型禁用降级、同会话历史带入、跨用户/跨角色续聊拒绝、越权简历/投递上下文拒绝和空消息拒绝。
- `mvn -f backend/pom.xml package -DskipTests`：通过并生成 Spring Boot jar。
- `pnpm --dir frontend build`：通过，保留既有第三方注释和 chunk 体积警告；本次没有前端页面改动。
- 未执行真实 MySQL 迁移或数据库端到端；本模块没有新增表或迁移。
- 未调用真实 DeepSeek API；默认关闭，未发送真实个人简历或真实招聘数据。
- 未做浏览器交互验证；Day 28 没有前端入口，页面和人工复核流程留到 Day 29。

### 修改文件与运行方式

- 后端：`backend/src/main/java/com/example/aijobs/assistant/` 的接口、请求/响应 DTO、角色枚举和服务。
- 安全配置：`backend/src/main/java/com/example/aijobs/auth/SecurityConfiguration.java` 增加学生/HR 助手路径角色保护。
- 配置：`backend/src/main/resources/application.yml` 增加 `app.assistant.max-history-messages` / `ASSISTANT_MAX_HISTORY_MESSAGES`。
- 测试：`backend/src/test/java/com/example/aijobs/assistant/RecruitmentChatServiceTests.java`。
- 文档：README、ROADMAP、本计划和 `docs/verification/DAY28_2026-10-04.md`。
- 前后端运行方式沿用 README；上线真实模型前必须通过环境变量显式启用 DeepSeek 并使用合成数据单独验证。

### Git 交付与下一步

当前工作分支为 `King/day-28-chat-context`，基线为 Day 27 提交 `1f4f35b3973c5d0de4156325caecba139965bcd8`。下一模块为 Day 29：前端助手入口、历史展示和人工复核流程；不能提前做 Day 30 的受控比较。
