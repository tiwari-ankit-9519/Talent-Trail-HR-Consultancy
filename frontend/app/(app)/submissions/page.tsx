import SubmissionsView from "@/components/SubmissionsView";
import { t } from "@/lib/messages";

export default function SubmissionsPage() {
  return (
    <section>
      <h1>{t("submissions.heading")}</h1>
      <SubmissionsView />
    </section>
  );
}
