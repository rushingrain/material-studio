import Link from "next/link";

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
        <nav className="header-nav" aria-label="主导航">
          <Link href="/" aria-current="page">灵感模板</Link>
          <span className="nav-note">把日常做成作品</span>
        </nav>
      </div>
    </header>
  );
}
