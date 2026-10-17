"use client";

import { useState, type FormEvent } from "react";
import { Alert } from "@/components/shadcn/alert";
import { Button } from "@/components/shadcn/button";
import { Card } from "@/components/shadcn/card";
import { DateField } from "@/components/shadcn/date-picker";
import { Input } from "@/components/shadcn/input";
import { FormSelect } from "@/components/shadcn/select";
import { ErrorBanner, Field } from "@/components/ui";
import { api } from "@/lib/api";
import { t } from "@/lib/messages";
import type { EmploymentType, ProfileManagerOption, ProfileView } from "@/lib/types";

type Props = {
  profile: ProfileView;
  managers: ProfileManagerOption[];
  self: boolean;
  onSaved: (profile: ProfileView) => void;
};

type Form = {
  employeeCode: string;
  employmentType: EmploymentType;
  designation: string;
  department: string;
  joiningDate: string;
  managerUserId: string;
  phone: string;
  dateOfBirth: string;
  addressLine: string;
  city: string;
  state: string;
  pincode: string;
  emergencyName: string;
  emergencyPhone: string;
  emergencyRelation: string;
};

const types: EmploymentType[] = ["FULL_TIME", "PART_TIME", "CONTRACT", "INTERN"];

function toForm(p: ProfileView): Form {
  return {
    employeeCode: p.employeeCode ?? "",
    employmentType: p.employmentType,
    designation: p.designation ?? "",
    department: p.department ?? "",
    joiningDate: p.joiningDate ?? "",
    managerUserId: p.managerUserId ?? "",
    phone: p.phone ?? "",
    dateOfBirth: p.dateOfBirth ?? "",
    addressLine: p.addressLine ?? "",
    city: p.city ?? "",
    state: p.state ?? "",
    pincode: p.pincode ?? "",
    emergencyName: p.emergencyName ?? "",
    emergencyPhone: p.emergencyPhone ?? "",
    emergencyRelation: p.emergencyRelation ?? "",
  };
}

function orNull(value: string): string | null {
  const trimmed = value.trim();
  return trimmed === "" ? null : trimmed;
}

