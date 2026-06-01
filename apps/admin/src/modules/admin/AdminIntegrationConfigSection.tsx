import { Button, StatusState, SurfaceCard, Tag } from "@delta/ui";
import { useEffect, useState } from "react";
import {
  saveAdminDistributionConfig,
  saveAdminListingPublishConfig,
  loadAdminIntegrationConfigs,
  saveAdminLoginConfig,
  saveAdminPaymentConfig,
  type AdminDistributionConfig,
  type AdminListingPublishConfig,
  type AdminLoginConfig,
  type AdminPaymentConfig,
} from "./admin-api";

type LoadStatus = "idle" | "loading" | "success" | "error";

const INITIAL_PAYMENT: AdminPaymentConfig = {
  wechatEnabled: false,
  wechatMockMode: true,
  wechatAppId: "",
  wechatMchId: "",
  wechatNotifyUrl: "",
  alipayEnabled: false,
  alipayMockMode: true,
  alipayAppId: "",
  alipayMerchantNo: "",
  alipayNotifyUrl: "",
};

const INITIAL_LOGIN: AdminLoginConfig = {
  smsEnabled: false,
  smsMockMode: true,
  smsSignName: "",
  smsTemplateCode: "",
  wechatOpenEnabled: false,
  wechatOpenMockMode: true,
  wechatOpenAppId: "",
  wechatOpenRedirectUri: "",
};

const INITIAL_DISTRIBUTION: AdminDistributionConfig = {
  autoEnableAfterVerified: true,
  defaultTradeCommissionRate: "0.1000",
  defaultBoostingCommissionRate: "0.1000",
};

const INITIAL_LISTING_PUBLISH: AdminListingPublishConfig = {
  defaultExchangeRate: "8",
  personalSellerCommissionRate: "0.1000",
};

function toWanRate(value: string) {
  const numeric = Number(value);
  if (!Number.isFinite(numeric) || numeric <= 0) {
    return value;
  }
  return String(numeric >= 10000 ? numeric / 10000 : numeric);
}

function toRawRate(value: string) {
  const numeric = Number(value);
  if (!Number.isFinite(numeric) || numeric <= 0) {
    return value;
  }
  return String(numeric < 10000 ? numeric * 10000 : numeric);
}

function normalizeListingPublishConfig(config: AdminListingPublishConfig): AdminListingPublishConfig {
  return {
    ...config,
    defaultExchangeRate: toWanRate(config.defaultExchangeRate),
  };
}

function serializeListingPublishConfig(config: AdminListingPublishConfig): AdminListingPublishConfig {
  return {
    ...config,
    defaultExchangeRate: toRawRate(config.defaultExchangeRate),
  };
}

