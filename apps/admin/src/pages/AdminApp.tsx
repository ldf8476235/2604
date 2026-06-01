import { useEffect, useMemo, useRef, useState } from "react";
import { Button, MetricCard, SectionHeading, StatusState, SurfaceCard, Tag } from "@delta/ui";
import { AdminIntegrationConfigSection } from "../modules/admin/AdminIntegrationConfigSection";
import { AdminGunCodeUploadSection } from "../modules/admin/AdminGunCodeUploadSection";
import { AdminSupportWorkbench } from "../modules/admin/AdminSupportWorkbench";
import {
  assignAdminRoleMembers,
  batchAdminOperationStatus,
  deleteAdminAnnouncement,
  deleteAdminBanner,
  deleteAdminListing,
  forceAdminRefund,
  deleteAdminShortcut,
  loadAdminSession,
  loadAdminStudioApplications,
  loadAdminStudioDetail,
  loadAdminStudioWithdraws,
  loadAdminUserDetail,
  loadAdminOperations,
  loadAdminImConversation,
  loadAdminOrderDetail,
  loadAdminRealNameReviews,
  loadAdminRoles,
  loadAdminUsers,
  loadAdminBoostingServices,
  loadAdminDashboard,
  loadAdminListingDetail,
  loadAdminListings,
  loadAdminOrders,
  loadAdminStudios,
  loadAdminWithdraws,
  reviewAdminRealName,
  reviewAdminListing,
  reviewStudioApplication,
  reviewStudioWithdraw,
  resetAdminUserPassword,
  reviewWithdraw,
  saveAdminStudio,
  saveAdminAnnouncement,
  saveAdminBanner,
  saveAdminRole,
  saveAdminShortcut,
  uploadAdminAsset,
  updateAdminUserStatus,
  updateBoostingServiceStatus,
  updateStudioPolicy,
  updateStudioShareRatio,
  updateStudioStatus,
  type AdminBoostingCenter,
  type AdminConsoleSession,
  type AdminDashboard,
  type AdminListingCenter,
  type AdminListingDetail,
  type AdminOperationCenter,
  type AdminImConversation,
  type AdminOrderCenter,
  type AdminOrderDetail,
  type AdminRealNameCenter,
  type AdminRoleCenter,
  type AdminStudioApplicationCenter,
  type AdminStudioCenter,
  type AdminStudioDetail,
  type AdminStudioWithdrawCenter,
  type AdminUserDetail,
  type AdminUserCenter,
  type AdminWithdrawCenter,
} from "../modules/admin/admin-api";
import { clearAuthSession, hasAuthToken, loginByPassword, logout } from "../lib/auth";

type AdminTab = "dashboard" | "listing" | "order" | "studio" | "boosting" | "withdraw" | "operation" | "gunCode" | "support" | "user" | "realName" | "role";
const PERMISSION_GROUPS: Array<{ label: string; permissions: Array<{ value: string; label: string }> }> = [
  {
    label: "总览与交易",
    permissions: [
      { value: "dashboard", label: "仪表盘" },
      { value: "listing", label: "账号库管理" },
      { value: "order", label: "订单中心" },
      { value: "boosting", label: "代肝服务" },
      { value: "support", label: "客服监管" },
    ],
  },
  {
    label: "合作与财务",
    permissions: [
      { value: "studio", label: "工作室管理" },
      { value: "withdraw", label: "提现审核" },
      { value: "operation", label: "运营配置" },
    ],
  },
  {
    label: "账号与权限",
    permissions: [
      { value: "user", label: "用户管理" },
      { value: "realName", label: "实名审核" },
      { value: "role", label: "角色权限" },
    ],
  },
];

const PERMISSION_OPTIONS: Array<{ value: string; label: string }> = [
  ...PERMISSION_GROUPS.flatMap((group) => group.permissions),
];

const MENU_ITEMS: Array<{ key: AdminTab; label: string; description: string }> = [
  { key: "dashboard", label: "仪表盘", description: "看待办优先级与异常摘要" },
  { key: "listing", label: "账号库管理", description: "审核、驳回、下架账号资源" },
  { key: "order", label: "订单中心", description: "查看交易订单状态与金额" },
  { key: "studio", label: "工作室管理", description: "维护合作状态与免审策略" },
  { key: "boosting", label: "代肝服务", description: "启停代肝服务与查看销量" },
  { key: "withdraw", label: "提现审核", description: "审核打款与驳回提现申请" },
  { key: "operation", label: "运营配置", description: "管理轮播图、金刚区与公告系统" },
  { key: "gunCode", label: "改枪码管理", description: "上传、预览和导入改枪码库" },
  { key: "support", label: "客服监管", description: "统一处理交易群聊、售前咨询和代肝会话" },
  { key: "user", label: "用户管理", description: "查看用户状态、封禁与工作室归属" },
  { key: "realName", label: "实名审核", description: "审核用户提交的实名认证资料" },
  { key: "role", label: "角色权限", description: "维护后台角色与成员归属" },
];

