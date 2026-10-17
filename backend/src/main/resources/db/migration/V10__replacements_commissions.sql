insert into permission (code, description) values
    ('REPLACEMENTS_VIEW', 'See candidate exits and replacements'),
    ('REPLACEMENTS_MANAGE', 'Record candidate exits and manage replacements'),
    ('COMMISSIONS_VIEW', 'See all recruiter commissions'),
    ('COMMISSIONS_VIEW_OWN', 'See own recruiter commissions'),
    ('COMMISSIONS_MANAGE', 'Set commission amounts and mark them paid');

insert into role_permission (role_id, permission_code) values
    ('a0000000-0000-0000-0000-000000000001', 'REPLACEMENTS_VIEW'),
    ('a0000000-0000-0000-0000-000000000001', 'REPLACEMENTS_MANAGE'),
    ('a0000000-0000-0000-0000-000000000001', 'COMMISSIONS_VIEW'),
    ('a0000000-0000-0000-0000-000000000001', 'COMMISSIONS_MANAGE'),
    ('a0000000-0000-0000-0000-000000000002', 'REPLACEMENTS_VIEW'),
    ('a0000000-0000-0000-0000-000000000002', 'REPLACEMENTS_MANAGE'),
    ('a0000000-0000-0000-0000-000000000002', 'COMMISSIONS_VIEW'),
    ('a0000000-0000-0000-0000-000000000003', 'REPLACEMENTS_VIEW'),
    ('a0000000-0000-0000-0000-000000000003', 'COMMISSIONS_VIEW_OWN'),
    ('a0000000-0000-0000-0000-000000000004', 'REPLACEMENTS_VIEW'),
    ('a0000000-0000-0000-0000-000000000004', 'REPLACEMENTS_MANAGE'),
    ('a0000000-0000-0000-0000-000000000005', 'REPLACEMENTS_VIEW'),
    ('a0000000-0000-0000-0000-000000000005', 'COMMISSIONS_VIEW'),
    ('a0000000-0000-0000-0000-000000000005', 'COMMISSIONS_MANAGE');

create table replacement (
    id uuid primary key,
    placement_id uuid not null references placement (id),
    client_id uuid not null references client (id),
    job_id uuid not null references job (id),
    candidate_id uuid not null references candidate (id),
    joining_date date not null,
    exit_date date not null,
    exit_reason varchar(500) not null,
    period_days integer not null,
    free_until date not null,
    within_period boolean not null,
    status varchar(20) not null,
    replacement_placement_id uuid references placement (id),
    close_note varchar(500),
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    created_by varchar(255),
    updated_at timestamptz not null default now(),
    updated_by varchar(255),
    constraint replacement_status_chk check (status in ('OPEN', 'FILLED', 'CLOSED', 'NOT_ELIGIBLE')),
    constraint replacement_exit_chk check (exit_date >= joining_date)
);

create unique index uq_replacement_placement on replacement (placement_id);
create unique index uq_replacement_filled_by on replacement (replacement_placement_id) where replacement_placement_id is not null;
create index idx_replacement_status on replacement (status);
create index idx_replacement_client on replacement (client_id);

create table commission (
    id uuid primary key,
    placement_id uuid not null references placement (id),
    recruiter_user_id uuid references app_user (id),
    recruiter_email varchar(255),
    recruiter_name varchar(200),
    amount numeric(12, 2),
    period_days integer not null,
    payable_after date not null,
    status varchar(20) not null default 'PENDING',
    paid_on date,
    payment_reference varchar(100),
    cancel_reason varchar(500),
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    created_by varchar(255),
    updated_at timestamptz not null default now(),
    updated_by varchar(255),
    constraint commission_status_chk check (status in ('PENDING', 'PAID', 'CANCELLED')),
    constraint commission_amount_chk check (amount is null or amount > 0),
    constraint commission_paid_chk check (status <> 'PAID' or (amount is not null and paid_on is not null))
);

create unique index uq_commission_placement on commission (placement_id);
create index idx_commission_recruiter on commission (recruiter_user_id);
create index idx_commission_status on commission (status);
