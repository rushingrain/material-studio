import type { Metadata } from "next";
import { Suspense } from "react";
import { LoginForm } from "@/components/login-form";

export const metadata: Metadata = {
  title: "登录 · 拾光 Studio",
  description: "登录后管理个人素材制作任务。",
};

export default function LoginPage() {
  return (
    <main className="page-shell auth-page-shell" id="main-content" tabIndex={-1}>
      <Suspense
        fallback={(
          <section className="login-card login-loading" aria-busy="true">
            <div className="skeleton skeleton-line history-loading-title" />
            <div className="skeleton skeleton-line history-loading-line" />
            <div className="skeleton skeleton-line history-loading-line-short" />
          </section>
        )}
      >
        <LoginForm />
      </Suspense>
      <footer className="site-footer">
        <span>拾光 STUDIO</span>
        <span>你的作品，只属于你。</span>
      </footer>
    </main>
  );
}
