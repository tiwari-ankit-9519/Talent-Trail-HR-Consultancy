"use client";

import { Download } from "lucide-react";
import { useRef, useState } from "react";
import ExportButton from "@/components/reports/ExportButton";
import { Alert } from "@/components/shadcn/alert";
import { Badge } from "@/components/shadcn/badge";
import { Button } from "@/components/shadcn/button";
import { Card } from "@/components/shadcn/card";
import { Input } from "@/components/shadcn/input";
import { FormSelect } from "@/components/shadcn/select";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import { ErrorBanner, Field } from "@/components/ui";
import { api } from "@/lib/api";
import { downloadCsv } from "@/lib/csv";
import { confirmDialog } from "@/lib/confirm";
import { importColumns, type ImportType } from "@/lib/importConfig";
import { MAX_IMPORT_ROWS, parseImportFile } from "@/lib/importFile";
import { t, tx } from "@/lib/messages";
import type { ImportReport, ImportRowResult } from "@/lib/types";
import { downloadXlsx } from "@/lib/xlsx";

type Filter = "ALL" | "OK" | "DUPLICATE" | "INVALID";

const badge: Record<ImportRowResult["status"], "ok" | "warn" | "bad" | "accent"> = {
  VALID: "accent",
  IMPORTED: "ok",
  DUPLICATE: "warn",
  INVALID: "bad",
};

