insert into permission (code, description) values
    ('PROFILES_VIEW', 'View employee profiles and documents'),
    ('PROFILES_MANAGE', 'Edit employee profiles and manage their documents'),
    ('PROFILES_SENSITIVE', 'View and edit bank and tax details'),
    ('PROFILE_SELF', 'View own profile and edit own contact details');

insert into role_permission (role_id, permission_code) values
    ('a0000000-0000-0000-0000-000000000001', 'PROFILES_VIEW'),
    ('a0000000-0000-0000-0000-000000000006', 'PROFILES_VIEW'),
    ('a0000000-0000-0000-0000-000000000005', 'PROFILES_VIEW'),
    ('a0000000-0000-0000-0000-000000000001', 'PROFILES_MANAGE'),
    ('a0000000-0000-0000-0000-000000000006', 'PROFILES_MANAGE'),
    ('a0000000-0000-0000-0000-000000000001', 'PROFILES_SENSITIVE'),
    ('a0000000-0000-0000-0000-000000000006', 'PROFILES_SENSITIVE'),
    ('a0000000-0000-0000-0000-000000000005', 'PROFILES_SENSITIVE'),
    ('a0000000-0000-0000-0000-000000000001', 'PROFILE_SELF'),
    ('a0000000-0000-0000-0000-000000000002', 'PROFILE_SELF'),
    ('a0000000-0000-0000-0000-000000000003', 'PROFILE_SELF'),
    ('a0000000-0000-0000-0000-000000000004', 'PROFILE_SELF'),
    ('a0000000-0000-0000-0000-000000000005', 'PROFILE_SELF'),
    ('a0000000-0000-0000-0000-000000000006', 'PROFILE_SELF');

create table employee_profile (
    user_id uuid primary key references app_user (id),
    employee_code varchar(30),
    employment_type varchar(20) not null default 'FULL_TIME',
    designation varchar(100),
    department varchar(100),
    joining_date date,
    manager_user_id uuid references app_user (id),
    phone varchar(20),
    date_of_birth date,
    address_line varchar(255),
    city varchar(100),
    state varchar(100),
    pincode varchar(10),
    emergency_name varchar(150),
    emergency_phone varchar(20),
    emergency_relation varchar(60),
    bank_account_holder varchar(150),
    bank_name varchar(100),
    bank_ifsc varchar(11),
    bank_account_enc text,
    pan_enc text,
    uan_enc text,
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    created_by varchar(255),
    updated_at timestamptz not null default now(),
    updated_by varchar(255),
    constraint employee_profile_type_chk check (employment_type in ('FULL_TIME', 'PART_TIME', 'CONTRACT', 'INTERN')),
    constraint employee_profile_manager_chk check (manager_user_id is null or manager_user_id <> user_id)
);

create unique index uq_employee_profile_code on employee_profile (lower(employee_code)) where employee_code is not null;
create index idx_employee_profile_manager on employee_profile (manager_user_id);

create table employee_document (
    id uuid primary key,
    user_id uuid not null references app_user (id),
    doc_type varchar(30) not null,
    title varchar(150) not null,
    file_path varchar(400) not null,
    file_name varchar(200) not null,
    content_type varchar(100) not null,
    file_size bigint not null,
    expires_on date,
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    created_by varchar(255),
    updated_at timestamptz not null default now(),
    updated_by varchar(255),
    constraint employee_document_type_chk check (
        doc_type in ('ID_PROOF', 'ADDRESS_PROOF', 'OFFER_LETTER', 'EDUCATION', 'EXPERIENCE', 'BANK_PROOF', 'OTHER'))
);

create index idx_employee_document_user on employee_document (user_id);
