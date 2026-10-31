import { Suspense } from "react";
import Link from "next/link";
import { SiteHeaderNav, SiteHeaderNavFallback } from "@/components/site-header-nav";

export function SiteHeader() {
  return (
    <header className="site-header">
      <div className="header-inner">
        <Link className="brand" href="/" aria-label="拾光，返回模板首页">
          <span className="brand-mark" aria-hidden="true">
            <svg viewBox="0 0 32 32" fill="none">
              <path d="M16 3.5 28.5 16 16 28.5 3.5 16 16 3.5Z" fill="currentColor" />
              <path d="M16 9v14M9 16h14" stroke="#F8F7F2" strokeWidth="2.2" strokeLinecap="round" />
            </svg>
          </span>
          <span className="brand-word">拾光<span>STUDIO</span></span>
        </Link>
        <Suspense fallback={<SiteHeaderNavFallback />}>
          <SiteHeaderNav />
        </Suspense>
      </div>
    </header>
  );
}
