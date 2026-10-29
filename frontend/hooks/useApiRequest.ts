"use client";

import { useCallback, useState } from "react";
import { ApiError } from "@/lib/api";
import type { ApiState } from "@/lib/types";

export function useApiRequest<T>(request: () => Promise<T>) {
  const [state, setState] = useState<ApiState<T>>({ status: "idle" });

  const run = useCallback(async () => {
    setState({ status: "loading" });
    try {
      const data = await request();
      setState({ status: "success", data });
      return data;
    } catch (cause) {
      const error = cause instanceof ApiError
        ? cause
        : new ApiError(cause instanceof Error ? cause.message : "请求失败", -1, 0);
      setState({ status: "error", error });
      return undefined;
    }
  }, [request]);

  const reset = useCallback(() => setState({ status: "idle" }), []);

  return { state, run, reset };
}
