package ncpl.bms.reports.controller;
import lombok.extern.slf4j.Slf4j;
import ncpl.bms.reports.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

//--------------------COMPLETE FILE IS WRITTEN BY VISHAL----------------------//

@RestController
@RequestMapping("v1")
@CrossOrigin(origins = "http://localhost:4200")
@Slf4j
public class SchedulingController {


    @Autowired
    private SchedulingService schedulingService;

    // Schedule daily report
    @PostMapping("/schedule-report-daily")
    public void scheduleReport(@RequestBody Map<String, String> requestBody) {
        String name = requestBody.get("name");
        String Strid = requestBody.get("id");
        String assigned_approver = requestBody.get("assignedApprover");
        String assignedTo = requestBody.get("assignedReview");
        int id = Integer.parseInt(Strid);
        String ScheduledBy = requestBody.get("scheduledBy");
        String dailyTime = requestBody.get("dailyTime");  // Get the dailyTime from the request
        schedulingService.scheduleDailyReport( id, name, assignedTo, assigned_approver, ScheduledBy, dailyTime);
    }

    @PostMapping("/schedule-report-weekly")
    public void scheduleWeeklyReport(@RequestBody Map<String, String> requestBody) {
        String name = requestBody.get("name");
        String Strid = requestBody.get("id");
        String assigned_approver = requestBody.get("assignedApprover");
        String assignedTo = requestBody.get("assignedReview");
        int id = Integer.parseInt(Strid);
        String ScheduledBy = requestBody.get("scheduledBy");
        String weeklyTime = requestBody.get("weeklyTime");
        String weeklyDay = requestBody.get("weeklyDay");
        schedulingService.scheduleWeeklyReport( id, name, assignedTo, assigned_approver, ScheduledBy, weeklyTime, weeklyDay);
    }

    @PostMapping("/schedule-report-monthly")
    public void scheduleMonthlyReport(@RequestBody Map<String, String> requestBody) {
        String name = requestBody.get("name");
        String Strid = requestBody.get("id");
        String assigned_approver = requestBody.get("assignedApprover");
        String assignedTo = requestBody.get("assignedReview");
        int id = Integer.parseInt(Strid);
        String ScheduledBy = requestBody.get("scheduledBy");
        String monthTime = requestBody.get("monthlyTime");
        String monthDay = requestBody.get("monthlyDay");
        schedulingService.scheduleMonthlyReport( id, name, assignedTo, assigned_approver, ScheduledBy, monthTime, monthDay);
    }

    @GetMapping("/get-all-daily-scheduled-reports")
    public List<Integer> getAllScheduledReports() {
        return schedulingService.getAllScheduledReports();
    }

    @GetMapping("/get-all-weekly-scheduled-reports")
    public List<Integer> getAllWeeklyScheduledReports() {
        return schedulingService.getAllWeeklyScheduledReports();
    }

    @GetMapping("/get-all-monthly-scheduled-reports")
    public List<Integer> getAllMonthlyScheduledReports() {
        return schedulingService.getAllMonthlyScheduledReports();
    }

}