export default function ProfileForm({ profile, managers, self, onSaved }: Props) {
  const [form, setForm] = useState<Form>(() => toForm(profile));
  const [error, setError] = useState<unknown>(null);
  const [saved, setSaved] = useState(false);
  const [busy, setBusy] = useState(false);

  const full = profile.canEdit;
  const contact = full || (self && profile.canEditContact);
  const set = (patch: Partial<Form>) => setForm((current) => ({ ...current, ...patch }));

  async function save(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setSaved(false);
    try {
      const contactBody = {
        phone: orNull(form.phone),
        addressLine: orNull(form.addressLine),
        city: orNull(form.city),
        state: orNull(form.state),
        pincode: orNull(form.pincode),
        emergencyName: orNull(form.emergencyName),
        emergencyPhone: orNull(form.emergencyPhone),
        emergencyRelation: orNull(form.emergencyRelation),
        version: profile.version,
      };
      const next = full
        ? await api<ProfileView>(`/profiles/${profile.userId}`, {
            method: "PUT",
            body: {
              ...contactBody,
              employeeCode: orNull(form.employeeCode),
              employmentType: form.employmentType,
              designation: orNull(form.designation),
              department: orNull(form.department),
              joiningDate: orNull(form.joiningDate),
              managerUserId: orNull(form.managerUserId),
              dateOfBirth: orNull(form.dateOfBirth),
            },
          })
        : await api<ProfileView>("/profiles/me/contact", { method: "PUT", body: contactBody });
      onSaved(next);
      setForm(toForm(next));
      setSaved(true);
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="grid gap-4" onSubmit={(event) => void save(event)}>
      <Card className="grid gap-4 p-5">
        <h2 className="m-0 text-base font-semibold">{t("profile.sections.employment")}</h2>
        <div className="grid gap-3 sm:grid-cols-2">
          <Field label={t("profile.fields.employeeCode")}>
            <Input
              disabled={!full}
              maxLength={30}
              value={form.employeeCode}
              onChange={(e) => set({ employeeCode: e.target.value })}
            />
          </Field>
          <Field label={t("profile.fields.employmentType")}>
            <FormSelect
              disabled={!full}
              value={form.employmentType}
              onChange={(e) => set({ employmentType: e.target.value as EmploymentType })}
            >
              {types.map((type) => (
                <option key={type} value={type}>
                  {t(`profile.employmentTypes.${type}`)}
                </option>
              ))}
            </FormSelect>
          </Field>
          <Field label={t("profile.fields.designation")}>
            <Input
              disabled={!full}
              maxLength={100}
              value={form.designation}
              onChange={(e) => set({ designation: e.target.value })}
            />
          </Field>
          <Field label={t("profile.fields.department")}>
            <Input
              disabled={!full}
              maxLength={100}
              value={form.department}
              onChange={(e) => set({ department: e.target.value })}
            />
          </Field>
          <Field label={t("profile.fields.joiningDate")}>
            <DateField
              disabled={!full}
              value={form.joiningDate}
              onChange={(e) => set({ joiningDate: e.target.value })}
            />
          </Field>
          <Field label={t("profile.fields.manager")}>
            {full ? (
              <FormSelect value={form.managerUserId} onChange={(e) => set({ managerUserId: e.target.value })}>
                <option value="">{t("profile.fields.noManager")}</option>
                {managers
                  .filter((manager) => manager.id !== profile.userId)
                  .map((manager) => (
                    <option key={manager.id} value={manager.id}>
                      {manager.fullName}
                    </option>
                  ))}
              </FormSelect>
            ) : (
              <Input disabled value={profile.managerName ?? t("profile.fields.noManager")} readOnly />
            )}
          </Field>
        </div>
      </Card>

      <Card className="grid gap-4 p-5">
        <h2 className="m-0 text-base font-semibold">{t("profile.sections.personal")}</h2>
        <div className="grid gap-3 sm:grid-cols-2">
          <Field label={t("profile.fields.dateOfBirth")}>
            <DateField
              disabled={!full}
              value={form.dateOfBirth}
              max={new Date().toISOString().slice(0, 10)}
              onChange={(e) => set({ dateOfBirth: e.target.value })}
            />
          </Field>
          <Field label={t("profile.fields.phone")}>
            <Input
              disabled={!contact}
              maxLength={20}
              inputMode="tel"
              value={form.phone}
              onChange={(e) => set({ phone: e.target.value })}
            />
          </Field>
        </div>
        <h3 className="m-0 text-sm font-semibold">{t("profile.sections.address")}</h3>
        <div className="grid gap-3 sm:grid-cols-2">
          <div className="sm:col-span-2">
            <Field label={t("profile.fields.addressLine")}>
              <Input
                disabled={!contact}
                maxLength={255}
                value={form.addressLine}
                onChange={(e) => set({ addressLine: e.target.value })}
              />
            </Field>
          </div>
          <Field label={t("profile.fields.city")}>
            <Input
              disabled={!contact}
              maxLength={100}
              value={form.city}
              onChange={(e) => set({ city: e.target.value })}
            />
          </Field>
          <Field label={t("profile.fields.state")}>
            <Input
              disabled={!contact}
              maxLength={100}
              value={form.state}
              onChange={(e) => set({ state: e.target.value })}
            />
          </Field>
          <Field label={t("profile.fields.pincode")}>
            <Input
              disabled={!contact}
              maxLength={6}
              inputMode="numeric"
              value={form.pincode}
              onChange={(e) => set({ pincode: e.target.value })}
            />
          </Field>
        </div>
        <h3 className="m-0 text-sm font-semibold">{t("profile.sections.emergency")}</h3>
        <div className="grid gap-3 sm:grid-cols-3">
          <Field label={t("profile.fields.emergencyName")}>
            <Input
              disabled={!contact}
              maxLength={150}
              value={form.emergencyName}
              onChange={(e) => set({ emergencyName: e.target.value })}
            />
          </Field>
          <Field label={t("profile.fields.emergencyPhone")}>
            <Input
              disabled={!contact}
              maxLength={20}
              inputMode="tel"
              value={form.emergencyPhone}
              onChange={(e) => set({ emergencyPhone: e.target.value })}
            />
          </Field>
          <Field label={t("profile.fields.emergencyRelation")}>
            <Input
              disabled={!contact}
              maxLength={60}
              value={form.emergencyRelation}
              onChange={(e) => set({ emergencyRelation: e.target.value })}
            />
          </Field>
        </div>
        {!full && contact && <p className="muted m-0 text-sm">{t("profile.contactOnlyHint")}</p>}
        <ErrorBanner error={error} />
        {saved && <Alert variant="success">{t("common.saved")}</Alert>}
        {contact && (
          <div>
            <Button type="submit" disabled={busy}>
              {t("common.save")}
            </Button>
          </div>
        )}
      </Card>
    </form>
  );
}
