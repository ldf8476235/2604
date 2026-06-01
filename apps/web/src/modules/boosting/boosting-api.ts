import { request } from "../../lib/http-client";

export type BoostingOption = {
  value: string;
  label: string;
};

export type BoostingHallMeta = {
  categories: BoostingOption[];
  cycleOptions: BoostingOption[];
  sortOptions: BoostingOption[];
};

export type BoostingServiceCard = {
  serviceNo: string;
  categoryCode: string;
  categoryLabel: string;
  name: string;
  description: string;
  price: number;
  cycleLabel: string;
  guaranteeNote: string;
  providerLabel: string;
  salesCount: number;
};

export type BoostingHallResponse = {
  category: string;
  cycle: string;
  sort: string;
  rows: BoostingServiceCard[];
};

export type BoostingServiceDetail = BoostingServiceCard & {
  cycleCode: string;
  providerType: string;
  notices: string[];
};

export type BoostingCreateOrderPayload = {
  serviceNo: string;
  gameRegion: string;
  accountName: string;
  accountPassword: string;
  characterName: string;
  specialRequirement: string;
  paymentMethod: "ALIPAY" | "WECHAT";
  agreementCode: string;
};

export type BoostingCreateOrderResult = {
  orderNo: string;
  status: string;
  message: string;
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
  paymentMethod: "WECHAT";
  tradeType: "NATIVE" | "JSAPI";
  codeUrl: string;
  expireAt: string;
  jsapiPayParams: WechatJsapiPayParams | null;
};

export type BoostingOrderStatus =
  | "ALL"
  | "PENDING_PAYMENT"
  | "WAITING_SERVICE"
  | "IN_SERVICE"
  | "COMPLETED"
  | "AFTER_SALE"
  | "CANCELED";

export type BoostingOrderRange = "ALL" | "D7" | "D30" | "CUSTOM";

export type BoostingOrderCounts = {
  total: number;
  pendingPayment: number;
  waitingService: number;
  inService: number;
  completed: number;
  afterSale: number;
  canceled: number;
};

export type BoostingOrderListItem = {
  orderNo: string;
  serviceName: string;
  price: number;
  createdAt: string;
  statusLabel: string;
  statusCode: BoostingOrderStatus;
  progressPercent: number;
  progressSummary: string;
  chatGroupNo: string;
  canPay: boolean;
  canApplyAfterSale: boolean;
  canContactService: boolean;
  canConfirmComplete: boolean;
};

export type BoostingOrderCenterResponse = {
  status: BoostingOrderStatus;
  range: BoostingOrderRange;
  counts: BoostingOrderCounts;
  rows: BoostingOrderListItem[];
};

export type BoostingProgressItem = {
  title: string;
  content: string;
  progressPercent: number;
  createdAt: string;
  createdBy: string;
};

export type BoostingOrderDetail = {
  orderNo: string;
  serviceNo: string;
  serviceName: string;
  serviceCategory: string;
  serviceDescription: string;
  price: number;
  cycleLabel: string;
  guaranteeNote: string;
  providerLabel: string;
  statusLabel: string;
  statusCode: BoostingOrderStatus;
  paymentMethod: string;
  createdAt: string;
  paidAt: string;
  completedAt: string;
  gameRegion: string;
  accountName: string;
  maskedPassword: string;
  characterName: string;
  specialRequirement: string;
  progressPercent: number;
  progressSummary: string;
  chatGroupNo: string;
  canPay: boolean;
  canApplyAfterSale: boolean;
  canContactService: boolean;
  canConfirmComplete: boolean;
  afterSaleReason: string | null;
  afterSaleProofUrl: string;
  progressLogs: BoostingProgressItem[];
};

export function loadBoostingHallMeta() {
  return request<BoostingHallMeta>("/api/public/boosting/services/meta", { auth: false });
}

export function loadBoostingHall(params: {
  category?: string;
  minPrice?: string;
  maxPrice?: string;
  cycle?: string;
  sort?: string;
}) {
  const query = new URLSearchParams();
  if (params.category && params.category !== "ALL") query.set("category", params.category);
  if (params.minPrice) query.set("minPrice", params.minPrice);
  if (params.maxPrice) query.set("maxPrice", params.maxPrice);
  if (params.cycle && params.cycle !== "ALL") query.set("cycle", params.cycle);
  if (params.sort) query.set("sort", params.sort);
  const suffix = query.toString() ? `?${query.toString()}` : "";
  return request<BoostingHallResponse>(`/api/public/boosting/services${suffix}`, { auth: false });
}

export function loadBoostingServiceDetail(serviceNo: string) {
  return request<BoostingServiceDetail>(`/api/public/boosting/services/${encodeURIComponent(serviceNo)}`, { auth: false });
}

export function createBoostingOrder(payload: BoostingCreateOrderPayload) {
  return request<BoostingCreateOrderResult>("/api/boosting/orders", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function loadBoostingOrders(params: {
  status: BoostingOrderStatus;
  range: BoostingOrderRange;
  startDate?: string;
  endDate?: string;
}) {
  const query = new URLSearchParams();
  query.set("status", params.status);
  query.set("range", params.range);
  if (params.startDate) query.set("startDate", params.startDate);
  if (params.endDate) query.set("endDate", params.endDate);
  return request<BoostingOrderCenterResponse>(`/api/boosting/orders?${query.toString()}`);
}

export function loadBoostingOrderDetail(orderNo: string) {
  return request<BoostingOrderDetail>(`/api/boosting/orders/${encodeURIComponent(orderNo)}`);
}

export function payBoostingOrder(orderNo: string, paymentMethod: "ALIPAY" | "WECHAT") {
  return request<BoostingOrderDetail>(`/api/boosting/orders/${encodeURIComponent(orderNo)}/pay`, {
    method: "POST",
    body: JSON.stringify({ paymentMethod }),
  });
}

export function confirmBoostingOrderComplete(orderNo: string) {
  return request<BoostingOrderDetail>(`/api/boosting/orders/${encodeURIComponent(orderNo)}/confirm-complete`, {
    method: "POST",
    body: JSON.stringify({}),
  });
}

export function createBoostingWechatPayment(orderNo: string, tradeType: "NATIVE" | "JSAPI") {
  return request<Omit<WechatPayPayload, "paymentMethod">>(`/api/boosting/orders/${encodeURIComponent(orderNo)}/wechat-pay`, {
    method: "POST",
    body: JSON.stringify({ tradeType }),
  }).then((payload) => ({
    ...payload,
    paymentMethod: "WECHAT" as const,
  }));
}

export function applyBoostingAfterSale(orderNo: string, reason: string, proofKey: string) {
  return request<BoostingOrderDetail>(`/api/boosting/orders/${encodeURIComponent(orderNo)}/after-sale`, {
    method: "POST",
    body: JSON.stringify({ reason, proofKey }),
  });
}
