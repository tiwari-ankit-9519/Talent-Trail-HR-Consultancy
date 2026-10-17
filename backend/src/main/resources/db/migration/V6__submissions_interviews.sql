insert into permission (code, description) values
    ('SUBMISSIONS_VIEW', 'See submissions to clients, interviews and feedback'),
    ('SUBMISSIONS_MANAGE', 'Submit candidates to clients, schedule interviews and record feedback');

insert into role_permission (role_id, permission_code) values
    ('a0000000-0000-0000-0000-000000000001', 'SUBMISSIONS_VIEW'),
    ('a0000000-0000-0000-0000-000000000001', 'SUBMISSIONS_MANAGE'),
    ('a0000000-0000-0000-0000-000000000002', 'SUBMISSIONS_VIEW'),
    ('a0000000-0000-0000-0000-000000000002', 'SUBMISSIONS_MANAGE'),
    ('a0000000-0000-0000-0000-000000000003', 'SUBMISSIONS_VIEW'),
    ('a0000000-0000-0000-0000-000000000003', 'SUBMISSIONS_MANAGE'),
    ('a0000000-0000-0000-0000-000000000004', 'SUBMISSIONS_VIEW'),
    ('a0000000-0000-0000-0000-000000000004', 'SUBMISSIONS_MANAGE');

create table submission (
    id uuid primary key,
    job_id uuid not null references job (id),
    candidate_id uuid not null references candidate (id),
    client_id uuid not null references client (id),
    status varchar(20) not null default 'SUBMITTED',
    recipients text not null,
    note text,
    email_status varchar(20) not null default 'PENDING',
    email_sent_at timestamptz,
    email_error varchar(500),
    submitted_at timestamptz not null default now(),
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    created_by varchar(255),
    updated_at timestamptz not null default now(),
    updated_by varchar(255),
    constraint submission_status_chk check (status in
        ('SUBMITTED', 'SHORTLISTED', 'INTERVIEWING', 'SELECTED', 'REJECTED', 'ON_HOLD', 'WITHDRAWN')),
    constraint submission_email_chk check (email_status in ('PENDING', 'SENT', 'FAILED', 'NOT_CONFIGURED'))
);

create unique index uq_submission_job_candidate on submission (job_id, candidate_id);
create index idx_submission_candidate on submission (candidate_id);
create index idx_submission_client on submission (client_id);
create index idx_submission_status on submission (status);

create table interview (
    id uuid primary key,
    submission_id uuid not null references submission (id),
    round_number integer not null,
    scheduled_at timestamptz not null,
    duration_minutes integer not null default 60,
    mode varchar(20) not null,
    location varchar(500),
    interviewer_name varchar(150),
    status varchar(20) not null default 'SCHEDULED',
    notes text,
    invite_status varchar(20) not null default 'PENDING',
    invite_sent_at timestamptz,
    invite_error varchar(500),
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    created_by varchar(255),
    updated_at timestamptz not null default now(),
    updated_by varchar(255),
    constraint interview_mode_chk check (mode in ('PHONE', 'VIDEO', 'IN_PERSON')),
    constraint interview_status_chk check (status in ('SCHEDULED', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    constraint interview_invite_chk check (invite_status in ('PENDING', 'SENT', 'FAILED', 'NOT_CONFIGURED', 'SKIPPED')),
    constraint interview_duration_chk check (duration_minutes between 15 and 480)
);

create unique index uq_interview_round on interview (submission_id, round_number);
create index idx_interview_scheduled on interview (scheduled_at);
create index idx_interview_status on interview (status);

create table submission_feedback (
    id uuid primary key,
    submission_id uuid not null references submission (id),
    interview_id uuid references interview (id),
    decision varchar(20) not null,
    comments text,
    received_on date not null,
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    created_by varchar(255),
    updated_at timestamptz not null default now(),
    updated_by varchar(255),
    constraint feedback_decision_chk check (decision in ('SHORTLISTED', 'SELECTED', 'REJECTED', 'ON_HOLD'))
);

create index idx_feedback_submission on submission_feedback (submission_id, received_on desc);
