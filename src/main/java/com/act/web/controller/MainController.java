package com.act.web.controller;

import com.act.json.model.Config;
import com.act.json.model.Event;
import com.act.json.model.EventAction;
import com.act.json.model.LocalDateAdapter;
import com.act.model.InvoiceDetail;
import com.act.model.JournalEntry;
import com.act.model.Ledger;
import com.act.model.InvoiceMaster;
import com.act.repo.JournalEntryRepository;
import com.act.repo.LedgerRepository;

import com.act.repo.InvoiceMasterRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("api/v1/")
public class MainController {

    private  final LedgerRepository ledgerRepository ;
    private final InvoiceMasterRepository invoiceMasterRepository ;
    private final JournalEntryRepository journalEntryRepository ;

    public MainController(LedgerRepository ledgerRepository, InvoiceMasterRepository invoiceMasterRepository, JournalEntryRepository journalEntryRepository) {
        this.ledgerRepository = ledgerRepository;
        this.invoiceMasterRepository = invoiceMasterRepository;
        this.journalEntryRepository = journalEntryRepository;
    }


    @GetMapping("/login")
    public String getLoginPage(Model model) {
        return "login";
    }

    @GetMapping
    public String getHomePage() {
        return "index";
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

        model.addAttribute("invoiceMaster",t.get());

        Optional<Ledger> clients = ledgerRepository.findByIsEmployeeAndTypeAndLabel("N","Asset","AR");
        model.addAttribute("clients", clients.get());
        Optional<Ledger> employees = ledgerRepository.findByIsEmployeeAndType("Y", "Expense");
        model.addAttribute("employees", employees.get());




        return "invoiceMaster-add";
    }

    //InvoiceMaster
    @GetMapping("/invoicesMaster/add")
    public String showAddInvoiceForm(Model model) {
        model.addAttribute("invoiceMaster", new InvoiceMaster());

        Optional<Ledger> clients = ledgerRepository.findByIsEmployeeAndTypeAndLabel("N","Asset","AR");
        model.addAttribute("clients", clients.get());
        Optional<Ledger> employees = ledgerRepository.findByIsEmployeeAndType("Y", "Expense");
        model.addAttribute("employees", employees.get());



        return "invoiceMaster-add";
    }

    // Handle submit
    @PostMapping("/invoicesMaster/add")
    public String saveInvoiceMaster(@ModelAttribute InvoiceMaster invoiceMaster) {
        Optional<InvoiceMaster> t = invoiceMasterRepository.findByReference(invoiceMaster.getReference());

        if (t.isPresent()) {
            t.get().setInvoiceDate(invoiceMaster.getInvoiceDate());
            t.get().setDetails(invoiceMaster.getDetails());
            for (InvoiceDetail invd : invoiceMaster.getDetails()) {
                invd.setInvoiceMaster(t.get());
            }
            t.get().setClient(invoiceMaster.getClient());
            invoiceMasterRepository.save(t.get());
        } else {
            for (InvoiceDetail invd : invoiceMaster.getDetails()) {
                invd.setInvoiceMaster(t.get());
            }
            invoiceMasterRepository.save(invoiceMaster);
        }
        //return "redirect:/employees";  // redirect to list page
        return "redirect:/api/v1/invoicesMaster/list?success";
    }


    @GetMapping("/invoicesMaster/list")
    public String listMasterList(Model model) {
        model.addAttribute("invoiceMasterList", invoiceMasterRepository.findAll());
        return "invoicesMaster-List";
    }



    //journal
    @GetMapping("/journal/add")
    public String showAddJournalForm(Model model) {
        model.addAttribute("journal", new JournalEntry());
        Optional<Ledger> clients = ledgerRepository.findByType("Expense");
        model.addAttribute("clients", clients.get());

        //Optional<Ledger> clients = ledgerRepository.findByIsEmployeeAndType("N","Liabilities");
        //model.addAttribute("clients", clients.get());

        return "journal-add";
    }

    // Handle submit
    @PostMapping("/journal/add")
    public String saveJournal(@ModelAttribute JournalEntry journalEntry) {
        //from
        //Optional<Ledger> client = ledgerRepository.f
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .setPrettyPrinting()
                .create();

        Config config = gson.fromJson( journalEntry.getToAccount().getConfig(), Config.class);

        Iterator<Event> it = config.getEvents().iterator();

        Event toApply = null;
        while (it.hasNext()) {
            Event event = it.next();
            if (event.getName().equals(journalEntry.getType()) ) {
                LocalDate today = LocalDate.now();   // Current date
                boolean isBetween =  (!today.isBefore(event.getEventConfig().getValidFrom()) )
                && ( !today.isAfter(event.getEventConfig().getValidTo()) );
                if (isBetween) {
                    toApply = event;
                    break;
                }
            }
        }
        if (toApply!=null) {
            Iterator<EventAction> itAction = toApply.getEventConfig().getEventAction().iterator();
            while (itAction.hasNext()) {
                EventAction ea = itAction.next();
                JournalEntry je = new JournalEntry();
                je.setTransactionDate(journalEntry.getTransactionDate());
                if (ea.getType().equals("source")){
                    je.setAmount(journalEntry.getAmount());
                }
                je.setFromAccount(ledgerRepository.findByLedgerName(ea.getFromLedgerName()).get());
                je.setToAccount(ledgerRepository.findByLedgerName(ea.getToLedgerName()).get());
                je.setType(ea.getType());
                je.setDescription(journalEntry.getDescription());
                je.getFromAccount().setBalance(je.getFromAccount().getBalance().subtract(je.getAmount()) );
                je.getToAccount().setBalance(je.getToAccount().getBalance().add(je.getAmount()) );

                journalEntryRepository.save(je);
            }
        }
        //journalEntryRepository.save(journalEntry);
        return "redirect:/api/v1/journal/list?success";
    }


    @GetMapping("/journal/list")
    public String listJournalList(Model model) {
        model.addAttribute("journalList", journalEntryRepository.findAll());
        return "journal-List";
    }

}
