# 拾光 Studio

素材制作演示网站：浏览模板、上传图片、提交异步制作任务，并在“我的任务”中查看历史记录和结果。

## 本地运行

环境要求：Java 21、Maven 3.9、Node.js 24、MySQL 8。

首次运行时初始化数据库：

```bash
mysql -u <user> -p < mysql/init/001_schema.sql
mysql -u <user> -p < mysql/init/002_seed.sql
mysql -u <user> -p < mysql/init/003_upload_asset.sql
```

启动后端：

```bash
cd backend
DB_URL='jdbc:mysql://127.0.0.1:3306/material_studio?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true' \
DB_USERNAME='<user>' \
DB_PASSWORD='<password>' \
APP_DEMO_USER_ID=1 \
mvn spring-boot:run
```

启动前端：

```bash
cd frontend
npm install
npm run dev
```

默认地址为 `http://localhost:3000`，后端为 `http://localhost:8080`。如后端地址不同，启动前端时设置 `NEXT_PUBLIC_API_BASE_URL`。

## 用户身份方案

当前版本采用轻量固定演示用户，不实现登录、账号切换或真实鉴权。

- 后端通过 `app.demo-user-id`（环境变量 `APP_DEMO_USER_ID`）确定当前用户，默认值为 `1`。
- 提交任务时，新任务固定写入该用户；`GET /tasks` 也只返回该用户的任务。
- 前端不提交或选择用户 ID，避免客户端任意指定查询用户。
- 初始化 SQL 已创建 ID 为 `1` 的“演示用户”。如需使用其他用户，需要先在 `app_user` 表创建对应 ID，再修改 `APP_DEMO_USER_ID` 并重启后端。

该方案适合本地演示和单实例作品展示，不应直接用于需要真实隔离的多用户生产环境。

## 历史记录与刷新恢复

访问 `/tasks` 或点击顶部“我的任务”查看当前用户的历史记录。页面加载时从后端读取任务，展示创建时间、状态、阶段、失败原因或成功结果。

- `PENDING`、`PROCESSING` 任务会继续独立轮询，进入终态后停止对应轮询。
- 刷新页面后会重新读取服务端记录并恢复未完成任务的跟踪。
- 查询异常只展示异常提示并保留已有任务信息，不会把任务状态改为失败。

历史接口：

```http
GET /tasks
```

响应中的 `data` 为按创建时间倒序排列的当前用户任务列表。单条任务包含 `id`、`templateId`、`status`、`stage`、`failureStage`、`failureReason`、`resultUrl`、`createdAt`、`updatedAt` 和 `completedAt`。

## 演示异步处理

模拟参数只从后端配置读取。可通过环境变量调整耗时或指定失败阶段：

```bash
APP_STEP_DELAY_MS=3000 \
APP_FAIL_STAGE=SIMULATING \
mvn spring-boot:run
```

`APP_FAIL_STAGE` 可设为 `ASSET_CHECK`、`SIMULATING` 或 `RESULT_PACKAGING`；留空表示成功。建议设置较长的 `APP_STEP_DELAY_MS`，提交后立即进入“我的任务”并刷新页面，观察未完成任务恢复和继续更新。

## 验证

```bash
cd frontend && npx tsc --noEmit
cd backend && mvn test
```
