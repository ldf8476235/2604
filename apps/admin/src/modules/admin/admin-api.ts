import { prepareImageForUpload } from "@delta/ui";
import { request } from "../../lib/request";

export type MetricItem = { label: string; value: string; trend: string };

export type AdminDashboard = {
  metrics: MetricItem[];
  overview: {
    walletFrozenAmount: string;
    studioCount: number;
    userCount: number;
    boostingServiceCount: number;
  };
  pendingQueue: Array<{
    type: string;
    title: string;
    subtitle: string;
    status: string;
    primaryKey: string;
  }>;
};

export type AdminConsoleSession = {
  userId: number;
  nickname: string;
  phone: string;
  roles: Array<{
    roleCode: string;
    roleName: string;
  }>;
  permissions: string[];
};

export type AdminImSummaryItem = {
  label: string;
  value: string;
};

export type AdminImMessage = {
  id: number;
  senderRole: string;
  senderRoleLabel: string;
  senderName: string;
  avatarUrl: string | null;
  mine: boolean;
  messageType: "TEXT" | "IMAGE" | "FILE" | "SYSTEM";
  text: string | null;
  fileUrl: string | null;
  fileName: string | null;
  createdAt: string;
};

export type AdminImConversation = {
  conversationNo: string;
  sceneCode: string;
  sceneLabel: string;
  sourceOrderNo: string;
  title: string;
  description: string;
  statusLabel: string;
  supportDisplayName: string;
  currentUserRole: string;
  currentUserName: string;
  currentUserAvatarUrl: string | null;
  summaryItems: AdminImSummaryItem[];
  messages: AdminImMessage[];
};

export type AdminImWorkbenchRow = {
  conversationNo: string;
  sceneCode: string;
  sceneLabel: string;
  sourceOrderNo: string;
  title: string;
  description: string;
  statusLabel: string;
  buyerName: string;
  counterpartyName: string;
  lastMessageExcerpt: string;
  lastMessageAt: string;
  latestMessageId: number;
  unread: boolean;
  unreadCount: number;
};

export type AdminImWorkbenchRefreshEvent = {
  conversationNo: string;
  sceneCode: string;
  sourceOrderNo: string;
  title: string;
  lastMessageExcerpt: string;
  lastMessageAt: string;
  statusLabel: string;
  reason: string;
  latestMessageId: number;
  senderRole: string;
  eventType: "USER_MESSAGE" | "SUPPORT_MESSAGE" | "INIT" | "SYSTEM_MESSAGE";
};

export type AdminImWorkbench = {
  supportDisplayName: string;
  supportAvatarUrl: string | null;
  rows: AdminImWorkbenchRow[];
  totalCount: number;
  boostingCount: number;
  tradeCount: number;
};

export type AdminPaymentConfig = {
  wechatEnabled: boolean;
  wechatMockMode: boolean;
  wechatAppId: string;
  wechatMchId: string;
  wechatNotifyUrl: string;
  alipayEnabled: boolean;
  alipayMockMode: boolean;
  alipayAppId: string;
  alipayMerchantNo: string;
  alipayNotifyUrl: string;
};

export type AdminLoginConfig = {
  smsEnabled: boolean;
  smsMockMode: boolean;
  smsSignName: string;
  smsTemplateCode: string;
  wechatOpenEnabled: boolean;
  wechatOpenMockMode: boolean;
  wechatOpenAppId: string;
  wechatOpenRedirectUri: string;
};

export type AdminDistributionConfig = {
  autoEnableAfterVerified: boolean;
  defaultTradeCommissionRate: string;
  defaultBoostingCommissionRate: string;
};

export type AdminListingPublishConfig = {
  defaultExchangeRate: string;
  personalSellerCommissionRate: string;
};

export type AdminIntegrationConfigCenter = {
  payment: AdminPaymentConfig;
  login: AdminLoginConfig;
  distribution: AdminDistributionConfig;
  listingPublish: AdminListingPublishConfig;
};

