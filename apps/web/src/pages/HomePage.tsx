import { useEffect, useState } from "react";
import { loadHome, type HomePayload } from "../modules/home/home-api";
import { ListingMarket } from "../modules/listing/ListingMarket";

type HomePageProps = {
  marketType: "rental" | "trade";
};

export function HomePage({ marketType }: HomePageProps) {
  const [homePayload, setHomePayload] = useState<HomePayload | null>(null);

  useEffect(() => {
    let active = true;
    loadHome()
      .then((payload) => {
        if (!active) {
          return;
        }
        setHomePayload(payload);
      })
      .catch(() => {
        if (!active) {
          return;
        }
        setHomePayload(null);
      });
    return () => {
      active = false;
    };
  }, []);

  return (
    <main className="market-page">
      <ListingMarket marketType={marketType} />

      <section className="assurance-strip" id="assurance">
        <div className="dt-container assurance-strip__inner">
          {(homePayload?.siteAssurances ?? []).map((item) => (
            <div className="assurance-item" key={item}>
              {item}
            </div>
          ))}
        </div>
      </section>

      <footer className="site-footer" id="footer">
        <div className="dt-container site-footer__inner">
          <div className="site-footer__brand">
            <span className="site-footer__mark site-footer__mark--image">
              <img alt="萌虎" src="/brand/menghu-ai-logo.png" />
            </span>
          </div>
          <nav className="site-footer__links" aria-label="页脚导航">
            {(homePayload?.footerLinks ?? []).map((item) => (
              <a href={item.href} key={item.label}>
                {item.label}
              </a>
            ))}
          </nav>
          <div className="site-footer__meta">
            <span>网站备案号：{homePayload?.icpNo ?? "—"}</span>
            <span>联系电话：{homePayload?.contactPhone ?? "—"}</span>
            <span>{homePayload?.copyright ?? "版权所有"}</span>
          </div>
        </div>
      </footer>
    </main>
  );
}
