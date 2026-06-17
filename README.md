# AI 智能求职招聘平台

这是一个长期迭代的前后端分离项目，目标是构建面向学生、HR、管理员三类角色的 AI 智能求职招聘平台。

## 当前进度

- 当前阶段：Day 2
- 今日目标：搭建 Spring Boot 3 后端基础框架，保证后端项目可以构建
- 当前状态：已完成后端 Maven 工程、Spring Boot 启动类、基础配置和最小测试，不包含注册登录、业务接口或数据库功能

## 技术栈规划

### 后端

- Spring Boot 3
- Maven
- MyBatis-Plus
- MySQL 8
- Redis
- JWT

### 前端

- Vue 3
- Vite
- Element Plus
- Axios
- Pinia
- Vue Router
- ECharts

## 项目结构

```text
backend/
  pom.xml
  src/main/java/com/example/aijobs/AiJobPlatformApplication.java
  src/main/resources/application.yml
  src/test/java/com/example/aijobs/AiJobPlatformApplicationTests.java
frontend/
docs/
  DAILY_TASKS.md
  ROADMAP.md
  init.sql
AGENTS.md
README.md
```

## 核心模块

1. 用户注册登录
2. JWT 权限认证
3. 学生简历管理
4. 岗位列表和岗位详情
5. 学生投递岗位
6. 防止重复投递
7. HR 发布岗位
8. HR 查看投递列表
9. HR 修改投递状态
10. AI 简历岗位匹配
11. Redis 缓存热门岗位和岗位详情
12. 管理员用户管理
13. 管理员数据统计看板
14. 前端页面对接
15. 项目部署文档

## 如何运行

后端基础框架位于 `backend/`，可使用 Maven 启动：

```powershell
cd backend
mvn spring-boot:run
```

启动后默认监听 `http://localhost:8080`。Day 2 尚未实现业务接口，因此当前只验证应用可以启动和构建。

## 如何测试

运行后端当前阶段可用测试：

```powershell
cd backend
mvn test
```

也可以只执行打包构建：

```powershell
cd backend
mvn package
```

## 本次修改文件

- `backend/pom.xml`
- `backend/src/main/java/com/example/aijobs/AiJobPlatformApplication.java`
- `backend/src/main/resources/application.yml`
- `backend/src/test/java/com/example/aijobs/AiJobPlatformApplicationTests.java`
- `.gitignore`
- `README.md`
- `docs/DAILY_TASKS.md`

## 下一步

Day 3：设计 MySQL 表结构并更新 `docs/init.sql`，不提前实现业务接口。
