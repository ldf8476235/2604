package com.deltatrade.platform.modules.admin.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

@TableName("operation_shortcut")
public class OperationShortcutDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    @TableField("shortcut_no")
    private String shortcutNo;
    @TableField("name")
    private String name;
    @TableField("icon_key")
    private String iconKey;
    @TableField("link_url")
    private String linkUrl;
    @TableField("sort_no")
    private Integer sortNo;
    @TableField("status")
    private String status;
    @TableField("created_at")
    private LocalDateTime createdAt;
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getShortcutNo() { return shortcutNo; }
    public void setShortcutNo(String shortcutNo) { this.shortcutNo = shortcutNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getIconKey() { return iconKey; }
    public void setIconKey(String iconKey) { this.iconKey = iconKey; }
    public String getLinkUrl() { return linkUrl; }
    public void setLinkUrl(String linkUrl) { this.linkUrl = linkUrl; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
