package com.abchr.platform.report;

import com.abchr.platform.report.ReportDtos.Aging;
import com.abchr.platform.report.ReportDtos.CountPoint;
import com.abchr.platform.report.ReportDtos.FunnelReport;
import com.abchr.platform.report.ReportDtos.FunnelStage;
import com.abchr.platform.report.ReportDtos.JoiningRow;
import com.abchr.platform.report.ReportDtos.JoiningsReport;
import com.abchr.platform.report.ReportDtos.MoneyPoint;
import com.abchr.platform.report.ReportDtos.RecruiterRow;
import com.abchr.platform.report.ReportDtos.ReplacementRow;
import com.abchr.platform.report.ReportDtos.RevenueReport;
import com.abchr.platform.report.ReportDtos.RevenueRow;
import com.abchr.platform.shared.security.Permissions;
import com.abchr.platform.shared.web.ApiException;
import com.abchr.platform.shared.web.ErrorCodes;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {

    private static final int MAX_ROWS = 5000;
    private static final long MAX_DAYS = 1100;

    private final JdbcTemplate jdbc;
    private final ZoneId zone;

    public ReportService(JdbcTemplate jdbc, @Value("${app.timezone:Asia/Kolkata}") String zoneId) {
        this.jdbc = jdbc;
        this.zone = ZoneId.of(zoneId);
    }

    @Transactional(readOnly = true)
    public JoiningsReport joinings(LocalDate from, LocalDate to, UUID clientId) {
        LocalDate[] range = range(from, to);
        boolean money = Permissions.hasAny("INVOICES_VIEW", "INVOICES_MANAGE");
        List<Object> args = new ArrayList<>(List.of(Date.valueOf(range[0]), Date.valueOf(range[1])));
        String clientFilter = clientFilter("p.client_id", clientId, args);
        List<JoiningRow> rows = jdbc.query(
                "select p.id, p.joining_date, c.full_name as candidate, cl.name as client, j.code, j.title, "
                        + "coalesce(u.full_name, s.created_by) as recruiter, p.offered_ctc, "
                        + "inv.number as invoice_number, inv.total_amount as invoice_total, inv.status as invoice_status "
                        + "from placement p "
                        + "join candidate c on c.id = p.candidate_id "
                        + "join client cl on cl.id = p.client_id "
                        + "join job j on j.id = p.job_id "
                        + "join submission s on s.id = p.submission_id "
                        + "left join app_user u on lower(u.email) = lower(s.created_by) "
                        + "left join lateral (select i.number, i.total_amount, i.status from invoice i "
                        + "where i.placement_id = p.id and i.status <> 'CANCELLED' order by i.created_at desc limit 1) inv on true "
                        + "where p.status = 'JOINED' and p.joining_date between ? and ? " + clientFilter
                        + " order by p.joining_date desc, c.full_name limit " + MAX_ROWS,
                (rs, n) -> new JoiningRow(
                        rs.getObject("id", UUID.class),
                        rs.getDate("joining_date").toLocalDate(),
                        rs.getString("candidate"),
                        rs.getString("client"),
                        rs.getString("code"),
                        rs.getString("title"),
                        rs.getString("recruiter"),
                        rs.getBigDecimal("offered_ctc"),
                        money ? rs.getString("invoice_number") : null,
                        money ? rs.getBigDecimal("invoice_total") : null,
                        money ? rs.getString("invoice_status") : null),
                args.toArray());
        BigDecimal totalCtc = rows.stream().map(JoiningRow::offeredCtc).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalInvoiced = money
                ? rows.stream().map(JoiningRow::invoiceTotal).filter(v -> v != null).reduce(BigDecimal.ZERO, BigDecimal::add)
                : null;
        Map<String, Long> months = new LinkedHashMap<>();
        for (LocalDate m = range[0].withDayOfMonth(1); !m.isAfter(range[1]); m = m.plusMonths(1)) {
            months.put(String.format(Locale.ROOT, "%d-%02d", m.getYear(), m.getMonthValue()), 0L);
        }
        for (JoiningRow row : rows) {
            String key = String.format(Locale.ROOT, "%d-%02d", row.joiningDate().getYear(),
                    row.joiningDate().getMonthValue());
            months.merge(key, 1L, Long::sum);
        }
        List<CountPoint> byMonth = months.entrySet().stream()
                .map(e -> new CountPoint(e.getKey(), e.getValue())).toList();
        return new JoiningsReport(rows.size(), totalCtc, totalInvoiced, byMonth, rows);
    }

    @Transactional(readOnly = true)
    public RevenueReport revenue(LocalDate from, LocalDate to, UUID clientId) {
        LocalDate[] range = range(from, to);
        LocalDate today = LocalDate.now(zone);
        Date f = Date.valueOf(range[0]);
        Date t = Date.valueOf(range[1]);
        List<RevenueRow> rows = jdbc.query(
                "select cl.id, cl.name, "
                        + "coalesce((select sum(i.total_amount) from invoice i where i.client_id = cl.id "
                        + "and i.status <> 'CANCELLED' and i.invoice_date between ? and ?), 0) as invoiced, "
                        + "coalesce((select sum(x.amount_received) from invoice_payment x join invoice i on i.id = x.invoice_id "
                        + "where i.client_id = cl.id and x.paid_on between ? and ?), 0) as collected, "
                        + "coalesce((select sum(i.total_amount - i.settled_amount) from invoice i where i.client_id = cl.id "
                        + "and i.status in ('ISSUED', 'PARTIALLY_PAID')), 0) as outstanding, "
                        + "coalesce((select sum(i.total_amount - i.settled_amount) from invoice i where i.client_id = cl.id "
                        + "and i.status in ('ISSUED', 'PARTIALLY_PAID') and i.due_date < ?), 0) as overdue "
                        + "from client cl " + (clientId == null ? "" : "where cl.id = ? ") + "order by cl.name",
                (rs, n) -> new RevenueRow(rs.getObject("id", UUID.class), rs.getString("name"),
                        rs.getBigDecimal("invoiced"), rs.getBigDecimal("collected"), rs.getBigDecimal("outstanding"),
                        rs.getBigDecimal("overdue")),
                clientId == null ? new Object[] {f, t, f, t, Date.valueOf(today)}
                        : new Object[] {f, t, f, t, Date.valueOf(today), clientId});
        List<RevenueRow> visible = rows.stream()
                .filter(r -> r.invoiced().signum() != 0 || r.collected().signum() != 0 || r.outstanding().signum() != 0)
                .sorted((a, b) -> b.invoiced().compareTo(a.invoiced()))
                .toList();
        BigDecimal invoiced = sum(visible, RevenueRow::invoiced);
        BigDecimal collected = sum(visible, RevenueRow::collected);
        BigDecimal outstanding = sum(visible, RevenueRow::outstanding);
        BigDecimal overdue = sum(visible, RevenueRow::overdue);
        List<Object> agingArgs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            agingArgs.add(Date.valueOf(today));
        }
        String agingClient = clientFilter("i.client_id", clientId, agingArgs);
        Aging aging = jdbc.queryForObject(
                "select "
                        + "coalesce(sum(case when i.due_date >= cast(? as date) then i.total_amount - i.settled_amount end), 0) as not_due, "
                        + "coalesce(sum(case when cast(? as date) - i.due_date between 1 and 30 then i.total_amount - i.settled_amount end), 0) as d30, "
                        + "coalesce(sum(case when cast(? as date) - i.due_date between 31 and 60 then i.total_amount - i.settled_amount end), 0) as d60, "
                        + "coalesce(sum(case when cast(? as date) - i.due_date between 61 and 90 then i.total_amount - i.settled_amount end), 0) as d90, "
                        + "coalesce(sum(case when cast(? as date) - i.due_date > 90 then i.total_amount - i.settled_amount end), 0) as d90p "
                        + "from invoice i where i.status in ('ISSUED', 'PARTIALLY_PAID')" + agingClient,
                (rs, n) -> new Aging(rs.getBigDecimal("not_due"), rs.getBigDecimal("d30"), rs.getBigDecimal("d60"),
                        rs.getBigDecimal("d90"), rs.getBigDecimal("d90p")),
                agingArgs.toArray());
        List<MoneyPoint> trend = jdbc.query(
                "select to_char(m, 'YYYY-MM') as month, "
                        + "coalesce((select sum(i.total_amount) from invoice i where i.status <> 'CANCELLED' "
                        + "and i.invoice_date >= m::date and i.invoice_date < (m + interval '1 month')::date"
                        + (clientId == null ? "" : " and i.client_id = ?") + "), 0) as invoiced, "
                        + "coalesce((select sum(x.amount_received) from invoice_payment x join invoice i on i.id = x.invoice_id "
                        + "where x.paid_on >= m::date and x.paid_on < (m + interval '1 month')::date"
                        + (clientId == null ? "" : " and i.client_id = ?") + "), 0) as collected "
                        + "from generate_series(cast(? as date), cast(? as date), interval '1 month') m order by m",
                (rs, n) -> new MoneyPoint(rs.getString("month"), rs.getBigDecimal("invoiced"),
                        rs.getBigDecimal("collected")),
                trendSimple(clientId, range));
        return new RevenueReport(invoiced, collected, outstanding, overdue, aging, trend, visible);
    }

    private Object[] trendSimple(UUID clientId, LocalDate[] range) {
        List<Object> args = new ArrayList<>();
        if (clientId != null) {
            args.add(clientId);
            args.add(clientId);
        }
        args.add(Date.valueOf(range[0].withDayOfMonth(1)));
        args.add(Date.valueOf(range[1]));
        return args.toArray();
    }

    @Transactional(readOnly = true)
    public List<RecruiterRow> recruiters(LocalDate from, LocalDate to, UUID clientId) {
        LocalDate[] range = range(from, to);
        boolean commissions = Permissions.hasAny("COMMISSIONS_VIEW", "COMMISSIONS_MANAGE");
        List<Object> args = new ArrayList<>(List.of(Date.valueOf(range[0]), Date.valueOf(range[1])));
        String clientFilter = clientFilter("s.client_id", clientId, args);
        Map<String, BigDecimal[]> money = new HashMap<>();
        if (commissions) {
            jdbc.query("select lower(c.recruiter_email) as email, "
                            + "coalesce(sum(case when c.status <> 'CANCELLED' then c.amount end), 0) as total, "
                            + "coalesce(sum(case when c.status = 'PAID' then c.amount end), 0) as paid "
                            + "from commission c join placement p on p.id = c.placement_id "
                            + "where p.joining_date between ? and ? and c.recruiter_email is not null "
                            + "group by lower(c.recruiter_email)",
                    rs -> {
                        money.put(rs.getString("email"),
                                new BigDecimal[] {rs.getBigDecimal("total"), rs.getBigDecimal("paid")});
                    },
                    Date.valueOf(range[0]), Date.valueOf(range[1]));
        }
        return jdbc.query(
                "select s.created_by as email, max(u.full_name) as name, count(distinct s.id) as submissions, "
                        + "count(distinct s.id) filter (where s.status = 'SELECTED') as selected, "
                        + "count(distinct s.id) filter (where s.status = 'REJECTED') as rejected, "
                        + "count(distinct p.id) filter (where p.status = 'JOINED') as joined "
                        + "from submission s "
                        + "left join placement p on p.submission_id = s.id "
                        + "left join app_user u on lower(u.email) = lower(s.created_by) "
                        + "where " + submittedOn() + " between ? and ? " + clientFilter
                        + " group by s.created_by order by joined desc, submissions desc",
                (rs, n) -> {
                    String email = rs.getString("email");
                    BigDecimal[] values = email == null ? null : money.get(email.toLowerCase(Locale.ROOT));
                    return new RecruiterRow(email, rs.getString("name"), rs.getLong("submissions"),
                            rs.getLong("selected"), rs.getLong("rejected"), rs.getLong("joined"),
                            commissions ? (values == null ? BigDecimal.ZERO : values[0]) : null,
                            commissions ? (values == null ? BigDecimal.ZERO : values[1]) : null);
                },
                args.toArray());
    }

    @Transactional(readOnly = true)
    public FunnelReport funnel(LocalDate from, LocalDate to, UUID clientId) {
        LocalDate[] range = range(from, to);
        List<Object> args = new ArrayList<>(List.of(Date.valueOf(range[0]), Date.valueOf(range[1])));
        String clientFilter = clientFilter("s.client_id", clientId, args);
        String base = "from submission s left join placement p on p.submission_id = s.id "
                + "where " + submittedOn() + " between ? and ? " + clientFilter;
        Map<String, Long> counts = jdbc.queryForObject(
                "select count(*) as submitted, "
                        + "count(*) filter (where s.status in ('SHORTLISTED', 'INTERVIEWING', 'SELECTED') "
                        + "or exists (select 1 from submission_feedback f where f.submission_id = s.id "
                        + "and f.decision in ('SHORTLISTED', 'SELECTED'))) as shortlisted, "
                        + "count(*) filter (where exists (select 1 from interview iv where iv.submission_id = s.id "
                        + "and iv.status = 'COMPLETED')) as interviewed, "
                        + "count(*) filter (where s.status = 'SELECTED' or p.id is not null) as selected, "
                        + "count(*) filter (where p.id is not null) as offered, "
                        + "count(*) filter (where p.status in ('OFFER_ACCEPTED', 'JOINED')) as accepted, "
                        + "count(*) filter (where p.status = 'JOINED') as joined " + base,
                (rs, n) -> {
                    Map<String, Long> m = new LinkedHashMap<>();
                    for (String key : List.of("submitted", "shortlisted", "interviewed", "selected", "offered",
                            "accepted", "joined")) {
                        m.put(key, rs.getLong(key));
                    }
                    return m;
                },
                args.toArray());
        Double average = jdbc.queryForObject(
                "select avg(p.joining_date - " + submittedOn() + ")::float8 " + base + " and p.status = 'JOINED'",
                Double.class, args.toArray());
        List<FunnelStage> stages = counts.entrySet().stream()
                .map(e -> new FunnelStage(e.getKey(), e.getValue())).toList();
        return new FunnelReport(stages, average);
    }

    @Transactional(readOnly = true)
    public List<ReplacementRow> replacements(LocalDate from, LocalDate to, UUID clientId) {
        LocalDate[] range = range(from, to);
        Date f = Date.valueOf(range[0]);
        Date t = Date.valueOf(range[1]);
        List<Object> args = new ArrayList<>(List.of(f, t, f, t, f, t, f, t, f, t));
        if (clientId != null) {
            args.add(clientId);
        }
        return jdbc.query(
                "select * from (select cl.id, cl.name, "
                        + "(select count(*) from placement p where p.client_id = cl.id and p.status = 'JOINED' "
                        + "and p.joining_date between ? and ?) as joined, "
                        + "(select count(*) from replacement r where r.client_id = cl.id "
                        + "and r.exit_date between ? and ?) as exits, "
                        + "(select count(*) from replacement r where r.client_id = cl.id and r.within_period "
                        + "and r.exit_date between ? and ?) as within_period, "
                        + "(select count(*) from replacement r where r.client_id = cl.id and r.status = 'FILLED' "
                        + "and r.exit_date between ? and ?) as filled, "
                        + "(select count(*) from replacement r where r.client_id = cl.id and r.status = 'OPEN' "
                        + "and r.exit_date between ? and ?) as open "
                        + "from client cl " + (clientId == null ? "" : "where cl.id = ? ")
                        + ") x where x.joined > 0 or x.exits > 0 order by x.exits desc, x.name",
                (rs, n) -> new ReplacementRow(rs.getObject("id", UUID.class), rs.getString("name"),
                        rs.getLong("joined"), rs.getLong("exits"), rs.getLong("within_period"),
                        rs.getLong("filled"), rs.getLong("open")),
                args.toArray());
    }

    private String submittedOn() {
        return "(s.submitted_at at time zone '" + zone.getId() + "')::date";
    }

    private LocalDate[] range(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(zone);
        LocalDate end = to == null ? today : to;
        LocalDate start = from == null ? end.minusMonths(6) : from;
        if (start.isAfter(end)) {
            throw ApiException.rule(ErrorCodes.REPORT_RANGE_INVALID, "The start date must be on or before the end date");
        }
        if (ChronoUnit.DAYS.between(start, end) > MAX_DAYS) {
            throw ApiException.rule(ErrorCodes.REPORT_RANGE_INVALID, "Please choose a range of up to three years");
        }
        return new LocalDate[] {start, end};
    }

    private String clientFilter(String column, UUID clientId, List<Object> args) {
        if (clientId == null) {
            return "";
        }
        args.add(clientId);
        return " and " + column + " = ?";
    }

    private BigDecimal sum(List<RevenueRow> rows, java.util.function.Function<RevenueRow, BigDecimal> getter) {
        return rows.stream().map(getter).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
