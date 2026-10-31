import { TemplateGallery } from "@/components/template-gallery";

export default function Home() {
  return (
    <main className="page-shell" id="main-content" tabIndex={-1}>
      <section className="hero-section">
        <div className="hero-copy">
          <p className="eyebrow"><span className="eyebrow-dot" /> 灵感，从一张照片开始</p>
          <h1>让每一份日常<br /><span>都值得被珍藏。</span></h1>
          <p className="hero-description">挑选喜欢的创作灵感，上传照片，把平凡瞬间变成属于你的作品。</p>
          <a className="hero-link" href="#templates">探索模板 <span aria-hidden="true">↓</span></a>
        </div>
        <div className="hero-art" aria-label="几何风格的创作灵感插画" role="img">
          <div className="art-sun" />
          <div className="art-frame art-frame-back"><span /></div>
          <div className="art-frame art-frame-front">
            <div className="art-mountain art-mountain-back" />
            <div className="art-mountain art-mountain-front" />
            <div className="art-water" />
          </div>
          <div className="art-sparkle sparkle-one">✳</div>
          <div className="art-sparkle sparkle-two">✳</div>
          <div className="art-caption">A LITTLE MOMENT<br /><span>NO. 04 — AUTUMN LIGHT</span></div>
        </div>
        <div className="hero-index" aria-hidden="true"><span>01</span> / 04 <i /></div>
      </section>

      <section className="templates-section" id="templates">
        <div className="section-heading">
          <div>
            <p className="eyebrow">从灵感到作品</p>
            <h2>选一个，开始你的故事。</h2>
          </div>
          <p className="section-aside">每一份灵感，都为真实生活而来。</p>
        </div>
        <TemplateGallery />
      </section>
      <footer className="site-footer">
        <span>拾光 STUDIO</span>
        <span>为每一个值得记住的瞬间。</span>
      </footer>
    </main>
  );
}
