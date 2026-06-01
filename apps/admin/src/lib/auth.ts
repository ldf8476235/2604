export type AuthProfile = {
  userId: number;
  nickname: string;
  phone: string;
  verified: boolean;
  hasPassword: boolean;
};

export type AuthSession = {
  token: string;
  expireDays: number;
  profile: AuthProfile;
};

const TOKEN_KEY = "delta_trade_token";
const SESSION_KEY = "delta_trade_session";
const COOKIE_NAME = "delta_trade_token";

export async function loginByPassword(phone: string, password: string) {
  const response = await fetch("/api/auth/password-login", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ phone, password }),
  });
  const payload = await response.json();
  if (!response.ok || !payload?.success) {
    throw new Error(payload?.message || "登录失败");
  }
  const session = payload.data as AuthSession;
  persistAuthSession(session);
  return session;
}

export async function logout() {
  try {
    await fetch("/api/auth/logout", {
      method: "POST",
      credentials: "include",
      headers: buildHeaders(),
    });
  } finally {
    clearAuthSession();
  }
}

export function persistAuthSession(session: AuthSession) {
  window.localStorage.setItem(SESSION_KEY, JSON.stringify(session));
  window.localStorage.setItem(TOKEN_KEY, session.token);
  document.cookie = `${COOKIE_NAME}=${encodeURIComponent(session.token)}; Max-Age=${session.expireDays * 24 * 60 * 60}; Path=/; SameSite=Lax`;
}

export function clearAuthSession() {
  window.localStorage.removeItem(SESSION_KEY);
  window.localStorage.removeItem(TOKEN_KEY);
  document.cookie = `${COOKIE_NAME}=; Max-Age=0; Path=/; SameSite=Lax`;
}

export function hasAuthToken() {
  return Boolean(readAuthToken());
}

function readAuthToken() {
  const localToken = window.localStorage.getItem(TOKEN_KEY);
  if (localToken) {
    return localToken;
  }
  const cookieMatch = document.cookie.split("; ").find((item) => item.startsWith(`${COOKIE_NAME}=`));
  return cookieMatch ? decodeURIComponent(cookieMatch.split("=").slice(1).join("=")) : null;
}

function buildHeaders() {
  const headers = new Headers();
  const token = readAuthToken();
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  return headers;
}
