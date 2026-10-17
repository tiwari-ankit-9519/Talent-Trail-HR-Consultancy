insert into
    permission (code, description)
values (
        'INVOICES_VIEW',
        'See invoices and payments'
    ),
    (
        'INVOICES_MANAGE',
        'Raise, send and cancel invoices and record payments'
    ),
    (
        'SETTINGS_MANAGE',
        'Edit the company profile used on invoices'
    );

insert into
    role_permission (role_id, permission_code)
values (
        'a0000000-0000-0000-0000-000000000001',
        'INVOICES_VIEW'
    ),
    (
        'a0000000-0000-0000-0000-000000000001',
        'INVOICES_MANAGE'
    ),
    (
        'a0000000-0000-0000-0000-000000000001',
        'SETTINGS_MANAGE'
    ),
    (
        'a0000000-0000-0000-0000-000000000002',
        'INVOICES_VIEW'
    ),
    (
        'a0000000-0000-0000-0000-000000000004',
        'INVOICES_VIEW'
    ),
    (
        'a0000000-0000-0000-0000-000000000005',
        'INVOICES_VIEW'
    ),
    (
        'a0000000-0000-0000-0000-000000000005',
        'INVOICES_MANAGE'
    ),
    (
        'a0000000-0000-0000-0000-000000000005',
        'SETTINGS_MANAGE'
    );

create table company_profile (
    id smallint primary key,
    legal_name varchar(250) not null,
    address_line1 varchar(200),
    address_line2 varchar(200),
    city varchar(100),
    state varchar(100),
    postal_code varchar(12),
    gstin varchar(15),
    pan varchar(10),
    email varchar(255),
    phone varchar(30),
    sac_code varchar(10) not null default '998512',
    invoice_prefix varchar(10) not null default 'ABC',
    bank_account_name varchar(200),
    bank_name varchar(200),
    bank_account_number varchar(40),
    bank_ifsc varchar(15),
    bank_branch varchar(200),
    invoice_footer text,
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    created_by varchar(255),
    updated_at timestamptz not null default now(),
    updated_by varchar(255),
    constraint company_profile_singleton_chk check (id = 1)
);

insert into
    company_profile (id, legal_name)
values (
        1,
        'Talent Trail HR Solutions'
    );

create table invoice_counter (
    fy varchar(9) primary key,
    last_number integer not null
);

create table invoice (
    id uuid primary key,
    number varchar(40) not null,
    placement_id uuid not null references placement (id),
    client_id uuid not null references client (id),
    job_id uuid not null references job (id),
    candidate_id uuid not null references candidate (id),
    status varchar(20) not null default 'ISSUED',
    invoice_date date not null,
    due_date date not null,
    joining_date date not null,
    description varchar(600) not null,
    candidate_name varchar(150) not null,
    job_code varchar(20) not null,
    job_title varchar(200) not null,
    seller_name varchar(250) not null,
    seller_gstin varchar(15),
    seller_pan varchar(10),
    seller_address text,
    seller_state varchar(100),
    seller_bank text,
    sac_code varchar(10) not null,
    buyer_name varchar(250) not null,
    buyer_gstin varchar(15),
    buyer_address text,
    buyer_state varchar(100),
    offered_ctc numeric(12, 2) not null,
    fee_type varchar(30) not null,
    fee_value numeric(10, 2) not null,
    base_amount numeric(12, 2) not null,
    gst_rate numeric(5, 2) not null,
    cgst_amount numeric(12, 2) not null default 0,
    sgst_amount numeric(12, 2) not null default 0,
    igst_amount numeric(12, 2) not null default 0,
    total_amount numeric(12, 2) not null,
    tds_rate numeric(5, 2) not null,
    tds_amount numeric(12, 2) not null default 0,
    net_receivable numeric(12, 2) not null,
    settled_amount numeric(12, 2) not null default 0,
    cancel_reason varchar(500),
    cancelled_at timestamptz,
    email_status varchar(20) not null default 'PENDING',
    email_sent_at timestamptz,
    email_error varchar(500),
    sent_to text,
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    created_by varchar(255),
    updated_at timestamptz not null default now(),
    updated_by varchar(255),
    constraint invoice_status_chk check (
        status in (
            'ISSUED',
            'PARTIALLY_PAID',
            'PAID',
            'CANCELLED'
        )
    ),
    constraint invoice_email_chk check (
        email_status in (
            'PENDING',
            'SENT',
            'FAILED',
            'NOT_CONFIGURED',
            'SKIPPED'
        )
    ),
    constraint invoice_amounts_chk check (
        base_amount > 0
        and total_amount >= base_amount
        and settled_amount >= 0
    ),
    constraint invoice_due_chk check (due_date >= invoice_date)
);

create unique index uq_invoice_number on invoice (number);

create unique index uq_invoice_active_placement on invoice (placement_id)
where
    status <> 'CANCELLED';

create index idx_invoice_client on invoice (client_id);

create index idx_invoice_status on invoice (status);

create index idx_invoice_due on invoice (due_date);

create table invoice_payment (
    id uuid primary key,
    invoice_id uuid not null references invoice (id),
    paid_on date not null,
    amount_received numeric(12, 2) not null,
    tds_deducted numeric(12, 2) not null default 0,
    mode varchar(20) not null,
    reference varchar(100),
    note varchar(500),
    version bigint not null default 0,
    created_at timestamptz not null default now(),
    created_by varchar(255),
    updated_at timestamptz not null default now(),
    updated_by varchar(255),
    constraint payment_mode_chk check (
        mode in (
            'BANK_TRANSFER',
            'UPI',
            'CHEQUE',
            'CASH',
            'OTHER'
        )
    ),
    constraint payment_amount_chk check (
        amount_received >= 0
        and tds_deducted >= 0
        and amount_received + tds_deducted > 0
    )
);

create index idx_payment_invoice on invoice_payment (invoice_id, paid_on);