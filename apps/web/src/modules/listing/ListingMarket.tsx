import { Button, StatusState } from "@delta/ui";
import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState, type KeyboardEvent, type MouseEvent, type PointerEvent as ReactPointerEvent, type RefObject } from "react";
import { useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../../auth/auth-context";
import {
  getListingAgreementContent,
  LISTING_REQUIRED_AGREEMENTS,
  type ListingAgreementContent,
} from "./listing-agreements";
import { WechatPayDialog, type WechatPayDialogPayload } from "../payment/WechatPayDialog";
import { createTradeAlipayPayment, createTradeOrder, createTradeWechatPayment, loadOrderDetail } from "../profile/profile-api";
import {
  DEFAULT_MARKET_QUERY,
  favoriteListing,
  loadFavoriteListingNos,
  loadMarketplaceListings,
  loadMarketplaceMeta,
  type ListingImportantFact,
  type ListingRow,
  type MarketplaceMeta,
  type MarketplaceQuery,
  type OptionItem,
  type RegionOption,
  unfavoriteListing,
} from "./listing-api";

const mobileQuickEntries = [
  { label: "资源号租赁", icon: "租" },
  { label: "账号交易", icon: "账" },
  { label: "改枪码", icon: "枪" },
  { label: "代肝", icon: "肝" },
  { label: "客服", icon: "服" },
] as const;

const desktopPresetActions = [
  { key: "list", label: "列表显示" },
  { key: "budget", label: "小额专区" },
  { key: "promotion", label: "特惠区" },
  { key: "studio", label: "工作室" },
  { key: "recent", label: "近7天" },
  { key: "always-online", label: "24h随时上号区" },
] as const;

type DesktopPresetKey = (typeof desktopPresetActions)[number]["key"];
type SortDirection = "asc" | "desc";

const sortableGroups = [
  { base: "newest", asc: "oldest", desc: "newest" },
  { base: "haf", asc: "haf_asc", desc: "haf_desc" },
  { base: "price", asc: "price_asc", desc: "price_desc" },
  { base: "ratio", asc: "ratio_asc", desc: "ratio_desc" },
  { base: "awm", asc: "awm_asc", desc: "awm_desc" },
  { base: "helmet", asc: "helmet_asc", desc: "helmet_desc" },
  { base: "armor", asc: "armor_asc", desc: "armor_desc" },
] as const;

function cloneQuery(source: MarketplaceQuery): MarketplaceQuery {
  return {
    ...source,
    regionCodes: [...source.regionCodes],
    weaponCodes: [...source.weaponCodes],
    knifeSkins: [...source.knifeSkins],
    redSkins: [...source.redSkins],
  };
}

function resetQueryPagination(source: MarketplaceQuery): MarketplaceQuery {
  return {
    ...cloneQuery(source),
    page: 1,
    pageSize: DEFAULT_MARKET_QUERY.pageSize,
  };
}

function getSortGroup(sort: string) {
  return sortableGroups.find((item) => item.asc === sort || item.desc === sort || item.base === sort);
}

function getSortDirection(sort: string): SortDirection {
  const group = getSortGroup(sort);
  return group?.asc === sort ? "asc" : "desc";
}

function nextSortValue(currentSort: string, optionValue: string) {
  const optionGroup = getSortGroup(optionValue);
  if (!optionGroup) {
    return optionValue;
  }
  const currentGroup = getSortGroup(currentSort);
  if (currentGroup?.base !== optionGroup.base) {
    return optionGroup.desc;
  }
  return getSortDirection(currentSort) === "desc" ? optionGroup.asc : optionGroup.desc;
}

function isSortOptionActive(currentSort: string, optionValue: string) {
  const optionGroup = getSortGroup(optionValue);
  const currentGroup = getSortGroup(currentSort);
  return optionGroup ? currentGroup?.base === optionGroup.base : currentSort === optionValue;
}

function appendListingRows(current: ListingRow[], incoming: ListingRow[]) {
  if (!current.length) {
    return incoming;
  }
  const existingIds = new Set(current.map((item) => item.id));
  const nextRows = incoming.filter((item) => !existingIds.has(item.id));
  return nextRows.length ? [...current, ...nextRows] : current;
}

function getPanelValue(item: ListingRow, label: string) {
  return item.panels
    .flatMap((panel) => panel.entries)
    .find((entry) => entry.label === label)
    ?.value;
}

function compactNumberLabel(value?: string) {
  return value?.replace(/\s+/g, "") ?? "";
}

function compactSafeBoxLabel(value?: string) {
  const match = value?.match(/\(([^)]+)\)/);
  return match ? `安全箱(${match[1]})` : value ? value.replace("等级", "") : "";
}

function compactLevelLabel(label: string, value?: string) {
  const normalized = compactNumberLabel(value);
  return normalized ? `${label}${normalized}` : "";
}

function compactExchangeRateLabel(value?: string) {
  if (!value) {
    return "";
  }
  const shortMatch = value.match(/(?:默认|加速|特惠|自定义)?\s*(\d+(?:\.\d+)?)w/i);
  if (shortMatch) {
    return `${shortMatch[1]}w/1元`;
  }
  const match = value.match(/1元=(\d+(?:\.\d+)?)哈夫币/);
  if (!match) {
    return value;
  }
  const amount = Number(match[1]);
  if (!Number.isFinite(amount)) {
    return value;
  }
  return amount >= 10000 ? `${amount / 10000}w/1元` : `${amount}/1元`;
}

function getMobileOverview(item: ListingRow) {
  return [
    item.hafCurrencyLabel,
    item.exchangeRateLabel,
    getPanelValue(item, "保险等级"),
    getPanelValue(item, "体力等级"),
    getPanelValue(item, "负重等级"),
    item.highlights[0],
  ]
    .filter(Boolean)
    .join(" | ");
}

function getCompactOverview(item: ListingRow) {
  return [
    `哈夫币${compactNumberLabel(item.hafCurrencyLabel)}`,
    compactSafeBoxLabel(getPanelValue(item, "保险等级")),
    compactLevelLabel("体力", getPanelValue(item, "体力等级")),
    compactLevelLabel("负重", getPanelValue(item, "负重等级")),
  ]
    .filter(Boolean)
    .join(" ");
}

function getCompactFacts(item: ListingRow) {
  if (item.importantFacts?.length) {
    return item.importantFacts.filter((fact) => fact.label !== "比例");
  }
  const fallbackFacts: ListingImportantFact[] = [];
  const fallbackEntries: Array<[string, string | undefined]> = [
    ["AWM", getPanelValue(item, "AWM")],
    ["六头", getPanelValue(item, "六头")],
    ["六甲", getPanelValue(item, "六甲")],
    ["六弹", getPanelValue(item, "六级弹")],
    ["巴雷特", getPanelValue(item, "巴雷特")],
  ];
  fallbackEntries.forEach(([label, value]) => {
    if (value) {
      fallbackFacts.push({ label, value });
    }
  });
  return fallbackFacts;
}

function getImportantFacts(item: ListingRow) {
  return item.importantFacts?.length ? item.importantFacts : getCompactFacts(item);
}

function getDetailEntryValue(item: ListingRow, label: string) {
  return [item.baseInfoSection, item.assetInfoSection, item.combatInfoSection]
    .flatMap((section) => section.entries)
    .find((entry) => entry.label === label)
    ?.value;
}

function getSummaryChipValue(item: ListingRow, prefix: string) {
  return item.summaryChips.find((chip) => chip.label.startsWith(prefix))?.label ?? "";
}

function getExportEquipmentSummary(item: ListingRow) {
  const facts = getCompactFacts(item)
    .map((entry) => `${entry.label}${entry.value}`)
    .filter(Boolean);
  const extras = item.extraItems
    .map((entry) => `${entry.label}${entry.count}`)
    .filter(Boolean);
  return Array.from(new Set([...facts, ...extras])).join("、") || "无";
}

function normalizeExportText(value?: string | null) {
  return value?.replace(/\s+/g, " ").trim() || "";
}

