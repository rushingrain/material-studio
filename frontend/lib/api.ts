import type { ApiResponse } from "./types";

export class ApiError extends Error {
  readonly code: number;
  readonly httpStatus: number;

  constructor(message: string, code: number, httpStatus: number) {
    super(message);
    this.name = "ApiError";
    this.code = code;
    this.httpStatus = httpStatus;
  }
}

const apiBaseUrl = (process.env.NEXT_PUBLIC_API_BASE_URL ?? "http://localhost:8080").replace(/\/$/, "");

export async function apiRequest<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  const headers = new Headers(init?.headers);
  if (!headers.has("Accept")) headers.set("Accept", "application/json");

  try {
    response = await fetch(`${apiBaseUrl}${path}`, {
      ...init,
      headers,
    });
  } catch {
    throw new ApiError("无法连接服务器", -1, 0);
  }

  const payload = (await response.json().catch(() => null)) as ApiResponse<T> | null;
  if (!response.ok) {
    throw new ApiError(
      payload?.msg ?? `请求失败（HTTP ${response.status}）`,
      payload?.code ?? -1,
      response.status,
    );
  }
  if (!payload) {
    throw new ApiError("服务器返回了无效响应", -1, response.status);
  }
  if (payload.code !== 0) {
    throw new ApiError(payload.msg || "请求失败", payload.code, response.status);
  }
  return payload.data;
}
