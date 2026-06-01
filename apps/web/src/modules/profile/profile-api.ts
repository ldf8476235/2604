import { request } from "../../lib/http-client";

export type UserOrderSummary = {
  orderNo: string;
  status: string;
  totalAmount: number;
};

export type OrderCenterRole = "BUY" | "SELL";
export type OrderCenterStatus =
  | "ALL"
  | "PENDING_PAYMENT"
  | "WAITING_TRADE"
  | "IN_PROGRESS"
  | "COMPLETED"
  | "REFUND_PENDING"
  | "AFTER_SALE"
  | "REFUNDED"
  | "CLOSED";
export type OrderCenterRange = "ALL" | "D7" | "D30" | "CUSTOM";

export type OrderCounts = {
  total: number;
  pendingPayment: number;
  waitingTrade: number;
  inProgress: number;
  completed: number;
  refundPending: number;
  afterSale: number;
  refunded: number;
  closed: number;
};

export type OrderListItem = {
  orderNo: string;
  title: string;
  summary: string;
  coverUrl: string;
  totalAmount: number;
  tradeTime: string;
  statusLabel: string;
  statusCode: OrderCenterStatus;
  buyerNickname: string;
  sellerNickname: string;
  sellerType: string;
  sellerDisplayName: string;
  chatGroupNo: string;
  canCancel: boolean;
  canApplyAfterSale: boolean;
  canApplyRefund: boolean;
  canReviewRefund: boolean;
  canDelete: boolean;
  canEnterChat: boolean;
  canDownloadCertificate: boolean;
  canConfirmComplete: boolean;
  paymentExpireAt: string | null;
};

export type OrderCenterResponse = {
  role: OrderCenterRole;
  status: OrderCenterStatus;
  range: OrderCenterRange;
  counts: OrderCounts;
  rows: OrderListItem[];
};

export type OrderProgressStep = {
  title: string;
  description: string;
  done: boolean;
  current: boolean;
};

export type OrderDetail = {
  orderNo: string;
  role: OrderCenterRole;
  listingNo: string;
  title: string;
  summary: string;
  coverUrl: string;
  statusLabel: string;
  statusCode: OrderCenterStatus;
  createdAt: string;
  paidAt: string;
  completedAt: string;
  paymentMethod: string;
  itemAmount: number;
  serviceFee: number;
  totalAmount: number;
  depositAmount: number;
  buyerConfirmed: boolean;
  sellerConfirmed: boolean;
  buyerNickname: string;
  sellerNickname: string;
  sellerType: string;
  sellerDisplayName: string;
  canCancel: boolean;
  canApplyAfterSale: boolean;
  canApplyRefund: boolean;
  canReviewRefund: boolean;
  canDelete: boolean;
  canEnterChat: boolean;
  chatGroupNo: string;
  canConfirmComplete: boolean;
  paymentExpireAt: string | null;
  guaranteeNote: string;
  violationNote: string;
  progress: OrderProgressStep[];
};

export type OrderCertificate = {
  filename: string;
  content: string;
};

export type WechatJsapiPayParams = {
  appId: string;
  timeStamp: string;
  nonceStr: string;
  packageValue: string;
  signType: string;
  paySign: string;
};

export type WechatPayPayload = {
  orderNo: string;
  paymentMethod: "WECHAT" | "ALIPAY";
  tradeType: "NATIVE" | "JSAPI";
  codeUrl: string;
  expireAt: string;
  jsapiPayParams: WechatJsapiPayParams | null;
};

export type WalletRechargeStatus = {
  rechargeNo: string;
  amount: number;
  status: "PENDING_PAYMENT" | "SUCCESS" | "CLOSED" | string;
  paidAt: string | null;
  paid: boolean;
};

export type WalletChannel = "ALIPAY" | "WECHAT";

export type WithdrawAccountView = {
  channel: WalletChannel;
  accountName: string;
  maskedAccountNo: string;
  qrCodeUrl: string | null;
};

export type WalletRecord = {
  id: string;
  title: string;
  amount: string;
  status: string;
  time: string;
  type: string;
  channel: string | null;
  referenceNo: string | null;
};

export type WalletOverview = {
  availableBalance: number;
  frozenBalance: number;
  totalCommission: number;
  withdrawAccount: WithdrawAccountView | null;
  records: WalletRecord[];
};

