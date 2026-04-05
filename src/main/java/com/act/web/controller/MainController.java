package com.act.web.controller;

import com.act.dto.TransactionDto;
import com.act.json.model.Config;
import com.act.json.model.Event;
import com.act.json.model.EventAction;
import com.act.json.model.LocalDateAdapter;
import com.act.model.*;
import com.act.repo.InvoiceMasterRepository;
import com.act.repo.JournalEntryRepository;
import com.act.repo.LedgerRepository;
import com.act.repo.TrasactionRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Controller
@RequestMapping("api/v1/")
public class MainController {

    private final LedgerRepository ledgerRepository;
    private final InvoiceMasterRepository invoiceMasterRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final TrasactionRepository trasactionRepository;

    public MainController(LedgerRepository ledgerRepository, InvoiceMasterRepository invoiceMasterRepository,
                          JournalEntryRepository journalEntryRepository, TrasactionRepository trasactionRepository) {
        this.ledgerRepository = ledgerRepository;
        this.invoiceMasterRepository = invoiceMasterRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.trasactionRepository = trasactionRepository;
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
        transaction.setDescription(desc + ":"+ from + "==>" + to);
        transaction.setTransactionDate(transactionDate);
        transaction.setInvoiceMaster(invoiceMaster);
        transaction.setJournalEntry(journalEntry);
        trasactionRepository.save(transaction);

        Transaction transactionNew = new Transaction();
        transactionNew.setAccount(ledgerRepository.findByLedgerName(to).get());
        transactionNew.setAmount(totalAmount);
        transaction.setDescription(desc + ":"+ from + "==>" + to);
        transactionNew.setTransactionDate(transactionDate);
        transactionNew.setInvoiceMaster(invoiceMaster);
        transactionNew.setJournalEntry(journalEntry);
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
        return "statement-list";
    }

    //employees
    // Show form
    @GetMapping("/ledger/add")
    public String showAddLedgerForm(Model model) {
        model.addAttribute("ledger", new Ledger());
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
            ledgerRepository.save(t.get());
        } else {
            ledger.setBalance(new BigDecimal(0));
            ledger.setBalanceUpdateDate(LocalDateTime.now());
            ledgerRepository.save(ledger);
        }
        //return "redirect:/employees";  // redirect to list page
        return "redirect:/api/v1/ledger/list";

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

        Optional<Ledger> clients = ledgerRepository.findByIsEmployeeAndTypeAndLabel("N", "Asset", "AR");
        model.addAttribute("clients", clients.get());
        Optional<Ledger> employees = ledgerRepository.findByIsEmployeeAndType("Y", "Expense");
        model.addAttribute("employees", employees.get());


        return "invoiceMaster-add";
    }

    //InvoiceMaster
    @Transactional
    @GetMapping("/invoicesMaster/add")
    public String showAddInvoiceForm(Model model) {
        InvoiceMaster invoiceMaster = new InvoiceMaster();
        invoiceMaster.setDetails(new ArrayList<>());
        invoiceMaster.getDetails().add(new InvoiceDetail());
        invoiceMaster.setReference(UUID.randomUUID().toString());
        invoiceMaster.setStatus("DRAFT");
        model.addAttribute("invoiceMaster", invoiceMaster);

        Optional<Ledger> clients = ledgerRepository.findByIsEmployeeAndTypeAndLabel("N", "Asset", "AR");
        model.addAttribute("clients", clients.get());
        Optional<Ledger> employees = ledgerRepository.findByIsEmployeeAndType("Y", "Expense");
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


            for (InvoiceDetail invd : invoiceMaster.getDetails()) {
                invd.setInvoiceMaster(invoiceMaster);
            }

            invoiceMasterRepository.save(invoiceMaster);
            invoiceMasterEdit = invoiceMaster;

        }


        if (action.equals("submit")) {
            Event toApply = getConfigEvent(invoiceMasterEdit.getClient(), "invoice");
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
                            ea.getFromLedgerName(),
                            ea.getToLedgerName(),
                            "invoice (Submit Event)" ,
                            invoiceMaster.getInvoiceDate(),
                            true);

                }
            }
        }

        if (action.equals("receivePayment")) {
            Event toApply = getConfigEvent(invoiceMasterEdit.getClient(), "invoicePayment");
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
                            ea.getFromLedgerName(),
                            ea.getToLedgerName(),
                            "receivePayment Event (MasterEntry) from "
                                    + invoiceMasterEdit.getClient().getLedgerName()
                                    + " : "
                                +invoiceMasterEdit.getReference()
                                + " : "
                                + invoiceMasterEdit.getInvoiceDate()
                                    ,
                            invoiceMasterEdit.getInvoiceDate(),
                            true);

                }
            }

            {
                Iterator<InvoiceDetail> iterator = invoiceMasterEdit.getDetails().iterator();
                while (iterator.hasNext()) {

                    InvoiceDetail invd = iterator.next();
                    Event toApplyLocal = getConfigEvent(invd.getEmployee(), "invoicePayment");
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
                                        null,
                                        ea.getFromLedgerName(),
                                        ea.getToLedgerName(),
                                        "receivePayment Event (LineEntry) from : "
                                                + invd.getEmployee().getLedgerName()
                                                + " : "
                                                + invd.getNoOfHrs()
                                                + " : "
                                                + invd.getStartDate()
                                                + " : "
                                                + invd.getEndDate(),
                                        invoiceMasterEdit.getInvoiceDate(),
                                        true);
                            }

                        }
                    }


                }
            }

        }


        //return "redirect:/employees";  // redirect to list page
        return "redirect:/api/v1/invoicesMaster/list?success";
    }


    @GetMapping("/invoicesMaster/list")
    public String listMasterList(Model model) {
        model.addAttribute("invoiceMasterList", invoiceMasterRepository.findAll());
        return "invoicesMaster-List";
    }


    @GetMapping("/journal/edit/{id}")
    public String showEditjournalForm(Model model, @PathVariable String id) {
        Optional<JournalEntry> t = journalEntryRepository.findById(id);
        model.addAttribute("journal", t.get());
        Optional<Ledger> clients = ledgerRepository.findByType("Expense");
        model.addAttribute("clients", clients.get());
        return "journal-add";
    }

    //journal
    @GetMapping("/journal/add")
    public String showAddJournalForm(Model model) {
        JournalEntry je = new JournalEntry();
        je.setId(UUID.randomUUID().toString());
        model.addAttribute("journal", je);
        Optional<Ledger> clients = ledgerRepository.findByType("Expense");
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


                updateBalanceandTransaction(je.getAmount(), null,
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
        return "redirect:/api/v1/journal/list?success";
    }


    @GetMapping("/journal/list")
    public String listJournalList(Model model) {
        List<JournalEntry> dd = journalEntryRepository.findAll();
        dd.sort(Comparator.comparing(JournalEntry::getTransactionDate).reversed());
        model.addAttribute("journalList", dd);
        return "journal-List";
    }

}
