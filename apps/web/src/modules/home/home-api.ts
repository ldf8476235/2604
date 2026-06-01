import { request } from "../../lib/http-client";

export type HomeMetric = {
  label: string;
  value: string;
};

export type HomeFooterLink = {
  label: string;
  href: string;
};

export type HomePayload = {
  siteAssurances: string[];
  metrics: HomeMetric[];
  footerLinks: HomeFooterLink[];
  icpNo: string;
  contactPhone: string;
  copyright: string;
};

export function loadHome() {
  return request<HomePayload>("/api/public/home", { auth: false });
}
