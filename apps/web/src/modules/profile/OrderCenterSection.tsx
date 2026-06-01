import { Button, StatusState } from "@delta/ui";
import { useEffect, useState } from "react";
import type {
  OrderCenterRange,
  OrderCenterResponse,
  OrderCenterRole,
  OrderCenterStatus,
  OrderDetail,
  OrderListItem,
} from "./profile-api";

type LoadStatus = "idle" | "loading" | "success" | "error";

const ORDER_ROLE_TABS: { key: OrderCenterRole; label: string }[] = [
  { key: "BUY", label: "购买订单" },
  { key: "SELL", label: "出售订单" },
];

const ORDER_STATUS_OPTIONS: { key: OrderCenterStatus; label: string }[] = [
  { key: "ALL", label: "全部状态" },
  { key: "PENDING_PAYMENT", label: "待付款" },
  { key: "WAITING_TRADE", label: "交易中" },
  { key: "IN_PROGRESS", label: "交易中" },
  { key: "COMPLETED", label: "已完成" },
  { key: "REFUND_PENDING", label: "退款审核中" },
  { key: "AFTER_SALE", label: "售后中" },
  { key: "REFUNDED", label: "已退款" },
  { key: "CLOSED", label: "已关闭" },
];

const ORDER_RANGE_OPTIONS: { key: OrderCenterRange; label: string }[] = [
  { key: "D7", label: "近7天" },
  { key: "D30", label: "近30天" },
  { key: "CUSTOM", label: "自定义" },
  { key: "ALL", label: "全部时间" },
];

