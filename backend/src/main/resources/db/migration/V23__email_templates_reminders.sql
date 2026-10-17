insert into permission (code, description) values
    ('EMAIL_TEMPLATES_MANAGE', 'Edit email templates and automatic reminders');

insert into role_permission (role_id, permission_code) values
    ('a0000000-0000-0000-0000-000000000001', 'EMAIL_TEMPLATES_MANAGE');

create table email_template (
    code varchar(40) primary key,
    subject varchar(200) not null,
    body text not null,
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    created_by varchar(255),
    updated_at timestamptz not null default now(),
    updated_by varchar(255)
);

create table reminder_setting (
    code varchar(40) primary key,
    enabled boolean not null default false,
    offsets varchar(120) not null,
    extra_recipients varchar(600) not null default '',
    updated_at timestamptz not null default now(),
    updated_by varchar(255)
);

insert into reminder_setting (code, enabled, offsets) values
    ('INVOICE_DUE', false, '-3,0,7,14,30'),
    ('REPLACEMENT_ENDING', false, '-14,-7,-1'),
    ('CONSENT_PENDING', false, '2,5,10'),
    ('DOCUMENT_EXPIRY', false, '-30,-7,0');

create table reminder_log (
    id uuid primary key,
    code varchar(40) not null,
    entity_id uuid not null,
    offset_days integer not null,
    recipients varchar(800) not null,
    subject varchar(300) not null,
    status varchar(20) not null,
    error varchar(500),
    sent_at timestamptz not null default now(),
    constraint reminder_log_status_chk check (status in ('SENDING', 'SENT'))
);

create unique index uq_reminder_log_once on reminder_log (code, entity_id, offset_days);
create index idx_reminder_log_sent on reminder_log (sent_at desc);
