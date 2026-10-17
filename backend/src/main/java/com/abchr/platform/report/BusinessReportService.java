package com.abchr.platform.report;

import com.abchr.platform.profile.FieldCrypto;
import com.abchr.platform.report.BusinessReportDtos.CommissionReport;
import com.abchr.platform.report.BusinessReportDtos.CommissionRow;
import com.abchr.platform.report.BusinessReportDtos.LeaveCell;
import com.abchr.platform.report.BusinessReportDtos.LeaveReport;
import com.abchr.platform.report.BusinessReportDtos.LeaveRow;
import com.abchr.platform.report.BusinessReportDtos.LeaveTypeColumn;
import com.abchr.platform.report.BusinessReportDtos.PayrollRegister;
import com.abchr.platform.report.BusinessReportDtos.PayrollRow;
import com.abchr.platform.report.BusinessReportDtos.PayrollTotals;
import com.abchr.platform.report.BusinessReportDtos.ReceivableRow;
import com.abchr.platform.report.BusinessReportDtos.ReceivablesReport;
import com.abchr.platform.report.BusinessReportDtos.RecruiterCommission;
import com.abchr.platform.report.BusinessReportDtos.TaxMonth;
import com.abchr.platform.report.BusinessReportDtos.TaxSummary;
import com.abchr.platform.report.ReportDtos.Aging;
import com.abchr.platform.shared.security.Permissions;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BusinessReportService {

    private static final int MAX_ROWS = 5000;
    private static final long MAX_DAYS = 1100;

    private final JdbcTemplate jdbc;
    private final FieldCrypto crypto;
    private final ZoneId zone;

    public BusinessReportService(
            JdbcTemplate jdbc, FieldCrypto crypto, @Value("${app.timezone:Asia/Kolkata}") String zoneId) {
        this.jdbc = jdbc;
        this.crypto = crypto;
        this.zone = ZoneId.of(zoneId);
    }

    @Transactional(readOnly = true)
    public PayrollRegister payrollRegister(String monthParam) {
        String month = month(monthParam);
        List<String> statuses = jdbc.queryForList("select status from payroll_run where month = ?", String.class,
                month);
        if (statuses.isEmpty()) {
            return new PayrollRegister(month, "NONE", false, List.of(), zeroTotals());
        }
        boolean bank = Permissions.hasAny("PROFILES_SENSITIVE");
        List<PayrollRow> rows = jdbc.query(
                "select s.user_id, s.employee_name, s.employee_email, s.payable_days, s.loss_of_pay_days, s.gross, "
                        + "s.earned_gross, s.basic, s.hra, s.allowance, s.commission, s.other_earnings, s.pf, s.pt, "
                        + "s.tds, s.other_deductions, s.net, p.employee_code, p.designation, p.bank_account_holder, "
                        + "p.bank_name, p.bank_ifsc, p.bank_account_enc, p.pan_enc "
                        + "from payslip s join payroll_run r on r.id = s.run_id "
                        + "left join employee_profile p on p.user_id = s.user_id "
                        + "where r.month = ? order by lower(s.employee_name)",
                (rs, n) -> new PayrollRow(
                        rs.getObject("user_id", UUID.class),
                        rs.getString("employee_name"),
                        rs.getString("employee_email"),
                        rs.getString("employee_code"),
                        rs.getString("designation"),
                        rs.getBigDecimal("payable_days"),
                        rs.getBigDecimal("loss_of_pay_days"),
                        rs.getBigDecimal("gross"),
                        rs.getBigDecimal("earned_gross"),
                        rs.getBigDecimal("basic"),
                        rs.getBigDecimal("hra"),
                        rs.getBigDecimal("allowance"),
                        rs.getBigDecimal("commission"),
                        rs.getBigDecimal("other_earnings"),
                        rs.getBigDecimal("pf"),
                        rs.getBigDecimal("pt"),
                        rs.getBigDecimal("tds"),
                        rs.getBigDecimal("other_deductions"),
                        rs.getBigDecimal("net"),
                        bank ? rs.getString("bank_account_holder") : null,
                        bank ? rs.getString("bank_name") : null,
                        bank ? rs.getString("bank_ifsc") : null,
                        bank ? crypto.decrypt(rs.getString("bank_account_enc")) : null,
                        bank ? crypto.decrypt(rs.getString("pan_enc")) : null),
                month);
        PayrollTotals totals = new PayrollTotals(
                add(rows, PayrollRow::earnedGross), add(rows, PayrollRow::commission),
                add(rows, PayrollRow::otherEarnings), add(rows, PayrollRow::pf), add(rows, PayrollRow::pt),
                add(rows, PayrollRow::tds), add(rows, PayrollRow::otherDeductions), add(rows, PayrollRow::net));
        return new PayrollRegister(month, statuses.get(0), bank, rows, totals);
    }

    @Transactional(readOnly = true)
    public LeaveReport leave(Integer yearParam) {
        int year = yearParam == null ? LocalDate.now(zone).getYear() : yearParam;
        if (year < 2000 || year > 2100) {
            throw ApiException.rule(ErrorCodes.REPORT_RANGE_INVALID, "Choose a valid year");
        }
        List<LeaveTypeColumn> types = jdbc.query(
                "select id, name, unpaid, yearly_days from leave_type where active order by sort_order, name",
                (rs, n) -> new LeaveTypeColumn(rs.getObject("id", UUID.class), rs.getString("name"),
                        rs.getBoolean("unpaid"), rs.getBigDecimal("yearly_days").doubleValue()));
        Map<String, double[]> used = new HashMap<>();
        jdbc.query(
                "select user_id, leave_type_id, status, sum(days) as days from leave_application "
                        + "where status in ('APPROVED', 'PENDING') and from_date between ? and ? "
                        + "group by user_id, leave_type_id, status",
                rs -> {
                    String key = rs.getObject("user_id", UUID.class) + "|" + rs.getObject("leave_type_id", UUID.class);
                    double[] pair = used.computeIfAbsent(key, k -> new double[2]);
                    pair["APPROVED".equals(rs.getString("status")) ? 0 : 1] += rs.getBigDecimal("days").doubleValue();
                },
                Date.valueOf(LocalDate.of(year, 1, 1)), Date.valueOf(LocalDate.of(year, 12, 31)));
        List<LeaveRow> rows = jdbc.query(
                "select id, full_name, email from app_user where status = 'ACTIVE' order by lower(full_name)",
                (rs, n) -> {
                    UUID userId = rs.getObject("id", UUID.class);
                    List<LeaveCell> cells = new ArrayList<>();
                    double approved = 0;
                    double pending = 0;
                    for (LeaveTypeColumn type : types) {
                        double[] pair = used.getOrDefault(userId + "|" + type.id(), new double[2]);
                        approved += pair[0];
                        pending += pair[1];
                        Double remaining = type.unpaid() ? null : Math.max(0, type.yearlyDays() - pair[0] - pair[1]);
                        cells.add(new LeaveCell(type.id(), pair[0], pair[1], remaining));
                    }
                    return new LeaveRow(userId, rs.getString("full_name"), rs.getString("email"), cells, approved,
                            pending);
                });
        return new LeaveReport(year, types, rows);
    }

    @Transactional(readOnly = true)
    public ReceivablesReport receivables(UUID clientId) {
        LocalDate today = LocalDate.now(zone);
        List<Object> args = new ArrayList<>();
        String clientFilter = "";
        if (clientId != null) {
            clientFilter = " and i.client_id = ?";
            args.add(clientId);
        }
        List<ReceivableRow> rows = jdbc.query(
                "select i.id, i.number, cl.name as client, i.candidate_name, i.status, i.invoice_date, i.due_date, "
                        + "i.total_amount, i.settled_amount from invoice i join client cl on cl.id = i.client_id "
                        + "where i.status in ('ISSUED', 'PARTIALLY_PAID')" + clientFilter
                        + " order by i.due_date, i.number limit " + MAX_ROWS,
                (rs, n) -> {
                    LocalDate due = rs.getDate("due_date").toLocalDate();
                    BigDecimal total = rs.getBigDecimal("total_amount");
                    BigDecimal settled = rs.getBigDecimal("settled_amount");
                    long overdue = Math.max(0, ChronoUnit.DAYS.between(due, today));
                    return new ReceivableRow(rs.getObject("id", UUID.class), rs.getString("number"),
                            rs.getString("client"), rs.getString("candidate_name"), rs.getString("status"),
                            rs.getDate("invoice_date").toLocalDate(), due, total, settled, total.subtract(settled),
                            overdue, bucket(due, today));
                },
                args.toArray());
        BigDecimal notDue = BigDecimal.ZERO;
        BigDecimal d30 = BigDecimal.ZERO;
        BigDecimal d60 = BigDecimal.ZERO;
        BigDecimal d90 = BigDecimal.ZERO;
        BigDecimal d90p = BigDecimal.ZERO;
        BigDecimal outstanding = BigDecimal.ZERO;
        BigDecimal overdue = BigDecimal.ZERO;
        for (ReceivableRow row : rows) {
            outstanding = outstanding.add(row.outstanding());
            if (!"NOT_DUE".equals(row.bucket())) {
                overdue = overdue.add(row.outstanding());
            }
            switch (row.bucket()) {
                case "NOT_DUE" -> notDue = notDue.add(row.outstanding());
                case "D1_30" -> d30 = d30.add(row.outstanding());
                case "D31_60" -> d60 = d60.add(row.outstanding());
                case "D61_90" -> d90 = d90.add(row.outstanding());
                default -> d90p = d90p.add(row.outstanding());
            }
        }
        return new ReceivablesReport(today, outstanding, overdue, new Aging(notDue, d30, d60, d90, d90p), rows);
    }

    @Transactional(readOnly = true)
    public TaxSummary taxSummary(LocalDate from, LocalDate to, UUID clientId) {
        LocalDate[] range = range(from, to);
        Map<String, BigDecimal[]> byMonth = new TreeMap<>();
        List<Object> invoiceArgs = new ArrayList<>(List.of(Date.valueOf(range[0]), Date.valueOf(range[1])));
        String invoiceClient = "";
        List<Object> paymentArgs = new ArrayList<>(List.of(Date.valueOf(range[0]), Date.valueOf(range[1])));
        String paymentClient = "";
        if (clientId != null) {
            invoiceClient = " and i.client_id = ?";
            invoiceArgs.add(clientId);
            paymentClient = " and i.client_id = ?";
            paymentArgs.add(clientId);
        }
        jdbc.query(
                "select to_char(i.invoice_date, 'YYYY-MM') as month, sum(i.base_amount) as base, "
                        + "sum(i.cgst_amount) as cgst, sum(i.sgst_amount) as sgst, sum(i.igst_amount) as igst, "
                        + "sum(i.total_amount) as total, sum(i.tds_amount) as tds from invoice i "
                        + "where i.status <> 'CANCELLED' and i.invoice_date between ? and ?" + invoiceClient
                        + " group by 1",
                rs -> {
                    BigDecimal[] v = slot(byMonth, rs.getString("month"));
                    v[0] = rs.getBigDecimal("base");
                    v[1] = rs.getBigDecimal("cgst");
                    v[2] = rs.getBigDecimal("sgst");
                    v[3] = rs.getBigDecimal("igst");
                    v[4] = rs.getBigDecimal("total");
                    v[5] = rs.getBigDecimal("tds");
                },
                invoiceArgs.toArray());
        jdbc.query(
                "select to_char(x.paid_on, 'YYYY-MM') as month, sum(x.tds_deducted) as tds, "
                        + "sum(x.amount_received) as received from invoice_payment x "
                        + "join invoice i on i.id = x.invoice_id where x.paid_on between ? and ?" + paymentClient
                        + " group by 1",
                rs -> {
                    BigDecimal[] v = slot(byMonth, rs.getString("month"));
                    v[6] = rs.getBigDecimal("tds");
                    v[7] = rs.getBigDecimal("received");
                },
                paymentArgs.toArray());
        List<TaxMonth> months = new ArrayList<>();
        BigDecimal[] sums = zeros();
        byMonth.forEach((month, v) -> {
            months.add(new TaxMonth(month, v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7]));
            for (int i = 0; i < sums.length; i++) {
                sums[i] = sums[i].add(v[i]);
            }
        });
        TaxMonth totals = new TaxMonth("", sums[0], sums[1], sums[2], sums[3], sums[4], sums[5], sums[6], sums[7]);
        return new TaxSummary(months, totals);
    }

    @Transactional(readOnly = true)
    public CommissionReport commissions(LocalDate from, LocalDate to, String status) {
        LocalDate[] range = range(from, to);
        LocalDate today = LocalDate.now(zone);
        List<Object> args = new ArrayList<>(List.of(Date.valueOf(range[0]), Date.valueOf(range[1])));
        String statusFilter = "";
        if (status != null && !status.isBlank()) {
            if (!List.of("PENDING", "PAID", "CANCELLED").contains(status)) {
                throw ApiException.rule(ErrorCodes.REPORT_RANGE_INVALID, "Unknown commission status");
            }
            statusFilter = " and c.status = ?";
            args.add(status);
        }
        List<CommissionRow> rows = jdbc.query(
                "select c.id, coalesce(u.full_name, c.recruiter_name, c.recruiter_email) as recruiter, "
                        + "cand.full_name as candidate, cl.name as client, j.code, p.joining_date, c.amount, c.status, "
                        + "c.payable_after, c.paid_on, c.payment_reference, inv.number as invoice_number, "
                        + "inv.status as invoice_status from commission c "
                        + "join placement p on p.id = c.placement_id "
                        + "join candidate cand on cand.id = p.candidate_id "
                        + "join client cl on cl.id = p.client_id "
                        + "join job j on j.id = p.job_id "
                        + "left join app_user u on u.id = c.recruiter_user_id "
                        + "left join lateral (select i.number, i.status from invoice i where i.placement_id = p.id "
                        + "and i.status <> 'CANCELLED' order by i.created_at desc limit 1) inv on true "
                        + "where p.joining_date between ? and ?" + statusFilter
                        + " order by coalesce(u.full_name, c.recruiter_name, c.recruiter_email), p.joining_date desc "
                        + "limit " + MAX_ROWS,
                (rs, n) -> {
                    BigDecimal amount = rs.getBigDecimal("amount");
                    String st = rs.getString("status");
                    LocalDate payableAfter = rs.getDate("payable_after").toLocalDate();
                    String invoiceStatus = rs.getString("invoice_status");
                    String state;
                    if (!"PENDING".equals(st)) {
                        state = st;
                    } else if (amount == null) {
                        state = "AMOUNT_NOT_SET";
                    } else if (payableAfter.isAfter(today)) {
                        state = "PERIOD_RUNNING";
                    } else if (!"PAID".equals(invoiceStatus)) {
                        state = "INVOICE_UNPAID";
                    } else {
                        state = "PAYABLE";
                    }
                    Date paidOn = rs.getDate("paid_on");
                    return new CommissionRow(rs.getObject("id", UUID.class), rs.getString("recruiter"),
                            rs.getString("candidate"), rs.getString("client"), rs.getString("code"),
                            rs.getDate("joining_date").toLocalDate(), amount, st, state, payableAfter,
                            paidOn == null ? null : paidOn.toLocalDate(), rs.getString("payment_reference"),
                            rs.getString("invoice_number"), invoiceStatus);
                },
                args.toArray());
        Map<String, BigDecimal[]> byRecruiter = new LinkedHashMap<>();
        Map<String, Long> counts = new HashMap<>();
        BigDecimal payable = BigDecimal.ZERO;
        BigDecimal waiting = BigDecimal.ZERO;
        BigDecimal paid = BigDecimal.ZERO;
        for (CommissionRow row : rows) {
            BigDecimal amount = row.amount() == null ? BigDecimal.ZERO : row.amount();
            BigDecimal[] slot = byRecruiter.computeIfAbsent(row.recruiter(),
                    k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            counts.merge(row.recruiter(), 1L, Long::sum);
            if ("PAYABLE".equals(row.state())) {
                payable = payable.add(amount);
                slot[0] = slot[0].add(amount);
            } else if ("PAID".equals(row.state())) {
                paid = paid.add(amount);
                slot[2] = slot[2].add(amount);
            } else if (!"CANCELLED".equals(row.state())) {
                waiting = waiting.add(amount);
                slot[1] = slot[1].add(amount);
            }
        }
        List<RecruiterCommission> summary = new ArrayList<>();
        byRecruiter.forEach((name, v) -> summary.add(new RecruiterCommission(name, counts.get(name), v[0], v[1], v[2])));
        return new CommissionReport(payable, waiting, paid, summary, rows);
    }

    private String month(String value) {
        if (value == null || value.isBlank()) {
            return YearMonth.now(zone).toString();
        }
        try {
            return YearMonth.parse(value).toString();
        } catch (DateTimeParseException ex) {
            throw ApiException.rule(ErrorCodes.PAYROLL_MONTH_INVALID, "Month must look like 2026-10");
        }
    }

    private LocalDate[] range(LocalDate from, LocalDate to) {
        LocalDate end = to == null ? LocalDate.now(zone) : to;
        LocalDate start = from == null ? end.minusMonths(6) : from;
        if (start.isAfter(end)) {
            throw ApiException.rule(ErrorCodes.REPORT_RANGE_INVALID, "The start date must be on or before the end date");
        }
        if (ChronoUnit.DAYS.between(start, end) > MAX_DAYS) {
            throw ApiException.rule(ErrorCodes.REPORT_RANGE_INVALID, "Please choose a range of up to three years");
        }
        return new LocalDate[] {start, end};
    }

    static String bucket(LocalDate due, LocalDate today) {
        long late = ChronoUnit.DAYS.between(due, today);
        if (late <= 0) {
            return "NOT_DUE";
        }
        if (late <= 30) {
            return "D1_30";
        }
        if (late <= 60) {
            return "D31_60";
        }
        if (late <= 90) {
            return "D61_90";
        }
        return "D90_PLUS";
    }

    private static BigDecimal[] slot(Map<String, BigDecimal[]> map, String month) {
        return map.computeIfAbsent(month, k -> zeros());
    }

    private static BigDecimal[] zeros() {
        BigDecimal[] values = new BigDecimal[8];
        java.util.Arrays.fill(values, BigDecimal.ZERO);
        return values;
    }

    private static PayrollTotals zeroTotals() {
        BigDecimal z = BigDecimal.ZERO;
        return new PayrollTotals(z, z, z, z, z, z, z, z);
    }

    private static BigDecimal add(List<PayrollRow> rows, java.util.function.Function<PayrollRow, BigDecimal> getter) {
        return rows.stream().map(getter).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
