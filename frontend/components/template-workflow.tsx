"use client";

import { useState } from "react";
import { MaterialUploader } from "@/components/material-uploader";
import { TaskStatusPanel } from "@/components/task-status-panel";
import { useTaskPolling } from "@/hooks/useTaskPolling";
import { createTask } from "@/lib/api";
import type { Template } from "@/lib/types";

export function TemplateWorkflow({ template }: { template: Template }) {
  const { tasks, queryErrors, trackTask } = useTaskPolling();
  const [activeTaskId, setActiveTaskId] = useState<string | null>(null);

  const handleSubmit = async (assetIds: string[]) => {
    const task = await createTask(template.id, assetIds);
    setActiveTaskId(task.id);
    trackTask(task.id);
  };

  const activeTask = activeTaskId ? tasks[activeTaskId] ?? null : null;
  const queryError = activeTaskId ? queryErrors[activeTaskId] ?? null : null;

  return (
    <>
      <MaterialUploader onSubmit={handleSubmit} template={template} />
      {activeTaskId && (
        <TaskStatusPanel
          queryError={queryError}
          task={activeTask}
          taskId={activeTaskId}
        />
      )}
    </>
  );
}
