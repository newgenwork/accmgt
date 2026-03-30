package com.act.web.controller;

import com.act.model.Employee;
import com.act.model.InvoiceMaster;
import com.act.repo.EmployeeRepository;

import com.act.repo.InvoiceMasterRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Controller
@RequestMapping("api/v1/")
public class MainController {

    private  final EmployeeRepository employeeRepository ;
    private final InvoiceMasterRepository invoiceMasterRepository ;

    public MainController(EmployeeRepository employeeRepository, InvoiceMasterRepository invoiceMasterRepository) {
        this.employeeRepository = employeeRepository;
        this.invoiceMasterRepository = invoiceMasterRepository;
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
    @GetMapping("/employees/add")
    public String showAddEmployeeForm(Model model) {
        model.addAttribute("employee", new Employee());
        return "employee-add";
    }

    // Handle submit
    @PostMapping("/employees/add")
    public String saveEmployee(@ModelAttribute Employee employee) {
        Optional<Employee> t = employeeRepository.findByEmail(employee.getEmail());
        if (t.isPresent()) {
            t.get().setConfig(employee.getConfig());
            t.get().setEnable(employee.getEnable());
            t.get().setFirstName(employee.getFirstName());
            t.get().setLastName(employee.getLastName());
            employeeRepository.save(t.get());
        } else {
            employee.setBalance(new BigDecimal(0));
            employee.setBalanceUpdateDate(LocalDateTime.now());
            employeeRepository.save(employee);
        }
        //return "redirect:/employees";  // redirect to list page
        return "redirect:/api/v1/employees";

    }


    @GetMapping("/employees")
    public String listEmployees(Model model) {
        model.addAttribute("employees", employeeRepository.findAll());
        return "enployeeList";
    }

    //Client

    //Vendor

    //InvoiceMaster
    @GetMapping("/invoicesMaster/add")
    public String showAddInvoiceForm(Model model) {
        model.addAttribute("employee", new Employee());
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
        return "redirect:/api/v1/invoicesMasterList";
    }


    @GetMapping("/invoicesMasterList")
    public String listMasterList(Model model) {
        model.addAttribute("invoiceMasterList", invoiceMasterRepository.findAll());
        return "invoicesMasterList";
    }


}
