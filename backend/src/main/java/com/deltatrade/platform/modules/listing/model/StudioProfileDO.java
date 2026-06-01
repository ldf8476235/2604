package com.deltatrade.platform.modules.listing.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("studio_profile")
public class StudioProfileDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("owner_user_id")
    private Long ownerUserId;

    @TableField("studio_name")
    private String studioName;

    @TableField("description")
    private String description;

    @TableField("contact_phone")
    private String contactPhone;

    @TableField("contact_name")
    private String contactName;

    @TableField("contact_wechat")
    private String contactWechat;

    @TableField("qualification_code")
    private String qualificationCode;

    @TableField("qualification_material_key")
    private String qualificationMaterialKey;

    @TableField("qualification_note")
    private String qualificationNote;

    @TableField("review_strategy")
    private String reviewStrategy;

    @TableField("share_ratio")
    private BigDecimal shareRatio;

    @TableField("distribution_commission_rate")
    private BigDecimal distributionCommissionRate;

    @TableField("active")
    private Boolean active;

    @TableField("cooperation_status")
    private String cooperationStatus;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(Long ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getStudioName() {
        return studioName;
    }

    public void setStudioName(String studioName) {
        this.studioName = studioName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactWechat() {
        return contactWechat;
    }

    public void setContactWechat(String contactWechat) {
        this.contactWechat = contactWechat;
    }

    public String getQualificationCode() {
        return qualificationCode;
    }

    public void setQualificationCode(String qualificationCode) {
        this.qualificationCode = qualificationCode;
    }

    public String getQualificationMaterialKey() {
        return qualificationMaterialKey;
    }

    public void setQualificationMaterialKey(String qualificationMaterialKey) {
        this.qualificationMaterialKey = qualificationMaterialKey;
    }

    public String getQualificationNote() {
        return qualificationNote;
    }

    public void setQualificationNote(String qualificationNote) {
        this.qualificationNote = qualificationNote;
    }

    public String getReviewStrategy() {
        return reviewStrategy;
    }

    public void setReviewStrategy(String reviewStrategy) {
        this.reviewStrategy = reviewStrategy;
    }

    public BigDecimal getShareRatio() {
        return shareRatio;
    }

    public void setShareRatio(BigDecimal shareRatio) {
        this.shareRatio = shareRatio;
    }

    public BigDecimal getDistributionCommissionRate() {
        return distributionCommissionRate;
    }

    public void setDistributionCommissionRate(BigDecimal distributionCommissionRate) {
        this.distributionCommissionRate = distributionCommissionRate;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getCooperationStatus() {
        return cooperationStatus;
    }

    public void setCooperationStatus(String cooperationStatus) {
        this.cooperationStatus = cooperationStatus;
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
}
