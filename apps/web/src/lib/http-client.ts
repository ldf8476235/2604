import { clearAuthSession, getAuthToken } from "../auth/auth-storage";

type ApiEnvelope<T> = {
  success: boolean;
  code: string;
  message: string;
  data: T;
  traceId: string;
};

type RequestOptions = RequestInit & {
  auth?: boolean;
  skipUnauthorizedRedirect?: boolean;
};

export const AUTH_EXPIRED_EVENT = "delta-trade:auth-expired";

export async function request<T>(path: string, init?: RequestOptions): Promise<T> {
  const authEnabled = init?.auth !== false;
  const token = authEnabled ? getAuthToken() : null;
  const headers = new Headers(init?.headers ?? {});
  const isFormDataBody = typeof FormData !== "undefined" && init?.body instanceof FormData;

  if (!headers.has("Content-Type") && init?.body && !isFormDataBody) {
    headers.set("Content-Type", "application/json");
  }
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(path, {
    ...init,
    headers,
    credentials: "same-origin",
  });

  let payload: ApiEnvelope<T> | null = null;
  try {
    payload = (await response.json()) as ApiEnvelope<T>;
  } catch {
    if (response.status === 401) {
      handleUnauthorized(init?.skipUnauthorizedRedirect);
      throw new Error("登录已失效，请重新登录");
    }
    throw new Error("服务返回异常，请稍后再试");
  }

  if (response.status === 401 || payload.code === "UNAUTHORIZED") {
    handleUnauthorized(init?.skipUnauthorizedRedirect);
    throw new Error(payload.message || "登录已失效，请重新登录");
  }

  if (!response.ok || !payload.success) {
    throw new Error(payload.message || "请求失败");
  }

  return payload.data;
}

function handleUnauthorized(skipUnauthorizedRedirect?: boolean) {
  clearAuthSession();
  if (skipUnauthorizedRedirect || typeof window === "undefined") {
    return;
  }
  window.dispatchEvent(new CustomEvent(AUTH_EXPIRED_EVENT));
}
