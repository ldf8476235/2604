import { request } from "../../lib/http-client";

export type ImSummaryItem = {
  label: string;
  value: string;
};

export type ImMessage = {
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

export type ImConversation = {
  conversationNo: string;
  sceneCode: string;
  sceneLabel: string;
  sourceOrderNo: string;
  listingNo: string;
  title: string;
  description: string;
  statusLabel: string;
  supportDisplayName: string;
  currentUserRole: string;
  currentUserName: string;
  currentUserAvatarUrl: string | null;
  participants: Array<{
    roleCode: string;
    roleLabel: string;
    displayName: string;
    avatarUrl: string | null;
    currentUser: boolean;
  }>;
  summaryItems: ImSummaryItem[];
  messages: ImMessage[];
};

export type ImReadResult = {
  conversationNo: string;
  lastReadMessageId: number;
};

export type LatestImConversation = {
  conversationNo: string | null;
};

export type ImWorkbenchConversationItem = {
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
};

export type ImWorkbenchResponse = {
  supportDisplayName: string;
  supportAvatarUrl: string | null;
  rows: ImWorkbenchConversationItem[];
  totalCount: number;
  boostingCount: number;
  tradeCount: number;
};

export type ImConversationRefreshEvent = {
  conversationNo: string;
  sourceOrderNo: string;
  sceneCode: string;
  latestMessageId: number;
  reason: string;
  lastMessageAt: string;
};

export type ImWorkbenchRefreshEvent = {
  conversationNo: string;
  sceneCode: string;
  sourceOrderNo: string;
  title: string;
  lastMessageExcerpt: string;
  lastMessageAt: string;
  statusLabel: string;
  reason: string;
};

export function loadConversation(conversationNo: string) {
  return request<ImConversation>(`/api/im/conversations/${conversationNo}`);
}

export function loadLatestConversation() {
  return request<LatestImConversation>("/api/im/conversations/latest");
}

export function sendConversationMessage(
  conversationNo: string,
  payload: { text?: string; fileKey?: string; fileName?: string },
) {
  return request<ImConversation>(`/api/im/conversations/${conversationNo}/messages`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function markConversationRead(conversationNo: string) {
  return request<ImReadResult>(`/api/im/conversations/${conversationNo}/read`, {
    method: "POST",
    body: JSON.stringify({}),
  });
}

export function createListingConsultation(listingNo: string, presetText?: string) {
  return request<ImConversation>(`/api/im/listings/${encodeURIComponent(listingNo)}/consultation`, {
    method: "POST",
    body: JSON.stringify({ presetText }),
  });
}

export function loadWorkbenchConversations(params?: { keyword?: string; sceneCode?: string }) {
  const query = new URLSearchParams();
  if (params?.keyword) {
    query.set("keyword", params.keyword);
  }
  if (params?.sceneCode) {
    query.set("sceneCode", params.sceneCode);
  }
  const suffix = query.toString() ? `?${query}` : "";
  return request<ImWorkbenchResponse>(`/api/admin/im/conversations${suffix}`);
}

export function loadWorkbenchConversation(conversationNo: string) {
  return request<ImConversation>(`/api/admin/im/conversations/${conversationNo}`);
}

export function sendWorkbenchMessage(
  conversationNo: string,
  payload: { text?: string; fileKey?: string; fileName?: string },
) {
  return request<ImConversation>(`/api/admin/im/conversations/${conversationNo}/messages`, {
    method: "POST",
    body: JSON.stringify(payload),
  });
}