function escapeExcelCell(value?: string | number | null) {
  return String(value ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function buildListingExportRows(items: ListingRow[]) {
  return items.map((item) => ({
    编号: item.id,
    区服: item.exportInfo?.deliveryMethod || getSummaryChipValue(item, "QQ") || getSummaryChipValue(item, "微信") || getSummaryChipValue(item, "Steam") || item.regionLabel,
    纯币: item.hafCurrencyLabel,
    租期: getDetailEntryValue(item, "出租天数") || getSummaryChipValue(item, "租期"),
    段位: getDetailEntryValue(item, "段位") || getDetailEntryValue(item, "最高段位"),
    保险: getDetailEntryValue(item, "安全箱等级") || getPanelValue(item, "保险等级"),
    体力: getPanelValue(item, "体力等级"),
    负重: getPanelValue(item, "负重等级"),
    价格: item.pricing.rent,
    押金: item.pricing.deposit,
    比例: item.exchangeRateLabel,
    刀皮: item.exportInfo?.knifeSkins || getPanelValue(item, "持有刀皮") || item.highlights.join("、"),
    人物皮: item.exportInfo?.redSkins || getPanelValue(item, "干员外观") || getDetailEntryValue(item, "核心干员"),
    备注装备: [
      item.exportInfo?.weaponSkins,
      item.exportInfo?.goldSkins,
      item.exportInfo?.extraItems,
    ].filter((value) => value && value !== "无").join("、") || getExportEquipmentSummary(item),
    客服: "",
    备注: normalizeExportText(item.description),
    号主在线时间: item.exportInfo?.deliveryTimeRange || (item.assuranceTags.includes("24h在线") ? "24h在线" : ""),
    号结算情况: "",
  }));
}

function downloadListingsExcel(items: ListingRow[]) {
  const rows = buildListingExportRows(items);
  const columns = [
    "编号",
    "区服",
    "纯币",
    "租期",
    "段位",
    "保险",
    "体力",
    "负重",
    "价格",
    "押金",
    "比例",
    "刀皮",
    "人物皮",
    "备注装备",
    "客服",
    "备注",
    "号主在线时间",
    "号结算情况",
  ] as const;
  const today = new Date();
  const stamp = [
    today.getFullYear(),
    String(today.getMonth() + 1).padStart(2, "0"),
    String(today.getDate()).padStart(2, "0"),
    String(today.getHours()).padStart(2, "0"),
    String(today.getMinutes()).padStart(2, "0"),
  ].join("");
  const bodyRows = rows
    .map((row, rowIndex) => {
      const cells = columns
        .map((column) => {
          const value = row[column];
          const className = column === "比例"
            ? "ratio-cell"
            : column === "刀皮" || column === "人物皮" || column === "备注装备"
              ? "accent-cell"
              : "";
          return `<td class="${className}">${escapeExcelCell(value)}</td>`;
        })
        .join("");
      return `<tr><td class="index-cell">${rowIndex + 1}</td>${cells}</tr>`;
    })
    .join("");
  const html = `<!doctype html>
<html>
<head>
  <meta charset="UTF-8" />
  <style>
    table { border-collapse: collapse; font-family: "Microsoft YaHei", Arial, sans-serif; }
    td, th { border: 1px solid #111827; padding: 7px 10px; font-size: 12px; mso-number-format:"\\@"; }
    .hero td { background: #b9d8f2; border-color: #b9d8f2; font-weight: 700; }
    .hero-title { font-size: 30px; text-align: center; color: #111827; }
    .hero-title strong { color: #e11d48; }
    .note td { background: #b9d8f2; border-color: #b9d8f2; font-weight: 600; }
    .head th { background: #050505; color: #ffffff; font-size: 14px; font-weight: 700; text-align: center; }
    .index-cell { text-align: center; color: #64748b; }
    .ratio-cell { background: #facc15; color: #111827; font-weight: 700; text-align: center; }
    .accent-cell { color: #ef4444; font-weight: 600; }
  </style>
</head>
<body>
  <table>
    <colgroup>
      <col style="width:48px" />
      ${columns.map(() => '<col style="width:110px" />').join("")}
    </colgroup>
    <tr class="hero"><td colspan="${columns.length + 1}" class="hero-title">萌虎商行 <strong>已上架账号导出</strong></td></tr>
    <tr class="note"><td colspan="${columns.length + 1}">1、当前文件按首页已应用筛选条件导出，只包含已上架账号。</td></tr>
    <tr class="note"><td colspan="${columns.length + 1}">2、价格、押金、比例、物资信息来自发布快照，便于客服核对和线下沟通。</td></tr>
    <tr class="note"><td colspan="${columns.length + 1}">3、导出时间：${escapeExcelCell(today.toLocaleString("zh-CN"))}，账号数量：${rows.length}。</td></tr>
    <tr class="head"><th>序号</th>${columns.map((column) => `<th>${escapeExcelCell(column)}</th>`).join("")}</tr>
    ${bodyRows}
  </table>
</body>
</html>`;
  const blob = new Blob(["\ufeff", html], { type: "application/vnd.ms-excel;charset=utf-8" });
  const url = window.URL.createObjectURL(blob);
  const anchor = document.createElement("a");
  anchor.href = url;
  anchor.download = `萌虎商行-已上架账号-${stamp}.xls`;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();
  window.URL.revokeObjectURL(url);
}

function getKeySummaryChips(item: ListingRow, limit = 5) {
  return item.summaryChips
    .filter((chip) => !chip.label.startsWith("建议价"))
    .slice(0, limit);
}

function findOptionLabel(options: OptionItem[], value: string) {
  return options.find((item) => item.value === value)?.label ?? "";
}

function summarizeMultiSelect(values: string[], resolveLabel: (value: string) => string, emptyLabel: string) {
  if (!values.length) {
    return emptyLabel;
  }
  if (values.length === 1) {
    return resolveLabel(values[0]);
  }
  return `${resolveLabel(values[0])} 等 ${values.length} 项`;
}

function selectSingleOption(target: HTMLElement, value: string, onSelect: (value: string) => void) {
  onSelect(value);
  const details = target.closest("details");
  if (details instanceof HTMLDetailsElement) {
    details.open = false;
  }
}

function closeCompactFilters(container: HTMLElement | null, except?: HTMLDetailsElement) {
  container?.querySelectorAll<HTMLDetailsElement>("details.market-compact-filter[open]").forEach((details) => {
    if (details !== except) {
      details.open = false;
    }
  });
}

async function copyText(text: string) {
  if (typeof navigator !== "undefined" && navigator.clipboard?.writeText) {
    try {
      await navigator.clipboard.writeText(text);
      return;
    } catch {
      // Fall through to the textarea fallback for restricted clipboard contexts.
    }
  }
  if (typeof document === "undefined") {
    throw new Error("clipboard unavailable");
  }
  const textarea = document.createElement("textarea");
  textarea.value = text;
  textarea.setAttribute("readonly", "true");
  textarea.style.position = "fixed";
  textarea.style.left = "-9999px";
  textarea.style.top = "0";
  document.body.appendChild(textarea);
  textarea.select();
  const copied = document.execCommand("copy");
  document.body.removeChild(textarea);
  if (!copied) {
    throw new Error("copy command failed");
  }
}

function handleCompactFilterSummaryClick(target: HTMLElement) {
  const details = target.closest("details.market-compact-filter");
  if (!(details instanceof HTMLDetailsElement)) {
    return;
  }
  closeCompactFilters(target.closest(".market-desktop-filters"), details);
}

function useCompactFilterAutoClose(containerRef: RefObject<HTMLElement | null>, enabled: boolean) {
  useEffect(() => {
    if (!enabled) {
      return;
    }
    function handlePointerDown(event: globalThis.MouseEvent) {
      const container = containerRef.current;
      if (!container) {
        return;
      }
      const target = event.target as Node;
      if (!container.contains(target)) {
        closeCompactFilters(container);
        return;
      }
      const dropdownRoot = target instanceof Element ? target.closest("details.market-compact-filter, .market-region-picker") : null;
      if (dropdownRoot instanceof HTMLDetailsElement) {
        closeCompactFilters(container, dropdownRoot);
        return;
      }
      if (dropdownRoot) {
        closeCompactFilters(container);
      }
    }

    function handleKeyDown(event: globalThis.KeyboardEvent) {
      if (event.key === "Escape") {
        closeCompactFilters(containerRef.current);
      }
    }

    document.addEventListener("mousedown", handlePointerDown);
    document.addEventListener("keydown", handleKeyDown);
    return () => {
      document.removeEventListener("mousedown", handlePointerDown);
      document.removeEventListener("keydown", handleKeyDown);
    };
  }, [containerRef, enabled]);
}

type RegionProvinceGroup = {
  code: string;
  name: string;
  cities: RegionOption[];
};

type ListingMarketProps = {
  marketType: "rental" | "trade";
};

function groupRegionsByProvince(regions: RegionOption[]) {
  const groups = new Map<string, RegionProvinceGroup>();
  regions.forEach((region) => {
    if (!groups.has(region.provinceCode)) {
      groups.set(region.provinceCode, {
        code: region.provinceCode,
        name: region.provinceName,
        cities: [],
      });
    }
    groups.get(region.provinceCode)?.cities.push(region);
  });
  return Array.from(groups.values());
}

function findSelectedRegion(regions: RegionOption[], selectedCodes: string[]) {
  if (!selectedCodes.length) {
    return null;
  }
  return regions.find((region) => selectedCodes.includes(region.code)) ?? null;
}

function formatSelectedRegion(regions: RegionOption[], selectedCodes: string[], placeholder: string) {
  const selected = findSelectedRegion(regions, selectedCodes);
  if (!selected) {
    return placeholder;
  }
  return `${selected.provinceName}-${selected.cityName}`;
}

type FilterPanelProps = {
  meta: MarketplaceMeta;
  draft: MarketplaceQuery;
  onChange: (next: MarketplaceQuery) => void;
  onApply: () => void;
  onReset: () => void;
  mode: "desktop" | "mobile";
};

function FilterPanel({ meta, draft, onChange, onApply, onReset, mode }: FilterPanelProps) {
  const desktopFiltersRef = useRef<HTMLDivElement | null>(null);
  useCompactFilterAutoClose(desktopFiltersRef, mode === "desktop");

  const update = <K extends keyof MarketplaceQuery>(key: K, value: MarketplaceQuery[K]) => {
    onChange({
      ...draft,
      [key]: value,
    });
  };

  if (mode === "desktop") {
    return (
      <div className="market-desktop-filters" ref={desktopFiltersRef}>
        <div className="market-desktop-filters__row">
          <CompactSearchField
            placeholder="编号搜索"
            value={draft.keyword}
            onChange={(value) => update("keyword", value)}
            onSubmit={onApply}
          />
          <CompactRangeField
            label="价格区间"
            leftPlaceholder="最低价"
            leftValue={draft.minPrice}
            rightPlaceholder="最高价"
            rightValue={draft.maxPrice}
            onLeftChange={(value) => update("minPrice", value)}
            onRightChange={(value) => update("maxPrice", value)}
          />
          <CompactSelectField
            label="押金"
            placeholder="押金"
            options={meta.depositRanges}
            selected={draft.depositRange}
            onSelect={(value) => update("depositRange", value)}
          />
          <CompactRangeField
            label="账号等级"
            leftPlaceholder="最低等级"
            leftValue={draft.minLevel}
            rightPlaceholder="最高等级"
            rightValue={draft.maxLevel}
            onLeftChange={(value) => update("minLevel", value)}
            onRightChange={(value) => update("maxLevel", value)}
          />
          <CompactRegionField
            placeholder="地区选择"
            regions={meta.regions}
            selected={draft.regionCodes}
            onSelect={(values) => update("regionCodes", values)}
          />
          <CompactSelectField
            label="安全箱"
            placeholder="安全箱"
            options={meta.safeBoxLevels}
            selected={draft.safeBoxLevel}
            onSelect={(value) => update("safeBoxLevel", value)}
          />
          <CompactSelectField
            label="体力"
            placeholder="体力"
            options={meta.levelOptions}
            selected={draft.staminaLevel}
            onSelect={(value) => update("staminaLevel", value)}
          />
          <CompactSelectField
            label="负重"
            placeholder="负重"
            options={meta.levelOptions}
            selected={draft.carryLevel}
            onSelect={(value) => update("carryLevel", value)}
          />
          <CompactMultiSelectField
            label="刀皮筛选"
            placeholder="刀皮筛选"
            options={meta.knifeSkins}
            selected={draft.knifeSkins}
            onToggle={(value) => update("knifeSkins", toggleValue(draft.knifeSkins, value))}
          />
          <CompactMultiSelectField
            label="红皮筛选"
            placeholder="红皮筛选"
            options={meta.redSkins}
            selected={draft.redSkins}
            onToggle={(value) => update("redSkins", toggleValue(draft.redSkins, value))}
          />
          <CompactSelectField
            label="段位"
            placeholder="段位"
            options={meta.ranks}
            selected={draft.rank}
            onSelect={(value) => update("rank", value)}
          />
          <CompactSelectField
            label="登陆方式"
            placeholder="登陆方式"
            options={meta.deliveryMethods}
            selected={draft.deliveryMethod}
            onSelect={(value) => update("deliveryMethod", value)}
          />
          <div className="market-desktop-filters__actions">
            <button className="market-filter-action market-filter-action--confirm" type="button" onClick={onApply}>
              筛选
            </button>
            <button className="market-filter-action market-filter-action--reset" type="button" onClick={onReset}>
              重置筛选条件
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className={`market-filters market-filters--${mode}`}>
      <div className="market-filters__grid">
        <FilterTextField
          label="关键词"
          note="支持编号、标题、卖家关键词搜索"
          placeholder="输入编号或标题"
          value={draft.keyword}
          onChange={(value) => update("keyword", value)}
        />
        <FilterRangeField
          label="价格区间"
          leftPlaceholder="最低价"
          leftValue={draft.minPrice}
          rightPlaceholder="最高价"
          rightValue={draft.maxPrice}
          note="支持手动输入，单位元"
          onLeftChange={(value) => update("minPrice", value)}
          onRightChange={(value) => update("maxPrice", value)}
        />
        <FilterSingleSelectField
          label="押金"
          note="按押金金额分档筛选"
          placeholder="选择押金"
          options={meta.depositRanges}
          selected={draft.depositRange}
          onSelect={(value) => update("depositRange", value)}
        />
        <FilterRangeField
          label="账号等级"
          leftPlaceholder="最低等级"
          leftValue={draft.minLevel}
          rightPlaceholder="最高等级"
          rightValue={draft.maxLevel}
          note="区间筛选，空则不限"
          onLeftChange={(value) => update("minLevel", value)}
          onRightChange={(value) => update("maxLevel", value)}
        />
        <FilterRegionField
          label="地区选择"
          note="参考站省市二栏筛选，按城市单选"
          placeholder="请选择地区"
          regions={meta.regions}
          selected={draft.regionCodes}
          onSelect={(values) => update("regionCodes", values)}
        />
        <FilterSingleSelectField
          label="安全箱等级"
          note="与参考站安全箱分档保持一致"
          placeholder="选择安全箱"
          options={meta.safeBoxLevels}
          selected={draft.safeBoxLevel}
          onSelect={(value) => update("safeBoxLevel", value)}
        />
        <FilterSingleSelectField
          label="体力"
          note="按体力等级筛选"
          placeholder="选择体力"
          options={meta.levelOptions}
          selected={draft.staminaLevel}
          onSelect={(value) => update("staminaLevel", value)}
        />
        <FilterSingleSelectField
          label="负重"
          note="按负重等级筛选"
          placeholder="选择负重"
          options={meta.levelOptions}
          selected={draft.carryLevel}
          onSelect={(value) => update("carryLevel", value)}
        />
        <FilterMultiSelectField
          label="刀皮筛选"
          note="多选刀皮，命中任一项即会展示"
          placeholder="选择刀皮"
          options={meta.knifeSkins}
          selected={draft.knifeSkins}
          onToggle={(value) => update("knifeSkins", toggleValue(draft.knifeSkins, value))}
        />
        <FilterMultiSelectField
          label="红皮筛选"
          note="多选红皮，命中任一项即会展示"
          placeholder="选择红皮"
          options={meta.redSkins}
          selected={draft.redSkins}
          onToggle={(value) => update("redSkins", toggleValue(draft.redSkins, value))}
        />
        <FilterSingleSelectField
          label="段位"
          note="按游戏段位筛选"
          placeholder="选择段位"
          options={meta.ranks}
          selected={draft.rank}
          onSelect={(value) => update("rank", value)}
        />
        <FilterSingleSelectField
          label="登陆方式"
          note="按上号方式筛选"
          placeholder="选择登陆方式"
          options={meta.deliveryMethods}
          selected={draft.deliveryMethod}
          onSelect={(value) => update("deliveryMethod", value)}
        />
      </div>

      <div className="market-filters__actions">
        <span className="market-filters__summary">
          当前排序：{findOptionLabel(meta.sortOptions, draft.sort) || "最新发布"}
        </span>
        <div className="market-filters__action-buttons">
          <button className="filter-reset" type="button" onClick={onReset}>
            重置筛选条件
          </button>
          <button className="filter-confirm" type="button" onClick={onApply}>
            应用筛选条件
          </button>
        </div>
      </div>
    </div>
  );
}

function CompactSearchField({
  value,
  placeholder,
  onChange,
  onSubmit,
}: {
  value: string;
  placeholder: string;
  onChange: (value: string) => void;
  onSubmit: () => void;
}) {
  return (
    <label className="market-compact-search">
      <span className="market-compact-search__icon" aria-hidden="true" />
      <input
        value={value}
        placeholder={placeholder}
        onChange={(event) => onChange(event.target.value)}
        onKeyDown={(event) => {
          if (event.key === "Enter") {
            onSubmit();
          }
        }}
      />
    </label>
  );
}

function CompactRangeField({
  label,
  leftPlaceholder,
  rightPlaceholder,
  leftValue,
  rightValue,
  onLeftChange,
  onRightChange,
}: {
  label: string;
  leftPlaceholder: string;
  rightPlaceholder: string;
  leftValue: string;
  rightValue: string;
  onLeftChange: (value: string) => void;
  onRightChange: (value: string) => void;
}) {
  return (
    <details className="market-compact-filter market-compact-filter--range">
      <summary onClick={(event) => handleCompactFilterSummaryClick(event.currentTarget)}>
        <span>{label}</span>
        <i aria-hidden="true" />
      </summary>
      <div className="market-filter-select__menu market-filter-select__menu--range">
        <div className="market-filter-range">
          <label className="market-filter-input">
            <input value={leftValue} placeholder={leftPlaceholder} onChange={(event) => onLeftChange(event.target.value)} />
          </label>
          <span className="market-filter-range__divider">-</span>
          <label className="market-filter-input">
            <input value={rightValue} placeholder={rightPlaceholder} onChange={(event) => onRightChange(event.target.value)} />
          </label>
        </div>
      </div>
    </details>
  );
}

function CompactRegionField({
  placeholder,
  regions,
  selected,
  onSelect,
}: {
  placeholder: string;
  regions: RegionOption[];
  selected: string[];
  onSelect: (values: string[]) => void;
}) {
  return <RegionPicker className="market-region-picker--compact" placeholder={placeholder} regions={regions} selected={selected} onSelect={onSelect} />;
}

function CompactSelectField({
  label,
  placeholder,
  options,
  selected,
  onSelect,
}: {
  label: string;
  placeholder: string;
  options: OptionItem[];
  selected: string;
  onSelect: (value: string) => void;
}) {
  return (
    <details className="market-compact-filter">
      <summary onClick={(event) => handleCompactFilterSummaryClick(event.currentTarget)}>
        <span>{selected ? findOptionLabel(options, selected) : placeholder || label}</span>
        <i aria-hidden="true" />
      </summary>
      <div className="market-filter-select__menu">
        <button
          className={`market-filter-option ${!selected ? "is-active" : ""}`}
          type="button"
          onClick={(event) => selectSingleOption(event.currentTarget, "", onSelect)}
        >
          不限
        </button>
        {options.map((item) => (
          <button
            className={`market-filter-option ${item.value === selected ? "is-active" : ""}`}
            key={`${label}-${item.value}`}
            type="button"
            onClick={(event) => selectSingleOption(event.currentTarget, item.value, onSelect)}
          >
            {item.label}
          </button>
        ))}
      </div>
    </details>
  );
}

function CompactMultiSelectField({
  label,
  placeholder,
  options,
  selected,
  onToggle,
}: {
  label: string;
  placeholder: string;
  options: Array<{ value: string; label: string }>;
  selected: string[];
  onToggle: (value: string) => void;
}) {
  const [keyword, setKeyword] = useState("");

  const visibleOptions = useMemo(() => {
    const normalized = keyword.trim().toLowerCase();
    if (!normalized) {
      return options;
    }
    return options.filter((item) => item.label.toLowerCase().includes(normalized));
  }, [keyword, options]);

  return (
    <details className="market-compact-filter market-compact-filter--wide">
      <summary onClick={(event) => handleCompactFilterSummaryClick(event.currentTarget)}>
        <span>
          {summarizeMultiSelect(
            selected,
            (value) => options.find((item) => item.value === value)?.label ?? value,
            placeholder || label,
          )}
        </span>
        <i aria-hidden="true" />
      </summary>
      <div className="market-filter-select__menu market-filter-select__menu--multi">
        <label className="market-filter-search">
          <input value={keyword} placeholder={`搜索${label}`} onChange={(event) => setKeyword(event.target.value)} />
        </label>
        <div className="market-filter-option-list">
          {visibleOptions.map((item) => (
            <button
              className={`market-filter-option market-filter-option--checkbox ${selected.includes(item.value) ? "is-active" : ""}`}
              key={`${label}-${item.value}`}
              type="button"
              onClick={() => onToggle(item.value)}
            >
              <span className="market-filter-option__mark" aria-hidden="true" />
              <span>{item.label}</span>
            </button>
          ))}
        </div>
      </div>
    </details>
  );
}

type FilterFieldProps = {
  label: string;
  note: string;
};

function FilterTextField({
  label,
  note,
  value,
  onChange,
  placeholder,
}: FilterFieldProps & {
  value: string;
  placeholder: string;
  onChange: (value: string) => void;
}) {
  return (
    <div className="market-filter-field">
      <div className="market-filter-field__head">
        <strong>{label}</strong>
        <span>{note}</span>
      </div>
      <label className="market-filter-input">
        <input value={value} placeholder={placeholder} onChange={(event) => onChange(event.target.value)} />
      </label>
    </div>
  );
}

function FilterRangeField({
  label,
  note,
  leftValue,
  rightValue,
  leftPlaceholder,
  rightPlaceholder,
  onLeftChange,
  onRightChange,
}: FilterFieldProps & {
  leftValue: string;
  rightValue: string;
  leftPlaceholder: string;
  rightPlaceholder: string;
  onLeftChange: (value: string) => void;
  onRightChange: (value: string) => void;
}) {
  return (
    <div className="market-filter-field">
      <div className="market-filter-field__head">
        <strong>{label}</strong>
        <span>{note}</span>
      </div>
      <div className="market-filter-range">
        <label className="market-filter-input">
          <input value={leftValue} placeholder={leftPlaceholder} onChange={(event) => onLeftChange(event.target.value)} />
        </label>
        <span className="market-filter-range__divider">-</span>
        <label className="market-filter-input">
          <input value={rightValue} placeholder={rightPlaceholder} onChange={(event) => onRightChange(event.target.value)} />
        </label>
      </div>
    </div>
  );
}

function FilterRegionField({
  label,
  note,
  placeholder,
  regions,
  selected,
  onSelect,
}: FilterFieldProps & {
  placeholder: string;
  regions: RegionOption[];
  selected: string[];
  onSelect: (values: string[]) => void;
}) {
  return (
    <div className="market-filter-field">
      <div className="market-filter-field__head">
        <strong>{label}</strong>
        <span>{note}</span>
      </div>
      <RegionPicker placeholder={placeholder} regions={regions} selected={selected} onSelect={onSelect} />
    </div>
  );
}

function FilterChoiceField({
  label,
  note,
  value,
  options,
  onChange,
}: FilterFieldProps & {
  value: string;
  options: Array<{ value: string; label: string }>;
  onChange: (value: string) => void;
}) {
  return (
    <div className="market-filter-field">
      <div className="market-filter-field__head">
        <strong>{label}</strong>
        <span>{note}</span>
      </div>
      <div className="market-choice-group">
        {options.map((item) => (
          <button
            className={`market-choice-pill ${item.value === value ? "is-active" : ""}`}
            key={`${label}-${item.value || "all"}`}
            type="button"
            onClick={() => onChange(item.value)}
          >
            {item.label}
          </button>
        ))}
      </div>
    </div>
  );
}

function FilterSingleSelectField({
  label,
  note,
  placeholder,
  options,
  selected,
  onSelect,
}: FilterFieldProps & {
  placeholder: string;
  options: OptionItem[];
  selected: string;
  onSelect: (value: string) => void;
}) {
  return (
    <div className="market-filter-field">
      <div className="market-filter-field__head">
        <strong>{label}</strong>
        <span>{note}</span>
      </div>
      <details className="market-filter-select">
        <summary>
          <span>{selected ? findOptionLabel(options, selected) : placeholder}</span>
          <i aria-hidden="true" />
        </summary>
        <div className="market-filter-select__menu">
          <button
            className={`market-filter-option ${!selected ? "is-active" : ""}`}
            type="button"
            onClick={(event) => selectSingleOption(event.currentTarget, "", onSelect)}
          >
            不限
          </button>
          {options.map((item) => (
            <button
              className={`market-filter-option ${item.value === selected ? "is-active" : ""}`}
              key={item.value}
              type="button"
              onClick={(event) => selectSingleOption(event.currentTarget, item.value, onSelect)}
            >
              {item.label}
            </button>
          ))}
        </div>
      </details>
    </div>
  );
}

function FilterMultiSelectField({
  label,
  note,
  placeholder,
  options,
  selected,
  onToggle,
}: FilterFieldProps & {
  placeholder: string;
  options: Array<{ value: string; label: string }>;
  selected: string[];
  onToggle: (value: string) => void;
}) {
  const [keyword, setKeyword] = useState("");

  const visibleOptions = useMemo(() => {
    const normalized = keyword.trim().toLowerCase();
    if (!normalized) {
      return options;
    }
    return options.filter((item) => item.label.toLowerCase().includes(normalized));
  }, [keyword, options]);

  return (
    <div className="market-filter-field">
      <div className="market-filter-field__head">
        <strong>{label}</strong>
        <span>{note}</span>
      </div>
      <details className="market-filter-select market-filter-select--wide">
        <summary>
          <span>
            {summarizeMultiSelect(
              selected,
              (value) => options.find((item) => item.value === value)?.label ?? value,
              placeholder,
            )}
          </span>
          <i aria-hidden="true" />
        </summary>
        <div className="market-filter-select__menu market-filter-select__menu--multi">
          <label className="market-filter-search">
            <input
              value={keyword}
              placeholder={`搜索${label}`}
              onChange={(event) => setKeyword(event.target.value)}
            />
          </label>
          <div className="market-filter-option-list">
            {visibleOptions.map((item) => (
              <button
                className={`market-filter-option market-filter-option--checkbox ${selected.includes(item.value) ? "is-active" : ""}`}
                key={item.value}
                type="button"
                onClick={() => onToggle(item.value)}
              >
                <span className="market-filter-option__mark" aria-hidden="true" />
                <span>{item.label}</span>
              </button>
            ))}
          </div>
        </div>
      </details>
    </div>
  );
}

function RegionPicker({
  className,
  placeholder,
  regions,
  selected,
  onSelect,
}: {
  className?: string;
  placeholder: string;
  regions: RegionOption[];
  selected: string[];
  onSelect: (values: string[]) => void;
}) {
  const containerRef = useRef<HTMLDivElement | null>(null);
  const searchInputRef = useRef<HTMLInputElement | null>(null);
  const cityTapRef = useRef<{ code: string; x: number; y: number } | null>(null);
  const suppressCityClickRef = useRef(false);
  const [open, setOpen] = useState(false);
  const [keyword, setKeyword] = useState("");
  const provinceGroups = useMemo(() => groupRegionsByProvince(regions), [regions]);
  const selectedRegion = useMemo(() => findSelectedRegion(regions, selected), [regions, selected]);
  const [activeProvinceCode, setActiveProvinceCode] = useState(selectedRegion?.provinceCode ?? provinceGroups[0]?.code ?? "");

  const normalizedKeyword = keyword.trim().toLowerCase();
  const filteredGroups = useMemo(() => {
    if (!normalizedKeyword) {
      return provinceGroups;
    }
    return provinceGroups.filter((group) => {
      if (group.name.toLowerCase().includes(normalizedKeyword)) {
        return true;
      }
      return group.cities.some(
        (city) =>
          city.cityName.toLowerCase().includes(normalizedKeyword) ||
          `${city.provinceName}-${city.cityName}`.toLowerCase().includes(normalizedKeyword),
      );
    });
  }, [normalizedKeyword, provinceGroups]);

  const activeProvince = filteredGroups.find((group) => group.code === activeProvinceCode) ?? filteredGroups[0] ?? null;
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
    return activeProvince.cities.filter(
      (city) =>
        city.cityName.toLowerCase().includes(normalizedKeyword) ||
        `${city.provinceName}-${city.cityName}`.toLowerCase().includes(normalizedKeyword),
    );
  }, [activeProvince, normalizedKeyword]);

  useEffect(() => {
    if (!open) {
      return;
    }
    setActiveProvinceCode(selectedRegion?.provinceCode ?? filteredGroups[0]?.code ?? "");
  }, [filteredGroups, open, selectedRegion]);

  useEffect(() => {
    if (!open) {
      return;
    }
    function handlePointerDown(event: globalThis.PointerEvent) {
      if (containerRef.current?.contains(event.target as Node)) {
        return;
      }
      setOpen(false);
    }
    document.addEventListener("pointerdown", handlePointerDown);
    return () => document.removeEventListener("pointerdown", handlePointerDown);
  }, [open]);

  const selectRegion = (values: string[]) => {
    onSelect(values);
    setKeyword("");
    setOpen(false);
    searchInputRef.current?.blur();
  };

  const handleCityPointerDown = (event: ReactPointerEvent<HTMLButtonElement>, cityCode: string) => {
    if (event.pointerType === "mouse") {
      return;
    }
    cityTapRef.current = {
      code: cityCode,
      x: event.clientX,
      y: event.clientY,
    };
    suppressCityClickRef.current = false;
  };

  const handleCityPointerUp = (event: ReactPointerEvent<HTMLButtonElement>, cityCode: string) => {
    if (event.pointerType === "mouse") {
      return;
    }
    const start = cityTapRef.current;
    cityTapRef.current = null;
    if (!start || start.code !== cityCode) {
      return;
    }
    const moved = Math.hypot(event.clientX - start.x, event.clientY - start.y);
    if (moved > 8) {
      return;
    }
    suppressCityClickRef.current = true;
    selectRegion([cityCode]);
  };

  return (
    <div className={`market-region-picker ${className ?? ""} ${open ? "is-open" : ""}`.trim()} ref={containerRef}>
      <button
        aria-expanded={open}
        className="market-region-picker__trigger"
        type="button"
        onClick={() => setOpen((previous) => !previous)}
      >
        <span className={selected.length ? "" : "is-placeholder"}>{formatSelectedRegion(regions, selected, placeholder)}</span>
        <span className="market-region-picker__arrow" />
      </button>
      {open ? (
        <div className="market-region-picker__panel" onPointerDown={(event) => event.stopPropagation()}>
          <div className="market-region-picker__search">
            <input
              ref={searchInputRef}
              placeholder="搜索省份/区域"
              value={keyword}
              onChange={(event) => setKeyword(event.target.value)}
            />
          </div>
          <div className="market-region-picker__columns">
            <div className="market-region-picker__column market-region-picker__column--province" role="listbox" aria-label="省份列表">
              <button
                className={`market-region-picker__option ${!selected.length ? "is-active" : ""}`}
                type="button"
                onClick={() => {
                  selectRegion([]);
                }}
              >
                全部地区
              </button>
              {filteredGroups.length ? (
                filteredGroups.map((group) => (
                  <button
                    className={`market-region-picker__option ${group.code === activeProvince?.code ? "is-active" : ""}`}
                    key={group.code}
                    type="button"
                    onMouseEnter={() => setActiveProvinceCode(group.code)}
                    onClick={() => setActiveProvinceCode(group.code)}
                  >
                    {group.name}
                  </button>
                ))
              ) : (
                <div className="market-region-picker__empty">没有匹配的省份</div>
              )}
            </div>
            <div className="market-region-picker__column market-region-picker__column--city" role="listbox" aria-label="城市列表">
              {visibleCities.length ? (
                visibleCities.map((city) => (
                  <button
                    className={`market-region-picker__option ${selected.includes(city.code) ? "is-active" : ""}`}
                    key={city.code}
                    type="button"
                    onPointerCancel={() => {
                      cityTapRef.current = null;
                    }}
                    onPointerDown={(event) => {
                      handleCityPointerDown(event, city.code);
                    }}
                    onPointerUp={(event) => {
                      handleCityPointerUp(event, city.code);
                    }}
                    onClick={() => {
                      if (suppressCityClickRef.current) {
                        suppressCityClickRef.current = false;
                        return;
                      }
                      selectRegion([city.code]);
                    }}
                  >
                    {city.cityName}
                  </button>
                ))
              ) : (
                <div className="market-region-picker__empty">请选择左侧省份</div>
              )}
            </div>
          </div>
        </div>
      ) : null}
    </div>
  );
}

type ListingCardProps = {
  item: ListingRow;
  favorited: boolean;
  onFavorite: (item: ListingRow, event: MouseEvent<HTMLButtonElement>) => void;
  onShare: (item: ListingRow, event: MouseEvent<HTMLButtonElement>) => void;
  onCopyId: (item: ListingRow, event: MouseEvent<HTMLButtonElement>) => void;
  onOpen: (item: ListingRow) => void;
};

function ListingMobileCard({ item, favorited, onFavorite, onShare }: Omit<ListingCardProps, "onOpen" | "onCopyId">) {
  return (
    <div className="market-card-mobile">
      <div className={`market-card-mobile__hero market-row__thumb--${item.coverTone}`}>
        {item.coverUrl ? (
          <img alt={item.title} className="market-card-mobile__cover-image" src={item.coverUrl} />
        ) : (
          <div className="market-row__thumb-layout">
            <div className="thumb-preview">
              <div className="thumb-preview__character" />
              <div className="thumb-preview__hud" />
            </div>
            <div className="thumb-grid" aria-hidden="true">
              <span className="thumb-slot" />
              <span className="thumb-slot" />
              <span className="thumb-slot thumb-slot--wide" />
              <span className="thumb-slot" />
              <span className="thumb-slot" />
            </div>
          </div>
        )}
        <div className="market-card-mobile__actions">
          <button
            aria-label={favorited ? "取消收藏" : "收藏"}
            className={`market-icon-button ${favorited ? "is-active" : ""}`}
            type="button"
            onClick={(event) => onFavorite(item, event)}
          >
            ♥
          </button>
          <button aria-label="快速分享" className="market-icon-button" type="button" onClick={(event) => onShare(item, event)}>
            ↗
          </button>
        </div>
        <div className="market-card-mobile__overlay">{getMobileOverview(item)}</div>
      </div>
      <div className="market-card-mobile__body">
        <div className="market-card-mobile__meta">
          <div className="market-card-mobile__seller">
            <span className={`seller-avatar seller-avatar--${item.coverTone}`}>{item.avatar}</span>
            <strong>{item.seller}</strong>
            <span className={`market-seller-tag market-seller-tag--${item.sellerType.toLowerCase()}`}>{item.sellerTypeLabel}</span>
          </div>
          <div className="market-card-mobile__serial">
            <span>{item.id}</span>
          </div>
        </div>
        <div className="market-card-mobile__title">{item.title}</div>
        <div className="market-card-mobile__price">
          <span>租金：{item.pricing.rent}</span>
          <span>押金：{item.pricing.deposit}</span>
          <strong>{item.pricing.total}</strong>
        </div>
        <div className="market-card-mobile__stats">
          <span>浏览 {item.stats.viewCount}</span>
          <span>收藏 {item.stats.favoriteCount}</span>
          <span>{item.regionLabel}</span>
        </div>
        <div className="market-card-mobile__tags">
          {item.assuranceTags.map((tag) => (
            <span className="market-assurance-tag" key={`mobile-${item.id}-${tag}`}>
              {tag}
            </span>
          ))}
        </div>
        <div className="market-card-mobile__chips">
          {getKeySummaryChips(item).map((chip) => (
            <span className={`summary-chip summary-chip--${chip.tone}`} key={`mobile-${item.id}-${chip.label}`}>
              {chip.label}
            </span>
          ))}
        </div>
      </div>
    </div>
  );
}

function ListingCard({ item, favorited, onFavorite, onShare, onCopyId, onOpen }: ListingCardProps) {
  const openByKeyboard = (event: KeyboardEvent<HTMLElement>) => {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      onOpen(item);
    }
  };

  return (
    <article className="market-row market-row--interactive" key={item.id} tabIndex={0} role="button" onClick={() => onOpen(item)} onKeyDown={openByKeyboard}>
      <div className={`market-row__thumb market-row__thumb--${item.coverTone}`}>
        {item.coverUrl ? (
          <img alt={item.title} className="market-row__cover-image" src={item.coverUrl} />
        ) : (
          <div className="market-row__thumb-layout">
            <div className="thumb-preview">
              <div className="thumb-preview__character" />
              <div className="thumb-preview__hud" />
            </div>
            <div className="thumb-grid" aria-hidden="true">
              <span className="thumb-slot" />
              <span className="thumb-slot" />
              <span className="thumb-slot thumb-slot--wide" />
              <span className="thumb-slot" />
              <span className="thumb-slot" />
            </div>
          </div>
        )}
        <div className="market-row__thumb-actions">
          <button
            aria-label={favorited ? "取消收藏" : "收藏"}
            className={`market-icon-button ${favorited ? "is-active" : ""}`}
            type="button"
            onClick={(event) => onFavorite(item, event)}
          >
            ♥
          </button>
          <button aria-label="快速分享" className="market-icon-button" type="button" onClick={(event) => onShare(item, event)}>
            ↗
          </button>
        </div>
      </div>

      <div className="market-row__body">
        <div className="market-row__head">
          <div className="market-row__seller">
            <span className={`seller-avatar seller-avatar--${item.coverTone}`}>{item.avatar}</span>
            <strong>{item.seller}</strong>
            <span className={`market-seller-tag market-seller-tag--${item.sellerType.toLowerCase()}`}>{item.sellerTypeLabel}</span>
            <div className="market-row__chips">
            {getKeySummaryChips(item).map((chip) => (
              <span className={`summary-chip summary-chip--${chip.tone}`} key={`${item.id}-${chip.label}`}>
                {chip.label}
              </span>
              ))}
            </div>
          </div>
          <div className="market-row__serial">
            <span>{item.id}</span>
            <button
              aria-label="复制编号"
              className="copy-icon"
              type="button"
              onClick={(event) => onCopyId(item, event)}
            >
              <span />
            </button>
          </div>
        </div>

        <div className="market-row__title">{item.title}</div>
        <div className="market-row__meta">
          <div className="market-row__highlights">
            {item.highlights.slice(0, 5).map((tag) => (
              <span className="market-highlight-tag" key={`${item.id}-${tag}`}>
                {tag}
              </span>
            ))}
          </div>
          <div className="market-row__assurances">
            {item.assuranceTags.map((tag) => (
              <span className="market-assurance-tag" key={`${item.id}-${tag}`}>
                {tag}
              </span>
            ))}
          </div>
        </div>
        <div className="market-row__stats">
          <span>浏览 {item.stats.viewCount}</span>
          <span>收藏 {item.stats.favoriteCount}</span>
          <span>销量 {item.stats.salesCount}</span>
          <span>{item.regionLabel}</span>
        </div>

        <div className="market-panels">
          {item.panels.map((panel, index) => (
            <section
              className={`market-panel market-panel--${panel.tone} ${panel.wide ? "market-panel--wide" : ""}`}
              key={`${item.id}-panel-${index}`}
            >
              <dl className="market-panel__list">
                {panel.entries.map((entry) => (
                  <div className="market-panel__item" key={`${item.id}-${entry.label}`}>
                    <dt>{entry.label}</dt>
                    <dd>{entry.value}</dd>
                  </div>
                ))}
              </dl>
            </section>
          ))}
          <aside className="market-price-card">
            <div className="market-price-card__line">
              <span>押金</span>
              <strong>{item.pricing.deposit}</strong>
            </div>
            <div className="market-price-card__line">
              <span>租金</span>
              <strong>{item.pricing.rent}</strong>
            </div>
            <div className="market-price-card__total">{item.pricing.total}</div>
          </aside>
        </div>
      </div>

      <ListingMobileCard item={item} favorited={favorited} onFavorite={onFavorite} onShare={onShare} />
    </article>
  );
}

