# AI 智能求职招聘平台

这是一个长期迭代的前后端分离项目，目标是构建面向学生、HR、管理员三类角色的 AI 智能求职招聘平台。

## 当前进度

- 当前阶段：Day 3
- 今日目标：设计 MySQL 8 核心表结构和初始化脚本
- 当前状态：已完成用户、角色、简历、岗位、投递和 AI 匹配结果的数据结构设计；尚未接入数据库或实现业务接口

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

启动后默认监听 `http://localhost:8080`。Day 3 尚未接入数据库或实现业务接口，因此当前只验证应用可以启动和构建。

使用 MySQL 8 初始化数据库：

```powershell
mysql -u root -p -e "source docs/init.sql"
```

脚本将创建 `ai_job_platform` 数据库、7 张核心表、外键和查询索引，并初始化学生、HR、管理员三个角色。重复执行脚本会重建这些表，请勿直接用于保留业务数据的环境。

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

- `docs/init.sql`
- `README.md`
- `docs/DAILY_TASKS.md`

## 下一步

Day 4：实现用户注册、登录和 JWT，不提前实现角色权限控制或其他业务模块。
