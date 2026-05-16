
package com.act.web.controller;

import com.act.dto.DateRange;
import com.act.dto.LedgerDto;
import com.act.dto.TransactionDto;
import com.act.json.model.Event;
import com.act.json.model.EventAction;
import com.act.model.Ledger;
import com.act.model.LedgerDocument;
import com.act.model.TimeSheet;
import com.act.model.Transaction;
import com.act.repo.LedgerDocumentRepository;
import com.act.repo.LedgerRepository;
import com.act.repo.TimesheetRepository;
import com.act.repo.TrasactionRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpSession;
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

    private final LedgerDocumentRepository ledgerDocumentRepository;

    public LedgerController(
            LedgerRepository ledgerRepository,
            TrasactionRepository trasactionRepository,
            TimesheetRepository timesheetRepository, LedgerDocumentRepository ledgerDocumentRepository
    ) {
        this.ledgerRepository = ledgerRepository;
        this.trasactionRepository = trasactionRepository;
        this.timesheetRepository = timesheetRepository;
        this.ledgerDocumentRepository = ledgerDocumentRepository;
    }


    @GetMapping("/edit/{id}")
    public String showEditLedgerForm(Model model, @PathVariable long id) {

        Optional<Ledger> t = ledgerRepository.findById(id);
        model.addAttribute("ledger", t.get());

        List<Ledger> clients = ledgerRepository.findByIsEmployeeAndTypeAndLabelOrderByLedgerNameAsc("N", "Asset", "AR");
        model.addAttribute("clients", clients);
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

        List<Ledger> clients = ledgerRepository.findByIsEmployeeAndTypeAndLabelOrderByLedgerNameAsc("N", "Asset", "AR");
        model.addAttribute("clients", clients);
        return "ledger-add";
    }

    // Handle submit
    @PostMapping("/add")
    public String saveLedger(@ModelAttribute Ledger ledger,
                             HttpSession session) {

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
            t.get().setNotes(ledger.getNotes());
            ledgerRepository.save(t.get());

            return getRedirectURL("redirect:/api/v1/ledger/list?success=" + t.get().getLedgerName(), session);
        } else {
            ledger.setBalance(new BigDecimal(0));
            ledger.setBalanceUpdateDate(LocalDateTime.now());
            ledgerRepository.save(ledger);
            return getRedirectURL("redirect:/api/v1/ledger/list?success=" + ledger.getLedgerName(), session);

        }


    }

    private String getRedirectURL(String baseUrl,  HttpSession session) {
        HashMap<String, String> mapParams = (HashMap) session.getAttribute("lastLedgerQuery");

        StringBuilder redirectUrl =
                new StringBuilder(baseUrl);

        if (mapParams!=null) {
            for (Map.Entry<String, String> entry : mapParams.entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    redirectUrl.append("&")
                            .append(entry.getKey())
                            .append("=")
                            .append(entry.getValue());
                }
            }
        }
        return redirectUrl.toString();
    }

    @GetMapping("/list")
    public String listLedgers(
            @RequestParam(name = "label", required = false) String label,
            @RequestParam(name = "endClient", required = false) String endClient,
            @RequestParam(name = "invoiceLedger", required = false) String invoiceLedger,
            //0==>include no expired ledger, 1==>include all, 2==> only expired
            @RequestParam(name = "includeExpiredLedger", required = false, defaultValue = "0") String includeExpiredLedger,
            @RequestParam(name = "viewName", required = false, defaultValue = "ledger-List") String viewName,
            HttpSession session,
            Model model) {

        Map<String, String > mapParams =new HashMap<>();
        mapParams.put("label", label);
        mapParams.put("endClient", endClient);
        mapParams.put("invoiceLedger", invoiceLedger);
        mapParams.put("viewName", viewName);
        mapParams.put("includeExpiredLedger", includeExpiredLedger);
        session.setAttribute("lastLedgerQuery", mapParams);

        DateTimeFormatter dateFormatter =
                DateTimeFormatter.ofPattern("MM/dd/yyyy");
        List<Ledger> retList = ledgerRepository.findAllByOrderByLedgerNameAsc();
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

            if (includeExpiredLedger.equalsIgnoreCase("0")) {
                if (led.getInvoiceRateValidateToDate()!=null && LocalDate.now().isAfter(led.getInvoiceRateValidateToDate())) {
                    continue;
                }
            }
            if (includeExpiredLedger.equalsIgnoreCase("2")) {
                if (led.getInvoiceRateValidateToDate()!=null && !LocalDate.now().isAfter(led.getInvoiceRateValidateToDate())) {
                    continue;
                }
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
            dto.setNotes(led.getNotes());
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
                List<DateRange> dateRanges = findMissingTimeSheetRanges(led, led.getInvoiceRateValidateFromDate(),
                        (LocalDate.now().isBefore(led.getInvoiceRateValidateToDate())) ? LocalDate.now():led.getInvoiceRateValidateToDate());
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


        return viewName;
    }


    public String getEventConfiguration(String eventName, Ledger led, Set<String> ledset,
                                        LedgerDto ledgerDto, boolean updateDto, Map<String, BigDecimal> totalAmountRateMap) {
        String retString = "";
        Event invoicePaymentEvent = Util.getConfigEvent(led, eventName);
        if (invoicePaymentEvent != null) {
            List<EventAction> listEventActions = invoicePaymentEvent.getEventConfig().getEventAction();
            Iterator<EventAction> itlistEventActions = listEventActions.iterator();
            if (updateDto && LocalDate.now().isAfter(led.getInvoiceRateValidateToDate())) {
                updateDto = false;
            }
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


    /* ================= LIST ================= */

    @Transactional
    @GetMapping("/{ledgerId}/documents")
    public String showDocuments(
            @PathVariable Long ledgerId,
            HttpSession session,
            Model model) {

        Ledger ledger = ledgerRepository.findById(ledgerId).orElseThrow();

        model.addAttribute("ledger", ledger);
        model.addAttribute(
                "documents",
                ledgerDocumentRepository.findByLedgerOrderByExpiryDateAsc(ledger)
        );
        model.addAttribute("newDocument", new LedgerDocument());
        model.addAttribute("ledgerListEndpoint",getRedirectURL("/api/v1/ledger/list?success=" + ledger.getLedgerName(), session));



        return "ledger-documents";
    }

    @Transactional
    @GetMapping("/documents/expiring-soon")
    public String showExpiringDocuments(Model model) {

        LocalDate today = LocalDate.now();
        LocalDate fourMonthsLater = today.plusMonths(4);

        List<LedgerDocument> docs =
                ledgerDocumentRepository.findExpiringActiveDocs(today, fourMonthsLater);
        List<LedgerDocument>  retDocs = new ArrayList<>();
        for (LedgerDocument doc : docs) {
            if (LocalDate.now().isBefore(doc.getLedger().getInvoiceRateValidateToDate())) {
                retDocs.add(doc);
            }
        }

        model.addAttribute("documents", retDocs);

        return "ledger-documents-list.html";
    }

    /* ================= ADD ================= */


    @Transactional
    @PostMapping("/{ledgerId}/documents/add")
    public String addDocument(
            @PathVariable Long ledgerId,
            @RequestParam("file") MultipartFile file,
            @ModelAttribute LedgerDocument newDocument) throws Exception {

        Ledger ledger = ledgerRepository.findById(ledgerId).orElseThrow();


        newDocument.setLedger(ledger);
        newDocument.setCreatedDate(LocalDate.now());

        // ✅ SAVE FILE CONTENT
        newDocument.setDocumentContent(file.getBytes());

        // ✅ SAVE ORIGINAL FILE NAME
        newDocument.setFileName(file.getOriginalFilename());

        // ✅ SAVE CONTENT TYPE
        newDocument.setContentType(file.getContentType());



        ledgerDocumentRepository.save(newDocument);

        return "redirect:/api/v1/ledger/" + ledgerId + "/documents";
    }


    /* ================= DELETE ================= */


    @Transactional
    @PostMapping("/{ledgerId}/documents/delete/{docId}")
    public String deleteDocument(
            @PathVariable Long ledgerId,
            @PathVariable Long docId) {

        ledgerDocumentRepository.deleteById(docId);

        return "redirect:/api/v1/ledger/" + ledgerId + "/documents";
    }



    @Transactional
    @GetMapping("/documents/download/{docId}")
    public ResponseEntity<byte[]> downloadDocument(@PathVariable Long docId) {

        LedgerDocument doc = ledgerDocumentRepository.findById(docId).orElseThrow();

        return ResponseEntity.ok()
                // ✅ USE ORIGINAL FILE NAME
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.getFileName() + "\"")

                // ✅ USE CORRECT MIME TYPE
                .contentType(MediaType.parseMediaType(doc.getContentType()))

                .body(doc.getDocumentContent());
    }



}
