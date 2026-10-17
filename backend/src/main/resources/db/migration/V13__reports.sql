insert into permission (code, description) values
    ('REPORTS_VIEW', 'See business reports');

insert into role_permission (role_id, permission_code) values
    ('a0000000-0000-0000-0000-000000000001', 'REPORTS_VIEW'),
    ('a0000000-0000-0000-0000-000000000002', 'REPORTS_VIEW'),
    ('a0000000-0000-0000-0000-000000000004', 'REPORTS_VIEW'),
    ('a0000000-0000-0000-0000-000000000005', 'REPORTS_VIEW');
