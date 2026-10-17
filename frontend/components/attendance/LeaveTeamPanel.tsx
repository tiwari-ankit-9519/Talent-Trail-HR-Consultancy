"use client";

import { useEffect, useState, type FormEvent } from "react";
import { Alert } from "@/components/shadcn/alert";
import { Button } from "@/components/shadcn/button";
import { Card } from "@/components/shadcn/card";
import { Input } from "@/components/shadcn/input";
import { FormSelect } from "@/components/shadcn/select";
import LeaveRequestsPanel from "@/components/attendance/LeaveRequestsPanel";
import { ErrorBanner, Field } from "@/components/ui";
import { api } from "@/lib/api";
import { todayIso } from "@/lib/attendance";
import { t } from "@/lib/messages";
import type { AttendanceTeamDay } from "@/lib/types";
import { DateField } from "@/components/shadcn/date-picker";

export default function LeaveTeamPanel() {
  const [people, setPeople] = useState<{ id: string; name: string }[]>([]);
  const [userId, setUserId] = useState("");
  const [earnedDate, setEarnedDate] = useState(todayIso());
  const [note, setNote] = useState("");
  const [granted, setGranted] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    api<AttendanceTeamDay>("/attendance/team")
      .then((day) => setPeople(day.rows.map((row) => ({ id: row.userId, name: row.name }))))
      .catch(() => setPeople([]));
  }, []);

  async function grant(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    setGranted(false);
    try {
      await api<void>("/attendance/leave/comp-off/grant", {
        method: "POST",
        body: { userId, earnedDate, note: note || null },
      });
      setGranted(true);
      setNote("");
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="grid gap-4">
      <ErrorBanner error={error} />
      <Card className="p-5">
        <form onSubmit={grant} className="grid gap-4">
          <div>
            <h2 className="m-0 text-lg">{t("attendance.leaveTeam.grantHeading")}</h2>
            <p className="muted small m-0 mt-1">{t("attendance.leaveTeam.grantHelp")}</p>
          </div>
          {granted && (
            <Alert variant="success">
              <p>{t("attendance.leaveTeam.granted")}</p>
            </Alert>
          )}
          <div className="form-grid">
            <Field label={t("attendance.team.name")}>
              <FormSelect required value={userId} onChange={(e) => setUserId(e.target.value)}>
                <option value="">{t("attendance.leaveTeam.pickPerson")}</option>
                {people.map((person) => (
                  <option key={person.id} value={person.id}>
                    {person.name}
                  </option>
                ))}
              </FormSelect>
            </Field>
            <Field label={t("attendance.leaveTeam.earnedDate")}>
              <DateField required max={todayIso()} value={earnedDate} onChange={(e) => setEarnedDate(e.target.value)} />
            </Field>
            <Field label={t("attendance.today.note")}>
              <Input maxLength={300} value={note} onChange={(e) => setNote(e.target.value)} />
            </Field>
          </div>
          <div>
            <Button type="submit" disabled={busy || !userId}>
              {t("attendance.leaveTeam.grant")}
            </Button>
          </div>
        </form>
      </Card>

      <LeaveRequestsPanel />
    </div>
  );
}
