import { useEffect, useMemo, useState } from "react";
import { Button, Tag } from "@delta/ui";
import {
  importAdminGunCodes,
  loadAdminGunCodes,
  type AdminGunCodeCenter,
  type AdminGunCodeImportResult,
  type AdminGunCodeImportRow,
} from "./admin-api";

const TEMPLATE_TEXT = `creator,source,badges,title,category,entryCode,tags,groupSortNo,entrySortNo
Bosh,抖音,主播顶护魔王同款,M14射手步枪,精确射手步枪,6J4F3B001T61VNNP5VK98,,10,10
辰阿,抖音,主播顶护魔王同款|主播同款,AS Val突击步枪,步枪,6JIHRKC0473LGNSN240H5,主播同款,20,10`;

type ParseResult = {
  rows: AdminGunCodeImportRow[];
  warnings: string[];
};

export function AdminGunCodeUploadSection() {
  const [center, setCenter] = useState<AdminGunCodeCenter | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [sourceText, setSourceText] = useState(TEMPLATE_TEXT);
  const [replaceExisting, setReplaceExisting] = useState(false);
  const [parsedRows, setParsedRows] = useState<AdminGunCodeImportRow[]>([]);
  const [parseWarnings, setParseWarnings] = useState<string[]>([]);
  const [parseError, setParseError] = useState("");
  const [importing, setImporting] = useState(false);
  const [result, setResult] = useState<AdminGunCodeImportResult | null>(null);

  useEffect(() => {
    void refresh();
  }, []);

  const previewRows = useMemo(() => parsedRows.slice(0, 8), [parsedRows]);
  const onlinePreviewRows = useMemo(() => center?.rows.slice(0, 12) ?? [], [center]);

  async function refresh() {
    setLoading(true);
    setError("");
    try {
      setCenter(await loadAdminGunCodes());
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : "改枪码中心加载失败");
    } finally {
      setLoading(false);
    }
  }

  function handleParse() {
    setParseError("");
    setResult(null);
    try {
      const next = parseSourceText(sourceText);
      setParsedRows(next.rows);
      setParseWarnings(next.warnings);
    } catch (requestError) {
      setParsedRows([]);
      setParseWarnings([]);
      setParseError(requestError instanceof Error ? requestError.message : "解析失败");
    }
  }

  async function handleImport() {
    if (!parsedRows.length) {
      setParseError("请先解析至少一条改枪码数据");
      return;
    }
    setImporting(true);
    setParseError("");
    try {
      const response = await importAdminGunCodes({ replaceExisting, rows: parsedRows });
      setCenter(response.center);
      setResult(response);
    } catch (requestError) {
      setParseError(requestError instanceof Error ? requestError.message : "导入失败");
    } finally {
      setImporting(false);
    }
  }

  async function handleFilePicked(file: File) {
    const text = await file.text();
    setSourceText(text);
    setResult(null);
  }

  return (
    <div className="console-gun-code-workbench">
      <header className="console-gun-code-hero">
        <div className="console-gun-code-hero__copy">
          <p className="console-gun-code-hero__eyebrow">改枪码库</p>
          <h3>全量导入、解析预览与线上库巡检</h3>
          <p>这一页作为独立工作台使用，导入、预览和线上核对拆成固定区域，不再塞在普通后台卡片里。</p>
        </div>
        <div className="console-gun-code-hero__actions">
          <Button kind="secondary" onClick={() => void refresh()}>刷新现有数据</Button>
        </div>
      </header>

      <div className="console-gun-code-panel">
        <div className="console-gun-code-summary">
          <SummaryItem label="分组数" value={String(center?.summary.groupCount ?? 0)} />
          <SummaryItem label="枪码数" value={String(center?.summary.entryCount ?? 0)} />
          <SummaryItem label="分类数" value={String(center?.summary.categoryCount ?? 0)} />
          <SummaryItem label="标签数" value={String(center?.summary.tagCount ?? 0)} />
        </div>

        <div className="console-gun-code-grid">
          <section className="console-operation-editor console-gun-code-editor console-gun-code-card">
            <div className="console-gun-code-header">
              <div>
                <strong>上传源数据</strong>
                <p>支持 CSV / TSV 文本。表头固定为 `creator,source,badges,title,category,entryCode,tags,groupSortNo,entrySortNo`，其中 `badges/tags` 用 `|` 分隔。</p>
              </div>
              <div className="console-operation-editor__actions">
                <input
                  id="admin-gun-code-upload"
                  type="file"
                  accept=".csv,.tsv,.txt"
                  hidden
                  onChange={(event) => {
                    const file = event.target.files?.[0];
                    event.currentTarget.value = "";
                    if (file) {
                      void handleFilePicked(file);
                    }
                  }}
                />
                <label className="dt-button dt-button--secondary publish-upload-trigger" htmlFor="admin-gun-code-upload">
                  选择文件
                </label>
                <Button kind="secondary" onClick={() => setSourceText(TEMPLATE_TEXT)}>载入模板</Button>
                <Button kind="ghost" onClick={() => setSourceText("")}>清空文本</Button>
              </div>
            </div>

            <label>
              改枪码原始内容
              <textarea
                className="console-gun-code-textarea"
                rows={14}
                value={sourceText}
                onChange={(event) => setSourceText(event.target.value)}
                placeholder="把 CSV 内容直接贴进来，或先选择文件。"
              />
            </label>

            <label className="console-operation-editor__checkbox">
              <input
                checked={replaceExisting}
                onChange={(event) => setReplaceExisting(event.target.checked)}
                type="checkbox"
              />
              覆盖导入
            </label>

            <div className="console-gun-code-notice">
              <span>关闭时为增量导入，已存在枪码会更新资料；开启后会删除本次未出现的枪码和空分组。</span>
            </div>

            <div className="console-operation-editor__actions">
              <Button onClick={handleParse}>解析并预览</Button>
              <Button kind="secondary" disabled={!parsedRows.length || importing} onClick={() => void handleImport()}>
                {importing ? "导入中" : "确认导入"}
              </Button>
            </div>

            {parseError ? <p className="console-gun-code-error">{parseError}</p> : null}
            {parseWarnings.length ? (
              <div className="console-gun-code-warning">
                {parseWarnings.map((item) => (
                  <p key={item}>{item}</p>
                ))}
              </div>
            ) : null}
            {result ? (
              <div className="console-gun-code-result">
                <Tag tone="success">{result.replaceExisting ? "覆盖导入完成" : "增量导入完成"}</Tag>
                <span>共导入 {result.importedRowCount} 行，新增分组 {result.insertedGroups}，更新分组 {result.updatedGroups}，新增枪码 {result.insertedEntries}，更新枪码 {result.updatedEntries}</span>
              </div>
            ) : null}
          </section>

          <section className="console-gun-code-preview console-gun-code-card">
            <div className="console-gun-code-header">
              <div>
                <strong>解析预览</strong>
                <p>{parsedRows.length ? `当前已解析 ${parsedRows.length} 条枪码` : "先解析后再导入，避免字段错位直接写库。"}</p>
              </div>
              {replaceExisting ? <Tag tone="warning">覆盖模式</Tag> : <Tag tone="accent">增量模式</Tag>}
            </div>

            <div className="console-gun-code-preview-metrics">
              <SummaryItem label="已解析条数" value={String(parsedRows.length)} />
              <SummaryItem label="预警数" value={String(parseWarnings.length)} />
              <SummaryItem label="预览行数" value={String(previewRows.length)} />
            </div>

            {previewRows.length ? (
              <div className="console-gun-code-table">
                <table>
                  <thead>
                    <tr>
                      <th>创作者</th>
                      <th>枪码</th>
                      <th>标题</th>
                      <th>分类</th>
                      <th>标签</th>
                    </tr>
                  </thead>
                  <tbody>
                    {previewRows.map((row) => (
                      <tr key={row.entryCode}>
                        <td>{row.creator}</td>
                        <td>{row.entryCode}</td>
                        <td>{row.title}</td>
                        <td>{row.category}</td>
                        <td>{row.tags.join(" / ") || "-"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
                {parsedRows.length > previewRows.length ? <small>仅预览前 {previewRows.length} 条，剩余导入时一并写入。</small> : null}
              </div>
            ) : (
              <div className="console-gun-code-empty">暂无解析结果</div>
            )}
          </section>
        </div>

        <section className="console-gun-code-existing console-gun-code-card">
          <div className="console-gun-code-header">
            <div>
              <strong>当前线上分组</strong>
              <p>用于快速核对创作者、来源、标签和排序是否符合预期。</p>
            </div>
            {center ? <Tag tone="success">共 {center.rows.length} 个分组</Tag> : null}
          </div>
          {loading ? <div className="console-gun-code-empty">正在加载现有改枪码...</div> : null}
          {error ? <p className="console-gun-code-error">{error}</p> : null}
          {!loading && !error && onlinePreviewRows.length ? (
            <div className="console-gun-code-existing-list">
              {onlinePreviewRows.map((row) => (
                <article className="console-gun-code-existing-item" key={row.groupKey}>
                  <div className="console-gun-code-existing-item__top">
                    <strong>{row.source ? `${row.source} ${row.creator}` : row.creator}</strong>
                    <Tag tone="success">{row.entryCount} 条</Tag>
                  </div>
                  <p>排序 {row.sortNo} · {row.updatedAt}</p>
                  <div className="console-gun-code-existing-item__badges">
                    {(row.badges.length ? row.badges : ["无标签"]).map((badge) => (
                      <Tag key={`${row.groupKey}-${badge}`} tone="default">{badge}</Tag>
                    ))}
                  </div>
                </article>
              ))}
            </div>
          ) : null}
          {!loading && !error && !onlinePreviewRows.length ? <div className="console-gun-code-empty">当前线上还没有分组数据</div> : null}
        </section>
      </div>
    </div>
  );
}

function SummaryItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="console-gun-code-stat">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function parseSourceText(sourceText: string): ParseResult {
  const normalized = sourceText.trim();
  if (!normalized) {
    throw new Error("请先粘贴或上传改枪码内容");
  }
  const matrix = parseDelimitedMatrix(normalized);
  if (matrix.length < 2) {
    throw new Error("至少需要表头和 1 行数据");
  }

  const header = matrix[0].map((item) => normalizeHeader(item));
  const rows = matrix.slice(1).filter((item) => item.some((value) => value.trim()));
  const indexMap = buildHeaderIndexMap(header);
  const warnings: string[] = [];
  const parsedRows: AdminGunCodeImportRow[] = rows.map((columns, index) => {
    const read = (key: keyof ReturnType<typeof buildHeaderIndexMap>) => {
      const headerIndex = indexMap[key];
      return headerIndex >= 0 ? (columns[headerIndex] ?? "").trim() : "";
    };
    const creator = read("creator");
    const title = read("title");
    const category = read("category");
    const entryCode = read("entryCode").toUpperCase();
    if (!creator || !title || !category || !entryCode) {
      throw new Error(`第 ${index + 2} 行缺少必填字段，至少需要 creator/title/category/entryCode`);
    }
    return {
      creator,
      source: read("source"),
      badges: splitInlineValues(read("badges")),
      title,
      category,
      entryCode,
      tags: splitInlineValues(read("tags")),
      groupSortNo: parseOptionalNumber(read("groupSortNo"), warnings, index + 2, "groupSortNo"),
      entrySortNo: parseOptionalNumber(read("entrySortNo"), warnings, index + 2, "entrySortNo"),
    };
  });

  return { rows: parsedRows, warnings };
}

function parseDelimitedMatrix(text: string) {
  const delimiter = detectDelimiter(text);
  const rows: string[][] = [];
  let field = "";
  let currentRow: string[] = [];
  let quoted = false;

  for (let index = 0; index < text.length; index += 1) {
    const current = text[index];
    const next = text[index + 1];
    if (current === "\"") {
      if (quoted && next === "\"") {
        field += "\"";
        index += 1;
      } else {
        quoted = !quoted;
      }
      continue;
    }
    if (!quoted && current === delimiter) {
      currentRow.push(field);
      field = "";
      continue;
    }
    if (!quoted && (current === "\n" || current === "\r")) {
      if (current === "\r" && next === "\n") {
        index += 1;
      }
      currentRow.push(field);
      rows.push(currentRow);
      currentRow = [];
      field = "";
      continue;
    }
    field += current;
  }
  currentRow.push(field);
  rows.push(currentRow);
  return rows;
}

function detectDelimiter(text: string) {
  const firstLine = text.split(/\r?\n/, 1)[0] ?? "";
  return firstLine.includes("\t") ? "\t" : ",";
}

function buildHeaderIndexMap(header: string[]) {
  return {
    creator: findHeaderIndex(header, ["creator", "创作者", "作者"]),
    source: findHeaderIndex(header, ["source", "来源", "平台来源"]),
    badges: findHeaderIndex(header, ["badges", "徽标", "分组标签", "分组徽标"]),
    title: findHeaderIndex(header, ["title", "标题", "枪码标题"]),
    category: findHeaderIndex(header, ["category", "分类"]),
    entryCode: findHeaderIndex(header, ["entrycode", "code", "枪码", "枪码编号"]),
    tags: findHeaderIndex(header, ["tags", "标签"]),
    groupSortNo: findHeaderIndex(header, ["groupsortno", "groupsort", "组排序", "分组排序"]),
    entrySortNo: findHeaderIndex(header, ["entrysortno", "entrysort", "条目排序", "枪码排序"]),
  };
}

function findHeaderIndex(header: string[], aliases: string[]) {
  return header.findIndex((item) => aliases.includes(item));
}

function normalizeHeader(value: string) {
  return value.trim().replace(/\s+/g, "").toLowerCase();
}

function splitInlineValues(value: string) {
  return value
    .split(/[|,，/]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function parseOptionalNumber(value: string, warnings: string[], rowNo: number, field: string) {
  if (!value) {
    return undefined;
  }
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) {
    warnings.push(`第 ${rowNo} 行字段 ${field} 不是有效数字，已忽略并自动排序`);
    return undefined;
  }
  return parsed;
}
