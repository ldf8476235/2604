import { prepareImageForUpload } from "@delta/ui";
import { request } from "../../lib/http-client";

export type OptionItem = {
  value: string;
  label: string;
};

export type ExtraItemMeta = {
  code: string;
  label: string;
  unitLabel: string;
  unitPrice: number;
};

export type RegionCity = {
  code: string;
  name: string;
};

export type RegionProvince = {
  code: string;
  name: string;
  cities: RegionCity[];
};

export type SellerContext = {
  sellerType: "PERSONAL" | "STUDIO";
  sellerLabel: string;
  reviewStrategy: "REQUIRED_REVIEW" | "DIRECT_PUBLISH";
  reviewStrategyLabel: string;
  studioName: string | null;
  sellerTypeLocked: boolean;
};

export type PublishMeta = {
  sellerContext: SellerContext;
  regions: RegionProvince[];
  deliveryMethods: OptionItem[];
  hourOptions: OptionItem[];
  ranks: OptionItem[];
  safeBoxLevels: OptionItem[];
  staminaLevels: OptionItem[];
  carryLevels: OptionItem[];
  diveLevels: OptionItem[];
  operators: OptionItem[];
  weapons: OptionItem[];
  knifeSkins: OptionItem[];
  redSkins: OptionItem[];
  weaponSkinCatalog: OptionItem[];
  goldSkins: OptionItem[];
  defaultSpendOptions: OptionItem[];
  exchangeRateOptions: OptionItem[];
  compensationPlans: OptionItem[];
  agreementOptions: OptionItem[];
  extraItems: ExtraItemMeta[];
	  modCodes: OptionItem[];
	  defaultExchangeRate: number;
	  personalSellerCommissionRate: number;
	  notices: string[];
	};

export type OssUploadTicket = {
  objectKey: string;
  uploadUrl: string;
  expireAt: string;
};

export type OssPreviewResult = {
  objectKey: string;
  previewUrl: string;
};

export type OssUploadResult = {
  objectKey: string;
  previewUrl: string;
  uploadedFile?: File;
  compressed?: boolean;
};

export type PublishPayload = {
  provinceCode: string;
  cityCode: string;
  deliveryMethod: string;
  alwaysOnline: boolean;
  deliveryStartHour: number;
  deliveryEndHour: number;
  accountLevel: number;
  rankName: string;
  safeBoxLevel: number;
  staminaLevel: number;
  carryLevel: number;
  diveLevel: number;
  banRecord: boolean;
  punishmentImageKey: string | null;
  faceOwned: boolean;
  unlockSaeed: boolean;
  hafCurrency: number;
  knifeSkins: string[];
  redSkins: string[];
  title: string;
  description: string;
  operatorCount: number;
  operators: string[];
  weapons: string[];
  weaponSkins: string[];
  goldSkins: string[];
  secretKd: number;
  defaultSpend: string;
  rentalDays: number;
  exchangeRateType: string;
  customExchangeRate: number | null;
  compensationPlan: string;
  deposit: number;
  remarks: string;
  agreements: string[];
  extraItems: Array<{
    code: string;
    count: number;
    chargeMode: "gift" | "charge";
  }>;
  otherItems: string;
  imageKeys: string[];
  videoKey: string | null;
  price: number;
  negotiable: boolean;
  modCodes: string[];
};

export type PublishSubmitResult = {
  listingNo: string;
  status: string;
  message: string;
  suggestedPrice: number;
  estimateDetail: string;
};

export type MyListingStatus = "ALL" | "PUBLISHED" | "PENDING_REVIEW" | "REJECTED" | "OFFLINE";

export type MyListingListItem = {
  listingNo: string;
  title: string;
  price: number;
  status: string;
  statusLabel: string;
  reviewProgress: string;
  viewCount: number;
  favoriteCount: number;
  salesCount: number;
  salesStatus: string;
  rejectionReason: string | null;
  updatedAt: string;
  canEdit: boolean;
  canWithdraw: boolean;
  canResubmit: boolean;
};

export type MyListingListResult = {
  total: number;
  rows: MyListingListItem[];
};

export type MyListingAsset = {
  objectKey: string;
  previewUrl: string;
  filename: string;
};

export type MyListingReviewRecord = {
  title: string;
  result: string;
  createdAt: string;
  note: string | null;
};

