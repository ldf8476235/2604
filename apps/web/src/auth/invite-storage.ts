const INVITE_CODE_KEY = "delta_trade_invite_code";

export function loadPendingInviteCode() {
  if (typeof window === "undefined") {
    return "";
  }
  return window.localStorage.getItem(INVITE_CODE_KEY) ?? "";
}

export function persistPendingInviteCode(inviteCode: string) {
  if (typeof window === "undefined") {
    return;
  }
  const next = inviteCode.trim();
  if (!next) {
    window.localStorage.removeItem(INVITE_CODE_KEY);
    return;
  }
  window.localStorage.setItem(INVITE_CODE_KEY, next);
}

export function clearPendingInviteCode() {
  if (typeof window === "undefined") {
    return;
  }
  window.localStorage.removeItem(INVITE_CODE_KEY);
}