export function AdminApp() {
  const [activeTab, setActiveTab] = useState<AdminTab>("dashboard");
  const [notice, setNotice] = useState("");
  const [sessionLoading, setSessionLoading] = useState(true);
  const [sessionError, setSessionError] = useState("");
  const [consoleSession, setConsoleSession] = useState<AdminConsoleSession | null>(null);

  const [dashboard, setDashboard] = useState<AdminDashboard | null>(null);
  const [dashboardLoading, setDashboardLoading] = useState(true);
  const [dashboardError, setDashboardError] = useState("");

  const [listingFilters, setListingFilters] = useState({ status: "ALL", sellerType: "ALL", keyword: "" });
  const [listings, setListings] = useState<AdminListingCenter | null>(null);
  const [listingsLoading, setListingsLoading] = useState(false);
  const [listingsError, setListingsError] = useState("");
  const [selectedListingNo, setSelectedListingNo] = useState("");
  const [listingDetail, setListingDetail] = useState<AdminListingDetail | null>(null);
  const [listingDetailLoading, setListingDetailLoading] = useState(false);
  const [listingReviewReason, setListingReviewReason] = useState("");
  const [listingDeleting, setListingDeleting] = useState(false);
  const [listingPreviewUrl, setListingPreviewUrl] = useState("");
  const [listingPreviewIndex, setListingPreviewIndex] = useState(0);
  const [listingPreviewBoundaryNotice, setListingPreviewBoundaryNotice] = useState("");
  const listingPreviewNoticeTimerRef = useRef<number | null>(null);

  const [orderFilters, setOrderFilters] = useState({ status: "ALL", sellerType: "ALL" });
  const [orders, setOrders] = useState<AdminOrderCenter | null>(null);
  const [ordersLoading, setOrdersLoading] = useState(false);
  const [ordersError, setOrdersError] = useState("");
  const [selectedOrderNo, setSelectedOrderNo] = useState("");
  const [orderDetail, setOrderDetail] = useState<AdminOrderDetail | null>(null);
  const [orderDetailLoading, setOrderDetailLoading] = useState(false);
  const [orderDetailError, setOrderDetailError] = useState("");
  const [orderConversation, setOrderConversation] = useState<AdminImConversation | null>(null);
  const [orderConversationLoading, setOrderConversationLoading] = useState(false);
  const [orderConversationError, setOrderConversationError] = useState("");

  const [studioFilters, setStudioFilters] = useState({ active: "ALL", keyword: "" });
  const [studios, setStudios] = useState<AdminStudioCenter | null>(null);
  const [studiosLoading, setStudiosLoading] = useState(false);
  const [studiosError, setStudiosError] = useState("");
  const [studioApplicationFilters, setStudioApplicationFilters] = useState({ status: "PENDING", keyword: "" });
  const [studioApplications, setStudioApplications] = useState<AdminStudioApplicationCenter | null>(null);
  const [studioApplicationsLoading, setStudioApplicationsLoading] = useState(false);
  const [studioApplicationsError, setStudioApplicationsError] = useState("");
  const [selectedStudioApplicationNo, setSelectedStudioApplicationNo] = useState("");
  const [studioApplicationReviewReason, setStudioApplicationReviewReason] = useState("");
  const [selectedStudioId, setSelectedStudioId] = useState<number | null>(null);
  const [studioModalOpen, setStudioModalOpen] = useState(false);
  const [studioDetail, setStudioDetail] = useState<AdminStudioDetail | null>(null);
  const [studioDetailLoading, setStudioDetailLoading] = useState(false);
  const [studioShareRatioDrafts, setStudioShareRatioDrafts] = useState<Record<number, string>>({});
  const [studioSaving, setStudioSaving] = useState(false);
  const [studioAssetUploading, setStudioAssetUploading] = useState(false);
  const [studioForm, setStudioForm] = useState({
    studioId: 0,
    ownerPhone: "",
    studioName: "",
    description: "",
    contactPhone: "",
    contactName: "",
    contactWechat: "",
    qualificationCode: "",
    qualificationMaterialKey: "",
    qualificationMaterialUrl: "",
    qualificationNote: "",
    reviewStrategy: "REVIEW_REQUIRED",
    shareRatio: "0.70",
    active: true,
    cooperationStatus: "ACTIVE",
  });

  const [boostingFilters, setBoostingFilters] = useState({ status: "ALL", providerType: "ALL" });
  const [boostingCenter, setBoostingCenter] = useState<AdminBoostingCenter | null>(null);
  const [boostingLoading, setBoostingLoading] = useState(false);
  const [boostingError, setBoostingError] = useState("");

  const [withdrawFilters, setWithdrawFilters] = useState({ status: "ALL" });
  const [withdrawCenter, setWithdrawCenter] = useState<AdminWithdrawCenter | null>(null);
  const [withdrawLoading, setWithdrawLoading] = useState(false);
  const [withdrawError, setWithdrawError] = useState("");
  const [selectedWithdrawApplicationNo, setSelectedWithdrawApplicationNo] = useState("");
  const [withdrawReviewReason, setWithdrawReviewReason] = useState("");
  const [withdrawReviewDialog, setWithdrawReviewDialog] = useState<"USER" | "STUDIO" | null>(null);
  const [studioWithdrawCenter, setStudioWithdrawCenter] = useState<AdminStudioWithdrawCenter | null>(null);
  const [studioWithdrawLoading, setStudioWithdrawLoading] = useState(false);
  const [studioWithdrawError, setStudioWithdrawError] = useState("");
  const [selectedStudioWithdrawApplicationNo, setSelectedStudioWithdrawApplicationNo] = useState("");
  const [studioWithdrawReviewReason, setStudioWithdrawReviewReason] = useState("");

  const [operationCenter, setOperationCenter] = useState<AdminOperationCenter | null>(null);
  const [operationLoading, setOperationLoading] = useState(false);
  const [operationError, setOperationError] = useState("");
  const [selectedBannerId, setSelectedBannerId] = useState<number | null>(null);
  const [selectedShortcutId, setSelectedShortcutId] = useState<number | null>(null);
  const [selectedAnnouncementId, setSelectedAnnouncementId] = useState<number | null>(null);
  const [selectedBannerIds, setSelectedBannerIds] = useState<number[]>([]);
  const [selectedShortcutIds, setSelectedShortcutIds] = useState<number[]>([]);
  const [selectedAnnouncementIds, setSelectedAnnouncementIds] = useState<number[]>([]);
  const [bannerForm, setBannerForm] = useState({ bannerId: 0, title: "", imageKey: "", imageUrl: "", linkUrl: "", sortNo: "10", status: "ACTIVE" });
  const [shortcutForm, setShortcutForm] = useState({ shortcutId: 0, name: "", iconKey: "", iconUrl: "", linkUrl: "", sortNo: "10", status: "ACTIVE" });
  const [announcementForm, setAnnouncementForm] = useState({
    announcementId: 0,
    title: "",
    content: "",
    category: "SYSTEM",
    pinned: false,
    status: "PUBLISHED",
  });
  const [operationSaving, setOperationSaving] = useState<"" | "banner" | "shortcut" | "announcement">("");
  const [operationUploading, setOperationUploading] = useState<"" | "banner" | "shortcut">("");
  const [operationBatchLoading, setOperationBatchLoading] = useState<"" | "banner" | "shortcut" | "announcement">("");
  const [operationDeleting, setOperationDeleting] = useState<"" | "banner" | "shortcut" | "announcement">("");

  const [userFilters, setUserFilters] = useState({ status: "ALL", verified: "ALL", studioOwner: "ALL", keyword: "" });
  const [userCenter, setUserCenter] = useState<AdminUserCenter | null>(null);
  const [userLoading, setUserLoading] = useState(false);
  const [userError, setUserError] = useState("");
  const [selectedUserId, setSelectedUserId] = useState<number | null>(null);
  const [userDetail, setUserDetail] = useState<AdminUserDetail | null>(null);
  const [userDetailLoading, setUserDetailLoading] = useState(false);
  const [resetPassword, setResetPassword] = useState({ password: "", confirmPassword: "" });
  const [userStatusReason, setUserStatusReason] = useState("");
  const [userActionLoading, setUserActionLoading] = useState(false);

  const [realNameStatus, setRealNameStatus] = useState("ALL");
  const [realNameCenter, setRealNameCenter] = useState<AdminRealNameCenter | null>(null);
  const [realNameLoading, setRealNameLoading] = useState(false);
  const [realNameError, setRealNameError] = useState("");
  const [selectedRealNameUserId, setSelectedRealNameUserId] = useState<number | null>(null);
  const [realNameReviewReason, setRealNameReviewReason] = useState("");

  const [roleCenter, setRoleCenter] = useState<AdminRoleCenter | null>(null);
  const [roleLoading, setRoleLoading] = useState(false);
  const [roleError, setRoleError] = useState("");
  const [selectedRoleId, setSelectedRoleId] = useState<number | null>(null);
  const [roleForm, setRoleForm] = useState({ roleId: 0, roleCode: "", roleName: "", description: "", status: "ACTIVE", permissions: [] as string[], memberIds: [] as number[] });
  const [roleSaving, setRoleSaving] = useState(false);
  const [roleUserCandidates, setRoleUserCandidates] = useState<AdminUserCenter["rows"]>([]);
  const [roleUserLoading, setRoleUserLoading] = useState(false);
  const [roleUserKeyword, setRoleUserKeyword] = useState("");

  const allowedMenuItems = useMemo(() => {
    if (!consoleSession) {
      return [] as typeof MENU_ITEMS;
    }
    const roleCodes = consoleSession.roles.map((item) => item.roleCode);
    return MENU_ITEMS.filter((item) => {
      if (item.key === "gunCode") {
        return consoleSession.permissions.includes("operation");
      }
      if (consoleSession.permissions.includes(item.key)) {
        return true;
      }
      return item.key === "support" && (roleCodes.includes("SUPER_ADMIN") || roleCodes.includes("SERVICE_ADMIN"));
    });
  }, [consoleSession]);

  const selectedUser = useMemo(
    () => userCenter?.rows.find((row) => row.userId === selectedUserId) ?? null,
    [selectedUserId, userCenter]
  );
  const selectedRole = useMemo(
    () => roleCenter?.rows.find((row) => row.roleId === selectedRoleId) ?? null,
    [roleCenter, selectedRoleId]
  );
  const selectedWithdraw = useMemo(
    () => withdrawCenter?.rows.find((row) => row.applicationNo === selectedWithdrawApplicationNo) ?? null,
    [selectedWithdrawApplicationNo, withdrawCenter]
  );
  const selectedStudioWithdraw = useMemo(
    () => studioWithdrawCenter?.rows.find((row) => row.applicationNo === selectedStudioWithdrawApplicationNo) ?? null,
    [selectedStudioWithdrawApplicationNo, studioWithdrawCenter]
  );
  const selectedStudioApplication = useMemo(
    () => studioApplications?.rows.find((row) => row.applicationNo === selectedStudioApplicationNo) ?? null,
    [selectedStudioApplicationNo, studioApplications]
  );
  const selectedRealNameReview = useMemo(
    () => realNameCenter?.rows.find((row) => row.userId === selectedRealNameUserId) ?? null,
    [realNameCenter, selectedRealNameUserId]
  );
  const selectedBanner = useMemo(
    () => operationCenter?.banners.find((row) => row.bannerId === selectedBannerId) ?? null,
    [operationCenter, selectedBannerId]
  );
  const selectedShortcut = useMemo(
    () => operationCenter?.shortcuts.find((row) => row.shortcutId === selectedShortcutId) ?? null,
    [operationCenter, selectedShortcutId]
  );
  const selectedAnnouncement = useMemo(
    () => operationCenter?.announcements.find((row) => row.announcementId === selectedAnnouncementId) ?? null,
    [operationCenter, selectedAnnouncementId]
  );
  const listingPreviewImages = useMemo(() => {
    if (!listingDetail) {
      return [] as string[];
    }
    return Array.from(new Set([listingDetail.summary.coverUrl, ...listingDetail.summary.imageUrls].filter((url): url is string => Boolean(url))));
  }, [listingDetail]);
  const operationOverviewCards = useMemo(() => {
    if (!operationCenter) {
      return [] as Array<{ title: string; value: string; detail: string }>;
    }
    return [
      {
        title: "轮播图",
        value: String(operationCenter.banners.length),
        detail: `启用中 ${operationCenter.banners.filter((row) => row.status === "ACTIVE").length} 条`,
      },
      {
        title: "金刚区",
        value: String(operationCenter.shortcuts.length),
        detail: `启用中 ${operationCenter.shortcuts.filter((row) => row.status === "ACTIVE").length} 个`,
      },
      {
        title: "公告",
        value: String(operationCenter.announcements.length),
        detail: `已发布 ${operationCenter.announcements.filter((row) => row.status === "PUBLISHED").length} 条`,
      },
      {
        title: "系统参数",
        value: "3",
        detail: "支付 / 登录 / 分销",
      },
    ];
  }, [operationCenter]);
  const queueTabMap: Record<string, AdminTab> = {
    STUDIO_APPLICATION_REVIEW: "studio",
    LISTING_REVIEW: "listing",
    WITHDRAW_REVIEW: "withdraw",
  };
  const activeMenuItem = allowedMenuItems.find((item) => item.key === activeTab) ?? MENU_ITEMS.find((item) => item.key === activeTab) ?? null;

  useEffect(() => {
    void bootstrapSession();
  }, []);

  useEffect(() => {
    if (!consoleSession) {
      return;
    }
    if (!allowedMenuItems.find((item) => item.key === activeTab) && allowedMenuItems[0]) {
      setActiveTab(allowedMenuItems[0].key);
      return;
    }
    if (activeTab === "dashboard") {
      void refreshDashboard();
      return;
    }
    if (activeTab === "listing") {
      void refreshListings();
    } else if (activeTab === "order") {
      void refreshOrders();
    } else if (activeTab === "studio") {
      void refreshStudioApplications();
      void refreshStudios();
    } else if (activeTab === "boosting") {
      void refreshBoosting();
    } else if (activeTab === "withdraw") {
      void refreshWithdraws();
      void refreshStudioWithdraws();
    } else if (activeTab === "operation") {
      void refreshOperations();
    } else if (activeTab === "gunCode") {
      return;
    } else if (activeTab === "user") {
      void refreshUsers();
    } else if (activeTab === "realName") {
      void refreshRealNames();
    } else if (activeTab === "role") {
      void refreshRoles();
      void refreshRoleCandidates();
    }
  }, [activeTab, consoleSession, allowedMenuItems]);

  useEffect(() => {
    if (!selectedRole) {
      return;
    }
    setRoleForm({
      roleId: selectedRole.roleId,
      roleCode: selectedRole.roleCode,
      roleName: selectedRole.roleName,
      description: selectedRole.description === "-" ? "" : selectedRole.description,
      status: selectedRole.status,
      permissions: selectedRole.permissions,
      memberIds: selectedRole.members.map((member) => member.userId),
    });
  }, [selectedRole]);

  useEffect(() => {
    if (!consoleSession || activeTab !== "user" || !selectedUserId) {
      return;
    }
    setUserDetailLoading(true);
    loadAdminUserDetail(selectedUserId)
      .then(setUserDetail)
      .catch((error: Error) => {
        setNotice(error.message);
        setUserDetail(null);
      })
      .finally(() => setUserDetailLoading(false));
  }, [activeTab, consoleSession, selectedUserId]);

  useEffect(() => {
    if (!selectedBanner) {
      return;
    }
    setBannerForm({
      bannerId: selectedBanner.bannerId,
      title: selectedBanner.title,
      imageKey: selectedBanner.imageKey,
      imageUrl: selectedBanner.imageUrl ?? "",
      linkUrl: selectedBanner.linkUrl === "-" ? "" : selectedBanner.linkUrl,
      sortNo: String(selectedBanner.sortNo),
      status: selectedBanner.status,
    });
  }, [selectedBanner]);

  useEffect(() => {
    if (!selectedShortcut) {
      return;
    }
    setShortcutForm({
      shortcutId: selectedShortcut.shortcutId,
      name: selectedShortcut.name,
      iconKey: selectedShortcut.iconKey ?? "",
      iconUrl: selectedShortcut.iconUrl ?? "",
      linkUrl: selectedShortcut.linkUrl,
      sortNo: String(selectedShortcut.sortNo),
      status: selectedShortcut.status,
    });
  }, [selectedShortcut]);

  useEffect(() => {
    if (!selectedAnnouncement) {
      return;
    }
    setAnnouncementForm({
      announcementId: selectedAnnouncement.announcementId,
      title: selectedAnnouncement.title,
      content: selectedAnnouncement.content,
      category: selectedAnnouncement.category,
      pinned: selectedAnnouncement.pinned,
      status: selectedAnnouncement.status,
    });
  }, [selectedAnnouncement]);

  useEffect(() => {
    if (!selectedOrderNo) {
      return;
    }
    function handleKeyDown(event: globalThis.KeyboardEvent) {
      if (event.key === "Escape") {
        setSelectedOrderNo("");
      }
    }
    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [selectedOrderNo]);

  useEffect(() => {
    if (!consoleSession) {
      return;
    }
    if (!selectedListingNo) {
      setListingDetail(null);
      return;
    }
    setListingDetailLoading(true);
    loadAdminListingDetail(selectedListingNo)
      .then(setListingDetail)
      .catch((error: Error) => {
        setNotice(error.message);
        setListingDetail(null);
      })
      .finally(() => setListingDetailLoading(false));
  }, [selectedListingNo, consoleSession]);

  useEffect(() => {
    if (!consoleSession || activeTab !== "order") {
      return;
    }
    if (!selectedOrderNo) {
      setOrderDetail(null);
      setOrderDetailError("");
      setOrderConversation(null);
      setOrderConversationError("");
      return;
    }
    setOrderDetailLoading(true);
    setOrderDetailError("");
    setOrderConversation(null);
    setOrderConversationError("");
    loadAdminOrderDetail(selectedOrderNo)
      .then((detail) => {
        setOrderDetail(detail);
        if (detail.summary.chatGroupNo) {
          setOrderConversationLoading(true);
          loadAdminImConversation(detail.summary.chatGroupNo)
            .then(setOrderConversation)
            .catch((error: Error) => {
              setOrderConversation(null);
              setOrderConversationError(error.message);
            })
            .finally(() => setOrderConversationLoading(false));
        }
      })
      .catch((error: Error) => {
        setOrderDetail(null);
        setOrderDetailError(error.message);
      })
      .finally(() => setOrderDetailLoading(false));
  }, [selectedOrderNo, consoleSession, activeTab]);

  useEffect(() => {
    return () => {
      if (listingPreviewNoticeTimerRef.current) {
        window.clearTimeout(listingPreviewNoticeTimerRef.current);
      }
    };
  }, []);

  useEffect(() => {
    if (!listingPreviewUrl) {
      return;
    }

    function handleKeyDown(event: globalThis.KeyboardEvent) {
      if (event.key === "Escape") {
        event.preventDefault();
        clearListingPreviewBoundaryNotice();
        setListingPreviewUrl("");
        return;
      }
      if (event.key === "ArrowLeft") {
        event.preventDefault();
        moveListingPreview(-1);
        return;
      }
      if (event.key === "ArrowRight") {
        event.preventDefault();
        moveListingPreview(1);
      }
    }

    window.addEventListener("keydown", handleKeyDown);
    return () => {
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [listingPreviewBoundaryNotice, listingPreviewImages, listingPreviewIndex, listingPreviewUrl]);

  async function bootstrapSession() {
    setSessionLoading(true);
    setSessionError("");
    if (!hasAuthToken()) {
      setConsoleSession(null);
      setSessionLoading(false);
      return;
    }
    try {
      setConsoleSession(await loadAdminSession());
    } catch (error) {
      clearAuthSession();
      setConsoleSession(null);
      setSessionError((error as Error).message);
    } finally {
      setSessionLoading(false);
    }
  }

  async function refreshDashboard() {
    setDashboardLoading(true);
    setDashboardError("");
    try {
      const data = await loadAdminDashboard();
      setDashboard(data);
    } catch (error) {
      setDashboardError((error as Error).message);
    } finally {
      setDashboardLoading(false);
    }
  }

  async function refreshListings() {
    setListingsLoading(true);
    setListingsError("");
    try {
      const data = await loadAdminListings(listingFilters);
      setListings(data);
      if (!selectedListingNo && data.rows[0]) {
        setSelectedListingNo(data.rows[0].listingNo);
      }
      if (selectedListingNo && !data.rows.find((item) => item.listingNo === selectedListingNo)) {
        setSelectedListingNo(data.rows[0]?.listingNo ?? "");
      }
    } catch (error) {
      setListingsError((error as Error).message);
    } finally {
      setListingsLoading(false);
    }
  }

  async function refreshOrders() {
    setOrdersLoading(true);
    setOrdersError("");
    try {
      const data = await loadAdminOrders(orderFilters);
      setOrders(data);
      if (selectedOrderNo && !data.rows.find((item) => item.orderNo === selectedOrderNo)) {
        setSelectedOrderNo(data.rows[0]?.orderNo ?? "");
      }
    } catch (error) {
      setOrdersError((error as Error).message);
    } finally {
      setOrdersLoading(false);
    }
  }

  async function refreshStudios() {
    setStudiosLoading(true);
    setStudiosError("");
    try {
      const data = await loadAdminStudios(studioFilters);
      setStudios(data);
      if (selectedStudioId && !data.rows.find((row) => row.studioId === selectedStudioId)) {
        setSelectedStudioId(null);
      }
      setStudioShareRatioDrafts(
        data.rows.reduce<Record<number, string>>((result, row) => {
          result[row.studioId] = row.shareRatio.replace("%", "");
          return result;
        }, {})
      );
    } catch (error) {
      setStudiosError((error as Error).message);
    } finally {
      setStudiosLoading(false);
    }
  }

  async function refreshStudioApplications() {
    setStudioApplicationsLoading(true);
    setStudioApplicationsError("");
    try {
      const data = await loadAdminStudioApplications(studioApplicationFilters);
      setStudioApplications(data);
      if (!selectedStudioApplicationNo && data.rows[0]) {
        setSelectedStudioApplicationNo(data.rows[0].applicationNo);
      }
      if (selectedStudioApplicationNo && !data.rows.find((row) => row.applicationNo === selectedStudioApplicationNo)) {
        setSelectedStudioApplicationNo(data.rows[0]?.applicationNo ?? "");
      }
    } catch (error) {
      setStudioApplicationsError((error as Error).message);
    } finally {
      setStudioApplicationsLoading(false);
    }
  }

  useEffect(() => {
    if (activeTab !== "studio" || !selectedStudioId) {
      setStudioDetail(null);
      return;
    }
    setStudioDetailLoading(true);
    loadAdminStudioDetail(selectedStudioId)
      .then(setStudioDetail)
      .catch((error: Error) => {
        setNotice(error.message);
        setStudioDetail(null);
      })
      .finally(() => setStudioDetailLoading(false));
  }, [activeTab, selectedStudioId]);

  useEffect(() => {
    if (!studioDetail) {
      return;
    }
    setStudioForm({
      studioId: studioDetail.summary.studioId,
      ownerPhone: studioDetail.summary.ownerPhoneRaw || (studios?.rows.find((row) => row.studioId === studioDetail.summary.studioId)?.ownerPhoneRaw ?? ""),
      studioName: studioDetail.summary.studioName === "-" ? "" : studioDetail.summary.studioName,
      description: studioDetail.summary.description === "暂未填写工作室简介" ? "" : studioDetail.summary.description,
      contactPhone: studioDetail.summary.contactPhone === "-" ? "" : studioDetail.summary.contactPhone,
      contactName: studioDetail.summary.contactName === "-" ? "" : studioDetail.summary.contactName,
      contactWechat: studioDetail.summary.contactWechat === "-" ? "" : studioDetail.summary.contactWechat,
      qualificationCode: studioDetail.summary.qualificationCode === "-" ? "" : studioDetail.summary.qualificationCode,
      qualificationMaterialKey: studioDetail.summary.qualificationMaterialKey || "",
      qualificationMaterialUrl: studioDetail.summary.qualificationMaterialUrl || "",
      qualificationNote: studioDetail.summary.qualificationNote === "-" ? "" : studioDetail.summary.qualificationNote,
      reviewStrategy: studios?.rows.find((row) => row.studioId === studioDetail.summary.studioId)?.reviewStrategy ?? "REVIEW_REQUIRED",
      shareRatio: (studioDetail.summary.shareRatio || "70%").replace("%", ""),
      active: studioDetail.summary.activeText === "合作中",
      cooperationStatus: studioDetail.summary.cooperationStatus || "ACTIVE",
    });
  }, [studioDetail, studios]);

  async function refreshBoosting() {
    setBoostingLoading(true);
    setBoostingError("");
    try {
      setBoostingCenter(await loadAdminBoostingServices(boostingFilters));
    } catch (error) {
      setBoostingError((error as Error).message);
    } finally {
      setBoostingLoading(false);
    }
  }

  async function refreshWithdraws() {
    setWithdrawLoading(true);
    setWithdrawError("");
    try {
      const data = await loadAdminWithdraws(withdrawFilters);
      setWithdrawCenter(data);
      if (!selectedWithdrawApplicationNo && data.rows[0]) {
        setSelectedWithdrawApplicationNo(data.rows[0].applicationNo);
      }
      if (selectedWithdrawApplicationNo && !data.rows.find((row) => row.applicationNo === selectedWithdrawApplicationNo)) {
        setSelectedWithdrawApplicationNo(data.rows[0]?.applicationNo ?? "");
      }
    } catch (error) {
      setWithdrawError((error as Error).message);
    } finally {
      setWithdrawLoading(false);
    }
  }

  async function refreshStudioWithdraws() {
    setStudioWithdrawLoading(true);
    setStudioWithdrawError("");
    try {
      const data = await loadAdminStudioWithdraws(withdrawFilters);
      setStudioWithdrawCenter(data);
      if (!selectedStudioWithdrawApplicationNo && data.rows[0]) {
        setSelectedStudioWithdrawApplicationNo(data.rows[0].applicationNo);
      }
      if (selectedStudioWithdrawApplicationNo && !data.rows.find((row) => row.applicationNo === selectedStudioWithdrawApplicationNo)) {
        setSelectedStudioWithdrawApplicationNo(data.rows[0]?.applicationNo ?? "");
      }
    } catch (error) {
      setStudioWithdrawError((error as Error).message);
    } finally {
      setStudioWithdrawLoading(false);
    }
  }

  async function refreshOperations() {
    setOperationLoading(true);
    setOperationError("");
    try {
      const data = await loadAdminOperations();
      setOperationCenter(data);
      setSelectedBannerIds((current) => current.filter((id) => data.banners.some((row) => row.bannerId === id)));
      setSelectedShortcutIds((current) => current.filter((id) => data.shortcuts.some((row) => row.shortcutId === id)));
      setSelectedAnnouncementIds((current) => current.filter((id) => data.announcements.some((row) => row.announcementId === id)));
      if (!selectedBannerId && data.banners[0]) {
        setSelectedBannerId(data.banners[0].bannerId);
      }
      if (selectedBannerId && !data.banners.find((row) => row.bannerId === selectedBannerId)) {
        setSelectedBannerId(data.banners[0]?.bannerId ?? null);
      }
      if (!selectedShortcutId && data.shortcuts[0]) {
        setSelectedShortcutId(data.shortcuts[0].shortcutId);
      }
      if (selectedShortcutId && !data.shortcuts.find((row) => row.shortcutId === selectedShortcutId)) {
        setSelectedShortcutId(data.shortcuts[0]?.shortcutId ?? null);
      }
      if (!selectedAnnouncementId && data.announcements[0]) {
        setSelectedAnnouncementId(data.announcements[0].announcementId);
      }
      if (selectedAnnouncementId && !data.announcements.find((row) => row.announcementId === selectedAnnouncementId)) {
        setSelectedAnnouncementId(data.announcements[0]?.announcementId ?? null);
      }
    } catch (error) {
      setOperationError((error as Error).message);
    } finally {
      setOperationLoading(false);
    }
  }

  async function refreshUsers() {
    setUserLoading(true);
    setUserError("");
    try {
      const data = await loadAdminUsers(userFilters);
      setUserCenter(data);
      if (!selectedUserId && data.rows[0]) {
        setSelectedUserId(data.rows[0].userId);
      }
      if (selectedUserId && !data.rows.find((row) => row.userId === selectedUserId)) {
        setSelectedUserId(data.rows[0]?.userId ?? null);
      }
    } catch (error) {
      setUserError((error as Error).message);
    } finally {
      setUserLoading(false);
    }
  }

  async function refreshRealNames() {
    setRealNameLoading(true);
    setRealNameError("");
    try {
      const data = await loadAdminRealNameReviews(realNameStatus);
      setRealNameCenter(data);
      if (!selectedRealNameUserId && data.rows[0]) {
        setSelectedRealNameUserId(data.rows[0].userId);
      }
      if (selectedRealNameUserId && !data.rows.find((row) => row.userId === selectedRealNameUserId)) {
        setSelectedRealNameUserId(data.rows[0]?.userId ?? null);
      }
    } catch (error) {
      setRealNameError((error as Error).message);
    } finally {
      setRealNameLoading(false);
    }
  }

  async function refreshRoles() {
    setRoleLoading(true);
    setRoleError("");
    try {
      const data = await loadAdminRoles();
      setRoleCenter(data);
      if (!selectedRoleId && data.rows[0]) {
        setSelectedRoleId(data.rows[0].roleId);
      }
      if (selectedRoleId && !data.rows.find((row) => row.roleId === selectedRoleId)) {
        setSelectedRoleId(data.rows[0]?.roleId ?? null);
      }
    } catch (error) {
      setRoleError((error as Error).message);
    } finally {
      setRoleLoading(false);
    }
  }

  async function refreshRoleCandidates() {
    setRoleUserLoading(true);
    try {
      const data = await loadAdminUsers({ status: "ALL", verified: "ALL", studioOwner: "ALL", keyword: roleUserKeyword });
      setRoleUserCandidates(data.rows);
    } catch (error) {
      setNotice((error as Error).message);
    } finally {
      setRoleUserLoading(false);
    }
  }

  async function handleListingAction(action: "APPROVE" | "REJECT" | "OFFLINE") {
    if (!selectedListingNo) return;
    const rejectReason = action === "REJECT" ? listingReviewReason.trim() : "";
    if (action === "REJECT" && !rejectReason) {
      setNotice("请输入驳回原因");
      return;
    }
    try {
      const result = await reviewAdminListing(selectedListingNo, action, rejectReason || undefined);
      setNotice(action === "APPROVE" ? "已经通过" : String((result as { message?: string }).message ?? "操作成功"));
      setListingReviewReason("");
      await Promise.all([refreshListings(), refreshDashboard()]);
      setSelectedListingNo(selectedListingNo);
    } catch (error) {
      setNotice((error as Error).message);
    }
  }

  function openListingPreview(url: string, index?: number) {
    const nextIndex = typeof index === "number" ? index : listingPreviewImages.indexOf(url);
    const normalizedIndex = nextIndex >= 0 ? nextIndex : 0;
    clearListingPreviewBoundaryNotice();
    setListingPreviewIndex(normalizedIndex);
    setListingPreviewUrl(listingPreviewImages[normalizedIndex] ?? url);
  }

  function moveListingPreview(delta: number) {
    if (!listingPreviewImages.length) {
      return;
    }
    const nextIndex = listingPreviewIndex + delta;
    if (nextIndex < 0) {
      showListingPreviewBoundaryNotice("已经是第一张图");
      return;
    }
    if (nextIndex >= listingPreviewImages.length) {
      showListingPreviewBoundaryNotice("此为最后一张图");
      return;
    }
    clearListingPreviewBoundaryNotice();
    setListingPreviewIndex(nextIndex);
    setListingPreviewUrl(listingPreviewImages[nextIndex]);
  }

  function showListingPreviewBoundaryNotice(message: string) {
    if (listingPreviewBoundaryNotice) {
      return;
    }
    setListingPreviewBoundaryNotice(message);
    listingPreviewNoticeTimerRef.current = window.setTimeout(() => {
      listingPreviewNoticeTimerRef.current = null;
      setListingPreviewBoundaryNotice("");
    }, 500);
  }

  function clearListingPreviewBoundaryNotice() {
    if (listingPreviewNoticeTimerRef.current) {
      window.clearTimeout(listingPreviewNoticeTimerRef.current);
      listingPreviewNoticeTimerRef.current = null;
    }
    setListingPreviewBoundaryNotice("");
  }

  async function handleDeleteListing() {
    if (!selectedListingNo || !listingDetail) {
      return;
    }
    if (listingDetail.summary.status !== "OFFLINE") {
      setNotice("请先下架账号后再删除");
      return;
    }
    if (!window.confirm(`删除账号《${listingDetail.summary.title}》后不可恢复，确认继续吗？`)) {
      return;
    }
    setListingDeleting(true);
    try {
      const result = await deleteAdminListing(selectedListingNo);
      setNotice(String((result as { message?: string }).message ?? "账号记录已删除"));
      setListingReviewReason("");
      setListingDetail(null);
      setSelectedListingNo("");
      await Promise.all([refreshListings(), refreshDashboard()]);
    } catch (error) {
      setNotice((error as Error).message);
    } finally {
      setListingDeleting(false);
    }
  }

  async function handleStudioPolicy(studioId: number, reviewStrategy: string) {
    try {
      await updateStudioPolicy(studioId, reviewStrategy);
      setNotice("工作室审核策略已更新");
      await Promise.all([refreshStudios(), refreshDashboard()]);
    } catch (error) {
      setNotice((error as Error).message);
    }
  }

  async function handleStudioStatus(studioId: number, active: boolean) {
    try {
      await updateStudioStatus(studioId, active);
      setNotice(active ? "工作室已恢复合作" : "工作室已暂停合作");
      await Promise.all([refreshStudios(), refreshDashboard()]);
    } catch (error) {
      setNotice((error as Error).message);
    }
  }

  async function handleStudioApplicationReview(action: "APPROVE" | "REJECT", applicationNo = selectedStudioApplicationNo) {
    if (!applicationNo) {
      return;
    }
    const rejectReason = action === "REJECT" ? studioApplicationReviewReason.trim() : "";
    if (action === "REJECT" && !rejectReason) {
      setNotice("请输入驳回原因");
      return;
    }
    try {
      const result = await reviewStudioApplication(applicationNo, action, rejectReason || undefined) as { message?: string; studioId?: number };
      setNotice(String(result.message ?? "审核成功"));
      setStudioApplicationReviewReason("");
      await Promise.all([refreshStudioApplications(), refreshStudios(), refreshDashboard()]);
      if (result.studioId) {
        setSelectedStudioId(result.studioId);
      }
    } catch (error) {
      setNotice((error as Error).message);
    }
  }

  async function handleStudioShareRatio(studioId: number) {
    const value = (studioShareRatioDrafts[studioId] ?? "").trim();
    if (!value) {
      setNotice("请输入分润比例，示例 0.70");
      return;
    }
    try {
      await updateStudioShareRatio(studioId, value);
      setNotice("工作室分润比例已更新");
      await Promise.all([refreshStudios(), refreshDashboard()]);
    } catch (error) {
      setNotice((error as Error).message);
    }
  }

  function handleCreateStudioDraft() {
    setSelectedStudioId(null);
    setStudioModalOpen(true);
    setStudioDetail(null);
    setStudioForm({
      studioId: 0,
      ownerPhone: "",
      studioName: "",
      description: "",
      contactPhone: "",
      contactName: "",
      contactWechat: "",
      qualificationCode: "",
      qualificationMaterialKey: "",
      qualificationMaterialUrl: "",
      qualificationNote: "",
      reviewStrategy: "REVIEW_REQUIRED",
      shareRatio: "0.70",
      active: true,
      cooperationStatus: "ACTIVE",
    });
  }

  function handleOpenStudioModal(studioId: number) {
    setSelectedStudioId(studioId);
    setStudioModalOpen(true);
  }

  function handleCloseStudioModal() {
    setStudioModalOpen(false);
  }

  async function handleSaveStudio() {
    if (!studioForm.ownerPhone.trim() || !studioForm.studioName.trim() || !studioForm.contactPhone.trim()) {
      setNotice("请填写负责人手机号、工作室名称和联系电话");
      return;
    }
    setStudioSaving(true);
    try {
      const result = await saveAdminStudio({
        studioId: studioForm.studioId || undefined,
        ownerPhone: studioForm.ownerPhone.trim(),
        studioName: studioForm.studioName.trim(),
        description: studioForm.description.trim(),
        contactPhone: studioForm.contactPhone.trim(),
        contactName: studioForm.contactName.trim(),
        contactWechat: studioForm.contactWechat.trim(),
        qualificationCode: studioForm.qualificationCode.trim(),
        qualificationMaterialKey: studioForm.qualificationMaterialKey.trim(),
        qualificationNote: studioForm.qualificationNote.trim(),
        reviewStrategy: studioForm.reviewStrategy,
        shareRatio: studioForm.shareRatio.trim(),
        active: studioForm.active,
        cooperationStatus: studioForm.cooperationStatus,
      });
      const nextStudioId = Number((result as { studioId?: number }).studioId ?? studioForm.studioId ?? 0);
      setNotice(String((result as { message?: string }).message ?? "工作室资料已保存"));
      await Promise.all([refreshStudios(), refreshDashboard()]);
      if (nextStudioId) {
        setSelectedStudioId(nextStudioId);
      }
    } catch (error) {
      setNotice((error as Error).message);
    } finally {
      setStudioSaving(false);
    }
  }

  async function handleUploadStudioQualification(file: File) {
    setStudioAssetUploading(true);
    try {
      if (!file.type.startsWith("image/")) {
        throw new Error("资质材料当前仅支持图片格式");
      }
      const uploaded = await uploadAdminAsset(file, "studio-qualification");
      setStudioForm((current) => ({
        ...current,
        qualificationMaterialKey: uploaded.objectKey,
        qualificationMaterialUrl: uploaded.previewUrl,
      }));
      setNotice("工作室资质材料已上传");
    } catch (error) {
      setNotice((error as Error).message);
    } finally {
      setStudioAssetUploading(false);
    }
  }

  async function handleBoostingStatus(serviceNo: string, status: string) {
    try {
      await updateBoostingServiceStatus(serviceNo, status);
      setNotice(status === "ACTIVE" ? "代肝服务已启用" : "代肝服务已停用");
      await Promise.all([refreshBoosting(), refreshDashboard()]);
    } catch (error) {
      setNotice((error as Error).message);
    }
  }

  async function handleWithdrawReview(action: "APPROVE" | "REJECT", applicationNo = selectedWithdrawApplicationNo) {
    if (!applicationNo) return;
    const rejectReason = action === "REJECT" ? withdrawReviewReason.trim() : "";
    if (action === "REJECT" && !rejectReason) {
      setNotice("请输入驳回原因");
      return;
    }
    try {
      const result = await reviewWithdraw(applicationNo, action, rejectReason || undefined);
      setNotice(String((result as { message?: string }).message ?? "操作成功"));
      setWithdrawReviewReason("");
      setWithdrawReviewDialog(null);
      await Promise.all([refreshWithdraws(), refreshDashboard()]);
    } catch (error) {
      setNotice((error as Error).message);
    }
  }

  async function handleStudioWithdrawReview(action: "APPROVE" | "REJECT", applicationNo = selectedStudioWithdrawApplicationNo) {
    if (!applicationNo) return;
    const rejectReason = action === "REJECT" ? studioWithdrawReviewReason.trim() : "";
    if (action === "REJECT" && !rejectReason) {
      setNotice("请输入驳回原因");
      return;
    }
    try {
      const result = await reviewStudioWithdraw(applicationNo, action, rejectReason || undefined);
      setNotice(String((result as { message?: string }).message ?? "操作成功"));
      setStudioWithdrawReviewReason("");
      setWithdrawReviewDialog(null);
      await Promise.all([refreshStudioWithdraws(), refreshDashboard()]);
    } catch (error) {
      setNotice((error as Error).message);
    }
  }

  async function handleCreateBanner() {
    setSelectedBannerId(null);
    setBannerForm({ bannerId: 0, title: "", imageKey: "", imageUrl: "", linkUrl: "", sortNo: "10", status: "ACTIVE" });
  }

  async function handleCreateShortcut() {
    setSelectedShortcutId(null);
    setShortcutForm({ shortcutId: 0, name: "", iconKey: "", iconUrl: "", linkUrl: "", sortNo: "10", status: "ACTIVE" });
  }

  async function handleCreateAnnouncement() {
    setSelectedAnnouncementId(null);
    setAnnouncementForm({ announcementId: 0, title: "", content: "", category: "SYSTEM", pinned: false, status: "PUBLISHED" });
  }

  function toggleSelectedId(kind: "banner" | "shortcut" | "announcement", id: number) {
    if (kind === "banner") {
      setSelectedBannerIds((current) => current.includes(id) ? current.filter((item) => item !== id) : [...current, id]);
      return;
    }
    if (kind === "shortcut") {
      setSelectedShortcutIds((current) => current.includes(id) ? current.filter((item) => item !== id) : [...current, id]);
      return;
    }
    setSelectedAnnouncementIds((current) => current.includes(id) ? current.filter((item) => item !== id) : [...current, id]);
  }

  function toggleSelectAll(kind: "banner" | "shortcut" | "announcement", ids: number[]) {
    if (kind === "banner") {
      setSelectedBannerIds((current) => current.length === ids.length ? [] : ids);
      return;
    }
    if (kind === "shortcut") {
      setSelectedShortcutIds((current) => current.length === ids.length ? [] : ids);
      return;
    }
    setSelectedAnnouncementIds((current) => current.length === ids.length ? [] : ids);
  }

  async function handleDeleteOperation(kind: "banner" | "shortcut" | "announcement", id?: number) {
    const targetId = id ?? (kind === "banner" ? selectedBannerId : kind === "shortcut" ? selectedShortcutId : selectedAnnouncementId);
    if (!targetId) {
      setNotice("请先选择要删除的记录");
      return;
    }
    setOperationDeleting(kind);
    try {
      if (kind === "banner") {
        await deleteAdminBanner(targetId);
        setSelectedBannerId(null);
        setSelectedBannerIds((current) => current.filter((item) => item !== targetId));
      } else if (kind === "shortcut") {
        await deleteAdminShortcut(targetId);
        setSelectedShortcutId(null);
        setSelectedShortcutIds((current) => current.filter((item) => item !== targetId));
      } else {
        await deleteAdminAnnouncement(targetId);
        setSelectedAnnouncementId(null);
        setSelectedAnnouncementIds((current) => current.filter((item) => item !== targetId));
      }
      setNotice("记录已删除");
      await refreshOperations();
    } catch (error) {
      setNotice((error as Error).message);
    } finally {
      setOperationDeleting("");
    }
  }

  async function handleBatchOperationStatus(
    kind: "banner" | "shortcut" | "announcement",
    status: string
  ) {
    const ids = kind === "banner" ? selectedBannerIds : kind === "shortcut" ? selectedShortcutIds : selectedAnnouncementIds;
    if (ids.length === 0) {
      setNotice("请至少勾选一条记录");
      return;
    }
    setOperationBatchLoading(kind);
    try {
      await batchAdminOperationStatus(kind, { ids, status });
      setNotice("批量状态已更新");
      await refreshOperations();
    } catch (error) {
      setNotice((error as Error).message);
    } finally {
      setOperationBatchLoading("");
    }
  }

  async function handleUploadOperationAsset(kind: "banner" | "shortcut", file: File) {
    setOperationUploading(kind);
    try {
      if (!file.type.startsWith("image/")) {
        throw new Error("仅支持上传图片格式素材");
      }
      const uploaded = await uploadAdminAsset(file, kind === "banner" ? "operation-banners" : "operation-shortcuts");
      if (kind === "banner") {
        setBannerForm((current) => ({ ...current, imageKey: uploaded.objectKey, imageUrl: uploaded.previewUrl }));
      } else {
        setShortcutForm((current) => ({ ...current, iconKey: uploaded.objectKey, iconUrl: uploaded.previewUrl }));
      }
      setNotice(kind === "banner" ? "轮播图素材已上传" : "快捷入口图标已上传");
    } catch (error) {
      setNotice((error as Error).message);
    } finally {
      setOperationUploading("");
    }
  }

  async function handleSaveBannerEditor() {
    if (!bannerForm.title.trim() || !bannerForm.imageKey.trim()) {
      setNotice("请先填写轮播标题并上传轮播图片");
      return;
    }
    setOperationSaving("banner");
    try {
      await saveAdminBanner({
        bannerId: bannerForm.bannerId || undefined,
        title: bannerForm.title.trim(),
        imageKey: bannerForm.imageKey.trim(),
        linkUrl: bannerForm.linkUrl.trim(),
        sortNo: Number(bannerForm.sortNo || "0"),
        status: bannerForm.status,
      });
      setNotice("轮播图已保存");
      await refreshOperations();
    } catch (error) {
      setNotice((error as Error).message);
    } finally {
      setOperationSaving("");
    }
  }

  async function handleSaveShortcutEditor() {
    if (!shortcutForm.name.trim() || !shortcutForm.linkUrl.trim()) {
      setNotice("请先填写入口名称与跳转链接");
      return;
    }
    setOperationSaving("shortcut");
    try {
      await saveAdminShortcut({
        shortcutId: shortcutForm.shortcutId || undefined,
        name: shortcutForm.name.trim(),
        iconKey: shortcutForm.iconKey.trim() || null,
        linkUrl: shortcutForm.linkUrl.trim(),
        sortNo: Number(shortcutForm.sortNo || "0"),
        status: shortcutForm.status,
      });
      setNotice("快捷入口已保存");
      await refreshOperations();
    } catch (error) {
      setNotice((error as Error).message);
    } finally {
      setOperationSaving("");
    }
  }

  async function handleSaveAnnouncementEditor() {
    if (!announcementForm.title.trim() || !announcementForm.content.trim()) {
      setNotice("请先填写公告标题与内容");
      return;
    }
    setOperationSaving("announcement");
    try {
      await saveAdminAnnouncement({
        announcementId: announcementForm.announcementId || undefined,
        title: announcementForm.title.trim(),
        content: announcementForm.content.trim(),
        category: announcementForm.category,
        pinned: announcementForm.pinned,
        status: announcementForm.status,
      });
      setNotice("公告已保存");
      await refreshOperations();
    } catch (error) {
      setNotice((error as Error).message);
    } finally {
      setOperationSaving("");
    }
  }

  async function handleUserStatus(userId: number, currentStatus: string) {
    const nextStatus = currentStatus === "ACTIVE" ? "DISABLED" : "ACTIVE";
    const reason = nextStatus === "DISABLED" ? userStatusReason.trim() : "";
    if (nextStatus === "DISABLED" && !reason) {
      setNotice("请输入封禁原因");
      return;
    }
    try {
      await updateAdminUserStatus(userId, { status: nextStatus, reason: reason || undefined });
      setNotice(nextStatus === "DISABLED" ? "用户已封禁" : "用户已恢复");
      setUserStatusReason("");
      await refreshUsers();
    } catch (error) {
      setNotice((error as Error).message);
    }
  }

  async function handleResetUserPassword() {
    if (!selectedUserId) {
      setNotice("请先选中一个用户");
      return;
    }
    if (!resetPassword.password || !resetPassword.confirmPassword) {
      setNotice("请完整输入新密码和确认密码");
      return;
    }
    if (resetPassword.password !== resetPassword.confirmPassword) {
      setNotice("两次输入的新密码不一致");
      return;
    }
    setUserActionLoading(true);
    try {
      await resetAdminUserPassword(selectedUserId, { password: resetPassword.password });
      setNotice("用户登录密码已重置");
      setResetPassword({ password: "", confirmPassword: "" });
      await refreshUsers();
    } catch (error) {
      setNotice((error as Error).message);
    } finally {
      setUserActionLoading(false);
    }
  }

  async function handleRealNameReview(userId: number, action: "APPROVE" | "REJECT") {
    const reason = action === "REJECT" ? realNameReviewReason.trim() : "";
    if (action === "REJECT" && !reason) {
      setNotice("请输入驳回原因");
      return;
    }
    try {
      await reviewAdminRealName(userId, { action, reason: reason || undefined });
      setNotice(action === "APPROVE" ? "实名认证已通过" : "实名认证已驳回");
      setRealNameReviewReason("");
      await Promise.all([refreshRealNames(), refreshUsers()]);
    } catch (error) {
      setNotice((error as Error).message);
    }
  }

  async function handleCreateRole() {
    setSelectedRoleId(null);
    setRoleForm({ roleId: 0, roleCode: "", roleName: "", description: "", status: "ACTIVE", permissions: [], memberIds: [] });
  }

  async function handleSaveRoleEditor() {
    if (!roleForm.roleCode.trim() || !roleForm.roleName.trim()) {
      setNotice("角色编码和角色名称不能为空");
      return;
    }
    setRoleSaving(true);
    try {
      const saved = await saveAdminRole({
        roleId: roleForm.roleId || undefined,
        roleCode: roleForm.roleCode.trim().toUpperCase(),
        roleName: roleForm.roleName.trim(),
        description: roleForm.description.trim(),
        permissions: roleForm.permissions,
        status: roleForm.status,
      });
      const nextRoleId = Number((saved as { roleId?: number }).roleId ?? roleForm.roleId);
      const userIds = roleForm.memberIds;
      if (nextRoleId > 0) {
        await assignAdminRoleMembers(nextRoleId, userIds);
      }
      setNotice("角色与成员已保存");
      await refreshRoles();
      if (nextRoleId > 0) {
        setSelectedRoleId(nextRoleId);
      }
    } catch (error) {
      setNotice((error as Error).message);
    } finally {
      setRoleSaving(false);
    }
  }

  function handleToggleRoleMember(userId: number) {
    setRoleForm((current) => ({
      ...current,
      memberIds: current.memberIds.includes(userId)
        ? current.memberIds.filter((item) => item !== userId)
        : [...current.memberIds, userId],
    }));
  }

  function handleToggleRolePermission(permission: string) {
    setRoleForm((current) => ({
      ...current,
      permissions: current.permissions.includes(permission)
        ? current.permissions.filter((item) => item !== permission)
        : [...current.permissions, permission],
    }));
  }

  function handleTogglePermissionGroup(permissionValues: string[]) {
    setRoleForm((current) => {
      const allChecked = permissionValues.every((item) => current.permissions.includes(item));
      return {
        ...current,
        permissions: allChecked
          ? current.permissions.filter((item) => !permissionValues.includes(item))
          : Array.from(new Set([...current.permissions, ...permissionValues])),
      };
    });
  }

  async function handleLogin(phone: string, password: string) {
    setSessionError("");
    try {
      await loginByPassword(phone, password);
      await bootstrapSession();
    } catch (error) {
      setSessionError((error as Error).message);
    }
  }

  async function handleLogout() {
    await logout();
    setConsoleSession(null);
    setSessionError("");
  }

  useEffect(() => {
    if (!notice) {
      return;
    }
    const timer = window.setTimeout(() => setNotice(""), 2000);
    return () => window.clearTimeout(timer);
  }, [notice]);

  const heroDescription = useMemo(() => activeMenuItem?.description ?? "", [activeMenuItem]);
  const heroActions = useMemo(() => {
    if (activeTab === "dashboard") {
      return [
        { label: "刷新仪表盘", kind: "primary" as const, onClick: () => void refreshDashboard() },
        { label: "进入待审核", kind: "secondary" as const, onClick: () => setActiveTab("listing") },
      ];
    }
    if (activeTab === "listing") {
      return [
        { label: "刷新账号库", kind: "primary" as const, onClick: () => void refreshListings() },
        ...(selectedListingNo ? [{ label: "收起详情", kind: "secondary" as const, onClick: () => setSelectedListingNo("") }] : []),
      ];
    }
    if (activeTab === "order") {
      return [{ label: "刷新订单中心", kind: "primary" as const, onClick: () => void refreshOrders() }];
    }
    if (activeTab === "studio") {
      return [{ label: "刷新工作室数据", kind: "primary" as const, onClick: () => { void refreshStudioApplications(); void refreshStudios(); } }];
    }
    if (activeTab === "boosting") {
      return [{ label: "刷新代肝服务", kind: "primary" as const, onClick: () => void refreshBoosting() }];
    }
    if (activeTab === "withdraw") {
      return [{ label: "刷新提现审核", kind: "primary" as const, onClick: () => { void refreshWithdraws(); void refreshStudioWithdraws(); } }];
    }
    if (activeTab === "operation") {
      return [{ label: "刷新运营配置", kind: "primary" as const, onClick: () => void refreshOperations() }];
    }
    if (activeTab === "gunCode") {
      return [];
    }
    if (activeTab === "user") {
      return [{ label: "刷新用户列表", kind: "primary" as const, onClick: () => void refreshUsers() }];
    }
    if (activeTab === "realName") {
      return [{ label: "刷新实名队列", kind: "primary" as const, onClick: () => void refreshRealNames() }];
    }
    if (activeTab === "role") {
      return [{ label: "刷新角色权限", kind: "primary" as const, onClick: () => { void refreshRoles(); void refreshRoleCandidates(); } }];
    }
    return [];
  }, [activeTab, selectedListingNo]);

  if (sessionLoading) {
    return (
      <div className="console-shell console-auth-shell">
        <SurfaceCard eyebrow="管理后台" title="正在校验后台会话">
          <LoadingBlock title="正在同步后台登录态" />
        </SurfaceCard>
      </div>
    );
  }

  if (!consoleSession) {
    return (
      <ConsoleLoginScreen
        badge="管理后台"
        title="使用管理员账号进入控制台"
        description="请输入已分配后台角色的手机号和密码。"
        error={sessionError}
        onSubmit={handleLogin}
      />
    );
  }

  return (
    <div className="console-shell">
      <aside className="console-sidebar">
        <div className="dt-brand">
          <span className="dt-brand__mark dt-brand__mark--image">
            <img alt="萌虎" src={`${import.meta.env.BASE_URL}brand/menghu-ai-logo.png`} />
          </span>
          <div>
            <div>萌虎后台</div>
            <small>平台总控台</small>
          </div>
        </div>
        <div className="console-sidebar__profile">
          <strong>{consoleSession.nickname}</strong>
          <p>{consoleSession.roles.map((item) => item.roleName).join(" / ")}</p>
          <Button kind="secondary" onClick={() => void handleLogout()}>退出登录</Button>
        </div>
        <nav className="console-menu" aria-label="后台主菜单">
          {allowedMenuItems.map((item) => (
            <button
              className={`console-menu__item ${activeTab === item.key ? "is-active" : ""}`}
              key={item.key}
              onClick={() => setActiveTab(item.key)}
              type="button"
            >
              <strong>{item.label}</strong>
              <small>{item.description}</small>
            </button>
          ))}
        </nav>
      </aside>

      <main className="console-main">
        <section className="console-hero">
          <SectionHeading
            badge={activeTab === "dashboard" ? "管理后台" : "后台工作区"}
            title={activeTab === "dashboard" ? "统一处理审核、订单、工作室与财务动作" : activeMenuItem?.label ?? "后台工作台"}
            description={heroDescription}
          />
          {heroActions.length ? (
            <div className="console-actions">
              {heroActions.map((action) => (
                <Button key={action.label} kind={action.kind} onClick={action.onClick}>{action.label}</Button>
              ))}
            </div>
          ) : null}
        </section>

        {notice ? (
          <div className="console-notice" role="status" aria-live="polite">
            <span>{notice}</span>
            <button onClick={() => setNotice("")} type="button">关闭</button>
          </div>
        ) : null}

        {dashboardLoading ? <LoadingBlock title="正在加载管理后台数据" /> : null}
        {dashboardError ? <ErrorBlock title="管理后台加载失败" message={dashboardError} onRetry={() => void refreshDashboard()} /> : null}
        {activeTab === "dashboard" && dashboard ? (
          <section className="console-metrics">
            {dashboard.metrics.map((metric) => (
              <MetricCard key={metric.label} label={metric.label} value={metric.value} trend={metric.trend} />
            ))}
          </section>
        ) : null}

        {activeTab === "dashboard" && dashboard ? (
          <section className="console-grid">
            <SurfaceCard eyebrow="待办入口" title="高优先级队列" actions={<Tag tone="warning">实时待办</Tag>}>
              {dashboard.pendingQueue.length === 0 ? (
                <EmptyBlock title="当前没有待处理队列" />
              ) : (
                <div className="console-queue">
                  {dashboard.pendingQueue.map((item) => (
                    <button
                      className="console-queue__item"
                      key={`${item.type}-${item.primaryKey}`}
                      type="button"
                      onClick={() => setActiveTab(queueTabMap[item.type] ?? "dashboard")}
                    >
                      <div>
                        <strong>{item.title}</strong>
                        <p>{item.subtitle}</p>
                      </div>
                      <Tag tone="accent">{item.status}</Tag>
                    </button>
                  ))}
                </div>
              )}
            </SurfaceCard>
            <SurfaceCard eyebrow="运行概览" title="全局摘要">
              <div className="console-overview">
                <div className="console-overview__item">
                  <span>用户总数</span>
                  <strong>{dashboard.overview.userCount}</strong>
                </div>
                <div className="console-overview__item">
                  <span>工作室总数</span>
                  <strong>{dashboard.overview.studioCount}</strong>
                </div>
                <div className="console-overview__item">
                  <span>代肝服务数</span>
                  <strong>{dashboard.overview.boostingServiceCount}</strong>
                </div>
                <div className="console-overview__item">
                  <span>冻结资金</span>
                  <strong>{dashboard.overview.walletFrozenAmount}</strong>
                </div>
              </div>
            </SurfaceCard>
          </section>
        ) : null}

        {activeTab === "listing" ? (
          <section className="console-grid console-grid--detail console-listing-review-layout">
            <div className="console-listing-review-pane">
              <SurfaceCard eyebrow="账号库管理" title="审核队列">
              <ListingFilters value={listingFilters} onChange={setListingFilters} onSearch={() => void refreshListings()} />
              {listingsLoading ? <LoadingBlock title="正在加载账号库" compact /> : null}
              {listingsError ? <ErrorBlock title="账号库加载失败" message={listingsError} compact onRetry={() => void refreshListings()} /> : null}
              {listings && listings.rows.length > 0 ? (
                <>
                  <SummaryPills summary={listings.summary} labels={{ pendingReview: "待审核", published: "已上架", rejected: "已驳回", offline: "已下架" }} />
                  <div className="console-table-scroll">
                    <table className="console-table console-table--listing">
                      <thead>
                        <tr>
                          <th>账号标题</th>
                          <th>卖家</th>
                          <th>状态</th>
                          <th>售价</th>
                          <th>发布比例</th>
                          <th>浏览/收藏</th>
                          <th>成交</th>
                          <th>更新时间</th>
                        </tr>
                      </thead>
                      <tbody>
                        {listings.rows.map((row) => (
                          <tr
                            className={selectedListingNo === row.listingNo ? "is-active" : ""}
                            key={row.listingNo}
                            onClick={() => setSelectedListingNo(row.listingNo)}
                          >
                            <td>
                              <strong>{row.title}</strong>
                              <p>{row.listingNo}</p>
                            </td>
                            <td>{row.sellerDisplayName}<p>{row.sellerTypeText}</p></td>
                            <td>{row.statusText}<p>{row.reviewProgress}</p></td>
                            <td>{row.price}</td>
                            <td>{row.exchangeRateLabel || "-"}</td>
                            <td>{row.viewCount} / {row.favoriteCount}</td>
                            <td>{row.salesCount}</td>
                            <td>{row.updatedAt}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </>
              ) : listings && !listingsLoading ? <EmptyBlock title="没有符合条件的账号资源" /> : null}
              </SurfaceCard>
            </div>

            <div className="console-listing-review-pane console-listing-review-pane--detail">
              <SurfaceCard eyebrow="审核详情" title="当前账号详情">
              {listingDetailLoading ? <LoadingBlock title="正在加载账号详情" compact /> : null}
              {!listingDetailLoading && listingDetail ? (
                <div className="console-listing-detail">
                  <div className="console-listing-detail__hero">
                    <div className="console-listing-detail__gallery">
                      {listingDetail.summary.coverUrl ? (
                        <button className="console-listing-detail__cover" type="button" onClick={() => openListingPreview(listingDetail.summary.coverUrl || "", 0)}>
                          <img alt={listingDetail.summary.title} src={listingDetail.summary.coverUrl} />
                        </button>
                      ) : (
                        <div className="console-detail__placeholder">无封面</div>
                      )}
                      {listingDetail.summary.imageUrls.length ? (
                        <div className="console-listing-detail__thumbs">
                          {listingDetail.summary.imageUrls.map((url, index) => (
                            <button key={`${listingDetail.summary.listingNo}-${index}-${url}`} type="button" onClick={() => openListingPreview(url)}>
                              <img alt={`账号截图 ${index + 1}`} src={url} />
                            </button>
                          ))}
                        </div>
                      ) : null}
                    </div>
                    <div className="console-listing-detail__head">
                      <h3>{listingDetail.summary.title}</h3>
                      <div className="console-detail__meta">
                        <Tag>{listingDetail.summary.statusText}</Tag>
                        <Tag tone="accent">{listingDetail.summary.sellerTypeText}</Tag>
                        <Tag tone="success">{listingDetail.summary.reviewStrategy}</Tag>
                      </div>
                      <div className="console-detail__actions">
                        {listingDetail.summary.status === "PENDING_REVIEW" ? (
                          <Button onClick={() => void handleListingAction("APPROVE")}>审核通过</Button>
                        ) : null}
                        {listingDetail.summary.status !== "PENDING_REVIEW" && listingDetail.summary.status !== "PUBLISHED" ? (
                          <Button onClick={() => void handleListingAction("APPROVE")}>强制上架</Button>
                        ) : null}
                        {listingDetail.summary.status === "PENDING_REVIEW" ? (
                          <Button kind="secondary" onClick={() => void handleListingAction("REJECT")}>驳回</Button>
                        ) : null}
                        {listingDetail.summary.status === "PUBLISHED" ? (
                          <Button kind="ghost" onClick={() => void handleListingAction("OFFLINE")}>下架</Button>
                        ) : null}
                        {listingDetail.summary.status === "OFFLINE" ? (
                          <Button className="console-danger-action" kind="ghost" disabled={listingDeleting} onClick={() => void handleDeleteListing()}>
                            {listingDeleting ? "删除中..." : "删除"}
                          </Button>
                        ) : null}
                      </div>
                    </div>
                  </div>

                  <section className="console-listing-detail-section">
                    <h4>基础信息</h4>
                    <dl className="console-detail__facts">
                      <div><dt>账号编号</dt><dd>{listingDetail.summary.listingNo}</dd></div>
                      <div><dt>卖家</dt><dd>{listingDetail.summary.sellerDisplayName}</dd></div>
                      <div><dt>手机号</dt><dd>{listingDetail.summary.sellerPhone}</dd></div>
                      <div><dt>售价</dt><dd>{listingDetail.summary.price}</dd></div>
                      <div><dt>发布比例</dt><dd>{listingDetail.summary.exchangeRateLabel || "-"}</dd></div>
                      <div><dt>提交时间</dt><dd>{listingDetail.summary.submittedAt}</dd></div>
                    </dl>
                  </section>

                  <section className="console-listing-detail-section">
                    <h4>账号描述</h4>
                    <p className="console-detail__desc">{listingDetail.summary.description}</p>
                  </section>

                  {listingDetail.detailSections.map((section) => (
                    <section className="console-listing-detail-section" key={section.title}>
                      <h4>{section.title}</h4>
                      <dl className="console-detail__facts">
                        {section.items.map((item) => (
                          <div key={`${section.title}-${item.label}`}>
                            <dt>{item.label}</dt>
                            <dd>{item.value}</dd>
                          </div>
                        ))}
                      </dl>
                    </section>
                  ))}

                  <section className="console-listing-detail-section">
                    <h4>审核记录</h4>
                      {listingDetail.reviewRecords.map((record, index) => (
                        <div className="console-timeline__item" key={`${record.label}-${index}`}>
                          <strong>{record.label}</strong>
                          <span>{record.status}</span>
                          <p>{record.remark}</p>
                          <small>{record.time}</small>
                        </div>
                      ))}
                  </section>

                  <div className="console-review-panel">
                      <div className="console-review-panel__head">
                        <h4>审核操作</h4>
                        <span>
                          {listingDetail.summary.status === "PENDING_REVIEW"
                            ? "驳回时需填写原因，审核通过后会直接上架。"
                            : listingDetail.summary.status === "PUBLISHED"
                              ? "当前账号已完成审核，如需删除请先下架。"
                              : listingDetail.summary.status === "OFFLINE"
                                ? "当前账号已下架，可以直接删除，也可强制上架恢复展示。"
                                : `当前状态为${listingDetail.summary.statusText}，可强制上架恢复展示。`}
                        </span>
                      </div>
                      {listingDetail.summary.status === "PENDING_REVIEW" ? (
                        <label>
                          驳回原因
                          <textarea
                            rows={3}
                            placeholder="例如：截图不清晰、账号信息不完整"
                            value={listingReviewReason}
                            onChange={(event) => setListingReviewReason(event.target.value)}
                          />
                        </label>
                      ) : (
                        <div className="console-review-panel__summary">
                          <strong>当前状态：{listingDetail.summary.statusText}</strong>
                          <span>{listingDetail.summary.rejectionReason || "该账号当前没有可执行的审核动作。"}</span>
                        </div>
                      )}
                      <div className="console-user-panel__actions">
                        {listingDetail.summary.status === "PENDING_REVIEW" ? (
                          <Button onClick={() => void handleListingAction("APPROVE")}>审核通过</Button>
                        ) : null}
                        {listingDetail.summary.status !== "PENDING_REVIEW" && listingDetail.summary.status !== "PUBLISHED" ? (
                          <Button onClick={() => void handleListingAction("APPROVE")}>强制上架</Button>
                        ) : null}
                        {listingDetail.summary.status === "PENDING_REVIEW" ? (
                          <Button kind="secondary" onClick={() => void handleListingAction("REJECT")}>驳回</Button>
                        ) : null}
                        {listingDetail.summary.status === "PUBLISHED" ? (
                          <Button kind="ghost" onClick={() => void handleListingAction("OFFLINE")}>下架</Button>
                        ) : null}
                        {listingDetail.summary.status === "OFFLINE" ? (
                          <Button className="console-danger-action" kind="ghost" disabled={listingDeleting} onClick={() => void handleDeleteListing()}>
                            {listingDeleting ? "删除中..." : "删除"}
                          </Button>
                        ) : null}
                      </div>
                    </div>
                </div>
              ) : null}
              {!listingDetailLoading && !listingDetail ? <EmptyBlock title="从左侧列表选择一个账号查看详情" /> : null}
              </SurfaceCard>
            </div>
          </section>
        ) : null}

        {activeTab === "order" ? (
          <SurfaceCard eyebrow="订单中心" title="交易订单">
            <InlineFilters
              fields={[
                {
                  key: "status",
                  label: "订单状态",
                  value: orderFilters.status,
                  options: [
                    ["ALL", "全部"],
                    ["PENDING_PAYMENT", "待付款"],
                    ["WAITING_TRADE", "待交易"],
                    ["COMPLETED", "已完成"],
                    ["REFUND_PENDING", "退款审核中"],
                    ["AFTER_SALE", "售后中"],
                    ["REFUNDED", "已退款"],
                    ["CLOSED", "已关闭"],
                  ],
                },
                {
                  key: "sellerType",
                  label: "卖家类型",
                  value: orderFilters.sellerType,
                  options: [["ALL", "全部"], ["PERSONAL", "个人"], ["STUDIO", "工作室"]],
                },
              ]}
              onChange={(key, value) => setOrderFilters((current) => ({ ...current, [key]: value }))}
              onSearch={() => void refreshOrders()}
            />
            {ordersLoading ? <LoadingBlock title="正在加载交易订单" compact /> : null}
            {ordersError ? <ErrorBlock title="订单中心加载失败" message={ordersError} compact onRetry={() => void refreshOrders()} /> : null}
            {orders && orders.rows.length > 0 ? (
              <>
                <SummaryPills summary={orders.summary} labels={{ pendingPayment: "待付款", waitingTrade: "待交易", completed: "已完成", refundPending: "退款审核中", afterSale: "售后中", refunded: "已退款", closed: "已关闭" }} />
                <div className="console-table-scroll">
                  <table className="console-table console-table--orders">
                    <colgroup>
                      <col className="console-order-col--order" />
                      <col className="console-order-col--listing" />
                      <col className="console-order-col--users" />
                      <col className="console-order-col--status" />
                      <col className="console-order-col--amount" />
                      <col className="console-order-col--refund" />
                      <col className="console-order-col--pay" />
                      <col className="console-order-col--time" />
                      <col className="console-order-col--actions" />
                    </colgroup>
                    <thead>
                      <tr>
                        <th>订单号</th>
                        <th>商品</th>
                        <th>买家 / 卖家</th>
                        <th>状态</th>
                        <th>金额</th>
                        <th>退款</th>
                        <th>支付方式</th>
                        <th>时间</th>
                        <th>操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      {orders.rows.map((row) => (
                        <tr
                          key={row.orderNo}
                        >
                          <td>{row.orderNo}</td>
                          <td><strong>{row.listingTitle}</strong><p>{row.listingNo}</p></td>
                          <td>{row.buyerNickname}<p>{row.sellerDisplayName} · {row.sellerTypeText}</p></td>
                          <td>{row.statusText}</td>
                          <td>{row.totalAmount}<p>货款 {row.itemAmount} / 服务费 {row.serviceFee}</p></td>
                          <td>{row.refundAmount}<p>{row.refundRequestedAt}</p></td>
                          <td>{row.paymentMethod}</td>
                          <td>{row.createdAt}<p>{row.completedAt}</p></td>
                          <td>
                            <div className="console-table__actions">
                              <Button kind="secondary" onClick={() => setSelectedOrderNo(row.orderNo)}>
                                详情
                              </Button>
                              {row.chatGroupNo ? (
                                <Button kind="ghost" onClick={() => setSelectedOrderNo(row.orderNo)}>
                                  聊天
                                </Button>
                              ) : null}
                              {row.canForceRefund ? (
                                <Button
                                  kind="secondary"
                                  onClick={async (event) => {
                                    event.stopPropagation();
                                  const reason = window.prompt("请输入强制退款原因", row.refundReason || "平台客服介入强制退款");
                                  if (!reason || !reason.trim()) return;
                                  try {
                                    await forceAdminRefund(row.orderNo, reason.trim());
                                    await refreshOrders();
                                  } catch (error) {
                                    setOrdersError(error instanceof Error ? error.message : "强制退款失败");
                                  }
                                }}
                                >
                                  强制退款
                                </Button>
                              ) : null}
                            </div>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </>
            ) : orders && !ordersLoading ? <EmptyBlock title="暂无交易订单" /> : null}
          </SurfaceCard>
        ) : null}

        {activeTab === "studio" ? (
          <section className="console-studio-layout">
            <SurfaceCard eyebrow="入驻审核" title="用户申请成为工作室">
              <InlineKeywordFilters
                keyword={studioApplicationFilters.keyword}
                keywordLabel="申请单 / 工作室 / 联系人搜索"
                selectLabel="审核状态"
                selectValue={studioApplicationFilters.status}
                selectOptions={[["ALL", "全部"], ["PENDING", "待审核"], ["APPROVED", "已通过"], ["REJECTED", "已驳回"]]}
                onKeywordChange={(keyword) => setStudioApplicationFilters((current) => ({ ...current, keyword }))}
                onSelectChange={(status) => setStudioApplicationFilters((current) => ({ ...current, status }))}
                onSearch={() => void refreshStudioApplications()}
              />
              {studioApplicationsLoading ? <LoadingBlock title="正在加载工作室申请列表" compact /> : null}
              {studioApplicationsError ? (
                <ErrorBlock title="工作室申请列表加载失败" message={studioApplicationsError} compact onRetry={() => void refreshStudioApplications()} />
              ) : null}
              {studioApplications ? (
                <>
                  <SummaryPills summary={studioApplications.summary} labels={{ pending: "待审核", approved: "已通过", rejected: "已驳回" }} />
                  {studioApplications.rows.length > 0 ? (
                    <div className="console-grid console-grid--detail console-studio-review-grid">
                      <div className="console-table-scroll console-studio-review-table">
                        <table className="console-table">
                          <thead>
                            <tr>
                              <th>申请单号</th>
                              <th>工作室</th>
                              <th>申请人</th>
                              <th>联系人</th>
                              <th>主体说明</th>
                              <th>状态</th>
                              <th>提交时间</th>
                            </tr>
                          </thead>
                          <tbody>
                            {studioApplications.rows.map((row) => (
                              <tr
                                key={row.applicationNo}
                                className={selectedStudioApplicationNo === row.applicationNo ? "is-active" : ""}
                                onClick={() => setSelectedStudioApplicationNo(row.applicationNo)}
                              >
                                <td>{row.applicationNo}</td>
                                <td><strong>{row.studioName}</strong></td>
                                <td>{row.applicantNickname}<p>ID {row.applicantUserId} · {row.applicantPhone}</p></td>
                                <td>{row.contactName}<p>{row.contactPhone}</p></td>
                                <td>{row.qualificationCode}</td>
                                <td>{row.statusText}</td>
                                <td>{row.createdAt}<p>{row.reviewedAt || "-"}</p></td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                      {selectedStudioApplication ? (
                        <div className="console-detail-panel console-studio-review-panel">
                          <h3>{selectedStudioApplication.studioName}</h3>
                          <dl className="console-detail-grid">
                            <div><dt>申请单号</dt><dd>{selectedStudioApplication.applicationNo}</dd></div>
                            <div><dt>审核状态</dt><dd>{selectedStudioApplication.statusText}</dd></div>
                            <div><dt>申请用户</dt><dd>{selectedStudioApplication.applicantNickname} / {selectedStudioApplication.applicantPhone}</dd></div>
                            <div><dt>平台用户ID</dt><dd>{selectedStudioApplication.applicantUserId}</dd></div>
                            <div><dt>联系人</dt><dd>{selectedStudioApplication.contactName}</dd></div>
                            <div><dt>联系电话</dt><dd>{selectedStudioApplication.contactPhone}</dd></div>
                            <div><dt>主体说明</dt><dd>{selectedStudioApplication.qualificationCode}</dd></div>
                            <div><dt>提交时间</dt><dd>{selectedStudioApplication.createdAt}</dd></div>
                          </dl>
                          {selectedStudioApplication.qualificationNote ? <p>{selectedStudioApplication.qualificationNote}</p> : null}
                          {selectedStudioApplication.qualificationMaterialUrl ? (
                            <a className="console-detail-link" href={selectedStudioApplication.qualificationMaterialUrl} rel="noreferrer" target="_blank">
                              查看营业执照 / 证明材料
                            </a>
                          ) : null}
                          {selectedStudioApplication.rejectReason ? (
                            <p className="console-review-reason">驳回原因：{selectedStudioApplication.rejectReason}</p>
                          ) : null}
                          {selectedStudioApplication.status === "PENDING" ? (
                            <div className="console-action-group">
                              <textarea
                                placeholder="驳回时填写原因，审核通过可留空"
                                rows={3}
                                value={studioApplicationReviewReason}
                                onChange={(event) => setStudioApplicationReviewReason(event.target.value)}
                              />
                              <div className="console-operation-actions">
                                <Button onClick={() => void handleStudioApplicationReview("APPROVE")}>审核通过并开通工作室</Button>
                                <Button kind="secondary" onClick={() => void handleStudioApplicationReview("REJECT")}>驳回申请</Button>
                              </div>
                            </div>
                          ) : null}
                        </div>
                      ) : (
                        <EmptyBlock title="请选择一条工作室申请查看详情" />
                      )}
                    </div>
                  ) : (
                    <EmptyBlock title="暂无工作室申请" />
                  )}
                </>
              ) : null}
            </SurfaceCard>

            <SurfaceCard eyebrow="工作室管理" title="合作策略与状态" actions={<Button kind="secondary" onClick={() => handleCreateStudioDraft()}>新增工作室</Button>}>
              <InlineKeywordFilters
                keyword={studioFilters.keyword}
                keywordLabel="工作室 / 负责人搜索"
                selectLabel="合作状态"
                selectValue={studioFilters.active}
                selectOptions={[["ALL", "全部"], ["ACTIVE", "合作中"], ["PAUSED", "暂停合作"], ["CLEARED", "已清退"]]}
                onKeywordChange={(keyword) => setStudioFilters((current) => ({ ...current, keyword }))}
                onSelectChange={(active) => setStudioFilters((current) => ({ ...current, active }))}
                onSearch={() => void refreshStudios()}
              />
              {studiosLoading ? <LoadingBlock title="正在加载工作室列表" compact /> : null}
              {studiosError ? <ErrorBlock title="工作室列表加载失败" message={studiosError} compact onRetry={() => void refreshStudios()} /> : null}
              {studios && studios.rows.length > 0 ? (
                <>
                  <SummaryPills summary={studios.summary} labels={{ activeCount: "合作中", directPublishCount: "免审直发", reviewRequiredCount: "需要审核", clearedCount: "已清退" }} />
                  <div className="console-studio-list">
                    {studios.rows.map((row) => (
                      <article
                        className={`console-studio-card ${selectedStudioId === row.studioId ? "is-active" : ""}`}
                        key={row.studioId}
                        onClick={() => handleOpenStudioModal(row.studioId)}
                        onKeyDown={(event) => {
                          if (event.key === "Enter" || event.key === " ") {
                            event.preventDefault();
                            handleOpenStudioModal(row.studioId);
                          }
                        }}
                        role="button"
                        tabIndex={0}
                      >
                        <header>
                          <div>
                            <h3>{row.studioName}</h3>
                            <p>负责人：{row.ownerNickname} · {row.ownerPhone}</p>
                          </div>
                          <Tag tone={row.active ? "success" : "warning"}>{row.activeText}</Tag>
                        </header>
                        <div className="console-studio-card__meta">
                          <Tag tone={row.reviewStrategy === "DIRECT_PUBLISH" ? "success" : "default"}>{row.reviewStrategyText}</Tag>
                          <Tag tone="default">分润 {row.shareRatio}</Tag>
                          <Tag tone="default">{row.contactName || "未填写联系人"}</Tag>
                        </div>
                        <dl className="console-studio-card__stats">
                          <div><dt>主体编号</dt><dd>{row.qualificationCode}</dd></div>
                          <div><dt>审核策略</dt><dd>{row.reviewStrategyText}</dd></div>
                          <div><dt>在售账号</dt><dd>{row.listingCount}</dd></div>
                          <div><dt>累计订单</dt><dd>{row.orderCount}</dd></div>
                          <div><dt>成交额</dt><dd>{row.gmv}</dd></div>
                        </dl>
                        <div className="console-studio-card__footer">
                          <span>{selectedStudioId === row.studioId && studioModalOpen ? "当前详情弹窗已打开" : "点击打开详情弹窗并调整合作策略"}</span>
                          <strong>{row.ownerPhone || "未填写负责人手机号"}</strong>
                        </div>
                      </article>
                    ))}
                  </div>
                </>
              ) : studios && !studiosLoading ? <EmptyBlock title="暂无工作室数据" /> : null}
            </SurfaceCard>

            {studioModalOpen ? (
              <div className="console-modal-backdrop" onClick={handleCloseStudioModal} role="presentation">
                <div className="console-modal console-modal--studio" onClick={(event) => event.stopPropagation()} role="dialog" aria-modal="true" aria-label="工作室详情弹窗">
                  <div className="console-modal__header">
                    <div>
                      <span className="console-modal__eyebrow">工作室详情</span>
                      <h3>{selectedStudioId ? "当前选中工作室" : "新工作室录入"}</h3>
                    </div>
                    <Button kind="ghost" onClick={handleCloseStudioModal}>关闭</Button>
                  </div>

                  <div className="console-modal__body">
              {studioDetailLoading ? <LoadingBlock title="正在加载工作室详情" compact /> : null}
              {!studioDetailLoading && studioDetail ? (
                <div className="console-card-list console-studio-detail-shell">
                  <div className="console-detail-panel">
                    <h3>{studioDetail.summary.studioName}</h3>
                    <p>{studioDetail.summary.description}</p>
                    <dl className="console-detail-grid">
                      <div><dt>负责人</dt><dd>{studioDetail.summary.ownerNickname}</dd></div>
                      <div><dt>负责人手机号</dt><dd>{studioDetail.summary.ownerPhone}</dd></div>
                      <div><dt>联系电话</dt><dd>{studioDetail.summary.contactPhone}</dd></div>
                      <div><dt>联系人</dt><dd>{studioDetail.summary.contactName}</dd></div>
                      <div><dt>联系微信</dt><dd>{studioDetail.summary.contactWechat}</dd></div>
                      <div><dt>主体编号</dt><dd>{studioDetail.summary.qualificationCode}</dd></div>
                      <div><dt>合作状态</dt><dd>{studioDetail.summary.activeText}</dd></div>
                      <div><dt>审核策略</dt><dd>{studioDetail.summary.reviewStrategyText}</dd></div>
                      <div><dt>分润比例</dt><dd>{studioDetail.summary.shareRatio}</dd></div>
                      <div><dt>创建时间</dt><dd>{studioDetail.summary.createdAt}</dd></div>
                    </dl>
                    {studioDetail.summary.qualificationMaterialUrl ? (
                      <a className="console-detail-link" href={studioDetail.summary.qualificationMaterialUrl} rel="noreferrer" target="_blank">
                        查看资质材料
                      </a>
                    ) : null}
                    <p>{studioDetail.summary.qualificationNote}</p>
                  </div>
                  <div className="console-studio-detail-toolbar">
                    <label className="console-studio-inline-field">
                      <span>分润比例</span>
                      <input
                        inputMode="decimal"
                        placeholder="例如 0.75"
                        value={studioShareRatioDrafts[studioDetail.summary.studioId] ?? ""}
                        onChange={(event) => setStudioShareRatioDrafts((current) => ({ ...current, [studioDetail.summary.studioId]: event.target.value }))}
                      />
                    </label>
                    <Button kind="secondary" onClick={() => void handleStudioPolicy(studioDetail.summary.studioId, studioForm.reviewStrategy === "DIRECT_PUBLISH" ? "REVIEW_REQUIRED" : "DIRECT_PUBLISH")}>
                      切换为{studioForm.reviewStrategy === "DIRECT_PUBLISH" ? "需审核" : "免审直发"}
                    </Button>
                    <Button kind="ghost" onClick={() => void handleStudioShareRatio(studioDetail.summary.studioId)}>
                      保存分润比例
                    </Button>
                    <Button kind={studioForm.active ? "ghost" : "primary"} onClick={() => void handleStudioStatus(studioDetail.summary.studioId, !studioForm.active)}>
                      {studioForm.active ? "暂停合作" : "恢复合作"}
                    </Button>
                  </div>
                  <div className="console-detail-split">
                    <SurfaceCard eyebrow="最近商品" title="最新账号发布">
                      {studioDetail.recentListings.length > 0 ? (
                        <ul className="console-list">
                          {studioDetail.recentListings.map((row) => (
                            <li className="console-list__item" key={row.listingNo}>
                              <div>
                                <strong>{row.title}</strong>
                                <p>{row.listingNo} · {row.statusText}</p>
                              </div>
                              <span>{row.price}</span>
                            </li>
                          ))}
                        </ul>
                      ) : <EmptyBlock title="暂无商品记录" />}
                    </SurfaceCard>
                    <SurfaceCard eyebrow="最近订单" title="卖家订单快照">
                      {studioDetail.recentOrders.length > 0 ? (
                        <ul className="console-list">
                          {studioDetail.recentOrders.map((row) => (
                            <li className="console-list__item" key={row.orderNo}>
                              <div>
                                <strong>{row.listingTitle}</strong>
                                <p>{row.orderNo} · {row.buyerNickname} · {row.statusText}</p>
                              </div>
                              <span>{row.totalAmount}</span>
                            </li>
                          ))}
                        </ul>
                      ) : <EmptyBlock title="暂无订单记录" />}
                    </SurfaceCard>
                  </div>
                </div>
              ) : null}
              {!studioDetailLoading && !studioDetail && studioForm.studioId ? <EmptyBlock title="请选择左侧工作室查看详情" /> : null}

              {!studioDetailLoading || !selectedStudioId ? (
              <div className="console-profile-form console-profile-form--studio">
                <h4>{studioForm.studioId ? "编辑工作室资料" : "录入新工作室"}</h4>
                <div className="console-studio-form-grid">
                  <label>
                    负责人手机号
                    <input
                      inputMode="numeric"
                      maxLength={11}
                      placeholder="请输入已注册的平台手机号"
                      value={studioForm.ownerPhone}
                      onChange={(event) => setStudioForm((current) => ({ ...current, ownerPhone: event.target.value }))}
                    />
                  </label>
                  <label>
                    工作室名称
                    <input
                      maxLength={100}
                      placeholder="请输入工作室展示名称"
                      value={studioForm.studioName}
                      onChange={(event) => setStudioForm((current) => ({ ...current, studioName: event.target.value }))}
                    />
                  </label>
                  <label>
                    联系电话
                    <input
                      inputMode="numeric"
                      maxLength={11}
                      placeholder="请输入工作室联系电话"
                      value={studioForm.contactPhone}
                      onChange={(event) => setStudioForm((current) => ({ ...current, contactPhone: event.target.value }))}
                    />
                  </label>
                  <label>
                    联系人
                    <input
                      maxLength={64}
                      placeholder="请输入工作室联系人姓名"
                      value={studioForm.contactName}
                      onChange={(event) => setStudioForm((current) => ({ ...current, contactName: event.target.value }))}
                    />
                  </label>
                  <label>
                    联系微信
                    <input
                      maxLength={64}
                      placeholder="请输入工作室联系微信"
                      value={studioForm.contactWechat}
                      onChange={(event) => setStudioForm((current) => ({ ...current, contactWechat: event.target.value }))}
                    />
                  </label>
                  <label>
                    统一社会信用代码 / 主体说明
                    <input
                      maxLength={64}
                      placeholder="请输入统一社会信用代码或主体说明"
                      value={studioForm.qualificationCode}
                      onChange={(event) => setStudioForm((current) => ({ ...current, qualificationCode: event.target.value }))}
                    />
                  </label>
                  <label>
                    审核策略
                    <select
                      value={studioForm.reviewStrategy}
                      onChange={(event) => setStudioForm((current) => ({ ...current, reviewStrategy: event.target.value }))}
                    >
                      <option value="REVIEW_REQUIRED">需要审核</option>
                      <option value="DIRECT_PUBLISH">免审直发</option>
                    </select>
                  </label>
                  <label>
                    合作档案状态
                    <select
                      value={studioForm.cooperationStatus}
                      onChange={(event) => setStudioForm((current) => ({
                        ...current,
                        cooperationStatus: event.target.value,
                        active: event.target.value === "ACTIVE",
                      }))}
                    >
                      <option value="ACTIVE">合作中</option>
                      <option value="PAUSED">暂停合作</option>
                      <option value="CLEARED">已清退</option>
                    </select>
                  </label>
                  <label>
                    分润比例
                    <input
                      inputMode="decimal"
                      placeholder="例如 0.75"
                      value={studioForm.shareRatio}
                      onChange={(event) => setStudioForm((current) => ({ ...current, shareRatio: event.target.value }))}
                    />
                  </label>
                </div>
                <label>
                  资质说明
                  <textarea
                    maxLength={255}
                    placeholder="备注营业执照、主体说明、签约状态等"
                    rows={3}
                    value={studioForm.qualificationNote}
                    onChange={(event) => setStudioForm((current) => ({ ...current, qualificationNote: event.target.value }))}
                  />
                </label>
                <div className="console-operation-editor__asset console-operation-editor__asset--studio">
                  <div>
                    <strong>资质材料</strong>
                    <small>{studioForm.qualificationMaterialKey || "尚未上传工作室资质材料"}</small>
                  </div>
                  {studioForm.qualificationMaterialUrl ? (
                    <a href={studioForm.qualificationMaterialUrl} rel="noreferrer" target="_blank">
                      <img alt="工作室资质材料预览" src={studioForm.qualificationMaterialUrl} />
                    </a>
                  ) : (
                    <div className="console-operation-editor__placeholder">资质材料预览</div>
                  )}
                  <div className="console-operation-editor__actions">
                    <input
                      accept="image/png,image/jpeg,image/webp"
                      id="studio-qualification-upload"
                      style={{ display: "none" }}
                      type="file"
                      onChange={(event) => {
                        const file = event.target.files?.[0];
                        event.currentTarget.value = "";
                        if (!file) return;
                        void handleUploadStudioQualification(file);
                      }}
                    />
                    <label className="dt-button dt-button--secondary publish-upload-trigger" htmlFor="studio-qualification-upload">
                      {studioAssetUploading ? "上传中" : "上传资质材料"}
                    </label>
                  </div>
                </div>
                <label>
                  工作室简介
                  <textarea
                    maxLength={500}
                    placeholder="介绍工作室定位、承接能力和售后承诺"
                    rows={4}
                    value={studioForm.description}
                    onChange={(event) => setStudioForm((current) => ({ ...current, description: event.target.value }))}
                  />
                </label>
                <label className="console-checkbox">
                  <input
                    checked={studioForm.active}
                    type="checkbox"
                    onChange={(event) => setStudioForm((current) => ({
                      ...current,
                      active: event.target.checked,
                      cooperationStatus: current.cooperationStatus === "CLEARED"
                        ? "CLEARED"
                        : (event.target.checked ? "ACTIVE" : "PAUSED"),
                    }))}
                  />
                  <span>合作中快捷开关（仅用于 ACTIVE / PAUSED）</span>
                </label>
                <div className="console-operation-actions">
                  <Button onClick={() => void handleSaveStudio()} disabled={studioSaving}>
                    {studioSaving ? "正在保存" : studioForm.studioId ? "保存工作室资料" : "创建工作室"}
                  </Button>
                  <Button kind="ghost" onClick={() => handleCreateStudioDraft()}>
                    重置表单
                  </Button>
                </div>
              </div>
              ) : null}
                  </div>
                </div>
              </div>
            ) : null}
          </section>
        ) : null}

        {activeTab === "boosting" ? (
          <SurfaceCard eyebrow="代肝服务" title="服务启停管理">
            <InlineFilters
              fields={[
                {
                  key: "status",
                  label: "服务状态",
                  value: boostingFilters.status,
                  options: [["ALL", "全部"], ["ACTIVE", "启用中"], ["DISABLED", "已停用"]],
                },
                {
                  key: "providerType",
                  label: "服务商",
                  value: boostingFilters.providerType,
                  options: [["ALL", "全部"], ["PLATFORM", "平台直营"], ["STUDIO", "工作室"]],
                },
              ]}
              onChange={(key, value) => setBoostingFilters((current) => ({ ...current, [key]: value }))}
              onSearch={() => void refreshBoosting()}
            />
            {boostingLoading ? <LoadingBlock title="正在加载代肝服务" compact /> : null}
            {boostingError ? <ErrorBlock title="代肝服务加载失败" message={boostingError} compact onRetry={() => void refreshBoosting()} /> : null}
            {boostingCenter && boostingCenter.rows.length > 0 ? (
              <>
                <SummaryPills summary={boostingCenter.summary} labels={{ activeCount: "启用中", disabledCount: "已停用" }} />
                <div className="console-table-scroll">
                  <table className="console-table">
                    <thead>
                      <tr>
                        <th>服务名称</th>
                        <th>分类</th>
                        <th>价格 / 周期</th>
                        <th>服务商</th>
                        <th>销量</th>
                        <th>状态</th>
                        <th>操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      {boostingCenter.rows.map((row) => (
                        <tr key={row.serviceNo}>
                          <td><strong>{row.name}</strong><p>{row.description}</p></td>
                          <td>{row.categoryLabel}</td>
                          <td>{row.price}<p>{row.cycleLabel}</p></td>
                          <td>{row.providerName}<p>{row.providerTypeText}</p></td>
                          <td>{row.salesCount}</td>
                          <td>{row.statusText}</td>
                          <td>
                            <Button kind={row.status === "ACTIVE" ? "ghost" : "primary"} onClick={() => void handleBoostingStatus(row.serviceNo, row.status === "ACTIVE" ? "DISABLED" : "ACTIVE")}>
                              {row.status === "ACTIVE" ? "停用" : "启用"}
                            </Button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </>
            ) : boostingCenter && !boostingLoading ? <EmptyBlock title="暂无代肝服务" /> : null}
          </SurfaceCard>
        ) : null}

        {activeTab === "withdraw" ? (
          <SurfaceCard eyebrow="提现审核" title="财务打款处理">
            <InlineFilters
              fields={[
                {
                  key: "status",
                  label: "审核状态",
                  value: withdrawFilters.status,
                  options: [["ALL", "全部"], ["PENDING", "待审核"], ["PAID", "已到账"], ["REJECTED", "已驳回"]],
                },
              ]}
              onChange={(key, value) => setWithdrawFilters((current) => ({ ...current, [key]: value }))}
              onSearch={() => {
                void refreshWithdraws();
                void refreshStudioWithdraws();
              }}
            />
            {withdrawLoading ? <LoadingBlock title="正在加载提现申请" compact /> : null}
            {withdrawError ? <ErrorBlock title="提现申请加载失败" message={withdrawError} compact onRetry={() => void refreshWithdraws()} /> : null}
            {withdrawCenter && withdrawCenter.rows.length > 0 ? (
              <>
                <SummaryPills summary={withdrawCenter.summary} labels={{ pending: "待审核", paid: "已到账", rejected: "已驳回" }} />
                <div className="console-table-scroll">
                  <table className="console-table">
                    <thead>
                      <tr>
                        <th>申请单号</th>
                        <th>用户</th>
                        <th>渠道</th>
                        <th>金额</th>
                        <th>状态</th>
                        <th>账号</th>
                        <th>时间</th>
                        <th>操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      {withdrawCenter.rows.map((row) => (
                        <tr className={selectedWithdrawApplicationNo === row.applicationNo ? "is-active" : ""} key={row.applicationNo} onClick={() => setSelectedWithdrawApplicationNo(row.applicationNo)}>
                          <td>{row.applicationNo}</td>
                          <td>{row.nickname}<p>{row.realName}</p></td>
                          <td>{row.channelText}</td>
                          <td>{row.amount}</td>
                          <td>{row.statusText}<p>{row.rejectReason || row.paidAt}</p></td>
                          <td>{row.accountNo}</td>
                          <td>{row.createdAt}</td>
                          <td className="console-table__actions">
                            {row.status === "PENDING" ? (
                              <Button kind="secondary" onClick={(event) => {
                                event.stopPropagation();
                                setSelectedWithdrawApplicationNo(row.applicationNo);
                                setWithdrawReviewReason("");
                                setWithdrawReviewDialog("USER");
                              }}>查看处理</Button>
                            ) : (
                              <Tag tone={row.status === "PAID" ? "success" : "warning"}>{row.statusText}</Tag>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </>
            ) : withdrawCenter && !withdrawLoading ? <EmptyBlock title="暂无提现申请" /> : null}

            <SectionHeading title="工作室提现" description="审核工作室分润提现申请，形成工作室财务闭环。" />
            {studioWithdrawLoading ? <LoadingBlock title="正在加载工作室提现申请" compact /> : null}
            {studioWithdrawError ? <ErrorBlock title="工作室提现申请加载失败" message={studioWithdrawError} compact onRetry={() => void refreshStudioWithdraws()} /> : null}
            {studioWithdrawCenter && studioWithdrawCenter.rows.length > 0 ? (
              <>
                <SummaryPills summary={studioWithdrawCenter.summary} labels={{ pending: "待审核", paid: "已到账", rejected: "已驳回" }} />
                <div className="console-table-scroll">
                  <table className="console-table">
                    <thead>
                      <tr>
                        <th>申请单号</th>
                        <th>工作室</th>
                        <th>渠道</th>
                        <th>金额</th>
                        <th>状态</th>
                        <th>账号</th>
                        <th>时间</th>
                        <th>操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      {studioWithdrawCenter.rows.map((row) => (
                        <tr className={selectedStudioWithdrawApplicationNo === row.applicationNo ? "is-active" : ""} key={row.applicationNo} onClick={() => setSelectedStudioWithdrawApplicationNo(row.applicationNo)}>
                          <td>{row.applicationNo}</td>
                          <td>{row.studioName}<p>{row.ownerNickname}</p></td>
                          <td>{row.channelText}<p>{row.accountName}</p></td>
                          <td>{row.amount}</td>
                          <td>{row.statusText}<p>{row.rejectReason || row.paidAt}</p></td>
                          <td>{row.accountNo}</td>
                          <td>{row.createdAt}</td>
                          <td className="console-table__actions">
                            {row.status === "PENDING" ? (
                              <Button kind="secondary" onClick={(event) => {
                                event.stopPropagation();
                                setSelectedStudioWithdrawApplicationNo(row.applicationNo);
                                setStudioWithdrawReviewReason("");
                                setWithdrawReviewDialog("STUDIO");
                              }}>查看处理</Button>
                            ) : (
                              <Tag tone={row.status === "PAID" ? "success" : "warning"}>{row.statusText}</Tag>
                            )}
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
              </>
            ) : studioWithdrawCenter && !studioWithdrawLoading ? <EmptyBlock title="暂无工作室提现申请" /> : null}

            {withdrawReviewDialog === "USER" && selectedWithdraw ? (
              <div className="console-modal-backdrop" onClick={() => setWithdrawReviewDialog(null)} role="presentation">
                <div className="console-modal console-modal--withdraw" onClick={(event) => event.stopPropagation()} role="dialog" aria-modal="true" aria-label="用户提现处理">
                  <div className="console-modal__header console-modal__header--accent">
                    <div>
                      <span className="console-modal__eyebrow">提现处理</span>
                      <h3>用户钱包提现审核</h3>
                      <p>{selectedWithdraw.applicationNo}</p>
                    </div>
                    <Button kind="ghost" onClick={() => setWithdrawReviewDialog(null)}>关闭</Button>
                  </div>
                  <div className="console-modal__body">
                    <WithdrawReviewDialogContent
                      amount={selectedWithdraw.amount}
                      applicant={`${selectedWithdraw.nickname} / ${selectedWithdraw.realName}`}
                      applicationNo={selectedWithdraw.applicationNo}
                      channel={selectedWithdraw.channelText}
                      accountName={selectedWithdraw.realName}
                      accountNo={selectedWithdraw.accountNo}
                      qrCodeUrl={selectedWithdraw.qrCodeUrl}
                      createdAt={selectedWithdraw.createdAt}
                      paidAt={selectedWithdraw.paidAt}
                      rejectReason={selectedWithdraw.rejectReason}
                      statusText={selectedWithdraw.statusText}
                      reason={withdrawReviewReason}
                      reasonPlaceholder="驳回时必填，例如账户信息不一致"
                      onReasonChange={setWithdrawReviewReason}
                      onApprove={() => void handleWithdrawReview("APPROVE", selectedWithdraw.applicationNo)}
                      onReject={() => void handleWithdrawReview("REJECT", selectedWithdraw.applicationNo)}
                    />
                  </div>
                </div>
              </div>
            ) : null}

            {withdrawReviewDialog === "STUDIO" && selectedStudioWithdraw ? (
              <div className="console-modal-backdrop" onClick={() => setWithdrawReviewDialog(null)} role="presentation">
                <div className="console-modal console-modal--withdraw" onClick={(event) => event.stopPropagation()} role="dialog" aria-modal="true" aria-label="工作室提现处理">
                  <div className="console-modal__header console-modal__header--accent">
                    <div>
                      <span className="console-modal__eyebrow">提现处理</span>
                      <h3>工作室提现审核</h3>
                      <p>{selectedStudioWithdraw.applicationNo}</p>
                    </div>
                    <Button kind="ghost" onClick={() => setWithdrawReviewDialog(null)}>关闭</Button>
                  </div>
                  <div className="console-modal__body">
                    <WithdrawReviewDialogContent
                      amount={selectedStudioWithdraw.amount}
                      applicant={`${selectedStudioWithdraw.studioName} / ${selectedStudioWithdraw.ownerNickname}`}
                      applicationNo={selectedStudioWithdraw.applicationNo}
                      channel={selectedStudioWithdraw.channelText}
                      accountName={selectedStudioWithdraw.accountName}
                      accountNo={selectedStudioWithdraw.accountNo}
                      qrCodeUrl={null}
                      createdAt={selectedStudioWithdraw.createdAt}
                      paidAt={selectedStudioWithdraw.paidAt}
                      rejectReason={selectedStudioWithdraw.rejectReason}
                      statusText={selectedStudioWithdraw.statusText}
                      reason={studioWithdrawReviewReason}
                      reasonPlaceholder="驳回时必填，例如工作室收款账号与实名不一致"
                      onReasonChange={setStudioWithdrawReviewReason}
                      onApprove={() => void handleStudioWithdrawReview("APPROVE", selectedStudioWithdraw.applicationNo)}
                      onReject={() => void handleStudioWithdrawReview("REJECT", selectedStudioWithdraw.applicationNo)}
                    />
                  </div>
                </div>
              </div>
            ) : null}
          </SurfaceCard>
        ) : null}

        {activeTab === "operation" ? (
          <section className="console-operation-page">
            <div className="console-operation-page__hero">
              <div className="console-operation-page__copy">
                <p className="console-operation-page__eyebrow">运营配置</p>
                <h3>所有运营模块直接平铺在同一页处理</h3>
                <p>不再做二级切换，首页资源位、公告和系统参数都在当前页直接维护。素材类模块保持左侧列表、右侧编辑，系统参数放在页面底部统一配置。</p>
              </div>
              <div className="console-operation-page__actions">
                <Button kind="secondary" onClick={() => void refreshOperations()} disabled={operationLoading}>
                  {operationLoading ? "刷新中" : "刷新数据"}
                </Button>
                <Button onClick={() => void handleCreateBanner()}>新增轮播图</Button>
                <Button onClick={() => void handleCreateShortcut()}>新增快捷入口</Button>
                <Button onClick={() => void handleCreateAnnouncement()}>新增公告</Button>
              </div>
            </div>

            {operationError ? <ErrorBlock title="运营配置加载失败" message={operationError} onRetry={() => void refreshOperations()} /> : null}
            {operationLoading && !operationCenter ? <LoadingBlock title="正在加载运营配置" /> : null}

            {operationCenter ? (
              <>
                <div className="console-operation-page__overview">
                  {operationOverviewCards.map((item) => (
                    <article className="console-operation-page__overview-card" key={item.title}>
                      <span>{item.title}</span>
                      <strong>{item.value}</strong>
                      <small>{item.detail}</small>
                    </article>
                  ))}
                </div>

                <SurfaceCard eyebrow="轮播图管理" title="首页轮播配置" actions={<Button onClick={() => void handleCreateBanner()}>新增轮播图</Button>}>
                <div className="console-grid console-grid--detail console-operation-workspace">
                  {operationCenter.banners.length > 0 ? (
                    <div className="console-operation-column console-operation-column--list">
                      <div className="console-operation-column__head">
                        <div>
                          <p>轮播列表</p>
                          <h4>选择要维护的首页轮播</h4>
                        </div>
                        <span>{operationCenter.banners.length} 条资源</span>
                      </div>
                      <div className="console-card-list">
                        <div className="console-list-toolbar">
                          <label className="console-list-toolbar__checkbox">
                            <input
                              checked={selectedBannerIds.length > 0 && selectedBannerIds.length === operationCenter.banners.length}
                              onChange={() => toggleSelectAll("banner", operationCenter.banners.map((row) => row.bannerId))}
                              type="checkbox"
                            />
                          </label>
                          <div className="console-list-toolbar__actions">
                            <Button kind="secondary" onClick={() => void handleBatchOperationStatus("banner", "ACTIVE")} disabled={operationBatchLoading === "banner"}>
                              {operationBatchLoading === "banner" ? "处理中" : "批量启用"}
                            </Button>
                            <Button kind="secondary" onClick={() => void handleBatchOperationStatus("banner", "DISABLED")} disabled={operationBatchLoading === "banner"}>
                              {operationBatchLoading === "banner" ? "处理中" : "批量停用"}
                            </Button>
                          </div>
                        </div>
                        {operationCenter.banners.map((row) => (
                          <article
                            className={`console-card-list__item ${selectedBannerId === row.bannerId ? "is-active" : ""}`}
                            key={row.bannerNo}
                            onClick={() => setSelectedBannerId(row.bannerId)}
                            role="button"
                            tabIndex={0}
                            onKeyDown={(event) => {
                              if (event.key === "Enter" || event.key === " ") {
                                event.preventDefault();
                                setSelectedBannerId(row.bannerId);
                              }
                            }}
                          >
                            <label className="console-card-list__check" onClick={(event) => event.stopPropagation()}>
                              <input
                                checked={selectedBannerIds.includes(row.bannerId)}
                                onChange={() => toggleSelectedId("banner", row.bannerId)}
                                type="checkbox"
                              />
                            </label>
                            <div>
                              <strong>{row.title}</strong>
                              <p>{row.linkUrl} · 排序 {row.sortNo}</p>
                            </div>
                            <Tag tone={row.status === "ACTIVE" ? "success" : "warning"}>{row.statusText}</Tag>
                          </article>
                        ))}
                      </div>
                    </div>
                  ) : <EmptyBlock title="暂无轮播图配置" />}
                  <div className="console-operation-column console-operation-column--editor">
                    <div className="console-operation-column__head">
                      <div>
                        <p>轮播编辑</p>
                        <h4>{selectedBanner ? selectedBanner.title : "新建轮播图"}</h4>
                      </div>
                      <span>{selectedBanner ? `${selectedBanner.statusText} · 排序 ${selectedBanner.sortNo}` : "填写素材与跳转信息"}</span>
                    </div>
                    <div className="console-operation-editor">
                      <label>
                        轮播标题
                        <input
                          value={bannerForm.title}
                          onChange={(event) => setBannerForm((current) => ({ ...current, title: event.target.value }))}
                          placeholder="例如：五一活动专区"
                        />
                      </label>
                      <label>
                        跳转链接
                        <input
                          value={bannerForm.linkUrl}
                          onChange={(event) => setBannerForm((current) => ({ ...current, linkUrl: event.target.value }))}
                          placeholder="https:// 或站内路径"
                        />
                      </label>
                      <div className="console-operation-editor__grid">
                        <label>
                          排序
                          <input
                            value={bannerForm.sortNo}
                            onChange={(event) => setBannerForm((current) => ({ ...current, sortNo: event.target.value }))}
                            inputMode="numeric"
                          />
                        </label>
                        <label>
                          状态
                          <select value={bannerForm.status} onChange={(event) => setBannerForm((current) => ({ ...current, status: event.target.value }))}>
                            <option value="ACTIVE">启用</option>
                            <option value="DISABLED">停用</option>
                          </select>
                        </label>
                      </div>
                      <div className="console-operation-editor__asset">
                        {bannerForm.imageUrl ? <img alt="轮播图预览" src={bannerForm.imageUrl} /> : <div className="console-operation-editor__placeholder">轮播图预览</div>}
                        <div className="console-operation-editor__asset-meta">
                          <small>{bannerForm.imageKey || "尚未上传轮播图素材"}</small>
                          <input
                            id="admin-banner-upload"
                            type="file"
                            accept="image/*"
                            hidden
                            onChange={(event) => {
                              const file = event.target.files?.[0];
                              event.currentTarget.value = "";
                              if (file) {
                                void handleUploadOperationAsset("banner", file);
                              }
                            }}
                          />
                          <label className="dt-button dt-button--secondary publish-upload-trigger" htmlFor="admin-banner-upload">
                            {operationUploading === "banner" ? "上传中" : "上传轮播图"}
                          </label>
                        </div>
                      </div>
                      <div className="console-operation-editor__actions">
                        <Button onClick={() => void handleSaveBannerEditor()} disabled={operationSaving === "banner"}>
                          {operationSaving === "banner" ? "正在保存" : "保存轮播图"}
                        </Button>
                        <Button kind="ghost" onClick={() => void handleDeleteOperation("banner")} disabled={operationDeleting === "banner" || !selectedBannerId}>
                          {operationDeleting === "banner" ? "正在删除" : "删除当前"}
                        </Button>
                        <Button kind="secondary" onClick={() => void handleCreateBanner()}>
                          清空当前表单
                        </Button>
                      </div>
                    </div>
                  </div>
                </div>
                </SurfaceCard>

                <SurfaceCard eyebrow="金刚区配置" title="快捷入口管理" actions={<Button onClick={() => void handleCreateShortcut()}>新增快捷入口</Button>}>
                <div className="console-grid console-grid--detail console-operation-workspace">
                  {operationCenter.shortcuts.length > 0 ? (
                    <div className="console-operation-column console-operation-column--list">
                      <div className="console-operation-column__head">
                        <div>
                          <p>快捷入口列表</p>
                          <h4>维护首页金刚区入口</h4>
                        </div>
                        <span>{operationCenter.shortcuts.length} 个入口</span>
                      </div>
                      <div className="console-card-list">
                        <div className="console-list-toolbar">
                          <label className="console-list-toolbar__checkbox">
                            <input
                              checked={selectedShortcutIds.length > 0 && selectedShortcutIds.length === operationCenter.shortcuts.length}
                              onChange={() => toggleSelectAll("shortcut", operationCenter.shortcuts.map((row) => row.shortcutId))}
                              type="checkbox"
                            />
                          </label>
                          <div className="console-list-toolbar__actions">
                            <Button kind="secondary" onClick={() => void handleBatchOperationStatus("shortcut", "ACTIVE")} disabled={operationBatchLoading === "shortcut"}>
                              {operationBatchLoading === "shortcut" ? "处理中" : "批量启用"}
                            </Button>
                            <Button kind="secondary" onClick={() => void handleBatchOperationStatus("shortcut", "DISABLED")} disabled={operationBatchLoading === "shortcut"}>
                              {operationBatchLoading === "shortcut" ? "处理中" : "批量停用"}
                            </Button>
                          </div>
                        </div>
                        {operationCenter.shortcuts.map((row) => (
                          <article
                            className={`console-card-list__item ${selectedShortcutId === row.shortcutId ? "is-active" : ""}`}
                            key={row.shortcutNo}
                            onClick={() => setSelectedShortcutId(row.shortcutId)}
                            role="button"
                            tabIndex={0}
                            onKeyDown={(event) => {
                              if (event.key === "Enter" || event.key === " ") {
                                event.preventDefault();
                                setSelectedShortcutId(row.shortcutId);
                              }
                            }}
                          >
                            <label className="console-card-list__check" onClick={(event) => event.stopPropagation()}>
                              <input
                                checked={selectedShortcutIds.includes(row.shortcutId)}
                                onChange={() => toggleSelectedId("shortcut", row.shortcutId)}
                                type="checkbox"
                              />
                            </label>
                            <div>
                              <strong>{row.name}</strong>
                              <p>{row.linkUrl} · 排序 {row.sortNo}</p>
                            </div>
                            <Tag tone={row.status === "ACTIVE" ? "success" : "warning"}>{row.statusText}</Tag>
                          </article>
                        ))}
                      </div>
                    </div>
                  ) : <EmptyBlock title="暂无快捷入口" />}
                  <div className="console-operation-column console-operation-column--editor">
                    <div className="console-operation-column__head">
                      <div>
                        <p>入口编辑</p>
                        <h4>{selectedShortcut ? selectedShortcut.name : "新建快捷入口"}</h4>
                      </div>
                      <span>{selectedShortcut ? `${selectedShortcut.statusText} · 排序 ${selectedShortcut.sortNo}` : "设置名称、跳转与图标"}</span>
                    </div>
                    <div className="console-operation-editor">
                      <label>
                        入口名称
                        <input
                          value={shortcutForm.name}
                          onChange={(event) => setShortcutForm((current) => ({ ...current, name: event.target.value }))}
                          placeholder="例如：热门账号"
                        />
                      </label>
                      <label>
                        跳转链接
                        <input
                          value={shortcutForm.linkUrl}
                          onChange={(event) => setShortcutForm((current) => ({ ...current, linkUrl: event.target.value }))}
                          placeholder="/listing 或完整链接"
                        />
                      </label>
                      <div className="console-operation-editor__grid">
                        <label>
                          排序
                          <input
                            value={shortcutForm.sortNo}
                            onChange={(event) => setShortcutForm((current) => ({ ...current, sortNo: event.target.value }))}
                            inputMode="numeric"
                          />
                        </label>
                        <label>
                          状态
                          <select value={shortcutForm.status} onChange={(event) => setShortcutForm((current) => ({ ...current, status: event.target.value }))}>
                            <option value="ACTIVE">启用</option>
                            <option value="DISABLED">停用</option>
                          </select>
                        </label>
                      </div>
                      <div className="console-operation-editor__asset">
                        {shortcutForm.iconUrl ? <img alt="快捷入口图标预览" src={shortcutForm.iconUrl} /> : <div className="console-operation-editor__placeholder">快捷图标预览</div>}
                        <div className="console-operation-editor__asset-meta">
                          <small>{shortcutForm.iconKey || "尚未上传图标素材"}</small>
                          <input
                            id="admin-shortcut-upload"
                            type="file"
                            accept="image/*"
                            hidden
                            onChange={(event) => {
                              const file = event.target.files?.[0];
                              event.currentTarget.value = "";
                              if (file) {
                                void handleUploadOperationAsset("shortcut", file);
                              }
                            }}
                          />
                          <label className="dt-button dt-button--secondary publish-upload-trigger" htmlFor="admin-shortcut-upload">
                            {operationUploading === "shortcut" ? "上传中" : "上传图标"}
                          </label>
                        </div>
                      </div>
                      <div className="console-operation-editor__actions">
                        <Button onClick={() => void handleSaveShortcutEditor()} disabled={operationSaving === "shortcut"}>
                          {operationSaving === "shortcut" ? "正在保存" : "保存快捷入口"}
                        </Button>
                        <Button kind="ghost" onClick={() => void handleDeleteOperation("shortcut")} disabled={operationDeleting === "shortcut" || !selectedShortcutId}>
                          {operationDeleting === "shortcut" ? "正在删除" : "删除当前"}
                        </Button>
                        <Button kind="secondary" onClick={() => void handleCreateShortcut()}>
                          清空当前表单
                        </Button>
                      </div>
                    </div>
                  </div>
                </div>
                </SurfaceCard>

                <SurfaceCard eyebrow="公告系统" title="公告发布与置顶" actions={<Button onClick={() => void handleCreateAnnouncement()}>新增公告</Button>}>
                <div className="console-grid console-grid--detail console-operation-workspace">
                  {operationCenter.announcements.length > 0 ? (
                    <div className="console-operation-column console-operation-column--list">
                      <div className="console-operation-column__head">
                        <div>
                          <p>公告列表</p>
                          <h4>集中维护发布状态与置顶</h4>
                        </div>
                        <span>{operationCenter.announcements.length} 条公告</span>
                      </div>
                      <div className="console-card-list">
                        <div className="console-list-toolbar">
                          <label className="console-list-toolbar__checkbox">
                            <input
                              checked={selectedAnnouncementIds.length > 0 && selectedAnnouncementIds.length === operationCenter.announcements.length}
                              onChange={() => toggleSelectAll("announcement", operationCenter.announcements.map((row) => row.announcementId))}
                              type="checkbox"
                            />
                          </label>
                          <div className="console-list-toolbar__actions">
                            <Button kind="secondary" onClick={() => void handleBatchOperationStatus("announcement", "PUBLISHED")} disabled={operationBatchLoading === "announcement"}>
                              {operationBatchLoading === "announcement" ? "处理中" : "批量发布"}
                            </Button>
                            <Button kind="secondary" onClick={() => void handleBatchOperationStatus("announcement", "OFFLINE")} disabled={operationBatchLoading === "announcement"}>
                              {operationBatchLoading === "announcement" ? "处理中" : "批量下架"}
                            </Button>
                          </div>
                        </div>
                        {operationCenter.announcements.map((row) => (
                          <article
                            className={`console-card-list__item ${selectedAnnouncementId === row.announcementId ? "is-active" : ""}`}
                            key={row.announcementNo}
                            onClick={() => setSelectedAnnouncementId(row.announcementId)}
                            role="button"
                            tabIndex={0}
                            onKeyDown={(event) => {
                              if (event.key === "Enter" || event.key === " ") {
                                event.preventDefault();
                                setSelectedAnnouncementId(row.announcementId);
                              }
                            }}
                          >
                            <label className="console-card-list__check" onClick={(event) => event.stopPropagation()}>
                              <input
                                checked={selectedAnnouncementIds.includes(row.announcementId)}
                                onChange={() => toggleSelectedId("announcement", row.announcementId)}
                                type="checkbox"
                              />
                            </label>
                            <div>
                              <strong>{row.title}</strong>
                              <p>{row.categoryText} · {row.publishAt}</p>
                              <small>{row.content}</small>
                            </div>
                            <div className="console-role-tags">
                              {row.pinned ? <Tag tone="accent">置顶</Tag> : null}
                              <Tag tone={row.status === "PUBLISHED" ? "success" : "warning"}>{row.statusText}</Tag>
                            </div>
                          </article>
                        ))}
                      </div>
                    </div>
                  ) : <EmptyBlock title="暂无公告" />}
                  <div className="console-operation-column console-operation-column--editor">
                    <div className="console-operation-column__head">
                      <div>
                        <p>公告编辑</p>
                        <h4>{selectedAnnouncement ? selectedAnnouncement.title : "新建公告"}</h4>
                      </div>
                      <span>
                        {selectedAnnouncement
                          ? `${selectedAnnouncement.statusText} · ${selectedAnnouncement.categoryText}${selectedAnnouncement.pinned ? " · 已置顶" : ""}`
                          : "设置标题、分类和正文"}
                      </span>
                    </div>
                    <div className="console-operation-editor">
                      <label>
                        公告标题
                        <input
                          value={announcementForm.title}
                          onChange={(event) => setAnnouncementForm((current) => ({ ...current, title: event.target.value }))}
                          placeholder="请输入公告标题"
                        />
                      </label>
                      <label>
                        公告分类
                        <select value={announcementForm.category} onChange={(event) => setAnnouncementForm((current) => ({ ...current, category: event.target.value }))}>
                          <option value="SYSTEM">系统公告</option>
                          <option value="ACTIVITY">活动公告</option>
                          <option value="TRADE">交易通知</option>
                        </select>
                      </label>
                      <label>
                        公告内容
                        <textarea
                          rows={5}
                          value={announcementForm.content}
                          onChange={(event) => setAnnouncementForm((current) => ({ ...current, content: event.target.value }))}
                          placeholder="请输入公告正文"
                        />
                      </label>
                      <div className="console-operation-editor__grid">
                        <label>
                          发布状态
                          <select value={announcementForm.status} onChange={(event) => setAnnouncementForm((current) => ({ ...current, status: event.target.value }))}>
                            <option value="PUBLISHED">已发布</option>
                            <option value="DRAFT">草稿</option>
                            <option value="OFFLINE">已下架</option>
                          </select>
                        </label>
                        <label className="console-operation-editor__checkbox">
                          <input
                            checked={announcementForm.pinned}
                            onChange={(event) => setAnnouncementForm((current) => ({ ...current, pinned: event.target.checked }))}
                            type="checkbox"
                          />
                          置顶公告
                        </label>
                      </div>
                      <div className="console-operation-editor__actions">
                        <Button onClick={() => void handleSaveAnnouncementEditor()} disabled={operationSaving === "announcement"}>
                          {operationSaving === "announcement" ? "正在保存" : "保存公告"}
                        </Button>
                        <Button kind="ghost" onClick={() => void handleDeleteOperation("announcement")} disabled={operationDeleting === "announcement" || !selectedAnnouncementId}>
                          {operationDeleting === "announcement" ? "正在删除" : "删除当前"}
                        </Button>
                        <Button kind="secondary" onClick={() => void handleCreateAnnouncement()}>
                          清空当前表单
                        </Button>
                      </div>
                    </div>
                  </div>
                </div>
                </SurfaceCard>

                <AdminIntegrationConfigSection />
              </>
            ) : null}
          </section>
        ) : null}

        {activeTab === "gunCode" ? (
          <section className="console-gun-code-shell">
            <AdminGunCodeUploadSection />
          </section>
        ) : null}

        {activeTab === "support" ? <AdminSupportWorkbench /> : null}

        {activeTab === "user" ? (
          <div className="console-grid console-grid--detail">
          <SurfaceCard eyebrow="用户管理" title="用户状态与身份">
            <InlineKeywordFilters
              keyword={userFilters.keyword}
              keywordLabel="昵称 / 手机号搜索"
              selectLabel="账号状态"
              selectValue={userFilters.status}
              selectOptions={[["ALL", "全部"], ["ACTIVE", "正常"], ["DISABLED", "已封禁"]]}
              onKeywordChange={(keyword) => setUserFilters((current) => ({ ...current, keyword }))}
              onSelectChange={(status) => setUserFilters((current) => ({ ...current, status }))}
              onSearch={() => void refreshUsers()}
            />
            <InlineFilters
              fields={[
                { key: "verified", label: "实名状态", value: userFilters.verified, options: [["ALL", "全部"], ["VERIFIED", "已实名"], ["UNVERIFIED", "未实名"]] },
                { key: "studioOwner", label: "工作室身份", value: userFilters.studioOwner, options: [["ALL", "全部"], ["YES", "工作室管理员"], ["NO", "普通用户"]] },
              ]}
              onChange={(key, value) => setUserFilters((current) => ({ ...current, [key]: value }))}
              onSearch={() => void refreshUsers()}
            />
            {userLoading ? <LoadingBlock title="正在加载用户列表" compact /> : null}
            {userError ? <ErrorBlock title="用户列表加载失败" message={userError} compact onRetry={() => void refreshUsers()} /> : null}
            {userCenter ? (
              <>
                <SummaryPills summary={userCenter.summary} labels={{ activeCount: "正常账号", disabledCount: "已封禁", verifiedCount: "已实名", studioOwnerCount: "工作室管理员" }} />
                {userCenter.rows.length > 0 ? (
                  <div className="console-table-scroll">
                    <table className="console-table">
                      <thead>
                        <tr>
                          <th>用户</th>
                          <th>账号状态</th>
                          <th>实名</th>
                          <th>身份</th>
                          <th>时间</th>
                          <th>操作</th>
                        </tr>
                      </thead>
                      <tbody>
                        {userCenter.rows.map((row) => (
                          <tr className={selectedUserId === row.userId ? "is-active" : ""} key={row.userId} onClick={() => setSelectedUserId(row.userId)}>
                            <td><strong>{row.nickname}</strong><p>{row.phone}</p></td>
                            <td>{row.accountStatusText}{row.banReason ? <p>{row.banReason}</p> : null}</td>
                            <td>{row.verifiedText}<p>{row.realNameStatusText}</p></td>
                            <td>{row.studioOwnerText}</td>
                            <td>{row.createdAt}<p>{row.updatedAt}</p></td>
                            <td className="console-table__actions">
                              <Button kind="secondary" onClick={() => setSelectedUserId(row.userId)}>查看详情</Button>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : <EmptyBlock title="暂无用户数据" />}
              </>
            ) : null}
          </SurfaceCard>
          <SurfaceCard eyebrow="用户详情" title="当前选中用户">
            {selectedUser ? (
              <div className="console-user-panel">
                <div className="console-user-panel__summary">
                  <strong>{selectedUser.nickname}</strong>
                  <p>用户ID：{selectedUser.userId} · {selectedUser.phone}</p>
                  <div className="console-user-panel__tags">
                    <Tag tone={selectedUser.accountStatus === "ACTIVE" ? "success" : "warning"}>{selectedUser.accountStatusText}</Tag>
                    <Tag tone={selectedUser.verified ? "accent" : "default"}>{selectedUser.verifiedText}</Tag>
                    <Tag tone={selectedUser.isStudioOwner ? "success" : "default"}>{selectedUser.studioOwnerText}</Tag>
                  </div>
                </div>
                {userDetailLoading ? <LoadingBlock title="正在加载用户详情" compact /> : null}
                {userDetail ? (
                  <>
                    <div className="console-user-detail">
                      <div className="console-user-detail__avatar">
                        {userDetail.summary.avatarUrl ? <img alt={userDetail.summary.nickname} src={userDetail.summary.avatarUrl} /> : <div>头像</div>}
                      </div>
                      <dl className="console-user-detail__facts">
                        <div><dt>实名姓名</dt><dd>{userDetail.summary.realName}</dd></div>
                        <div><dt>身份证号</dt><dd>{userDetail.summary.idCardNo}</dd></div>
                        <div><dt>实名状态</dt><dd>{userDetail.summary.realNameStatusText}</dd></div>
                        <div><dt>注册时间</dt><dd>{userDetail.summary.createdAt}</dd></div>
                        <div><dt>异地提醒</dt><dd>{userDetail.summary.loginAlertEnabled ? "已开启" : "未开启"}</dd></div>
                        <div><dt>二次验证</dt><dd>{userDetail.summary.secondaryVerifyEnabled ? "已开启" : "未开启"}</dd></div>
                      </dl>
                    </div>
                    <div className="console-user-kpis">
                      <div className="console-user-kpis__item"><span>可用余额</span><strong>{userDetail.wallet.availableBalance}</strong></div>
                      <div className="console-user-kpis__item"><span>冻结金额</span><strong>{userDetail.wallet.frozenBalance}</strong></div>
                      <div className="console-user-kpis__item"><span>累计佣金</span><strong>{userDetail.wallet.totalCommission}</strong></div>
                      <div className="console-user-kpis__item"><span>买家订单</span><strong>{userDetail.stats.buyerOrderCount}</strong></div>
                      <div className="console-user-kpis__item"><span>卖家订单</span><strong>{userDetail.stats.sellerOrderCount}</strong></div>
                      <div className="console-user-kpis__item"><span>发布账号</span><strong>{userDetail.stats.publishCount}</strong></div>
                    </div>
                    <div className="console-user-detail__blocks">
                      <div className="console-user-detail__block">
                        <h4>工作室归属</h4>
                        <p>{userDetail.studio.isStudioOwner ? `${userDetail.studio.studioName} · ${userDetail.studio.reviewStrategyText} · ${userDetail.studio.activeText}` : "当前不是工作室管理员"}</p>
                      </div>
                      <div className="console-user-detail__block">
                        <h4>发布统计</h4>
                        <p>已上架 {userDetail.stats.publishedCount} 条，待审核 {userDetail.stats.pendingListingCount} 条，已驳回 {userDetail.stats.rejectedListingCount} 条</p>
                      </div>
                    </div>
                    <div className="console-user-detail__lists">
                      <div className="console-user-detail__list">
                        <h4>最近订单</h4>
                        {userDetail.recentOrders.length > 0 ? (
                          <ul>
                            {userDetail.recentOrders.map((row) => (
                              <li key={row.orderNo}>
                                <strong>{row.listingTitle}</strong>
                                <span>{row.roleText} · {row.statusText} · {row.totalAmount}</span>
                              </li>
                            ))}
                          </ul>
                        ) : <EmptyBlock title="暂无订单记录" />}
                      </div>
                      <div className="console-user-detail__list">
                        <h4>最近发布</h4>
                        {userDetail.recentListings.length > 0 ? (
                          <ul>
                            {userDetail.recentListings.map((row) => (
                              <li key={row.listingNo}>
                                <strong>{row.title}</strong>
                                <span>{row.statusText} · {row.price}</span>
                              </li>
                            ))}
                          </ul>
                        ) : <EmptyBlock title="暂无发布记录" />}
                      </div>
                    </div>
                  </>
                ) : null}
                <div className="console-user-panel__form">
                  <div className="console-review-panel">
                    <div className="console-review-panel__head">
                      <h4>账号状态操作</h4>
                      <span>{selectedUser.accountStatus === "ACTIVE" ? "封禁账号时必须填写原因。" : "恢复账号会清除当前封禁原因。"}</span>
                    </div>
                    {selectedUser.accountStatus === "ACTIVE" ? (
                      <label>
                        封禁原因
                        <textarea
                          rows={3}
                          placeholder="请输入封禁原因，例如恶意骚扰、私下交易等"
                          value={userStatusReason}
                          onChange={(event) => setUserStatusReason(event.target.value)}
                        />
                      </label>
                    ) : (
                      <div className="console-review-panel__summary">
                        当前封禁原因：{selectedUser.banReason || "未记录"}
                      </div>
                    )}
                    <div className="console-user-panel__actions">
                      <Button
                        kind={selectedUser.accountStatus === "ACTIVE" ? "ghost" : "primary"}
                        onClick={() => void handleUserStatus(selectedUser.userId, selectedUser.accountStatus)}
                      >
                        {selectedUser.accountStatus === "ACTIVE" ? "封禁账号" : "恢复账号"}
                      </Button>
                    </div>
                  </div>
                  <label>
                    新登录密码
                    <input
                      autoComplete="new-password"
                      placeholder="6-18 位，需同时包含字母和数字"
                      type="password"
                      value={resetPassword.password}
                      onChange={(event) => setResetPassword((current) => ({ ...current, password: event.target.value }))}
                    />
                  </label>
                  <label>
                    确认新密码
                    <input
                      autoComplete="new-password"
                      placeholder="再次输入新密码"
                      type="password"
                      value={resetPassword.confirmPassword}
                      onChange={(event) => setResetPassword((current) => ({ ...current, confirmPassword: event.target.value }))}
                    />
                  </label>
                  <div className="console-user-panel__actions">
                    <Button kind="primary" onClick={() => void handleResetUserPassword()} disabled={userActionLoading}>
                      {userActionLoading ? "正在重置" : "重置密码"}
                    </Button>
                    <Button kind="secondary" onClick={() => setResetPassword({ password: "", confirmPassword: "" })}>
                      清空输入
                    </Button>
                  </div>
                </div>
              </div>
            ) : <EmptyBlock title="请先在左侧列表选中一个用户" />}
          </SurfaceCard>
          </div>
        ) : null}

        {activeTab === "realName" ? (
          <SurfaceCard eyebrow="实名认证审核" title="实名资料审核队列">
            <InlineFilters
              fields={[
                { key: "status", label: "审核状态", value: realNameStatus, options: [["ALL", "全部"], ["PENDING", "待审核"], ["APPROVED", "已通过"], ["REJECTED", "已驳回"]] },
              ]}
              onChange={(_, value) => setRealNameStatus(value)}
              onSearch={() => void refreshRealNames()}
            />
            {realNameLoading ? <LoadingBlock title="正在加载实名审核队列" compact /> : null}
            {realNameError ? <ErrorBlock title="实名审核加载失败" message={realNameError} compact onRetry={() => void refreshRealNames()} /> : null}
            {realNameCenter ? (
              <>
                <SummaryPills summary={realNameCenter.summary} labels={{ pendingCount: "待审核", approvedCount: "已通过", rejectedCount: "已驳回" }} />
                {realNameCenter.rows.length > 0 ? (
                  <div className="console-table-scroll">
                    <table className="console-table">
                      <thead>
                        <tr>
                          <th>用户</th>
                          <th>实名信息</th>
                          <th>状态</th>
                          <th>证件</th>
                          <th>操作</th>
                        </tr>
                      </thead>
                      <tbody>
                        {realNameCenter.rows.map((row) => (
                          <tr className={selectedRealNameUserId === row.userId ? "is-active" : ""} key={row.userId} onClick={() => setSelectedRealNameUserId(row.userId)}>
                            <td><strong>{row.nickname}</strong><p>{row.phone}</p></td>
                            <td>{row.realName}<p>{row.realNamePhone}</p><p>{row.idCardNo}</p></td>
                            <td>{row.statusText}{row.rejectReason ? <p>{row.rejectReason}</p> : null}</td>
                            <td>{row.frontUrl ? <a href={row.frontUrl} rel="noreferrer" target="_blank">身份证正面</a> : "未上传"}<p>{row.backUrl ? <a href={row.backUrl} rel="noreferrer" target="_blank">身份证反面</a> : "未上传"}</p></td>
                            <td>
                              <div className="console-inline-actions">
                                <Button kind="secondary" onClick={() => setSelectedRealNameUserId(row.userId)}>查看审核</Button>
                              </div>
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : <EmptyBlock title="暂无实名审核数据" />}
                <div className="console-review-panel">
                  <div className="console-review-panel__head">
                    <h4>实名审核操作</h4>
                    <span>{selectedRealNameReview ? `${selectedRealNameReview.nickname} · ${selectedRealNameReview.realName} · ${selectedRealNameReview.realNamePhone} · ${selectedRealNameReview.idCardNo}` : "请选择一条实名申请后处理。"}</span>
                  </div>
                  {selectedRealNameReview ? (
                    <>
                      <div className="console-review-panel__links">
                        {selectedRealNameReview.frontUrl ? <a href={selectedRealNameReview.frontUrl} rel="noreferrer" target="_blank">查看身份证正面</a> : <span>未上传身份证正面</span>}
                        {selectedRealNameReview.backUrl ? <a href={selectedRealNameReview.backUrl} rel="noreferrer" target="_blank">查看身份证反面</a> : <span>未上传身份证反面</span>}
                      </div>
                      {selectedRealNameReview.status === "PENDING" ? (
                        <>
                          <label>
                            驳回原因
                            <textarea
                              rows={3}
                              placeholder="驳回时必填，例如身份证信息不一致"
                              value={realNameReviewReason}
                              onChange={(event) => setRealNameReviewReason(event.target.value)}
                            />
                          </label>
                          <div className="console-user-panel__actions">
                            <Button onClick={() => void handleRealNameReview(selectedRealNameReview.userId, "APPROVE")}>审核通过</Button>
                            <Button kind="secondary" onClick={() => void handleRealNameReview(selectedRealNameReview.userId, "REJECT")}>驳回申请</Button>
                          </div>
                        </>
                      ) : (
                        <div className="console-review-panel__summary">
                          <strong>当前状态：{selectedRealNameReview.statusText}</strong>
                          <span>{selectedRealNameReview.rejectReason ? `驳回原因：${selectedRealNameReview.rejectReason}` : "该记录已完成审核，不可重复操作。"}</span>
                        </div>
                      )}
                    </>
                  ) : (
                    <EmptyBlock title="暂无待处理的实名申请" />
                  )}
                </div>
              </>
            ) : null}
          </SurfaceCard>
        ) : null}

        {activeTab === "role" ? (
          <div className="console-grid console-grid--detail">
          <SurfaceCard eyebrow="角色权限" title="后台角色与成员">
            <div className="console-actions console-actions--inline">
              <Button onClick={() => void handleCreateRole()}>新增角色</Button>
              <Button kind="secondary" onClick={() => void refreshRoles()}>刷新角色</Button>
            </div>
            {roleLoading ? <LoadingBlock title="正在加载角色列表" compact /> : null}
            {roleError ? <ErrorBlock title="角色列表加载失败" message={roleError} compact onRetry={() => void refreshRoles()} /> : null}
            {roleCenter ? (
              <>
                <SummaryPills summary={roleCenter.summary} labels={{ roleCount: "角色数", enabledCount: "启用中", memberCount: "角色成员" }} />
                {roleCenter.rows.length > 0 ? (
                  <div className="console-card-list">
                    {roleCenter.rows.map((row) => (
                      <article className={`console-card-list__item ${selectedRoleId === row.roleId ? "is-active" : ""}`} key={row.roleId} onClick={() => setSelectedRoleId(row.roleId)}>
                        <div>
                          <strong>{row.roleName}</strong>
                          <p>{row.roleCode} · {row.description}</p>
                          <div className="console-role-tags">
                            {row.permissions.map((permission) => <Tag key={permission}>{permission}</Tag>)}
                          </div>
                          <small>成员：{row.members.map((member) => `${member.nickname}(${member.phone})`).join("、") || "暂无"}</small>
                        </div>
                        <div className="console-inline-actions">
                          <Tag tone={row.status === "ACTIVE" ? "success" : "warning"}>{row.statusText}</Tag>
                        </div>
                      </article>
                    ))}
                  </div>
                ) : <EmptyBlock title="暂无角色配置" />}
              </>
            ) : null}
          </SurfaceCard>
          <SurfaceCard eyebrow="角色编辑器" title={selectedRole ? "编辑当前角色" : "创建新角色"}>
            <div className="console-role-editor">
              <div className="console-role-editor__grid">
                <label>
                  角色编码
                  <input
                    placeholder="如 CUSTOM_ADMIN"
                    value={roleForm.roleCode}
                    onChange={(event) => setRoleForm((current) => ({ ...current, roleCode: event.target.value }))}
                  />
                </label>
                <label>
                  角色名称
                  <input
                    placeholder="请输入角色名称"
                    value={roleForm.roleName}
                    onChange={(event) => setRoleForm((current) => ({ ...current, roleName: event.target.value }))}
                  />
                </label>
              </div>
              <label>
                角色说明
                <textarea
                  placeholder="说明该角色的职责边界"
                  rows={3}
                  value={roleForm.description}
                  onChange={(event) => setRoleForm((current) => ({ ...current, description: event.target.value }))}
                />
              </label>
              <label>
                状态
                <select value={roleForm.status} onChange={(event) => setRoleForm((current) => ({ ...current, status: event.target.value }))}>
                  <option value="ACTIVE">启用</option>
                  <option value="DISABLED">停用</option>
                </select>
              </label>
              <div className="console-role-editor__permissions">
                <span>权限范围</span>
                <div className="console-role-editor__permission-groups">
                  {PERMISSION_GROUPS.map((group) => {
                    const groupValues = group.permissions.map((permission) => permission.value);
                    const allChecked = groupValues.every((item) => roleForm.permissions.includes(item));
                    const checkedCount = groupValues.filter((item) => roleForm.permissions.includes(item)).length;
                    return (
                      <section className="console-role-editor__permission-group" key={group.label}>
                        <div className="console-role-editor__permission-group-head">
                          <div>
                            <strong>{group.label}</strong>
                            <small>已选 {checkedCount} / {groupValues.length}</small>
                          </div>
                          <Button kind="secondary" onClick={() => handleTogglePermissionGroup(groupValues)} type="button">
                            {allChecked ? "取消整组" : "整组全选"}
                          </Button>
                        </div>
                        <div className="console-role-editor__permission-list">
                          {group.permissions.map((permission) => (
                            <label className="console-role-editor__permission" key={permission.value}>
                              <input
                                checked={roleForm.permissions.includes(permission.value)}
                                type="checkbox"
                                onChange={() => handleToggleRolePermission(permission.value)}
                              />
                              <span>{permission.label}</span>
                            </label>
                          ))}
                        </div>
                      </section>
                    );
                  })}
                </div>
              </div>
              <div className="console-role-editor__permissions">
                <div className="console-role-editor__members-head">
                  <span>角色成员</span>
                  <div className="console-role-editor__members-tools">
                    <input
                      placeholder="搜索昵称 / 手机号"
                      value={roleUserKeyword}
                      onChange={(event) => setRoleUserKeyword(event.target.value)}
                    />
                    <Button kind="secondary" onClick={() => void refreshRoleCandidates()}>{roleUserLoading ? "刷新中" : "刷新候选"}</Button>
                  </div>
                </div>
                <div className="console-role-editor__member-list">
                  {roleUserCandidates.map((row) => (
                    <label className="console-role-editor__permission" key={row.userId}>
                      <input
                        checked={roleForm.memberIds.includes(row.userId)}
                        type="checkbox"
                        onChange={() => handleToggleRoleMember(row.userId)}
                      />
                      <span>{row.nickname}（{row.phone}）</span>
                    </label>
                  ))}
                </div>
              </div>
              <div className="console-user-panel__actions">
                <Button kind="primary" onClick={() => void handleSaveRoleEditor()} disabled={roleSaving}>
                  {roleSaving ? "正在保存" : "保存角色"}
                </Button>
                {selectedRole ? (
                  <Button
                    kind="secondary"
                    onClick={() =>
                      setRoleForm({
                        roleId: selectedRole.roleId,
                        roleCode: selectedRole.roleCode,
                        roleName: selectedRole.roleName,
                        description: selectedRole.description === "-" ? "" : selectedRole.description,
                        status: selectedRole.status,
                        permissions: selectedRole.permissions,
                        memberIds: selectedRole.members.map((member) => member.userId),
                      })
                    }
                  >
                    恢复当前角色
                  </Button>
                ) : (
                  <Button kind="secondary" onClick={() => setRoleForm({ roleId: 0, roleCode: "", roleName: "", description: "", status: "ACTIVE", permissions: [], memberIds: [] })}>
                    清空表单
                  </Button>
                )}
              </div>
            </div>
          </SurfaceCard>
          </div>
        ) : null}
      </main>
      {listingPreviewUrl ? (
        <div className="console-image-lightbox" role="dialog" aria-modal="true" aria-label="账号截图预览">
          <button
            className="console-image-lightbox__backdrop"
            type="button"
            aria-label="关闭预览"
            onClick={() => {
              clearListingPreviewBoundaryNotice();
              setListingPreviewUrl("");
            }}
          />
          <div className="console-image-lightbox__dialog">
            <div className="console-image-lightbox__toolbar">
              <span>{listingPreviewImages.length ? `第 ${listingPreviewIndex + 1} 张 / 共 ${listingPreviewImages.length} 张` : "图片预览"}</span>
              <button
                className="console-image-lightbox__close"
                type="button"
                onClick={() => {
                  clearListingPreviewBoundaryNotice();
                  setListingPreviewUrl("");
                }}
              >
                关闭
              </button>
            </div>
            <div className="console-image-lightbox__stage">
              {listingPreviewBoundaryNotice ? (
                <div className="console-image-lightbox__notice" role="status" aria-live="polite">
                  {listingPreviewBoundaryNotice}
                </div>
              ) : null}
              {listingPreviewImages.length > 1 ? (
                <button className="console-image-lightbox__nav console-image-lightbox__nav--prev" type="button" aria-label="上一张" onClick={() => moveListingPreview(-1)}>
                  ‹
                </button>
              ) : null}
              <img alt="账号截图大图预览" src={listingPreviewUrl} />
              {listingPreviewImages.length > 1 ? (
                <button className="console-image-lightbox__nav console-image-lightbox__nav--next" type="button" aria-label="下一张" onClick={() => moveListingPreview(1)}>
                  ›
                </button>
              ) : null}
            </div>
          </div>
        </div>
      ) : null}
      {selectedOrderNo ? (
        <div className="console-modal-backdrop" onClick={() => setSelectedOrderNo("")} role="presentation">
          <div className="console-modal console-modal--order" onClick={(event) => event.stopPropagation()} role="dialog" aria-modal="true" aria-label="订单详情弹窗">
            <div className="console-modal__header">
              <div>
                <span className="console-modal__eyebrow">订单详情</span>
                <h3>{orderDetail?.summary.orderNo ?? selectedOrderNo}</h3>
                <p>{orderDetail ? `${orderDetail.summary.buyerNickname} / ${orderDetail.summary.sellerDisplayName}` : "正在同步订单详情、进度与聊天记录"}</p>
              </div>
              <Button kind="ghost" onClick={() => setSelectedOrderNo("")}>关闭</Button>
            </div>
            <div className="console-modal__body">
              {orderDetailLoading ? <LoadingBlock title="正在加载订单详情" compact /> : null}
              {orderDetailError ? <ErrorBlock title="订单详情加载失败" message={orderDetailError} compact onRetry={() => selectedOrderNo && setSelectedOrderNo(selectedOrderNo)} /> : null}
              {orderDetail ? (
                <div className="console-order-detail">
                  <div className="console-support-meta">
                    <Tag>{orderDetail.summary.statusText}</Tag>
                    <Tag tone="warning">{orderDetail.summary.sellerTypeText}</Tag>
                    <span>{orderDetail.summary.listingTitle}</span>
                  </div>

                  <div className="console-support-facts">
                    <div className="console-support-facts__item"><span>订单总额</span><strong>{orderDetail.summary.totalAmount}</strong></div>
                    <div className="console-support-facts__item"><span>支付流水</span><strong>{orderDetail.summary.paymentTransactionId}</strong></div>
                    <div className="console-support-facts__item"><span>支付时间</span><strong>{orderDetail.summary.paidAt}</strong></div>
                    <div className="console-support-facts__item"><span>交易群聊</span><strong>{orderDetail.summary.chatGroupNo || "-"}</strong></div>
                  </div>

                  <div className="console-order-progress">
                    {orderDetail.progress.map((step) => (
                      <div className={`console-order-progress__step ${step.done ? "is-done" : ""} ${step.current ? "is-current" : ""}`} key={step.title}>
                        <span />
                        <div>
                          <strong>{step.title}</strong>
                          <p>{step.description}</p>
                          <time>{step.time}</time>
                        </div>
                      </div>
                    ))}
                  </div>

                  <div className="console-order-sections">
                    {orderDetail.detailSections.map((section) => (
                      <section className="console-order-section" key={section.title}>
                        <h4>{section.title}</h4>
                        <div className="console-order-section__grid">
                          {section.items.map((item) => (
                            <div className="console-order-section__item" key={`${section.title}-${item.label}`}>
                              <span>{item.label}</span>
                              <strong>{item.value}</strong>
                            </div>
                          ))}
                        </div>
                      </section>
                    ))}
                  </div>

                  <section className="console-order-section">
                    <h4>聊天记录</h4>
                    {orderConversationLoading ? <LoadingBlock title="正在加载聊天记录" compact /> : null}
                    {orderConversationError ? <p className="console-order-section__hint" role="alert">{orderConversationError}</p> : null}
                    {orderConversation ? (
                      <div className="console-support-messages console-order-messages">
                        {orderConversation.messages.map((message) => (
                          <article className={`console-support-message ${message.mine ? "is-mine" : ""}`} key={message.id}>
                            <div className="console-support-message__meta">
                              <strong>{message.senderName}</strong>
                              <span>{message.senderRoleLabel}</span>
                              <time>{message.createdAt}</time>
                            </div>
                            {message.text ? <p>{message.text}</p> : null}
                            {message.messageType === "IMAGE" && message.fileUrl ? (
                              <a className="console-support-message__image" href={message.fileUrl} rel="noreferrer" target="_blank">
                                <img alt={message.fileName ?? "图片消息"} src={message.fileUrl} />
                              </a>
                            ) : null}
                            {message.messageType === "FILE" && message.fileUrl ? (
                              <a className="console-support-message__file" href={message.fileUrl} rel="noreferrer" target="_blank">
                                {message.fileName ?? "查看附件"}
                              </a>
                            ) : null}
                          </article>
                        ))}
                      </div>
                    ) : !orderConversationLoading && !orderConversationError ? <p className="console-order-section__hint">当前订单暂无可查看的聊天记录。</p> : null}
                  </section>
                </div>
              ) : !orderDetailLoading && !orderDetailError ? <EmptyBlock title="未找到订单详情" /> : null}
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}

function ListingFilters({
  value,
  onChange,
  onSearch,
}: {
  value: { status: string; sellerType: string; keyword: string };
  onChange: (next: { status: string; sellerType: string; keyword: string }) => void;
  onSearch: () => void;
}) {
  return (
    <div className="console-filter-row">
      <label>
        状态
        <select value={value.status} onChange={(event) => onChange({ ...value, status: event.target.value })}>
          <option value="ALL">全部</option>
          <option value="PENDING_REVIEW">待审核</option>
          <option value="PUBLISHED">已上架</option>
          <option value="REJECTED">已驳回</option>
          <option value="OFFLINE">已下架</option>
        </select>
      </label>
      <label>
        卖家类型
        <select value={value.sellerType} onChange={(event) => onChange({ ...value, sellerType: event.target.value })}>
          <option value="ALL">全部</option>
          <option value="PERSONAL">个人</option>
          <option value="STUDIO">工作室</option>
        </select>
      </label>
      <label className="console-filter-row__search">
        关键词
        <input placeholder="账号标题 / 卖家 / 编号" value={value.keyword} onChange={(event) => onChange({ ...value, keyword: event.target.value })} />
      </label>
      <Button onClick={onSearch}>筛选</Button>
    </div>
  );
}

function InlineFilters({
  fields,
  onChange,
  onSearch,
}: {
  fields: Array<{ key: string; label: string; value: string; options: Array<[string, string]> }>;
  onChange: (key: string, value: string) => void;
  onSearch: () => void;
}) {
  return (
    <div className="console-filter-row">
      {fields.map((field) => (
        <label key={field.key}>
          {field.label}
          <select value={field.value} onChange={(event) => onChange(field.key, event.target.value)}>
            {field.options.map(([value, label]) => (
              <option key={value} value={value}>{label}</option>
            ))}
          </select>
        </label>
      ))}
      <Button onClick={onSearch}>筛选</Button>
    </div>
  );
}

function InlineKeywordFilters({
  keyword,
  keywordLabel,
  selectLabel,
  selectValue,
  selectOptions,
  onKeywordChange,
  onSelectChange,
  onSearch,
}: {
  keyword: string;
  keywordLabel: string;
  selectLabel: string;
  selectValue: string;
  selectOptions: Array<[string, string]>;
  onKeywordChange: (value: string) => void;
  onSelectChange: (value: string) => void;
  onSearch: () => void;
}) {
  return (
    <div className="console-filter-row">
      <label className="console-filter-row__search">
        {keywordLabel}
        <input value={keyword} onChange={(event) => onKeywordChange(event.target.value)} placeholder="输入关键词搜索" />
      </label>
      <label>
        {selectLabel}
        <select value={selectValue} onChange={(event) => onSelectChange(event.target.value)}>
          {selectOptions.map(([value, label]) => (
            <option key={value} value={value}>{label}</option>
          ))}
        </select>
      </label>
      <Button onClick={onSearch}>筛选</Button>
    </div>
  );
}

function SummaryPills({ summary, labels }: { summary: Record<string, number>; labels: Record<string, string> }) {
  return (
    <div className="console-summary-pills">
      {Object.entries(summary).map(([key, value]) => (
        <div className="console-summary-pill" key={key}>
          <strong>{value}</strong>
          <span>{labels[key] ?? key}</span>
        </div>
      ))}
    </div>
  );
}

function WithdrawReviewDialogContent({
  applicationNo,
  applicant,
  channel,
  amount,
  statusText,
  accountName,
  accountNo,
  qrCodeUrl,
  createdAt,
  paidAt,
  rejectReason,
  reason,
  reasonPlaceholder,
  onReasonChange,
  onApprove,
  onReject,
}: {
  applicationNo: string;
  applicant: string;
  channel: string;
  amount: string;
  statusText: string;
  accountName: string;
  accountNo: string;
  qrCodeUrl: string | null;
  createdAt: string;
  paidAt: string;
  rejectReason: string;
  reason: string;
  reasonPlaceholder: string;
  onReasonChange: (value: string) => void;
  onApprove: () => void;
  onReject: () => void;
}) {
  return (
    <div className="withdraw-review-dialog">
      <section className="withdraw-review-dialog__hero">
        <div>
          <span>提现金额</span>
          <strong>{amount}</strong>
        </div>
        <Tag tone={statusText === "待审核" ? "warning" : "success"}>{statusText}</Tag>
      </section>
      <div className="withdraw-review-dialog__grid">
        <DetailItem label="申请单号" value={applicationNo} />
        <DetailItem label="申请人" value={applicant} />
        <DetailItem label="提现渠道" value={channel} />
        <DetailItem label="收款姓名" value={accountName || "-"} />
        <DetailItem label="收款账号" value={accountNo || "-"} wide />
        <div className="withdraw-review-dialog__qrcode">
          <span>收款码图片</span>
          {qrCodeUrl ? (
            <a href={qrCodeUrl} rel="noreferrer" target="_blank">
              <img alt="收款码图片" src={qrCodeUrl} />
            </a>
          ) : (
            <strong>未上传</strong>
          )}
        </div>
        <DetailItem label="提交时间" value={createdAt || "-"} />
        <DetailItem label="到账时间" value={paidAt || "-"} />
        <DetailItem label="驳回记录" value={rejectReason || "-"} wide />
      </div>
      <label className="withdraw-review-dialog__reason">
        驳回原因
        <textarea
          rows={4}
          placeholder={reasonPlaceholder}
          value={reason}
          onChange={(event) => onReasonChange(event.target.value)}
        />
      </label>
      <div className="withdraw-review-dialog__actions">
        <Button onClick={onApprove}>确认已打款，通过审核</Button>
        <Button kind="secondary" onClick={onReject}>驳回并退回余额</Button>
      </div>
    </div>
  );
}

function DetailItem({ label, value, wide = false }: { label: string; value: string; wide?: boolean }) {
  return (
    <div className={`withdraw-review-dialog__item ${wide ? "withdraw-review-dialog__item--wide" : ""}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function LoadingBlock({ title, compact = false }: { title: string; compact?: boolean }) {
  return (
    <div className={`console-status-block ${compact ? "console-status-block--compact" : ""}`.trim()}>
      <StatusState
        title={title}
        description="正在同步后台最新数据，请稍候。"
        tone="neutral"
        action={compact ? null : undefined}
      />
    </div>
  );
}

function ErrorBlock({ title, message, onRetry, compact = false }: { title: string; message: string; onRetry: () => void; compact?: boolean }) {
  return (
    <div className={`console-status-block ${compact ? "console-status-block--compact" : ""}`.trim()}>
      <StatusState
        title={title}
        description={message}
        tone="error"
        action={<Button onClick={onRetry} kind={compact ? "secondary" : "primary"}>重试</Button>}
      />
    </div>
  );
}

function EmptyBlock({ title }: { title: string }) {
  return (
    <div className="console-status-block console-status-block--compact">
      <StatusState title={title} description="当前筛选条件下没有数据，可调整筛选条件后重试。" tone="neutral" />
    </div>
  );
}

function ConsoleLoginScreen({
  badge,
  title,
  description,
  error,
  onSubmit,
}: {
  badge: string;
  title: string;
  description: string;
  error: string;
  onSubmit: (phone: string, password: string) => Promise<void>;
}) {
  const [phone, setPhone] = useState("");
  const [password, setPassword] = useState("");
  const [submitting, setSubmitting] = useState(false);

  async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmitting(true);
    try {
      await onSubmit(phone.trim(), password);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="console-shell console-auth-shell">
      <SurfaceCard eyebrow={badge} title={title}>
        <form className="console-login" onSubmit={handleSubmit}>
          <p>{description}</p>
          <label>
            手机号
            <input value={phone} onChange={(event) => setPhone(event.target.value)} placeholder="请输入管理员手机号" />
          </label>
          <label>
            密码
            <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="请输入密码" />
          </label>
          {error ? <p className="console-login__error">{error}</p> : null}
          <div className="console-login__actions">
            <Button type="submit" disabled={submitting}>{submitting ? "登录中..." : "进入管理后台"}</Button>
          </div>
        </form>
      </SurfaceCard>
    </div>
  );
}
