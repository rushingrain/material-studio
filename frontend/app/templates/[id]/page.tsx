import { Suspense } from "react";
import { TemplateDetail, TemplateDetailSkeleton } from "@/components/template-detail";

export default function TemplatePage() {
  return (
    <main className="page-shell detail-page-shell" id="main-content" tabIndex={-1}>
      <Suspense fallback={<TemplateDetailSkeleton />}>
        <TemplateDetail />
      </Suspense>
      <footer className="site-footer detail-footer">
        <span>拾光 STUDIO</span>
        <span>为每一个值得记住的瞬间。</span>
      </footer>
    </main>
  );
}
