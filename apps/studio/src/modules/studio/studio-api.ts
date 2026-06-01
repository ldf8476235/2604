import { request } from "../../lib/request";

export type MetricItem = { label: string; value: string; trend: string };

export type StudioDashboard = {
  metrics: MetricItem[];
  profile: {
    studioId: number;
    studioName: string;
    ownerUserId: number;
    reviewStrategy: string;
    reviewStrategyText: string;
    shareRatio: string;
    active: boolean;
    activeText: string;
  };
  recentListings: Array<{ listingNo: string; title: string; statusText: string; price: string; updatedAt: string }>;
  recentOrders: Array<{ orderNo: string; listingTitle: string; statusText: string; totalAmount: string; createdAt: string }>;
  boostingServices: Array<{ serviceNo: string; name: string; statusText: string; price: string; salesCount: number }>;
  announcements: Array<{ announcementNo: string; title: string; content: string; categoryText: string; pinned: boolean; publishAt: string }>;
};

export type StudioConsoleSession = {
  userId: number;
  nickname: string;
  phone: string;
  studioId: number;
  studioName: string;
  reviewStrategy: string;
  active: boolean;
};

export type StudioListingCenter = {
  strategy: {
    reviewStrategy: string;
    reviewStrategyText: string;
    strategyTip: string;
  };
  rows: Array<{
    listingNo: string;
    title: string;
    status: string;
    statusText: string;
    reviewProgress: string;
    price: string;
    viewCount: number;
    favoriteCount: number;
    salesCount: number;
    cityName: string;
    coverUrl: string | null;
    updatedAt: string;
    rejectionReason: string;
    canEdit: boolean;
    canWithdraw: boolean;
    canResubmit: boolean;
  }>;
  summary: Record<string, number>;
};

export type StudioListingDetail = {
  summary: {
    listingNo: string;
    title: string;
    status: string;
    statusText: string;
    price: string;
    coverUrl: string | null;
    imageUrls: string[];
    videoUrl: string | null;
    description: string;
    estimateDetail: string;
    updatedAt: string;
    submittedAt: string;
    publishedAt: string;
    rejectionReason: string;
  };
  reviewRecords: Array<{ label: string; status: string; time: string; remark: string }>;
  tradeRecords: Array<{ orderNo: string; buyerNickname: string; status: string; totalAmount: string; createdAt: string; completedAt: string }>;
};

export type StudioOrders = {
  rows: Array<{
    orderNo: string;
    listingNo: string;
    listingTitle: string;
    buyerNickname: string;
    status: string;
    statusText: string;
    paymentMethod: string;
    itemAmount: string;
    serviceFee: string;
    totalAmount: string;
    chatGroupNo: string;
    createdAt: string;
    paidAt: string;
    completedAt: string;
  }>;
  summary: Record<string, number>;
};

export type StudioOrderDetail = {
  orderNo: string;
  listingNo: string;
  listingTitle: string;
  listingSummary: string;
  listingCoverUrl: string | null;
  buyerNickname: string;
  sellerDisplayName: string;
  sellerTypeText: string;
  status: string;
  statusText: string;
  paymentMethod: string;
  itemAmount: string;
  serviceFee: string;
  totalAmount: string;
  chatGroupNo: string;
  createdAt: string;
  paidAt: string;
  completedAt: string;
  afterSaleAt: string;
  afterSaleNote: string;
  afterSaleProofKey: string;
  afterSaleProofUrl: string | null;
  afterSaleHandledAt: string;
  refundRequestedAt: string;
  refundReviewedAt: string;
  refundedAt: string;
  refundAmount: string;
  refundReason: string;
  refundReviewNote: string;
  canReviewRefund: boolean;
};

export type StudioOperatorCenter = {
  rows: Array<{
    operatorId: number;
    operatorNo: string;
    name: string;
    phoneRaw: string;
    phone: string;
    status: string;
    statusText: string;
    permissions: string[];
    permissionText: string;
    updatedAt: string;
  }>;
  summary: {
    total: number;
    active: number;
    disabled: number;
  };
  studio: {
    studioId: number;
    studioName: string;
  };
};

