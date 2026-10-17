insert into permission (code, description) values
    ('PLACEMENTS_VIEW', 'See offers, joinings and joining proof'),
    ('PLACEMENTS_MANAGE', 'Record offers, joinings and attach the joining email');

insert into role_permission (role_id, permission_code) values
    ('a0000000-0000-0000-0000-000000000001', 'PLACEMENTS_VIEW'),
    ('a0000000-0000-0000-0000-000000000001', 'PLACEMENTS_MANAGE'),
    ('a0000000-0000-0000-0000-000000000002', 'PLACEMENTS_VIEW'),
    ('a0000000-0000-0000-0000-000000000002', 'PLACEMENTS_MANAGE'),
    ('a0000000-0000-0000-0000-000000000003', 'PLACEMENTS_VIEW'),
    ('a0000000-0000-0000-0000-000000000003', 'PLACEMENTS_MANAGE'),
    ('a0000000-0000-0000-0000-000000000004', 'PLACEMENTS_VIEW'),
    ('a0000000-0000-0000-0000-000000000004', 'PLACEMENTS_MANAGE'),
    ('a0000000-0000-0000-0000-000000000005', 'PLACEMENTS_VIEW');

create table placement (
    id uuid primary key,
    submission_id uuid not null references submission (id),
    job_id uuid not null references job (id),
    candidate_id uuid not null references candidate (id),
    client_id uuid not null references client (id),
    status varchar(20) not null default 'OFFERED',
    offered_ctc numeric(12, 2) not null,
    offer_date date not null,
    expected_joining_date date not null,
    accepted_on date,
    close_reason varchar(500),
    joining_date date,
    joining_email_path varchar(500),
    joining_email_file_name varchar(255),
    joining_email_content_type varchar(100),
    joining_email_size bigint,
    notes text,
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    created_by varchar(255),
    updated_at timestamptz not null default now(),
    updated_by varchar(255),
    constraint placement_status_chk check (status in
        ('OFFERED', 'OFFER_ACCEPTED', 'OFFER_DECLINED', 'JOINED', 'NOT_JOINED')),
    constraint placement_ctc_chk check (offered_ctc > 0),
    constraint placement_dates_chk check (expected_joining_date >= offer_date),
    constraint placement_joined_chk check (status <> 'JOINED' or joining_date is not null)
);

create unique index uq_placement_submission on placement (submission_id);
create index idx_placement_status on placement (status);
create index idx_placement_client on placement (client_id);
create index idx_placement_candidate on placement (candidate_id);
create index idx_placement_joining on placement (joining_date);
