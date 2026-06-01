import { Button, StatusState } from "@delta/ui";
import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../auth/auth-context";
import { WechatPayDialog, type WechatPayDialogPayload } from "../modules/payment/WechatPayDialog";
import { uploadOssFileDirect } from "../modules/publish/publish-api";
import {
  applyBoostingAfterSale,
  confirmBoostingOrderComplete,
  createBoostingWechatPayment,
  loadBoostingOrderDetail,
  loadBoostingOrders,
  type BoostingOrderDetail,
  type BoostingOrderListItem,
  type BoostingOrderRange,
  type BoostingOrderStatus,
  type BoostingOrderCenterResponse,
} from "../modules/boosting/boosting-api";

type LoadStatus = "idle" | "loading" | "success" | "error";

const STATUS_OPTIONS: Array<{ key: BoostingOrderStatus; label: string }> = [
  { key: "ALL", label: "全部状态" },
  { key: "PENDING_PAYMENT", label: "待付款" },
  { key: "WAITING_SERVICE", label: "待代肝" },
  { key: "IN_SERVICE", label: "代肝中" },
  { key: "COMPLETED", label: "已完成" },
  { key: "AFTER_SALE", label: "售后中" },
  { key: "CANCELED", label: "已取消" },
];

const RANGE_OPTIONS: Array<{ key: BoostingOrderRange; label: string }> = [
  { key: "D7", label: "近7天" },
  { key: "D30", label: "近30天" },
  { key: "CUSTOM", label: "自定义" },
  { key: "ALL", label: "全部时间" },
];

