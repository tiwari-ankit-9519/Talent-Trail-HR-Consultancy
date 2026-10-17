alter table payroll_settings
    add column basic_percent numeric(5, 2) not null default 50,
    add column hra_percent numeric(5, 2) not null default 50,
    add column employer_pf_rate numeric(5, 2) not null default 12,
    add column gratuity_enabled boolean not null default true,
    add column gratuity_percent numeric(5, 2) not null default 4.81;

alter table payroll_settings add constraint payroll_settings_ctc_chk check (
    basic_percent > 0 and basic_percent <= 100 and hra_percent >= 0 and hra_percent <= 100
    and employer_pf_rate >= 0 and employer_pf_rate <= 100 and gratuity_percent >= 0 and gratuity_percent <= 100);

alter table salary_structure
    add column ctc_annual numeric(14, 2),
    add column variable_percent numeric(5, 2),
    add column employer_pf numeric(12, 2),
    add column gratuity numeric(12, 2);
