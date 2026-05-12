
package com.act.web.controller;

import com.act.dto.InvoiceFilter;
import com.act.dto.InvoiceMasterDto;
import com.act.json.model.Event;
import com.act.json.model.EventAction;
import com.act.model.*;
import com.act.repo.*;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.List;

@Controller
@RequestMapping("/api/v1/invoicesMaster")
public class InvoiceController {

    private final InvoiceMasterRepository invoiceMasterRepository;
    private final LedgerRepository ledgerRepository;
    private final TimesheetRepository timesheetRepository;
    private final SequenceRepository sequenceRepository;
    private final TrasactionRepository trasactionRepository;

    @Autowired
    private  TransactionUtil transactionUtil;

    public InvoiceController(
            InvoiceMasterRepository invoiceMasterRepository,
            LedgerRepository ledgerRepository,
            TimesheetRepository timesheetRepository,
            SequenceRepository sequenceRepository,
            TrasactionRepository trasactionRepository
    ) {
        this.invoiceMasterRepository = invoiceMasterRepository;
        this.ledgerRepository = ledgerRepository;
        this.timesheetRepository = timesheetRepository;
        this.sequenceRepository = sequenceRepository;
        this.trasactionRepository = trasactionRepository;
    }


    @GetMapping("/edit/{reference}")
    public String showEditInvoiceForm(Model model, @PathVariable String reference) {
        Optional<InvoiceMaster> t = invoiceMasterRepository.findByReference(reference);

        t.get().getDetails().size();

        model.addAttribute("invoiceMaster", t.get());

        List<Ledger> clients = ledgerRepository.findByIsEmployeeAndTypeAndLabelOrderByLedgerNameAsc("N", "Asset", "AR");
        model.addAttribute("clients", clients);
        Optional<List<Ledger>> employees = ledgerRepository.findByIsEmployeeAndType("Y", "Expense");
        model.addAttribute("employees", employees.get());


        return "invoiceMaster-add-withFilter";
    }

    //InvoiceMaster
    @Transactional
    @GetMapping("/add")
    public String showAddInvoiceForm(Model model) {
        InvoiceMaster invoiceMaster = new InvoiceMaster();
        invoiceMaster.setDetails(new ArrayList<>());
        invoiceMaster.getDetails().add(new InvoiceDetail());
        //invoiceMaster.setReference(UUID.randomUUID().toString());
        invoiceMaster.setReference("INV-" + sequenceRepository.getNextInvoiceSequence().toString());
        invoiceMaster.setStatus("DRAFT");
        model.addAttribute("invoiceMaster", invoiceMaster);

        List<Ledger> clients = ledgerRepository.findByIsEmployeeAndTypeAndLabelOrderByLedgerNameAsc("N", "Asset", "AR");
        model.addAttribute("clients", clients);
        Optional<List<Ledger>> employees = ledgerRepository.findByIsEmployeeAndType("Y", "Expense");
        model.addAttribute("employees", employees.get());


        return "invoiceMaster-add";
    }

    @Transactional
    @GetMapping("/filter")
    public String showAddInvoiceFilterForm(Model model) {
        model.addAttribute("invoiceFilter", new InvoiceFilter());
        List<Ledger> clients = ledgerRepository.findByIsEmployeeAndTypeAndLabelOrderByLedgerNameAsc("N", "Asset", "AR");
        model.addAttribute("clients", clients);

        return "invoiceMaster-filter";
    }

    @Transactional
    @PostMapping("/filter")
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
            invd.setAmount(invd.getRate().multiply(invd.getNoOfHrs()));
            if (invd.getNoOfHrs().compareTo(BigDecimal.ZERO) ==0) {
               /* return "redirect:/api/v1/invoicesMaster/filter?error=Employee Not having hrs for "
                        + local.getLedgerName()
                        + " for "
                        +  invoiceFilter.getStartDate()
                        + " - "
                        + invoiceFilter.getEndDate();*/
                continue;
            }

            invd.setStartDate(invoiceFilter.getStartDate());
            invd.setEndDate(invoiceFilter.getEndDate());