export type AdminListingRow = {
  listingNo: string;
  title: string;
  sellerType: string;
  sellerTypeText: string;
  sellerDisplayName: string;
  status: string;
  statusText: string;
  reviewProgress: string;
  cityName: string;
  accountLevel: number;
  rankName: string;
  price: string;
  exchangeRateLabel: string;
  viewCount: number;
  favoriteCount: number;
  salesCount: number;
  publishedAt: string;
  submittedAt: string;
  updatedAt: string;
  rejectionReason: string;
};

export type AdminListingCenter = {
  rows: AdminListingRow[];
  summary: {
    pendingReview: number;
    published: number;
    rejected: number;
    offline: number;
  };
};

export type AdminListingDetail = {
  summary: {
    listingNo: string;
    title: string;
    status: string;
    statusText: string;
    sellerDisplayName: string;
    sellerPhone: string;
    sellerTypeText: string;
    price: string;
    exchangeRateLabel: string;
    reviewStrategy: string;
    submittedAt: string;
    updatedAt: string;
    publishedAt: string;
    coverUrl: string | null;
    imageUrls: string[];
    description: string;
    estimateDetail: string;
    rejectionReason: string;
  };
  detailSections: Array<{
    title: string;
    items: Array<{ label: string; value: string }>;
  }>;
  reviewRecords: Array<{ label: string; status: string; operator?: string; time: string; remark: string }>;
  tradeRecords: Array<{ orderNo: string; buyerNickname: string; sellerNickname: string; status: string; statusText: string; totalAmount: string; createdAt: string; completedAt: string }>;
};

export type AdminOrderCenter = {
  rows: Array<{
    orderNo: string;
    listingNo: string;
    listingTitle: string;
    buyerNickname: string;
    sellerDisplayName: string;
    sellerType: string;
    sellerTypeText: string;
    status: string;
    statusText: string;
    chatGroupNo: string;
    paymentMethod: string;
    itemAmount: string;
    serviceFee: string;
    totalAmount: string;
    createdAt: string;
    paidAt: string;
    completedAt: string;
    refundAmount: string;
    refundRequestedAt: string;
    refundReason: string;
    canForceRefund: boolean;
  }>;
  summary: Record<string, number>;
};

export type AdminOrderDetail = {
  summary: {
    orderNo: string;
    listingNo: string;
    listingTitle: string;
    listingSummary: string;
    buyerUserId: number;
    buyerNickname: string;
    sellerUserId: number;
    sellerNickname: string;
    sellerDisplayName: string;
    sellerType: string;
    sellerTypeText: string;
    status: string;
    statusText: string;
    chatGroupNo: string;
    paymentMethod: string;
    paymentTradeType: string;
    paymentTransactionId: string;
    itemAmount: string;
    serviceFee: string;
    extraItemsAmount: string;
    totalAmount: string;
    refundAmount: string;
    refundReason: string;
    refundReviewNote: string;
    createdAt: string;
    paidAt: string;
    tradeStartedAt: string;
    completedAt: string;
    closedAt: string;
    afterSaleAt: string;
    refundRequestedAt: string;
    refundReviewedAt: string;
    refundedAt: string;
    updatedAt: string;
    canForceRefund: boolean;
  };
  detailSections: Array<{
    title: string;
    items: Array<{ label: string; value: string }>;
  }>;
  progress: Array<{
    title: string;
    description: string;
    time: string;
    done: boolean;
    current: boolean;
  }>;
};

export type AdminStudioCenter = {
  rows: Array<{
    studioId: number;
    studioName: string;
    ownerUserId: number;
    ownerNickname: string;
    ownerPhoneRaw: string;
    ownerPhone: string;
    contactName: string;
    qualificationCode: string;
    reviewStrategy: string;
    reviewStrategyText: string;
    shareRatio: string;
    active: boolean;
    activeText: string;
    cooperationStatus: string;
    listingCount: number;
    orderCount: number;
    gmv: string;
    createdAt: string;
  }>;
  summary: {
    activeCount: number;
    directPublishCount: number;
    reviewRequiredCount: number;
  };
};

