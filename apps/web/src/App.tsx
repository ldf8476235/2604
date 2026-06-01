import { useEffect, useRef, useState } from "react";
import { Link, NavLink, Outlet, useLocation, useNavigate } from "react-router-dom";
import { loadRealNameProfile } from "./auth/auth-api";
import { useAuth } from "./auth/auth-context";
import { loadLatestConversation } from "./modules/im/im-api";
import { loadNotificationSummary, type NotificationItem, type NotificationSummary } from "./modules/profile/profile-api";

const MARKET_NOTICE =
  "Q 群私聊添加你出号、买号的都是骗子。平台 24 小时营业，账号交易、代肝和客服沟通统一走站内流程。";
const NOTIFICATION_REFRESH_EVENT = "dt:notifications-refresh";

function isSaleNotificationToast(item: NotificationItem) {
  return item.title === "账号售出成功" || item.title === "账号被购买";
}

export function App() {
  const { isAuthenticated, logout, openAuthModal, saveSession, session } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const [comingSoonOpen, setComingSoonOpen] = useState(false);
  const [comingSoonTitle, setComingSoonTitle] = useState("");
  const [appNotice, setAppNotice] = useState("");
  const [notificationSummary, setNotificationSummary] = useState<NotificationSummary | null>(null);
  const [activeNotification, setActiveNotification] = useState<NotificationItem | null>(null);
  const lastNotificationIdRef = useRef(window.sessionStorage.getItem("dt:last-notification-id") ?? "");
  const isImRoute = location.pathname.startsWith("/im");
  const hasUnreadIm = Boolean(notificationSummary && notificationSummary.imUnread > 0);

  const handleProtectedEntry = (target = "/profile") => {
    if (isAuthenticated) {
      window.location.href = target;
      return;
    }
    openAuthModal("login");
  };

  const syncVerifiedSession = () => {
    if (!session || session.profile.verified) {
      return;
    }
    saveSession({
      ...session,
      profile: {
        ...session.profile,
        verified: true,
      },
    });
  };

  const handlePublishEntry = async () => {
    if (!isAuthenticated) {
      openAuthModal("login");
      return;
    }
    if (session?.profile.verified) {
      navigate("/publish");
      return;
    }
    try {
      const realName = await loadRealNameProfile();
      if (realName.verified) {
        syncVerifiedSession();
        navigate("/publish");
        return;
      }
    } catch {
      setAppNotice("实名状态校验失败，请稍后再试");
      return;
    }
    if (!session?.profile.verified) {
      setAppNotice("未实名不允许发布账号");
      navigate("/profile?tab=verify");
      return;
    }
  };

  const handleComingSoon = (title: string) => {
    setComingSoonTitle(title);
    setComingSoonOpen(true);
  };

  const handleImEntry = async () => {
    if (!isAuthenticated) {
      openAuthModal("login");
      return;
    }
    try {
      const latest = await loadLatestConversation();
      navigate(latest.conversationNo ? `/im/${latest.conversationNo}` : "/profile?tab=messages");
    } catch {
      navigate("/profile?tab=messages");
    }
  };

  useEffect(() => {
    const handler = (event: Event) => {
      const customEvent = event as CustomEvent<string>;
      handleComingSoon(customEvent.detail || "敬请期待");
    };
    window.addEventListener("dt:coming-soon", handler as EventListener);
    return () => window.removeEventListener("dt:coming-soon", handler as EventListener);
  }, []);

  useEffect(() => {
    const handler = (event: Event) => {
      const customEvent = event as CustomEvent<string>;
      setAppNotice(customEvent.detail || "操作失败，请稍后再试");
    };
    window.addEventListener("dt:app-toast", handler as EventListener);
    return () => window.removeEventListener("dt:app-toast", handler as EventListener);
  }, []);

  useEffect(() => {
    if (!appNotice) {
      return;
    }
    const timer = window.setTimeout(() => setAppNotice(""), 2200);
    return () => window.clearTimeout(timer);
  }, [appNotice]);

  useEffect(() => {
    if (!isAuthenticated) {
      setNotificationSummary(null);
      setActiveNotification(null);
      return;
    }
    let disposed = false;
    let initialized = false;

    const refresh = async () => {
      try {
        const summary = await loadNotificationSummary();
        if (disposed) {
          return;
        }
        setNotificationSummary(summary);
        const latest = summary.latest;
        const isCurrentIm = latest?.conversationNo && location.pathname === `/im/${latest.conversationNo}`;
        if (latest && latest.id !== lastNotificationIdRef.current && !isCurrentIm) {
          if (initialized || isSaleNotificationToast(latest)) {
            setActiveNotification(latest);
          }
          lastNotificationIdRef.current = latest.id;
          window.sessionStorage.setItem("dt:last-notification-id", latest.id);
        }
        initialized = true;
      } catch {
        if (!disposed) {
          setNotificationSummary(null);
        }
      }
    };

    void refresh();
    const refreshHandler = () => void refresh();
    window.addEventListener(NOTIFICATION_REFRESH_EVENT, refreshHandler);
    const timer = window.setInterval(() => void refresh(), 15000);
    return () => {
      disposed = true;
      window.removeEventListener(NOTIFICATION_REFRESH_EVENT, refreshHandler);
      window.clearInterval(timer);
    };
  }, [isAuthenticated, location.pathname]);

  useEffect(() => {
    if (!activeNotification) {
      return;
    }
    if (isSaleNotificationToast(activeNotification)) {
      return;
    }
    const timer = window.setTimeout(() => setActiveNotification(null), 8000);
    return () => window.clearTimeout(timer);
  }, [activeNotification]);

  return (
    <div className={`dt-page-shell dt-page-shell--market ${isImRoute ? "dt-page-shell--im" : ""}`}>
      <div className="notice-strip">
        <div className="dt-container notice-strip__inner">
          <strong className="notice-strip__label">温馨提示</strong>
          <div className="notice-strip__viewport">
            <div className="notice-strip__track">
              <div className="notice-strip__group">
                <span className="notice-strip__message">{MARKET_NOTICE}</span>
                <span className="notice-strip__dot" aria-hidden="true">
                  ·
                </span>
              </div>
              <div aria-hidden="true" className="notice-strip__group">
                <span className="notice-strip__message">{MARKET_NOTICE}</span>
                <span className="notice-strip__dot">·</span>
              </div>
            </div>
          </div>
        </div>
      </div>
      <header className="dt-topbar">
        <div className="dt-container dt-topbar__inner">
          <div className="dt-brand">
            <span className="dt-brand__mark dt-brand__mark--image">
              <img alt="萌虎" src="/brand/menghu-ai-logo.png" />
            </span>
            <div className="dt-brand__text">
              <small>账号交易 / 代肝 / 改枪码</small>
            </div>
          </div>
          <nav className="dt-nav" aria-label="主导航">
            <NavLink className="dt-nav__link" end to="/">
              资源号租赁
            </NavLink>
            <button className="dt-nav__link dt-nav__link--button" onClick={() => handleComingSoon("账号交易")} type="button">
              账号交易
            </button>
            <NavLink className="dt-nav__link" to="/gun-code">
              改枪码
            </NavLink>
            <NavLink className="dt-nav__link" to="/boosting">
              代肝
            </NavLink>
            <button className="dt-nav__link dt-nav__link--button" onClick={() => handleComingSoon("陪练大厅")} type="button">
              陪练大厅
            </button>
            <Link className="dt-nav__link" to="/profile?tab=officialNotice">
              官方公告
            </Link>
          </nav>
          <div className="header-actions">
            {isAuthenticated ? (
              <>
                <span className="header-user" title={session?.profile.phone}>
                  {session?.profile.nickname}
                </span>
                <button className="header-link header-link--button" onClick={() => void logout()} type="button">
                  退出
                </button>
              </>
            ) : (
              <>
                <button className="header-link header-link--button" onClick={() => openAuthModal("login")} type="button">
                  登录
                </button>
                <button className="header-link header-link--button" onClick={() => openAuthModal("register")} type="button">
                  注册
                </button>
              </>
            )}
            {isAuthenticated ? (
              <Link className="header-link" to="/profile">
                个人中心
              </Link>
            ) : (
              <button className="header-link header-link--button" onClick={() => openAuthModal("login")} type="button">
                个人中心
              </button>
            )}
            <button
              className={`header-link header-link--button header-link--im ${hasUnreadIm ? "has-unread" : ""}`}
              onClick={() => void handleImEntry()}
              type="button"
            >
              消息
              {hasUnreadIm ? <span className="header-link__dot" aria-label="有未读 IM 消息" /> : null}
            </button>
            <button className="header-publish header-publish--button" onClick={() => void handlePublishEntry()} type="button">
              账号发布
            </button>
          </div>
        </div>
      </header>
      <Outlet />
      <nav className="mobile-tabbar" aria-label="移动端底部导航">
        <NavLink className={({ isActive }) => `mobile-tabbar__item ${isActive ? "is-active" : ""}`} end to="/">
          <span className="mobile-tabbar__icon" aria-hidden="true">
            ⌂
          </span>
          <span>主页</span>
        </NavLink>
        <a className="mobile-tabbar__item" href="/#market">
          <span className="mobile-tabbar__icon" aria-hidden="true">
            ○
          </span>
          <span>热点</span>
        </a>
        <button className={`mobile-tabbar__item mobile-tabbar__item--publish ${location.pathname.startsWith("/publish") ? "is-active" : ""}`} onClick={() => void handlePublishEntry()} type="button">
          <span className="mobile-tabbar__icon" aria-hidden="true">
            ＋
          </span>
          <span>出租发布</span>
        </button>
        <button className={`mobile-tabbar__item ${location.pathname.startsWith("/im") ? "is-active" : ""}`} onClick={() => void handleImEntry()} type="button">
          <span className="mobile-tabbar__icon" aria-hidden="true">
            ☰
          </span>
          {hasUnreadIm ? <span className="mobile-tabbar__dot" aria-label="有未读 IM 消息" /> : null}
          <span>消息</span>
        </button>
        {isAuthenticated ? (
          <Link className={`mobile-tabbar__item ${location.pathname.startsWith("/profile") ? "is-active" : ""}`} to="/profile">
            <span className="mobile-tabbar__icon" aria-hidden="true">
              ◌
            </span>
            <span>个人中心</span>
          </Link>
        ) : (
          <button className="mobile-tabbar__item" onClick={() => handleProtectedEntry("/profile")} type="button">
            <span className="mobile-tabbar__icon" aria-hidden="true">
              ◌
            </span>
            <span>个人中心</span>
          </button>
        )}
      </nav>
      {comingSoonOpen ? (
        <div aria-labelledby="coming-soon-title" aria-modal="true" className="coming-soon-dialog" role="dialog">
          <button aria-label="关闭敬请期待弹层" className="coming-soon-dialog__backdrop" onClick={() => setComingSoonOpen(false)} type="button" />
          <section className="coming-soon-dialog__panel">
            <header className="coming-soon-dialog__header">
              <p className="coming-soon-dialog__eyebrow">功能预告</p>
              <h3 id="coming-soon-title">{comingSoonTitle}</h3>
            </header>
            <div className="coming-soon-dialog__body">
              <p>敬请期待</p>
              <span>该模块正在开发中，后续会上线完整功能与交互流程。</span>
            </div>
            <div className="coming-soon-dialog__footer">
              <button className="dt-button dt-button--primary" onClick={() => setComingSoonOpen(false)} type="button">
                我知道了
              </button>
            </div>
          </section>
        </div>
      ) : null}
      {activeNotification ? (
        <div
          aria-live={isSaleNotificationToast(activeNotification) ? "assertive" : "polite"}
          className={`global-notification-toast ${
            isSaleNotificationToast(activeNotification) ? "global-notification-toast--sale" : ""
          }`}
          role="status"
        >
          {isSaleNotificationToast(activeNotification) ? (
            <button
              aria-label="关闭售出提醒"
              className="global-notification-toast__close"
              onClick={() => setActiveNotification(null)}
              type="button"
            >
              ×
            </button>
          ) : null}
          <span>{activeNotification.label}</span>
          <strong>{activeNotification.title}</strong>
          <small>
            {isSaleNotificationToast(activeNotification)
              ? "账号已售出，请进入群聊进行交接。"
              : activeNotification.content}
          </small>
	          <button
	            className="global-notification-toast__action"
	            onClick={() => {
	              const target = activeNotification.targetUrl || "/profile?tab=messages";
	              setActiveNotification(null);
	              navigate(target);
	            }}
	            type="button"
	          >
	            {activeNotification.conversationNo ? "进入群聊" : "查看消息"}
	          </button>
	        </div>
      ) : null}
      {appNotice ? (
        <div className="app-toast" role="status" aria-live="polite">
          <span>{appNotice}</span>
          <button onClick={() => setAppNotice("")} type="button">关闭</button>
        </div>
      ) : null}
    </div>
  );
}
