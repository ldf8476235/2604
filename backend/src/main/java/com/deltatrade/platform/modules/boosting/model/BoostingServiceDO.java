package com.deltatrade.platform.modules.boosting.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("boosting_service")
public class BoostingServiceDO {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("service_no")
    private String serviceNo;

    @TableField("category_code")
    private String categoryCode;

    @TableField("category_label")
    private String categoryLabel;

    @TableField("name")
    private String name;

    @TableField("description")
    private String description;

    @TableField("price")
    private BigDecimal price;

    @TableField("cycle_code")
    private String cycleCode;

    @TableField("cycle_label")
    private String cycleLabel;

    @TableField("guarantee_note")
    private String guaranteeNote;

    @TableField("provider_type")
    private String providerType;

    @TableField("provider_name")
    private String providerName;

    @TableField("sales_count")
    private Integer salesCount;

    @TableField("status")
    private String status;

    @TableField("distribution_commission_rate")
    private BigDecimal distributionCommissionRate;

    @TableField("sort_no")
    private Integer sortNo;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getServiceNo() { return serviceNo; }
    public void setServiceNo(String serviceNo) { this.serviceNo = serviceNo; }
    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    public String getCategoryLabel() { return categoryLabel; }
    public void setCategoryLabel(String categoryLabel) { this.categoryLabel = categoryLabel; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public String getCycleCode() { return cycleCode; }
    public void setCycleCode(String cycleCode) { this.cycleCode = cycleCode; }
    public String getCycleLabel() { return cycleLabel; }
    public void setCycleLabel(String cycleLabel) { this.cycleLabel = cycleLabel; }
    public String getGuaranteeNote() { return guaranteeNote; }
    public void setGuaranteeNote(String guaranteeNote) { this.guaranteeNote = guaranteeNote; }
    public String getProviderType() { return providerType; }
    public void setProviderType(String providerType) { this.providerType = providerType; }
    public String getProviderName() { return providerName; }
    public void setProviderName(String providerName) { this.providerName = providerName; }
    public Integer getSalesCount() { return salesCount; }
    public void setSalesCount(Integer salesCount) { this.salesCount = salesCount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getDistributionCommissionRate() { return distributionCommissionRate; }
    public void setDistributionCommissionRate(BigDecimal distributionCommissionRate) { this.distributionCommissionRate = distributionCommissionRate; }
    public Integer getSortNo() { return sortNo; }
    public void setSortNo(Integer sortNo) { this.sortNo = sortNo; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