function ListingCompactCard({ item, favorited, onFavorite, onShare, onCopyId, onOpen }: ListingCardProps) {
  const openByKeyboard = (event: KeyboardEvent<HTMLElement>) => {
    if (event.key === "Enter" || event.key === " ") {
      event.preventDefault();
      onOpen(item);
    }
  };

  const compactFacts = getCompactFacts(item);
  const compactPills = [
    item.accountLevelLabel,
    item.regionLabel.split("-")[0],
    item.negotiable ? "可议价" : "一口价",
    item.assuranceTags[0],
  ].filter(Boolean);

  return (
    <article
      className="market-compact-card market-row--interactive"
      key={item.id}
      tabIndex={0}
      role="button"
      onClick={() => onOpen(item)}
      onKeyDown={openByKeyboard}
    >
      <div className={`market-compact-card__media market-row__thumb--${item.coverTone}`}>
        {item.coverUrl ? (
          <img alt={item.title} className="market-row__cover-image" src={item.coverUrl} />
        ) : (
          <div className="market-row__thumb-layout">
            <div className="thumb-preview">
              <div className="thumb-preview__character" />
              <div className="thumb-preview__hud" />
            </div>
            <div className="thumb-grid" aria-hidden="true">
              <span className="thumb-slot" />
              <span className="thumb-slot" />
              <span className="thumb-slot thumb-slot--wide" />
              <span className="thumb-slot" />
              <span className="thumb-slot" />
            </div>
          </div>
        )}
        <div className="market-compact-card__media-actions">
          <button
            aria-label={favorited ? "取消收藏" : "收藏"}
            className={`market-icon-button ${favorited ? "is-active" : ""}`}
            type="button"
            onClick={(event) => onFavorite(item, event)}
          >
            ♥
          </button>
          <button aria-label="快速分享" className="market-icon-button" type="button" onClick={(event) => onShare(item, event)}>
            ↗
          </button>
        </div>
      </div>

      <div className="market-compact-card__main">
        <div className="market-compact-card__top">
          <div className="market-compact-card__seller">
            <span className={`seller-avatar seller-avatar--${item.coverTone}`}>{item.avatar}</span>
            <strong>{item.seller}</strong>
            <span className={`market-seller-tag market-seller-tag--${item.sellerType.toLowerCase()}`}>{item.sellerTypeLabel}</span>
          </div>
          <div className="market-compact-card__meta">
            <span>{item.id}</span>
            <button
              aria-label="复制编号"
              className="copy-icon"
              type="button"
              onClick={(event) => onCopyId(item, event)}
            >
              <span />
            </button>
          </div>
        </div>
        <div className="market-compact-card__overview">{getCompactOverview(item)}</div>
        <div className="market-compact-card__facts">
          {compactFacts.map((fact) => (
            <span className={`market-compact-card__fact ${fact.emphasis ? "is-emphasis" : ""}`} key={`${item.id}-${fact.label}`}>
              {fact.label}:{fact.value}
            </span>
          ))}
        </div>
        <div className="market-compact-card__tags">
          {item.highlights.slice(0, 6).map((tag) => (
            <span className="market-compact-card__tag" key={`compact-${item.id}-${tag}`}>
              {tag}
            </span>
          ))}
        </div>
        <div className="market-compact-card__bottom">
          <div className="market-compact-card__badges">
            {compactPills.map((pill) => (
              <span className="market-compact-card__pill" key={`compact-pill-${item.id}-${pill}`}>
                {pill}
              </span>
            ))}
            {getKeySummaryChips(item, 5).map((chip) => (
              <span
                className={`market-compact-card__pill ${chip.tone === "sale" ? "market-compact-card__pill--accent" : ""}`}
                key={`compact-chip-${item.id}-${chip.label}`}
              >
                {chip.label}
              </span>
            ))}
          </div>
          <div className="market-compact-card__price">
            <strong>{item.pricing.total}</strong>
          </div>
        </div>
      </div>

      <ListingMobileCard item={item} favorited={favorited} onFavorite={onFavorite} onShare={onShare} />
    </article>
  );
}

