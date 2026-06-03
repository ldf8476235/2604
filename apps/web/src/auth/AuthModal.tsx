import { Button } from "@delta/ui";
import { useEffect, useMemo, useRef, useState, type FormEvent } from "react";
import {
  bindWechatPhone,
  completeRegister,
  completeResetPassword,
  createWechatQr,
  loginByPassword,
  loginBySms,
  pollWechatQr,
  sendSmsCode,
  verifyRegisterCode,
  verifyResetCode,
  type SmsScene,
  type WechatQrResult,
} from "./auth-api";
import type { AuthSession } from "./auth-storage";
import { clearPendingInviteCode, loadPendingInviteCode } from "./invite-storage";

type AuthEntry = "login" | "register" | "reset";
type LoginTab = "sms" | "password" | "wechat";
type ModalView = AuthEntry | "bindWechat";

type AuthModalProps = {
  open: boolean;
  entry: AuthEntry;
  onClose: () => void;
  onLoginSuccess: (session: AuthSession) => void;
  pendingWechatBindToken?: string;
};

type SmsFormState = { phone: string; code: string };
type PasswordFormState = { phone: string; password: string };
type RegisterState = { phone: string; code: string; verifyToken: string; password: string; confirmPassword: string; step: 1 | 2 };
type ResetState = { phone: string; code: string; verifyToken: string; password: string; confirmPassword: string; step: 1 | 2 };
type BindState = { phone: string; code: string; bindToken: string };

const PHONE_REGEXP = /^1\d{10}$/;
const PASSWORD_REGEXP = /^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d]{6,18}$/;

const INITIAL_SMS_FORM: SmsFormState = { phone: "", code: "" };
const INITIAL_PASSWORD_FORM: PasswordFormState = { phone: "", password: "" };
const INITIAL_REGISTER_FORM: RegisterState = { phone: "", code: "", verifyToken: "", password: "", confirmPassword: "", step: 1 };
const INITIAL_RESET_FORM: ResetState = { phone: "", code: "", verifyToken: "", password: "", confirmPassword: "", step: 1 };
const INITIAL_BIND_FORM: BindState = { phone: "", code: "", bindToken: "" };

const LOGIN_MODE_DESCRIPTIONS: Record<LoginTab, { title: string; caption: string }> = {
  sms: { title: "短信登录", caption: "适合快速回到交易、代肝和客服场景" },
  password: { title: "账号密码", caption: "适合常用设备和稳定账号切换" },
  wechat: { title: "微信扫码登录", caption: "适合 PC 端快速授权，扫码后自动轮询状态" },
};

