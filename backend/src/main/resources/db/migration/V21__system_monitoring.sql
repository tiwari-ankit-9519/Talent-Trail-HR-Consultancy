insert into permission (code, description) values
    ('SYSTEM_VIEW', 'View system health and receive system alerts');

insert into role_permission (role_id, permission_code) values
    ('a0000000-0000-0000-0000-000000000001', 'SYSTEM_VIEW');
