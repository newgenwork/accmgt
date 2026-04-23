
package com.act.web.controller;

import com.act.dto.DateRange;
import com.act.dto.LedgerDto;
import com.act.dto.TransactionDto;
import com.act.json.model.Config;
import com.act.json.model.Event;
import com.act.json.model.EventAction;
import com.act.json.model.LocalDateAdapter;
import com.act.model.Ledger;
import com.act.model.TimeSheet;
import com.act.model.Transaction;
import com.act.repo.LedgerRepository;
import com.act.repo.TimesheetRepository;
import com.act.repo.TrasactionRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/api/v1/ledger")
public class LedgerController {

    private final LedgerRepository ledgerRepository;
    private final TrasactionRepository trasactionRepository;
    private final TimesheetRepository timesheetRepository;

    public LedgerController(
            LedgerRepository ledgerRepository,
            TrasactionRepository trasactionRepository,
            TimesheetRepository timesheetRepository
    ) {
        this.ledgerRepository = ledgerRepository;
        this.trasactionRepository = trasactionRepository;
        this.timesheetRepository = timesheetRepository;
    }
    @GetMapping("/edit/{id}")
    public String showEditLedgerForm(Model model, @PathVariable long id) {

        Optional<Ledger> t = ledgerRepository.findById(id);
        model.addAttribute("ledger", t.get());

        Optional<List<Ledger>> clients = ledgerRepository.findByIsEmployeeAndTypeAndLabel("N", "Asset", "AR");
        model.addAttribute("clients", clients.get());
        return "ledger-add";
    }
    @GetMapping("/statement/{id}")
    public String showStatementLedger(Model model, @PathVariable long id) {
        Optional<Ledger> t = ledgerRepository.findById(id);
        Optional<List<Transaction>> to = trasactionRepository.findByAccount(t.get());

        Iterator<Transaction> it = to.get().iterator();
        List<TransactionDto> transactions = new ArrayList<>();
        BigDecimal totalAmount = BigDecimal.ZERO;
        while (it.hasNext()) {
            Transaction tran = it.next();
            TransactionDto tdto = new TransactionDto();
            transactions.add(tdto);

            tdto.setAmount(tran.getAmount());
            tdto.setDescription(tran.getDescription());
            tdto.setTransactionDate(tran.getTransactionDate());

            if (tran.getInvoiceMaster()!=null) {
                tdto.setDetails("Invoice : " + tran.getInvoiceMaster().getClient().getLedgerName());
            }
            if (tran.getJournalEntry()!=null) {
                tdto.setDetails("Journal Entry : " + tran.getJournalEntry().getTargetAccount().getLedgerName() + ":" +
                        tran.getJournalEntry().getType())  ;
            }
            totalAmount = totalAmount.add(tran.getAmount());
            tdto.setBalance(totalAmount);
        }


        transactions.sort(
                Comparator.comparing(TransactionDto::getTransactionDate)
                        .reversed()
        );

        model.addAttribute("transactions", transactions);
        model.addAttribute("account", t.get().getLedgerName());
        return "statement-List";
    }



    @GetMapping("/add")
    public String showAddLedgerForm(Model model) {

        //Ledger ledger = new Ledger();
        //ledger.setId(sequenceRepository.getNextInvoiceSequence());
        model.addAttribute("ledger", new Ledger());

        Optional<List<Ledger>> clients = ledgerRepository.findByIsEmployeeAndTypeAndLabel("N", "Asset", "AR");
        model.addAttribute("clients", clients.get());
        return "ledger-add";
    }

    // Handle submit
    @PostMapping("/add")
    public String saveLedger(@ModelAttribute Ledger ledger) {

        Optional<Ledger> t = null;
        if (ledger.getId() != null) {
            t = ledgerRepository.findById(ledger.getId());
        }

        if (t!=null && t.isPresent()) {
            t.get().setLedgerName(ledger.getLedgerName());
            t.get().setConfig(ledger.getConfig());
            t.get().setEnable(ledger.getEnable());
            //t.get().setBalance(ledger.getBalance());
            t.get().setType(ledger.getType());
            t.get().setIsEmployee(ledger.getIsEmployee());
            t.get().setInvoiceRate(ledger.getInvoiceRate());
            t.get().setInvoiceRateValidateFromDate(ledger.getInvoiceRateValidateFromDate());
            t.get().setInvoiceRateValidateToDate(ledger.getInvoiceRateValidateToDate());
            t.get().setLabel(ledger.getLabel());
            t.get().setCompanyName(ledger.getCompanyName());
            t.get().setCompanyAddress(ledger.getCompanyAddress());
            t.get().setInvoiceLedger(ledger.getInvoiceLedger());
            t.get().setInvoiceCreationType(ledger.getInvoiceCreationType());
            t.get().setIsJournalEntryPossible(ledger.getIsJournalEntryPossible());
            t.get().setEndClientName(ledger.getEndClientName());
            ledgerRepository.save(t.get());
            return "redirect:/api/v1/ledger/list?success=" + t.get().getLedgerName();

        } else {
            ledger.setBalance(new BigDecimal(0));
            ledger.setBalanceUpdateDate(LocalDateTime.now());
            ledgerRepository.save(ledger);
            return "redirect:/api/v1/ledger/list?success="+ledger.getLedgerName();

        }
        //return "redirect:/employees";  // redirect to list page
//return "redirect:/api/v1/ledger/list?success";


    }