export function AuthModal({
  open,
  entry,
  onClose,
  onLoginSuccess,
  pendingWechatBindToken,
}: AuthModalProps) {
  const dialogRef = useRef<HTMLDivElement | null>(null);
  const [view, setView] = useState<ModalView>(entry);
  const [loginTab, setLoginTab] = useState<LoginTab>("sms");
  const [smsLoginForm, setSmsLoginForm] = useState<SmsFormState>(INITIAL_SMS_FORM);
  const [passwordForm, setPasswordForm] = useState<PasswordFormState>(INITIAL_PASSWORD_FORM);
  const [registerForm, setRegisterForm] = useState<RegisterState>(INITIAL_REGISTER_FORM);
  const [resetForm, setResetForm] = useState<ResetState>(INITIAL_RESET_FORM);
  const [bindForm, setBindForm] = useState<BindState>(INITIAL_BIND_FORM);
  const [cooldowns, setCooldowns] = useState<Record<string, number>>({ login: 0, register: 0, reset: 0, bind: 0 });
  const [loadingKey, setLoadingKey] = useState<string | null>(null);
  const [errorMessage, setErrorMessage] = useState("");
  const [noticeMessage, setNoticeMessage] = useState("");
  const [pendingInviteCode, setPendingInviteCode] = useState("");
  const [wechatQr, setWechatQr] = useState<WechatQrResult | null>(null);
  const [wechatStatus, setWechatStatus] = useState<"idle" | "loading" | "waiting" | "expired">("idle");
  const [wechatRefreshKey, setWechatRefreshKey] = useState(0);

  useEffect(() => {
    if (!open) {
      return;
    }
    if (pendingWechatBindToken) {
      setView("bindWechat");
      setBindForm({ ...INITIAL_BIND_FORM, bindToken: pendingWechatBindToken });
      setNoticeMessage("微信授权已完成，请补齐手机号并完成绑定。");
    } else {
      setView(entry);
      setBindForm(INITIAL_BIND_FORM);
    }
    setLoginTab("sms");
    setSmsLoginForm(INITIAL_SMS_FORM);
    setPasswordForm(INITIAL_PASSWORD_FORM);
    setRegisterForm(INITIAL_REGISTER_FORM);
    setResetForm(INITIAL_RESET_FORM);
    setWechatQr(null);
    setWechatStatus("idle");
    setErrorMessage("");
    if (!pendingWechatBindToken) {
      setNoticeMessage("");
    }
    setPendingInviteCode(loadPendingInviteCode());
    dialogRef.current?.focus();
  }, [entry, open, pendingWechatBindToken]);

  useEffect(() => {
    if (!open) {
      return;
    }
    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === "Escape") {
        onClose();
      }
    };
    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    window.addEventListener("keydown", handleEscape);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener("keydown", handleEscape);
    };
  }, [onClose, open]);

  useEffect(() => {
    if (!open) {
      return;
    }
    const interval = window.setInterval(() => {
      setCooldowns((previous) => {
        const next: Record<string, number> = {};
        const keys = Object.keys(previous);
        for (const key of keys) {
          next[key] = previous[key] > 0 ? previous[key] - 1 : 0;
        }
        return next;
      });
    }, 1000);
    return () => window.clearInterval(interval);
  }, [open]);

  useEffect(() => {
    if (!open || view !== "login" || loginTab !== "wechat") {
      return;
    }

    let disposed = false;
    let pollingTimer: number | null = null;

    async function bootstrapWechatQr() {
      try {
        setWechatStatus("loading");
        const isWechatInternal = typeof navigator !== "undefined" && /MicroMessenger/i.test(navigator.userAgent);
        const qr = await createWechatQr({
          clientMode: isWechatInternal ? "WECHAT_INTERNAL" : "QR",
          returnPath: typeof window === "undefined" ? "/" : `${window.location.pathname}${window.location.search}${window.location.hash}`,
        });
        if (disposed) {
          return;
        }
        if (isWechatInternal && qr.authorizeUrl) {
          setNoticeMessage("正在跳转微信授权...");
          window.location.assign(qr.authorizeUrl);
          return;
        }
        setWechatQr(qr);
        setWechatStatus("waiting");
        setNoticeMessage("二维码 5 分钟有效，请使用已绑定微信的手机扫码确认。");
        pollingTimer = window.setInterval(async () => {
          try {
            const result = await pollWechatQr(qr.sceneId);
            if (disposed) {
              return;
            }
            if (result.status === "AUTHORIZED_BOUND" && result.loginResult) {
              onLoginSuccess(result.loginResult);
              return;
            }
            if (result.status === "AUTHORIZED_UNBOUND" && result.bindToken) {
              const bindToken = result.bindToken;
              setBindForm((previous) => ({ ...previous, bindToken }));
              setView("bindWechat");
              setNoticeMessage("检测到该微信尚未绑定手机号，请完成短信校验后继续。");
              if (pollingTimer) {
                window.clearInterval(pollingTimer);
              }
              return;
            }
            if (result.status === "EXPIRED") {
              setWechatStatus("expired");
              setNoticeMessage("二维码已过期，请刷新二维码。");
              if (pollingTimer) {
                window.clearInterval(pollingTimer);
              }
            }
          } catch (error) {
            if (!disposed) {
              setWechatStatus("expired");
              setErrorMessage(getErrorMessage(error));
              if (pollingTimer) {
                window.clearInterval(pollingTimer);
              }
            }
          }
        }, qr.pollIntervalSeconds * 1000);
      } catch (error) {
        if (!disposed) {
          setWechatStatus("expired");
          setErrorMessage(getErrorMessage(error));
        }
      }
    }

    bootstrapWechatQr();

    return () => {
      disposed = true;
      if (pollingTimer) {
        window.clearInterval(pollingTimer);
      }
    };
  }, [loginTab, onLoginSuccess, open, view, wechatRefreshKey]);

  const modalTitle = useMemo(() => {
    if (view === "register") {
      return "手机号注册";
    }
    if (view === "reset") {
      return "找回密码";
    }
    if (view === "bindWechat") {
      return "绑定手机号";
    }
    return "登录 / 注册";
  }, [view]);

  const modalDescription = useMemo(() => {
    if (view === "register") {
      return registerForm.step === 1
        ? "先完成手机号校验，再设置登录密码。整个流程不会跳离当前页面。"
        : "手机号已校验通过，现在设置一组可长期使用的登录密码。";
    }
    if (view === "reset") {
      return resetForm.step === 1
        ? "通过绑定手机号验证身份，确认后立即进入新密码设置。"
        : "输入新的登录密码并确认，保存后可直接回到密码登录。";
    }
    if (view === "bindWechat") {
      return "该微信尚未和平台手机号绑定。补齐短信校验后，本次授权会直接完成登录。";
    }
    return LOGIN_MODE_DESCRIPTIONS[loginTab].caption;
  }, [loginTab, registerForm.step, resetForm.step, view]);

  if (!open) {
    return null;
  }

  const sendCode = async (scene: SmsScene, phone: string, key: "login" | "register" | "reset" | "bind") => {
    if (!PHONE_REGEXP.test(phone)) {
      setErrorMessage("请输入正确的 11 位手机号");
      return;
    }

    try {
      setErrorMessage("");
      setLoadingKey(`sms-${key}`);
      const result = await sendSmsCode(phone, scene);
      setCooldowns((previous) => ({ ...previous, [key]: result.cooldownSeconds }));
      setNoticeMessage(result.hint || "验证码已发送，请注意查收");
    } catch (error) {
      const message = getErrorMessage(error);
      if (scene === "LOGIN" && message.includes("账号不存在")) {
        setView("register");
        setRegisterForm({ ...INITIAL_REGISTER_FORM, phone });
        setErrorMessage("");
        setNoticeMessage("账号不存在，请注册");
        return;
      }
      setErrorMessage(message);
    } finally {
      setLoadingKey(null);
    }
  };

  const submitSmsLogin = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!PHONE_REGEXP.test(smsLoginForm.phone)) {
      setErrorMessage("请输入正确的 11 位手机号");
      return;
    }
    if (!/^\d{6}$/.test(smsLoginForm.code)) {
      setErrorMessage("请输入 6 位短信验证码");
      return;
    }
    try {
      setLoadingKey("submit-login-sms");
      setErrorMessage("");
      const result = await loginBySms(smsLoginForm.phone, smsLoginForm.code);
      onLoginSuccess(result);
    } catch (error) {
      setErrorMessage(getErrorMessage(error));
    } finally {
      setLoadingKey(null);
    }
  };

  const submitPasswordLogin = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!PHONE_REGEXP.test(passwordForm.phone)) {
      setErrorMessage("请输入正确的 11 位手机号");
      return;
    }
    if (!passwordForm.password) {
      setErrorMessage("请输入密码");
      return;
    }
    try {
      setLoadingKey("submit-login-password");
      setErrorMessage("");
      const result = await loginByPassword(passwordForm.phone, passwordForm.password);
      onLoginSuccess(result);
    } catch (error) {
      setErrorMessage(getErrorMessage(error));
    } finally {
      setLoadingKey(null);
    }
  };

  const submitRegister = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (registerForm.step === 1) {
      if (!PHONE_REGEXP.test(registerForm.phone)) {
        setErrorMessage("请输入正确的 11 位手机号");
        return;
      }
      if (!/^\d{6}$/.test(registerForm.code)) {
        setErrorMessage("请输入 6 位短信验证码");
        return;
      }
      try {
        setLoadingKey("verify-register");
        setErrorMessage("");
        const result = await verifyRegisterCode(registerForm.phone, registerForm.code);
        setRegisterForm((previous) => ({ ...previous, verifyToken: result.verifyToken, step: 2 }));
        setNoticeMessage("手机号校验成功，请设置登录密码。");
      } catch (error) {
        setErrorMessage(getErrorMessage(error));
      } finally {
        setLoadingKey(null);
      }
      return;
    }

    if (!PASSWORD_REGEXP.test(registerForm.password)) {
      setErrorMessage("密码需为 6-18 位字母和数字组合");
      return;
    }
    if (registerForm.password !== registerForm.confirmPassword) {
      setErrorMessage("两次输入的密码不一致");
      return;
    }

    try {
      setLoadingKey("complete-register");
      setErrorMessage("");
      const result = await completeRegister(
        registerForm.phone,
        registerForm.verifyToken,
        registerForm.password,
        registerForm.confirmPassword,
        pendingInviteCode || undefined,
      );
      clearPendingInviteCode();
      onLoginSuccess(result);
    } catch (error) {
      setErrorMessage(getErrorMessage(error));
    } finally {
      setLoadingKey(null);
    }
  };

  const submitReset = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (resetForm.step === 1) {
      if (!PHONE_REGEXP.test(resetForm.phone)) {
        setErrorMessage("请输入正确的 11 位手机号");
        return;
      }
      if (!/^\d{6}$/.test(resetForm.code)) {
        setErrorMessage("请输入 6 位短信验证码");
        return;
      }
      try {
        setLoadingKey("verify-reset");
        setErrorMessage("");
        const result = await verifyResetCode(resetForm.phone, resetForm.code);
        setResetForm((previous) => ({ ...previous, verifyToken: result.verifyToken, step: 2 }));
        setNoticeMessage("验证码校验通过，请设置新密码。");
      } catch (error) {
        setErrorMessage(getErrorMessage(error));
      } finally {
        setLoadingKey(null);
      }
      return;
    }

    if (!PASSWORD_REGEXP.test(resetForm.password)) {
      setErrorMessage("新密码需为 6-18 位字母和数字组合");
      return;
    }
    if (resetForm.password !== resetForm.confirmPassword) {
      setErrorMessage("两次输入的密码不一致");
      return;
    }

    try {
      setLoadingKey("complete-reset");
      setErrorMessage("");
      const result = await completeResetPassword(
        resetForm.phone,
        resetForm.verifyToken,
        resetForm.password,
        resetForm.confirmPassword,
      );
      setNoticeMessage(result.message);
      setView("login");
      setLoginTab("password");
      setPasswordForm({ phone: resetForm.phone, password: "" });
      setResetForm(INITIAL_RESET_FORM);
    } catch (error) {
      setErrorMessage(getErrorMessage(error));
    } finally {
      setLoadingKey(null);
    }
  };

  const submitWechatBind = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (!PHONE_REGEXP.test(bindForm.phone)) {
      setErrorMessage("请输入正确的 11 位手机号");
      return;
    }
    if (!/^\d{6}$/.test(bindForm.code)) {
      setErrorMessage("请输入 6 位短信验证码");
      return;
    }
    if (!bindForm.bindToken) {
      setErrorMessage("微信绑定会话无效，请重新扫码");
      return;
    }
    try {
      setLoadingKey("wechat-bind");
      setErrorMessage("");
      const result = await bindWechatPhone(bindForm.bindToken, bindForm.phone, bindForm.code, pendingInviteCode || undefined);
      clearPendingInviteCode();
      onLoginSuccess(result);
    } catch (error) {
      setErrorMessage(getErrorMessage(error));
    } finally {
      setLoadingKey(null);
    }
  };

  const openLoginView = (tab: LoginTab = "sms") => {
    setView("login");
    setLoginTab(tab);
    setSmsLoginForm(INITIAL_SMS_FORM);
    setPasswordForm(INITIAL_PASSWORD_FORM);
    setErrorMessage("");
    setNoticeMessage("");
    if (tab !== "wechat") {
      setWechatQr(null);
      setWechatStatus("idle");
    }
  };

  const openRegisterView = () => {
    setView("register");
    setRegisterForm(INITIAL_REGISTER_FORM);
    setErrorMessage("");
    setNoticeMessage("");
  };

  const openResetView = () => {
    setView("reset");
    setResetForm(INITIAL_RESET_FORM);
    setErrorMessage("");
    setNoticeMessage("");
  };

  const refreshWechat = () => {
    setWechatQr(null);
    setWechatStatus("idle");
    setErrorMessage("");
    setNoticeMessage("");
    setLoginTab("wechat");
    setWechatRefreshKey((previous) => previous + 1);
  };

  const openWechatWindow = () => {
    if (!wechatQr?.qrCodeUrl) {
      return;
    }
    window.open(wechatQr.qrCodeUrl, "_blank", "noopener,noreferrer,width=540,height=720");
  };

  const isWechatInternal = typeof navigator !== "undefined" && /MicroMessenger/i.test(navigator.userAgent);

  return (
    <div className="auth-modal" role="presentation">
      <div className="auth-modal__overlay" onClick={onClose} />
      <div
        aria-labelledby="auth-modal-title"
        aria-modal="true"
        className="auth-modal__dialog"
        ref={dialogRef}
        role="dialog"
        tabIndex={-1}
      >
        <section className="auth-modal__panel">
          <button aria-label="关闭弹框" className="auth-modal__close auth-modal__close--floating" onClick={onClose} type="button">
            ×
          </button>

          <div className="auth-modal__welcome">
            <div className="auth-modal__welcome-copy">
              <span>欢迎！</span>
              <strong>Hi</strong>
            </div>
          </div>

          <div className="auth-modal__header">
            <div>
              <h3 id="auth-modal-title">{view === "login" ? LOGIN_MODE_DESCRIPTIONS[loginTab].title : modalTitle}</h3>
              {view === "login" ? (
                <p className="auth-modal__description">
                  还不是用户？
                  {" "}
                  <button className="auth-modal__inline-link" onClick={openRegisterView} type="button">
                    立即注册
                  </button>
                </p>
              ) : (
                <p className="auth-modal__description">{modalDescription}</p>
              )}
            </div>
          </div>

          {errorMessage ? <div className="auth-feedback auth-feedback--error">{errorMessage}</div> : null}
          {noticeMessage ? <div className="auth-feedback auth-feedback--info">{noticeMessage}</div> : null}

          {(view === "register" || view === "reset") ? (
            <div className="auth-progress">
              <div className={`auth-progress__step ${((view === "register" ? registerForm.step : resetForm.step) === 1) ? "is-active" : "is-complete"}`}>
                <span>1</span>
                <strong>手机号校验</strong>
              </div>
              <div className={`auth-progress__divider ${((view === "register" ? registerForm.step : resetForm.step) === 2) ? "is-active" : ""}`} />
              <div className={`auth-progress__step ${((view === "register" ? registerForm.step : resetForm.step) === 2) ? "is-active" : ""}`}>
                <span>2</span>
                <strong>{view === "register" ? "设置密码" : "重置密码"}</strong>
              </div>
            </div>
          ) : null}

          {view === "login" ? (
            <section className="auth-modal__body">
              {loginTab === "sms" ? (
                <form className="dt-form-grid auth-form" onSubmit={submitSmsLogin}>
                  <AuthField
                    label="手机号"
                    onChange={(value) => setSmsLoginForm((previous) => ({ ...previous, phone: value }))}
                    placeholder="请输入 11 位手机号"
                    value={smsLoginForm.phone}
                  />
                  <div className="code-row auth-code-row">
                    <AuthField
                      label="短信验证码"
                      onChange={(value) => setSmsLoginForm((previous) => ({ ...previous, code: value }))}
                      placeholder="请输入 6 位验证码"
                      value={smsLoginForm.code}
                    />
                    <Button
                      disabled={cooldowns.login > 0 || loadingKey === "sms-login"}
                      kind="secondary"
                      onClick={() => sendCode("LOGIN", smsLoginForm.phone, "login")}
                    >
                      {cooldowns.login > 0 ? `${cooldowns.login}s` : "获取验证码"}
                    </Button>
                  </div>
                  <Button className="auth-login-submit" fullWidth type="submit">
                    登录
                  </Button>
                </form>
              ) : null}

              {loginTab === "password" ? (
                <form className="dt-form-grid auth-form" onSubmit={submitPasswordLogin}>
                  <AuthField
                    label="手机号"
                    onChange={(value) => setPasswordForm((previous) => ({ ...previous, phone: value }))}
                    placeholder="请输入 11 位手机号"
                    value={passwordForm.phone}
                  />
                  <AuthField
                    label="登录密码"
                    onChange={(value) => setPasswordForm((previous) => ({ ...previous, password: value }))}
                    placeholder="请输入 6-18 位字母+数字组合"
                    type="password"
                    value={passwordForm.password}
                  />
                  <Button className="auth-login-submit" fullWidth type="submit">
                    登录
                  </Button>
                </form>
              ) : null}

              {loginTab === "wechat" ? (
                <div className="wechat-box auth-wechat-panel">
                  <div className="wechat-box__code auth-wechat-panel__code">
                    {isWechatInternal && wechatStatus === "loading" ? (
                      <div aria-label="跳转微信授权中" className="auth-wechat-panel__placeholder" role="status">
                        <span aria-hidden="true" className="auth-wechat-panel__placeholder-loader" />
                      </div>
                    ) : wechatQr?.qrCodeUrl ? (
                      <iframe
                        allow="fullscreen"
                        className="auth-wechat-panel__frame"
                        src={wechatQr.qrCodeUrl}
                        title="微信扫码登录"
                      />
                    ) : (
                      <div
                        aria-label={wechatStatus === "loading" ? "生成二维码中" : "等待生成二维码"}
                        className="auth-wechat-panel__placeholder"
                        role="status"
                      >
                        <span aria-hidden="true" className="auth-wechat-panel__placeholder-loader" />
                      </div>
                    )}
                  </div>
                  <div className="auth-wechat-panel__meta auth-wechat-panel__meta--compact">
                    <span>{isWechatInternal ? "微信内将直接拉起公众号授权，授权后自动返回当前页面。" : getWechatExpireText(wechatStatus, wechatQr?.expireAt)}</span>
                  </div>
                  <div className="auth-wechat-panel__actions auth-wechat-panel__actions--compact">
                    <span className="auth-wechat-panel__hint">{isWechatInternal ? "没有自动跳转？" : "无法扫码？"}</span>
                    {!isWechatInternal ? (
                      <>
                        <button
                          className="auth-wechat-panel__link"
                          disabled={!wechatQr?.qrCodeUrl}
                          onClick={openWechatWindow}
                          type="button"
                        >
                          新窗口打开
                        </button>
                        <span aria-hidden="true" className="auth-wechat-panel__divider">
                          ·
                        </span>
                      </>
                    ) : null}
                    <button
                      className="auth-wechat-panel__link"
                      disabled={wechatStatus === "loading"}
                      onClick={refreshWechat}
                      type="button"
                    >
                      {wechatStatus === "loading" ? "生成中..." : "刷新"}
                    </button>
                  </div>
                </div>
              ) : null}

              <div className="auth-alt-login">
                <span className="auth-alt-login__label">其他登录</span>
                <div className="auth-alt-login__actions" role="tablist" aria-label="登录方式">
                  <button
                    className="auth-alt-button"
                    onClick={() => {
                      if (loginTab === "wechat") {
                        setLoginTab("sms");
                        return;
                      }

                      setLoginTab((previous) => (previous === "password" ? "sms" : "password"));
                    }}
                    type="button"
                  >
                    {loginTab === "wechat" ? "手机号登录" : loginTab === "password" ? "验证码登录" : "密码登录"}
                  </button>
                  <button className={`auth-alt-button ${loginTab === "wechat" ? "is-active" : ""}`} onClick={() => setLoginTab("wechat")} type="button">
                    微信扫码登录
                  </button>
                </div>
              </div>
            </section>
          ) : null}

          {view === "register" ? (
            <form className="dt-form-grid auth-modal__body auth-form" onSubmit={submitRegister}>
              <AuthField
                disabled={registerForm.step === 2}
                label="手机号"
                onChange={(value) => setRegisterForm((previous) => ({ ...previous, phone: value }))}
                placeholder="请输入 11 位手机号"
                value={registerForm.phone}
              />
              {registerForm.step === 1 ? (
                <>
                  <div className="code-row auth-code-row">
                    <AuthField
                      label="短信验证码"
                      onChange={(value) => setRegisterForm((previous) => ({ ...previous, code: value }))}
                      placeholder="请输入 6 位验证码"
                      value={registerForm.code}
                    />
                    <Button
                      disabled={cooldowns.register > 0 || loadingKey === "sms-register"}
                      kind="secondary"
                      onClick={() => sendCode("REGISTER", registerForm.phone, "register")}
                    >
                      {cooldowns.register > 0 ? `${cooldowns.register}s` : "获取验证码"}
                    </Button>
                  </div>
                  <Button fullWidth type="submit">
                    {loadingKey === "verify-register" ? "校验中..." : "校验并继续"}
                  </Button>
                </>
              ) : (
                <>
                  <AuthField
                    label="设置密码"
                    onChange={(value) => setRegisterForm((previous) => ({ ...previous, password: value }))}
                    placeholder="请输入 6-18 位字母+数字组合"
                    type="password"
                    value={registerForm.password}
                  />
                  <AuthField
                    label="确认密码"
                    onChange={(value) => setRegisterForm((previous) => ({ ...previous, confirmPassword: value }))}
                    placeholder="请再次输入密码"
                    type="password"
                    value={registerForm.confirmPassword}
                  />
                  <Button fullWidth type="submit">
                    {loadingKey === "complete-register" ? "注册中..." : "完成注册并登录"}
                  </Button>
                </>
              )}
            </form>
          ) : null}

          {view === "reset" ? (
            <form className="dt-form-grid auth-modal__body auth-form" onSubmit={submitReset}>
              <AuthField
                disabled={resetForm.step === 2}
                label="绑定手机号"
                onChange={(value) => setResetForm((previous) => ({ ...previous, phone: value }))}
                placeholder="请输入绑定手机号"
                value={resetForm.phone}
              />
              {resetForm.step === 1 ? (
                <>
                  <div className="code-row auth-code-row">
                    <AuthField
                      label="短信验证码"
                      onChange={(value) => setResetForm((previous) => ({ ...previous, code: value }))}
                      placeholder="请输入 6 位验证码"
                      value={resetForm.code}
                    />
                    <Button
                      disabled={cooldowns.reset > 0 || loadingKey === "sms-reset"}
                      kind="secondary"
                      onClick={() => sendCode("RESET_PASSWORD", resetForm.phone, "reset")}
                    >
                      {cooldowns.reset > 0 ? `${cooldowns.reset}s` : "获取验证码"}
                    </Button>
                  </div>
                  <Button fullWidth type="submit">
                    {loadingKey === "verify-reset" ? "校验中..." : "验证手机号"}
                  </Button>
                </>
              ) : (
                <>
                  <AuthField
                    label="新密码"
                    onChange={(value) => setResetForm((previous) => ({ ...previous, password: value }))}
                    placeholder="请输入 6-18 位字母+数字组合"
                    type="password"
                    value={resetForm.password}
                  />
                  <AuthField
                    label="确认新密码"
                    onChange={(value) => setResetForm((previous) => ({ ...previous, confirmPassword: value }))}
                    placeholder="请再次输入新密码"
                    type="password"
                    value={resetForm.confirmPassword}
                  />
                  <Button fullWidth type="submit">
                    {loadingKey === "complete-reset" ? "保存中..." : "重置密码"}
                  </Button>
                </>
              )}
            </form>
          ) : null}

          {view === "bindWechat" ? (
            <form className="dt-form-grid auth-modal__body auth-form" onSubmit={submitWechatBind}>
              <div className="auth-bind-tip">微信已授权，但当前平台账号未绑定手机号。请先完成短信校验。</div>
              <AuthField
                label="绑定手机号"
                onChange={(value) => setBindForm((previous) => ({ ...previous, phone: value }))}
                placeholder="请输入 11 位手机号"
                value={bindForm.phone}
              />
              <div className="code-row auth-code-row">
                <AuthField
                  label="短信验证码"
                  onChange={(value) => setBindForm((previous) => ({ ...previous, code: value }))}
                  placeholder="请输入 6 位验证码"
                  value={bindForm.code}
                />
                <Button
                  disabled={cooldowns.bind > 0 || loadingKey === "sms-bind"}
                  kind="secondary"
                  onClick={() => sendCode("BIND_PHONE", bindForm.phone, "bind")}
                >
                  {cooldowns.bind > 0 ? `${cooldowns.bind}s` : "获取验证码"}
                </Button>
              </div>
              <Button fullWidth type="submit">
                {loadingKey === "wechat-bind" ? "绑定中..." : "绑定并登录"}
              </Button>
            </form>
          ) : null}

          <div className="auth-modal__switchline">
            {view !== "login" ? (
              <button className="auth-modal__switchlink" onClick={() => openLoginView(view === "reset" ? "password" : "sms")} type="button">
                返回登录
              </button>
            ) : null}
            {view === "login" ? (
              <button className="auth-modal__switchlink" onClick={openResetView} type="button">
                忘记密码
              </button>
            ) : null}
          </div>

        </section>
      </div>
    </div>
  );
}

