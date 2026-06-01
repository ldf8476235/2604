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

export function getAuthToken(): string | null {
  if (typeof window === "undefined") {
    return null;
  }

  const localToken = window.localStorage.getItem(TOKEN_KEY);
  if (localToken) {
    return localToken;
  }

  const cookieMatch = document.cookie
    .split("; ")
    .find((item) => item.startsWith(`${COOKIE_NAME}=`));
  if (!cookieMatch) {
    return null;
  }
  return decodeURIComponent(cookieMatch.split("=").slice(1).join("="));
}

export function loadAuthSession(): AuthSession | null {
  if (typeof window === "undefined") {
    return null;
  }

  const raw = window.localStorage.getItem(SESSION_KEY);
  if (!raw) {
    return null;
  }

  try {
    return JSON.parse(raw) as AuthSession;
  } catch {
    clearAuthSession();
    return null;
  }
}

export function persistAuthSession(session: AuthSession) {
  if (typeof window === "undefined") {
    return;
  }

  window.localStorage.setItem(SESSION_KEY, JSON.stringify(session));
  window.localStorage.setItem(TOKEN_KEY, session.token);
  document.cookie = `${COOKIE_NAME}=${encodeURIComponent(session.token)}; Max-Age=${session.expireDays * 24 * 60 * 60}; Path=/; SameSite=Lax`;
}

export function clearAuthSession() {
  if (typeof window === "undefined") {
    return;
  }

  window.localStorage.removeItem(SESSION_KEY);
  window.localStorage.removeItem(TOKEN_KEY);
  document.cookie = `${COOKIE_NAME}=; Max-Age=0; Path=/; SameSite=Lax`;
}
