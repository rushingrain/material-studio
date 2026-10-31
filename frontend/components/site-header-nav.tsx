"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

export function SiteHeaderNav() {
  const pathname = usePathname();
  const templatesActive = pathname === "/" || pathname.startsWith("/templates/");
  const tasksActive = pathname.startsWith("/tasks");

  return (
    <nav className="header-nav" aria-label="主导航">
      <Link aria-current={templatesActive ? "page" : undefined} href="/">灵感模板</Link>
      <Link aria-current={tasksActive ? "page" : undefined} href="/tasks">我的任务</Link>
      <span className="nav-note">把日常做成作品</span>
    </nav>
  );
}

export function SiteHeaderNavFallback() {
  return (
    <nav className="header-nav" aria-label="主导航">
      <Link href="/">灵感模板</Link>
      <Link href="/tasks">我的任务</Link>
      <span className="nav-note">把日常做成作品</span>
    </nav>
  );
}