export type AdminStudioApplicationCenter = {
  rows: Array<{
    applicationNo: string;
    status: string;
    statusText: string;
    studioId: number | null;
    studioName: string;
    applicantUserId: number;
    applicantNickname: string;
    applicantPhone: string;
    applicantPhoneRaw: string;
    contactName: string;
    contactPhone: string;
    qualificationCode: string;
    qualificationNote: string;
    qualificationMaterialKey: string;
    qualificationMaterialUrl: string | null;
    rejectReason: string;
    createdAt: string;
    reviewedAt: string;
  }>;
  summary: {
    pending: number;
    approved: number;
    rejected: number;
  };
};

export type AdminStudioDetail = {
  summary: {
    studioId: number;
    studioName: string;
    description: string;
    contactPhone: string;
    contactName: string;
    contactWechat: string;
    qualificationCode: string;
    qualificationMaterialKey: string;
    qualificationMaterialUrl: string | null;
    qualificationNote: string;
    ownerNickname: string;
    ownerPhoneRaw: string;
    ownerPhone: string;
    reviewStrategyText: string;
    shareRatio: string;
    cooperationStatus: string;
    activeText: string;
    createdAt: string;
    updatedAt: string;
  };
  recentListings: Array<{
    listingNo: string;
    title: string;
    statusText: string;
    price: string;
    updatedAt: string;
  }>;
  recentOrders: Array<{
    orderNo: string;
    listingTitle: string;
    buyerNickname: string;
    statusText: string;
    totalAmount: string;
    createdAt: string;
  }>;
};

export type AdminStudioSavePayload = {
  studioId?: number;
  ownerPhone: string;
  studioName: string;
  description?: string;
  contactPhone: string;
  contactName?: string;
  contactWechat?: string;
  qualificationCode?: string;
  qualificationMaterialKey?: string;
  qualificationNote?: string;
  reviewStrategy: string;
  shareRatio: string;
  active: boolean;
  cooperationStatus: string;
};

export type AdminBoostingCenter = {
  rows: Array<{
    serviceNo: string;
    name: string;
    categoryLabel: string;
    description: string;
    price: string;
    cycleLabel: string;
    guaranteeNote: string;
    providerType: string;
    providerTypeText: string;
    providerName: string;
    salesCount: number;
    status: string;
    statusText: string;
    sortNo: number;
  }>;
  summary: {
    activeCount: number;
    disabledCount: number;
  };
};

export type AdminWithdrawCenter = {
  rows: Array<{
    applicationNo: string;
    userId: number;
    nickname: string;
    realName: string;
    channel: string;
    channelText: string;
    amount: string;
    status: string;
    statusText: string;
    rejectReason: string;
    accountNo: string;
    qrCodeUrl: string | null;
    createdAt: string;
    reviewedAt: string;
    paidAt: string;
  }>;
  summary: {
    pending: number;
    paid: number;
    rejected: number;
  };
};

export type AdminStudioWithdrawCenter = {
  rows: Array<{
    applicationNo: string;
    ownerUserId: number;
    studioName: string;
    ownerNickname: string;
    accountName: string;
    channel: string;
    channelText: string;
    amount: string;
    status: string;
    statusText: string;
    rejectReason: string;
    accountNo: string;
    createdAt: string;
    reviewedAt: string;
    paidAt: string;
  }>;
  summary: {
    pending: number;
    paid: number;
    rejected: number;
  };
};

