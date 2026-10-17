"use client";

import { useEffect, useState } from "react";
import {
  AlertDialog,
  AlertDialogAction,
  AlertDialogCancel,
  AlertDialogContent,
  AlertDialogDescription,
  AlertDialogFooter,
  AlertDialogTitle,
} from "@/components/shadcn/alert-dialog";
import { registerConfirm } from "@/lib/confirm";
import { t } from "@/lib/messages";

type Pending = { message: string; resolve: (value: boolean) => void };

export default function ConfirmHost() {
  const [pending, setPending] = useState<Pending | null>(null);

  useEffect(() => {
    registerConfirm((message) => new Promise<boolean>((resolve) => setPending({ message, resolve })));
    return () => registerConfirm(null);
  }, []);

  function close(result: boolean) {
    pending?.resolve(result);
    setPending(null);
  }

  return (
    <AlertDialog open={pending !== null} onOpenChange={(open) => !open && close(false)}>
      <AlertDialogContent>
        <AlertDialogTitle>{t("common.confirmTitle")}</AlertDialogTitle>
        <AlertDialogDescription>{pending?.message}</AlertDialogDescription>
        <AlertDialogFooter>
          <AlertDialogCancel onClick={() => close(false)}>{t("common.cancel")}</AlertDialogCancel>
          <AlertDialogAction onClick={() => close(true)}>{t("common.confirm")}</AlertDialogAction>
        </AlertDialogFooter>
      </AlertDialogContent>
    </AlertDialog>
  );
}
