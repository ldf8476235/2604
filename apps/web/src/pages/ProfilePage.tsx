import { Button, StatusState } from "@delta/ui";
import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { loadFaceRealNameStatus, loadRealNameProfile, sendSmsCode, startFaceRealName, type RealNameProfile } from "../auth/auth-api";
import { useAuth } from "../auth/auth-context";
import type { AuthProfile } from "../auth/auth-storage";
import { uploadOssFileDirect } from "../modules/publish/publish-api";
import { MyListingsPage } from "./MyListingsPage";
import { WechatPayDialog, type WechatPayDialogPayload } from "../modules/payment/WechatPayDialog";
import { DistributionCenterSection } from "../modules/profile/DistributionCenterSection";
import { OfficialAnnouncementSection } from "../modules/profile/OfficialAnnouncementSection";
import { OrderCenterSection } from "../modules/profile/OrderCenterSection";
import {
  applyWithdraw,
  applyOrderAfterSale,
  applyOrderRefund,
  bindWithdrawAccount,
  cancelOrder,
  changeBoundPhone,
  changePassword,
  deleteOrder,
  deleteMessages,
  createWalletWechatRecharge,
  createTradeWechatPayment,
  loadCouponCenter,
  loadMessageCenter,
  loadOrderCenter,
  loadOrderCertificate,
  loadOrderDetail,
  loadWalletRechargeStatus,
  confirmTradeOrder,
  loadSettingsProfile,
  loadWalletOverview,
  markAllMessagesRead,
  markMessagesRead,
  reviewOrderRefund,
  loadStudioApplication,
  unbindBoundPhone,
  unbindWechat,
  submitStudioApplication,
  updateAvatar,
  updateNickname,
  updateSecuritySettings,
  type OrderCounts,
  type OrderCenterRange,
  type OrderCenterResponse,
  type OrderCenterRole,
  type OrderCenterStatus,
  type OrderDetail,
  type OrderListItem,
  type CouponCenterResponse,
  type CouponRecord,
  type MessageCategory,
  type MessageCenter,
  type SettingsProfile,
  type SimpleResult,
  type StudioApplicationProfile,
  type WalletChannel,
  type WalletOverview,
} from "../modules/profile/profile-api";
import { PROFILE_MENU_ITEMS, type ProfileMenuKey } from "../modules/profile/profile-constants";

type LoadStatus = "idle" | "loading" | "success" | "error";
type CouponFilter = "available" | "history";
type UploadedImageAsset = {
  objectKey: string;
  previewUrl: string;
  filename: string;
};

const PROFILE_MENU_SET = new Set<ProfileMenuKey>(PROFILE_MENU_ITEMS.map((item) => item.key));
const FACE_REAL_NAME_ORDER_KEY = "delta:face-real-name-order-id";
const FACE_REAL_NAME_POLL_INTERVAL_MS = 3000;
const MESSAGE_TABS: { key: MessageCategory; label: string }[] = [
  { key: "ALL", label: "全部消息" },
  { key: "SYSTEM", label: "系统公告" },
  { key: "TRADE", label: "交易通知" },
  { key: "SERVICE", label: "客服消息" },
  { key: "DISTRIBUTION", label: "分销通知" },
];

