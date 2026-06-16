# 每日开发计划

## 执行规则

- 每天只完成当天任务。
- 不提前实现后续天数的功能。
- 每次改动必须保持项目能启动或能通过当前阶段的验证。
- 后端接口统一返回格式。
- 代码命名清晰。
- 不生成伪代码。
- 不留下空实现。
- 每天任务完成后更新 README 和本文件。
- 每天任务完成后运行构建或测试。
- 每天任务完成后提交一个清晰的 commit。
- 如果当前仓库连接 GitHub，提交后创建 Pull Request。

## 15 天计划

| 天数 | 任务 | 状态 |
| --- | --- | --- |
| Day 1 | 初始化项目结构、README、ROADMAP、AGENTS.md | 已完成 |
| Day 2 | 搭建 Spring Boot 后端基础框架 | 未开始 |
| Day 3 | 设计 MySQL 表结构和 init.sql | 未开始 |
| Day 4 | 实现用户注册、登录、JWT | 未开始 |
| Day 5 | 实现角色权限控制 | 未开始 |
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
