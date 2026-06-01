import { Button, StatusState } from "@delta/ui";
import { type KeyboardEvent, useEffect, useMemo, useRef, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../auth/auth-context";
import { type ImConversation, loadConversation, markConversationRead, sendConversationMessage } from "../modules/im/im-api";
import { buildImTimeline, getConversationLastActivity } from "../modules/im/im-presenter";
import { createImRealtimeSession, type ImRealtimeState } from "../modules/im/im-realtime";
import { uploadOssFileDirect } from "../modules/publish/publish-api";

type LoadStatus = "idle" | "loading" | "success" | "error";
const NOTIFICATION_REFRESH_EVENT = "dt:notifications-refresh";

function refreshNotifications() {
  window.dispatchEvent(new CustomEvent(NOTIFICATION_REFRESH_EVENT));
}

export function ImChatPage() {
  const { conversationNo = "" } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated, openAuthModal } = useAuth();
  const [loadStatus, setLoadStatus] = useState<LoadStatus>("idle");
  const [error, setError] = useState("");
  const [conversation, setConversation] = useState<ImConversation | null>(null);
  const [notice, setNotice] = useState("");
  const [messageText, setMessageText] = useState("");
  const [sending, setSending] = useState(false);
  const [uploadingImage, setUploadingImage] = useState(false);
  const [uploadingFile, setUploadingFile] = useState(false);
  const [realtimeState, setRealtimeState] = useState<ImRealtimeState>("connecting");
  const imageInputRef = useRef<HTMLInputElement | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const listRef = useRef<HTMLDivElement | null>(null);

  useEffect(() => {
    if (!isAuthenticated || !conversationNo) {
      return;
    }
    let disposed = false;
    async function bootstrap(silent?: boolean) {
      try {
        if (!silent) {
          setLoadStatus("loading");
        }
        setError("");
        const result = await loadConversation(conversationNo);
        if (disposed) {
          return;
        }
        setConversation(result);
        setLoadStatus("success");
        void markConversationRead(conversationNo).finally(refreshNotifications);
      } catch (requestError) {
        if (disposed) {
          return;
        }
        setError(getErrorMessage(requestError));
        setLoadStatus("error");
      }
    }
    void bootstrap();
    const disposeRealtime = createImRealtimeSession({
      subscriptions: [
        {
          topic: `/topic/im/${conversationNo}`,
          onMessage: () => {
            void bootstrap(true);
          },
        },
      ],
      onStateChange: setRealtimeState,
    });
    return () => {
      disposed = true;
      disposeRealtime();
    };
  }, [conversationNo, isAuthenticated]);

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

  const canSendText = messageText.trim().length > 0 && !sending;
  const participants = useMemo(() => conversation?.participants ?? [], [conversation?.participants]);
  const groupedSummary = useMemo(() => conversation?.summaryItems ?? [], [conversation?.summaryItems]);
  const timeline = useMemo(() => buildImTimeline(conversation?.messages ?? []), [conversation?.messages]);
  const lastActivity = useMemo(() => getConversationLastActivity(conversation), [conversation]);
  const sendingStateText = uploadingImage ? "图片上传中" : uploadingFile ? "文件上传中" : sending ? "消息发送中" : "实时会话已就绪";
  const headerTitle = conversation?.listingNo || conversation?.sourceOrderNo || conversationNo;

  if (!isAuthenticated) {
    return (
      <main className="im-page">
        <div className="dt-container im-shell">
          <StatusState
            title="登录后才能进入客服会话"
            description="站内 IM 会话与订单、代肝进度绑定，当前页面需要登录后访问。"
            action={<Button onClick={() => openAuthModal("login")}>立即登录</Button>}
          />
        </div>
      </main>
    );
  }

  return (
    <main className="im-page">
      <div className="dt-container im-shell">
        <section className="im-layout">
          <aside className="im-sidebar">
            <div className="im-sidebar__head">
              <p className="boosting-hero__eyebrow">IM 客服聊天</p>
              <h1>{conversation?.title ?? "正在连接会话..."}</h1>
              <p>{conversation?.description ?? "加载中..."}</p>
            </div>

            <div className="im-sidebar__summary">
              <div className="im-summary-chip">{conversation?.sceneLabel ?? "会话"}</div>
              <div className="im-summary-chip">{conversation?.statusLabel ?? "状态加载中"}</div>
              <div className={`im-summary-chip im-summary-chip--${realtimeState}`}>实时 {renderRealtimeLabel(realtimeState)}</div>
            </div>

            <div className="im-sidebar__panel">
              <div className="im-sidebar__panel-row">
                <span>当前身份</span>
                <strong>{conversation?.currentUserName ?? "当前用户"}</strong>
              </div>
              <div className="im-sidebar__panel-row">
                <span>最近活跃</span>
                <strong>{lastActivity}</strong>
              </div>
              <div className="im-sidebar__panel-row">
                <span>会话编号</span>
                <strong>{conversation?.sourceOrderNo ?? conversationNo}</strong>
              </div>
            </div>

            <div className="im-sidebar__members">
              <div className="im-sidebar__members-head">
                <strong>群聊成员</strong>
                <span>固定三人</span>
              </div>
              <div className="im-sidebar__member-list">
                {participants.map((participant) => (
                  <div className={`im-member-card ${participant.currentUser ? "is-current" : ""}`} key={participant.roleCode}>
                    {participant.avatarUrl ? (
                      <img alt={participant.displayName} className="im-member-card__avatar" src={participant.avatarUrl} />
                    ) : (
                      <span className="im-member-card__avatar im-member-card__avatar--placeholder">{participant.displayName.slice(0, 1)}</span>
                    )}
                    <div className="im-member-card__content">
                      <div className="im-member-card__meta">
                        <strong>{participant.displayName}</strong>
                        <span className={`im-member-card__badge im-member-card__badge--${participant.roleCode.toLowerCase()}`}>{participant.roleLabel}</span>
                      </div>
                      <p>{participant.currentUser ? "当前登录身份" : "已加入当前群聊"}</p>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            <div className="im-sidebar__meta">
              {groupedSummary.map((item) => (
                <div className="im-meta-row" key={item.label}>
                  <span>{item.label}</span>
                  <strong>{item.value}</strong>
                </div>
              ))}
            </div>

	          </aside>

          <section className="im-chat-card">
            <header className="im-chat-card__header">
              <button className="im-chat-card__back" onClick={() => navigate(-1)} type="button">
                返回
              </button>
              <div className="im-chat-card__header-main">
                <strong>{headerTitle}</strong>
                <p>{conversation?.sceneLabel ?? "IM 会话"} · 订单号 {conversation?.sourceOrderNo ?? conversationNo}</p>
              </div>
              {conversation ? (
                <button className="im-chat-card__detail" onClick={() => openConversationDetail(conversation)} type="button">
                  查看详情
                </button>
              ) : null}
              <div className="im-chat-card__presence">
                <span className={`im-chat-card__presence-dot im-chat-card__presence-dot--${realtimeState}`} />
                <strong>{renderRealtimeLabel(realtimeState)}</strong>
                <small>{sendingStateText}</small>
              </div>
            </header>

	            {notice ? (
              <div className="app-toast" role="status" aria-live="polite">
                <span>{notice}</span>
                <button onClick={() => setNotice("")} type="button">关闭</button>
              </div>
            ) : null}

            {loadStatus === "loading" ? (
              <div className="im-chat-card__status">
                <StatusState title="会话加载中" description="正在同步聊天记录、订单摘要与客服信息。" />
              </div>
            ) : loadStatus === "error" ? (
              <div className="im-chat-card__status">
                <StatusState title="会话加载失败" description={error} tone="error" />
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
                  <div className="im-composer__toolbar">
                    <div className="im-composer__actions">
                      <button disabled={uploadingImage || sending} onClick={() => imageInputRef.current?.click()} type="button">
                        {uploadingImage ? "上传图片中..." : "发图片"}
                      </button>
                      <button disabled={uploadingFile || sending} onClick={() => fileInputRef.current?.click()} type="button">
                        {uploadingFile ? "上传文件中..." : "发文件"}
                      </button>
                    </div>
                    <div className="im-composer__state">{sendingStateText}</div>
                  </div>
                  <textarea
                    maxLength={500}
                    placeholder="输入消息，按 Enter 发送，Shift + Enter 换行"
                    value={messageText}
                    onChange={(event) => setMessageText(event.target.value)}
                    onKeyDown={handleComposerKeyDown}
                  />
                  <div className="im-composer__footer">
                    <span>{conversation.sceneLabel} · Enter 发送，Shift + Enter 换行 · {messageText.length}/500</span>
                    <button className="dt-button dt-button--primary" disabled={!canSendText} onClick={() => void handleSendText()} type="button">
                      {sending ? "发送中..." : "发送"}
                    </button>
                  </div>
                </div>
              </>
            ) : null}
          </section>
        </section>
      </div>

      <input
        accept="image/png,image/jpeg,image/webp,image/gif"
	        className="im-file-input"
        onChange={(event) => {
          const file = event.target.files?.[0];
          event.target.value = "";
          if (file) {
            void handleUploadAndSend(file, "image");
          }
        }}
        ref={imageInputRef}
        type="file"
      />
      <input
	        className="im-file-input"
        onChange={(event) => {
          const file = event.target.files?.[0];
          event.target.value = "";
          if (file) {
            void handleUploadAndSend(file, "file");
          }
        }}
        ref={fileInputRef}
        type="file"
      />
    </main>
  );

  async function handleSendText() {
    const text = messageText.trim();
    if (!text || !conversationNo) {
      return;
    }
    try {
      setSending(true);
      setNotice("");
      const result = await sendConversationMessage(conversationNo, { text });
      setConversation(result);
      setMessageText("");
      void markConversationRead(conversationNo).finally(refreshNotifications);
    } catch (requestError) {
      setNotice(getErrorMessage(requestError));
    } finally {
      setSending(false);
    }
  }

  function handleComposerKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key !== "Enter" || event.shiftKey || event.nativeEvent.isComposing) {
      return;
    }
    event.preventDefault();
    if (canSendText) {
      void handleSendText();
    }
  }

  function openConversationDetail(target: ImConversation) {
    if (target.sceneCode === "BOOSTING_ORDER") {
      navigate(`/boosting/orders?focus=${encodeURIComponent(target.sourceOrderNo)}`);
      return;
    }
    if (target.sceneCode === "LISTING_CONSULT" && target.listingNo) {
      navigate(`/?listing=${encodeURIComponent(target.listingNo)}`);
      return;
    }
    navigate(`/profile?tab=orders&focus=${encodeURIComponent(target.sourceOrderNo)}`);
  }

  async function handleUploadAndSend(file: File, kind: "image" | "file") {
    if (!conversationNo) {
      return;
    }
    if (kind === "image") {
      setUploadingImage(true);
    } else {
      setUploadingFile(true);
    }
    try {
      setNotice("");
      const upload = await uploadOssFileDirect("im-attachments", file, file.name);
      const result = await sendConversationMessage(conversationNo, {
        fileKey: upload.objectKey,
        fileName: file.name,
      });
      setConversation(result);
      void markConversationRead(conversationNo).finally(refreshNotifications);
    } catch (requestError) {
      setNotice(getErrorMessage(requestError));
    } finally {
      if (kind === "image") {
        setUploadingImage(false);
      } else {
        setUploadingFile(false);
      }
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
