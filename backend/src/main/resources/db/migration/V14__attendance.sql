insert into permission (code, description) values
    ('ATTENDANCE_SELF', 'Mark own attendance and raise correction requests'),
    ('ATTENDANCE_MANAGE', 'See everyone''s attendance, mark it, approve corrections and manage holidays and rules');

insert into role_permission (role_id, permission_code) values
    ('a0000000-0000-0000-0000-000000000001', 'ATTENDANCE_SELF'),
    ('a0000000-0000-0000-0000-000000000002', 'ATTENDANCE_SELF'),
    ('a0000000-0000-0000-0000-000000000003', 'ATTENDANCE_SELF'),
    ('a0000000-0000-0000-0000-000000000004', 'ATTENDANCE_SELF'),
    ('a0000000-0000-0000-0000-000000000005', 'ATTENDANCE_SELF'),
    ('a0000000-0000-0000-0000-000000000006', 'ATTENDANCE_SELF'),
    ('a0000000-0000-0000-0000-000000000001', 'ATTENDANCE_MANAGE'),
    ('a0000000-0000-0000-0000-000000000006', 'ATTENDANCE_MANAGE');

create table attendance_settings (
    id smallint primary key,
    shift_start time not null default '10:00',
    late_grace_minutes integer not null default 15,
    half_day_minutes integer not null default 240,
    full_day_minutes integer not null default 480,
    weekly_off varchar(80) not null default 'SUNDAY',
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    created_by varchar(255),
    updated_at timestamptz not null default now(),
    updated_by varchar(255),
    constraint attendance_settings_single check (id = 1),
    constraint attendance_settings_minutes_chk check (
        late_grace_minutes >= 0 and half_day_minutes > 0 and full_day_minutes > half_day_minutes)
);

insert into attendance_settings (id) values (1);

create table holiday (
    id uuid primary key,
    holiday_date date not null,
    name varchar(150) not null,
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    created_by varchar(255),
    updated_at timestamptz not null default now(),
    updated_by varchar(255)
);

create unique index uq_holiday_date on holiday (holiday_date);

create table attendance (
    id uuid primary key,
    user_id uuid not null references app_user (id),
    work_date date not null,
    check_in timestamptz,
    check_out timestamptz,
    work_mode varchar(10) not null default 'OFFICE',
    status varchar(10) not null,
    late boolean not null default false,
    worked_minutes integer,
    source varchar(10) not null,
    note varchar(300),
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    created_by varchar(255),
    updated_at timestamptz not null default now(),
    updated_by varchar(255),
    constraint attendance_status_chk check (status in ('PRESENT', 'HALF_DAY', 'ABSENT')),
    constraint attendance_mode_chk check (work_mode in ('OFFICE', 'WFH')),
    constraint attendance_source_chk check (source in ('SELF', 'HR', 'REQUEST'))
);

create unique index uq_attendance_user_date on attendance (user_id, work_date);
create index idx_attendance_date on attendance (work_date);

create table attendance_request (
    id uuid primary key,
    user_id uuid not null references app_user (id),
    work_date date not null,
    check_in time not null,
    check_out time,
    work_mode varchar(10) not null default 'OFFICE',
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
    constraint attendance_request_status_chk check (status in ('PENDING', 'APPROVED', 'REJECTED')),
    constraint attendance_request_mode_chk check (work_mode in ('OFFICE', 'WFH'))
);

create index idx_attendance_request_user on attendance_request (user_id, work_date);
create index idx_attendance_request_status on attendance_request (status);
