"use client";

import LeaveRequestsPanel from "@/components/attendance/LeaveRequestsPanel";
import { t } from "@/lib/messages";

export default function LeaveApprovalsView() {
  return (
    <section>
      <div className="page-head">
        <h1>{t("attendance.leaveApprovals.heading")}</h1>
      </div>
      <p className="muted">{t("attendance.leaveApprovals.help")}</p>
      <LeaveRequestsPanel />
    </section>
  );
}
