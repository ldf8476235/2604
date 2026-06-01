import { Button, StatusState } from "@delta/ui";
import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { useAuth } from "../auth/auth-context";
import {
  loadMyListingDetail,
  loadMyListings,
  loadPublishMeta,
  resubmitMyListing,
  withdrawMyListing,
  type MyListingDetail,
  type MyListingListItem,
  type MyListingStatus,
  type OptionItem,
  type PublishMeta,
} from "../modules/publish/publish-api";

type LoadStatus = "idle" | "loading" | "success" | "error";

const STATUS_TABS: Array<{ key: MyListingStatus; label: string }> = [
  { key: "ALL", label: "全部" },
  { key: "PENDING_REVIEW", label: "待审核" },
  { key: "PUBLISHED", label: "已发布" },
  { key: "REJECTED", label: "已驳回" },
  { key: "OFFLINE", label: "已下架" },
];

const FALLBACK_DELIVERY_METHODS: OptionItem[] = [
  { value: "wechat_qr", label: "微信扫码上号" },
  { value: "qq_qr", label: "QQ 扫码上号" },
  { value: "account_password", label: "账号密码上号" },
];

const FALLBACK_RANKS: OptionItem[] = [
  { value: "bronze", label: "青铜" },
  { value: "silver", label: "白银" },
  { value: "gold", label: "黄金" },
  { value: "platinum", label: "铂金" },
  { value: "diamond", label: "钻石" },
  { value: "blackhawk", label: "黑鹰" },
  { value: "summit", label: "巅峰" },
];

const FALLBACK_SAFE_BOX_LEVELS: OptionItem[] = [
  { value: "1", label: "基础安全箱(1*2)" },
  { value: "2", label: "进阶安全箱(2*2)" },
  { value: "3", label: "高级安全箱(2*3)" },
  { value: "4", label: "顶级安全箱(3*3)" },
];

type MyListingsPageProps = {
  embedded?: boolean;
};

