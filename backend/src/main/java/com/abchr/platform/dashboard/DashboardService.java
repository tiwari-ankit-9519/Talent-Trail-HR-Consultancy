package com.abchr.platform.dashboard;

import com.abchr.platform.dashboard.DashboardDtos.CountPoint;
import com.abchr.platform.dashboard.DashboardDtos.DashboardResponse;
import com.abchr.platform.dashboard.DashboardDtos.Money;
import com.abchr.platform.dashboard.DashboardDtos.MoneyPoint;
import com.abchr.platform.dashboard.DashboardDtos.Pipeline;
import com.abchr.platform.shared.security.Permissions;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {

    private static final List<String> SUBMISSION_ORDER =
            List.of("SUBMITTED", "SHORTLISTED", "INTERVIEWING", "SELECTED", "ON_HOLD", "REJECTED", "WITHDRAWN");

    private static final String ACTIVE_INVOICE =
            "not exists (select 1 from invoice i where i.placement_id = p.id and i.status <> 'CANCELLED')";

    private final JdbcTemplate jdbc;
    private final ZoneId zone;

    public DashboardService(JdbcTemplate jdbc, @Value("${app.timezone:Asia/Kolkata}") String zoneId) {
        this.jdbc = jdbc;
        this.zone = ZoneId.of(zoneId);
    }

    @Transactional(readOnly = true)
    public DashboardResponse load() {
        LocalDate today = LocalDate.now(zone);
        LocalDate monthStart = today.withDayOfMonth(1);
        boolean pipeline = Permissions.hasAny("JOBS_VIEW", "JOBS_MANAGE", "CANDIDATES_VIEW", "CANDIDATES_MANAGE",
                "SUBMISSIONS_VIEW", "SUBMISSIONS_MANAGE", "PLACEMENTS_VIEW", "PLACEMENTS_MANAGE");
        boolean money = Permissions.hasAny("INVOICES_VIEW", "INVOICES_MANAGE");
        return new DashboardResponse(pipeline ? pipeline(monthStart) : null, money ? money(today, monthStart) : null);
    }

    private Pipeline pipeline(LocalDate monthStart) {
        long openJobs = count("select count(*) from job where status = 'OPEN'");
        long candidates = count("select count(*) from candidate");
        long available = count("select count(*) from candidate where status = 'AVAILABLE'");
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (String status : SUBMISSION_ORDER) {
            byStatus.put(status, 0L);
        }
        jdbc.query("select status, count(*) as total from submission group by status",
                rs -> {
                    byStatus.put(rs.getString("status"), rs.getLong("total"));
                });
        long interviews = count("select count(*) from interview where status = 'SCHEDULED' "
                + "and scheduled_at >= now() and scheduled_at < now() + interval '7 days'");
        long offers = count("select count(*) from placement where status in ('OFFERED', 'OFFER_ACCEPTED')");
        long joinings = jdbc.queryForObject(
                "select count(*) from placement where status = 'JOINED' and joining_date >= ?", Long.class,
                Date.valueOf(monthStart));
        List<CountPoint> trend = jdbc.query(
                "select to_char(m, 'YYYY-MM') as month, (select count(*) from placement p where p.status = 'JOINED' "
                        + "and p.joining_date >= m::date and p.joining_date < (m + interval '1 month')::date) as total "
                        + "from generate_series(cast(? as date), cast(? as date), interval '1 month') m order by m",
                (rs, n) -> new CountPoint(rs.getString("month"), rs.getLong("total")),
                Date.valueOf(monthStart.minusMonths(5)), Date.valueOf(monthStart));
        return new Pipeline(openJobs, candidates, available, byStatus, interviews, offers, joinings, trend);
    }

    private Money money(LocalDate today, LocalDate monthStart) {
        BigDecimal invoiced = decimal("select coalesce(sum(total_amount), 0) from invoice "
                + "where status <> 'CANCELLED' and invoice_date >= ?", Date.valueOf(monthStart));
        BigDecimal collected = decimal("select coalesce(sum(amount_received), 0) from invoice_payment "
                + "where paid_on >= ?", Date.valueOf(monthStart));
        BigDecimal outstanding = decimal("select coalesce(sum(total_amount - settled_amount), 0) from invoice "
                + "where status in ('ISSUED', 'PARTIALLY_PAID')");
        long overdueCount = jdbc.queryForObject("select count(*) from invoice "
                + "where status in ('ISSUED', 'PARTIALLY_PAID') and due_date < ?", Long.class, Date.valueOf(today));
        BigDecimal overdueAmount = decimal("select coalesce(sum(total_amount - settled_amount), 0) from invoice "
                + "where status in ('ISSUED', 'PARTIALLY_PAID') and due_date < ?", Date.valueOf(today));
        long ready = count("select count(*) from placement p where p.status = 'JOINED' "
                + "and p.joining_email_path is not null and " + ACTIVE_INVOICE
                + " and not exists (select 1 from replacement r where r.replacement_placement_id = p.id)");
        long awaiting = count("select count(*) from placement p where p.status = 'JOINED' "
                + "and p.joining_email_path is null and not exists "
                + "(select 1 from replacement r where r.replacement_placement_id = p.id)");
        List<MoneyPoint> trend = jdbc.query(
                "select to_char(m, 'YYYY-MM') as month, "
                        + "coalesce((select sum(total_amount) from invoice i where i.status <> 'CANCELLED' "
                        + "and i.invoice_date >= m::date and i.invoice_date < (m + interval '1 month')::date), 0) as invoiced, "
                        + "coalesce((select sum(amount_received) from invoice_payment x where x.paid_on >= m::date "
                        + "and x.paid_on < (m + interval '1 month')::date), 0) as collected "
                        + "from generate_series(cast(? as date), cast(? as date), interval '1 month') m order by m",
                (rs, n) -> new MoneyPoint(rs.getString("month"), rs.getBigDecimal("invoiced"),
                        rs.getBigDecimal("collected")),
                Date.valueOf(monthStart.minusMonths(5)), Date.valueOf(monthStart));
        return new Money(invoiced, collected, outstanding, overdueCount, overdueAmount, ready, awaiting, trend);
    }

    private long count(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    private BigDecimal decimal(String sql, Object... args) {
        BigDecimal value = jdbc.queryForObject(sql, BigDecimal.class, args);
        return value == null ? BigDecimal.ZERO : value;
    }
}
