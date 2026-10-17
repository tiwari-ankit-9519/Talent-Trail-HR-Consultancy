"use client";

import { Printer } from "lucide-react";
import { createPortal } from "react-dom";
import { Button } from "@/components/shadcn/button";
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/shadcn/dialog";
import PayslipDocument from "@/components/payroll/PayslipDocument";
import { t } from "@/lib/messages";
import type { Payslip } from "@/lib/types";

export default function PayslipDialog({ slip, onClose }: { slip: Payslip | null; onClose: () => void }) {
  return (
    <Dialog open={slip !== null} onOpenChange={(open) => !open && onClose()}>
      <DialogContent closeLabel={t("common.close")} className="max-w-2xl">
        <DialogHeader>
          <DialogTitle>{t("payroll.payslip.dialogTitle")}</DialogTitle>
          <DialogDescription>{t("payroll.payslip.dialogHelp")}</DialogDescription>
        </DialogHeader>
        {slip && <PayslipDocument slip={slip} />}
        <DialogFooter>
          <Button type="button" variant="outline" onClick={onClose}>
            {t("common.close")}
          </Button>
          <Button type="button" onClick={() => window.print()}>
            <Printer aria-hidden="true" className="size-4" />
            {t("payroll.payslip.download")}
          </Button>
        </DialogFooter>
      </DialogContent>
      {slip &&
        typeof document !== "undefined" &&
        createPortal(
          <div id="payslip-print-root" className="payslip-plain">
            <PayslipDocument slip={slip} />
          </div>,
          document.body,
        )}
    </Dialog>
  );
}