export default function ImportPanel({ type }: { type: ImportType }) {
  const columns = importColumns[type];
  const label = (key: string) => tx(`import.cols.${key}`, key);
  const [rows, setRows] = useState<Record<string, string>[]>([]);
  const [report, setReport] = useState<ImportReport | null>(null);
  const [fileName, setFileName] = useState("");
  const [problem, setProblem] = useState<string | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);
  const [filter, setFilter] = useState<Filter>("ALL");
  const input = useRef<HTMLInputElement>(null);

  const headers = columns.map((column) => label(column.key));
  const sample = columns.map((column) => column.sample);

  function reset() {
    setRows([]);
    setReport(null);
    setFileName("");
    setProblem(null);
    setError(null);
    setFilter("ALL");
    if (input.current) input.current.value = "";
  }

  async function choose(file: File | null) {
    reset();
    if (!file) return;
    setBusy(true);
    try {
      const parsed = await parseImportFile(file, columns, label);
      if (parsed.missing.length > 0) {
        setProblem(`${t("import.missingColumns")} ${parsed.missing.map(label).join(", ")}`);
        return;
      }
      if (parsed.rows.length === 0) {
        setProblem(t("import.empty"));
        return;
      }
      if (parsed.tooMany) setProblem(t("import.tooMany").replace("{max}", String(MAX_IMPORT_ROWS)));
      setFileName(file.name);
      setRows(parsed.rows);
      setReport(await api<ImportReport>(`/imports/${type}/preview`, { method: "POST", body: { rows: parsed.rows } }));
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  async function commit() {
    if (!report) return;
    if (!(await confirmDialog(t("import.confirm").replace("{count}", String(report.valid))))) return;
    setBusy(true);
    setError(null);
    try {
      setReport(await api<ImportReport>(`/imports/${type}/commit`, { method: "POST", body: { rows } }));
      setRows([]);
      setFilter("ALL");
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  const shown = (report?.rows ?? []).filter((row) => {
    if (filter === "ALL") return true;
    if (filter === "OK") return row.status === "VALID" || row.status === "IMPORTED";
    return row.status === filter;
  });

  return (
    <div className="mt-4 grid gap-4">
      <Card className="grid gap-3 p-4">
        <h2 className="m-0">{t("import.step1")}</h2>
        <p className="muted m-0 text-sm">{tx(`import.help.${type}`)}</p>
        <p className="muted m-0 text-sm">
          {t("import.required")}:{" "}
          {columns
            .filter((column) => column.required)
            .map((column) => label(column.key))
            .join(", ")}
        </p>
        <div className="flex flex-wrap gap-2">
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => void downloadXlsx(`${type}-template.xlsx`, type, headers, [sample])}
          >
            <Download aria-hidden="true" className="size-4" />
            {t("import.templateExcel")}
          </Button>
          <Button
            type="button"
            variant="outline"
            size="sm"
            onClick={() => downloadCsv(`${type}-template.csv`, headers, [sample])}
          >
            <Download aria-hidden="true" className="size-4" />
            {t("import.templateCsv")}
          </Button>
        </div>
      </Card>
      <Card className="grid gap-3 p-4">
        <h2 className="m-0">{t("import.step2")}</h2>
        <Field label={t("import.file")} hint={t("import.fileHint").replace("{max}", String(MAX_IMPORT_ROWS))}>
          <Input
            ref={input}
            type="file"
            accept=".csv,.xlsx"
            disabled={busy}
            onChange={(e) => void choose(e.target.files?.[0] ?? null)}
          />
        </Field>
        {problem && <Alert variant="destructive">{problem}</Alert>}
        <ErrorBanner error={error} />
      </Card>
      {report && (
        <Card className="grid gap-4 p-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <h2 className="m-0">{report.committed ? t("import.resultTitle") : t("import.previewTitle")}</h2>
            <span className="muted text-sm">{fileName}</span>
          </div>
          <div className="flex flex-wrap gap-2">
            <Badge variant="default">
              {t("import.total")}: {report.total}
            </Badge>
            {!report.committed && (
              <Badge variant="accent">
                {t("import.ready")}: {report.valid}
              </Badge>
            )}
            {report.committed && (
              <Badge variant="ok">
                {t("import.imported")}: {report.imported}
              </Badge>
            )}
            <Badge variant="warn">
              {t("import.duplicates")}: {report.duplicates}
            </Badge>
            <Badge variant="bad">
              {t("import.errors")}: {report.invalid}
            </Badge>
          </div>
          {!report.committed && report.valid > 0 && <Alert variant="info">{t("import.skipNote")}</Alert>}
          {report.credentials.length > 0 && (
            <Alert variant="success">
              <div className="grid gap-2">
                <p>{t("import.credentialsNote")}</p>
                <div>
                  <ExportButton
                    fileName="new-employee-logins"
                    headers={[t("import.cols.fullName"), t("import.cols.email"), t("import.tempPassword")]}
                    rows={report.credentials.map((c) => [c.fullName, c.email, c.password])}
                  />
                </div>
              </div>
            </Alert>
          )}
          <div className="flex flex-wrap items-end gap-3">
            <Field label={t("import.show")}>
              <FormSelect value={filter} onChange={(e) => setFilter(e.target.value as Filter)}>
                <option value="ALL">{t("import.filters.all")}</option>
                <option value="OK">
                  {report.committed ? t("import.filters.imported") : t("import.filters.ready")}
                </option>
                <option value="DUPLICATE">{t("import.filters.duplicates")}</option>
                <option value="INVALID">{t("import.filters.errors")}</option>
              </FormSelect>
            </Field>
            {!report.committed && (
              <Button type="button" disabled={busy || report.valid === 0} onClick={() => void commit()}>
                {t("import.run").replace("{count}", String(report.valid))}
              </Button>
            )}
            <Button type="button" variant="outline" onClick={reset}>
              {report.committed ? t("import.another") : t("import.cancel")}
            </Button>
          </div>
          <div className="overflow-x-auto">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead>{t("import.row")}</TableHead>
                  <TableHead>{t("import.record")}</TableHead>
                  <TableHead>{t("import.status")}</TableHead>
                  <TableHead>{t("import.details")}</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {shown.map((row) => (
                  <TableRow key={`${row.row}-${row.label}`}>
                    <TableCell>{row.row}</TableCell>
                    <TableCell>{row.label}</TableCell>
                    <TableCell>
                      <Badge variant={badge[row.status]}>{t(`import.statuses.${row.status}`)}</Badge>
                    </TableCell>
                    <TableCell>{row.messages.join("; ")}</TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </div>
        </Card>
      )}
    </div>
  );
}