    @GetMapping("/list")
    public String listLedgers(
            @RequestParam(name = "label", required = false) String label,
            @RequestParam(name = "endClient", required = false) String endClient,
            @RequestParam(name = "invoiceLedger", required = false) String invoiceLedger,
            Model model) {
        DateTimeFormatter dateFormatter =
                DateTimeFormatter.ofPattern("MM/dd/yyyy");
        List<Ledger> retList = ledgerRepository.findAll();
        List<LedgerDto> retListDto = new ArrayList<LedgerDto>();
        Iterator<Ledger> it = retList.iterator();

        Set<String> ledgerNames =
                retList.stream()
                        .map(Ledger::getLedgerName)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        //BigDecimal totalAmountRate = BigDecimal.ZERO;
        Map<String, BigDecimal> totalAmountRateMap = new HashMap<>();
        while (it.hasNext()) {
            Ledger led = it.next();
            if (label !=null & led.getLabel() != null &&  !led.getLabel().equals(label)) {
                continue;
            }
            if (endClient !=null & led.getEndClientName() != null &&  !led.getEndClientName().equals(endClient)) {
                continue;
            }

            if (invoiceLedger !=null & led.getInvoiceLedger() != null &&  !led.getInvoiceLedger().getLedgerName().equals(invoiceLedger)) {
                continue;
            }
            LedgerDto dto = new LedgerDto();
            dto.setId(led.getId());
            dto.setBalance(led.getBalance());
            dto.setBalanceUpdateDate(led.getBalanceUpdateDate());
            dto.setConfig(led.getConfig());

            if (led.getLabel() != null &&  led.getLabel().equals("employee")) {
                dto.setShareConfig(getEventConfiguration("invoicePayment", led, ledgerNames, dto, true, totalAmountRateMap));

                //totalAmountRate = totalAmountRate.add(dto.getCompanyHourRate()==null?BigDecimal.ZERO:dto.getCompanyHourRate());
                //totalAmountRateMap.put("Megson", totalAmountRate);

            } else {
                dto.setShareConfig(getEventConfiguration("invoicePayment", led, ledgerNames, dto, false,totalAmountRateMap));
            }

            dto.setShareConfig(dto.getShareConfig() + "   :   " + getEventConfiguration("invoice", led, ledgerNames, dto, false,totalAmountRateMap));
            dto.setShareConfig(dto.getShareConfig() + "   :   " + getEventConfiguration("Non Client Billable Hrs Payment to AP", led,ledgerNames, dto, false,totalAmountRateMap));
            dto.setShareConfig(dto.getShareConfig() + "   :   " + getEventConfiguration("Release Payment to Vendor/Candidate", led,ledgerNames, dto, false,totalAmountRateMap));

            dto.setLedgerName(led.getLedgerName());
            dto.setIsEmployee(led.getIsEmployee());
            dto.setEnable(led.getEnable());

            dto.setLabel(led.getLabel());
            dto.setCompanyName(led.getCompanyName());
            dto.setCompanyAddress(led.getCompanyAddress());
            dto.setInvoiceRate(led.getInvoiceRate());
            dto.setInvoiceRateValidateFromDate(led.getInvoiceRateValidateFromDate());
            dto.setInvoiceRateValidateToDate(led.getInvoiceRateValidateToDate());
            dto.setInvoiceLedger(led.getInvoiceLedger());
            dto.setInvoiceCreationType(led.getInvoiceCreationType());
            dto.setType(led.getType());
            dto.setIsJournalEntryPossible(led.getIsJournalEntryPossible());
            dto.setInvoiceCreationType(led.getInvoiceCreationType());
            dto.setEndClientName(led.getEndClientName());
            if (led.getIsEmployee().equalsIgnoreCase("Y") &&
                    led.getLabel().equalsIgnoreCase("employee") &&
                    led.getInvoiceRateValidateFromDate()!=null &&
                    led.getInvoiceRateValidateToDate()!=null) {
                List<DateRange> dateRanges = findMissingTimeSheetRanges(led, led.getInvoiceRateValidateFromDate(), LocalDate.now());
                String result = dateRanges.stream()
                        .map(dr -> dr.getStartDate().format(dateFormatter) + " - " + dr.getEndDate().format(dateFormatter))
                        .collect(Collectors.joining("\n"));

                dto.setMissingTimsheet(result);
            }
            retListDto.add(dto);
        }
        model.addAttribute("ledgers", retListDto);
        model.addAttribute("totalCount", retListDto.size());
        //model.addAttribute("totalAmountRate", totalAmountRateMap.get("Megson"));// ✅ ADD THIS
        model.addAttribute("ledgerTotals", totalAmountRateMap);


        BigDecimal ledgerTotalAmount = totalAmountRateMap.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("ledgerTotalAmount", ledgerTotalAmount);


        return "ledger-List";
    }