export function ProfilePage() {
  const { isAuthenticated, openAuthModal, saveSession, session } = useAuth();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const activeMenu = resolveActiveMenu(searchParams.get("tab"));
  const compactTradeMessages = activeMenu === "orders" && searchParams.get("entry") === "tradeMessages";
  const [isMobileProfileView, setIsMobileProfileView] = useState(false);
  const hasExplicitProfileTab = Boolean(searchParams.get("tab"));
  const showMobileProfileHome = isMobileProfileView && !compactTradeMessages && !hasExplicitProfileTab;
  const showMobileSubpageHeader = isMobileProfileView && !compactTradeMessages && hasExplicitProfileTab;
  const showProfileOverview = !compactTradeMessages && (!isMobileProfileView || !hasExplicitProfileTab);
  const showProfileContent = !showMobileProfileHome;

  const [orderRole, setOrderRole] = useState<OrderCenterRole>("BUY");
  const [orderStatus, setOrderStatus] = useState<OrderCenterStatus>("ALL");
  const [orderRange, setOrderRange] = useState<OrderCenterRange>("D7");
  const [orderStartDate, setOrderStartDate] = useState("");
  const [orderEndDate, setOrderEndDate] = useState("");
  const [orderCenter, setOrderCenter] = useState<OrderCenterResponse | null>(null);
  const [orderLoadStatus, setOrderLoadStatus] = useState<LoadStatus>("idle");
  const [orderError, setOrderError] = useState("");
  const [activeOrderNo, setActiveOrderNo] = useState("");
  const [orderDetail, setOrderDetail] = useState<OrderDetail | null>(null);
  const [orderDetailStatus, setOrderDetailStatus] = useState<LoadStatus>("idle");
  const [orderDetailError, setOrderDetailError] = useState("");
  const [orderActionLoadingKey, setOrderActionLoadingKey] = useState("");
  const [orderNotice, setOrderNotice] = useState("");
  const [wechatPayPayload, setWechatPayPayload] = useState<WechatPayDialogPayload | null>(null);

  useEffect(() => {
    if (typeof window === "undefined") {
      return undefined;
    }
    const query = window.matchMedia("(max-width: 768px)");
    const sync = () => setIsMobileProfileView(query.matches);
    sync();
    query.addEventListener?.("change", sync);
    return () => query.removeEventListener?.("change", sync);
  }, []);

  useEffect(() => {
    const role = searchParams.get("role");
    if (activeMenu === "orders" && (role === "BUY" || role === "SELL")) {
      setOrderRole(role);
    }
  }, [activeMenu, searchParams]);

  useEffect(() => {
    const focusOrderNo = searchParams.get("focus");
    if (activeMenu === "orders" && focusOrderNo) {
      setActiveOrderNo(focusOrderNo);
    }
  }, [activeMenu, searchParams]);

  const [realNameProfile, setRealNameProfile] = useState<RealNameProfile | null>(null);
  const [realNameLoadStatus, setRealNameLoadStatus] = useState<LoadStatus>("idle");
  const [realNameError, setRealNameError] = useState("");
  const [realNameForm, setRealNameForm] = useState({ realName: "", idCardNo: "" });
  const [realNameFieldErrors, setRealNameFieldErrors] = useState<{
    realName?: string;
    idCardNo?: string;
  }>({});
  const [submittingRealName, setSubmittingRealName] = useState(false);
  const [faceQrDialog, setFaceQrDialog] = useState<{
    orderId: string;
    verifyUrl: string;
    expireAt: string;
    status: "WAITING" | "CHECKING" | "FAILED";
    message: string;
  } | null>(null);

  const [studioApplicationLoadStatus, setStudioApplicationLoadStatus] = useState<LoadStatus>("idle");
  const [studioApplicationError, setStudioApplicationError] = useState("");
  const [studioApplicationNotice, setStudioApplicationNotice] = useState("");
  const [studioApplicationProfile, setStudioApplicationProfile] = useState<StudioApplicationProfile | null>(null);
  const [studioApplicationForm, setStudioApplicationForm] = useState({
    studioName: "",
    qualificationCode: "",
    qualificationNote: "",
    contactName: "",
    contactPhone: "",
  });
  const [studioApplicationFieldErrors, setStudioApplicationFieldErrors] = useState<{
    studioName?: string;
    qualificationCode?: string;
    contactName?: string;
    contactPhone?: string;
    qualificationMaterial?: string;
  }>({});
  const [studioQualificationAsset, setStudioQualificationAsset] = useState<UploadedImageAsset | null>(null);
  const [studioApplicationSubmitting, setStudioApplicationSubmitting] = useState(false);
  const [studioApplicationUploadLoading, setStudioApplicationUploadLoading] = useState(false);

  const [walletLoadStatus, setWalletLoadStatus] = useState<LoadStatus>("idle");
  const [walletError, setWalletError] = useState("");
  const [walletOverview, setWalletOverview] = useState<WalletOverview | null>(null);
  const [walletNotice, setWalletNotice] = useState("");
  const [walletLoadingKey, setWalletLoadingKey] = useState("");
  const [rechargeForm, setRechargeForm] = useState({ amount: "" });
  const [wechatPayScene, setWechatPayScene] = useState<"ORDER" | "WALLET">("ORDER");
  const [withdrawAccountForm, setWithdrawAccountForm] = useState({ channel: "ALIPAY" as WalletChannel, accountName: "", accountNo: "" });
  const [withdrawQrCodeAsset, setWithdrawQrCodeAsset] = useState<UploadedImageAsset | null>(null);
  const [withdrawForm, setWithdrawForm] = useState({ amount: "" });

  const [messageCategory, setMessageCategory] = useState<MessageCategory>("ALL");
  const [messageLoadStatus, setMessageLoadStatus] = useState<LoadStatus>("idle");
  const [messageError, setMessageError] = useState("");
  const [messageCenter, setMessageCenter] = useState<MessageCenter | null>(null);
  const [selectedMessageIds, setSelectedMessageIds] = useState<number[]>([]);
  const [messageActionLoading, setMessageActionLoading] = useState("");

  const [settingsLoadStatus, setSettingsLoadStatus] = useState<LoadStatus>("idle");
  const [settingsError, setSettingsError] = useState("");
  const [settingsProfile, setSettingsProfile] = useState<SettingsProfile | null>(null);
  const [settingsNotice, setSettingsNotice] = useState("");
  const [settingsLoadingKey, setSettingsLoadingKey] = useState("");
  const [nicknameForm, setNicknameForm] = useState("");
  const [passwordForm, setPasswordForm] = useState({ currentPassword: "", nextPassword: "", confirmPassword: "" });
  const [phoneForm, setPhoneForm] = useState({ phone: "", code: "" });
  const [unbindPhoneCode, setUnbindPhoneCode] = useState("");
  const [phoneCodeCooldown, setPhoneCodeCooldown] = useState(0);
  const [securityCodeCooldown, setSecurityCodeCooldown] = useState(0);
  const [securityForm, setSecurityForm] = useState({ loginAlertEnabled: true, secondaryVerifyEnabled: false });

  const [couponFilter, setCouponFilter] = useState<CouponFilter>("available");
  const [couponLoadStatus, setCouponLoadStatus] = useState<LoadStatus>("idle");
  const [couponError, setCouponError] = useState("");
  const [couponCenter, setCouponCenter] = useState<CouponCenterResponse | null>(null);

  const couponBuckets = useMemo(() => {
    const source = couponCenter?.rows ?? [];
    const available = source.filter((item) => item.status === "available");
    const history = source.filter((item) => item.status !== "available");
    return { available, history };
  }, [couponCenter]);

  const profileLabel = useMemo(() => {
    if (!session?.profile) {
      return {
        nickname: "未登录用户",
        phone: "",
        verifyLabel: "未实名",
        passwordLabel: "未设置密码",
      };
    }
    return {
      nickname: session.profile.nickname,
      phone: maskPhone(session.profile.phone),
      verifyLabel: session.profile.verified ? "已实名" : "待实名",
      passwordLabel: session.profile.hasPassword ? "密码已设置" : "待设置密码",
    };
  }, [session]);

  const orderMetrics = useMemo(() => {
    return {
      walletAmount: walletOverview ? `¥${walletOverview.availableBalance.toFixed(2)}` : "¥0",
      totalCount: orderCenter?.counts.total ?? 0,
      pendingCount: orderCenter?.counts.pendingPayment ?? 0,
      progressCount: (orderCenter?.counts.waitingTrade ?? 0) + (orderCenter?.counts.inProgress ?? 0),
      completedCount: orderCenter?.counts.completed ?? 0,
    };
  }, [orderCenter, walletOverview]);

  const isRealNameVerified = Boolean(realNameProfile?.verified);
  const hasLoginPassword = Boolean(session?.profile.hasPassword);
  const selectedMessages = useMemo(
    () => (messageCenter?.rows ?? []).filter((item) => selectedMessageIds.includes(item.id)),
    [messageCenter, selectedMessageIds],
  );
  const visibleMessageIds = useMemo(() => (messageCenter?.rows ?? []).map((item) => item.id), [messageCenter]);
  const messageSelectAllDisabled = !visibleMessageIds.length || messageLoadStatus !== "success";
  const allVisibleMessagesSelected =
    visibleMessageIds.length > 0 && visibleMessageIds.every((messageId) => selectedMessageIds.includes(messageId));

  const syncSessionProfile = (patch: Partial<AuthProfile>) => {
    if (!session) {
      return;
    }
    saveSession({
      ...session,
      profile: {
        ...session.profile,
        ...patch,
      },
    });
  };

  useEffect(() => {
    if (phoneCodeCooldown <= 0) {
      return;
    }
    const timer = window.setTimeout(() => setPhoneCodeCooldown((current) => Math.max(current - 1, 0)), 1000);
    return () => window.clearTimeout(timer);
  }, [phoneCodeCooldown]);

  useEffect(() => {
    if (securityCodeCooldown <= 0) {
      return;
    }
    const timer = window.setTimeout(() => setSecurityCodeCooldown((current) => Math.max(current - 1, 0)), 1000);
    return () => window.clearTimeout(timer);
  }, [securityCodeCooldown]);

  useEffect(() => {
    const timers = [
      orderNotice ? window.setTimeout(() => setOrderNotice(""), 2000) : undefined,
      studioApplicationNotice ? window.setTimeout(() => setStudioApplicationNotice(""), 2000) : undefined,
      walletNotice ? window.setTimeout(() => setWalletNotice(""), 2000) : undefined,
      settingsNotice ? window.setTimeout(() => setSettingsNotice(""), 2000) : undefined,
    ].filter((timer): timer is number => typeof timer === "number");

    return () => {
      timers.forEach((timer) => window.clearTimeout(timer));
    };
  }, [orderNotice, studioApplicationNotice, walletNotice, settingsNotice]);

  useEffect(() => {
    if (!isAuthenticated) {
      return;
    }
    let active = true;
    setOrderLoadStatus("loading");
    setOrderError("");
    loadOrderCenter({
      role: orderRole,
      status: orderStatus,
      range: orderRange,
      startDate: orderRange === "CUSTOM" ? orderStartDate : undefined,
      endDate: orderRange === "CUSTOM" ? orderEndDate : undefined,
    })
      .then((rows) => {
        if (!active) return;
        setOrderCenter(rows);
        setOrderLoadStatus("success");
      })
      .catch((error) => {
        if (!active) return;
        setOrderError(error instanceof Error ? error.message : "订单中心加载失败");
        setOrderLoadStatus("error");
      });
    return () => {
      active = false;
    };
  }, [isAuthenticated, orderRole, orderStatus, orderRange, orderStartDate, orderEndDate]);

  useEffect(() => {
    if (!isAuthenticated || !activeOrderNo) {
      setOrderDetail(null);
      setOrderDetailStatus("idle");
      setOrderDetailError("");
      return;
    }
    let active = true;
    setOrderDetailStatus("loading");
    setOrderDetailError("");
    loadOrderDetail(activeOrderNo)
      .then((detail) => {
        if (!active) return;
        setOrderDetail(detail);
        setOrderDetailStatus("success");
      })
      .catch((error) => {
        if (!active) return;
        setOrderDetailError(error instanceof Error ? error.message : "订单详情加载失败");
        setOrderDetailStatus("error");
      });
    return () => {
      active = false;
    };
  }, [activeOrderNo, isAuthenticated]);

  useEffect(() => {
    if (!isAuthenticated) {
      return;
    }
    let active = true;
    setRealNameLoadStatus("loading");
    setRealNameError("");
    loadRealNameProfile()
      .then((profile) => {
        if (!active) return;
        setRealNameProfile(profile);
        setRealNameForm((current) => ({
          ...current,
          realName: profile.realName || current.realName,
          idCardNo: current.idCardNo,
        }));
        setRealNameLoadStatus("success");
      })
      .catch((error) => {
        if (!active) return;
        setRealNameError(error instanceof Error ? error.message : "实名认证信息加载失败");
        setRealNameLoadStatus("error");
      });
    return () => {
      active = false;
    };
  }, [isAuthenticated]);

  useEffect(() => {
    if (!isAuthenticated || activeMenu !== "verify" || searchParams.get("faceReturn") !== "1") {
      return;
    }
    const orderId = window.sessionStorage.getItem(FACE_REAL_NAME_ORDER_KEY);
    if (!orderId) {
      setRealNameError("未找到本次人脸认证订单，请重新发起认证。");
      return;
    }
    let active = true;
    setSubmittingRealName(true);
    setRealNameError("");
    loadFaceRealNameStatus(orderId)
      .then(async (profile) => {
        if (!active) return;
        setRealNameProfile(profile);
        setRealNameLoadStatus("success");
        window.sessionStorage.removeItem(FACE_REAL_NAME_ORDER_KEY);
        if (profile.verified) {
          syncSessionProfile({ verified: true });
          setWalletNotice("实名认证已完成，现在可以绑定提现账户并提交提现。");
          await Promise.all([refreshSettings(), refreshWallet()]);
          return;
        }
        setRealNameError(profile.rejectReason || "实名认证未通过，请重新发起认证。");
      })
      .catch((error) => {
        if (!active) return;
        setRealNameError(error instanceof Error ? error.message : "实名认证结果查询失败");
      })
      .finally(() => {
        if (active) {
          setSubmittingRealName(false);
        }
      });
    setSearchParams((current) => {
      const next = new URLSearchParams(current);
      next.delete("faceReturn");
      return next;
    }, { replace: true });
    return () => {
      active = false;
    };
  }, [activeMenu, isAuthenticated, searchParams, setSearchParams]);

  useEffect(() => {
    if (!isAuthenticated || !faceQrDialog || faceQrDialog.status === "FAILED") {
      return;
    }
    let active = true;
    const timer = window.setInterval(() => {
      setFaceQrDialog((current) => current ? { ...current, status: "CHECKING", message: "正在等待手机端完成认证..." } : current);
      loadFaceRealNameStatus(faceQrDialog.orderId)
        .then(async (profile) => {
          if (!active) return;
          setRealNameProfile(profile);
          setRealNameLoadStatus("success");
          if (profile.verified) {
            window.clearInterval(timer);
            window.sessionStorage.removeItem(FACE_REAL_NAME_ORDER_KEY);
            setFaceQrDialog(null);
            setSubmittingRealName(false);
            syncSessionProfile({ verified: true });
            setWalletNotice("实名认证已完成，现在可以绑定提现账户并提交提现。");
            await Promise.all([refreshSettings(), refreshWallet()]);
            return;
          }
          if (profile.status === "REJECTED") {
            window.clearInterval(timer);
            window.sessionStorage.removeItem(FACE_REAL_NAME_ORDER_KEY);
            setSubmittingRealName(false);
            setRealNameError(profile.rejectReason || "实名认证未通过，请重新发起认证。");
            setFaceQrDialog((current) => current ? {
              ...current,
              status: "FAILED",
              message: profile.rejectReason || "实名认证未通过，请重新发起认证。",
            } : current);
            return;
          }
          setFaceQrDialog((current) => current ? { ...current, status: "WAITING", message: "请使用手机扫码完成活体认证。" } : current);
        })
        .catch((error) => {
          if (!active) return;
          setFaceQrDialog((current) => current ? {
            ...current,
            status: "WAITING",
            message: error instanceof Error ? error.message : "认证结果查询中，请稍候。",
          } : current);
        });
    }, FACE_REAL_NAME_POLL_INTERVAL_MS);
    return () => {
      active = false;
      window.clearInterval(timer);
    };
  }, [faceQrDialog?.orderId, faceQrDialog?.status, isAuthenticated]);

  useEffect(() => {
    if (!isAuthenticated) {
      return;
    }
    void refreshWallet();
    void refreshSettings();
    void refreshStudioApplication();
  }, [isAuthenticated]);

  useEffect(() => {
    if (!isAuthenticated) {
      return;
    }
    void refreshMessages(messageCategory);
  }, [isAuthenticated, messageCategory]);

  useEffect(() => {
    if (!isAuthenticated || activeMenu !== "coupons") {
      return;
    }
    void refreshCoupons();
  }, [activeMenu, isAuthenticated]);

  async function refreshWallet() {
    setWalletLoadStatus("loading");
    setWalletError("");
    try {
      const next = await loadWalletOverview();
      setWalletOverview(next);
      setWalletLoadStatus("success");
      if (!withdrawAccountForm.accountName && !withdrawAccountForm.accountNo) {
        setWithdrawAccountForm((current) => ({
          ...current,
          channel: next.withdrawAccount?.channel ?? current.channel,
        }));
      }
    } catch (error) {
      setWalletError(error instanceof Error ? error.message : "钱包信息加载失败");
      setWalletLoadStatus("error");
    }
  }

  async function refreshSettings() {
    setSettingsLoadStatus("loading");
    setSettingsError("");
    try {
      const next = await loadSettingsProfile();
      setSettingsProfile(next);
      setNicknameForm(next.nickname);
      setSecurityForm({
        loginAlertEnabled: next.loginAlertEnabled,
        secondaryVerifyEnabled: next.secondaryVerifyEnabled,
      });
      setSettingsLoadStatus("success");
    } catch (error) {
      setSettingsError(error instanceof Error ? error.message : "设置数据加载失败");
      setSettingsLoadStatus("error");
    }
  }

  async function refreshStudioApplication() {
    setStudioApplicationLoadStatus("loading");
    setStudioApplicationError("");
    try {
      const next = await loadStudioApplication();
      setStudioApplicationProfile(next);
      setStudioQualificationAsset(next.qualificationMaterialUrl ? {
        objectKey: next.qualificationMaterialKey,
        previewUrl: next.qualificationMaterialUrl,
        filename: "营业执照或证明材料",
      } : null);
      setStudioApplicationForm({
        studioName: next.studioName ?? "",
        qualificationCode: next.qualificationCode ?? "",
        qualificationNote: next.qualificationNote ?? "",
        contactName: next.contactName ?? "",
        contactPhone: next.contactPhone || session?.profile.phone || "",
      });
      setStudioApplicationLoadStatus("success");
    } catch (error) {
      setStudioApplicationError(error instanceof Error ? error.message : "工作室申请信息加载失败");
      setStudioApplicationLoadStatus("error");
    }
  }

  async function refreshMessages(category: MessageCategory) {
    setMessageLoadStatus("loading");
    setMessageError("");
    try {
      const next = await loadMessageCenter(category);
      setMessageCenter(next);
      setSelectedMessageIds([]);
      setMessageLoadStatus("success");
    } catch (error) {
      setMessageError(error instanceof Error ? error.message : "消息中心加载失败");
      setMessageLoadStatus("error");
    }
  }

  async function refreshCoupons() {
    setCouponLoadStatus("loading");
    setCouponError("");
    try {
      const next = await loadCouponCenter();
      setCouponCenter(next);
      setCouponLoadStatus("success");
    } catch (error) {
      setCouponError(error instanceof Error ? error.message : "优惠券数据加载失败");
      setCouponLoadStatus("error");
    }
  }

  async function refreshOrderCenter() {
    setOrderLoadStatus("loading");
    setOrderError("");
    try {
      const next = await loadOrderCenter({
        role: orderRole,
        status: orderStatus,
        range: orderRange,
        startDate: orderRange === "CUSTOM" ? orderStartDate : undefined,
        endDate: orderRange === "CUSTOM" ? orderEndDate : undefined,
      });
      setOrderCenter(next);
      setOrderLoadStatus("success");
    } catch (error) {
      setOrderError(error instanceof Error ? error.message : "订单中心加载失败");
      setOrderLoadStatus("error");
    }
  }

  function syncOrderDetailIfOpen(orderNo: string, nextDetail: OrderDetail) {
    if (activeOrderNo !== orderNo) {
      return;
    }
    setOrderDetail(nextDetail);
  }

  function patchOrderCenterRow(orderNo: string, nextDetail: OrderDetail) {
    setOrderCenter((current) => {
      if (!current) {
        return current;
      }
      const currentRow = current.rows.find((item) => item.orderNo === orderNo);
      if (!currentRow) {
        return current;
      }
      const nextRow = mergeOrderListItem(currentRow, nextDetail);
      const shouldKeepRow = current.status === "ALL" || current.status === nextDetail.statusCode;
      return {
        ...current,
        counts: patchOrderCounts(current.counts, currentRow.statusCode, nextDetail.statusCode),
        rows: shouldKeepRow
          ? current.rows.map((item) => (item.orderNo === orderNo ? nextRow : item))
          : current.rows.filter((item) => item.orderNo !== orderNo),
      };
    });
  }

  if (!isAuthenticated) {
    if (activeMenu === "officialNotice") {
      return (
        <main className="profile-page">
          <div className="dt-container profile-page__container">
            <OfficialAnnouncementSection showLoginPrompt onLogin={() => openAuthModal("login")} />
          </div>
        </main>
      );
    }
    return (
      <main className="profile-page">
        <div className="dt-container profile-page__container">
          <StatusState
            title="订单中心需要先登录"
            description="登录后可查看购买订单、出售订单、实名认证、钱包、消息和分销统计。"
            action={<Button onClick={() => openAuthModal("login")}>立即登录</Button>}
          />
        </div>
      </main>
    );
  }

  return (
    <main className="profile-page">
      <div className="dt-container profile-page__container">
        {showProfileOverview ? (
          <>
            <section className="profile-overview-card">
              <div className="profile-overview-card__identity">
                {settingsProfile?.avatarUrl ? (
                  <img alt="用户头像" className="profile-avatar-badge profile-avatar-badge--image" src={settingsProfile.avatarUrl} />
                ) : (
                  <div className="profile-avatar-badge" aria-hidden="true">
                    {profileLabel.nickname.slice(0, 1)}
                  </div>
                )}
                <div className="profile-overview-card__meta">
                  <strong>{profileLabel.nickname}</strong>
                  <p>{profileLabel.phone}</p>
                  <div className="profile-overview-card__chips">
                    <span>{profileLabel.verifyLabel}</span>
                    <span>{profileLabel.passwordLabel}</span>
                    <span>交易受平台 IM 监管</span>
                  </div>
                </div>
              </div>
              <div className="profile-overview-card__actions">
                <button
                  className={`profile-action-link ${session?.profile.verified ? "profile-action-link--ghost" : ""}`}
                  onClick={() => setActiveMenu(setSearchParams, "verify")}
                  type="button"
                >
                  {session?.profile.verified ? "查看实名" : "实名认证"}
                </button>
              </div>
            </section>

            <nav className="profile-menu-tabs" aria-label="账户功能菜单">
              {PROFILE_MENU_ITEMS.map((item) => (
                <button
                  className={`profile-menu-tabs__item ${activeMenu === item.key ? "is-active" : ""}`}
                  key={item.key}
                  onClick={() => {
                    setActiveMenu(setSearchParams, item.key);
                  }}
                  type="button"
                >
                  <strong>{item.label}</strong>
                  <span>{item.description}</span>
                </button>
              ))}
            </nav>

            <section className="profile-stats-grid">
              <StatsCard label="钱包余额" value={orderMetrics.walletAmount} suffix="元" tone="blue" />
              <StatsCard label="订单总数" value={String(orderMetrics.totalCount)} suffix="单" tone="teal" />
              <StatsCard label="待处理订单" value={String(orderMetrics.pendingCount + orderMetrics.progressCount)} suffix="单" tone="gold" />
              <StatsCard label="已完成订单" value={String(orderMetrics.completedCount)} suffix="单" tone="rose" />
            </section>
          </>
        ) : null}

        {showMobileSubpageHeader ? (
          <section className="profile-mobile-subpage-header">
            <button type="button" onClick={() => setSearchParams({})}>
              返回
            </button>
            <div>
              <strong>{getProfileMenuLabel(activeMenu)}</strong>
              <span>个人中心</span>
            </div>
          </section>
        ) : null}

        {showProfileContent && activeMenu === "orders" ? (
          <OrderCenterSection
            role={orderRole}
            status={orderStatus}
            range={orderRange}
            startDate={orderStartDate}
            endDate={orderEndDate}
            loadStatus={orderLoadStatus}
            error={orderError}
            response={orderCenter}
            detail={orderDetail}
            detailStatus={orderDetailStatus}
            detailError={orderDetailError}
            notice={orderNotice}
            actionLoadingKey={orderActionLoadingKey}
            onRoleChange={(next) => {
              setOrderRole(next);
              setSearchParams(next === "BUY" ? { tab: "orders" } : { tab: "orders", role: next });
              setOrderNotice("");
            }}
            onStatusChange={(next) => setOrderStatus(next)}
            onRangeChange={(next) => {
              setOrderRange(next);
              if (next !== "CUSTOM") {
                setOrderStartDate("");
                setOrderEndDate("");
              }
            }}
            onCustomDateChange={(field, value) => {
              if (field === "startDate") {
                setOrderStartDate(value);
              } else {
                setOrderEndDate(value);
              }
            }}
            onViewDetail={(orderNo) => setActiveOrderNo(orderNo)}
            onCloseDetail={() => {
              setActiveOrderNo("");
              setOrderDetail(null);
              setOrderDetailStatus("idle");
              setOrderDetailError("");
            }}
            onPayOrder={async (orderNo) => {
              setOrderActionLoadingKey(`pay:${orderNo}`);
              try {
                const payload = await createTradeWechatPayment(orderNo, preferredWechatTradeType());
                setWechatPayScene("ORDER");
                setWechatPayPayload(payload);
                setOrderNotice("已生成微信支付，请按弹层提示完成付款。");
              } catch (error) {
                setOrderNotice(error instanceof Error ? error.message : "微信支付创建失败");
              } finally {
                setOrderActionLoadingKey("");
              }
            }}
            onCancelOrder={async (orderNo) => {
              setOrderActionLoadingKey(`cancel:${orderNo}`);
              try {
                const next = await cancelOrder(orderNo);
                syncOrderDetailIfOpen(orderNo, next);
                await refreshOrderCenter();
                setOrderNotice(resolveCancelOrderNotice(next));
              } catch (error) {
                setOrderNotice(error instanceof Error ? error.message : "取消订单失败");
              } finally {
                setOrderActionLoadingKey("");
              }
            }}
            onApplyRefund={async (orderNo) => {
              const reason = window.prompt("请输入退款原因", "买家申请退款") ?? "";
              setOrderActionLoadingKey(`refund:${orderNo}`);
              try {
                const next = await applyOrderRefund(orderNo, reason);
                syncOrderDetailIfOpen(orderNo, next);
                await refreshOrderCenter();
                setOrderNotice("退款申请已提交，等待卖家审核。");
              } catch (error) {
                setOrderNotice(error instanceof Error ? error.message : "申请退款失败");
              } finally {
                setOrderActionLoadingKey("");
              }
            }}
            onReviewRefund={async (orderNo, action) => {
              const note = window.prompt(action === "APPROVE" ? "请输入同意退款备注" : "请输入拒绝退款原因", action === "APPROVE" ? "同意退款" : "");
              if (!note || !note.trim()) {
                setOrderNotice("请填写退款审核备注。");
                return;
              }
              setOrderActionLoadingKey(`refund-review:${orderNo}`);
              try {
                const next = await reviewOrderRefund(orderNo, action, note.trim());
                syncOrderDetailIfOpen(orderNo, next);
                await refreshOrderCenter();
                await refreshWallet();
                setOrderNotice(action === "APPROVE" ? "已同意退款，款项已退回买家钱包。" : "已拒绝退款，订单已转入售后。");
              } catch (error) {
                setOrderNotice(error instanceof Error ? error.message : "退款审核失败");
              } finally {
                setOrderActionLoadingKey("");
              }
            }}
            onApplyAfterSale={async (orderNo) => {
              setOrderActionLoadingKey(`after-sale:${orderNo}`);
              try {
                const next = await applyOrderAfterSale(orderNo);
                syncOrderDetailIfOpen(orderNo, next);
                await refreshOrderCenter();
                setOrderNotice("售后申请已提交。");
              } catch (error) {
                setOrderNotice(error instanceof Error ? error.message : "申请售后失败");
              } finally {
                setOrderActionLoadingKey("");
              }
            }}
            onConfirmComplete={async (orderNo) => {
              setOrderActionLoadingKey(`confirm:${orderNo}`);
              try {
                const next = await confirmTradeOrder(orderNo);
                syncOrderDetailIfOpen(orderNo, next);
                await refreshOrderCenter();
                await refreshWallet();
                setOrderNotice(next.statusCode === "COMPLETED" ? "双方已确认完成，租金已结算，押金已退还买家钱包。" : "已提交确认，等待对方确认完成。");
              } catch (error) {
                setOrderNotice(error instanceof Error ? error.message : "确认完成失败");
              } finally {
                setOrderActionLoadingKey("");
              }
            }}
            onRefreshOrders={refreshOrderCenter}
            onDeleteOrder={async (orderNo) => {
              if (!window.confirm("确认删除该订单？删除后当前账号的订单中心不再展示。")) {
                return;
              }
              setOrderActionLoadingKey(`delete:${orderNo}`);
              try {
                await deleteOrder(orderNo);
                setActiveOrderNo((current) => (current === orderNo ? "" : current));
                setOrderDetail((current) => (current?.orderNo === orderNo ? null : current));
                await refreshOrderCenter();
                setOrderNotice("订单已删除。");
              } catch (error) {
                setOrderNotice(error instanceof Error ? error.message : "删除订单失败");
              } finally {
                setOrderActionLoadingKey("");
              }
            }}
            onEnterChat={(item) => {
              if (!item.chatGroupNo) {
                setOrderNotice("当前订单尚未生成群聊，请稍后再试。");
                return;
              }
              navigate(`/im/${item.chatGroupNo}`, {
                state: {
                  from: "/profile",
                },
              });
            }}
            onDownloadCertificate={async (orderNo) => {
              setOrderActionLoadingKey(`certificate:${orderNo}`);
              try {
                const certificate = await loadOrderCertificate(orderNo);
                const blob = new Blob([certificate.content], { type: "text/plain;charset=utf-8" });
                const url = window.URL.createObjectURL(blob);
                const anchor = document.createElement("a");
                anchor.href = url;
                anchor.download = certificate.filename;
                anchor.click();
                window.URL.revokeObjectURL(url);
              } catch (error) {
                setOrderNotice(error instanceof Error ? error.message : "订单凭证下载失败");
              } finally {
                setOrderActionLoadingKey("");
              }
            }}
          />
        ) : null}

        {showProfileContent && activeMenu === "myListings" ? (
          <MyListingsPage embedded />
        ) : null}

        {showProfileContent && activeMenu === "verify" ? (
          <section className="profile-feature-layout">
            <section className="profile-panel">
              <header className="profile-panel__header">
                <div>
                  <h2>实名认证</h2>
                  <p>填写姓名和身份证号后进入人脸活体认证，认证完成会自动同步实名状态。</p>
                </div>
                <span className={`profile-status-badge ${isRealNameVerified ? "profile-status-badge--success" : ""}`}>
                  {isRealNameVerified ? "已实名" : "待认证"}
                </span>
              </header>
              {renderRealNamePanel({
                loadStatus: realNameLoadStatus,
                error: realNameError,
                profile: realNameProfile,
                form: realNameForm,
                fieldErrors: realNameFieldErrors,
                submitting: submittingRealName,
                onChange: (field, value) => {
                  setRealNameForm((current) => ({ ...current, [field]: value }));
                  setRealNameFieldErrors((current) => ({ ...current, [field]: "" }));
                },
                onSubmit: async () => {
                  const nextErrors = validateRealNameForm(realNameForm);
                  setRealNameFieldErrors(nextErrors);
                  if (nextErrors.realName || nextErrors.idCardNo) {
                    return;
                  }

                  setSubmittingRealName(true);
                  setRealNameError("");
                  try {
                    const result = await startFaceRealName(realNameForm.realName, realNameForm.idCardNo);
                    window.sessionStorage.setItem(FACE_REAL_NAME_ORDER_KEY, result.orderId);
                    if (isMobileRuntime()) {
                      window.location.href = result.verifyUrl;
                      return;
                    }
                    setFaceQrDialog({
                      orderId: result.orderId,
                      verifyUrl: result.verifyUrl,
                      expireAt: result.expireAt,
                      status: "WAITING",
                      message: "请使用手机扫码完成活体认证。",
                    });
                  } catch (error) {
                    setRealNameError(error instanceof Error ? error.message : "实名认证发起失败");
                    setSubmittingRealName(false);
                  }
                },
              })}
            </section>
            {faceQrDialog ? (
              <FaceRealNameQrDialog
                dialog={faceQrDialog}
                onClose={() => {
                  setFaceQrDialog(null);
                  setSubmittingRealName(false);
                }}
              />
            ) : null}
          </section>
        ) : null}

        {showProfileContent && activeMenu === "studioApply" ? (
          <section className="profile-feature-layout">
            <section className="profile-panel">
              <header className="profile-panel__header">
                <div>
                  <h2>申请成为工作室</h2>
                  <p>提交工作室基础资料与营业执照后进入后台审核。审核通过会自动开通工作室身份和工作室后台访问权限。</p>
                </div>
                <span
                  className={`profile-status-badge ${
                    studioApplicationProfile?.status === "APPROVED"
                      ? "profile-status-badge--success"
                      : studioApplicationProfile?.status === "REJECTED"
                        ? "profile-status-badge--warning"
                        : ""
                  }`}
                >
                  {studioApplicationProfile?.statusText ?? "未提交"}
                </span>
              </header>

              {studioApplicationNotice ? (
                <div className="app-toast" role="status" aria-live="polite">
                  <span>{studioApplicationNotice}</span>
                  <button onClick={() => setStudioApplicationNotice("")} type="button">关闭</button>
                </div>
              ) : null}

              {studioApplicationLoadStatus === "loading" || studioApplicationLoadStatus === "idle" ? (
                <StatusState title="工作室申请信息加载中" description="正在同步你的入驻申请与审核状态。" />
              ) : studioApplicationLoadStatus === "error" ? (
                <StatusState title="工作室申请信息加载失败" description={studioApplicationError} tone="error" />
              ) : (
                <div className="profile-studio-apply-grid">
                  <section className="profile-settings-card">
                    <div className="profile-settings-card__header">
                      <h3>申请状态</h3>
                      <span>{studioApplicationProfile?.applicationNo || "暂未生成申请单号"}</span>
                    </div>
                    <div className="profile-info-block profile-info-block--studio">
                      <dl className="profile-detail-grid">
                        <div><dt>当前状态</dt><dd>{studioApplicationProfile?.statusText || "未提交"}</dd></div>
                        <div><dt>平台账号 ID</dt><dd>{studioApplicationProfile?.applicantUserId || "-"}</dd></div>
                        <div><dt>账号昵称</dt><dd>{studioApplicationProfile?.applicantNickname || profileLabel.nickname}</dd></div>
                        <div><dt>绑定手机号</dt><dd>{maskPhone(studioApplicationProfile?.applicantPhone || session?.profile.phone || "")}</dd></div>
                        <div><dt>提交时间</dt><dd>{studioApplicationProfile?.createdAt || "-"}</dd></div>
                        <div><dt>审核时间</dt><dd>{studioApplicationProfile?.reviewedAt || "-"}</dd></div>
                        {studioApplicationProfile?.hasStudioAccess ? <div><dt>工作室后台</dt><dd>已开通</dd></div> : null}
                        {studioApplicationProfile?.reviewStrategy ? <div><dt>发布策略</dt><dd>{studioApplicationProfile.reviewStrategy === "DIRECT_PUBLISH" ? "免审直发" : "需要审核"}</dd></div> : null}
                      </dl>
                      {studioApplicationProfile?.status === "REJECTED" && studioApplicationProfile.rejectReason ? (
                        <div className="profile-warning-card">
                          <strong>驳回原因</strong>
                          <p>{studioApplicationProfile.rejectReason}</p>
                        </div>
                      ) : null}
                      {studioApplicationProfile?.status === "PENDING" ? (
                        <div className="profile-info-callout">资料已提交，后台审核通过后会自动开通工作室身份。</div>
                      ) : null}
                      {studioApplicationProfile?.status === "APPROVED" ? (
                        <div className="profile-info-callout profile-info-callout--success">
                          工作室身份已开通，你现在可以进入工作室后台管理账号与订单。
                        </div>
                      ) : null}
                    </div>
                  </section>

                  <section className="profile-settings-card">
                    <div className="profile-settings-card__header">
                      <h3>{studioApplicationProfile?.status === "REJECTED" ? "重新提交申请" : "入驻资料"}</h3>
                      <span>资质材料上传</span>
                    </div>
                    <div className="profile-settings-list">
                      <div className="profile-inline-form">
                        <div className="profile-input-stack">
                          <span>工作室名称</span>
                          <input
                            disabled={studioApplicationProfile?.status === "PENDING" || studioApplicationProfile?.status === "APPROVED"}
                            placeholder="请输入工作室展示名称"
                            value={studioApplicationForm.studioName}
                            onChange={(event) => {
                              setStudioApplicationForm((current) => ({ ...current, studioName: event.target.value }));
                              setStudioApplicationFieldErrors((current) => ({ ...current, studioName: "" }));
                            }}
                          />
                          {studioApplicationFieldErrors.studioName ? <small className="profile-field-error">{studioApplicationFieldErrors.studioName}</small> : null}
                        </div>
                        <div className="profile-input-stack">
                          <span>统一社会信用代码 / 主体说明</span>
                          <input
                            disabled={studioApplicationProfile?.status === "PENDING" || studioApplicationProfile?.status === "APPROVED"}
                            placeholder="请输入统一社会信用代码或主体说明"
                            value={studioApplicationForm.qualificationCode}
                            onChange={(event) => {
                              setStudioApplicationForm((current) => ({ ...current, qualificationCode: event.target.value }));
                              setStudioApplicationFieldErrors((current) => ({ ...current, qualificationCode: "" }));
                            }}
                          />
                          {studioApplicationFieldErrors.qualificationCode ? <small className="profile-field-error">{studioApplicationFieldErrors.qualificationCode}</small> : null}
                        </div>
                        <div className="profile-inline-fields">
                          <label className="profile-input-stack">
                            <span>联系人</span>
                            <input
                              disabled={studioApplicationProfile?.status === "PENDING" || studioApplicationProfile?.status === "APPROVED"}
                              placeholder="请输入联系人姓名"
                              value={studioApplicationForm.contactName}
                              onChange={(event) => {
                                setStudioApplicationForm((current) => ({ ...current, contactName: event.target.value }));
                                setStudioApplicationFieldErrors((current) => ({ ...current, contactName: "" }));
                              }}
                            />
                            {studioApplicationFieldErrors.contactName ? <small className="profile-field-error">{studioApplicationFieldErrors.contactName}</small> : null}
                          </label>
                          <label className="profile-input-stack">
                            <span>联系电话</span>
                            <input
                              disabled={studioApplicationProfile?.status === "PENDING" || studioApplicationProfile?.status === "APPROVED"}
                              placeholder="请输入联系电话"
                              value={studioApplicationForm.contactPhone}
                              onChange={(event) => {
                                setStudioApplicationForm((current) => ({ ...current, contactPhone: event.target.value }));
                                setStudioApplicationFieldErrors((current) => ({ ...current, contactPhone: "" }));
                              }}
                            />
                            {studioApplicationFieldErrors.contactPhone ? <small className="profile-field-error">{studioApplicationFieldErrors.contactPhone}</small> : null}
                          </label>
                        </div>
                        <label className="profile-input-stack">
                          <span>工作室补充说明</span>
                          <textarea
                            disabled={studioApplicationProfile?.status === "PENDING" || studioApplicationProfile?.status === "APPROVED"}
                            maxLength={255}
                            placeholder="可填写主体说明、线下签约情况或补充备注"
                            rows={4}
                            value={studioApplicationForm.qualificationNote}
                            onChange={(event) => setStudioApplicationForm((current) => ({ ...current, qualificationNote: event.target.value }))}
                          />
                        </label>

                        <div className="profile-upload-panel">
                          <div className="profile-upload-panel__meta">
                            <strong>营业执照 / 证明材料</strong>
                            <p>支持 JPG / PNG，上传后可用于平台审核。</p>
                            {studioApplicationFieldErrors.qualificationMaterial ? (
                              <small className="profile-field-error">{studioApplicationFieldErrors.qualificationMaterial}</small>
                            ) : null}
                          </div>
                          <div className="profile-upload-panel__content">
                            {studioQualificationAsset?.previewUrl ? (
                              <a href={studioQualificationAsset.previewUrl} rel="noreferrer" target="_blank" className="profile-upload-panel__preview">
                                <img alt="工作室资质材料预览" src={studioQualificationAsset.previewUrl} />
                              </a>
                            ) : (
                              <div className="profile-upload-panel__placeholder">尚未上传资质材料</div>
                            )}
                            <div className="profile-upload-panel__actions">
                              <input
                                accept="image/jpeg,image/png"
                                id="studio-qualification-upload"
                                style={{ display: "none" }}
                                type="file"
                                onChange={async (event) => {
                                  const file = event.target.files?.[0];
                                  event.currentTarget.value = "";
                                  if (!file) {
                                    return;
                                  }
                                  try {
                                    validateProfileImageFile(file, "营业执照或证明材料");
                                    setStudioApplicationUploadLoading(true);
                                    const uploaded = await uploadProfileImage(file, "studio-qualification");
                                    setStudioQualificationAsset(uploaded);
                                    setStudioApplicationFieldErrors((current) => ({ ...current, qualificationMaterial: "" }));
                                  } catch (error) {
                                    setStudioApplicationNotice(error instanceof Error ? error.message : "资质材料上传失败");
                                  } finally {
                                    setStudioApplicationUploadLoading(false);
                                  }
                                }}
                              />
                              <label
                                className="dt-button dt-button--secondary publish-upload-trigger"
                                htmlFor="studio-qualification-upload"
                              >
                                {studioApplicationUploadLoading ? "上传中..." : "上传资质材料"}
                              </label>
                              {studioQualificationAsset?.previewUrl && studioApplicationProfile?.status !== "APPROVED" ? (
                                <button
                                  type="button"
                                  className="profile-inline-button"
                                  onClick={() => setStudioQualificationAsset(null)}
                                >
                                  删除材料
                                </button>
                              ) : null}
                            </div>
                          </div>
                        </div>

                        <div className="profile-settings-actions">
                          <Button
                            disabled={studioApplicationSubmitting || studioApplicationProfile?.status === "PENDING" || studioApplicationProfile?.status === "APPROVED"}
                            onClick={async () => {
                              const nextErrors: {
                                studioName?: string;
                                qualificationCode?: string;
                                contactName?: string;
                                contactPhone?: string;
                                qualificationMaterial?: string;
                              } = {};
                              if (studioApplicationForm.studioName.trim().length < 2) nextErrors.studioName = "请填写工作室名称";
                              if (studioApplicationForm.qualificationCode.trim().length < 6) nextErrors.qualificationCode = "请填写统一社会信用代码或主体说明";
                              if (studioApplicationForm.contactName.trim().length < 2) nextErrors.contactName = "请填写联系人";
                              if (studioApplicationForm.contactPhone.trim().length < 6) nextErrors.contactPhone = "请填写联系电话";
                              if (!studioQualificationAsset?.objectKey) nextErrors.qualificationMaterial = "请上传营业执照或证明材料";
                              setStudioApplicationFieldErrors(nextErrors);
                              if (Object.values(nextErrors).some(Boolean)) {
                                return;
                              }
                              setStudioApplicationSubmitting(true);
                              try {
                                const next = await submitStudioApplication({
                                  studioName: studioApplicationForm.studioName.trim(),
                                  qualificationCode: studioApplicationForm.qualificationCode.trim(),
                                  qualificationNote: studioApplicationForm.qualificationNote.trim(),
                                  contactName: studioApplicationForm.contactName.trim(),
                                  contactPhone: studioApplicationForm.contactPhone.trim(),
                                  qualificationMaterialKey: studioQualificationAsset?.objectKey ?? "",
                                });
                                setStudioApplicationProfile(next);
                                setStudioApplicationNotice("申请已提交，等待管理员审核。");
                                setStudioApplicationLoadStatus("success");
                              } catch (error) {
                                setStudioApplicationNotice(error instanceof Error ? error.message : "工作室申请提交失败");
                              } finally {
                                setStudioApplicationSubmitting(false);
                              }
                            }}
                          >
                            {studioApplicationSubmitting ? "提交中..." : studioApplicationProfile?.status === "REJECTED" ? "重新提交申请" : "提交入驻申请"}
                          </Button>
                          <Button
                            kind="secondary"
                            onClick={() => void refreshStudioApplication()}
                          >
                            刷新状态
                          </Button>
                        </div>
                      </div>
                    </div>
                  </section>
                </div>
              )}
            </section>
          </section>
        ) : null}

        {showProfileContent && activeMenu === "wallet" ? (
          <section className="profile-feature-layout">
            <section className="profile-panel">
              <header className="profile-panel__header">
                <div>
                  <h2>我的钱包</h2>
                  <p>充值仅支持微信支付，支付回调确认成功后余额才会入账；提现会生成待审核申请并冻结金额。</p>
                </div>
                <div className="profile-wallet-actions">
                  <button type="button" onClick={() => setWalletNotice("充值需要完成微信支付并等待平台确认到账，确认后余额和流水会自动刷新。")}>
                    钱包说明
                  </button>
                  <button type="button" onClick={() => void refreshWallet()}>
                    刷新余额
                  </button>
                </div>
              </header>

              {walletNotice ? (
                <div className="app-toast" role="status" aria-live="polite">
                  <span>{walletNotice}</span>
                  <button onClick={() => setWalletNotice("")} type="button">关闭</button>
                </div>
              ) : null}

              {walletLoadStatus === "loading" || walletLoadStatus === "idle" ? (
                <StatusState title="钱包数据加载中" description="正在同步可用余额、冻结金额和最近流水。" />
              ) : walletLoadStatus === "error" ? (
                <StatusState title="钱包数据加载失败" description={walletError} tone="error" />
              ) : (
                <>
                  <div className="profile-wallet-summary">
                    <WalletStatCard label="可用余额" value={`¥${walletOverview?.availableBalance.toFixed(2) ?? "0.00"}`} tone="blue" />
                    <WalletStatCard label="冻结金额" value={`¥${walletOverview?.frozenBalance.toFixed(2) ?? "0.00"}`} tone="orange" />
                    <WalletStatCard label="累计佣金" value={`¥${walletOverview?.totalCommission.toFixed(2) ?? "0.00"}`} tone="teal" />
                  </div>

                  <div className="profile-operation-grid">
                    <section className="profile-operation-card">
                      <div className="profile-operation-card__header">
                        <h3>充值</h3>
                        <span>最小 10 元</span>
                      </div>
                      <div className="profile-inline-form">
                        <label className="profile-input-stack">
                          <span>充值金额</span>
                          <input
                            inputMode="decimal"
                            placeholder="请输入充值金额"
                            value={rechargeForm.amount}
                            onChange={(event) => setRechargeForm((current) => ({ ...current, amount: event.target.value }))}
                          />
                        </label>
                        <label className="profile-input-stack">
                          <span>支付方式</span>
                          <input value="微信" readOnly />
                        </label>
                        <Button
                          disabled={walletLoadingKey === "recharge"}
                          onClick={async () => {
                            const amount = Number(rechargeForm.amount);
                            if (!Number.isFinite(amount) || amount < 10) {
                              setWalletNotice("充值金额不能低于 10 元");
                              return;
                            }
                            setWalletLoadingKey("recharge");
                            try {
                              const payload = await createWalletWechatRecharge(amount, preferredWechatTradeType());
                              setWechatPayScene("WALLET");
                              setWechatPayPayload(payload);
                              setWalletNotice("已生成微信支付，请完成付款后等待平台确认到账。");
                            } catch (error) {
                              setWalletNotice(error instanceof Error ? error.message : "充值失败");
                            } finally {
                              setWalletLoadingKey("");
                            }
                          }}
                        >
                          {walletLoadingKey === "recharge" ? "充值中..." : "立即充值"}
                        </Button>
                      </div>
                    </section>

                    <section className="profile-operation-card">
                      <div className="profile-operation-card__header">
                        <h3>提现账户</h3>
                        <span>{walletOverview?.withdrawAccount ? "已绑定" : "待绑定"}</span>
                      </div>
                      <div className="profile-inline-form">
                        <p className="profile-inline-hint">
                          当前账户：{walletOverview?.withdrawAccount ? `${walletOverview.withdrawAccount.accountName} / ${walletOverview.withdrawAccount.maskedAccountNo}` : "未绑定"}
                        </p>
                        {walletOverview?.withdrawAccount?.qrCodeUrl ? (
                          <a className="profile-withdraw-qrcode-preview" href={walletOverview.withdrawAccount.qrCodeUrl} target="_blank" rel="noreferrer">
                            <img alt="当前收款码" src={walletOverview.withdrawAccount.qrCodeUrl} />
                            <span>当前收款码</span>
                          </a>
                        ) : null}
                        <label className="profile-input-stack">
                          <span>提现渠道</span>
                          <select
                            value={withdrawAccountForm.channel}
                            onChange={(event) => setWithdrawAccountForm((current) => ({ ...current, channel: event.target.value as WalletChannel }))}
                          >
                            <option value="ALIPAY">支付宝</option>
                            <option value="WECHAT">微信</option>
                          </select>
                        </label>
                        <label className="profile-input-stack">
                          <span>账户姓名</span>
                          <input
                            placeholder="需与实名认证姓名一致"
                            value={withdrawAccountForm.accountName}
                            onChange={(event) => setWithdrawAccountForm((current) => ({ ...current, accountName: event.target.value }))}
                          />
                        </label>
                        <label className="profile-input-stack">
                          <span>账户号</span>
                          <input
                            placeholder="请输入支付宝账号或微信号"
                            value={withdrawAccountForm.accountNo}
                            onChange={(event) => setWithdrawAccountForm((current) => ({ ...current, accountNo: event.target.value }))}
                          />
                        </label>
                        <div className="profile-withdraw-qrcode-field">
                          <div>
                            <span>收款码图片</span>
                            <p>支持 JPG / PNG / WEBP，绑定后后台审核提现时可查看。</p>
                          </div>
                          <div className="profile-withdraw-qrcode-field__content">
                            {withdrawQrCodeAsset?.previewUrl ? (
                              <img alt="收款码预览" src={withdrawQrCodeAsset.previewUrl} />
                            ) : (
                              <div className="profile-withdraw-qrcode-field__placeholder">未选择图片</div>
                            )}
                            <label className="profile-upload-action">
                              <input
                                accept="image/jpeg,image/png,image/webp"
                                disabled={walletLoadingKey === "upload-qrcode"}
                                type="file"
                                onChange={async (event) => {
                                  const file = event.target.files?.[0];
                                  event.target.value = "";
                                  if (!file) return;
                                  try {
                                    validateProfileImageFile(file, "收款码图片", ["image/jpeg", "image/png", "image/webp"]);
                                    setWalletLoadingKey("upload-qrcode");
                                    const uploaded = await uploadProfileImage(file, "withdraw-qr-codes");
                                    setWithdrawQrCodeAsset(uploaded);
                                    setWalletNotice("收款码图片已上传，请继续绑定账户。");
                                  } catch (error) {
                                    setWalletNotice(error instanceof Error ? error.message : "收款码图片上传失败");
                                  } finally {
                                    setWalletLoadingKey("");
                                  }
                                }}
                              />
                              {walletLoadingKey === "upload-qrcode" ? "上传中..." : withdrawQrCodeAsset ? "更换图片" : "上传图片"}
                            </label>
                            {withdrawQrCodeAsset ? (
                              <button className="profile-link-button" type="button" onClick={() => setWithdrawQrCodeAsset(null)}>移除</button>
                            ) : null}
                          </div>
                        </div>
                        <Button
                          disabled={walletLoadingKey === "bind-account"}
                          onClick={async () => {
                            if (!withdrawAccountForm.accountName.trim() || !withdrawAccountForm.accountNo.trim()) {
                              setWalletNotice("请完整填写提现账户信息");
                              return;
                            }
                            setWalletLoadingKey("bind-account");
                            try {
                              await bindWithdrawAccount(
                                withdrawAccountForm.channel,
                                withdrawAccountForm.accountName,
                                withdrawAccountForm.accountNo,
                                withdrawQrCodeAsset?.objectKey ?? null
                              );
                              await refreshWallet();
                              await refreshSettings();
                              setWalletNotice("提现账户绑定成功。");
                              setWithdrawAccountForm((current) => ({ ...current, accountName: "", accountNo: "" }));
                              setWithdrawQrCodeAsset(null);
                            } catch (error) {
                              setWalletNotice(error instanceof Error ? error.message : "绑定提现账户失败");
                            } finally {
                              setWalletLoadingKey("");
                            }
                          }}
                        >
                          {walletLoadingKey === "bind-account" ? "保存中..." : "绑定账户"}
                        </Button>
                      </div>
                    </section>

                    <section className="profile-operation-card">
                      <div className="profile-operation-card__header">
                        <h3>提现申请</h3>
                        <span>提交后转入冻结金额</span>
                      </div>
                      <div className="profile-inline-form">
                        <p className="profile-inline-hint">当前可提现：¥{walletOverview?.availableBalance.toFixed(2) ?? "0.00"}</p>
                        <label className="profile-input-stack">
                          <span>提现金额</span>
                          <input
                            inputMode="decimal"
                            placeholder="请输入提现金额"
                            value={withdrawForm.amount}
                            onChange={(event) => setWithdrawForm({ amount: event.target.value })}
                          />
                        </label>
                        <Button
                          disabled={walletLoadingKey === "withdraw"}
                          onClick={async () => {
                            const amount = Number(withdrawForm.amount);
                            if (!Number.isFinite(amount) || amount < 10) {
                              setWalletNotice("提现金额不能低于 10 元");
                              return;
                            }
                            setWalletLoadingKey("withdraw");
                            try {
                              const next = await applyWithdraw(amount);
                              setWalletOverview(next);
                              setWalletNotice("提现申请已提交，金额已转入冻结。");
                              setWithdrawForm({ amount: "" });
                            } catch (error) {
                              setWalletNotice(error instanceof Error ? error.message : "提现申请失败");
                            } finally {
                              setWalletLoadingKey("");
                            }
                          }}
                        >
                          {walletLoadingKey === "withdraw" ? "提交中..." : "提交提现"}
                        </Button>
                      </div>
                    </section>
                  </div>

                  {walletOverview?.records.length ? (
                    <div className="profile-record-list">
                      {walletOverview.records.map((item) => (
                        <article className="profile-record-card" key={item.id}>
                          <div className="profile-record-card__main">
                            <div className="profile-record-card__topline">
                              <span className="profile-record-tag">{item.status}</span>
                              <strong>{item.title}</strong>
                            </div>
                            <p>{item.time}</p>
                            {item.referenceNo ? <p>单号 {item.referenceNo}</p> : null}
                          </div>
                          <div className="profile-record-card__side">
                            <strong>{item.amount}</strong>
                            <span>{renderChannelLabel(item.channel)}</span>
                          </div>
                        </article>
                      ))}
                    </div>
                  ) : (
                    <StatusState title="暂无钱包记录" description="可以先尝试充值或绑定提现账户。" />
                  )}
                </>
              )}
            </section>
          </section>
        ) : null}

        {showProfileContent && activeMenu === "messages" ? (
          <section className="profile-feature-layout">
            <section className="profile-panel">
              <header className="profile-panel__header">
                <div>
                  <h2>我的消息</h2>
                  <p>系统公告、交易通知、客服消息和分销通知都已接成真实分类，并支持已读与删除。</p>
                </div>
                <div className="profile-panel__header-side">
                  <span>未读 {messageCenter?.counts.unread ?? 0}</span>
                  <span>总消息 {messageCenter?.counts.all ?? 0}</span>
                </div>
              </header>

              <div className="profile-tab-row">
                {MESSAGE_TABS.map((item) => (
                  <button
                    className={`profile-chip-tab ${messageCategory === item.key ? "is-active" : ""}`}
                    key={item.key}
                    onClick={() => setMessageCategory(item.key)}
                    type="button"
                  >
                    {item.label}
                    {messageCenter ? <small>{renderMessageCount(messageCenter.counts, item.key)}</small> : null}
                  </button>
                ))}
              </div>

              <div className="profile-message-toolbar">
                <div className="profile-message-toolbar__selection">
                  <label className={`profile-message-select-all ${messageSelectAllDisabled ? "is-disabled" : ""}`}>
                    <input
                      checked={allVisibleMessagesSelected}
                      disabled={messageSelectAllDisabled}
                      type="checkbox"
                      onChange={(event) => setSelectedMessageIds(event.target.checked ? visibleMessageIds : [])}
                    />
                    <span>{allVisibleMessagesSelected ? "取消全选" : "全选"}</span>
                  </label>
                  <span>已选 {selectedMessages.length} 条</span>
                </div>
                <div className="profile-message-toolbar__actions">
                  <button
                    disabled={messageActionLoading === "read-all"}
                    onClick={async () => {
                      setMessageActionLoading("read-all");
                      try {
                        const next = await markAllMessagesRead(messageCategory);
                        setMessageCenter(next);
                        setSelectedMessageIds([]);
                      } catch (error) {
                        setMessageError(error instanceof Error ? error.message : "全部已读失败");
                      } finally {
                        setMessageActionLoading("");
                      }
                    }}
                    type="button"
                  >
                    全部已读
                  </button>
                  <button
                    disabled={!selectedMessages.length || messageActionLoading === "read"}
                    onClick={async () => {
                      setMessageActionLoading("read");
                      try {
                        const next = await markMessagesRead(selectedMessageIds, messageCategory);
                        setMessageCenter(next);
                        setSelectedMessageIds([]);
                      } catch (error) {
                        setMessageError(error instanceof Error ? error.message : "标记已读失败");
                      } finally {
                        setMessageActionLoading("");
                      }
                    }}
                    type="button"
                  >
                    标记选中已读
                  </button>
                  <button
                    disabled={!selectedMessages.length || messageActionLoading === "delete"}
                    onClick={async () => {
                      setMessageActionLoading("delete");
                      try {
                        const next = await deleteMessages(selectedMessageIds, messageCategory);
                        setMessageCenter(next);
                        setSelectedMessageIds([]);
                      } catch (error) {
                        setMessageError(error instanceof Error ? error.message : "删除消息失败");
                      } finally {
                        setMessageActionLoading("");
                      }
                    }}
                    type="button"
                  >
                    删除选中
                  </button>
                </div>
              </div>

              {messageLoadStatus === "loading" || messageLoadStatus === "idle" ? (
                <StatusState title="消息中心加载中" description="正在同步你的站内消息和未读状态。" />
              ) : messageLoadStatus === "error" ? (
                <StatusState title="消息中心加载失败" description={messageError} tone="error" />
              ) : messageCenter?.rows.length ? (
                <div className="profile-message-list">
                  {messageCenter.rows.map((item) => (
                    <article className={`profile-message-card ${item.unread ? "is-unread" : ""}`} key={item.id}>
                      <div className="profile-message-card__head">
                        <div className="profile-message-card__selection">
                          <input
                            checked={selectedMessageIds.includes(item.id)}
                            type="checkbox"
                            onChange={(event) =>
                              setSelectedMessageIds((current) =>
                                event.target.checked ? [...current, item.id] : current.filter((value) => value !== item.id),
                              )
                            }
                          />
                          <span className="profile-record-tag">{item.categoryLabel}</span>
                        </div>
                        <time>{item.time}</time>
                      </div>
                      <strong>{item.title}</strong>
                      <p>{item.content}</p>
                      <div className="profile-message-card__actions">
                        {item.unread ? (
                          <button
                            onClick={async () => {
                              setMessageActionLoading(`read-${item.id}`);
                              try {
                                const next = await markMessagesRead([item.id], messageCategory);
                                setMessageCenter(next);
                                setSelectedMessageIds((current) => current.filter((value) => value !== item.id));
                              } catch (error) {
                                setMessageError(error instanceof Error ? error.message : "标记已读失败");
                              } finally {
                                setMessageActionLoading("");
                              }
                            }}
                            type="button"
                          >
                            标记已读
                          </button>
                        ) : null}
                        <button
                          onClick={async () => {
                            setMessageActionLoading(`delete-${item.id}`);
                            try {
                              const next = await deleteMessages([item.id], messageCategory);
                              setMessageCenter(next);
                              setSelectedMessageIds((current) => current.filter((value) => value !== item.id));
                            } catch (error) {
                              setMessageError(error instanceof Error ? error.message : "删除消息失败");
                            } finally {
                              setMessageActionLoading("");
                            }
                          }}
                          type="button"
                        >
                          删除
                        </button>
                      </div>
                    </article>
                  ))}
                </div>
              ) : (
                <StatusState title="暂无消息" description="当前分类下没有任何消息记录。" />
              )}
            </section>
          </section>
        ) : null}

        {showProfileContent && activeMenu === "officialNotice" ? (
          <OfficialAnnouncementSection />
        ) : null}

        {showProfileContent && activeMenu === "coupons" ? (
          <section className="profile-feature-layout">
            <section className="profile-panel">
              <header className="profile-panel__header">
                <div>
                  <h2>优惠券中心</h2>
                  <p>把“可用优惠券 + 使用记录”拆成两层查看，后续下单页可直接复用同一份券数据。</p>
                </div>
                <div className="profile-panel__header-side">
                  <span>可用券：{couponCenter?.availableCount ?? 0}</span>
                  <span>历史券：{couponCenter?.historyCount ?? 0}</span>
                </div>
              </header>
              {couponLoadStatus === "loading" || couponLoadStatus === "idle" ? (
                <StatusState title="优惠券加载中" description="正在同步你的可用券和历史券记录。" />
              ) : couponLoadStatus === "error" ? (
                <StatusState title="优惠券加载失败" description={couponError} tone="error" />
              ) : (
                <>
                  <div className="profile-tab-row">
                    <button
                      className={`profile-chip-tab ${couponFilter === "available" ? "is-active" : ""}`}
                      onClick={() => setCouponFilter("available")}
                      type="button"
                    >
                      可用优惠券
                    </button>
                    <button
                      className={`profile-chip-tab ${couponFilter === "history" ? "is-active" : ""}`}
                      onClick={() => setCouponFilter("history")}
                      type="button"
                    >
                      使用记录
                    </button>
                  </div>
                  {(couponFilter === "available" ? couponBuckets.available : couponBuckets.history).length ? (
                    <div className="profile-coupon-grid">
                      {(couponFilter === "available" ? couponBuckets.available : couponBuckets.history).map((item) => (
                        <CouponCard key={item.id} record={item} />
                      ))}
                    </div>
                  ) : (
                    <StatusState
                      title={couponFilter === "available" ? "暂无可用优惠券" : "暂无历史优惠券"}
                      description={couponFilter === "available" ? "当前账户还没有可使用的优惠券。" : "当前账户还没有使用或失效的优惠券记录。"}
                    />
                  )}
                </>
              )}
            </section>
          </section>
        ) : null}

        {showProfileContent && activeMenu === "settings" ? (
          <section className="profile-feature-layout">
            <section className="profile-panel">
              <header className="profile-panel__header">
                <div>
                  <h2>设置与安全</h2>
                  <p>头像、昵称、密码、绑定管理、实名认证和安全设置都按用户中心真实链路处理，PC 与 H5 共用一套交互。</p>
                </div>
              </header>

              {settingsNotice ? (
                <div className="app-toast" role="status" aria-live="polite">
                  <span>{settingsNotice}</span>
                  <button onClick={() => setSettingsNotice("")} type="button">关闭</button>
                </div>
              ) : null}

              {settingsLoadStatus === "loading" || settingsLoadStatus === "idle" ? (
                <StatusState title="设置数据加载中" description="正在同步你的账号资料和绑定关系。" />
              ) : settingsLoadStatus === "error" ? (
                <StatusState title="设置数据加载失败" description={settingsError} tone="error" />
              ) : (
                <div className="profile-settings-grid">
                  <section className="profile-settings-card">
                    <div className="profile-settings-card__header">
                      <h3>账号设置</h3>
                      <span>基础资料</span>
                    </div>
                    <div className="profile-settings-list">
                      <div className="profile-setting-form">
                        <div className="profile-setting-form__meta">
                          <span>头像</span>
                          <strong>{settingsProfile?.avatarUrl ? "已上传头像" : "未设置头像"}</strong>
                        </div>
                        <div className="profile-avatar-setting">
                          <div className="profile-avatar-setting__preview">
                            {settingsProfile?.avatarUrl ? (
                              <img alt="当前头像" src={settingsProfile.avatarUrl} />
                            ) : (
                              <span aria-hidden="true">{profileLabel.nickname.slice(0, 1)}</span>
                            )}
                          </div>
                          <div className="profile-avatar-setting__content">
                            <p>支持 JPG / PNG，上传前自动压缩，上传后会立即更新头像预览。</p>
                            <input
                              accept="image/jpeg,image/png"
                              id="profile-avatar-upload"
                              style={{ display: "none" }}
                              type="file"
                              onChange={async (event) => {
                                const file = event.target.files?.[0];
                                event.currentTarget.value = "";
                                if (!file) {
                                  return;
                                }
                                try {
                                  validateProfileImageFile(file, "头像");
                                  setSettingsLoadingKey("avatar");
                                  const uploaded = await uploadProfileImage(file, "avatars");
                                  const next = await updateAvatar(uploaded.objectKey);
                                  setSettingsProfile(next);
                                  setSettingsNotice("头像更新成功。");
                                } catch (error) {
                                  setSettingsNotice(error instanceof Error ? error.message : "头像上传失败");
                                } finally {
                                  setSettingsLoadingKey("");
                                }
                              }}
                            />
                            <label className="dt-button dt-button--secondary publish-upload-trigger" htmlFor="profile-avatar-upload">
                              {settingsLoadingKey === "avatar" ? "上传中..." : "上传头像"}
                            </label>
                          </div>
                        </div>
                      </div>

                      <div className="profile-setting-form">
                        <div className="profile-setting-form__meta">
                          <span>当前昵称</span>
                          <strong>{settingsProfile?.nickname}</strong>
                        </div>
                        <div className="profile-inline-fields">
                          <input value={nicknameForm} onChange={(event) => setNicknameForm(event.target.value)} />
                          <Button
                            disabled={settingsLoadingKey === "nickname"}
                            onClick={async () => {
                              if (nicknameForm.trim().length < 2) {
                                setSettingsNotice("昵称长度不能少于 2 个字符");
                                return;
                              }
                              setSettingsLoadingKey("nickname");
                              try {
                                const next = await updateNickname(nicknameForm);
                                setSettingsProfile(next);
                                syncSessionProfile({ nickname: next.nickname });
                                setSettingsNotice("昵称修改成功。");
                              } catch (error) {
                                setSettingsNotice(error instanceof Error ? error.message : "昵称修改失败");
                              } finally {
                                setSettingsLoadingKey("");
                              }
                            }}
                          >
                            {settingsLoadingKey === "nickname" ? "保存中..." : "修改昵称"}
                          </Button>
                        </div>
                      </div>

                      <div className="profile-setting-form">
                        <div className="profile-setting-form__meta">
                          <span>登录密码</span>
                          <strong>{hasLoginPassword ? "已设置" : "未设置"}</strong>
                        </div>
                        <div className="profile-inline-fields profile-inline-fields--stack">
                          {hasLoginPassword ? (
                            <input
                              placeholder="原密码"
                              type="password"
                              value={passwordForm.currentPassword}
                              onChange={(event) => setPasswordForm((current) => ({ ...current, currentPassword: event.target.value }))}
                            />
                          ) : null}
                          <input
                            placeholder="新密码（6-18 位字母+数字）"
                            type="password"
                            value={passwordForm.nextPassword}
                            onChange={(event) => setPasswordForm((current) => ({ ...current, nextPassword: event.target.value }))}
                          />
                          <input
                            placeholder="确认新密码"
                            type="password"
                            value={passwordForm.confirmPassword}
                            onChange={(event) => setPasswordForm((current) => ({ ...current, confirmPassword: event.target.value }))}
                          />
                          <Button
                            disabled={settingsLoadingKey === "password"}
                            onClick={async () => {
                              setSettingsLoadingKey("password");
                              try {
                                const result = await changePassword(
                                  hasLoginPassword ? passwordForm.currentPassword : "",
                                  passwordForm.nextPassword,
                                  passwordForm.confirmPassword,
                                );
                                syncSessionProfile({ hasPassword: true });
                                setSettingsNotice(result.message);
                                setPasswordForm({ currentPassword: "", nextPassword: "", confirmPassword: "" });
                              } catch (error) {
                                setSettingsNotice(error instanceof Error ? error.message : "密码修改失败");
                              } finally {
                                setSettingsLoadingKey("");
                              }
                            }}
                          >
                            {settingsLoadingKey === "password" ? "提交中..." : hasLoginPassword ? "修改密码" : "设置密码"}
                          </Button>
                        </div>
                      </div>
                    </div>
                  </section>

                  <section className="profile-settings-card">
                    <div className="profile-settings-card__header">
                      <h3>绑定管理</h3>
                      <span>手机号 / 微信</span>
                    </div>
                    <div className="profile-settings-list">
                      <div className="profile-setting-form">
                        <div className="profile-setting-form__meta">
                          <span>{settingsProfile?.phoneBound ? "更换手机号" : "绑定手机号"}</span>
                          <strong>{settingsProfile?.phoneBound ? maskPhone(settingsProfile?.phone ?? "") : "未绑定手机号"}</strong>
                        </div>
                        <div className="profile-inline-fields profile-inline-fields--stack">
                          <input
                            placeholder={settingsProfile?.phoneBound ? "请输入新的手机号" : "请输入手机号"}
                            value={phoneForm.phone}
                            onChange={(event) => setPhoneForm((current) => ({ ...current, phone: event.target.value }))}
                          />
                          <div className="profile-inline-fields">
                            <input
                              placeholder="请输入短信验证码"
                              value={phoneForm.code}
                              onChange={(event) => setPhoneForm((current) => ({ ...current, code: event.target.value }))}
                            />
                            <button
                              className="profile-secondary-button"
                              disabled={phoneCodeCooldown > 0}
                              onClick={async () => {
                                if (!/^1\d{10}$/.test(phoneForm.phone)) {
                                  setSettingsNotice("请输入正确的手机号");
                                  return;
                                }
                                try {
                                  const result = await sendSmsCode(phoneForm.phone, "BIND_PHONE");
                                  setPhoneCodeCooldown(result.cooldownSeconds);
                                  setSettingsNotice(result.hint);
                                } catch (error) {
                                  setSettingsNotice(error instanceof Error ? error.message : "验证码发送失败");
                                }
                              }}
                              type="button"
                            >
                              {phoneCodeCooldown > 0 ? `${phoneCodeCooldown}s` : "发送验证码"}
                            </button>
                          </div>
                          <Button
                            disabled={settingsLoadingKey === "phone"}
                            onClick={async () => {
                              setSettingsLoadingKey("phone");
                              try {
                                const next = await changeBoundPhone(phoneForm.phone, phoneForm.code);
                                setSettingsProfile(next);
                                syncSessionProfile({ phone: next.phone });
                                setSettingsNotice(next.phoneBound ? "手机号绑定信息已更新。" : "手机号处理成功。");
                                setPhoneForm({ phone: "", code: "" });
                              } catch (error) {
                                setSettingsNotice(error instanceof Error ? error.message : "手机号更换失败");
                              } finally {
                                setSettingsLoadingKey("");
                              }
                            }}
                          >
                            {settingsLoadingKey === "phone" ? "提交中..." : settingsProfile?.phoneBound ? "更换手机号" : "绑定手机号"}
                          </Button>
                        </div>
                      </div>

                      {settingsProfile?.phoneBound ? (
                        <div className="profile-setting-form">
                          <div className="profile-setting-form__meta">
                            <span>解绑手机号</span>
                            <strong>需短信校验</strong>
                          </div>
                          <div className="profile-inline-fields profile-inline-fields--stack">
                            <p className="profile-inline-hint">
                              解绑手机号前需向当前号码 {maskPhone(settingsProfile.phone)} 发送验证码。若未绑定微信，将无法解绑。
                            </p>
                            <div className="profile-inline-fields">
                              <input
                                placeholder="请输入验证码"
                                value={unbindPhoneCode}
                                onChange={(event) => setUnbindPhoneCode(event.target.value)}
                              />
                              <button
                                className="profile-secondary-button"
                                disabled={securityCodeCooldown > 0}
                                onClick={async () => {
                                  if (!settingsProfile.phone) {
                                    setSettingsNotice("当前账号未绑定手机号");
                                    return;
                                  }
                                  try {
                                    const result = await sendSmsCode(settingsProfile.phone, "SECURITY_VERIFY");
                                    setSecurityCodeCooldown(result.cooldownSeconds);
                                    setSettingsNotice(result.hint);
                                  } catch (error) {
                                    setSettingsNotice(error instanceof Error ? error.message : "验证码发送失败");
                                  }
                                }}
                                type="button"
                              >
                                {securityCodeCooldown > 0 ? `${securityCodeCooldown}s` : "发送验证码"}
                              </button>
                            </div>
                            <Button
                              disabled={settingsLoadingKey === "unbind-phone"}
                              onClick={async () => {
                                setSettingsLoadingKey("unbind-phone");
                                try {
                                  const next = await unbindBoundPhone(unbindPhoneCode);
                                  setSettingsProfile(next);
                                  syncSessionProfile({ phone: next.phone });
                                  setSettingsNotice("手机号已解绑。");
                                  setUnbindPhoneCode("");
                                } catch (error) {
                                  setSettingsNotice(error instanceof Error ? error.message : "手机号解绑失败");
                                } finally {
                                  setSettingsLoadingKey("");
                                }
                              }}
                            >
                              {settingsLoadingKey === "unbind-phone" ? "提交中..." : "解绑手机号"}
                            </Button>
                          </div>
                        </div>
                      ) : null}

                      <div className="profile-setting-row">
                        <div className="profile-setting-row__meta">
                          <span>微信绑定</span>
                          <strong>{settingsProfile?.wechatBound ? "已绑定微信登录" : "未绑定微信"}</strong>
                        </div>
                        {settingsProfile?.wechatBound ? (
                          <button
                            disabled={settingsLoadingKey === "wechat"}
                            onClick={async () => {
                              setSettingsLoadingKey("wechat");
                              try {
                                const next = await unbindWechat();
                                setSettingsProfile(next);
                                setSettingsNotice("微信绑定已解除。");
                              } catch (error) {
                                setSettingsNotice(error instanceof Error ? error.message : "微信解绑失败");
                              } finally {
                                setSettingsLoadingKey("");
                              }
                            }}
                            type="button"
                          >
                            {settingsLoadingKey === "wechat" ? "处理中..." : "解绑微信"}
                          </button>
                        ) : (
                          <button type="button" onClick={() => setSettingsNotice("绑定微信请使用登录弹窗里的“微信扫码登录”，首次扫码并绑定后即可在这里看到状态。")}>
                            绑定微信
                          </button>
                        )}
                      </div>

                      <div className="profile-setting-row">
                        <div className="profile-setting-row__meta">
                          <span>提现账户</span>
                          <strong>{settingsProfile?.withdrawAccountLabel ?? "未绑定"}</strong>
                        </div>
                        <button onClick={() => setActiveMenu(setSearchParams, "wallet")} type="button">
                          去钱包管理
                        </button>
                      </div>
                    </div>
                  </section>

                  <section className="profile-settings-card">
                    <div className="profile-settings-card__header">
                      <h3>实名认证</h3>
                      <span>{settingsProfile?.verified ? "已通过" : "待提交"}</span>
                    </div>
                    <div className="profile-settings-list">
                      <div className="profile-setting-row">
                        <div className="profile-setting-row__meta">
                          <span>认证状态</span>
                          <strong>{renderRealNameStatusText(settingsProfile?.realNameStatus ?? "UNVERIFIED")}</strong>
                        </div>
                        <button onClick={() => setActiveMenu(setSearchParams, "verify")} type="button">
                          前往处理
                        </button>
                      </div>
                      <div className="profile-setting-row">
                        <div className="profile-setting-row__meta">
                          <span>认证姓名</span>
                          <strong>{settingsProfile?.realName || "未提交"}</strong>
                        </div>
                      </div>
                      <div className="profile-setting-row">
                        <div className="profile-setting-row__meta">
                          <span>身份证号</span>
                          <strong>{settingsProfile?.maskedIdCardNo || "未提交"}</strong>
                        </div>
                      </div>
                      {settingsProfile?.realNameRejectReason ? (
                        <div className="profile-setting-row">
                          <div className="profile-setting-row__meta">
                            <span>驳回原因</span>
                            <strong>{settingsProfile.realNameRejectReason}</strong>
                          </div>
                        </div>
                      ) : null}
                    </div>
                  </section>

                  <section className="profile-settings-card">
                    <div className="profile-settings-card__header">
                      <h3>安全设置</h3>
                      <span>提醒与保护</span>
                    </div>
                    <div className="profile-settings-list">
                      <label className="profile-switch-row">
                        <div className="profile-switch-row__meta">
                          <span>异地登录提醒</span>
                          <strong>{securityForm.loginAlertEnabled ? "已开启" : "已关闭"}</strong>
                        </div>
                        <input
                          checked={securityForm.loginAlertEnabled}
                          type="checkbox"
                          onChange={(event) => setSecurityForm((current) => ({ ...current, loginAlertEnabled: event.target.checked }))}
                        />
                      </label>
                      <label className="profile-switch-row">
                        <div className="profile-switch-row__meta">
                          <span>二次验证</span>
                          <strong>{securityForm.secondaryVerifyEnabled ? "已开启" : "未开启"}</strong>
                        </div>
                        <input
                          checked={securityForm.secondaryVerifyEnabled}
                          type="checkbox"
                          onChange={(event) => setSecurityForm((current) => ({ ...current, secondaryVerifyEnabled: event.target.checked }))}
                        />
                      </label>
                      <Button
                        disabled={settingsLoadingKey === "security"}
                        onClick={async () => {
                          setSettingsLoadingKey("security");
                          try {
                            const next = await updateSecuritySettings(securityForm.loginAlertEnabled, securityForm.secondaryVerifyEnabled);
                            setSettingsProfile(next);
                            setSettingsNotice("安全设置已更新。");
                          } catch (error) {
                            setSettingsNotice(error instanceof Error ? error.message : "安全设置更新失败");
                          } finally {
                            setSettingsLoadingKey("");
                          }
                        }}
                      >
                        {settingsLoadingKey === "security" ? "保存中..." : "保存安全设置"}
                      </Button>
                    </div>
                  </section>
                </div>
              )}
            </section>
          </section>
        ) : null}

        {showProfileContent && activeMenu === "distributor" ? (
          <DistributionCenterSection />
        ) : null}
        <WechatPayDialog
          open={Boolean(wechatPayPayload)}
          payload={wechatPayPayload}
          title={wechatPayScene === "WALLET" ? "微信充值" : "订单微信支付"}
          onClose={() => setWechatPayPayload(null)}
          onCheckPaid={async () => {
            if (!wechatPayPayload?.orderNo) {
              return false;
            }
            if (wechatPayScene === "WALLET") {
              const status = await loadWalletRechargeStatus(wechatPayPayload.orderNo);
              return status.paid || status.status === "SUCCESS";
            }
            const nextDetail = await loadOrderDetail(wechatPayPayload.orderNo);
            setOrderDetail(nextDetail);
            return nextDetail.statusCode !== "PENDING_PAYMENT";
          }}
          onPaid={async () => {
            if (wechatPayScene === "WALLET") {
              await refreshWallet();
              setRechargeForm({ amount: "" });
              setWalletNotice("微信充值已到账，余额已刷新。");
              return;
            }
            await refreshOrderCenter();
            if (wechatPayPayload?.orderNo) {
              const nextDetail = await loadOrderDetail(wechatPayPayload.orderNo);
              setOrderDetail(nextDetail);
              setActiveOrderNo(wechatPayPayload.orderNo);
              if (nextDetail.chatGroupNo) {
                window.open(`/im/${encodeURIComponent(nextDetail.chatGroupNo)}`, "_blank", "noopener,noreferrer");
              }
            }
            setOrderNotice("订单已支付成功。");
          }}
        />
      </div>
    </main>
  );

  function preferredWechatTradeType(): "NATIVE" | "JSAPI" {
    if (typeof navigator !== "undefined" && /MicroMessenger/i.test(navigator.userAgent)) {
      return "JSAPI";
    }
    return "NATIVE";
  }
}

