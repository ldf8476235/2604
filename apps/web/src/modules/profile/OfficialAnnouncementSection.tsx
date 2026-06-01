import { Button, StatusState } from "@delta/ui";
import { useEffect, useMemo, useState } from "react";
import { loadPublicAnnouncements, type OfficialAnnouncementCenterResponse } from "./profile-api";

type LoadStatus = "idle" | "loading" | "success" | "error";

export function OfficialAnnouncementSection({
  showLoginPrompt = false,
  onLogin,
}: {
  showLoginPrompt?: boolean;
  onLogin?: () => void;
}) {
  const [loadStatus, setLoadStatus] = useState<LoadStatus>("idle");
  const [error, setError] = useState("");
  const [response, setResponse] = useState<OfficialAnnouncementCenterResponse | null>(null);
  const [selectedNo, setSelectedNo] = useState("");
  const [refreshToken, setRefreshToken] = useState(0);

  useEffect(() => {
    let disposed = false;
    async function bootstrap() {
      try {
        setLoadStatus("loading");
        setError("");
        const next = await loadPublicAnnouncements(48);
        if (disposed) {
          return;
        }
        setResponse(next);
        setLoadStatus("success");
      } catch (requestError) {
        if (disposed) {
          return;
        }
        setError(requestError instanceof Error ? requestError.message : "官方公告加载失败");
        setLoadStatus("error");
      }
    }
    void bootstrap();
    return () => {
      disposed = true;
    };
  }, [refreshToken]);

  useEffect(() => {
    if (!response?.rows.length) {
      setSelectedNo("");
      return;
    }
    if (!selectedNo || !response.rows.some((item) => item.announcementNo === selectedNo)) {
      setSelectedNo(response.rows[0].announcementNo);
    }
  }, [response, selectedNo]);

  const selectedAnnouncement = useMemo(
    () => response?.rows.find((item) => item.announcementNo === selectedNo) ?? response?.rows[0] ?? null,
    [response, selectedNo],
  );

  return (
    <section className="profile-feature-layout">
      <section className="profile-panel official-announcement-panel">
        <header className="profile-panel__header official-announcement-panel__header">
          <div>
            <h2>官方公告</h2>
            <p>平台更新、活动通知和交易规则统一收口在这里，首页点击官方公告会直接进入当前页面。</p>
          </div>
          <div className="official-announcement-panel__actions">
            <span className="profile-status-badge profile-status-badge--success">{response?.total ?? 0} 条公告</span>
            <button className="profile-secondary-button" onClick={() => setRefreshToken((current) => current + 1)} type="button">
              刷新公告
            </button>
          </div>
        </header>

        {showLoginPrompt ? (
          <div className="official-announcement-hero">
            <div>
              <span className="official-announcement-hero__eyebrow">游客可查看公告</span>
              <strong>登录后继续查看订单、消息、钱包和分销中心</strong>
            </div>
            {onLogin ? (
              <Button onClick={onLogin}>立即登录</Button>
            ) : null}
          </div>
        ) : null}

        {loadStatus === "loading" || loadStatus === "idle" ? (
          <StatusState title="官方公告加载中" description="正在同步最新的平台公告与活动通知。" />
        ) : loadStatus === "error" ? (
          <StatusState title="官方公告加载失败" description={error} tone="error" />
        ) : response?.rows.length ? (
          <div className="official-announcement-layout">
            <aside className="official-announcement-list" aria-label="官方公告列表">
              {response.rows.map((item, index) => (
                <button
                  className={`official-announcement-card ${selectedAnnouncement?.announcementNo === item.announcementNo ? "is-active" : ""}`}
                  key={item.announcementNo || `${item.title}-${index}`}
                  onClick={() => setSelectedNo(item.announcementNo)}
                  type="button"
                >
                  <div className="official-announcement-card__meta">
                    <span className="official-announcement-card__category">{item.categoryText}</span>
                    {item.pinned ? <span className="official-announcement-card__pinned">置顶</span> : null}
                  </div>
                  <strong>{item.title}</strong>
                  <p>{item.summary || "暂无公告摘要"}</p>
                  <small>{item.publishAt || "-"}</small>
                </button>
              ))}
            </aside>

            <article className="official-announcement-detail">
              <div className="official-announcement-detail__head">
                <div className="official-announcement-detail__badges">
                  <span>{selectedAnnouncement?.categoryText || "官方公告"}</span>
                  {selectedAnnouncement?.pinned ? <span>置顶通知</span> : null}
                </div>
                <h3>{selectedAnnouncement?.title || "暂无公告"}</h3>
                <time>{selectedAnnouncement?.publishAt || "-"}</time>
              </div>
              <div className="official-announcement-detail__body">
                {(selectedAnnouncement?.content || "").split(/\n+/).filter(Boolean).map((paragraph, index) => (
                  <p key={`${selectedAnnouncement?.announcementNo || "announcement"}-${index}`}>{paragraph}</p>
                ))}
              </div>
            </article>
          </div>
        ) : (
          <StatusState title="暂无官方公告" description="管理员发布公告后，这里会自动同步最新内容。" />
        )}
      </section>
    </section>
  );
}