export type StudioSettlements = {
  rows: Array<{
    orderNo: string;
    listingTitle: string;
    buyerNickname: string;
    grossAmount: string;
    itemAmount: string;
    shareRatio: string;
    shareAmount: string;
    settlementStatus: string;
    statusText: string;
    completedAt: string;
    createdAt: string;
  }>;
  summary: {
    settledTotal: string;
    pendingTotal: string;
    orderCount: number;
  };
};

export type StudioProfile = {
  studioId: number;
  studioName: string;
  description: string;
  contactPhone: string;
  contactWechat: string;
  reviewStrategy: string;
  reviewStrategyText: string;
  shareRatio: string;
  active: boolean;
  activeText: string;
  ownerNickname: string;
  ownerPhone: string;
  avatarUrl: string | null;
  verified: boolean;
  realName: string | null;
};

export type StudioSettingsProfile = {
  nickname: string;
  phone: string;
  phoneBound: boolean;
  wechatBound: boolean;
  verified: boolean;
  realNameStatus: "UNVERIFIED" | "APPROVED" | "REJECTED";
  realName: string;
  maskedIdCardNo: string;
  realNameRejectReason: string;
  avatarUrl: string;
  idCardFrontUrl: string;
  idCardBackUrl: string;
  loginAlertEnabled: boolean;
  secondaryVerifyEnabled: boolean;
  withdrawAccountLabel: string;
};

export type SmsCodeResult = {
  phone: string;
  scene: string;
  expireAt: string;
  cooldownSeconds: number;
  hint: string;
};

export type SimpleResult = {
  status: string;
  message: string;
};

export type UploadedStudioAsset = {
  objectKey: string;
  previewUrl: string;
};

export type StudioFinance = {
  account: null | {
    channel: string;
    channelText: string;
    accountName: string;
    accountNo: string;
  };
  summary: {
    studioId: number;
    studioName: string;
    settledTotal: string;
    pendingTotal: string;
    withdrawPendingTotal: string;
    withdrawPaidTotal: string;
    withdrawableTotal: string;
  };
  withdraws: Array<{
    applicationNo: string;
    amount: string;
    channel: string;
    channelText: string;
    accountName: string;
    accountNo: string;
    status: string;
    statusText: string;
    rejectReason: string;
    createdAt: string;
    reviewedAt: string;
    paidAt: string;
  }>;
  statements: StudioSettlements["rows"];
};

export function loadStudioDashboard() {
  return request<StudioDashboard>("/api/studio/dashboard");
}

export function loadStudioSession() {
  return request<StudioConsoleSession>("/api/studio/session");
}

export function loadStudioListings(status?: string) {
  const query = new URLSearchParams();
  if (status) query.set("status", status);
  return request<StudioListingCenter>(`/api/studio/listings${query.toString() ? `?${query.toString()}` : ""}`);
}

export function loadStudioListingDetail(listingNo: string) {
  return request<StudioListingDetail>(`/api/studio/listings/${listingNo}`);
}

export function withdrawStudioListing(listingNo: string) {
  return request(`/api/studio/listings/${listingNo}/withdraw`, { method: "POST" });
}

export function resubmitStudioListing(listingNo: string) {
  return request(`/api/studio/listings/${listingNo}/resubmit`, { method: "POST" });
}

export function loadStudioOrders(status?: string) {
  const query = new URLSearchParams();
  if (status) query.set("status", status);
  return request<StudioOrders>(`/api/studio/orders${query.toString() ? `?${query.toString()}` : ""}`);
}

export function loadStudioOrderDetail(orderNo: string) {
  return request<StudioOrderDetail>(`/api/studio/orders/${orderNo}`);
}

export function handleStudioAfterSale(orderNo: string, payload: { action: "RESOLVE" | "CLOSE"; note: string; proofKey?: string }) {
  return request(`/api/studio/orders/${orderNo}/after-sale`, { method: "POST", body: JSON.stringify(payload) });
}

export function reviewStudioRefund(orderNo: string, payload: { action: "APPROVE" | "REJECT"; note: string }) {
  return request(`/api/studio/orders/${orderNo}/refund-review`, { method: "POST", body: JSON.stringify(payload) });
}

export function loadStudioOperators(status?: string, keyword?: string) {
  const query = new URLSearchParams();
  if (status) query.set("status", status);
  if (keyword) query.set("keyword", keyword);
  return request<StudioOperatorCenter>(`/api/studio/operators${query.toString() ? `?${query.toString()}` : ""}`);
}

