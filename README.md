# 拾光 Studio

一个可独立运行的素材制作演示网站。用户浏览模板、上传图片、提交异步制作任务，并在任务后台处理期间继续使用网站；登录后可在“我的任务”中查看个人历史、失败原因和成功结果。

制作流程由 Java 后端按“素材检查 → 模拟生成 → 结果整理”执行，不调用真实 AI。

## 快速演示

推荐使用 Docker Compose 启动前端、后端、MySQL 和初始化数据：

```bash
cp .env.example .env
docker compose up --build
```

启动后访问：

- 前端：`http://localhost:3000`
- 后端：`http://localhost:8080`
- 登录页：`http://localhost:3000/login`

演示账号：

| 用户名 | 密码 | 昵称 |
|---|---|---|
| `demo` | `demo1234` | 演示用户 |
| `alice` | `alice1234` | 光影访客 |

Compose 使用以下持久卷：

- `mysql-data`：MySQL 数据
- `upload-data`：后端上传文件

初始化 SQL 使用固定数据库名 `material_studio`；数据库用户和密码可通过 `.env` 覆盖。

停止服务：

```bash
docker compose down
```

需要连同数据库和上传文件一起重置时，可执行 `docker compose down -v`。该命令会删除本地演示数据，使用前应确认不需要保留记录。

## 技术栈

| 层级 | 技术 | 版本 |
|---|---|---|
| 前端框架 | Next.js App Router | 16.4.3 |
| UI 运行时 | React / React DOM | 19.3.0 |
| 前端语言 | TypeScript | 5.x，精确版本见 `package-lock.json` |
| 样式 | Tailwind CSS / `@tailwindcss/turbopack` | 4.x |
| 前端运行时 | Node.js | 24 |
| 后端语言 | Java | 21 |
| 后端框架 | Spring Boot | 3.2.5 |
| ORM | MyBatis Spring Boot Starter | 3.0.3 |
| 数据库 | MySQL | 8.4 |
| 构建 | npm / Maven | npm 随 Node 24，Maven 3.9.11 |

前端依赖锁定在 `frontend/package-lock.json`，后端 Maven 依赖版本由 `backend/pom.xml` 固定或继承 Spring Boot Parent。

## 项目结构

```text
material-studio/
├── .github/workflows/ci.yml     # CI：前端检查/测试/构建，后端测试/打包
├── .env.example                 # 无真实凭据的本地配置示例
├── docker-compose.yml           # frontend + backend + MySQL
├── mysql/init/                  # 表结构、种子模板、认证数据
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/
│       ├── main/java/.../       # Controller、Service、Mapper、异步执行器
│       ├── main/resources/      # YAML、MyBatis XML、模板 SVG
│       └── test/                # 后端核心行为测试
├── frontend/
│   ├── Dockerfile
│   ├── app/                     # 模板列表、登录、任务历史等页面
│   ├── components/              # 上传、任务状态、认证组件
│   ├── hooks/                   # 多任务轮询、上传请求管理
│   ├── lib/                     # API、类型、纯状态函数
│   ├── tests/                   # 前端任务状态隔离测试
│   └── package-lock.json
└── README.md
```

## 手工启动

需要 Java 21、Maven 3.9、Node.js 24、MySQL 8。

### 初始化 MySQL

依次执行：

```bash
mysql -u <user> -p < mysql/init/001_schema.sql
mysql -u <user> -p < mysql/init/002_seed.sql
mysql -u <user> -p < mysql/init/004_auth.sql
```

`004_auth.sql` 可重复执行，会补齐认证表、演示账号和 `upload_asset.user_id`。已有素材会归属到用户 `1`。

### 启动后端

```bash
cd backend
DB_URL='jdbc:mysql://localhost:3306/material_studio?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true&allowMultiQueries=true' \
DB_USERNAME='<user>' \
DB_PASSWORD='<password>' \
mvn spring-boot:run
```

### 启动前端

```bash
cd frontend
npm ci
npm run dev
```

默认后端地址为 `http://localhost:8080`。改用其他地址时设置 `NEXT_PUBLIC_API_BASE_URL`。

本地登录 Cookie 依赖前后端使用同一主机名，请统一使用 `localhost` 或统一域名。

## 配置项

`.env.example` 不包含真实凭据。Compose 和无环境变量本地开发使用可公开的演示值；部署环境必须通过环境变量覆盖。

