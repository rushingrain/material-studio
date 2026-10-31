"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { getTask } from "@/lib/api";
import {
  addTrackedTask,
  applyTaskQueryError,
  applyTaskSnapshot,
  emptyTaskPollingState,
  isTerminalStatus,
  type TaskPollingState,
} from "@/lib/task-polling-state";
import type { Task } from "@/lib/types";

type TaskPollingOptions = {
  scopeKey?: string;
  intervalMs?: number;
};

export function useTaskPolling({
  scopeKey = "default",
  intervalMs = 1000,
}: TaskPollingOptions = {}) {
  const [state, setState] = useState<TaskPollingState>(
    () => emptyTaskPollingState(scopeKey),
  );
  // 每个任务独立管理定时器、请求和终态，任务之间不共享轮询状态。
  const intervals = useRef<Map<string, number>>(new Map());
  const controllers = useRef<Map<string, AbortController>>(new Map());
  const trackedIdsRef = useRef<Set<string>>(new Set());
  const terminalIds = useRef<Set<string>>(new Set());
  const scopeGeneration = useRef(0);
  const mounted = useRef(false);

  const clearTaskInterval = useCallback((taskId: string) => {
    const intervalId = intervals.current.get(taskId);
    if (intervalId === undefined) return;
    window.clearInterval(intervalId);
    intervals.current.delete(taskId);
  }, []);

  const fetchTask = useCallback(async (taskId: string) => {
    // 同一任务已有请求时跳过本次轮询，避免请求堆积和回写顺序反转。
    if (controllers.current.has(taskId)) return;

    const generation = scopeGeneration.current;
    const controller = new AbortController();
    controllers.current.set(taskId, controller);

    try {
      const task = await getTask(taskId, controller.signal);
      if (
        !mounted.current
        || controller.signal.aborted
        || scopeGeneration.current !== generation
      ) {
        return;
      }

      setState((current) => applyTaskSnapshot(current, scopeKey, task));

      if (isTerminalStatus(task.status)) {
        terminalIds.current.add(taskId);
        clearTaskInterval(taskId);
      }
    } catch (cause) {
      if (
        !mounted.current
        || controller.signal.aborted
        || scopeGeneration.current !== generation
      ) {
        return;
      }
      // 查询失败只记录错误，不覆盖后端已返回的任务状态。
      const message = cause instanceof Error ? cause.message : "状态查询失败";
      setState((current) => applyTaskQueryError(current, scopeKey, taskId, message));
    } finally {
      if (controllers.current.get(taskId) === controller) {
        controllers.current.delete(taskId);
      }
    }
  }, [clearTaskInterval, scopeKey]);

  const startTaskInterval = useCallback((taskId: string) => {
    if (
      terminalIds.current.has(taskId)
      || intervals.current.has(taskId)
    ) {
      return;
    }
    const intervalId = window.setInterval(() => {
      void fetchTask(taskId);
    }, intervalMs);
    intervals.current.set(taskId, intervalId);
  }, [fetchTask, intervalMs]);

  const trackTask = useCallback((taskId: string) => {
    if (!mounted.current || !taskId || trackedIdsRef.current.has(taskId)) return;

    trackedIdsRef.current.add(taskId);
    setState((current) => addTrackedTask(current, scopeKey, taskId));

    void fetchTask(taskId);
    startTaskInterval(taskId);
  }, [fetchTask, scopeKey, startTaskInterval]);

  const retryTask = useCallback((taskId: string) => {
    if (!taskId || terminalIds.current.has(taskId)) return;
    void fetchTask(taskId);
    startTaskInterval(taskId);
  }, [fetchTask, startTaskInterval]);

  const hydrateTasks = useCallback((taskList: Task[]) => {
    if (!mounted.current) return;

    // 历史查询返回新的完整快照，先停止旧请求，再按记录状态重建轮询。
    controllers.current.forEach((controller) => controller.abort());
    controllers.current.clear();
    intervals.current.forEach((intervalId) => window.clearInterval(intervalId));
    intervals.current.clear();

    const seen = new Set<string>();
    const records = taskList.filter((task) => {
      if (!task.id || seen.has(task.id)) return false;
      seen.add(task.id);
      return true;
    });
    const nextTasks = Object.fromEntries(records.map((task) => [task.id, task]));
    const nextTaskIds = records.map((task) => task.id);

    trackedIdsRef.current.clear();
    terminalIds.current.clear();
    nextTaskIds.forEach((taskId) => trackedIdsRef.current.add(taskId));

    setState({
      scopeKey,
      tasks: nextTasks,
      queryErrors: {},
      trackedTaskIds: nextTaskIds,
    });

    records.forEach((task) => {
      if (isTerminalStatus(task.status)) {
        terminalIds.current.add(task.id);
      } else {
        startTaskInterval(task.id);
      }
    });
  }, [scopeKey, startTaskInterval]);

  useEffect(() => {
    mounted.current = true;
    const activeIntervals = intervals.current;
    const activeControllers = controllers.current;
    const activeTrackedIds = trackedIdsRef.current;
    const activeTerminalIds = terminalIds.current;

    return () => {
      mounted.current = false;
      scopeGeneration.current += 1;
      activeIntervals.forEach((intervalId) => window.clearInterval(intervalId));
      activeIntervals.clear();
      activeControllers.forEach((controller) => controller.abort());
      activeControllers.clear();
      activeTrackedIds.clear();
      activeTerminalIds.clear();
    };
  }, [intervalMs, scopeKey]);

  const currentState = state.scopeKey === scopeKey
    ? state
    : emptyTaskPollingState(scopeKey);

  return {
    tasks: currentState.tasks,
    queryErrors: currentState.queryErrors,
    trackedTaskIds: currentState.trackedTaskIds,
    trackTask,
    retryTask,
    hydrateTasks,
  };
}
