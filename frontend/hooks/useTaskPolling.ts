"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { getTask } from "@/lib/api";
import type { Task } from "@/lib/types";

function isTerminalStatus(status: Task["status"]): boolean {
  return status === "SUCCEEDED" || status === "FAILED";
}

export function useTaskPolling(intervalMs = 1000) {
  const [tasks, setTasks] = useState<Record<string, Task>>({});
  const [queryErrors, setQueryErrors] = useState<Record<string, string>>({});
  // 每个任务使用独立定时器，终态只清理自身，避免任务之间串扰。
  const intervals = useRef<Map<string, number>>(new Map());
  const mounted = useRef(true);

  const clearTaskInterval = useCallback((taskId: string) => {
    const intervalId = intervals.current.get(taskId);
    if (intervalId === undefined) return;
    window.clearInterval(intervalId);
    intervals.current.delete(taskId);
  }, []);

  const fetchTask = useCallback(async (taskId: string) => {
    try {
      const task = await getTask(taskId);
      if (!mounted.current) return;
      setTasks((current) => ({ ...current, [taskId]: task }));
      setQueryErrors((current) => {
        if (!(taskId in current)) return current;
        const next = { ...current };
        delete next[taskId];
        return next;
      });
      if (isTerminalStatus(task.status)) {
        clearTaskInterval(taskId);
      }
    } catch (cause) {
      if (!mounted.current) return;
      // 查询失败只记录错误，不覆盖后端已返回的任务状态。
      const message = cause instanceof Error ? cause.message : "状态查询失败";
      setQueryErrors((current) => ({ ...current, [taskId]: message }));
    }
  }, [clearTaskInterval]);

  const trackTask = useCallback((taskId: string) => {
    if (!taskId) return;
    setQueryErrors((current) => {
      if (!(taskId in current)) return current;
      const next = { ...current };
      delete next[taskId];
      return next;
    });
    void fetchTask(taskId);
    if (intervals.current.has(taskId)) return;
    const intervalId = window.setInterval(() => {
      void fetchTask(taskId);
    }, intervalMs);
    intervals.current.set(taskId, intervalId);
  }, [fetchTask, intervalMs]);

  useEffect(() => {
    mounted.current = true;
    const activeIntervals = intervals.current;
    return () => {
      mounted.current = false;
      activeIntervals.forEach((intervalId) => window.clearInterval(intervalId));
      activeIntervals.clear();
    };
  }, []);

  return { tasks, queryErrors, trackTask };
}
