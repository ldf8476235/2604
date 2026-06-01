import { Button, StatusState } from "@delta/ui";
import { useEffect, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/auth-context";
import {
  loadBoostingHall,
  loadBoostingHallMeta,
  type BoostingHallMeta,
  type BoostingServiceCard,
} from "../modules/boosting/boosting-api";

type LoadStatus = "idle" | "loading" | "success" | "error";

export function BoostingHallPage() {
  const navigate = useNavigate();
  const { isAuthenticated, openAuthModal } = useAuth();
  const [meta, setMeta] = useState<BoostingHallMeta | null>(null);
  const [rows, setRows] = useState<BoostingServiceCard[]>([]);
  const [loadStatus, setLoadStatus] = useState<LoadStatus>("idle");
  const [error, setError] = useState("");
  const [filters, setFilters] = useState({
    category: "ALL",
    cycle: "ALL",
    minPrice: "",
    maxPrice: "",
    sort: "SALES_DESC",
  });

  useEffect(() => {
    let disposed = false;
    async function bootstrap() {
      try {
        setLoadStatus("loading");
        setError("");
        const [metaResult, hallResult] = await Promise.all([
          loadBoostingHallMeta(),
          loadBoostingHall({ category: "ALL", cycle: "ALL", sort: "SALES_DESC" }),
        ]);
        if (disposed) return;
        setMeta(metaResult);
        setRows(hallResult.rows);
        setLoadStatus("success");
      } catch (requestError) {
        if (disposed) return;
        setError(getErrorMessage(requestError));
        setLoadStatus("error");
      }
    }
    void bootstrap();
    return () => {
      disposed = true;
    };
  }, []);

  async function applyFilters() {
    try {
      setLoadStatus("loading");
      setError("");
      const hallResult = await loadBoostingHall(filters);
      setRows(hallResult.rows);
      setLoadStatus("success");
    } catch (requestError) {
      setError(getErrorMessage(requestError));
      setLoadStatus("error");
    }
  }

  function resetFilters() {
    const next = {
      category: "ALL",
      cycle: "ALL",
      minPrice: "",
      maxPrice: "",
      sort: "SALES_DESC",
    };
    setFilters(next);
    void (async () => {
      try {
        setLoadStatus("loading");
        setError("");
        const hallResult = await loadBoostingHall(next);
        setRows(hallResult.rows);
        setLoadStatus("success");
      } catch (requestError) {
        setError(getErrorMessage(requestError));
        setLoadStatus("error");
      }
    })();
  }

  return (
    <main className="boosting-page">
      <div className="dt-container boosting-shell">
        <section className="boosting-hero">
          <div>
            <p className="boosting-hero__eyebrow">代肝服务大厅</p>
            <h1>按服务类型、价格、周期快速筛选代肝服务</h1>
            <p>当前支持哈夫币代肝与安全箱代肝，下单后直接进入代肝订单页完成测试支付、查看进度和申请售后。</p>
          </div>
          <div className="boosting-hero__actions">
            {isAuthenticated ? (
              <Link className="dt-button dt-button--secondary" to="/boosting/orders">
                我的代肝订单
              </Link>
            ) : (
              <Button kind="secondary" onClick={() => openAuthModal("login")}>
                登录后查看代肝订单
              </Button>
            )}
          </div>
        </section>

        {meta ? (
          <section className="boosting-filter-card">
            <div className="boosting-filter-grid">
              <label className="boosting-filter-field">
                <span>服务分类</span>
                <select value={filters.category} onChange={(event) => setFilters((current) => ({ ...current, category: event.target.value }))}>
                  {meta.categories.map((item) => (
                    <option key={item.value} value={item.value}>
                      {item.label}
                    </option>
                  ))}
                </select>
              </label>
              <label className="boosting-filter-field">
                <span>完成周期</span>
                <select value={filters.cycle} onChange={(event) => setFilters((current) => ({ ...current, cycle: event.target.value }))}>
                  {meta.cycleOptions.map((item) => (
                    <option key={item.value} value={item.value}>
                      {item.label}
                    </option>
                  ))}
                </select>
              </label>
              <label className="boosting-filter-field">
                <span>最低价格</span>
                <input value={filters.minPrice} onChange={(event) => setFilters((current) => ({ ...current, minPrice: event.target.value }))} placeholder="如 100" />
              </label>
              <label className="boosting-filter-field">
                <span>最高价格</span>
                <input value={filters.maxPrice} onChange={(event) => setFilters((current) => ({ ...current, maxPrice: event.target.value }))} placeholder="如 500" />
              </label>
              <label className="boosting-filter-field">
                <span>排序方式</span>
                <select value={filters.sort} onChange={(event) => setFilters((current) => ({ ...current, sort: event.target.value }))}>
                  {meta.sortOptions.map((item) => (
                    <option key={item.value} value={item.value}>
                      {item.label}
                    </option>
                  ))}
                </select>
              </label>
            </div>
            <div className="boosting-filter-actions">
              <Button onClick={() => void applyFilters()}>筛选服务</Button>
              <Button kind="secondary" onClick={resetFilters}>
                重置筛选
              </Button>
            </div>
          </section>
        ) : null}

        {loadStatus === "loading" ? (
          <StatusState title="代肝服务加载中" description="正在同步代肝分类、价格区间和当前可下单服务。" />
        ) : loadStatus === "error" ? (
          <StatusState title="代肝服务加载失败" description={error} tone="error" action={<Button onClick={() => void applyFilters()}>重试</Button>} />
        ) : rows.length ? (
          <section className="boosting-service-grid">
            {rows.map((item) => (
              <article className="boosting-service-card" key={item.serviceNo}>
                <div className="boosting-service-card__tags">
                  <span className="boosting-service-card__tag">{item.categoryLabel}</span>
                  <span className="boosting-service-card__tag">{item.cycleLabel}</span>
                </div>
                <h3>{item.name}</h3>
                <p className="boosting-service-card__desc">{item.description}</p>
                <div className="boosting-service-card__meta">
                  <span>保障：{item.guaranteeNote}</span>
                  <span>服务商：{item.providerLabel}</span>
                  <span>销量：{item.salesCount}</span>
                </div>
                <div className="boosting-service-card__foot">
                  <strong>¥{item.price.toFixed(2)}</strong>
                  <button
                    className="boosting-primary-link"
                    type="button"
                    onClick={() => navigate(`/boosting/${encodeURIComponent(item.serviceNo)}/order`)}
                  >
                    立即下单
                  </button>
                </div>
              </article>
            ))}
          </section>
        ) : (
          <StatusState title="暂无匹配服务" description="当前筛选条件下没有代肝服务，可调整价格或周期后再试。" />
        )}
      </div>
    </main>
  );
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : "请求失败，请稍后再试";
}
