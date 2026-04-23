package com.act.web.controller;

import com.act.dto.*;
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
import org.springframework.format.annotation.DateTimeFormat;
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
import java.util.stream.Collectors;

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
    private TransactionUtil transactionUtil;

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



    @GetMapping("/guide")
    public String showGuide(Model model) {
        return "guide";
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





}
