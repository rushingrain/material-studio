"use client";

import Image from "next/image";
import { assetUrl } from "@/lib/api";
import type { Task, TaskStage, TaskStatus } from "@/lib/types";

const STAGES: Array<{ stage: TaskStage; label: string }> = [
  { stage: "ASSET_CHECK", label: "素材检查" },
  { stage: "SIMULATING", label: "模拟生成" },
  { stage: "RESULT_PACKAGING", label: "结果整理" },
];

const STATUS_LABELS: Record<TaskStatus, string> = {
  PENDING: "等待处理",
  PROCESSING: "处理中",
  SUCCEEDED: "制作完成",
  FAILED: "处理失败",
};

const STAGE_LABELS: Record<TaskStage, string> = {
  QUEUED: "等待处理",
  ASSET_CHECK: "素材检查",
  SIMULATING: "模拟生成",
  RESULT_PACKAGING: "结果整理",
  COMPLETED: "制作完成",
  FAILED: "处理失败",
};

const STAGE_ORDER: Record<TaskStage, number> = {
  QUEUED: 0,
  ASSET_CHECK: 1,
  SIMULATING: 2,
  RESULT_PACKAGING: 3,
  COMPLETED: 4,
  FAILED: 5,
};

type StepState = "pending" | "active" | "complete" | "failed";

const STEP_STATE_LABELS: Record<StepState, string> = {
  pending: "尚未开始",
  active: "正在进行",
  complete: "已完成",
  failed: "失败",
};

function formatTaskTime(value: string): string {
  return value.replace("T", " ").replace(/\.\d+.*$/, "").slice(0, 19);
}

function getStepState(task: Task, stage: TaskStage): StepState {
  const currentStage = task.status === "FAILED" ? task.failureStage : task.stage;
  if (!currentStage) return "pending";

  const currentOrder = STAGE_ORDER[currentStage];
  const stepOrder = STAGE_ORDER[stage];
  if (task.status === "FAILED" && currentOrder === stepOrder) return "failed";
  if (currentOrder > stepOrder) return "complete";
  if (currentOrder === stepOrder) return "active";
  return "pending";
}

export function TaskStatusPanel({
  taskId,
  task,
  queryError,
  onRetry,
}: {
  taskId: string;
  task: Task | null;
  queryError: string | null;
  onRetry: () => void;
}) {
  const currentStage = task?.status === "FAILED" ? task.failureStage : task?.stage;
  const badgeText = task ? STATUS_LABELS[task.status] : queryError ? "查询异常" : "读取中";
  const badgeClass = task
    ? `is-${task.status.toLowerCase()}`
    : queryError
      ? "is-query-error"
      : "is-pending";
  const panelClass = task
    ? `is-${task.status.toLowerCase()}`
    : queryError
      ? "is-query-error"
      : "is-loading";
  const shortTaskId = taskId.slice(0, 8);

  return (
    <section
      aria-busy={!task && !queryError}
      aria-live="polite"
      className={`task-status-panel ${panelClass}`}
      role="listitem"
    >
      <div className="task-status-heading">
        <div>
          <p className="eyebrow">服务端处理进度</p>
          <h2>任务 #{shortTaskId}</h2>
        </div>
        <span className={`task-status-badge ${badgeClass}`}>{badgeText}</span>
      </div>

      {task && (
        <ol className="task-stage-list" aria-label="任务处理阶段">
          {STAGES.map((item, index) => {
            const state = getStepState(task, item.stage);
            return (
              <li
                aria-current={state === "active" || state === "failed" ? "step" : undefined}
                className={`task-stage is-${state}`}
                key={item.stage}
              >
                <span aria-hidden="true" className="task-stage-number">
                  {String(index + 1).padStart(2, "0")}
                </span>
                <span className="task-stage-label">{item.label}</span>
                <span className="visually-hidden">，{STEP_STATE_LABELS[state]}</span>
              </li>
            );
          })}
        </ol>
      )}

      {!task && !queryError && (
        <p className="task-status-loading" role="status">任务已提交，正在读取状态…</p>
      )}

      {task && (
        <div className="task-status-meta">
          <p>
            当前状态：<strong>{STATUS_LABELS[task.status]}</strong>
            {currentStage ? ` · ${STAGE_LABELS[currentStage]}` : ""}
          </p>
          <p>创建时间：{formatTaskTime(task.createdAt)}</p>
          {task.status === "FAILED" && (
            <div className="task-failure-detail">
              <p>
                失败阶段：<strong>{task.failureStage ? STAGE_LABELS[task.failureStage] : "处理中"}</strong>
              </p>
              <p>
                失败原因：<strong>{task.failureReason ?? "任务处理失败，请稍后重试"}</strong>
              </p>
            </div>
          )}
          {task.status === "SUCCEEDED" && task.resultUrl && (
            <div className="task-success-detail">
              <h3>制作结果</h3>
              <div className="task-result-preview">
                <Image
                  alt={`任务 #${shortTaskId} 制作结果预览`}
                  height={480}
                  src={assetUrl(task.resultUrl)}
                  unoptimized
                  width={640}
                />
              </div>
              <a
                className="task-result-link"
                href={assetUrl(task.resultUrl)}
                rel="noreferrer"
                target="_blank"
              >
                查看结果原图 ↗
              </a>
            </div>
          )}
          {task.status === "SUCCEEDED" && !task.resultUrl && (
            <p>制作已经完成，但结果地址暂未返回。</p>
          )}
        </div>
      )}

      {queryError && (
        <div className="task-query-error" role="alert">
          <p>状态查询失败：{queryError}。任务状态未因此改变，正在自动重试。</p>
          <button
            aria-label={`立即重新查询任务 #${shortTaskId} 的状态`}
            onClick={onRetry}
            type="button"
          >
            立即重试
          </button>
        </div>
      )}

      <span className="task-id">完整任务 ID：{taskId}</span>
    </section>
  );
}
