import { useEffect, useMemo, useState } from "react";
import { Button, MetricCard, SectionHeading, StatusState, SurfaceCard, Tag } from "@delta/ui";
import { useNavigate } from "react-router-dom";
import {
  applyStudioWithdraw,
  changeStudioBoundPhone,
  changeStudioPassword,
  downloadStudioStatement,
  handleStudioAfterSale,
  loadStudioFinance,
  loadStudioDashboard,
  loadStudioListingDetail,
  loadStudioOrderDetail,
  loadStudioListings,
  loadStudioOrders,
  loadStudioOperators,
  loadStudioProfile,
  loadStudioSettings,
  saveStudioProfile,
  saveStudioOperator,
  loadStudioSession,
  saveStudioPayoutAccount,
  resetStudioOperatorPassword,
  reviewStudioRefund,
  sendStudioSmsCode,
  unbindStudioBoundPhone,
  unbindStudioWechat,
  updateStudioAvatar,
  updateStudioOperatorStatus,
  updateStudioSecuritySettings,
  uploadStudioAsset,
  loadStudioSettlements,
  resubmitStudioListing,
  withdrawStudioListing,
  type StudioDashboard,
  type StudioFinance,
  type StudioConsoleSession,
  type StudioListingCenter,
  type StudioListingDetail,
  type StudioOperatorCenter,
  type StudioOrderDetail,
  type StudioOrders,
  type StudioProfile,
  type StudioSettingsProfile,
  type StudioSettlements,
} from "../modules/studio/studio-api";
import { clearAuthSession, hasAuthToken, loginByPassword, logout } from "../lib/auth";

type StudioTab = "dashboard" | "listing" | "order" | "operator" | "settlement" | "finance" | "profile";

const MENU_ITEMS: Array<{ key: StudioTab; label: string; description: string }> = [
  { key: "dashboard", label: "工作台", description: "看在售、订单和分润概览" },
  { key: "listing", label: "账号商品", description: "管理工作室商品状态与审核进度" },
  { key: "order", label: "订单管理", description: "查看当前工作室卖家订单" },
  { key: "operator", label: "子账号", description: "维护工作室操作员与权限配置" },
  { key: "settlement", label: "分润明细", description: "查看订单维度分润与结算状态" },
  { key: "finance", label: "财务与提现", description: "维护收款信息、提现申请和对账下载" },
  { key: "profile", label: "资料设置", description: "查看工作室资料与审核策略" },
];

const OPERATOR_PERMISSION_OPTIONS = ["LISTING", "ORDER", "AFTER_SALE", "FINANCE", "PROFILE"];

