package com.act.web.controller;

import com.act.json.model.Config;
import com.act.json.model.Event;
import com.act.json.model.EventAction;
import com.act.json.model.LocalDateAdapter;
import com.act.model.JournalEntry;
import com.act.model.Ledger;
import com.act.model.PayableInvoice;
import com.act.repo.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/api/v1/journal")
public class JournalController {

    private final JournalEntryRepository journalEntryRepository;
    private final LedgerRepository ledgerRepository;
    private final SequenceRepository sequenceRepository;
    private final TrasactionRepository trasactionRepository;
    private final PayableInvoiceRepository payableInvoiceRepository;

    @Autowired
    private TransactionUtil transactionUtil;

    public JournalController(
            JournalEntryRepository journalEntryRepository,
            LedgerRepository ledgerRepository,
            TrasactionRepository trasactionRepository,
            SequenceRepository sequenceRepository, PayableInvoiceRepository payableInvoiceRepository
    ) {
        this.journalEntryRepository = journalEntryRepository;
        this.ledgerRepository = ledgerRepository;
        this.trasactionRepository = trasactionRepository;
        this.sequenceRepository = sequenceRepository;
        this.payableInvoiceRepository = payableInvoiceRepository;
    }

    /* ================= ADD ================= */

    @Transactional
    @GetMapping("/add")
    public String showAddJournalForm(Model model) {
        JournalEntry je = new JournalEntry();
        je.setId("JN-" + sequenceRepository.getNextInvoiceSequence());
        model.addAttribute("journal", je);

        List<Ledger> retclients = new ArrayList<>();
        Optional<List<Ledger>> retClientsOpt = ledgerRepository.findByTypeAndIsJournalEntryPossible("Expense", "Y");
        if (retClientsOpt.isPresent()) {
            retclients = retClientsOpt.get();
        }
        model.addAttribute(
                "clients",retclients
        );

        List<PayableInvoice> invoices =
                payableInvoiceRepository.findByStatus("SUBMITTED");

        model.addAttribute("payableInvoices", invoices);

        return "journal-add";
    }


    @Transactional
    @GetMapping("/invoices/by-vendor/{vendorId}")
    @ResponseBody
    public List<Map<String, Object>> getInvoicesByVendor(@PathVariable Long vendorId) {

        Ledger vendor = ledgerRepository.findById(vendorId).orElseThrow();

        List<PayableInvoice> invoices =
                payableInvoiceRepository.findByVendorAndStatus(vendor, "SUBMITTED");

        // ✅ Convert to lightweight JSON
        List<Map<String, Object>> result = new ArrayList<>();

        for (PayableInvoice p : invoices) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", p.getId());
            map.put("reference", p.getReference());
            map.put("amount", p.getAmount());
            result.add(map);
        }

        return result;
    }


    /* ================= EDIT ================= */

    @Transactional
    @GetMapping("/edit/{id}")
    public String showEditJournalForm(
            @PathVariable String id,
            Model model) {

        JournalEntry journal =
                journalEntryRepository.findById(id).orElseThrow();

        model.addAttribute("journal", journal);
        model.addAttribute(
                "clients",
                ledgerRepository.findByTypeAndIsJournalEntryPossible("Expense", "Y").get()
        );

        List<PayableInvoice> invoices =
                payableInvoiceRepository.findByStatus("SUBMITTED");

        model.addAttribute("payableInvoices", invoices);

        return "journal-add";
    }

    /* ================= SAVE ================= */

    @Transactional
    @PostMapping("/add")
    public String saveJournal(@ModelAttribute JournalEntry journalEntry) {

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new LocalDateAdapter())
                .setPrettyPrinting()
                .create();

        Config config =
                gson.fromJson(
                        journalEntry.getTargetAccount().getConfig(),
                        Config.class
                );

        Event event = getConfigEvent(
                journalEntry.getTargetAccount(),
                journalEntry.getType(),
                config
        );

        if (event == null) {
            throw new RuntimeException(
                    "No Event Found for Journal Type " +
                            journalEntry.getType() +
                            " on ledger " +
                            journalEntry.getTargetAccount().getLedgerName()
            );
        }

        for (EventAction ea : event.getEventConfig().getEventAction()) {

            JournalEntry je =
                    journalEntryRepository
                            .findById(journalEntry.getId())
                            .orElse(new JournalEntry());

            je.setId(journalEntry.getId());
            je.setTransactionDate(journalEntry.getTransactionDate());
            je.setAmount(journalEntry.getAmount());
            je.setTargetAccount(journalEntry.getTargetAccount());
            je.setType(journalEntry.getType());
            je.setDescription(journalEntry.getDescription());

            if (je.getPayableInvoice()==null || !je.getPayableInvoice().getId().equals(journalEntry.getPayableInvoice().getId())){

                if (je.getPayableInvoice()!=null) {
                    Optional<PayableInvoice> aa = payableInvoiceRepository.findById(je.getPayableInvoice().getId());
                    aa.get().setStatus("SUBMITTED");
                    payableInvoiceRepository.save(aa.get());
                }

                if (journalEntry.getPayableInvoice() !=null) {
                    Optional<PayableInvoice> bb = payableInvoiceRepository.findById(journalEntry.getPayableInvoice().getId());
                    bb.get().setStatus("PAID");
                    payableInvoiceRepository.save(bb.get());
                    je.setPayableInvoice(journalEntry.getPayableInvoice());
                }
            }


            journalEntryRepository.save(je);


            transactionUtil. updateBalanceandTransaction(je.getAmount(), null, null,
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

        return "redirect:/api/v1/journal/list?success=" + journalEntry.getId();
    }

    /* ================= LIST ================= */

    @GetMapping("/list")
    public String listJournal(Model model) {

        List<JournalEntry> journals =
                journalEntryRepository.findAll();

        journals.sort(
                Comparator.comparing(JournalEntry::getTransactionDate).reversed()
        );

        model.addAttribute("journalList", journals);
        return "journal-List";
    }

    @GetMapping("/payslip/{id}/pdf")
    public void downloadPayslip(
            @PathVariable Long id,
            HttpServletResponse response) throws Exception {

    //    PayableInvoice invoice = repo.findById(id).orElseThrow();

        response.setContentType("application/pdf");
      //  response.setHeader("Content-Disposition",
        //        "attachment; filename=payslip-" + invoice.getId() + ".pdf");
    }

    /* ================= HELPERS ================= */

    private Event getConfigEvent(
            Ledger ledger,
            String type,
            Config config
    ) {
        if (config.getEvents() == null) {
            return null;
        }

        for (Event event : config.getEvents()) {
            if (event.getName().equals(type)) {
                LocalDate today = LocalDate.now();
                if (!today.isBefore(event.getEventConfig().getValidFrom())
                        && !today.isAfter(event.getEventConfig().getValidTo())) {
                    return event;
                }
            }
        }
        return null;
    }




}