| 配置项 | 环境变量 | 说明 |
|---|---|---|
| `spring.datasource.url` | `DB_URL` | JDBC 地址 |
| `spring.datasource.username` | `DB_USERNAME` | 数据库用户 |
| `spring.datasource.password` | `DB_PASSWORD` | 数据库密码 |
| `app.upload-dir` | `APP_UPLOAD_DIR` | 后端上传目录 |
| `app.cors.allowed-origin` | `APP_CORS_ALLOWED_ORIGIN` | 允许的前端来源 |
| `app.auth.cookie-name` | `APP_AUTH_COOKIE_NAME` | 会话 Cookie 名称 |
| `app.auth.cookie-secure` | `APP_AUTH_COOKIE_SECURE` | HTTPS 环境应设为 `true` |
| `app.auth.session-ttl-seconds` | `APP_AUTH_SESSION_TTL_SECONDS` | 会话有效期，默认 7 天 |
| `app.auth.password-iterations` | `APP_AUTH_PASSWORD_ITERATIONS` | PBKDF2 迭代次数 |
| `app.execution.core-pool-size` | `APP_TASK_CORE_POOL_SIZE` | 任务线程池核心线程数 |
| `app.execution.max-pool-size` | `APP_TASK_MAX_POOL_SIZE` | 任务线程池最大线程数 |
| `app.execution.queue-capacity` | `APP_TASK_QUEUE_CAPACITY` | 任务队列容量 |
| `app.simulation.step-delay-ms` | `APP_STEP_DELAY_MS` | 阶段耗时兜底值 |
| `app.simulation.asset-check-delay-ms` | `APP_ASSET_CHECK_DELAY_MS` | 素材检查耗时 |
| `app.simulation.simulating-delay-ms` | `APP_SIMULATING_DELAY_MS` | 模拟生成耗时 |
| `app.simulation.result-packaging-delay-ms` | `APP_RESULT_PACKAGING_DELAY_MS` | 结果整理耗时 |
| `app.simulation.fail-stage` | `APP_FAIL_STAGE` | 指定模拟失败阶段 |

前端配置：

| 配置项 | 环境变量 | 说明 |
|---|---|---|
| API 地址 | `NEXT_PUBLIC_API_BASE_URL` | Java 后端根地址，构建前端时注入 |

## 任务模型与异步流程

统一响应结构：

```json
{
  "code": 0,
  "msg": "成功",
  "data": {}
}
```

任务整体状态：

| 状态 | 含义 |
|---|---|
| `PENDING` | 已受理，等待执行 |
| `PROCESSING` | 后台正在执行 |
| `SUCCEEDED` | 制作成功 |
| `FAILED` | 制作失败 |

任务阶段：

| 阶段 | 含义 |
|---|---|
| `QUEUED` | 等待处理 |
| `ASSET_CHECK` | 素材检查 |
| `SIMULATING` | 模拟生成 |
| `RESULT_PACKAGING` | 结果整理 |
| `COMPLETED` | 制作完成 |
| `FAILED` | 已失败 |

执行流程：

1. `POST /tasks` 校验模板、素材数量、格式、大小和素材所有者。
2. 创建持久化任务和任务素材记录，提交事务。
3. 事务提交后通过 `@TransactionalEventListener(AFTER_COMMIT)` 投递到 Java 线程池。
4. 后台依次执行三个独立阶段，每次阶段推进写入 MySQL。
5. 查询接口只读，不驱动状态推进；终态写入使用状态条件，完成后不会回落为处理中。
6. 指定阶段失败后停止后续阶段，保存 `failureStage` 和 `failureReason`。

## 主要接口

除模板、登录和开发模拟接口外，生产业务接口都要求有效会话 Cookie。未登录返回 HTTP 401。

### 认证

`POST /auth/login`

```json
{
  "username": "demo",
  "password": "demo1234"
}
```

成功时设置 HttpOnly Cookie，`data` 返回：

```json
{
  "id": 1,
  "nickname": "演示用户",
  "createdAt": "2026-10-31T10:00:00"
}
```

其他认证接口：

- `GET /auth/me`：读取当前用户
- `POST /auth/logout`：删除数据库会话并清除 Cookie

### 模板

- `GET /templates`：返回启用模板，可按 `?category=<分类>` 筛选
- `GET /templates/{id}`：返回模板详情，不存在时返回 404

模板字段：

```json
{
  "id": 1,
  "code": "portrait-editorial",
  "name": "柔光人像",
  "category": "人像写真",
  "description": "适合单人肖像与自然光氛围的竖版写真模板。",
  "coverUrl": "/assets/templates/portrait-cover.svg",
  "exampleImageUrl": "/assets/templates/portrait-sample.svg",
  "requiredImageCount": 1,
  "allowedMimeTypes": ["image/jpeg", "image/png", "image/webp"],
  "maxImageSizeBytes": 10485760,
  "sortOrder": 10,
  "active": true
}
```

### 上传

`POST /uploads`，`multipart/form-data`：

