"use client";

import Image from "next/image";
import Link from "next/link";
import { useEffect, useState } from "react";
import { ApiError, apiRequest, assetUrl } from "@/lib/api";
import type { ApiState, Template } from "@/lib/types";

const ALL_CATEGORY = "全部灵感";

function GallerySkeleton() {
  return (
    <div className="template-grid" aria-label="正在加载模板" aria-busy="true">
      {Array.from({ length: 8 }, (_, index) => (
        <div className="template-card skeleton-card" key={index}>
          <div className="skeleton skeleton-cover" />
          <div className="template-card-copy">
            <div className="skeleton skeleton-line skeleton-title" />
            <div className="skeleton skeleton-line skeleton-caption" />
          </div>
        </div>
      ))}
    </div>
  );
}

function GalleryError({ error, onRetry }: { error: Error; onRetry: () => void }) {
  const offline = error instanceof ApiError && error.httpStatus === 0;

  return (
    <section className="feedback-card" role="alert">
      <span className="feedback-icon feedback-icon-error" aria-hidden="true">!</span>
      <h2>{offline ? "暂时连不上灵感库" : "模板没有加载出来"}</h2>
      <p>{offline ? "请确认后端服务已启动，再试一次。" : "可能是网络开了个小差，请稍后重试。"}</p>
      <button className="button button-dark" onClick={onRetry} type="button">重新加载</button>
    </section>
  );
}

export function TemplateGallery() {
  const [category, setCategory] = useState(ALL_CATEGORY);
  const [categories, setCategories] = useState<string[]>([ALL_CATEGORY]);
  const [result, setResult] = useState<{ key: string; state: ApiState<Template[]> }>({
    key: "",
    state: { status: "loading" },
  });
  const [reloadKey, setReloadKey] = useState(0);
  const requestKey = `${category}:${reloadKey}`;

  useEffect(() => {
    const controller = new AbortController();
    let current = true;

    const query = category === ALL_CATEGORY ? "" : `?category=${encodeURIComponent(category)}`;
    apiRequest<Template[]>(`/templates${query}`, { signal: controller.signal })
      .then((templates) => {
        if (!current) return;
        setResult({ key: requestKey, state: { status: "success", data: templates } });
        if (category === ALL_CATEGORY) {
          setCategories([ALL_CATEGORY, ...new Set(templates.map((template) => template.category))]);
        }
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
  }, [category, reloadKey, requestKey]);

  const state = result.key === requestKey ? result.state : { status: "loading" as const };

  return (
    <>
      <section className="gallery-toolbar" aria-label="模板分类筛选">
        <div className="category-tabs" role="group" aria-label="按分类筛选模板">
          {categories.map((item) => (
            <button
              aria-pressed={category === item}
              className={`category-tab${category === item ? " is-active" : ""}`}
              key={item}
              onClick={() => setCategory(item)}
              type="button"
            >
              {item}
            </button>
          ))}
        </div>
        {state.status === "success" && (
          <p className="result-count">精选模板 <span>{String(state.data.length).padStart(2, "0")}</span></p>
        )}
      </section>

      {state.status === "loading" && <GallerySkeleton />}
      {state.status === "error" && (
        <GalleryError error={state.error} onRetry={() => setReloadKey((key) => key + 1)} />
      )}
      {state.status === "success" && state.data.length === 0 && (
        <section className="feedback-card" role="status">
          <span className="feedback-icon feedback-icon-empty" aria-hidden="true">✳</span>
          <h2>这个分类还在准备灵感</h2>
          <p>试试其他分类，更多模板很快就会出现。</p>
          <button className="button button-dark" onClick={() => setCategory(ALL_CATEGORY)} type="button">
            查看全部模板
          </button>
        </section>
      )}
      {state.status === "success" && state.data.length > 0 && (
        <div className="template-grid">
          {state.data.map((template, index) => (
            <Link className="template-card" href={`/templates/${template.id}`} key={template.id}>
              <div className="template-cover-wrap">
                <Image
                  alt={`${template.name}模板封面`}
                  className="template-cover"
                  height={800}
                  loading={index < 4 ? "eager" : "lazy"}
                  src={assetUrl(template.coverUrl)}
                  unoptimized
                  width={640}
                />
                <span className="cover-action" aria-hidden="true">↗</span>
              </div>
              <div className="template-card-copy">
                <div>
                  <p className="template-category">{template.category}</p>
                  <h2>{template.name}</h2>
                </div>
                <span className="template-card-arrow" aria-hidden="true">↗</span>
              </div>
            </Link>
          ))}
        </div>
      )}
    </>
  );
}