    public String getEventConfiguration(String eventName, Ledger led, Set<String> ledset,
                                        LedgerDto ledgerDto, boolean updateDto, Map<String, BigDecimal> totalAmountRateMap) {
        String retString = "";
        Event invoicePaymentEvent = Util.getConfigEvent(led, eventName);
        if (invoicePaymentEvent != null) {
            List<EventAction> listEventActions = invoicePaymentEvent.getEventConfig().getEventAction();
            Iterator<EventAction> itlistEventActions = listEventActions.iterator();
            if (updateDto) {
                ledgerDto.setCompanyHourRate(led.getInvoiceRate());
            }
            while (itlistEventActions.hasNext()) {
                EventAction eventAction = itlistEventActions.next();
                if (eventAction != null) {
                    if (updateDto) {
                        ledgerDto.setCompanyHourRate(
                                ledgerDto.getCompanyHourRate().subtract(eventAction.getAmountRatePerHour()));

                        if (totalAmountRateMap.get(eventAction.getToLedgerName()) == null) {
                            totalAmountRateMap.put(eventAction.getToLedgerName(), new BigDecimal(0));
                        }
                        totalAmountRateMap.put(eventAction.getToLedgerName(),
                                totalAmountRateMap.get(eventAction.getToLedgerName()).add(eventAction.getAmountRatePerHour()));


                    }

                    retString = (retString + "|" + eventName + ": "
                            + appendError(eventAction.getFromLedgerName(),ledset) + "=>"
                            + appendError(eventAction.getToLedgerName(), ledset) + ":"
                            + eventAction.getType() + ":"
                            + eventAction.getAmountRatePerHour() + "|");
                }
            }

            if (updateDto) {
                if (totalAmountRateMap.get("Megson-AP") == null) {
                    totalAmountRateMap.put("Megson-AP", new BigDecimal(0));
                }
                totalAmountRateMap.put("Megson-AP", totalAmountRateMap.get("Megson-AP").add(ledgerDto.getCompanyHourRate()));
            }
        }


        return retString;
    }

    private String appendError(String ledgerName, Set<String> ledset) {
        if (ledset.contains(ledgerName)) {
            return ledgerName;
        } else {

            return ledgerName + "___ERROR__";
        }
    }


    public List<DateRange> findMissingTimeSheetRanges(
            Ledger employee,
            LocalDate startDate,
            LocalDate endDate
    ) {

        List<TimeSheet> sheets =
                timesheetRepository
                        .findByEmployeeAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateAsc(
                                employee, endDate, startDate
                        );

        List<DateRange> missingRanges = new ArrayList<>();

        // ✅ No timesheets at all → full range missing
        if (sheets.isEmpty()) {
            missingRanges.add(new DateRange(startDate, endDate));
            return missingRanges;
        }

        // ✅ Check gap before first entry
        LocalDate firstStart = sheets.get(0).getStartDate();
        if (startDate.isBefore(firstStart)) {
            missingRanges.add(
                    new DateRange(startDate, firstStart.minusDays(1))
            );
        }

        // ✅ Gaps between entries
        for (int i = 1; i < sheets.size(); i++) {
            LocalDate prevEnd = sheets.get(i - 1).getEndDate();
            LocalDate currStart = sheets.get(i).getStartDate();

            LocalDate gapStart = prevEnd.plusDays(1);
            LocalDate gapEnd = currStart.minusDays(1);

            if (!gapStart.isAfter(gapEnd)) {
                missingRanges.add(new DateRange(gapStart, gapEnd));
            }
        }

        // ✅ Check gap after last entry
        LocalDate lastEnd = sheets.get(sheets.size() - 1).getEndDate();
        if (endDate.isAfter(lastEnd)) {
            missingRanges.add(
                    new DateRange(lastEnd.plusDays(1), endDate)
            );
        }

        return missingRanges;
    }



}
