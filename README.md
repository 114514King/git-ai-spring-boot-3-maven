# AI 智能求职招聘平台

这是一个长期迭代的前后端分离项目，目标是构建面向学生、HR、管理员三类角色的 AI 智能求职招聘平台。

## 当前进度

- 当前阶段：Day 1
- 今日目标：初始化仓库结构、README、ROADMAP、DAILY_TASKS、AGENTS.md
- 当前状态：已完成基础目录和文档初始化，尚未实现业务功能

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

Day 1 只完成仓库结构与文档初始化，暂时没有可启动的后端或前端应用。

从 Day 2 开始，将在 `backend/` 中搭建 Spring Boot 基础框架，并补充后端启动命令。

## 如何测试

Day 1 的验证方式是检查基础文件和目录是否存在：

```powershell
Test-Path README.md
Test-Path AGENTS.md
Test-Path docs/ROADMAP.md
Test-Path docs/DAILY_TASKS.md
Test-Path docs/init.sql
Test-Path backend/.gitkeep
Test-Path frontend/.gitkeep
```

## 下一步

Day 2：搭建 Spring Boot 后端基础框架，并保证后端项目可以构建或启动。
