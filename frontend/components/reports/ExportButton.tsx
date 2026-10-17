"use client";

import { Download } from "lucide-react";
import { Button } from "@/components/shadcn/button";
import { downloadCsv } from "@/lib/csv";
import { t } from "@/lib/messages";

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
  return (
    <Button
      type="button"
      variant="outline"
      size="sm"
      disabled={disabled}
      onClick={() => downloadCsv(fileName, headers, rows)}
    >
      <Download aria-hidden="true" className="size-4" />
      {t("reports.exportCsv")}
    </Button>
  );
}
