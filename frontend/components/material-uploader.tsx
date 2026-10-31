"use client";

import Image from "next/image";
import Link from "next/link";
import { useState } from "react";
import { ApiError, assetUrl } from "@/lib/api";
import { formatAllowedTypes, formatFileSize, useMaterialUploads } from "@/hooks/useMaterialUploads";
import type { AuthStatus, TaskStatus, Template } from "@/lib/types";

type SubmitMessage = {
  tone: "error" | "info";
  text: string;
};

type MaterialUploaderProps = {
  template: Template;
  onSubmit?: (assetIds: string[]) => Promise<void>;
  authStatus: AuthStatus;
  loginHref: string;
  onRetryAuth: () => void;
  latestTaskStatus: TaskStatus | null;
  latestTaskActive: boolean;
  latestTaskQueryError: boolean;
};

function taskStatusLabel(status: TaskStatus | null, hasQueryError: boolean): string {
  if (status === "PROCESSING") return "任务处理中";
  if (status === "PENDING") return "任务等待处理";
  if (hasQueryError) return "任务状态等待恢复";
  return "任务等待处理";
}

export function MaterialUploader({
  template,
  onSubmit,
  authStatus,
  loginHref,
  onRetryAuth,
  latestTaskStatus,
  latestTaskActive,
  latestTaskQueryError,
}: MaterialUploaderProps) {
  const { slots, readyCount, uploadingCount, allReady, selectFile, removeFile, validateSelection } =
    useMaterialUploads(template);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitMessage, setSubmitMessage] = useState<SubmitMessage | null>(null);
  const authenticated = authStatus === "authenticated";
  const submitDisabled = !authenticated || !allReady || isSubmitting || latestTaskActive;
  const summaryId = `material-upload-summary-${template.id}`;
  const submitMessageId = `material-upload-message-${template.id}`;

  const handleSubmit = async () => {
    if (submitDisabled) return;
    const validationError = validateSelection();
    if (validationError) {
      setSubmitMessage({ tone: "error", text: validationError });
      return;
    }

    const assetIds = slots.flatMap((slot) => slot.asset ? [slot.asset.id] : []);
    if (!onSubmit) {
      setSubmitMessage({ tone: "info", text: "素材已经准备完成，任务提交将在下一步接入。" });
      return;
    }

    setIsSubmitting(true);
    setSubmitMessage(null);
    try {
      await onSubmit(assetIds);
      setSubmitMessage({ tone: "info", text: "任务已受理，处理进度会在下方实时更新。" });
    } catch (cause) {
      const message = cause instanceof ApiError
        ? cause.message
        : cause instanceof Error
          ? cause.message
          : "提交失败，请稍后重试";
      setSubmitMessage({ tone: "error", text: message });
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <section className="requirements-panel" aria-labelledby="requirements-title">
      <div className="panel-heading">
        <div>
          <p className="eyebrow">开始前准备</p>
          <h2 id="requirements-title">素材要求</h2>
        </div>
        <span className="image-count">{template.requiredImageCount}<small> 张</small></span>
      </div>

      {authStatus !== "authenticated" && (
        <div className={`upload-auth-notice is-${authStatus}`} role="status">
          {authStatus === "loading" && <p>正在确认登录状态…</p>}
          {authStatus === "anonymous" && (
            <>
              <p>登录后才能上传素材并提交制作任务。</p>
              <Link className="button button-dark" href={loginHref}>登录后开始</Link>
            </>
          )}
          {authStatus === "error" && (
            <>
              <p>暂时无法确认登录状态，请稍后重试。</p>
              <button className="button button-light" onClick={onRetryAuth} type="button">
                重新检查
              </button>
            </>
          )}
        </div>
      )}

      <div className="upload-slots" aria-label={`需要上传 ${template.requiredImageCount} 张图片`}>
        {slots.map((slot, index) => {
          const inputId = `material-upload-${template.id}-${index}`;
          const errorId = `${inputId}-error`;
          const previewUrl = slot.localPreviewUrl ?? (slot.asset ? assetUrl(slot.asset.url) : null);
          const slotNumber = String(index + 1).padStart(2, "0");
          return (
            <div
              aria-busy={slot.uploading}
              className={`upload-slot${previewUrl ? " is-filled" : ""}${slot.uploading ? " is-uploading" : ""}${slot.error ? " is-error" : ""}`}
              key={index}
            >
              <span className="upload-slot-number" aria-hidden="true">{slotNumber}</span>
              <input
                accept={template.allowedMimeTypes.join(",")}
                aria-describedby={`${summaryId}${slot.error ? ` ${errorId}` : ""}`}
                aria-invalid={slot.error ? true : undefined}
                aria-label={`选择第 ${index + 1} 张图片`}
                className="upload-slot-input"
                disabled={!authenticated || latestTaskActive || isSubmitting}
                id={inputId}
                onChange={(event) => {
                  const file = event.currentTarget.files?.[0];
                  event.currentTarget.value = "";
                  setSubmitMessage(null);
                  if (file) void selectFile(index, file);
                }}
                type="file"
              />

              {previewUrl && (
                <Image
                  alt={`第 ${index + 1} 张素材预览`}
                  className="upload-slot-preview"
                  fill
                  sizes="120px"
                  src={previewUrl}
                  unoptimized
                />
              )}

              {!previewUrl && (
                <label className="upload-slot-trigger" htmlFor={inputId}>
                  <span className="upload-slot-plus" aria-hidden="true">＋</span>
                  <span>{slot.error ? "重新选择" : "添加图片"}</span>
                </label>
              )}

              {slot.uploading && (
                <div className="upload-slot-overlay" role="status">
                  <span className="upload-spinner" aria-hidden="true" />
                  <span>上传中</span>
                </div>
              )}

              {previewUrl && authenticated && !slot.uploading && !latestTaskActive && !isSubmitting && (
                <div className="upload-slot-actions">
                  <label
                    className="upload-slot-action"
                    htmlFor={inputId}
                    title={`替换第 ${index + 1} 张图片`}
                  >
                    <span aria-hidden="true">↻</span><span>替换</span>
                  </label>
                  <button
                    aria-label={`移除第 ${index + 1} 张图片`}
                    className="upload-slot-action upload-slot-remove"
                    onClick={() => {
                      setSubmitMessage(null);
                      removeFile(index);
                    }}
                    title={`移除第 ${index + 1} 张图片`}
                    type="button"
                  >
                    <span aria-hidden="true">×</span><span>移除</span>
                  </button>
                </div>
              )}

              {slot.uploading && (
                <button
                  aria-label={`取消上传并移除第 ${index + 1} 张图片`}
                  className="upload-slot-cancel"
                  onClick={() => {
                    setSubmitMessage(null);
                    removeFile(index);
                  }}
                  title="取消上传并移除"
                  type="button"
                >
                  ×
                </button>
              )}

              {slot.error && (
                <p className="upload-slot-error" id={errorId} role="alert">{slot.error}</p>
              )}
            </div>
          );
        })}
      </div>

      <p
        className={`upload-summary${allReady ? " is-ready" : ""}`}
        id={summaryId}
        aria-live="polite"
      >
        {uploadingCount > 0
          ? `正在上传 ${uploadingCount} 张图片…`
          : allReady
            ? `${template.requiredImageCount} 张素材已就绪`
            : `已准备 ${readyCount} / ${template.requiredImageCount} 张`}
      </p>

      <dl className="requirement-list">
        <div>
          <dt>支持格式</dt>
          <dd>{formatAllowedTypes(template)}</dd>
        </div>
        <div>
          <dt>单张大小</dt>
          <dd>不超过 {formatFileSize(template.maxImageSizeBytes)}</dd>
        </div>
      </dl>

      <div className="detail-cta-row">
        <button
          className={`button button-dark detail-cta${latestTaskActive ? " is-task-active" : ""}`}
          aria-describedby={submitMessage ? submitMessageId : undefined}
          disabled={submitDisabled}
          onClick={() => void handleSubmit()}
          type="button"
        >
          <span>
            {isSubmitting
              ? "正在提交…"
              : latestTaskActive
                ? taskStatusLabel(latestTaskStatus, latestTaskQueryError)
                : authStatus === "loading"
                  ? "正在确认登录状态…"
                  : authStatus === "anonymous"
                    ? "登录后开始创作"
                    : authStatus === "error"
                      ? "身份服务暂不可用"
                : allReady
                  ? "素材已就绪，开始创作"
                  : "上传素材，开始创作"}
          </span>
          <span aria-hidden="true">↗</span>
        </button>
        {submitMessage && (
          <p
            className={`upload-submit-message is-${submitMessage.tone}`}
            id={submitMessageId}
            role={submitMessage.tone === "error" ? "alert" : "status"}
          >
            {submitMessage.text}
          </p>
        )}
      </div>
    </section>
  );
}