export type MessageCategory = "ALL" | "SYSTEM" | "TRADE" | "SERVICE" | "DISTRIBUTION";

export type MessageCounts = {
  all: number;
  unread: number;
  system: number;
  trade: number;
  service: number;
  distribution: number;
};

export type MessageItem = {
  id: number;
  category: MessageCategory;
  categoryLabel: string;
  title: string;
  content: string;
  time: string;
  unread: boolean;
};

export type MessageCenter = {
  counts: MessageCounts;
  rows: MessageItem[];
};

export type NotificationSummary = {
  totalUnread: number;
  messageUnread: number;
  imUnread: number;
  latest: NotificationItem | null;
};

export type NotificationItem = {
  id: string;
  type: "MESSAGE" | "IM";
  label: string;
  title: string;
  content: string;
  time: string;
  targetUrl: string;
  conversationNo: string | null;
  messageId: number | null;
};

export type CouponRecord = {
  id: string;
  couponNo: string | null;
  name: string;
  amount: string;
  condition: string;
  expireAt: string;
  scope: string;
  status: "available" | "used" | "expired" | "void";
  orderNo?: string | null;
  usedAt?: string | null;
};

export type CouponCenterResponse = {
  availableCount: number;
  historyCount: number;
  rows: CouponRecord[];
};

export type OfficialAnnouncementRecord = {
  announcementNo: string;
  title: string;
  content: string;
  summary: string;
  category: string;
  categoryText: string;
  pinned: boolean;
  publishAt: string;
};

export type OfficialAnnouncementCenterResponse = {
  total: number;
  rows: OfficialAnnouncementRecord[];
};

export type DistributionFanRecord = {
  id: string;
  nickname: string;
  phone: string;
  status: string;
  registeredAt: string;
  effectiveAt: string;
  firstOrderNo: string;
};

export type DistributionOrderRecord = {
  id: string;
  orderNo: string;
  nickname: string;
  orderType: string;
  amount: string;
  commission: string;
  status: string;
  settledAt: string;
  createdAt: string;
};

export type DistributionCommissionRecord = {
  id: string;
  orderNo: string;
  amount: string;
  status: string;
  settledAt: string;
  createdAt: string;
};

export type DistributionCenterResponse = {
  enabled: boolean;
  lockedReason: string;
  inviteCode: string;
  invitePath: string;
  posterUrl: string;
  totalPromotedUsers: number;
  totalOrders: number;
  totalCommission: string;
  monthCommission: string;
  yesterdayCommission: string;
  todayEstimatedCommission: string;
  fans: DistributionFanRecord[];
  orderRows: DistributionOrderRecord[];
  commissionRows: DistributionCommissionRecord[];
};

export type DistributionInviteLinkResult = {
  inviteCode: string;
  invitePath: string;
  posterUrl: string;
};

export type PublicDistributionInvite = {
  active: boolean;
  promoterNickname: string;
  promoterPhone: string;
  inviteCode: string;
  invitePath: string;
  posterUrl: string;
  description: string;
};

