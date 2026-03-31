package com.act.web.controller;

import com.act.model.JournalEntry;
import com.act.model.Ledger;
import com.act.model.InvoiceMaster;
import com.act.repo.JournalEntryRepository;
import com.act.repo.LedgerRepository;

import com.act.repo.InvoiceMasterRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
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


    //InvoiceMaster
    @GetMapping("/invoicesMaster/add")
    public String showAddInvoiceForm(Model model) {
        model.addAttribute("invoiceMaster", new InvoiceMaster());

        Optional<Ledger> clients = ledgerRepository.findByIsEmployeeAndType("N","Asset");
        model.addAttribute("clients", clients.get());

        return "invoiceMaster-add";
    }

    // Handle submit
    @PostMapping("/invoicesMaster/add")
    public String saveInvoiceMaster(@ModelAttribute InvoiceMaster invoiceMaster) {
        Optional<InvoiceMaster> t = invoiceMasterRepository.findByReference(invoiceMaster.getReference());

        if (t.isPresent()) {
            t.get().setInvoiceDate(invoiceMaster.getInvoiceDate());
            invoiceMasterRepository.save(t.get());
        } else {
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
        journalEntryRepository.save(journalEntry);
        return "redirect:/api/v1/journal/list?success";
    }


    @GetMapping("/journal/list")
    public String listJournalList(Model model) {
        model.addAttribute("journalList", journalEntryRepository.findAll());
        return "journal-List";
    }

}
