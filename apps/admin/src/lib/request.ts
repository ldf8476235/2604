type ApiEnvelope<T> = {
  success: boolean;
  code: string;
  message: string;
  data: T;
  traceId: string;
};

export async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const headers = new Headers(init?.headers ?? {});
  const token = readCookieToken();
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  if (init?.body && !(init.body instanceof FormData) && !headers.has("Content-Type")) {
    headers.set("Content-Type", "application/json");
  }
  const response = await fetch(path, {
    ...init,
    headers,
    credentials: "include",
  });
  let payload: ApiEnvelope<T> | null = null;
  try {
    payload = (await response.json()) as ApiEnvelope<T>;
  } catch (error) {
    throw new Error(response.status === 401 ? "登录已失效，请先在用户端重新登录" : "服务返回异常");
  }
  if (response.status === 401 || !payload.success) {
    throw new Error(payload.message || "请求失败");
  }
  return payload.data;
}

function readCookieToken() {
  if (typeof document === "undefined") {
    return null;
  }
  const match = document.cookie.split("; ").find((item) => item.startsWith("delta_trade_token="));
  return match ? decodeURIComponent(match.split("=").slice(1).join("=")) : null;
}
