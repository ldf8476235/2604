import type { AuthSession } from "./auth-storage";
import { request } from "../lib/http-client";

export type SmsScene = "LOGIN" | "REGISTER" | "RESET_PASSWORD" | "BIND_PHONE" | "SECURITY_VERIFY";

export type SmsCodeResult = {
  phone: string;
  scene: string;
  expireAt: string;
  cooldownSeconds: number;
  hint: string;
};

export type VerifyTicketResult = {
  verifyToken: string;
  expireAt: string;
};

export type SimpleResult = {
  status: string;
  message: string;
};

export type RealNameProfile = {
  verified: boolean;
  status: "UNVERIFIED" | "APPROVED" | "REJECTED";
  realName: string;
  phone: string;
  maskedIdCardNo: string;
  rejectReason: string;
  idCardFrontKey: string;
  idCardBackKey: string;
  idCardFrontUrl: string;
  idCardBackUrl: string;
};

export type WechatQrResult = {
  sceneId: string;
  qrCodeUrl: string;
  authorizeUrl: string;
  expireAt: string;
  pollIntervalSeconds: number;
};

export type WechatPollResult = {
  status: "WAITING" | "EXPIRED" | "AUTHORIZED_BOUND" | "AUTHORIZED_UNBOUND";
  loginResult: AuthSession | null;
  bindToken: string | null;
  maskedPhone: string | null;
};

export function sendSmsCode(phone: string, scene: SmsScene) {
  return request<SmsCodeResult>("/api/auth/sms-code", {
    method: "POST",
    body: JSON.stringify({ phone, scene }),
    auth: false,
  });
}

export function loginBySms(phone: string, code: string) {
  return request<AuthSession>("/api/auth/sms-login", {
    method: "POST",
    body: JSON.stringify({ phone, code }),
    auth: false,
  });
}

export function loginByPassword(phone: string, password: string) {
  return request<AuthSession>("/api/auth/password-login", {
    method: "POST",
    body: JSON.stringify({ phone, password }),
    auth: false,
  });
}

export function verifyRegisterCode(phone: string, code: string) {
  return request<VerifyTicketResult>("/api/auth/register/verify-code", {
    method: "POST",
    body: JSON.stringify({ phone, code }),
    auth: false,
  });
}

export function completeRegister(phone: string, verifyToken: string, password: string, confirmPassword: string, inviteCode?: string) {
  return request<AuthSession>("/api/auth/register/complete", {
    method: "POST",
    body: JSON.stringify({ phone, verifyToken, password, confirmPassword, inviteCode }),
    auth: false,
  });
}

export function verifyResetCode(phone: string, code: string) {
  return request<VerifyTicketResult>("/api/auth/password-reset/verify-code", {
    method: "POST",
    body: JSON.stringify({ phone, code }),
    auth: false,
  });
}

export function completeResetPassword(phone: string, verifyToken: string, password: string, confirmPassword: string) {
  return request<SimpleResult>("/api/auth/password-reset/complete", {
    method: "POST",
    body: JSON.stringify({ phone, verifyToken, password, confirmPassword }),
    auth: false,
  });
}

export function createWechatQr(payload?: { clientMode?: "QR" | "WECHAT_INTERNAL"; returnPath?: string }) {
  return request<WechatQrResult>("/api/auth/wechat/qr-code", {
    method: "POST",
    body: JSON.stringify(payload ?? {}),
    auth: false,
  });
}

export function pollWechatQr(sceneId: string) {
  return request<WechatPollResult>("/api/auth/wechat/poll", {
    method: "POST",
    body: JSON.stringify({ sceneId }),
    auth: false,
  });
}

export function bindWechatPhone(bindToken: string, phone: string, code: string, inviteCode?: string) {
  return request<AuthSession>("/api/auth/wechat/bind-phone", {
    method: "POST",
    body: JSON.stringify({ bindToken, phone, code, inviteCode }),
    auth: false,
  });
}

export function logout() {
  return request<SimpleResult>("/api/auth/logout", {
    method: "POST",
    skipUnauthorizedRedirect: true,
  });
}

export function loadRealNameProfile() {
  return request<RealNameProfile>("/api/auth/real-name");
}

export function submitRealName(realName: string, phone: string, idCardNo: string, idCardFrontKey: string, idCardBackKey: string) {
  return request<RealNameProfile>("/api/auth/real-name", {
    method: "POST",
    body: JSON.stringify({ realName, phone, idCardNo, idCardFrontKey, idCardBackKey }),
  });
}
