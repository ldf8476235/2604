package com.deltatrade.platform.modules.studio.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("studio_application")
public class StudioApplicationDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("application_no")
    private String applicationNo;

    @TableField("applicant_user_id")
    private Long applicantUserId;

    @TableField("studio_id")
    private Long studioId;

    @TableField("studio_name")
    private String studioName;

    @TableField("qualification_code")
    private String qualificationCode;

    @TableField("qualification_note")
    private String qualificationNote;

    @TableField("contact_name")
    private String contactName;

    @TableField("contact_phone")
    private String contactPhone;

    @TableField("qualification_material_key")
    private String qualificationMaterialKey;

    @TableField("status")
    private String status;

    @TableField("reject_reason")
    private String rejectReason;

    @TableField("reviewed_by_user_id")
    private Long reviewedByUserId;

    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getApplicationNo() { return applicationNo; }
    public void setApplicationNo(String applicationNo) { this.applicationNo = applicationNo; }
    public Long getApplicantUserId() { return applicantUserId; }
    public void setApplicantUserId(Long applicantUserId) { this.applicantUserId = applicantUserId; }
    public Long getStudioId() { return studioId; }
    public void setStudioId(Long studioId) { this.studioId = studioId; }
    public String getStudioName() { return studioName; }
    public void setStudioName(String studioName) { this.studioName = studioName; }
    public String getQualificationCode() { return qualificationCode; }
    public void setQualificationCode(String qualificationCode) { this.qualificationCode = qualificationCode; }
    public String getQualificationNote() { return qualificationNote; }
    public void setQualificationNote(String qualificationNote) { this.qualificationNote = qualificationNote; }
    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }
    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }
    public String getQualificationMaterialKey() { return qualificationMaterialKey; }
    public void setQualificationMaterialKey(String qualificationMaterialKey) { this.qualificationMaterialKey = qualificationMaterialKey; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRejectReason() { return rejectReason; }
    public void setRejectReason(String rejectReason) { this.rejectReason = rejectReason; }
    public Long getReviewedByUserId() { return reviewedByUserId; }
    public void setReviewedByUserId(Long reviewedByUserId) { this.reviewedByUserId = reviewedByUserId; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
