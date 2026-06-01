import { request } from "../../lib/http-client";

export type OptionItem = {
  value: string;
  label: string;
};

export type RegionOption = {
  code: string;
  label: string;
  provinceCode: string;
  provinceName: string;
  cityName: string;
};

export type ListingSummaryChip = {
  label: string;
  tone: "sky" | "sale";
};

export type ListingPanelEntry = {
  label: string;
  value: string;
};

export type ListingPanel = {
  tone: "sky" | "orange" | "lavender" | "mint";
  wide?: boolean;
  entries: ListingPanelEntry[];
};

export type ListingPricing = {
  deposit: string;
  rent: string;
  total: string;
  extraItemsAmount: string;
};

export type ListingExtraItem = {
  label: string;
  count: number;
  unitPrice: string;
  subtotal: string;
};

export type ListingStats = {
  viewCount: number;
  favoriteCount: number;
  salesCount: number;
};

export type ListingExportInfo = {
  deliveryMethod: string;
  deliveryTimeRange: string;
  knifeSkins: string;
  redSkins: string;
  goldSkins: string;
  weaponSkins: string;
  extraItems: string;
};

export type ListingEstimateReport = {
  suggestedPrice: string;
  basis: string;
  estimatedAt: string;
};

export type ListingSellerInfo = {
  nickname: string;
  avatarText: string;
  sellerTypeLabel: string;
  studioName?: string | null;
  favorableRate: string;
  dealCount: number;
  publishCount: number;
};

export type ListingGuaranteeInfo = {
  platformReview: string;
  afterSalePeriod: string;
  violationPolicy: string;
};

export type ListingImportantFact = {
  label: string;
  value: string;
  emphasis?: boolean;
};

export type ListingDetailEntry = {
  label: string;
  value: string;
};

export type ListingDetailSection = {
  title: string;
  entries: ListingDetailEntry[];
};

export type ListingDetailCategoryItem = {
  name: string;
  note: string;
};

export type ListingDetailCategoryGroup = {
  title: string;
  items: ListingDetailCategoryItem[];
};

export type ListingRow = {
  id: string;
  seller: string;
  avatar: string;
  coverTone: "steel" | "ember" | "forest" | "midnight";
  coverUrl?: string | null;
  title: string;
  summaryChips: ListingSummaryChip[];
  panels: ListingPanel[];
  pricing: ListingPricing;
  sellerType: string;
  sellerTypeLabel: string;
  assuranceTags: string[];
  highlights: string[];
  stats: ListingStats;
  regionLabel: string;
  accountLevelLabel: string;
  hafCurrencyLabel: string;
  exchangeRateLabel: string;
  importantFacts?: ListingImportantFact[];
  extraItems: ListingExtraItem[];
  negotiable: boolean;
  publishedAtLabel: string;
  imageUrls: string[];
  videoUrl?: string | null;
  description: string;
  estimateReport: ListingEstimateReport;
  sellerInfo: ListingSellerInfo;
  guaranteeInfo: ListingGuaranteeInfo;
  baseInfoSection: ListingDetailSection;
  assetInfoSection: ListingDetailSection;
  combatInfoSection: ListingDetailSection;
  equipmentGroups: ListingDetailCategoryGroup[];
  exportInfo?: ListingExportInfo | null;
};

export type MarketplaceMeta = {
  regions: RegionOption[];
  weapons: OptionItem[];
  knifeSkins: OptionItem[];
  redSkins: OptionItem[];
  awmBulletRanges: OptionItem[];
  depositRanges: OptionItem[];
  ranks: OptionItem[];
  safeBoxLevels: OptionItem[];
  levelOptions: OptionItem[];
  deliveryMethods: OptionItem[];
  sellerTypes: OptionItem[];
  publishedDayOptions: OptionItem[];
  sortOptions: OptionItem[];
};

