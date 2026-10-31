"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useState, type FormEvent } from "react";
import { useAuth } from "@/components/auth-provider";
import { ApiError } from "@/lib/api";

function safeNextPath(value: string | null): string {
  if (!value || !value.startsWith("/") || value.startsWith("//")) return "/tasks";
  return value;
}

export function LoginForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const nextPath = safeNextPath(searchParams.get("next"));
  const { status, error: authError, login, refresh } = useAuth();
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    if (status === "authenticated") {
      router.replace(nextPath);
    }
  }, [nextPath, router, status]);

  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (submitting) return;

    setSubmitting(true);
    setFormError(null);
    try {
      await login({ username, password });
    } catch (cause) {
      setFormError(
        cause instanceof ApiError
          ? cause.message
          : cause instanceof Error
            ? cause.message
            : "登录失败，请稍后重试",
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <section className="login-card" aria-labelledby="login-title">
      <div className="login-intro">
        <p className="eyebrow">回到你的创作空间</p>
        <h1 id="login-title">登录拾光</h1>
        <p>登录后上传素材、提交任务，并在“我的任务”中查看个人记录。</p>
      </div>

      {status === "error" && authError && (
        <div className="login-service-error" role="alert">
          <p>暂时无法确认登录状态：{authError}</p>
          <button className="button button-light" onClick={refresh} type="button">
            重新检查
          </button>
        </div>
      )}

      <form className="login-form" onSubmit={handleSubmit}>
        <label className="field-label" htmlFor="login-username">用户名</label>
        <input
          autoComplete="username"
          className="field-input"
          id="login-username"
          maxLength={80}
          onChange={(event) => setUsername(event.currentTarget.value)}
          required
          value={username}
        />

        <label className="field-label" htmlFor="login-password">密码</label>
        <input
          aria-describedby={formError ? "login-form-error" : undefined}
          aria-invalid={formError ? true : undefined}
          autoComplete="current-password"
          className="field-input"
          id="login-password"
          maxLength={200}
          onChange={(event) => setPassword(event.currentTarget.value)}
          required
          type="password"
          value={password}
        />

        {formError && (
          <p className="login-form-error" id="login-form-error" role="alert">
            {formError}
          </p>
        )}

        <button
          className="button button-dark login-submit"
          disabled={submitting || status === "loading"}
          type="submit"
        >
          {submitting ? "正在登录…" : status === "loading" ? "正在确认登录状态…" : "登录"}
        </button>
      </form>

      <div className="login-demo">
        <p>演示账号</p>
        <code>demo / demo1234</code>
        <code>alice / alice1234</code>
      </div>

      <Link className="back-link login-back" href="/">← <span>返回模板列表</span></Link>
    </section>
  );
}
