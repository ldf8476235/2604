import { Button, StatusState } from "@delta/ui";
import { useEffect, useMemo, useRef, useState, type MouseEvent as ReactMouseEvent, type PointerEvent as ReactPointerEvent, type ReactNode } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { loadRealNameProfile } from "../auth/auth-api";
import { useAuth } from "../auth/auth-context";
import {
  loadOssPreview,
  loadMyListingDetail,
  loadPublishMeta,
  submitPublish,
  uploadOssFileDirect,
  updateMyListing,
  type ExtraItemMeta,
  type MyListingDetail,
  type OptionItem,
  type PublishMeta,
  type PublishPayload,
  type PublishSubmitResult,
  type RegionProvince,
} from "../modules/publish/publish-api";
import { getPublishAgreementContent, type PublishAgreementContent } from "../modules/publish/publish-agreements";

const PUBLISH_DROPDOWN_CLOSE_EVENT = "delta-trade:publish-dropdown-close";
let publishDropdownOpenBlockedUntil = 0;

function closeOtherPublishDropdowns(id: string) {
  window.dispatchEvent(new CustomEvent(PUBLISH_DROPDOWN_CLOSE_EVENT, { detail: { id } }));
}

function blockPublishDropdownOpen(durationMs = 600) {
  publishDropdownOpenBlockedUntil = Date.now() + durationMs;
}

function isPublishDropdownOpenBlocked() {
  return Date.now() < publishDropdownOpenBlockedUntil;
}

type ExtraItemDraft = {
  count: string;
  chargeMode: "gift" | "charge";
};

type PublishFormState = {
  provinceCode: string;
  cityCode: string;
  deliveryMethod: string;
  alwaysOnline: boolean;
  deliveryStartHour: string;
  deliveryEndHour: string;
  safeBoxLevel: string;
  staminaLevel: string;
  carryLevel: string;
  diveLevel: string;
  banRecord: "" | "has" | "none";
  faceOwned: "" | "yes" | "no";
  unlockSaeed: "" | "yes" | "no";
  accountLevel: string;
  rankName: string;
  hafCurrency: string;
  secretKd: string;
  title: string;
  description: string;
  operatorCount: string;
  operators: string[];
  weapons: string[];
  knifeSkins: string[];
  redSkins: string[];
  weaponSkins: string[];
  goldSkins: string[];
  defaultSpend: string;
  rentalDays: string;
  exchangeRateType: string;
  customExchangeRate: string;
  compensationPlan: string;
  remarks: string;
  otherItems: string;
  price: string;
  deposit: string;
  negotiable: boolean;
  modCodes: string[];
  agreements: string[];
  extraItems: Record<string, ExtraItemDraft>;
};

type UploadedAsset = {
  objectKey: string;
  previewUrl: string;
  filename: string;
  source: "oss" | "mock";
};

type EditSeed = {
  form: PublishFormState;
  imageAssets: UploadedAsset[];
  videoAsset: UploadedAsset | null;
  punishmentAsset: UploadedAsset | null;
};

type RentalQuote = {
  ratio: number;
  price: number;
  detail: string;
  formula: string;
};

type FieldErrors = Partial<Record<
  keyof PublishFormState | "provinceCode" | "cityCode" | "imageKeys" | "videoKey" | "punishmentImageKey" | "extraItems",
  string
>>;

const IMAGE_INPUT_ID = "publish-image-input";
const VIDEO_INPUT_ID = "publish-video-input";
const PUNISHMENT_INPUT_ID = "publish-punishment-input";

const DEFAULT_DELIVERY_METHODS: OptionItem[] = [
  { value: "wechat_qr", label: "微信扫码" },
  { value: "qq_account", label: "QQ账密" },
  { value: "qq_qr", label: "QQ扫码" },
  { value: "steam_cn", label: "Steam国服" },
  { value: "steam_global", label: "Steam国际服" },
];

const DEFAULT_HOUR_OPTIONS: OptionItem[] = Array.from({ length: 25 }, (_, hour) => ({
  value: String(hour),
  label: String(hour),
}));

const DEFAULT_RANKS: OptionItem[] = [
  { value: "bronze", label: "青铜" },
  { value: "silver", label: "白银" },
  { value: "gold", label: "黄金" },
  { value: "platinum", label: "铂金" },
  { value: "diamond", label: "钻石" },
  { value: "blackhawk", label: "黑鹰" },
  { value: "summit", label: "巅峰" },
];

const DEFAULT_LEVEL_OPTIONS: OptionItem[] = Array.from({ length: 6 }, (_, index) => ({
  value: String(index + 1),
  label: `${index + 1}级`,
}));

const DEFAULT_SAFE_BOX_OPTIONS: OptionItem[] = [
  { value: "1", label: "基础安全箱(1*2)" },
  { value: "2", label: "进阶安全箱(2*2)" },
  { value: "3", label: "高级安全箱(2*3)" },
  { value: "4", label: "顶级安全箱(3*3)" },
];

const DEFAULT_STAMINA_OPTIONS: OptionItem[] = [
  { value: "4", label: "4级" },
  { value: "5", label: "5级" },
  { value: "6", label: "6级" },
  { value: "7", label: "7级" },
];

const DEFAULT_CARRY_OPTIONS: OptionItem[] = [
  { value: "4", label: "4级" },
  { value: "5", label: "5级" },
  { value: "6", label: "6级" },
  { value: "7", label: "7级" },
];

const DEFAULT_DIVE_OPTIONS: OptionItem[] = [
  { value: "0", label: "0级" },
  { value: "1", label: "1级" },
  { value: "2", label: "2级" },
  { value: "3", label: "3级" },
  { value: "-1", label: "无" },
];

const DEFAULT_DEFAULT_SPEND_OPTIONS: OptionItem[] = [
  { value: "10m", label: "10M/天" },
  { value: "20m_plus_2", label: "20M/天+2" },
  { value: "30m_plus_3", label: "30M/天+3" },
  { value: "40m_plus_4", label: "40M/天+4" },
  { value: "50m_plus_5", label: "50M/天+5" },
];

const DEFAULT_EXCHANGE_RATE_OPTIONS: OptionItem[] = [
  { value: "default", label: "默认比例" },
  { value: "custom", label: "自定义比例" },
  { value: "accelerated", label: "特惠比例" },
];

const DEFAULT_COMPENSATION_OPTIONS: OptionItem[] = [
  { value: "normal", label: "普通赔付" },
  { value: "full", label: "全额包赔" },
];

const DEFAULT_AGREEMENT_OPTIONS: OptionItem[] = [
  { value: "virtual_asset", label: "《虚拟资产出售协议》" },
  { value: "owner_protocol", label: "《号主协议》" },
  { value: "full_coverage", label: "《全额包赔协议》" },
];

const DEFAULT_KNIFE_SKINS: OptionItem[] = [
  { value: "坠星者", label: "坠星者" },
  { value: "处刑者", label: "处刑者" },
  { value: "暗星", label: "暗星" },
  { value: "龙牙", label: "龙牙" },
  { value: "信条", label: "信条" },
  { value: "影锋", label: "影锋" },
  { value: "电锯惊魂", label: "电锯惊魂" },
  { value: "北极星", label: "北极星" },
  { value: "黑海", label: "黑海" },
  { value: "怜悯", label: "怜悯" },
  { value: "赤霄", label: "赤霄" },
];

const DEFAULT_RED_SKINS: OptionItem[] = [
  { value: "凌霄戍卫", label: "凌霄戍卫" },
  { value: "维什戴尔", label: "维什戴尔" },
  { value: "蚀金玫瑰", label: "蚀金玫瑰" },
  { value: "水墨云图", label: "水墨云图" },
  { value: "午夜邮差", label: "午夜邮差" },
  { value: "天际线", label: "天际线" },
];

const DEFAULT_WEAPON_SKINS: OptionItem[] = [
  { value: "KC17-造物纪元", label: "KC17-造物纪元" },
  { value: "电玩高手-MP7", label: "电玩高手-MP7" },
  { value: "电玩高手-M250", label: "电玩高手-M250" },
  { value: "AS Val突击步枪-悬赏令", label: "AS Val突击步枪-悬赏令" },
  { value: "M7棱镜攻势", label: "M7棱镜攻势" },
  { value: "M4棱镜攻势", label: "M4棱镜攻势" },
  { value: "K416-命运", label: "K416-命运" },
  { value: "SCAR-电玩", label: "SCAR-电玩" },
  { value: "腾龙-气象感应", label: "腾龙-气象感应" },
  { value: "AUG-气象感应", label: "AUG-气象感应" },
  { value: "QBZ95-王牌之剑", label: "QBZ95-王牌之剑" },
  { value: "Vctor-冲锋枪-美杜莎", label: "Vctor-冲锋枪-美杜莎" },
];

const DEFAULT_GOLD_SKINS: OptionItem[] = [
  { value: "鸟兽兽-荒原猎手", label: "鸟兽兽-荒原猎手" },
  { value: "露娜-金牌射手", label: "露娜-金牌射手" },
  { value: "牧羊人-街头之星", label: "牧羊人-街头之星" },
  { value: "蜂医-危险物质", label: "蜂医-危险物质" },
  { value: "蜂医-送葬人", label: "蜂医-送葬人" },
  { value: "无名-夜鹰", label: "无名-夜鹰" },
  { value: "威龙-壮志凌云", label: "威龙-壮志凌云" },
  { value: "威龙-蛟龙特战队", label: "威龙-蛟龙特战队" },
  { value: "威龙-铁面判官", label: "威龙-铁面判官" },
  { value: "威龙-吴彦祖", label: "威龙-吴彦祖" },
  { value: "红狼-电锯惊魂", label: "红狼-电锯惊魂" },
];

const DEFAULT_OPERATORS: OptionItem[] = [
  { value: "red-wolf", label: "红狼" },
  { value: "saeed", label: "赛依德" },
  { value: "vyshdel", label: "维什戴尔" },
  { value: "beeast", label: "鸟兽兽" },
  { value: "lingxiao", label: "凌霄戍卫" },
  { value: "tianji", label: "天际线" },
];

const DEFAULT_WEAPONS: OptionItem[] = [
  { value: "awm", label: "AWM" },
  { value: "m4a1", label: "M4A1" },
  { value: "akm", label: "AKM" },
  { value: "vector", label: "Vector" },
  { value: "scar", label: "SCAR" },
  { value: "sr25", label: "SR-25" },
];

const DEFAULT_MOD_CODES: OptionItem[] = [
  { value: "m416-stable", label: "M416 稳定压枪码" },
  { value: "akm-rapid", label: "AKM 急速爆发码" },
  { value: "vector-close", label: "Vector 近战拉枪码" },
];

const DEFAULT_EXTRA_ITEMS: ExtraItemMeta[] = [
  { code: "awm_bullet", label: "AWM子弹", unitLabel: "发", unitPrice: 0.8 },
  { code: "bullet_level_6", label: "6级子弹", unitLabel: "组", unitPrice: 6 },
  { code: "helmet_level_6", label: "6级头盔", unitLabel: "个", unitPrice: 2 },
  { code: "armor_level_6", label: "6级护甲", unitLabel: "个", unitPrice: 3 },
  { code: "barrett_bullet", label: "巴雷特子弹", unitLabel: "发", unitPrice: 0.8 },
  { code: "premium_insurance", label: "顶级保险卡", unitLabel: "张", unitPrice: 5 },
  { code: "premium_coffee", label: "高级咖啡豆", unitLabel: "个", unitPrice: 2.5 },
  { code: "premium_bullet_part", label: "高级子弹零件", unitLabel: "个", unitPrice: 3 },
];