function StatsCard({
  label,
  value,
  suffix,
  tone,
}: {
  label: string;
  value: string;
  suffix: string;
  tone: "blue" | "teal" | "gold" | "rose";
}) {
  return (
    <article className={`profile-stat-card profile-stat-card--${tone}`}>
      <span>{label}</span>
      <div className="profile-stat-card__value">
        <strong>{value}</strong>
        <small>{suffix}</small>
      </div>
    </article>
  );
}

function WalletStatCard({ label, value, tone }: { label: string; value: string; tone: "blue" | "orange" | "teal" | "gold" }) {
  return (
    <article className={`profile-wallet-card profile-wallet-card--${tone}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </article>
  );
}

function CouponCard({ record }: { record: CouponRecord }) {
  return (
    <article className={`profile-coupon-card profile-coupon-card--${record.status}`}>
      <div className="profile-coupon-card__amount">{record.amount}</div>
      <div className="profile-coupon-card__content">
        <strong>{record.name}</strong>
        <p>{record.condition}</p>
        <p>适用范围：{record.scope}</p>
        <p>有效期至：{record.expireAt}</p>
        {record.orderNo ? <p>关联订单：{record.orderNo}</p> : null}
        {record.usedAt ? <p>使用时间：{record.usedAt}</p> : null}
      </div>
      <span className="profile-coupon-card__status">{formatCouponStatus(record.status)}</span>
    </article>
  );
}

function setActiveMenu(setSearchParams: ReturnType<typeof useSearchParams>[1], key: ProfileMenuKey) {
  setSearchParams({ tab: key });
}

function resolveActiveMenu(value: string | null): ProfileMenuKey {
  if (value === "center") {
    return "orders";
  }
  if (value && PROFILE_MENU_SET.has(value as ProfileMenuKey)) {
    return value as ProfileMenuKey;
  }
  return "orders";
}

function getProfileMenuLabel(key: ProfileMenuKey) {
  return PROFILE_MENU_ITEMS.find((item) => item.key === key)?.label ?? "个人中心";
}

function maskPhone(phone: string) {
  if (!phone || phone.length < 7) return phone;
  return `${phone.slice(0, 3)}****${phone.slice(-4)}`;
}

function formatCurrency(amount: number) {
  return `¥${amount.toFixed(2)}`;
}

function formatDateTime(value: string) {
  if (!value) return "-";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  const pad = (input: number) => String(input).padStart(2, "0");
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`;
}

function isMobileRuntime() {
  if (typeof navigator === "undefined") {
    return false;
  }
  return /Android|iPhone|iPad|iPod|Mobile|MicroMessenger/i.test(navigator.userAgent);
}

function formatCouponStatus(status: CouponRecord["status"]) {
  if (status === "available") return "可用";
  if (status === "used") return "已使用";
  if (status === "expired") return "已过期";
  return "已作废";
}

function renderMessageCount(counts: MessageCenter["counts"], category: MessageCategory) {
  if (category === "ALL") return counts.all;
  if (category === "SYSTEM") return counts.system;
  if (category === "TRADE") return counts.trade;
  if (category === "SERVICE") return counts.service;
  return counts.distribution;
}

function renderChannelLabel(channel: string | null) {
  if (channel === "ALIPAY") return "支付宝";
  if (channel === "WECHAT") return "微信";
  return "系统";
}

function FaceRealNameQrDialog({
  dialog,
  onClose,
}: {
  dialog: {
    orderId: string;
    verifyUrl: string;
    expireAt: string;
    status: "WAITING" | "CHECKING" | "FAILED";
    message: string;
  };
  onClose: () => void;
}) {
  const qrImageUrl = `https://api.qrserver.com/v1/create-qr-code/?size=260x260&data=${encodeURIComponent(dialog.verifyUrl)}`;
  return (
    <div className="profile-face-dialog" role="dialog" aria-modal="true" aria-labelledby="profile-face-dialog-title">
      <div className="profile-face-dialog__panel">
        <button className="profile-face-dialog__close" type="button" onClick={onClose} aria-label="关闭">×</button>
        <div className="profile-face-dialog__header">
          <strong id="profile-face-dialog-title">手机扫码认证</strong>
          <span>{dialog.status === "CHECKING" ? "查询中" : dialog.status === "FAILED" ? "未通过" : "等待认证"}</span>
        </div>
        <div className="profile-face-dialog__qr">
          <img alt="人脸实名认证二维码" src={qrImageUrl} />
        </div>
        <p>{dialog.message}</p>
        <div className="profile-face-dialog__meta">
          <span>认证单号</span>
          <strong>{dialog.orderId}</strong>
        </div>
        <div className="profile-face-dialog__meta">
          <span>有效期至</span>
          <strong>{formatDateTime(dialog.expireAt)}</strong>
        </div>
        <div className="profile-face-dialog__actions">
          <Button kind="secondary" onClick={() => window.open(dialog.verifyUrl, "_blank", "noopener,noreferrer")}>打开认证链接</Button>
          <Button onClick={onClose}>关闭</Button>
        </div>
      </div>
    </div>
  );
}

function renderRealNamePanel({
  loadStatus,
  error,
  profile,
  form,
  fieldErrors,
  submitting,
  onChange,
  onSubmit,
}: {
  loadStatus: LoadStatus;
  error: string;
  profile: RealNameProfile | null;
  form: { realName: string; idCardNo: string };
  fieldErrors: { realName?: string; idCardNo?: string };
  submitting: boolean;
  onChange: (field: "realName" | "idCardNo", value: string) => void;
  onSubmit: () => Promise<void>;
}) {
  if (loadStatus === "loading" || loadStatus === "idle") {
    return <StatusState title="实名认证信息加载中" description="正在同步当前账号的实名状态，请稍候。" />;
  }
  if (loadStatus === "error" && !profile) {
    return <StatusState title="实名认证信息加载失败" description={error} tone="error" />;
  }
  if (profile?.status === "APPROVED") {
    return (
      <div className="profile-real-name-verified">
        <div className="profile-real-name-verified__badge">
          <span aria-hidden="true">✓</span>
          <strong>已实名认证</strong>
        </div>
        <div className="profile-real-name-verified__fields">
          <label className="profile-real-name-field">
            <span>认证姓名</span>
            <input readOnly type="text" value={profile.realName} />
          </label>
          <label className="profile-real-name-field">
            <span>手机号</span>
            <input readOnly type="tel" value={profile.phone} />
          </label>
          <label className="profile-real-name-field">
            <span>身份证号</span>
            <input readOnly type="text" value={profile.maskedIdCardNo} />
          </label>
        </div>
      </div>
    );
  }
  return (
    <div className="profile-real-name-form">
      <div className="profile-real-name-form__tips">
        <strong>{profile?.status === "REJECTED" ? "认证未通过，可重新发起" : "进入人脸活体认证"}</strong>
        <p>填写姓名与身份证号后，将跳转到聚合数据人脸认证页面。认证完成回到平台后会自动刷新结果。</p>
        <ul>
          <li>实名认证后默认用于钱包提现、分销开通和账号发布风控。</li>
          <li>未通过会返回失败原因，核对信息后可重新发起。</li>
          <li>平台只保存认证结果，不保存人脸图片。</li>
        </ul>
        {profile?.status === "REJECTED" && profile.rejectReason ? <p className="profile-real-name-form__error">{profile.rejectReason}</p> : null}
      </div>
      <div className="profile-real-name-form__fields">
        <label className="profile-real-name-field">
          <span>认证姓名</span>
          <input autoComplete="name" placeholder="请输入真实姓名" type="text" value={form.realName} onChange={(event) => onChange("realName", event.target.value)} />
          {fieldErrors.realName ? <em>{fieldErrors.realName}</em> : null}
        </label>
        <label className="profile-real-name-field">
          <span>身份证号</span>
          <input
            autoComplete="off"
            placeholder="请输入身份证号"
            type="text"
            value={form.idCardNo}
            onChange={(event) => onChange("idCardNo", event.target.value.toUpperCase())}
          />
          {fieldErrors.idCardNo ? <em>{fieldErrors.idCardNo}</em> : null}
        </label>
        {error ? <p className="profile-real-name-form__error">{error}</p> : null}
        <div className="profile-real-name-form__actions">
          <Button disabled={submitting} onClick={() => void onSubmit()}>
            {submitting ? "处理中..." : "开始人脸认证"}
          </Button>
        </div>
      </div>
    </div>
  );
}

function validateRealNameForm(form: { realName: string; idCardNo: string }) {
  const nextErrors: { realName?: string; idCardNo?: string } = {};
  const normalizedName = form.realName.trim();
  const normalizedIdCard = form.idCardNo.trim().toUpperCase();

  if (!normalizedName) {
    nextErrors.realName = "请输入姓名";
  } else if (!/^[\u4e00-\u9fa5·]{2,20}$/.test(normalizedName)) {
    nextErrors.realName = "姓名需为 2-20 位中文字符";
  }

  if (!normalizedIdCard) {
    nextErrors.idCardNo = "请输入身份证号";
  } else if (!/^\d{17}[0-9X]$/.test(normalizedIdCard)) {
    nextErrors.idCardNo = "请输入 18 位身份证号";
  }

  return nextErrors;
}

function UploadImageCard({
  label,
  asset,
  uploading,
  triggerId,
  error,
  onDelete,
  onSelect,
}: {
  label: string;
  asset: UploadedImageAsset | null;
  uploading: boolean;
  triggerId: string;
  error?: string;
  onDelete: () => void;
  onSelect: (file: File) => Promise<void>;
}) {
  return (
    <div className="profile-upload-card">
      <span>{label}</span>
      <input
        accept="image/jpeg,image/png"
        id={triggerId}
        style={{ display: "none" }}
        type="file"
        onChange={async (event) => {
          const file = event.target.files?.[0];
          event.currentTarget.value = "";
          if (!file) {
            return;
          }
          await onSelect(file);
        }}
      />
      {asset ? (
        <div className="profile-upload-card__preview">
          <img alt={label} src={asset.previewUrl} />
          <div className="profile-upload-card__actions">
            <strong>{asset.filename}</strong>
            <button type="button" onClick={onDelete}>删除</button>
          </div>
        </div>
      ) : (
        <label className="profile-upload-card__empty" htmlFor={triggerId}>
          {uploading ? "上传中..." : "上传图片"}
        </label>
      )}
      {error ? <em>{error}</em> : null}
    </div>
  );
}

function ReadOnlyImageCard({ label, previewUrl }: { label: string; previewUrl: string }) {
  return (
    <div className="profile-upload-card profile-upload-card--readonly">
      <span>{label}</span>
      {previewUrl ? <img alt={label} src={previewUrl} /> : <div className="profile-upload-card__placeholder">未上传</div>}
    </div>
  );
}

function validateProfileImageFile(file: File, label: string, allowedTypes = ["image/jpeg", "image/png"]) {
  if (!allowedTypes.includes(file.type)) {
    throw new Error(`${label}仅支持 ${allowedTypes.map(renderImageTypeName).join("/")}`);
  }
}

function renderImageTypeName(type: string) {
  if (type === "image/jpeg") return "JPG";
  if (type === "image/png") return "PNG";
  if (type === "image/webp") return "WEBP";
  return type;
}

async function uploadProfileImage(file: File, businessScope: string): Promise<UploadedImageAsset> {
  const extension = file.name.includes(".") ? file.name.substring(file.name.lastIndexOf(".")) : "";
  const filename = `${Date.now()}-${Math.random().toString(36).slice(2, 8)}${extension}`;
  const uploaded = await uploadOssFileDirect(businessScope, file, filename);
  return {
    objectKey: uploaded.objectKey,
    previewUrl: uploaded.previewUrl,
    filename: file.name,
  };
}

function renderRealNameStatusText(status: RealNameProfile["status"]) {
  if (status === "APPROVED") return "已实名认证";
  if (status === "REJECTED") return "认证未通过";
  return "未实名";
}

function mergeOrderListItem(current: OrderListItem, detail: OrderDetail): OrderListItem {
  return {
    ...current,
    orderNo: detail.orderNo,
    title: detail.title,
    summary: detail.summary,
    coverUrl: detail.coverUrl,
    totalAmount: detail.totalAmount,
    statusLabel: detail.statusLabel,
    statusCode: detail.statusCode,
    buyerNickname: detail.buyerNickname,
    sellerNickname: detail.sellerNickname,
    sellerType: detail.sellerType,
    sellerDisplayName: detail.sellerDisplayName,
    chatGroupNo: detail.chatGroupNo,
    canCancel: detail.canCancel,
    canApplyAfterSale: detail.canApplyAfterSale,
    canApplyRefund: detail.canApplyRefund,
    canReviewRefund: detail.canReviewRefund,
    canDelete: detail.canDelete,
    canEnterChat: detail.canEnterChat,
    canConfirmComplete: detail.canConfirmComplete,
    paymentExpireAt: detail.paymentExpireAt,
  };
}

function patchOrderCounts(counts: OrderCounts, previousStatus: OrderCenterStatus, nextStatus: OrderCenterStatus): OrderCounts {
  if (previousStatus === nextStatus) {
    return counts;
  }
  const nextCounts = { ...counts };
  const previousKey = mapOrderCountKey(previousStatus);
  const nextKey = mapOrderCountKey(nextStatus);
  if (previousKey) {
    nextCounts[previousKey] = Math.max(0, nextCounts[previousKey] - 1);
  }
  if (nextKey) {
    nextCounts[nextKey] += 1;
  }
  return nextCounts;
}

function mapOrderCountKey(status: OrderCenterStatus): keyof OrderCounts | null {
  if (status === "PENDING_PAYMENT") return "pendingPayment";
  if (status === "WAITING_TRADE") return "waitingTrade";
  if (status === "IN_PROGRESS") return "inProgress";
  if (status === "COMPLETED") return "completed";
  if (status === "REFUND_PENDING") return "refundPending";
  if (status === "AFTER_SALE") return "afterSale";
  if (status === "REFUNDED") return "refunded";
  if (status === "CLOSED") return "closed";
  return null;
}

function resolveCancelOrderNotice(detail: OrderDetail) {
  if (detail.statusCode === "CLOSED") {
    return "订单已关闭。";
  }
  if (detail.statusCode === "REFUND_PENDING") {
    return "已提交取消申请，等待卖家审核退款。";
  }
  if (detail.statusCode === "REFUNDED") {
    return "订单已取消并退款。";
  }
  return "取消订单已提交。";
}
