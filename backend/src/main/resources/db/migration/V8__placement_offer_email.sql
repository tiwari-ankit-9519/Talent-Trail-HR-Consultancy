alter table placement
    add column offer_email_status varchar(20) not null default 'SKIPPED',
    add column offer_email_sent_at timestamptz,
    add column offer_email_error varchar(500);

alter table placement
    add constraint placement_offer_email_chk
    check (offer_email_status in ('PENDING', 'SENT', 'FAILED', 'NOT_CONFIGURED', 'SKIPPED'));
