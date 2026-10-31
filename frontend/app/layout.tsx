import type { Metadata } from "next";
import { SiteHeader } from "@/components/site-header";
import "./globals.css";

export const metadata: Metadata = {
  title: "拾光 Studio · 把日常做成作品",
  description: "挑选创作灵感，上传照片，把平凡瞬间变成属于你的作品。",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html lang="zh-CN" className="h-full antialiased">
      <body className="min-h-full flex flex-col">
        <a className="skip-link" href="#main-content">跳到主要内容</a>
        <SiteHeader />
        {children}
      </body>
    </html>
  );
}
