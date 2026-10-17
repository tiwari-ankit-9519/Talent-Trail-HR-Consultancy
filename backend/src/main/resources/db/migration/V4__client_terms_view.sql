insert into permission (code, description) values
    ('CLIENTS_TERMS_VIEW', 'See a client''s fee, GST, TDS and payment terms');

insert into role_permission (role_id, permission_code) values
    ('a0000000-0000-0000-0000-000000000001', 'CLIENTS_TERMS_VIEW'),
    ('a0000000-0000-0000-0000-000000000004', 'CLIENTS_TERMS_VIEW'),
    ('a0000000-0000-0000-0000-000000000005', 'CLIENTS_TERMS_VIEW');
