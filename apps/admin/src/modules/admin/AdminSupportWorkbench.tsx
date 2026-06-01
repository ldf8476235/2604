import { Button, StatusState, SurfaceCard, Tag } from "@delta/ui";
import { type KeyboardEvent, useEffect, useMemo, useRef, useState } from "react";
import { createAdminImRealtimeSession, type AdminImRealtimeState } from "./admin-im-realtime";
import {
  loadAdminImConversation,
  loadAdminImWorkbench,
  sendAdminImMessage,
  uploadAdminAsset,
  type AdminImConversation,
  type AdminImWorkbench,
  type AdminImWorkbenchRefreshEvent,
} from "./admin-api";

type SceneCode = "" | "TRADE_ORDER" | "LISTING_CONSULT" | "BOOSTING_ORDER";
type LoadStatus = "idle" | "loading" | "success" | "error";

export function AdminSupportWorkbench() {
  const [keywordDraft, setKeywordDraft] = useState("");
  const [keyword, setKeyword] = useState("");
  const [sceneCode, setSceneCode] = useState<SceneCode>("");
  const [listStatus, setListStatus] = useState<LoadStatus>("idle");
  const [listError, setListError] = useState("");
  const [workbench, setWorkbench] = useState<AdminImWorkbench | null>(null);
  const [activeConversationNo, setActiveConversationNo] = useState("");
  const [conversationStatus, setConversationStatus] = useState<LoadStatus>("idle");
  const [conversationError, setConversationError] = useState("");
  const [conversation, setConversation] = useState<AdminImConversation | null>(null);
  const [messageText, setMessageText] = useState("");
  const [sending, setSending] = useState(false);
  const [uploadingImage, setUploadingImage] = useState(false);
  const [notice, setNotice] = useState("");
  const [realtimeState, setRealtimeState] = useState<AdminImRealtimeState>("closed");
  const imageInputRef = useRef<HTMLInputElement | null>(null);
  const messageBottomRef = useRef<HTMLDivElement | null>(null);
  const listLoadingRef = useRef(false);
  const conversationLoadingRef = useRef(false);
  const initializedRealtimeRef = useRef(false);
  const activeConversationNoRef = useRef("");
  const keywordRef = useRef("");
  const sceneCodeRef = useRef<SceneCode>("");

  useEffect(() => {
    activeConversationNoRef.current = activeConversationNo;
  }, [activeConversationNo]);

  useEffect(() => {
    keywordRef.current = keyword;
  }, [keyword]);

  useEffect(() => {
    sceneCodeRef.current = sceneCode;
  }, [sceneCode]);

  useEffect(() => {
    let disposed = false;
    listLoadingRef.current = true;
    setListStatus("loading");
    setListError("");
    loadAdminImWorkbench({
      keyword: keyword || undefined,
      sceneCode: sceneCode || undefined,
    })
      .then((payload) => {
        if (disposed) {
          return;
        }
        setWorkbench(payload);
        setListStatus("success");
        setActiveConversationNo((current) => {
          if (current && payload.rows.some((item) => item.conversationNo === current)) {
            return current;
          }
          return payload.rows[0]?.conversationNo ?? "";
        });
      })
      .catch((error) => {
        if (disposed) {
          return;
        }
        setListError(getErrorMessage(error));
        setListStatus("error");
      })
      .finally(() => {
        if (!disposed) {
          listLoadingRef.current = false;
        }
      });
    return () => {
      disposed = true;
      listLoadingRef.current = false;
    };
  }, [keyword, sceneCode]);

  useEffect(() => {
    if (!activeConversationNo) {
      setConversation(null);
      setConversationStatus("idle");
      setConversationError("");
      return;
    }
    let disposed = false;
    conversationLoadingRef.current = true;
    setConversationStatus("loading");
    setConversationError("");
    loadAdminImConversation(activeConversationNo)
      .then((payload) => {
        if (disposed) {
          return;
        }
        setConversation(payload);
        setConversationStatus("success");
      })
      .catch((error) => {
        if (disposed) {
          return;
        }
        setConversationError(getErrorMessage(error));
        setConversationStatus("error");
      })
      .finally(() => {
        if (!disposed) {
          conversationLoadingRef.current = false;
        }
      });
    return () => {
      disposed = true;
      conversationLoadingRef.current = false;
    };
  }, [activeConversationNo]);

  useEffect(() => {
    const timer = window.setInterval(() => {
      if (activeConversationNo) {
        void refreshConversation(activeConversationNo, true);
      }
      void refreshWorkbench(true);
    }, 10000);
    return () => window.clearInterval(timer);
  }, [activeConversationNo, keyword, sceneCode]);

  useEffect(() => {
    initializedRealtimeRef.current = false;
    let initializationTimer: number | undefined;
    const dispose = createAdminImRealtimeSession({
      subscriptions: [
        {
          topic: "/topic/im/workbench",
          onMessage: (payload) => {
            const event = payload as AdminImWorkbenchRefreshEvent;
            if (!event?.conversationNo) {
              return;
            }
            const currentConversationNo = activeConversationNoRef.current;
            void refreshWorkbench(true);
            if (event.conversationNo === currentConversationNo) {
              void refreshConversation(currentConversationNo, true);
            }
            if (initializedRealtimeRef.current && shouldPlayNoticeSound(event)) {
              playSupportNoticeSound();
            }
          },
        },
      ],
      onStateChange: (state) => {
        setRealtimeState(state);
        if (state === "connected") {
          initializationTimer = window.setTimeout(() => {
            initializedRealtimeRef.current = true;
          }, 500);
        }
      },
    });
    return () => {
      if (initializationTimer) {
        window.clearTimeout(initializationTimer);
      }
      dispose();
    };
  }, []);

  useEffect(() => {
    if (!notice) {
      return;
    }
    const timer = window.setTimeout(() => setNotice(""), 2000);
    return () => window.clearTimeout(timer);
  }, [notice]);

  useEffect(() => {
    if (!conversation?.messages.length) {
      return;
    }
    window.requestAnimationFrame(() => {
      messageBottomRef.current?.scrollIntoView({ block: "end" });
    });
  }, [activeConversationNo, conversation?.messages.length, conversation?.messages[conversation.messages.length - 1]?.id]);

  const activeConversation = useMemo(
    () => workbench?.rows.find((item) => item.conversationNo === activeConversationNo) ?? null,
    [activeConversationNo, workbench],
  );

  return (
    <section className="console-grid console-grid--detail console-support-workbench">
      <SurfaceCard eyebrow="客服监管" title="IM 工作台">
        <div className="console-support-toolbar">
          <label>
            关键字
            <input
              placeholder="搜索订单号 / 会话号 / 买家 / 卖家"
              value={keywordDraft}
              onChange={(event) => setKeywordDraft(event.target.value)}
            />
          </label>
          <label>
            会话类型
            <select value={sceneCode} onChange={(event) => setSceneCode(event.target.value as SceneCode)}>
              <option value="">全部会话</option>
              <option value="TRADE_ORDER">交易群聊</option>
              <option value="LISTING_CONSULT">售前咨询</option>
              <option value="BOOSTING_ORDER">代肝客服</option>
            </select>
          </label>
          <div className="console-support-toolbar__actions">
            <Button kind="secondary" onClick={() => {
              setKeywordDraft("");
              setKeyword("");
              setSceneCode("");
            }}>
              重置
            </Button>
            <Button onClick={() => setKeyword(keywordDraft.trim())}>筛选</Button>
          </div>
        </div>

        <div className="console-support-summary">
          <Tag tone="accent">总会话 {workbench?.totalCount ?? 0}</Tag>
          <Tag tone="warning">交易 {workbench?.tradeCount ?? 0}</Tag>
          <Tag tone="success">代肝 {workbench?.boostingCount ?? 0}</Tag>
          <Tag tone={realtimeState === "connected" ? "success" : "warning"}>{renderRealtimeState(realtimeState)}</Tag>
        </div>

        {notice ? (
          <div className="console-notice" role="status" aria-live="polite">
            <span>{notice}</span>
            <button onClick={() => setNotice("")} type="button">关闭</button>
          </div>
        ) : null}

        {listStatus === "loading" || listStatus === "idle" ? (
          <StatusState title="会话列表加载中" description="正在同步最新客服会话。" />
        ) : null}
        {listStatus === "error" ? <StatusState title="会话列表加载失败" description={listError} tone="error" /> : null}
        {listStatus === "success" && workbench?.rows.length ? (
          <div className="console-support-list">
            {workbench.rows.map((item) => (
              <button
                className={`console-support-list__item ${item.conversationNo === activeConversationNo ? "is-active" : ""}`}
                key={item.conversationNo}
                onClick={() => setActiveConversationNo(item.conversationNo)}
                type="button"
              >
                <div>
                  <strong>
                    {item.title}
                    {item.unread ? <span className="console-support-list__unread">{item.unreadCount > 99 ? "99+" : item.unreadCount || ""}</span> : null}
                  </strong>
                  <p>{item.sceneLabel} · {item.statusLabel}</p>
                </div>
                <small>{item.lastMessageAt}</small>
              </button>
            ))}
          </div>
        ) : null}
        {listStatus === "success" && !workbench?.rows.length ? <StatusState title="暂无会话" description="当前筛选条件下没有客服会话。" /> : null}
      </SurfaceCard>

      <SurfaceCard eyebrow="会话详情" title={conversation?.title ?? activeConversation?.title ?? "请选择会话"}>
        {conversationStatus === "loading" || conversationStatus === "idle" ? (
          <StatusState title="会话加载中" description="正在同步聊天记录与交易摘要。" />
        ) : null}
        {conversationStatus === "error" ? <StatusState title="会话加载失败" description={conversationError} tone="error" /> : null}
        {conversation ? (
          <div className="console-support-detail">
            <div className="console-support-meta">
              <Tag>{conversation.sceneLabel}</Tag>
              <Tag tone="warning">{conversation.statusLabel}</Tag>
              <span>来源编号 {conversation.sourceOrderNo}</span>
            </div>

            <div className="console-support-facts">
              {conversation.summaryItems.map((item) => (
                <div className="console-support-facts__item" key={item.label}>
                  <span>{item.label}</span>
                  <strong>{item.value}</strong>
                </div>
              ))}
            </div>

            <div className="console-support-messages">
              {conversation.messages.map((item) => (
                <article className={`console-support-message ${item.mine ? "is-mine" : ""}`} key={item.id}>
                  <div className="console-support-message__meta">
                    <strong>{item.senderName}</strong>
                    <span>{item.senderRoleLabel}</span>
                    <time>{item.createdAt}</time>
                  </div>
                  {item.text ? <p>{item.text}</p> : null}
                  {item.messageType === "IMAGE" && item.fileUrl ? (
                    <a className="console-support-message__image" href={item.fileUrl} rel="noreferrer" target="_blank">
                      <img alt={item.fileName ?? "图片消息"} src={item.fileUrl} />
                    </a>
                  ) : null}
                  {item.messageType === "FILE" && item.fileUrl ? (
                    <a className="console-support-message__file" href={item.fileUrl} rel="noreferrer" target="_blank">
                      {item.fileName ?? "查看附件"}
                    </a>
                  ) : null}
                </article>
              ))}
              <div ref={messageBottomRef} />
            </div>

            <div className="console-support-composer">
              <textarea
                placeholder="输入客服回复内容，按 Enter 发送，Shift + Enter 换行"
                rows={4}
                value={messageText}
                onChange={(event) => setMessageText(event.target.value)}
                onKeyDown={handleComposerKeyDown}
              />
              <div className="console-support-composer__actions">
                <input
                  accept="image/png,image/jpeg,image/webp,image/gif"
                  hidden
                  ref={imageInputRef}
                  type="file"
                  onChange={(event) => {
                    const file = event.target.files?.[0];
                    event.target.value = "";
                    if (file) {
                      void handleSendImage(file);
                    }
                  }}
                />
                <Button kind="secondary" onClick={() => void refreshConversation(activeConversationNo)}>
                  刷新会话
                </Button>
                <Button kind="secondary" disabled={!activeConversationNo || uploadingImage || sending} onClick={() => imageInputRef.current?.click()}>
                  {uploadingImage ? "图片上传中..." : "发送图片"}
                </Button>
                <Button
                  disabled={!messageText.trim() || sending}
                  onClick={() => void handleSendMessage()}
                >
                  {sending ? "发送中..." : "发送回复"}
                </Button>
              </div>
            </div>
          </div>
        ) : null}
      </SurfaceCard>
    </section>
  );

  async function refreshWorkbench(silent?: boolean) {
    if (listLoadingRef.current) {
      return;
    }
    listLoadingRef.current = true;
    try {
      if (!silent) {
        setListStatus("loading");
      }
      const payload = await loadAdminImWorkbench({
        keyword: keywordRef.current || undefined,
        sceneCode: sceneCodeRef.current || undefined,
      });
      setWorkbench(payload);
      setListStatus("success");
    } catch (error) {
      if (!silent) {
        setListError(getErrorMessage(error));
        setListStatus("error");
      }
    } finally {
      listLoadingRef.current = false;
    }
  }

  async function refreshConversation(conversationNo: string, silent?: boolean) {
    if (!conversationNo) {
      return;
    }
    if (conversationLoadingRef.current) {
      return;
    }
    conversationLoadingRef.current = true;
    try {
      if (!silent) {
        setConversationStatus("loading");
      }
      const payload = await loadAdminImConversation(conversationNo);
      setConversation(payload);
      setConversationStatus("success");
    } catch (error) {
      if (!silent) {
        setConversationError(getErrorMessage(error));
        setConversationStatus("error");
      }
    } finally {
      conversationLoadingRef.current = false;
    }
  }

  async function handleSendMessage() {
    if (!activeConversationNo || !messageText.trim()) {
      return;
    }
    try {
      setSending(true);
      const payload = await sendAdminImMessage(activeConversationNo, { text: messageText.trim() });
      setConversation(payload);
      setMessageText("");
      setNotice("客服消息已发送。");
      void refreshWorkbench(true);
    } catch (error) {
      setNotice(getErrorMessage(error));
    } finally {
      setSending(false);
    }
  }

  function handleComposerKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key !== "Enter" || event.shiftKey || event.nativeEvent.isComposing) {
      return;
    }
    event.preventDefault();
    if (activeConversationNo && messageText.trim() && !sending) {
      void handleSendMessage();
    }
  }

  async function handleSendImage(file: File) {
    if (!activeConversationNo) {
      return;
    }
    if (!file.type.startsWith("image/")) {
      setNotice("仅支持发送图片");
      return;
    }
    try {
      setUploadingImage(true);
      const uploaded = await uploadAdminAsset(file, "admin-im-images");
      const payload = await sendAdminImMessage(activeConversationNo, {
        fileKey: uploaded.objectKey,
        fileName: file.name,
      });
      setConversation(payload);
      setNotice("图片已发送。");
      void refreshWorkbench(true);
    } catch (error) {
      setNotice(getErrorMessage(error));
    } finally {
      setUploadingImage(false);
    }
  }
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : "请求失败";
}

