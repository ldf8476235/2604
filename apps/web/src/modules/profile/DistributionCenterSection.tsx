import { Button, StatusState } from "@delta/ui";
import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import {
  generateDistributionInviteLink,
  loadDistributionCenter,
  type DistributionCenterResponse,
  type DistributionCommissionRecord,
  type DistributionFanRecord,
  type DistributionOrderRecord,
} from "./profile-api";

type LoadStatus = "idle" | "loading" | "success" | "error";
type DistributionRange = "ALL" | "D7" | "D30" | "CUSTOM";
type DistributionTab = "fans" | "orders" | "commissions";

const RANGE_OPTIONS: Array<{ key: DistributionRange; label: string }> = [
  { key: "D7", label: "近7天" },
  { key: "D30", label: "近30天" },
  { key: "CUSTOM", label: "自定义" },
  { key: "ALL", label: "全部时间" },
];

const TAB_OPTIONS: Array<{ key: DistributionTab; label: string }> = [
  { key: "fans", label: "我的粉丝" },
  { key: "orders", label: "分销订单" },
  { key: "commissions", label: "佣金明细" },
];

export function DistributionCenterSection() {
  const [loadStatus, setLoadStatus] = useState<LoadStatus>("idle");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [center, setCenter] = useState<DistributionCenterResponse | null>(null);
  const [actionLoading, setActionLoading] = useState("");
  const [keywordInput, setKeywordInput] = useState("");
  const [keyword, setKeyword] = useState("");
  const [range, setRange] = useState<DistributionRange>("D30");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [activeTab, setActiveTab] = useState<DistributionTab>("fans");

  useEffect(() => {
    let disposed = false;
    async function bootstrap() {
      try {
        setLoadStatus("loading");
        setError("");
        const next = await loadDistributionCenter({
          keyword: keyword || undefined,
          range,
          startDate: range === "CUSTOM" ? startDate : undefined,
          endDate: range === "CUSTOM" ? endDate : undefined,
        });
        if (disposed) {
          return;
        }
        setCenter(next);
        setLoadStatus("success");
      } catch (requestError) {
        if (disposed) {
          return;
        }
        setError(requestError instanceof Error ? requestError.message : "分销数据加载失败");
        setLoadStatus("error");
      }
    }
    void bootstrap();
    return () => {
      disposed = true;
    };
  }, [keyword, range, startDate, endDate]);

  useEffect(() => {
    if (!notice) {
      return undefined;
    }
    const timer = window.setTimeout(() => setNotice(""), 2000);
    return () => window.clearTimeout(timer);
  }, [notice]);

  const inviteLink = useMemo(() => buildInviteLink(center?.invitePath ?? ""), [center?.invitePath]);

  const currentRows = useMemo(() => {
    if (!center) {
      return [];
    }
    if (activeTab === "fans") {
      return center.fans;
    }
    if (activeTab === "orders") {
      return center.orderRows;
    }
    return center.commissionRows;
  }, [activeTab, center]);

  return (
    <section className="profile-feature-layout">
      <section className="profile-panel distributor-center-panel">
        <header className="profile-panel__header distributor-center-panel__header">
          <div>
            <h2>分销中心</h2>
            <p>推广链接、粉丝转化、分销订单和佣金到账全部收口到这里，结构对齐你给的参考站。</p>
          </div>
          <span className={`profile-status-badge ${center?.enabled ? "profile-status-badge--success" : ""}`}>
            {center?.enabled ? "已开通" : "待开通"}
          </span>
        </header>

        {notice ? (
          <div className="app-toast" role="status" aria-live="polite">
            <span>{notice}</span>
            <button onClick={() => setNotice("")} type="button">关闭</button>
          </div>
        ) : null}

        {loadStatus === "loading" || loadStatus === "idle" ? (
          <StatusState title="分销数据加载中" description="正在同步推广链路、粉丝和佣金统计。" />
        ) : loadStatus === "error" ? (
          <StatusState title="分销数据加载失败" description={error} tone="error" />
        ) : center && !center.enabled ? (
          <section className="distribution-lock-card">
            <div>
              <span className="distribution-lock-card__eyebrow">分销未开通</span>
              <h3>完成实名认证后可自动开启分销权限</h3>
              <p>{center.lockedReason || "当前账号还不满足分销条件，请先完成实名认证。"}</p>
            </div>
            <div className="distribution-lock-card__actions">
              <Link className="dt-button dt-button--primary" to="/profile?tab=verify">
                去实名认证
              </Link>
              <Link className="dt-button dt-button--secondary" to="/profile?tab=wallet">
                查看钱包
              </Link>
            </div>
          </section>
        ) : center ? (
          <>
            <div className="distribution-summary-grid">
              <SummaryCard accent="forest" caption="累计" label="累计佣金" value={center.totalCommission} />
              <SummaryCard accent="mint" caption="本月" label="本月佣金" value={center.monthCommission} />
              <SummaryCard accent="teal" caption="昨日" label="昨日佣金" value={center.yesterdayCommission} />
              <SummaryCard accent="lime" caption="预估" label="今日预估收益" value={center.todayEstimatedCommission} />
            </div>

            <div className="distribution-secondary-grid">
              <MetricCard label="累计推广人数" value={String(center.totalPromotedUsers)} />
              <MetricCard label="累计推广订单" value={String(center.totalOrders)} />
              <MetricCard label="推广码" value={center.inviteCode || "-"} />
            </div>

            <section className="distribution-link-panel">
              <div className="distribution-link-panel__main">
                <div className="distribution-link-panel__meta">
                  <span>专属推广链接</span>
                  <strong>{inviteLink || "尚未生成"}</strong>
                </div>
                <div className="distribution-link-panel__meta">
                  <span>推广海报</span>
                  <strong>{center.posterUrl ? "已生成，可下载或分享" : "暂未生成海报"}</strong>
                </div>
              </div>
              <div className="distribution-link-panel__actions">
                <Button
                  disabled={actionLoading === "generate"}
                  onClick={() => void handleGenerateLink(false)}
                >
                  {actionLoading === "generate" ? "生成中..." : center.inviteCode ? "刷新海报" : "生成推广链接"}
                </Button>
                <button
                  className="profile-secondary-button"
                  disabled={!inviteLink}
                  onClick={() => void handleCopyLink(inviteLink)}
                  type="button"
                >
                  复制链接
                </button>
                <button
                  className="profile-secondary-button"
                  disabled={!center.posterUrl}
                  onClick={() => void handleDownloadPoster(center.posterUrl)}
                  type="button"
                >
                  下载海报
                </button>
                <button
                  className="profile-secondary-button"
                  disabled={actionLoading === "regenerate"}
                  onClick={() => void handleGenerateLink(true)}
                  type="button"
                >
                  {actionLoading === "regenerate" ? "更新中..." : "重新生成链接"}
                </button>
              </div>
            </section>

            <div className="distribution-filter-row">
              <div className="distribution-filter-row__tabs" role="tablist" aria-label="分销数据分类">
                {TAB_OPTIONS.map((item) => (
                  <button
                    className={`distribution-filter-row__tab ${activeTab === item.key ? "is-active" : ""}`}
                    key={item.key}
                    onClick={() => setActiveTab(item.key)}
                    role="tab"
                    type="button"
                  >
                    {item.label}
                  </button>
                ))}
              </div>

              <div className="distribution-filter-row__controls">
                <label className="profile-input-stack distribution-filter-row__search">
                  <span>搜索</span>
                  <input
                    placeholder={activeTab === "fans" ? "昵称 / 手机号" : "订单号 / 昵称"}
                    value={keywordInput}
                    onChange={(event) => setKeywordInput(event.target.value)}
                  />
                </label>
                <label className="profile-input-stack">
                  <span>时间范围</span>
                  <select
                    value={range}
                    onChange={(event) => {
                      const next = event.target.value as DistributionRange;
                      setRange(next);
                      if (next !== "CUSTOM") {
                        setStartDate("");
                        setEndDate("");
                      }
                    }}
                  >
                    {RANGE_OPTIONS.map((item) => (
                      <option key={item.key} value={item.key}>
                        {item.label}
                      </option>
                    ))}
                  </select>
                </label>
                {range === "CUSTOM" ? (
                  <>
                    <label className="profile-input-stack">
                      <span>开始日期</span>
                      <input type="date" value={startDate} onChange={(event) => setStartDate(event.target.value)} />
                    </label>
                    <label className="profile-input-stack">
                      <span>结束日期</span>
                      <input type="date" value={endDate} onChange={(event) => setEndDate(event.target.value)} />
                    </label>
                  </>
                ) : null}
                <button className="profile-secondary-button" onClick={() => setKeyword(keywordInput.trim())} type="button">
                  搜索
                </button>
                <button
                  className="profile-secondary-button"
                  onClick={() => {
                    setKeywordInput("");
                    setKeyword("");
                    setRange("D30");
                    setStartDate("");
                    setEndDate("");
                  }}
                  type="button"
                >
                  重置
                </button>
              </div>
            </div>

            {currentRows.length ? (
              <div className="distribution-record-list">
                {activeTab === "fans" ? (center.fans as DistributionFanRecord[]).map((item) => <FanCard item={item} key={item.id} />) : null}
                {activeTab === "orders"
                  ? (center.orderRows as DistributionOrderRecord[]).map((item) => <OrderCard item={item} key={item.id} />)
                  : null}
                {activeTab === "commissions"
                  ? (center.commissionRows as DistributionCommissionRecord[]).map((item) => <CommissionCard item={item} key={item.id} />)
                  : null}
              </div>
            ) : (
              <StatusState
                title={activeTab === "fans" ? "暂无粉丝数据" : activeTab === "orders" ? "暂无分销订单" : "暂无佣金明细"}
                description="当前筛选条件下没有匹配记录。"
              />
            )}
          </>
        ) : null}
      </section>
    </section>
  );

  async function handleGenerateLink(regenerate: boolean) {
    try {
      setActionLoading(regenerate ? "regenerate" : "generate");
      const result = await generateDistributionInviteLink(regenerate);
      setCenter((current) => {
        if (!current) {
          return current;
        }
        return {
          ...current,
          inviteCode: result.inviteCode,
          invitePath: result.invitePath,
          posterUrl: result.posterUrl,
        };
      });
      setNotice(regenerate ? "推广链接已更新，旧链接已失效。" : "推广链接和海报已生成。");
    } catch (requestError) {
      setNotice(requestError instanceof Error ? requestError.message : "推广链接生成失败");
    } finally {
      setActionLoading("");
    }
  }

  async function handleCopyLink(link: string) {
    try {
      await navigator.clipboard.writeText(link);
      setNotice("推广链接已复制。");
    } catch (requestError) {
      setNotice(requestError instanceof Error ? requestError.message : "复制推广链接失败");
    }
  }

  async function handleDownloadPoster(posterUrl: string) {
    try {
      const response = await fetch(posterUrl);
      if (!response.ok) {
        throw new Error("海报下载失败");
      }
      const blob = await response.blob();
      const objectUrl = window.URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = objectUrl;
      anchor.download = `distribution-poster-${center?.inviteCode || "share"}.svg`;
      anchor.click();
      window.URL.revokeObjectURL(objectUrl);
      setNotice("推广海报已开始下载。");
    } catch (requestError) {
      setNotice(requestError instanceof Error ? requestError.message : "推广海报下载失败");
    }
  }
}