const INITIAL_FORM: PublishFormState = {
  provinceCode: "",
  cityCode: "",
  deliveryMethod: "",
  alwaysOnline: false,
  deliveryStartHour: "",
  deliveryEndHour: "",
  safeBoxLevel: "",
  staminaLevel: "",
  carryLevel: "",
  diveLevel: "",
  banRecord: "",
  faceOwned: "",
  unlockSaeed: "",
  accountLevel: "",
  rankName: "",
  hafCurrency: "",
  secretKd: "",
  title: "",
  description: "",
  operatorCount: "",
  operators: [],
  weapons: [],
  knifeSkins: [],
  redSkins: [],
  weaponSkins: [],
  goldSkins: [],
  defaultSpend: "10m",
  rentalDays: "",
  exchangeRateType: "default",
  customExchangeRate: "",
  compensationPlan: "",
  remarks: "",
  otherItems: "",
  price: "",
  deposit: "",
  negotiable: false,
  modCodes: [],
  agreements: [],
  extraItems: {},
};

export function PublishPage() {
  const { isAuthenticated, openAuthModal, saveSession, session } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const [meta, setMeta] = useState<PublishMeta | null>(null);
  const [form, setForm] = useState<PublishFormState>(INITIAL_FORM);
  const [imageAssets, setImageAssets] = useState<UploadedAsset[]>([]);
  const [videoAsset, setVideoAsset] = useState<UploadedAsset | null>(null);
  const [punishmentAsset, setPunishmentAsset] = useState<UploadedAsset | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [uploadingImages, setUploadingImages] = useState(false);
  const [uploadingVideo, setUploadingVideo] = useState(false);
  const [uploadingPunishment, setUploadingPunishment] = useState(false);
  const [pageError, setPageError] = useState("");
  const [submitError, setSubmitError] = useState("");
  const [fieldErrors, setFieldErrors] = useState<FieldErrors>({});
  const [successResult, setSuccessResult] = useState<PublishSubmitResult | null>(null);
  const [activeAgreement, setActiveAgreement] = useState<PublishAgreementContent | null>(null);
  const [editSeed, setEditSeed] = useState<EditSeed | null>(null);
  const imageInputRef = useRef<HTMLInputElement | null>(null);
  const videoInputRef = useRef<HTMLInputElement | null>(null);
  const punishmentInputRef = useRef<HTMLInputElement | null>(null);
  const editListingNo = searchParams.get("edit")?.trim() || "";

  useEffect(() => {
    if (!isAuthenticated || session?.profile.verified) {
      return;
    }
    let disposed = false;
    async function verifyLatestRealNameStatus() {
      try {
        const realName = await loadRealNameProfile();
        if (disposed || realName.verified || !session) {
          if (!disposed && realName.verified && session) {
            saveSession({
              ...session,
              profile: {
                ...session.profile,
                verified: true,
              },
            });
          }
          return;
        }
      } catch {
        if (disposed) {
          return;
        }
      }
      window.dispatchEvent(new CustomEvent("dt:app-toast", { detail: "未实名不允许发布账号" }));
      navigate("/profile?tab=verify", { replace: true });
    }
    void verifyLatestRealNameStatus();
    return () => {
      disposed = true;
    };
  }, [isAuthenticated, navigate, saveSession, session, session?.profile.verified]);

  useEffect(() => {
    if (!isAuthenticated || !session?.profile.verified) {
      setLoading(false);
      return;
    }

    let disposed = false;
    async function bootstrap() {
      try {
        setLoading(true);
        setPageError("");
        const nextMeta = await loadPublishMeta();
        if (!disposed) {
          setMeta(nextMeta);
          const nextExtraItems = nextMeta.extraItems?.length ? nextMeta.extraItems : DEFAULT_EXTRA_ITEMS;
          if (editListingNo) {
            const detail = await loadMyListingDetail(editListingNo);
            if (!disposed) {
              const seed = buildEditSeed(detail, nextExtraItems);
              applyEditSeed(seed);
              setEditSeed(seed);
            }
          } else {
            resetPublishForm(nextExtraItems);
            setEditSeed(null);
          }
        }
      } catch (error) {
        if (!disposed) {
          setPageError(getErrorMessage(error));
        }
      } finally {
        if (!disposed) {
          setLoading(false);
        }
      }
    }

    void bootstrap();
    return () => {
      disposed = true;
    };
  }, [editListingNo, isAuthenticated, session?.profile.verified]);

  useEffect(() => {
    if (form.banRecord === "has") {
      return;
    }
    if (punishmentAsset) {
      setPunishmentAsset(null);
    }
    setFieldErrors((previous) => {
      if (!previous.punishmentImageKey) {
        return previous;
      }
      return { ...previous, punishmentImageKey: undefined };
    });
  }, [form.banRecord, punishmentAsset]);

  const deliveryMethods = meta?.deliveryMethods?.length ? meta.deliveryMethods : DEFAULT_DELIVERY_METHODS;
  const hourOptions = (meta?.hourOptions?.length ? meta.hourOptions : DEFAULT_HOUR_OPTIONS).map((item) => ({
    ...item,
    label: item.label.replace(/:00$/, ""),
  }));
  const ranks = meta?.ranks?.length ? meta.ranks : DEFAULT_RANKS;
  const safeBoxLevels = meta?.safeBoxLevels?.length ? meta.safeBoxLevels : DEFAULT_SAFE_BOX_OPTIONS;
  const staminaLevels = meta?.staminaLevels?.length ? meta.staminaLevels : DEFAULT_STAMINA_OPTIONS;
  const carryLevels = meta?.carryLevels?.length ? meta.carryLevels : DEFAULT_CARRY_OPTIONS;
  const diveLevels = meta?.diveLevels?.length ? meta.diveLevels : DEFAULT_DIVE_OPTIONS;
  const defaultSpendOptions = DEFAULT_DEFAULT_SPEND_OPTIONS;
  const exchangeRateOptions = meta?.exchangeRateOptions?.length ? meta.exchangeRateOptions : DEFAULT_EXCHANGE_RATE_OPTIONS;
  const compensationPlans = meta?.compensationPlans?.length ? meta.compensationPlans : DEFAULT_COMPENSATION_OPTIONS;
  const agreementOptions = meta?.agreementOptions?.length ? meta.agreementOptions : DEFAULT_AGREEMENT_OPTIONS;
  const knifeSkins = meta?.knifeSkins?.length ? meta.knifeSkins : DEFAULT_KNIFE_SKINS;
  const redSkins = meta?.redSkins?.length ? meta.redSkins : DEFAULT_RED_SKINS;
  const weaponSkinCatalog = meta?.weaponSkinCatalog?.length ? meta.weaponSkinCatalog : DEFAULT_WEAPON_SKINS;
  const goldSkins = meta?.goldSkins?.length ? meta.goldSkins : DEFAULT_GOLD_SKINS;
  const extraItemsMeta = meta?.extraItems?.length ? meta.extraItems : DEFAULT_EXTRA_ITEMS;

  const extraIncome = useMemo(
    () => calculateExtraIncome(form.extraItems, extraItemsMeta),
    [extraItemsMeta, form.extraItems],
  );
  const rentalBaseRatio = resolveRentalBaseRatio(form, meta?.defaultExchangeRate);
  const rentalQuote = useMemo(() => calculateRentalQuote(form, rentalBaseRatio), [form, rentalBaseRatio]);
  const calculatedPrice = rentalQuote ? formatDecimal(rentalQuote.price) : "";
  const displayedExchangeRateValue = form.customExchangeRate;
  const defaultExchangeRateWan = formatWanRate(meta?.defaultExchangeRate);
  const promotionalExchangeRateWan = formatWanRate(promotionalExchangeRate(meta?.defaultExchangeRate));
  const defaultDisplayExchangeRateWan = rentalQuote && form.exchangeRateType === "default" ? String(rentalQuote.ratio) : defaultExchangeRateWan;
  const sellerCommissionRateText = formatCommissionRate(meta?.personalSellerCommissionRate);

  useEffect(() => {
    const nextRentalDays = calculateRentalDays(form.hafCurrency, form.defaultSpend);
    setForm((previous) => {
      if (previous.rentalDays === nextRentalDays) {
        return previous;
      }
      return { ...previous, rentalDays: nextRentalDays };
    });
    if (nextRentalDays) {
      setFieldErrors((previous) => ({ ...previous, rentalDays: undefined }));
    }
  }, [form.defaultSpend, form.hafCurrency]);

  if (!isAuthenticated) {
    return (
      <main className="publish-page">
        <div className="dt-container">
          <StatusState
            title="登录后才能发布账号"
            description="发布账号涉及卖家身份和审核策略校验，当前页面必须先完成登录。"
            action={<Button onClick={() => openAuthModal("login")}>立即登录</Button>}
          />
        </div>
      </main>
    );
  }

  if (!session?.profile.verified) {
    return (
      <main className="publish-page">
        <div className="dt-container">
          <StatusState
            title="未实名不允许发布账号"
            description="发布账号前需要先完成实名认证。"
            tone="error"
            action={<Button onClick={() => navigate("/profile?tab=verify", { replace: true })}>去实名认证</Button>}
          />
        </div>
      </main>
    );
  }

  if (loading) {
    return (
      <main className="publish-page">
        <div className="dt-container">
          <StatusState title="发布配置加载中" description="正在加载行政区、下拉字典、审核策略和上传能力。" />
        </div>
      </main>
    );
  }

  if (pageError || !meta) {
    return (
      <main className="publish-page">
        <div className="dt-container">
          <StatusState
            title="发布页初始化失败"
            description={pageError || "发布页配置缺失，请稍后重试。"}
            tone="error"
            action={<Button kind="secondary" onClick={() => window.location.reload()}>重新加载</Button>}
          />
        </div>
      </main>
    );
  }

  const canSubmit = !submitting && !uploadingImages && !uploadingVideo && !uploadingPunishment;

  return (
    <main className="publish-page">
      <div className="dt-container publish-shell">
        <section className="publish-toolbar-card">
          <div>
            <p className="publish-toolbar-card__eyebrow">{editListingNo ? "编辑模式" : "卖家工作台"}</p>
            <h1>{editListingNo ? "编辑已发布账号" : "账号发布"}</h1>
          </div>
          <div className="publish-toolbar-card__actions">
            <Link className="dt-button dt-button--secondary" to="/publish/mine">
              我的发布
            </Link>
          </div>
        </section>

        {editListingNo && editSeed ? (
          <section className="publish-edit-banner">
            <div>
              <strong>正在编辑：{form.title || editListingNo}</strong>
              <p>编辑仅对待审核、已驳回、已下架账号开放；保存后将根据个人/工作室审核策略重新流转。</p>
            </div>
            <Link className="publish-inline-link" to="/publish/mine">
              返回我的发布
            </Link>
          </section>
        ) : null}

        {successResult ? (
          <section className="publish-success-card">
            <div>
              <p className="publish-success-card__eyebrow">提交成功</p>
              <h2>{successResult.listingNo}</h2>
              <p>{successResult.message}</p>
              <p>系统建议价：{formatCurrency(successResult.suggestedPrice)}</p>
              <p>{successResult.estimateDetail}</p>
            </div>
            <div className="publish-success-card__actions">
              {editListingNo ? (
                <Link className="dt-button dt-button--secondary" to="/publish/mine">
                  返回我的发布
                </Link>
              ) : null}
              <Button
                kind="secondary"
                onClick={() => {
                  if (editListingNo && editSeed) {
                    applyEditSeed(editSeed);
                  } else {
                    resetPublishForm(extraItemsMeta);
                  }
                  setSuccessResult(null);
                }}
              >
                {editListingNo ? "继续编辑" : "继续发布"}
              </Button>
              <Button onClick={() => window.scrollTo({ top: 0, behavior: "smooth" })}>查看提交结果</Button>
            </div>
          </section>
        ) : null}

        <div className="publish-layout publish-layout--simple">
          <div className="publish-main">
            <section className="publish-card">
              <div className="publish-card__header">
                <div>
                  <h2>基础信息</h2>
                </div>
              </div>
              <div className="publish-grid publish-grid--2">
                <Field label="地区选择" required error={fieldErrors.provinceCode || fieldErrors.cityCode}>
                  <RegionPicker
                    cityCode={form.cityCode}
                    provinceCode={form.provinceCode}
                    regions={meta.regions}
                    valueText={displayRegion(meta.regions, form.provinceCode, form.cityCode)}
                    onChange={(provinceCode, cityCode) => {
                      setForm((previous) => ({ ...previous, provinceCode, cityCode }));
                      setFieldErrors((previous) => ({ ...previous, provinceCode: undefined, cityCode: undefined }));
                      setSubmitError("");
                    }}
                  />
                </Field>
                <Field label="上号方式" required error={fieldErrors.deliveryMethod} helper="配合租客登陆账号的方式">
                  <OptionPopover
                    options={deliveryMethods}
                    placeholder="请选择"
                    value={form.deliveryMethod}
                    onChange={(value) => updateField("deliveryMethod", value)}
                  />
                </Field>
                <Field label="最早时间" required error={fieldErrors.deliveryStartHour}>
                  <OptionPopover
                    options={hourOptions}
                    placeholder="请选择最早时间"
                    value={form.deliveryStartHour}
                    onChange={(value) => updateField("deliveryStartHour", value)}
                  />
                </Field>
                <Field label="最晚时间" required error={fieldErrors.deliveryEndHour} helper="可以辅助上号 0-24 小时制">
                  <OptionPopover
                    options={hourOptions}
                    placeholder="请选择最晚时间"
                    value={form.deliveryEndHour}
                    onChange={(value) => updateField("deliveryEndHour", value)}
                  />
                </Field>
                <Field label="是否全天在线" required helper="开启后首页可进入 24h 随时上号区">
                  <BinaryChoiceGroup
                    options={[
                      { value: "true", label: "是" },
                      { value: "false", label: "否" },
                    ]}
                    value={String(form.alwaysOnline)}
                    onChange={(value) => updateField("alwaysOnline", value === "true")}
                  />
                </Field>
              </div>
            </section>

            <section className="publish-card">
              <div className="publish-card__header">
                <div>
                  <h2>账号属性</h2>
                </div>
              </div>
              <div className="publish-grid publish-grid--3">
                <Field label="安全箱" required error={fieldErrors.safeBoxLevel} helper="指赛季永久保险箱容量，非临时体验卡">
                  <OptionPopover
                    options={safeBoxLevels}
                    placeholder="请选择"
                    value={form.safeBoxLevel}
                    onChange={(value) => updateField("safeBoxLevel", value)}
                  />
                </Field>
                <Field label="体力" required error={fieldErrors.staminaLevel}>
                  <OptionPopover
                    options={staminaLevels}
                    placeholder="请选择"
                    value={form.staminaLevel}
                    onChange={(value) => updateField("staminaLevel", value)}
                  />
                </Field>
                <Field label="负重" required error={fieldErrors.carryLevel}>
                  <OptionPopover
                    options={carryLevels}
                    placeholder="请选择"
                    value={form.carryLevel}
                    onChange={(value) => updateField("carryLevel", value)}
                  />
                </Field>
                <Field label="潜水等级" required error={fieldErrors.diveLevel}>
                  <OptionPopover
                    options={diveLevels}
                    placeholder="请选择"
                    value={form.diveLevel}
                    onChange={(value) => updateField("diveLevel", value)}
                  />
                </Field>
                <Field label="账号等级" required error={fieldErrors.accountLevel}>
                  <input value={form.accountLevel} onChange={(event) => updateField("accountLevel", event.target.value)} placeholder="请输入账号等级" />
                </Field>
                <Field label="账号段位" required error={fieldErrors.rankName}>
                  <OptionPopover
                    options={ranks}
                    placeholder="当前赛季段位"
                    value={form.rankName}
                    onChange={(value) => updateField("rankName", value)}
                  />
                </Field>
                <Field label="封禁记录" required error={fieldErrors.banRecord} className="publish-field--span-2">
                  <BinaryChoiceGroup
                    options={[
                      { value: "has", label: "有封禁记录" },
                      { value: "none", label: "无封禁记录" },
                    ]}
                    value={form.banRecord}
                    onChange={(value) => {
                      updateField("banRecord", value as PublishFormState["banRecord"]);
                      if (value !== "has") {
                        setSubmitError("");
                      }
                    }}
                  />
                </Field>
                <Field label="人脸归属" required error={fieldErrors.faceOwned} helper="人脸是否为账号主本人">
                  <BinaryChoiceGroup
                    options={[
                      { value: "yes", label: "是" },
                      { value: "no", label: "否" },
                    ]}
                    value={form.faceOwned}
                    onChange={(value) => updateField("faceOwned", value as PublishFormState["faceOwned"])}
                  />
                </Field>
                <Field label="解锁赛依德" required error={fieldErrors.unlockSaeed}>
                  <BinaryChoiceGroup
                    options={[
                      { value: "yes", label: "是" },
                      { value: "no", label: "否" },
                    ]}
                    value={form.unlockSaeed}
                    onChange={(value) => updateField("unlockSaeed", value as PublishFormState["unlockSaeed"])}
                  />
                </Field>
                {form.banRecord === "has" ? (
                  <Field
                    label="处罚截图"
                    required
                    error={fieldErrors.punishmentImageKey}
                    helper="请上传腾讯安全中心处罚截图"
                    className="publish-field--span-2"
                  >
                    <SingleAssetUpload
                      asset={punishmentAsset}
                      buttonLabel={uploadingPunishment ? "上传中..." : punishmentAsset ? "重新上传" : "上传处罚截图"}
                      onDelete={() => setPunishmentAsset(null)}
                      triggerId={PUNISHMENT_INPUT_ID}
                      triggerDisabled={uploadingPunishment}
                    />
                    <input
                      id={PUNISHMENT_INPUT_ID}
                      ref={punishmentInputRef}
                      accept="image/png,image/jpeg"
                      hidden
                      type="file"
                      onChange={(event) => {
                        const file = event.target.files?.[0];
                        if (file) {
                          void handlePunishmentUpload(file);
                        }
                        event.currentTarget.value = "";
                      }}
                    />
                  </Field>
                ) : null}
              </div>
            </section>

            <section className="publish-card">
              <div className="publish-card__header">
                <div>
                  <h2>外观与资产</h2>
                </div>
              </div>
              <TagEditor
                label="特殊刀皮"
                options={knifeSkins}
                placeholder="输入或选择特殊刀皮"
                values={form.knifeSkins}
                onChange={(values) => updateField("knifeSkins", values)}
                collapsed
              />
              <TagEditor
                label="人物红皮"
                options={redSkins}
                placeholder="输入或选择人物红皮"
                values={form.redSkins}
                onChange={(values) => updateField("redSkins", values)}
                collapsed
              />
              <TagEditor
                label="武器皮肤"
                options={weaponSkinCatalog}
                placeholder="输入或选择武器皮肤"
                values={form.weaponSkins}
                onChange={(values) => updateField("weaponSkins", values)}
                collapsed
              />
              <TagEditor
                label="人物金皮"
                options={goldSkins}
                placeholder="输入或选择人物金皮"
                values={form.goldSkins}
                onChange={(values) => updateField("goldSkins", values)}
                collapsed
              />
              <div className="publish-grid publish-grid--3">
                <Field label="绝密KD" required error={fieldErrors.secretKd} helper="当前赛季绝密模式KD">
                  <input value={form.secretKd} onChange={(event) => updateField("secretKd", event.target.value)} placeholder="如 2.35" />
                </Field>
                <Field label="哈夫币余额" required error={fieldErrors.hafCurrency} helper="只填写整数，单位 M。例：300M 填 300">
                  <input
                    inputMode="numeric"
                    pattern="[0-9]*"
                    value={form.hafCurrency}
                    onChange={(event) => updateField("hafCurrency", normalizeIntegerInput(event.target.value))}
                    placeholder="例：300"
                  />
                </Field>
              </div>
            </section>

            <section className="publish-card">
              <div className="publish-card__header">
                <div>
                  <h2>额外物资</h2>
                </div>
              </div>
              <div className="publish-extra-items">
                {extraItemsMeta.map((item) => {
                  const draft = form.extraItems[item.code] ?? { count: "0", chargeMode: "charge" as const };
                  const count = toBoundedInteger(draft.count, 0, 9999);
                  const total = draft.chargeMode === "charge" ? count * extraItemSettlementPrice(item.unitPrice) : 0;
                  return (
                    <div className="publish-extra-item" key={item.code}>
                      <div className="publish-extra-item__header">
                        <strong>{item.label}</strong>
                        <span>单价 {formatCurrency(extraItemSettlementPrice(item.unitPrice))}</span>
                      </div>
                      <div className="publish-extra-item__body">
                        <button
                          className="publish-extra-item__stepper"
                          type="button"
                          onClick={() => adjustExtraItemCount(item.code, -1)}
                          aria-label={`减少${item.label}数量`}
                        >
                          −
                        </button>
                        <input
                          inputMode="numeric"
                          pattern="[0-9]*"
                          value={draft.count}
                          onChange={(event) =>
                            updateExtraItem(item.code, {
                              ...draft,
                              count: normalizeBoundedIntegerInput(event.target.value, 0, 9999),
                            })
                          }
                          placeholder="0"
                        />
                        <button
                          className="publish-extra-item__stepper"
                          type="button"
                          onClick={() => adjustExtraItemCount(item.code, 1)}
                          aria-label={`增加${item.label}数量`}
                        >
                          +
                        </button>
                        <span className="publish-extra-item__unit">{item.unitLabel}</span>
                        <span className="publish-extra-item__price">单价({formatUnitPrice(extraItemSettlementPrice(item.unitPrice))}/{item.unitLabel})</span>
                        <div className="publish-extra-item__mode">
                          <BinaryChoiceGroup
                            compact
                            options={[
                              { value: "gift", label: "赠送" },
                              { value: "charge", label: "收费" },
                            ]}
                            value={draft.chargeMode}
                            onChange={(value) => updateExtraItem(item.code, { ...draft, chargeMode: value as "gift" | "charge" })}
                          />
                        </div>
                        <span className="publish-extra-item__total">总价：{formatCurrency(total)}</span>
                      </div>
                    </div>
                  );
                })}
              </div>
              {fieldErrors.extraItems ? <div className="publish-field__error">{fieldErrors.extraItems}</div> : null}
              <div className="publish-extra-summary">
                <strong>额外物品总收入</strong>
                <output>{formatMoneyValue(extraIncome)}</output>
                <span>元</span>
                <small>不包含在基础租金内，由租客实际消耗多少决定，从租客押金中扣除。</small>
              </div>
            </section>

            <section className="publish-card">
              <div className="publish-card__header">
                <div>
                  <h2>租赁参数</h2>
                </div>
              </div>
              <div className="publish-grid publish-grid--2">
                <Field
                  label="卖家类型"
                  required
                  helper={meta.sellerContext.sellerTypeLocked ? "当前账号已绑定工作室身份，不可修改" : "当前登录身份决定卖家类型展示"}
                >
                  <BinaryChoiceGroup
                    options={[
                      { value: "PERSONAL", label: "个人" },
                      { value: "STUDIO", label: "工作室" },
                    ]}
                    value={meta.sellerContext.sellerType}
                    onChange={() => undefined}
                    disabled
                  />
                </Field>
              </div>
              <ChoicePillGroup
                label="默认消耗"
                required
                error={fieldErrors.defaultSpend}
                options={defaultSpendOptions}
                value={form.defaultSpend}
                onChange={(value) => updateField("defaultSpend", value)}
                helper={"流量消耗与比例规则\n默认消耗：10-50M\n比例调整：在默认基础上，每增加10M/天，出租比例相应 +1\n10M/天时：可自定义兑换比例\n请根据实际需求合理设置，感谢您的配合。"}
              />
              <div className="publish-grid publish-grid--3">
                <Field label="出租天数" required error={fieldErrors.rentalDays} helper="账号允许的最长出租时间，超期可申请全额结算">
                  <div className="publish-computed-value">
                    {form.rentalDays ? (
                      <>
                        <strong>{form.rentalDays}</strong>
                        <span>天</span>
                      </>
                    ) : (
                      <span>按余额自动计算</span>
                    )}
                  </div>
                </Field>
                <Field
                  label="租金"
                  required
                  error={fieldErrors.price}
                  helper={rentalQuote ? <RentalRuleHelper quote={rentalQuote} hafCurrency={form.hafCurrency} exchangeRateType={form.exchangeRateType} /> : "填写关键字段后自动计算"}
                >
                  <input readOnly value={calculatedPrice} placeholder="自动计算" />
                  <span className="publish-commission-tip">出售成功平台抽取{sellerCommissionRateText}服务费</span>
                </Field>
                <Field label="押金" required error={fieldErrors.deposit}>
                  <div className="publish-inline-input">
                    <input value={form.deposit} onChange={(event) => updateField("deposit", event.target.value)} placeholder="0-2000 元" />
                    <button
                      className="publish-inline-button"
                      type="button"
                      onClick={() => updateField("deposit", recommendDeposit(calculatedPrice))}
                    >
                      推荐押金
                    </button>
                  </div>
                </Field>
              </div>
              <ChoicePillGroup
                label="兑换比例"
                required
                error={fieldErrors.exchangeRateType}
                options={exchangeRateOptions}
                value={form.exchangeRateType}
                onChange={(value) => {
                  updateField("exchangeRateType", value);
                  if (value === "default" || value === "custom" || value === "accelerated") {
                    updateField("customExchangeRate", "");
                  }
                }}
                helper={getExchangeRateDescription(form.exchangeRateType)}
              />
              {form.exchangeRateType === "custom" ? (
                <Field
                  label="自定义比例"
                  required
                  error={fieldErrors.customExchangeRate}
                  helper="含义：1 元等于多少万哈夫币"
                >
	                  <div className="publish-exchange-rate-control">
	                    <input
	                      inputMode="numeric"
	                      pattern="[0-9]*"
	                      value={displayedExchangeRateValue}
	                      onChange={(event) => updateField("customExchangeRate", normalizeIntegerInput(event.target.value))}
	                      placeholder="例如 8"
	                    />
	                    <span className="publish-exchange-rate-preview">
	                      1元={positiveInt(displayedExchangeRateValue, 1, 999999999) ? displayedExchangeRateValue : "-"}万哈夫币
	                    </span>
	                  </div>
                </Field>
              ) : form.exchangeRateType === "accelerated" ? (
                <Field label="特惠比例" helper={rentalQuote ? `租金计算：${rentalQuote.detail}` : "后台默认比例基础上固定 +5，填写关键字段后自动计算租金"}>
                  <input readOnly value={promotionalExchangeRateWan ? `1 元 = ${promotionalExchangeRateWan} 万哈夫币` : "等待后台配置"} />
                </Field>
              ) : form.exchangeRateType === "default" ? (
                <Field label="默认比例" helper={rentalQuote ? `租金计算：${rentalQuote.detail}` : "后台配置的默认兑换比例，填写关键字段后自动计算租金"}>
                  <input readOnly value={defaultDisplayExchangeRateWan ? `最终比例：1 元 = ${defaultDisplayExchangeRateWan} 万哈夫币` : "等待后台配置"} />
                </Field>
              ) : null}
              <ChoicePillGroup
                label="赔付方案"
                required
                error={fieldErrors.compensationPlan}
                options={compensationPlans}
                value={form.compensationPlan}
                onChange={(value) => updateField("compensationPlan", value)}
                helper={getCompensationPlanDescription(form.compensationPlan)}
              />
              <div className="publish-grid">
                <Field label="账号标题" required error={fieldErrors.title} helper={`${form.title.trim().length} 字`}>
                  <input value={form.title} onChange={(event) => updateField("title", event.target.value)} placeholder="例：满仓红皮，全天可辅助上号，安全箱满级" />
                </Field>
                <Field label="账号描述" error={fieldErrors.description} helper={`${form.description.trim().length}/500`}>
                  <textarea
                    rows={6}
                    value={form.description}
                    onChange={(event) => updateField("description", event.target.value)}
                    placeholder="描述账号亮点、装备详情、可上号时段、赔付方式、是否本人等关键信息"
                  />
                </Field>
                <Field label="备注" error={fieldErrors.remarks}>
                  <textarea
                    rows={4}
                    value={form.remarks}
                    onChange={(event) => updateField("remarks", event.target.value)}
                    placeholder="要求租方遵守的额外说明，或其他信息"
                  />
                </Field>
              </div>
            </section>

            <section className="publish-card">
              <div className="publish-card__header">
                <div>
                  <h2>图片与协议</h2>
                </div>
              </div>
              <Field label="账号截图" required error={fieldErrors.imageKeys} helper="JPG/PNG，上传前自动压缩，3-10 张">
                <div className="publish-upload">
                  <div className="publish-upload__toolbar">
                    {uploadingImages || imageAssets.length >= 10 ? (
                      <Button kind="secondary" disabled>
                        {uploadingImages ? "上传中..." : "上传图片"}
                      </Button>
                    ) : (
                      <label className="dt-button dt-button--secondary publish-upload-trigger" htmlFor={IMAGE_INPUT_ID}>
                        上传图片
                      </label>
                    )}
                    <span>已上传 {imageAssets.length} / 10 张</span>
                  </div>
                  <input
                    id={IMAGE_INPUT_ID}
                    ref={imageInputRef}
                    accept="image/png,image/jpeg"
                    hidden
                    multiple
                    type="file"
                    onChange={(event) => {
                      const files = Array.from(event.target.files ?? []);
                      void handleImageUpload(files);
                      event.currentTarget.value = "";
                    }}
                  />
                  <div className="publish-media-grid">
                    {imageAssets.map((asset) => (
                      <figure className="publish-media-card" key={asset.objectKey}>
                        <button
                          className="publish-media-card__remove"
                          type="button"
                          onClick={() => removeImage(asset.objectKey)}
                          aria-label={`删除图片 ${asset.filename}`}
                        >
                          删除
                        </button>
                        <img alt={asset.filename} src={asset.previewUrl} />
                        <figcaption>
                          <span className="publish-media-card__name">{asset.filename}</span>
                        </figcaption>
                      </figure>
                    ))}
                  </div>
                </div>
              </Field>
              <Field label="短视频" error={fieldErrors.videoKey} helper="MP4，≤50MB，时长≤60秒">
                <div className="publish-upload">
                  <div className="publish-upload__toolbar">
                    {uploadingVideo || Boolean(videoAsset) ? (
                      <Button kind="secondary" disabled>
                        {uploadingVideo ? "上传中..." : videoAsset ? "已上传视频" : "上传视频"}
                      </Button>
                    ) : (
                      <label className="dt-button dt-button--secondary publish-upload-trigger" htmlFor={VIDEO_INPUT_ID}>
                        上传视频
                      </label>
                    )}
                    {videoAsset ? (
                      <button className="publish-inline-link" type="button" onClick={removeVideo}>删除视频</button>
                    ) : null}
                  </div>
                  <input
                    id={VIDEO_INPUT_ID}
                    ref={videoInputRef}
                    accept="video/mp4"
                    hidden
                    type="file"
                    onChange={(event) => {
                      const file = event.target.files?.[0];
                      if (file) {
                        void handleVideoUpload(file);
                      }
                      event.currentTarget.value = "";
                    }}
                  />
                  {videoAsset ? (
                    <div className="publish-video-card">
                      <video controls src={videoAsset.previewUrl} />
                      <div>
                        <strong>{videoAsset.filename}</strong>
                        <span>{videoAsset.source === "mock" ? "当前为本地预览" : "已上传"}</span>
                      </div>
                    </div>
                  ) : null}
                </div>
              </Field>
              <Field label="协议同意" required error={fieldErrors.agreements}>
                <AgreementChecklist
                  options={agreementOptions}
                  values={form.agreements}
                  compensationPlan={form.compensationPlan}
                  onToggle={toggleAgreement}
                  onOpenAgreement={setActiveAgreement}
                />
              </Field>
            </section>

            {submitError ? <div className="auth-feedback auth-feedback--error">{submitError}</div> : null}

            <section className="publish-actions">
              <Button
                kind="secondary"
                onClick={() => {
                  if (editListingNo && editSeed) {
                    applyEditSeed(editSeed);
                    return;
                  }
                  resetPublishForm(extraItemsMeta);
                }}
              >
                {editListingNo ? "恢复原始内容" : "清空内容"}
              </Button>
              <Button disabled={!canSubmit} onClick={() => void handleSubmit()}>
                {submitting ? "提交中..." : editListingNo ? "保存修改" : "确认发布"}
              </Button>
            </section>
          </div>

        </div>
      </div>
      <AgreementDialog agreement={activeAgreement} onClose={() => setActiveAgreement(null)} />
    </main>
  );

  function updateField<K extends keyof PublishFormState>(key: K, value: PublishFormState[K]) {
    setForm((previous) => ({ ...previous, [key]: value }));
    setFieldErrors((previous) => ({ ...previous, [key]: undefined }));
    setSubmitError("");
  }

  function toggleSelection(key: "weapons" | "modCodes", value: string) {
    setForm((previous) => {
      const current = previous[key];
      const next = current.includes(value) ? current.filter((item) => item !== value) : [...current, value];
      return { ...previous, [key]: next };
    });
    setFieldErrors((previous) => ({ ...previous, [key]: undefined }));
  }

  function toggleAgreement(value: string) {
    setForm((previous) => {
      const exists = previous.agreements.includes(value);
      return {
        ...previous,
        agreements: exists ? previous.agreements.filter((item) => item !== value) : [...previous.agreements, value],
      };
    });
    setFieldErrors((previous) => ({ ...previous, agreements: undefined }));
  }

  function updateExtraItem(code: string, nextValue: ExtraItemDraft) {
    setForm((previous) => ({
      ...previous,
      extraItems: {
        ...previous.extraItems,
        [code]: nextValue,
      },
    }));
    setFieldErrors((previous) => ({ ...previous, extraItems: undefined }));
  }

  function adjustExtraItemCount(code: string, delta: number) {
    const current = form.extraItems[code] ?? { count: "0", chargeMode: "charge" as const };
    const nextCount = Math.max(0, Math.min(9999, toBoundedInteger(current.count, 0, 9999) + delta));
    updateExtraItem(code, {
      ...current,
      count: String(nextCount),
    });
  }

  function removeImage(objectKey: string) {
    setImageAssets((previous) => previous.filter((item) => item.objectKey !== objectKey));
  }

  function removeVideo() {
    setVideoAsset(null);
  }

  function applyEditSeed(seed: EditSeed) {
    setForm(seed.form);
    setImageAssets(seed.imageAssets);
    setVideoAsset(seed.videoAsset);
    setPunishmentAsset(seed.punishmentAsset);
    setFieldErrors({});
    setSubmitError("");
    setSuccessResult(null);
  }

  function resetPublishForm(extraItemsMeta: ExtraItemMeta[] = meta?.extraItems?.length ? meta.extraItems : DEFAULT_EXTRA_ITEMS) {
    setForm({
      ...INITIAL_FORM,
      extraItems: buildInitialExtraItemState(extraItemsMeta),
    });
    setImageAssets([]);
    setVideoAsset(null);
    setPunishmentAsset(null);
    setFieldErrors({});
    setSubmitError("");
  }

  async function handleImageUpload(files: File[]) {
    if (!files.length) {
      return;
    }
    if (imageAssets.length + files.length > 10) {
      setFieldErrors((previous) => ({ ...previous, imageKeys: "截图最多上传 10 张" }));
      return;
    }

    setUploadingImages(true);
    setSubmitError("");
    try {
      const uploaded = await Promise.all(
        files.map(async (file) => {
          validateImageFile(file);
          return uploadMedia(file, "listing-images");
        }),
      );
      setImageAssets((previous) => [...previous, ...uploaded]);
      setFieldErrors((previous) => ({ ...previous, imageKeys: undefined }));
    } catch (error) {
      setSubmitError(getErrorMessage(error));
    } finally {
      setUploadingImages(false);
    }
  }

  async function handleVideoUpload(file: File) {
    setUploadingVideo(true);
    setSubmitError("");
    try {
      validateVideoFile(file);
      const duration = await readVideoDuration(file);
      if (duration > 60) {
        throw new Error("视频时长不能超过 60 秒");
      }
      const uploaded = await uploadMedia(file, "listing-videos");
      setVideoAsset(uploaded);
      setFieldErrors((previous) => ({ ...previous, videoKey: undefined }));
    } catch (error) {
      setSubmitError(getErrorMessage(error));
    } finally {
      setUploadingVideo(false);
    }
  }

  async function handlePunishmentUpload(file: File) {
    setUploadingPunishment(true);
    setSubmitError("");
    try {
      validateImageFile(file);
      const uploaded = await uploadMedia(file, "listing-punishment");
      setPunishmentAsset(uploaded);
      setFieldErrors((previous) => ({ ...previous, punishmentImageKey: undefined }));
    } catch (error) {
      setSubmitError(getErrorMessage(error));
    } finally {
      setUploadingPunishment(false);
    }
  }

  async function handleSubmit() {
    const validationErrors = validateForm(form, imageAssets, videoAsset, punishmentAsset, extraItemsMeta, rentalBaseRatio);
    if (Object.keys(validationErrors).length) {
      setFieldErrors(validationErrors);
      setSubmitError("请先修正表单校验项后再提交");
      return;
    }

    const computedRentalDays = calculateRentalDays(form.hafCurrency, form.defaultSpend);
    const payload: PublishPayload = {
      provinceCode: form.provinceCode,
      cityCode: form.cityCode,
      deliveryMethod: form.deliveryMethod,
      alwaysOnline: form.alwaysOnline,
      deliveryStartHour: Number(form.deliveryStartHour),
      deliveryEndHour: Number(form.deliveryEndHour),
      accountLevel: Number(form.accountLevel),
      rankName: form.rankName,
      safeBoxLevel: Number(form.safeBoxLevel),
      staminaLevel: Number(form.staminaLevel),
      carryLevel: Number(form.carryLevel),
      diveLevel: Number(form.diveLevel),
      banRecord: form.banRecord === "has",
      punishmentImageKey: form.banRecord === "has" ? (punishmentAsset?.objectKey ?? null) : null,
      faceOwned: form.faceOwned === "yes",
      unlockSaeed: form.unlockSaeed === "yes",
      hafCurrency: Number(form.hafCurrency),
      knifeSkins: form.knifeSkins,
      redSkins: form.redSkins,
      title: form.title.trim(),
      description: form.description.trim(),
      operatorCount: 0,
      operators: [],
      weapons: [],
      weaponSkins: form.weaponSkins,
      goldSkins: form.goldSkins,
      secretKd: Number(form.secretKd),
      defaultSpend: form.defaultSpend,
      rentalDays: Number(computedRentalDays),
      exchangeRateType: form.exchangeRateType,
	      customExchangeRate:
	        form.exchangeRateType === "default" || form.exchangeRateType === "accelerated"
	          ? null
	          : toBoundedInteger(form.customExchangeRate, 1, 999999999) * 10000,
      compensationPlan: form.compensationPlan,
      deposit: Number(form.deposit),
      remarks: form.remarks.trim(),
      agreements: form.agreements,
      extraItems: extraItemsMeta.map((item) => ({
        code: item.code,
        count: toBoundedInteger(form.extraItems[item.code]?.count, 0, 9999),
        chargeMode: form.extraItems[item.code]?.chargeMode ?? "charge",
      })),
      otherItems: "",
      imageKeys: imageAssets.map((item) => item.objectKey),
      videoKey: videoAsset?.objectKey ?? null,
      price: Number(calculatedPrice),
      negotiable: false,
      modCodes: [],
    };

    try {
      setSubmitting(true);
      setSubmitError("");
      const result = editListingNo
        ? await updateMyListing(editListingNo, payload)
        : await submitPublish(payload);
      setSuccessResult(result);
      window.scrollTo({ top: 0, behavior: "smooth" });
    } catch (error) {
      setSubmitError(getErrorMessage(error));
    } finally {
      setSubmitting(false);
    }
  }
}