export function BoostingOrdersPage() {
  const { isAuthenticated, openAuthModal } = useAuth();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [status, setStatus] = useState<BoostingOrderStatus>("ALL");
  const [range, setRange] = useState<BoostingOrderRange>("D7");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [loadStatus, setLoadStatus] = useState<LoadStatus>("idle");
  const [error, setError] = useState("");
  const [response, setResponse] = useState<BoostingOrderCenterResponse | null>(null);
  const [detailStatus, setDetailStatus] = useState<LoadStatus>("idle");
  const [detailError, setDetailError] = useState("");
  const [detail, setDetail] = useState<BoostingOrderDetail | null>(null);
  const [actionLoading, setActionLoading] = useState("");
  const [notice, setNotice] = useState("");
  const [afterSaleReason, setAfterSaleReason] = useState("");
  const [afterSaleProofUrl, setAfterSaleProofUrl] = useState("");
  const [afterSaleProofKey, setAfterSaleProofKey] = useState("");
  const [uploadingProof, setUploadingProof] = useState(false);
  const [wechatPayPayload, setWechatPayPayload] = useState<WechatPayDialogPayload | null>(null);

  const focusOrderNo = searchParams.get("focus") ?? "";

  useEffect(() => {
    if (!isAuthenticated) {
      setResponse(null);
      return;
    }
    let disposed = false;
    async function bootstrap() {
      try {
        setLoadStatus("loading");
        setError("");
        const result = await loadBoostingOrders({
          status,
          range,
          startDate: range === "CUSTOM" ? startDate : undefined,
          endDate: range === "CUSTOM" ? endDate : undefined,
        });
        if (disposed) return;
        setResponse(result);
        setLoadStatus("success");
      } catch (requestError) {
        if (disposed) return;
        setError(getErrorMessage(requestError));
        setLoadStatus("error");
      }
    }
    void bootstrap();
    return () => {
      disposed = true;
    };
  }, [isAuthenticated, status, range, startDate, endDate]);

  useEffect(() => {
    if (!isAuthenticated || !focusOrderNo) {
      setDetail(null);
      setDetailStatus("idle");
      setDetailError("");
      return;
    }
    let disposed = false;
    async function bootstrapDetail() {
      try {
        setDetailStatus("loading");
        setDetailError("");
        const result = await loadBoostingOrderDetail(focusOrderNo);
        if (disposed) return;
        setDetail(result);
        setAfterSaleReason(result.afterSaleReason ?? "");
        setAfterSaleProofUrl(result.afterSaleProofUrl ?? "");
        setAfterSaleProofKey("");
        setDetailStatus("success");
      } catch (requestError) {
        if (disposed) return;
        setDetailError(getErrorMessage(requestError));
        setDetailStatus("error");
      }
    }
    void bootstrapDetail();
    return () => {
      disposed = true;
    };
  }, [focusOrderNo, isAuthenticated]);

  useEffect(() => {
    if (!notice) {
      return undefined;
    }
    const timer = window.setTimeout(() => setNotice(""), 2000);
    return () => window.clearTimeout(timer);
  }, [notice]);

  const metricValues = useMemo(() => {
    return {
      total: response?.counts.total ?? 0,
      waiting: (response?.counts.waitingService ?? 0) + (response?.counts.inService ?? 0),
      completed: response?.counts.completed ?? 0,
      afterSale: response?.counts.afterSale ?? 0,
    };
  }, [response]);

  if (!isAuthenticated) {
    return (
      <main className="boosting-page">
        <div className="dt-container boosting-shell">
          <StatusState
            title="登录后才能查看代肝订单"
            description="代肝订单涉及账号信息、客服群聊和售后记录，当前页面需要登录后访问。"
            action={<Button onClick={() => openAuthModal("login")}>立即登录</Button>}
          />
        </div>
      </main>
    );
  }

  return (
    <main className="boosting-page">
      <div className="dt-container boosting-shell">
        <section className="boosting-order-center">
          <header className="boosting-order-center__header">
            <div>
              <p className="boosting-hero__eyebrow">代肝订单管理</p>
              <h1>查看进度、联系客服、申请售后</h1>
              <p>订单支付成功后自动进入待代肝，完成后 24 小时内可提交售后原因与凭证。</p>
            </div>
            <div className="boosting-order-center__header-actions">
              <Link className="dt-button dt-button--secondary" to="/boosting">
                去代肝大厅
              </Link>
            </div>
          </header>

          {notice ? (
            <div className="app-toast" role="status" aria-live="polite">
              <span>{notice}</span>
              <button onClick={() => setNotice("")} type="button">关闭</button>
            </div>
          ) : null}

          <div className="boosting-order-center__filters">
            <label className="boosting-filter-field">
              <span>订单状态</span>
              <select value={status} onChange={(event) => setStatus(event.target.value as BoostingOrderStatus)}>
                {STATUS_OPTIONS.map((item) => (
                  <option key={item.key} value={item.key}>
                    {item.label}
                  </option>
                ))}
              </select>
            </label>
            <label className="boosting-filter-field">
              <span>时间范围</span>
              <select value={range} onChange={(event) => setRange(event.target.value as BoostingOrderRange)}>
                {RANGE_OPTIONS.map((item) => (
                  <option key={item.key} value={item.key}>
                    {item.label}
                  </option>
                ))}
              </select>
            </label>
            {range === "CUSTOM" ? (
              <>
                <label className="boosting-filter-field">
                  <span>开始日期</span>
                  <input type="date" value={startDate} onChange={(event) => setStartDate(event.target.value)} />
                </label>
                <label className="boosting-filter-field">
                  <span>结束日期</span>
                  <input type="date" value={endDate} onChange={(event) => setEndDate(event.target.value)} />
                </label>
              </>
            ) : null}
          </div>

          <div className="boosting-order-center__metrics">
            <Metric label="订单总数" value={String(metricValues.total)} />
            <Metric label="待处理" value={String(metricValues.waiting)} />
            <Metric label="已完成" value={String(metricValues.completed)} />
            <Metric label="售后中" value={String(metricValues.afterSale)} />
          </div>

          {loadStatus === "loading" ? (
            <StatusState title="代肝订单加载中" description="正在同步订单状态、时间范围和进度摘要。" />
          ) : loadStatus === "error" ? (
            <StatusState title="代肝订单加载失败" description={error} tone="error" />
          ) : response?.rows.length ? (
            <div className="boosting-order-list">
              {response.rows.map((item) => (
                <article className="boosting-order-item" key={item.orderNo}>
                  <div className="boosting-order-item__main">
                    <div className="boosting-order-item__head">
                      <strong>{item.serviceName}</strong>
                      <span className={`boosting-order-item__status boosting-order-item__status--${toneByStatus(item.statusCode)}`}>{item.statusLabel}</span>
                    </div>
                    <p>订单号 {item.orderNo} · 下单时间 {item.createdAt}</p>
                    <p>完成进度 {item.progressPercent}% · {item.progressSummary}</p>
                  </div>
                  <div className="boosting-order-item__side">
                    <strong>¥{item.price.toFixed(2)}</strong>
                    <div className="boosting-order-item__actions">
                      <button className="profile-secondary-button" type="button" onClick={() => openDetail(item.orderNo)}>
                        查看进度
                      </button>
                      {item.canPay ? (
                        <button
                          className="profile-secondary-button"
                          disabled={actionLoading === `pay:${item.orderNo}`}
                          type="button"
                          onClick={() => void handlePay(item.orderNo)}
                        >
                          {actionLoading === `pay:${item.orderNo}` ? "支付中..." : "立即支付"}
                        </button>
                      ) : null}
                      {item.canContactService ? (
                        <button className="profile-secondary-button" type="button" onClick={() => openConversation(item.chatGroupNo, item.orderNo)}>
                          联系客服
                        </button>
                      ) : null}
                      {item.canApplyAfterSale ? (
                        <button className="profile-secondary-button" type="button" onClick={() => openDetail(item.orderNo)}>
                          申请售后
                        </button>
                      ) : null}
                      {item.canConfirmComplete ? (
                        <button
                          className="profile-secondary-button"
                          disabled={actionLoading === `confirm:${item.orderNo}`}
                          type="button"
                          onClick={() => void handleConfirmComplete(item.orderNo)}
                        >
                          {actionLoading === `confirm:${item.orderNo}` ? "处理中..." : "确认完成"}
                        </button>
                      ) : null}
                    </div>
                  </div>
                </article>
              ))}
            </div>
          ) : (
            <StatusState title="暂无代肝订单" description="你还没有代肝订单，可以先去大厅挑选服务后再回来管理进度。" />
          )}
        </section>
      </div>

      {focusOrderNo ? (
        <div className="boosting-detail-dialog" role="dialog" aria-modal="true" aria-label="代肝订单详情">
          <button className="boosting-detail-dialog__backdrop" type="button" onClick={closeDetail} />
          <div className="boosting-detail-dialog__panel">
            <header className="boosting-detail-dialog__header">
              <div>
                <h3>代肝订单详情</h3>
                <p>{detail?.orderNo ?? focusOrderNo}</p>
              </div>
              <button className="boosting-detail-dialog__close" type="button" onClick={closeDetail}>
                关闭
              </button>
            </header>

            {detailStatus === "loading" ? (
              <StatusState title="订单详情加载中" description="正在同步账号信息、代肝进度和售后状态。" />
            ) : detailStatus === "error" || !detail ? (
              <StatusState title="订单详情加载失败" description={detailError || "未获取到订单详情"} tone="error" />
            ) : (
              <div className="boosting-detail-dialog__content">
                <section className="boosting-detail-hero">
                  <div>
                    <span className={`boosting-order-item__status boosting-order-item__status--${toneByStatus(detail.statusCode)}`}>{detail.statusLabel}</span>
                    <h4>{detail.serviceName}</h4>
                    <p>{detail.serviceDescription}</p>
                  </div>
                  <div className="boosting-detail-hero__actions">
                    {detail.canPay ? (
                      <Button disabled={actionLoading === `pay:${detail.orderNo}`} onClick={() => void handlePay(detail.orderNo)}>
                        {actionLoading === `pay:${detail.orderNo}` ? "支付中..." : "去支付"}
                      </Button>
                    ) : null}
                    {detail.canContactService ? (
                      <Button kind="secondary" onClick={() => openConversation(detail.chatGroupNo, detail.orderNo)}>
                        联系客服
                      </Button>
                    ) : null}
                    {detail.canConfirmComplete ? (
                      <Button kind="secondary" disabled={actionLoading === `confirm:${detail.orderNo}`} onClick={() => void handleConfirmComplete(detail.orderNo)}>
                        {actionLoading === `confirm:${detail.orderNo}` ? "处理中..." : "确认完成"}
                      </Button>
                    ) : null}
                  </div>
                </section>

                <div className="boosting-detail-grid">
                  <section className="boosting-detail-card">
                    <h4>订单信息</h4>
                    <DetailRow label="订单号" value={detail.orderNo} />
                    <DetailRow label="服务分类" value={detail.serviceCategory} />
                    <DetailRow label="订单金额" value={`¥${detail.price.toFixed(2)}`} />
                    <DetailRow label="支付方式" value={detail.paymentMethod} />
                    <DetailRow label="创建时间" value={detail.createdAt} />
                    <DetailRow label="支付时间" value={detail.paidAt} />
                    <DetailRow label="完成时间" value={detail.completedAt} />
                  </section>
                  <section className="boosting-detail-card">
                    <h4>账号信息</h4>
                    <DetailRow label="游戏区服" value={detail.gameRegion} />
                    <DetailRow label="账号" value={detail.accountName} />
                    <DetailRow label="账号密码" value={detail.maskedPassword} />
                    <DetailRow label="角色名称" value={detail.characterName} />
                    <DetailRow label="特殊需求" value={detail.specialRequirement} />
                  </section>
                </div>

                <div className="boosting-detail-grid">
                  <section className="boosting-detail-card">
                    <h4>代肝进度</h4>
                    <div className="boosting-progress-bar">
                      <span style={{ width: `${detail.progressPercent}%` }} />
                    </div>
                    <p className="boosting-progress-bar__label">
                      当前进度 {detail.progressPercent}% · {detail.progressSummary}
                    </p>
                    <div className="boosting-progress-timeline">
                      {detail.progressLogs.map((item) => (
                        <div className="boosting-progress-timeline__item" key={`${item.createdAt}-${item.title}`}>
                          <span className="boosting-progress-timeline__dot" aria-hidden="true" />
                          <div>
                            <strong>{item.title}</strong>
                            <p>{item.content}</p>
                            <small>
                              {item.createdAt} · {item.createdBy}
                            </small>
                          </div>
                        </div>
                      ))}
                    </div>
                  </section>
                  <section className="boosting-detail-card">
                    <h4>保障与售后</h4>
                    <p>{detail.guaranteeNote}</p>
                    <p>客服群聊：{detail.chatGroupNo || "待生成"}</p>
                    {detail.afterSaleReason ? <p>售后原因：{detail.afterSaleReason}</p> : null}
                    {detail.afterSaleProofUrl ? (
                      <a className="boosting-inline-link" href={detail.afterSaleProofUrl} rel="noreferrer" target="_blank">
                        查看售后凭证
                      </a>
                    ) : null}
                    {detail.canApplyAfterSale ? (
                      <div className="boosting-after-sale-form">
                        <label className="boosting-filter-field boosting-filter-field--full">
                          <span>售后原因</span>
                          <textarea
                            rows={4}
                            value={afterSaleReason}
                            onChange={(event) => setAfterSaleReason(event.target.value)}
                            placeholder="说明代肝未按约定完成或导致账号资产损坏的情况"
                          />
                        </label>
                        <div className="boosting-after-sale-upload">
                          <span>售后凭证截图</span>
                          <label className="boosting-upload-trigger">
                            <input
                              accept="image/png,image/jpeg"
                              hidden
                              type="file"
                              onChange={(event) => {
                                const file = event.target.files?.[0];
                                event.currentTarget.value = "";
                                if (file) {
                                  void handleUploadProof(file);
                                }
                              }}
                            />
                            {uploadingProof ? "上传中..." : "上传截图"}
                          </label>
                          {afterSaleProofUrl ? <img alt="售后凭证" className="boosting-after-sale-preview" src={afterSaleProofUrl} /> : null}
                        </div>
                        <div className="boosting-after-sale-actions">
                          <Button
                            disabled={actionLoading === `after-sale:${detail.orderNo}`}
                            onClick={() => void handleAfterSale(detail.orderNo)}
                          >
                            {actionLoading === `after-sale:${detail.orderNo}` ? "提交中..." : "提交售后"}
                          </Button>
                        </div>
                      </div>
                    ) : null}
                  </section>
                </div>
              </div>
            )}
          </div>
        </div>
      ) : null}
      <WechatPayDialog
        open={Boolean(wechatPayPayload)}
        payload={wechatPayPayload}
        title="代肝订单微信支付"
        onClose={() => setWechatPayPayload(null)}
        onCheckPaid={async () => {
          if (!wechatPayPayload?.orderNo) {
            return false;
          }
          const nextDetail = await loadBoostingOrderDetail(wechatPayPayload.orderNo);
          setDetail(nextDetail);
          return nextDetail.statusCode !== "PENDING_PAYMENT";
        }}
        onPaid={async () => {
          if (wechatPayPayload?.orderNo) {
            const nextDetail = await loadBoostingOrderDetail(wechatPayPayload.orderNo);
            setDetail(nextDetail);
          }
          await reloadOrders();
          setNotice("代肝订单已支付成功。");
        }}
      />
    </main>
  );

  function openDetail(orderNo: string) {
    const next = new URLSearchParams(searchParams);
    next.set("focus", orderNo);
    setSearchParams(next, { replace: true });
  }

  function closeDetail() {
    const next = new URLSearchParams(searchParams);
    next.delete("focus");
    setSearchParams(next, { replace: true });
  }

  async function handlePay(orderNo: string) {
    try {
      setActionLoading(`pay:${orderNo}`);
      const snapshot = detail?.orderNo === orderNo ? detail : await loadBoostingOrderDetail(orderNo);
      if (detail?.orderNo !== orderNo) {
        setDetail(snapshot);
      }
      if (snapshot.paymentMethod.includes("微信")) {
        const payload = await createBoostingWechatPayment(orderNo, preferredWechatTradeType());
        setWechatPayPayload(payload);
        setNotice(`订单 ${orderNo} 已生成微信支付，请完成扫码或公众号支付。`);
      } else {
        setNotice("支付宝支付需要等待支付平台回调确认到账，当前暂不支持手动确认付款。");
      }
    } catch (requestError) {
      setNotice(getErrorMessage(requestError));
    } finally {
      setActionLoading("");
    }
  }

  async function handleAfterSale(orderNo: string) {
    if (!afterSaleReason.trim()) {
      setNotice("请先填写售后原因。");
      return;
    }
    if (!afterSaleProofKey.trim()) {
      setNotice("请先上传售后凭证截图。");
      return;
    }
    try {
      setActionLoading(`after-sale:${orderNo}`);
      const nextDetail = await applyBoostingAfterSale(orderNo, afterSaleReason.trim(), afterSaleProofKey.trim());
      setDetail(nextDetail);
      setNotice(`订单 ${orderNo} 已提交售后申请。`);
      await reloadOrders();
    } catch (requestError) {
      setNotice(getErrorMessage(requestError));
    } finally {
      setActionLoading("");
    }
  }

  async function handleConfirmComplete(orderNo: string) {
    try {
      setActionLoading(`confirm:${orderNo}`);
      const nextDetail = await confirmBoostingOrderComplete(orderNo);
      setDetail(nextDetail);
      setNotice(`订单 ${orderNo} 已确认完成，若符合条件分销佣金会自动结算。`);
      await reloadOrders();
    } catch (requestError) {
      setNotice(getErrorMessage(requestError));
    } finally {
      setActionLoading("");
    }
  }

  async function handleUploadProof(file: File) {
    try {
      setUploadingProof(true);
      const uploadResult = await uploadOssFileDirect("boosting-after-sale", file, file.name);
      setAfterSaleProofKey(uploadResult.objectKey);
      setAfterSaleProofUrl(uploadResult.previewUrl);
      setNotice("售后凭证已上传。");
    } catch (requestError) {
      setNotice(getErrorMessage(requestError));
    } finally {
      setUploadingProof(false);
    }
  }

  function openConversation(chatGroupNo: string, orderNo: string) {
    if (!chatGroupNo) {
      setNotice("当前订单尚未生成客服群聊，请稍后再试。");
      return;
    }
    navigate(`/im/${chatGroupNo}`, {
      state: {
        from: `/boosting/orders?focus=${orderNo}`,
      },
    });
  }

  async function reloadOrders() {
    const result = await loadBoostingOrders({
      status,
      range,
      startDate: range === "CUSTOM" ? startDate : undefined,
      endDate: range === "CUSTOM" ? endDate : undefined,
    });
    setResponse(result);
  }
}

function preferredWechatTradeType(): "NATIVE" | "JSAPI" {
  if (typeof navigator !== "undefined" && /MicroMessenger/i.test(navigator.userAgent)) {
    return "JSAPI";
  }
  return "NATIVE";
}

function Metric({ label, value }: { label: string; value: string }) {
  return (
    <div className="boosting-metric-card">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="boosting-detail-row">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function toneByStatus(status: BoostingOrderStatus) {
  if (status === "COMPLETED") return "success";
  if (status === "AFTER_SALE") return "warning";
  if (status === "CANCELED") return "muted";
  if (status === "IN_SERVICE") return "accent";
  return "default";
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : "请求失败，请稍后再试";
}
