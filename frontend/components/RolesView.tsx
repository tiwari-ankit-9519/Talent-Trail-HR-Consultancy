"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import { useAuth } from "@/components/AuthProvider";
import { Empty, ErrorBanner, Field, Loading } from "@/components/ui";
import { api } from "@/lib/api";
import { t } from "@/lib/messages";
import type { PermissionRow, RoleRow } from "@/lib/types";
import { confirmDialog } from "@/lib/confirm";
import { Button } from "@/components/shadcn/button";
import { CheckboxField } from "@/components/shadcn/checkbox";
import { Input } from "@/components/shadcn/input";
import { Badge } from "@/components/shadcn/badge";
import { Card } from "@/components/shadcn/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";

type Draft = {
  id: string | null;
  name: string;
  description: string;
  permissions: string[];
  systemRole: boolean;
  version: number | null;
};

export default function RolesView() {
  const { can } = useAuth();
  const canManage = can("ROLES_MANAGE");
  const [roles, setRoles] = useState<RoleRow[] | null>(null);
  const [permissions, setPermissions] = useState<PermissionRow[]>([]);
  const [draft, setDraft] = useState<Draft | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      setError(null);
      setRoles(await api<RoleRow[]>("/roles"));
    } catch (failure) {
      setError(failure);
    }
  }, []);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    api<PermissionRow[]>("/permissions").then(setPermissions).catch(setError);
  }, []);

  function open(role: RoleRow | null) {
    setDraft(
      role
        ? {
            id: role.id,
            name: role.name,
            description: role.description ?? "",
            permissions: role.permissions,
            systemRole: role.systemRole,
            version: role.version,
          }
        : { id: null, name: "", description: "", permissions: [], systemRole: false, version: null },
    );
  }

  function toggle(code: string) {
    if (!draft) return;
    const has = draft.permissions.includes(code);
    setDraft({
      ...draft,
      permissions: has ? draft.permissions.filter((p) => p !== code) : [...draft.permissions, code],
    });
  }

  async function save(event: FormEvent) {
    event.preventDefault();
    if (!draft) return;
    setBusy(true);
    setError(null);
    try {
      const body = {
        name: draft.name,
        description: draft.description,
        permissions: draft.permissions,
        version: draft.version,
      };
      if (draft.id) await api(`/roles/${draft.id}`, { method: "PUT", body });
      else await api("/roles", { method: "POST", body });
      setDraft(null);
      await load();
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  async function remove(role: RoleRow) {
    if (!(await confirmDialog(t("roles.confirmDelete")))) return;
    setError(null);
    try {
      await api(`/roles/${role.id}`, { method: "DELETE" });
      await load();
    } catch (failure) {
      setError(failure);
    }
  }

  return (
    <section>
      <div className="page-head">
        <h1>{t("roles.heading")}</h1>
        {canManage && (
          <Button type="button" onClick={() => open(null)}>
            {t("roles.add")}
          </Button>
        )}
      </div>
      <ErrorBanner error={error} />

      {draft && (
        <Card asChild>
          <form className="form-card" onSubmit={(e) => void save(e)}>
            <h2>{draft.id ? t("roles.edit") : t("roles.add")}</h2>
            <Field label={t("roles.name")}>
              <Input
                required
                disabled={draft.systemRole}
                value={draft.name}
                onChange={(e) => setDraft({ ...draft, name: e.target.value })}
              />
            </Field>
            <Field label={t("roles.description")}>
              <Input value={draft.description} onChange={(e) => setDraft({ ...draft, description: e.target.value })} />
            </Field>
            <fieldset className="checks">
              <legend>{t("roles.permissions")}</legend>
              {permissions.map((permission) => (
                <label key={permission.code} className="check">
                  <CheckboxField
                    checked={draft.permissions.includes(permission.code)}
                    onChange={() => toggle(permission.code)}
                  />
                  <span>
                    {permission.code}
                    <span className="muted small"> — {permission.description}</span>
                  </span>
                </label>
              ))}
            </fieldset>
            <div className="actions">
              <Button type="submit" disabled={busy}>
                {t("common.save")}
              </Button>
              <Button variant="outline" type="button" onClick={() => setDraft(null)}>
                {t("common.cancel")}
              </Button>
            </div>
          </form>
        </Card>
      )}

      {!roles ? (
        <Loading />
      ) : roles.length === 0 ? (
        <Empty />
      ) : (
        <div className="table-wrap">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("roles.name")}</TableHead>
                <TableHead>{t("roles.description")}</TableHead>
                <TableHead>{t("roles.permissions")}</TableHead>
                <TableHead>{t("roles.users")}</TableHead>
                {canManage && <TableHead>{t("common.actions")}</TableHead>}
              </TableRow>
            </TableHeader>
            <TableBody>
              {roles.map((role) => (
                <TableRow key={role.id}>
                  <TableCell>
                    {role.name}
                    {role.systemRole && <Badge>{t("roles.system")}</Badge>}
                  </TableCell>
                  <TableCell>{role.description}</TableCell>
                  <TableCell className="small">{role.permissions.join(", ")}</TableCell>
                  <TableCell>{role.userCount}</TableCell>
                  {canManage && (
                    <TableCell className="row-actions">
                      <Button variant="outline" type="button" onClick={() => open(role)}>
                        {t("common.edit")}
                      </Button>
                      {!role.systemRole && (
                        <Button variant="outline" type="button" onClick={() => void remove(role)}>
                          {t("common.delete")}
                        </Button>
                      )}
                    </TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
    </section>
  );
}