- `templateId`
- `file`

成功响应：

```json
{
  "id": "uuid",
  "url": "/uploads/uuid/content",
  "originalFilename": "photo.jpg",
  "mimeType": "image/jpeg",
  "sizeBytes": 123456
}
```

其他接口：

- `GET /uploads/{id}/content`：校验所有者后读取图片
- `DELETE /uploads/{id}`：校验所有者后删除图片

### 任务与历史

`POST /tasks`

```json
{
  "templateId": 1,
  "assetIds": ["asset-uuid"]
}
```

成功响应：

```json
{
  "id": "task-uuid"
}
```

其他接口：

- `GET /tasks/{id}`：只返回当前用户自己的任务
- `GET /tasks`：返回当前用户任务，按创建时间倒序

任务字段：

```json
{
  "id": "task-uuid",
  "userId": 1,
  "templateId": 1,
  "status": "PROCESSING",
  "stage": "SIMULATING",
  "failureStage": null,
  "failureReason": null,
  "resultUrl": null,
  "createdAt": "2026-10-31T10:00:00",
  "updatedAt": "2026-10-31T10:00:02",
  "completedAt": null
}
```

### 开发模拟配置

仅在 `dev` profile 注册：

- `GET /dev/simulation`
- `PUT /dev/simulation`

```json
{
  "failStage": "SIMULATING",
  "assetCheckDelayMs": 500,
  "simulatingDelayMs": 3000,
  "resultPackagingDelayMs": 800
}
```

`failStage` 可为 `ASSET_CHECK`、`SIMULATING`、`RESULT_PACKAGING` 或 `null`。每个任务启动时固定一份配置快照。

## 用户身份与个人记录

使用账号密码和数据库会话，不依赖 Spring Security 或 JWT。

- 密码使用 PBKDF2-HMAC-SHA256 加随机盐存储。
- 浏览器保存 HttpOnly 会话 Cookie，数据库只保存会话令牌的 SHA-256 哈希。
- Cookie 使用 `SameSite=Lax`、`Path=/`；生产 HTTPS 应启用 `Secure`。
- 退出登录删除服务端会话，不只清理前端状态。
- 任务、上传素材和文件读取均按当前会话用户校验。
- `GET /tasks` 不接受客户端指定用户 ID。

该实现用于笔试演示，不包含注册、找回密码、登录限流、全局会话撤销和风控。

### 登录和多用户隔离演示

1. 使用 `demo / demo1234` 登录并提交一个制作任务。
2. 退出后使用 `alice / alice1234` 登录，确认“我的任务”为空。
3. 退出并重新登录 `demo`，确认原任务仍在，刷新后未完成任务继续更新。
4. 在另一账号登录时直接访问 demo 的任务 ID，应返回 404。
5. 退出后访问 `/tasks`，应显示登录入口；请求受保护接口应返回 401。

也可使用 Cookie 文件验证：

```bash
curl -sS -c /tmp/material-studio-cookie \
  -H 'Content-Type: application/json' \
  -d '{"username":"demo","password":"demo1234"}' \
  http://localhost:8080/auth/login

curl -sS -b /tmp/material-studio-cookie http://localhost:8080/tasks
curl -sS -b /tmp/material-studio-cookie -X POST http://localhost:8080/auth/logout
```

## 成功、失败、耗时与多任务演示

### 成功

```bash
curl -sS -X PUT http://localhost:8080/dev/simulation \
  -H 'Content-Type: application/json' \
  -d '{
    "failStage": null,
    "assetCheckDelayMs": 800,
    "simulatingDelayMs": 2200,
    "resultPackagingDelayMs": 800
  }'
```

登录后提交任务，前端应依次显示素材检查、模拟生成、结果整理和成功结果预览。

### 指定阶段失败

将 `failStage` 改为 `ASSET_CHECK`、`SIMULATING` 或 `RESULT_PACKAGING` 后提交新任务。失败任务应停止后续阶段，并显示失败阶段和原因。

### 环境变量配置

也可在启动前配置：

```bash
APP_ASSET_CHECK_DELAY_MS=500 \
APP_SIMULATING_DELAY_MS=3000 \
APP_RESULT_PACKAGING_DELAY_MS=800 \
APP_FAIL_STAGE=SIMULATING \
mvn spring-boot:run
```

### 多任务与刷新恢复

- 一个任务结束后，可在同一模板页继续提交下一个任务。
- 在不同模板页可以产生并发任务；后端继续执行，不依赖页面是否打开。
- `/tasks` 可同时展示多个任务，各自独立轮询。
- 刷新 `/tasks` 会从后端恢复记录并仅继续跟踪未完成任务。
- 查询异常显示恢复入口，不修改任务状态。

