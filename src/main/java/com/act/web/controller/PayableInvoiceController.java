package com.act.web.controller;

import com.act.model.Ledger;
import com.act.model.PayableInvoice;
import com.act.repo.LedgerRepository;
import com.act.repo.PayableInvoiceRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/api/v1/payable-invoice")
public class PayableInvoiceController {

    private final PayableInvoiceRepository repo;
    private final LedgerRepository ledgerRepository;
    private final PayableInvoiceRepository payableInvoiceRepository;

    public PayableInvoiceController(
            PayableInvoiceRepository repo,
            LedgerRepository ledgerRepository, PayableInvoiceRepository payableInvoiceRepository) {
        this.repo = repo;
        this.ledgerRepository = ledgerRepository;
        this.payableInvoiceRepository = payableInvoiceRepository;
    }

    /* ================= LIST ================= */
    @GetMapping("/list")
    public String list(Model model) {
        model.addAttribute("invoices", repo.findAll());
        return "payable-invoice-list";
    }

    /* ================= SHOW ADD FORM ================= */
    @GetMapping("/add")
    public String showAdd(Model model) {
        PayableInvoice pi =  new PayableInvoice();
        pi.setStatus("SUBMITTED");
        model.addAttribute("invoice", pi);
        model.addAttribute("vendors",
                ledgerRepository.findByIsEmployeeAndTypeAndLabel("N", "Expense", "vendor").get());
        return "payable-invoice-add";
    }

    @GetMapping("/edit/{id}")
    public String showEditLedgerForm(Model model, @PathVariable long id) {

        Optional<PayableInvoice> t = payableInvoiceRepository.findById(id);
        model.addAttribute("invoice", t.get());

        model.addAttribute("vendors",
                ledgerRepository.findByIsEmployeeAndTypeAndLabel("N", "Expense", "vendor").get());
        return "payable-invoice-add";
    }

    /* ================= SAVE ================= */
    @PostMapping("/add")
    public String add(
            @ModelAttribute PayableInvoice invoice,
            @RequestParam("file") MultipartFile file) throws Exception {

        //invoice.setInvoiceReceiveDate(LocalDate.now());
        invoice.setStatus("SUBMITTED");
        if (file != null && !file.isEmpty()) {
            invoice.setDocumentContent(file.getBytes());
            invoice.setFileName(file.getOriginalFilename());
            invoice.setContentType(file.getContentType());
        }

        repo.save(invoice);

        return "redirect:/api/v1/payable-invoice/list";
    }

    /* ================= DOWNLOAD ================= */
    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {

        PayableInvoice doc = repo.findById(id).orElseThrow();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + doc.getFileName() + "\"")
                .body(doc.getDocumentContent());
    }
}