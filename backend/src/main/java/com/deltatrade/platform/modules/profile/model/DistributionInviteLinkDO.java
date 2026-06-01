package com.deltatrade.platform.modules.profile.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("distribution_invite_link")
public class DistributionInviteLinkDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("promoter_user_id")
    private Long promoterUserId;

    @TableField("invite_code")
    private String inviteCode;

    @TableField("invite_path")
    private String invitePath;

    @TableField("poster_key")
    private String posterKey;

    @TableField("active")
    private Boolean active;

    @TableField("invalidated_at")
    private LocalDateTime invalidatedAt;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPromoterUserId() { return promoterUserId; }
    public void setPromoterUserId(Long promoterUserId) { this.promoterUserId = promoterUserId; }
    public String getInviteCode() { return inviteCode; }
    public void setInviteCode(String inviteCode) { this.inviteCode = inviteCode; }
    public String getInvitePath() { return invitePath; }
    public void setInvitePath(String invitePath) { this.invitePath = invitePath; }
    public String getPosterKey() { return posterKey; }
    public void setPosterKey(String posterKey) { this.posterKey = posterKey; }
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    public LocalDateTime getInvalidatedAt() { return invalidatedAt; }
    public void setInvalidatedAt(LocalDateTime invalidatedAt) { this.invalidatedAt = invalidatedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
