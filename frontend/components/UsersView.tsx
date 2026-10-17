"use client";

import { useCallback, useEffect, useState, type FormEvent } from "react";
import { useAuth } from "@/components/AuthProvider";
import { Empty, ErrorBanner, Field, Loading, Pager, StatusBadge } from "@/components/ui";
import { api } from "@/lib/api";
import { formatDate, t } from "@/lib/messages";
import type { Page, RoleRow, UserRow } from "@/lib/types";
import { Button } from "@/components/shadcn/button";
import { FormSelect } from "@/components/shadcn/select";
import { CheckboxField } from "@/components/shadcn/checkbox";
import { Input } from "@/components/shadcn/input";
import { Badge } from "@/components/shadcn/badge";
import { Card } from "@/components/shadcn/card";
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/shadcn/table";

type Draft = {
  id: string | null;
  email: string;
  fullName: string;
  password: string;
  roleIds: string[];
  version: number | null;
};

const emptyDraft: Draft = { id: null, email: "", fullName: "", password: "", roleIds: [], version: null };

export default function UsersView() {
  const { can } = useAuth();
  const canManage = can("USERS_MANAGE");
  const [query, setQuery] = useState("");
  const [status, setStatus] = useState("");
  const [pageNumber, setPageNumber] = useState(0);
  const [page, setPage] = useState<Page<UserRow> | null>(null);
  const [roles, setRoles] = useState<RoleRow[]>([]);
  const [error, setError] = useState<unknown>(null);
  const [draft, setDraft] = useState<Draft | null>(null);
  const [resetFor, setResetFor] = useState<UserRow | null>(null);
  const [resetPassword, setResetPassword] = useState("");
  const [busy, setBusy] = useState(false);

  const load = useCallback(async () => {
    try {
      setError(null);
      setPage(
        await api<Page<UserRow>>("/users", {
          query: { q: query, status, page: pageNumber, size: 25 },
        }),
      );
    } catch (failure) {
      setError(failure);
    }
  }, [query, status, pageNumber]);

  useEffect(() => {
    void load();
  }, [load]);

  useEffect(() => {
    api<RoleRow[]>("/roles").then(setRoles).catch(setError);
  }, []);

  function edit(user: UserRow) {
    setDraft({
      id: user.id,
      email: user.email,
      fullName: user.fullName,
      password: "",
      roleIds: user.roles.map((r) => r.id),
      version: user.version,
    });
  }

  function toggleRole(id: string) {
    if (!draft) return;
    const has = draft.roleIds.includes(id);
    setDraft({
      ...draft,
      roleIds: has ? draft.roleIds.filter((r) => r !== id) : [...draft.roleIds, id],
    });
  }

  async function save(event: FormEvent) {
    event.preventDefault();
    if (!draft) return;
    setBusy(true);
    setError(null);
    try {
      if (draft.id) {
        await api(`/users/${draft.id}`, {
          method: "PUT",
          body: {
            email: draft.email,
            fullName: draft.fullName,
            roleIds: draft.roleIds,
            version: draft.version,
          },
        });
      } else {
        await api("/users", {
          method: "POST",
          body: {
            email: draft.email,
            fullName: draft.fullName,
            password: draft.password,
            roleIds: draft.roleIds,
          },
        });
      }
      setDraft(null);
      await load();
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  async function toggleStatus(user: UserRow) {
    setError(null);
    try {
      await api(`/users/${user.id}/status`, {
        method: "PATCH",
        body: { active: user.status !== "ACTIVE" },
      });
      await load();
    } catch (failure) {
      setError(failure);
    }
  }

  async function submitReset(event: FormEvent) {
    event.preventDefault();
    if (!resetFor) return;
    setBusy(true);
    setError(null);
    try {
      await api(`/users/${resetFor.id}/reset-password`, {
        method: "POST",
        body: { password: resetPassword },
      });
      setResetFor(null);
      setResetPassword("");
      await load();
    } catch (failure) {
      setError(failure);
    } finally {
      setBusy(false);
    }
  }

  return (
    <section>
      <div className="page-head">
        <h1>{t("users.heading")}</h1>
        {canManage && (
          <Button type="button" onClick={() => setDraft({ ...emptyDraft })}>
            {t("users.add")}
          </Button>
        )}
      </div>
      <ErrorBanner error={error} />

      <div className="toolbar">
        <Input
          type="search"
          placeholder={t("users.search")}
          value={query}
          onChange={(e) => {
            setPageNumber(0);
            setQuery(e.target.value);
          }}
        />
        <FormSelect
          value={status}
          onChange={(e) => {
            setPageNumber(0);
            setStatus(e.target.value);
          }}
        >
          <option value="">{t("common.allStatuses")}</option>
          <option value="ACTIVE">{t("common.active")}</option>
          <option value="INACTIVE">{t("common.inactive")}</option>
        </FormSelect>
      </div>

      {draft && (
        <Card asChild>
          <form className="form-card" onSubmit={(e) => void save(e)}>
            <h2>{draft.id ? t("users.edit") : t("users.add")}</h2>
            <Field label={t("users.fullName")}>
              <Input
                required
                value={draft.fullName}
                onChange={(e) => setDraft({ ...draft, fullName: e.target.value })}
              />
            </Field>
            <Field label={t("users.email")}>
              <Input
                type="email"
                required
                value={draft.email}
                onChange={(e) => setDraft({ ...draft, email: e.target.value })}
              />
            </Field>
            {!draft.id && (
              <Field label={t("users.initialPassword")} hint={t("users.initialPasswordHint")}>
                <Input
                  type="password"
                  autoComplete="new-password"
                  required
                  value={draft.password}
                  onChange={(e) => setDraft({ ...draft, password: e.target.value })}
                />
              </Field>
            )}
            <fieldset className="checks">
              <legend>{t("users.roles")}</legend>
              {roles.map((role) => (
                <label key={role.id} className="check">
                  <CheckboxField checked={draft.roleIds.includes(role.id)} onChange={() => toggleRole(role.id)} />
                  {role.name}
                </label>
              ))}
            </fieldset>
            <div className="actions">
              <Button type="submit" disabled={busy || draft.roleIds.length === 0}>
                {t("common.save")}
              </Button>
              <Button variant="outline" type="button" onClick={() => setDraft(null)}>
                {t("common.cancel")}
              </Button>
            </div>
          </form>
        </Card>
      )}

      {resetFor && (
        <Card asChild>
          <form className="form-card" onSubmit={(e) => void submitReset(e)}>
            <h2>
              {t("users.resetPassword")}: {resetFor.email}
            </h2>
            <p className="muted small">{t("users.resetHint")}</p>
            <Field label={t("users.newPassword")}>
              <Input
                type="password"
                autoComplete="new-password"
                required
                value={resetPassword}
                onChange={(e) => setResetPassword(e.target.value)}
              />
            </Field>
            <div className="actions">
              <Button type="submit" disabled={busy}>
                {t("users.resetSubmit")}
              </Button>
              <Button variant="outline" type="button" onClick={() => setResetFor(null)}>
                {t("common.cancel")}
              </Button>
            </div>
          </form>
        </Card>
      )}

      {!page ? (
        <Loading />
      ) : page.items.length === 0 ? (
        <Empty />
      ) : (
        <div className="table-wrap">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>{t("users.fullName")}</TableHead>
                <TableHead>{t("users.email")}</TableHead>
                <TableHead>{t("users.roles")}</TableHead>
                <TableHead>{t("common.status")}</TableHead>
                <TableHead>{t("users.lastLogin")}</TableHead>
                {canManage && <TableHead>{t("common.actions")}</TableHead>}
              </TableRow>
            </TableHeader>
            <TableBody>
              {page.items.map((user) => (
                <TableRow key={user.id}>
                  <TableCell>{user.fullName}</TableCell>
                  <TableCell>{user.email}</TableCell>
                  <TableCell>{user.roles.map((r) => r.name).join(", ")}</TableCell>
                  <TableCell>
                    <StatusBadge status={user.status} />
                    {user.locked && <Badge variant="warn">{t("users.locked")}</Badge>}
                  </TableCell>
                  <TableCell>{formatDate(user.lastLoginAt)}</TableCell>
                  {canManage && (
                    <TableCell className="row-actions">
                      <Button variant="outline" type="button" onClick={() => edit(user)}>
                        {t("common.edit")}
                      </Button>
                      <Button variant="outline" type="button" onClick={() => void toggleStatus(user)}>
                        {user.status === "ACTIVE" ? t("users.deactivate") : t("users.activate")}
                      </Button>
                      <Button variant="outline" type="button" onClick={() => setResetFor(user)}>
                        {t("users.resetPassword")}
                      </Button>
                    </TableCell>
                  )}
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </div>
      )}
      <Pager page={page} onChange={setPageNumber} />
    </section>
  );
}
