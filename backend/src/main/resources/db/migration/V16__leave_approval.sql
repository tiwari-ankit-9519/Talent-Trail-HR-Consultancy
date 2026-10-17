insert into permission (code, description) values
    ('ATTENDANCE_APPROVE', 'Approve or reject leave requests');

insert into role_permission (role_id, permission_code) values
    ('a0000000-0000-0000-0000-000000000001', 'ATTENDANCE_APPROVE'),
    ('a0000000-0000-0000-0000-000000000002', 'ATTENDANCE_APPROVE'),
    ('a0000000-0000-0000-0000-000000000006', 'ATTENDANCE_APPROVE');

alter table leave_entry drop constraint leave_entry_status_chk;
alter table leave_entry add constraint leave_entry_status_chk
    check (status in ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED'));

alter table leave_entry add column decided_by varchar(255);
alter table leave_entry add column decided_at timestamptz;
alter table leave_entry add column decision_note varchar(300);

drop index uq_leave_entry_active;
create unique index uq_leave_entry_active on leave_entry (user_id, leave_date)
    where status in ('PENDING', 'APPROVED');
