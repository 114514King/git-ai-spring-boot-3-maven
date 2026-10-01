# 外部招聘数据接入说明

核查及真实联调日期：2026-10-01。已接入 Jobicy 公开远程岗位数据；国内平台尚未接入。

## 已接入来源：Jobicy

[Jobicy 官方文档](https://jobicy.com/jobs-rss-feed)及[官方 API 仓库](https://github.com/Jobicy/remote-jobs-api)提供无认证公共接口，并允许招聘网站、应用和职业工具展示岗位，无需单独申请 API Key。必须标明来源，保留原始岗位链接，申请按钮跳转来源返回的 URL。

```text
GET https://jobicy.com/api/v2/remote-jobs?count=200
```

来源覆盖最近 7 天发布的远程岗位，约延迟 3 小时。`count` 最大为 200，完整读取可以顺序遍历返回的 `nextCursor`；当前模块只读取最新一页，不遍历游标。因此页面和接口明确限定“本次同步的最多 200 条”，不能称为完整最近 7 天或全站岗位库。远程岗位仍有国家/地区限制，以 `jobGeo` 和原始描述为准。

## 实际实现

- 公开浏览页 `/external-jobs`，登录/注册页和学生工作台有入口。不强制注册后查看数据。
- `GET /api/external-jobs` 返回统一响应，支持 `keyword`、`location`、`page`、`pageSize`。筛选在共享快照内完成；不同筛选和刷新按钮不会分别调用来源接口。`total` 是快照中匹配数。
- 固定 HTTPS 来源接口；3 秒连接超时、8 秒读取超时，不自动跟随重定向；响应体上限 6 MiB。无用户提供的 URL 或客户端凭证。
- 按需每小时最多一次尝试，成功和失败均受限制；同进程并发请求只执行一次同步。有不超过 24 小时的旧快照时显示暂存提示，过旧或首次失败返回统一 503，不把网络失败误判为岗位关闭。
- `lastSyncedAt` 是本系统读取成功时间；来源 `lastUpdate` 是最新发布时间，不作为本地同步时间。按 `id` 去重；缺少身份、标题、公司或安全链接的记录跳过；非空数据全部不合法时视为同步失败，保留旧快照。
- HTML 描述用 Jsoup 转为纯文本，去除脚本/样式，前端文本呈现；岗位 URL 限定 HTTPS、Jobicy 域名及 `/jobs/` 路径。没有读取或展示招聘聊天、联系人或求职者数据。
- 保留 `salaryMin`/`salaryMax` 的币种和周期；缺少薪资显示“未提供”，不换算成人民币/月。`pubDate` 只按显式时区解析，没有时区或无效时显示未提供。
- 原站申请按钮不创建本平台“已投递”记录。本平台 HR 管理、候选人排序、投递和匹配继续使用本平台数据；外部岗位不写入 `job_posting`，不任意关联 HR。
- 通过 `EXTERNAL_JOBS_ENABLED=false` 可禁用。无需本次数据库迁移。

当前缓存和请求限制属于单个后端进程，重启会清空；多实例部署前需实现共享缓存和同步限制。未实现来源游标遍历、状态接口、定时后台同步或历史库；离开 7 天数据窗口不等于岗位已关闭。

## 国内平台核查结果

用户优先选择 BOSS直聘、智联招聘和前程无忧。本次未找到可直接使用、面向全站岗位读取的公开接口文档，未对三家平台做 API 联调。“未找到”不证明不存在企业或合作接口。

| 来源 | 本次可以确认的内容 | 接入仍需要的条件 |
| --- | --- | --- |
| BOSS直聘 | BossHi 官方文档的接口面向企业消息和通讯录，不能当作 BOSS直聘岗位 API | 官方岗位数据合作文档、应用凭证和展示授权 |
| 智联招聘 | 有企业帮助中心，尚未核实公开岗位读取接口 | 官方合作接口和数据展示范围 |
| 前程无忧 | 有企业招聘服务入口，尚未核实公开岗位读取接口 | 官方合作接口或有权导出的数据 |

参考：[BossHi API 调用说明](https://histatic.zhipin.com/front/bosshi-mp-docs/service/ready/apiCall/callProcess/callApi.html)、[智联企业帮助中心](https://rd6.zhaopin.com/app/help)、[前程无忧企业招聘服务](https://mall.51job.com/)。账户登录、职位发布权限和全站读取权限不能相互替代。

## 其他来源的选择边界

[Remotive 官方 API 条款](https://remotive.com/remote-jobs/api)限制以收集注册或邮箱为目的展示岗位；本次没有接入。[Greenhouse Job Board API](https://docs.greenhouse.io/job-board.html)及[Lever Postings API](https://github.com/lever/postings-api)适用于指定企业招聘板，需要明确企业及展示使用条件；本次没有实现这些来源，也没有实现 CSV/JSON 文件导入。
