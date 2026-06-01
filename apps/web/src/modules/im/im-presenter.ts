import type { ImConversation, ImMessage } from "./im-api";

export type ImTimelineEntry =
  | { kind: "divider"; key: string; label: string }
  | { kind: "message"; key: string; message: ImMessage };

export function buildImTimeline(messages: ImMessage[]): ImTimelineEntry[] {
  const timeline: ImTimelineEntry[] = [];
  let lastDateKey = "";

  messages.forEach((message) => {
    const dateKey = extractDateKey(message.createdAt);
    if (dateKey !== lastDateKey) {
      timeline.push({
        kind: "divider",
        key: `divider-${dateKey}-${message.id}`,
        label: formatDateLabel(dateKey),
      });
      lastDateKey = dateKey;
    }
    timeline.push({
      kind: "message",
      key: `message-${message.id}`,
      message,
    });
  });

  return timeline;
}

export function getImQuickReplies(sceneCode: string | undefined, role: "user" | "support"): string[] {
  if (role === "support") {
    if (sceneCode === "TRADE_ORDER") {
      return ["已收到，我先核对订单信息。", "双方可以继续在群聊确认细节。", "确认无误后请按流程完成支付。"]; 
    }
    if (sceneCode === "BOOSTING_ORDER") {
      return ["已收到需求，我先核对服务信息。", "请补充当前段位、区服和目标要求。", "服务开始后我会持续同步进度。"]; 
    }
    return ["已收到消息，我来协助处理。", "请补充具体问题和订单信息。", "我先帮你核对当前会话记录。"]; 
  }

  if (sceneCode === "TRADE_ORDER") {
    return ["你好，我想确认一下交易细节。", "当前账号资料能再补充一点吗？", "确认无误后我这边可以继续下单。"]; 
  }
  if (sceneCode === "BOOSTING_ORDER") {
    return ["你好，我想确认一下代肝进度。", "当前服务什么时候可以开始？", "有最新进度的话麻烦同步我一下。"]; 
  }
  return ["你好，我想继续咨询一下。", "麻烦帮我看下这个问题。", "方便的话请尽快回复，谢谢。"]; 
}

export function getConversationLastActivity(conversation: ImConversation | null) {
  const latest = conversation?.messages[conversation.messages.length - 1];
  return latest?.createdAt ?? "暂无消息";
}

function extractDateKey(value: string) {
  if (!value) {
    return "unknown";
  }
  const normalized = value.replace(/\./g, "-").replace(/\//g, "-");
  const match = normalized.match(/^(\d{4}-\d{2}-\d{2})/);
  return match?.[1] ?? normalized.slice(0, 10);
}

function formatDateLabel(dateKey: string) {
  if (dateKey === "unknown") {
    return "会话记录";
  }
  const today = new Date();
  const pad = (value: number) => String(value).padStart(2, "0");
  const todayKey = `${today.getFullYear()}-${pad(today.getMonth() + 1)}-${pad(today.getDate())}`;
  if (dateKey === todayKey) {
    return "今天";
  }
  const yesterday = new Date(today);
  yesterday.setDate(today.getDate() - 1);
  const yesterdayKey = `${yesterday.getFullYear()}-${pad(yesterday.getMonth() + 1)}-${pad(yesterday.getDate())}`;
  if (dateKey === yesterdayKey) {
    return "昨天";
  }
  return dateKey;
}
