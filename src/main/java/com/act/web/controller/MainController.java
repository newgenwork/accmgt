package com.act.web.controller;

import com.act.dto.InvoiceFilter;
import com.act.dto.InvoiceMasterDto;
import com.act.dto.TransactionDto;
import com.act.json.model.Config;
import com.act.json.model.Event;
import com.act.json.model.EventAction;
import com.act.json.model.LocalDateAdapter;
import com.act.model.*;
import com.act.repo.*;
import com.act.service.impl.UserServiceImpl;
import com.act.web.dto.MyUserRegistrationDto;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.List;

@Controller
@RequestMapping("api/v1/")
public class MainController {

    private final LedgerRepository ledgerRepository;
    private final InvoiceMasterRepository invoiceMasterRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final TrasactionRepository trasactionRepository;
    private final UserRepository userRepository;
    private final TimesheetRepository timesheetRepository;

    @Autowired
    private  SequenceRepository sequenceRepository;
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    public MainController(LedgerRepository ledgerRepository, InvoiceMasterRepository invoiceMasterRepository,
                          JournalEntryRepository journalEntryRepository, TrasactionRepository trasactionRepository, UserRepository userRepository, TimesheetRepository timesheetRepository) {
        this.ledgerRepository = ledgerRepository;
        this.invoiceMasterRepository = invoiceMasterRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.trasactionRepository = trasactionRepository;
        this.userRepository = userRepository;
        //this.sequenceRepository = sequenceRepository;
        this.timesheetRepository = timesheetRepository;
    }


    @GetMapping("/login")
    public String getLoginPage(Model model) {
        return "login";
    }

    @GetMapping
    public String getHomePage() {
        return "index";
    }


    void updateBalanceandTransaction(BigDecimal totalAmount,
                                     InvoiceMaster invoiceMaster,
                                     InvoiceDetail invoiceDetail,
                                     JournalEntry journalEntry,
                                     String from,
                                     String to,
                                     String desc,
                                     LocalDate transactionDate,
                                     boolean skipdeleteTransactions) {
        if (!skipdeleteTransactions) {
            Optional<List<Transaction>> retJeList = null;
            if (invoiceMaster != null && invoiceMaster.getId() != null) {
                retJeList = trasactionRepository.findByInvoiceMaster(invoiceMaster);
            }
            if (journalEntry != null) {
                retJeList = trasactionRepository.findByJournalEntry(journalEntry);
            }

            if (retJeList != null && retJeList.isPresent()) {
                Iterator<Transaction> ita = retJeList.get().iterator();
                while (ita.hasNext()) {
                    trasactionRepository.deleteById(ita.next().getId());
                }
            }
        }
        Transaction transaction = new Transaction();
        transaction.setAccount(ledgerRepository.findByLedgerName(from).get());
        transaction.setAmount(totalAmount.multiply(new BigDecimal(-1)));
        transaction.setDescription(desc + " : flow : "+ from + " ==> " + to);
        transaction.setTransactionDate(transactionDate);
        transaction.setInvoiceMaster(invoiceMaster);
        transaction.setJournalEntry(journalEntry);
        transaction.setInvoiceDetail(invoiceDetail);
        trasactionRepository.save(transaction);

        Transaction transactionNew = new Transaction();
        transactionNew.setAccount(ledgerRepository.findByLedgerName(to).get());
        transactionNew.setAmount(totalAmount);
        transactionNew.setDescription(desc + " : flow : "+ from + " ==> " + to);
        transactionNew.setTransactionDate(transactionDate);
        transactionNew.setInvoiceMaster(invoiceMaster);
        transactionNew.setJournalEntry(journalEntry);
        transaction.setInvoiceDetail(invoiceDetail);
        trasactionRepository.save(transactionNew);
    }

    Event getConfigEvent(Ledger ledger, String type) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .setPrettyPrinting()
                .create();

        Config config = gson.fromJson(ledger.getConfig(), Config.class);

        Iterator<Event> it = config.getEvents().iterator();

