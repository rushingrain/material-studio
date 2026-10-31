"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useState,
  type ReactNode,
} from "react";
import {
  ApiError,
  getCurrentUser,
  login as loginRequest,
  logout as logoutRequest,
} from "@/lib/api";
import type { AppUser, AuthStatus, LoginCredentials } from "@/lib/types";

type AuthContextValue = {
  status: AuthStatus;
  user: AppUser | null;
  error: string | null;
  login: (credentials: LoginCredentials) => Promise<AppUser>;
  logout: () => Promise<void>;
  refresh: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("loading");
  const [user, setUser] = useState<AppUser | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [reloadKey, setReloadKey] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    let current = true;

    getCurrentUser(controller.signal)
      .then((currentUser) => {
        if (!current) return;
        setUser(currentUser);
        setError(null);
        setStatus("authenticated");
      })
      .catch((cause: unknown) => {
        if (!current) return;
        if (cause instanceof ApiError && cause.httpStatus === 401) {
          setUser(null);
          setError(null);
          setStatus("anonymous");
          return;
        }
        setUser(null);
        setError(cause instanceof Error ? cause.message : "无法确认登录状态");
        setStatus("error");
      });

    return () => {
      current = false;
      controller.abort();
    };
  }, [reloadKey]);

  useEffect(() => {
    const handleExpired = () => {
      setUser(null);
      setError(null);
      setStatus("anonymous");
    };
    window.addEventListener("material-studio:auth-expired", handleExpired);
    return () => window.removeEventListener("material-studio:auth-expired", handleExpired);
  }, []);

  const login = useCallback(async (credentials: LoginCredentials) => {
    const currentUser = await loginRequest(credentials);
    setUser(currentUser);
    setError(null);
    setStatus("authenticated");
    return currentUser;
  }, []);

  const logout = useCallback(async () => {
    await logoutRequest();
    setUser(null);
    setError(null);
    setStatus("anonymous");
  }, []);

  const refresh = useCallback(() => {
    setStatus("loading");
    setReloadKey((key) => key + 1);
  }, []);

  return (
    <AuthContext.Provider value={{ status, user, error, login, logout, refresh }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth 必须在 AuthProvider 内使用");
  }
  return context;
}