export function OrderCenterSection({
  role,
  status,
  range,
  startDate,
  endDate,
  loadStatus,
  error,
  response,
  detail,
  detailStatus,
  detailError,
  notice,
  actionLoadingKey,
  onRoleChange,
  onStatusChange,
  onRangeChange,
  onCustomDateChange,
  onViewDetail,
  onCloseDetail,
  onPayOrder,
  onCancelOrder,
  onApplyRefund,
  onReviewRefund,
  onApplyAfterSale,
  onConfirmComplete,
  onDeleteOrder,
  onRefreshOrders,
  onEnterChat,
  onDownloadCertificate,
}: {
  role: OrderCenterRole;
  status: OrderCenterStatus;
  range: OrderCenterRange;
  startDate: string;
  endDate: string;
  loadStatus: LoadStatus;
  error: string;
  response: OrderCenterResponse | null;
  detail: OrderDetail | null;
  detailStatus: LoadStatus;
  detailError: string;
  notice: string;
  actionLoadingKey: string;
  onRoleChange: (next: OrderCenterRole) => void;
  onStatusChange: (next: OrderCenterStatus) => void;
  onRangeChange: (next: OrderCenterRange) => void;
  onCustomDateChange: (field: "startDate" | "endDate", value: string) => void;
  onViewDetail: (orderNo: string) => void;
  onCloseDetail: () => void;
  onPayOrder: (orderNo: string) => Promise<void>;
  onCancelOrder: (orderNo: string) => Promise<void>;
  onApplyRefund: (orderNo: string) => Promise<void>;
  onReviewRefund: (orderNo: string, action: "APPROVE" | "REJECT") => Promise<void>;
  onApplyAfterSale: (orderNo: string) => Promise<void>;
  onConfirmComplete: (orderNo: string) => Promise<void>;
  onDeleteOrder: (orderNo: string) => Promise<void>;
  onRefreshOrders: () => Promise<void>;
  onEnterChat: (item: OrderListItem | OrderDetail) => void;
  onDownloadCertificate: (orderNo: string) => Promise<void>;
}) {
  const [now, setNow] = useState(() => Date.now());

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), 1000);
    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => {
    const expireTimes = (response?.rows ?? [])
      .filter((item) => item.statusCode === "PENDING_PAYMENT" && item.paymentExpireAt)
      .map((item) => new Date(item.paymentExpireAt as string).getTime())
      .filter((time) => Number.isFinite(time));
    if (!expireTimes.length) {
      return undefined;
    }
    const delay = Math.max(0, Math.min(...expireTimes) - Date.now()) + 800;
    const timer = window.setTimeout(() => void onRefreshOrders(), delay);
    return () => window.clearTimeout(timer);
  }, [onRefreshOrders, response?.rows]);

  return (
    <section className="profile-feature-layout">
      <section className="profile-panel order-center-panel">
        <header className="profile-panel__header">
          <div>
            <h2>订单中心</h2>
            <p>集中查看购买订单与出售订单，支持状态、时间范围筛选和站内详情弹层。</p>
          </div>
        </header>

        {notice ? (
          <div className="app-toast" role="status" aria-live="polite">
            <span>{notice}</span>
          </div>
        ) : null}

        <div className="order-center-tabs" role="tablist" aria-label="订单分类">
          {ORDER_ROLE_TABS.map((item) => (
            <button
              className={`order-center-tabs__item ${role === item.key ? "is-active" : ""}`}
              key={item.key}
              onClick={() => onRoleChange(item.key)}
              role="tab"
              type="button"
            >
              {item.label}
            </button>
          ))}
        </div>

        <div className="order-center-filters">
          <label className="profile-input-stack">
            <span>订单状态</span>
            <select value={status} onChange={(event) => onStatusChange(event.target.value as OrderCenterStatus)}>
              {ORDER_STATUS_OPTIONS.map((item) => (
                <option key={item.key} value={item.key}>
                  {item.label}
                </option>
              ))}
            </select>
          </label>
          <label className="profile-input-stack">
            <span>时间范围</span>
            <select value={range} onChange={(event) => onRangeChange(event.target.value as OrderCenterRange)}>
              {ORDER_RANGE_OPTIONS.map((item) => (
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
                <input type="date" value={startDate} onChange={(event) => onCustomDateChange("startDate", event.target.value)} />
              </label>
              <label className="profile-input-stack">
                <span>结束日期</span>
                <input type="date" value={endDate} onChange={(event) => onCustomDateChange("endDate", event.target.value)} />
              </label>
            </>
          ) : null}
        </div>

        <div className="order-center-summary">
          <SummaryCard label="订单总数" value={String(response?.counts.total ?? 0)} />
          <SummaryCard label="待付款" value={String(response?.counts.pendingPayment ?? 0)} />
          <SummaryCard label="交易中" value={String((response?.counts.waitingTrade ?? 0) + (response?.counts.inProgress ?? 0))} />
          <SummaryCard label="已完成" value={String(response?.counts.completed ?? 0)} />
        </div>

        {renderOrderList({
          role,
          loadStatus,
          error,
          rows: response?.rows ?? [],
          actionLoadingKey,
          onViewDetail,
          onPayOrder,
          onCancelOrder,
          onApplyRefund,
          onReviewRefund,
          onApplyAfterSale,
          onConfirmComplete,
          onDeleteOrder,
          onEnterChat,
          onDownloadCertificate,
          now,
        })}
      </section>

      {detail || detailStatus === "loading" ? (
        <div className="order-detail-dialog" role="dialog" aria-modal="true" aria-label="订单详情">
          <div className="order-detail-dialog__backdrop" onClick={onCloseDetail} />
          <div className="order-detail-dialog__panel">
            <header className="order-detail-dialog__header">
              <div>
                <h3>订单详情</h3>
                <p>订单号 {detail?.orderNo ?? "加载中..."}</p>
              </div>
              <button className="order-detail-dialog__close" onClick={onCloseDetail} type="button">
                关闭
              </button>
            </header>

            {detailStatus === "loading" || !detail ? (
              <StatusState title="订单详情加载中" description="正在获取订单金额、交易双方和进度信息。" />
            ) : detailError ? (
              <StatusState title="订单详情加载失败" description={detailError} tone="error" />
            ) : (
              <div className="order-detail-dialog__content">
                <section className="order-detail-hero">
                  <div className="order-detail-hero__media">
                    {detail.coverUrl ? <img alt={detail.title} src={detail.coverUrl} /> : <div className="order-detail-hero__placeholder">订单封面</div>}
                  </div>
                  <div className="order-detail-hero__meta">
                    <div className="order-detail-hero__head">
                      <span className={`profile-order-status profile-order-status--${mapStatusTone(detail.statusCode)}`}>{detail.statusLabel}</span>
                      <strong>{detail.title}</strong>
                    </div>
                    <p>{detail.summary}</p>
                    <div className="order-detail-amounts">
                      <DetailMetric label="租金/号钱" value={formatCurrency(detail.itemAmount)} />
                      <DetailMetric label="托管押金" value={formatCurrency(detail.depositAmount)} />
                      <DetailMetric label="平台服务费" value={formatCurrency(detail.serviceFee)} />
                      <DetailMetric label="订单总金额" value={formatCurrency(detail.totalAmount)} />
                    </div>
                    <div className="order-detail-section__row">
                      <span>完成确认</span>
                      <strong>买家{detail.buyerConfirmed ? "已确认" : "未确认"} / 卖家{detail.sellerConfirmed ? "已确认" : "未确认"}</strong>
                    </div>
                    <div className="order-detail-actions">
                      <Button disabled={actionLoadingKey === `certificate:${detail.orderNo}`} onClick={() => void onDownloadCertificate(detail.orderNo)}>
                        {actionLoadingKey === `certificate:${detail.orderNo}` ? "下载中..." : "下载订单凭证"}
                      </Button>
                      {detail.canEnterChat ? (
                        <button className="profile-secondary-button" onClick={() => onEnterChat(detail)} type="button">
                          进入群聊
                        </button>
                      ) : null}
                      {detail.canCancel ? (
                        <button className="profile-secondary-button" onClick={() => void onCancelOrder(detail.orderNo)} type="button">
                          取消订单
                        </button>
                      ) : null}
                      {detail.canApplyRefund && !detail.canCancel ? (
                        <button className="profile-secondary-button" onClick={() => void onApplyRefund(detail.orderNo)} type="button">
                          申请退款
                        </button>
                      ) : null}
                      {detail.canDelete ? (
                        <button className="profile-secondary-button profile-secondary-button--danger" onClick={() => void onDeleteOrder(detail.orderNo)} type="button">
                          删除订单
                        </button>
                      ) : null}
                      {detail.canReviewRefund ? (
                        <>
                          <button className="profile-secondary-button" onClick={() => void onReviewRefund(detail.orderNo, "APPROVE")} type="button">
                            同意退款
                          </button>
                          <button className="profile-secondary-button" onClick={() => void onReviewRefund(detail.orderNo, "REJECT")} type="button">
                            拒绝退款
                          </button>
                        </>
                      ) : null}
                      {detail.canApplyAfterSale ? (
                        <button className="profile-secondary-button" onClick={() => void onApplyAfterSale(detail.orderNo)} type="button">
                          申请售后
                        </button>
                      ) : null}
                      {detail.canConfirmComplete ? (
                        <button className="profile-secondary-button" onClick={() => void onConfirmComplete(detail.orderNo)} type="button">
                          {detail.role === "SELL" ? "确认账号安全" : "确认完成"}
                        </button>
                      ) : null}
                    </div>
                  </div>
                </section>

                <div className="order-detail-grid">
                  <section className="order-detail-section">
                    <h4>订单信息</h4>
                    <DetailRow label="订单号" value={detail.orderNo} />
                    <DetailRow label="创建时间" value={detail.createdAt} />
                    {detail.statusCode === "PENDING_PAYMENT" ? (
                      <DetailRow label="付款倒计时" value={formatPaymentCountdown(detail.paymentExpireAt, now)} />
                    ) : null}
                    <DetailRow label="支付时间" value={detail.paidAt} />
                    <DetailRow label="交易完成时间" value={detail.completedAt} />
                    <DetailRow label="支付方式" value={detail.paymentMethod} />
                  </section>

                  <section className="order-detail-section">
                    <h4>交易双方</h4>
                    <DetailRow label="买家昵称" value={detail.buyerNickname} />
                    <DetailRow label="卖家昵称" value={detail.sellerNickname} />
                    <DetailRow label="卖家类型" value={detail.sellerType} />
                    <DetailRow label="卖家展示名" value={detail.sellerDisplayName} />
                  </section>
                </div>

                <div className="order-detail-grid">
                  <section className="order-detail-section">
                    <h4>保障说明</h4>
                    <p>{detail.guaranteeNote}</p>
                    <p>{detail.violationNote}</p>
                  </section>

                  <section className="order-detail-section">
                    <h4>交易进度</h4>
                    <div className="order-progress-list">
                      {detail.progress.map((item) => (
                        <div className={`order-progress-step ${item.current ? "is-current" : item.done ? "is-done" : ""}`} key={item.title}>
                          <span className="order-progress-step__dot" aria-hidden="true" />
                          <div>
                            <strong>{item.title}</strong>
                            <p>{item.description}</p>
                          </div>
                        </div>
                      ))}
                    </div>
                  </section>
                </div>
              </div>
            )}
          </div>
        </div>
      ) : null}
    </section>
  );
}