export function saveStudioOperator(payload: {
  operatorId?: number;
  name: string;
  phone: string;
  permissions: string[];
  password?: string;
}) {
  return request("/api/studio/operators", { method: "POST", body: JSON.stringify(payload) });
}

export function updateStudioOperatorStatus(operatorId: number, status: "ACTIVE" | "DISABLED") {
  return request(`/api/studio/operators/${operatorId}/status`, {
    method: "POST",
    body: JSON.stringify({ status }),
  });
}

export function resetStudioOperatorPassword(operatorId: number, password: string) {
  return request(`/api/studio/operators/${operatorId}/reset-password`, {
    method: "POST",
    body: JSON.stringify({ password }),
  });
}

export function loadStudioSettlements(range?: string) {
  const query = new URLSearchParams();
  if (range) query.set("range", range);
  return request<StudioSettlements>(`/api/studio/settlements${query.toString() ? `?${query.toString()}` : ""}`);
}

export function loadStudioProfile() {
  return request<StudioProfile>("/api/studio/profile");
}

export function saveStudioProfile(payload: {
  studioName: string;
  description?: string;
  contactPhone: string;
  contactWechat?: string;
}) {
  return request("/api/studio/profile", { method: "POST", body: JSON.stringify(payload) });
}

export function uploadStudioAsset(file: File, businessScope: string) {
  const formData = new FormData();
  formData.append("businessScope", businessScope);
  formData.append("file", file, file.name);
  return request<UploadedStudioAsset>("/api/oss/upload", {
    method: "POST",
    body: formData,
  });
}

export function updateStudioAvatar(avatarKey: string) {
  return request("/api/auth/settings/avatar", {
    method: "POST",
    body: JSON.stringify({ avatarKey }),
  });
}

export function sendStudioSmsCode(phone: string, scene: "BIND_PHONE" | "SECURITY_VERIFY") {
  return request<SmsCodeResult>("/api/auth/sms-code", {
    method: "POST",
    body: JSON.stringify({ phone, scene }),
  });
}

export function loadStudioSettings() {
  return request<StudioSettingsProfile>("/api/auth/settings");
}

export function changeStudioPassword(currentPassword: string, nextPassword: string, confirmPassword: string) {
  return request<SimpleResult>("/api/auth/settings/password", {
    method: "POST",
    body: JSON.stringify({ currentPassword, nextPassword, confirmPassword }),
  });
}

export function changeStudioBoundPhone(phone: string, code: string) {
  return request<StudioSettingsProfile>("/api/auth/settings/phone", {
    method: "POST",
    body: JSON.stringify({ phone, code }),
  });
}

export function unbindStudioBoundPhone(code: string) {
  return request<StudioSettingsProfile>("/api/auth/settings/phone/unbind", {
    method: "POST",
    body: JSON.stringify({ code }),
  });
}

export function updateStudioSecuritySettings(loginAlertEnabled: boolean, secondaryVerifyEnabled: boolean) {
  return request<StudioSettingsProfile>("/api/auth/settings/security", {
    method: "POST",
    body: JSON.stringify({ loginAlertEnabled, secondaryVerifyEnabled }),
  });
}

export function unbindStudioWechat() {
  return request<StudioSettingsProfile>("/api/auth/settings/wechat/unbind", {
    method: "POST",
    body: JSON.stringify({}),
  });
}

export function loadStudioFinance(range?: string) {
  const query = new URLSearchParams();
  if (range) query.set("range", range);
  return request<StudioFinance>(`/api/studio/finance${query.toString() ? `?${query.toString()}` : ""}`);
}

export function saveStudioPayoutAccount(payload: { channel: string; accountName: string; accountNo: string }) {
  return request("/api/studio/finance/payout-account", { method: "POST", body: JSON.stringify(payload) });
}

export function applyStudioWithdraw(amount: string) {
  return request("/api/studio/finance/withdraw", { method: "POST", body: JSON.stringify({ amount }) });
}

export function downloadStudioStatement(range?: string) {
  const query = new URLSearchParams();
  if (range) query.set("range", range);
  return request<{ fileName: string; content: string; rowCount: number }>(`/api/studio/finance/statement${query.toString() ? `?${query.toString()}` : ""}`);
}
