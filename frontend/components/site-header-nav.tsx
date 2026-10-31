"use client";

import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { useState } from "react";
import { useAuth } from "@/components/auth-provider";

export function SiteHeaderNav() {
  const pathname = usePathname();
  const router = useRouter();
  const { status, user, error, logout, refresh } = useAuth();
  const [loggingOut, setLoggingOut] = useState(false);
  const [logoutError, setLogoutError] = useState<string | null>(null);
  const templatesActive = pathname === "/" || pathname.startsWith("/templates/");
  const tasksActive = pathname.startsWith("/tasks");
  const loginHref = `/login?next=${encodeURIComponent(pathname)}`;

  const handleLogout = async () => {
    if (loggingOut) return;
    setLoggingOut(true);
    setLogoutError(null);
    try {
      await logout();
      router.push("/");
    } catch (cause) {
      setLogoutError(cause instanceof Error ? cause.message : "退出失败，请重试");
    } finally {
      setLoggingOut(false);
    }
  };

  return (
    <nav className="header-nav" aria-label="主导航">
      <Link aria-current={templatesActive ? "page" : undefined} href="/">灵感模板</Link>
      <Link aria-current={tasksActive ? "page" : undefined} href="/tasks">我的任务</Link>
      {status === "loading" && <span className="auth-loading">身份确认中…</span>}
      {status === "anonymous" && <Link className="auth-login" href={loginHref}>登录</Link>}
      {status === "error" && (
        <button
          className="auth-retry"
          onClick={refresh}
          title={error ?? undefined}
          type="button"
        >
          重试身份
        </button>
      )}
      {status === "authenticated" && user && (
        <div className="auth-controls">
          <span className="auth-user" title={user.nickname}>{user.nickname}</span>
          <button disabled={loggingOut} onClick={() => void handleLogout()} type="button">
            {loggingOut ? "退出中…" : "退出"}
          </button>
        </div>
      )}
      {logoutError && <span className="auth-error" role="alert">{logoutError}</span>}
    </nav>
  );
}

export function SiteHeaderNavFallback() {
  return (
    <nav className="header-nav" aria-label="主导航">
      <Link href="/">灵感模板</Link>
      <Link href="/tasks">我的任务</Link>
      <Link href="/login">登录</Link>
    </nav>
  );
}