function renderOrderList({
  role,
  loadStatus,
  error,
  rows,
  actionLoadingKey,
  onViewDetail,
  onPayOrder,
  onCancelOrder,
  onApplyRefund,
  onReviewRefund,
  onApplyAfterSale,
  onConfirmComplete,
  onDeleteOrder,
  onEnterChat,
  onDownloadCertificate,
  now,
}: {
  role: OrderCenterRole;
  loadStatus: LoadStatus;
  error: string;
  rows: OrderListItem[];
  actionLoadingKey: string;
  onViewDetail: (orderNo: string) => void;
  onPayOrder: (orderNo: string) => Promise<void>;
  onCancelOrder: (orderNo: string) => Promise<void>;
  onApplyRefund: (orderNo: string) => Promise<void>;
  onReviewRefund: (orderNo: string, action: "APPROVE" | "REJECT") => Promise<void>;
  onApplyAfterSale: (orderNo: string) => Promise<void>;
  onConfirmComplete: (orderNo: string) => Promise<void>;
  onDeleteOrder: (orderNo: string) => Promise<void>;
  onEnterChat: (item: OrderListItem) => void;
  onDownloadCertificate: (orderNo: string) => Promise<void>;
  now: number;
}) {
  if (loadStatus === "loading" || loadStatus === "idle") {
    return <StatusState title="订单列表加载中" description="正在同步购买/出售订单，请稍候。" />;
  }
  if (loadStatus === "error") {
    return <StatusState title="订单列表加载失败" description={error} tone="error" />;
  }
  if (!rows.length) {
    return <StatusState title="暂无订单" description="当前筛选条件下没有匹配的订单记录。" />;
  }
  return (
    <div className="order-center-list">
      {rows.map((item) => (
        <article className="order-center-card" key={item.orderNo}>
          <div className="order-center-card__main">
            <div className="order-center-card__cover">
              {item.coverUrl ? <img alt={item.title} src={item.coverUrl} /> : <div className="order-center-card__cover-placeholder">订单封面</div>}
            </div>
            <div className="order-center-card__content">
              <div className="order-center-card__topline">
                <span className={`profile-order-status profile-order-status--${mapStatusTone(item.statusCode)}`}>{item.statusLabel}</span>
                <strong>{item.title}</strong>
              </div>
              <p>{item.summary}</p>
              <div className="order-center-card__meta">
                <span>订单号：{item.orderNo}</span>
                <span>交易时间：{item.tradeTime}</span>
                <span>卖家类型：{item.sellerType}</span>
                {item.statusCode === "PENDING_PAYMENT" ? <span>付款倒计时：{formatPaymentCountdown(item.paymentExpireAt, now)}</span> : null}
              </div>
            </div>
          </div>
          <div className="order-center-card__side">
            <strong>{formatCurrency(item.totalAmount)}</strong>
            <div className="order-center-card__actions">
              <button className="profile-secondary-button" onClick={() => onViewDetail(item.orderNo)} type="button">
                查看详情
              </button>
              {item.statusCode === "PENDING_PAYMENT" && role === "BUY" ? (
                <button
                  className="profile-secondary-button"
                  disabled={actionLoadingKey === `pay:${item.orderNo}`}
                  onClick={() => void onPayOrder(item.orderNo)}
                  type="button"
                >
                  {actionLoadingKey === `pay:${item.orderNo}` ? "支付中..." : "去付款"}
                </button>
              ) : null}
              {item.canEnterChat ? (
                <button className="profile-secondary-button" onClick={() => onEnterChat(item)} type="button">
                  进入群聊
                </button>
              ) : null}
              {item.canCancel ? (
                <button className="profile-secondary-button" onClick={() => void onCancelOrder(item.orderNo)} type="button">
                  取消订单
                </button>
              ) : null}
              {item.canApplyRefund && !item.canCancel ? (
                <button className="profile-secondary-button" onClick={() => void onApplyRefund(item.orderNo)} type="button">
                  申请退款
                </button>
              ) : null}
              {item.canDelete ? (
                <button
                  className="profile-secondary-button profile-secondary-button--danger"
                  disabled={actionLoadingKey === `delete:${item.orderNo}`}
                  onClick={() => void onDeleteOrder(item.orderNo)}
                  type="button"
                >
                  {actionLoadingKey === `delete:${item.orderNo}` ? "删除中..." : "删除订单"}
                </button>
              ) : null}
              {item.canReviewRefund ? (
                <>
                  <button className="profile-secondary-button" onClick={() => void onReviewRefund(item.orderNo, "APPROVE")} type="button">
                    同意退款
                  </button>
                  <button className="profile-secondary-button" onClick={() => void onReviewRefund(item.orderNo, "REJECT")} type="button">
                    拒绝退款
                  </button>
                </>
              ) : null}
              {item.canApplyAfterSale ? (
                <button className="profile-secondary-button" onClick={() => void onApplyAfterSale(item.orderNo)} type="button">
                  申请售后
                </button>
              ) : null}
              {item.canConfirmComplete ? (
                <button className="profile-secondary-button" onClick={() => void onConfirmComplete(item.orderNo)} type="button">
                  {role === "SELL" ? "确认账号安全" : "确认完成"}
                </button>
              ) : null}
              {item.canDownloadCertificate ? (
                <button
                  className="profile-secondary-button"
                  disabled={actionLoadingKey === `certificate:${item.orderNo}`}
                  onClick={() => void onDownloadCertificate(item.orderNo)}
                  type="button"
                >
                  {actionLoadingKey === `certificate:${item.orderNo}` ? "下载中..." : "订单凭证"}
                </button>
              ) : null}
            </div>
          </div>
        </article>
      ))}
    </div>
  );
}

function SummaryCard({ label, value }: { label: string; value: string }) {
  return (
    <article className="order-center-summary__card">
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
}

function DetailMetric({ label, value }: { label: string; value: string }) {
  return (
    <div className="order-detail-amounts__item">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="order-detail-section__row">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function formatCurrency(amount: number) {
  return `¥${amount.toFixed(2)}`;
}

function formatPaymentCountdown(expireAt: string | null, now: number) {
  if (!expireAt) return "10:00";
  const expireTime = new Date(expireAt).getTime();
  if (!Number.isFinite(expireTime)) return "10:00";
  const seconds = Math.max(0, Math.floor((expireTime - now) / 1000));
  const minutes = Math.floor(seconds / 60);
  const restSeconds = seconds % 60;
  return `${String(minutes).padStart(2, "0")}:${String(restSeconds).padStart(2, "0")}`;
}

function mapStatusTone(status: OrderCenterStatus) {
  if (status === "PENDING_PAYMENT") return "pending";
  if (status === "WAITING_TRADE" || status === "IN_PROGRESS") return "progress";
  if (status === "COMPLETED") return "success";
  if (status === "AFTER_SALE" || status === "REFUND_PENDING") return "confirm";
  if (status === "REFUNDED") return "success";
  return "cancel";
}