type ListingDetailModalProps = {
  item: ListingRow | null;
  favorited: boolean;
  buyingChannel: "" | "WECHAT" | "ALIPAY";
  loading: boolean;
  error: string;
  onClose: () => void;
  onFavorite: () => void;
  onShare: () => void;
  onBuyWechat: (includeExtraItems: boolean) => void;
  onBuyAlipay: (includeExtraItems: boolean) => void;
};

function ListingAgreementDialog({
  agreement,
  onClose,
}: {
  agreement: ListingAgreementContent | null;
  onClose: () => void;
}) {
  useEffect(() => {
    if (!agreement) {
      return;
    }

    const previousOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    function handleKeyDown(event: globalThis.KeyboardEvent) {
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
    <div aria-labelledby="listing-agreement-title" aria-modal="true" className="publish-agreement-dialog" role="dialog">
      <button aria-label="关闭协议弹层" className="publish-agreement-dialog__backdrop" type="button" onClick={onClose} />
      <div className="publish-agreement-dialog__panel">
        <div className="publish-agreement-dialog__header">
          <div>
            <h3 id="listing-agreement-title">{agreement.title}</h3>
            <p>下单前请完整阅读协议内容，勾选后才可继续支付。</p>
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

function ListingDetailModal({
  item,
  favorited,
  buyingChannel,
  loading,
  error,
  onClose,
  onFavorite,
  onShare,
  onBuyWechat,
  onBuyAlipay,
}: ListingDetailModalProps) {
  const mediaAssets = useMemo(() => {
    const items: Array<{ type: "image" | "video"; url: string; label: string }> = [];
    if (!item) {
      return items;
    }
    (item.imageUrls ?? []).filter(Boolean).forEach((url, index) => {
      items.push({
        type: "image",
        url,
        label: `截图 ${index + 1}`,
      });
    });
    if (item.videoUrl) {
      items.push({
        type: "video",
        url: item.videoUrl,
        label: "短视频",
      });
    }
    return items;
  }, [item]);
  const [activeMediaIndex, setActiveMediaIndex] = useState(0);
  const [previewOpen, setPreviewOpen] = useState(false);
  const [acceptedAgreements, setAcceptedAgreements] = useState<string[]>([]);
  const [activeAgreement, setActiveAgreement] = useState<ListingAgreementContent | null>(null);
  const [includeExtraItems, setIncludeExtraItems] = useState(false);
  const modalBodyRef = useRef<HTMLDivElement | null>(null);
  const detailContentRef = useRef<HTMLElement | null>(null);
  const pendingAgreementScrollRef = useRef<{
    modalScrollTop: number;
    contentScrollTop: number;
    windowScrollY: number;
  } | null>(null);
  const imageMediaIndexes = useMemo(
    () => mediaAssets.map((asset, index) => (asset.type === "image" ? index : -1)).filter((index) => index >= 0),
    [mediaAssets],
  );
  const activeMedia = mediaAssets[activeMediaIndex] ?? null;
  const extraItems = item?.extraItems ?? [];
  const extraItemsAmount = item ? parsePricingAmount(item.pricing.extraItemsAmount) : 0;
  const detailTotalAmount = item
    ? parsePricingAmount(item.pricing.total) + (includeExtraItems ? extraItemsAmount : 0)
    : 0;
  const detailTotalLabel = formatYuanAmount(detailTotalAmount);
  const detailSummaryFacts = item
    ? [
        { label: "地区", value: item.regionLabel },
        { label: "账号等级", value: item.accountLevelLabel },
        { label: "哈夫币", value: item.hafCurrencyLabel },
        { label: "发布比例", value: item.exchangeRateLabel },
        { label: "发布时间", value: item.publishedAtLabel },
        { label: "交易方式", value: item.negotiable ? "支持议价" : "一口价" },
      ].filter((entry) => Boolean(entry.value))
    : [];
  const importantFacts = item ? getImportantFacts(item) : [];

  useEffect(() => {
    setActiveMediaIndex(0);
    setPreviewOpen(false);
    setAcceptedAgreements([]);
    setActiveAgreement(null);
    setIncludeExtraItems(false);
  }, [item?.id]);

  const agreementsAccepted = LISTING_REQUIRED_AGREEMENTS.every((agreement) => acceptedAgreements.includes(agreement.value));

  const restorePendingAgreementScroll = useCallback(() => {
    const pendingScroll = pendingAgreementScrollRef.current;
    if (!pendingScroll) {
      return;
    }
    if (modalBodyRef.current) {
      modalBodyRef.current.scrollTop = pendingScroll.modalScrollTop;
    }
    if (detailContentRef.current) {
      detailContentRef.current.scrollTop = pendingScroll.contentScrollTop;
    }
    window.scrollTo({ top: pendingScroll.windowScrollY });
  }, []);

  useLayoutEffect(() => {
    restorePendingAgreementScroll();
  }, [acceptedAgreements, restorePendingAgreementScroll]);

  const moveMedia = (delta: number) => {
    if (!mediaAssets.length) {
      return;
    }
    setActiveMediaIndex((current) => (current + delta + mediaAssets.length) % mediaAssets.length);
  };

  const movePreviewImage = useCallback(
    (delta: number) => {
      if (!imageMediaIndexes.length) {
        return;
      }
      setActiveMediaIndex((current) => {
        const currentPosition = imageMediaIndexes.indexOf(current);
        const safePosition = currentPosition >= 0 ? currentPosition : 0;
        const nextPosition = (safePosition + delta + imageMediaIndexes.length) % imageMediaIndexes.length;
        return imageMediaIndexes[nextPosition];
      });
    },
    [imageMediaIndexes],
  );

  useEffect(() => {
    if (!previewOpen || activeMedia?.type !== "image") {
      return;
    }

    function handleKeyDown(event: globalThis.KeyboardEvent) {
      if (event.key === "Escape") {
        event.preventDefault();
        setPreviewOpen(false);
        return;
      }
      if (event.key === "ArrowLeft") {
        event.preventDefault();
        movePreviewImage(-1);
        return;
      }
      if (event.key === "ArrowRight") {
        event.preventDefault();
        movePreviewImage(1);
      }
    }

    window.addEventListener("keydown", handleKeyDown);
    return () => {
      window.removeEventListener("keydown", handleKeyDown);
    };
  }, [activeMedia?.type, movePreviewImage, previewOpen]);

  const toggleAgreement = (value: string) => {
    pendingAgreementScrollRef.current = {
      modalScrollTop: modalBodyRef.current?.scrollTop ?? 0,
      contentScrollTop: detailContentRef.current?.scrollTop ?? 0,
      windowScrollY: window.scrollY,
    };

    if (document.activeElement instanceof HTMLElement) {
      document.activeElement.blur();
    }

    setAcceptedAgreements((current) =>
      current.includes(value) ? current.filter((entry) => entry !== value) : [...current, value],
    );

    window.requestAnimationFrame(() => {
      restorePendingAgreementScroll();
      window.requestAnimationFrame(() => {
        restorePendingAgreementScroll();
        window.setTimeout(() => {
          restorePendingAgreementScroll();
          pendingAgreementScrollRef.current = null;
        }, 80);
      });
    });
  };

  return (
    <div className="listing-detail-modal" role="dialog" aria-modal="true" aria-label="账号详情">
      <button className="listing-detail-modal__backdrop" type="button" aria-label="关闭详情" onClick={onClose} />
      <section className="listing-detail-modal__dialog">
        <div className="listing-detail-modal__toolbar">
          <strong>账号详情</strong>
          <button className="listing-detail-modal__close" type="button" onClick={onClose}>
            关闭
          </button>
        </div>

        {loading ? (
          <div className="listing-detail-modal__state">
            <StatusState title="详情正在加载" description="正在拉取最新账号详情、图片与统计信息，请稍候。" />
          </div>
        ) : null}

        {!loading && error ? (
          <div className="listing-detail-modal__state">
            <StatusState title="详情加载失败" description={error} tone="error" action={<Button onClick={onClose}>返回大厅</Button>} />
          </div>
        ) : null}

        {!loading && !error && item ? (
          <div className="listing-detail-modal__body" ref={modalBodyRef}>
            <div className="listing-detail">
              <aside className="listing-detail__gallery">
                <div className="listing-detail__cover">
                  {activeMedia?.type === "video" ? (
                    <video className="listing-detail__video" controls src={activeMedia.url} />
                  ) : activeMedia?.url ? (
                    <button className="listing-detail__cover-button" type="button" onClick={() => setPreviewOpen(true)}>
                      <img alt={item.title} src={activeMedia.url} />
                    </button>
                  ) : item.coverUrl ? (
                    <button className="listing-detail__cover-button" type="button" onClick={() => setPreviewOpen(true)}>
                      <img alt={item.title} src={item.coverUrl} />
                    </button>
                  ) : (
                    <div className={`market-row__thumb market-row__thumb--${item.coverTone}`} />
                  )}
                  {mediaAssets.length > 1 ? (
                    <>
                      <button
                        aria-label="查看上一张"
                        className="listing-detail__gallery-nav listing-detail__gallery-nav--prev"
                        type="button"
                        onClick={() => moveMedia(-1)}
                      >
                        ‹
                      </button>
                      <button
                        aria-label="查看下一张"
                        className="listing-detail__gallery-nav listing-detail__gallery-nav--next"
                        type="button"
                        onClick={() => moveMedia(1)}
                      >
                        ›
                      </button>
                    </>
                  ) : null}
                </div>
                {mediaAssets.length ? (
                  <div className="listing-detail__thumbs">
                    {mediaAssets.map((asset, index) => (
                      <button
                        className={`listing-detail__thumb ${index === activeMediaIndex ? "is-active" : ""}`}
                        key={`${item.id}-${asset.label}-${index}`}
                        type="button"
                        onClick={() => setActiveMediaIndex(index)}
                      >
                        {asset.type === "video" ? <span className="listing-detail__thumb-video">视频</span> : <img alt={asset.label} src={asset.url} />}
                      </button>
                    ))}
                  </div>
                ) : null}
                <div className="listing-detail__gallery-note">
                  最多展示 10 张截图与 1 个短视频，支持放大预览与左右切换。
                </div>
              </aside>

              <section className="listing-detail__content" ref={detailContentRef}>
                <section className="listing-detail__hero">
                  <div className="listing-detail__hero-main">
                    <div className="listing-detail__eyebrow">账号编号 {item.id}</div>
                    <div className="listing-detail__head">
                      <div>
                        <h1>{item.title}</h1>
                        <div className="listing-detail__seller">
                          <span>{item.seller}</span>
                          <span className={`market-seller-tag market-seller-tag--${item.sellerType.toLowerCase()}`}>
                            {item.sellerTypeLabel}
                          </span>
                          <span>{item.publishedAtLabel}</span>
                        </div>
                      </div>
                      <div className="listing-detail__actions">
                        <button className={`market-icon-button ${favorited ? "is-active" : ""}`} type="button" onClick={onFavorite}>
                          ♥ {item.stats.favoriteCount}
                        </button>
                        <button className="market-icon-button" type="button" onClick={onShare}>
                          ↗ 分享
                        </button>
                      </div>
                    </div>

                    <div className="listing-detail__stats">
                      <span>浏览 {item.stats.viewCount}</span>
                      <span>收藏 {item.stats.favoriteCount}</span>
                      <span>销量 {item.stats.salesCount}</span>
                      <span>{item.regionLabel}</span>
                    </div>

                    <div className="listing-detail__summary-grid">
                      {detailSummaryFacts.map((entry) => (
                        <div className="listing-detail__summary-item" key={`${item.id}-${entry.label}`}>
                          <span>{entry.label}</span>
                          <strong>{entry.value}</strong>
                        </div>
                      ))}
                    </div>

                    {importantFacts.length ? (
                      <div className="listing-detail__important-facts">
                        {importantFacts.map((fact) => (
                          <div className={`listing-detail__important-fact ${fact.emphasis ? "is-emphasis" : ""}`} key={`${item.id}-important-${fact.label}`}>
                            <span>{fact.label}</span>
                            <strong>{fact.value}</strong>
                          </div>
                        ))}
                      </div>
                    ) : null}

                    {getKeySummaryChips(item).length ? (
                      <div className="listing-detail__chips">
                        {getKeySummaryChips(item).map((chip) => (
                          <span className={`summary-chip summary-chip--${chip.tone}`} key={`${item.id}-${chip.label}`}>
                            {chip.label}
                          </span>
                        ))}
                      </div>
                    ) : null}

                    {item.assuranceTags.length ? (
                      <div className="listing-detail__assurances">
                        {item.assuranceTags.map((tag) => (
                          <span className="market-assurance-tag" key={`${item.id}-${tag}`}>
                            {tag}
                          </span>
                        ))}
                      </div>
                    ) : null}

                    {item.highlights.length ? (
                      <div className="listing-detail__highlights">
                        {item.highlights.map((tag) => (
                          <span className="market-highlight-tag" key={`${item.id}-${tag}`}>
                            {tag}
                          </span>
                        ))}
                      </div>
                    ) : null}

                    {extraItems.length ? (
                      <section className="listing-detail-extra-items">
                        <div className="listing-detail-extra-items__head">
                          <h2>额外物资</h2>
                          <span>勾选后随订单一并支付</span>
                        </div>
                        <div className="listing-detail-extra-items__grid">
                          {extraItems.map((extraItem) => (
                            <div className="listing-detail-extra-item" key={`${item.id}-${extraItem.label}`}>
                              <strong>{extraItem.label}</strong>
                              <span>{extraItem.count} 件 / 单价 {extraItem.unitPrice}</span>
                              <em>{extraItem.subtotal}</em>
                            </div>
                          ))}
                        </div>
                      </section>
                    ) : null}
                  </div>

                  <aside className="market-price-card market-price-card--detail">
                    <div className="listing-detail-price-card__eyebrow">立即交易</div>
                    <div className="market-price-card__line">
                      <span>押金</span>
                      <strong>{item.pricing.deposit}</strong>
                    </div>
                    <div className="market-price-card__line">
                      <span>租金</span>
                      <strong>{item.pricing.rent}</strong>
                    </div>
                    <div className="market-price-card__line">
                      <span>额外物资</span>
                      <strong>{item.pricing.extraItemsAmount}</strong>
                    </div>
                    {extraItems.length ? (
                      <button
                        aria-pressed={includeExtraItems}
                        className={`listing-detail-extra-toggle ${includeExtraItems ? "is-active" : ""}`}
                        type="button"
                        onClick={() => setIncludeExtraItems((current) => !current)}
                      >
                        <span className="listing-detail-extra-toggle__check">{includeExtraItems ? "✓" : ""}</span>
                        <span>包含额外物资一并支付</span>
                      </button>
                    ) : null}
                    <div className="market-price-card__total">{detailTotalLabel}</div>
                    <div className="listing-detail-price-actions">
                      <button
                        className="listing-detail-primary-action"
                        disabled={Boolean(buyingChannel) || !agreementsAccepted}
                        type="button"
                        onClick={() => onBuyWechat(includeExtraItems)}
                      >
                        {buyingChannel === "WECHAT" ? "创建微信订单中..." : "微信支付"}
                      </button>
                      <button
                        className="listing-detail-secondary-action"
                        disabled={Boolean(buyingChannel) || !agreementsAccepted}
                        type="button"
                        onClick={() => onBuyAlipay(includeExtraItems)}
                      >
                        {buyingChannel === "ALIPAY" ? "创建支付宝订单中..." : "支付宝支付"}
                      </button>
                      <button className="listing-detail-secondary-action" type="button" onClick={onFavorite}>
                        {favorited ? "取消收藏" : "加入收藏"}
                      </button>
                    </div>
                    <div className="listing-detail-agreements">
                      <div className="listing-detail-agreements__header">
                        <strong>下单前需同意协议</strong>
                        <span>{agreementsAccepted ? "已解锁下单" : "请先勾选全部协议"}</span>
                      </div>
                      <div className="listing-detail-agreements__list">
                        {LISTING_REQUIRED_AGREEMENTS.map((agreement) => {
                          const checked = acceptedAgreements.includes(agreement.value);
                          const content = getListingAgreementContent(agreement.value);
                          return (
                            <div className={`publish-agreement-row listing-detail-agreements__row ${checked ? "is-active" : ""}`} key={agreement.value}>
                              <button
                                aria-pressed={checked}
                                className={`publish-agreement ${checked ? "is-active" : ""}`}
                                type="button"
                                onMouseDown={(event) => event.preventDefault()}
                                onTouchStart={(event) => event.currentTarget.blur()}
                                onClick={() => toggleAgreement(agreement.value)}
                              >
                                <span className="publish-agreement__check">{checked ? "✓" : ""}</span>
                                <span>我已阅读并同意</span>
                              </button>
                              {content ? (
                                <button className="publish-agreement__link" type="button" onClick={() => setActiveAgreement(content)}>
                                  {agreement.label}
                                </button>
                              ) : (
                                <span className="publish-agreement__text">{agreement.label}</span>
                              )}
                            </div>
                          );
                        })}
                      </div>
                      <p className="listing-detail-agreements__hint">未勾选协议前，支付按钮将保持不可用。</p>
                    </div>
                  </aside>
                </section>

                <div className="listing-detail__top-grid">
                  <section className="listing-detail-card listing-detail-card--important">
                    <div className="listing-detail-card__head">
                      <h2>重点信息</h2>
                      <span>下单前先看</span>
                    </div>
                    <div className="listing-detail-card__important-list">
                      {importantFacts.map((fact) => (
                        <div className={`listing-detail-card__important-item ${fact.emphasis ? "is-emphasis" : ""}`} key={`${item.id}-top-important-${fact.label}`}>
                          <span>{fact.label}</span>
                          <strong>{fact.value}</strong>
                        </div>
                      ))}
                    </div>
                  </section>

                  <section className="listing-detail-card listing-detail-card--seller">
                    <div className="listing-detail-card__head">
                      <h2>卖家信息</h2>
                      <span>{item.sellerInfo.sellerTypeLabel}</span>
                    </div>
                    <div className="listing-detail-seller">
                      <span className={`seller-avatar seller-avatar--${item.coverTone}`}>{item.sellerInfo.avatarText}</span>
                      <div>
                        <strong>{item.sellerInfo.nickname}</strong>
                        <p>{item.sellerInfo.studioName || "个人卖家"}</p>
                      </div>
                    </div>
                    <div className="listing-detail-seller__stats">
                      <div>
                        <span>好评率</span>
                        <strong>{item.sellerInfo.favorableRate}</strong>
                      </div>
                      <div>
                        <span>成交笔数</span>
                        <strong>{item.sellerInfo.dealCount}</strong>
                      </div>
                      <div>
                        <span>发布账号</span>
                        <strong>{item.sellerInfo.publishCount}</strong>
                      </div>
                    </div>
                  </section>

                  <section className="listing-detail-card listing-detail-card--guarantee">
                    <div className="listing-detail-card__head">
                      <h2>保障说明</h2>
                    </div>
                    <dl className="listing-detail-facts listing-detail-facts--compact">
                      <div>
                        <dt>审核标识</dt>
                        <dd>{item.guaranteeInfo.platformReview}</dd>
                      </div>
                      <div>
                        <dt>售后保障</dt>
                        <dd>{item.guaranteeInfo.afterSalePeriod}</dd>
                      </div>
                      <div>
                        <dt>违规处理</dt>
                        <dd>{item.guaranteeInfo.violationPolicy}</dd>
                      </div>
                    </dl>
                  </section>
                </div>

                <section className="listing-detail-section">
                  <div className="listing-detail-section__head">
                    <strong>核心数据</strong>
                    <span>账号、装备与资产面板</span>
                  </div>
                  <div className="listing-detail__panels">
                    {item.panels.map((panel, index) => (
                      <section className={`market-panel market-panel--${panel.tone}`} key={`${item.id}-detail-${index}`}>
                        <dl className="market-panel__list">
                          {panel.entries.map((entry) => (
                            <div className="market-panel__item" key={`${item.id}-${entry.label}`}>
                              <dt>{entry.label}</dt>
                              <dd>{entry.value}</dd>
                            </div>
                          ))}
                        </dl>
                      </section>
                    ))}
                  </div>
                </section>

                <section className="listing-detail-section">
                  <div className="listing-detail-section__head">
                    <strong>详细资料</strong>
                    <span>基础信息、资产信息与战备信息</span>
                  </div>
                  <div className="listing-detail__sections">
                    {[item.baseInfoSection, item.assetInfoSection, item.combatInfoSection].map((section) => (
                      <section className="listing-detail-card listing-detail-card--facts" key={`${item.id}-${section.title}`}>
                        <div className="listing-detail-card__head">
                          <h2>{section.title}</h2>
                        </div>
                        <dl className="listing-detail-facts listing-detail-facts--detail">
                          {section.entries.map((entry) => (
                            <div key={`${item.id}-${section.title}-${entry.label}`}>
                              <dt>{entry.label}</dt>
                              <dd>{entry.value}</dd>
                            </div>
                          ))}
                        </dl>
                      </section>
                    ))}
                  </div>
                </section>

                {item.equipmentGroups.length ? (
                  <section className="listing-detail-section">
                    <div className="listing-detail-section__head">
                      <strong>装备标签</strong>
                      <span>按类别整理的关键资产说明</span>
                    </div>
                    <div className="listing-detail__sections">
                      {item.equipmentGroups.map((group) => (
                        <section className="listing-detail-card" key={`${item.id}-${group.title}`}>
                          <div className="listing-detail-card__head">
                            <h2>{group.title}</h2>
                          </div>
                          <div className="listing-detail-tags">
                            {group.items.map((entry) => (
                              <span className="listing-detail-tag" key={`${item.id}-${group.title}-${entry.name}`}>
                                <strong>{entry.name}</strong>
                                <em>{entry.note}</em>
                              </span>
                            ))}
                          </div>
                        </section>
                      ))}
                    </div>
                  </section>
                ) : null}

                <section className="listing-detail-card listing-detail-card--description">
                  <div className="listing-detail-card__head">
                    <h2>账号说明</h2>
                  </div>
                  <p>{item.description || "卖家暂未补充更多说明。"}</p>
                </section>
              </section>
            </div>

            {previewOpen && activeMedia && activeMedia.type === "image" ? (
              <div className="listing-detail-lightbox" role="dialog" aria-modal="true" aria-label="大图预览">
                <button className="listing-detail-lightbox__backdrop" type="button" aria-label="关闭预览" onClick={() => setPreviewOpen(false)} />
                <div className="listing-detail-lightbox__dialog">
                  <button className="listing-detail-lightbox__close" type="button" onClick={() => setPreviewOpen(false)}>
                    关闭
                  </button>
                  <img alt={item.title} src={activeMedia.url} />
                  {imageMediaIndexes.length > 1 ? (
                    <>
                      <button className="listing-detail-lightbox__nav listing-detail-lightbox__nav--prev" type="button" onClick={() => movePreviewImage(-1)}>
                        ‹
                      </button>
                      <button className="listing-detail-lightbox__nav listing-detail-lightbox__nav--next" type="button" onClick={() => movePreviewImage(1)}>
                        ›
                      </button>
                    </>
                  ) : null}
                </div>
              </div>
            ) : null}
          </div>
        ) : null}
      </section>
      <ListingAgreementDialog agreement={activeAgreement} onClose={() => setActiveAgreement(null)} />
    </div>
  );
}

function toggleValue(source: string[], value: string) {
  if (source.includes(value)) {
    return source.filter((item) => item !== value);
  }
  return [...source, value];
}

export function ListingMarket({ marketType }: ListingMarketProps) {
  const navigate = useNavigate();
  const location = useLocation();
  const { isAuthenticated, openAuthModal } = useAuth();
  const [desktopDisplayMode, setDesktopDisplayMode] = useState<"default" | "list">("default");
  const [meta, setMeta] = useState<MarketplaceMeta | null>(null);
  const [draft, setDraft] = useState<MarketplaceQuery>(() => cloneQuery(DEFAULT_MARKET_QUERY));
  const [query, setQuery] = useState<MarketplaceQuery>(() => cloneQuery(DEFAULT_MARKET_QUERY));
  const [rows, setRows] = useState<ListingRow[]>([]);
  const [total, setTotal] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [favoriteIds, setFavoriteIds] = useState<string[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState("");
  const [loadMoreError, setLoadMoreError] = useState("");
  const [notice, setNotice] = useState("");
  const [mobileFilterOpen, setMobileFilterOpen] = useState(false);
  const [exportingListings, setExportingListings] = useState(false);
  const [buyingChannel, setBuyingChannel] = useState<"" | "WECHAT" | "ALIPAY">("");
  const [tradePayPayload, setTradePayPayload] = useState<WechatPayDialogPayload | null>(null);
  const loadMoreRef = useRef<HTMLDivElement | null>(null);
  const loadingMoreLockRef = useRef(false);
  const [requestNonce, setRequestNonce] = useState(0);

  const queryKey = useMemo(() => JSON.stringify({ marketType, query, requestNonce }), [marketType, query, requestNonce]);
  const marketPath = marketType === "trade" ? "/trade" : "/";
  const activeListingNo = useMemo(() => new URLSearchParams(location.search).get("listing") ?? "", [location.search]);
  const activeListing = useMemo(() => rows.find((item) => item.id === activeListingNo) ?? null, [activeListingNo, rows]);
  const detailFavorited = activeListingNo ? favoriteIds.includes(activeListingNo) : false;

  useEffect(() => {
    if (!notice) {
      return;
    }
    const timer = window.setTimeout(() => setNotice(""), 2000);
    return () => window.clearTimeout(timer);
  }, [notice]);

  useEffect(() => {
    let disposed = false;
    async function bootstrap() {
      try {
        const result = await loadMarketplaceMeta();
        if (!disposed) {
          setMeta(result);
        }
      } catch (nextError) {
        if (!disposed) {
          setError(nextError instanceof Error ? nextError.message : "筛选配置加载失败");
          setLoading(false);
        }
      }
    }
    void bootstrap();
    return () => {
      disposed = true;
    };
  }, []);

  useEffect(() => {
    if (!meta) {
      return;
    }
    let disposed = false;
    async function fetchRows() {
      const firstPage = query.page <= 1;
      try {
        if (firstPage) {
          setLoading(true);
          setError("");
        } else {
          setLoadingMore(true);
          setLoadMoreError("");
        }
        const result = await loadMarketplaceListings(query);
        if (!disposed) {
          setRows((current) => (result.page <= 1 ? result.rows : appendListingRows(current, result.rows)));
          setTotal(result.total);
          setHasMore(result.hasMore);
          setLoadMoreError("");
        }
      } catch (nextError) {
        if (!disposed) {
          const message = nextError instanceof Error ? nextError.message : "资源列表加载失败";
          if (firstPage) {
            setError(message);
          } else {
            setLoadMoreError(message);
          }
        }
      } finally {
        if (!disposed) {
          if (firstPage) {
            setLoading(false);
          } else {
            setLoadingMore(false);
          }
        }
      }
    }
    void fetchRows();
    return () => {
      disposed = true;
    };
  }, [meta, queryKey]);

  useEffect(() => {
    if (!loadingMore) {
      loadingMoreLockRef.current = false;
    }
  }, [loadingMore]);

  useEffect(() => {
    if (!meta || loading || loadingMore || !hasMore || !!error || !rows.length) {
      return;
    }
    const node = loadMoreRef.current;
    if (!node) {
      return;
    }
    const observer = new IntersectionObserver(
      (entries) => {
        const entry = entries[0];
        if (!entry?.isIntersecting || loadingMoreLockRef.current) {
          return;
        }
        loadingMoreLockRef.current = true;
        setQuery((current) => ({
          ...current,
          page: current.page + 1,
        }));
      },
      {
        rootMargin: "240px 0px",
      },
    );
    observer.observe(node);
    return () => {
      observer.disconnect();
    };
  }, [meta, loading, loadingMore, hasMore, error, rows.length]);

  useEffect(() => {
    if (!isAuthenticated || !rows.length) {
      setFavoriteIds([]);
      return;
    }
    let disposed = false;
    async function fetchFavorites() {
      try {
        const result = await loadFavoriteListingNos(rows.map((item) => item.id));
        if (!disposed) {
          setFavoriteIds(result);
        }
      } catch {
        if (!disposed) {
          setFavoriteIds([]);
        }
      }
    }
    void fetchFavorites();
    return () => {
      disposed = true;
    };
  }, [isAuthenticated, rows]);

  useEffect(() => {
    if (!activeListingNo) {
      return;
    }
    const originalOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => {
      document.body.style.overflow = originalOverflow;
    };
  }, [activeListingNo]);

  const applyFilters = () => {
    setQuery(resetQueryPagination(draft));
    setMobileFilterOpen(false);
  };

  const resetFilters = () => {
    const empty = cloneQuery(DEFAULT_MARKET_QUERY);
    setDraft(empty);
    setQuery(resetQueryPagination(empty));
    setMobileFilterOpen(false);
  };

  const isPresetActive = (preset: DesktopPresetKey) => {
    if (preset === "list") {
      return desktopDisplayMode === "list";
    }
    if (preset === "budget") {
      return query.maxHafCurrency === "100";
    }
    if (preset === "studio") {
      return query.sellerType === "STUDIO";
    }
    if (preset === "promotion") {
      return query.exchangeRateType === "accelerated";
    }
    if (preset === "recent") {
      return query.publishedWithinDays === "7";
    }
    return query.alwaysOnline === "true";
  };

  const applyPreset = (preset: DesktopPresetKey) => {
    if (!meta) {
      return;
    }
    if (preset === "list") {
      setDesktopDisplayMode((current) => (current === "default" ? "list" : "default"));
      return;
    }
    if (preset === "budget") {
      const next = { ...draft, maxHafCurrency: isPresetActive(preset) ? "" : "100" };
      setDraft(next);
      setQuery(resetQueryPagination(next));
      return;
    }
    if (preset === "studio") {
      const next = { ...draft, sellerType: isPresetActive(preset) ? "" : "STUDIO" };
      setDraft(next);
      setQuery(resetQueryPagination(next));
      return;
    }
    if (preset === "promotion") {
      const next = { ...draft, exchangeRateType: isPresetActive(preset) ? "" : "accelerated" };
      setDraft(next);
      setQuery(resetQueryPagination(next));
      return;
    }
    if (preset === "recent") {
      const next = { ...draft, publishedWithinDays: isPresetActive(preset) ? "" : "7" };
      setDraft(next);
      setQuery(resetQueryPagination(next));
      return;
    }
    if (preset === "always-online") {
      const next = { ...draft, alwaysOnline: isPresetActive(preset) ? "" as const : "true" as const };
      setDraft(next);
      setQuery(resetQueryPagination(next));
    }
  };

  const applyFavoriteResult = (listingNo: string, result: { favorite: boolean; favoriteCount: number }) => {
    setFavoriteIds((current) =>
      result.favorite ? Array.from(new Set([...current, listingNo])) : current.filter((value) => value !== listingNo),
    );
    setRows((current) =>
      current.map((row) =>
        row.id === listingNo
          ? {
              ...row,
              stats: {
                ...row.stats,
                favoriteCount: result.favoriteCount,
              },
            }
          : row,
      ),
    );
  };

  const updateListingSearch = (listingNo: string) => {
    const params = new URLSearchParams(location.search);
    if (listingNo) {
      params.set("listing", listingNo);
    } else {
      params.delete("listing");
    }
    navigate(
      {
        pathname: location.pathname,
        search: params.toString() ? `?${params.toString()}` : "",
        hash: location.hash,
      },
      { replace: false },
    );
  };

  const openListing = (item: ListingRow) => {
    updateListingSearch(item.id);
  };

  const closeListingDetail = () => {
    updateListingSearch("");
  };

  const handleFavorite = async (item: ListingRow, event: MouseEvent<HTMLButtonElement>) => {
    event.stopPropagation();
    if (!isAuthenticated) {
      openAuthModal("login");
      return;
    }
    const favorited = favoriteIds.includes(item.id);
    try {
      const result = favorited ? await unfavoriteListing(item.id) : await favoriteListing(item.id);
      applyFavoriteResult(item.id, result);
    } catch (nextError) {
      setNotice(nextError instanceof Error ? nextError.message : "收藏操作失败");
    }
  };

  const buildShareUrl = (listingNo: string) => `${window.location.origin}${marketPath}?listing=${listingNo}`;

  const handleShare = async (item: ListingRow, event: MouseEvent<HTMLButtonElement>) => {
    event.stopPropagation();
    const shareUrl = buildShareUrl(item.id);
    try {
      if (navigator.share) {
        await navigator.share({
          title: item.title,
          text: `${item.title} · ${item.pricing.total}`,
          url: shareUrl,
        });
        return;
      }
      if (navigator.clipboard) {
        await navigator.clipboard.writeText(shareUrl);
      }
      setNotice("链接已复制，请粘贴到微信或 QQ 进行分享。");
    } catch {
      // 用户主动取消分享时不提示错误。
    }
  };

  const handleCopyListingId = async (item: ListingRow, event: MouseEvent<HTMLButtonElement>) => {
    event.preventDefault();
    event.stopPropagation();
    try {
      await copyText(item.id);
      setNotice(`已复制编号 ${item.id}`);
    } catch {
      setNotice("复制失败，请手动选择编号复制。");
    }
  };

  const handleExportListings = async () => {
    if (exportingListings) {
      return;
    }
    try {
      setExportingListings(true);
      const exportRows: ListingRow[] = [];
      let page = 1;
      let hasNext = true;
      while (hasNext && page <= 200) {
        const result = await loadMarketplaceListings({
          ...cloneQuery(query),
          page,
          pageSize: 50,
        });
        exportRows.push(...result.rows);
        hasNext = result.hasMore;
        page += 1;
      }
      if (!exportRows.length) {
        setNotice("当前筛选下没有可导出的已上架账号。");
        return;
      }
      downloadListingsExcel(exportRows);
      setNotice(`已导出 ${exportRows.length} 个已上架账号。`);
    } catch (nextError) {
      setNotice(nextError instanceof Error ? nextError.message : "导出失败，请稍后再试");
    } finally {
      setExportingListings(false);
    }
  };

  const handleDetailFavorite = async () => {
    if (!activeListing) {
      return;
    }
    if (!isAuthenticated) {
      openAuthModal("login");
      return;
    }
    try {
      const result = detailFavorited ? await unfavoriteListing(activeListing.id) : await favoriteListing(activeListing.id);
      applyFavoriteResult(activeListing.id, result);
    } catch (nextError) {
      setNotice(nextError instanceof Error ? nextError.message : "收藏失败");
    }
  };

  const handleDetailShare = async () => {
    if (!activeListing) {
      return;
    }
    const shareUrl = buildShareUrl(activeListing.id);
    try {
      if (navigator.share) {
        await navigator.share({
          title: activeListing.title,
          text: `${activeListing.title} · ${activeListing.pricing.total}`,
          url: shareUrl,
        });
        return;
      }
      if (navigator.clipboard) {
        await navigator.clipboard.writeText(shareUrl);
      }
      setNotice("详情链接已复制，请粘贴到微信或 QQ 进行分享。");
    } catch {
      // 用户主动取消分享时不提示错误。
    }
  };

  const ensureAuth = () => {
    if (!isAuthenticated) {
      openAuthModal("login");
      return false;
    }
    return true;
  };

  const handleBuyNow = async (paymentMethod: "WECHAT" | "ALIPAY", includeExtraItems = false) => {
    if (!activeListing || !ensureAuth()) {
      return;
    }
    try {
      setBuyingChannel(paymentMethod);
      const order = await createTradeOrder(activeListing.id, parsePricingAmount(activeListing.pricing.total), includeExtraItems);
      const payment = paymentMethod === "ALIPAY"
        ? await createTradeAlipayPayment(order.orderNo)
        : await createTradeWechatPayment(order.orderNo, preferredWechatTradeType());
      setTradePayPayload(payment);
    } catch (requestError) {
      setNotice(requestError instanceof Error ? requestError.message : "下单失败，请稍后再试");
    } finally {
      setBuyingChannel("");
    }
  };

  const handleMobileShortcut = (label: (typeof mobileQuickEntries)[number]["label"]) => {
    if (label === "资源号租赁") {
      navigate("/");
      return;
    }
    if (label === "账号交易") {
      window.dispatchEvent(new CustomEvent("dt:coming-soon", { detail: "账号交易" }));
      return;
    }
    if (label === "改枪码") {
      navigate("/gun-code");
      return;
    }
    if (label === "代肝") {
      navigate("/boosting");
      return;
    }
    if (label === "客服") {
      if (!isAuthenticated) {
        openAuthModal("login");
        return;
      }
      navigate("/profile?tab=messages");
    }
  };

  const renderContent = () => {
    if (loading && !rows.length) {
      return (
        <StatusState
          title="资源列表正在同步"
          description="正在拉取最新库存、热度和收藏状态，请稍候。"
        />
      );
    }

    if (error && !rows.length) {
      return (
        <StatusState
          title="资源列表加载失败"
          description={error}
          tone="error"
          action={
            <Button kind="secondary" onClick={() => window.location.reload()}>
              重新加载
            </Button>
          }
        />
      );
    }

    if (!rows.length) {
      return (
        <StatusState
          title="当前筛选下暂无可售账号"
          description="可以放宽价格区间、地区或枪械条件，继续查看其他可租账号。"
          action={<Button onClick={resetFilters}>清空筛选</Button>}
        />
      );
    }

    return (
      <>
        <div className={`market-list market-list--${desktopDisplayMode}`}>
          {rows.map((item) => (
            desktopDisplayMode === "list" ? (
              <ListingCard
                key={item.id}
                item={item}
                favorited={favoriteIds.includes(item.id)}
                onFavorite={handleFavorite}
                onShare={handleShare}
                onCopyId={handleCopyListingId}
                onOpen={openListing}
              />
            ) : (
              <ListingCompactCard
                key={item.id}
                item={item}
                favorited={favoriteIds.includes(item.id)}
                onFavorite={handleFavorite}
                onShare={handleShare}
                onCopyId={handleCopyListingId}
                onOpen={openListing}
              />
            )
          ))}
        </div>
        <div className="market-load-more" ref={loadMoreRef}>
          {loadingMore ? <span>正在加载下一页...</span> : null}
          {!loadingMore && loadMoreError ? (
            <button type="button" onClick={() => setRequestNonce((current) => current + 1)}>
              {loadMoreError}，点此重试
            </button>
          ) : null}
          {!hasMore ? <span>已经到底了</span> : null}
        </div>
      </>
    );
  };

  return (
    <>
      {notice ? (
        <div className="app-toast" role="status" aria-live="polite">
          <span>{notice}</span>
          <button onClick={() => setNotice("")} type="button">关闭</button>
        </div>
      ) : null}
      <section className="market-mobile-shell" aria-label="移动端首页快捷入口">
        <div className="dt-container">
          <div className="market-mobile-hub">
            <div className="market-mobile-shortcuts">
              {mobileQuickEntries.map((item) => (
                <button
                  className="market-mobile-shortcut"
                  key={item.label}
                  type="button"
                  onClick={() => handleMobileShortcut(item.label)}
                >
                  <span className="market-mobile-shortcut__icon" aria-hidden="true">
                    {item.icon}
                  </span>
                  <span>{item.label}</span>
                </button>
              ))}
            </div>
            <div className="market-mobile-search">
              <button aria-label="高级筛选" className="market-mobile-search__filter" type="button" onClick={() => setMobileFilterOpen(true)}>
                <span />
              </button>
              <label className="market-mobile-search__input">
                <span className="market-mobile-search__lens" aria-hidden="true" />
                <input
                  aria-label="编号搜索"
                  placeholder="搜索编号、标题、卖家"
                  value={draft.keyword}
                  onChange={(event) => setDraft({ ...draft, keyword: event.target.value })}
                  onKeyDown={(event) => {
                    if (event.key === "Enter") {
                      applyFilters();
                    }
                  }}
                />
              </label>
            </div>
            <div className="market-mobile-actions" aria-label="移动端排序与筛选">
              <button className="market-mobile-action is-active" type="button" onClick={() => setMobileFilterOpen(true)}>
                高级筛选
              </button>
              <button
                className="market-mobile-action"
                type="button"
                onClick={() => {
                  const next = { ...draft, sort: "newest" };
                  setDraft(next);
                  setQuery(resetQueryPagination(next));
                }}
              >
                默认排序
              </button>
              <button
                className={`market-mobile-action ${query.maxPrice === "1000" ? "is-active" : ""}`}
                type="button"
                onClick={() => {
                  const next = { ...draft, maxPrice: query.maxPrice === "1000" ? "" : "1000" };
                  setDraft(next);
                  setQuery(resetQueryPagination(next));
                }}
              >
                小额专区
              </button>
              <button className="market-mobile-action" type="button" onClick={applyFilters}>
                应用搜索
              </button>
            </div>
          </div>
        </div>
      </section>

      <section className="market-board" id="market">
        <div className="dt-container">
          {meta ? (
            <>
              <section className="market-surface market-surface--desktop">
                <FilterPanel meta={meta} draft={draft} onChange={setDraft} onApply={applyFilters} onReset={resetFilters} mode="desktop" />
                <div className="market-desktop-toolbar">
                  <div className="market-desktop-toolbar__presets">
                    {desktopPresetActions.map((item) => {
                      const active = isPresetActive(item.key);
                      return (
                        <button
                          aria-pressed={active}
                          className={`market-desktop-toolbar__preset ${active ? "is-active" : ""}`}
                          key={item.key}
                          type="button"
                          onClick={() => applyPreset(item.key)}
                        >
                          {item.key === "list" ? (desktopDisplayMode === "default" ? "列表显示" : "默认显示") : item.label}
                        </button>
                      );
                    })}
                    <button
                      className="market-desktop-toolbar__preset market-desktop-toolbar__preset--export"
                      disabled={exportingListings || loading}
                      type="button"
                      onClick={handleExportListings}
                    >
                      {exportingListings ? "导出中..." : "导出Excel"}
                    </button>
                  </div>
                  <div className="market-desktop-toolbar__sorts">
                    {meta.sortOptions.map((item) => {
                      const active = isSortOptionActive(draft.sort, item.value);
                      const direction = active ? getSortDirection(draft.sort) : "desc";
                      return (
                        <button
                          aria-label={`${item.label}${active ? (direction === "asc" ? "升序" : "降序") : "排序"}`}
                          className={`market-desktop-toolbar__sort ${active ? "is-active" : ""} is-${direction}`}
                          key={item.value}
                          type="button"
                          onClick={() => {
                            const next = { ...draft, sort: nextSortValue(draft.sort, item.value) };
                            setDraft(next);
                            setQuery(resetQueryPagination(next));
                          }}
                        >
                          <span>{item.label}</span>
                          <span className="market-desktop-toolbar__sort-arrows" aria-hidden="true" />
                        </button>
                      );
                    })}
                  </div>
                </div>
              </section>

              <section className="market-surface market-surface--mobile">
                <div className="market-toolbar market-toolbar--mobile">
                  <div className="market-toolbar__group">
                    <button className="market-toolbar__sort is-active" type="button" onClick={() => setMobileFilterOpen(true)}>
                      高级筛选
                    </button>
                    {meta.sortOptions.map((item) => {
                      const active = isSortOptionActive(draft.sort, item.value);
                      const direction = active ? getSortDirection(draft.sort) : "desc";
                      return (
                        <button
                          className={`market-toolbar__sort ${active ? "is-active" : ""}`}
                          key={`mobile-${item.value}`}
                          type="button"
                          onClick={() => {
                            const next = { ...draft, sort: nextSortValue(draft.sort, item.value) };
                            setDraft(next);
                            setQuery(resetQueryPagination(next));
                          }}
                        >
                          {item.label}{active ? (direction === "asc" ? "↑" : "↓") : ""}
                        </button>
                      );
                    })}
                  </div>
                  <div className="market-toolbar__group market-toolbar__group--stats">
                    <button
                      className="market-toolbar__sort market-toolbar__sort--export"
                      disabled={exportingListings || loading}
                      type="button"
                      onClick={handleExportListings}
                    >
                      {exportingListings ? "导出中..." : "导出Excel"}
                    </button>
                    <span>{total} 个在售账号</span>
                  </div>
                </div>
              </section>
            </>
          ) : null}

          {renderContent()}
        </div>
      </section>

      {mobileFilterOpen && meta ? (
        <div className="market-mobile-filter">
          <button
            aria-label="关闭筛选抽屉"
            className="market-mobile-filter__backdrop"
            type="button"
            onClick={() => setMobileFilterOpen(false)}
          />
          <div className="market-mobile-filter__sheet" role="dialog" aria-modal="true" aria-label="账号筛选">
            <div className="market-mobile-filter__head">
              <div>
                <strong>高级筛选</strong>
                <span>移动端筛选抽屉，保留 PC 端全部筛选条件。</span>
              </div>
              <button type="button" onClick={() => setMobileFilterOpen(false)}>
                关闭
              </button>
            </div>
            <FilterPanel meta={meta} draft={draft} onChange={setDraft} onApply={applyFilters} onReset={resetFilters} mode="mobile" />
          </div>
        </div>
      ) : null}

      {activeListingNo ? (
        <ListingDetailModal
          item={activeListing}
          favorited={detailFavorited}
          buyingChannel={buyingChannel}
          loading={loading && !activeListing}
          error={!loading && !activeListing ? "该资源可能已下架或已被当前筛选隐藏" : ""}
          onClose={closeListingDetail}
          onFavorite={handleDetailFavorite}
          onShare={handleDetailShare}
          onBuyWechat={(includeExtraItems) => void handleBuyNow("WECHAT", includeExtraItems)}
          onBuyAlipay={(includeExtraItems) => void handleBuyNow("ALIPAY", includeExtraItems)}
        />
      ) : null}
      <WechatPayDialog
        open={Boolean(tradePayPayload)}
        payload={tradePayPayload}
        title={`账号订单${tradePayPayload?.paymentMethod === "ALIPAY" ? "支付宝" : "微信"}支付`}
        onClose={() => setTradePayPayload(null)}
        onCheckPaid={async () => {
          if (!tradePayPayload?.orderNo) {
            return false;
          }
          const detail = await loadOrderDetail(tradePayPayload.orderNo);
          return detail.statusCode !== "PENDING_PAYMENT";
        }}
        onPaid={async () => {
          if (!tradePayPayload?.orderNo) {
            return;
          }
          const detail = await loadOrderDetail(tradePayPayload.orderNo);
          closeListingDetail();
          if (detail.chatGroupNo) {
            window.open(`/im/${encodeURIComponent(detail.chatGroupNo)}`, "_blank", "noopener,noreferrer");
            return;
          }
          navigate(`/profile?tab=orders&focus=${encodeURIComponent(tradePayPayload.orderNo)}`);
        }}
      />
    </>
  );
}

function parsePricingAmount(value?: string) {
  const normalized = (value ?? "").replace(/[^\d.]/g, "");
  const amount = Number.parseFloat(normalized);
  return Number.isFinite(amount) && amount > 0 ? amount : 0;
}

function formatYuanAmount(value: number) {
  return `${value.toLocaleString("zh-CN", { minimumFractionDigits: 0, maximumFractionDigits: 2 })} 元`;
}

function preferredWechatTradeType(): "NATIVE" | "JSAPI" {
  if (typeof navigator !== "undefined" && /MicroMessenger/i.test(navigator.userAgent)) {
    return "JSAPI";
  }
  return "NATIVE";
}
