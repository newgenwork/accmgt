package com.act.web.controller;

import com.act.model.Employee;
import com.act.repo.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("api/v1/")
@RequiredArgsConstructor
public class MainController {

    private final EmployeeRepository employeeRepository;

    @GetMapping("/login")
    public String getLoginPage(Model model) {
        return "login";
    }
    @GetMapping("/employeeMaster")
    public String getEmployeePage(Model model) {
        return "employeeMaster";
    }

    @GetMapping
    public String getHomePage() {
        return "index";
    }


    // Show form
    @GetMapping("/employees/add")
    public String showAddEmployeeForm(Model model) {

        model.addAttribute("employee", new Employee());

        return "employee-add";
    }

    // Handle submit
    @PostMapping("/employees/add")
    public String saveEmployee(@ModelAttribute Employee employee) {
        employeeRepository.save(employee);
        return "redirect:/employees";  // redirect to list page
    }


    @GetMapping("/employees")
    public String listEmployees(Model model) {
        model.addAttribute("employees", employeeRepository.findAll());
        return "employee-list";
    }


}
