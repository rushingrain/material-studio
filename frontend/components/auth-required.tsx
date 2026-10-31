import Link from "next/link";

export function AuthRequired({
  nextPath,
  description = "登录后即可访问个人任务和制作结果。",
}: {
  nextPath: string;
  description?: string;
}) {
  return (
    <section className="feedback-card auth-required" role="status">
      <span className="feedback-icon feedback-icon-empty" aria-hidden="true">钥</span>
      <h2>登录后继续</h2>
      <p>{description}</p>
      <Link
        className="button button-dark"
        href={`/login?next=${encodeURIComponent(nextPath)}`}
      >
        前往登录
      </Link>
    </section>
  );
}
