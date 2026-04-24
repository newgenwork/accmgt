package com.act.web.controller;

import com.act.json.model.Config;
import com.act.json.model.Event;
import com.act.json.model.EventAction;
import com.act.json.model.LocalDateAdapter;
import com.act.model.JournalEntry;
import com.act.model.Ledger;
import com.act.repo.JournalEntryRepository;
import com.act.repo.LedgerRepository;
import com.act.repo.SequenceRepository;
import com.act.repo.TrasactionRepository;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@Controller
@RequestMapping("/api/v1/journal")
public class JournalController {

    private final JournalEntryRepository journalEntryRepository;
    private final LedgerRepository ledgerRepository;
    private final SequenceRepository sequenceRepository;
    private final TrasactionRepository trasactionRepository;
    @Autowired
    private TransactionUtil transactionUtil;

    public JournalController(
            JournalEntryRepository journalEntryRepository,
            LedgerRepository ledgerRepository,
            TrasactionRepository trasactionRepository,
            SequenceRepository sequenceRepository
    ) {
        this.journalEntryRepository = journalEntryRepository;
        this.ledgerRepository = ledgerRepository;
        this.trasactionRepository = trasactionRepository;
        this.sequenceRepository = sequenceRepository;
    }

    /* ================= ADD ================= */

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

        return "journal-add";
    }

    /* ================= EDIT ================= */

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