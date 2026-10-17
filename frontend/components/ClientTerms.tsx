"use client";

import { useState, type FormEvent } from "react";
import { ErrorBanner, Field } from "@/components/ui";
import { api } from "@/lib/api";
import { t } from "@/lib/messages";
import type { ClientDetail, ClientTerms as Terms, FeeType } from "@/lib/types";
import { Button } from "@/components/shadcn/button";
import { FormSelect } from "@/components/shadcn/select";
import { CheckboxField } from "@/components/shadcn/checkbox";
import { Input } from "@/components/shadcn/input";
import { Alert } from "@/components/shadcn/alert";
import { Card } from "@/components/shadcn/card";

type Props = {
  client: ClientDetail;
  terms: Terms;
  canManage: boolean;
  onSaved: (client: ClientDetail) => void;
};

export default function ClientTerms({ client, terms, canManage, onSaved }: Props) {
  const [feeType, setFeeType] = useState<FeeType>(terms.feeType);
  const [feeValue, setFeeValue] = useState(String(terms.feeValue));
  const [gstApplicable, setGstApplicable] = useState(terms.gstApplicable);
  const [gstRate, setGstRate] = useState(String(terms.gstRate));
  const [tdsApplicable, setTdsApplicable] = useState(terms.tdsApplicable);
  const [tdsRate, setTdsRate] = useState(String(terms.tdsRate));
  const [paymentDays, setPaymentDays] = useState(String(terms.paymentTermsDays));
  const [replacementDays, setReplacementDays] = useState(String(terms.replacementPeriodDays));
  const [error, setError] = useState<unknown>(null);
  const [saved, setSaved] = useState(false);
  const [busy, setBusy] = useState(false);

  async function submit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setSaved(false);
    try {
      const result = await api<ClientDetail>(`/clients/${client.id}/terms`, {
        method: "PUT",
        body: {
          feeType,
          feeValue: Number(feeValue),
          gstApplicable,
          gstRate: Number(gstRate),
          tdsApplicable,
          tdsRate: Number(tdsRate),
          paymentTermsDays: Number(paymentDays),
          replacementPeriodDays: Number(replacementDays),
          version: client.version,
        },
      });
      setSaved(true);
      onSaved(result);
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  return (
    <Card asChild>
      <form className="form-card" onSubmit={(e) => void submit(e)}>
        <ErrorBanner error={error} />
        {saved && <Alert variant="success">{t("common.saved")}</Alert>}
        <div className="form-grid">
          <Field label={t("terms.feeType")}>
            <FormSelect disabled={!canManage} value={feeType} onChange={(e) => setFeeType(e.target.value as FeeType)}>
              <option value="MONTHS_OF_SALARY">{t("terms.monthsOfSalary")}</option>
              <option value="CTC_PERCENT">{t("terms.ctcPercent")}</option>
            </FormSelect>
          </Field>
          <Field label={feeType === "MONTHS_OF_SALARY" ? t("terms.feeMonths") : t("terms.feePercent")}>
            <Input
              type="number"
              step="0.01"
              min="0"
              required
              disabled={!canManage}
              value={feeValue}
              onChange={(e) => setFeeValue(e.target.value)}
            />
          </Field>
          <Field label={t("terms.paymentDays")} hint={t("terms.paymentDaysHint")}>
            <Input
              type="number"
              min="0"
              max="365"
              required
              disabled={!canManage}
              value={paymentDays}
              onChange={(e) => setPaymentDays(e.target.value)}
            />
          </Field>
          <Field label={t("terms.replacementDays")} hint={t("terms.replacementDaysHint")}>
            <Input
              type="number"
              min="0"
              max="730"
              required
              disabled={!canManage}
              value={replacementDays}
              onChange={(e) => setReplacementDays(e.target.value)}
            />
          </Field>
        </div>
        <div className="form-grid">
          <label className="check">
            <CheckboxField
              disabled={!canManage}
              checked={gstApplicable}
              onChange={(e) => setGstApplicable(e.target.checked)}
            />
            {t("terms.gstApplicable")}
          </label>
          <Field label={t("terms.gstRate")}>
            <Input
              type="number"
              step="0.01"
              min="0"
              max="100"
              disabled={!canManage || !gstApplicable}
              value={gstRate}
              onChange={(e) => setGstRate(e.target.value)}
            />
          </Field>
          <label className="check">
            <CheckboxField
              disabled={!canManage}
              checked={tdsApplicable}
              onChange={(e) => setTdsApplicable(e.target.checked)}
            />
            {t("terms.tdsApplicable")}
          </label>
          <Field label={t("terms.tdsRate")}>
            <Input
              type="number"
              step="0.01"
              min="0"
              max="100"
              disabled={!canManage || !tdsApplicable}
              value={tdsRate}
              onChange={(e) => setTdsRate(e.target.value)}
            />
          </Field>
        </div>
        {canManage && (
          <div className="actions">
            <Button type="submit" disabled={busy}>
              {t("common.save")}
            </Button>
          </div>
        )}
      </form>
    </Card>
  );
}
