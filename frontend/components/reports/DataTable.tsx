"use client";

import type { ReactNode } from "react";
import ExportButton from "@/components/reports/ExportButton";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";
import { Empty } from "@/components/ui";
import type { ExportCell } from "@/lib/xlsx";

export type Column<T> = {
  header: string;
  cell: (row: T) => ReactNode;
  value: (row: T) => ExportCell;
  numeric?: boolean;
};

export default function DataTable<T>({
  title,
  fileName,
  columns,
  rows,
  rowKey,
  footer,
}: {
  title: string;
  fileName: string;
  columns: Column<T>[];
  rows: T[];
  rowKey: (row: T) => string;
  footer?: ReactNode;
}) {
  return (
    <div className="grid gap-3">
      <div className="flex items-center justify-between gap-3">
        <h2 className="m-0">{title}</h2>
        <ExportButton
          fileName={fileName}
          disabled={rows.length === 0}
          headers={columns.map((column) => column.header)}
          rows={rows.map((row) => columns.map((column) => column.value(row)))}
        />
      </div>
      {rows.length === 0 ? (
        <Empty />
      ) : (
        <div className="overflow-x-auto">
          <Table>
            <TableHeader>
              <TableRow>
                {columns.map((column) => (
                  <TableHead key={column.header} className={column.numeric ? "text-right" : undefined}>
                    {column.header}
                  </TableHead>
                ))}
              </TableRow>
            </TableHeader>
            <TableBody>
              {rows.map((row) => (
                <TableRow key={rowKey(row)}>
                  {columns.map((column) => (
                    <TableCell key={column.header} className={column.numeric ? "text-right" : undefined}>
                      {column.cell(row)}
                    </TableCell>
                  ))}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
      {footer}
    </div>
  );
}