            invoiceMaster.getDetails().add(invd);
        }
        if (invoiceMaster.getDetails().isEmpty()) {
            return "redirect:/api/v1/invoicesMaster/filter?error=Employee Not having hrs"
                    + " for "
                    +  invoiceFilter.getStartDate()
                    + " - "
                    + invoiceFilter.getEndDate();
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
    @GetMapping("/add/{ar}")
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
    @PostMapping("/add")
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

                       /* List<TimeSheet> tsList = timesheetRepository.findCollidingTimeSheets(invoiceMaster.getDetails().get(i).getEmployee(),
                                invoiceMaster.getDetails().get(i).getStartDate(),
                                invoiceMaster.getDetails().get(i).getEndDate(), null);

                        for (TimeSheet timeSheet : tsList) {
                            timeSheet.setInvoiceDetail(invoiceMasterEdit.getDetails().get(i));
                            timesheetRepository.save(timeSheet);
                        }*/


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
                        //invd.setAmount(invoiceMaster.getDetails().get(i).getAmount());
                        invd.setEmployee(invoiceMaster.getDetails().get(i).getEmployee());
                        invd.setRate(invoiceMaster.getDetails().get(i).getRate());
                        invd.setStartDate(invoiceMaster.getDetails().get(i).getStartDate());
                        invd.setEndDate(invoiceMaster.getDetails().get(i).getEndDate());

                        //invd.setNoOfHrs(invoiceMaster.getDetails().get(i).getNoOfHrs());

                        invd.setNoOfHrs(timesheetRepository.getTotalHoursByEmployeeAndDateRange(invd.getEmployee().getId(),
                                invd.getStartDate(),invd.getEndDate()));
                        invd.setAmount(invd.getRate().multiply(invd.getNoOfHrs()));

                        invd.setInvoiceMaster(invoiceMasterLocal);
                        if (invoiceMasterLocal.getStatus().equalsIgnoreCase("INIT")) {
                            invoiceMasterLocal.setStatus("DRAFT");
                        }
                        List<TimeSheet> tsList = timesheetRepository.findCollidingTimeSheets(
                                invd.getEmployee(),
                                invd.getStartDate(),
                                invd.getEndDate());

                        for (TimeSheet timeSheet : tsList) {
                            timeSheet.setInvoiceDetail(invd);
                            timesheetRepository.save(timeSheet);
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
                Map<TimeSheet,InvoiceDetail> tsMapSave = new HashMap<>();
                for (int i = 0; i < invoiceMaster.getDetails().size(); i++) {
                    if (invoiceMaster.getDetails().get(i).getEmployee() != null) {
                        if (invoiceMasterEdit.getDetails() == null) {
                            invoiceMasterEdit.setDetails(new ArrayList<InvoiceDetail>());
                        }
                        InvoiceDetail invd = new InvoiceDetail();
                        invoiceMasterEdit.getDetails().add(invd);
                        //line = line + 1;
                        //invd.setId(line);
                        //invd.setAmount(invoiceMaster.getDetails().get(i).getAmount());
                        invd.setEmployee(invoiceMaster.getDetails().get(i).getEmployee());
                        invd.setRate(invoiceMaster.getDetails().get(i).getRate());
                        invd.setStartDate(invoiceMaster.getDetails().get(i).getStartDate());
                        invd.setEndDate(invoiceMaster.getDetails().get(i).getEndDate());
                        //invd.setNoOfHrs(invoiceMaster.getDetails().get(i).getNoOfHrs());

                        invd.setNoOfHrs(timesheetRepository.getTotalHoursByEmployeeAndDateRange(
                                invoiceMaster.getDetails().get(i).getEmployee().getId(),
                                invoiceMaster.getDetails().get(i).getStartDate(),
                                invoiceMaster.getDetails().get(i).getEndDate()));
                        invd.setAmount(invd.getRate().multiply(invd.getNoOfHrs()));

                        invd.setInvoiceMaster(invoiceMasterEdit);
                        List<TimeSheet> tsList = timesheetRepository.findCollidingTimeSheets(
                                invd.getEmployee(),
                                invd.getStartDate(),
                                invd.getEndDate());

                        for (TimeSheet timeSheet : tsList) {
                            //timeSheet.setInvoiceDetail(invd);
                            //tsListSave.add(timeSheet);
                            tsMapSave.put(timeSheet, invd);
                            //timesheetRepository.save(timeSheet);
                        }
                    }

                }
                invoiceMasterRepository.save(invoiceMasterEdit);

                Iterator<TimeSheet> itTsList = tsMapSave.keySet().iterator();
                while (itTsList.hasNext()) {
                    TimeSheet tm = itTsList.next();
                    tm.setInvoiceDetail(tsMapSave.get(tm));
                    timesheetRepository.save(tm);
                }

            }

        }


        if (action.equals("submit")) {
            Event toApply = Util.getConfigEvent(invoiceMasterEdit.getClient(), "invoice");
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

                    transactionUtil.updateBalanceandTransaction(
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
            Event toApply = Util.getConfigEvent(invoiceMasterEdit.getClient(), "invoicePayment");
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

                    transactionUtil.updateBalanceandTransaction(totalAmount,
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
                    Event toApplyLocal = Util.getConfigEvent(invd.getEmployee(), "invoicePayment");
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
                                transactionUtil.updateBalanceandTransaction(amt,
                                        invoiceMasterEdit,
                                        invd,
                                        null,
                                        ea.getFromLedgerName(),
                                        ea.getToLedgerName(),
                                        "receivePayment Event (LineEntry) from Name: "
                                                + invd.getEmployee().getLedgerName()
                                                + " : Hrs :"
                                                + invd.getNoOfHrs()
                                                + " : rate to southbound :"
                                                + ea.getAmountRatePerHour()
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


    @GetMapping("/list")
    public String listMasterList(Model model) {

        List<InvoiceMaster> dd = invoiceMasterRepository.findAll();
        dd.sort(Comparator.comparing(InvoiceMaster::getInvoiceDate).reversed());

        List<InvoiceMasterDto> retList = new ArrayList<InvoiceMasterDto>();

        Iterator<InvoiceMaster> it = dd.iterator();
        BigDecimal grandTotalAmount = BigDecimal.ZERO;
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
            grandTotalAmount = grandTotalAmount.add(totalAmount);
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

        model.addAttribute("grandTotalAmount", grandTotalAmount);
        return "invoicesMaster-List";
    }
    //journal



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



}