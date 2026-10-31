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

模拟参数只由后端控制，不会出现在前端或模板字段中。默认值来自 `application.yml`：

| 配置项 | 环境变量 | 默认行为 |
|---|---|---|
| `app.simulation.step-delay-ms` | `APP_STEP_DELAY_MS` | 三个阶段共用的兜底耗时，默认 1000ms |
| `app.simulation.asset-check-delay-ms` | `APP_ASSET_CHECK_DELAY_MS` | 素材检查耗时，未设置时使用兜底耗时 |
| `app.simulation.simulating-delay-ms` | `APP_SIMULATING_DELAY_MS` | 模拟生成耗时，未设置时使用兜底耗时 |
| `app.simulation.result-packaging-delay-ms` | `APP_RESULT_PACKAGING_DELAY_MS` | 结果整理耗时，未设置时使用兜底耗时 |
| `app.simulation.fail-stage` | `APP_FAIL_STAGE` | 指定失败阶段，留空表示不模拟失败 |

可直接通过环境变量启动：

```bash
APP_ASSET_CHECK_DELAY_MS=500 \
APP_SIMULATING_DELAY_MS=3000 \
APP_RESULT_PACKAGING_DELAY_MS=800 \
APP_FAIL_STAGE=SIMULATING \
mvn spring-boot:run
```

### 开发接口调整

`dev` profile 下提供运行时配置接口，修改后无需重启，可反复触发不同场景。该接口不会在 `prod` profile 下注册。

查看当前配置：

```bash
curl -sS http://localhost:8080/dev/simulation
```

设置模拟生成阶段 3 秒后失败：

```bash
curl -sS -X PUT http://localhost:8080/dev/simulation \
  -H 'Content-Type: application/json' \
  -d '{
    "failStage": "SIMULATING",
    "assetCheckDelayMs": 500,
    "simulatingDelayMs": 3000,
    "resultPackagingDelayMs": 800
  }'
```

`failStage` 可取 `ASSET_CHECK`、`SIMULATING`、`RESULT_PACKAGING`，设为 `null` 表示不模拟失败。三个耗时字段必填且不能小于 0。

每次更新后提交新任务即可重复验证。每个任务在执行开始时固定一份配置快照，因此修改配置不会影响已经开始的任务；正在排队的任务会在真正开始执行时读取最新配置。

某阶段模拟失败后，后端不会执行后续阶段，并保存 `FAILED` 状态、失败阶段和类似“模拟失败：模拟生成阶段”的原因。前端任务面板和历史页会展示该阶段与原因。

### 手动验证 case

1. 将 `failStage` 设为 `null`，三个阶段设为不同耗时，提交任务；状态应按“素材检查 → 模拟生成 → 结果整理 → 制作完成”推进。
2. 将 `failStage` 设为 `ASSET_CHECK` 后提交；任务应在第一阶段变为失败，后两个阶段保持未执行，且结果地址为空。
3. 将 `failStage` 改为 `SIMULATING` 后再次提交；应保存 `failureStage: "SIMULATING"` 和可读失败原因，证明配置可重复触发。
4. 设置较长耗时后提交一个任务，在该任务开始后修改 `failStage`，再提交第二个任务；第一个任务应继续使用启动时配置，第二个任务使用新配置。
5. 传入不支持的阶段或负耗时应返回 400；将 `failStage` 清空后提交新任务应恢复成功流程。
6. 使用 `SPRING_PROFILES_ACTIVE=prod` 启动时，`/dev/simulation` 不应注册。

开发接口不应暴露在公网环境。

## 前端边界回归

1. 访问 `/templates/999999`：应显示模板不存在提示，并可返回模板列表。
2. 模板接口返回空数组：首页应显示空状态和“查看全部模板”恢复入口。
3. 历史列表接口不可用：历史页应显示重新加载入口，不将任何本地任务标记为失败。
4. 已展示处理中任务后，单任务查询接口不可用：应保留原有“处理中”状态，显示查询异常和手动重试入口。
5. 同时打开两个未完成任务：两个任务应各自更新，一个进入终态时另一个继续轮询。
6. 未完成任务存在时刷新 `/tasks`：应从服务端恢复记录并继续更新，成功结果可以预览。
7. 配置某阶段失败后提交任务：应停在失败阶段，后续阶段不推进，页面显示失败阶段和原因。
8. 键盘操作：首次按 Tab 应可聚焦“跳到主要内容”，导航、分类、上传替换/移除、重试等操作应有可见焦点。

## 验证

```bash
cd frontend && npx tsc --noEmit
cd backend && mvn test
```
