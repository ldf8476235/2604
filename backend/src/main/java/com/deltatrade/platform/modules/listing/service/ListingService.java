package com.deltatrade.platform.modules.listing.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.deltatrade.platform.common.auth.AuthPrincipal;
import com.deltatrade.platform.common.exception.BusinessException;
import com.deltatrade.platform.common.exception.ErrorCode;
import com.deltatrade.platform.modules.admin.service.AdminIntegrationConfigService;
import com.deltatrade.platform.modules.listing.mapper.AccountListingMapper;
import com.deltatrade.platform.modules.listing.mapper.ListingFavoriteMapper;
import com.deltatrade.platform.modules.listing.model.AccountListingDO;
import com.deltatrade.platform.modules.listing.model.ListingFavoriteDO;
import com.deltatrade.platform.modules.oss.service.OssStorageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ListingService {

    private static final Logger log = LoggerFactory.getLogger(ListingService.class);
    private static final DateTimeFormatter PUBLISH_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<List<String>>() {
    };
    private static final List<OptionItem> WEAPON_OPTIONS = Arrays.asList(
        new OptionItem("awm", "AWM"),
        new OptionItem("m4a1", "M416"),
        new OptionItem("akm", "AKM"),
        new OptionItem("vector", "Vector"),
        new OptionItem("scar", "SCAR"),
        new OptionItem("sr25", "SR-25")
    );
    private static final List<OptionItem> KNIFE_SKIN_OPTIONS = Arrays.asList(
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
    private static final List<OptionItem> RED_SKIN_OPTIONS = Arrays.asList(
        new OptionItem("凌霄戍卫", "凌霄戍卫"),
        new OptionItem("维什戴尔", "维什戴尔"),
        new OptionItem("蚀金玫瑰", "蚀金玫瑰"),
        new OptionItem("水墨云图", "水墨云图"),
        new OptionItem("午夜邮差", "午夜邮差"),
        new OptionItem("天际线", "天际线"),
        new OptionItem("飞虎", "飞虎"),
        new OptionItem("铁面判官", "铁面判官"),
        new OptionItem("蛟龙特战队", "蛟龙特战队")
    );
    private static final List<OptionItem> GOLD_SKIN_OPTIONS = Arrays.asList(
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
    private static final List<OptionItem> AWM_BULLET_RANGE_OPTIONS = Arrays.asList(
        new OptionItem("0_50", "0-50"),
        new OptionItem("50_100", "50-100"),
        new OptionItem("100_300", "100-300"),
        new OptionItem("300_plus", "300以上")
    );
    private static final List<OptionItem> DEPOSIT_RANGE_OPTIONS = Arrays.asList(
        new OptionItem("0_100", "0-100"),
        new OptionItem("100_300", "100-300"),
        new OptionItem("300_700", "300-700"),
        new OptionItem("700_1500", "700-1500"),
        new OptionItem("1500_plus", "1500以上")
    );
    private static final List<OptionItem> RANK_OPTIONS = Arrays.asList(
        new OptionItem("bronze", "青铜"),
        new OptionItem("silver", "白银"),
        new OptionItem("gold", "黄金"),
        new OptionItem("platinum", "铂金"),
        new OptionItem("diamond", "钻石"),
        new OptionItem("blackhawk", "黑鹰"),
        new OptionItem("summit", "巅峰")
    );
    private static final List<OptionItem> SAFE_BOX_OPTIONS = Arrays.asList(
        new OptionItem("1", "基础安全箱(1*2)"),
        new OptionItem("2", "进阶安全箱(2*2)"),
        new OptionItem("3", "高级安全箱(2*3)"),
        new OptionItem("4", "顶级安全箱(3*3)")
    );
    private static final List<OptionItem> LEVEL_OPTIONS = Arrays.asList(
        new OptionItem("1", "1级"),
        new OptionItem("2", "2级"),
        new OptionItem("3", "3级"),
        new OptionItem("4", "4级"),
        new OptionItem("5", "5级"),
        new OptionItem("6", "6级"),
        new OptionItem("7", "7级")
    );
    private static final List<OptionItem> DELIVERY_METHOD_OPTIONS = Arrays.asList(
        new OptionItem("wechat_qr", "微信扫码"),
        new OptionItem("qq_account", "QQ账密"),
        new OptionItem("qq_qr", "QQ扫码"),
        new OptionItem("steam_cn", "Steam国服"),
        new OptionItem("steam_global", "Steam国际服")
    );
    private static final List<OptionItem> SELLER_TYPE_OPTIONS = Arrays.asList(
        new OptionItem("PERSONAL", "个人"),
        new OptionItem("STUDIO", "工作室")
    );
    private static final List<OptionItem> PUBLISHED_DAY_OPTIONS = Arrays.asList(
        new OptionItem("1", "近1天"),
        new OptionItem("3", "近3天"),
        new OptionItem("7", "近7天")
    );
    private static final List<OptionItem> SORT_OPTIONS = Arrays.asList(
        new OptionItem("newest", "默认排序"),
        new OptionItem("haf_desc", "哈夫币排序"),
        new OptionItem("price_desc", "价格排序"),
        new OptionItem("ratio_asc", "比例排序"),
        new OptionItem("awm_desc", "AWM排序"),
        new OptionItem("helmet_desc", "六头排序"),
        new OptionItem("armor_desc", "六甲排序")
    );

    private final AccountListingMapper accountListingMapper;
    private final ListingFavoriteMapper listingFavoriteMapper;
    private final ObjectMapper objectMapper;
    private final OssStorageService ossStorageService;
    private final ChinaCityCatalog chinaCityCatalog;
    private final JdbcTemplate jdbcTemplate;
    private final AdminIntegrationConfigService adminIntegrationConfigService;

    public ListingService(
        AccountListingMapper accountListingMapper,
        ListingFavoriteMapper listingFavoriteMapper,
        ObjectMapper objectMapper,
        OssStorageService ossStorageService,
        ChinaCityCatalog chinaCityCatalog,
        JdbcTemplate jdbcTemplate,
        AdminIntegrationConfigService adminIntegrationConfigService
    ) {
        this.accountListingMapper = accountListingMapper;
        this.listingFavoriteMapper = listingFavoriteMapper;
        this.objectMapper = objectMapper;
        this.ossStorageService = ossStorageService;
        this.chinaCityCatalog = chinaCityCatalog;
        this.jdbcTemplate = jdbcTemplate;
        this.adminIntegrationConfigService = adminIntegrationConfigService;
    }

    public MarketplaceMeta loadMarketplaceMeta() {
        return new MarketplaceMeta(
            buildRegionOptions(),
            WEAPON_OPTIONS,
            KNIFE_SKIN_OPTIONS,
            RED_SKIN_OPTIONS,
            GOLD_SKIN_OPTIONS,
            AWM_BULLET_RANGE_OPTIONS,
            DEPOSIT_RANGE_OPTIONS,
            RANK_OPTIONS,
            SAFE_BOX_OPTIONS,
            LEVEL_OPTIONS,
            DELIVERY_METHOD_OPTIONS,
            SELLER_TYPE_OPTIONS,
            PUBLISHED_DAY_OPTIONS,
            SORT_OPTIONS
        );
    }

    public MarketplaceListResult loadMarketplace(MarketplaceQuery query) {
        long startAt = System.currentTimeMillis();
        MarketplaceQuery normalized = query == null ? new MarketplaceQuery() : query;
        if (canUseDatabasePaging(normalized)) {
            return loadMarketplacePageFromDatabase(normalized, startAt);
        }
        List<AccountListingDO> rows = accountListingMapper.selectList(buildMarketplaceBaseQuery(normalized));
        Map<Long, SellerMetrics> sellerMetricsMap = loadSellerMetrics(rows);

        List<ListingCard> result = new ArrayList<ListingCard>();
        for (AccountListingDO row : rows) {
            try {
                ListingProjection projection = toProjection(row, sellerMetricsMap.get(row.getSellerUserId()));
                if (matches(normalized, projection)) {
                    result.add(projection.getCard());
                }
            } catch (Exception exception) {
                log.warn("marketplace listing skipped listingNo={} reason={}", row.getListingNo(), exception.getMessage());
            }
        }

        result.sort(resolveComparator(normalized.getSort()));
        int total = result.size();
        int page = normalized.getPage();
        int pageSize = normalized.getPageSize();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        int toIndex = Math.min(total, fromIndex + pageSize);
        List<ListingCard> pagedRows = fromIndex >= total
            ? Collections.emptyList()
            : new ArrayList<ListingCard>(result.subList(fromIndex, toIndex));
        boolean hasMore = toIndex < total;
        log.info(
            "marketplace query success target=account_listing filters=region:{} weapon:{} rank:{} safeBox:{} sellerType:{} exchangeRateType:{} negotiable:{} alwaysOnline:{} publishedDays:{} sort:{} page:{} pageSize:{} count={} returned={} costMs={}",
            normalized.getRegionCodes().size(),
            normalized.getWeaponCodes().size(),
            normalized.getRank(),
            normalized.getSafeBoxLevel(),
            normalized.getSellerType(),
            normalized.getExchangeRateType(),
            normalized.getNegotiable(),
            normalized.getAlwaysOnline(),
            normalized.getPublishedWithinDays(),
            normalized.getSort(),
            page,
            pageSize,
            total,
            pagedRows.size(),
            System.currentTimeMillis() - startAt
        );
        return new MarketplaceListResult(total, page, pageSize, hasMore, pagedRows);
    }

    private MarketplaceListResult loadMarketplacePageFromDatabase(MarketplaceQuery normalized, long startAt) {
        int page = normalized.getPage();
        int pageSize = normalized.getPageSize();
        int fromIndex = Math.max(0, (page - 1) * pageSize);
        Long totalCount = accountListingMapper.selectCount(buildMarketplaceBaseQuery(normalized));
        int total = totalCount == null || totalCount.longValue() <= 0L
            ? 0
            : (int) Math.min(Integer.MAX_VALUE, totalCount.longValue());
        List<ListingCard> pagedRows = new ArrayList<ListingCard>();
        if (fromIndex < total) {
            LambdaQueryWrapper<AccountListingDO> pageQuery = buildMarketplaceBaseQuery(normalized);
            pageQuery.last(buildMarketplacePageClause(normalized.getSort(), fromIndex, pageSize));
            List<AccountListingDO> rows = accountListingMapper.selectList(pageQuery);
            Map<Long, SellerMetrics> sellerMetricsMap = loadSellerMetrics(rows);
            for (AccountListingDO row : rows) {
                try {
                    pagedRows.add(toProjection(row, sellerMetricsMap.get(row.getSellerUserId())).getCard());
                } catch (Exception exception) {
                    log.warn("marketplace listing skipped listingNo={} reason={}", row.getListingNo(), exception.getMessage());
                }
            }
        }
        boolean hasMore = fromIndex + pageSize < total;
        log.info(
            "marketplace query success target=account_listing mode=db_page filters=region:{} weapon:{} rank:{} safeBox:{} sellerType:{} exchangeRateType:{} negotiable:{} alwaysOnline:{} publishedDays:{} sort:{} page:{} pageSize:{} count={} returned={} costMs={}",
            normalized.getRegionCodes().size(),
            normalized.getWeaponCodes().size(),
            normalized.getRank(),
            normalized.getSafeBoxLevel(),
            normalized.getSellerType(),
            normalized.getExchangeRateType(),
            normalized.getNegotiable(),
            normalized.getAlwaysOnline(),
            normalized.getPublishedWithinDays(),
            normalized.getSort(),
            page,
            pageSize,
            total,
            pagedRows.size(),
            System.currentTimeMillis() - startAt
        );
        return new MarketplaceListResult(total, page, pageSize, hasMore, pagedRows);
    }

    private boolean canUseDatabasePaging(MarketplaceQuery query) {
        String sort = query.getSort();
        if (!"newest".equals(sort) && !"oldest".equals(sort)) {
            return false;
        }
        return !StringUtils.hasText(query.getDepositRange())
            && query.getWeaponCodes().isEmpty()
            && query.getKnifeSkins().isEmpty()
            && query.getRedSkins().isEmpty()
            && query.getGoldSkins().isEmpty()
            && !StringUtils.hasText(query.getAwmBulletRange())
            && !StringUtils.hasText(query.getExchangeRateType());
    }

    private String buildMarketplacePageClause(String sort, int fromIndex, int pageSize) {
        String direction = "oldest".equals(sort) ? "ASC" : "DESC";
        return "ORDER BY COALESCE(published_at, submitted_at, created_at) "
            + direction
            + ", id "
            + direction
            + " LIMIT "
            + fromIndex
            + ", "
            + pageSize;
    }

    private LambdaQueryWrapper<AccountListingDO> buildMarketplaceBaseQuery(MarketplaceQuery query) {
        LambdaQueryWrapper<AccountListingDO> wrapper = Wrappers.<AccountListingDO>lambdaQuery()
            .eq(AccountListingDO::getStatus, "PUBLISHED");
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim();
            wrapper.and(inner -> inner
                .like(AccountListingDO::getListingNo, keyword)
                .or()
                .like(AccountListingDO::getTitle, keyword)
                .or()
                .like(AccountListingDO::getSellerNickname, keyword)
                .or()
                .like(AccountListingDO::getStudioName, keyword)
                .or()
                .like(AccountListingDO::getProvinceName, keyword)
                .or()
                .like(AccountListingDO::getCityName, keyword)
            );
        }
        if (query.getMinPrice() != null) {
            wrapper.ge(AccountListingDO::getPrice, query.getMinPrice());
        }
        if (query.getMaxPrice() != null) {
            wrapper.le(AccountListingDO::getPrice, query.getMaxPrice());
        }
        if (query.getMinHafCurrency() != null) {
            wrapper.ge(AccountListingDO::getHafCurrency, query.getMinHafCurrency());
        }
        if (query.getMaxHafCurrency() != null) {
            wrapper.le(AccountListingDO::getHafCurrency, query.getMaxHafCurrency());
        }
        if (query.getMinAccountLevel() != null) {
            wrapper.ge(AccountListingDO::getAccountLevel, query.getMinAccountLevel());
        }
        if (query.getMaxAccountLevel() != null) {
            wrapper.le(AccountListingDO::getAccountLevel, query.getMaxAccountLevel());
        }
        if (!query.getRegionCodes().isEmpty()) {
            wrapper.and(inner -> inner
                .in(AccountListingDO::getCityCode, query.getRegionCodes())
                .or()
                .in(AccountListingDO::getProvinceCode, query.getRegionCodes())
            );
        }
        if (StringUtils.hasText(query.getRank())) {
            wrapper.eq(AccountListingDO::getRankName, query.getRank());
        }
        if (query.getSafeBoxLevel() != null) {
            wrapper.eq(AccountListingDO::getSafeBoxLevel, query.getSafeBoxLevel());
        }
        if (query.getStaminaLevel() != null) {
            wrapper.apply(
                "JSON_VALID(publish_attributes_json) AND CAST(JSON_UNQUOTE(JSON_EXTRACT(publish_attributes_json, '$.staminaLevel')) AS UNSIGNED) = {0}",
                query.getStaminaLevel()
            );
        }
        if (query.getCarryLevel() != null) {
            wrapper.apply(
                "JSON_VALID(publish_attributes_json) AND CAST(JSON_UNQUOTE(JSON_EXTRACT(publish_attributes_json, '$.carryLevel')) AS UNSIGNED) = {0}",
                query.getCarryLevel()
            );
        }
        if (StringUtils.hasText(query.getDeliveryMethod())) {
            wrapper.eq(AccountListingDO::getDeliveryMethod, query.getDeliveryMethod());
        }
        if (StringUtils.hasText(query.getSellerType())) {
            wrapper.eq(AccountListingDO::getSellerType, query.getSellerType());
        }
        if (query.getNegotiable() != null) {
            wrapper.eq(AccountListingDO::getNegotiable, query.getNegotiable());
        }
        if (query.getAlwaysOnline() != null) {
            wrapper.eq(AccountListingDO::getAlwaysOnline, query.getAlwaysOnline());
        }
        if (query.getPublishedWithinDays() != null) {
            LocalDateTime threshold = LocalDateTime.now().minusDays(query.getPublishedWithinDays());
            wrapper.apply("COALESCE(published_at, submitted_at) >= {0}", threshold);
        }
        return wrapper;
    }

    public ListingDetail loadDetail(String listingNo) {
        long startAt = System.currentTimeMillis();
        AccountListingDO row = findPublishedListing(listingNo);
        jdbcTemplate.update(
            "UPDATE account_listing SET view_count = COALESCE(view_count, 0) + 1 WHERE listing_no = ?",
            listingNo
        );
        row.setViewCount(defaultNumber(row.getViewCount()) + 1);
        ListingProjection projection = toProjection(row, loadSellerMetrics(row.getSellerUserId()));
        log.info("marketplace detail success listingNo={} costMs={}", listingNo, System.currentTimeMillis() - startAt);
        return new ListingDetail(
            projection.getCard(),
            row.getDescription(),
            buildImageUrls(row),
            resolveMediaUrl(row.getVideoKey())
        );
    }

    public Set<String> loadFavoriteListingNos(AuthPrincipal principal, List<String> listingNos) {
        if (principal == null || listingNos == null || listingNos.isEmpty()) {
            return Collections.emptySet();
        }
        List<ListingFavoriteDO> favorites = listingFavoriteMapper.selectList(
            Wrappers.<ListingFavoriteDO>lambdaQuery()
                .eq(ListingFavoriteDO::getUserId, principal.getUserId())
                .in(ListingFavoriteDO::getListingNo, listingNos)
        );
        Set<String> result = new LinkedHashSet<String>();
        for (ListingFavoriteDO favorite : favorites) {
            result.add(favorite.getListingNo());
        }
        return result;
    }

    public FavoriteToggleResult favoriteListing(AuthPrincipal principal, String listingNo) {
        return updateFavorite(principal, listingNo, true);
    }

    public FavoriteToggleResult unfavoriteListing(AuthPrincipal principal, String listingNo) {
        return updateFavorite(principal, listingNo, false);
    }

    private FavoriteToggleResult updateFavorite(AuthPrincipal principal, String listingNo, boolean favorite) {
        AccountListingDO row = findPublishedListing(listingNo);
        int nextFavoriteCount = defaultNumber(row.getFavoriteCount());
        ListingFavoriteDO existing = listingFavoriteMapper.selectOne(
            Wrappers.<ListingFavoriteDO>lambdaQuery()
                .eq(ListingFavoriteDO::getListingNo, listingNo)
                .eq(ListingFavoriteDO::getUserId, principal.getUserId())
                .last("LIMIT 1")
        );

        if (favorite && existing == null) {
            ListingFavoriteDO record = new ListingFavoriteDO();
            record.setListingNo(listingNo);
            record.setUserId(principal.getUserId());
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            listingFavoriteMapper.insert(record);
            nextFavoriteCount = nextFavoriteCount + 1;
        } else if (!favorite && existing != null) {
            listingFavoriteMapper.deleteById(existing.getId());
            nextFavoriteCount = Math.max(0, nextFavoriteCount - 1);
        } else {
            log.warn("marketplace favorite noop listingNo={} userId={} favorite={}", listingNo, principal.getUserId(), favorite);
        }

        jdbcTemplate.update("UPDATE account_listing SET favorite_count = ? WHERE listing_no = ?", nextFavoriteCount, listingNo);
        row.setFavoriteCount(nextFavoriteCount);
        log.info(
            "marketplace favorite success listingNo={} userId={} favorite={} favoriteCount={}",
            listingNo,
            principal.getUserId(),
            favorite,
            nextFavoriteCount
        );
        return new FavoriteToggleResult(listingNo, favorite, nextFavoriteCount);
    }

    private AccountListingDO findPublishedListing(String listingNo) {
        AccountListingDO row = accountListingMapper.selectOne(
            Wrappers.<AccountListingDO>lambdaQuery()
                .eq(AccountListingDO::getListingNo, listingNo)
                .eq(AccountListingDO::getStatus, "PUBLISHED")
                .last("LIMIT 1")
        );
        if (row == null) {
            throw new BusinessException(ErrorCode.BUSINESS_ERROR, "账号资源不存在或已下架");
        }
        return row;
    }

    private List<RegionOption> buildRegionOptions() {
        List<RegionOption> result = new ArrayList<RegionOption>();
        for (ChinaCityCatalog.ProvinceNode province : chinaCityCatalog.getProvinces()) {
            for (ChinaCityCatalog.CityNode city : province.getCities()) {
                result.add(new RegionOption(
                    city.getCode(),
                    province.getName() + "-" + city.getName(),
                    province.getCode(),
                    province.getName(),
                    city.getName()
                ));
            }
        }
        return result;
    }

    private Map<Long, SellerMetrics> loadSellerMetrics(List<AccountListingDO> rows) {
        LinkedHashSet<Long> sellerIds = new LinkedHashSet<Long>();
        for (AccountListingDO row : rows) {
            if (row.getSellerUserId() != null) {
                sellerIds.add(row.getSellerUserId());
            }
        }
        if (sellerIds.isEmpty()) {
            return Collections.emptyMap();
        }
        String placeholders = String.join(",", Collections.nCopies(sellerIds.size(), "?"));
        String sql =
            "SELECT seller_user_id, COUNT(*) AS publish_count, COALESCE(SUM(sales_count), 0) AS sales_count " +
                "FROM account_listing WHERE seller_user_id IN (" + placeholders + ") GROUP BY seller_user_id";
        Map<Long, SellerMetrics> result = new HashMap<Long, SellerMetrics>();
        jdbcTemplate.query(
            sql,
            sellerIds.toArray(),
            (org.springframework.jdbc.core.ResultSetExtractor<Void>) rs -> {
                while (rs.next()) {
                    result.put(
                        rs.getLong("seller_user_id"),
                        new SellerMetrics(rs.getInt("publish_count"), rs.getInt("sales_count"))
                    );
                }
                return null;
            }
        );
        return result;
    }

    private SellerMetrics loadSellerMetrics(Long sellerUserId) {
        if (sellerUserId == null) {
            return new SellerMetrics(0, 0);
        }
        List<SellerMetrics> rows = jdbcTemplate.query(
            "SELECT COUNT(*) AS publish_count, COALESCE(SUM(sales_count), 0) AS sales_count FROM account_listing WHERE seller_user_id = ?",
            (rs, rowNum) -> new SellerMetrics(rs.getInt("publish_count"), rs.getInt("sales_count")),
            sellerUserId
        );
        return rows.isEmpty() ? new SellerMetrics(0, 0) : rows.get(0);
    }

    private ListingProjection toProjection(AccountListingDO row, SellerMetrics sellerMetrics) {
        PublishAttributesSnapshot attributes = readPublishAttributes(row.getPublishAttributesJson());
        List<String> imageUrls = buildImageUrls(row);
        List<String> weaponSkins = readStringList(row.getWeaponSkinsJson());
        List<String> weapons = readStringList(row.getWeaponsJson());
        List<String> operators = readStringList(row.getOperatorsJson());
        List<String> knifeSkins = attributes.getKnifeSkins() == null ? Collections.<String>emptyList() : attributes.getKnifeSkins();
        List<String> redSkins = attributes.getRedSkins() == null ? Collections.<String>emptyList() : attributes.getRedSkins();
        List<String> goldSkins = attributes.getGoldSkins() == null ? Collections.<String>emptyList() : attributes.getGoldSkins();
        List<ExtraItemSnapshot> extraItems = attributes.getExtraItems() == null ? Collections.<ExtraItemSnapshot>emptyList() : attributes.getExtraItems();
        List<ExtraItemLine> chargeExtraItems = buildChargeExtraItems(extraItems);
        BigDecimal extraItemsAmount = calculateExtraItemsAmount(chargeExtraItems);
        BigDecimal deposit = attributes.getDeposit() == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : attributes.getDeposit();
        BigDecimal rent = row.getPrice() == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP) : row.getPrice().setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = deposit.add(rent).setScale(2, RoundingMode.HALF_UP);
        int rentalRatio = calculateRentalRatio(row, attributes);
        String exchangeRateLabel = formatRentalRatio(rentalRatio);
        int awmBulletCount = getExtraItemCount(extraItems, "awm_bullet");
        int helmetLevel6Count = getExtraItemCount(extraItems, "helmet_level_6");
        int armorLevel6Count = getExtraItemCount(extraItems, "armor_level_6");
        String sellerLabel = "STUDIO".equalsIgnoreCase(row.getSellerType()) && StringUtils.hasText(row.getStudioName())
            ? row.getStudioName()
            : row.getSellerNickname();
        String regionLabel = buildRegionLabel(row.getProvinceName(), row.getCityName());
        ExportInfo exportInfo = new ExportInfo(
            formatDeliveryMethod(row.getDeliveryMethod()),
            formatTimeRange(attributes.getDeliveryStartHour(), attributes.getDeliveryEndHour()),
            joinOrDefault(knifeSkins, "无"),
            joinOrDefault(redSkins, "无"),
            joinOrDefault(goldSkins, "无"),
            joinOrDefault(weaponSkins, "无"),
            buildExportExtraItems(extraItems)
        );

        ListingCard card = new ListingCard(
            row.getListingNo(),
            sellerLabel,
            buildAvatar(row),
            resolveTone(row.getListingNo()),
            firstOrNull(imageUrls),
            row.getTitle(),
            buildSummaryChips(row, attributes, exchangeRateLabel),
            Arrays.asList(
                new Panel("sky", false, Arrays.asList(
                    new PanelEntry("哈夫币纯币", formatHaf(row.getHafCurrency())),
                    new PanelEntry("保险等级", formatSafeBoxLevel(row.getSafeBoxLevel())),
                    new PanelEntry("体力等级", formatLevel(attributes.getStaminaLevel())),
                    new PanelEntry("负重等级", formatLevel(attributes.getCarryLevel())),
                    new PanelEntry("发布比例", exchangeRateLabel)
                )),
                new Panel("sky", false, Arrays.asList(
                    new PanelEntry("AWM", formatExtraItem(extraItems, "awm_bullet", "发")),
                    new PanelEntry("六头", formatExtraItem(extraItems, "helmet_level_6", "个")),
                    new PanelEntry("六甲", formatExtraItem(extraItems, "armor_level_6", "个")),
                    new PanelEntry("六级弹", formatExtraItem(extraItems, "bullet_level_6", "组")),
                    new PanelEntry("巴雷特", formatExtraItem(extraItems, "barrett_bullet", "发"))
                )),
                new Panel("orange", false, Arrays.asList(
                    new PanelEntry("绝密KD", formatDecimal(attributes.getSecretKd(), 2)),
                    new PanelEntry("段位", formatRank(row.getRankName())),
                    new PanelEntry("等级", row.getAccountLevel() == null ? "-" : row.getAccountLevel() + " 级"),
                    new PanelEntry("所在地区", regionLabel)
                )),
                new Panel("lavender", false, Arrays.asList(
                    new PanelEntry("人脸归属", Boolean.TRUE.equals(attributes.getFaceOwned()) ? "本人" : "非本人"),
                    new PanelEntry("赛依德", Boolean.TRUE.equals(attributes.getUnlockSaeed()) ? "解锁" : "未解锁"),
                    new PanelEntry("潜水等级", formatDiveLevel(attributes.getDiveLevel())),
                    new PanelEntry("发布时间", formatPublishTime(row.getPublishedAt(), row.getSubmittedAt()))
                )),
                new Panel("mint", true, Arrays.asList(
                    new PanelEntry("持有刀皮", joinOrDefault(knifeSkins, "-")),
                    new PanelEntry("干员外观", joinOrDefault(redSkins, "-")),
                    new PanelEntry("武器皮肤", joinOrDefault(weaponSkins, "-")),
                    new PanelEntry("人物金皮", joinOrDefault(goldSkins, "-"))
                ))
            ),
            new Pricing(formatMoney(deposit), formatMoney(rent), formatMoney(total), formatMoney(extraItemsAmount)),
            row.getSellerType(),
            formatSellerType(row.getSellerType()),
            buildAssuranceTags(row),
            buildHighlights(weapons, weaponSkins, knifeSkins, redSkins, goldSkins),
            new Stats(defaultNumber(row.getViewCount()), defaultNumber(row.getFavoriteCount()), defaultNumber(row.getSalesCount())),
            regionLabel,
            row.getAccountLevel() == null ? "-" : row.getAccountLevel() + "级",
            formatHaf(row.getHafCurrency()),
            exchangeRateLabel,
            buildImportantFacts(extraItems, exchangeRateLabel),
            chargeExtraItems,
            Boolean.TRUE.equals(row.getNegotiable()),
            formatPublishTime(row.getPublishedAt(), row.getSubmittedAt()),
            imageUrls,
            resolveMediaUrl(row.getVideoKey()),
            row.getDescription(),
            new EstimateReport(
                formatMoneyCompact(row.getSuggestedPrice()),
                safeText(row.getEstimateDetail()),
                formatPublishTime(row.getSubmittedAt(), row.getCreatedAt())
            ),
            new SellerInfo(
                sellerLabel,
                buildAvatar(row),
                formatSellerType(row.getSellerType()),
                StringUtils.hasText(row.getStudioName()) ? row.getStudioName() : null,
                buildFavorableRateLabel(sellerMetrics),
                sellerMetrics == null ? 0 : sellerMetrics.getSalesCount(),
                sellerMetrics == null ? 0 : sellerMetrics.getPublishCount()
            ),
            new GuaranteeInfo(
                "平台审核通过",
                "交易完成后24小时内可申请售后",
                "如发现虚假描述、私下交易或违规内容，平台将介入处理并保留封禁权利"
            ),
            buildInfoSection("基础信息", Arrays.asList(
                new DetailEntry("账号等级", row.getAccountLevel() == null ? "-" : row.getAccountLevel() + "级"),
                new DetailEntry("游戏区服", regionLabel),
                new DetailEntry("段位", formatRank(row.getRankName())),
                new DetailEntry("注册时长", "待补")
            )),
            buildInfoSection("资产信息", Arrays.asList(
                new DetailEntry("哈夫币数量", formatHaf(row.getHafCurrency())),
                new DetailEntry("发布比例", exchangeRateLabel),
                new DetailEntry("默认消耗", formatDefaultSpend(attributes.getDefaultSpend())),
                new DetailEntry("出租天数", defaultNumber(attributes.getRentalDays()) + "天"),
                new DetailEntry("安全箱等级", formatSafeBoxLevel(row.getSafeBoxLevel())),
                new DetailEntry("干员数量", formatOperatorCount(row.getOperatorCount())),
                new DetailEntry("核心干员", firstOrDash(operators))
            )),
            buildInfoSection("战绩信息", Arrays.asList(
                new DetailEntry("KD值", formatDecimal(attributes.getSecretKd(), 2)),
                new DetailEntry("胜率", "待补"),
                new DetailEntry("最高段位", formatRank(row.getRankName())),
                new DetailEntry("发布时间", formatPublishTime(row.getPublishedAt(), row.getSubmittedAt()))
            )),
            Arrays.asList(
                buildCategoryGroup("枪械皮肤", weaponSkins, "未标注"),
                buildCategoryGroup("防具装备", buildArmorItems(extraItems), "已配置"),
                buildCategoryGroup("其他道具", buildOtherItemLabels(extraItems, row.getOtherItems()), "物资")
            ),
            exportInfo,
            defaultLong(row.getHafCurrency()),
            rentalRatio,
            awmBulletCount,
            helmetLevel6Count,
            armorLevel6Count
        );
        return new ListingProjection(card, row, weapons, imageUrls, attributes);
    }

    private boolean matches(MarketplaceQuery query, ListingProjection projection) {
        AccountListingDO row = projection.getRow();
        if (query.getMinPrice() != null && (row.getPrice() == null || row.getPrice().compareTo(query.getMinPrice()) < 0)) {
            return false;
        }
        if (query.getMaxPrice() != null && (row.getPrice() == null || row.getPrice().compareTo(query.getMaxPrice()) > 0)) {
            return false;
        }
        PublishAttributesSnapshot attributes = projection.getAttributes();
        BigDecimal deposit = attributes == null || attributes.getDeposit() == null ? BigDecimal.ZERO : attributes.getDeposit();
        if (StringUtils.hasText(query.getDepositRange()) && !matchesMoneyRange(query.getDepositRange(), deposit)) {
            return false;
        }
        if (query.getMinHafCurrency() != null && (row.getHafCurrency() == null || row.getHafCurrency() < query.getMinHafCurrency())) {
            return false;
        }
        if (query.getMaxHafCurrency() != null && (row.getHafCurrency() == null || row.getHafCurrency() > query.getMaxHafCurrency())) {
            return false;
        }
        if (query.getMinAccountLevel() != null && (row.getAccountLevel() == null || row.getAccountLevel() < query.getMinAccountLevel())) {
            return false;
        }
        if (query.getMaxAccountLevel() != null && (row.getAccountLevel() == null || row.getAccountLevel() > query.getMaxAccountLevel())) {
            return false;
        }
        if (StringUtils.hasText(query.getRank()) && !query.getRank().equals(row.getRankName())) {
            return false;
        }
        if (query.getSafeBoxLevel() != null && !query.getSafeBoxLevel().equals(row.getSafeBoxLevel())) {
            return false;
        }
        if (query.getStaminaLevel() != null && (attributes == null || !query.getStaminaLevel().equals(attributes.getStaminaLevel()))) {
            return false;
        }
        if (query.getCarryLevel() != null && (attributes == null || !query.getCarryLevel().equals(attributes.getCarryLevel()))) {
            return false;
        }
        if (StringUtils.hasText(query.getDeliveryMethod()) && !query.getDeliveryMethod().equalsIgnoreCase(row.getDeliveryMethod())) {
            return false;
        }
        if (StringUtils.hasText(query.getSellerType()) && !query.getSellerType().equalsIgnoreCase(row.getSellerType())) {
            return false;
        }
        if (StringUtils.hasText(query.getExchangeRateType())
            && (attributes == null || !query.getExchangeRateType().equalsIgnoreCase(attributes.getExchangeRateType()))) {
            return false;
        }
        if (query.getNegotiable() != null && !query.getNegotiable().equals(row.getNegotiable())) {
            return false;
        }
        if (query.getAlwaysOnline() != null && !query.getAlwaysOnline().equals(row.getAlwaysOnline())) {
            return false;
        }
        if (!query.getRegionCodes().isEmpty()
            && !query.getRegionCodes().contains(row.getCityCode())
            && !query.getRegionCodes().contains(row.getProvinceCode())) {
            return false;
        }
        if (!query.getWeaponCodes().isEmpty() && Collections.disjoint(query.getWeaponCodes(), projection.getWeapons())) {
            return false;
        }
        if (!query.getKnifeSkins().isEmpty() && Collections.disjoint(query.getKnifeSkins(), safeList(attributes.getKnifeSkins()))) {
            return false;
        }
        if (!query.getRedSkins().isEmpty() && Collections.disjoint(query.getRedSkins(), safeList(attributes.getRedSkins()))) {
            return false;
        }
        if (!query.getGoldSkins().isEmpty() && Collections.disjoint(query.getGoldSkins(), safeList(attributes.getGoldSkins()))) {
            return false;
        }
        if (StringUtils.hasText(query.getAwmBulletRange()) && !matchesAwmBulletRange(query.getAwmBulletRange(), attributes.getExtraItems())) {
            return false;
        }
        if (query.getPublishedWithinDays() != null) {
            LocalDateTime baseTime = row.getPublishedAt() != null ? row.getPublishedAt() : row.getSubmittedAt();
            LocalDateTime threshold = LocalDateTime.now().minusDays(query.getPublishedWithinDays());
            if (baseTime == null || baseTime.isBefore(threshold)) {
                return false;
            }
        }
        if (StringUtils.hasText(query.getKeyword())) {
            String keyword = query.getKeyword().trim().toLowerCase();
            String haystack = (
                safeText(row.getListingNo()) + "|" +
                    safeText(row.getTitle()) + "|" +
                    safeText(row.getSellerNickname()) + "|" +
                    safeText(row.getStudioName()) + "|" +
                    safeText(row.getProvinceName()) + "|" +
                    safeText(row.getCityName())
            ).toLowerCase();
            if (!haystack.contains(keyword)) {
                return false;
            }
        }
        return true;
    }

    private Comparator<ListingCard> resolveComparator(String sort) {
        if ("oldest".equals(sort)) {
            return Comparator.comparing(ListingCard::getPublishedTime, Comparator.nullsLast(Comparator.naturalOrder()));
        }
        if ("haf_asc".equals(sort)) {
            return Comparator.comparingLong(ListingCard::getHafCurrencyAmount)
                .thenComparing(ListingCard::getPublishedTime, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        if ("haf_desc".equals(sort)) {
            return Comparator.comparingLong(ListingCard::getHafCurrencyAmount)
                .reversed()
                .thenComparing(ListingCard::getPublishedTime, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        if ("price_asc".equals(sort)) {
            return Comparator.comparing(ListingCard::getRentAmount)
                .thenComparing(ListingCard::getPublishedTime, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        if ("price_desc".equals(sort)) {
            return Comparator.comparing(ListingCard::getRentAmount)
                .reversed()
                .thenComparing(ListingCard::getPublishedTime, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        if ("ratio_asc".equals(sort)) {
            return Comparator.comparingInt(ListingCard::getRentalRatio)
                .thenComparing(ListingCard::getPublishedTime, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        if ("ratio_desc".equals(sort)) {
            return Comparator.comparingInt(ListingCard::getRentalRatio)
                .reversed()
                .thenComparing(ListingCard::getPublishedTime, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        if ("awm_asc".equals(sort)) {
            return Comparator.comparingInt(ListingCard::getAwmBulletCount)
                .thenComparing(ListingCard::getPublishedTime, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        if ("awm_desc".equals(sort)) {
            return Comparator.comparingInt(ListingCard::getAwmBulletCount)
                .reversed()
                .thenComparing(ListingCard::getPublishedTime, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        if ("helmet_asc".equals(sort)) {
            return Comparator.comparingInt(ListingCard::getHelmetLevel6Count)
                .thenComparing(ListingCard::getPublishedTime, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        if ("helmet_desc".equals(sort)) {
            return Comparator.comparingInt(ListingCard::getHelmetLevel6Count)
                .reversed()
                .thenComparing(ListingCard::getPublishedTime, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        if ("armor_asc".equals(sort)) {
            return Comparator.comparingInt(ListingCard::getArmorLevel6Count)
                .thenComparing(ListingCard::getPublishedTime, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        if ("armor_desc".equals(sort)) {
            return Comparator.comparingInt(ListingCard::getArmorLevel6Count)
                .reversed()
                .thenComparing(ListingCard::getPublishedTime, Comparator.nullsLast(Comparator.reverseOrder()));
        }
        return Comparator.comparing(ListingCard::getPublishedTime, Comparator.nullsLast(Comparator.reverseOrder()));
    }

    private List<SummaryChip> buildSummaryChips(AccountListingDO row, PublishAttributesSnapshot attributes, String exchangeRateLabel) {
        List<SummaryChip> chips = new ArrayList<SummaryChip>();
        chips.add(new SummaryChip(exchangeRateLabel, "sky"));
        chips.add(new SummaryChip("租期" + defaultNumber(attributes.getRentalDays()) + "天", "sky"));
        chips.add(new SummaryChip(formatDefaultSpend(attributes.getDefaultSpend()), "sky"));
        chips.add(new SummaryChip(formatDeliveryMethod(row.getDeliveryMethod()), "sky"));
        if (hasGiftExtraItem(attributes.getExtraItems())) {
            chips.add(new SummaryChip("特惠", "sale"));
        }
        if (Boolean.TRUE.equals(row.getAlwaysOnline())) {
            chips.add(new SummaryChip("24h随时上号", "sale"));
        }
        if (Boolean.TRUE.equals(row.getNegotiable())) {
            chips.add(new SummaryChip("可议价", "sale"));
        }
        return chips;
    }

    private boolean hasGiftExtraItem(List<ExtraItemSnapshot> extraItems) {
        if (extraItems == null) {
            return false;
        }
        for (ExtraItemSnapshot item : extraItems) {
            if (defaultNumber(item.getCount()) > 0 && "gift".equalsIgnoreCase(item.getChargeMode())) {
                return true;
            }
        }
        return false;
    }

    private List<ImportantFact> buildImportantFacts(List<ExtraItemSnapshot> extraItems, String exchangeRateLabel) {
        List<ImportantFact> facts = new ArrayList<ImportantFact>();
        facts.add(new ImportantFact("AWM", formatExtraItem(extraItems, "awm_bullet", "发"), true));
        facts.add(new ImportantFact("六头", formatExtraItem(extraItems, "helmet_level_6", "个"), true));
        facts.add(new ImportantFact("六甲", formatExtraItem(extraItems, "armor_level_6", "个"), true));
        facts.add(new ImportantFact("六弹", formatExtraItem(extraItems, "bullet_level_6", "组"), true));
        facts.add(new ImportantFact("巴雷特", formatExtraItem(extraItems, "barrett_bullet", "发"), true));
        facts.add(new ImportantFact("比例", exchangeRateLabel, true));
        return facts;
    }

    private List<String> buildAssuranceTags(AccountListingDO row) {
        List<String> result = new ArrayList<String>();
        result.add("平台审核");
        result.add("售后保障");
        if (Boolean.TRUE.equals(row.getAlwaysOnline())) {
            result.add("24h在线");
        }
        if ("STUDIO".equalsIgnoreCase(row.getSellerType())) {
            result.add("工作室供号");
        }
        return result;
    }

    private List<String> buildHighlights(
        List<String> weapons,
        List<String> weaponSkins,
        List<String> knifeSkins,
        List<String> redSkins,
        List<String> goldSkins
    ) {
        LinkedHashSet<String> values = new LinkedHashSet<String>();
        for (String weapon : weapons) {
            values.add(resolveWeaponLabel(weapon));
        }
        values.addAll(cleanTexts(weaponSkins));
        values.addAll(cleanTexts(knifeSkins));
        values.addAll(cleanTexts(redSkins));
        values.addAll(cleanTexts(goldSkins));

        List<String> result = new ArrayList<String>();
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                result.add(value);
            }
            if (result.size() >= 5) {
                break;
            }
        }
        return result;
    }

    private DetailSection buildInfoSection(String title, List<DetailEntry> entries) {
        List<DetailEntry> cleaned = new ArrayList<DetailEntry>();
        for (DetailEntry entry : entries) {
            if (entry != null) {
                cleaned.add(entry);
            }
        }
        return new DetailSection(title, cleaned);
    }

    private DetailCategoryGroup buildCategoryGroup(String title, List<String> names, String badge) {
        List<DetailCategoryItem> items = new ArrayList<DetailCategoryItem>();
        if (names != null) {
            for (String name : names) {
                if (StringUtils.hasText(name)) {
                    items.add(new DetailCategoryItem(name.trim(), badge));
                }
            }
        }
        if (items.isEmpty()) {
            items.add(new DetailCategoryItem("暂无", "-"));
        }
        return new DetailCategoryGroup(title, items);
    }

    private List<String> buildArmorItems(List<ExtraItemSnapshot> extraItems) {
        List<String> result = new ArrayList<String>();
        for (ExtraItemSnapshot item : extraItems) {
            if ("helmet_level_6".equals(item.getCode()) && defaultNumber(item.getCount()) > 0) {
                result.add("六级头盔 x" + item.getCount());
            }
            if ("armor_level_6".equals(item.getCode()) && defaultNumber(item.getCount()) > 0) {
                result.add("六级护甲 x" + item.getCount());
            }
            if ("premium_insurance".equals(item.getCode()) && defaultNumber(item.getCount()) > 0) {
                result.add("顶级保险卡 x" + item.getCount());
            }
        }
        return result;
    }

    private List<ExtraItemLine> buildChargeExtraItems(List<ExtraItemSnapshot> extraItems) {
        List<ExtraItemLine> result = new ArrayList<ExtraItemLine>();
        if (extraItems == null) {
            return result;
        }
        for (ExtraItemSnapshot item : extraItems) {
            int count = defaultNumber(item.getCount());
            if (count <= 0 || !"charge".equals(item.getChargeMode())) {
                continue;
            }
            BigDecimal unitPrice = item.getUnitPrice() == null ? BigDecimal.ZERO : item.getUnitPrice();
            BigDecimal subtotal = item.getTotalPrice() == null
                ? unitPrice.multiply(new BigDecimal(count)).setScale(2, RoundingMode.HALF_UP)
                : item.getTotalPrice().setScale(2, RoundingMode.HALF_UP);
            result.add(new ExtraItemLine(
                StringUtils.hasText(item.getLabel()) ? item.getLabel().trim() : item.getCode(),
                count,
                formatMoney(unitPrice),
                formatMoney(subtotal)
            ));
        }
        return result;
    }

    private String buildExportExtraItems(List<ExtraItemSnapshot> extraItems) {
        if (extraItems == null || extraItems.isEmpty()) {
            return "无";
        }
        List<String> result = new ArrayList<String>();
        for (ExtraItemSnapshot item : extraItems) {
            int count = defaultNumber(item.getCount());
            if (count <= 0) {
                continue;
            }
            String label = StringUtils.hasText(item.getLabel()) ? item.getLabel().trim() : item.getCode();
            String mode = "gift".equalsIgnoreCase(item.getChargeMode()) ? "赠送" : "收费";
            result.add(label + count + "（" + mode + "）");
        }
        return result.isEmpty() ? "无" : String.join("、", result);
    }

    private String joinOrDefault(List<String> values, String fallback) {
        if (values == null || values.isEmpty()) {
            return fallback;
        }
        List<String> cleaned = cleanTexts(values);
        return cleaned.isEmpty() ? fallback : String.join("、", cleaned);
    }

    private BigDecimal calculateExtraItemsAmount(List<ExtraItemLine> items) {
        BigDecimal total = BigDecimal.ZERO;
        if (items == null) {
            return total.setScale(2, RoundingMode.HALF_UP);
        }
        for (ExtraItemLine item : items) {
            total = total.add(parseMoneyValue(item.getSubtotal()));
        }
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private List<String> buildOtherItemLabels(List<ExtraItemSnapshot> extraItems, String otherItems) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        for (ExtraItemSnapshot item : extraItems) {
            if (defaultNumber(item.getCount()) <= 0) {
                continue;
            }
            if ("awm_bullet".equals(item.getCode())) {
                result.add("AWM子弹 x" + item.getCount());
            } else if ("bullet_level_6".equals(item.getCode())) {
                result.add("六级子弹 x" + item.getCount());
            } else if ("barrett_bullet".equals(item.getCode())) {
                result.add("巴雷特子弹 x" + item.getCount());
            } else if ("premium_coffee".equals(item.getCode())) {
                result.add("高级咖啡豆 x" + item.getCount());
            } else if ("premium_bullet_part".equals(item.getCode())) {
                result.add("高级子弹零件 x" + item.getCount());
            }
        }
        if (StringUtils.hasText(otherItems)) {
            for (String value : otherItems.split("[,，/\\n]")) {
                if (StringUtils.hasText(value)) {
                    result.add(value.trim());
                }
            }
        }
        return new ArrayList<String>(result);
    }

    private List<String> safeList(List<String> values) {
        return values == null ? Collections.<String>emptyList() : values;
    }

    private boolean matchesAwmBulletRange(String range, List<ExtraItemSnapshot> extraItems) {
        int count = getExtraItemCount(extraItems, "awm_bullet");
        if ("0_50".equals(range)) {
            return count >= 0 && count < 50;
        }
        if ("50_100".equals(range)) {
            return count >= 50 && count < 100;
        }
        if ("100_300".equals(range)) {
            return count >= 100 && count < 300;
        }
        if ("300_plus".equals(range)) {
            return count >= 300;
        }
        return true;
    }

    private boolean matchesMoneyRange(String range, BigDecimal amount) {
        BigDecimal value = amount == null ? BigDecimal.ZERO : amount;
        if ("0_100".equals(range)) {
            return value.compareTo(BigDecimal.ZERO) >= 0 && value.compareTo(new BigDecimal("100")) < 0;
        }
        if ("100_300".equals(range)) {
            return value.compareTo(new BigDecimal("100")) >= 0 && value.compareTo(new BigDecimal("300")) < 0;
        }
        if ("300_700".equals(range)) {
            return value.compareTo(new BigDecimal("300")) >= 0 && value.compareTo(new BigDecimal("700")) < 0;
        }
        if ("700_1500".equals(range)) {
            return value.compareTo(new BigDecimal("700")) >= 0 && value.compareTo(new BigDecimal("1500")) < 0;
        }
        if ("1500_plus".equals(range)) {
            return value.compareTo(new BigDecimal("1500")) >= 0;
        }
        return true;
    }

    private int getExtraItemCount(List<ExtraItemSnapshot> extraItems, String code) {
        if (extraItems == null) {
            return 0;
        }
        for (ExtraItemSnapshot item : extraItems) {
            if (item != null && code.equals(item.getCode())) {
                return defaultNumber(item.getCount());
            }
        }
        return 0;
    }

    private String buildFavorableRateLabel(SellerMetrics sellerMetrics) {
        if (sellerMetrics == null || sellerMetrics.getSalesCount() <= 0) {
            return "待积累";
        }
        return "100.0%";
    }

    private String formatOperatorCount(Integer value) {
        if (value == null) {
            return "-";
        }
        return value + " 名";
    }

    private List<String> cleanTexts(Collection<String> source) {
        if (source == null || source.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<String>();
        for (String item : source) {
            if (StringUtils.hasText(item)) {
                result.add(item.trim());
            }
        }
        return result;
    }

    private String resolveMediaUrl(String objectKey) {
        if (!StringUtils.hasText(objectKey)) {
            return null;
        }
        return ossStorageService.previewUrl(objectKey);
    }

    private List<String> buildImageUrls(AccountListingDO row) {
        List<String> imageKeys = readStringList(row.getImageKeysJson());
        List<String> result = new ArrayList<String>();
        for (String key : imageKeys) {
            if (StringUtils.hasText(key)) {
                result.add(resolveMediaUrl(key));
            }
        }
        if (result.isEmpty() && StringUtils.hasText(row.getCoverImageKey())) {
            result.add(resolveMediaUrl(row.getCoverImageKey()));
        }
        return result;
    }

    private PublishAttributesSnapshot readPublishAttributes(String raw) {
        if (!StringUtils.hasText(raw)) {
            return new PublishAttributesSnapshot();
        }
        try {
            return objectMapper.readValue(raw, PublishAttributesSnapshot.class);
        } catch (Exception exception) {
            throw new IllegalStateException("发布属性解析失败");
        }
    }

    private List<String> readStringList(String raw) {
        if (!StringUtils.hasText(raw)) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(raw, STRING_LIST_TYPE);
        } catch (Exception exception) {
            return Collections.emptyList();
        }
    }

    private String buildAvatar(AccountListingDO row) {
        if (row.getSellerUserId() == null) {
            return "00";
        }
        return String.format("%02d", Math.abs(row.getSellerUserId() % 100));
    }

    private String resolveTone(String listingNo) {
        String[] tones = {"steel", "ember", "forest", "midnight"};
        if (!StringUtils.hasText(listingNo)) {
            return tones[0];
        }
        return tones[Math.abs(listingNo.hashCode()) % tones.length];
    }

    private String resolveWeaponLabel(String value) {
        for (OptionItem option : WEAPON_OPTIONS) {
            if (option.getValue().equals(value)) {
                return option.getLabel();
            }
        }
        return value;
    }

    private String formatDefaultSpend(String value) {
        if (!StringUtils.hasText(value)) {
            return "日耗待补";
        }
        if ("10m".equals(value)) {
            return "日耗10M";
        }
        if ("20m_plus_2".equals(value)) {
            return "日耗20M";
        }
        if ("30m_plus_3".equals(value)) {
            return "日耗30M";
        }
        if ("40m_plus_4".equals(value)) {
            return "日耗40M";
        }
        if ("50m_plus_5".equals(value)) {
            return "日耗50M";
        }
        return value;
    }

    private String formatExchangeRateChip(PublishAttributesSnapshot attributes) {
        String label = formatExchangeRate(attributes);
        if (!StringUtils.hasText(label) || "-".equals(label)) {
            return "比例待补";
        }
        if (label.startsWith("默认 ")) {
            return label.substring("默认 ".length());
        }
        if (label.startsWith("加速 ")) {
            return label.substring("加速 ".length());
        }
        if (label.startsWith("特惠 ")) {
            return label.substring("特惠 ".length());
        }
        return label;
    }

    private String formatExchangeRate(PublishAttributesSnapshot attributes) {
        if (attributes == null) {
            return "-";
        }
        String type = attributes.getExchangeRateType();
        BigDecimal rate = attributes.getCustomExchangeRate();
        if ("custom".equals(type)) {
            return rate == null ? "自定义比例" : formatExchangeRateWan(rate);
        }
        if ("accelerated".equals(type)) {
            return rate == null ? "特惠比例" : formatExchangeRateWan(rate);
        }
        if ("default".equals(type)) {
            return rate == null ? "默认比例" : formatExchangeRateWan(rate);
        }
        return "-";
    }

    private String formatExchangeRateWan(BigDecimal value) {
        if (value == null) {
            return "-";
        }
        BigDecimal wan = value.divide(new BigDecimal("10000"), 2, RoundingMode.HALF_UP).stripTrailingZeros();
        return wan.toPlainString() + "w/1元";
    }

    private String formatRentalRatio(int ratio) {
        return Math.max(1, ratio) + "w/1元";
    }

    private String formatDeliveryMethod(String value) {
        if ("wechat_qr".equals(value)) {
            return "微信扫码";
        }
        if ("qq_account".equals(value)) {
            return "QQ账密";
        }
        if ("qq_qr".equals(value)) {
            return "QQ扫码";
        }
        if ("steam_cn".equals(value)) {
            return "Steam国服";
        }
        if ("steam_global".equals(value)) {
            return "Steam国际服";
        }
        return "上号方式待补";
    }

    private String formatTimeRange(Integer start, Integer end) {
        if (start == null || end == null) {
            return "上号时间待补";
        }
        return "上号：" + start + "点 - " + end + "点";
    }

    private String formatExtraItem(List<ExtraItemSnapshot> items, String code, String unit) {
        for (ExtraItemSnapshot item : items) {
            if (code.equals(item.getCode())) {
                int count = defaultNumber(item.getCount());
                String giftLabel = count > 0 && "gift".equalsIgnoreCase(item.getChargeMode()) ? " · 赠送" : "";
                return count + " " + unit + giftLabel;
            }
        }
        return "0 " + unit;
    }

    private String formatLevel(Integer value) {
        return value == null ? "-" : value + " 级";
    }

    private String formatSafeBoxLevel(Integer value) {
        if (value == null) {
            return "-";
        }
        if (value == 1) {
            return "基础安全箱(1*2)";
        }
        if (value == 2) {
            return "进阶安全箱(2*2)";
        }
        if (value == 3) {
            return "高级安全箱(2*3)";
        }
        if (value == 4) {
            return "顶级安全箱(3*3)";
        }
        return value + " 级";
    }

    private String formatDiveLevel(Integer value) {
        if (value == null) {
            return "-";
        }
        if (value == -1) {
            return "无";
        }
        return value + " 级";
    }

    private String formatRank(String value) {
        for (OptionItem option : RANK_OPTIONS) {
            if (option.getValue().equals(value)) {
                return option.getLabel();
            }
        }
        if ("master".equals(value)) {
            return "大师";
        }
        if ("legend".equals(value)) {
            return "传奇";
        }
        return "-";
    }

    private String formatSellerType(String value) {
        return "STUDIO".equalsIgnoreCase(value) ? "工作室" : "个人";
    }

    private String buildRegionLabel(String provinceName, String cityName) {
        if (StringUtils.hasText(provinceName) && StringUtils.hasText(cityName)) {
            return provinceName + "-" + cityName;
        }
        if (StringUtils.hasText(cityName)) {
            return cityName;
        }
        if (StringUtils.hasText(provinceName)) {
            return provinceName;
        }
        return "-";
    }

    private LocalDateTime resolvePublishedTime(LocalDateTime publishedAt, LocalDateTime submittedAt) {
        return publishedAt != null ? publishedAt : submittedAt;
    }

    private String formatPublishTime(LocalDateTime publishedAt, LocalDateTime submittedAt) {
        LocalDateTime value = resolvePublishedTime(publishedAt, submittedAt);
        return value == null ? "-" : value.format(PUBLISH_TIME_FORMATTER);
    }

    private String firstOrDash(List<String> values) {
        if (values == null || values.isEmpty() || !StringUtils.hasText(values.get(0))) {
            return "-";
        }
        return values.get(0);
    }

    private String firstOrNull(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return values.get(0);
    }

    private String formatHaf(Long value) {
        return value == null ? "-" : value + " M";
    }

    private int calculateRentalRatio(AccountListingDO row, PublishAttributesSnapshot attributes) {
        if (attributes != null
            && "custom".equals(attributes.getExchangeRateType())
            && attributes.getCustomExchangeRate() != null
            && attributes.getCustomExchangeRate().compareTo(BigDecimal.ZERO) > 0) {
            return rentalBaseRatio(attributes);
        }
        int ratio = rentalBaseRatio(attributes);
        ratio += safeBoxRatioDelta(row.getSafeBoxLevel());
        ratio += levelRatioDelta(attributes.getStaminaLevel());
        ratio += levelRatioDelta(attributes.getCarryLevel());
        ratio += hafCurrencyRatioDelta(row.getHafCurrency());
        ratio += defaultSpendRatioOffset(attributes.getDefaultSpend());
        if (hasEffectiveKnifeSkin(attributes.getKnifeSkins())) {
            ratio -= 1;
        }
        return Math.max(1, ratio);
    }

    private int rentalBaseRatio(PublishAttributesSnapshot attributes) {
        if (attributes != null
            && "custom".equals(attributes.getExchangeRateType())
            && attributes.getCustomExchangeRate() != null
            && attributes.getCustomExchangeRate().compareTo(BigDecimal.ZERO) > 0) {
            return Math.max(1, attributes.getCustomExchangeRate().divide(new BigDecimal("10000"), 0, RoundingMode.DOWN).intValue());
        }
        if (attributes != null && "accelerated".equals(attributes.getExchangeRateType())) {
            return defaultRentalBaseRatio() + 5;
        }
        return defaultRentalBaseRatio();
    }

    private int defaultRentalBaseRatio() {
        Object value = adminIntegrationConfigService.loadListingPublishConfig().get("defaultExchangeRate");
        try {
            BigDecimal rate = new BigDecimal(String.valueOf(value));
            return Math.max(1, rate.divide(new BigDecimal("10000"), 0, RoundingMode.DOWN).intValue());
        } catch (Exception ignored) {
            return 38;
        }
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
        if (values == null) {
            return false;
        }
        for (String value : values) {
            if (value != null && RENTAL_EFFECTIVE_KNIFE_SKINS.contains(value.trim())) {
                return true;
            }
        }
        return false;
    }

    private String formatDecimal(BigDecimal value, int scale) {
        if (value == null) {
            return "-";
        }
        return value.setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }

    private String formatMoney(BigDecimal value) {
        BigDecimal safe = value == null ? BigDecimal.ZERO : value.setScale(2, RoundingMode.HALF_UP);
        return safe.stripTrailingZeros().toPlainString() + " 元";
    }

    private String formatMoneyCompact(BigDecimal value) {
        if (value == null) {
            return "¥0";
        }
        return "¥" + value.setScale(0, RoundingMode.HALF_UP).toPlainString();
    }

    private int defaultNumber(Integer value) {
        return value == null ? 0 : value;
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    private String safeText(String value) {
        return value == null ? "" : value;
    }

    public static class MarketplaceMeta {
        private final List<RegionOption> regions;
        private final List<OptionItem> weapons;
        private final List<OptionItem> knifeSkins;
        private final List<OptionItem> redSkins;
        private final List<OptionItem> goldSkins;
        private final List<OptionItem> awmBulletRanges;
        private final List<OptionItem> depositRanges;
        private final List<OptionItem> ranks;
        private final List<OptionItem> safeBoxLevels;
        private final List<OptionItem> levelOptions;
        private final List<OptionItem> deliveryMethods;
        private final List<OptionItem> sellerTypes;
        private final List<OptionItem> publishedDayOptions;
        private final List<OptionItem> sortOptions;

        public MarketplaceMeta(
            List<RegionOption> regions,
            List<OptionItem> weapons,
            List<OptionItem> knifeSkins,
            List<OptionItem> redSkins,
            List<OptionItem> goldSkins,
            List<OptionItem> awmBulletRanges,
            List<OptionItem> depositRanges,
            List<OptionItem> ranks,
            List<OptionItem> safeBoxLevels,
            List<OptionItem> levelOptions,
            List<OptionItem> deliveryMethods,
            List<OptionItem> sellerTypes,
            List<OptionItem> publishedDayOptions,
            List<OptionItem> sortOptions
        ) {
            this.regions = regions;
            this.weapons = weapons;
            this.knifeSkins = knifeSkins;
            this.redSkins = redSkins;
            this.goldSkins = goldSkins;
            this.awmBulletRanges = awmBulletRanges;
            this.depositRanges = depositRanges;
            this.ranks = ranks;
            this.safeBoxLevels = safeBoxLevels;
            this.levelOptions = levelOptions;
            this.deliveryMethods = deliveryMethods;
            this.sellerTypes = sellerTypes;
            this.publishedDayOptions = publishedDayOptions;
            this.sortOptions = sortOptions;
        }

        public List<RegionOption> getRegions() {
            return regions;
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

        public List<OptionItem> getGoldSkins() {
            return goldSkins;
        }

        public List<OptionItem> getAwmBulletRanges() {
            return awmBulletRanges;
        }

        public List<OptionItem> getDepositRanges() {
            return depositRanges;
        }

        public List<OptionItem> getRanks() {
            return ranks;
        }

        public List<OptionItem> getSafeBoxLevels() {
            return safeBoxLevels;
        }

        public List<OptionItem> getLevelOptions() {
            return levelOptions;
        }

        public List<OptionItem> getDeliveryMethods() {
            return deliveryMethods;
        }

        public List<OptionItem> getSellerTypes() {
            return sellerTypes;
        }

        public List<OptionItem> getPublishedDayOptions() {
            return publishedDayOptions;
        }

        public List<OptionItem> getSortOptions() {
            return sortOptions;
        }
    }

    public static class MarketplaceListResult {
        private final int total;
        private final int page;
        private final int pageSize;
        private final boolean hasMore;
        private final List<ListingCard> rows;

        public MarketplaceListResult(int total, int page, int pageSize, boolean hasMore, List<ListingCard> rows) {
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
            this.hasMore = hasMore;
            this.rows = rows;
        }

        public int getTotal() {
            return total;
        }

        public int getPage() {
            return page;
        }

        public int getPageSize() {
            return pageSize;
        }

        public boolean isHasMore() {
            return hasMore;
        }

        public List<ListingCard> getRows() {
            return rows;
        }
    }

    public static class MarketplaceQuery {
        private String keyword;
        private BigDecimal minPrice;
        private BigDecimal maxPrice;
        private String depositRange;
        private Long minHafCurrency;
        private Long maxHafCurrency;
        private Integer minAccountLevel;
        private Integer maxAccountLevel;
        private List<String> regionCodes = Collections.emptyList();
        private List<String> weaponCodes = Collections.emptyList();
        private List<String> knifeSkins = Collections.emptyList();
        private List<String> redSkins = Collections.emptyList();
        private List<String> goldSkins = Collections.emptyList();
        private String awmBulletRange;
        private String rank;
        private Integer safeBoxLevel;
        private Integer staminaLevel;
        private Integer carryLevel;
        private String deliveryMethod;
        private String sellerType;
        private String exchangeRateType;
        private Boolean negotiable;
        private Boolean alwaysOnline;
        private Integer publishedWithinDays;
        private String sort = "newest";
        private Integer page = 1;
        private Integer pageSize = 10;

        public String getKeyword() {
            return keyword;
        }

        public void setKeyword(String keyword) {
            this.keyword = keyword;
        }

        public BigDecimal getMinPrice() {
            return minPrice;
        }

        public void setMinPrice(BigDecimal minPrice) {
            this.minPrice = minPrice;
        }

        public BigDecimal getMaxPrice() {
            return maxPrice;
        }

        public void setMaxPrice(BigDecimal maxPrice) {
            this.maxPrice = maxPrice;
        }

        public String getDepositRange() {
            return depositRange;
        }

        public void setDepositRange(String depositRange) {
            this.depositRange = depositRange;
        }

        public Long getMinHafCurrency() {
            return minHafCurrency;
        }

        public void setMinHafCurrency(Long minHafCurrency) {
            this.minHafCurrency = minHafCurrency;
        }

        public Long getMaxHafCurrency() {
            return maxHafCurrency;
        }

        public void setMaxHafCurrency(Long maxHafCurrency) {
            this.maxHafCurrency = maxHafCurrency;
        }

        public Integer getMinAccountLevel() {
            return minAccountLevel;
        }

        public void setMinAccountLevel(Integer minAccountLevel) {
            this.minAccountLevel = minAccountLevel;
        }

        public Integer getMaxAccountLevel() {
            return maxAccountLevel;
        }

        public void setMaxAccountLevel(Integer maxAccountLevel) {
            this.maxAccountLevel = maxAccountLevel;
        }

        public List<String> getRegionCodes() {
            return regionCodes == null ? Collections.<String>emptyList() : regionCodes;
        }

        public void setRegionCodes(List<String> regionCodes) {
            this.regionCodes = regionCodes;
        }

        public List<String> getWeaponCodes() {
            return weaponCodes == null ? Collections.<String>emptyList() : weaponCodes;
        }

        public void setWeaponCodes(List<String> weaponCodes) {
            this.weaponCodes = weaponCodes;
        }

        public List<String> getKnifeSkins() {
            return knifeSkins == null ? Collections.<String>emptyList() : knifeSkins;
        }

        public void setKnifeSkins(List<String> knifeSkins) {
            this.knifeSkins = knifeSkins;
        }

        public List<String> getRedSkins() {
            return redSkins == null ? Collections.<String>emptyList() : redSkins;
        }

        public void setRedSkins(List<String> redSkins) {
            this.redSkins = redSkins;
        }

        public List<String> getGoldSkins() {
            return goldSkins == null ? Collections.<String>emptyList() : goldSkins;
        }

        public void setGoldSkins(List<String> goldSkins) {
            this.goldSkins = goldSkins;
        }

        public String getAwmBulletRange() {
            return awmBulletRange;
        }

        public void setAwmBulletRange(String awmBulletRange) {
            this.awmBulletRange = awmBulletRange;
        }

        public String getRank() {
            return rank;
        }

        public void setRank(String rank) {
            this.rank = rank;
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

        public String getDeliveryMethod() {
            return deliveryMethod;
        }

        public void setDeliveryMethod(String deliveryMethod) {
            this.deliveryMethod = deliveryMethod;
        }

        public String getSellerType() {
            return sellerType;
        }

        public void setSellerType(String sellerType) {
            this.sellerType = sellerType;
        }

        public String getExchangeRateType() {
            return exchangeRateType;
        }

        public void setExchangeRateType(String exchangeRateType) {
            this.exchangeRateType = exchangeRateType;
        }

        public Boolean getNegotiable() {
            return negotiable;
        }

        public void setNegotiable(Boolean negotiable) {
            this.negotiable = negotiable;
        }

        public Boolean getAlwaysOnline() {
            return alwaysOnline;
        }

        public void setAlwaysOnline(Boolean alwaysOnline) {
            this.alwaysOnline = alwaysOnline;
        }

        public Integer getPublishedWithinDays() {
            return publishedWithinDays;
        }

        public void setPublishedWithinDays(Integer publishedWithinDays) {
            this.publishedWithinDays = publishedWithinDays;
        }

        public String getSort() {
            return StringUtils.hasText(sort) ? sort : "newest";
        }

        public void setSort(String sort) {
            this.sort = sort;
        }

        public int getPage() {
            return page == null || page.intValue() < 1 ? 1 : page.intValue();
        }

        public void setPage(Integer page) {
            this.page = page;
        }

        public int getPageSize() {
            if (pageSize == null || pageSize.intValue() < 1) {
                return 10;
            }
            return Math.min(pageSize.intValue(), 50);
        }

        public void setPageSize(Integer pageSize) {
            this.pageSize = pageSize;
        }
    }

    public static class ListingCard {
        private final String id;
        private final String seller;
        private final String avatar;
        private final String coverTone;
        private final String coverUrl;
        private final String title;
        private final List<SummaryChip> summaryChips;
        private final List<Panel> panels;
        private final Pricing pricing;
        private final String sellerType;
        private final String sellerTypeLabel;
        private final List<String> assuranceTags;
        private final List<String> highlights;
        private final Stats stats;
        private final String regionLabel;
        private final String accountLevelLabel;
        private final String hafCurrencyLabel;
        private final String exchangeRateLabel;
        private final List<ImportantFact> importantFacts;
        private final List<ExtraItemLine> extraItems;
        private final boolean negotiable;
        private final String publishedAtLabel;
        private final List<String> imageUrls;
        private final String videoUrl;
        private final String description;
        private final EstimateReport estimateReport;
        private final SellerInfo sellerInfo;
        private final GuaranteeInfo guaranteeInfo;
        private final DetailSection baseInfoSection;
        private final DetailSection assetInfoSection;
        private final DetailSection combatInfoSection;
        private final List<DetailCategoryGroup> equipmentGroups;
        private final ExportInfo exportInfo;
        private final LocalDateTime publishedTime;
        private final BigDecimal rentAmount;
        private final long hafCurrencyAmount;
        private final int rentalRatio;
        private final int awmBulletCount;
        private final int helmetLevel6Count;
        private final int armorLevel6Count;

        public ListingCard(
            String id,
            String seller,
            String avatar,
            String coverTone,
            String coverUrl,
            String title,
            List<SummaryChip> summaryChips,
            List<Panel> panels,
            Pricing pricing,
            String sellerType,
            String sellerTypeLabel,
            List<String> assuranceTags,
            List<String> highlights,
            Stats stats,
            String regionLabel,
            String accountLevelLabel,
            String hafCurrencyLabel,
            String exchangeRateLabel,
            List<ImportantFact> importantFacts,
            List<ExtraItemLine> extraItems,
            boolean negotiable,
            String publishedAtLabel,
            List<String> imageUrls,
            String videoUrl,
            String description,
            EstimateReport estimateReport,
            SellerInfo sellerInfo,
            GuaranteeInfo guaranteeInfo,
            DetailSection baseInfoSection,
            DetailSection assetInfoSection,
            DetailSection combatInfoSection,
            List<DetailCategoryGroup> equipmentGroups,
            ExportInfo exportInfo,
            long hafCurrencyAmount,
            int rentalRatio,
            int awmBulletCount,
            int helmetLevel6Count,
            int armorLevel6Count
        ) {
            this.id = id;
            this.seller = seller;
            this.avatar = avatar;
            this.coverTone = coverTone;
            this.coverUrl = coverUrl;
            this.title = title;
            this.summaryChips = summaryChips;
            this.panels = panels;
            this.pricing = pricing;
            this.sellerType = sellerType;
            this.sellerTypeLabel = sellerTypeLabel;
            this.assuranceTags = assuranceTags;
            this.highlights = highlights;
            this.stats = stats;
            this.regionLabel = regionLabel;
            this.accountLevelLabel = accountLevelLabel;
            this.hafCurrencyLabel = hafCurrencyLabel;
            this.exchangeRateLabel = exchangeRateLabel;
            this.importantFacts = importantFacts;
            this.extraItems = extraItems == null ? Collections.<ExtraItemLine>emptyList() : extraItems;
            this.negotiable = negotiable;
            this.publishedAtLabel = publishedAtLabel;
            this.imageUrls = imageUrls;
            this.videoUrl = videoUrl;
            this.description = description;
            this.estimateReport = estimateReport;
            this.sellerInfo = sellerInfo;
            this.guaranteeInfo = guaranteeInfo;
            this.baseInfoSection = baseInfoSection;
            this.assetInfoSection = assetInfoSection;
            this.combatInfoSection = combatInfoSection;
            this.equipmentGroups = equipmentGroups;
            this.exportInfo = exportInfo;
            this.publishedTime = null;
            this.rentAmount = pricing.getRentAmount();
            this.hafCurrencyAmount = hafCurrencyAmount;
            this.rentalRatio = rentalRatio;
            this.awmBulletCount = awmBulletCount;
            this.helmetLevel6Count = helmetLevel6Count;
            this.armorLevel6Count = armorLevel6Count;
        }

        public ListingCard withPublishedTime(LocalDateTime publishedTime) {
            return new ListingCard(
                id,
                seller,
                avatar,
                coverTone,
                coverUrl,
                title,
                summaryChips,
                panels,
                pricing,
                sellerType,
                sellerTypeLabel,
                assuranceTags,
                highlights,
                stats,
                regionLabel,
                accountLevelLabel,
                hafCurrencyLabel,
                exchangeRateLabel,
                importantFacts,
                extraItems,
                negotiable,
                publishedAtLabel,
                imageUrls,
                videoUrl,
                description,
                estimateReport,
                sellerInfo,
                guaranteeInfo,
                baseInfoSection,
                assetInfoSection,
                combatInfoSection,
                equipmentGroups,
                exportInfo,
                publishedTime,
                rentAmount,
                hafCurrencyAmount,
                rentalRatio,
                awmBulletCount,
                helmetLevel6Count,
                armorLevel6Count
            );
        }

        private ListingCard(
            String id,
            String seller,
            String avatar,
            String coverTone,
            String coverUrl,
            String title,
            List<SummaryChip> summaryChips,
            List<Panel> panels,
            Pricing pricing,
            String sellerType,
            String sellerTypeLabel,
            List<String> assuranceTags,
            List<String> highlights,
            Stats stats,
            String regionLabel,
            String accountLevelLabel,
            String hafCurrencyLabel,
            String exchangeRateLabel,
            List<ImportantFact> importantFacts,
            List<ExtraItemLine> extraItems,
            boolean negotiable,
            String publishedAtLabel,
            List<String> imageUrls,
            String videoUrl,
            String description,
            EstimateReport estimateReport,
            SellerInfo sellerInfo,
            GuaranteeInfo guaranteeInfo,
            DetailSection baseInfoSection,
            DetailSection assetInfoSection,
            DetailSection combatInfoSection,
            List<DetailCategoryGroup> equipmentGroups,
            ExportInfo exportInfo,
            LocalDateTime publishedTime,
            BigDecimal rentAmount,
            long hafCurrencyAmount,
            int rentalRatio,
            int awmBulletCount,
            int helmetLevel6Count,
            int armorLevel6Count
        ) {
            this.id = id;
            this.seller = seller;
            this.avatar = avatar;
            this.coverTone = coverTone;
            this.coverUrl = coverUrl;
            this.title = title;
            this.summaryChips = summaryChips;
            this.panels = panels;
            this.pricing = pricing;
            this.sellerType = sellerType;
            this.sellerTypeLabel = sellerTypeLabel;
            this.assuranceTags = assuranceTags;
            this.highlights = highlights;
            this.stats = stats;
            this.regionLabel = regionLabel;
            this.accountLevelLabel = accountLevelLabel;
            this.hafCurrencyLabel = hafCurrencyLabel;
            this.exchangeRateLabel = exchangeRateLabel;
            this.importantFacts = importantFacts;
            this.extraItems = extraItems == null ? Collections.<ExtraItemLine>emptyList() : extraItems;
            this.negotiable = negotiable;
            this.publishedAtLabel = publishedAtLabel;
            this.imageUrls = imageUrls;
            this.videoUrl = videoUrl;
            this.description = description;
            this.estimateReport = estimateReport;
            this.sellerInfo = sellerInfo;
            this.guaranteeInfo = guaranteeInfo;
            this.baseInfoSection = baseInfoSection;
            this.assetInfoSection = assetInfoSection;
            this.combatInfoSection = combatInfoSection;
            this.equipmentGroups = equipmentGroups;
            this.exportInfo = exportInfo;
            this.publishedTime = publishedTime;
            this.rentAmount = rentAmount;
            this.hafCurrencyAmount = hafCurrencyAmount;
            this.rentalRatio = rentalRatio;
            this.awmBulletCount = awmBulletCount;
            this.helmetLevel6Count = helmetLevel6Count;
            this.armorLevel6Count = armorLevel6Count;
        }

        public String getId() {
            return id;
        }

        public String getSeller() {
            return seller;
        }

        public String getAvatar() {
            return avatar;
        }

        public String getCoverTone() {
            return coverTone;
        }

        public String getCoverUrl() {
            return coverUrl;
        }

        public String getTitle() {
            return title;
        }

        public List<SummaryChip> getSummaryChips() {
            return summaryChips;
        }

        public List<Panel> getPanels() {
            return panels;
        }

        public Pricing getPricing() {
            return pricing;
        }

        public String getSellerType() {
            return sellerType;
        }

        public String getSellerTypeLabel() {
            return sellerTypeLabel;
        }

        public List<String> getAssuranceTags() {
            return assuranceTags;
        }

        public List<String> getHighlights() {
            return highlights;
        }

        public Stats getStats() {
            return stats;
        }

        public String getRegionLabel() {
            return regionLabel;
        }

        public String getAccountLevelLabel() {
            return accountLevelLabel;
        }

        public String getHafCurrencyLabel() {
            return hafCurrencyLabel;
        }

        public String getExchangeRateLabel() {
            return exchangeRateLabel;
        }

        public List<ImportantFact> getImportantFacts() {
            return importantFacts;
        }

        public List<ExtraItemLine> getExtraItems() {
            return extraItems;
        }

        public boolean isNegotiable() {
            return negotiable;
        }

        public String getPublishedAtLabel() {
            return publishedAtLabel;
        }

        public List<String> getImageUrls() {
            return imageUrls;
        }

        public String getVideoUrl() {
            return videoUrl;
        }

        public String getDescription() {
            return description;
        }

        public EstimateReport getEstimateReport() {
            return estimateReport;
        }

        public SellerInfo getSellerInfo() {
            return sellerInfo;
        }

        public GuaranteeInfo getGuaranteeInfo() {
            return guaranteeInfo;
        }

        public DetailSection getBaseInfoSection() {
            return baseInfoSection;
        }

        public DetailSection getAssetInfoSection() {
            return assetInfoSection;
        }

        public DetailSection getCombatInfoSection() {
            return combatInfoSection;
        }

        public List<DetailCategoryGroup> getEquipmentGroups() {
            return equipmentGroups;
        }

        public ExportInfo getExportInfo() {
            return exportInfo;
        }

        public LocalDateTime getPublishedTime() {
            return publishedTime;
        }

        public BigDecimal getRentAmount() {
            return rentAmount;
        }

        public long getHafCurrencyAmount() {
            return hafCurrencyAmount;
        }

        public int getRentalRatio() {
            return rentalRatio;
        }

        public int getAwmBulletCount() {
            return awmBulletCount;
        }

        public int getHelmetLevel6Count() {
            return helmetLevel6Count;
        }

        public int getArmorLevel6Count() {
            return armorLevel6Count;
        }
    }

    public static class ListingDetail {
        private final ListingCard card;
        private final String description;
        private final List<String> imageUrls;
        private final String videoUrl;

        public ListingDetail(ListingCard card, String description, List<String> imageUrls, String videoUrl) {
            this.card = card;
            this.description = description;
            this.imageUrls = imageUrls;
            this.videoUrl = videoUrl;
        }

        public ListingCard getCard() {
            return card;
        }

        public String getDescription() {
            return description;
        }

        public List<String> getImageUrls() {
            return imageUrls;
        }

        public String getVideoUrl() {
            return videoUrl;
        }
    }

    public static class ExportInfo {
        private final String deliveryMethod;
        private final String deliveryTimeRange;
        private final String knifeSkins;
        private final String redSkins;
        private final String goldSkins;
        private final String weaponSkins;
        private final String extraItems;

        public ExportInfo(
            String deliveryMethod,
            String deliveryTimeRange,
            String knifeSkins,
            String redSkins,
            String goldSkins,
            String weaponSkins,
            String extraItems
        ) {
            this.deliveryMethod = deliveryMethod;
            this.deliveryTimeRange = deliveryTimeRange;
            this.knifeSkins = knifeSkins;
            this.redSkins = redSkins;
            this.goldSkins = goldSkins;
            this.weaponSkins = weaponSkins;
            this.extraItems = extraItems;
        }

        public String getDeliveryMethod() {
            return deliveryMethod;
        }

        public String getDeliveryTimeRange() {
            return deliveryTimeRange;
        }

        public String getKnifeSkins() {
            return knifeSkins;
        }

        public String getRedSkins() {
            return redSkins;
        }

        public String getGoldSkins() {
            return goldSkins;
        }

        public String getWeaponSkins() {
            return weaponSkins;
        }

        public String getExtraItems() {
            return extraItems;
        }
    }

    public static class EstimateReport {
        private final String suggestedPrice;
        private final String basis;
        private final String estimatedAt;

        public EstimateReport(String suggestedPrice, String basis, String estimatedAt) {
            this.suggestedPrice = suggestedPrice;
            this.basis = basis;
            this.estimatedAt = estimatedAt;
        }

        public String getSuggestedPrice() {
            return suggestedPrice;
        }

        public String getBasis() {
            return basis;
        }

        public String getEstimatedAt() {
            return estimatedAt;
        }
    }

    public static class SellerInfo {
        private final String nickname;
        private final String avatarText;
        private final String sellerTypeLabel;
        private final String studioName;
        private final String favorableRate;
        private final int dealCount;
        private final int publishCount;

        public SellerInfo(
            String nickname,
            String avatarText,
            String sellerTypeLabel,
            String studioName,
            String favorableRate,
            int dealCount,
            int publishCount
        ) {
            this.nickname = nickname;
            this.avatarText = avatarText;
            this.sellerTypeLabel = sellerTypeLabel;
            this.studioName = studioName;
            this.favorableRate = favorableRate;
            this.dealCount = dealCount;
            this.publishCount = publishCount;
        }

        public String getNickname() {
            return nickname;
        }

        public String getAvatarText() {
            return avatarText;
        }

        public String getSellerTypeLabel() {
            return sellerTypeLabel;
        }

        public String getStudioName() {
            return studioName;
        }

        public String getFavorableRate() {
            return favorableRate;
        }

        public int getDealCount() {
            return dealCount;
        }

        public int getPublishCount() {
            return publishCount;
        }
    }

    public static class GuaranteeInfo {
        private final String platformReview;
        private final String afterSalePeriod;
        private final String violationPolicy;

        public GuaranteeInfo(String platformReview, String afterSalePeriod, String violationPolicy) {
            this.platformReview = platformReview;
            this.afterSalePeriod = afterSalePeriod;
            this.violationPolicy = violationPolicy;
        }

        public String getPlatformReview() {
            return platformReview;
        }

        public String getAfterSalePeriod() {
            return afterSalePeriod;
        }

        public String getViolationPolicy() {
            return violationPolicy;
        }
    }

    public static class DetailSection {
        private final String title;
        private final List<DetailEntry> entries;

        public DetailSection(String title, List<DetailEntry> entries) {
            this.title = title;
            this.entries = entries;
        }

        public String getTitle() {
            return title;
        }

        public List<DetailEntry> getEntries() {
            return entries;
        }
    }

    public static class DetailEntry {
        private final String label;
        private final String value;

        public DetailEntry(String label, String value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public String getValue() {
            return value;
        }
    }

    public static class DetailCategoryGroup {
        private final String title;
        private final List<DetailCategoryItem> items;

        public DetailCategoryGroup(String title, List<DetailCategoryItem> items) {
            this.title = title;
            this.items = items;
        }

        public String getTitle() {
            return title;
        }

        public List<DetailCategoryItem> getItems() {
            return items;
        }
    }

    public static class DetailCategoryItem {
        private final String name;
        private final String note;

        public DetailCategoryItem(String name, String note) {
            this.name = name;
            this.note = note;
        }

        public String getName() {
            return name;
        }

        public String getNote() {
            return note;
        }
    }

    public static class FavoriteToggleResult {
        private final String listingNo;
        private final boolean favorite;
        private final int favoriteCount;

        public FavoriteToggleResult(String listingNo, boolean favorite, int favoriteCount) {
            this.listingNo = listingNo;
            this.favorite = favorite;
            this.favoriteCount = favoriteCount;
        }

        public String getListingNo() {
            return listingNo;
        }

        public boolean isFavorite() {
            return favorite;
        }

        public int getFavoriteCount() {
            return favoriteCount;
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

    public static class RegionOption {
        private final String code;
        private final String label;
        private final String provinceCode;
        private final String provinceName;
        private final String cityName;

        public RegionOption(String code, String label, String provinceCode, String provinceName, String cityName) {
            this.code = code;
            this.label = label;
            this.provinceCode = provinceCode;
            this.provinceName = provinceName;
            this.cityName = cityName;
        }

        public String getCode() {
            return code;
        }

        public String getLabel() {
            return label;
        }

        public String getProvinceCode() {
            return provinceCode;
        }

        public String getProvinceName() {
            return provinceName;
        }

        public String getCityName() {
            return cityName;
        }
    }

    public static class SummaryChip {
        private final String label;
        private final String tone;

        public SummaryChip(String label, String tone) {
            this.label = label;
            this.tone = tone;
        }

        public String getLabel() {
            return label;
        }

        public String getTone() {
            return tone;
        }
    }

    public static class ImportantFact {
        private final String label;
        private final String value;
        private final boolean emphasis;

        public ImportantFact(String label, String value, boolean emphasis) {
            this.label = label;
            this.value = value;
            this.emphasis = emphasis;
        }

        public String getLabel() {
            return label;
        }

        public String getValue() {
            return value;
        }

        public boolean isEmphasis() {
            return emphasis;
        }
    }

    public static class Panel {
        private final String tone;
        private final boolean wide;
        private final List<PanelEntry> entries;

        public Panel(String tone, boolean wide, List<PanelEntry> entries) {
            this.tone = tone;
            this.wide = wide;
            this.entries = entries;
        }

        public String getTone() {
            return tone;
        }

        public boolean isWide() {
            return wide;
        }

        public List<PanelEntry> getEntries() {
            return entries;
        }
    }

    public static class PanelEntry {
        private final String label;
        private final String value;

        public PanelEntry(String label, String value) {
            this.label = label;
            this.value = value;
        }

        public String getLabel() {
            return label;
        }

        public String getValue() {
            return value;
        }
    }

    public static class Pricing {
        private final String deposit;
        private final String rent;
        private final String total;
        private final String extraItemsAmount;
        private final BigDecimal rentAmount;

        public Pricing(String deposit, String rent, String total) {
            this(deposit, rent, total, "0.00 元", parseMoneyValue(rent));
        }

        public Pricing(String deposit, String rent, String total, String extraItemsAmount) {
            this(deposit, rent, total, extraItemsAmount, parseMoneyValue(rent));
        }

        public Pricing(String deposit, String rent, String total, String extraItemsAmount, BigDecimal rentAmount) {
            this.deposit = deposit;
            this.rent = rent;
            this.total = total;
            this.extraItemsAmount = extraItemsAmount;
            this.rentAmount = rentAmount;
        }

        public String getDeposit() {
            return deposit;
        }

        public String getRent() {
            return rent;
        }

        public String getTotal() {
            return total;
        }

        public String getExtraItemsAmount() {
            return extraItemsAmount;
        }

        public BigDecimal getRentAmount() {
            return rentAmount;
        }
    }

    public static class ExtraItemLine {
        private final String label;
        private final int count;
        private final String unitPrice;
        private final String subtotal;

        public ExtraItemLine(String label, int count, String unitPrice, String subtotal) {
            this.label = label;
            this.count = count;
            this.unitPrice = unitPrice;
            this.subtotal = subtotal;
        }

        public String getLabel() {
            return label;
        }

        public int getCount() {
            return count;
        }

        public String getUnitPrice() {
            return unitPrice;
        }

        public String getSubtotal() {
            return subtotal;
        }
    }

    public static class Stats {
        private final int viewCount;
        private final int favoriteCount;
        private final int salesCount;

        public Stats(int viewCount, int favoriteCount, int salesCount) {
            this.viewCount = viewCount;
            this.favoriteCount = favoriteCount;
            this.salesCount = salesCount;
        }

        public int getViewCount() {
            return viewCount;
        }

        public int getFavoriteCount() {
            return favoriteCount;
        }

        public int getSalesCount() {
            return salesCount;
        }
    }

    private static class ListingProjection {
        private final ListingCard card;
        private final AccountListingDO row;
        private final List<String> weapons;
        private final List<String> imageUrls;
        private final PublishAttributesSnapshot attributes;

        private ListingProjection(
            ListingCard card,
            AccountListingDO row,
            List<String> weapons,
            List<String> imageUrls,
            PublishAttributesSnapshot attributes
        ) {
            this.card = card.withPublishedTime(row.getPublishedAt() != null ? row.getPublishedAt() : row.getSubmittedAt());
            this.row = row;
            this.weapons = weapons;
            this.imageUrls = imageUrls;
            this.attributes = attributes;
        }

        public ListingCard getCard() {
            return card;
        }

        public AccountListingDO getRow() {
            return row;
        }

        public List<String> getWeapons() {
            return weapons;
        }

        public List<String> getImageUrls() {
            return imageUrls;
        }

        public PublishAttributesSnapshot getAttributes() {
            return attributes;
        }
    }

    private static class SellerMetrics {
        private final int publishCount;
        private final int salesCount;

        private SellerMetrics(int publishCount, int salesCount) {
            this.publishCount = publishCount;
            this.salesCount = salesCount;
        }

        public int getPublishCount() {
            return publishCount;
        }

        public int getSalesCount() {
            return salesCount;
        }
    }

    private static class PublishAttributesSnapshot {
        private Integer deliveryStartHour;
        private Integer deliveryEndHour;
        private Integer staminaLevel;
        private Integer carryLevel;
        private Integer diveLevel;
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
        private BigDecimal deposit;
        private List<ExtraItemSnapshot> extraItems;

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

        public BigDecimal getDeposit() {
            return deposit;
        }

        public void setDeposit(BigDecimal deposit) {
            this.deposit = deposit;
        }

        public List<ExtraItemSnapshot> getExtraItems() {
            return extraItems;
        }

        public void setExtraItems(List<ExtraItemSnapshot> extraItems) {
            this.extraItems = extraItems;
        }
    }

    private static class ExtraItemSnapshot {
        private String code;
        private String label;
        private Integer count;
        private String chargeMode;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
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

        public BigDecimal getUnitPrice() {
            return unitPrice;
        }

        public void setUnitPrice(BigDecimal unitPrice) {
            this.unitPrice = unitPrice;
        }

        public BigDecimal getTotalPrice() {
            return totalPrice;
        }

        public void setTotalPrice(BigDecimal totalPrice) {
            this.totalPrice = totalPrice;
        }
    }

    private static BigDecimal parseMoneyValue(String value) {
        if (!StringUtils.hasText(value)) {
            return BigDecimal.ZERO;
        }
        String normalized = value.replace("元", "").trim();
        if (!StringUtils.hasText(normalized)) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(normalized);
    }
}
