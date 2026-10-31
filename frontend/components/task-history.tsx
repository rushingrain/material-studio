"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { AuthRequired } from "@/components/auth-required";
import { useAuth } from "@/components/auth-provider";
import { TaskStatusPanel } from "@/components/task-status-panel";
import { useTaskPolling } from "@/hooks/useTaskPolling";
import { ApiError, getTasks } from "@/lib/api";
import { taskScopeKey } from "@/lib/identity";
import type { ApiState, Task } from "@/lib/types";

function isActiveTask(task: Task | undefined): boolean {
  return task?.status === "PENDING" || task?.status === "PROCESSING";
}

function HistorySkeleton() {
  return (
    <div className="history-loading-list" aria-busy="true" aria-label="正在加载任务记录">
      {Array.from({ length: 3 }, (_, index) => (
        <div className="history-loading-card" key={index}>
          <div className="skeleton skeleton-line history-loading-title" />
          <div className="skeleton skeleton-line history-loading-line" />
          <div className="skeleton skeleton-line history-loading-line-short" />
        </div>
      ))}
    </div>
  );
}

function HistoryError({ error, onRetry }: { error: Error; onRetry: () => void }) {
  const offline = error instanceof ApiError && error.httpStatus === 0;

  return (
    <section className="feedback-card history-feedback" role="alert">
      <span className="feedback-icon feedback-icon-error" aria-hidden="true">!</span>
      <h1>{offline ? "暂时连不上任务记录" : "任务记录没有加载出来"}</h1>
      <p>{offline ? "请确认后端服务已启动，再重新加载。" : "记录仍然保存在服务器，可以稍后重试。"}</p>
      <button className="button button-dark" onClick={onRetry} type="button">重新加载</button>
    </section>
  );
}

export function TaskHistory() {
  const { status: authStatus, user, error: authError, refresh: refreshAuth } = useAuth();
  const [result, setResult] = useState<{ key: string; state: ApiState<Task[]> }>({
    key: "",
    state: { status: "loading" },
  });
  const [reloadKey, setReloadKey] = useState(0);
  const { tasks, queryErrors, trackedTaskIds, hydrateTasks, retryTask } = useTaskPolling({
    scopeKey: taskScopeKey(user?.id),
  });
  const requestKey = `${user?.id ?? "anonymous"}:${reloadKey}`;

  useEffect(() => {
    if (authStatus !== "authenticated" || !user) return;

    const controller = new AbortController();
    let current = true;

    getTasks(controller.signal)
      .then((records) => {
        if (!current) return;
        hydrateTasks(records);
        setResult({ key: requestKey, state: { status: "success", data: records } });
      })
      .catch((error: unknown) => {
        if (!current) return;
        setResult({
          key: requestKey,
          state: {
            status: "error",
            error: error instanceof Error ? error : new Error("请求失败"),
          },
        });
      });

    return () => {
      current = false;
      controller.abort();
    };
  }, [authStatus, hydrateTasks, requestKey, user]);

  const state = result.key === requestKey ? result.state : { status: "loading" as const };

  if (authStatus === "loading") {
    return (
      <section className="history-page" aria-labelledby="history-title">
        <header className="history-heading">
          <div>
            <p className="eyebrow">你的创作档案</p>
            <h1 id="history-title">我的任务</h1>
            <p>正在确认登录状态，请稍候。</p>
          </div>
        </header>
        <HistorySkeleton />
      </section>
    );
  }

  if (authStatus === "error") {
    return (
      <section className="history-page" aria-labelledby="history-title">
        <header className="history-heading">
          <div>
            <p className="eyebrow">你的创作档案</p>
            <h1 id="history-title">我的任务</h1>
          </div>
        </header>
        <section className="feedback-card history-feedback" role="alert">
          <span className="feedback-icon feedback-icon-error" aria-hidden="true">!</span>
          <h2>暂时无法确认登录状态</h2>
          <p>{authError ?? "请稍后重新尝试。"}</p>
          <button className="button button-dark" onClick={refreshAuth} type="button">
            重新检查
          </button>
        </section>
      </section>
    );
  }

  if (authStatus === "anonymous") {
    return (
      <section className="history-page" aria-labelledby="history-title">
        <header className="history-heading">
          <div>
            <p className="eyebrow">你的创作档案</p>
            <h1 id="history-title">我的任务</h1>
            <p>登录后查看每一份创作记录和处理进度。</p>
          </div>
        </header>
        <AuthRequired nextPath="/tasks" description="登录后可查看和继续跟踪你的个人制作任务。" />
      </section>
    );
  }

  return (
    <section className="history-page" aria-labelledby="history-title">
      <header className="history-heading">
        <div>
          <p className="eyebrow">你的创作档案</p>
          <h1 id="history-title">我的任务</h1>
          <p>记录从服务器恢复，未完成的制作会在这里继续更新。</p>
        </div>
        {state.status === "success" && (
          <div className="history-summary" aria-live="polite">
            <strong>{String(state.data.length).padStart(2, "0")}</strong>
            <span>
              {state.data.length === 0
                ? "暂无记录"
                : `${state.data.filter((task) => isActiveTask(tasks[task.id] ?? task)).length} 个进行中`}
            </span>
          </div>
        )}
      </header>

      {(state.status === "idle" || state.status === "loading") && <HistorySkeleton />}

      {state.status === "error" && (
        <HistoryError
          error={state.error}
          onRetry={() => setReloadKey((key) => key + 1)}
        />
      )}

      {state.status === "success" && state.data.length === 0 && (
        <section className="feedback-card history-feedback" role="status">
          <span className="feedback-icon feedback-icon-empty" aria-hidden="true">✳</span>
          <h2>还没有创作记录</h2>
          <p>选择一个模板，上传素材后，任务会出现在这里。</p>
          <Link className="button button-dark" href="/">开始创作</Link>
        </section>
      )}

      {state.status === "success" && trackedTaskIds.length > 0 && (
        <div
          className="task-status-list history-task-list"
          aria-label="个人任务记录"
          role="list"
        >
          {trackedTaskIds.map((taskId) => (
            <TaskStatusPanel
              key={taskId}
              onRetry={() => retryTask(taskId)}
              queryError={queryErrors[taskId] ?? null}
              task={tasks[taskId] ?? state.data.find((task) => task.id === taskId) ?? null}
              taskId={taskId}
            />
          ))}
        </div>
      )}
    </section>
  );
}
