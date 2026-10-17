create table client (
    id uuid primary key,
    name varchar(200) not null,
    legal_name varchar(250),
    industry varchar(100),
    website varchar(255),
    gstin varchar(15),
    pan varchar(10),
    address_line1 varchar(200),
    address_line2 varchar(200),
    city varchar(100),
    state varchar(100),
    postal_code varchar(12),
    country varchar(100) not null default 'India',
    status varchar(20) not null default 'ACTIVE',
    notes text,
    fee_type varchar(30) not null default 'MONTHS_OF_SALARY',
    fee_value numeric(10, 2) not null default 1.00,
    gst_applicable boolean not null default true,
    gst_rate numeric(5, 2) not null default 18.00,
    tds_applicable boolean not null default false,
    tds_rate numeric(5, 2) not null default 0.00,
    payment_terms_days integer not null default 30,
    replacement_period_days integer not null default 90,
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    created_by varchar(255),
    updated_at timestamptz not null default now(),
    updated_by varchar(255),
    constraint client_status_chk check (status in ('ACTIVE', 'INACTIVE')),
    constraint client_fee_type_chk check (fee_type in ('MONTHS_OF_SALARY', 'CTC_PERCENT')),
    constraint client_fee_value_chk check (fee_value > 0),
    constraint client_gst_rate_chk check (gst_rate between 0 and 100),
    constraint client_tds_rate_chk check (tds_rate between 0 and 100),
    constraint client_payment_terms_chk check (payment_terms_days between 0 and 365),
    constraint client_replacement_chk check (replacement_period_days between 0 and 730)
);

create unique index uq_client_name on client (lower(name));
create index idx_client_status on client (status);

create table client_contact (
    id uuid primary key,
    client_id uuid not null references client (id) on delete cascade,
    name varchar(150) not null,
    email varchar(255) not null,
    phone varchar(30),
    designation varchar(100),
    primary_contact boolean not null default false,
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    created_by varchar(255),
    updated_at timestamptz not null default now(),
    updated_by varchar(255)
);

create index idx_client_contact_client on client_contact (client_id);

insert into permission (code, description) values
    ('CLIENTS_VIEW', 'View clients and their contacts'),
    ('CLIENTS_MANAGE', 'Create and edit clients and contacts'),
    ('CLIENTS_TERMS_MANAGE', 'Change a client''s fee, GST, TDS and payment terms');

insert into role_permission (role_id, permission_code)
select 'a0000000-0000-0000-0000-000000000001', code from permission
where code in ('CLIENTS_VIEW', 'CLIENTS_MANAGE', 'CLIENTS_TERMS_MANAGE');

insert into role_permission (role_id, permission_code) values
    ('a0000000-0000-0000-0000-000000000002', 'CLIENTS_VIEW'),
    ('a0000000-0000-0000-0000-000000000003', 'CLIENTS_VIEW'),
    ('a0000000-0000-0000-0000-000000000004', 'CLIENTS_VIEW'),
    ('a0000000-0000-0000-0000-000000000004', 'CLIENTS_MANAGE'),
    ('a0000000-0000-0000-0000-000000000005', 'CLIENTS_VIEW'),
    ('a0000000-0000-0000-0000-000000000005', 'CLIENTS_TERMS_MANAGE');