export type MyListingTradeRecord = {
  orderNo: string;
  buyerNickname: string;
  status: string;
  statusLabel: string;
  totalAmount: number;
  createdAt: string;
  completedAt: string | null;
};

export type MyListingDetail = {
  summary: MyListingListItem;
  draft: PublishPayload;
  images: MyListingAsset[];
  video: MyListingAsset | null;
  punishmentImage: MyListingAsset | null;
  reviewRecords: MyListingReviewRecord[];
  tradeRecords: MyListingTradeRecord[];
};

export type MyListingActionResult = {
  listingNo: string;
  status: string;
  message: string;
};

export function loadPublishMeta() {
  return request<PublishMeta>("/api/listings/publish/meta");
}

export function createUploadTicket(businessScope: string, filename: string, contentType?: string) {
  return request<OssUploadTicket>("/api/oss/upload-ticket", {
    method: "POST",
    body: JSON.stringify({ businessScope, filename, contentType }),
  });
}

export async function uploadOssFile(businessScope: string, file: File, filename: string) {
  const prepared = await prepareImageForUpload(file);
  return uploadPreparedFile(businessScope, prepared.file, normalizeUploadFilename(filename, prepared.file), prepared.compressed);
}

async function uploadPreparedFile(businessScope: string, file: File, filename: string, compressed: boolean) {
  const formData = new FormData();
  formData.append("businessScope", businessScope);
  formData.append("file", file, filename);
  const result = await request<OssUploadResult>("/api/oss/upload", {
    method: "POST",
    body: formData,
  });
  return { ...result, uploadedFile: file, compressed };
}

export async function uploadOssFileDirect(businessScope: string, file: File, filename: string) {
  const prepared = await prepareImageForUpload(file);
  const uploadFile = prepared.file;
  const uploadFilename = normalizeUploadFilename(filename, uploadFile);
  try {
    const ticket = await createUploadTicket(businessScope, uploadFilename, uploadFile.type);
    const response = await fetch(ticket.uploadUrl, {
      method: "PUT",
      headers: uploadFile.type ? { "Content-Type": uploadFile.type } : undefined,
      body: uploadFile,
    });
    if (!response.ok) {
      throw new Error("OSS 直传失败");
    }
    const preview = await loadOssPreview(ticket.objectKey).catch(() => ({ previewUrl: "" }));
    return {
      objectKey: ticket.objectKey,
      previewUrl: preview.previewUrl,
      uploadedFile: uploadFile,
      compressed: prepared.compressed,
    };
  } catch {
    return uploadPreparedFile(businessScope, uploadFile, uploadFilename, prepared.compressed);
  }
}

function normalizeUploadFilename(filename: string, file: File) {
  const extension = file.type === "image/webp" ? ".webp" : file.type === "image/jpeg" ? ".jpg" : file.type === "image/png" ? ".png" : "";
  if (!extension) {
    return filename || file.name;
  }
  const baseName = (filename || file.name || "image").replace(/\.[^.]+$/, "");
  return `${baseName}${extension}`;
}

export function submitPublish(payload: PublishPayload) {
  return request<PublishSubmitResult>("/api/listings/publish", {
    method: "POST",
    body: JSON.stringify(payload),
  });
}

export function loadMyListings(status: MyListingStatus) {
  const query = status && status !== "ALL" ? `?status=${encodeURIComponent(status)}` : "";
  return request<MyListingListResult>(`/api/listings/mine${query}`);
}

export function loadMyListingDetail(listingNo: string) {
  return request<MyListingDetail>(`/api/listings/mine/${encodeURIComponent(listingNo)}`);
}

export function updateMyListing(listingNo: string, payload: PublishPayload) {
  return request<PublishSubmitResult>(`/api/listings/mine/${encodeURIComponent(listingNo)}`, {
    method: "PUT",
    body: JSON.stringify(payload),
  });
}

export function withdrawMyListing(listingNo: string) {
  return request<MyListingActionResult>(`/api/listings/mine/${encodeURIComponent(listingNo)}/withdraw`, {
    method: "POST",
    body: JSON.stringify({}),
  });
}

export function resubmitMyListing(listingNo: string) {
  return request<MyListingActionResult>(`/api/listings/mine/${encodeURIComponent(listingNo)}/resubmit`, {
    method: "POST",
    body: JSON.stringify({}),
  });
}

export function loadOssPreview(objectKey: string) {
  return request<OssPreviewResult>(`/api/oss/preview?objectKey=${encodeURIComponent(objectKey)}`);
}
