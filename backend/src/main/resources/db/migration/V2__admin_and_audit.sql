create table permission (
    code varchar(60) primary key,
    description varchar(255) not null
);

create table app_role (
    id uuid primary key,
    name varchar(80) not null,
    description varchar(255),
    system_role boolean not null default false,
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    created_by varchar(255),
    updated_at timestamptz not null default now(),
    updated_by varchar(255)
);

create unique index uq_app_role_name on app_role (lower(name));

create table role_permission (
    role_id uuid not null references app_role (id) on delete cascade,
    permission_code varchar(60) not null references permission (code),
    primary key (role_id, permission_code)
);

create table app_user (
    id uuid primary key,
    email varchar(255) not null,
    full_name varchar(150) not null,
    password_hash varchar(100) not null,
    status varchar(20) not null,
    must_change_password boolean not null default true,
    failed_login_attempts integer not null default 0,
    locked_until timestamptz,
    last_login_at timestamptz,
    password_changed_at timestamptz,
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    created_by varchar(255),
    updated_at timestamptz not null default now(),
    updated_by varchar(255),
    constraint app_user_status_chk check (status in ('ACTIVE', 'INACTIVE'))
);

create unique index uq_app_user_email on app_user (lower(email));

create table user_role (
    user_id uuid not null references app_user (id) on delete cascade,
    role_id uuid not null references app_role (id),
    primary key (user_id, role_id)
);

create index idx_user_role_role on user_role (role_id);

create table refresh_token (
    id uuid primary key,
    user_id uuid not null references app_user (id) on delete cascade,
    family_id uuid not null,
    token_hash varchar(64) not null,
    expires_at timestamptz not null,
    revoked_at timestamptz,
    created_at timestamptz not null default now(),
    ip_address varchar(64),
    user_agent varchar(300)
);

create unique index uq_refresh_token_hash on refresh_token (token_hash);
create index idx_refresh_token_user on refresh_token (user_id);
create index idx_refresh_token_family on refresh_token (family_id);

create table audit_log (
    id uuid primary key,
    occurred_at timestamptz not null default now(),
    actor_id uuid,
    actor_email varchar(255),
    action varchar(60) not null,
    entity_type varchar(60) not null,
    entity_id varchar(100),
    summary varchar(500) not null,
    details text,
    outcome varchar(10) not null,
    ip_address varchar(64),
    user_agent varchar(300),
    request_id varchar(64),
    constraint audit_log_outcome_chk check (outcome in ('SUCCESS', 'FAILURE'))
);

create index idx_audit_log_occurred on audit_log (occurred_at desc);
create index idx_audit_log_entity on audit_log (entity_type, entity_id);
create index idx_audit_log_actor on audit_log (actor_id);
create index idx_audit_log_action on audit_log (action);

create function audit_log_block_change() returns trigger as $$
begin
    raise exception 'audit_log is append-only';
end;
$$ language plpgsql;

create trigger audit_log_no_row_change
    before update or delete on audit_log
    for each row execute function audit_log_block_change();

create trigger audit_log_no_truncate
    before truncate on audit_log
    for each statement execute function audit_log_block_change();

insert into permission (code, description) values
    ('USERS_VIEW', 'View users'),
    ('USERS_MANAGE', 'Create and edit users, reset passwords'),
    ('ROLES_VIEW', 'View roles and permissions'),
    ('ROLES_MANAGE', 'Create, edit and delete roles'),
    ('AUDIT_VIEW', 'View the audit log');

insert into app_role (id, name, description, system_role) values
    ('a0000000-0000-0000-0000-000000000001', 'Admin', 'Full access to everything', true),
    ('a0000000-0000-0000-0000-000000000002', 'Recruitment Manager', 'Manages recruiters and hiring', true),
    ('a0000000-0000-0000-0000-000000000003', 'Recruiter', 'Sources and screens candidates', true),
    ('a0000000-0000-0000-0000-000000000004', 'Account Manager', 'Manages client relationships', true),
    ('a0000000-0000-0000-0000-000000000005', 'Finance Executive', 'Handles invoicing and payments', true),
    ('a0000000-0000-0000-0000-000000000006', 'HR Manager', 'Manages internal staff records', true);

insert into role_permission (role_id, permission_code)
select 'a0000000-0000-0000-0000-000000000001', code from permission;

insert into role_permission (role_id, permission_code) values
    ('a0000000-0000-0000-0000-000000000002', 'USERS_VIEW'),
    ('a0000000-0000-0000-0000-000000000006', 'USERS_VIEW');