export type MarketplaceQuery = {
  keyword: string;
  minPrice: string;
  maxPrice: string;
  depositRange: string;
  maxHafCurrency: string;
  minLevel: string;
  maxLevel: string;
  regionCodes: string[];
  weaponCodes: string[];
  knifeSkins: string[];
  redSkins: string[];
  awmBulletRange: string;
  rank: string;
  safeBoxLevel: string;
  staminaLevel: string;
  carryLevel: string;
  deliveryMethod: string;
  sellerType: string;
  exchangeRateType: string;
  negotiable: "" | "true" | "false";
  alwaysOnline: "" | "true" | "false";
  publishedWithinDays: string;
  sort: string;
  page: number;
  pageSize: number;
};

export type MarketplaceListResult = {
  total: number;
  page: number;
  pageSize: number;
  hasMore: boolean;
  rows: ListingRow[];
};

export const DEFAULT_MARKET_QUERY: MarketplaceQuery = {
  keyword: "",
  minPrice: "",
  maxPrice: "",
  depositRange: "",
  maxHafCurrency: "",
  minLevel: "",
  maxLevel: "",
  regionCodes: [],
  weaponCodes: [],
  knifeSkins: [],
  redSkins: [],
  awmBulletRange: "",
  rank: "",
  safeBoxLevel: "",
  staminaLevel: "",
  carryLevel: "",
  deliveryMethod: "",
  sellerType: "",
  exchangeRateType: "",
  negotiable: "",
  alwaysOnline: "",
  publishedWithinDays: "",
  sort: "newest",
  page: 1,
  pageSize: 10,
};

export function loadMarketplaceMeta() {
  return request<MarketplaceMeta>("/api/public/listings/meta", { auth: false });
}

export function loadMarketplaceListings(query: MarketplaceQuery) {
  const params = new URLSearchParams();
  appendParam(params, "keyword", query.keyword);
  appendParam(params, "minPrice", query.minPrice);
  appendParam(params, "maxPrice", query.maxPrice);
  appendParam(params, "depositRange", query.depositRange);
  appendParam(params, "maxHafCurrency", query.maxHafCurrency);
  appendParam(params, "minLevel", query.minLevel);
  appendParam(params, "maxLevel", query.maxLevel);
  appendCsv(params, "regionCodes", query.regionCodes);
  appendCsv(params, "weaponCodes", query.weaponCodes);
  appendCsv(params, "knifeSkins", query.knifeSkins);
  appendCsv(params, "redSkins", query.redSkins);
  appendParam(params, "awmBulletRange", query.awmBulletRange);
  appendParam(params, "rank", query.rank);
  appendParam(params, "safeBoxLevel", query.safeBoxLevel);
  appendParam(params, "staminaLevel", query.staminaLevel);
  appendParam(params, "carryLevel", query.carryLevel);
  appendParam(params, "deliveryMethod", query.deliveryMethod);
  appendParam(params, "sellerType", query.sellerType);
  appendParam(params, "exchangeRateType", query.exchangeRateType);
  appendParam(params, "negotiable", query.negotiable);
  appendParam(params, "alwaysOnline", query.alwaysOnline);
  appendParam(params, "publishedWithinDays", query.publishedWithinDays);
  appendParam(params, "sort", query.sort || "newest");
  params.set("page", String(query.page || 1));
  params.set("pageSize", String(query.pageSize || 10));
  const search = params.toString();
  return request<MarketplaceListResult>(`/api/public/listings${search ? `?${search}` : ""}`, { auth: false });
}

export function loadFavoriteListingNos(listingNos: string[]) {
  const params = new URLSearchParams();
  appendCsv(params, "listingNos", listingNos);
  const search = params.toString();
  return request<string[]>(`/api/listings/favorites${search ? `?${search}` : ""}`);
}

export function favoriteListing(listingNo: string) {
  return request<{ listingNo: string; favorite: boolean; favoriteCount: number }>(`/api/listings/${listingNo}/favorite`, {
    method: "POST",
  });
}

export function unfavoriteListing(listingNo: string) {
  return request<{ listingNo: string; favorite: boolean; favoriteCount: number }>(`/api/listings/${listingNo}/favorite`, {
    method: "DELETE",
  });
}

function appendParam(params: URLSearchParams, key: string, value?: string) {
  const normalized = value?.trim();
  if (normalized) {
    params.set(key, normalized);
  }
}

function appendCsv(params: URLSearchParams, key: string, values?: string[]) {
  if (values && values.length) {
    params.set(key, values.join(","));
  }
}
