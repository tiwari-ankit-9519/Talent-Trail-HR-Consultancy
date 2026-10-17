"use client";

import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { useAuth } from "@/components/AuthProvider";
import ClientContacts from "@/components/ClientContacts";
import ClientProfileForm from "@/components/ClientProfileForm";
import ClientTerms from "@/components/ClientTerms";
import { ErrorBanner, Loading, StatusBadge } from "@/components/ui";
import { api } from "@/lib/api";
import { t } from "@/lib/messages";
import type { ClientDetail } from "@/lib/types";
import { Button } from "@/components/shadcn/button";
import { Alert } from "@/components/shadcn/alert";
import { Tabs, TabsList, TabsTrigger } from "@/components/shadcn/tabs";
import { useBreadcrumbLabel } from "@/lib/breadcrumb";

type Tab = "profile" | "contacts" | "terms";

export default function ClientDetailView({ id }: { id: string }) {
  const { can } = useAuth();
  const router = useRouter();
  const isNew = id === "new";
  const [client, setClient] = useState<ClientDetail | null>(null);
  useBreadcrumbLabel(client?.name);
  const [tab, setTab] = useState<Tab>("profile");
  const [error, setError] = useState<unknown>(null);
  const [formKey, setFormKey] = useState(0);

  const load = useCallback(async () => {
    if (isNew) return;
    try {
      setError(null);
      setClient(await api<ClientDetail>(`/clients/${id}`));
      setFormKey((k) => k + 1);
    } catch (failure) {
      setError(failure);
    }
  }, [id, isNew]);

  useEffect(() => {
    void load();
  }, [load]);

  async function toggleStatus() {
    if (!client) return;
    setError(null);
    try {
      const next = client.status === "ACTIVE" ? "INACTIVE" : "ACTIVE";
      setClient(await api<ClientDetail>(`/clients/${client.id}/status`, { method: "PATCH", body: { status: next } }));
      setFormKey((k) => k + 1);
    } catch (failure) {
      setError(failure);
    }
  }

  const canManage = can("CLIENTS_MANAGE");

  if (isNew) {
    return (
      <section>
        <h1>{t("clients.add")}</h1>
        {canManage ? (
          <ClientProfileForm client={null} canManage onSaved={(created) => router.replace(`/clients/${created.id}`)} />
        ) : (
          <Alert variant="destructive">{t("errors.FORBIDDEN")}</Alert>
        )}
      </section>
    );
  }

  if (!client) {
    return (
      <section>
        <ErrorBanner error={error} />
        {!error && <Loading />}
      </section>
    );
  }

  return (
    <section>
      <div className="page-head">
        <h1>
          {client.name} <StatusBadge status={client.status} />
        </h1>
        {canManage && (
          <Button variant="outline" type="button" onClick={() => void toggleStatus()}>
            {client.status === "ACTIVE" ? t("clients.deactivate") : t("clients.activate")}
          </Button>
        )}
      </div>
      <ErrorBanner error={error} />
      <Tabs value={tab} onValueChange={(value) => setTab(value as typeof tab)}>
        <TabsList>
          <TabsTrigger value="profile">{t("clients.tabProfile")}</TabsTrigger>
          <TabsTrigger value="contacts">{t("clients.tabContacts")}</TabsTrigger>
          {client.terms && <TabsTrigger value="terms">{t("clients.tabTerms")}</TabsTrigger>}
        </TabsList>
      </Tabs>
      {tab === "profile" && (
        <ClientProfileForm key={formKey} client={client} canManage={canManage} onSaved={setClient} />
      )}
      {tab === "contacts" && (
        <ClientContacts
          clientId={client.id}
          contacts={client.contacts}
          canManage={canManage}
          onChanged={() => void load()}
        />
      )}
      {tab === "terms" && client.terms && (
        <ClientTerms
          key={formKey}
          client={client}
          terms={client.terms}
          canManage={can("CLIENTS_TERMS_MANAGE")}
          onSaved={(updated) => {
            setClient(updated);
            setFormKey((k) => k + 1);
          }}
        />
      )}
    </section>
  );
}
