alter table holiday add column kind varchar(10) not null default 'NATIONAL';
alter table holiday add constraint holiday_kind_chk check (kind in ('NATIONAL', 'OPTIONAL'));

alter table attendance_settings add column optional_quota integer not null default 2;
alter table attendance_settings add column comp_off_valid_days integer not null default 60;
alter table attendance_settings add constraint attendance_settings_leave_chk
    check (optional_quota >= 0 and comp_off_valid_days >= 1);
alter table attendance_settings alter column weekly_off set default 'SATURDAY,SUNDAY';
update attendance_settings set weekly_off = 'SATURDAY,SUNDAY' where weekly_off = 'SUNDAY';

create table comp_off (
    id uuid primary key,
    user_id uuid not null references app_user (id),
    earned_date date not null,
    expires_on date not null,
    status varchar(10) not null default 'AVAILABLE',
    used_on date,
    source varchar(10) not null,
    note varchar(300),
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    created_by varchar(255),
    updated_at timestamptz not null default now(),
    updated_by varchar(255),
    constraint comp_off_status_chk check (status in ('AVAILABLE', 'USED')),
    constraint comp_off_source_chk check (source in ('AUTO', 'HR'))
);

create unique index uq_comp_off_user_date on comp_off (user_id, earned_date);

create table leave_entry (
    id uuid primary key,
    user_id uuid not null references app_user (id),
    leave_date date not null,
    kind varchar(20) not null,
    holiday_id uuid references holiday (id),
    comp_off_id uuid references comp_off (id),
    status varchar(10) not null default 'APPROVED',
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    created_by varchar(255),
    updated_at timestamptz not null default now(),
    updated_by varchar(255),
    constraint leave_entry_kind_chk check (kind in ('OPTIONAL_HOLIDAY', 'COMP_OFF')),
    constraint leave_entry_status_chk check (status in ('APPROVED', 'CANCELLED'))
);

create unique index uq_leave_entry_active on leave_entry (user_id, leave_date) where status = 'APPROVED';
create index idx_leave_entry_date on leave_entry (leave_date);
