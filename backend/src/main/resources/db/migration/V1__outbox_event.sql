create table outbox_event (
    id uuid primary key,
    aggregate_type varchar(100) not null,
    aggregate_id varchar(100) not null,
    event_type varchar(150) not null,
    payload text not null,
    status varchar(20) not null default 'PENDING',
    attempts integer not null default 0,
    last_error text,
    created_at timestamptz not null default now(),
    published_at timestamptz,
    constraint outbox_event_status_chk check (status in ('PENDING', 'PUBLISHED', 'FAILED'))
);

create index idx_outbox_event_pending on outbox_event (created_at) where status = 'PENDING';
