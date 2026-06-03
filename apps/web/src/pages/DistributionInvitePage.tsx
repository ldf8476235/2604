import { StatusState } from "@delta/ui";
import { useEffect } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { useAuth } from "../auth/auth-context";
import { persistPendingInviteCode } from "../auth/invite-storage";

export function DistributionInvitePage() {
  const { inviteCode = "" } = useParams();
  const navigate = useNavigate();
  const { isAuthenticated, openAuthModal } = useAuth();

  useEffect(() => {
    const normalizedInviteCode = inviteCode.trim();
    if (normalizedInviteCode) {
      persistPendingInviteCode(normalizedInviteCode);
    }
    if (!isAuthenticated) {
      openAuthModal("register");
    }
    navigate("/", { replace: true });
  }, [inviteCode, isAuthenticated, navigate, openAuthModal]);

  return (
    <main className="distribution-invite-page">
      <div className="dt-container distribution-invite-shell">
        <StatusState title="正在进入注册页面" description="推广关系已记录，请完成注册后自动绑定。" />
      </div>
    </main>
  );
}
