insert into permission (code, description) values
    ('JOBS_VIEW', 'See jobs'),
    ('JOBS_MANAGE', 'Create, edit, publish and close jobs'),
    ('CANDIDATES_VIEW', 'See candidates and download resumes'),
    ('CANDIDATES_MANAGE', 'Create and edit candidates, record consent, link candidates to jobs');

insert into role_permission (role_id, permission_code) values
    ('a0000000-0000-0000-0000-000000000001', 'JOBS_VIEW'),
    ('a0000000-0000-0000-0000-000000000001', 'JOBS_MANAGE'),
    ('a0000000-0000-0000-0000-000000000001', 'CANDIDATES_VIEW'),
    ('a0000000-0000-0000-0000-000000000001', 'CANDIDATES_MANAGE'),
    ('a0000000-0000-0000-0000-000000000002', 'JOBS_VIEW'),
    ('a0000000-0000-0000-0000-000000000002', 'JOBS_MANAGE'),
    ('a0000000-0000-0000-0000-000000000002', 'CANDIDATES_VIEW'),
    ('a0000000-0000-0000-0000-000000000002', 'CANDIDATES_MANAGE'),
    ('a0000000-0000-0000-0000-000000000003', 'JOBS_VIEW'),
    ('a0000000-0000-0000-0000-000000000003', 'CANDIDATES_VIEW'),
    ('a0000000-0000-0000-0000-000000000003', 'CANDIDATES_MANAGE'),
    ('a0000000-0000-0000-0000-000000000004', 'JOBS_VIEW'),
    ('a0000000-0000-0000-0000-000000000004', 'JOBS_MANAGE');

create sequence job_code_seq start with 1 increment by 1;

create table job (
    id uuid primary key,
    code varchar(20) not null,
    client_id uuid not null references client (id),
    title varchar(200) not null,
    description text,
    location varchar(150),
    skills text,
    min_experience_years numeric(4, 1),
    max_experience_years numeric(4, 1),
    min_ctc numeric(12, 2),
    max_ctc numeric(12, 2),
    openings integer not null default 1,
    status varchar(20) not null default 'DRAFT',
    published boolean not null default false,
    closing_date date,
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    created_by varchar(255),
    updated_at timestamptz not null default now(),
    updated_by varchar(255),
    constraint job_status_chk check (status in ('DRAFT', 'OPEN', 'ON_HOLD', 'CLOSED')),
    constraint job_openings_chk check (openings between 1 and 1000),
    constraint job_experience_chk check (min_experience_years is null or max_experience_years is null
        or min_experience_years <= max_experience_years),
    constraint job_ctc_chk check (min_ctc is null or max_ctc is null or min_ctc <= max_ctc)
);

create unique index uq_job_code on job (code);
create index idx_job_client on job (client_id);
create index idx_job_status on job (status);

create table candidate (
    id uuid primary key,
    full_name varchar(150) not null,
    email varchar(255) not null,
    phone varchar(30),
    current_company varchar(200),
    current_title varchar(200),
    total_experience_years numeric(4, 1),
    current_ctc numeric(12, 2),
    expected_ctc numeric(12, 2),
    notice_period_days integer,
    current_location varchar(150),
    preferred_location varchar(150),
    skills text,
    source varchar(20) not null,
    status varchar(20) not null default 'NEW',
    notes text,
    resume_path varchar(500),
    resume_file_name varchar(255),
    resume_content_type varchar(100),
    resume_size bigint,
    consent_status varchar(20) not null default 'NOT_ASKED',
    consent_method varchar(20),
    consent_recorded_at timestamptz,
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    created_by varchar(255),
    updated_at timestamptz not null default now(),
    updated_by varchar(255),
    constraint candidate_source_chk check (source in ('LINKEDIN', 'NAUKRI', 'APPLY_LINK', 'REFERRAL', 'OTHER')),
    constraint candidate_status_chk check (status in ('NEW', 'SCREENING', 'AVAILABLE', 'NOT_INTERESTED', 'PLACED', 'BLOCKED')),
    constraint candidate_consent_chk check (consent_status in ('NOT_ASKED', 'GRANTED', 'DENIED', 'WITHDRAWN')),
    constraint candidate_notice_chk check (notice_period_days is null or notice_period_days between 0 and 365)
);

create unique index uq_candidate_email on candidate (lower(email));
create index idx_candidate_phone on candidate (phone);
create index idx_candidate_status on candidate (status);
create index idx_candidate_consent on candidate (consent_status);

create table candidate_consent_event (
    id uuid primary key,
    candidate_id uuid not null references candidate (id) on delete cascade,
    status varchar(20) not null,
    method varchar(20) not null,
    note varchar(500),
    recorded_by varchar(255) not null,
    recorded_at timestamptz not null default now(),
    constraint consent_event_status_chk check (status in ('GRANTED', 'DENIED', 'WITHDRAWN')),
    constraint consent_event_method_chk check (method in ('CALL', 'EMAIL', 'APPLY_FORM'))
);

create index idx_consent_event_candidate on candidate_consent_event (candidate_id, recorded_at desc);

create table job_application (
    id uuid primary key,
    job_id uuid not null references job (id),
    candidate_id uuid not null references candidate (id) on delete cascade,
    source varchar(20) not null,
    applied_at timestamptz not null default now(),
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    created_by varchar(255),
    updated_at timestamptz not null default now(),
    updated_by varchar(255),
    constraint job_application_source_chk check (source in ('LINKEDIN', 'NAUKRI', 'APPLY_LINK', 'REFERRAL', 'OTHER'))
);

create unique index uq_job_application on job_application (job_id, candidate_id);
create index idx_job_application_candidate on job_application (candidate_id);
