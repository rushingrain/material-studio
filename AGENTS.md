## 必读文件

- `PROJECT.md`：项目定位、核心/非核心目标、技术栈、目录结构、演示路径

## 项目约束

- 任务查询接口只读。TaskController 的查询方法体内禁止任何 INSERT/UPDATE/Thread.sleep/调用执行器。状态推进只发生在 @Async 方法内
- 前端轮询按 taskId 独立管理。用 useRef<Map<string, number>> 存 interval，终态时清除对应项，组件卸载时清理全部。不要用单个全局 interval 轮询所有任务
- 查询异常不等于任务失败。前端 fetch 封装中，非 2xx 或网络异常只设置 queryError 标志，不修改 task.status。后端返回 status: "FAILED" 是正常 200 响应，走另一条处理路径
- 上传文件存后端目录。不要用 Next.js public/ 存运行时上传的文件；后端 app.upload-dir 是唯一存储位置。
- 不引入额外依赖。不要添加消息队列、Redis、SSE/WebSocket、状态管理库、React Query/SWR
- 模拟参数只从配置读。step-delay-ms 和 fail-stage 只能来自 application.yml，不要硬编码在 Service 里，也不要在前端或模板字段中暴露

## 代码风格

- 后端：Controller 只做参数校验和响应组装，业务逻辑在 Service，SQL 在 Mapper。异步执行器与普通 Service 分开写
- 前端：组件不直接写 setInterval，轮询逻辑集中在 hooks/。API 调用统一走 lib/api.ts，不散落在组件里
- 状态枚举：后端 TaskStatus、TaskStage 与前端 lib/types.ts 中的联合类型保持一致，改一处必须同步另一处

## 注释语言

- 代码注释使用中文书写，力求简洁、清晰、准确表达意图。不要为了注释而注释；只在逻辑不直观、存在隐含约束或需要解释“为什么”时添加注释。
- 避免直译式或啰嗦的注释；注释应说明目的或原因，而非重复代码字面含义。

## 验证

- 前端改动后在 `frontend/` 执行 `npx tsc --noEmit`，确保没有 TypeScript 错误。
- 后端改动后在 `backend/` 执行 `mvn test`