export type AdminOperationCenter = {
  banners: Array<{
    bannerId: number;
    bannerNo: string;
    title: string;
    imageUrl: string | null;
    imageKey: string;
    linkUrl: string;
    sortNo: number;
    status: string;
    statusText: string;
    updatedAt: string;
  }>;
  shortcuts: Array<{
    shortcutId: number;
    shortcutNo: string;
    name: string;
    iconUrl: string | null;
    iconKey: string | null;
    linkUrl: string;
    sortNo: number;
    status: string;
    statusText: string;
    updatedAt: string;
  }>;
  announcements: Array<{
    announcementId: number;
    announcementNo: string;
    title: string;
    content: string;
    category: string;
    categoryText: string;
    pinned: boolean;
    status: string;
    statusText: string;
    publishAt: string;
    updatedAt: string;
  }>;
  summary: {
    bannerCount: number;
    shortcutCount: number;
    publishedAnnouncementCount: number;
  };
};

export type AdminGunCodeCenter = {
  summary: {
    groupCount: number;
    entryCount: number;
    categoryCount: number;
    tagCount: number;
  };
  rows: Array<{
    groupKey: string;
    creator: string;
    source: string;
    badges: string[];
    entryCount: number;
    sortNo: number;
    updatedAt: string;
  }>;
};

export type AdminGunCodeImportRow = {
  creator: string;
  source?: string;
  badges: string[];
  title: string;
  category: string;
  entryCode: string;
  tags: string[];
  groupSortNo?: number;
  entrySortNo?: number;
};

export type AdminGunCodeImportResult = {
  replaceExisting: boolean;
  importedRowCount: number;
  insertedGroups: number;
  updatedGroups: number;
  insertedEntries: number;
  updatedEntries: number;
  center: AdminGunCodeCenter;
};

export type AdminUserCenter = {
  rows: Array<{
    userId: number;
    nickname: string;
    phone: string;
    verified: boolean;
    verifiedText: string;
    realNameStatus: string;
    realNameStatusText: string;
    accountStatus: string;
    accountStatusText: string;
    banReason: string;
    isStudioOwner: boolean;
    studioOwnerText: string;
    createdAt: string;
    updatedAt: string;
  }>;
  summary: {
    activeCount: number;
    disabledCount: number;
    verifiedCount: number;
    studioOwnerCount: number;
  };
};

export type AdminUserDetail = {
  summary: {
    userId: number;
    nickname: string;
    phone: string;
    avatarUrl: string | null;
    accountStatus: string;
    accountStatusText: string;
    banReason: string;
    verified: boolean;
    verifiedText: string;
    realName: string;
    realNameStatus: string;
    realNameStatusText: string;
    idCardNo: string;
    frontUrl: string | null;
    backUrl: string | null;
    loginAlertEnabled: boolean;
    secondaryVerifyEnabled: boolean;
    createdAt: string;
    updatedAt: string;
  };
  wallet: {
    availableBalance: string;
    frozenBalance: string;
    totalCommission: string;
  };
  studio: {
    isStudioOwner: boolean;
    studioName: string;
    reviewStrategyText: string;
    activeText: string;
  };
  stats: {
    buyerOrderCount: number;
    sellerOrderCount: number;
    publishCount: number;
    publishedCount: number;
    pendingListingCount: number;
    rejectedListingCount: number;
  };
  recentOrders: Array<{
    orderNo: string;
    listingTitle: string;
    roleText: string;
    statusText: string;
    totalAmount: string;
    createdAt: string;
  }>;
  recentListings: Array<{
    listingNo: string;
    title: string;
    statusText: string;
    price: string;
    updatedAt: string;
  }>;
};

export type AdminRealNameCenter = {
  rows: Array<{
    userId: number;
    nickname: string;
    phone: string;
    realNamePhone: string;
    realName: string;
    idCardNo: string;
    status: string;
    statusText: string;
    frontUrl: string | null;
    backUrl: string | null;
    rejectReason: string;
    updatedAt: string;
  }>;
  summary: {
    pendingCount: number;
    approvedCount: number;
    rejectedCount: number;
  };
};

