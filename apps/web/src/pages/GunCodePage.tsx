import { useEffect, useMemo, useState } from "react";
import { loadHome, type HomePayload } from "../modules/home/home-api";
import { loadGunCodes, loadGunCodeVotes, voteGunCode } from "../modules/gun-code/gun-code-api";
import { useAuth } from "../auth/auth-context";
import {
  gunCodeCategoryOptions,
  gunCodeGroups as fallbackGroups,
  gunCodeTagOptions,
  type GunCodeCategory,
  type GunCodeEntry,
  type GunCodeGroup,
  type GunCodeVoteType,
} from "../modules/gun-code/gun-code-data";

const GROUPS_PER_PAGE = 6;

type GunCodePageData = {
  categories: GunCodeCategory[];
  tags: string[];
  groups: GunCodeGroup[];
};

const FALLBACK_PAGE_DATA: GunCodePageData = {
  categories: gunCodeCategoryOptions,
  tags: [...gunCodeTagOptions],
  groups: fallbackGroups,
};

export function GunCodePage() {
  const { isAuthenticated, openAuthModal } = useAuth();
  const [homePayload, setHomePayload] = useState<HomePayload | null>(null);
  const [pageData, setPageData] = useState<GunCodePageData>(FALLBACK_PAGE_DATA);
  const [draftKeyword, setDraftKeyword] = useState("");
  const [keyword, setKeyword] = useState("");
  const [draftTag, setDraftTag] = useState("全部标签");
  const [activeTag, setActiveTag] = useState("全部标签");
  const [activeCategory, setActiveCategory] = useState<GunCodeCategory | "全部">("全部");
  const [currentPage, setCurrentPage] = useState(1);
  const [expandedGroups, setExpandedGroups] = useState<string[]>([]);
  const [copiedCode, setCopiedCode] = useState("");
  const [notice, setNotice] = useState("");
  const [pendingVoteCode, setPendingVoteCode] = useState("");

  useEffect(() => {
    let active = true;
    loadHome()
      .then((payload) => {
        if (active) {
          setHomePayload(payload);
        }
      })
      .catch(() => {
        if (active) {
          setHomePayload(null);
        }
      });
    loadGunCodes()
      .then((payload) => {
        if (active) {
          const nextPageData = {
            categories: payload.categories.length > 0 ? payload.categories : FALLBACK_PAGE_DATA.categories,
            tags: payload.tags.length > 0 ? payload.tags : FALLBACK_PAGE_DATA.tags,
            groups: payload.groups.length > 0 ? payload.groups : FALLBACK_PAGE_DATA.groups,
          };
          setPageData((current) => applyVoteMap(nextPageData, collectCurrentVoteMap(current)));
        }
      })
      .catch(() => {
        if (active) {
          setPageData((current) => applyVoteMap(FALLBACK_PAGE_DATA, collectCurrentVoteMap(current)));
        }
      });

    return () => {
      active = false;
    };
  }, []);

  useEffect(() => {
    if (!copiedCode) {
      return undefined;
    }
    const timer = window.setTimeout(() => setCopiedCode(""), 1800);
    return () => window.clearTimeout(timer);
  }, [copiedCode]);

  useEffect(() => {
    if (!notice) {
      return undefined;
    }
    const timer = window.setTimeout(() => setNotice(""), 2000);
    return () => window.clearTimeout(timer);
  }, [notice]);

  const allEntryCodes = useMemo(() => pageData.groups.flatMap((group) => group.entries.map((entry) => entry.code)), [pageData.groups]);
  const allEntryCodesKey = useMemo(() => allEntryCodes.join(","), [allEntryCodes]);

  useEffect(() => {
    const currentEntryCodes = allEntryCodesKey ? allEntryCodesKey.split(",") : [];
    if (!isAuthenticated || currentEntryCodes.length === 0) {
      setPageData((current) => applyVoteMap(current, {}));
      return;
    }
    let active = true;
    loadGunCodeVotes(currentEntryCodes)
      .then((voteMap) => {
        if (active) {
          setPageData((current) => applyVoteMap(current, voteMap));
        }
      })
      .catch(() => {
        if (active) {
          setPageData((current) => applyVoteMap(current, {}));
        }
      });
    return () => {
      active = false;
    };
  }, [allEntryCodesKey, isAuthenticated]);

  const filteredGroups = useMemo(() => {
    const normalizedKeyword = keyword.trim().toLowerCase();
    return pageData.groups
      .map((group) => {
        const categoryFilteredEntries = group.entries.filter((entry) => {
          if (activeCategory !== "全部" && entry.category !== activeCategory) {
            return false;
          }
          if (activeTag === "全部标签") {
            return true;
          }
          if (group.badges.includes(activeTag)) {
            return true;
          }
          return (entry.tags ?? []).includes(activeTag);
        });

        if (!normalizedKeyword) {
          return { ...group, entries: categoryFilteredEntries };
        }

        const groupMatches =
          `${group.source} ${group.creator}`.toLowerCase().includes(normalizedKeyword) ||
          group.badges.some((badge) => badge.toLowerCase().includes(normalizedKeyword));

        const keywordFilteredEntries = categoryFilteredEntries.filter((entry) => {
          const entryText = `${entry.code} ${entry.title} ${(entry.tags ?? []).join(" ")}`.toLowerCase();
          return entryText.includes(normalizedKeyword);
        });

        return {
          ...group,
          entries: groupMatches ? categoryFilteredEntries : keywordFilteredEntries,
        };
      })
      .filter((group) => group.entries.length > 0);
  }, [activeCategory, activeTag, keyword, pageData.groups]);

  const totalEntries = useMemo(
    () => filteredGroups.reduce((sum, group) => sum + group.entries.length, 0),
    [filteredGroups],
  );

  const totalPages = Math.max(1, Math.ceil(filteredGroups.length / GROUPS_PER_PAGE));
  const pagedGroups = filteredGroups.slice((currentPage - 1) * GROUPS_PER_PAGE, currentPage * GROUPS_PER_PAGE);

  useEffect(() => {
    setCurrentPage(1);
    setExpandedGroups([]);
  }, [activeCategory, activeTag, keyword]);

  useEffect(() => {
    if (currentPage > totalPages) {
      setCurrentPage(totalPages);
    }
  }, [currentPage, totalPages]);

  const pageNumbers = useMemo(() => {
    return Array.from({ length: totalPages }, (_, index) => index + 1);
  }, [totalPages]);

  const applyFilters = () => {
    setKeyword(draftKeyword.trim());
    setActiveTag(draftTag);
  };

  const toggleGroup = (groupId: string) => {
    setExpandedGroups((current) =>
      current.includes(groupId) ? current.filter((item) => item !== groupId) : [...current, groupId],
    );
  };

  const handleCopy = async (entry: GunCodeEntry) => {
    try {
      if (navigator.clipboard?.writeText) {
        await navigator.clipboard.writeText(entry.code);
      } else {
        const input = document.createElement("textarea");
        input.value = entry.code;
        input.setAttribute("readonly", "true");
        input.style.position = "absolute";
        input.style.left = "-9999px";
        document.body.appendChild(input);
        input.select();
        document.execCommand("copy");
        document.body.removeChild(input);
      }
      setCopiedCode(entry.code);
      setNotice(`已复制枪码 ${entry.code}`);
    } catch {
      setCopiedCode("");
      setNotice("复制失败，请稍后再试");
    }
  };

  const handleVote = async (entry: GunCodeEntry, type: GunCodeVoteType) => {
    if (!isAuthenticated) {
      setNotice("登录后即可点赞或点踩");
      openAuthModal("login");
      return;
    }
    try {
      setPendingVoteCode(entry.code);
      const result = await voteGunCode(entry.code, type);
      setPageData((current) =>
        updateEntryVote(current, result.entryCode, {
          likes: result.likes,
          dislikes: result.dislikes,
          currentVote: result.currentVote,
        }),
      );
      if (result.currentVote === type) {
        setNotice(type === "LIKE" ? "已点赞" : "已点踩");
      } else {
        setNotice("已撤销投票");
      }
    } catch (requestError) {
      setNotice(getErrorMessage(requestError));
    } finally {
      setPendingVoteCode("");
    }
  };

  return (
    <main className="gun-code-page">
      <section className="gun-code-hero">
        <div className="dt-container">
          <div className="gun-code-filter-panel">
            <div className="gun-code-filter-panel__header">
              <div>
                <h1>改枪码大全</h1>
                <p>支持分类筛选、搜索、一键复制</p>
              </div>
            </div>

            <div className="gun-code-toolbar">
              <input
                className="gun-code-toolbar__input"
                onChange={(event) => setDraftKeyword(event.target.value)}
                onKeyDown={(event) => {
                  if (event.key === "Enter") {
                    applyFilters();
                  }
                }}
                placeholder="搜索武器名"
                type="search"
                value={draftKeyword}
              />
              <button className="gun-code-toolbar__submit" onClick={applyFilters} type="button">
                筛选
              </button>
              <label className="gun-code-toolbar__select">
                <span>标签</span>
                <select onChange={(event) => setDraftTag(event.target.value)} value={draftTag}>
                  {pageData.tags.map((tag) => (
                    <option key={tag} value={tag}>
                      {tag}
                    </option>
                  ))}
                </select>
              </label>
            </div>

            <div className="gun-code-chip-row" role="tablist" aria-label="枪码分类">
              <button
                className={`gun-code-chip ${activeCategory === "全部" ? "is-active" : ""}`}
                onClick={() => setActiveCategory("全部")}
                type="button"
              >
                全部
              </button>
              {pageData.categories.map((category) => (
                <button
                  className={`gun-code-chip ${activeCategory === category ? "is-active" : ""}`}
                  key={category}
                  onClick={() => setActiveCategory(category)}
                  type="button"
                >
                  {category}
                </button>
              ))}
            </div>

            {notice ? (
              <div className="app-toast" role="status" aria-live="polite">
                <span>{notice}</span>
                <button onClick={() => setNotice("")} type="button">关闭</button>
              </div>
            ) : null}
          </div>
        </div>
      </section>

      <section className="gun-code-content">
        <div className="dt-container">
          {pagedGroups.length > 0 ? (
            <div className="gun-code-grid">
              {pagedGroups.map((group) => {
                const expanded = expandedGroups.includes(group.id);
                const visibleEntries = expanded ? group.entries : group.entries.slice(0, 5);
                return (
                  <article className="gun-code-card" key={group.id}>
                    <header className="gun-code-card__header">
                      <div className="gun-code-card__title-block">
                        <h3>{`${group.source ? `${group.source} ` : ""}${group.creator}`}</h3>
                        <div className="gun-code-card__badges">
                          {group.badges.map((badge) => (
                            <span className="gun-code-badge" key={`${group.id}-${badge}`}>
                              {badge}
                            </span>
                          ))}
                        </div>
                      </div>
                      {group.entries.length > 5 ? (
                        <button className="gun-code-card__more" onClick={() => toggleGroup(group.id)} type="button">
                          {expanded ? "收起" : "更多"}
                        </button>
                      ) : null}
                    </header>

                    <div className="gun-code-card__list">
                      {visibleEntries.map((entry) => (
                        <div className="gun-code-row" key={entry.code}>
                          <div className="gun-code-row__main">
                            <strong>{entry.code}</strong>
                            <span>{entry.title}</span>
                          </div>
                          <div className="gun-code-row__actions">
                            <button className="gun-code-copy-button" onClick={() => void handleCopy(entry)} type="button">
                              {copiedCode === entry.code ? "已复制" : "复制"}
                            </button>
                            <button
                              aria-pressed={entry.currentVote === "LIKE"}
                              className={`gun-code-vote gun-code-vote--up ${entry.currentVote === "LIKE" ? "is-active" : ""}`}
                              disabled={pendingVoteCode === entry.code}
                              onClick={() => void handleVote(entry, "LIKE")}
                              type="button"
                            >
                              👍 {entry.likes}
                            </button>
                            <button
                              aria-pressed={entry.currentVote === "DISLIKE"}
                              className={`gun-code-vote gun-code-vote--down ${entry.currentVote === "DISLIKE" ? "is-active" : ""}`}
                              disabled={pendingVoteCode === entry.code}
                              onClick={() => void handleVote(entry, "DISLIKE")}
                              type="button"
                            >
                              👎 {entry.dislikes}
                            </button>
                          </div>
                        </div>
                      ))}
                    </div>
                  </article>
                );
              })}
            </div>
          ) : (
            <div className="gun-code-empty">
              <strong>当前筛选下没有匹配枪码</strong>
              <p>可以试试切换分类、清空标签，或者直接搜索具体武器名。</p>
            </div>
          )}

          <div className="gun-code-pagination">
            <span className="gun-code-pagination__summary">总条数 {totalEntries}</span>
            <div className="gun-code-pagination__controls">
              <button disabled={currentPage === 1} onClick={() => setCurrentPage((page) => Math.max(1, page - 1))} type="button">
                上一页
              </button>
              {pageNumbers.map((page) => (
                <button
                  className={currentPage === page ? "is-active" : ""}
                  key={page}
                  onClick={() => setCurrentPage(page)}
                  type="button"
                >
                  {page}
                </button>
              ))}
              <button disabled={currentPage === totalPages} onClick={() => setCurrentPage((page) => Math.min(totalPages, page + 1))} type="button">
                下一页
              </button>
            </div>
          </div>
        </div>
      </section>

      <section className="assurance-strip">
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

function applyVoteMap(pageData: GunCodePageData, voteMap: Record<string, GunCodeVoteType>) {
  return {
    ...pageData,
    groups: pageData.groups.map((group) => ({
      ...group,
      entries: group.entries.map((entry) => ({
        ...entry,
        currentVote: voteMap[entry.code] ?? null,
      })),
    })),
  };
}

function updateEntryVote(
  pageData: GunCodePageData,
  entryCode: string,
  next: { likes: number; dislikes: number; currentVote: GunCodeVoteType | null },
) {
  return {
    ...pageData,
    groups: pageData.groups.map((group) => ({
      ...group,
      entries: group.entries.map((entry) =>
        entry.code === entryCode
          ? {
            ...entry,
            likes: next.likes,
            dislikes: next.dislikes,
            currentVote: next.currentVote,
          }
          : entry,
      ),
    })),
  };
}

function collectCurrentVoteMap(pageData: GunCodePageData) {
  const voteMap: Record<string, GunCodeVoteType> = {};
  for (const group of pageData.groups) {
    for (const entry of group.entries) {
      if (entry.currentVote) {
        voteMap[entry.code] = entry.currentVote;
      }
    }
  }
  return voteMap;
}

function getErrorMessage(error: unknown) {
  return error instanceof Error ? error.message : "操作失败，请稍后再试";
}