function Field({
  label,
  error,
  helper,
  required,
  className,
  children,
}: {
  label: string;
  error?: string;
  helper?: ReactNode;
  required?: boolean;
  className?: string;
  children: ReactNode;
}) {
  return (
    <label className={["publish-field", className].filter(Boolean).join(" ")}>
      <span className="publish-field__label">
        <strong className={required ? "is-required" : ""}>{label}</strong>
        {helper ? <span>{helper}</span> : null}
      </span>
      {children}
      {error ? <span className="publish-field__error">{error}</span> : null}
    </label>
  );
}

function RentalRuleHelper({ quote, hafCurrency, exchangeRateType }: { quote: RentalQuote; hafCurrency: string; exchangeRateType: string }) {
  return (
    <span className="publish-rental-rule">
      <span>{quote.formula}</span>
      <span>{quote.detail}</span>
      {exchangeRateType === "default" ? <span>最终比例：1 元 = {quote.ratio} 万哈夫币</span> : null}
      <strong>本次计算：{quote.formula.includes("× 100") ? `${hafCurrency}M × 100 ÷ ${quote.ratio}` : `${hafCurrency}M ÷ ${quote.ratio}`} = {quote.price} 元</strong>
    </span>
  );
}

function RegionPicker({
  regions,
  provinceCode,
  cityCode,
  valueText,
  onChange,
}: {
  regions: RegionProvince[];
  provinceCode: string;
  cityCode: string;
  valueText: string;
  onChange: (provinceCode: string, cityCode: string) => void;
}) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const searchInputRef = useRef<HTMLInputElement | null>(null);
  const dropdownIdRef = useRef(`publish-region-${Math.random().toString(36).slice(2)}`);
  const [open, setOpen] = useState(false);
  const [keyword, setKeyword] = useState("");
  const [activeProvinceCode, setActiveProvinceCode] = useState(provinceCode);

  const normalizedKeyword = keyword.trim().toLowerCase();
  const filteredRegions = useMemo(() => {
    if (!normalizedKeyword) {
      return regions;
    }
    return regions.filter((region) => {
      if (region.name.toLowerCase().includes(normalizedKeyword)) {
        return true;
      }
      return region.cities.some((city) => {
        const cityName = city.name.toLowerCase();
        const fullName = `${region.name}-${city.name}`.toLowerCase();
        return cityName.includes(normalizedKeyword) || fullName.includes(normalizedKeyword);
      });
    });
  }, [normalizedKeyword, regions]);

  const activeProvince = useMemo(
    () => filteredRegions.find((item) => item.code === activeProvinceCode) ?? filteredRegions[0] ?? null,
    [activeProvinceCode, filteredRegions],
  );
  const visibleCities = useMemo(() => {
    if (!activeProvince) {
      return [];
    }
    if (!normalizedKeyword) {
      return activeProvince.cities;
    }
    if (activeProvince.name.toLowerCase().includes(normalizedKeyword)) {
      return activeProvince.cities;
    }
    return activeProvince.cities.filter((city) => {
      const cityName = city.name.toLowerCase();
      const fullName = `${activeProvince.name}-${city.name}`.toLowerCase();
      return cityName.includes(normalizedKeyword) || fullName.includes(normalizedKeyword);
    });
  }, [activeProvince, normalizedKeyword]);

  useEffect(() => {
    if (!open) {
      return;
    }
    if (normalizedKeyword) {
      setActiveProvinceCode(filteredRegions[0]?.code || "");
      return;
    }
    setActiveProvinceCode(provinceCode || filteredRegions[0]?.code || "");
  }, [filteredRegions, normalizedKeyword, open, provinceCode]);

  useEffect(() => {
    if (!open) {
      return;
    }
    function handlePointerDown(event: PointerEvent) {
      if (containerRef.current?.contains(event.target as Node)) {
        return;
      }
      setOpen(false);
    }
    document.addEventListener("pointerdown", handlePointerDown);
    return () => document.removeEventListener("pointerdown", handlePointerDown);
  }, [open]);

  useEffect(() => {
    function handleCloseEvent(event: Event) {
      const detail = (event as CustomEvent<{ id?: string }>).detail;
      if (detail?.id !== dropdownIdRef.current) {
        setOpen(false);
      }
    }
    window.addEventListener(PUBLISH_DROPDOWN_CLOSE_EVENT, handleCloseEvent);
    return () => window.removeEventListener(PUBLISH_DROPDOWN_CLOSE_EVENT, handleCloseEvent);
  }, []);

  const toggleOpen = () => {
    setOpen((previous) => {
      const next = !previous;
      if (next) {
        closeOtherPublishDropdowns(dropdownIdRef.current);
      }
      return next;
    });
  };

  const selectCity = (nextProvinceCode: string, nextCityCode: string) => {
    setActiveProvinceCode(nextProvinceCode);
    onChange(nextProvinceCode, nextCityCode);
    setKeyword("");
    setOpen(false);
    blockPublishDropdownOpen();
    searchInputRef.current?.blur();
  };

  return (
    <div className={`publish-region-picker ${open ? "is-open" : ""}`} ref={containerRef}>
      <button
        aria-expanded={open}
        className="publish-region-picker__trigger"
        type="button"
        onClick={toggleOpen}
      >
        <span className={valueText ? "" : "is-placeholder"}>{valueText || "请选择地区"}</span>
        <span className="publish-region-picker__arrow" />
      </button>
      {open ? (
        <div className="publish-region-picker__panel" onPointerDown={(event) => event.stopPropagation()}>
          <div className="publish-region-picker__search">
            <input
              ref={searchInputRef}
              placeholder="搜索省份/区域"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
            />
          </div>
          <div className="publish-region-picker__columns">
            <div className="publish-region-picker__column publish-region-picker__column--province" role="listbox" aria-label="省份列表">
              {filteredRegions.length ? (
                filteredRegions.map((region) => (
                  <button
                    className={`publish-region-picker__option ${region.code === activeProvince?.code ? "is-active" : ""}`}
                    key={region.code}
                    type="button"
                    onMouseEnter={() => setActiveProvinceCode(region.code)}
                    onClick={() => setActiveProvinceCode(region.code)}
                  >
                    {region.name}
                  </button>
                ))
              ) : (
                <div className="publish-region-picker__empty">没有匹配的省份</div>
              )}
            </div>
            <div className="publish-region-picker__column publish-region-picker__column--city" role="listbox" aria-label="城市列表">
              {visibleCities.length ? (
                visibleCities.map((city) => (
                  <button
                    className={`publish-region-picker__option ${provinceCode === activeProvince?.code && city.code === cityCode ? "is-active" : ""}`}
                    key={city.code}
                    type="button"
                    onClick={() => {
                      if (!activeProvince) {
                        return;
                      }
                      selectCity(activeProvince.code, city.code);
                    }}
                  >
                    {city.name}
                  </button>
                ))
              ) : activeProvince ? (
                <div className="publish-region-picker__empty">没有匹配的城市</div>
              ) : (
                <div className="publish-region-picker__empty">请选择左侧省份</div>
              )}
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}

function OptionPopover({
  options,
  value,
  placeholder,
  onChange,
}: {
  options: OptionItem[];
  value: string;
  placeholder: string;
  onChange: (value: string) => void;
}) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const panelRef = useRef<HTMLDivElement | null>(null);
  const dropdownIdRef = useRef(`publish-option-${Math.random().toString(36).slice(2)}`);
  const optionPointerRef = useRef<{ x: number; y: number; scrollTop: number; dragged: boolean } | null>(null);
  const [open, setOpen] = useState(false);

  useEffect(() => {
    if (!open) {
      return;
    }
    function handlePointerDown(event: MouseEvent) {
      if (containerRef.current?.contains(event.target as Node)) {
        return;
      }
      setOpen(false);
    }
    document.addEventListener("mousedown", handlePointerDown);
    return () => document.removeEventListener("mousedown", handlePointerDown);
  }, [open]);

  useEffect(() => {
    function handleCloseEvent(event: Event) {
      const detail = (event as CustomEvent<{ id?: string }>).detail;
      if (detail?.id !== dropdownIdRef.current) {
        setOpen(false);
      }
    }
    window.addEventListener(PUBLISH_DROPDOWN_CLOSE_EVENT, handleCloseEvent);
    return () => window.removeEventListener(PUBLISH_DROPDOWN_CLOSE_EVENT, handleCloseEvent);
  }, []);

  const toggleOpen = () => {
    if (isPublishDropdownOpenBlocked()) {
      return;
    }
    setOpen((previous) => {
      const next = !previous;
      if (next) {
        closeOtherPublishDropdowns(dropdownIdRef.current);
      }
      return next;
    });
  };

  const selected = options.find((item) => item.value === value);
  const selectOption = (nextValue: string) => {
    onChange(nextValue);
    setOpen(false);
    blockPublishDropdownOpen();
    if (document.activeElement instanceof HTMLElement) {
      document.activeElement.blur();
    }
  };

  const recordOptionPointerStart = (event: ReactPointerEvent<HTMLButtonElement>) => {
    optionPointerRef.current = {
      x: event.clientX,
      y: event.clientY,
      scrollTop: panelRef.current?.scrollTop ?? 0,
      dragged: false,
    };
  };

  const recordOptionPointerMove = (event: ReactPointerEvent<HTMLButtonElement>) => {
    const start = optionPointerRef.current;
    if (!start) {
      return;
    }
    const distance = Math.hypot(event.clientX - start.x, event.clientY - start.y);
    if (distance > 8) {
      start.dragged = true;
    }
  };

  const handleOptionClick = (event: ReactMouseEvent<HTMLButtonElement>, nextValue: string) => {
    event.stopPropagation();
    const start = optionPointerRef.current;
    optionPointerRef.current = null;
    const scrollDelta = Math.abs((panelRef.current?.scrollTop ?? 0) - (start?.scrollTop ?? 0));
    if (start?.dragged || scrollDelta > 2) {
      return;
    }
    selectOption(nextValue);
  };

  return (
    <div className={`publish-option-popover ${open ? "is-open" : ""}`} ref={containerRef}>
      <button
        aria-expanded={open}
        className="publish-option-popover__trigger"
        type="button"
        onClick={toggleOpen}
      >
        <span className={selected ? "" : "is-placeholder"}>{selected?.label || placeholder}</span>
        <span className="publish-option-popover__arrow" />
      </button>
      {open ? (
        <div className="publish-option-popover__panel" ref={panelRef}>
          {options.map((item) => (
            <button
              className={`publish-option-popover__item ${item.value === value ? "is-active" : ""}`}
              key={item.value}
              type="button"
              onPointerDown={recordOptionPointerStart}
              onPointerMove={recordOptionPointerMove}
              onClick={(event) => handleOptionClick(event, item.value)}
            >
              {item.label}
            </button>
          ))}
        </div>
      ) : null}
    </div>
  );
}

function OptionSelector({
  label,
  options,
  values,
  onToggle,
  error,
  required,
}: {
  label: string;
  options: OptionItem[];
  values: string[];
  onToggle: (value: string) => void;
  error?: string;
  required?: boolean;
}) {
  return (
    <div className="publish-tag-editor">
      <div className="publish-tag-editor__header">
        <strong className={required ? "is-required" : ""}>{label}</strong>
      </div>
      <div className="publish-option-grid">
        {options.map((item) => (
          <button
            className={`publish-option ${values.includes(item.value) ? "is-active" : ""}`}
            key={item.value}
            type="button"
            onClick={() => onToggle(item.value)}
          >
            {item.label}
          </button>
        ))}
      </div>
      {error ? <span className="publish-field__error">{error}</span> : null}
    </div>
  );
}

function TagEditor({
  label,
  options,
  values,
  onChange,
  placeholder,
  draft,
  onDraftChange,
  error,
  required,
  collapsed,
}: {
  label: string;
  options: OptionItem[];
  values: string[];
  onChange: (values: string[]) => void;
  placeholder: string;
  draft?: string;
  onDraftChange?: (value: string) => void;
  error?: string;
  required?: boolean;
  collapsed?: boolean;
}) {
  const rootRef = useRef<HTMLDivElement | null>(null);
  const [innerDraft, setInnerDraft] = useState("");
  const [expanded, setExpanded] = useState(!collapsed);
  const localDraft = draft ?? innerDraft;
  const setDraft = onDraftChange ?? setInnerDraft;
  const normalizedSuggestions = options.filter((item) => !values.includes(item.label));
  const showEditor = !collapsed || expanded;

  useEffect(() => {
    if (!collapsed || !expanded) {
      return;
    }

    function handlePointerDown(event: PointerEvent) {
      if (!rootRef.current?.contains(event.target as Node)) {
        setExpanded(false);
      }
    }

    document.addEventListener("pointerdown", handlePointerDown);
    return () => document.removeEventListener("pointerdown", handlePointerDown);
  }, [collapsed, expanded]);

  function commitTag(rawValue: string) {
    const value = rawValue.trim();
    if (!value || values.includes(value)) {
      setDraft("");
      return;
    }
    onChange([...values, value]);
    setDraft("");
  }

  return (
    <div ref={rootRef} className={`publish-tag-editor ${collapsed ? "is-collapsible" : ""} ${showEditor ? "is-expanded" : ""}`}>
      <div className="publish-tag-editor__header">
        <div className="publish-tag-editor__title-row">
          <strong className={required ? "is-required" : ""}>{label}</strong>
          {collapsed ? (
            <button
              aria-label={showEditor ? `收起${label}` : `添加${label}`}
              className="publish-tag-editor__toggle"
              type="button"
              onClick={() => setExpanded((current) => !current)}
            >
              {showEditor ? "−" : "+"}
            </button>
          ) : null}
          {collapsed && values.length ? (
            <div className="publish-tag-list publish-tag-list--inline">
              {values.map((value) => (
                <span className="publish-tag" key={value}>
                  {value}
                  <button type="button" onClick={() => onChange(values.filter((item) => item !== value))}>×</button>
                </span>
              ))}
            </div>
          ) : null}
        </div>
        {collapsed ? null : <span>{values.length} 项</span>}
      </div>
      {showEditor || (!collapsed && values.length) ? (
        <div className="publish-tag-editor__content">
          {!collapsed && values.length ? (
            <div className="publish-tag-list">
              {values.map((value) => (
                <span className="publish-tag" key={value}>
                  {value}
                  <button type="button" onClick={() => onChange(values.filter((item) => item !== value))}>×</button>
                </span>
              ))}
            </div>
          ) : null}
          {showEditor ? (
            <>
              <div className="publish-tag-editor__input">
                <input
                  value={localDraft}
                  onChange={(event) => setDraft(event.target.value)}
                  onKeyDown={(event) => {
                    if (event.key === "Enter" || event.key === ",") {
                      event.preventDefault();
                      commitTag(localDraft);
                    }
                  }}
                  placeholder={placeholder}
                />
                <Button kind="secondary" onClick={() => commitTag(localDraft)}>添加</Button>
              </div>
              {normalizedSuggestions.length ? (
                <div className="publish-option-grid publish-option-grid--compact">
                  {normalizedSuggestions.map((item) => (
                    <button className="publish-option" key={item.value} type="button" onClick={() => commitTag(item.label)}>
                      {item.label}
                    </button>
                  ))}
                </div>
              ) : null}
            </>
          ) : null}
        </div>
      ) : null}
      {error ? <span className="publish-field__error">{error}</span> : null}
    </div>
  );
}

function BinaryChoiceGroup({
  options,
  value,
  onChange,
  compact,
  disabled,
}: {
  options: Array<{ value: string; label: string }>;
  value: string;
  onChange: (value: string) => void;
  compact?: boolean;
  disabled?: boolean;
}) {
  return (
    <div className={`publish-radio-group ${compact ? "is-compact" : ""} ${disabled ? "is-disabled" : ""}`}>
      {options.map((item) => (
        <button
          className={`publish-radio-pill ${value === item.value ? "is-active" : ""} ${disabled ? "is-disabled" : ""}`}
          key={item.value}
          type="button"
          onClick={() => {
            if (!disabled) {
              onChange(item.value);
            }
          }}
          disabled={disabled}
        >
          <span className="publish-radio-pill__dot" />
          {item.label}
        </button>
      ))}
    </div>
  );
}

function ChoicePillGroup({
  label,
  options,
  value,
  onChange,
  helper,
  error,
  required,
}: {
  label: string;
  options: OptionItem[];
  value: string;
  onChange: (value: string) => void;
  helper?: string;
  error?: string;
  required?: boolean;
}) {
  return (
    <div className="publish-tag-editor">
      <div className="publish-tag-editor__header">
        <strong className={required ? "is-required" : ""}>{label}</strong>
      </div>
      <div className="publish-choice-grid">
        {options.map((item) => (
          <button
            className={`publish-choice-pill ${value === item.value ? "is-active" : ""}`}
            key={item.value}
            type="button"
            onClick={() => onChange(item.value)}
          >
            {item.label}
          </button>
        ))}
      </div>
      {helper ? <div className="publish-choice-helper">{helper}</div> : null}
      {error ? <span className="publish-field__error">{error}</span> : null}
    </div>
  );
}

function AgreementChecklist({
  options,
  values,
  compensationPlan,
  onToggle,
  onOpenAgreement,
}: {
  options: OptionItem[];
  values: string[];
  compensationPlan: string;
  onToggle: (value: string) => void;
  onOpenAgreement: (agreement: PublishAgreementContent) => void;
}) {
  const visibleOptions = compensationPlan === "full"
    ? options
    : options.filter((item) => item.value !== "full_coverage");

  return (
    <div className="publish-agreement-list">
      {visibleOptions.map((item) => {
        const checked = values.includes(item.value);
        const agreementContent = getPublishAgreementContent(item.value);
        return (
          <div className={`publish-agreement-row ${checked ? "is-active" : ""}`} key={item.value}>
            <button
              aria-pressed={checked}
              className={`publish-agreement ${checked ? "is-active" : ""}`}
              type="button"
              onClick={() => onToggle(item.value)}
            >
              <span className="publish-agreement__check">{checked ? "✓" : ""}</span>
              <span>我已阅读并同意</span>
            </button>
            {agreementContent ? (
              <button
                className="publish-agreement__link"
                type="button"
                onClick={() => onOpenAgreement(agreementContent)}
              >
                {item.label}
              </button>
            ) : (
              <span className="publish-agreement__text">{item.label}</span>
            )}
          </div>
        );
      })}
    </div>
  );
}

function AgreementDialog({
  agreement,
  onClose,
}: {
  agreement: PublishAgreementContent | null;
  onClose: () => void;
}) {
  useEffect(() => {
    if (!agreement) {
      return;
    }

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    function handleKeyDown(event: KeyboardEvent) {
      if (event.key === "Escape") {
        onClose();
      }
    }
    window.addEventListener("keydown", handleKeyDown);
    return () => {
      document.body.style.overflow = previousOverflow;
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [agreement, onClose]);

  if (!agreement) {
    return null;
  }

  return (
    <div aria-labelledby="publish-agreement-title" aria-modal="true" className="publish-agreement-dialog" role="dialog">
      <button aria-label="关闭协议弹层" className="publish-agreement-dialog__backdrop" type="button" onClick={onClose} />
      <div className="publish-agreement-dialog__panel">
        <div className="publish-agreement-dialog__header">
          <div>
            <h3 id="publish-agreement-title">{agreement.title}</h3>
            <p>已按参考站协议内容同步，桌面端与 H5 共享同一份正文。</p>
          </div>
          <button className="publish-agreement-dialog__close" type="button" onClick={onClose}>
            关闭
          </button>
        </div>
        <div className="publish-agreement-dialog__body">
          <pre>{agreement.body}</pre>
        </div>
        <div className="publish-agreement-dialog__footer">
          <Button onClick={onClose}>我已阅读</Button>
        </div>
      </div>
    </div>
  );
}

function SingleAssetUpload({
  asset,
  buttonLabel,
  triggerId,
  triggerDisabled,
  onDelete,
}: {
  asset: UploadedAsset | null;
  buttonLabel: string;
  triggerId: string;
  triggerDisabled?: boolean;
  onDelete: () => void;
}) {
  return (
    <div className="publish-single-upload">
      <div className="publish-single-upload__actions">
        {triggerDisabled ? (
          <Button kind="secondary" disabled>{buttonLabel}</Button>
        ) : (
          <label className="dt-button dt-button--secondary publish-upload-trigger" htmlFor={triggerId}>
            {buttonLabel}
          </label>
        )}
        {asset ? <button className="publish-inline-link" type="button" onClick={onDelete}>删除</button> : null}
      </div>
      {asset ? (
        <div className="publish-single-upload__preview">
          <img alt={asset.filename} src={asset.previewUrl} />
          <span>{asset.filename}</span>
        </div>
      ) : null}
    </div>
  );
}

async function uploadMedia(file: File, businessScope: string): Promise<UploadedAsset> {
  const uploaded = await uploadOssFileDirect(businessScope, file, buildUploadFilename(file));
  const previewUrl = URL.createObjectURL(uploaded.uploadedFile ?? file);

  let resolvedPreviewUrl = previewUrl;
  try {
    if (uploaded.previewUrl) {
      resolvedPreviewUrl = uploaded.previewUrl;
    } else {
      const preview = await loadOssPreview(uploaded.objectKey);
      if (preview.previewUrl) {
        resolvedPreviewUrl = preview.previewUrl;
      }
    }
  } catch {
    resolvedPreviewUrl = previewUrl;
  }
  return {
    objectKey: uploaded.objectKey,
    previewUrl: resolvedPreviewUrl,
    filename: file.name,
    source: "oss",
  };
}

function buildEditSeed(detail: MyListingDetail, extraItemsMeta: ExtraItemMeta[]): EditSeed {
  const extraItems = buildInitialExtraItemState(extraItemsMeta);
  for (const item of detail.draft.extraItems ?? []) {
    extraItems[item.code] = {
      count: String(toBoundedInteger(item.count, 0, 9999)),
      chargeMode: item.chargeMode ?? "charge",
    };
  }

  return {
    form: {
      provinceCode: detail.draft.provinceCode || "",
      cityCode: detail.draft.cityCode || "",
      deliveryMethod: detail.draft.deliveryMethod || "",
      alwaysOnline: Boolean(detail.draft.alwaysOnline),
      deliveryStartHour: stringifyNumber(detail.draft.deliveryStartHour),
      deliveryEndHour: stringifyNumber(detail.draft.deliveryEndHour),
      safeBoxLevel: stringifyNumber(detail.draft.safeBoxLevel),
      staminaLevel: stringifyNumber(detail.draft.staminaLevel),
      carryLevel: stringifyNumber(detail.draft.carryLevel),
      diveLevel: stringifyNumber(detail.draft.diveLevel),
      banRecord: detail.draft.banRecord ? "has" : detail.draft.banRecord === false ? "none" : "",
      faceOwned: detail.draft.faceOwned ? "yes" : detail.draft.faceOwned === false ? "no" : "",
      unlockSaeed: detail.draft.unlockSaeed ? "yes" : detail.draft.unlockSaeed === false ? "no" : "",
      accountLevel: stringifyNumber(detail.draft.accountLevel),
      rankName: detail.draft.rankName || "",
      hafCurrency: stringifyNumber(detail.draft.hafCurrency),
      secretKd: stringifyNumber(detail.draft.secretKd),
      title: detail.draft.title || "",
      description: detail.draft.description || "",
      operatorCount: stringifyNumber(detail.draft.operatorCount),
      operators: detail.draft.operators ?? [],
      weapons: detail.draft.weapons ?? [],
      knifeSkins: detail.draft.knifeSkins ?? [],
      redSkins: detail.draft.redSkins ?? [],
      weaponSkins: detail.draft.weaponSkins ?? [],
      goldSkins: detail.draft.goldSkins ?? [],
      defaultSpend: detail.draft.defaultSpend || "10m",
      rentalDays: stringifyNumber(detail.draft.rentalDays),
      exchangeRateType: detail.draft.exchangeRateType || "default",
      customExchangeRate:
        detail.draft.exchangeRateType === "default"
          ? ""
          : stringifyNumber(Number(detail.draft.customExchangeRate ?? 0) / 10000),
      compensationPlan: detail.draft.compensationPlan || "",
      remarks: detail.draft.remarks || "",
      otherItems: "",
      price: stringifyNumber(detail.draft.price),
      deposit: stringifyNumber(detail.draft.deposit),
      negotiable: Boolean(detail.draft.negotiable),
      modCodes: detail.draft.modCodes ?? [],
      agreements: detail.draft.agreements ?? [],
      extraItems,
    },
    imageAssets: (detail.images ?? []).map(toUploadedAsset),
    videoAsset: detail.video ? toUploadedAsset(detail.video) : null,
    punishmentAsset: detail.punishmentImage ? toUploadedAsset(detail.punishmentImage) : null,
  };
}

function toUploadedAsset(asset: { objectKey: string; previewUrl: string; filename: string }): UploadedAsset {
  return {
    objectKey: asset.objectKey,
    previewUrl: asset.previewUrl,
    filename: asset.filename,
    source: "oss",
  };
}

function stringifyNumber(value: unknown) {
  if (value === null || value === undefined || value === "") {
    return "";
  }
  return String(value);
}

function buildUploadFilename(file: File) {
  const extension = file.name.includes(".") ? file.name.substring(file.name.lastIndexOf(".")) : "";
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}${extension}`;
}

function validateImageFile(file: File) {
  if (!["image/jpeg", "image/png"].includes(file.type)) {
    throw new Error(`截图 ${file.name} 格式不正确，仅支持 JPG/PNG`);
  }
}

function validateVideoFile(file: File) {
  if (file.type !== "video/mp4") {
    throw new Error("视频仅支持 MP4 格式");
  }
  if (file.size > 50 * 1024 * 1024) {
    throw new Error("视频大小不能超过 50MB");
  }
}

function readVideoDuration(file: File) {
  return new Promise<number>((resolve, reject) => {
    const video = document.createElement("video");
    video.preload = "metadata";
    video.onloadedmetadata = () => {
      URL.revokeObjectURL(video.src);
      resolve(video.duration);
    };
    video.onerror = () => {
      URL.revokeObjectURL(video.src);
      reject(new Error("无法读取视频时长"));
    };
    video.src = URL.createObjectURL(file);
  });
}

function validateForm(
  form: PublishFormState,
  imageAssets: UploadedAsset[],
  videoAsset: UploadedAsset | null,
  punishmentAsset: UploadedAsset | null,
  extraItems: ExtraItemMeta[],
  rentalBaseRatio = 38,
): FieldErrors {
  const errors: FieldErrors = {};
  if (!form.provinceCode) {
    errors.provinceCode = "请选择所在省份";
  }
  if (!form.cityCode) {
    errors.cityCode = "请选择所在城市";
  }
  if (!form.deliveryMethod) {
    errors.deliveryMethod = "请选择上号方式";
  }
  if (!positiveInt(form.deliveryStartHour, 0, 24)) {
    errors.deliveryStartHour = "最早时间需在 0-24 之间";
  }
  if (!positiveInt(form.deliveryEndHour, 0, 24)) {
    errors.deliveryEndHour = "最晚时间需在 0-24 之间";
  }
  if (!errors.deliveryStartHour && !errors.deliveryEndHour && Number(form.deliveryStartHour) > Number(form.deliveryEndHour)) {
    errors.deliveryEndHour = "最晚时间不能早于最早时间";
  }
  if (!positiveInt(form.safeBoxLevel, 1, 4)) {
    errors.safeBoxLevel = "请选择基础到顶级的安全箱档位";
  }
  if (!positiveInt(form.staminaLevel, 4, 7)) {
    errors.staminaLevel = "体力需在 4-7 级之间";
  }
  if (!positiveInt(form.carryLevel, 4, 7)) {
    errors.carryLevel = "负重需在 4-7 级之间";
  }
  if (!(positiveInt(form.diveLevel, 0, 3) || form.diveLevel === "-1")) {
    errors.diveLevel = "潜水等级需在无、0-3级之间";
  }
  if (!form.banRecord) {
    errors.banRecord = "请选择封禁记录";
  }
  if (form.banRecord === "has" && !punishmentAsset?.objectKey) {
    errors.punishmentImageKey = "有封禁记录时必须上传处罚截图";
  }
  if (!form.faceOwned) {
    errors.faceOwned = "请选择人脸归属";
  }
  if (!form.unlockSaeed) {
    errors.unlockSaeed = "请选择是否解锁赛依德";
  }
  if (!positiveInt(form.accountLevel, 1, 120)) {
    errors.accountLevel = "账号等级需在 1-120 之间";
  }
  if (!form.rankName) {
    errors.rankName = "请选择账号段位";
  }
  if (!positiveInt(form.hafCurrency, 0, 999999999)) {
    errors.hafCurrency = "哈夫币需为非负整数";
  }
  if (!positiveDecimal(form.secretKd, 0)) {
    errors.secretKd = "绝密KD需为合法数字";
  }
  if (!form.title.trim()) {
    errors.title = "请输入账号标题";
  }
  const descriptionLength = form.description.trim().length;
  if (descriptionLength > 500) {
    errors.description = "账号描述不能超过 500 个字符";
  }
  if (!positiveInt(form.rentalDays, 1, 30)) {
    errors.rentalDays = "出租天数需在 1-30 天之间";
  }
  if (!form.defaultSpend) {
    errors.defaultSpend = "请选择默认消耗";
  } else if (positiveInt(form.hafCurrency, 0, 999999999) && Number(form.hafCurrency) < defaultSpendDailyM(form.defaultSpend)) {
    errors.hafCurrency = "哈夫币余额不能低于所选默认消耗";
  }
  if (!form.exchangeRateType) {
    errors.exchangeRateType = "请选择兑换比例";
  }
  if (form.exchangeRateType === "custom" && !positiveInt(form.customExchangeRate, 1, 999999999)) {
    errors.customExchangeRate = "兑换比例需填写正整数";
  }
  if (!form.compensationPlan) {
    errors.compensationPlan = "请选择赔付方案";
  }
  const rentalQuote = calculateRentalQuote(form, rentalBaseRatio);
  if (!rentalQuote) {
    errors.price = Number(form.hafCurrency) < 30 ? "哈夫币余额需达到 30M 起算" : "请先填写影响租金计算的字段";
  } else if (rentalQuote.price < 0.1) {
    errors.price = "租金不能低于 0.1 元";
  }
  if (!positiveDecimal(form.deposit, 0)) {
    errors.deposit = "押金需为合法金额";
  } else if (Number(form.deposit) > 2000) {
    errors.deposit = "押金不能超过 2000 元";
  }
  if (form.remarks.trim().length > 500) {
    errors.remarks = "备注不能超过 500 个字符";
  }
  if (!hasRequiredAgreements(form.agreements, form.compensationPlan)) {
    errors.agreements = "请先勾选当前方案所需协议";
  }
  const invalidExtra = extraItems.some((item) => !positiveInt(form.extraItems[item.code]?.count ?? "0", 0, 9999));
  if (invalidExtra) {
    errors.extraItems = "额外物资数量需为 0-9999 的整数";
  }
  if (imageAssets.length < 3 || imageAssets.length > 10) {
    errors.imageKeys = "截图需上传 3-10 张";
  }
  if (videoAsset && !videoAsset.objectKey) {
    errors.videoKey = "视频上传未完成";
  }
  return errors;
}

function positiveInt(value: string, min: number, max: number) {
  if (!/^\d+$/.test(value)) {
    return false;
  }
  const parsed = Number(value);
  return parsed >= min && parsed <= max;
}

function normalizeIntegerInput(value: string) {
  return value.replace(/[^\d]/g, "");
}

function normalizeBoundedIntegerInput(value: string, min: number, max: number) {
  const normalized = normalizeIntegerInput(value);
  if (!normalized) {
    return "";
  }
  return String(toBoundedInteger(normalized, min, max));
}

function toBoundedInteger(value: string | number | null | undefined, min: number, max: number) {
  const parsed = Math.trunc(Number(value));
  if (!Number.isFinite(parsed)) {
    return min;
  }
  return Math.max(min, Math.min(max, parsed));
}

function positiveDecimal(value: string, min: number) {
  if (!/^\d+(\.\d{1,2})?$/.test(value)) {
    return false;
  }
  return Number(value) >= min;
}

function calculateRentalQuote(form: PublishFormState, baseRatio = 38) {
  const hafCurrency = Number(form.hafCurrency) || 0;
  if (hafCurrency < 30) {
    return null;
  }
  if (form.exchangeRateType === "custom" && positiveInt(form.customExchangeRate, 1, 999999999)) {
    const ratio = toBoundedInteger(form.customExchangeRate, 1, 999999999);
    return {
      ratio,
      price: Math.max(0, Math.floor(hafCurrency * 100 / ratio)),
      detail: `自定义比例${ratio}，不叠加安全箱、体力、负重、大额币、默认消耗或刀皮修正`,
      formula: "租金 = 哈夫币数量 × 100 ÷ 自定义比例，向下取整。",
    };
  }
  if (form.exchangeRateType === "custom") {
    return null;
  }
  if (!form.safeBoxLevel || !form.staminaLevel || !form.carryLevel || !form.defaultSpend) {
    return null;
  }
  let ratio = baseRatio;
  ratio += safeBoxRatioDelta(form.safeBoxLevel);
  ratio += levelRatioDelta(form.staminaLevel);
  ratio += levelRatioDelta(form.carryLevel);
  ratio += hafCurrencyRatioDelta(hafCurrency);
  ratio += defaultSpendRatioOffset(form.defaultSpend);
  if (hasEffectiveKnifeSkin(form.knifeSkins)) {
    ratio -= 1;
  }
  ratio = Math.max(1, ratio);
  const price = Math.max(0, Math.floor(hafCurrency * 100 / ratio));
  const spendOffset = defaultSpendRatioOffset(form.defaultSpend);
  const detail = `后台基础${baseRatio}，默认消耗${formatSigned(spendOffset)}，当前基础${baseRatio + spendOffset}，安全箱${formatSigned(safeBoxRatioDelta(form.safeBoxLevel))}，体力${formatSigned(levelRatioDelta(form.staminaLevel))}，负重${formatSigned(levelRatioDelta(form.carryLevel))}，大额币${formatSigned(hafCurrencyRatioDelta(hafCurrency))}，刀皮${hasEffectiveKnifeSkin(form.knifeSkins) ? "-1" : "+0"}`;
  return {
    ratio,
    price,
    detail,
    formula: "租金 = 哈夫币数量 × 100 ÷ 最终比例，向下取整。",
  };
}

function safeBoxRatioDelta(value: string) {
  if (value === "4") return -1;
  if (value === "3") return 1;
  if (value === "1" || value === "2") return 3;
  return 0;
}

function levelRatioDelta(value: string) {
  if (value === "6") return 1;
  if (value === "5") return 2;
  if (value === "4") return 3;
  return 0;
}

function hafCurrencyRatioDelta(value: number) {
  if (value >= 1000) return 4;
  if (value >= 700) return 3;
  if (value >= 500) return 2;
  if (value >= 300) return 1;
  return 0;
}

function defaultSpendRatioOffset(value: string) {
  if (value === "20m_plus_2") return 2;
  if (value === "30m_plus_3") return 3;
  if (value === "40m_plus_4") return 4;
  if (value === "50m_plus_5") return 5;
  return 0;
}

function adjustDefaultExchangeRate(value: number | string | null | undefined, defaultSpend: string) {
  if (value === undefined || value === null || value === "") {
    return value;
  }
  const numeric = Number(value);
  if (!Number.isFinite(numeric) || numeric <= 0) {
    return value;
  }
  return numeric + defaultSpendRatioOffset(defaultSpend) * 10000;
}

function promotionalExchangeRate(value: number | string | null | undefined) {
  if (value === undefined || value === null || value === "") {
    return value;
  }
  const numeric = Number(value);
  if (!Number.isFinite(numeric) || numeric <= 0) {
    return value;
  }
  return numeric + 50000;
}

function resolveRentalBaseRatio(form: Pick<PublishFormState, "exchangeRateType" | "customExchangeRate">, defaultExchangeRate: number | string | null | undefined = 380000) {
  if (form.exchangeRateType === "custom" && positiveInt(form.customExchangeRate, 1, 999999999)) {
    return toBoundedInteger(form.customExchangeRate, 1, 999999999);
  }
  if (form.exchangeRateType === "accelerated") {
    return defaultRentalBaseRatio(defaultExchangeRate) + 5;
  }
  return defaultRentalBaseRatio(defaultExchangeRate);
}

function defaultRentalBaseRatio(value: number | string | null | undefined = 380000) {
  const numeric = Number(value);
  if (!Number.isFinite(numeric) || numeric <= 0) {
    return 38;
  }
  return Math.max(1, Math.floor(numeric / 10000));
}

function hasEffectiveKnifeSkin(values: string[]) {
  const effectiveKnifeSkins = new Set(["坠星者", "暗星", "龙牙", "信条", "影锋", "北极星", "黑海", "怜悯", "赤霄"]);
  return values.some((value) => effectiveKnifeSkins.has(value));
}

function calculateRentalDays(hafCurrencyValue: string, defaultSpendValue: string) {
  if (!positiveInt(hafCurrencyValue, 0, 999999999) || !defaultSpendValue) {
    return "";
  }
  const dailySpend = defaultSpendDailyM(defaultSpendValue);
  if (dailySpend <= 0) {
    return "";
  }
  if (Number(hafCurrencyValue) < dailySpend) {
    return "";
  }
  const days = Math.floor(Number(hafCurrencyValue) / dailySpend);
  return String(Math.max(1, Math.min(30, days)));
}

function defaultSpendDailyM(value: string) {
  const matched = value.match(/^(\d+)m/i);
  return matched ? Number(matched[1]) : 0;
}

function formatSigned(value: number) {
  return value >= 0 ? `+${value}` : String(value);
}

function formatDecimal(value: number) {
  return String(Math.floor(value));
}

function calculateExtraIncome(extraItems: Record<string, ExtraItemDraft>, configs: ExtraItemMeta[]) {
  return configs.reduce((sum, item) => {
    const draft = extraItems[item.code];
    if (!draft || draft.chargeMode !== "charge") {
      return sum;
    }
    return sum + toBoundedInteger(draft.count, 0, 9999) * extraItemSettlementPrice(item.unitPrice);
  }, 0);
}

function buildInitialExtraItemState(configs: ExtraItemMeta[]) {
  return configs.reduce<Record<string, ExtraItemDraft>>((accumulator, item) => {
    accumulator[item.code] = { count: "0", chargeMode: "charge" };
    return accumulator;
  }, {});
}

function mergeExtraItemsState(configs: ExtraItemMeta[], current: Record<string, ExtraItemDraft>) {
  const next = buildInitialExtraItemState(configs);
  for (const item of configs) {
    if (current[item.code]) {
      next[item.code] = current[item.code];
    }
  }
  return next;
}

function hasRequiredAgreements(values: string[], compensationPlan: string) {
  const required = ["virtual_asset", "owner_protocol"];
  if (compensationPlan === "full") {
    required.push("full_coverage");
  }
  return required.every((item) => values.includes(item));
}

function recommendDeposit(price: string) {
  const parsed = positiveDecimal(price, 0) ? Number(price) : 0;
  const extraHundreds = Math.max(0, Math.ceil(parsed / 100) - 1);
  return String(Math.min(2000, 300 + extraHundreds * 100));
}

function extraItemSettlementPrice(unitPrice: number) {
  return unitPrice;
}

function displayRegion(regions: RegionProvince[], provinceCode: string, cityCode: string) {
  const province = regions.find((item) => item.code === provinceCode);
  if (!province) {
    return "";
  }
  const city = province.cities.find((item) => item.code === cityCode);
  if (!city) {
    return province.name;
  }
  return `${province.name}-${city.name}`;
}

function formatCurrency(value: number) {
  return `¥ ${value.toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
}

function formatMoneyValue(value: number) {
  return value.toLocaleString("zh-CN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatUnitPrice(value: number) {
  return value.toLocaleString("zh-CN", { minimumFractionDigits: 0, maximumFractionDigits: 2 });
}

function formatWanRate(value?: number | string | null) {
  if (value === undefined || value === null || value === "") {
    return "";
  }
  const numeric = Number(value);
  if (!Number.isFinite(numeric) || numeric <= 0) {
    return "";
  }
  const wan = numeric >= 10000 ? numeric / 10000 : numeric;
  return wan.toLocaleString("zh-CN", { minimumFractionDigits: 0, maximumFractionDigits: 2 });
}

function formatCommissionRate(value?: number | string | null) {
  const numeric = Number(value);
  const rate = Number.isFinite(numeric) && numeric >= 0 ? numeric : 0.1;
  return `百分之${formatPercentNumber(rate * 100)}`;
}

function formatPercentNumber(value: number) {
  return value.toFixed(2).replace(/\.?0+$/, "");
}

function getExchangeRateDescription(value: string) {
  switch (value) {
    case "custom":
      return "为提升账号出租效率，平台现就比例设置作如下说明，请您知悉：\n【自定义比例】：租金直接按哈夫币余额 × 100 ÷ 自定义比例计算，不再叠加安全箱、体力、负重、大额币、默认消耗或刀皮修正。\n【自定义比例】：当前设置高于市场主流水平，可能延长出租等待时间。建议参考市场标准进行调整，以提升出租效率。";
    case "default":
      return "为提升账号出租效率，平台现就比例设置作如下说明，请您知悉：\n【默认比例】：采用后台统一配置的兑换数量，通常 1-3 日内可成功出租。若等待时间较长，可适当下调比例或搭配赠品，进一步增强对租客的吸引力。";
    case "accelerated":
      return "为提升账号出租效率，平台现就比例设置作如下说明，请您知悉：\n【特惠比例】：采用后台默认比例基础上固定 +5 的兑换数量，适合做更有吸引力的优惠出租。每日消耗加成会继续叠加。";
    default:
      return "为提升账号出租效率，平台现就比例设置作如下说明，请先选择兑换比例。";
  }
}

function getCompensationPlanDescription(value: string) {
  switch (value) {
    case "normal":
      return "【普通赔付】：依据《虚拟资产出售协议》第四条，若账号在租期内或租期外出现封禁、追回、洗号等情况，最高可获赔全额租金 + 押金。";
    case "full":
      return "【全额包赔】：在普通赔付的基础上，额外按照号市场价值进行赔付（可参照购置、助之等主流交易平台）。注：选择此方案，需额外抽取订单佣金的8%作为服务费。";
    default:
      return "请选择赔付方案，平台将根据所选方案展示对应赔付说明。";
  }
}

function getErrorMessage(error: unknown) {
  if (error instanceof Error) {
    return error.message;
  }
  return "请求失败，请稍后再试";
}