export type AdminRoleCenter = {
  rows: Array<{
    roleId: number;
    roleCode: string;
    roleName: string;
    description: string;
    permissions: string[];
    status: string;
    statusText: string;
    memberCount: number;
    members: Array<{ userId: number; nickname: string; phone: string }>;
    updatedAt: string;
  }>;
  summary: {
    roleCount: number;
    enabledCount: number;
    memberCount: number;
  };
};

export function loadAdminDashboard() {
  return request<AdminDashboard>("/api/admin/dashboard");
}

export function loadAdminSession() {
  return request<AdminConsoleSession>("/api/admin/session");
}

export function loadAdminListings(params: { status?: string; sellerType?: string; keyword?: string }) {
  const query = new URLSearchParams();
  if (params.status) query.set("status", params.status);
  if (params.sellerType) query.set("sellerType", params.sellerType);
  if (params.keyword) query.set("keyword", params.keyword);
  return request<AdminListingCenter>(`/api/admin/listings${query.toString() ? `?${query.toString()}` : ""}`);
}

export function loadAdminListingDetail(listingNo: string) {
  return request<AdminListingDetail>(`/api/admin/listings/${listingNo}`);
}

export function reviewAdminListing(listingNo: string, action: string, reason?: string) {
  return request(`/api/admin/listings/${listingNo}/review`, {
    method: "POST",
    body: JSON.stringify({ action, reason }),
  });
}

export function deleteAdminListing(listingNo: string) {
  return request(`/api/admin/listings/${listingNo}`, {
    method: "DELETE",
  });
}

export function loadAdminOrders(params: { status?: string; sellerType?: string }) {
  const query = new URLSearchParams();
  if (params.status) query.set("status", params.status);
  if (params.sellerType) query.set("sellerType", params.sellerType);
  return request<AdminOrderCenter>(`/api/admin/orders${query.toString() ? `?${query.toString()}` : ""}`);
}

export function loadAdminOrderDetail(orderNo: string) {
  return request<AdminOrderDetail>(`/api/admin/orders/${encodeURIComponent(orderNo)}`);
}

export function forceAdminRefund(orderNo: string, reason: string) {
  return request(`/api/admin/orders/${orderNo}/force-refund`, {
    method: "POST",
    body: JSON.stringify({ reason }),
  });
}

export function loadAdminStudios(params: { keyword?: string; active?: string }) {
  const query = new URLSearchParams();
  if (params.keyword) query.set("keyword", params.keyword);
  if (params.active) query.set("active", params.active);
  return request<AdminStudioCenter>(`/api/admin/studios${query.toString() ? `?${query.toString()}` : ""}`);
}

export function loadAdminStudioApplications(params: { status?: string; keyword?: string }) {
  const query = new URLSearchParams();
  if (params.status) query.set("status", params.status);
  if (params.keyword) query.set("keyword", params.keyword);
  return request<AdminStudioApplicationCenter>(`/api/admin/studio-applications${query.toString() ? `?${query.toString()}` : ""}`);
}

export function loadAdminStudioDetail(studioId: number) {
  return request<AdminStudioDetail>(`/api/admin/studios/${studioId}`);
}