export function StudioApp() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<StudioTab>("dashboard");
  const [notice, setNotice] = useState("");
  const [sessionLoading, setSessionLoading] = useState(true);
  const [sessionError, setSessionError] = useState("");
  const [consoleSession, setConsoleSession] = useState<StudioConsoleSession | null>(null);

  const [dashboard, setDashboard] = useState<StudioDashboard | null>(null);
  const [dashboardLoading, setDashboardLoading] = useState(true);
  const [dashboardError, setDashboardError] = useState("");

  const [listingStatus, setListingStatus] = useState("ALL");
  const [listings, setListings] = useState<StudioListingCenter | null>(null);
  const [listingsLoading, setListingsLoading] = useState(false);
  const [listingsError, setListingsError] = useState("");
  const [selectedListingNo, setSelectedListingNo] = useState("");
  const [listingDetail, setListingDetail] = useState<StudioListingDetail | null>(null);
  const [listingDetailLoading, setListingDetailLoading] = useState(false);

  const [orderStatus, setOrderStatus] = useState("ALL");
  const [orders, setOrders] = useState<StudioOrders | null>(null);
  const [ordersLoading, setOrdersLoading] = useState(false);
  const [ordersError, setOrdersError] = useState("");
  const [selectedOrderNo, setSelectedOrderNo] = useState("");
  const [orderDetail, setOrderDetail] = useState<StudioOrderDetail | null>(null);
  const [orderDetailLoading, setOrderDetailLoading] = useState(false);
  const [afterSaleNote, setAfterSaleNote] = useState("");
  const [afterSaleProofKey, setAfterSaleProofKey] = useState("");
  const [afterSaleProofUrl, setAfterSaleProofUrl] = useState("");
  const [afterSaleHandling, setAfterSaleHandling] = useState(false);
  const [afterSaleProofUploading, setAfterSaleProofUploading] = useState(false);
  const [refundReviewNote, setRefundReviewNote] = useState("");
  const [refundReviewHandling, setRefundReviewHandling] = useState(false);

  const [operatorStatus, setOperatorStatus] = useState("ALL");
  const [operatorKeyword, setOperatorKeyword] = useState("");
  const [operators, setOperators] = useState<StudioOperatorCenter | null>(null);
  const [operatorsLoading, setOperatorsLoading] = useState(false);
  const [operatorsError, setOperatorsError] = useState("");
  const [selectedOperatorId, setSelectedOperatorId] = useState<number | null>(null);
  const [operatorSaving, setOperatorSaving] = useState(false);
  const [operatorActionLoading, setOperatorActionLoading] = useState(false);
  const [operatorForm, setOperatorForm] = useState({
    operatorId: undefined as number | undefined,
    name: "",
    phone: "",
    permissions: [] as string[],
    password: "",
  });
  const [operatorResetPassword, setOperatorResetPassword] = useState("");

  const [settlementRange, setSettlementRange] = useState("ALL");
  const [settlements, setSettlements] = useState<StudioSettlements | null>(null);
  const [settlementsLoading, setSettlementsLoading] = useState(false);
  const [settlementsError, setSettlementsError] = useState("");

  const [profile, setProfile] = useState<StudioProfile | null>(null);
  const [profileLoading, setProfileLoading] = useState(false);
  const [profileError, setProfileError] = useState("");
  const [profileForm, setProfileForm] = useState({ studioName: "", description: "", contactPhone: "", contactWechat: "" });
  const [profileSaving, setProfileSaving] = useState(false);
  const [avatarUploading, setAvatarUploading] = useState(false);
  const [settingsProfile, setSettingsProfile] = useState<StudioSettingsProfile | null>(null);
  const [settingsLoading, setSettingsLoading] = useState(false);
  const [settingsError, setSettingsError] = useState("");
  const [passwordForm, setPasswordForm] = useState({ currentPassword: "", nextPassword: "", confirmPassword: "" });
  const [passwordSaving, setPasswordSaving] = useState(false);
  const [phoneForm, setPhoneForm] = useState({ phone: "", bindCode: "", unbindCode: "" });
  const [phoneActionLoading, setPhoneActionLoading] = useState(false);
  const [bindCodeSending, setBindCodeSending] = useState(false);
  const [unbindCodeSending, setUnbindCodeSending] = useState(false);
  const [securitySaving, setSecuritySaving] = useState(false);
  const [securityForm, setSecurityForm] = useState({ loginAlertEnabled: true, secondaryVerifyEnabled: false });

  const [financeRange, setFinanceRange] = useState("ALL");
  const [finance, setFinance] = useState<StudioFinance | null>(null);
  const [financeLoading, setFinanceLoading] = useState(false);
  const [financeError, setFinanceError] = useState("");
  const [financeForm, setFinanceForm] = useState({ channel: "ALIPAY", accountName: "", accountNo: "" });
  const [financeSaving, setFinanceSaving] = useState(false);
  const [withdrawAmount, setWithdrawAmount] = useState("");
  const [withdrawApplying, setWithdrawApplying] = useState(false);

  useEffect(() => {
    void bootstrapSession();
  }, []);

  useEffect(() => {
    if (!consoleSession) {
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
    } else if (activeTab === "operator") {
      void refreshOperators();
    } else if (activeTab === "settlement") {
      void refreshSettlements();
    } else if (activeTab === "profile") {
      void refreshProfile();
    } else if (activeTab === "finance") {
      void refreshFinance();
    }
  }, [activeTab, consoleSession]);

  useEffect(() => {
    if (!consoleSession) {
      return;
    }
    if (!selectedListingNo) {
      setListingDetail(null);
      return;
    }
    setListingDetailLoading(true);
    loadStudioListingDetail(selectedListingNo)
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
      setAfterSaleNote("");
      setAfterSaleProofKey("");
      setAfterSaleProofUrl("");
      return;
    }
    setOrderDetailLoading(true);
    loadStudioOrderDetail(selectedOrderNo)
      .then((data) => {
        setOrderDetail(data);
        setAfterSaleNote(data.afterSaleNote ?? "");
        setAfterSaleProofKey(data.afterSaleProofKey ?? "");
        setAfterSaleProofUrl(data.afterSaleProofUrl ?? "");
      })
      .catch((error: Error) => {
        setNotice(error.message);
        setOrderDetail(null);
      })
      .finally(() => setOrderDetailLoading(false));
  }, [selectedOrderNo, consoleSession, activeTab]);

  useEffect(() => {
    if (activeTab !== "operator") {
      return;
    }
    if (!selectedOperatorId || !operators) {
      return;
    }
    const row = operators.rows.find((item) => item.operatorId === selectedOperatorId);
    if (!row) {
      return;
    }
    setOperatorForm({
      operatorId: row.operatorId,
      name: row.name,
      phone: row.phoneRaw,
      permissions: row.permissions,
      password: "",
    });
  }, [selectedOperatorId, operators, activeTab]);

  async function bootstrapSession() {
    setSessionLoading(true);
    setSessionError("");
    if (!hasAuthToken()) {
      setConsoleSession(null);
      setSessionLoading(false);
      return;
    }
    try {
      setConsoleSession(await loadStudioSession());
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
      setDashboard(await loadStudioDashboard());
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
      const data = await loadStudioListings(listingStatus);
      setListings(data);
      if (!selectedListingNo && data.rows[0]) {
        setSelectedListingNo(data.rows[0].listingNo);
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
      const data = await loadStudioOrders(orderStatus);
      setOrders(data);
      if (!selectedOrderNo && data.rows[0]) {
        setSelectedOrderNo(data.rows[0].orderNo);
      }
      if (selectedOrderNo && !data.rows.find((row) => row.orderNo === selectedOrderNo)) {
        setSelectedOrderNo(data.rows[0]?.orderNo ?? "");
      }
    } catch (error) {
      setOrdersError((error as Error).message);
    } finally {
      setOrdersLoading(false);
    }
  }

  async function refreshSettlements() {
    setSettlementsLoading(true);
    setSettlementsError("");
    try {
      setSettlements(await loadStudioSettlements(settlementRange));
    } catch (error) {
      setSettlementsError((error as Error).message);
    } finally {
      setSettlementsLoading(false);
    }
  }

  async function refreshOperators() {
    setOperatorsLoading(true);
    setOperatorsError("");
    try {
      const data = await loadStudioOperators(operatorStatus, operatorKeyword.trim());
      setOperators(data);
      if (!selectedOperatorId && data.rows[0]) {
        setSelectedOperatorId(data.rows[0].operatorId);
      }
      if (selectedOperatorId && !data.rows.find((row) => row.operatorId === selectedOperatorId)) {
        setSelectedOperatorId(data.rows[0]?.operatorId ?? null);
      }
    } catch (error) {
      setOperatorsError((error as Error).message);
    } finally {
      setOperatorsLoading(false);
    }
  }

  async function refreshProfile() {
    setProfileLoading(true);
    setSettingsLoading(true);
    setProfileError("");
    setSettingsError("");
    try {
      const [profileData, settingsData] = await Promise.all([loadStudioProfile(), loadStudioSettings()]);
      setProfile(profileData);
      setSettingsProfile(settingsData);
      setProfileForm({
        studioName: profileData.studioName,
        description: profileData.description ?? "",
        contactPhone: profileData.contactPhone ?? "",
        contactWechat: profileData.contactWechat ?? "",
      });
      setPhoneForm((current) => ({
        ...current,
        phone: settingsData.phoneBound ? settingsData.phone : current.phone,
      }));
      setSecurityForm({
        loginAlertEnabled: settingsData.loginAlertEnabled,
        secondaryVerifyEnabled: settingsData.secondaryVerifyEnabled,
      });
    } catch (error) {
      const message = (error as Error).message;
      setProfileError(message);
      setSettingsError(message);
    } finally {
      setProfileLoading(false);
      setSettingsLoading(false);
    }
  }

  async function refreshSettings() {
    setSettingsLoading(true);
    setSettingsError("");
    try {
      const data = await loadStudioSettings();
      setSettingsProfile(data);
      setPhoneForm((current) => ({
        ...current,
        phone: data.phoneBound ? data.phone : current.phone,
      }));
      setSecurityForm({
        loginAlertEnabled: data.loginAlertEnabled,
        secondaryVerifyEnabled: data.secondaryVerifyEnabled,
      });
    } catch (error) {
      setSettingsError((error as Error).message);
    } finally {
      setSettingsLoading(false);
    }
  }

  async function refreshFinance() {
    setFinanceLoading(true);
    setFinanceError("");
    try {
      const data = await loadStudioFinance(financeRange);
      setFinance(data);
      setFinanceForm({
        channel: data.account?.channel ?? "ALIPAY",
        accountName: data.account?.accountName ?? "",
        accountNo: data.account?.accountNo ?? "",
      });
    } catch (error) {
      setFinanceError((error as Error).message);
    } finally {
      setFinanceLoading(false);
    }
  }

  async function handleWithdraw(listingNo: string) {
    try {
      await withdrawStudioListing(listingNo);
      setNotice("商品已下架");
      await Promise.all([refreshListings(), refreshDashboard()]);
    } catch (error) {
      setNotice((error as Error).message);
    }
  }

  async function handleResubmit(listingNo: string) {
    try {
      await resubmitStudioListing(listingNo);
      setNotice("商品已重新提交");
      await Promise.all([refreshListings(), refreshDashboard()]);
    } catch (error) {
      setNotice((error as Error).message);
    }
  }

  async function handleSavePayoutAccount() {
    if (!financeForm.channel || !financeForm.accountName.trim() || !financeForm.accountNo.trim()) {
      setNotice("请完整填写收款渠道、姓名和账号");
      return;
    }
    setFinanceSaving(true);
    try {
      await saveStudioPayoutAccount({
        channel: financeForm.channel,
        accountName: financeForm.accountName.trim(),
        accountNo: financeForm.accountNo.trim(),
      });
      setNotice("工作室收款信息已保存");
      await refreshFinance();
    } catch (error) {
      setNotice((error as Error).message);
    } finally {
      setFinanceSaving(false);
    }
  }

  async function handleApplyWithdraw() {
    if (!withdrawAmount.trim()) {
      setNotice("请输入提现金额");
      return;
    }
    setWithdrawApplying(true);
    try {
      await applyStudioWithdraw(withdrawAmount.trim());
      setNotice("提现申请已提交");
      setWithdrawAmount("");
      await refreshFinance();
    } catch (error) {
      setNotice((error as Error).message);
    } finally {
      setWithdrawApplying(false);
    }
  }

  async function handleOrderAfterSale(action: "RESOLVE" | "CLOSE") {
    if (!selectedOrderNo) {
      setNotice("请先选择订单");
      return;
    }
    if (!afterSaleNote.trim()) {
      setNotice("请填写售后处理备注");
      return;
    }
    setAfterSaleHandling(true);
    try {
      const result = await handleStudioAfterSale(selectedOrderNo, {
        action,
        note: afterSaleNote.trim(),
        proofKey: afterSaleProofKey || undefined,
      });
      setNotice(String((result as { message?: string }).message ?? "售后处理已提交"));
      await Promise.all([refreshOrders(), refreshSettlements(), refreshFinance()]);
      const detail = await loadStudioOrderDetail(selectedOrderNo);
      setOrderDetail(detail);
      setAfterSaleNote(detail.afterSaleNote ?? "");
      setAfterSaleProofKey(detail.afterSaleProofKey ?? "");
      setAfterSaleProofUrl(detail.afterSaleProofUrl ?? "");
      setRefundReviewNote(detail.refundReviewNote ?? "");
    } catch (error) {
      setNotice((error as Error).message);
    } finally {
      setAfterSaleHandling(false);
    }
  }

  async function handleRefundReview(action: "APPROVE" | "REJECT") {
    if (!selectedOrderNo) {
      setNotice("请先选择订单");
      return;
    }
    if (!refundReviewNote.trim()) {
      setNotice("请填写退款审核备注");
      return;
    }
    setRefundReviewHandling(true);
    try {
      const result = await reviewStudioRefund(selectedOrderNo, { action, note: refundReviewNote.trim() });
      setNotice(String((result as { message?: string }).message ?? "退款审核已提交"));
      await Promise.all([refreshOrders(), refreshSettlements(), refreshFinance()]);
      const detail = await loadStudioOrderDetail(selectedOrderNo);
      setOrderDetail(detail);
      setRefundReviewNote(detail.refundReviewNote ?? "");
    } catch (error) {
      setNotice((error as Error).message);
    } finally {
      setRefundReviewHandling(false);
    }
  }

  async function handleUploadAfterSaleProof(file: File) {
    setAfterSaleProofUploading(true);
    try {
      if (!file.type.startsWith("image/")) {
        throw new Error("售后凭证仅支持图片格式");
      }
      const uploaded = await uploadStudioAsset(file, "after-sale");
      setAfterSaleProofKey(uploaded.objectKey);
      setAfterSaleProofUrl(uploaded.previewUrl);
      setNotice("售后凭证已上传");
    } catch (error) {
      setNotice((error as Error).message);
    } finally {
      setAfterSaleProofUploading(false);
    }
  }

  function handleCreateOperator() {
    setSelectedOperatorId(null);
    setOperatorResetPassword("");
    setOperatorForm({
      operatorId: undefined,
      name: "",
      phone: "",
      permissions: ["LISTING", "ORDER"],
      password: "",
    });
  }

  async function handleSaveOperator() {
    if (!operatorForm.name.trim() || !operatorForm.phone.trim()) {
      setNotice("请完整填写操作员姓名和手机号");
      return;
    }
    if (!operatorForm.operatorId && !operatorForm.password.trim()) {
      setNotice("新增操作员时必须设置登录密码");
      return;
    }
    setOperatorSaving(true);
    try {
      await saveStudioOperator({
        operatorId: operatorForm.operatorId,
        name: operatorForm.name.trim(),
        phone: operatorForm.phone.trim(),
        permissions: operatorForm.permissions,
        password: operatorForm.password.trim() || undefined,
      });
      setNotice(operatorForm.operatorId ? "操作员资料已更新" : "操作员已新增");
      setOperatorForm((current) => ({ ...current, password: "" }));
      await refreshOperators();
    } catch (error) {
      setNotice((error as Error).message);
    } finally {
      setOperatorSaving(false);
    }
  }

  async function handleOperatorStatus(status: "ACTIVE" | "DISABLED") {
    if (!selectedOperatorId) {
      setNotice("请先选择一个操作员");
      return;
    }
    setOperatorActionLoading(true);
    try {
      await updateStudioOperatorStatus(selectedOperatorId, status);
      setNotice(status === "ACTIVE" ? "操作员已启用" : "操作员已停用");
      await refreshOperators();
    } catch (error) {
      setNotice((error as Error).message);
    } finally {
      setOperatorActionLoading(false);
    }
  }

  async function handleResetOperatorPassword() {
    if (!selectedOperatorId) {
      setNotice("请先选择一个操作员");
      return;
    }
    if (!operatorResetPassword.trim()) {
      setNotice("请输入新的登录密码");
      return;
    }
    setOperatorActionLoading(true);
    try {
      await resetStudioOperatorPassword(selectedOperatorId, operatorResetPassword.trim());
      setNotice("操作员密码已重置");
      setOperatorResetPassword("");
    } catch (error) {
      setNotice((error as Error).message);
    } finally {
      setOperatorActionLoading(false);
    }
  }

  async function handleDownloadStatement() {
    try {
      const result = await downloadStudioStatement(financeRange);
      const blob = new Blob([result.content], { type: "text/csv;charset=utf-8;" });
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement("a");
      anchor.href = url;
      anchor.download = result.fileName;
      anchor.click();
      URL.revokeObjectURL(url);
      setNotice("对账单已生成并开始下载");
    } catch (error) {
      setNotice((error as Error).message);
    }
  }

  async function handleSaveProfile() {
    setProfileSaving(true);
    try {
      await saveStudioProfile(profileForm);
      setNotice("工作室资料已保存");
      await Promise.all([refreshProfile(), refreshDashboard()]);
    } catch (error) {
      setNotice((error as Error).message);
    } finally {
      setProfileSaving(false);
    }
  }

  async function handleUploadAvatar(file: File) {
    setAvatarUploading(true);
    try {
      if (!file.type.startsWith("image/")) {
        throw new Error("仅支持上传图片格式头像");
      }
      const uploaded = await uploadStudioAsset(file, "avatars");
      await updateStudioAvatar(uploaded.objectKey);
      setNotice("工作室负责人头像已更新");
      await refreshProfile();
    } catch (error) {
      setNotice((error as Error).message);
    } finally {
      setAvatarUploading(false);
    }
  }

  async function handleSendBindCode() {
    if (!phoneForm.phone.trim()) {
      setNotice("请先填写新的绑定手机号");
      return;
    }
    setBindCodeSending(true);
    try {
      const result = await sendStudioSmsCode(phoneForm.phone.trim(), "BIND_PHONE");
      setNotice(result.hint);
    } catch (error) {
      setNotice((error as Error).message);
    } finally {
      setBindCodeSending(false);
    }
  }

  async function handleSendUnbindCode() {
    if (!settingsProfile?.phoneBound || !settingsProfile.phone) {
      setNotice("当前没有可解绑的手机号");
      return;
    }
    setUnbindCodeSending(true);
    try {
      const result = await sendStudioSmsCode(settingsProfile.phone, "SECURITY_VERIFY");
      setNotice(result.hint);
    } catch (error) {
      setNotice((error as Error).message);
    } finally {
      setUnbindCodeSending(false);
    }
  }

  async function handleChangePassword() {
    if (!passwordForm.currentPassword || !passwordForm.nextPassword || !passwordForm.confirmPassword) {
      setNotice("请完整填写原密码和新密码");
      return;
    }
    setPasswordSaving(true);
    try {
      const result = await changeStudioPassword(
        passwordForm.currentPassword,
        passwordForm.nextPassword,
        passwordForm.confirmPassword
      );
      setNotice(result.message || "登录密码已更新");
      setPasswordForm({ currentPassword: "", nextPassword: "", confirmPassword: "" });
    } catch (error) {
      setNotice((error as Error).message);
    } finally {
      setPasswordSaving(false);
    }
  }

  async function handleChangePhone() {
    if (!phoneForm.phone.trim() || !phoneForm.bindCode.trim()) {
      setNotice("请先填写新手机号和验证码");
      return;
    }
    setPhoneActionLoading(true);
    try {
      const data = await changeStudioBoundPhone(phoneForm.phone.trim(), phoneForm.bindCode.trim());
      setSettingsProfile(data);
      setPhoneForm({ phone: data.phone, bindCode: "", unbindCode: "" });
      setSecurityForm({
        loginAlertEnabled: data.loginAlertEnabled,
        secondaryVerifyEnabled: data.secondaryVerifyEnabled,
      });
      setNotice("绑定手机号已更新");
      await bootstrapSession();
      await refreshProfile();
    } catch (error) {
      setNotice((error as Error).message);
    } finally {
      setPhoneActionLoading(false);
    }
  }

  async function handleUnbindPhone() {
    if (!phoneForm.unbindCode.trim()) {
      setNotice("请输入安全验证码");
      return;
    }
    setPhoneActionLoading(true);
    try {
      const data = await unbindStudioBoundPhone(phoneForm.unbindCode.trim());
      setSettingsProfile(data);
      setPhoneForm((current) => ({ ...current, unbindCode: "" }));
      setSecurityForm({
        loginAlertEnabled: data.loginAlertEnabled,
        secondaryVerifyEnabled: data.secondaryVerifyEnabled,
      });
      setNotice("手机号已解绑");
      await bootstrapSession();
      await refreshProfile();
    } catch (error) {
      setNotice((error as Error).message);
    } finally {
      setPhoneActionLoading(false);
    }
  }

  async function handleSaveSecurity() {
    setSecuritySaving(true);
    try {
      const data = await updateStudioSecuritySettings(
        securityForm.loginAlertEnabled,
        securityForm.secondaryVerifyEnabled
      );
      setSettingsProfile(data);
      setNotice("安全策略已保存");
    } catch (error) {
      setNotice((error as Error).message);
    } finally {
      setSecuritySaving(false);
    }
  }

  async function handleUnbindWechatAction() {
    if (!settingsProfile?.wechatBound) {
      setNotice("当前账号未绑定微信");
      return;
    }
    setPhoneActionLoading(true);
    try {
      const data = await unbindStudioWechat();
      setSettingsProfile(data);
      setNotice("微信绑定已解除");
    } catch (error) {
      setNotice((error as Error).message);
    } finally {
      setPhoneActionLoading(false);
    }
  }

  async function handleLogin(phone: string, password: string) {
    setSessionError("");
    try {
      await loginByPassword(phone, password);
      await bootstrapSession();
      window.location.reload();
    } catch (error) {
      setSessionError((error as Error).message);
    }
  }

  async function handleLogout() {
    await logout();
    setConsoleSession(null);
    setSessionError("");
    window.location.assign("/");
  }

  useEffect(() => {
    if (!notice) {
      return;
    }
    const timer = window.setTimeout(() => setNotice(""), 2000);
    return () => window.clearTimeout(timer);
  }, [notice]);

  const activeMenuItem = useMemo(
    () => MENU_ITEMS.find((item) => item.key === activeTab) ?? null,
    [activeTab]
  );
  const heroActions = useMemo(() => {
    if (activeTab === "dashboard") {
      return [
        { label: "发布账号", kind: "primary" as const, onClick: () => navigate("/publish") },
        { label: "我的发布", kind: "secondary" as const, onClick: () => navigate("/publish/mine") },
      ];
    }
    if (activeTab === "listing") {
      return [
        { label: "刷新商品列表", kind: "primary" as const, onClick: () => void refreshListings() },
        { label: "发布账号", kind: "secondary" as const, onClick: () => navigate("/publish") },
      ];
    }
    if (activeTab === "order") {
      return [
        { label: "刷新订单", kind: "primary" as const, onClick: () => void refreshOrders() },
        ...(selectedOrderNo ? [{ label: "收起详情", kind: "secondary" as const, onClick: () => setSelectedOrderNo("") }] : []),
      ];
    }
    if (activeTab === "operator") {
      return [
        { label: "刷新子账号", kind: "primary" as const, onClick: () => void refreshOperators() },
        {
          label: "新增子账号",
          kind: "secondary" as const,
          onClick: () => {
            setSelectedOperatorId(null);
            setOperatorResetPassword("");
            setOperatorForm({ operatorId: undefined, name: "", phone: "", permissions: [], password: "" });
          },
        },
      ];
    }
    if (activeTab === "settlement") {
      return [{ label: "刷新分润明细", kind: "primary" as const, onClick: () => void refreshSettlements() }];
    }
    if (activeTab === "finance") {
      return [{ label: "刷新财务数据", kind: "primary" as const, onClick: () => void refreshFinance() }];
    }
    if (activeTab === "profile") {
      return [{ label: "刷新资料设置", kind: "primary" as const, onClick: () => { void refreshProfile(); void refreshSettings(); } }];
    }
    return [];
  }, [activeTab, navigate, selectedOrderNo]);

  if (sessionLoading) {
    return (
      <div className="studio-shell studio-auth-shell">
        <SurfaceCard eyebrow="工作室后台" title="正在校验工作室登录态">
          <LoadingBlock title="正在同步工作室会话" />
        </SurfaceCard>
      </div>
    );
  }

  if (!consoleSession) {
    return (
      <ConsoleLoginScreen
        badge="工作室后台"
        title="使用工作室管理员账号登录"
        description="请输入已绑定工作室的手机号和密码。默认演示工作室账号为 13900139000 / Pass123。"
        error={sessionError}
        onSubmit={handleLogin}
      />
    );
  }

  return (
    <div className="studio-shell">
      <aside className="studio-sidebar">
        <div className="dt-brand">
          <span className="dt-brand__mark dt-brand__mark--image">
            <img alt="萌虎" src="/studio/brand/menghu-ai-logo.png" />
          </span>
          <div>
            <div>萌虎工作室</div>
            <small>工作室运营台</small>
          </div>
        </div>
        <div className="studio-sidebar__profile">
          <strong>{consoleSession.studioName}</strong>
          <p>{consoleSession.nickname} · {consoleSession.active ? "合作中" : "暂停合作"}</p>
          <Button kind="secondary" onClick={() => void handleLogout()}>退出登录</Button>
        </div>
        <nav className="studio-menu" aria-label="工作室菜单">
          {MENU_ITEMS.map((item) => (
            <button
              className={`studio-menu__item ${activeTab === item.key ? "is-active" : ""}`}
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

      <main className="studio-main">
        <section className="studio-hero">
          <SectionHeading
            badge="工作室后台"
            title={activeMenuItem?.label ?? "工作室后台"}
            description={activeMenuItem?.description ?? ""}
          />
          {heroActions.length ? (
            <div className="studio-actions">
              {heroActions.map((action) => (
                <Button key={action.label} kind={action.kind} onClick={action.onClick}>{action.label}</Button>
              ))}
            </div>
          ) : null}
        </section>

        {notice ? (
          <div className="studio-notice" role="status" aria-live="polite">
            <span>{notice}</span>
            <button onClick={() => setNotice("")} type="button">关闭</button>
          </div>
        ) : null}

        {dashboardLoading ? <LoadingBlock title="正在加载工作室工作台" /> : null}
        {dashboardError ? <ErrorBlock title="工作室工作台加载失败" message={dashboardError} onRetry={() => void refreshDashboard()} /> : null}
        {activeTab === "dashboard" && dashboard ? (
          <>
            <section className="studio-metrics">
              {dashboard.metrics.map((metric) => (
                <MetricCard key={metric.label} label={metric.label} value={metric.value} trend={metric.trend} />
              ))}
            </section>
            <section className="studio-policy-banner">
              <Tag tone={dashboard.profile.reviewStrategy === "DIRECT_PUBLISH" ? "success" : "warning"}>
                当前策略：{dashboard.profile.reviewStrategyText}
              </Tag>
              <p>{dashboard.profile.activeText}，工作室名称：{dashboard.profile.studioName}</p>
            </section>
          </>
        ) : null}

        {activeTab === "dashboard" && dashboard ? (
            <section className="studio-grid">
            <SurfaceCard eyebrow="最近商品" title="账号商品快照">
              {dashboard.recentListings.length === 0 ? (
                <EmptyBlock title="暂无商品记录" />
              ) : (
                <ul className="studio-list">
                  {dashboard.recentListings.map((row) => (
                    <li className="studio-list__item" key={row.listingNo}>
                      <div>
                        <strong>{row.title}</strong>
                        <p>{row.listingNo} · {row.statusText}</p>
                      </div>
                      <span>{row.price}</span>
                    </li>
                  ))}
                </ul>
              )}
            </SurfaceCard>
            <SurfaceCard eyebrow="最近订单" title="卖家订单快照">
              {dashboard.recentOrders.length === 0 ? (
                <EmptyBlock title="暂无订单记录" />
              ) : (
                <ul className="studio-list">
                  {dashboard.recentOrders.map((row) => (
                    <li className="studio-list__item" key={row.orderNo}>
                      <div>
                        <strong>{row.listingTitle}</strong>
                        <p>{row.orderNo} · {row.statusText}</p>
                      </div>
                      <span>{row.totalAmount}</span>
                    </li>
                  ))}
                </ul>
              )}
            </SurfaceCard>
            <SurfaceCard eyebrow="代肝协作" title="挂靠代肝服务">
              {dashboard.boostingServices.length === 0 ? (
                <EmptyBlock title="当前没有绑定到你工作室名称的代肝服务" />
              ) : (
                <ul className="studio-list">
                  {dashboard.boostingServices.map((row) => (
                    <li className="studio-list__item" key={row.serviceNo}>
                      <div>
                        <strong>{row.name}</strong>
                        <p>{row.statusText} · 销量 {row.salesCount}</p>
                      </div>
                      <span>{row.price}</span>
                    </li>
                  ))}
                </ul>
              )}
            </SurfaceCard>
            <SurfaceCard eyebrow="平台公告" title="最新通知">
              {dashboard.announcements.length === 0 ? (
                <EmptyBlock title="当前没有可展示公告" />
              ) : (
                <ul className="studio-list">
                  {dashboard.announcements.map((row) => (
                    <li className="studio-list__item" key={row.announcementNo}>
                      <div>
                        <strong>{row.title}</strong>
                        <p>{row.categoryText} · {row.publishAt}</p>
                        <p>{row.content}</p>
                      </div>
                      {row.pinned ? <Tag tone="warning">置顶</Tag> : null}
                    </li>
                  ))}
                </ul>
              )}
            </SurfaceCard>
          </section>
        ) : null}

        {activeTab === "listing" ? (
          <section className="studio-grid studio-grid--detail">
            <SurfaceCard eyebrow="账号商品" title="我的商品列表">
              <div className="studio-filter-row">
                <label>
                  状态
                  <select value={listingStatus} onChange={(event) => setListingStatus(event.target.value)}>
                    <option value="ALL">全部</option>
                    <option value="PUBLISHED">已上架</option>
                    <option value="PENDING_REVIEW">待审核</option>
                    <option value="REJECTED">已驳回</option>
                    <option value="OFFLINE">已下架</option>
                  </select>
                </label>
                <Button onClick={() => void refreshListings()}>筛选</Button>
              </div>
              {listingsLoading ? <LoadingBlock title="正在加载商品列表" compact /> : null}
              {listingsError ? <ErrorBlock title="商品列表加载失败" message={listingsError} compact onRetry={() => void refreshListings()} /> : null}
              {listings ? (
                <>
                  <div className="studio-strategy-card">
                    <Tag tone={listings.strategy.reviewStrategy === "DIRECT_PUBLISH" ? "success" : "warning"}>{listings.strategy.reviewStrategyText}</Tag>
                    <p>{listings.strategy.strategyTip}</p>
                  </div>
                  <SummaryPills summary={listings.summary} labels={{ published: "已上架", pendingReview: "待审核", rejected: "已驳回", offline: "已下架" }} />
                  {listings.rows.length > 0 ? (
                    <div className="studio-table-scroll">
                      <table className="studio-table">
                        <thead>
                          <tr>
                            <th>商品</th>
                            <th>状态</th>
                            <th>价格</th>
                            <th>浏览/收藏</th>
                            <th>成交</th>
                            <th>更新时间</th>
                          </tr>
                        </thead>
                        <tbody>
                          {listings.rows.map((row) => (
                            <tr className={selectedListingNo === row.listingNo ? "is-active" : ""} key={row.listingNo} onClick={() => setSelectedListingNo(row.listingNo)}>
                              <td><strong>{row.title}</strong><p>{row.listingNo}</p></td>
                              <td>{row.statusText}<p>{row.reviewProgress}</p></td>
                              <td>{row.price}</td>
                              <td>{row.viewCount} / {row.favoriteCount}</td>
                              <td>{row.salesCount}</td>
                              <td>{row.updatedAt}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  ) : (
                    <EmptyBlock title="暂无商品记录" />
                  )}
                </>
              ) : null}
            </SurfaceCard>

            <SurfaceCard eyebrow="商品详情" title="当前选中商品">
              {listingDetailLoading ? <LoadingBlock title="正在加载商品详情" compact /> : null}
              {!listingDetailLoading && listingDetail ? (
                <div className="studio-detail">
                  <div className="studio-detail__cover">
                    {listingDetail.summary.coverUrl ? <img alt={listingDetail.summary.title} src={listingDetail.summary.coverUrl} /> : <div className="studio-detail__placeholder">无封面</div>}
                  </div>
                  <div className="studio-detail__body">
                    <h3>{listingDetail.summary.title}</h3>
                    <div className="studio-detail__meta">
                      <Tag>{listingDetail.summary.statusText}</Tag>
                      {listingDetail.summary.rejectionReason ? <Tag tone="warning">有驳回原因</Tag> : null}
                    </div>
                    <dl className="studio-detail__facts">
                      <div><dt>商品编号</dt><dd>{listingDetail.summary.listingNo}</dd></div>
                      <div><dt>售价</dt><dd>{listingDetail.summary.price}</dd></div>
                      <div><dt>提交时间</dt><dd>{listingDetail.summary.submittedAt}</dd></div>
                      <div><dt>发布时间</dt><dd>{listingDetail.summary.publishedAt}</dd></div>
                    </dl>
                    <p className="studio-detail__desc">{listingDetail.summary.description}</p>
                    <div className="studio-detail__actions">
                      <Button kind="secondary" onClick={() => navigate(`/publish?edit=${encodeURIComponent(listingDetail.summary.listingNo)}`)}>编辑</Button>
                      <Button kind="ghost" onClick={() => void handleWithdraw(listingDetail.summary.listingNo)}>下架</Button>
                      <Button onClick={() => void handleResubmit(listingDetail.summary.listingNo)}>重新提交</Button>
                    </div>
                    {listingDetail.summary.rejectionReason ? (
                      <div className="studio-detail__section">
                        <h4>驳回原因</h4>
                        <p>{listingDetail.summary.rejectionReason}</p>
                      </div>
                    ) : null}
                  </div>
                </div>
              ) : null}
              {!listingDetailLoading && !listingDetail ? <EmptyBlock title="请选择左侧商品查看详情" /> : null}
            </SurfaceCard>
          </section>
        ) : null}

        {activeTab === "order" ? (
          <section className="studio-grid studio-grid--detail">
            <SurfaceCard eyebrow="订单管理" title="我的卖家订单">
              <div className="studio-filter-row">
                <label>
                  订单状态
                  <select value={orderStatus} onChange={(event) => setOrderStatus(event.target.value)}>
                    <option value="ALL">全部</option>
                    <option value="WAITING_TRADE">待交易</option>
                    <option value="COMPLETED">已完成</option>
                    <option value="REFUND_PENDING">退款审核中</option>
                    <option value="AFTER_SALE">售后中</option>
                    <option value="REFUNDED">已退款</option>
                    <option value="CLOSED">已关闭</option>
                  </select>
                </label>
                <Button onClick={() => void refreshOrders()}>筛选</Button>
              </div>
              {ordersLoading ? <LoadingBlock title="正在加载卖家订单" compact /> : null}
              {ordersError ? <ErrorBlock title="卖家订单加载失败" message={ordersError} compact onRetry={() => void refreshOrders()} /> : null}
              {orders && orders.rows.length > 0 ? (
                <>
                  <SummaryPills summary={orders.summary} labels={{ waitingTrade: "待交易", completed: "已完成", afterSale: "售后中", closed: "已关闭" }} />
                  <div className="studio-table-scroll">
                    <table className="studio-table">
                      <thead>
                        <tr>
                          <th>订单号</th>
                          <th>商品</th>
                          <th>买家</th>
                          <th>状态</th>
                          <th>金额</th>
                          <th>群聊</th>
                          <th>时间</th>
                        </tr>
                      </thead>
                      <tbody>
                        {orders.rows.map((row) => (
                          <tr className={selectedOrderNo === row.orderNo ? "is-active" : ""} key={row.orderNo} onClick={() => setSelectedOrderNo(row.orderNo)}>
                            <td>{row.orderNo}</td>
                            <td><strong>{row.listingTitle}</strong><p>{row.listingNo}</p></td>
                            <td>{row.buyerNickname}</td>
                            <td>{row.statusText}</td>
                            <td>{row.totalAmount}<p>货款 {row.itemAmount}</p></td>
                            <td>
                              {row.chatGroupNo}
                              {row.chatGroupNo && row.chatGroupNo !== "-" ? (
                                <p>
                                  <button
                                    className="studio-inline-link"
                                    type="button"
                                    onClick={(event) => {
                                      event.stopPropagation();
                                      navigate(`/im/${encodeURIComponent(row.chatGroupNo)}`);
                                    }}
                                  >
                                    进入群聊
                                  </button>
                                </p>
                              ) : null}
                            </td>
                            <td>{row.createdAt}<p>{row.completedAt}</p></td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </>
              ) : orders && !ordersLoading ? <EmptyBlock title="暂无卖家订单" /> : null}
            </SurfaceCard>

            <SurfaceCard eyebrow="售后处理" title="当前选中订单">
              {orderDetailLoading ? <LoadingBlock title="正在加载订单详情" compact /> : null}
              {!orderDetailLoading && orderDetail ? (
                <div className="studio-detail studio-detail--order">
                  <div className="studio-detail__cover">
                    {orderDetail.listingCoverUrl ? <img alt={orderDetail.listingTitle} src={orderDetail.listingCoverUrl} /> : <div className="studio-detail__placeholder">订单封面</div>}
                  </div>
                  <div className="studio-detail__body">
                    <h3>{orderDetail.listingTitle}</h3>
                    <div className="studio-detail__meta">
                      <Tag>{orderDetail.statusText}</Tag>
                      <Tag tone="accent">{orderDetail.sellerTypeText}</Tag>
                      {orderDetail.chatGroupNo !== "-" ? <Tag tone="success">群聊 {orderDetail.chatGroupNo}</Tag> : null}
                    </div>
                    <dl className="studio-detail__facts">
                      <div><dt>订单号</dt><dd>{orderDetail.orderNo}</dd></div>
                      <div><dt>买家昵称</dt><dd>{orderDetail.buyerNickname}</dd></div>
                      <div><dt>支付方式</dt><dd>{orderDetail.paymentMethod}</dd></div>
                      <div><dt>总金额</dt><dd>{orderDetail.totalAmount}</dd></div>
                      <div><dt>创建时间</dt><dd>{orderDetail.createdAt}</dd></div>
                      <div><dt>完成时间</dt><dd>{orderDetail.completedAt}</dd></div>
                    </dl>
                    <p className="studio-detail__desc">{orderDetail.listingSummary}</p>
                    {orderDetail.chatGroupNo && orderDetail.chatGroupNo !== "-" ? (
                      <div className="studio-detail__actions">
                        <Button kind="secondary" onClick={() => navigate(`/im/${encodeURIComponent(orderDetail.chatGroupNo)}`)}>
                          进入群聊
                        </Button>
                      </div>
                    ) : null}
                    <div className="studio-detail__section">
                      <h4>退款审核</h4>
                      {orderDetail.status === "REFUND_PENDING" ? (
                        <div className="studio-after-sale-panel">
                          <dl className="studio-detail__facts">
                            <div><dt>退款金额</dt><dd>{orderDetail.refundAmount || "¥0.00"}</dd></div>
                            <div><dt>申请时间</dt><dd>{orderDetail.refundRequestedAt || "未提交"}</dd></div>
                            <div><dt>退款原因</dt><dd>{orderDetail.refundReason || "未填写"}</dd></div>
                          </dl>
                          <label>
                            审核备注
                            <textarea
                              rows={3}
                              placeholder="填写同意依据或拒绝原因"
                              value={refundReviewNote}
                              onChange={(event) => setRefundReviewNote(event.target.value)}
                            />
                          </label>
                          <div className="studio-detail__actions">
                            <Button onClick={() => void handleRefundReview("APPROVE")} disabled={refundReviewHandling}>
                              {refundReviewHandling ? "处理中" : "同意退款"}
                            </Button>
                            <Button kind="secondary" onClick={() => void handleRefundReview("REJECT")} disabled={refundReviewHandling}>
                              {refundReviewHandling ? "处理中" : "拒绝退款"}
                            </Button>
                          </div>
                        </div>
                      ) : (
                        <dl className="studio-detail__facts">
                          <div><dt>退款金额</dt><dd>{orderDetail.refundAmount || "¥0.00"}</dd></div>
                          <div><dt>申请时间</dt><dd>{orderDetail.refundRequestedAt || "未提交"}</dd></div>
                          <div><dt>处理备注</dt><dd>{orderDetail.refundReviewNote || "暂无退款审核记录"}</dd></div>
                        </dl>
                      )}
                    </div>
                    <div className="studio-detail__section">
                      <h4>售后处理</h4>
                      {orderDetail.status === "AFTER_SALE" ? (
                        <div className="studio-after-sale-panel">
                          <label>
                            处理备注
                            <textarea
                              rows={4}
                              placeholder="填写与买家沟通结果、补偿说明或结案依据"
                              value={afterSaleNote}
                              onChange={(event) => setAfterSaleNote(event.target.value)}
                            />
                          </label>
                          <div className="studio-proof-panel">
                            <div className="studio-proof-panel__header">
                              <strong>售后凭证</strong>
                              <span>可选。建议上传客服截图、补偿凭证或沟通记录。</span>
                            </div>
                            <input
                              accept="image/png,image/jpeg,image/webp"
                              id="studio-after-sale-proof-upload"
                              style={{ display: "none" }}
                              type="file"
                              onChange={async (event) => {
                                const file = event.target.files?.[0];
                                event.currentTarget.value = "";
                                if (!file) return;
                                await handleUploadAfterSaleProof(file);
                              }}
                            />
                            <label className="dt-button dt-button--secondary publish-upload-trigger" htmlFor="studio-after-sale-proof-upload">
                              {afterSaleProofUploading ? "上传中..." : "上传售后凭证"}
                            </label>
                            {afterSaleProofUrl ? (
                              <a className="studio-proof-preview" href={afterSaleProofUrl} rel="noreferrer" target="_blank">
                                <img alt="售后凭证" src={afterSaleProofUrl} />
                                <span>点击查看原图</span>
                              </a>
                            ) : (
                              <div className="studio-proof-preview studio-proof-preview--empty">暂未上传售后凭证</div>
                            )}
                          </div>
                          <div className="studio-detail__actions">
                            <Button onClick={() => void handleOrderAfterSale("RESOLVE")} disabled={afterSaleHandling}>
                              {afterSaleHandling ? "处理中" : "售后完成"}
                            </Button>
                            <Button kind="secondary" onClick={() => void handleOrderAfterSale("CLOSE")} disabled={afterSaleHandling}>
                              {afterSaleHandling ? "处理中" : "关闭订单"}
                            </Button>
                          </div>
                        </div>
                      ) : (
                        <dl className="studio-detail__facts">
                          <div><dt>售后备注</dt><dd>{orderDetail.afterSaleNote || "暂无售后备注"}</dd></div>
                          <div><dt>处理时间</dt><dd>{orderDetail.afterSaleHandledAt || "未处理"}</dd></div>
                          <div>
                            <dt>售后凭证</dt>
                            <dd>
                              {orderDetail.afterSaleProofUrl ? (
                                <a href={orderDetail.afterSaleProofUrl} rel="noreferrer" target="_blank">查看凭证</a>
                              ) : "未上传"}
                            </dd>
                          </div>
                        </dl>
                      )}
                    </div>
                  </div>
                </div>
              ) : null}
              {!orderDetailLoading && !orderDetail ? <EmptyBlock title="请选择左侧订单查看详情" /> : null}
            </SurfaceCard>
          </section>
        ) : null}

        {activeTab === "operator" ? (
          <section className="studio-grid studio-grid--detail">
            <SurfaceCard eyebrow="子账号" title="工作室操作员列表">
              <div className="studio-filter-row">
                <label>
                  状态
                  <select value={operatorStatus} onChange={(event) => setOperatorStatus(event.target.value)}>
                    <option value="ALL">全部</option>
                    <option value="ACTIVE">启用中</option>
                    <option value="DISABLED">已停用</option>
                  </select>
                </label>
                <label>
                  关键词
                  <input
                    placeholder="按姓名或手机号筛选"
                    value={operatorKeyword}
                    onChange={(event) => setOperatorKeyword(event.target.value)}
                  />
                </label>
                <Button onClick={() => void refreshOperators()}>筛选</Button>
                <Button kind="secondary" onClick={handleCreateOperator}>新增操作员</Button>
              </div>
              {operatorsLoading ? <LoadingBlock title="正在加载操作员列表" compact /> : null}
              {operatorsError ? <ErrorBlock title="操作员列表加载失败" message={operatorsError} compact onRetry={() => void refreshOperators()} /> : null}
              {operators ? (
                <>
                  <SummaryPills summary={operators.summary} labels={{ total: "总数", active: "启用中", disabled: "已停用" }} />
                  {operators.rows.length > 0 ? (
                    <div className="studio-operator-list">
                      {operators.rows.map((row) => (
                        <button
                          className={`studio-operator-card ${selectedOperatorId === row.operatorId ? "is-active" : ""}`}
                          key={row.operatorId}
                          onClick={() => {
                            setSelectedOperatorId(row.operatorId);
                            setOperatorResetPassword("");
                          }}
                          type="button"
                        >
                          <div className="studio-operator-card__top">
                            <strong>{row.name}</strong>
                            <Tag tone={row.status === "ACTIVE" ? "success" : "warning"}>{row.statusText}</Tag>
                          </div>
                          <p>{row.operatorNo}</p>
                          <p>{row.phone}</p>
                          <span>{row.permissionText}</span>
                          <small>更新于 {row.updatedAt}</small>
                        </button>
                      ))}
                    </div>
                  ) : (
                    <EmptyBlock title="当前没有操作员记录" />
                  )}
                </>
              ) : null}
            </SurfaceCard>

            <SurfaceCard eyebrow="编辑区" title={operatorForm.operatorId ? "编辑操作员" : "新增操作员"}>
              <div className="studio-operator-form">
                <label>
                  姓名
                  <input
                    maxLength={32}
                    placeholder="请输入操作员姓名"
                    value={operatorForm.name}
                    onChange={(event) => setOperatorForm((current) => ({ ...current, name: event.target.value }))}
                  />
                </label>
                <label>
                  手机号
                  <input
                    inputMode="numeric"
                    maxLength={11}
                    placeholder="请输入 11 位手机号"
                    value={operatorForm.phone}
                    onChange={(event) => setOperatorForm((current) => ({ ...current, phone: event.target.value }))}
                  />
                </label>
                <label>
                  登录密码
                  <input
                    placeholder={operatorForm.operatorId ? "留空表示不修改当前密码" : "请输入初始登录密码"}
                    type="password"
                    value={operatorForm.password}
                    onChange={(event) => setOperatorForm((current) => ({ ...current, password: event.target.value }))}
                  />
                </label>
                <div className="studio-operator-permissions">
                  <span>权限范围</span>
                  <div className="studio-operator-permissions__list">
                    {OPERATOR_PERMISSION_OPTIONS.map((permission) => {
                      const checked = operatorForm.permissions.includes(permission);
                      return (
                        <label key={permission}>
                          <input
                            checked={checked}
                            type="checkbox"
                            onChange={() => setOperatorForm((current) => ({
                              ...current,
                              permissions: checked
                                ? current.permissions.filter((item) => item !== permission)
                                : [...current.permissions, permission],
                            }))}
                          />
                          <span>{renderOperatorPermission(permission)}</span>
                        </label>
                      );
                    })}
                  </div>
                </div>
                <div className="studio-profile-form__actions">
                  <Button onClick={() => void handleSaveOperator()} disabled={operatorSaving}>
                    {operatorSaving ? "正在保存" : operatorForm.operatorId ? "保存修改" : "创建操作员"}
                  </Button>
                  {operatorForm.operatorId ? (
                    <>
                      <Button kind="secondary" onClick={() => void handleOperatorStatus("ACTIVE")} disabled={operatorActionLoading}>启用</Button>
                      <Button kind="ghost" onClick={() => void handleOperatorStatus("DISABLED")} disabled={operatorActionLoading}>停用</Button>
                    </>
                  ) : null}
                </div>
                {operatorForm.operatorId ? (
                  <div className="studio-reset-panel">
                    <label>
                      重置登录密码
                      <input
                        placeholder="请输入新的登录密码"
                        type="password"
                        value={operatorResetPassword}
                        onChange={(event) => setOperatorResetPassword(event.target.value)}
                      />
                    </label>
                    <Button kind="secondary" onClick={() => void handleResetOperatorPassword()} disabled={operatorActionLoading}>
                      {operatorActionLoading ? "处理中" : "重置密码"}
                    </Button>
                  </div>
                ) : null}
              </div>
            </SurfaceCard>
          </section>
        ) : null}

        {activeTab === "settlement" ? (
          <SurfaceCard eyebrow="分润明细" title="订单级结算视图">
            <div className="studio-filter-row">
              <label>
                时间范围
                <select value={settlementRange} onChange={(event) => setSettlementRange(event.target.value)}>
                  <option value="ALL">全部时间</option>
                  <option value="7D">近7天</option>
                  <option value="30D">近30天</option>
                </select>
              </label>
              <Button onClick={() => void refreshSettlements()}>筛选</Button>
            </div>
            {settlementsLoading ? <LoadingBlock title="正在加载分润明细" compact /> : null}
            {settlementsError ? <ErrorBlock title="分润明细加载失败" message={settlementsError} compact onRetry={() => void refreshSettlements()} /> : null}
            {settlements ? (
              <>
                <SummaryPills
                  summary={{
                    settledTotal: Number(settlements.summary.orderCount ? settlements.summary.orderCount : 0),
                    pendingTotal: 0,
                    orderCount: settlements.summary.orderCount,
                  }}
                  labels={{ settledTotal: "订单数", pendingTotal: "占位", orderCount: "明细数" }}
                />
                <div className="studio-settlement-cards">
                  <div className="studio-settlement-card">
                    <span>已结算分润</span>
                    <strong>{settlements.summary.settledTotal}</strong>
                  </div>
                  <div className="studio-settlement-card">
                    <span>待结算分润</span>
                    <strong>{settlements.summary.pendingTotal}</strong>
                  </div>
                  <div className="studio-settlement-card">
                    <span>订单笔数</span>
                    <strong>{settlements.summary.orderCount}</strong>
                  </div>
                </div>
                {settlements.rows.length > 0 ? (
                  <div className="studio-table-scroll">
                    <table className="studio-table">
                      <thead>
                        <tr>
                          <th>订单号</th>
                          <th>商品</th>
                          <th>买家</th>
                          <th>成交额</th>
                          <th>分润比例</th>
                          <th>分润金额</th>
                          <th>结算状态</th>
                        </tr>
                      </thead>
                      <tbody>
                        {settlements.rows.map((row) => (
                          <tr key={row.orderNo}>
                            <td>{row.orderNo}</td>
                            <td>{row.listingTitle}</td>
                            <td>{row.buyerNickname}</td>
                            <td>{row.grossAmount}</td>
                            <td>{row.shareRatio}</td>
                            <td>{row.shareAmount}</td>
                            <td>{row.settlementStatus}<p>{row.statusText}</p></td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                ) : (
                  <EmptyBlock title="当前时间范围内没有分润明细" />
                )}
              </>
            ) : null}
          </SurfaceCard>
        ) : null}

        {activeTab === "finance" ? (
          <SurfaceCard eyebrow="财务与提现" title="收款信息与工作室提现">
            <div className="studio-filter-row">
              <label>
                对账范围
                <select value={financeRange} onChange={(event) => setFinanceRange(event.target.value)}>
                  <option value="ALL">全部时间</option>
                  <option value="7D">近7天</option>
                  <option value="30D">近30天</option>
                </select>
              </label>
              <Button onClick={() => void refreshFinance()}>刷新</Button>
            </div>
            {financeLoading ? <LoadingBlock title="正在加载财务数据" compact /> : null}
            {financeError ? <ErrorBlock title="财务数据加载失败" message={financeError} compact onRetry={() => void refreshFinance()} /> : null}
            {finance ? (
              <>
                <div className="studio-settlement-cards">
                  <div className="studio-settlement-card">
                    <span>已结算分润</span>
                    <strong>{finance.summary.settledTotal}</strong>
                  </div>
                  <div className="studio-settlement-card">
                    <span>待结算分润</span>
                    <strong>{finance.summary.pendingTotal}</strong>
                  </div>
                  <div className="studio-settlement-card">
                    <span>提现处理中</span>
                    <strong>{finance.summary.withdrawPendingTotal}</strong>
                  </div>
                  <div className="studio-settlement-card">
                    <span>已提现金额</span>
                    <strong>{finance.summary.withdrawPaidTotal}</strong>
                  </div>
                  <div className="studio-settlement-card">
                    <span>可提现余额</span>
                    <strong>{finance.summary.withdrawableTotal}</strong>
                  </div>
                </div>

                <div className="studio-finance-grid">
                  <SurfaceCard eyebrow="收款信息" title="工作室收款账户" actions={<Button onClick={() => void handleSavePayoutAccount()} disabled={financeSaving}>{financeSaving ? "正在保存" : "保存收款信息"}</Button>}>
                    <div className="studio-finance-form">
                      <label>
                        收款渠道
                        <select value={financeForm.channel} onChange={(event) => setFinanceForm((current) => ({ ...current, channel: event.target.value }))}>
                          <option value="ALIPAY">支付宝</option>
                          <option value="WECHAT">微信</option>
                        </select>
                      </label>
                      <label>
                        收款姓名
                        <input
                          placeholder="需与实名认证姓名一致"
                          value={financeForm.accountName}
                          onChange={(event) => setFinanceForm((current) => ({ ...current, accountName: event.target.value }))}
                        />
                      </label>
                      <label>
                        收款账号
                        <input
                          placeholder="请输入支付宝账号或微信提现账号"
                          value={financeForm.accountNo}
                          onChange={(event) => setFinanceForm((current) => ({ ...current, accountNo: event.target.value }))}
                        />
                      </label>
                    </div>
                    {finance.account ? (
                      <dl className="studio-profile-card__facts">
                        <div><dt>当前渠道</dt><dd>{finance.account.channelText}</dd></div>
                        <div><dt>当前姓名</dt><dd>{finance.account.accountName}</dd></div>
                        <div><dt>当前账号</dt><dd>{finance.account.accountNo}</dd></div>
                      </dl>
                    ) : (
                      <EmptyBlock title="尚未维护工作室收款信息" />
                    )}
                  </SurfaceCard>
                  <SurfaceCard eyebrow="提现申请" title="工作室可提现入口" actions={<Button onClick={() => void handleApplyWithdraw()} disabled={withdrawApplying}>{withdrawApplying ? "正在提交" : "提交提现申请"}</Button>}>
                    <div className="studio-finance-form">
                      <label>
                        提现金额
                        <input
                          inputMode="decimal"
                          placeholder="最低 10 元，不超过可提现余额"
                          value={withdrawAmount}
                          onChange={(event) => setWithdrawAmount(event.target.value)}
                        />
                      </label>
                    </div>
                    {finance.withdraws.length > 0 ? (
                      <ul className="studio-list">
                        {finance.withdraws.map((row) => (
                          <li className="studio-list__item" key={row.applicationNo}>
                            <div>
                              <strong>{row.applicationNo}</strong>
                              <p>{row.channelText} · {row.accountNo}</p>
                            </div>
                            <span>{row.amount} · {row.statusText}</span>
                          </li>
                        ))}
                      </ul>
                    ) : (
                      <EmptyBlock title="暂无提现申请记录" />
                    )}
                  </SurfaceCard>
                </div>

                <SurfaceCard eyebrow="对账单" title="分润对账明细" actions={<Button kind="secondary" onClick={() => void handleDownloadStatement()}>下载 CSV</Button>}>
                  {finance.statements.length > 0 ? (
                    <div className="studio-table-scroll">
                      <table className="studio-table">
                        <thead>
                          <tr>
                            <th>订单号</th>
                            <th>商品</th>
                            <th>买家</th>
                            <th>成交额</th>
                            <th>分润金额</th>
                            <th>结算状态</th>
                          </tr>
                        </thead>
                        <tbody>
                          {finance.statements.map((row) => (
                            <tr key={row.orderNo}>
                              <td>{row.orderNo}</td>
                              <td>{row.listingTitle}</td>
                              <td>{row.buyerNickname}</td>
                              <td>{row.grossAmount}</td>
                              <td>{row.shareAmount}</td>
                              <td>{row.settlementStatus}</td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  ) : (
                    <EmptyBlock title="当前范围内暂无对账数据" />
                  )}
                </SurfaceCard>
              </>
            ) : null}
          </SurfaceCard>
        ) : null}

        {activeTab === "profile" ? (
          <>
            <SurfaceCard eyebrow="资料设置" title="工作室资料与联系方式">
              {profileLoading ? <LoadingBlock title="正在加载资料设置" compact /> : null}
              {profileError ? <ErrorBlock title="资料设置加载失败" message={profileError} compact onRetry={() => void refreshProfile()} /> : null}
              {profile ? (
                <div className="studio-profile-card">
                  <div className="studio-profile-card__media">
                    {profile.avatarUrl ? <img alt={profile.ownerNickname} src={profile.avatarUrl} /> : <div className="studio-detail__placeholder">头像</div>}
                    <div className="studio-profile-card__upload">
                      <input
                        accept="image/png,image/jpeg,image/webp"
                        id="studio-profile-avatar-upload"
                        style={{ display: "none" }}
                        type="file"
                        onChange={async (event) => {
                          const file = event.target.files?.[0];
                          event.currentTarget.value = "";
                          if (!file) return;
                          await handleUploadAvatar(file);
                        }}
                      />
                      <label className="dt-button dt-button--secondary publish-upload-trigger" htmlFor="studio-profile-avatar-upload">
                        {avatarUploading ? "上传中..." : "上传头像"}
                      </label>
                    </div>
                  </div>
                  <div className="studio-profile-card__content">
                    <h3>{profile.studioName}</h3>
                    <dl>
                      <div><dt>负责人昵称</dt><dd>{profile.ownerNickname}</dd></div>
                      <div><dt>负责人手机号</dt><dd>{profile.ownerPhone}</dd></div>
                      <div><dt>审核策略</dt><dd>{profile.reviewStrategyText}</dd></div>
                      <div><dt>分润比例</dt><dd>{profile.shareRatio}</dd></div>
                      <div><dt>合作状态</dt><dd>{profile.activeText}</dd></div>
                      <div><dt>实名状态</dt><dd>{profile.verified ? `已实名（${profile.realName ?? "-" }）` : "未实名"}</dd></div>
                    </dl>
                    <div className="studio-profile-form">
                      <label>
                        工作室名称
                        <input
                          maxLength={100}
                          placeholder="请输入工作室展示名称"
                          value={profileForm.studioName}
                          onChange={(event) => setProfileForm((current) => ({ ...current, studioName: event.target.value }))}
                        />
                      </label>
                      <label>
                        联系电话
                        <input
                          inputMode="numeric"
                          maxLength={11}
                          placeholder="请输入 11 位手机号"
                          value={profileForm.contactPhone}
                          onChange={(event) => setProfileForm((current) => ({ ...current, contactPhone: event.target.value }))}
                        />
                      </label>
                      <label>
                        联系微信
                        <input
                          maxLength={64}
                          placeholder="请输入工作室联系微信"
                          value={profileForm.contactWechat}
                          onChange={(event) => setProfileForm((current) => ({ ...current, contactWechat: event.target.value }))}
                        />
                      </label>
                      <label>
                        工作室简介
                        <textarea
                          maxLength={500}
                          placeholder="介绍工作室定位、承接能力和售后承诺"
                          rows={5}
                          value={profileForm.description}
                          onChange={(event) => setProfileForm((current) => ({ ...current, description: event.target.value }))}
                        />
                      </label>
                      <div className="studio-profile-form__actions">
                        <Button onClick={() => void handleSaveProfile()} disabled={profileSaving}>
                          {profileSaving ? "正在保存" : "保存资料"}
                        </Button>
                        <Button
                          kind="secondary"
                          onClick={() => setProfileForm({
                            studioName: profile.studioName,
                            description: profile.description ?? "",
                            contactPhone: profile.contactPhone ?? "",
                            contactWechat: profile.contactWechat ?? "",
                          })}
                        >
                          恢复当前资料
                        </Button>
                      </div>
                    </div>
                  </div>
                </div>
              ) : null}
            </SurfaceCard>

            <div className="studio-finance-grid studio-settings-grid">
              <SurfaceCard eyebrow="账号设置" title="登录密码与安全策略">
                {settingsLoading ? <LoadingBlock title="正在加载安全设置" compact /> : null}
                {settingsError ? <ErrorBlock title="安全设置加载失败" message={settingsError} compact onRetry={() => void refreshSettings()} /> : null}
                {settingsProfile ? (
                  <div className="studio-settings-panel">
                    <div className="studio-profile-card__content">
                      <dl>
                        <div><dt>当前登录账号</dt><dd>{settingsProfile.nickname}</dd></div>
                        <div><dt>实名状态</dt><dd>{settingsProfile.verified ? "已实名" : "未实名"}</dd></div>
                        <div><dt>身份证号</dt><dd>{settingsProfile.maskedIdCardNo || "未提交"}</dd></div>
                        <div><dt>提现账户</dt><dd>{settingsProfile.withdrawAccountLabel || "未绑定"}</dd></div>
                      </dl>
                    </div>

                    <div className="studio-profile-form">
                      <label>
                        原密码
                        <input
                          type="password"
                          placeholder="请输入当前登录密码"
                          value={passwordForm.currentPassword}
                          onChange={(event) => setPasswordForm((current) => ({ ...current, currentPassword: event.target.value }))}
                        />
                      </label>
                      <label>
                        新密码
                        <input
                          type="password"
                          placeholder="6-18 位，需同时包含字母和数字"
                          value={passwordForm.nextPassword}
                          onChange={(event) => setPasswordForm((current) => ({ ...current, nextPassword: event.target.value }))}
                        />
                      </label>
                      <label>
                        确认新密码
                        <input
                          type="password"
                          placeholder="再次输入新的登录密码"
                          value={passwordForm.confirmPassword}
                          onChange={(event) => setPasswordForm((current) => ({ ...current, confirmPassword: event.target.value }))}
                        />
                      </label>
                      <div className="studio-profile-form__actions">
                        <Button onClick={() => void handleChangePassword()} disabled={passwordSaving}>
                          {passwordSaving ? "正在提交" : "修改登录密码"}
                        </Button>
                      </div>
                    </div>

                    <div className="studio-settings-switches">
                      <label>
                        <input
                          checked={securityForm.loginAlertEnabled}
                          type="checkbox"
                          onChange={(event) => setSecurityForm((current) => ({ ...current, loginAlertEnabled: event.target.checked }))}
                        />
                        <span>
                          <strong>异地登录提醒</strong>
                          <small>登录位置异常时提醒负责人，便于及时风控处理。</small>
                        </span>
                      </label>
                      <label>
                        <input
                          checked={securityForm.secondaryVerifyEnabled}
                          type="checkbox"
                          onChange={(event) => setSecurityForm((current) => ({ ...current, secondaryVerifyEnabled: event.target.checked }))}
                        />
                        <span>
                          <strong>敏感操作二次验证</strong>
                          <small>提现、解绑等动作要求短信校验，降低误操作和盗号风险。</small>
                        </span>
                      </label>
                      <div className="studio-profile-form__actions">
                        <Button kind="secondary" onClick={() => void handleSaveSecurity()} disabled={securitySaving}>
                          {securitySaving ? "正在保存" : "保存安全策略"}
                        </Button>
                      </div>
                    </div>
                  </div>
                ) : null}
              </SurfaceCard>

              <SurfaceCard eyebrow="绑定管理" title="手机号与微信绑定">
                {settingsLoading ? <LoadingBlock title="正在加载绑定信息" compact /> : null}
                {settingsError ? <ErrorBlock title="绑定信息加载失败" message={settingsError} compact onRetry={() => void refreshSettings()} /> : null}
                {settingsProfile ? (
                  <div className="studio-settings-panel">
                    <div className="studio-profile-card__content">
                      <dl>
                        <div><dt>当前手机号</dt><dd>{settingsProfile.phoneBound ? settingsProfile.phone : "未绑定"}</dd></div>
                        <div><dt>微信绑定</dt><dd>{settingsProfile.wechatBound ? "已绑定" : "未绑定"}</dd></div>
                        <div><dt>实名人</dt><dd>{settingsProfile.realName || "未实名"}</dd></div>
                        <div><dt>实名审核</dt><dd>{settingsProfile.realNameStatus === "REJECTED" ? `驳回：${settingsProfile.realNameRejectReason || "待重新提交"}` : settingsProfile.realNameStatus === "APPROVED" ? "已通过" : "未提交"}</dd></div>
                      </dl>
                    </div>

                    <div className="studio-profile-form">
                      <label>
                        新绑定手机号
                        <input
                          inputMode="numeric"
                          maxLength={11}
                          placeholder="请输入新的手机号"
                          value={phoneForm.phone}
                          onChange={(event) => setPhoneForm((current) => ({ ...current, phone: event.target.value }))}
                        />
                      </label>
                      <label>
                        绑定验证码
                        <input
                          inputMode="numeric"
                          maxLength={6}
                          placeholder="输入新手机号收到的验证码"
                          value={phoneForm.bindCode}
                          onChange={(event) => setPhoneForm((current) => ({ ...current, bindCode: event.target.value }))}
                        />
                      </label>
                      <div className="studio-profile-form__actions">
                        <Button kind="secondary" onClick={() => void handleSendBindCode()} disabled={bindCodeSending}>
                          {bindCodeSending ? "发送中" : "发送绑定验证码"}
                        </Button>
                        <Button onClick={() => void handleChangePhone()} disabled={phoneActionLoading}>
                          {phoneActionLoading ? "处理中" : "更新绑定手机号"}
                        </Button>
                      </div>
                    </div>

                    <div className="studio-profile-form studio-settings-readonly">
                      <label>
                        解绑验证码
                        <input
                          inputMode="numeric"
                          maxLength={6}
                          placeholder="输入当前手机号收到的安全验证码"
                          value={phoneForm.unbindCode}
                          onChange={(event) => setPhoneForm((current) => ({ ...current, unbindCode: event.target.value }))}
                        />
                      </label>
                      <div className="studio-profile-form__actions">
                        <Button kind="secondary" onClick={() => void handleSendUnbindCode()} disabled={unbindCodeSending || !settingsProfile.phoneBound}>
                          {unbindCodeSending ? "发送中" : "发送解绑验证码"}
                        </Button>
                        <Button kind="ghost" onClick={() => void handleUnbindPhone()} disabled={phoneActionLoading || !settingsProfile.phoneBound}>
                          {phoneActionLoading ? "处理中" : "解绑手机号"}
                        </Button>
                      </div>
                      <div className="studio-profile-form__actions">
                        <Button kind="ghost" onClick={() => void handleUnbindWechatAction()} disabled={phoneActionLoading || !settingsProfile.wechatBound}>
                          {phoneActionLoading ? "处理中" : "解绑微信登录"}
                        </Button>
                      </div>
                    </div>
                  </div>
                ) : null}
              </SurfaceCard>
            </div>
          </>
        ) : null}
      </main>
    </div>
  );
}

function SummaryPills({ summary, labels }: { summary: Record<string, number>; labels: Record<string, string> }) {
  return (
    <div className="studio-summary-pills">
      {Object.entries(summary).map(([key, value]) => (
        <div className="studio-summary-pill" key={key}>
          <strong>{value}</strong>
          <span>{labels[key] ?? key}</span>
        </div>
      ))}
    </div>
  );
}

function LoadingBlock({ title, compact = false }: { title: string; compact?: boolean }) {
  return (
    <div className={`studio-status-block ${compact ? "studio-status-block--compact" : ""}`.trim()}>
      <StatusState
        title={title}
        description="正在同步最新工作室数据，请稍候。"
        tone="neutral"
        action={compact ? null : undefined}
      />
    </div>
  );
}

function ErrorBlock({ title, message, onRetry, compact = false }: { title: string; message: string; onRetry: () => void; compact?: boolean }) {
  return (
    <div className={`studio-status-block ${compact ? "studio-status-block--compact" : ""}`.trim()}>
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
    <div className="studio-status-block studio-status-block--compact">
      <StatusState title={title} description="当前没有可展示数据，可以切换筛选条件或等待业务数据进入。" tone="neutral" />
    </div>
  );
}

function renderOperatorPermission(permission: string) {
  if (permission === "LISTING") return "商品管理";
  if (permission === "ORDER") return "订单处理";
  if (permission === "AFTER_SALE") return "售后处理";
  if (permission === "FINANCE") return "财务查看";
  if (permission === "PROFILE") return "资料维护";
  return permission;
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
  const [phone, setPhone] = useState("13900139000");
  const [password, setPassword] = useState("Pass123");
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
    <div className="studio-shell studio-auth-shell">
      <SurfaceCard eyebrow={badge} title={title}>
        <form className="studio-login" onSubmit={handleSubmit}>
          <p>{description}</p>
          <label>
            手机号
            <input value={phone} onChange={(event) => setPhone(event.target.value)} placeholder="请输入工作室管理员手机号" />
          </label>
          <label>
            密码
            <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="请输入密码" />
          </label>
          {error ? <p className="studio-login__error">{error}</p> : null}
          <div className="studio-login__actions">
            <Button type="submit" disabled={submitting}>{submitting ? "登录中..." : "进入工作室后台"}</Button>
          </div>
        </form>
      </SurfaceCard>
    </div>
  );
}
