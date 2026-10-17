alter table employee_document
    add column review_status varchar(10) not null default 'VERIFIED',
    add column uploaded_by_self boolean not null default false,
    add column reviewed_by varchar(255),
    add column reviewed_at timestamptz,
    add column review_note varchar(300);

alter table employee_document
    add constraint employee_document_review_chk check (review_status in ('PENDING', 'VERIFIED', 'REJECTED'));

create index idx_employee_document_review on employee_document (review_status) where review_status = 'PENDING';
