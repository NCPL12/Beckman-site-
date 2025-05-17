package ncpl.bms.reports.service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.sql.PreparedStatement;
import java.util.List;

@Component
@Slf4j
public class SchedulingService {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void scheduleDailyReport(int reportId, String reportName ,  String assignedTo, String assigned_approver, String ScheduledBy,  String dailyTime) {
        int chk = (assigned_approver == null || assigned_approver.trim().isEmpty()) ? 0 : 1;
        long currentTimeMillis = System.currentTimeMillis();
       String sql = "INSERT INTO Daily_Scheduled_Reports (IdOfReport, Name, assigned_review, isApproverRequired, assignedApprover, scheduled_by, schedule_date, DailyTimeOfReport) VALUES (?, ?,?,?,?, ?, ?, ?)";
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, reportId);
            ps.setString(2, reportName);
            ps.setString(3, assignedTo);

            ps.setBoolean(4, chk==1);
            ps.setString(5, assigned_approver);
            ps.setString(6, ScheduledBy);
            ps.setLong(7, currentTimeMillis);
            ps.setString(8, dailyTime);  // Store the dailyTime in the database
            return ps;
        });
    }

    public void scheduleWeeklyReport(int reportId, String reportName ,  String assignedTo, String assigned_approver, String ScheduledBy,  String weeklyTime, String weeklyDay) {
        int chk = (assigned_approver == null || assigned_approver.trim().isEmpty()) ? 0 : 1;
        long currentTimeMillis = System.currentTimeMillis();
        String sql = "INSERT INTO weekly_scheduled_reports (IdOfReport, Name, assigned_review, isApproverRequired, assignedApprover, scheduled_by, schedule_date, TimeOfReport, dayOfReport) VALUES (?, ?,?,?,?, ?, ?, ?,?)";
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, reportId);
            ps.setString(2, reportName);
            ps.setString(3, assignedTo);

            ps.setBoolean(4, chk==1);
            ps.setString(5, assigned_approver);
            ps.setString(6, ScheduledBy);
            ps.setLong(7, currentTimeMillis);
            ps.setString(8, weeklyTime);
            ps.setString(9,weeklyDay);
            return ps;
        });
    }


    public void scheduleMonthlyReport(int reportId, String reportName ,  String assignedTo, String assigned_approver, String ScheduledBy,  String monthTime, String monthDay) {
        int chk = (assigned_approver == null || assigned_approver.trim().isEmpty()) ? 0 : 1;
        long currentTimeMillis = System.currentTimeMillis();
        String sql = "INSERT INTO monthly_scheduled_reports (IdOfReport, Name, assigned_review, isApproverRequired, assignedApprover, scheduled_by, schedule_date, TimeOfReport, dayOfReport) VALUES (?, ?,?,?,?, ?, ?, ?,?)";
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setInt(1, reportId);
            ps.setString(2, reportName);
            ps.setString(3, assignedTo);

            ps.setBoolean(4, chk==1);
            ps.setString(5, assigned_approver);
            ps.setString(6, ScheduledBy);
            ps.setLong(7, currentTimeMillis);
            ps.setString(8, monthTime);
            ps.setString(9,monthDay);
            return ps;
        });
    }

    public List<Integer> getAllScheduledReports() {
        String sql = "SELECT IdOfReport FROM Daily_Scheduled_Reports";
        return jdbcTemplate.queryForList(sql, Integer.class);
    }

    public List<Integer> getAllWeeklyScheduledReports() {
        String sql = "SELECT IdOfReport FROM weekly_scheduled_reports";
        return jdbcTemplate.queryForList(sql, Integer.class);
    }

    public List<Integer> getAllMonthlyScheduledReports() {
        String sql = "SELECT IdOfReport FROM monthly_scheduled_reports";
        return jdbcTemplate.queryForList(sql, Integer.class);
    }

}