export type SettingsProfile = {
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

export type StudioApplicationStatus = "NONE" | "PENDING" | "APPROVED" | "REJECTED";

export type StudioApplicationProfile = {
  applicationNo: string;
  status: StudioApplicationStatus;
  statusText: string;
  rejectReason: string;
  applicantUserId: number;
  applicantNickname: string;
  applicantPhone: string;
  studioId: number | null;
  studioName: string;
  qualificationCode: string;
  qualificationNote: string;
  contactName: string;
  contactPhone: string;
  qualificationMaterialKey: string;
  qualificationMaterialUrl: string | null;
  createdAt: string;
  reviewedAt: string;
  hasStudioAccess: boolean;
  reviewStrategy: string;
  cooperationStatus: string;
};

export type SimpleResult = {
  status: string;
  message: string;
};

export function loadMyOrders() {
  return request<UserOrderSummary[]>("/api/orders/mine");
}

export function loadOrderCenter(params: {
  role: OrderCenterRole;
  status: OrderCenterStatus;
  range: OrderCenterRange;
  startDate?: string;
  endDate?: string;
}) {
  const query = new URLSearchParams();
  query.set("role", params.role);
  query.set("status", params.status);
  query.set("range", params.range);
  if (params.startDate) {
    query.set("startDate", params.startDate);
  }
  if (params.endDate) {
    query.set("endDate", params.endDate);
  }
  return request<OrderCenterResponse>(`/api/orders/center?${query.toString()}`);
}

export function loadOrderDetail(orderNo: string) {
  return request<OrderDetail>(`/api/orders/${encodeURIComponent(orderNo)}`);
}

export function cancelOrder(orderNo: string) {
  return request<OrderDetail>(`/api/orders/${encodeURIComponent(orderNo)}/cancel`, {
    method: "POST",
    body: JSON.stringify({}),
  });
}

export function deleteOrder(orderNo: string) {
  return request<{ orderNo: string; deleted: boolean }>(`/api/orders/${encodeURIComponent(orderNo)}`, {
    method: "DELETE",
  });
}

export function applyOrderRefund(orderNo: string, reason?: string) {
  return request<OrderDetail>(`/api/orders/${encodeURIComponent(orderNo)}/refund-requests`, {
    method: "POST",
    body: JSON.stringify({ reason: reason ?? "" }),
  });
}

export function reviewOrderRefund(orderNo: string, action: "APPROVE" | "REJECT", note: string) {
  return request<OrderDetail>(`/api/orders/${encodeURIComponent(orderNo)}/refund-review`, {
    method: "POST",
    body: JSON.stringify({ action, note }),
  });
}

export function applyOrderAfterSale(orderNo: string) {
  return request<OrderDetail>(`/api/orders/${encodeURIComponent(orderNo)}/after-sale`, {
    method: "POST",
    body: JSON.stringify({}),
  });
}

export function loadOrderCertificate(orderNo: string) {
  return request<OrderCertificate>(`/api/orders/${encodeURIComponent(orderNo)}/certificate`);
}

export function createTradeOrder(listingId: string, totalAmount: number, includeExtraItems = false) {
  return request<{ orderNo: string; status: string; paymentExpireAt: string }>("/api/orders", {
    method: "POST",
    body: JSON.stringify({ listingId, totalAmount, includeExtraItems }),
  });
}

export function createTradeWechatPayment(orderNo: string, tradeType: "NATIVE" | "JSAPI") {
  return request<WechatPayPayload>(`/api/orders/${encodeURIComponent(orderNo)}/wechat-pay`, {
    method: "POST",
    body: JSON.stringify({ tradeType }),
  });
}

export function createTradeAlipayPayment(orderNo: string) {
  return request<WechatPayPayload>(`/api/orders/${encodeURIComponent(orderNo)}/alipay`, {
    method: "POST",
    body: JSON.stringify({}),
  });
}

export function payTradeOrder(orderNo: string, paymentMethod: "WECHAT" | "ALIPAY") {
  return request<OrderDetail>(`/api/orders/${encodeURIComponent(orderNo)}/pay`, {
    method: "POST",
    body: JSON.stringify({ paymentMethod }),
  });
}

export function confirmTradeOrder(orderNo: string) {
  return request<OrderDetail>(`/api/orders/${encodeURIComponent(orderNo)}/confirm-complete`, {
    method: "POST",
    body: JSON.stringify({}),
  });
}

export function loadWalletOverview() {
  return request<WalletOverview>("/api/profile/wallet");
}

export function rechargeWallet(amount: number, channel: WalletChannel) {
  return request<WalletOverview>("/api/profile/wallet/recharge", {
    method: "POST",
    body: JSON.stringify({ amount, channel }),
  });
}

export function createWalletWechatRecharge(amount: number, tradeType: "NATIVE" | "JSAPI") {
  return request<WechatPayPayload>("/api/profile/wallet/wechat-recharge", {
    method: "POST",
    body: JSON.stringify({ amount, tradeType }),
  });
}

export function loadWalletRechargeStatus(rechargeNo: string) {
  return request<WalletRechargeStatus>(`/api/profile/wallet/recharge-orders/${encodeURIComponent(rechargeNo)}`);
}

export function bindWithdrawAccount(channel: WalletChannel, accountName: string, accountNo: string, qrCodeKey?: string | null) {
  return request<WithdrawAccountView>("/api/profile/wallet/withdraw-account", {
    method: "POST",
    body: JSON.stringify({ channel, accountName, accountNo, qrCodeKey }),
  });
}

export function applyWithdraw(amount: number) {
  return request<WalletOverview>("/api/profile/wallet/withdraw", {
    method: "POST",
    body: JSON.stringify({ amount }),
  });
}

export function loadMessageCenter(category: MessageCategory = "ALL") {
  const query = category === "ALL" ? "" : `?category=${encodeURIComponent(category)}`;
  return request<MessageCenter>(`/api/profile/messages${query}`);
}

export function loadNotificationSummary() {
  return request<NotificationSummary>("/api/profile/notifications/summary");
}

export function loadCouponCenter() {
  return request<CouponCenterResponse>("/api/profile/coupons");
}

export function loadPublicAnnouncements(limit = 40) {
  return request<OfficialAnnouncementCenterResponse>(`/api/public/announcements?limit=${encodeURIComponent(String(limit))}`, {
    auth: false,
  });
}

export function loadDistributionCenter(params?: {
  keyword?: string;
  range?: "ALL" | "D7" | "D30" | "CUSTOM";
  startDate?: string;
  endDate?: string;
}) {
  const query = new URLSearchParams();
  if (params?.keyword) query.set("keyword", params.keyword);
  if (params?.range) query.set("range", params.range);
  if (params?.startDate) query.set("startDate", params.startDate);
  if (params?.endDate) query.set("endDate", params.endDate);
  const suffix = query.toString() ? `?${query.toString()}` : "";
  return request<DistributionCenterResponse>(`/api/profile/distribution${suffix}`);
}

export function generateDistributionInviteLink(regenerate = false) {
  return request<DistributionInviteLinkResult>("/api/profile/distribution/link", {
    method: "POST",
    body: JSON.stringify({ regenerate }),
  });
}

export function loadPublicDistributionInvite(inviteCode: string) {
  return request<PublicDistributionInvite>(`/api/public/distribution/invite/${encodeURIComponent(inviteCode)}`, {
    auth: false,
  });
}

export function markMessagesRead(messageIds: number[], category: MessageCategory) {
  return request<MessageCenter>("/api/profile/messages/read", {
    method: "POST",
    body: JSON.stringify({ messageIds, category }),
  });
}

export function markAllMessagesRead(category: MessageCategory) {
  return request<MessageCenter>("/api/profile/messages/read-all", {
    method: "POST",
    body: JSON.stringify({ category }),
  });
}

export function deleteMessages(messageIds: number[], category: MessageCategory) {
  return request<MessageCenter>("/api/profile/messages/delete", {
    method: "POST",
    body: JSON.stringify({ messageIds, category }),
  });
}

export function loadSettingsProfile() {
  return request<SettingsProfile>("/api/auth/settings");
}

export function updateNickname(nickname: string) {
  return request<SettingsProfile>("/api/auth/settings/nickname", {
    method: "POST",
    body: JSON.stringify({ nickname }),
  });
}

export function updateAvatar(avatarKey: string) {
  return request<SettingsProfile>("/api/auth/settings/avatar", {
    method: "POST",
    body: JSON.stringify({ avatarKey }),
  });
}

export function changePassword(currentPassword: string, nextPassword: string, confirmPassword: string) {
  return request<SimpleResult>("/api/auth/settings/password", {
    method: "POST",
    body: JSON.stringify({ currentPassword, nextPassword, confirmPassword }),
  });
}

export function changeBoundPhone(phone: string, code: string) {
  return request<SettingsProfile>("/api/auth/settings/phone", {
    method: "POST",
    body: JSON.stringify({ phone, code }),
  });
}

export function unbindBoundPhone(code: string) {
  return request<SettingsProfile>("/api/auth/settings/phone/unbind", {
    method: "POST",
    body: JSON.stringify({ code }),
  });
}

export function updateSecuritySettings(loginAlertEnabled: boolean, secondaryVerifyEnabled: boolean) {
  return request<SettingsProfile>("/api/auth/settings/security", {
    method: "POST",
    body: JSON.stringify({ loginAlertEnabled, secondaryVerifyEnabled }),
  });
}

export function unbindWechat() {
  return request<SettingsProfile>("/api/auth/settings/wechat/unbind", {
    method: "POST",
    body: JSON.stringify({}),
  });
}

export function loadStudioApplication() {
  return request<StudioApplicationProfile>("/api/profile/studio/application");
}

export function submitStudioApplication(payload: {
  studioName: string;
  qualificationCode: string;
  qualificationNote?: string;
  contactName: string;
  contactPhone: string;
  qualificationMaterialKey: string;
}) {
  return request<StudioApplicationProfile>("/api/profile/studio/application", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}
