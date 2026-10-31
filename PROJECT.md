# PROJECT.md

## 项目介绍

素材制作网站。用户浏览模板、上传图片、提交制作任务，任务在 Java 后台异步处理期间可继续使用网站，最后在历史页查看个人任务状态与结果。

制作不调用真实 AI，由后端可控制耗时与成功/失败的模拟生成器完成。项目重点：

1. Java 后端多步骤异步处理的编排
2. 前端异步状态管理与多任务隔离

## 核心目标

- 模板列表与详情，数据来自 Java 接口，支持分类筛选
- 素材上传、预览、替换、移除；前端与服务端双重校验数量/类型/大小
- 提交接口完成校验与任务记录后立即返回 taskId，不等待制作结束
- 后台顺序执行：素材检查 → 模拟生成 → 结果整理，每步独立执行边界，状态持久化 MySQL
- 查询接口只读，不驱动状态推进；完成后任务不回落为处理中
- 可配置指定阶段失败并保存失败阶段与原因；模拟耗时可配置
- 前端严格区分：上传中 / 提交中 / 等待 / 处理中 / 成功 / 失败 / 查询异常
- 查询接口异常不得将任务标记为失败；保留已有信息并提供恢复途径
- 多任务状态互不串扰；未完成任务自动跟踪，终态即停，卸载清理
- 刷新后从后端恢复任务并继续跟踪未结束任务；成功结果可预览
- 简化用户身份与个人记录方案

## 非核心目标（不实现）

- 真实 AI/支付、第三方账号、复杂模板后台、积分/会员、国际化、多区域部署
- 消息队列、Redis、SSE/WebSocket、状态管理库、React Query
- 集群运维、重启后自动恢复执行、断点续跑、自动重试、失败任务重执行
- 结果下载/删除、视频处理、端到端浏览器测试、自动发布
- 对象存储（文件存后端本地目录/容器卷）
- 逐像素还原参考截图

## 技术栈

| 层级 | 技术 | 版本      |
|---|---|---------|
| 后端语言 | Java | 21      |
| 后端框架 | Spring Boot | 3.2.5   |
| ORM | MyBatis Spring Boot Starter | 3.0.3   |
| 数据库 | MySQL | 8.x     |
| 后端构建 | Maven | 3.9.11  |
| 前端框架 | Next.js（App Router） | 16.x    |
| 前端语言 | TypeScript | 5.x     |
| 前端运行时 | Node.js | 24.17.0 |
| 样式 | Tailwind CSS | 4.x     |
| 容器编排 | Docker Compose | v2      |

## 目录结构

material-studio/
├── AGENTS.md
├── PROJECT.md
├── README.md
├── docker-compose.yml
├── .env.example
├── .github/
│   └── workflows/
│       └── ci.yml
│
├── backend/
│   ├── pom.xml
│   ├── Dockerfile
│   └── src/
│       ├── main/
│       │   └── resources/
│       └── test  # 核心行为测试
│
├── frontend/
│   ├── package.json
│   ├── next.config.ts
│   ├── tsconfig.json
│   ├── eslint.config.mjs
│   ├── Dockerfile
│   ├── public/
│   ├── app/                         # App Router 页面：模板列表、登录、详情、历史
│   ├── components/                  # 卡片、上传位、任务状态和认证组件
│   ├── hooks/                       # 多任务轮询、上传请求管理
│   ├── lib/                         # API、类型和纯状态函数
│   └── tests/                       # 前端异步状态测试
│
├── mysql/
│   └── init/                        # Compose 初始化 SQL（schema + 种子数据）
│
└── uploads/                         # 本地开发上传目录（.gitignore）
