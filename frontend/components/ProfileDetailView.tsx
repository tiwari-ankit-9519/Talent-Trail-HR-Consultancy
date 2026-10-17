"use client";

import { useCallback, useEffect, useState } from "react";
import Link from "next/link";
import { ArrowLeft } from "lucide-react";
import BankTaxCard from "@/components/profile/BankTaxCard";
import DocumentsPanel from "@/components/profile/DocumentsPanel";
import ProfileForm from "@/components/profile/ProfileForm";
import { Badge } from "@/components/shadcn/badge";
import { Tabs, TabsList, TabsTrigger } from "@/components/shadcn/tabs";
import { ErrorBanner, Loading } from "@/components/ui";
import { api } from "@/lib/api";
import { useBreadcrumbLabel } from "@/lib/breadcrumb";
import { t } from "@/lib/messages";
import type { ProfileManagerOption, ProfileView } from "@/lib/types";

type Tab = "overview" | "bank" | "documents";

export default function ProfileDetailView({ id, self = false }: { id?: string; self?: boolean }) {
  const [profile, setProfile] = useState<ProfileView | null>(null);
  const [managers, setManagers] = useState<ProfileManagerOption[]>([]);
  const [error, setError] = useState<unknown>(null);
  const [tab, setTab] = useState<Tab>("overview");
  const [formKey, setFormKey] = useState(0);

  useBreadcrumbLabel(self ? null : profile?.fullName);

  const load = useCallback(() => {
    api<ProfileView>(self ? "/profiles/me" : `/profiles/${id}`)
      .then((value) => {
        setProfile(value);
        setFormKey((key) => key + 1);
        setError(null);
        if (value.canEdit) {
          api<ProfileManagerOption[]>("/profiles/managers").then(setManagers).catch(setError);
        }
      })
      .catch(setError);
  }, [id, self]);

  useEffect(load, [load]);

  function saved(next: ProfileView) {
    setProfile(next);
    setFormKey((key) => key + 1);
  }

  if (!profile) return error ? <ErrorBanner error={error} /> : <Loading />;

  return (
    <section>
      {!self && (
        <p className="m-0 mb-2">
          <Link href="/people" className="inline-flex items-center gap-1 text-sm">
            <ArrowLeft aria-hidden="true" className="size-4" />
            {t("profile.back")}
          </Link>
        </p>
      )}
      <div className="page-head">
        <div>
          <h1>{self ? t("profile.mineHeading") : profile.fullName}</h1>
          <p className="muted m-0 text-sm">
            {profile.email}
            {profile.roles.length > 0 && ` · ${profile.roles.join(", ")}`}{" "}
            {!profile.active && <Badge variant="bad">{t("common.inactive")}</Badge>}
          </p>
        </div>
      </div>
      <ErrorBanner error={error} />
      <Tabs value={tab} onValueChange={(value) => setTab(value as Tab)}>
        <TabsList>
          <TabsTrigger value="overview">{t("profile.tabs.overview")}</TabsTrigger>
          {profile.sensitive && <TabsTrigger value="bank">{t("profile.tabs.bank")}</TabsTrigger>}
          <TabsTrigger value="documents">{t("profile.tabs.documents")}</TabsTrigger>
        </TabsList>
      </Tabs>
      <div className="mt-4">
        {tab === "overview" && (
          <ProfileForm key={formKey} profile={profile} managers={managers} self={self} onSaved={saved} />
        )}
        {tab === "bank" && profile.sensitive && <BankTaxCard key={formKey} profile={profile} onSaved={saved} />}
        {tab === "documents" && (
          <DocumentsPanel userId={profile.userId} self={self} canManage={profile.canManageDocuments} />
        )}
      </div>
    </section>
  );
}