function AuthField({
  disabled,
  label,
  onChange,
  placeholder,
  type = "text",
  value,
}: {
  disabled?: boolean;
  label: string;
  onChange: (value: string) => void;
  placeholder: string;
  type?: "text" | "password";
  value: string;
}) {
  return (
    <div className="dt-field auth-field">
      <label>{label}</label>
      <input disabled={disabled} onChange={(event) => onChange(event.target.value)} placeholder={placeholder} type={type} value={value} />
    </div>
  );
}

function getErrorMessage(error: unknown) {
  if (error instanceof Error) {
    return error.message;
  }
  return "操作失败，请稍后再试";
}

function formatTimestamp(value: string) {
  const timestamp = new Date(value);
  if (Number.isNaN(timestamp.getTime())) {
    return value;
  }
  return `${String(timestamp.getHours()).padStart(2, "0")}:${String(timestamp.getMinutes()).padStart(2, "0")}`;
}

function getWechatExpireText(status: "idle" | "loading" | "waiting" | "expired", expireAt?: string) {
  if (status === "loading") {
    return "二维码生成中";
  }
  if (!expireAt) {
    return "请使用微信扫一扫登录";
  }
  const expireTime = new Date(expireAt).getTime();
  if (Number.isNaN(expireTime)) {
    return `二维码有效期至 ${expireAt}`;
  }
  const remainSeconds = Math.max(0, Math.ceil((expireTime - Date.now()) / 1000));
  if (remainSeconds <= 0 || status === "expired") {
    return "二维码已失效，请刷新后重试";
  }
  return `二维码 ${remainSeconds} 秒后失效`;
}
