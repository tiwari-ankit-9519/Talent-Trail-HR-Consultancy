"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { Empty, ErrorBanner, Loading } from "@/components/ui";
import { publicApi } from "@/lib/api";
import { t } from "@/lib/messages";
import type { PublicJob } from "@/lib/types";
import { Card } from "@/components/shadcn/card";

export function experienceRange(job: PublicJob): string {
  if (job.minExperienceYears === null && job.maxExperienceYears === null) return "";
  if (job.minExperienceYears !== null && job.maxExperienceYears !== null) {
    return `${job.minExperienceYears}-${job.maxExperienceYears} ${t("apply.years")}`;
  }
  if (job.minExperienceYears !== null) return `${job.minExperienceYears}+ ${t("apply.years")}`;
  return `${t("apply.upTo")} ${job.maxExperienceYears} ${t("apply.years")}`;
}

export default function PublicJobsView() {
  const [jobs, setJobs] = useState<PublicJob[] | null>(null);
  const [error, setError] = useState<unknown>(null);

  useEffect(() => {
    publicApi<PublicJob[]>("/jobs").then(setJobs).catch(setError);
  }, []);

  return (
    <section>
      <h1>{t("apply.openRoles")}</h1>
      <ErrorBanner error={error} />
      {!jobs && !error && <Loading />}
      {jobs && jobs.length === 0 && <Empty />}
      {jobs && jobs.length > 0 && (
        <div className="job-list">
          {jobs.map((job) => (
            <Card key={job.code} asChild>
              <Link href={`/apply/${job.code}`} className="job-card">
                <h2>{job.title}</h2>
                <p className="muted">{[job.location, experienceRange(job)].filter(Boolean).join(" · ")}</p>
                {job.skills && <p className="small">{job.skills}</p>}
              </Link>
            </Card>
          ))}
        </div>
      )}
    </section>
  );
}
