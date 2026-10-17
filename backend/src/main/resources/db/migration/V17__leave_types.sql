create table leave_type (
    id uuid primary key,
    name varchar(60) not null,
    yearly_days numeric(5, 1) not null default 0,
    unpaid boolean not null default false,
    active boolean not null default true,
    sort_order integer not null default 0,
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    created_by varchar(255),
    updated_at timestamptz not null default now(),
    updated_by varchar(255),
    constraint leave_type_days_chk check (yearly_days >= 0)
);

create unique index uq_leave_type_name on leave_type (lower(name));

insert into leave_type (id, name, yearly_days, unpaid, sort_order) values
    ('b0000000-0000-0000-0000-000000000001', 'Casual leave', 12, false, 1),
    ('b0000000-0000-0000-0000-000000000002', 'Sick leave', 8, false, 2),
    ('b0000000-0000-0000-0000-000000000003', 'Paid leave', 15, false, 3),
    ('b0000000-0000-0000-0000-000000000004', 'Loss of pay', 0, true, 4);

create table leave_application (
    id uuid primary key,
    user_id uuid not null references app_user (id),
    leave_type_id uuid not null references leave_type (id),
    type_name varchar(60) not null,
    unpaid boolean not null default false,
    from_date date not null,
    to_date date not null,
    half_day varchar(10),
    days numeric(5, 1) not null,
    reason varchar(500) not null,
    status varchar(10) not null default 'PENDING',
    decided_by varchar(255),
    decided_at timestamptz,
    decision_note varchar(300),
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    created_by varchar(255),
    updated_at timestamptz not null default now(),
    updated_by varchar(255),
    constraint leave_application_status_chk check (status in ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    constraint leave_application_half_chk check (half_day is null or half_day in ('FIRST', 'SECOND')),
    constraint leave_application_range_chk check (to_date >= from_date)
);

create index idx_leave_application_user on leave_application (user_id, from_date);
create index idx_leave_application_status on leave_application (status);