function shouldPlayNoticeSound(event: AdminImWorkbenchRefreshEvent) {
  return event.eventType === "USER_MESSAGE" || event.eventType === "INIT";
}

function renderRealtimeState(state: AdminImRealtimeState) {
  if (state === "connected") return "实时已连接";
  if (state === "connecting") return "实时连接中";
  if (state === "reconnecting") return "实时重连中";
  if (state === "error") return "实时异常";
  return "实时未连接";
}

function playSupportNoticeSound() {
  if (typeof window === "undefined") {
    return;
  }
  const AudioContextCtor = window.AudioContext || window.webkitAudioContext;
  if (!AudioContextCtor) {
    return;
  }
  const context = new AudioContextCtor();
  const oscillator = context.createOscillator();
  const gain = context.createGain();
  oscillator.type = "sine";
  oscillator.frequency.value = 880;
  gain.gain.setValueAtTime(0.0001, context.currentTime);
  gain.gain.exponentialRampToValueAtTime(0.12, context.currentTime + 0.02);
  gain.gain.exponentialRampToValueAtTime(0.0001, context.currentTime + 0.22);
  oscillator.connect(gain);
  gain.connect(context.destination);
  oscillator.start();
  oscillator.stop(context.currentTime + 0.24);
  window.setTimeout(() => void context.close(), 320);
}

declare global {
  interface Window {
    webkitAudioContext?: typeof AudioContext;
  }
}
