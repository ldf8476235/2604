import { request } from "../../lib/http-client";
import type { GunCodeCategory, GunCodeGroup, GunCodeVoteType } from "./gun-code-data";

export type GunCodePayload = {
  categories: GunCodeCategory[];
  tags: string[];
  groups: GunCodeGroup[];
};

export function loadGunCodes() {
  return request<GunCodePayload>("/api/public/gun-codes", { auth: false });
}

export function loadGunCodeVotes(entryCodes: string[]) {
  const params = new URLSearchParams();
  if (entryCodes.length > 0) {
    params.set("entryCodes", entryCodes.join(","));
  }
  const search = params.toString();
  return request<Record<string, GunCodeVoteType>>(`/api/gun-codes/votes${search ? `?${search}` : ""}`);
}

export function voteGunCode(entryCode: string, type: GunCodeVoteType) {
  const params = new URLSearchParams();
  params.set("type", type);
  return request<{ entryCode: string; likes: number; dislikes: number; currentVote: GunCodeVoteType | null }>(
    `/api/gun-codes/${entryCode}/vote?${params.toString()}`,
    { method: "POST" },
  );
}
