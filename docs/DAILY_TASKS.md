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
| Day 6 | 实现岗位模块后端接口 | 未开始 |
| Day 7 | 实现简历模块后端接口 | 未开始 |
| Day 8 | 实现投递模块后端接口 | 未开始 |
| Day 9 | 实现 AI 匹配模块 | 未开始 |
| Day 10 | 接入 Redis 缓存 | 未开始 |
| Day 11 | 搭建 Vue3 前端基础框架 | 未开始 |
| Day 12 | 实现登录、注册、路由守卫 | 未开始 |
| Day 13 | 实现学生端页面 | 未开始 |
| Day 14 | 实现 HR 端页面 | 未开始 |
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
