"use client";

import Image from "next/image";
import Link from "next/link";
import { useParams } from "next/navigation";
import { useEffect, useState } from "react";
import { ApiError, apiRequest, assetUrl } from "@/lib/api";
import { TemplateWorkflow } from "@/components/template-workflow";
import type { ApiState, Template } from "@/lib/types";

export function TemplateDetailSkeleton() {
  return (
    <div className="detail-layout" aria-label="正在加载模板详情" aria-busy="true">
      <div className="skeleton detail-skeleton-cover" />
      <div className="detail-copy">
        <div className="skeleton skeleton-line skeleton-caption" />
        <div className="skeleton skeleton-line detail-skeleton-title" />
        <div className="skeleton skeleton-line detail-skeleton-description" />
        <div className="skeleton detail-skeleton-panel" />
      </div>
    </div>
  );
}

function DetailError({ error, onRetry }: { error: Error; onRetry: () => void }) {
  const missing = error instanceof ApiError && error.code === 20001;
  const offline = error instanceof ApiError && error.httpStatus === 0;

  return (
    <section className="feedback-card detail-feedback" role="alert">
      <span className={`feedback-icon ${missing ? "feedback-icon-empty" : "feedback-icon-error"}`} aria-hidden="true">
        {missing ? "?" : "!"}
      </span>
      <h1>{missing ? "这份灵感暂时找不到" : offline ? "暂时连不上灵感库" : "详情没有加载出来"}</h1>
      <p>{missing ? "模板可能已经下架，回到灵感列表看看其他选择吧。" : offline ? "请确认后端服务已启动，再试一次。" : "网络可能不太稳定，请稍后重试。"}</p>
      <div className="feedback-actions">
        {!missing && <button className="button button-dark" onClick={onRetry} type="button">重新加载</button>}
        <Link className="button button-light" href="/">返回模板列表</Link>
      </div>
    </section>
  );
}

export function TemplateDetail() {
  const { id } = useParams<{ id: string }>();
  const [result, setResult] = useState<{ key: string; state: ApiState<Template> }>({
    key: "",
    state: { status: "loading" },
  });
  const [reloadKey, setReloadKey] = useState(0);
  const requestKey = `${id}:${reloadKey}`;

  useEffect(() => {
    const controller = new AbortController();
    let current = true;

    const request = /^\d+$/.test(id) && Number(id) > 0
      ? apiRequest<Template>(`/templates/${encodeURIComponent(id)}`, { signal: controller.signal })
      : Promise.reject<Template>(new ApiError("模板不存在", 20001, 404));

    request
      .then((template) => {
        if (current) setResult({ key: requestKey, state: { status: "success", data: template } });
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
  }, [id, reloadKey, requestKey]);

  const state = result.key === requestKey ? result.state : { status: "loading" as const };

  if (state.status === "loading" || state.status === "idle") return <TemplateDetailSkeleton />;
  if (state.status === "error") {
    return <DetailError error={state.error} onRetry={() => setReloadKey((key) => key + 1)} />;
  }

  const template = state.data;
  return (
    <article aria-labelledby="template-title" className="detail-layout">
      <div className="detail-cover-wrap">
        <Image
          alt={`${template.name}模板封面`}
          className="detail-cover"
          height={800}
          priority
          src={assetUrl(template.coverUrl)}
          unoptimized
          width={640}
        />
        <span className="detail-cover-label">模板预览</span>
      </div>
      <div className="detail-copy">
        <Link className="back-link" href="/">← <span>返回灵感列表</span></Link>
        <p className="eyebrow">{template.category} <span>·</span> 创作模板</p>
        <h1 id="template-title">{template.name}</h1>
        <p className="detail-description">{template.description}</p>

        <TemplateWorkflow key={template.id} template={template} />
      </div>
    </article>
  );
}
