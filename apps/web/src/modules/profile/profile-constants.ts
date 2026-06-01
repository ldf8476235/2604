export type ProfileMenuKey = "orders" | "myListings" | "verify" | "studioApply" | "wallet" | "coupons" | "messages" | "officialNotice" | "settings" | "distributor";

export const PROFILE_MENU_ITEMS: { key: ProfileMenuKey; label: string; description: string }[] = [
  { key: "orders", label: "订单中心", description: "查看购买订单、出售订单和交易进度明细" },
  { key: "myListings", label: "我的发布", description: "管理已发布账号，支持查看状态、编辑和下架" },
  { key: "verify", label: "实名认证", description: "提交实名资料、查看审核状态与权益说明" },
  { key: "studioApply", label: "申请成为工作室", description: "提交入驻资料、查看审核进度与驳回原因" },
  { key: "wallet", label: "我的钱包", description: "查看余额、冻结金额和充值提现记录" },
  { key: "coupons", label: "优惠券中心", description: "查看可用优惠券、历史使用记录和失效券" },
  { key: "messages", label: "我的消息", description: "统一查看系统通知、交易消息和客服提醒" },
  { key: "officialNotice", label: "官方公告", description: "查看平台公告、活动通知和更新说明" },
  { key: "settings", label: "设置与安全", description: "集中管理账号信息、绑定关系和安全策略" },
  { key: "distributor", label: "分销商", description: "管理推广链接、佣金统计和分销订单" },
];
