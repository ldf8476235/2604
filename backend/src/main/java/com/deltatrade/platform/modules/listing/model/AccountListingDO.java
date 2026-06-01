package com.deltatrade.platform.modules.listing.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("account_listing")
public class AccountListingDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("listing_no")
    private String listingNo;

    @TableField("seller_user_id")
    private Long sellerUserId;

    @TableField("seller_nickname")
    private String sellerNickname;

    @TableField("seller_type")
    private String sellerType;

    @TableField("studio_name")
    private String studioName;

    @TableField("review_strategy")
    private String reviewStrategy;

    @TableField("status")
    private String status;

    @TableField("province_code")
    private String provinceCode;

    @TableField("province_name")
    private String provinceName;

    @TableField("city_code")
    private String cityCode;

    @TableField("city_name")
    private String cityName;

    @TableField("game_server")
    private String gameServer;

    @TableField("delivery_method")
    private String deliveryMethod;

    @TableField("always_online")
    private Boolean alwaysOnline;

    @TableField("account_level")
    private Integer accountLevel;

    @TableField("rank_name")
    private String rankName;

    @TableField("safe_box_level")
    private Integer safeBoxLevel;

    @TableField("haf_currency")
    private Long hafCurrency;

    @TableField("title")
    private String title;

    @TableField("description")
    private String description;

    @TableField("operator_count")
    private Integer operatorCount;

    @TableField("operators_json")
    private String operatorsJson;

    @TableField("weapons_json")
    private String weaponsJson;

    @TableField("weapon_skins_json")
    private String weaponSkinsJson;

    @TableField("other_items")
    private String otherItems;

    @TableField("image_keys_json")
    private String imageKeysJson;

    @TableField("video_key")
    private String videoKey;

    @TableField("price")
    private BigDecimal price;

    @TableField("negotiable")
    private Boolean negotiable;

    @TableField("mod_codes_json")
    private String modCodesJson;

    @TableField("publish_attributes_json")
    private String publishAttributesJson;

    @TableField("view_count")
    private Integer viewCount;

    @TableField("favorite_count")
    private Integer favoriteCount;

    @TableField("sales_count")
    private Integer salesCount;

    @TableField("cover_image_key")
    private String coverImageKey;

    @TableField("suggested_price")
    private BigDecimal suggestedPrice;

    @TableField("distribution_commission_rate")
    private BigDecimal distributionCommissionRate;

    @TableField("estimate_detail")
    private String estimateDetail;

    @TableField("rejection_reason")
    private String rejectionReason;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("submitted_at")
    private LocalDateTime submittedAt;

    @TableField("published_at")
    private LocalDateTime publishedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getListingNo() {
        return listingNo;
    }

    public void setListingNo(String listingNo) {
        this.listingNo = listingNo;
    }

    public Long getSellerUserId() {
        return sellerUserId;
    }

    public void setSellerUserId(Long sellerUserId) {
        this.sellerUserId = sellerUserId;
    }

    public String getSellerNickname() {
        return sellerNickname;
    }

    public void setSellerNickname(String sellerNickname) {
        this.sellerNickname = sellerNickname;
    }

    public String getSellerType() {
        return sellerType;
    }

    public void setSellerType(String sellerType) {
        this.sellerType = sellerType;
    }

    public String getStudioName() {
        return studioName;
    }

    public void setStudioName(String studioName) {
        this.studioName = studioName;
    }

    public String getReviewStrategy() {
        return reviewStrategy;
    }

    public void setReviewStrategy(String reviewStrategy) {
        this.reviewStrategy = reviewStrategy;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getGameServer() {
        return gameServer;
    }

    public void setGameServer(String gameServer) {
        this.gameServer = gameServer;
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

    public String getProvinceCode() {
        return provinceCode;
    }

    public void setProvinceCode(String provinceCode) {
        this.provinceCode = provinceCode;
    }

    public String getProvinceName() {
        return provinceName;
    }

    public void setProvinceName(String provinceName) {
        this.provinceName = provinceName;
    }

    public String getCityCode() {
        return cityCode;
    }

    public void setCityCode(String cityCode) {
        this.cityCode = cityCode;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
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

    public Long getHafCurrency() {
        return hafCurrency;
    }

    public void setHafCurrency(Long hafCurrency) {
        this.hafCurrency = hafCurrency;
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

    public String getOperatorsJson() {
        return operatorsJson;
    }

    public void setOperatorsJson(String operatorsJson) {
        this.operatorsJson = operatorsJson;
    }

    public String getWeaponsJson() {
        return weaponsJson;
    }

    public void setWeaponsJson(String weaponsJson) {
        this.weaponsJson = weaponsJson;
    }

    public String getWeaponSkinsJson() {
        return weaponSkinsJson;
    }

    public void setWeaponSkinsJson(String weaponSkinsJson) {
        this.weaponSkinsJson = weaponSkinsJson;
    }

    public String getOtherItems() {
        return otherItems;
    }

    public void setOtherItems(String otherItems) {
        this.otherItems = otherItems;
    }

    public String getImageKeysJson() {
        return imageKeysJson;
    }

    public void setImageKeysJson(String imageKeysJson) {
        this.imageKeysJson = imageKeysJson;
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

    public Boolean getNegotiable() {
        return negotiable;
    }

    public void setNegotiable(Boolean negotiable) {
        this.negotiable = negotiable;
    }

    public String getModCodesJson() {
        return modCodesJson;
    }

    public void setModCodesJson(String modCodesJson) {
        this.modCodesJson = modCodesJson;
    }

    public String getPublishAttributesJson() {
        return publishAttributesJson;
    }

    public void setPublishAttributesJson(String publishAttributesJson) {
        this.publishAttributesJson = publishAttributesJson;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }

    public Integer getFavoriteCount() {
        return favoriteCount;
    }

    public void setFavoriteCount(Integer favoriteCount) {
        this.favoriteCount = favoriteCount;
    }

    public Integer getSalesCount() {
        return salesCount;
    }

    public void setSalesCount(Integer salesCount) {
        this.salesCount = salesCount;
    }

    public String getCoverImageKey() {
        return coverImageKey;
    }

    public void setCoverImageKey(String coverImageKey) {
        this.coverImageKey = coverImageKey;
    }

    public BigDecimal getSuggestedPrice() {
        return suggestedPrice;
    }

    public void setSuggestedPrice(BigDecimal suggestedPrice) {
        this.suggestedPrice = suggestedPrice;
    }

    public BigDecimal getDistributionCommissionRate() {
        return distributionCommissionRate;
    }

    public void setDistributionCommissionRate(BigDecimal distributionCommissionRate) {
        this.distributionCommissionRate = distributionCommissionRate;
    }

    public String getEstimateDetail() {
        return estimateDetail;
    }

    public void setEstimateDetail(String estimateDetail) {
        this.estimateDetail = estimateDetail;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }
}
