export function taskScopeKey(userId: number | null | undefined): string {
  return userId ? `user:${userId}` : "anonymous";
}