## 测试与 CI

后端测试：

```bash
cd backend
mvn test
```

覆盖阶段顺序、三个阶段失败中断、配置解析/更新、密码哈希、登录会话、登出和任务用户归属。

前端检查：

```bash
cd frontend
npm ci
npm run lint
npx tsc --noEmit
npm test
npm run build
```

前端测试使用 Node 内置测试框架，不新增测试依赖，覆盖多任务状态隔离、查询异常与任务失败区分、终态判断和用户作用域切换。

CI 位于 `.github/workflows/ci.yml`，push 和 pull request 自动执行上述检查，并验证 Compose 配置。

## 数据库与素材

- 表结构：`mysql/init/001_schema.sql`
- 模板种子：`mysql/init/002_seed.sql`
- 认证表、账号和归属迁移：`mysql/init/004_auth.sql`
- 模板 SVG：`backend/src/main/resources/static/assets/templates/`

四个模板覆盖 1 至 4 张图片要求、不同分类、不同 MIME 类型和大小限制。示例封面与结果图均为项目内 SVG，不依赖付费或公司内部服务。

## 已完成、限制与实际投入

已完成：

- 模板列表、分类筛选和详情
- 上传、预览、替换、移除、前后端校验
- Java 异步三阶段任务、MySQL 持久化和线程池队列
- 新任务受理后立即返回
- 等待、处理、成功、失败、查询异常状态
- 多任务独立轮询、终态停止、页面卸载和用户切换清理
- 历史记录、刷新恢复、成功预览和失败原因
- 可重复调整阶段耗时和指定阶段失败
- 登录、退出、会话、密码哈希和多用户数据隔离
- 响应式布局、键盘焦点、语义标签和操作标签
- 后端单元测试、前端状态测试和 GitHub Actions CI
- Docker Compose 一键启动

未实现或有意不包含：

- 真实 AI、支付、注册、找回密码
- 对象存储、消息队列、Redis、SSE/WebSocket
- 任务失败后重做、自动重试、受理幂等、断点续跑
- 服务重启后自动恢复已排队/处理中任务；重启后记录保留，但执行线程不会自动续跑
- 登录限流、风控、全局会话撤销

实际投入按模板浏览、异步任务、失败模拟、历史恢复、认证和交付七个阶段迭代完成。项目包含完整前后端、数据库初始化、测试、CI 和容器启动配置；未统计具体小时数。

## 部署与回滚

基本部署步骤：

1. 准备服务器、Docker Compose 和持久卷目录。
2. 根据 `.env.example` 提供数据库密码、公开 API 地址和允许的前端来源。
3. 生产 HTTPS 环境设置 `APP_AUTH_COOKIE_SECURE=true`、`SPRING_PROFILES_ACTIVE=prod`。
4. 执行 `docker compose up -d --build`。
5. 检查 `http://<host>:3000` 登录、历史接口和上传结果。

回滚：

1. 切回上一版源码或上一组镜像。
2. 保留并备份 `mysql-data` 和 `upload-data` 卷。
3. 执行 `docker compose up -d --build` 恢复旧版本。
4. 本项目迁移以新增表和可重复补齐为主；回滚应用代码前仍需确认旧版本能读取当前数据库结构。

## AI 工作流与来源

本项目开发过程中使用了 OpenAI Codex 辅助。工作流包括：

1. 先读取 `PROJECT.md`、`AGENTS.md` 和现有接口，按题目要求拆分阶段。
2. 先检查数据流和事务边界，再修改 Controller、Service、Mapper 与前端状态。
3. 对异步任务、轮询异常、认证和用户隔离编写单元测试。
4. 执行 TypeScript、ESLint、Next.js 构建和 Maven 测试，并根据失败结果修正。
5. 对容器、CI 和 README 进行逐项回归审计。

具体例子：多任务轮询最初只有一个活动任务 ID，后续改为按任务 ID 管理定时器，并把纯状态更新抽成独立函数，增加多任务隔离和查询异常测试。所有 AI 生成代码均由项目维护者审阅，并以构建、测试和人工演示结果为准。

来源与复用：

- 前端基于 Next.js App Router 官方脚手架结构和 API 约定。
- 使用 Next.js、React、Tailwind CSS、Spring Boot、MyBatis 等开源依赖。
- 页面品牌、文案、模板数据和 SVG 示例图为本项目自定义内容。
- 未使用付费图片、公司内部资源或第三方 AI 制作服务。

## 验收提交

验收时请使用包含本 README、前后端源码、数据库脚本、CI 和 Compose 配置的同一个 Git commit。最终提交后可用以下命令记录具体哈希：

```bash
git rev-parse --short HEAD
```
