alter table submission drop constraint submission_email_chk;
alter table submission add constraint submission_email_chk
    check (email_status in ('PENDING', 'QUEUED', 'SENT', 'FAILED', 'NOT_CONFIGURED'));

alter table interview drop constraint interview_invite_chk;
alter table interview add constraint interview_invite_chk
    check (invite_status in ('PENDING', 'QUEUED', 'SENT', 'FAILED', 'NOT_CONFIGURED', 'SKIPPED'));

alter table invoice drop constraint invoice_email_chk;
alter table invoice add constraint invoice_email_chk
    check (email_status in ('PENDING', 'QUEUED', 'SENT', 'FAILED', 'NOT_CONFIGURED', 'SKIPPED'));

alter table placement drop constraint placement_offer_email_chk;
alter table placement add constraint placement_offer_email_chk
    check (offer_email_status in ('PENDING', 'QUEUED', 'SENT', 'FAILED', 'NOT_CONFIGURED', 'SKIPPED'));
