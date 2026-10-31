"use client";

import { useEffect, useRef } from "react";
import { useAuth } from "@/components/auth-provider";
import { MaterialUploader } from "@/components/material-uploader";
import { TaskStatusPanel } from "@/components/task-status-panel";
import { useTaskPolling } from "@/hooks/useTaskPolling";
import { createTask } from "@/lib/api";
import { taskScopeKey } from "@/lib/identity";
import type { Template } from "@/lib/types";

export function TemplateWorkflow({ template }: { template: Template }) {
  const { status: authStatus, user, refresh: refreshAuth } = useAuth();
  const { tasks, queryErrors, trackedTaskIds, trackTask, retryTask } = useTaskPolling({
    scopeKey: taskScopeKey(user?.id),
  });
  const submitController = useRef<AbortController | null>(null);
  const latestTaskId = trackedTaskIds.at(-1) ?? null;
  const latestTask = latestTaskId ? tasks[latestTaskId] ?? null : null;
  const latestTaskQueryError = latestTaskId ? queryErrors[latestTaskId] ?? null : null;
  const latestTaskIsActive = latestTaskId !== null
    && (!latestTask || latestTask.status === "PENDING" || latestTask.status === "PROCESSING");

  useEffect(() => () => {
    submitController.current?.abort();
  }, []);

  const handleSubmit = async (assetIds: string[]) => {
    submitController.current?.abort();
    const controller = new AbortController();
    submitController.current = controller;

    try {
      const task = await createTask(template.id, assetIds, controller.signal);
      if (controller.signal.aborted) return;
      trackTask(task.id);
    } finally {
      if (submitController.current === controller) {
        submitController.current = null;
      }
    }
  };

  return (
    <>
      <MaterialUploader
        authStatus={authStatus}
        key={`${template.id}:${user?.id ?? "anonymous"}`}
        latestTaskActive={latestTaskIsActive}
        latestTaskQueryError={latestTaskQueryError !== null}
        latestTaskStatus={latestTask?.status ?? null}
        loginHref={`/login?next=${encodeURIComponent(`/templates/${template.id}`)}`}
        onSubmit={handleSubmit}
        onRetryAuth={refreshAuth}
        template={template}
      />
      {trackedTaskIds.length > 0 && (
        <div className="task-status-list" aria-label="已提交任务" role="list">
          {trackedTaskIds.map((taskId) => (
            <TaskStatusPanel
              key={taskId}
              onRetry={() => retryTask(taskId)}
              queryError={queryErrors[taskId] ?? null}
              task={tasks[taskId] ?? null}
              taskId={taskId}
            />
          ))}
        </div>
      )}
    </>
  );
}