export function saveAdminStudio(payload: AdminStudioSavePayload) {
  return request("/api/admin/studios", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function updateStudioPolicy(studioId: number, reviewStrategy: string) {
  return request(`/api/admin/studios/${studioId}/policy`, {
    method: "POST",
    body: JSON.stringify({ reviewStrategy }),
  });
}

export function updateStudioStatus(studioId: number, active: boolean) {
  return request(`/api/admin/studios/${studioId}/status`, {
    method: "POST",
    body: JSON.stringify({ active }),
  });
}

export function updateStudioShareRatio(studioId: number, shareRatio: string) {
  return request(`/api/admin/studios/${studioId}/share-ratio`, {
    method: "POST",
    body: JSON.stringify({ shareRatio }),
  });
}

export function reviewStudioApplication(applicationNo: string, action: string, reason?: string) {
  return request(`/api/admin/studio-applications/${applicationNo}/review`, {
    method: "POST",
    body: JSON.stringify({ action, reason }),
  });
}

export function loadAdminBoostingServices(params: { status?: string; providerType?: string }) {
  const query = new URLSearchParams();
  if (params.status) query.set("status", params.status);
  if (params.providerType) query.set("providerType", params.providerType);
  return request<AdminBoostingCenter>(`/api/admin/boosting/services${query.toString() ? `?${query.toString()}` : ""}`);
}

export function updateBoostingServiceStatus(serviceNo: string, status: string) {
  return request(`/api/admin/boosting/services/${serviceNo}/status`, {
    method: "POST",
    body: JSON.stringify({ status }),
  });
}

export function loadAdminWithdraws(params: { status?: string }) {
  const query = new URLSearchParams();
  if (params.status) query.set("status", params.status);
  return request<AdminWithdrawCenter>(`/api/admin/withdraws${query.toString() ? `?${query.toString()}` : ""}`);
}

export function reviewWithdraw(applicationNo: string, action: string, reason?: string) {
  return request(`/api/admin/withdraws/${applicationNo}/review`, {
    method: "POST",
    body: JSON.stringify({ action, reason }),
  });
}

export function loadAdminStudioWithdraws(params: { status?: string }) {
  const query = new URLSearchParams();
  if (params.status) query.set("status", params.status);
  return request<AdminStudioWithdrawCenter>(`/api/admin/studio-withdraws${query.toString() ? `?${query.toString()}` : ""}`);
}

export function reviewStudioWithdraw(applicationNo: string, action: string, reason?: string) {
  return request(`/api/admin/studio-withdraws/${applicationNo}/review`, {
    method: "POST",
    body: JSON.stringify({ action, reason }),
  });
}

export function loadAdminOperations() {
  return request<AdminOperationCenter>("/api/admin/operations");
}

export function loadAdminGunCodes() {
  return request<AdminGunCodeCenter>("/api/admin/gun-codes");
}

export function importAdminGunCodes(payload: { replaceExisting: boolean; rows: AdminGunCodeImportRow[] }) {
  return request<AdminGunCodeImportResult>("/api/admin/gun-codes/import", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function saveAdminBanner(payload: {
  bannerId?: number;
  title: string;
  imageKey: string;
  linkUrl?: string;
  sortNo?: number;
  status: string;
}) {
  return request("/api/admin/operations/banners", { method: "POST", body: JSON.stringify(payload) });
}

export function saveAdminShortcut(payload: {
  shortcutId?: number;
  name: string;
  iconKey?: string | null;
  linkUrl: string;
  sortNo?: number;
  status: string;
}) {
  return request("/api/admin/operations/shortcuts", { method: "POST", body: JSON.stringify(payload) });
}

export function saveAdminAnnouncement(payload: {
  announcementId?: number;
  title: string;
  content: string;
  category: string;
  pinned: boolean;
  status: string;
}) {
  return request("/api/admin/operations/announcements", { method: "POST", body: JSON.stringify(payload) });
}

export function deleteAdminBanner(bannerId: number) {
  return request(`/api/admin/operations/banners/${bannerId}`, { method: "DELETE" });
}

export function deleteAdminShortcut(shortcutId: number) {
  return request(`/api/admin/operations/shortcuts/${shortcutId}`, { method: "DELETE" });
}

export function deleteAdminAnnouncement(announcementId: number) {
  return request(`/api/admin/operations/announcements/${announcementId}`, { method: "DELETE" });
}

export function batchAdminOperationStatus(type: "banner" | "shortcut" | "announcement", payload: { ids: number[]; status: string }) {
  return request(`/api/admin/operations/${type}/batch-status`, { method: "POST", body: JSON.stringify(payload) });
}

export type UploadedAdminAsset = {
  objectKey: string;
  previewUrl: string;
};

export async function uploadAdminAsset(file: File, businessScope: string) {
  const prepared = await prepareImageForUpload(file);
  const formData = new FormData();
  formData.append("businessScope", businessScope);
  formData.append("file", prepared.file, prepared.file.name);
  return request<UploadedAdminAsset>("/api/oss/upload", {
    method: "POST",
    body: formData,
  });
}

export function loadAdminUsers(params: { keyword?: string; status?: string; verified?: string; studioOwner?: string }) {
  const query = new URLSearchParams();
  if (params.keyword) query.set("keyword", params.keyword);
  if (params.status) query.set("status", params.status);
  if (params.verified) query.set("verified", params.verified);
  if (params.studioOwner) query.set("studioOwner", params.studioOwner);
  return request<AdminUserCenter>(`/api/admin/users${query.toString() ? `?${query.toString()}` : ""}`);
}

export function updateAdminUserStatus(userId: number, payload: { status: string; reason?: string }) {
  return request(`/api/admin/users/${userId}/status`, { method: "POST", body: JSON.stringify(payload) });
}

export function loadAdminUserDetail(userId: number) {
  return request<AdminUserDetail>(`/api/admin/users/${userId}`);
}

export function resetAdminUserPassword(userId: number, payload: { password: string }) {
  return request(`/api/admin/users/${userId}/reset-password`, { method: "POST", body: JSON.stringify(payload) });
}

export function loadAdminRealNameReviews(status?: string) {
  const query = new URLSearchParams();
  if (status) query.set("status", status);
  return request<AdminRealNameCenter>(`/api/admin/real-name/reviews${query.toString() ? `?${query.toString()}` : ""}`);
}

export function reviewAdminRealName(userId: number, payload: { action: string; reason?: string }) {
  return request(`/api/admin/real-name/reviews/${userId}`, { method: "POST", body: JSON.stringify(payload) });
}

export function loadAdminRoles() {
  return request<AdminRoleCenter>("/api/admin/roles");
}

export function saveAdminRole(payload: {
  roleId?: number;
  roleCode: string;
  roleName: string;
  description?: string;
  permissions: string[];
  status: string;
}) {
  return request("/api/admin/roles", { method: "POST", body: JSON.stringify(payload) });
}

export function assignAdminRoleMembers(roleId: number, userIds: number[]) {
  return request(`/api/admin/roles/${roleId}/members`, { method: "POST", body: JSON.stringify({ userIds }) });
}

export function loadAdminImWorkbench(params?: { keyword?: string; sceneCode?: string }) {
  const query = new URLSearchParams();
  if (params?.keyword) query.set("keyword", params.keyword);
  if (params?.sceneCode) query.set("sceneCode", params.sceneCode);
  return request<AdminImWorkbench>(`/api/admin/im/conversations${query.toString() ? `?${query.toString()}` : ""}`);
}

export function loadAdminImConversation(conversationNo: string) {
  return request<AdminImConversation>(`/api/admin/im/conversations/${encodeURIComponent(conversationNo)}`);
}

export function sendAdminImMessage(conversationNo: string, payload: { text?: string; fileKey?: string; fileName?: string }) {
  return request<AdminImConversation>(`/api/admin/im/conversations/${encodeURIComponent(conversationNo)}/messages`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function loadAdminIntegrationConfigs() {
  return request<AdminIntegrationConfigCenter>("/api/admin/integration-configs");
}

export function saveAdminPaymentConfig(payload: AdminPaymentConfig) {
  return request("/api/admin/integration-configs/payment", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function saveAdminLoginConfig(payload: AdminLoginConfig) {
  return request("/api/admin/integration-configs/login", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function saveAdminDistributionConfig(payload: AdminDistributionConfig) {
  return request("/api/admin/integration-configs/distribution", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function saveAdminListingPublishConfig(payload: AdminListingPublishConfig) {
  return request("/api/admin/integration-configs/listing-publish", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}
