# AI 智能求职招聘平台

这是一个长期迭代的前后端分离项目，目标是构建面向学生、HR、管理员三类角色的 AI 智能求职招聘平台。

## 当前进度

- 当前阶段：Day 4
- 今日目标：实现用户注册、登录和 JWT
- 当前状态：已接入 MyBatis-Plus 和 MySQL，支持学生/HR 注册、BCrypt 密码哈希、账号登录及 JWT 签发；尚未实现 Day 5 的接口鉴权和角色权限控制

## 技术栈

- 后端：Spring Boot 3、Maven、MyBatis-Plus、MySQL 8、JWT
- 前端规划：Vue 3、Vite、Element Plus、Axios、Pinia、Vue Router、ECharts
- 后续规划：Redis

## 项目结构

```text
backend/
  pom.xml
  src/main/java/com/example/aijobs/
    auth/                  注册、登录、JWT
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

登录成功会返回 `Bearer` JWT。Day 4 只负责签发和校验令牌，接口鉴权及角色权限控制留到 Day 5。

## 如何测试

```powershell
cd backend
mvn test
mvn package
```

当前共有 5 项测试，覆盖应用入口、注册核心逻辑、登录成功/失败和 JWT 签发校验。

## 本次修改文件

- `backend/pom.xml`
- `backend/src/main/resources/application.yml`
- `backend/src/main/java/com/example/aijobs/auth/`
- `backend/src/main/java/com/example/aijobs/common/`
- `backend/src/test/java/com/example/aijobs/auth/`
- `README.md`
- `docs/DAILY_TASKS.md`

## 下一步

Day 5：实现 JWT 请求认证和基于角色的权限控制，不提前实现岗位、简历或投递模块。
