import { Button, StatusState } from "@delta/ui";
import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../auth/auth-context";
import {
  createBoostingOrder,
  loadBoostingServiceDetail,
  type BoostingCreateOrderPayload,
  type BoostingServiceDetail,
} from "../modules/boosting/boosting-api";

type LoadStatus = "idle" | "loading" | "success" | "error";

export function BoostingOrderPage() {
  const navigate = useNavigate();
  const { serviceNo = "" } = useParams();
  const { isAuthenticated, openAuthModal } = useAuth();
  const [service, setService] = useState<BoostingServiceDetail | null>(null);
  const [loadStatus, setLoadStatus] = useState<LoadStatus>("idle");
  const [error, setError] = useState("");
  const [submitError, setSubmitError] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [form, setForm] = useState<BoostingCreateOrderPayload>({
    serviceNo,
    gameRegion: "",
    accountName: "",
    accountPassword: "",
    characterName: "",
    specialRequirement: "",
    paymentMethod: "ALIPAY",
    agreementCode: "BOOSTING_AGREEMENT",
  });

  useEffect(() => {
    let disposed = false;
    async function bootstrap() {
      try {
        setLoadStatus("loading");
        setError("");
        const result = await loadBoostingServiceDetail(serviceNo);
        if (disposed) return;
        setService(result);
        setLoadStatus("success");
      } catch (requestError) {
        if (disposed) return;
        setError(getErrorMessage(requestError));
        setLoadStatus("error");
      }
    }
    if (serviceNo) {
      void bootstrap();
    }
    return () => {
      disposed = true;
    };
  }, [serviceNo]);

  const formReady = useMemo(
    () => Boolean(form.gameRegion && form.accountName && form.accountPassword && form.characterName && form.agreementCode),
    [form],
  );

  async function handleSubmit() {
    if (!isAuthenticated) {
      openAuthModal("login");
      return;
    }
    if (!formReady) {
      setSubmitError("请先完整填写游戏区服、账号、密码、角色名称并同意协议。");
      return;
    }
    try {
      setSubmitting(true);
      setSubmitError("");
      const result = await createBoostingOrder(form);
      navigate(`/boosting/orders?focus=${encodeURIComponent(result.orderNo)}`);
    } catch (requestError) {
      setSubmitError(getErrorMessage(requestError));
    } finally {
      setSubmitting(false);
    }
  }

  if (loadStatus === "loading") {
    return (
      <main className="boosting-page">
        <div className="dt-container boosting-shell">
          <StatusState title="代肝服务加载中" description="正在同步服务详情、价格和保障说明。" />
        </div>
      </main>
    );
  }

  if (loadStatus === "error" || !service) {
    return (
      <main className="boosting-page">
        <div className="dt-container boosting-shell">
          <StatusState
            title="服务详情加载失败"
            description={error || "没有找到对应的代肝服务。"}
            tone="error"
            action={
              <Link className="dt-button dt-button--secondary" to="/boosting">
                返回代肝大厅
              </Link>
            }
          />
        </div>
      </main>
    );
  }

  return (
    <main className="boosting-page">
      <div className="dt-container boosting-shell">
        <section className="boosting-order-layout">
          <section className="boosting-order-summary">
            <p className="boosting-hero__eyebrow">代肝下单</p>
            <h1>{service.name}</h1>
            <p>{service.description}</p>
            <div className="boosting-order-summary__metrics">
              <Metric label="服务价格" value={`¥${service.price.toFixed(2)}`} />
              <Metric label="完成周期" value={service.cycleLabel} />
              <Metric label="销量" value={String(service.salesCount)} />
            </div>
            <div className="boosting-order-summary__stack">
              <div>
                <strong>保障说明</strong>
                <p>{service.guaranteeNote}</p>
              </div>
              <div>
                <strong>服务商</strong>
                <p>{service.providerLabel}</p>
              </div>
            </div>
            <ul className="boosting-order-summary__notice-list">
              {service.notices.map((item) => (
                <li key={item}>{item}</li>
              ))}
            </ul>
          </section>

          <section className="boosting-order-form-card">
            <header className="boosting-order-form-card__header">
              <div>
                <h2>填写账号信息</h2>
                <p>账号密码将加密存储，仅代肝人员和平台客服可查看。</p>
              </div>
              <Link className="boosting-inline-link" to="/boosting/orders">
                我的代肝订单
              </Link>
            </header>

            <div className="boosting-order-form-grid">
              <label className="boosting-filter-field">
                <span>游戏区服</span>
                <input value={form.gameRegion} onChange={(event) => setForm((current) => ({ ...current, gameRegion: event.target.value }))} placeholder="如 微信区-华东" />
              </label>
              <label className="boosting-filter-field">
                <span>角色名称</span>
                <input value={form.characterName} onChange={(event) => setForm((current) => ({ ...current, characterName: event.target.value }))} placeholder="输入角色名称" />
              </label>
              <label className="boosting-filter-field">
                <span>账号</span>
                <input value={form.accountName} onChange={(event) => setForm((current) => ({ ...current, accountName: event.target.value }))} placeholder="输入登录账号" />
              </label>
              <label className="boosting-filter-field">
                <span>账号密码</span>
                <input type="password" value={form.accountPassword} onChange={(event) => setForm((current) => ({ ...current, accountPassword: event.target.value }))} placeholder="输入登录密码" />
              </label>
              <label className="boosting-filter-field boosting-filter-field--full">
                <span>特殊需求</span>
                <textarea
                  rows={4}
                  value={form.specialRequirement}
                  onChange={(event) => setForm((current) => ({ ...current, specialRequirement: event.target.value }))}
                  placeholder="如 优先提升安全箱等级、每天晚上 9 点后再上号"
                />
              </label>
            </div>

            <div className="boosting-payment-row">
              <div className="boosting-choice-group">
                <span>支付方式</span>
                <div className="boosting-choice-group__buttons">
                  {[
                    { value: "ALIPAY" as const, label: "支付宝" },
                    { value: "WECHAT" as const, label: "微信支付" },
                  ].map((item) => (
                    <button
                      className={`boosting-choice ${form.paymentMethod === item.value ? "is-active" : ""}`}
                      key={item.value}
                      type="button"
                      onClick={() => setForm((current) => ({ ...current, paymentMethod: item.value }))}
                    >
                      {item.label}
                    </button>
                  ))}
                </div>
              </div>
              <label className="boosting-agreement-check">
                <input
                  checked={form.agreementCode === "BOOSTING_AGREEMENT"}
                  type="checkbox"
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      agreementCode: event.target.checked ? "BOOSTING_AGREEMENT" : ("" as "BOOSTING_AGREEMENT"),
                    }))
                  }
                />
                <span>我已阅读并同意《代肝服务协议》</span>
              </label>
            </div>

            {submitError ? <p className="profile-inline-notice">{submitError}</p> : null}

            <div className="boosting-order-form-actions">
              <Button disabled={submitting} onClick={() => void handleSubmit()}>
                {submitting ? "提交中..." : "提交订单"}
              </Button>
              <Link className="dt-button dt-button--secondary" to="/boosting">
                返回大厅
              </Link>
            </div>
          </section>
        </section>
      </div>
    </main>
  );
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="boosting-metric-card">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : "请求失败，请稍后再试";
}
