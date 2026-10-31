import type { Task } from "./types";

export type TaskPollingState = {
  scopeKey: string;
  tasks: Record<string, Task>;
  queryErrors: Record<string, string>;
  trackedTaskIds: string[];
};

export function emptyTaskPollingState(scopeKey: string): TaskPollingState {
  return {
    scopeKey,
    tasks: {},
    queryErrors: {},
    trackedTaskIds: [],
  };
}

export function stateForScope(
  current: TaskPollingState,
  scopeKey: string,
): TaskPollingState {
  return current.scopeKey === scopeKey ? current : emptyTaskPollingState(scopeKey);
}

export function isTerminalStatus(status: Task["status"]): boolean {
  return status === "SUCCEEDED" || status === "FAILED";
}

export function addTrackedTask(
  current: TaskPollingState,
  scopeKey: string,
  taskId: string,
): TaskPollingState {
  const scoped = stateForScope(current, scopeKey);
  if (scoped.trackedTaskIds.includes(taskId)) return scoped;

  const queryErrors = { ...scoped.queryErrors };
  delete queryErrors[taskId];
  return {
    ...scoped,
    queryErrors,
    trackedTaskIds: [...scoped.trackedTaskIds, taskId],
  };
}

export function applyTaskSnapshot(
  current: TaskPollingState,
  scopeKey: string,
  task: Task,
): TaskPollingState {
  const scoped = stateForScope(current, scopeKey);
  const queryErrors = { ...scoped.queryErrors };
  delete queryErrors[task.id];
  return {
    ...scoped,
    tasks: { ...scoped.tasks, [task.id]: task },
    queryErrors,
    trackedTaskIds: scoped.trackedTaskIds.includes(task.id)
      ? scoped.trackedTaskIds
      : [...scoped.trackedTaskIds, task.id],
  };
}

export function applyTaskQueryError(
  current: TaskPollingState,
  scopeKey: string,
  taskId: string,
  message: string,
): TaskPollingState {
  const scoped = stateForScope(current, scopeKey);
  return {
    ...scoped,
    queryErrors: { ...scoped.queryErrors, [taskId]: message },
  };
}
