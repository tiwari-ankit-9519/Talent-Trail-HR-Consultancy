"use client";

import LeaveApplicationsPanel from "@/components/attendance/LeaveApplicationsPanel";
import LeaveRequestsPanel from "@/components/attendance/LeaveRequestsPanel";
import { t } from "@/lib/messages";

export default function LeaveApprovalsView() {
  return (
    <section>
      <div className="page-head">
        <h1>{t("attendance.leaveApprovals.heading")}</h1>
      </div>
      <p className="muted">{t("attendance.leaveApprovals.help")}</p>
      <h2 className="mt-4">{t("attendance.leaveApprovals.applications")}</h2>
      <LeaveApplicationsPanel />
      <h2 className="mt-8">{t("attendance.leaveApprovals.holidays")}</h2>
      <LeaveRequestsPanel />
    </section>
  );
}
