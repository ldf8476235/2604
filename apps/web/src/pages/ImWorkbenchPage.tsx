import { Button, StatusState } from "@delta/ui";
import { useEffect, useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../auth/auth-context";
import {
  loadWorkbenchConversation,
  loadWorkbenchConversations,
  sendWorkbenchMessage,
  type ImConversation,
  type ImWorkbenchConversationItem,
  type ImWorkbenchRefreshEvent,
  type ImWorkbenchResponse,
} from "../modules/im/im-api";
import { buildImTimeline, getConversationLastActivity, getImQuickReplies } from "../modules/im/im-presenter";
import { createImRealtimeSession, type ImRealtimeState } from "../modules/im/im-realtime";

type LoadStatus = "idle" | "loading" | "success" | "error";

export function ImWorkbenchPage() {
  const { isAuthenticated, openAuthModal } = useAuth();
  const [sceneCode, setSceneCode] = useState<"" | "TRADE_ORDER" | "BOOSTING_ORDER" | "LISTING_CONSULT">("");
  const [keywordDraft, setKeywordDraft] = useState("");
  const [keyword, setKeyword] = useState("");
  const [loadStatus, setLoadStatus] = useState<LoadStatus>("idle");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [workbench, setWorkbench] = useState<ImWorkbenchResponse | null>(null);
  const [activeConversationNo, setActiveConversationNo] = useState("");
  const [conversation, setConversation] = useState<ImConversation | null>(null);
  const [conversationStatus, setConversationStatus] = useState<LoadStatus>("idle");
  const [conversationError, setConversationError] = useState("");
  const [messageText, setMessageText] = useState("");
  const [sending, setSending] = useState(false);
  const [realtimeState, setRealtimeState] = useState<ImRealtimeState>("connecting");
  const listRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!isAuthenticated) {
      return;
    }
    let active = true;
    setLoadStatus("loading");
    setError("");
    loadWorkbenchConversations({
      keyword: keyword || undefined,
      sceneCode: sceneCode || undefined,
    })
      .then((payload) => {
        if (!active) {
          return;
        }
        setWorkbench(payload);
        setLoadStatus("success");
        setActiveConversationNo((current) => {
          if (current && payload.rows.some((item) => item.conversationNo === current)) {
            return current;
          }
          return payload.rows[0]?.conversationNo ?? "";
        });
      })
      .catch((requestError) => {
        if (!active) {
          return;
        }
        setError(getErrorMessage(requestError));
        setLoadStatus("error");
      });
    return () => {
      active = false;
    };
  }, [isAuthenticated, keyword, sceneCode]);

  useEffect(() => {
    if (!isAuthenticated || !activeConversationNo) {
      setConversation(null);
      setConversationStatus("idle");
      setConversationError("");
      return;
    }
    let active = true;
    async function bootstrap(silent?: boolean) {
      try {
        if (!silent) {
          setConversationStatus("loading");
        }
        setConversationError("");
        const payload = await loadWorkbenchConversation(activeConversationNo);
        if (!active) {
          return;
        }
        setConversation(payload);
        setConversationStatus("success");
      } catch (requestError) {
        if (!active) {
          return;
        }
        setConversationError(getErrorMessage(requestError));
        setConversationStatus("error");
      }
    }
    void bootstrap();
    const disposeRealtime = createImRealtimeSession({
      subscriptions: [
        {
          topic: "/topic/im/workbench",
          onMessage: (payload) => {
            const event = payload as ImWorkbenchRefreshEvent;
            if (!event?.conversationNo) {
              return;
            }
            void refreshWorkbench(true);
            if (event.conversationNo === activeConversationNo) {
              void bootstrap(true);
            }
          },
        },
        {
          topic: `/topic/im/${activeConversationNo}`,
          onMessage: () => {
            void bootstrap(true);
          },
        },
      ],
      onStateChange: setRealtimeState,
    });

    return () => {
      active = false;
      disposeRealtime();
    };
  }, [activeConversationNo, isAuthenticated]);

  useEffect(() => {
    if (!conversation?.messages.length) {
      return;
    }
    listRef.current?.scrollTo({ top: listRef.current.scrollHeight, behavior: "smooth" });
  }, [conversation?.messages.length]);

  useEffect(() => {
    if (!notice) {
      return undefined;
    }
    const timer = window.setTimeout(() => setNotice(""), 2000);
    return () => window.clearTimeout(timer);
  }, [notice]);

  const activeConversation = useMemo(
    () => workbench?.rows.find((item) => item.conversationNo === activeConversationNo) ?? null,
    [activeConversationNo, workbench?.rows],
  );

  const canSend = messageText.trim().length > 0 && !sending;
  const timeline = useMemo(() => buildImTimeline(conversation?.messages ?? []), [conversation?.messages]);
  const quickReplies = useMemo(() => getImQuickReplies(conversation?.sceneCode ?? activeConversation?.sceneCode, "support"), [activeConversation?.sceneCode, conversation?.sceneCode]);
  const lastActivity = useMemo(() => getConversationLastActivity(conversation), [conversation]);

  if (!isAuthenticated) {
    return (
      <main className="im-page">
        <div className="dt-container im-shell">
          <StatusState
            title="登录后才能进入客服工作台"
            description="客服工作台会展示代肝和交易会话，需要先登录。"
            action={<Button onClick={() => openAuthModal("login")}>立即登录</Button>}
          />
        </div>
      </main>
    );
  }

  return (
    <main className="im-page im-page--workbench">
      <div className="dt-container im-shell">
        <section className="im-workbench">
          <aside className="im-workbench__sidebar">
            <div className="im-sidebar__head">
              <p className="boosting-hero__eyebrow">客服工作台</p>
              <h1>{workbench?.supportDisplayName ?? "平台客服"}</h1>
              <p>管理账号交易与代肝会话，实时查看最新消息并直接回复用户。</p>
            </div>

            <div className="im-sidebar__summary">
              <div className="im-summary-chip">总会话 {workbench?.totalCount ?? 0}</div>
              <div className="im-summary-chip">交易 {workbench?.tradeCount ?? 0}</div>
              <div className="im-summary-chip">代肝 {workbench?.boostingCount ?? 0}</div>
              <div className={`im-summary-chip im-summary-chip--${realtimeState}`}>实时 {renderRealtimeLabel(realtimeState)}</div>
            </div>

            <div className="im-sidebar__panel">
              <div className="im-sidebar__panel-row">
                <span>当前会话</span>
                <strong>{activeConversation?.title ?? "未选择"}</strong>
              </div>
              <div className="im-sidebar__panel-row">
                <span>最近活跃</span>
                <strong>{lastActivity}</strong>
              </div>
              <div className="im-sidebar__panel-row">
                <span>会话编号</span>
                <strong>{activeConversation?.sourceOrderNo ?? "—"}</strong>
              </div>
            </div>

            <div className="im-workbench__filters">
              <input
                placeholder="搜索订单号 / 会话号 / 买家"
                value={keywordDraft}
                onChange={(event) => setKeywordDraft(event.target.value)}
              />
              <select value={sceneCode} onChange={(event) => setSceneCode(event.target.value as "" | "TRADE_ORDER" | "BOOSTING_ORDER" | "LISTING_CONSULT")}>
                <option value="">全部会话</option>
                <option value="TRADE_ORDER">交易群聊</option>
                <option value="LISTING_CONSULT">售前咨询</option>
                <option value="BOOSTING_ORDER">代肝客服</option>
              </select>
              <div className="im-workbench__filter-actions">
                <button
                  className="dt-button dt-button--secondary"
                  onClick={() => {
                    setKeywordDraft("");
                    setKeyword("");
                    setSceneCode("");
                    setNotice("");
                  }}
                  type="button"
                >
                  重置
                </button>
                <button
                  className="dt-button dt-button--primary"
                  onClick={() => {
                    setKeyword(keywordDraft.trim());
                    setNotice("");
                  }}
                  type="button"
                >
                  筛选
                </button>
              </div>
            </div>

            {loadStatus === "loading" || loadStatus === "idle" ? (
              <StatusState title="会话列表加载中" description="正在同步当前所有订单会话。" />
            ) : loadStatus === "error" ? (
              <StatusState title="工作台加载失败" description={error} tone="error" />
            ) : workbench?.rows.length ? (
              <div className="im-workbench__list">
                {workbench.rows.map((item) => (
                  <button
                    className={`im-workbench__item ${item.conversationNo === activeConversationNo ? "is-active" : ""}`}
                    key={item.conversationNo}
                    onClick={() => {
                      setActiveConversationNo(item.conversationNo);
                      setNotice("");
                    }}
                    type="button"
                  >
                    <div className="im-workbench__item-head">
                      <strong>{item.title}</strong>
                      <span>{item.lastMessageAt}</span>
                    </div>
                    <div className="im-workbench__item-meta">
                      <span>{item.sceneLabel}</span>
                      <span>{item.statusLabel}</span>
                    </div>
                    <p>{item.lastMessageExcerpt}</p>
                    <small>
                      {item.sourceOrderNo} · {item.buyerName} / {item.counterpartyName}
                    </small>
                  </button>
                ))}
              </div>
            ) : (
              <StatusState title="暂无会话" description="当前筛选条件下没有可处理的代肝或交易会话。" />
            )}
          </aside>

          <section className="im-chat-card">
            <header className="im-chat-card__header">
              <div className="im-chat-card__header-main">
                <strong>{conversation?.title ?? activeConversation?.title ?? "请选择会话"}</strong>
                <p>
                  {conversation?.sceneLabel ?? activeConversation?.sceneLabel ?? "客服会话"} · 订单号{" "}
                  {conversation?.sourceOrderNo ?? activeConversation?.sourceOrderNo ?? "—"}
                </p>
              </div>
              <div className="im-chat-card__presence">
                <span className={`im-chat-card__presence-dot im-chat-card__presence-dot--${realtimeState}`} />
                <strong>{renderRealtimeLabel(realtimeState)}</strong>
                <small>{sending ? "回复发送中" : "消息实时同步到用户端"}</small>
              </div>
              {activeConversation ? (
                <Link className="im-chat-card__link" to={`/im/${activeConversation.conversationNo}`}>
                  用户视角预览
                </Link>
              ) : null}
            </header>

            {notice ? (
              <div className="app-toast" role="status" aria-live="polite">
                <span>{notice}</span>
                <button onClick={() => setNotice("")} type="button">关闭</button>
              </div>
            ) : null}

            {conversationStatus === "loading" || conversationStatus === "idle" ? (
              <div className="im-chat-card__status">
                <StatusState title="会话详情加载中" description="正在同步聊天记录与订单摘要。" />
              </div>
            ) : conversationStatus === "error" ? (
              <div className="im-chat-card__status">
                <StatusState title="会话详情加载失败" description={conversationError} tone="error" />
              </div>
            ) : conversation ? (
              <>
                <div className="im-message-list" ref={listRef}>
                  {timeline.map((entry) =>
                    entry.kind === "divider" ? (
                      <div className="im-message-divider" key={entry.key}>
                        <span>{entry.label}</span>
                      </div>
                    ) : (
                      <article className={`im-message ${entry.message.mine ? "is-mine" : ""} ${entry.message.messageType === "SYSTEM" ? "is-system" : ""}`} key={entry.key}>
                        {!entry.message.mine && entry.message.messageType !== "SYSTEM" ? (
                          entry.message.avatarUrl ? <img alt={entry.message.senderName} className="im-message__avatar" src={entry.message.avatarUrl} /> : <span className="im-message__avatar im-message__avatar--placeholder">{entry.message.senderName.slice(0, 1)}</span>
                        ) : null}
                        <div className="im-message__bubble">
                          <div className="im-message__meta">
                            <strong>{entry.message.senderName}</strong>
                            <span>{entry.message.senderRoleLabel}</span>
                            <time>{entry.message.createdAt}</time>
                          </div>
                          {entry.message.text ? <p className="im-message__text">{entry.message.text}</p> : null}
                          {entry.message.messageType === "IMAGE" && entry.message.fileUrl ? (
                            <a className="im-message__media" href={entry.message.fileUrl} rel="noreferrer" target="_blank">
                              <img alt={entry.message.fileName ?? "图片消息"} src={entry.message.fileUrl} />
                            </a>
                          ) : null}
                          {entry.message.messageType === "FILE" && entry.message.fileUrl ? (
                            <a className="im-message__file" href={entry.message.fileUrl} rel="noreferrer" target="_blank">
                              <span>文件</span>
                              <strong>{entry.message.fileName ?? "下载附件"}</strong>
                            </a>
                          ) : null}
                        </div>
                      </article>
                    ),
                  )}
                </div>

                <div className="im-composer">
                  <div className="im-composer__quick">
                    {quickReplies.map((reply) => (
                      <button className="im-quick-reply" key={reply} onClick={() => setMessageText(reply)} type="button">
                        {reply}
                      </button>
                    ))}
                  </div>
                  <textarea
                    maxLength={500}
                    placeholder="输入客服回复，按 Enter 发送，Shift + Enter 换行"
                    value={messageText}
                    onChange={(event) => setMessageText(event.target.value)}
                    onKeyDown={(event) => {
                      if (event.key === "Enter" && !event.shiftKey) {
                        event.preventDefault();
                        if (canSend) {
                          void handleSendText();
                        }
                      }
                    }}
                  />
                  <div className="im-composer__footer">
                    <span>当前回复会实时同步到用户会话 · {messageText.length}/500</span>
                    <button className="dt-button dt-button--primary" disabled={!canSend || !activeConversationNo} onClick={() => void handleSendText()} type="button">
                      {sending ? "发送中..." : "发送回复"}
                    </button>
                  </div>
                </div>
              </>
            ) : (
              <div className="im-chat-card__status">
                <StatusState title="请选择左侧会话" description="选中会话后即可查看历史消息并开始回复。" />
              </div>
            )}
          </section>
        </section>
      </div>
    </main>
  );

  async function refreshWorkbench(silent?: boolean) {
    try {
      if (!silent) {
        setLoadStatus("loading");
      }
      const payload = await loadWorkbenchConversations({
        keyword: keyword || undefined,
        sceneCode: sceneCode || undefined,
      });
      setWorkbench(payload);
      setLoadStatus("success");
      setActiveConversationNo((current) => {
        if (current && payload.rows.some((item) => item.conversationNo === current)) {
          return current;
        }
        return payload.rows[0]?.conversationNo ?? "";
      });
    } catch (requestError) {
      setError(getErrorMessage(requestError));
      setLoadStatus("error");
    }
  }

  async function handleSendText() {
    if (!activeConversationNo) {
      return;
    }
    const text = messageText.trim();
    if (!text) {
      return;
    }
    try {
      setSending(true);
      setNotice("");
      const payload = await sendWorkbenchMessage(activeConversationNo, { text });
      setConversation(payload);
      setMessageText("");
      void refreshWorkbench(true);
    } catch (requestError) {
      setNotice(getErrorMessage(requestError));
    } finally {
      setSending(false);
    }
  }
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : "请求失败，请稍后再试";
}

function renderRealtimeLabel(state: ImRealtimeState) {
  if (state === "connected") {
    return "已连接";
  }
  if (state === "reconnecting") {
    return "重连中";
  }
  if (state === "error") {
    return "异常";
  }
  if (state === "closed") {
    return "已关闭";
  }
  return "连接中";
}
