import { Button, StatusState } from "@delta/ui";
import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../auth/auth-context";
import { persistPendingInviteCode } from "../auth/invite-storage";
import { loadPublicDistributionInvite, type PublicDistributionInvite } from "../modules/profile/profile-api";

type LoadStatus = "idle" | "loading" | "success" | "error";

export function DistributionInvitePage() {
  const { inviteCode = "" } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated, openAuthModal } = useAuth();
  const [loadStatus, setLoadStatus] = useState<LoadStatus>("idle");
  const [error, setError] = useState("");
  const [invite, setInvite] = useState<PublicDistributionInvite | null>(null);

  useEffect(() => {
    if (!inviteCode) {
      setLoadStatus("error");
      setError("推广链接无效");
      return;
    }
    persistPendingInviteCode(inviteCode);
    let disposed = false;
    async function bootstrap() {
      try {
        setLoadStatus("loading");
        setError("");
        const next = await loadPublicDistributionInvite(inviteCode);
        if (disposed) {
          return;
        }
        setInvite(next);
        setLoadStatus("success");
      } catch (requestError) {
        if (disposed) {
          return;
        }
        setError(requestError instanceof Error ? requestError.message : "推广信息加载失败");
        setLoadStatus("error");
      }
    }
    void bootstrap();
    return () => {
      disposed = true;
    };
  }, [inviteCode]);

  const inviteLink = useMemo(() => {
    if (!invite?.invitePath) {
      return "";
    }
    if (/^https?:\/\//.test(invite.invitePath)) {
      return invite.invitePath;
    }
    if (typeof window === "undefined") {
      return invite.invitePath;
    }
    return `${window.location.origin}${invite.invitePath}`;
  }, [invite]);

  return (
    <main className="distribution-invite-page">
      <div className="dt-container distribution-invite-shell">
        {loadStatus === "loading" || loadStatus === "idle" ? (
          <StatusState title="推广信息加载中" description="正在验证推广链接与邀请关系。" />
        ) : loadStatus === "error" || !invite ? (
          <section className="distribution-invite-card distribution-invite-card--empty">
            <StatusState title="推广链接不可用" description={error || "该链接已失效或不存在。"} tone="error" />
            <div className="distribution-invite-card__footer">
              <Link className="dt-button dt-button--secondary" to="/">
                返回首页
              </Link>
            </div>
          </section>
        ) : (
          <section className="distribution-invite-card">
            <div className="distribution-invite-card__hero">
              <div className="distribution-invite-card__copy">
                <span className="distribution-invite-card__eyebrow">一级分销邀请</span>
                <h1>通过邀请注册，完成首单后自动绑定推广关系</h1>
                <p>{invite.description}</p>
                <div className="distribution-invite-card__chips">
                  <span>邀请人 {invite.promoterNickname || "平台推广员"}</span>
                  <span>联系方式 {invite.promoterPhone || "-"}</span>
                  <span>推广码 {invite.inviteCode}</span>
                </div>
              </div>
              {invite.posterUrl ? (
                <div className="distribution-invite-card__poster">
                  <img alt="推广海报" src={invite.posterUrl} />
                </div>
              ) : null}
            </div>

            <div className="distribution-invite-card__body">
              <article className="distribution-invite-metric">
                <span>注册链接</span>
                <strong>{inviteLink}</strong>
              </article>
              <article className="distribution-invite-metric">
                <span>绑定状态</span>
                <strong>{invite.active ? "已锁定当前邀请关系" : "邀请链接已失效"}</strong>
              </article>
            </div>

            <div className="distribution-invite-card__footer">
              {isAuthenticated ? (
                <Button onClick={() => navigate("/profile?tab=distributor")}>进入分销中心</Button>
              ) : (
                <>
                  <Button onClick={() => openAuthModal("register")}>注册并绑定邀请</Button>
                  <button className="dt-button dt-button--secondary" onClick={() => openAuthModal("login")} type="button">
                    已有账号去登录
                  </button>
                </>
              )}
            </div>
          </section>
        )}
      </div>
    </main>
  );
}
