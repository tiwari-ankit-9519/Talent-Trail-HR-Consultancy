"use client";

import Link from "next/link";
import { ArrowRight, Briefcase, MapPin } from "lucide-react";
import { useEffect, useState } from "react";
import { Badge } from "@/components/shadcn/badge";
import { Card } from "@/components/shadcn/card";
import { Empty, ErrorBanner, Loading } from "@/components/ui";
import { publicApi } from "@/lib/api";
import { t } from "@/lib/messages";
import type { PublicJob } from "@/lib/types";

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
    <section className="grid gap-6">
      <div className="grid gap-3">
        <Badge variant="accent" className="w-fit">
          {t("apply.tagline")}
        </Badge>
        <h1 className="m-0 text-3xl font-bold tracking-tight">{t("apply.openRoles")}</h1>
        <p className="muted m-0">{t("apply.lead")}</p>
      </div>
      <ErrorBanner error={error} />
      {!jobs && !error && <Loading />}
      {jobs && jobs.length === 0 && <Empty />}
      {jobs && jobs.length > 0 && (
        <div className="grid gap-4">
          {jobs.map((job) => {
            const experience = experienceRange(job);
            return (
              <Card key={job.code} asChild>
                <Link
                  href={`/apply/${job.code}`}
                  className="group grid gap-3 p-5 text-fg no-underline transition-all duration-200 hover:-translate-y-0.5 hover:border-accent"
                >
                  <div className="flex items-start justify-between gap-3">
                    <h2 className="m-0 text-lg text-accent">{job.title}</h2>
                    <ArrowRight
                      aria-hidden="true"
                      className="size-5 shrink-0 text-muted transition-transform group-hover:translate-x-1"
                    />
                  </div>
                  <div className="muted flex flex-wrap gap-x-4 gap-y-1 text-sm">
                    {job.location && (
                      <span className="flex items-center gap-1.5">
                        <MapPin aria-hidden="true" className="size-4" />
                        {job.location}
                      </span>
                    )}
                    {experience && (
                      <span className="flex items-center gap-1.5">
                        <Briefcase aria-hidden="true" className="size-4" />
                        {experience}
                      </span>
                    )}
                  </div>
                  {job.skills && (
                    <div className="flex flex-wrap gap-2">
                      {job.skills
                        .split(",")
                        .map((skill) => skill.trim())
                        .filter(Boolean)
                        .map((skill) => (
                          <Badge key={skill} variant="default">
                            {skill}
                          </Badge>
                        ))}
                    </div>
                  )}
                </Link>
              </Card>
            );
          })}
        </div>
      )}
    </section>
  );
}