        Event toApply = null;
        while (it.hasNext()) {
            Event event = it.next();
            if (event.getName().equals(type)) {
                LocalDate today = LocalDate.now();   // Current date
                boolean isBetween = (!today.isBefore(event.getEventConfig().getValidFrom()))
                        && (!today.isAfter(event.getEventConfig().getValidTo()));
                if (isBetween) {
                    toApply = event;
                    break;
                }
            }
        }
        return toApply;
    }

    @GetMapping("/ledger/edit/{id}")
    public String showEditLedgerForm(Model model, @PathVariable long id) {

        Optional<Ledger> t = ledgerRepository.findById(id);
        model.addAttribute("ledger", t.get());

        Optional<List<Ledger>> clients = ledgerRepository.findByIsEmployeeAndTypeAndLabel("N", "Asset", "AR");
        model.addAttribute("clients", clients.get());
        return "ledger-add";
    }
    @GetMapping("/ledger/statement/{id}")
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

    //employees
    // Show form
    @GetMapping("/ledger/add")
    public String showAddLedgerForm(Model model) {
        model.addAttribute("ledger", new Ledger());

        Optional<List<Ledger>> clients = ledgerRepository.findByIsEmployeeAndTypeAndLabel("N", "Asset", "AR");
        model.addAttribute("clients", clients.get());
        return "ledger-add";
    }

    // Handle submit
    @PostMapping("/ledger/add")
    public String saveLedger(@ModelAttribute Ledger ledger) {
        Optional<Ledger> t = ledgerRepository.findByLedgerName(ledger.getLedgerName());
        if (t.isPresent()) {
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


    @GetMapping("/ledger/list")
    public String listLedgers(Model model) {
        model.addAttribute("ledgers", ledgerRepository.findAll());
        return "ledger-List";
    }



    @GetMapping("/invoicesMaster/edit/{reference}")
    public String showEditInvoiceForm(Model model, @PathVariable String reference) {
        Optional<InvoiceMaster> t = invoiceMasterRepository.findByReference(reference);

        t.get().getDetails().size();

        model.addAttribute("invoiceMaster", t.get());

        Optional<List<Ledger>> clients = ledgerRepository.findByIsEmployeeAndTypeAndLabel("N", "Asset", "AR");
        model.addAttribute("clients", clients.get());
        Optional<List<Ledger>> employees = ledgerRepository.findByIsEmployeeAndType("Y", "Expense");
        model.addAttribute("employees", employees.get());


        return "invoiceMaster-add-withFilter";
    }

    //InvoiceMaster
    @Transactional
    @GetMapping("/invoicesMaster/add")
    public String showAddInvoiceForm(Model model) {
        InvoiceMaster invoiceMaster = new InvoiceMaster();
        invoiceMaster.setDetails(new ArrayList<>());
        invoiceMaster.getDetails().add(new InvoiceDetail());
        //invoiceMaster.setReference(UUID.randomUUID().toString());
        invoiceMaster.setReference("INV-" + sequenceRepository.getNextInvoiceSequence().toString());
        invoiceMaster.setStatus("DRAFT");
        model.addAttribute("invoiceMaster", invoiceMaster);

        Optional<List<Ledger>> clients = ledgerRepository.findByIsEmployeeAndTypeAndLabel("N", "Asset", "AR");
        model.addAttribute("clients", clients.get());
        Optional<List<Ledger>> employees = ledgerRepository.findByIsEmployeeAndType("Y", "Expense");
        model.addAttribute("employees", employees.get());


        return "invoiceMaster-add";
    }

    @Transactional
    @GetMapping("/invoicesMaster/filter")
    public String showAddInvoiceFilterForm(Model model) {
        model.addAttribute("invoiceFilter", new InvoiceFilter());
        Optional<List<Ledger>> clients = ledgerRepository.findByIsEmployeeAndTypeAndLabel("N", "Asset", "AR");
        model.addAttribute("clients", clients.get());

        return "invoiceMaster-filter";
    }

    @Transactional
    @PostMapping("/invoicesMaster/filter")
    public String acceptAddInvoiceFilterForm(@ModelAttribute InvoiceFilter invoiceFilter, Model model) {

        Ledger ledgerAR = ledgerRepository.findById(invoiceFilter.getClientId()).get();
        Optional<List<Ledger>> finAllEmployeesforAR = ledgerRepository.findByIsEmployeeAndTypeAndInvoiceLedger(
                "Y", "Expense", ledgerAR);
        InvoiceMaster invoiceMaster = new InvoiceMaster();
        invoiceMaster.setDetails(new ArrayList<>());

        Iterator<Ledger> it = finAllEmployeesforAR.get().iterator();
        while (it.hasNext()) {
            Ledger local = it.next();
            InvoiceDetail invd = new InvoiceDetail();
            invd.setId(local.getId());
            invd.setRate(local.getInvoiceRate());
            invd.setEmployee(local);
            invd.setNoOfHrs(timesheetRepository.getTotalHoursByEmployeeAndDateRange(local.getId(),
                    invoiceFilter.getStartDate(),invoiceFilter.getEndDate()));
            if (invd.getNoOfHrs().compareTo(BigDecimal.ZERO) ==0) {
                return "redirect:/api/v1/invoicesMaster/filter?error=Employee Not having hrs for " + local.getLedgerName() ;
            }

            invd.setStartDate(invoiceFilter.getStartDate());
            invd.setEndDate(invoiceFilter.getEndDate());


            invoiceMaster.getDetails().add(invd);
        }
        invoiceMaster.setReference("INV-" + sequenceRepository.getNextInvoiceSequence().toString());
        invoiceMaster.setStatus("INIT");
        invoiceMaster.setClient(ledgerAR);
        model.addAttribute("invoiceMaster", invoiceMaster);

        List<Ledger> clients = new ArrayList<>();
        clients.add(ledgerAR);

        model.addAttribute("clients", clients);
        Optional<List<Ledger>> employees = ledgerRepository.findByIsEmployeeAndType("Y", "Expense");
        model.addAttribute("employees", employees.get());

        return "invoiceMaster-add-withFilter";
    }

//http://localhost:8080/api/v1/invoicesMaster/add/4
    @GetMapping("/invoicesMaster/add/{ar}")
    String showAddInvoiceFormWithledgerAR(
            @PathVariable("ar") Long ar,
            Model model) {

        Ledger ledgerAR = ledgerRepository.findById(ar).get();

        Optional<List<Ledger>> finAllEmployeesforAR = ledgerRepository.findByIsEmployeeAndTypeAndInvoiceLedger(
                "Y", "Expense", ledgerAR);

        InvoiceMaster invoiceMaster = new InvoiceMaster();
        invoiceMaster.setDetails(new ArrayList<>());

        Iterator<Ledger> it = finAllEmployeesforAR.get().iterator();
        while (it.hasNext()) {
            Ledger local = it.next();
            InvoiceDetail invd = new InvoiceDetail();
            invd.setId(local.getId());
            invd.setRate(local.getInvoiceRate());
            invd.setEmployee(local);

            invd.setStartDate(LocalDate.now()
                    .with(TemporalAdjusters.firstDayOfMonth()));
            invd.setEndDate(LocalDate.now()
                    .with(TemporalAdjusters.lastDayOfMonth()));


            invoiceMaster.getDetails().add(invd);
        }
        invoiceMaster.setReference("INV-" + sequenceRepository.getNextInvoiceSequence().toString());
        invoiceMaster.setStatus("DRAFT");
        model.addAttribute("invoiceMaster", invoiceMaster);

        List<Ledger> clients = new ArrayList<>();
        clients.add(ledgerAR);

        model.addAttribute("clients", clients);
        Optional<List<Ledger>> employees = ledgerRepository.findByIsEmployeeAndType("Y", "Expense");
        model.addAttribute("employees", employees.get());


        return "invoiceMaster-add";
    }

    // Handle submit
    @Transactional
    @PostMapping("/invoicesMaster/add")
    public String saveInvoiceMaster(@ModelAttribute InvoiceMaster invoiceMaster, @RequestParam("action") String action) {



        Optional<InvoiceMaster> t = invoiceMasterRepository.findByReference(invoiceMaster.getReference());
        InvoiceMaster invoiceMasterEdit = null;
        if (t.isPresent()) {
             invoiceMasterEdit = t.get();
            invoiceMasterEdit.setInvoiceDate(invoiceMaster.getInvoiceDate());
            if (action.equals("save") && !invoiceMasterEdit.getStatus().equals("DRAFT")) {
                return "redirect:/api/v1/invoicesMaster/list?success";
            }
            if (action.equals("submit") && !invoiceMasterEdit.getStatus().equals("DRAFT")) {
                return "redirect:/api/v1/invoicesMaster/list?success";
            }
            if (action.equals("receivePayment") && !invoiceMasterEdit.getStatus().equals("SUBMITTED")) {
                return "redirect:/api/v1/invoicesMaster/list?success";
            }
            if (action.equals("receivePayment") && invoiceMaster.getReceivedDate() == null) {
                return "redirect:/api/v1/invoicesMaster/list?success";
            }
            if (action.equals("submit")) {
                invoiceMaster.setStatus("SUBMITTED");
            }
            if (action.equals("receivePayment")) {
                invoiceMaster.setStatus("PAID");
                invoiceMasterEdit.setReceivedDate(invoiceMaster.getReceivedDate());
            }
            invoiceMasterEdit.setStatus(invoiceMaster.getStatus());
            if (invoiceMaster.getDetails() != null) {
                for (int i = invoiceMaster.getDetails().size() - 1; i >= 0; i--) {
                    if (invoiceMaster.getDetails().get(i).getEmployee() == null) {
                        invoiceMaster.getDetails().remove(i);
                    }

                }

                int index = invoiceMaster.getDetails().size() - 1;

                for (int i = invoiceMasterEdit.getDetails().size() - 1; i > index; i--) {
                    invoiceMasterEdit.getDetails().remove(i);
                }

                for (int i = 0; i < invoiceMaster.getDetails().size(); i++) {
                    if (invoiceMaster.getDetails().get(i) != null) {
                        if (invoiceMasterEdit.getDetails().size() <= i) {
                            invoiceMasterEdit.getDetails().add(new InvoiceDetail());
                        }
                        invoiceMasterEdit.getDetails().get(i).setInvoiceMaster(invoiceMasterEdit);
                        invoiceMasterEdit.getDetails().get(i).setAmount(invoiceMaster.getDetails().get(i).getAmount());
                        invoiceMasterEdit.getDetails().get(i).setEmployee(invoiceMaster.getDetails().get(i).getEmployee());
                        invoiceMasterEdit.getDetails().get(i).setRate(invoiceMaster.getDetails().get(i).getRate());
                        invoiceMasterEdit.getDetails().get(i).setEndDate(invoiceMaster.getDetails().get(i).getEndDate());
                        invoiceMasterEdit.getDetails().get(i).setNoOfHrs(invoiceMaster.getDetails().get(i).getNoOfHrs());
                        invoiceMasterEdit.getDetails().get(i).setStartDate(invoiceMaster.getDetails().get(i).getStartDate());

                        List<TimeSheet> tsList = timesheetRepository.findCollidingTimeSheets(invoiceMaster.getDetails().get(i).getEmployee(),
                                invoiceMaster.getDetails().get(i).getStartDate(),
                                invoiceMaster.getDetails().get(i).getEndDate(), null);

                        for (TimeSheet timeSheet : tsList) {
                            timeSheet.setInvoiceDetail(invoiceMasterEdit.getDetails().get(i));
                            timesheetRepository.save(timeSheet);
                        }


                    } else {
                        break;
                    }
                }

            } else {
                invoiceMasterEdit.getDetails().clear();
            }
            invoiceMasterEdit.setClient(invoiceMaster.getClient());
            invoiceMasterRepository.save(invoiceMasterEdit);

        } else {

            if (action.equals("receivePayment") && !invoiceMaster.getStatus().equals("SUBMITTED")) {
                return "redirect:/api/v1/invoicesMaster/list?success";
            }
            if (action.equals("submit")) {
                invoiceMaster.setStatus("SUBMITTED");
            }

            if (invoiceMaster.getClient().getInvoiceCreationType().equalsIgnoreCase("Single")) {
                String refs = "";
                for (int i = 0; i < invoiceMaster.getDetails().size(); i++) {
                    InvoiceMaster invoiceMasterLocal = new InvoiceMaster();
                    invoiceMasterLocal.setClient(invoiceMaster.getClient());
                    invoiceMasterLocal.setReference("INV-" + sequenceRepository.getNextInvoiceSequence().toString());
                    if (refs.equalsIgnoreCase("")){
                        refs = invoiceMasterLocal.getReference();
                    }else {
                        refs = refs + "," + invoiceMasterLocal.getReference();
                    }
                    invoiceMasterLocal.setInvoiceDate(invoiceMaster.getInvoiceDate());
                    invoiceMasterLocal.setStatus(invoiceMaster.getStatus());
                    invoiceMasterLocal.setReceivedDate(invoiceMaster.getReceivedDate());

                    if (invoiceMaster.getDetails().get(i).getEmployee() != null) {
                        if (invoiceMasterLocal.getDetails() == null) {
                            invoiceMasterLocal.setDetails(new ArrayList<InvoiceDetail>());
                        }
                        InvoiceDetail invd = new InvoiceDetail();
                        invoiceMasterLocal.getDetails().add(invd);

                        invd.setAmount(invoiceMaster.getDetails().get(i).getAmount());
                        invd.setEmployee(invoiceMaster.getDetails().get(i).getEmployee());
                        invd.setRate(invoiceMaster.getDetails().get(i).getRate());
                        invd.setEndDate(invoiceMaster.getDetails().get(i).getEndDate());
                        invd.setNoOfHrs(invoiceMaster.getDetails().get(i).getNoOfHrs());
                        invd.setStartDate(invoiceMaster.getDetails().get(i).getStartDate());
                        invd.setInvoiceMaster(invoiceMasterLocal);
                        if (invoiceMasterLocal.getStatus().equalsIgnoreCase("INIT")) {
                            invoiceMasterLocal.setStatus("DRAFT");
                        }
                        invoiceMasterRepository.save(invoiceMasterLocal);
                    }
                    //invoiceMasterRepository.save(invoiceMasterLocal);
                }

                return "redirect:/api/v1/invoicesMaster/list?success=" + refs   ;
            }
            else {
                invoiceMasterEdit = new InvoiceMaster();
                invoiceMasterEdit.setClient(invoiceMaster.getClient());
                invoiceMasterEdit.setReference(invoiceMaster.getReference());
                invoiceMasterEdit.setInvoiceDate(invoiceMaster.getInvoiceDate());
                invoiceMasterEdit.setStatus(invoiceMaster.getStatus());
                invoiceMasterEdit.setReceivedDate(invoiceMaster.getReceivedDate());
                //invoiceMasterEdit.setId(invoiceMaster.getId());

                long line = 0;
                for (int i = 0; i < invoiceMaster.getDetails().size(); i++) {
                    if (invoiceMaster.getDetails().get(i).getEmployee() != null) {
                        if (invoiceMasterEdit.getDetails() == null) {
                            invoiceMasterEdit.setDetails(new ArrayList<InvoiceDetail>());
                        }
                        InvoiceDetail invd = new InvoiceDetail();
                        invoiceMasterEdit.getDetails().add(invd);
                        //line = line + 1;
                        //invd.setId(line);
                        invd.setAmount(invoiceMaster.getDetails().get(i).getAmount());
                        invd.setEmployee(invoiceMaster.getDetails().get(i).getEmployee());
                        invd.setRate(invoiceMaster.getDetails().get(i).getRate());
                        invd.setEndDate(invoiceMaster.getDetails().get(i).getEndDate());
                        invd.setNoOfHrs(invoiceMaster.getDetails().get(i).getNoOfHrs());
                        invd.setStartDate(invoiceMaster.getDetails().get(i).getStartDate());
                        invd.setInvoiceMaster(invoiceMasterEdit);
                    }

                }
                invoiceMasterRepository.save(invoiceMasterEdit);

            }

        }


        if (action.equals("submit")) {
            Event toApply = getConfigEvent(invoiceMasterEdit.getClient(), "invoice");
            if (toApply == null){
                throw new RuntimeException("No Event Found. Please configure the ledger config event for invoice Event / " + invoiceMasterEdit.getClient().getLedgerName());
            }
            if (toApply != null) {
                Iterator<EventAction> itAction = toApply.getEventConfig().getEventAction().iterator();
                while (itAction.hasNext()) {
                    EventAction ea = itAction.next();

                    BigDecimal totalAmount =
                            invoiceMasterEdit.getDetails().stream()
                                    .map(InvoiceDetail::getAmount)
                                    .filter(Objects::nonNull)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                    updateBalanceandTransaction(
                            totalAmount,
                            invoiceMasterEdit,
                            null,
                            null,
                            ea.getFromLedgerName(),
                            ea.getToLedgerName(),
                            "invoice (Submit Event) : ref : " + invoiceMasterEdit.getReference() ,
                            invoiceMasterEdit.getInvoiceDate(),
                            true);

                }
            }
        }

        if (action.equals("receivePayment")) {
            Event toApply = getConfigEvent(invoiceMasterEdit.getClient(), "invoicePayment");
            if (toApply == null){
                throw new RuntimeException("No Event Found. Please configure the ledger config event for invoice Payment Event /" + invoiceMasterEdit.getClient().getLedgerName());
            }
            if (toApply != null) {
                Iterator<EventAction> itAction = toApply.getEventConfig().getEventAction().iterator();
                while (itAction.hasNext()) {
                    EventAction ea = itAction.next();

                    BigDecimal totalAmount =
                            invoiceMaster.getDetails().stream()
                                    .map(InvoiceDetail::getAmount)
                                    .filter(Objects::nonNull)
                                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                    updateBalanceandTransaction(totalAmount,
                            invoiceMasterEdit,
                            null,
                            null,
                            ea.getFromLedgerName(),
                            ea.getToLedgerName(),
                            "receivePayment Event (MasterEntry) from "
                                    + invoiceMasterEdit.getClient().getLedgerName()
                                    + " : ref : "
                                +invoiceMasterEdit.getReference()
                                + " : inv Date : "
                                + invoiceMasterEdit.getInvoiceDate()
                                    ,
                            invoiceMasterEdit.getReceivedDate(),
                            true);

                }
            }

            {
                Iterator<InvoiceDetail> iterator = invoiceMasterEdit.getDetails().iterator();
                while (iterator.hasNext()) {

                    InvoiceDetail invd = iterator.next();
                    Event toApplyLocal = getConfigEvent(invd.getEmployee(), "invoicePayment");
                    if (toApply == null){
                        throw new RuntimeException("No Event Found. Please configure the ledger config event for invoice Payment Event /" + invd.getEmployee().getLedgerName());
                    }
                    Iterator<EventAction> itAction = toApplyLocal.getEventConfig().getEventAction().iterator();
                    while (itAction.hasNext()) {
                        if (toApplyLocal != null) {
                            EventAction ea = itAction.next();

                            BigDecimal amt = BigDecimal.ZERO;
                            if (ea.getType().equals("amountRatePerHour")) {
                                 amt = ea.getAmountRatePerHour().multiply(invd.getNoOfHrs());
                            }

                            if (amt != null && amt.compareTo(BigDecimal.ZERO) != 0) {
                                // amt is NOT zero
                                updateBalanceandTransaction(amt,
                                        invoiceMasterEdit,
                                        invd,
                                        null,
                                        ea.getFromLedgerName(),
                                        ea.getToLedgerName(),
                                        "receivePayment Event (LineEntry) from Name: "
                                                + invd.getEmployee().getLedgerName()
                                                + " : Hrs :"
                                                + invd.getNoOfHrs()
                                                + " : StartDate : "
                                                + invd.getStartDate()
                                                + " : End date : "
                                                + invd.getEndDate()
                                                + " : Invoice Ref : "
                                        +invoiceMasterEdit.getReference(),

                                        invoiceMasterEdit.getReceivedDate(),
                                        true);
                            }

                        }
                    }


                }
            }

        }


        //return "redirect:/employees";  // redirect to list page
        return "redirect:/api/v1/invoicesMaster/list?success=" + invoiceMasterEdit.getReference();
    }


    @GetMapping("/invoicesMaster/list")
    public String listMasterList(Model model) {

        List<InvoiceMaster> dd = invoiceMasterRepository.findAll();
        dd.sort(Comparator.comparing(InvoiceMaster::getInvoiceDate).reversed());

        List<InvoiceMasterDto> retList = new ArrayList<InvoiceMasterDto>();

        Iterator<InvoiceMaster> it = dd.iterator();

        while (it.hasNext()) {
            InvoiceMaster item = it.next();
            InvoiceMasterDto dto = new InvoiceMasterDto();
            dto.setId(item.getId());
            dto.setNotes(item.getNotes());
            dto.setInvoiceDate(item.getInvoiceDate());
            dto.setReceivedDate(item.getReceivedDate());
            dto.setReference(item.getReference());
            dto.setStatus(item.getStatus());
            dto.setClient(item.getClient());

            BigDecimal totalAmount =
                    item.getDetails().stream()
                            .map(InvoiceDetail::getAmount)
                            .filter(Objects::nonNull)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            dto.setAmount(totalAmount);
            Iterator<InvoiceDetail> ita = item.getDetails().iterator();
            while (ita.hasNext()) {
                InvoiceDetail localDet = ita.next();
                if (dto.getNotes() == null) {
                    dto.setNotes( localDet.getEmployee().getLedgerName() + "| hrs:" +
                            localDet.getNoOfHrs() +"|St Dt:" +
                            localDet.getStartDate() + "|End Dt:" +
                            localDet.getEndDate() + "|Amt:" +
                                    localDet.getAmount() + "|rate:" +
                            localDet.getRate()  );
                } else {
                    dto.setNotes(dto.getNotes() + "| " + localDet.getEmployee().getLedgerName());
                }
            }
            retList.add(dto);
        }


        model.addAttribute("invoiceMasterList", retList);
        return "invoicesMaster-List";
    }


    @GetMapping("/journal/edit/{id}")
    public String showEditjournalForm(Model model, @PathVariable String id) {
        Optional<JournalEntry> t = journalEntryRepository.findById(id);
        model.addAttribute("journal", t.get());
        Optional<List<Ledger>> clients = ledgerRepository.findByType("Expense");
        model.addAttribute("clients", clients.get());
        return "journal-add";
    }


    //journal
    @GetMapping("/guide")
    public String showGuide(Model model) {


        return "guide";
    }
    //journal
    @GetMapping("/journal/add")
    public String showAddJournalForm(Model model) {
        JournalEntry je = new JournalEntry();
       // je.setId(UUID.randomUUID().toString());
        je.setId("JN-" + sequenceRepository.getNextInvoiceSequence().toString());
        model.addAttribute("journal", je);
        Optional<List<Ledger>> clients = ledgerRepository.findByType("Expense");
        model.addAttribute("clients", clients.get());


        return "journal-add";
    }




    // Handle submit
    @Transactional
    @PostMapping("/journal/add")
    public String saveJournal(@ModelAttribute JournalEntry journalEntry) {
        //from
        //Optional<Ledger> client = ledgerRepository.f
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .setPrettyPrinting()
                .create();

        Config config = gson.fromJson(journalEntry.getTargetAccount().getConfig(), Config.class);

        Iterator<Event> it = config.getEvents().iterator();

        Event toApply = getConfigEvent(journalEntry.getTargetAccount(), journalEntry.getType());
        if (toApply == null){
            throw new RuntimeException("No Event Found. Please configure the ledger config event for  Event /"
                    + journalEntry.getTargetAccount().getLedgerName() + " / " + journalEntry.getType());
        }
        if (toApply != null) {
            Iterator<EventAction> itAction = toApply.getEventConfig().getEventAction().iterator();
            while (itAction.hasNext()) {
                EventAction ea = itAction.next();

                JournalEntry je = null;
                Optional<JournalEntry> optJe = journalEntryRepository.findById(journalEntry.getId());
                if (optJe.isPresent()) {
                    je = optJe.get();
                } else {
                    je = new JournalEntry();
                }
                je.setId(journalEntry.getId());
                je.setTransactionDate(journalEntry.getTransactionDate());
                if (ea.getType().equals("source")) {
                    je.setAmount(journalEntry.getAmount());
                }
                je.setTargetAccount(journalEntry.getTargetAccount());

                je.setType(journalEntry.getType());
                je.setDescription(journalEntry.getDescription());

                journalEntryRepository.save(je);


                updateBalanceandTransaction(je.getAmount(), null, null,
                        je,
                        ea.getFromLedgerName(),
                        ea.getToLedgerName(),
                        je.getDescription() + " : "
                                + ea.getFromLedgerName() + " : "
                                + ea.getToLedgerName() + ":"
                                + je.getTargetAccount().getLedgerName(),
                        je.getTransactionDate(),
                        false);

            }
        }
        //journalEntryRepository.save(journalEntry);
        return "redirect:/api/v1/journal/list?success=" +journalEntry.getId();
    }


    @GetMapping("/journal/list")
    public String listJournalList(Model model) {
        List<JournalEntry> dd = journalEntryRepository.findAll();
        dd.sort(Comparator.comparing(JournalEntry::getTransactionDate).reversed());
        model.addAttribute("journalList", dd);
        return "journal-List";
    }


    @GetMapping("/user/changeProfile")
    public String showChangeProfileForm(Model model) {

        Authentication auth =
                SecurityContextHolder.getContext().getAuthentication();

        String username = auth.getName();

        MyUser user = userRepository.findByEmail(username);
        model.addAttribute("user", user);
        return "changeProfile";
    }
    @Transactional
    @PostMapping("/user/changeProfile")
    public String saveChangeProfileForm(@ModelAttribute MyUser user) {

        MyUser myUser = userRepository.findByEmail(user.getEmail());
        myUser.setPassword(passwordEncoder.encode(user.getPassword()));
        myUser.setEnabled("true");
        myUser.setFirstName(user.getFirstName());
        myUser.setLastName(user.getLastName());

        userRepository.save(myUser);
        //return "changeProfile?success=" +user.getEmail();
        return "redirect:/api/v1/user/changeProfile?success=" +user.getEmail();
    }



    @GetMapping("/invoice/{reference}/pdf")
    public void generateInvoicePdf(
            @PathVariable String reference,
            HttpServletResponse response) throws Exception {

        Optional<InvoiceMaster> invoice = invoiceMasterRepository.findByReference(reference);

        response.setContentType("application/pdf");
        response.setHeader(
                "Content-Disposition",
                "attachment; filename=invoice-" + invoice.get().getReference() + ".pdf"
        );

        generate(invoice.get(), response.getOutputStream());
    }


    public void generate(InvoiceMaster invoice, OutputStream os) throws Exception {

        DateTimeFormatter dateFormatter =
                DateTimeFormatter.ofPattern("MM/dd/yyyy");

        Document document = new Document();
        PdfWriter.getInstance(document, os);
        document.open();

        /* ================= COMPANY HEADER ================= */

        PdfPTable companyHeader = new PdfPTable(2);
        companyHeader.setWidthPercentage(100);
        companyHeader.setSpacingAfter(15);
        companyHeader.setWidths(new float[]{3, 7});

        // Logo - LEFT
        Image logo = Image.getInstance("src/main/resources/static/megsonsoft-logo.png");
        logo.scaleToFit(120, 60);

        PdfPCell logoCell = new PdfPCell(logo);
        logoCell.setBorder(Rectangle.NO_BORDER);
        logoCell.setHorizontalAlignment(Element.ALIGN_LEFT);
        companyHeader.addCell(logoCell);

        // Company Address - RIGHT
        Font companyNameFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font infoFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        Paragraph companyInfo = new Paragraph();
        companyInfo.setAlignment(Element.ALIGN_RIGHT);
        Optional<Ledger> loca = ledgerRepository.findByLedgerName("Revenue");
        if (loca.isPresent()) {

            companyInfo.add(new Chunk( loca.get().getCompanyName() + "\n", infoFont));

            String[] parts = loca.get().getCompanyAddress().split("\\|");
            for (String part : parts) {
                companyInfo.add(new Chunk(part + "\n", infoFont));
            }

        }


        PdfPCell companyCell = new PdfPCell(companyInfo);
        companyCell.setBorder(Rectangle.NO_BORDER);
        companyCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        companyHeader.addCell(companyCell);

        document.add(companyHeader);

        /* ================= TITLE ================= */

        Paragraph title = new Paragraph(
                "INVOICE",
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16));
        title.setAlignment(Element.ALIGN_CENTER);
        title.setSpacingAfter(20);
        document.add(title);

        /* ================= INVOICE HEADER DETAILS ================= */

        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(50);
        headerTable.setHorizontalAlignment(Element.ALIGN_LEFT); // ✅ table left
        headerTable.setSpacingAfter(15);
        headerTable.setWidths(new float[]{2, 5});

        headerTable.addCell(leftNoBorderCell("Reference:"));
        headerTable.addCell(leftNoBorderCell(invoice.getReference()));

        headerTable.addCell(leftNoBorderCell("Date:"));
        headerTable.addCell(leftNoBorderCell(
                invoice.getInvoiceDate().format(dateFormatter)));

        headerTable.addCell(leftNoBorderCell("Client:"));
        headerTable.addCell(leftNoBorderCell(
                invoice.getClient().getCompanyName()));

        headerTable.addCell(leftNoBorderCell("Client Address:"));
        headerTable.addCell(leftNoBorderCell(
                invoice.getClient().getCompanyAddress()));

        document.add(headerTable);

        /* ================= INVOICE LINES ================= */

        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);

        table.addCell("Quantity");
        table.addCell("Description");
        table.addCell("Serviced");
        table.addCell("Rate");
        table.addCell("Amount");

        BigDecimal totalAmount = BigDecimal.ZERO;

        for (InvoiceDetail d : invoice.getDetails()) {

            table.addCell(d.getNoOfHrs() + " hrs");
            table.addCell("Professional Services for " +
                    d.getEmployee().getLedgerName());
            table.addCell(
                    d.getStartDate().format(dateFormatter) +
                            " - " +
                            d.getEndDate().format(dateFormatter)
            );
            table.addCell(d.getRate().toString());
            table.addCell(d.getAmount().toString());

            if (d.getAmount() != null) {
                totalAmount = totalAmount.add(d.getAmount());
            }
        }

        /* ================= TOTAL ROW ================= */

        Font totalFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);

        PdfPCell totalLabelCell = new PdfPCell(new Phrase("Total", totalFont));
        totalLabelCell.setColspan(4);
        totalLabelCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalLabelCell.setPadding(8);

        PdfPCell totalValueCell = new PdfPCell(
                new Phrase(totalAmount.toString(), totalFont));
        totalValueCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        totalValueCell.setPadding(8);

        table.addCell(totalLabelCell);
        table.addCell(totalValueCell);

        document.add(table);

        document.close();
    }

    /* ================= UTILITY ================= */

    private PdfPCell leftNoBorderCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(Element.ALIGN_LEFT);
        return cell;
    }


    private PdfPCell noBorderCell(String text) {
        PdfPCell cell = new PdfPCell(new Phrase(text));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(5);
        return cell;
    }


    @GetMapping("/timesheet/edit/{timesheetId}")
    public String showEditTimesheetForm(Model model, @PathVariable Long timesheetId) {

        //ledgers ids for all employees
        Optional<List<Ledger>> clients = ledgerRepository.findByIsEmployeeAndTypeAndLabel("Y", "Expense", "employee");
        model.addAttribute("employees", clients.get());

        Optional<TimeSheet> optTmesheet = timesheetRepository.findById(timesheetId);

        if (optTmesheet.isPresent()) {
            model.addAttribute("timesheet", optTmesheet.get());
        } else {
            return "timesheet-list?success=NoRecords" ;
        }

       // model.addAttribute("timesheet",new TimeSheet());

        return "timesheet-add";
    }

    @GetMapping("/timesheet/add")
    public String showAddTimesheetForm(Model model) {

        //ledgers ids for all employees
        Optional<List<Ledger>> clients = ledgerRepository.findByIsEmployeeAndTypeAndLabel("Y", "Expense", "employee");
        model.addAttribute("employees", clients.get());


        model.addAttribute("timesheet",new TimeSheet());

        return "timesheet-add";
    }



    @PostMapping("/timesheet/add")
    public String saveTimesheet(@ModelAttribute TimeSheet timeSheet) {


// ✅ Validate date order
        if (timeSheet.getStartDate().isAfter(timeSheet.getEndDate())) {
            return "redirect:/api/v1/timesheet/list?error=Invalid date range";
        }

        List<TimeSheet> collisions =
                timesheetRepository.findCollidingTimeSheets(
                        timeSheet.getEmployee(),
                        timeSheet.getStartDate(),
                        timeSheet.getEndDate(),
                        timeSheet.getId()
                );

        if (!collisions.isEmpty()) {
            return "redirect:/api/v1/timesheet/list?error=Timesheet date range overlaps with existing entry";
        }

        Optional<TimeSheet> t = null;
        if (timeSheet.getId() != null) {
            t = timesheetRepository.findById(timeSheet.getId());
        }
        if (timeSheet.getId() != null && t!=null && t.isPresent()) {
            if (t.get().getInvoiceDetail() !=null) {
                return "redirect:/api/v1/timesheet/list?error=hrs are already invoiced";
            }
            t.get().setEmployee(timeSheet.getEmployee());
            t.get().setEndDate(timeSheet.getEndDate());
            t.get().setStartDate(timeSheet.getStartDate());
            t.get().setNoOfHrs(timeSheet.getNoOfHrs());
            timesheetRepository.save(t.get());
            return "redirect:/api/v1/timesheet/list?success="+ t.get().getEmployee().getLedgerName() + ":"
                    +t.get().getStartDate() + ":" + t.get().getEndDate();
        } else {
            timesheetRepository.save(timeSheet);
            return "redirect:/api/v1/timesheet/list?success="+ timeSheet.getEmployee().getLedgerName() + ":"
                    +timeSheet.getStartDate() + ":" + timeSheet.getEndDate();
        }
    }

    @GetMapping("/timesheet/list")
    public String listTimeSheet(Model model) {
        List<TimeSheet> dd = timesheetRepository.findAll();
        dd.sort(Comparator.comparing(TimeSheet::getStartDate).reversed());
        model.addAttribute("timeSheetList", dd);
        return "timesheet-list";
    }

}