export function MyListingsPage({ embedded = false }: MyListingsPageProps) {
  const { isAuthenticated, openAuthModal } = useAuth();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const [loadStatus, setLoadStatus] = useState<LoadStatus>("idle");
  const [error, setError] = useState("");
  const [rows, setRows] = useState<MyListingListItem[]>([]);
  const [detailStatus, setDetailStatus] = useState<LoadStatus>("idle");
  const [detailError, setDetailError] = useState("");
  const [detail, setDetail] = useState<MyListingDetail | null>(null);
  const [publishMeta, setPublishMeta] = useState<PublishMeta | null>(null);
  const [actionLoading, setActionLoading] = useState("");
  const [notice, setNotice] = useState("");

  const status = useMemo<MyListingStatus>(() => {
    const raw = searchParams.get(embedded ? "listingStatus" : "status") ?? "ALL";
    return STATUS_TABS.some((item) => item.key === raw) ? (raw as MyListingStatus) : "ALL";
  }, [embedded, searchParams]);
  const activeListingNo = searchParams.get("listing") ?? "";

  useEffect(() => {
    if (!isAuthenticated) {
      setRows([]);
      setLoadStatus("idle");
      return;
    }
    let disposed = false;
    async function bootstrap() {
      try {
        setLoadStatus("loading");
        setError("");
        const result = await loadMyListings(status);
        if (!disposed) {
          setRows(result.rows);
          setLoadStatus("success");
        }
      } catch (requestError) {
        if (!disposed) {
          setError(getErrorMessage(requestError));
          setLoadStatus("error");
        }
      }
    }
    void bootstrap();
    return () => {
      disposed = true;
    };
  }, [isAuthenticated, status]);

  useEffect(() => {
    if (!isAuthenticated || !activeListingNo) {
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
        const [result, meta] = await Promise.all([
          loadMyListingDetail(activeListingNo),
          publishMeta ? Promise.resolve(publishMeta) : loadPublishMeta(),
        ]);
        if (!disposed) {
          setDetail(result);
          setPublishMeta(meta);
          setDetailStatus("success");
        }
      } catch (requestError) {
        if (!disposed) {
          setDetailError(getErrorMessage(requestError));
          setDetailStatus("error");
        }
      }
    }
    void bootstrapDetail();
    return () => {
      disposed = true;
    };
  }, [activeListingNo, isAuthenticated, publishMeta]);

  useEffect(() => {
    if (!notice) {
      return undefined;
    }
    const timer = window.setTimeout(() => setNotice(""), 2000);
    return () => window.clearTimeout(timer);
  }, [notice]);

  if (!isAuthenticated) {
    const loginPrompt = (
      <StatusState
        title="登录后才能查看我的发布"
        description="发布记录涉及卖家身份和审核状态，当前页面需要登录后访问。"
        action={<Button onClick={() => openAuthModal("login")}>立即登录</Button>}
      />
    );
    if (embedded) {
      return loginPrompt;
    }
    return (
      <main className="my-listings-page">
        <div className="dt-container">
          {loginPrompt}
        </div>
      </main>
    );
  }

  const pageContent = (
    <>
      <div className="my-listings-shell">
        <section className="my-listings-hero">
          <div>
            <p className="my-listings-hero__eyebrow">卖家工作台</p>
            <h1>我的发布</h1>
            <p>查看全部账号的审核进度、交易表现和可执行操作，编辑仍复用原发布表单，避免维护两套页面。</p>
          </div>
          <div className="my-listings-hero__actions">
            <Link className="dt-button dt-button--secondary" to="/publish">
              去发布新账号
            </Link>
          </div>
        </section>

        <section className="my-listings-card">
          <div className="my-listings-card__header">
            <div className="my-listings-tabs" role="tablist" aria-label="发布状态筛选">
              {STATUS_TABS.map((item) => (
                <button
                  className={`my-listings-tabs__item ${status === item.key ? "is-active" : ""}`}
                  key={item.key}
                  type="button"
                  onClick={() => {
                    const next = new URLSearchParams(searchParams);
                    if (item.key === "ALL") {
                      next.delete(embedded ? "listingStatus" : "status");
                    } else {
                      next.set(embedded ? "listingStatus" : "status", item.key);
                    }
                    next.delete("listing");
                    setSearchParams(next, { replace: true });
                  }}
                >
                  {item.label}
                </button>
              ))}
            </div>
            <div className="my-listings-card__meta">
              <span>共 {rows.length} 条</span>
              <span>状态流转：待审核 / 已发布 / 已驳回 / 已下架</span>
            </div>
          </div>

          {notice ? (
            <div className="app-toast" role="status" aria-live="polite">
              <span>{notice}</span>
              <button onClick={() => setNotice("")} type="button">关闭</button>
            </div>
          ) : null}

          {loadStatus === "loading" ? (
            <StatusState title="发布记录加载中" description="正在同步账号状态、审核进度和交易表现。" />
          ) : loadStatus === "error" ? (
            <StatusState
              title="我的发布加载失败"
              description={error}
              tone="error"
              action={<Button kind="secondary" onClick={() => window.location.reload()}>重新加载</Button>}
            />
          ) : rows.length ? (
            <div className="my-listings-list">
              {rows.map((item) => (
                <article className="my-listings-item" key={item.listingNo}>
                  <div className="my-listings-item__head">
                    <div>
                      <div className="my-listings-item__title-row">
                        <strong>{item.title}</strong>
                        <span className={`my-listings-status my-listings-status--${statusTone(item.status)}`}>{item.statusLabel}</span>
                        <span className="my-listings-progress">{item.reviewProgress}</span>
                      </div>
                      <p className="my-listings-item__subline">
                        编号 {item.listingNo} · 更新时间 {formatTime(item.updatedAt)} · {item.salesStatus}
                      </p>
                      {item.rejectionReason ? (
                        <p className="my-listings-item__reason">驳回原因：{item.rejectionReason}</p>
                      ) : null}
                    </div>
                    <div className="my-listings-item__price">{formatMoney(item.price)}</div>
                  </div>

                  <div className="my-listings-item__stats">
                    <span>浏览量 {item.viewCount}</span>
                    <span>收藏量 {item.favoriteCount}</span>
                    <span>成交状态 {item.salesStatus}</span>
                  </div>

                  <div className="my-listings-item__actions">
                    <button className="profile-secondary-button" type="button" onClick={() => openDetail(item.listingNo)}>
                      查看详情
                    </button>
                    {item.canEdit ? (
                      <button className="profile-secondary-button" type="button" onClick={() => navigate(`/publish?edit=${encodeURIComponent(item.listingNo)}`)}>
                        编辑
                      </button>
                    ) : null}
                    {item.canWithdraw ? (
                      <button
                        className="profile-secondary-button"
                        disabled={actionLoading === `withdraw:${item.listingNo}`}
                        type="button"
                        onClick={() => void handleWithdraw(item.listingNo)}
                      >
                        {actionLoading === `withdraw:${item.listingNo}` ? "下架中..." : "下架"}
                      </button>
                    ) : null}
                    {item.canResubmit ? (
                      <button
                        className="profile-secondary-button"
                        disabled={actionLoading === `resubmit:${item.listingNo}`}
                        type="button"
                        onClick={() => void handleResubmit(item.listingNo)}
                      >
                        {actionLoading === `resubmit:${item.listingNo}` ? "提交中..." : "重新提交审核"}
                      </button>
                    ) : null}
                  </div>
                </article>
              ))}
            </div>
          ) : (
            <StatusState
              title="当前没有发布记录"
              description="筛选条件下暂无账号，可先发布一个账号再回到这里管理。"
              action={
                <Link className="dt-button dt-button--primary" to="/publish">
                  立即发布
                </Link>
              }
            />
          )}
        </section>
      </div>

      {activeListingNo ? (
        <div className="my-listing-detail-dialog" role="dialog" aria-modal="true" aria-label="发布详情">
          <button className="my-listing-detail-dialog__backdrop" type="button" onClick={closeDetail} />
          <div className="my-listing-detail-dialog__panel">
            <div className="my-listing-detail-dialog__header">
              <div>
                <h3>发布详情</h3>
                <p>{detail?.summary.listingNo ?? activeListingNo}</p>
              </div>
              <button className="my-listing-detail-dialog__close" type="button" onClick={closeDetail}>
                关闭
              </button>
            </div>

            {detailStatus === "loading" ? (
              <StatusState title="发布详情加载中" description="正在同步媒体资源、审核记录和交易记录。" />
            ) : detailStatus === "error" || !detail ? (
              <StatusState title="发布详情加载失败" description={detailError || "未获取到发布详情"} tone="error" />
            ) : (
              <div className="my-listing-detail-dialog__content">
                <section className="my-listing-detail-hero">
                  <div className="my-listing-detail-media">
                    {detail.images.length ? (
                      <>
                        <img alt={detail.summary.title} className="my-listing-detail-media__cover" src={detail.images[0].previewUrl} />
                        {detail.images.length > 1 ? (
                          <div className="my-listing-detail-media__thumbs">
                            {detail.images.slice(0, 5).map((item) => (
                              <img alt={item.filename} key={item.objectKey} src={item.previewUrl} />
                            ))}
                          </div>
                        ) : null}
                      </>
                    ) : (
                      <div className="my-listing-detail-media__empty">暂无截图</div>
                    )}
                    {detail.video ? (
                      <video className="my-listing-detail-media__video" controls src={detail.video.previewUrl} />
                    ) : null}
                  </div>

                  <div className="my-listing-detail-summary">
                    <div className="my-listing-detail-summary__title">
                      <strong>{detail.summary.title}</strong>
                      <span className={`my-listings-status my-listings-status--${statusTone(detail.summary.status)}`}>{detail.summary.statusLabel}</span>
                    </div>
                    <div className="my-listing-detail-summary__grid">
                      <DetailLine label="售价" value={formatMoney(detail.summary.price)} />
                      <DetailLine label="审核进度" value={detail.summary.reviewProgress} />
                      <DetailLine label="浏览量" value={String(detail.summary.viewCount)} />
                      <DetailLine label="收藏量" value={String(detail.summary.favoriteCount)} />
                      <DetailLine label="成交状态" value={detail.summary.salesStatus} />
                      <DetailLine label="更新时间" value={formatTime(detail.summary.updatedAt)} />
                    </div>
                    <div className="my-listing-detail-summary__actions">
                      {detail.summary.canEdit ? (
                        <Button kind="secondary" onClick={() => navigate(`/publish?edit=${encodeURIComponent(detail.summary.listingNo)}`)}>
                          编辑当前账号
                        </Button>
                      ) : null}
                      {detail.summary.canWithdraw ? (
                        <Button
                          kind="secondary"
                          disabled={actionLoading === `withdraw:${detail.summary.listingNo}`}
                          onClick={() => void handleWithdraw(detail.summary.listingNo)}
                        >
                          {actionLoading === `withdraw:${detail.summary.listingNo}` ? "下架中..." : "下架"}
                        </Button>
                      ) : null}
                      {detail.summary.canResubmit ? (
                        <Button
                          disabled={actionLoading === `resubmit:${detail.summary.listingNo}`}
                          onClick={() => void handleResubmit(detail.summary.listingNo)}
                        >
                          {actionLoading === `resubmit:${detail.summary.listingNo}` ? "提交中..." : "重新提交审核"}
                        </Button>
                      ) : null}
                    </div>
                  </div>
                </section>

                <section className="my-listing-detail-section">
                  <h4>完整发布信息</h4>
                  <div className="my-listing-detail-grid">
                    <DetailLine label="地区" value={formatRegion(publishMeta, detail.draft.provinceCode, detail.draft.cityCode)} />
                    <DetailLine label="上号方式" value={formatOption(publishMeta?.deliveryMethods, detail.draft.deliveryMethod, FALLBACK_DELIVERY_METHODS)} />
                    <DetailLine label="账号等级" value={String(detail.draft.accountLevel ?? "-")} />
                    <DetailLine label="账号段位" value={formatOption(publishMeta?.ranks, detail.draft.rankName, FALLBACK_RANKS)} />
                    <DetailLine label="安全箱" value={formatOption(publishMeta?.safeBoxLevels, detail.draft.safeBoxLevel, FALLBACK_SAFE_BOX_LEVELS)} />
                    <DetailLine label="哈夫币" value={String(detail.draft.hafCurrency ?? "-")} />
                    <DetailLine label="议价" value={detail.draft.negotiable ? "支持议价" : "一口价"} />
                    <DetailLine label="其他道具" value={detail.draft.otherItems || "-"} />
                  </div>
                  <div className="my-listing-detail-text">
                    <strong>账号描述</strong>
                    <p>{detail.draft.description || "-"}</p>
                  </div>
                </section>

                <section className="my-listing-detail-section">
                  <h4>审核记录</h4>
                  {detail.reviewRecords.length ? (
                    <div className="my-listing-timeline">
                      {detail.reviewRecords.map((item) => (
                        <div className="my-listing-timeline__item" key={`${item.title}-${item.createdAt}`}>
                          <strong>{item.title}</strong>
                          <span>{formatTime(item.createdAt)}</span>
                          <p>{item.result}</p>
                          {item.note ? <em>{item.note}</em> : null}
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="my-listing-empty-line">暂无审核记录</p>
                  )}
                </section>

                <section className="my-listing-detail-section">
                  <h4>交易记录</h4>
                  {detail.tradeRecords.length ? (
                    <div className="my-listing-trade-list">
                      {detail.tradeRecords.map((item) => (
                        <article className="my-listing-trade-item" key={item.orderNo}>
                          <div>
                            <strong>{item.orderNo}</strong>
                            <p>买家 {item.buyerNickname}</p>
                          </div>
                          <div>
                            <span>{item.statusLabel}</span>
                            <p>{formatMoney(item.totalAmount)}</p>
                          </div>
                          <div>
                            <span>下单 {formatTime(item.createdAt)}</span>
                            <p>{item.completedAt ? `完成 ${formatTime(item.completedAt)}` : "尚未完成"}</p>
                          </div>
                        </article>
                      ))}
                    </div>
                  ) : (
                    <p className="my-listing-empty-line">当前账号暂无交易记录</p>
                  )}
                </section>
              </div>
            )}
          </div>
        </div>
      ) : null}
    </>
  );

  if (embedded) {
    return <section className="my-listings-page my-listings-page--embedded">{pageContent}</section>;
  }

  return (
    <main className="my-listings-page">
      <div className="dt-container">
        {pageContent}
      </div>
    </main>
  );

  function openDetail(listingNo: string) {
    const next = new URLSearchParams(searchParams);
    next.set("listing", listingNo);
    setSearchParams(next, { replace: true });
  }

  function closeDetail() {
    const next = new URLSearchParams(searchParams);
    next.delete("listing");
    setSearchParams(next, { replace: true });
  }

  async function refreshList(preferredListingNo?: string) {
    const result = await loadMyListings(status);
    setRows(result.rows);
    if (preferredListingNo) {
      const exists = result.rows.some((item) => item.listingNo === preferredListingNo);
      if (!exists) {
        closeDetail();
      }
    }
  }

  async function handleWithdraw(listingNo: string) {
    try {
      setActionLoading(`withdraw:${listingNo}`);
      const result = await withdrawMyListing(listingNo);
      setNotice(result.message);
      await refreshList(listingNo);
      if (activeListingNo === listingNo) {
        const nextDetail = await loadMyListingDetail(listingNo);
        setDetail(nextDetail);
        setDetailStatus("success");
      }
    } catch (requestError) {
      setNotice(getErrorMessage(requestError));
    } finally {
      setActionLoading("");
    }
  }

  async function handleResubmit(listingNo: string) {
    try {
      setActionLoading(`resubmit:${listingNo}`);
      const result = await resubmitMyListing(listingNo);
      setNotice(result.message);
      await refreshList(listingNo);
      if (activeListingNo === listingNo) {
        const nextDetail = await loadMyListingDetail(listingNo);
        setDetail(nextDetail);
        setDetailStatus("success");
      }
    } catch (requestError) {
      setNotice(getErrorMessage(requestError));
    } finally {
      setActionLoading("");
    }
  }
}

function DetailLine({ label, value }: { label: string; value: string }) {
  return (
    <div className="my-listing-detail-line">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function statusTone(status: string) {
  switch (status) {
    case "PUBLISHED":
      return "published";
    case "PENDING_REVIEW":
      return "pending";
    case "REJECTED":
      return "rejected";
    case "OFFLINE":
      return "offline";
    default:
      return "neutral";
  }
}

function formatMoney(value: number) {
  return `¥ ${value.toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

function formatTime(value: string) {
  if (!value) {
    return "-";
  }
  return value.replace("T", " ").slice(0, 16);
}

function formatOption(options: OptionItem[] | undefined, value: unknown, fallback: OptionItem[] = []) {
  if (value === null || value === undefined || value === "") {
    return "-";
  }
  const normalized = String(value);
  const match = [...(options ?? []), ...fallback].find((item) => item.value === normalized);
  return match?.label ?? normalized;
}

function formatRegion(meta: PublishMeta | null, provinceCode: string, cityCode: string) {
  if (!provinceCode && !cityCode) {
    return "-";
  }
  const province = meta?.regions.find((item) => item.code === provinceCode);
  const city = province?.cities.find((item) => item.code === cityCode)
    ?? meta?.regions.flatMap((item) => item.cities).find((item) => item.code === cityCode);
  if (province?.name && city?.name) {
    return `${province.name} / ${city.name}`;
  }
  if (city?.name) {
    return city.name;
  }
  if (province?.name) {
    return province.name;
  }
  return [provinceCode, cityCode].filter(Boolean).join(" / ") || "-";
}

function getErrorMessage(error: unknown) {
  if (error instanceof Error) {
    return error.message;
  }
  return "请求失败，请稍后再试";
}
