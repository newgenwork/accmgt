package com.act.web.controller;

import com.act.dto.TimesheetFilter;
import com.act.model.Ledger;
import com.act.model.TimeSheet;
import com.act.repo.LedgerRepository;
import com.act.repo.TimesheetRepository;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("api/v1/timesheet")
public class TimeSheetController {

    private final TimesheetRepository timesheetRepository;
    private final LedgerRepository ledgerRepository;

    public TimeSheetController(
            TimesheetRepository timesheetRepository,
            LedgerRepository ledgerRepository) {
        this.timesheetRepository = timesheetRepository;
        this.ledgerRepository = ledgerRepository;
    }

    /* ================= ADD ================= */

    @GetMapping("/add")
    public String showAddTimesheetForm(Model model) {
        model.addAttribute(
                "employees",
                ledgerRepository.findByIsEmployeeAndTypeAndLabelOrderByLedgerNameAsc(
                        "Y", "Expense", "employee"));
        model.addAttribute("timesheet", new TimeSheet());
        return "timesheet-add";
    }

    @PostMapping("/add")
    public String saveTimesheet(@ModelAttribute TimeSheet timeSheet) {

        // ✅ Validate date order
        if (timeSheet.getStartDate().isAfter(timeSheet.getEndDate())) {
            return "redirect:/api/v1/timesheet/list?error=Invalid date range";
        }

        // ✅ Overlap validation
        List<TimeSheet> collisions =
                timesheetRepository.findCollidingTimeSheets(
                        timeSheet.getEmployee(),
                        timeSheet.getStartDate(),
                        timeSheet.getEndDate(),
                        timeSheet.getId());

        if (!collisions.isEmpty()) {
            return "redirect:/api/v1/timesheet/list?error=Timesheet date range overlaps";
        }

        if (timeSheet.getEmployee().getInvoiceRateValidateToDate().isBefore(timeSheet.getEndDate())){
            return "redirect:/api/v1/timesheet/list?error=Invalid date range more than last configured InvoiceRateValidateToDate";
        }

        // ✅ Edit existing
        if (timeSheet.getId() != null) {
            Optional<TimeSheet> existing = timesheetRepository.findById(timeSheet.getId());

            if (existing.isPresent()) {
                if (existing.get().getInvoiceDetail() != null) {
                    return "redirect:/api/v1/timesheet/list?error=Already invoiced";
                }

                existing.get().setEmployee(timeSheet.getEmployee());
                existing.get().setStartDate(timeSheet.getStartDate());
                existing.get().setEndDate(timeSheet.getEndDate());
                existing.get().setNoOfHrs(timeSheet.getNoOfHrs());

                timesheetRepository.save(existing.get());
                return "redirect:/api/v1/timesheet/list?success=updated";
            }
        }

        // ✅ New insert
        timesheetRepository.save(timeSheet);
        return "redirect:/api/v1/timesheet/list?success=added";
    }

    @GetMapping("/employees/by-date")
    @ResponseBody
    public List<Map<String, Object>> getEmployeesByDate(
            @RequestParam String startDate,
            @RequestParam String endDate) {

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        List<Ledger> employees =
                ledgerRepository.findByIsEmployeeAndTypeAndLabelOrderByLedgerNameAsc(
                        "Y", "Expense", "employee");
        List<Map<String, Object>> result = new ArrayList<>();

        for (Ledger e : employees) {

            // ✅ VALIDATION LOGIC
            if (e.getInvoiceRateValidateToDate() != null &&
                    e.getInvoiceRateValidateToDate().isBefore(end)) {
                continue; // ❌ skip invalid contractor
            }

            Map<String, Object> map = new HashMap<>();
            map.put("id", e.getId());
            map.put("name", e.getLedgerName());

            result.add(map);
        }

        return result;
    }

    /* ================= EDIT ================= */

    @GetMapping("/edit/{timesheetId}")
    public String showEditTimesheetForm(
            @PathVariable Long timesheetId,
            Model model) {

        model.addAttribute(
                "employees",
                ledgerRepository.findByIsEmployeeAndTypeAndLabelOrderByLedgerNameAsc(
                        "Y", "Expense", "employee"));

        Optional<TimeSheet> timeSheet = timesheetRepository.findById(timesheetId);

        if (timeSheet.isEmpty()) {
            return "redirect:/api/v1/timesheet/list?error=NotFound";
        }

        model.addAttribute("timesheet", timeSheet.get());
        return "timesheet-add";
    }

    /* ================= LIST ================= */

    @GetMapping("/list")
    public String listTimeSheet(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {



        LocalDate safeStart = startDate != null
                ? startDate
                : LocalDate.of(1900, 1, 1);

        LocalDate safeEnd = endDate != null
                ? endDate
                : LocalDate.of(9999, 12, 31);

        List<TimeSheet> timeSheets =
                timesheetRepository.findWithFilters(
                        employeeId, clientId , safeStart, safeEnd);

        timeSheets.sort(
                Comparator.comparing(TimeSheet::getStartDate).reversed());

        model.addAttribute("timeSheetList", timeSheets);
        return "timesheet-list";
    }

    @Transactional
    @GetMapping("/filter")
    public String filterTimesheets(
            @RequestParam(required = false) Long clientId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Model model) {
        model.addAttribute("timesheetFilter", new TimesheetFilter());
        List<Ledger>clients = ledgerRepository.findByIsEmployeeAndTypeAndLabelOrderByLedgerNameAsc("N", "Asset", "AR");
        model.addAttribute("clients", clients);
        return "timesheet-filter";
    }





    @GetMapping("/vendors/{vendorId}/employees")
    @ResponseBody
    public List<Ledger> getEmployeesByVendor(@PathVariable Long vendorId) {

        // Example: employees linked to vendor
        List<Ledger> employees =
                ledgerRepository.findByIsEmployeeAndTypeAndInvoiceLedger(
                        "Y",
                        "Expense",
                        ledgerRepository.findById(vendorId).orElseThrow()
                ).orElse(List.of());

        return employees;
    }


}
