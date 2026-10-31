import { TaskHistory } from "@/components/task-history";

export default function TasksPage() {
  return (
    <main className="page-shell history-page-shell">
      <TaskHistory />
      <footer className="site-footer history-footer">
        <span>拾光 STUDIO</span>
        <span>每一份创作，都值得被记住。</span>
      </footer>
    </main>
  );
}
