create table notification (
    id uuid primary key,
    user_id uuid not null references app_user (id) on delete cascade,
    topic varchar(30) not null,
    title varchar(200) not null,
    body varchar(500),
    link varchar(200),
    read_at timestamptz,
    created_at timestamptz not null default now()
);

create index idx_notification_user on notification (user_id, created_at desc);
create index idx_notification_unread on notification (user_id) where read_at is null;
