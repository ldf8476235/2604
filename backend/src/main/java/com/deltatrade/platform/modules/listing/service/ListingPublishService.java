package com.deltatrade.platform.modules.listing.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.deltatrade.platform.common.auth.AuthPrincipal;
import com.deltatrade.platform.common.exception.BusinessException;
import com.deltatrade.platform.common.exception.ErrorCode;
import com.deltatrade.platform.modules.auth.mapper.AuthUserMapper;
import com.deltatrade.platform.modules.auth.model.AuthUserDO;
import com.deltatrade.platform.modules.admin.service.AdminIntegrationConfigService;
import com.deltatrade.platform.modules.listing.mapper.AccountListingMapper;
import com.deltatrade.platform.modules.listing.mapper.StudioProfileMapper;
import com.deltatrade.platform.modules.listing.model.AccountListingDO;
import com.deltatrade.platform.modules.listing.model.StudioProfileDO;
import com.deltatrade.platform.modules.order.mapper.TradeOrderMapper;
import com.deltatrade.platform.modules.order.model.TradeOrderDO;
import com.deltatrade.platform.modules.oss.service.OssStorageService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class ListingPublishService {

    private static final Logger log = LoggerFactory.getLogger(ListingPublishService.class);
    private static final DateTimeFormatter LISTING_NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final List<OptionItem> HOUR_OPTIONS = buildNumberOptions(0, 24, "");
    private static final List<OptionItem> LEVEL_OPTIONS = buildNumberOptions(1, 6, "级");
    private static final List<OptionItem> SAFE_BOX_OPTIONS = Arrays.asList(
        new OptionItem("1", "基础安全箱(1*2)"),
        new OptionItem("2", "进阶安全箱(2*2)"),
        new OptionItem("3", "高级安全箱(2*3)"),
        new OptionItem("4", "顶级安全箱(3*3)")
    );
    private static final List<OptionItem> STAMINA_OPTIONS = Arrays.asList(
        new OptionItem("4", "4级"),
        new OptionItem("5", "5级"),
        new OptionItem("6", "6级"),
        new OptionItem("7", "7级")
    );
    private static final List<OptionItem> CARRY_OPTIONS = Arrays.asList(
        new OptionItem("4", "4级"),
        new OptionItem("5", "5级"),
        new OptionItem("6", "6级"),
        new OptionItem("7", "7级")
    );
    private static final List<OptionItem> DIVE_OPTIONS = Arrays.asList(
        new OptionItem("0", "0级"),
        new OptionItem("1", "1级"),
        new OptionItem("2", "2级"),
        new OptionItem("3", "3级"),
        new OptionItem("-1", "无")
    );
    private static final List<OptionItem> RANKS = Arrays.asList(
        new OptionItem("bronze", "青铜"),
        new OptionItem("silver", "白银"),
        new OptionItem("gold", "黄金"),
        new OptionItem("platinum", "铂金"),
        new OptionItem("diamond", "钻石"),
        new OptionItem("blackhawk", "黑鹰"),
        new OptionItem("summit", "巅峰")
    );
    private static final List<OptionItem> DELIVERY_METHODS = Arrays.asList(
        new OptionItem("wechat_qr", "微信扫码"),
        new OptionItem("qq_account", "QQ账密"),
        new OptionItem("qq_qr", "QQ扫码"),
        new OptionItem("steam_cn", "Steam国服"),
        new OptionItem("steam_global", "Steam国际服")
    );
    private static final List<OptionItem> OPERATORS = Arrays.asList(
        new OptionItem("red-wolf", "红狼"),
        new OptionItem("saeed", "赛依德"),
        new OptionItem("vyshdel", "维什戴尔"),
        new OptionItem("beeast", "鸟兽兽"),
        new OptionItem("lingxiao", "凌霄戍卫"),
        new OptionItem("tianji", "天际线")
    );
    private static final List<OptionItem> WEAPONS = Arrays.asList(
        new OptionItem("awm", "AWM"),
        new OptionItem("m4a1", "M4A1"),
        new OptionItem("akm", "AKM"),
        new OptionItem("vector", "Vector"),
        new OptionItem("scar", "SCAR"),
        new OptionItem("sr25", "SR-25")
    );
    private static final List<OptionItem> KNIFE_SKINS = Arrays.asList(
        new OptionItem("坠星者", "坠星者"),
        new OptionItem("处刑者", "处刑者"),
        new OptionItem("暗星", "暗星"),
        new OptionItem("龙牙", "龙牙"),
        new OptionItem("信条", "信条"),
        new OptionItem("影锋", "影锋"),
        new OptionItem("电锯惊魂", "电锯惊魂"),
        new OptionItem("北极星", "北极星"),
        new OptionItem("黑海", "黑海"),
        new OptionItem("怜悯", "怜悯"),
        new OptionItem("赤霄", "赤霄")
    );
    private static final List<String> RENTAL_EFFECTIVE_KNIFE_SKINS = Arrays.asList(
        "坠星者", "暗星", "龙牙", "信条", "影锋", "北极星", "黑海", "怜悯", "赤霄"
    );
    private static final List<OptionItem> RED_SKINS = Arrays.asList(
        new OptionItem("凌霄戍卫", "凌霄戍卫"),
        new OptionItem("维什戴尔", "维什戴尔"),
        new OptionItem("蚀金玫瑰", "蚀金玫瑰"),
        new OptionItem("水墨云图", "水墨云图"),
        new OptionItem("午夜邮差", "午夜邮差"),
        new OptionItem("天际线", "天际线")
    );
    private static final List<OptionItem> WEAPON_SKIN_CATALOG = Arrays.asList(
        new OptionItem("KC17-造物纪元", "KC17-造物纪元"),
        new OptionItem("电玩高手-MP7", "电玩高手-MP7"),
        new OptionItem("电玩高手-M250", "电玩高手-M250"),
        new OptionItem("AS Val突击步枪-悬赏令", "AS Val突击步枪-悬赏令"),
        new OptionItem("M7棱镜攻势", "M7棱镜攻势"),
        new OptionItem("M4棱镜攻势", "M4棱镜攻势"),
        new OptionItem("K416-命运", "K416-命运"),
        new OptionItem("SCAR-电玩", "SCAR-电玩"),
        new OptionItem("腾龙-气象感应", "腾龙-气象感应"),
        new OptionItem("AUG-气象感应", "AUG-气象感应"),
        new OptionItem("QBZ95-王牌之剑", "QBZ95-王牌之剑"),
        new OptionItem("Vctor-冲锋枪-美杜莎", "Vctor-冲锋枪-美杜莎")
    );
    private static final List<OptionItem> GOLD_SKINS = Arrays.asList(
        new OptionItem("鸟兽兽-荒原猎手", "鸟兽兽-荒原猎手"),
        new OptionItem("露娜-金牌射手", "露娜-金牌射手"),
        new OptionItem("牧羊人-街头之星", "牧羊人-街头之星"),
        new OptionItem("蜂医-危险物质", "蜂医-危险物质"),
        new OptionItem("蜂医-送葬人", "蜂医-送葬人"),
        new OptionItem("无名-夜鹰", "无名-夜鹰"),
        new OptionItem("威龙-壮志凌云", "威龙-壮志凌云"),
        new OptionItem("威龙-蛟龙特战队", "威龙-蛟龙特战队"),
        new OptionItem("威龙-铁面判官", "威龙-铁面判官"),
        new OptionItem("威龙-吴彦祖", "威龙-吴彦祖"),
        new OptionItem("红狼-电锯惊魂", "红狼-电锯惊魂")
    );
    private static final List<OptionItem> DEFAULT_SPEND_OPTIONS = Arrays.asList(
        new OptionItem("10m", "10M/天"),
        new OptionItem("20m_plus_2", "20M/天+2"),
        new OptionItem("30m_plus_3", "30M/天+3"),
        new OptionItem("40m_plus_4", "40M/天+4"),
        new OptionItem("50m_plus_5", "50M/天+5")
    );
    private static final List<OptionItem> EXCHANGE_RATE_OPTIONS = Arrays.asList(
        new OptionItem("custom", "自定义比例"),
        new OptionItem("default", "默认比例"),
        new OptionItem("accelerated", "特惠比例")
    );
    private static final List<OptionItem> COMPENSATION_PLANS = Arrays.asList(
        new OptionItem("normal", "普通赔付"),
        new OptionItem("full", "全额包赔")
    );
    private static final List<OptionItem> AGREEMENT_OPTIONS = Arrays.asList(
        new OptionItem("virtual_asset", "《虚拟资产出售协议》"),
        new OptionItem("owner_protocol", "《号主协议》"),
        new OptionItem("full_coverage", "《全额包赔协议》")
    );
    private static final List<OptionItem> MOD_CODES = Arrays.asList(
        new OptionItem("m416-stable", "M416 稳定压枪码"),
        new OptionItem("akm-rapid", "AKM 急速爆发码"),
        new OptionItem("vector-close", "Vector 近战拉枪码")
    );
    private static final List<ExtraItemMeta> EXTRA_ITEMS = Arrays.asList(
        new ExtraItemMeta("awm_bullet", "AWM子弹", "发", new BigDecimal("0.8")),
        new ExtraItemMeta("bullet_level_6", "6级子弹", "组", new BigDecimal("6")),
        new ExtraItemMeta("helmet_level_6", "6级头盔", "个", new BigDecimal("2")),
        new ExtraItemMeta("armor_level_6", "6级护甲", "个", new BigDecimal("3")),
        new ExtraItemMeta("barrett_bullet", "巴雷特子弹", "发", new BigDecimal("0.8")),
        new ExtraItemMeta("premium_insurance", "顶级保险卡", "张", new BigDecimal("5")),
        new ExtraItemMeta("premium_coffee", "高级咖啡豆", "个", new BigDecimal("2.5")),
        new ExtraItemMeta("premium_bullet_part", "高级子弹零件", "个", new BigDecimal("3"))
    );
    private static final List<String> REQUIRED_AGREEMENTS = Arrays.asList("virtual_asset", "owner_protocol");
    private static final Map<String, String> LEGACY_RANK_MAPPINGS = buildLegacyRankMappings();

    private final AccountListingMapper accountListingMapper;
    private final StudioProfileMapper studioProfileMapper;
    private final AuthUserMapper authUserMapper;
    private final ChinaCityCatalog chinaCityCatalog;
    private final TradeOrderMapper tradeOrderMapper;
    private final OssStorageService ossStorageService;
    private final ObjectMapper objectMapper;
    private final AdminIntegrationConfigService adminIntegrationConfigService;

    public ListingPublishService(
        AccountListingMapper accountListingMapper,
        StudioProfileMapper studioProfileMapper,
        AuthUserMapper authUserMapper,
        ChinaCityCatalog chinaCityCatalog,
        TradeOrderMapper tradeOrderMapper,
        OssStorageService ossStorageService,
        ObjectMapper objectMapper,
        AdminIntegrationConfigService adminIntegrationConfigService
    ) {
        this.accountListingMapper = accountListingMapper;
        this.studioProfileMapper = studioProfileMapper;
        this.authUserMapper = authUserMapper;
        this.chinaCityCatalog = chinaCityCatalog;
        this.tradeOrderMapper = tradeOrderMapper;
        this.ossStorageService = ossStorageService;
        this.objectMapper = objectMapper;
        this.adminIntegrationConfigService = adminIntegrationConfigService;
    }

    public PublishMetaResult loadMeta(AuthPrincipal principal) {
        SellerContext sellerContext = resolveSellerContext(principal);
        return new PublishMetaResult(
            sellerContext,
            chinaCityCatalog.getProvinces(),
            DELIVERY_METHODS,
            HOUR_OPTIONS,
            RANKS,
            SAFE_BOX_OPTIONS,
            STAMINA_OPTIONS,
            CARRY_OPTIONS,
            DIVE_OPTIONS,
            OPERATORS,
            WEAPONS,
            KNIFE_SKINS,
            RED_SKINS,
            WEAPON_SKIN_CATALOG,
            GOLD_SKINS,
            DEFAULT_SPEND_OPTIONS,
            EXCHANGE_RATE_OPTIONS,
            COMPENSATION_PLANS,
            AGREEMENT_OPTIONS,
	            EXTRA_ITEMS,
	            MOD_CODES,
	            defaultExchangeRate(),
	            personalSellerCommissionRate(),
	            Arrays.asList(
	                "参考站可见字段已经全部补进发布页，包含接入信息、账号属性、额外物资、皮肤、赔付和协议。",
	                "有封禁记录时必须上传处罚截图；选择全额包赔时必须额外勾选《全额包赔协议》。",
                "上传图片建议包含：个人信息截图、特勤处截图、皮肤检视图、仓库近战武器图。"
            )
        );
    }

    public int repairHistoricalRentalPricing() {
        List<AccountListingDO> listings = accountListingMapper.selectList(Wrappers.<AccountListingDO>lambdaQuery());
        int repaired = 0;
        long startAt = System.currentTimeMillis();
        for (AccountListingDO listing : listings) {
            if (isTemporaryTestListing(listing)) {
                continue;
            }
            PublishAttributesSnapshot attributes = readPublishAttributes(listing.getPublishAttributesJson());
            if (!StringUtils.hasText(attributes.getExchangeRateType()) || listing.getHafCurrency() == null) {
                continue;
            }
            try {
                PublishCommand command = buildRepairCommand(listing, attributes);
                applyRentalDaysPolicy(command);
                applyExchangeRatePolicy(command);
                applyRentalPricePolicy(command);

                Long nextHafCurrency = command.getHafCurrency();
                BigDecimal nextPrice = command.getPrice().setScale(2, RoundingMode.HALF_UP);
                BigDecimal currentPrice = listing.getPrice() == null
                    ? null
                    : listing.getPrice().setScale(2, RoundingMode.HALF_UP);
                BigDecimal nextExchangeRate = scaleNullable(command.getCustomExchangeRate());
                BigDecimal currentExchangeRate = scaleNullable(attributes.getCustomExchangeRate());
                Integer nextRentalDays = command.getRentalDays();
                Integer currentRentalDays = attributes.getRentalDays();

                if (sameLong(listing.getHafCurrency(), nextHafCurrency)
                    && sameMoney(currentPrice, nextPrice)
                    && sameMoney(currentExchangeRate, nextExchangeRate)
                    && sameInteger(currentRentalDays, nextRentalDays)) {
                    continue;
                }

                attributes.setRentalDays(nextRentalDays);
                attributes.setCustomExchangeRate(nextExchangeRate);
                listing.setHafCurrency(nextHafCurrency);
                listing.setPrice(nextPrice);
                listing.setPublishAttributesJson(writeJson(attributes));
                listing.setUpdatedAt(LocalDateTime.now());
                accountListingMapper.updateById(listing);
                repaired += 1;
            } catch (Exception exception) {
                log.warn("historical listing rental repair skipped listingNo={} reason={}", listing.getListingNo(), exception.getMessage());
            }
        }
        if (repaired > 0) {
            log.info("historical listing rental repair success rows={} checked={} costMs={}",
                repaired, listings.size(), System.currentTimeMillis() - startAt);
        }
        return repaired;
    }

    private boolean isTemporaryTestListing(AccountListingDO listing) {
        return listing != null
            && listing.getPrice() != null
            && listing.getPrice().compareTo(new BigDecimal("0.10")) == 0
            && StringUtils.hasText(listing.getTitle())
            && listing.getTitle().contains("测试账号");
    }

    public PublishSubmitResult submit(AuthPrincipal principal, PublishCommand command) {
        requireRealNameVerifiedForPublishing(principal);
        SellerContext sellerContext = resolveSellerContext(principal);
        validateCommand(command);
        applyRentalDaysPolicy(command);
        applyRentalPricePolicy(command);
        applyExchangeRatePolicy(command);
        EstimateSnapshot estimate = estimate(command);
        ChinaCityCatalog.ResolvedCity resolvedCity = chinaCityCatalog.resolve(command.getProvinceCode(), command.getCityCode());
        LocalDateTime now = LocalDateTime.now();

        List<String> operators = normalizeTextList(command.getOperators());
        List<String> weapons = normalizeTextList(command.getWeapons());
        List<String> weaponSkins = normalizeTextList(command.getWeaponSkins());
        List<String> knifeSkins = normalizeTextList(command.getKnifeSkins());
        List<String> redSkins = normalizeTextList(command.getRedSkins());
        List<String> goldSkins = normalizeTextList(command.getGoldSkins());
        List<String> agreements = normalizeTextList(command.getAgreements());
        List<String> modCodes = normalizeTextList(command.getModCodes());
        List<ExtraItemSnapshot> extraItems = buildExtraItemSnapshots(command.getExtraItems());

        AccountListingDO listing = new AccountListingDO();
        listing.setListingNo(buildListingNo());
        listing.setSellerUserId(principal.getUserId());
        listing.setSellerNickname(resolveSellerNickname(principal));
        listing.setSellerType(sellerContext.getSellerType());
        listing.setStudioName(sellerContext.getStudioName());
        listing.setReviewStrategy(sellerContext.getReviewStrategy());
        listing.setStatus(sellerContext.isDirectPublish() ? "PUBLISHED" : "PENDING_REVIEW");
        listing.setProvinceCode(resolvedCity.getProvinceCode());
        listing.setProvinceName(resolvedCity.getProvinceName());
        listing.setCityCode(resolvedCity.getCityCode());
        listing.setCityName(resolvedCity.getCityName());
        listing.setGameServer(resolvedCity.getCityCode());
        listing.setDeliveryMethod(command.getDeliveryMethod());
        listing.setAlwaysOnline(Boolean.TRUE.equals(command.getAlwaysOnline()));
        listing.setAccountLevel(command.getAccountLevel());
        listing.setRankName(command.getRankName());
        listing.setSafeBoxLevel(command.getSafeBoxLevel());
        listing.setHafCurrency(command.getHafCurrency());
        listing.setTitle(command.getTitle().trim());
        listing.setDescription(command.getDescription().trim());
        listing.setOperatorCount(command.getOperatorCount());
        listing.setOperatorsJson(writeJson(operators));
        listing.setWeaponsJson(writeJson(weapons));
        listing.setWeaponSkinsJson(writeJson(weaponSkins));
        listing.setOtherItems(normalizeNullableText(command.getOtherItems()));
        listing.setImageKeysJson(writeJson(command.getImageKeys()));
        listing.setVideoKey(normalizeNullableText(command.getVideoKey()));
        listing.setPrice(command.getPrice().setScale(2, RoundingMode.HALF_UP));
        listing.setNegotiable(Boolean.TRUE.equals(command.getNegotiable()));
        listing.setModCodesJson(writeJson(modCodes));
        listing.setPublishAttributesJson(writeJson(new PublishAttributesSnapshot(
            command.getDeliveryStartHour(),
            command.getDeliveryEndHour(),
            command.getStaminaLevel(),
            command.getCarryLevel(),
            command.getDiveLevel(),
            command.getBanRecord(),
            Boolean.TRUE.equals(command.getBanRecord()) ? normalizeNullableText(command.getPunishmentImageKey()) : null,
            command.getFaceOwned(),
            command.getUnlockSaeed(),
            knifeSkins,
            redSkins,
            goldSkins,
            scaleNullable(command.getSecretKd()),
            command.getDefaultSpend(),
            command.getRentalDays(),
            command.getExchangeRateType(),
            scaleNullable(command.getCustomExchangeRate()),
            command.getCompensationPlan(),
            command.getDeposit().setScale(2, RoundingMode.HALF_UP),
            normalizeNullableText(command.getRemarks()),
            agreements,
            extraItems
        )));
        listing.setCoverImageKey(command.getImageKeys().get(0));
        listing.setSuggestedPrice(estimate.getSuggestedPrice());
        listing.setEstimateDetail(estimate.getEstimateDetail());
        listing.setRejectionReason(null);
        listing.setCreatedAt(now);
        listing.setUpdatedAt(now);
        listing.setSubmittedAt(now);
        listing.setPublishedAt(sellerContext.isDirectPublish() ? now : null);

        long startAt = System.currentTimeMillis();
        int rows = accountListingMapper.insert(listing);
        log.info("create listing success listingNo={} userId={} sellerType={} status={} cityCode={} deliveryMethod={} defaultSpend={} compensationPlan={} rows={} costMs={}",
            listing.getListingNo(), principal.getUserId(), listing.getSellerType(), listing.getStatus(), listing.getCityCode(),
            listing.getDeliveryMethod(), command.getDefaultSpend(), command.getCompensationPlan(), rows, System.currentTimeMillis() - startAt);

        String message = sellerContext.isDirectPublish()
            ? "工作室当前为免审直发，账号已上架展示。"
            : "账号已提交审核，待后台人工审核后上架。";
        return new PublishSubmitResult(
            listing.getListingNo(),
            listing.getStatus(),
            message,
            listing.getSuggestedPrice(),
            listing.getEstimateDetail()
        );
    }

    public MyListingListResult loadMyListings(AuthPrincipal principal, String status) {
        long startAt = System.currentTimeMillis();
        List<AccountListingDO> listings = accountListingMapper.selectList(
            Wrappers.<AccountListingDO>lambdaQuery()
                .eq(AccountListingDO::getSellerUserId, principal.getUserId())
                .eq(StringUtils.hasText(status) && !"ALL".equalsIgnoreCase(status), AccountListingDO::getStatus, status)
                .orderByDesc(AccountListingDO::getUpdatedAt)
                .orderByDesc(AccountListingDO::getCreatedAt)
        );
        log.info("mysql query success target=account_listing_by_owner costMs={} userId={} count={} status={}",
            System.currentTimeMillis() - startAt, principal.getUserId(), listings.size(), status == null ? "ALL" : status);
        List<MyListingListItem> rows = listings.stream()
            .map(this::buildMyListingListItem)
            .collect(Collectors.toList());
        return new MyListingListResult(rows.size(), rows);
    }

    public MyListingDetail loadMyListingDetail(AuthPrincipal principal, String listingNo) {
        long startAt = System.currentTimeMillis();
        AccountListingDO listing = loadOwnedListing(principal, listingNo);
        PublishAttributesSnapshot attributes = readPublishAttributes(listing.getPublishAttributesJson());
        PublishCommand draft = buildDraft(listing, attributes);
        List<ReviewRecord> reviewRecords = buildReviewRecords(listing);
        List<TradeRecord> tradeRecords = buildTradeRecords(principal, listing.getListingNo());
        MyListingDetail detail = new MyListingDetail(
            buildMyListingListItem(listing),
            draft,
            buildAssetItems(readStringList(listing.getImageKeysJson())),
            buildAssetItem(listing.getVideoKey()),
            buildAssetItem(attributes.getPunishmentImageKey()),
            reviewRecords,
            tradeRecords
        );
        log.info("load my listing detail success listingNo={} userId={} status={} tradeCount={} reviewCount={} costMs={}",
            listingNo, principal.getUserId(), listing.getStatus(), tradeRecords.size(), reviewRecords.size(), System.currentTimeMillis() - startAt);
        return detail;
    }

    public PublishSubmitResult updateMyListing(AuthPrincipal principal, String listingNo, PublishCommand command) {
        AccountListingDO listing = loadOwnedListing(principal, listingNo);
        if (!canEdit(listing.getStatus())) {
            log.warn("listing update rejected listingNo={} userId={} status={}", listingNo, principal.getUserId(), listing.getStatus());
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "当前状态不允许编辑");
        }
        requireRealNameVerifiedForPublishing(principal);
        SellerContext sellerContext = resolveSellerContext(principal);
        validateCommand(command);
        applyRentalDaysPolicy(command);
        applyRentalPricePolicy(command);
        applyExchangeRatePolicy(command);
        EstimateSnapshot estimate = estimate(command);
        ChinaCityCatalog.ResolvedCity resolvedCity = chinaCityCatalog.resolve(command.getProvinceCode(), command.getCityCode());
        LocalDateTime now = LocalDateTime.now();

        List<String> operators = normalizeTextList(command.getOperators());
        List<String> weapons = normalizeTextList(command.getWeapons());
        List<String> weaponSkins = normalizeTextList(command.getWeaponSkins());
        List<String> knifeSkins = normalizeTextList(command.getKnifeSkins());
        List<String> redSkins = normalizeTextList(command.getRedSkins());
        List<String> goldSkins = normalizeTextList(command.getGoldSkins());
        List<String> agreements = normalizeTextList(command.getAgreements());
        List<String> modCodes = normalizeTextList(command.getModCodes());
        List<ExtraItemSnapshot> extraItems = buildExtraItemSnapshots(command.getExtraItems());

        listing.setSellerNickname(resolveSellerNickname(principal));
        listing.setSellerType(sellerContext.getSellerType());
        listing.setStudioName(sellerContext.getStudioName());
        listing.setReviewStrategy(sellerContext.getReviewStrategy());
        listing.setProvinceCode(resolvedCity.getProvinceCode());
        listing.setProvinceName(resolvedCity.getProvinceName());
        listing.setCityCode(resolvedCity.getCityCode());
        listing.setCityName(resolvedCity.getCityName());
        listing.setGameServer(resolvedCity.getCityCode());
        listing.setDeliveryMethod(command.getDeliveryMethod());
        listing.setAlwaysOnline(Boolean.TRUE.equals(command.getAlwaysOnline()));
        listing.setAccountLevel(command.getAccountLevel());
        listing.setRankName(command.getRankName());
        listing.setSafeBoxLevel(command.getSafeBoxLevel());
        listing.setHafCurrency(command.getHafCurrency());
        listing.setTitle(command.getTitle().trim());
        listing.setDescription(command.getDescription().trim());
        listing.setOperatorCount(command.getOperatorCount());
        listing.setOperatorsJson(writeJson(operators));
        listing.setWeaponsJson(writeJson(weapons));
        listing.setWeaponSkinsJson(writeJson(weaponSkins));
        listing.setOtherItems(normalizeNullableText(command.getOtherItems()));
        listing.setImageKeysJson(writeJson(command.getImageKeys()));
        listing.setVideoKey(normalizeNullableText(command.getVideoKey()));
        listing.setPrice(command.getPrice().setScale(2, RoundingMode.HALF_UP));
        listing.setNegotiable(Boolean.TRUE.equals(command.getNegotiable()));
        listing.setModCodesJson(writeJson(modCodes));
        listing.setPublishAttributesJson(writeJson(new PublishAttributesSnapshot(
            command.getDeliveryStartHour(),
            command.getDeliveryEndHour(),
            command.getStaminaLevel(),
            command.getCarryLevel(),
            command.getDiveLevel(),
            command.getBanRecord(),
            Boolean.TRUE.equals(command.getBanRecord()) ? normalizeNullableText(command.getPunishmentImageKey()) : null,
            command.getFaceOwned(),
            command.getUnlockSaeed(),
            knifeSkins,
            redSkins,
            goldSkins,
            scaleNullable(command.getSecretKd()),
            command.getDefaultSpend(),
            command.getRentalDays(),
            command.getExchangeRateType(),
            scaleNullable(command.getCustomExchangeRate()),
            command.getCompensationPlan(),
            command.getDeposit().setScale(2, RoundingMode.HALF_UP),
            normalizeNullableText(command.getRemarks()),
            agreements,
            extraItems
        )));
        listing.setCoverImageKey(command.getImageKeys().get(0));
        listing.setSuggestedPrice(estimate.getSuggestedPrice());
        listing.setEstimateDetail(estimate.getEstimateDetail());
        listing.setRejectionReason(null);
        listing.setUpdatedAt(now);
        listing.setSubmittedAt(now);

        String nextStatus = sellerContext.isDirectPublish() ? "PUBLISHED" : "PENDING_REVIEW";
        if ("PUBLISHED".equals(nextStatus)) {
            ensureListingCanBePublished(listing);
        }
        listing.setStatus(nextStatus);
        listing.setPublishedAt("PUBLISHED".equals(nextStatus) ? now : null);

        long startAt = System.currentTimeMillis();
        int rows = accountListingMapper.updateById(listing);
        log.info("update my listing success listingNo={} userId={} nextStatus={} rows={} costMs={}",
            listingNo, principal.getUserId(), nextStatus, rows, System.currentTimeMillis() - startAt);
        String message = "PUBLISHED".equals(nextStatus)
            ? "修改已保存，账号已重新上架。"
            : "修改已保存，账号已重新进入人工审核。";
        return new PublishSubmitResult(listingNo, nextStatus, message, listing.getSuggestedPrice(), listing.getEstimateDetail());
    }

    public ActionResult withdrawMyListing(AuthPrincipal principal, String listingNo) {
        AccountListingDO listing = loadOwnedListing(principal, listingNo);
        if (!"PUBLISHED".equals(listing.getStatus())) {
            log.warn("listing withdraw rejected listingNo={} userId={} status={}", listingNo, principal.getUserId(), listing.getStatus());
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "仅已上架账号可执行下架");
        }
        listing.setStatus("OFFLINE");
        listing.setUpdatedAt(LocalDateTime.now());
        long startAt = System.currentTimeMillis();
        int rows = accountListingMapper.updateById(listing);
        log.info("withdraw my listing success listingNo={} userId={} rows={} costMs={}",
            listingNo, principal.getUserId(), rows, System.currentTimeMillis() - startAt);
        return new ActionResult(listingNo, "OFFLINE", "账号已下架，可在我的发布中编辑后重新上架。");
    }

    public ActionResult resubmitMyListing(AuthPrincipal principal, String listingNo) {
        AccountListingDO listing = loadOwnedListing(principal, listingNo);
        if (!"REJECTED".equals(listing.getStatus())) {
            log.warn("listing resubmit rejected listingNo={} userId={} status={}", listingNo, principal.getUserId(), listing.getStatus());
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "仅已驳回账号可重新提交审核");
        }
        requireRealNameVerifiedForPublishing(principal);
        SellerContext sellerContext = resolveSellerContext(principal);
        LocalDateTime now = LocalDateTime.now();
        String nextStatus = sellerContext.isDirectPublish() ? "PUBLISHED" : "PENDING_REVIEW";
        listing.setStatus(nextStatus);
        listing.setReviewStrategy(sellerContext.getReviewStrategy());
        listing.setRejectionReason(null);
        listing.setUpdatedAt(now);
        listing.setSubmittedAt(now);
        listing.setPublishedAt("PUBLISHED".equals(nextStatus) ? now : null);
        long startAt = System.currentTimeMillis();
        int rows = accountListingMapper.updateById(listing);
        log.info("resubmit my listing success listingNo={} userId={} nextStatus={} rows={} costMs={}",
            listingNo, principal.getUserId(), nextStatus, rows, System.currentTimeMillis() - startAt);
        String message = "PUBLISHED".equals(nextStatus)
            ? "账号已重新上架。"
            : "账号已重新提交审核，请等待平台处理。";
        return new ActionResult(listingNo, nextStatus, message);
    }

    private SellerContext resolveSellerContext(AuthPrincipal principal) {
        long startAt = System.currentTimeMillis();
        StudioProfileDO studio = studioProfileMapper.selectOne(
            Wrappers.<StudioProfileDO>lambdaQuery()
                .eq(StudioProfileDO::getOwnerUserId, principal.getUserId())
                .last("LIMIT 1")
        );
        log.info("mysql query success target=studio_profile_by_owner costMs={} hit={} userId={}",
            System.currentTimeMillis() - startAt, studio != null, principal.getUserId());
        if (studio == null) {
            return new SellerContext("PERSONAL", "个人卖家", "REQUIRED_REVIEW", "提交后进入平台人工审核", null, true);
        }
        String cooperationStatus = studio.getCooperationStatus() == null ? "ACTIVE" : studio.getCooperationStatus().trim().toUpperCase(java.util.Locale.ROOT);
        if (!"ACTIVE".equals(cooperationStatus) || !Boolean.TRUE.equals(studio.getActive())) {
            String message = "CLEARED".equals(cooperationStatus) ? "工作室已被平台清退，当前不可继续发布新商品" : "工作室当前已暂停合作，暂不可发布新商品";
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, message);
        }
        boolean directPublish = "DIRECT_PUBLISH".equalsIgnoreCase(studio.getReviewStrategy());
        return new SellerContext(
            "STUDIO",
            studio.getStudioName(),
            directPublish ? "DIRECT_PUBLISH" : "REQUIRED_REVIEW",
            directPublish ? "免审直发，提交后直接上架" : "工作室需审核，提交后进入人工审核",
            studio.getStudioName(),
            true
        );
    }

    private String resolveSellerNickname(AuthPrincipal principal) {
        long startAt = System.currentTimeMillis();
        AuthUserDO user = authUserMapper.selectById(principal.getUserId());
        log.info("mysql query success target=auth_user_by_id costMs={} hit={} userId={}",
            System.currentTimeMillis() - startAt, user != null, principal.getUserId());
        return user != null && StringUtils.hasText(user.getNickname()) ? user.getNickname() : principal.getNickname();
    }

    private void requireRealNameVerifiedForPublishing(AuthPrincipal principal) {
        long startAt = System.currentTimeMillis();
        AuthUserDO user = authUserMapper.selectById(principal.getUserId());
        log.info("mysql query success target=auth_user_publish_guard costMs={} hit={} userId={}",
            System.currentTimeMillis() - startAt, user != null, principal.getUserId());
        if (user == null
            || !Boolean.TRUE.equals(user.getVerified())
            || !"APPROVED".equalsIgnoreCase(String.valueOf(user.getRealNameStatus()))
            || (!StringUtils.hasText(user.getRealNamePhone()) && !StringUtils.hasText(user.getPhone()))) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "未实名不允许发布账号");
        }
    }

    private void validateCommand(PublishCommand command) {
        command.setRankName(normalizeRankName(command.getRankName()));
        if (!StringUtils.hasText(command.getProvinceCode())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请选择所在省份");
        }
        if (!StringUtils.hasText(command.getCityCode())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请选择所在城市");
        }
        if (!containsValue(DELIVERY_METHODS, command.getDeliveryMethod())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请选择有效的上号方式");
        }
        if (command.getDeliveryStartHour() == null || command.getDeliveryStartHour() < 0 || command.getDeliveryStartHour() > 24) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "最早上号时间需在 0-24 之间");
        }
        if (command.getDeliveryEndHour() == null || command.getDeliveryEndHour() < 0 || command.getDeliveryEndHour() > 24) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "最晚上号时间需在 0-24 之间");
        }
        if (command.getDeliveryStartHour() > command.getDeliveryEndHour()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "最早上号时间不能晚于最晚上号时间");
        }
        if (command.getAccountLevel() == null || command.getAccountLevel() < 1 || command.getAccountLevel() > 120) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "账号等级需在 1-120 之间");
        }
        if (!containsValue(RANKS, command.getRankName())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请选择有效的账号段位");
        }
        validateSafeBoxField(command.getSafeBoxLevel());
        validateStaminaField(command.getStaminaLevel());
        validateCarryField(command.getCarryLevel());
        validateDiveField(command.getDiveLevel());
        if (command.getBanRecord() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请选择封禁记录");
        }
        if (Boolean.TRUE.equals(command.getBanRecord()) && !StringUtils.hasText(command.getPunishmentImageKey())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "有封禁记录时必须上传处罚截图");
        }
        if (command.getPunishmentImageKey() != null && command.getPunishmentImageKey().trim().length() > 255) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "处罚截图资源标识过长");
        }
        if (command.getFaceOwned() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请选择人脸归属");
        }
        if (command.getUnlockSaeed() == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请选择是否解锁赛依德");
        }
        if (command.getHafCurrency() == null || command.getHafCurrency() < 30) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "哈夫币余额需达到 30M 起算");
        }
        if (!StringUtils.hasText(command.getTitle())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请输入账号标题");
        }
        if (StringUtils.hasText(command.getDescription()) && command.getDescription().trim().length() > 500) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "账号描述不能超过 500 个字符");
        }
        if (command.getOperatorCount() == null || command.getOperatorCount() < 0 || command.getOperatorCount() > 999) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "干员数量格式不正确");
        }
        if (command.getSecretKd() == null || command.getSecretKd().compareTo(BigDecimal.ZERO) < 0 || command.getSecretKd().compareTo(new BigDecimal("99.99")) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "绝密KD需在 0-99.99 之间");
        }
        if (!containsValue(DEFAULT_SPEND_OPTIONS, command.getDefaultSpend())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请选择默认消耗");
        }
        if (command.getHafCurrency() < defaultSpendDailyM(command.getDefaultSpend())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "哈夫币余额不能低于所选默认消耗");
        }
        if (!containsValue(EXCHANGE_RATE_OPTIONS, command.getExchangeRateType())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请选择兑换比例");
        }
        if ("custom".equals(command.getExchangeRateType())) {
            if (command.getCustomExchangeRate() == null || command.getCustomExchangeRate().compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "兑换比例必须大于 0");
            }
        }
        if (!containsValue(COMPENSATION_PLANS, command.getCompensationPlan())) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请选择赔付方案");
        }
        if (command.getPrice() == null || command.getPrice().compareTo(new BigDecimal("0.10")) < 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "租金不能低于 0.1 元");
        }
        if (command.getDeposit() == null || command.getDeposit().compareTo(BigDecimal.ZERO) < 0 || command.getDeposit().compareTo(new BigDecimal("2000.00")) > 0) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "押金需在 0-2000 元之间");
        }
        if (StringUtils.hasText(command.getRemarks()) && command.getRemarks().trim().length() > 500) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "备注不能超过 500 个字符");
        }
        validateAgreements(command);
        validateExtraItems(command.getExtraItems());
        if (command.getImageKeys() == null || command.getImageKeys().size() < 3 || command.getImageKeys().size() > 10) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "截图需上传 3-10 张");
        }
        if (command.getVideoKey() != null && command.getVideoKey().trim().length() > 255) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "视频资源标识过长");
        }
    }

    private void validateAgreements(PublishCommand command) {
        List<String> agreements = normalizeTextList(command.getAgreements());
        for (String requiredAgreement : REQUIRED_AGREEMENTS) {
            if (!agreements.contains(requiredAgreement)) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请先勾选必选协议");
            }
        }
        if ("full".equals(command.getCompensationPlan()) && !agreements.contains("full_coverage")) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "全额包赔需额外勾选《全额包赔协议》");
        }
    }

    private void validateExtraItems(List<ExtraItemCommand> extraItems) {
        if (extraItems == null || extraItems.isEmpty()) {
            return;
        }
        for (ExtraItemCommand item : extraItems) {
            ExtraItemMeta meta = findExtraItemMeta(item.getCode());
            if (meta == null) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, "存在无效的额外物资配置");
            }
            if (item.getCount() == null || item.getCount() < 0 || item.getCount() > 9999) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, meta.getLabel() + " 数量格式不正确");
            }
            if (!"gift".equals(item.getChargeMode()) && !"charge".equals(item.getChargeMode())) {
                throw new BusinessException(ErrorCode.BUSINESS_ERROR, meta.getLabel() + " 计费方式不正确");
            }
        }
    }

    private void validateLevelField(Integer value, String label) {
        if (value == null || value < 1 || value > 6) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, label + " 需在 1-6 级之间");
        }
    }

    private void validateSafeBoxField(Integer value) {
        if (value == null || value < 1 || value > 4) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "安全箱需在基础-顶级档位之间");
        }
    }

    private void validateStaminaField(Integer value) {
        if (value == null || value < 4 || value > 7) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "体力需在 4-7 级之间");
        }
    }

    private void validateCarryField(Integer value) {
        if (value == null || value < 4 || value > 7) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "负重需在 4-7 级之间");
        }
    }

    private void validateDiveField(Integer value) {
        if (value == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "请选择潜水等级");
        }
        if (value == -1) {
            return;
        }
        if (value < 0 || value > 3) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "潜水等级需在无、0-3 级之间");
        }
    }

    private PublishCommand buildRepairCommand(AccountListingDO listing, PublishAttributesSnapshot attributes) {
        PublishCommand command = new PublishCommand();
        command.setHafCurrency(normalizeHistoricalHafCurrency(listing.getHafCurrency()));
        command.setSafeBoxLevel(listing.getSafeBoxLevel());
        command.setStaminaLevel(attributes.getStaminaLevel());
        command.setCarryLevel(attributes.getCarryLevel());
        command.setDefaultSpend(attributes.getDefaultSpend());
        command.setExchangeRateType(attributes.getExchangeRateType());
        command.setCustomExchangeRate(attributes.getCustomExchangeRate());
        command.setKnifeSkins(copyTexts(attributes.getKnifeSkins()));
        return command;
    }

    private Long normalizeHistoricalHafCurrency(Long value) {
        if (value == null) {
            return null;
        }
        if (value >= 10000L) {
            return Math.max(1L, value / 1000L);
        }
        return value;
    }

    private boolean sameMoney(BigDecimal left, BigDecimal right) {
        if (left == null || right == null) {
            return left == right;
        }
        return left.compareTo(right) == 0;
    }

    private boolean sameLong(Long left, Long right) {
        if (left == null || right == null) {
            return left == right;
        }
        return left.equals(right);
    }

    private boolean sameInteger(Integer left, Integer right) {
        if (left == null || right == null) {
            return left == right;
        }
        return left.equals(right);
    }

    private void requireTags(List<String> values, String message) {
        if (normalizeTextList(values).isEmpty()) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, message);
        }
    }

    private void applyExchangeRatePolicy(PublishCommand command) {
        if ("default".equals(command.getExchangeRateType())) {
            command.setCustomExchangeRate(adjustDefaultExchangeRate(defaultExchangeRate(), command.getDefaultSpend()));
        } else if ("accelerated".equals(command.getExchangeRateType())) {
            command.setCustomExchangeRate(adjustDefaultExchangeRate(defaultExchangeRate().add(new BigDecimal("50000")), command.getDefaultSpend()));
        } else if (command.getCustomExchangeRate() != null) {
            command.setCustomExchangeRate(scaleNullable(command.getCustomExchangeRate()));
        }
    }

    private BigDecimal adjustDefaultExchangeRate(BigDecimal value, String defaultSpend) {
        if (value == null) {
            return null;
        }
        return value.add(new BigDecimal(defaultSpendRatioOffset(defaultSpend)).multiply(new BigDecimal("10000")));
    }

	    private BigDecimal defaultExchangeRate() {
	        Object value = adminIntegrationConfigService.loadListingPublishConfig().get("defaultExchangeRate");
	        try {
	            BigDecimal rate = new BigDecimal(String.valueOf(value));
            if (rate.compareTo(BigDecimal.ZERO) > 0) {
                return rate.stripTrailingZeros();
            }
        } catch (Exception ignored) {
            // Fall back below when stored configuration is malformed.
        }
	        return new BigDecimal("380000");
	    }

	    private BigDecimal personalSellerCommissionRate() {
	        Object value = adminIntegrationConfigService.loadListingPublishConfig().get("personalSellerCommissionRate");
	        try {
	            BigDecimal rate = new BigDecimal(String.valueOf(value));
	            if (rate.compareTo(BigDecimal.ZERO) >= 0 && rate.compareTo(BigDecimal.ONE) <= 0) {
	                return rate.setScale(4, RoundingMode.HALF_UP);
	            }
	        } catch (Exception ignored) {
	            // Fall back below when stored configuration is malformed.
	        }
	        return new BigDecimal("0.1000");
	    }

	    private void applyRentalPricePolicy(PublishCommand command) {
        command.setPrice(calculateRentalPrice(command));
    }

    private void applyRentalDaysPolicy(PublishCommand command) {
        command.setRentalDays(calculateRentalDays(command.getHafCurrency(), command.getDefaultSpend()));
    }

    private int calculateRentalDays(Long hafCurrency, String defaultSpend) {
        int dailySpend = defaultSpendDailyM(defaultSpend);
        if (hafCurrency == null || dailySpend <= 0) {
            return 1;
        }
        long days = hafCurrency / dailySpend;
        if (days < 1) {
            return 1;
        }
        return (int) Math.min(30L, days);
    }

    private int defaultSpendDailyM(String value) {
        if ("10m".equals(value)) {
            return 10;
        }
        if ("20m_plus_2".equals(value)) {
            return 20;
        }
        if ("30m_plus_3".equals(value)) {
            return 30;
        }
        if ("40m_plus_4".equals(value)) {
            return 40;
        }
        if ("50m_plus_5".equals(value)) {
            return 50;
        }
        return 0;
    }

    private BigDecimal calculateRentalPrice(PublishCommand command) {
        int ratio = calculateRentalRatio(command);
        return new BigDecimal(command.getHafCurrency())
            .multiply(new BigDecimal("100"))
            .divide(new BigDecimal(ratio), 0, RoundingMode.DOWN)
            .setScale(2, RoundingMode.DOWN);
    }

    private int calculateRentalRatio(PublishCommand command) {
        if ("custom".equals(command.getExchangeRateType())
            && command.getCustomExchangeRate() != null
            && command.getCustomExchangeRate().compareTo(BigDecimal.ZERO) > 0) {
            return rentalBaseRatio(command);
        }
        int ratio = rentalBaseRatio(command);
        ratio += safeBoxRatioDelta(command.getSafeBoxLevel());
        ratio += levelRatioDelta(command.getStaminaLevel());
        ratio += levelRatioDelta(command.getCarryLevel());
        ratio += hafCurrencyRatioDelta(command.getHafCurrency());
        ratio += defaultSpendRatioOffset(command.getDefaultSpend());
        if (hasEffectiveKnifeSkin(command.getKnifeSkins())) {
            ratio -= 1;
        }
        return Math.max(1, ratio);
    }

    private int rentalBaseRatio(PublishCommand command) {
        if ("custom".equals(command.getExchangeRateType())
            && command.getCustomExchangeRate() != null
            && command.getCustomExchangeRate().compareTo(BigDecimal.ZERO) > 0) {
            return Math.max(1, command.getCustomExchangeRate().divide(new BigDecimal("10000"), 0, RoundingMode.DOWN).intValue());
        }
        if ("accelerated".equals(command.getExchangeRateType())) {
            return Math.max(1, defaultExchangeRate().divide(new BigDecimal("10000"), 0, RoundingMode.DOWN).intValue() + 5);
        }
        return Math.max(1, defaultExchangeRate().divide(new BigDecimal("10000"), 0, RoundingMode.DOWN).intValue());
    }

    private int defaultSpendRatioOffset(String value) {
        if ("20m_plus_2".equals(value)) {
            return 2;
        }
        if ("30m_plus_3".equals(value)) {
            return 3;
        }
        if ("40m_plus_4".equals(value)) {
            return 4;
        }
        if ("50m_plus_5".equals(value)) {
            return 5;
        }
        return 0;
    }

    private int safeBoxRatioDelta(Integer value) {
        if (Integer.valueOf(4).equals(value)) {
            return -1;
        }
        if (Integer.valueOf(3).equals(value)) {
            return 1;
        }
        if (Integer.valueOf(1).equals(value) || Integer.valueOf(2).equals(value)) {
            return 3;
        }
        return 0;
    }

    private int levelRatioDelta(Integer value) {
        if (Integer.valueOf(6).equals(value)) {
            return 1;
        }
        if (Integer.valueOf(5).equals(value)) {
            return 2;
        }
        if (Integer.valueOf(4).equals(value)) {
            return 3;
        }
        return 0;
    }

    private int hafCurrencyRatioDelta(Long value) {
        if (value == null) {
            return 0;
        }
        if (value >= 1000L) {
            return 4;
        }
        if (value >= 700L) {
            return 3;
        }
        if (value >= 500L) {
            return 2;
        }
        if (value >= 300L) {
            return 1;
        }
        return 0;
    }

    private boolean hasEffectiveKnifeSkin(List<String> values) {
        for (String value : normalizeTextList(values)) {
            if (RENTAL_EFFECTIVE_KNIFE_SKINS.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private EstimateSnapshot estimate(PublishCommand command) {
        int levelPart = command.getAccountLevel() * 10;
        int hafPart = command.getHafCurrency().intValue() / 1000;
        int safeBoxPart = command.getSafeBoxLevel() * 50;
        int operatorPart = normalizeTextList(command.getOperators()).size() * 40;
        int weaponSkinPart = normalizeTextList(command.getWeaponSkins()).size() * 60;
        int knifePart = normalizeTextList(command.getKnifeSkins()).size() * 70;
        int redSkinPart = normalizeTextList(command.getRedSkins()).size() * 55;
        int goldSkinPart = normalizeTextList(command.getGoldSkins()).size() * 50;
        int kdPart = command.getSecretKd().multiply(new BigDecimal("12")).setScale(0, RoundingMode.DOWN).intValue();
        int rankPart = rankBonus(command.getRankName());
        int total = levelPart + hafPart + safeBoxPart + operatorPart + weaponSkinPart + knifePart + redSkinPart + goldSkinPart + kdPart + rankPart;
        Map<String, Integer> detail = new LinkedHashMap<String, Integer>();
        detail.put("等级估值", levelPart);
        detail.put("哈夫币估值", hafPart);
        detail.put("安全箱估值", safeBoxPart);
        detail.put("干员估值", operatorPart);
        detail.put("武器皮肤估值", weaponSkinPart);
        detail.put("特殊刀皮估值", knifePart);
        detail.put("人物红皮估值", redSkinPart);
        detail.put("人物金皮估值", goldSkinPart);
        detail.put("绝密KD估值", kdPart);
        detail.put("段位估值", rankPart);
        String estimateDetail = detail.entrySet().stream()
            .map(entry -> entry.getKey() + "+" + entry.getValue())
            .collect(Collectors.joining("，")) + "，综合建议 " + total + " 元";
        return new EstimateSnapshot(new BigDecimal(total).setScale(2, RoundingMode.HALF_UP), estimateDetail);
    }

    private int rankBonus(String rankName) {
        if ("summit".equals(rankName) || "legend".equals(rankName)) {
            return 600;
        }
        if ("blackhawk".equals(rankName) || "master".equals(rankName)) {
            return 420;
        }
        if ("diamond".equals(rankName)) {
            return 260;
        }
        if ("platinum".equals(rankName)) {
            return 160;
        }
        if ("gold".equals(rankName)) {
            return 90;
        }
        return 0;
    }

    private List<String> normalizeTextList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return Collections.emptyList();
        }
        return values.stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .distinct()
            .collect(Collectors.toList());
    }

    private List<ExtraItemSnapshot> buildExtraItemSnapshots(List<ExtraItemCommand> extraItems) {
        if (extraItems == null) {
            return Collections.emptyList();
        }
        List<ExtraItemSnapshot> snapshots = new ArrayList<ExtraItemSnapshot>();
        for (ExtraItemCommand item : extraItems) {
            ExtraItemMeta meta = findExtraItemMeta(item.getCode());
            if (meta == null) {
                continue;
            }
            BigDecimal totalPrice = "charge".equals(item.getChargeMode())
                ? meta.getUnitPrice().multiply(new BigDecimal(item.getCount())).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            snapshots.add(new ExtraItemSnapshot(item.getCode(), meta.getLabel(), item.getCount(), item.getChargeMode(), meta.getUnitPrice(), totalPrice));
        }
        return snapshots;
    }

    private ExtraItemMeta findExtraItemMeta(String code) {
        if (!StringUtils.hasText(code)) {
            return null;
        }
        for (ExtraItemMeta item : EXTRA_ITEMS) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        return null;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "发布信息序列化失败");
        }
    }

    private boolean containsValue(List<OptionItem> source, String value) {
        if (!StringUtils.hasText(value)) {
            return false;
        }
        for (OptionItem item : source) {
            if (item.getValue().equals(value)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, String> buildLegacyRankMappings() {
        Map<String, String> mappings = new HashMap<String, String>();
        mappings.put("legend", "summit");
        mappings.put("master", "blackhawk");
        return mappings;
    }

    private String normalizeRankName(String rankName) {
        if (!StringUtils.hasText(rankName)) {
            return rankName;
        }
        String normalized = rankName.trim().toLowerCase(java.util.Locale.ROOT);
        String mapped = LEGACY_RANK_MAPPINGS.get(normalized);
        return mapped != null ? mapped : normalized;
    }

    private BigDecimal scaleNullable(BigDecimal value) {
        return value == null ? null : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String buildListingNo() {
        return "PUB" + LocalDateTime.now().format(LISTING_NO_FORMATTER)
            + String.format("%04d", ThreadLocalRandom.current().nextInt(1000, 9999));
    }

    private String normalizeNullableText(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private AccountListingDO loadOwnedListing(AuthPrincipal principal, String listingNo) {
        long startAt = System.currentTimeMillis();
        AccountListingDO listing = accountListingMapper.selectOne(
            Wrappers.<AccountListingDO>lambdaQuery()
                .eq(AccountListingDO::getListingNo, listingNo)
                .eq(AccountListingDO::getSellerUserId, principal.getUserId())
                .last("LIMIT 1")
        );
        log.info("mysql query success target=account_listing_by_listing_no_owner costMs={} listingNo={} userId={} hit={}",
            System.currentTimeMillis() - startAt, listingNo, principal.getUserId(), listing != null);
        if (listing == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "未找到对应的发布记录");
        }
        return listing;
    }

    private boolean canEdit(String status) {
        return "PENDING_REVIEW".equals(status) || "REJECTED".equals(status) || "OFFLINE".equals(status);
    }

    private void ensureListingCanBePublished(AccountListingDO listing) {
        Long activeTradeCount = tradeOrderMapper.selectCount(
            Wrappers.<TradeOrderDO>lambdaQuery()
                .eq(TradeOrderDO::getListingNo, listing.getListingNo())
                .in(TradeOrderDO::getStatus, Arrays.asList("PENDING_PAYMENT", "WAITING_TRADE", "IN_PROGRESS", "COMPLETED", "AFTER_SALE"))
        );
        if (activeTradeCount != null && activeTradeCount > 0) {
            log.warn("listing publish rejected active trade listingNo={} activeTradeCount={}", listing.getListingNo(), activeTradeCount);
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "该账号已有交易订单，不能重新上架");
        }
    }

    private MyListingListItem buildMyListingListItem(AccountListingDO listing) {
        return new MyListingListItem(
            listing.getListingNo(),
            listing.getTitle(),
            listing.getPrice(),
            listing.getStatus(),
            formatListingStatus(listing.getStatus()),
            formatReviewProgress(listing.getStatus()),
            defaultNumber(listing.getViewCount()),
            defaultNumber(listing.getFavoriteCount()),
            defaultNumber(listing.getSalesCount()),
            defaultNumber(listing.getSalesCount()) > 0 ? "已成交 " + defaultNumber(listing.getSalesCount()) + " 单" : "未成交",
            listing.getRejectionReason(),
            listing.getUpdatedAt(),
            canEdit(listing.getStatus()),
            "PUBLISHED".equals(listing.getStatus()),
            "REJECTED".equals(listing.getStatus())
        );
    }

    private PublishCommand buildDraft(AccountListingDO listing, PublishAttributesSnapshot attributes) {
        PublishCommand draft = new PublishCommand();
        draft.setProvinceCode(listing.getProvinceCode());
        draft.setCityCode(listing.getCityCode());
        draft.setDeliveryMethod(listing.getDeliveryMethod());
        draft.setAlwaysOnline(Boolean.TRUE.equals(listing.getAlwaysOnline()));
        draft.setDeliveryStartHour(attributes.getDeliveryStartHour());
        draft.setDeliveryEndHour(attributes.getDeliveryEndHour());
        draft.setAccountLevel(listing.getAccountLevel());
        draft.setRankName(normalizeRankName(listing.getRankName()));
        draft.setSafeBoxLevel(listing.getSafeBoxLevel());
        draft.setStaminaLevel(attributes.getStaminaLevel());
        draft.setCarryLevel(attributes.getCarryLevel());
        draft.setDiveLevel(attributes.getDiveLevel());
        draft.setBanRecord(attributes.getBanRecord());
        draft.setPunishmentImageKey(attributes.getPunishmentImageKey());
        draft.setFaceOwned(attributes.getFaceOwned());
        draft.setUnlockSaeed(attributes.getUnlockSaeed());
        draft.setHafCurrency(listing.getHafCurrency());
        draft.setKnifeSkins(copyTexts(attributes.getKnifeSkins()));
        draft.setRedSkins(copyTexts(attributes.getRedSkins()));
        draft.setTitle(listing.getTitle());
        draft.setDescription(listing.getDescription());
        draft.setOperatorCount(listing.getOperatorCount());
        draft.setOperators(copyTexts(readStringList(listing.getOperatorsJson())));
        draft.setWeapons(copyTexts(readStringList(listing.getWeaponsJson())));
        draft.setWeaponSkins(copyTexts(readStringList(listing.getWeaponSkinsJson())));
        draft.setGoldSkins(copyTexts(attributes.getGoldSkins()));
        draft.setSecretKd(attributes.getSecretKd());
        draft.setDefaultSpend(attributes.getDefaultSpend());
        draft.setRentalDays(attributes.getRentalDays());
        draft.setExchangeRateType(attributes.getExchangeRateType());
        draft.setCustomExchangeRate(attributes.getCustomExchangeRate());
        draft.setCompensationPlan(attributes.getCompensationPlan());
        draft.setRemarks(attributes.getRemarks());
        draft.setAgreements(copyTexts(attributes.getAgreements()));
        draft.setExtraItems(buildExtraItemCommands(attributes.getExtraItems()));
        draft.setOtherItems(listing.getOtherItems());
        draft.setImageKeys(copyTexts(readStringList(listing.getImageKeysJson())));
        draft.setVideoKey(listing.getVideoKey());
        draft.setPrice(listing.getPrice());
        draft.setDeposit(attributes.getDeposit());
        draft.setNegotiable(Boolean.TRUE.equals(listing.getNegotiable()));
        draft.setModCodes(copyTexts(readStringList(listing.getModCodesJson())));
        return draft;
    }

    private List<ReviewRecord> buildReviewRecords(AccountListingDO listing) {
        List<ReviewRecord> records = new ArrayList<ReviewRecord>();
        if (listing.getSubmittedAt() != null) {
            records.add(new ReviewRecord("提交发布", "已提交待平台处理", listing.getSubmittedAt(), null));
        }
        if ("REJECTED".equals(listing.getStatus())) {
            records.add(new ReviewRecord("审核驳回", "平台审核未通过", listing.getUpdatedAt(), listing.getRejectionReason()));
        } else if ("PUBLISHED".equals(listing.getStatus())) {
            records.add(new ReviewRecord("审核通过", "账号已上架展示", listing.getPublishedAt() != null ? listing.getPublishedAt() : listing.getUpdatedAt(), null));
        } else if ("OFFLINE".equals(listing.getStatus())) {
            records.add(new ReviewRecord("账号下架", "卖家已手动下架", listing.getUpdatedAt(), null));
        } else if ("PENDING_REVIEW".equals(listing.getStatus())) {
            records.add(new ReviewRecord("审核中", "当前处于平台人工审核阶段", listing.getUpdatedAt() != null ? listing.getUpdatedAt() : listing.getSubmittedAt(), null));
        }
        return records;
    }

    private List<TradeRecord> buildTradeRecords(AuthPrincipal principal, String listingNo) {
        long startAt = System.currentTimeMillis();
        List<TradeOrderDO> orders = tradeOrderMapper.selectList(
            Wrappers.<TradeOrderDO>lambdaQuery()
                .eq(TradeOrderDO::getListingNo, listingNo)
                .eq(TradeOrderDO::getSellerUserId, principal.getUserId())
                .orderByDesc(TradeOrderDO::getCreatedAt)
        );
        log.info("mysql query success target=trade_order_by_listing_no_owner costMs={} listingNo={} userId={} count={}",
            System.currentTimeMillis() - startAt, listingNo, principal.getUserId(), orders.size());
        return orders.stream()
            .map(order -> new TradeRecord(
                order.getOrderNo(),
                order.getBuyerNickname(),
                order.getStatus(),
                formatOrderStatus(order.getStatus()),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getCompletedAt()
            ))
            .collect(Collectors.toList());
    }

    private List<ExtraItemCommand> buildExtraItemCommands(List<ExtraItemSnapshot> items) {
        if (items == null || items.isEmpty()) {
            return new ArrayList<ExtraItemCommand>();
        }
        List<ExtraItemCommand> commands = new ArrayList<ExtraItemCommand>();
        for (ExtraItemSnapshot item : items) {
            ExtraItemCommand command = new ExtraItemCommand();
            command.setCode(item.getCode());
            command.setCount(item.getCount());
            command.setChargeMode(item.getChargeMode());
            commands.add(command);
        }
        return commands;
    }

    private List<AssetItem> buildAssetItems(List<String> objectKeys) {
        if (objectKeys == null || objectKeys.isEmpty()) {
            return Collections.emptyList();
        }
        List<AssetItem> items = new ArrayList<AssetItem>();
        for (String objectKey : objectKeys) {
            AssetItem item = buildAssetItem(objectKey);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    private AssetItem buildAssetItem(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return null;
        }
        return new AssetItem(objectKey, ossStorageService.previewUrl(objectKey), extractFilename(objectKey));
    }

    private List<String> copyTexts(List<String> values) {
        return new ArrayList<String>(normalizeTextList(values));
    }

    private List<String> readStringList(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(raw, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (Exception exception) {
            return Collections.emptyList();
        }
    }

    private PublishAttributesSnapshot readPublishAttributes(String raw) {
        if (!StringUtils.hasText(raw)) {
            return new PublishAttributesSnapshot();
        }
        try {
            return objectMapper.readValue(raw, PublishAttributesSnapshot.class);
        } catch (Exception exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "发布属性解析失败");
        }
    }

    private String formatListingStatus(String status) {
        if ("PUBLISHED".equals(status)) {
            return "已发布";
        }
        if ("PENDING_REVIEW".equals(status)) {
            return "待审核";
        }
        if ("REJECTED".equals(status)) {
            return "已驳回";
        }
        if ("OFFLINE".equals(status)) {
            return "已下架";
        }
        return "未知状态";
    }

    private String formatReviewProgress(String status) {
        if ("PUBLISHED".equals(status)) {
            return "已通过";
        }
        if ("PENDING_REVIEW".equals(status)) {
            return "待审核";
        }
        if ("REJECTED".equals(status)) {
            return "已驳回";
        }
        if ("OFFLINE".equals(status)) {
            return "已通过";
        }
        return "-";
    }

    private String formatOrderStatus(String status) {
        if ("PENDING_PAYMENT".equals(status)) {
            return "待付款";
        }
	        if ("PENDING_TRADE".equals(status) || "WAITING_TRADE".equals(status)) {
	            return "交易中";
	        }
        if ("IN_PROGRESS".equals(status)) {
            return "交易中";
        }
        if ("COMPLETED".equals(status)) {
            return "已完成";
        }
        if ("AFTER_SALE".equals(status)) {
            return "售后中";
        }
        if ("CLOSED".equals(status)) {
            return "已关闭";
        }
        return status;
    }

    private String extractFilename(String objectKey) {
        int index = objectKey.lastIndexOf('/');
        return index >= 0 ? objectKey.substring(index + 1) : objectKey;
    }

    private int defaultNumber(Integer value) {
        return value == null ? 0 : value;
    }

    private static List<OptionItem> buildNumberOptions(int start, int end, String suffix) {
        List<OptionItem> result = new ArrayList<OptionItem>();
        for (int index = start; index <= end; index++) {
            result.add(new OptionItem(String.valueOf(index), index + suffix));
        }
        return result;
    }

    public static class PublishMetaResult {
        private final SellerContext sellerContext;
        private final List<ChinaCityCatalog.ProvinceNode> regions;
        private final List<OptionItem> deliveryMethods;
        private final List<OptionItem> hourOptions;
        private final List<OptionItem> ranks;
        private final List<OptionItem> safeBoxLevels;
        private final List<OptionItem> staminaLevels;
        private final List<OptionItem> carryLevels;
        private final List<OptionItem> diveLevels;
        private final List<OptionItem> operators;
        private final List<OptionItem> weapons;
        private final List<OptionItem> knifeSkins;
        private final List<OptionItem> redSkins;
        private final List<OptionItem> weaponSkinCatalog;
        private final List<OptionItem> goldSkins;
        private final List<OptionItem> defaultSpendOptions;
        private final List<OptionItem> exchangeRateOptions;
        private final List<OptionItem> compensationPlans;
        private final List<OptionItem> agreementOptions;
        private final List<ExtraItemMeta> extraItems;
	        private final List<OptionItem> modCodes;
	        private final BigDecimal defaultExchangeRate;
	        private final BigDecimal personalSellerCommissionRate;
	        private final List<String> notices;

        public PublishMetaResult(
            SellerContext sellerContext,
            List<ChinaCityCatalog.ProvinceNode> regions,
            List<OptionItem> deliveryMethods,
            List<OptionItem> hourOptions,
            List<OptionItem> ranks,
            List<OptionItem> safeBoxLevels,
            List<OptionItem> staminaLevels,
            List<OptionItem> carryLevels,
            List<OptionItem> diveLevels,
            List<OptionItem> operators,
            List<OptionItem> weapons,
            List<OptionItem> knifeSkins,
            List<OptionItem> redSkins,
            List<OptionItem> weaponSkinCatalog,
            List<OptionItem> goldSkins,
            List<OptionItem> defaultSpendOptions,
            List<OptionItem> exchangeRateOptions,
            List<OptionItem> compensationPlans,
            List<OptionItem> agreementOptions,
	            List<ExtraItemMeta> extraItems,
	            List<OptionItem> modCodes,
	            BigDecimal defaultExchangeRate,
	            BigDecimal personalSellerCommissionRate,
	            List<String> notices
	        ) {
            this.sellerContext = sellerContext;
            this.regions = regions;
            this.deliveryMethods = deliveryMethods;
            this.hourOptions = hourOptions;
            this.ranks = ranks;
            this.safeBoxLevels = safeBoxLevels;
            this.staminaLevels = staminaLevels;
            this.carryLevels = carryLevels;
            this.diveLevels = diveLevels;
            this.operators = operators;
            this.weapons = weapons;
            this.knifeSkins = knifeSkins;
            this.redSkins = redSkins;
            this.weaponSkinCatalog = weaponSkinCatalog;
            this.goldSkins = goldSkins;
            this.defaultSpendOptions = defaultSpendOptions;
            this.exchangeRateOptions = exchangeRateOptions;
            this.compensationPlans = compensationPlans;
            this.agreementOptions = agreementOptions;
	            this.extraItems = extraItems;
	            this.modCodes = modCodes;
	            this.defaultExchangeRate = defaultExchangeRate;
	            this.personalSellerCommissionRate = personalSellerCommissionRate;
	            this.notices = notices;
	        }

        public SellerContext getSellerContext() {
            return sellerContext;
        }

        public List<ChinaCityCatalog.ProvinceNode> getRegions() {
            return regions;
        }

        public List<OptionItem> getDeliveryMethods() {
            return deliveryMethods;
        }

        public List<OptionItem> getHourOptions() {
            return hourOptions;
        }

        public List<OptionItem> getRanks() {
            return ranks;
        }

        public List<OptionItem> getSafeBoxLevels() {
            return safeBoxLevels;
        }

        public List<OptionItem> getStaminaLevels() {
            return staminaLevels;
        }

        public List<OptionItem> getCarryLevels() {
            return carryLevels;
        }

        public List<OptionItem> getDiveLevels() {
            return diveLevels;
        }

        public List<OptionItem> getOperators() {
            return operators;
        }

        public List<OptionItem> getWeapons() {
            return weapons;
        }

        public List<OptionItem> getKnifeSkins() {
            return knifeSkins;
        }

        public List<OptionItem> getRedSkins() {
            return redSkins;
        }

        public List<OptionItem> getWeaponSkinCatalog() {
            return weaponSkinCatalog;
        }

        public List<OptionItem> getGoldSkins() {
            return goldSkins;
        }

        public List<OptionItem> getDefaultSpendOptions() {
            return defaultSpendOptions;
        }

        public List<OptionItem> getExchangeRateOptions() {
            return exchangeRateOptions;
        }

        public List<OptionItem> getCompensationPlans() {
            return compensationPlans;
        }

        public List<OptionItem> getAgreementOptions() {
            return agreementOptions;
        }

        public List<ExtraItemMeta> getExtraItems() {
            return extraItems;
        }

        public List<OptionItem> getModCodes() {
            return modCodes;
        }

	        public BigDecimal getDefaultExchangeRate() {
	            return defaultExchangeRate;
	        }

	        public BigDecimal getPersonalSellerCommissionRate() {
	            return personalSellerCommissionRate;
	        }

	        public List<String> getNotices() {
            return notices;
        }
    }

    public static class PublishSubmitResult {
        private final String listingNo;
        private final String status;
        private final String message;
        private final BigDecimal suggestedPrice;
        private final String estimateDetail;

        public PublishSubmitResult(String listingNo, String status, String message, BigDecimal suggestedPrice, String estimateDetail) {
            this.listingNo = listingNo;
            this.status = status;
            this.message = message;
            this.suggestedPrice = suggestedPrice;
            this.estimateDetail = estimateDetail;
        }

        public String getListingNo() {
            return listingNo;
        }

        public String getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public BigDecimal getSuggestedPrice() {
            return suggestedPrice;
        }

        public String getEstimateDetail() {
            return estimateDetail;
        }
    }

    public static class MyListingListResult {
        private final int total;
        private final List<MyListingListItem> rows;

        public MyListingListResult(int total, List<MyListingListItem> rows) {
            this.total = total;
            this.rows = rows;
        }

        public int getTotal() {
            return total;
        }

        public List<MyListingListItem> getRows() {
            return rows;
        }
    }

    public static class MyListingListItem {
        private final String listingNo;
        private final String title;
        private final BigDecimal price;
        private final String status;
        private final String statusLabel;
        private final String reviewProgress;
        private final int viewCount;
        private final int favoriteCount;
        private final int salesCount;
        private final String salesStatus;
        private final String rejectionReason;
        private final LocalDateTime updatedAt;
        private final boolean canEdit;
        private final boolean canWithdraw;
        private final boolean canResubmit;

        public MyListingListItem(
            String listingNo,
            String title,
            BigDecimal price,
            String status,
            String statusLabel,
            String reviewProgress,
            int viewCount,
            int favoriteCount,
            int salesCount,
            String salesStatus,
            String rejectionReason,
            LocalDateTime updatedAt,
            boolean canEdit,
            boolean canWithdraw,
            boolean canResubmit
        ) {
            this.listingNo = listingNo;
            this.title = title;
            this.price = price;
            this.status = status;
            this.statusLabel = statusLabel;
            this.reviewProgress = reviewProgress;
            this.viewCount = viewCount;
            this.favoriteCount = favoriteCount;
            this.salesCount = salesCount;
            this.salesStatus = salesStatus;
            this.rejectionReason = rejectionReason;
            this.updatedAt = updatedAt;
            this.canEdit = canEdit;
            this.canWithdraw = canWithdraw;
            this.canResubmit = canResubmit;
        }

        public String getListingNo() { return listingNo; }
        public String getTitle() { return title; }
        public BigDecimal getPrice() { return price; }
        public String getStatus() { return status; }
        public String getStatusLabel() { return statusLabel; }
        public String getReviewProgress() { return reviewProgress; }
        public int getViewCount() { return viewCount; }
        public int getFavoriteCount() { return favoriteCount; }
        public int getSalesCount() { return salesCount; }
        public String getSalesStatus() { return salesStatus; }
        public String getRejectionReason() { return rejectionReason; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public boolean isCanEdit() { return canEdit; }
        public boolean isCanWithdraw() { return canWithdraw; }
        public boolean isCanResubmit() { return canResubmit; }
    }

    public static class MyListingDetail {
        private final MyListingListItem summary;
        private final PublishCommand draft;
        private final List<AssetItem> images;
        private final AssetItem video;
        private final AssetItem punishmentImage;
        private final List<ReviewRecord> reviewRecords;
        private final List<TradeRecord> tradeRecords;

        public MyListingDetail(
            MyListingListItem summary,
            PublishCommand draft,
            List<AssetItem> images,
            AssetItem video,
            AssetItem punishmentImage,
            List<ReviewRecord> reviewRecords,
            List<TradeRecord> tradeRecords
        ) {
            this.summary = summary;
            this.draft = draft;
            this.images = images;
            this.video = video;
            this.punishmentImage = punishmentImage;
            this.reviewRecords = reviewRecords;
            this.tradeRecords = tradeRecords;
        }

        public MyListingListItem getSummary() { return summary; }
        public PublishCommand getDraft() { return draft; }
        public List<AssetItem> getImages() { return images; }
        public AssetItem getVideo() { return video; }
        public AssetItem getPunishmentImage() { return punishmentImage; }
        public List<ReviewRecord> getReviewRecords() { return reviewRecords; }
        public List<TradeRecord> getTradeRecords() { return tradeRecords; }
    }

    public static class AssetItem {
        private final String objectKey;
        private final String previewUrl;
        private final String filename;

        public AssetItem(String objectKey, String previewUrl, String filename) {
            this.objectKey = objectKey;
            this.previewUrl = previewUrl;
            this.filename = filename;
        }

        public String getObjectKey() { return objectKey; }
        public String getPreviewUrl() { return previewUrl; }
        public String getFilename() { return filename; }
    }

    public static class ReviewRecord {
        private final String title;
        private final String result;
        private final LocalDateTime createdAt;
        private final String note;

        public ReviewRecord(String title, String result, LocalDateTime createdAt, String note) {
            this.title = title;
            this.result = result;
            this.createdAt = createdAt;
            this.note = note;
        }

        public String getTitle() { return title; }
        public String getResult() { return result; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public String getNote() { return note; }
    }

    public static class TradeRecord {
        private final String orderNo;
        private final String buyerNickname;
        private final String status;
        private final String statusLabel;
        private final BigDecimal totalAmount;
        private final LocalDateTime createdAt;
        private final LocalDateTime completedAt;

        public TradeRecord(
            String orderNo,
            String buyerNickname,
            String status,
            String statusLabel,
            BigDecimal totalAmount,
            LocalDateTime createdAt,
            LocalDateTime completedAt
        ) {
            this.orderNo = orderNo;
            this.buyerNickname = buyerNickname;
            this.status = status;
            this.statusLabel = statusLabel;
            this.totalAmount = totalAmount;
            this.createdAt = createdAt;
            this.completedAt = completedAt;
        }

        public String getOrderNo() { return orderNo; }
        public String getBuyerNickname() { return buyerNickname; }
        public String getStatus() { return status; }
        public String getStatusLabel() { return statusLabel; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public LocalDateTime getCompletedAt() { return completedAt; }
    }

    public static class ActionResult {
        private final String listingNo;
        private final String status;
        private final String message;

        public ActionResult(String listingNo, String status, String message) {
            this.listingNo = listingNo;
            this.status = status;
            this.message = message;
        }

        public String getListingNo() { return listingNo; }
        public String getStatus() { return status; }
        public String getMessage() { return message; }
    }

    public static class PublishCommand {
        private String provinceCode;
        private String cityCode;
        private String deliveryMethod;
        private Boolean alwaysOnline;
        private Integer deliveryStartHour;
        private Integer deliveryEndHour;
        private Integer accountLevel;
        private String rankName;
        private Integer safeBoxLevel;
        private Integer staminaLevel;
        private Integer carryLevel;
        private Integer diveLevel;
        private Boolean banRecord;
        private String punishmentImageKey;
        private Boolean faceOwned;
        private Boolean unlockSaeed;
        private Long hafCurrency;
        private List<String> knifeSkins = new ArrayList<String>();
        private List<String> redSkins = new ArrayList<String>();
        private String title;
        private String description;
        private Integer operatorCount;
        private List<String> operators = new ArrayList<String>();
        private List<String> weapons = new ArrayList<String>();
        private List<String> weaponSkins = new ArrayList<String>();
        private List<String> goldSkins = new ArrayList<String>();
        private BigDecimal secretKd;
        private String defaultSpend;
        private Integer rentalDays;
        private String exchangeRateType;
        private BigDecimal customExchangeRate;
        private String compensationPlan;
        private String remarks;
        private List<String> agreements = new ArrayList<String>();
        private List<ExtraItemCommand> extraItems = new ArrayList<ExtraItemCommand>();
        private String otherItems;
        private List<String> imageKeys = new ArrayList<String>();
        private String videoKey;
        private BigDecimal price;
        private BigDecimal deposit;
        private Boolean negotiable;
        private List<String> modCodes = new ArrayList<String>();

        public String getProvinceCode() {
            return provinceCode;
        }

        public void setProvinceCode(String provinceCode) {
            this.provinceCode = provinceCode;
        }

        public String getCityCode() {
            return cityCode;
        }

        public void setCityCode(String cityCode) {
            this.cityCode = cityCode;
        }

        public String getDeliveryMethod() {
            return deliveryMethod;
        }

        public void setDeliveryMethod(String deliveryMethod) {
            this.deliveryMethod = deliveryMethod;
        }

        public Boolean getAlwaysOnline() {
            return alwaysOnline;
        }

        public void setAlwaysOnline(Boolean alwaysOnline) {
            this.alwaysOnline = alwaysOnline;
        }

        public Integer getDeliveryStartHour() {
            return deliveryStartHour;
        }

        public void setDeliveryStartHour(Integer deliveryStartHour) {
            this.deliveryStartHour = deliveryStartHour;
        }

        public Integer getDeliveryEndHour() {
            return deliveryEndHour;
        }

        public void setDeliveryEndHour(Integer deliveryEndHour) {
            this.deliveryEndHour = deliveryEndHour;
        }

        public Integer getAccountLevel() {
            return accountLevel;
        }

        public void setAccountLevel(Integer accountLevel) {
            this.accountLevel = accountLevel;
        }

        public String getRankName() {
            return rankName;
        }

        public void setRankName(String rankName) {
            this.rankName = rankName;
        }

        public Integer getSafeBoxLevel() {
            return safeBoxLevel;
        }

        public void setSafeBoxLevel(Integer safeBoxLevel) {
            this.safeBoxLevel = safeBoxLevel;
        }

        public Integer getStaminaLevel() {
            return staminaLevel;
        }

        public void setStaminaLevel(Integer staminaLevel) {
            this.staminaLevel = staminaLevel;
        }

        public Integer getCarryLevel() {
            return carryLevel;
        }

        public void setCarryLevel(Integer carryLevel) {
            this.carryLevel = carryLevel;
        }

        public Integer getDiveLevel() {
            return diveLevel;
        }

        public void setDiveLevel(Integer diveLevel) {
            this.diveLevel = diveLevel;
        }

        public Boolean getBanRecord() {
            return banRecord;
        }

        public void setBanRecord(Boolean banRecord) {
            this.banRecord = banRecord;
        }

        public String getPunishmentImageKey() {
            return punishmentImageKey;
        }

        public void setPunishmentImageKey(String punishmentImageKey) {
            this.punishmentImageKey = punishmentImageKey;
        }

        public Boolean getFaceOwned() {
            return faceOwned;
        }

        public void setFaceOwned(Boolean faceOwned) {
            this.faceOwned = faceOwned;
        }

        public Boolean getUnlockSaeed() {
            return unlockSaeed;
        }

        public void setUnlockSaeed(Boolean unlockSaeed) {
            this.unlockSaeed = unlockSaeed;
        }

        public Long getHafCurrency() {
            return hafCurrency;
        }

        public void setHafCurrency(Long hafCurrency) {
            this.hafCurrency = hafCurrency;
        }

        public List<String> getKnifeSkins() {
            return knifeSkins;
        }

        public void setKnifeSkins(List<String> knifeSkins) {
            this.knifeSkins = knifeSkins;
        }

        public List<String> getRedSkins() {
            return redSkins;
        }

        public void setRedSkins(List<String> redSkins) {
            this.redSkins = redSkins;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Integer getOperatorCount() {
            return operatorCount;
        }

        public void setOperatorCount(Integer operatorCount) {
            this.operatorCount = operatorCount;
        }

        public List<String> getOperators() {
            return operators;
        }

        public void setOperators(List<String> operators) {
            this.operators = operators;
        }

        public List<String> getWeapons() {
            return weapons;
        }

        public void setWeapons(List<String> weapons) {
            this.weapons = weapons;
        }

        public List<String> getWeaponSkins() {
            return weaponSkins;
        }

        public void setWeaponSkins(List<String> weaponSkins) {
            this.weaponSkins = weaponSkins;
        }

        public List<String> getGoldSkins() {
            return goldSkins;
        }

        public void setGoldSkins(List<String> goldSkins) {
            this.goldSkins = goldSkins;
        }

        public BigDecimal getSecretKd() {
            return secretKd;
        }

        public void setSecretKd(BigDecimal secretKd) {
            this.secretKd = secretKd;
        }

        public String getDefaultSpend() {
            return defaultSpend;
        }

        public void setDefaultSpend(String defaultSpend) {
            this.defaultSpend = defaultSpend;
        }

        public Integer getRentalDays() {
            return rentalDays;
        }

        public void setRentalDays(Integer rentalDays) {
            this.rentalDays = rentalDays;
        }

        public String getExchangeRateType() {
            return exchangeRateType;
        }

        public void setExchangeRateType(String exchangeRateType) {
            this.exchangeRateType = exchangeRateType;
        }

        public BigDecimal getCustomExchangeRate() {
            return customExchangeRate;
        }

        public void setCustomExchangeRate(BigDecimal customExchangeRate) {
            this.customExchangeRate = customExchangeRate;
        }

        public String getCompensationPlan() {
            return compensationPlan;
        }

        public void setCompensationPlan(String compensationPlan) {
            this.compensationPlan = compensationPlan;
        }

        public String getRemarks() {
            return remarks;
        }

        public void setRemarks(String remarks) {
            this.remarks = remarks;
        }

        public List<String> getAgreements() {
            return agreements;
        }

        public void setAgreements(List<String> agreements) {
            this.agreements = agreements;
        }

        public List<ExtraItemCommand> getExtraItems() {
            return extraItems;
        }

        public void setExtraItems(List<ExtraItemCommand> extraItems) {
            this.extraItems = extraItems;
        }

        public String getOtherItems() {
            return otherItems;
        }

        public void setOtherItems(String otherItems) {
            this.otherItems = otherItems;
        }

        public List<String> getImageKeys() {
            return imageKeys;
        }

        public void setImageKeys(List<String> imageKeys) {
            this.imageKeys = imageKeys;
        }

        public String getVideoKey() {
            return videoKey;
        }

        public void setVideoKey(String videoKey) {
            this.videoKey = videoKey;
        }

        public BigDecimal getPrice() {
            return price;
        }

        public void setPrice(BigDecimal price) {
            this.price = price;
        }

        public BigDecimal getDeposit() {
            return deposit;
        }

        public void setDeposit(BigDecimal deposit) {
            this.deposit = deposit;
        }

        public Boolean getNegotiable() {
            return negotiable;
        }

        public void setNegotiable(Boolean negotiable) {
            this.negotiable = negotiable;
        }

        public List<String> getModCodes() {
            return modCodes;
        }

        public void setModCodes(List<String> modCodes) {
            this.modCodes = modCodes;
        }
    }

    public static class ExtraItemCommand {
        private String code;
        private Integer count;
        private String chargeMode;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(Integer count) {
            this.count = count;
        }

        public String getChargeMode() {
            return chargeMode;
        }

        public void setChargeMode(String chargeMode) {
            this.chargeMode = chargeMode;
        }
    }

    public static class ExtraItemMeta {
        private final String code;
        private final String label;
        private final String unitLabel;
        private final BigDecimal unitPrice;

        public ExtraItemMeta(String code, String label, String unitLabel, BigDecimal unitPrice) {
            this.code = code;
            this.label = label;
            this.unitLabel = unitLabel;
            this.unitPrice = unitPrice;
        }

        public String getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        public String getUnitLabel() {
            return unitLabel;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }
    }

    public static class OptionItem {
        private final String value;
        private final String label;

        public OptionItem(String value, String label) {
            this.value = value;
            this.label = label;
        }

        public String getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }
    }

    public static class SellerContext {
        private final String sellerType;
        private final String sellerLabel;
        private final String reviewStrategy;
        private final String reviewStrategyLabel;
        private final String studioName;
        private final boolean sellerTypeLocked;

        public SellerContext(
            String sellerType,
            String sellerLabel,
            String reviewStrategy,
            String reviewStrategyLabel,
            String studioName,
            boolean sellerTypeLocked
        ) {
            this.sellerType = sellerType;
            this.sellerLabel = sellerLabel;
            this.reviewStrategy = reviewStrategy;
            this.reviewStrategyLabel = reviewStrategyLabel;
            this.studioName = studioName;
            this.sellerTypeLocked = sellerTypeLocked;
        }

        public String getSellerType() {
            return sellerType;
        }

        public String getSellerLabel() {
            return sellerLabel;
        }

        public String getReviewStrategy() {
            return reviewStrategy;
        }

        public String getReviewStrategyLabel() {
            return reviewStrategyLabel;
        }

        public String getStudioName() {
            return studioName;
        }

        public boolean isSellerTypeLocked() {
            return sellerTypeLocked;
        }

        public boolean isDirectPublish() {
            return "DIRECT_PUBLISH".equals(reviewStrategy);
        }
    }

    private static class EstimateSnapshot {
        private final BigDecimal suggestedPrice;
        private final String estimateDetail;

        private EstimateSnapshot(BigDecimal suggestedPrice, String estimateDetail) {
            this.suggestedPrice = suggestedPrice;
            this.estimateDetail = estimateDetail;
        }

        public BigDecimal getSuggestedPrice() {
            return suggestedPrice;
        }

        public String getEstimateDetail() {
            return estimateDetail;
        }
    }

    private static class PublishAttributesSnapshot {
        private Integer deliveryStartHour;
        private Integer deliveryEndHour;
        private Integer staminaLevel;
        private Integer carryLevel;
        private Integer diveLevel;
        private Boolean banRecord;
        private String punishmentImageKey;
        private Boolean faceOwned;
        private Boolean unlockSaeed;
        private List<String> knifeSkins;
        private List<String> redSkins;
        private List<String> goldSkins;
        private BigDecimal secretKd;
        private String defaultSpend;
        private Integer rentalDays;
        private String exchangeRateType;
        private BigDecimal customExchangeRate;
        private String compensationPlan;
        private BigDecimal deposit;
        private String remarks;
        private List<String> agreements;
        private List<ExtraItemSnapshot> extraItems;

        private PublishAttributesSnapshot() {
        }

        private PublishAttributesSnapshot(
            Integer deliveryStartHour,
            Integer deliveryEndHour,
            Integer staminaLevel,
            Integer carryLevel,
            Integer diveLevel,
            Boolean banRecord,
            String punishmentImageKey,
            Boolean faceOwned,
            Boolean unlockSaeed,
            List<String> knifeSkins,
            List<String> redSkins,
            List<String> goldSkins,
            BigDecimal secretKd,
            String defaultSpend,
            Integer rentalDays,
            String exchangeRateType,
            BigDecimal customExchangeRate,
            String compensationPlan,
            BigDecimal deposit,
            String remarks,
            List<String> agreements,
            List<ExtraItemSnapshot> extraItems
        ) {
            this.deliveryStartHour = deliveryStartHour;
            this.deliveryEndHour = deliveryEndHour;
            this.staminaLevel = staminaLevel;
            this.carryLevel = carryLevel;
            this.diveLevel = diveLevel;
            this.banRecord = banRecord;
            this.punishmentImageKey = punishmentImageKey;
            this.faceOwned = faceOwned;
            this.unlockSaeed = unlockSaeed;
            this.knifeSkins = knifeSkins;
            this.redSkins = redSkins;
            this.goldSkins = goldSkins;
            this.secretKd = secretKd;
            this.defaultSpend = defaultSpend;
            this.rentalDays = rentalDays;
            this.exchangeRateType = exchangeRateType;
            this.customExchangeRate = customExchangeRate;
            this.compensationPlan = compensationPlan;
            this.deposit = deposit;
            this.remarks = remarks;
            this.agreements = agreements;
            this.extraItems = extraItems;
        }

        public Integer getDeliveryStartHour() {
            return deliveryStartHour;
        }

        public Integer getDeliveryEndHour() {
            return deliveryEndHour;
        }

        public Integer getStaminaLevel() {
            return staminaLevel;
        }

        public Integer getCarryLevel() {
            return carryLevel;
        }

        public Integer getDiveLevel() {
            return diveLevel;
        }

        public Boolean getBanRecord() {
            return banRecord;
        }

        public String getPunishmentImageKey() {
            return punishmentImageKey;
        }

        public Boolean getFaceOwned() {
            return faceOwned;
        }

        public Boolean getUnlockSaeed() {
            return unlockSaeed;
        }

        public List<String> getKnifeSkins() {
            return knifeSkins;
        }

        public List<String> getRedSkins() {
            return redSkins;
        }

        public List<String> getGoldSkins() {
            return goldSkins;
        }

        public BigDecimal getSecretKd() {
            return secretKd;
        }

        public String getDefaultSpend() {
            return defaultSpend;
        }

        public Integer getRentalDays() {
            return rentalDays;
        }

        public void setRentalDays(Integer rentalDays) {
            this.rentalDays = rentalDays;
        }

        public String getExchangeRateType() {
            return exchangeRateType;
        }

        public BigDecimal getCustomExchangeRate() {
            return customExchangeRate;
        }

        public void setCustomExchangeRate(BigDecimal customExchangeRate) {
            this.customExchangeRate = customExchangeRate;
        }

        public String getCompensationPlan() {
            return compensationPlan;
        }

        public BigDecimal getDeposit() {
            return deposit;
        }

        public String getRemarks() {
            return remarks;
        }

        public List<String> getAgreements() {
            return agreements;
        }

        public List<ExtraItemSnapshot> getExtraItems() {
            return extraItems;
        }
    }

    private static class ExtraItemSnapshot {
        private String code;
        private String label;
        private Integer count;
        private String chargeMode;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;

        private ExtraItemSnapshot() {
        }

        private ExtraItemSnapshot(
            String code,
            String label,
            Integer count,
            String chargeMode,
            BigDecimal unitPrice,
            BigDecimal totalPrice
        ) {
            this.code = code;
            this.label = label;
            this.count = count;
            this.chargeMode = chargeMode;
            this.unitPrice = unitPrice;
            this.totalPrice = totalPrice;
        }

        public String getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        public Integer getCount() {
            return count;
        }

        public String getChargeMode() {
            return chargeMode;
        }

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public BigDecimal getTotalPrice() {
            return totalPrice;
        }
    }
}
