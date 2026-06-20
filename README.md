# AI 智能求职招聘平台

这是一个长期迭代的前后端分离项目，目标是构建面向学生、HR、管理员三类角色的 AI 智能求职招聘平台。

## 当前进度

- 当前阶段：Day 5
- 今日目标：实现 JWT 请求认证和基于角色的权限控制
- 当前状态：已完成注册登录、Bearer JWT 请求过滤、用户状态与角色加载，以及 STUDENT、HR、ADMIN 三类角色的接口权限隔离；尚未实现 Day 6 的岗位模块

## 技术栈

- 后端：Spring Boot 3、Maven、MyBatis-Plus、MySQL 8、JWT
- 前端规划：Vue 3、Vite、Element Plus、Axios、Pinia、Vue Router、ECharts
- 后续规划：Redis

## 项目结构

```text
backend/
  pom.xml
  src/main/java/com/example/aijobs/
    auth/                  注册、登录、JWT、请求认证、角色权限
    common/                统一响应和异常处理
    AiJobPlatformApplication.java
  src/main/resources/application.yml
  src/test/java/com/example/aijobs/
frontend/
docs/
  DAILY_TASKS.md
  ROADMAP.md
  init.sql
AGENTS.md
README.md
```

## 如何运行

先执行 MySQL 8 初始化脚本。脚本会重建其管理的 7 张表，仅适合本地初始化或可重置环境：

```powershell
mysql -u root -p -e "source docs/init.sql"
```

配置数据库密码和 JWT 密钥后启动后端：

```powershell
cd backend
$env:DB_USERNAME = "root"
$env:DB_PASSWORD = "你的本地数据库密码"
$env:JWT_SECRET = "至少32字节的自定义密钥"
mvn spring-boot:run
```

可选环境变量：`DB_URL`、`JWT_EXPIRATION`（默认 `PT2H`）。应用默认监听 `http://localhost:8080`。

## 注册和登录

注册仅接受 `STUDENT` 或 `HR`，不开放管理员自助注册：

```powershell
Invoke-RestMethod http://localhost:8080/api/auth/register `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"username":"student1","password":"StrongPass123","email":"student1@example.com","role":"STUDENT"}'
```

使用用户名或邮箱登录：

```powershell
Invoke-RestMethod http://localhost:8080/api/auth/login `
  -Method Post `
  -ContentType "application/json" `
  -Body '{"account":"student1","password":"StrongPass123"}'
```

登录成功会返回 `Bearer` JWT。除注册和登录外，后端接口默认要求有效令牌；每次请求会重新检查用户是否存在、是否启用及当前数据库角色。

## 权限验证

将登录响应中的 `accessToken` 放入请求头后，可验证对应角色的访问权限：

```powershell
$token = "登录响应中的 accessToken"
Invoke-RestMethod http://localhost:8080/api/access/student `
  -Headers @{ Authorization = "Bearer $token" }
```

- `GET /api/access/student`：仅 `STUDENT`
- `GET /api/access/hr`：仅 `HR`
- `GET /api/access/admin`：仅 `ADMIN`

缺少或无效令牌返回 `401`，角色不匹配返回 `403`，响应均使用统一 JSON 格式。

## 如何测试

```powershell
cd backend
mvn test
mvn package
```

当前共有 10 项测试，覆盖应用入口、注册登录、JWT 签发解析、请求认证、匿名访问拒绝、同角色放行和跨角色拒绝。

## 本次修改文件

- `backend/pom.xml`
- `backend/src/main/java/com/example/aijobs/auth/`
- `backend/src/test/java/com/example/aijobs/auth/`
- `README.md`
- `docs/DAILY_TASKS.md`

## 下一步

Day 6：实现岗位模块后端接口，不提前实现简历、投递或后续模块。
