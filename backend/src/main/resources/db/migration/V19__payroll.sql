insert into permission (code, description) values
    ('PAYROLL_MANAGE', 'Manage salary structures and run payroll'),
    ('PAYROLL_SELF', 'View own payslips');

insert into role_permission (role_id, permission_code) values
    ('a0000000-0000-0000-0000-000000000001', 'PAYROLL_MANAGE'),
    ('a0000000-0000-0000-0000-000000000005', 'PAYROLL_MANAGE'),
    ('a0000000-0000-0000-0000-000000000006', 'PAYROLL_MANAGE'),
    ('a0000000-0000-0000-0000-000000000001', 'PAYROLL_SELF'),
    ('a0000000-0000-0000-0000-000000000002', 'PAYROLL_SELF'),
    ('a0000000-0000-0000-0000-000000000003', 'PAYROLL_SELF'),
    ('a0000000-0000-0000-0000-000000000004', 'PAYROLL_SELF'),
    ('a0000000-0000-0000-0000-000000000005', 'PAYROLL_SELF'),
    ('a0000000-0000-0000-0000-000000000006', 'PAYROLL_SELF');

create table payroll_settings (
    id smallint primary key,
    pf_enabled boolean not null default true,
    pf_rate numeric(5, 2) not null default 12,
    pf_wage_ceiling numeric(12, 2) not null default 15000,
    pt_amount numeric(12, 2) not null default 200,
    pt_min_gross numeric(12, 2) not null default 0,
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    created_by varchar(255),
    updated_at timestamptz not null default now(),
    updated_by varchar(255),
    constraint payroll_settings_single check (id = 1),
    constraint payroll_settings_chk check (
        pf_rate >= 0 and pf_rate <= 100 and pf_wage_ceiling >= 0 and pt_amount >= 0 and pt_min_gross >= 0)
);

insert into payroll_settings (id) values (1);

create table salary_structure (
    id uuid primary key,
    user_id uuid not null references app_user (id),
    effective_from date not null,
    gross numeric(12, 2) not null,
    basic numeric(12, 2),
    hra numeric(12, 2),
    pf_applicable boolean not null default true,
    pt_applicable boolean not null default true,
    note varchar(200),
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    created_by varchar(255),
    updated_at timestamptz not null default now(),
    updated_by varchar(255),
    constraint salary_structure_chk check (
        gross >= 0 and (basic is null or basic >= 0) and (hra is null or hra >= 0)
        and coalesce(basic, 0) + coalesce(hra, 0) <= gross)
);

create unique index uq_salary_structure on salary_structure (user_id, effective_from);

create table payroll_run (
    id uuid primary key,
    month varchar(7) not null,
    status varchar(10) not null default 'DRAFT',
    finalized_by varchar(255),
    finalized_at timestamptz,
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    created_by varchar(255),
    updated_at timestamptz not null default now(),
    updated_by varchar(255),
    constraint payroll_run_status_chk check (status in ('DRAFT', 'FINALIZED'))
);

create unique index uq_payroll_run_month on payroll_run (month);

create table payslip (
    id uuid primary key,
    run_id uuid not null references payroll_run (id) on delete cascade,
    user_id uuid not null references app_user (id),
    employee_name varchar(150) not null,
    employee_email varchar(255) not null,
    calendar_days integer not null,
    payable_days numeric(5, 1) not null,
    leave_days numeric(5, 1) not null default 0,
    loss_of_pay_days numeric(5, 1) not null default 0,
    gross numeric(12, 2) not null,
    earned_gross numeric(12, 2) not null,
    basic numeric(12, 2),
    hra numeric(12, 2),
    allowance numeric(12, 2),
    commission numeric(12, 2) not null default 0,
    other_earnings numeric(12, 2) not null default 0,
    earnings_note varchar(200),
    pf numeric(12, 2) not null default 0,
    pt numeric(12, 2) not null default 0,
    tds numeric(12, 2) not null default 0,
    other_deductions numeric(12, 2) not null default 0,
    deductions_note varchar(200),
    net numeric(12, 2) not null,
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    created_by varchar(255),
    updated_at timestamptz not null default now(),
    updated_by varchar(255),
    constraint payslip_amounts_chk check (
        other_earnings >= 0 and tds >= 0 and other_deductions >= 0 and commission >= 0)
);

create unique index uq_payslip_run_user on payslip (run_id, user_id);
create index idx_payslip_user on payslip (user_id);

create table payslip_commission (
    payslip_id uuid not null references payslip (id) on delete cascade,
    commission_id uuid not null,
    amount numeric(12, 2) not null,
    label varchar(200) not null,
    primary key (payslip_id, commission_id)
);
