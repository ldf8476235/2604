import { createContext, useContext, type ReactNode, useEffect, useMemo, useState } from "react";
import { AuthModal } from "./AuthModal";
import { clearAuthSession, loadAuthSession, persistAuthSession, type AuthSession } from "./auth-storage";
import { AUTH_EXPIRED_EVENT } from "../lib/http-client";
import { logout as logoutRequest } from "./auth-api";

type AuthEntry = "login" | "register" | "reset";

type AuthContextValue = {
  session: AuthSession | null;
  isAuthenticated: boolean;
  openAuthModal: (entry?: AuthEntry) => void;
  closeAuthModal: () => void;
  saveSession: (session: AuthSession) => void;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<AuthSession | null>(() => loadAuthSession());
  const [modalEntry, setModalEntry] = useState<AuthEntry>("login");
  const [modalOpen, setModalOpen] = useState(false);
  const [pendingWechatBindToken, setPendingWechatBindToken] = useState("");

  const saveSession = (nextSession: AuthSession) => {
    persistAuthSession(nextSession);
    setSession(nextSession);
    setModalOpen(false);
  };

  useEffect(() => {
    const handleAuthExpired = () => {
      clearAuthSession();
      setSession(null);
      setModalEntry("login");
      setModalOpen(true);
    };
    window.addEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired);
    return () => window.removeEventListener(AUTH_EXPIRED_EVENT, handleAuthExpired);
  }, []);

  useEffect(() => {
    if (typeof window === "undefined") {
      return;
    }
    const url = new URL(window.location.href);
    const shouldBind = url.searchParams.get("wechat_bind");
    const bindToken = url.searchParams.get("bind_token");
    if (shouldBind !== "1" || !bindToken) {
      return;
    }
    clearAuthSession();
    setSession(null);
    setPendingWechatBindToken(bindToken);
    setModalEntry("login");
    setModalOpen(true);
    url.searchParams.delete("wechat_bind");
    url.searchParams.delete("bind_token");
    window.history.replaceState({}, document.title, `${url.pathname}${url.search}${url.hash}`);
  }, []);

  const logout = async () => {
    try {
      await logoutRequest();
    } catch {
      // 登录态失效时，前端仍需保证本地状态被清空。
    }
    clearAuthSession();
    setSession(null);
    setModalEntry("login");
    setModalOpen(true);
  };

  const value = useMemo<AuthContextValue>(
    () => ({
      session,
      isAuthenticated: Boolean(session?.token),
      openAuthModal: (entry = "login") => {
        setModalEntry(entry);
        setModalOpen(true);
      },
      closeAuthModal: () => setModalOpen(false),
      saveSession,
      logout,
    }),
    [session],
  );

  return (
    <AuthContext.Provider value={value}>
      {children}
      <AuthModal
        entry={modalEntry}
        onClose={() => {
          setModalOpen(false);
          setPendingWechatBindToken("");
        }}
        onLoginSuccess={(nextSession) => {
          setPendingWechatBindToken("");
          saveSession(nextSession);
        }}
        pendingWechatBindToken={pendingWechatBindToken}
        open={modalOpen}
      />
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth 必须在 AuthProvider 内使用");
  }
  return context;
}
