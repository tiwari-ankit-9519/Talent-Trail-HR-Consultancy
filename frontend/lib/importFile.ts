import type { ImportColumn } from "@/lib/importConfig";

export const MAX_IMPORT_ROWS = 1000;

export type ParsedImport = { rows: Record<string, string>[]; missing: string[]; tooMany: boolean };

function normalise(value: string): string {
  return value.toLowerCase().replace(/[^a-z0-9]/g, "");
}

function parseCsv(text: string): string[][] {
  const clean = text.replace(/^﻿/, "");
  const firstLine = clean.split(/\r?\n/, 1)[0] ?? "";
  const delimiter = firstLine.split(";").length > firstLine.split(",").length ? ";" : ",";
  const rows: string[][] = [];
  let row: string[] = [];
  let cell = "";
  let quoted = false;
  for (let i = 0; i < clean.length; i++) {
    const char = clean[i];
    if (quoted) {
      if (char === '"' && clean[i + 1] === '"') {
        cell += '"';
        i++;
      } else if (char === '"') {
        quoted = false;
      } else {
        cell += char;
      }
    } else if (char === '"') {
      quoted = true;
    } else if (char === delimiter) {
      row.push(cell);
      cell = "";
    } else if (char === "\n" || char === "\r") {
      if (char === "\r" && clean[i + 1] === "\n") i++;
      row.push(cell);
      rows.push(row);
      row = [];
      cell = "";
    } else {
      cell += char;
    }
  }
  if (cell !== "" || row.length > 0) {
    row.push(cell);
    rows.push(row);
  }
  return rows;
}

function cellText(value: unknown): string {
  if (value === null || value === undefined) return "";
  if (value instanceof Date) {
    const month = String(value.getUTCMonth() + 1).padStart(2, "0");
    const day = String(value.getUTCDate()).padStart(2, "0");
    return `${value.getUTCFullYear()}-${month}-${day}`;
  }
  if (typeof value === "object") {
    const record = value as Record<string, unknown>;
    if (Array.isArray(record.richText)) {
      return (record.richText as { text?: string }[]).map((part) => part.text ?? "").join("");
    }
    if ("result" in record) return cellText(record.result);
    if ("text" in record) return cellText(record.text);
    return "";
  }
  return String(value);
}

async function parseXlsx(file: File): Promise<string[][]> {
  const { Workbook } = await import("exceljs");
  const workbook = new Workbook();
  await workbook.xlsx.load(await file.arrayBuffer());
  const sheet = workbook.worksheets[0];
  const rows: string[][] = [];
  if (!sheet) return rows;
  sheet.eachRow({ includeEmpty: false }, (row) => {
    const values: string[] = [];
    for (let column = 1; column <= sheet.columnCount; column++) {
      values.push(cellText(row.getCell(column).value));
    }
    rows.push(values);
  });
  return rows;
}

export async function parseImportFile(file: File, columns: ImportColumn[], labelOf: (key: string) => string) {
  const raw = /\.xlsx$/i.test(file.name) ? await parseXlsx(file) : parseCsv(await file.text());
  const header = raw[0] ?? [];
  const byHeader = new Map<string, number>();
  header.forEach((name, index) => byHeader.set(normalise(name), index));
  const indexOf = new Map<string, number>();
  columns.forEach((column) => {
    const found = byHeader.get(normalise(labelOf(column.key))) ?? byHeader.get(normalise(column.key));
    if (found !== undefined) indexOf.set(column.key, found);
  });
  const missing = columns.filter((column) => column.required && !indexOf.has(column.key)).map((column) => column.key);
  const rows: Record<string, string>[] = [];
  for (const line of raw.slice(1)) {
    const record: Record<string, string> = {};
    let any = false;
    columns.forEach((column) => {
      const index = indexOf.get(column.key);
      const value = index === undefined ? "" : (line[index] ?? "").trim();
      if (value !== "") any = true;
      record[column.key] = value;
    });
    if (any) rows.push(record);
  }
  return { rows: rows.slice(0, MAX_IMPORT_ROWS), missing, tooMany: rows.length > MAX_IMPORT_ROWS } as ParsedImport;
}