function SummaryCard({ label, value, caption, accent }: { label: string; value: string; caption: string; accent: string }) {
  return (
    <article className={`distribution-summary-card distribution-summary-card--${accent}`}>
      <span>{caption}</span>
      <strong>{value}</strong>
      <small>{label}</small>
    </article>
  );
}

function MetricCard({ label, value }: { label: string; value: string }) {
  return (
    <article className="distribution-metric-card">
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
}

function FanCard({ item }: { item: DistributionFanRecord }) {
  return (
    <article className="distribution-record-card">
      <div className="distribution-record-card__main">
        <div className="distribution-record-card__headline">
          <strong>{item.nickname}</strong>
          <span>{item.status}</span>
        </div>
        <p>手机号 {item.phone}</p>
        <p>注册时间 {item.registeredAt}</p>
      </div>
      <div className="distribution-record-card__side">
        <span>首单号</span>
        <strong>{item.firstOrderNo || "-"}</strong>
        <small>生效时间 {item.effectiveAt}</small>
      </div>
    </article>
  );
}

function OrderCard({ item }: { item: DistributionOrderRecord }) {
  return (
    <article className="distribution-record-card">
      <div className="distribution-record-card__main">
        <div className="distribution-record-card__headline">
          <strong>{item.orderNo}</strong>
          <span>{item.status}</span>
        </div>
        <p>{item.nickname} · {item.orderType}</p>
        <p>下单时间 {item.createdAt}</p>
      </div>
      <div className="distribution-record-card__side">
        <span>{item.amount}</span>
        <strong>{item.commission}</strong>
        <small>到账时间 {item.settledAt}</small>
      </div>
    </article>
  );
}

function CommissionCard({ item }: { item: DistributionCommissionRecord }) {
  return (
    <article className="distribution-record-card">
      <div className="distribution-record-card__main">
        <div className="distribution-record-card__headline">
          <strong>{item.orderNo || "-"}</strong>
          <span>{item.status}</span>
        </div>
        <p>创建时间 {item.createdAt}</p>
      </div>
      <div className="distribution-record-card__side">
        <span>佣金</span>
        <strong>{item.amount}</strong>
        <small>到账时间 {item.settledAt}</small>
      </div>
    </article>
  );
}

function buildInviteLink(invitePath: string) {
  if (!invitePath) {
    return "";
  }
  if (/^https?:\/\//.test(invitePath)) {
    return invitePath;
  }
  if (typeof window === "undefined") {
    return invitePath;
  }
  return `${window.location.origin}${invitePath}`;
}
