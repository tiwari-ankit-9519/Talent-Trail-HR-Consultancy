"use client";

import { ChevronDown, Download } from "lucide-react";
import { useState } from "react";
import { Button } from "@/components/shadcn/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from "@/components/shadcn/dropdown-menu";
import { downloadCsv } from "@/lib/csv";
import { t } from "@/lib/messages";
import { downloadXlsx } from "@/lib/xlsx";

export default function ExportButton({
  fileName,
  headers,
  rows,
  disabled,
}: {
  fileName: string;
  headers: string[];
  rows: (string | number | null | undefined)[][];
  disabled?: boolean;
}) {
  const [busy, setBusy] = useState(false);
  const base = fileName.replace(/\.(csv|xlsx)$/i, "");

  async function excel() {
    setBusy(true);
    try {
      await downloadXlsx(`${base}.xlsx`, base, headers, rows);
    } finally {
      setBusy(false);
    }
  }

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button type="button" variant="outline" size="sm" disabled={disabled || busy}>
          <Download aria-hidden="true" className="size-4" />
          {t("reports.export")}
          <ChevronDown aria-hidden="true" className="size-4" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuItem onSelect={() => void excel()}>{t("reports.exportExcel")}</DropdownMenuItem>
        <DropdownMenuItem onSelect={() => downloadCsv(`${base}.csv`, headers, rows)}>
          {t("reports.exportCsv")}
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  );
}
