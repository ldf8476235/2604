package com.deltatrade.platform.modules.auth.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("auth_user")
public class AuthUserDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("phone")
    private String phone;

    @TableField("nickname")
    private String nickname;

    @TableField("password_hash")
    private String passwordHash;

    @TableField("open_id")
    private String openId;

    @TableField("avatar_key")
    private String avatarKey;

    @TableField("account_status")
    private String accountStatus;

    @TableField("ban_reason")
    private String banReason;

    @TableField("real_name")
    private String realName;

    @TableField("real_name_phone")
    private String realNamePhone;

    @TableField("id_card_no")
    private String idCardNo;

    @TableField("verified")
    private Boolean verified;

    @TableField("real_name_status")
    private String realNameStatus;

    @TableField("real_name_reject_reason")
    private String realNameRejectReason;

    @TableField("real_name_front_key")
    private String realNameFrontKey;

    @TableField("real_name_back_key")
    private String realNameBackKey;

    @TableField("login_alert_enabled")
    private Boolean loginAlertEnabled;

    @TableField("secondary_verify_enabled")
    private Boolean secondaryVerifyEnabled;

    @TableField("distribution_enabled")
    private Boolean distributionEnabled;

    @TableField("distribution_opened_at")
    private LocalDateTime distributionOpenedAt;

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

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getOpenId() {
        return openId;
    }

    public void setOpenId(String openId) {
        this.openId = openId;
    }

    public String getAvatarKey() {
        return avatarKey;
    }

    public void setAvatarKey(String avatarKey) {
        this.avatarKey = avatarKey;
    }

    public String getAccountStatus() {
        return accountStatus;
    }

    public void setAccountStatus(String accountStatus) {
        this.accountStatus = accountStatus;
    }

    public String getBanReason() {
        return banReason;
    }

    public void setBanReason(String banReason) {
        this.banReason = banReason;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getRealNamePhone() {
        return realNamePhone;
    }

    public void setRealNamePhone(String realNamePhone) {
        this.realNamePhone = realNamePhone;
    }

    public String getIdCardNo() {
        return idCardNo;
    }

    public void setIdCardNo(String idCardNo) {
        this.idCardNo = idCardNo;
    }

    public Boolean getVerified() {
        return verified;
    }

    public void setVerified(Boolean verified) {
        this.verified = verified;
    }

    public String getRealNameStatus() {
        return realNameStatus;
    }

    public void setRealNameStatus(String realNameStatus) {
        this.realNameStatus = realNameStatus;
    }

    public String getRealNameRejectReason() {
        return realNameRejectReason;
    }

    public void setRealNameRejectReason(String realNameRejectReason) {
        this.realNameRejectReason = realNameRejectReason;
    }

    public String getRealNameFrontKey() {
        return realNameFrontKey;
    }

    public void setRealNameFrontKey(String realNameFrontKey) {
        this.realNameFrontKey = realNameFrontKey;
    }

    public String getRealNameBackKey() {
        return realNameBackKey;
    }

    public void setRealNameBackKey(String realNameBackKey) {
        this.realNameBackKey = realNameBackKey;
    }

    public Boolean getLoginAlertEnabled() {
        return loginAlertEnabled;
    }

    public void setLoginAlertEnabled(Boolean loginAlertEnabled) {
        this.loginAlertEnabled = loginAlertEnabled;
    }

    public Boolean getSecondaryVerifyEnabled() {
        return secondaryVerifyEnabled;
    }

    public void setSecondaryVerifyEnabled(Boolean secondaryVerifyEnabled) {
        this.secondaryVerifyEnabled = secondaryVerifyEnabled;
    }

    public Boolean getDistributionEnabled() {
        return distributionEnabled;
    }

    public void setDistributionEnabled(Boolean distributionEnabled) {
        this.distributionEnabled = distributionEnabled;
    }

    public LocalDateTime getDistributionOpenedAt() {
        return distributionOpenedAt;
    }

    public void setDistributionOpenedAt(LocalDateTime distributionOpenedAt) {
        this.distributionOpenedAt = distributionOpenedAt;
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