export function AdminIntegrationConfigSection() {
  const [loadStatus, setLoadStatus] = useState<LoadStatus>("idle");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [paymentConfig, setPaymentConfig] = useState<AdminPaymentConfig>(INITIAL_PAYMENT);
  const [loginConfig, setLoginConfig] = useState<AdminLoginConfig>(INITIAL_LOGIN);
  const [distributionConfig, setDistributionConfig] = useState<AdminDistributionConfig>(INITIAL_DISTRIBUTION);
  const [listingPublishConfig, setListingPublishConfig] = useState<AdminListingPublishConfig>(INITIAL_LISTING_PUBLISH);
  const [savingKey, setSavingKey] = useState<"" | "payment" | "login" | "distribution" | "listingPublish">("");

  useEffect(() => {
    let disposed = false;
    setLoadStatus("loading");
    setError("");
    loadAdminIntegrationConfigs()
      .then((payload) => {
        if (disposed) {
          return;
        }
        setPaymentConfig(payload.payment);
        setLoginConfig(payload.login);
        setDistributionConfig(payload.distribution);
        setListingPublishConfig(normalizeListingPublishConfig(payload.listingPublish));
        setLoadStatus("success");
      })
      .catch((requestError) => {
        if (disposed) {
          return;
        }
        setError(requestError instanceof Error ? requestError.message : "配置加载失败");
        setLoadStatus("error");
      });
    return () => {
      disposed = true;
    };
  }, []);

  useEffect(() => {
    if (!notice) {
      return;
    }
    const timer = window.setTimeout(() => setNotice(""), 2000);
    return () => window.clearTimeout(timer);
  }, [notice]);

  if (loadStatus === "loading" || loadStatus === "idle") {
    return (
      <SurfaceCard eyebrow="扩展配置" title="支付 / 登录 / 分销参数">
        <StatusState title="配置加载中" description="正在同步支付与登录参数。" />
      </SurfaceCard>
    );
  }

  if (loadStatus === "error") {
    return (
      <SurfaceCard eyebrow="扩展配置" title="支付 / 登录 / 分销参数">
        <StatusState title="配置加载失败" description={error} tone="error" />
      </SurfaceCard>
    );
  }

  return (
    <SurfaceCard eyebrow="扩展配置" title="支付、登录与分销参数">
      {notice ? (
        <div className="console-notice" role="status" aria-live="polite">
          <span>{notice}</span>
          <button onClick={() => setNotice("")} type="button">关闭</button>
        </div>
      ) : null}
      <div className="console-operation-config">
        <div className="console-operation-config__hero">
          <div>
            <p className="console-operation-config__eyebrow">渠道接入总控</p>
            <h3>统一维护支付、登录与分销默认参数</h3>
            <p>
              运营层的资源位和渠道层的系统配置分开管理。这里专门处理支付通道、登录方式以及分销默认规则，避免和首页素材编辑混在一起。
            </p>
          </div>
          <div className="console-operation-config__stats">
            <div className="console-operation-config__stat">
              <span>支付通道</span>
              <strong>{Number(paymentConfig.wechatEnabled) + Number(paymentConfig.alipayEnabled)} / 2</strong>
            </div>
            <div className="console-operation-config__stat">
              <span>登录方式</span>
              <strong>{Number(loginConfig.smsEnabled) + Number(loginConfig.wechatOpenEnabled)} / 2</strong>
            </div>
            <div className="console-operation-config__stat">
              <span>分销开通</span>
              <strong>{distributionConfig.autoEnableAfterVerified ? "自动" : "手动"}</strong>
            </div>
            <div className="console-operation-config__stat">
              <span>默认兑换</span>
              <strong>{listingPublishConfig.defaultExchangeRate}万</strong>
            </div>
          </div>
        </div>

        <div className="console-operation-config__grid">
          <section className="console-operation-config__panel">
            <div className="console-operation-config__panel-head">
              <div>
                <p>支付配置</p>
                <h4>微信支付 / 支付宝</h4>
              </div>
              <div className="console-support-summary">
                <Tag tone={paymentConfig.wechatEnabled ? "success" : "warning"}>微信支付 {paymentConfig.wechatEnabled ? "启用" : "关闭"}</Tag>
                <Tag tone={paymentConfig.alipayEnabled ? "success" : "warning"}>支付宝 {paymentConfig.alipayEnabled ? "启用" : "关闭"}</Tag>
              </div>
            </div>
            <div className="console-operation-config__toggles">
              <label className="console-config-toggle">
                <input
                  checked={paymentConfig.wechatEnabled}
                  type="checkbox"
                  onChange={(event) => setPaymentConfig((current) => ({ ...current, wechatEnabled: event.target.checked }))}
                />
                <span>启用微信支付</span>
              </label>
              <label className="console-config-toggle">
                <input
                  checked={paymentConfig.wechatMockMode}
                  type="checkbox"
                  onChange={(event) => setPaymentConfig((current) => ({ ...current, wechatMockMode: event.target.checked }))}
                />
                <span>微信支付走模拟模式</span>
              </label>
              <label className="console-config-toggle">
                <input
                  checked={paymentConfig.alipayEnabled}
                  type="checkbox"
                  onChange={(event) => setPaymentConfig((current) => ({ ...current, alipayEnabled: event.target.checked }))}
                />
                <span>启用支付宝</span>
              </label>
              <label className="console-config-toggle">
                <input
                  checked={paymentConfig.alipayMockMode}
                  type="checkbox"
                  onChange={(event) => setPaymentConfig((current) => ({ ...current, alipayMockMode: event.target.checked }))}
                />
                <span>支付宝走模拟模式</span>
              </label>
            </div>
            <div className="console-config-form console-config-form--stacked">
              <label>
                微信 AppId
                <input value={paymentConfig.wechatAppId} onChange={(event) => setPaymentConfig((current) => ({ ...current, wechatAppId: event.target.value }))} />
              </label>
              <label>
                微信商户号
                <input value={paymentConfig.wechatMchId} onChange={(event) => setPaymentConfig((current) => ({ ...current, wechatMchId: event.target.value }))} />
              </label>
              <label>
                微信回调地址
                <input value={paymentConfig.wechatNotifyUrl} onChange={(event) => setPaymentConfig((current) => ({ ...current, wechatNotifyUrl: event.target.value }))} />
              </label>
              <label>
                支付宝 AppId
                <input value={paymentConfig.alipayAppId} onChange={(event) => setPaymentConfig((current) => ({ ...current, alipayAppId: event.target.value }))} />
              </label>
              <label>
                支付宝商户号
                <input value={paymentConfig.alipayMerchantNo} onChange={(event) => setPaymentConfig((current) => ({ ...current, alipayMerchantNo: event.target.value }))} />
              </label>
              <label>
                支付宝回调地址
                <input value={paymentConfig.alipayNotifyUrl} onChange={(event) => setPaymentConfig((current) => ({ ...current, alipayNotifyUrl: event.target.value }))} />
              </label>
            </div>
            <div className="console-operation-config__actions">
              <Button disabled={savingKey === "payment"} onClick={() => void handleSavePayment()}>
                {savingKey === "payment" ? "保存中..." : "保存支付配置"}
              </Button>
            </div>
          </section>

          <section className="console-operation-config__panel">
            <div className="console-operation-config__panel-head">
              <div>
                <p>登录配置</p>
                <h4>短信验证码 / 微信登录</h4>
              </div>
              <div className="console-support-summary">
                <Tag tone={loginConfig.smsEnabled ? "success" : "warning"}>短信登录 {loginConfig.smsEnabled ? "启用" : "关闭"}</Tag>
                <Tag tone={loginConfig.wechatOpenEnabled ? "success" : "warning"}>微信登录 {loginConfig.wechatOpenEnabled ? "启用" : "关闭"}</Tag>
              </div>
            </div>
            <div className="console-operation-config__toggles">
              <label className="console-config-toggle">
                <input
                  checked={loginConfig.smsEnabled}
                  type="checkbox"
                  onChange={(event) => setLoginConfig((current) => ({ ...current, smsEnabled: event.target.checked }))}
                />
                <span>启用短信验证码登录</span>
              </label>
              <label className="console-config-toggle">
                <input
                  checked={loginConfig.smsMockMode}
                  type="checkbox"
                  onChange={(event) => setLoginConfig((current) => ({ ...current, smsMockMode: event.target.checked }))}
                />
                <span>短信登录走模拟模式</span>
              </label>
              <label className="console-config-toggle">
                <input
                  checked={loginConfig.wechatOpenEnabled}
                  type="checkbox"
                  onChange={(event) => setLoginConfig((current) => ({ ...current, wechatOpenEnabled: event.target.checked }))}
                />
                <span>启用微信开放平台登录</span>
              </label>
              <label className="console-config-toggle">
                <input
                  checked={loginConfig.wechatOpenMockMode}
                  type="checkbox"
                  onChange={(event) => setLoginConfig((current) => ({ ...current, wechatOpenMockMode: event.target.checked }))}
                />
                <span>微信登录走模拟模式</span>
              </label>
            </div>
            <div className="console-config-form">
              <label>
                短信签名
                <input value={loginConfig.smsSignName} onChange={(event) => setLoginConfig((current) => ({ ...current, smsSignName: event.target.value }))} />
              </label>
              <label>
                短信模板编码
                <input value={loginConfig.smsTemplateCode} onChange={(event) => setLoginConfig((current) => ({ ...current, smsTemplateCode: event.target.value }))} />
              </label>
              <label>
                微信开放平台 AppId
                <input value={loginConfig.wechatOpenAppId} onChange={(event) => setLoginConfig((current) => ({ ...current, wechatOpenAppId: event.target.value }))} />
              </label>
              <label>
                微信登录回调地址
                <input value={loginConfig.wechatOpenRedirectUri} onChange={(event) => setLoginConfig((current) => ({ ...current, wechatOpenRedirectUri: event.target.value }))} />
              </label>
            </div>
            <div className="console-operation-config__actions">
              <Button disabled={savingKey === "login"} onClick={() => void handleSaveLogin()}>
                {savingKey === "login" ? "保存中..." : "保存登录配置"}
              </Button>
            </div>
          </section>

          <section className="console-operation-config__panel">
            <div className="console-operation-config__panel-head">
              <div>
                <p>分销配置</p>
                <h4>自动开通 / 默认佣金比例</h4>
              </div>
              <div className="console-support-summary">
                <Tag tone={distributionConfig.autoEnableAfterVerified ? "success" : "warning"}>
                  实名后自动开通 {distributionConfig.autoEnableAfterVerified ? "开启" : "关闭"}
                </Tag>
                <Tag tone="accent">账号交易 {distributionConfig.defaultTradeCommissionRate}</Tag>
                <Tag tone="accent">代肝服务 {distributionConfig.defaultBoostingCommissionRate}</Tag>
              </div>
            </div>
            <div className="console-operation-config__toggles">
              <label className="console-config-toggle console-config-toggle--wide">
                <input
                  checked={distributionConfig.autoEnableAfterVerified}
                  type="checkbox"
                  onChange={(event) => setDistributionConfig((current) => ({ ...current, autoEnableAfterVerified: event.target.checked }))}
                />
                <span>用户实名通过后自动开通分销权限</span>
              </label>
            </div>
            <div className="console-config-form">
              <label>
                账号交易默认佣金比例
                <input
                  value={distributionConfig.defaultTradeCommissionRate}
                  onChange={(event) => setDistributionConfig((current) => ({ ...current, defaultTradeCommissionRate: event.target.value }))}
                />
              </label>
              <label>
                代肝服务默认佣金比例
                <input
                  value={distributionConfig.defaultBoostingCommissionRate}
                  onChange={(event) => setDistributionConfig((current) => ({ ...current, defaultBoostingCommissionRate: event.target.value }))}
                />
              </label>
            </div>
            <div className="console-operation-config__actions">
              <Button disabled={savingKey === "distribution"} onClick={() => void handleSaveDistribution()}>
                {savingKey === "distribution" ? "保存中..." : "保存分销配置"}
              </Button>
            </div>
          </section>

          <section className="console-operation-config__panel">
            <div className="console-operation-config__panel-head">
              <div>
                <p>账号发布配置</p>
                <h4>默认兑换比例</h4>
              </div>
              <div className="console-support-summary">
                <Tag tone="accent">1 元 = {listingPublishConfig.defaultExchangeRate} 万哈夫币</Tag>
                <Tag tone="accent">普通卖家抽成 {listingPublishConfig.personalSellerCommissionRate}</Tag>
              </div>
            </div>
            <div className="console-config-form">
              <label>
                默认比例数值
                <input
                  inputMode="decimal"
                  value={listingPublishConfig.defaultExchangeRate}
                  onChange={(event) => setListingPublishConfig((current) => ({ ...current, defaultExchangeRate: event.target.value }))}
                  placeholder="例如 38"
                />
                <span>用于发布页选择“默认比例”时，单位为万。填 38 表示 1 元 = 38 万哈夫币。</span>
              </label>
              <label>
                普通用户卖家抽成比例
                <input
                  inputMode="decimal"
                  value={listingPublishConfig.personalSellerCommissionRate}
                  onChange={(event) => setListingPublishConfig((current) => ({ ...current, personalSellerCommissionRate: event.target.value }))}
                  placeholder="例如 0.1000"
                />
                <span>普通用户账号交易完成时扣除的平台抽成，0.1000 表示 10%。</span>
              </label>
            </div>
            <div className="console-operation-config__actions">
              <Button disabled={savingKey === "listingPublish"} onClick={() => void handleSaveListingPublish()}>
                {savingKey === "listingPublish" ? "保存中..." : "保存账号发布配置"}
              </Button>
            </div>
          </section>
        </div>
      </div>
    </SurfaceCard>
  );

  async function handleSavePayment() {
    try {
      setSavingKey("payment");
      const result = await saveAdminPaymentConfig(paymentConfig);
      setNotice(String((result as { message?: string }).message ?? "支付配置已保存"));
    } catch (requestError) {
      setNotice(requestError instanceof Error ? requestError.message : "支付配置保存失败");
    } finally {
      setSavingKey("");
    }
  }

  async function handleSaveLogin() {
    try {
      setSavingKey("login");
      const result = await saveAdminLoginConfig(loginConfig);
      setNotice(String((result as { message?: string }).message ?? "登录配置已保存"));
    } catch (requestError) {
      setNotice(requestError instanceof Error ? requestError.message : "登录配置保存失败");
    } finally {
      setSavingKey("");
    }
  }

  async function handleSaveDistribution() {
    try {
      setSavingKey("distribution");
      setNotice("");
      const payload = await saveAdminDistributionConfig(distributionConfig) as {
        message?: string;
        distribution: AdminDistributionConfig;
      };
      setDistributionConfig(payload.distribution);
      setNotice(payload.message || "分销配置已保存");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "分销配置保存失败");
    } finally {
      setSavingKey("");
    }
  }

  async function handleSaveListingPublish() {
    try {
      setSavingKey("listingPublish");
      setNotice("");
      const payload = await saveAdminListingPublishConfig(serializeListingPublishConfig(listingPublishConfig)) as {
        message?: string;
        listingPublish: AdminListingPublishConfig;
      };
      setListingPublishConfig(normalizeListingPublishConfig(payload.listingPublish));
      setNotice(payload.message || "账号发布配置已保存");
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "账号发布配置保存失败");
    } finally {
      setSavingKey("");
    }
  }
}
