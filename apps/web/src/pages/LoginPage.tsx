import { Button, SectionHeading, StatusState, SurfaceCard, Tag } from "@delta/ui";
import { useMemo, useState } from "react";

type LoginMode = "sms" | "wechat" | "password";

export function LoginPage() {
  const [mode, setMode] = useState<LoginMode>("sms");

  const modeDescription = useMemo(() => {
    if (mode === "sms") {
      return "适用于注册、登录、找回密码、绑手机和高风险验证场景。";
    }
    if (mode === "wechat") {
      return "PC 端扫码，H5 端拉起授权；未绑定手机号时进入绑定流程。";
    }
    return "适合常用设备快速登录，也支持后续找回和安全校验。";
  }, [mode]);

  return (
    <main className="content-section">
      <div className="dt-container login-layout">
        <div>
          <SectionHeading
            badge="认证中心"
            title="登录、注册、找回密码共用一套安全校验流"
            description="支持短信、微信授权和账号密码登录，找回密码与绑定流程也统一在这里完成。"
          />
          <div className="inline-tags">
            <Tag tone="success">验证码 15 分钟有效</Tag>
            <Tag tone="warning">1 分钟内禁止重复发送</Tag>
            <Tag>登录行为受安全校验保护</Tag>
          </div>
          <StatusState
            title="安全提示"
            description="如遇异常登录、验证码频繁触发限制或微信授权未完成，请稍后重试并核对当前设备环境。"
            tone="success"
          />
        </div>

        <SurfaceCard eyebrow="登录 / 注册" title="账号安全入口">
          <div className="tab-row" role="tablist" aria-label="登录方式">
            <button className={`mode-tab ${mode === "sms" ? "is-active" : ""}`} onClick={() => setMode("sms")} type="button">
              短信登录
            </button>
            <button className={`mode-tab ${mode === "wechat" ? "is-active" : ""}`} onClick={() => setMode("wechat")} type="button">
              微信扫码
            </button>
            <button className={`mode-tab ${mode === "password" ? "is-active" : ""}`} onClick={() => setMode("password")} type="button">
              账号密码
            </button>
          </div>

          <p className="muted">{modeDescription}</p>

          {mode === "wechat" ? (
            <div className="wechat-box">
              <div className="wechat-box__code" aria-label="微信二维码占位" />
              <p className="muted">二维码有效期 5 分钟，过期后自动刷新。</p>
              <Button fullWidth>刷新二维码</Button>
            </div>
          ) : (
            <form className="dt-form-grid">
              <div className="dt-field">
                <label htmlFor="phone">手机号</label>
                <input id="phone" placeholder="请输入 11 位手机号" />
              </div>
              {mode === "password" ? (
                <div className="dt-field">
                  <label htmlFor="password">密码</label>
                  <input id="password" placeholder="请输入 6-18 位字母+数字组合" type="password" />
                </div>
              ) : (
                <div className="code-row">
                  <div className="dt-field">
                    <label htmlFor="code">短信验证码</label>
                    <input id="code" placeholder="请输入 6 位验证码" />
                  </div>
                  <Button kind="secondary">获取验证码</Button>
                </div>
              )}
              <Button fullWidth>{mode === "password" ? "登录" : "校验并继续"}</Button>
            </form>
          )}
        </SurfaceCard>
      </div>
    </main>
  );
}
