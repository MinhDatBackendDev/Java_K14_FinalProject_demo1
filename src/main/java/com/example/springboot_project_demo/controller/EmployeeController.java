package com.example.springboot_project_demo.controller;

import com.example.springboot_project_demo.exception.ResourceNotFoundException;
import com.example.springboot_project_demo.model.Employee;
import com.example.springboot_project_demo.repository.EmployeeRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;


@Controller //Controller va RestController khac gi nhau
@RequestMapping("/api/v1/employees")
public class EmployeeController {

    @Autowired
    private EmployeeRepository employeeRepository;

    List<Employee> employeeList = new ArrayList<>();
    String nameS;
    String roleS;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @PostMapping("/login")
    public String checkLogin(@RequestParam(name = "name") String name,
                        @RequestParam(name = "password") String password,
                        HttpSession session,
                        Model model) {
        password = getSHAHash(password);
        employeeList = employeeRepository.findAll();
        for (int i=0;i<employeeList.size();i++) {
            if (employeeList.get(i).getName().compareToIgnoreCase(name)==0&&
            employeeList.get(i).getPassword().compareTo(password)==0) {
                session.setAttribute("nameS",employeeList.get(i).getName());
                session.setAttribute("roleS",employeeList.get(i).getRole());
                model.addAttribute("listUsers",employeeList);
                nameS = (String) session.getAttribute("nameS");
                model.addAttribute("nameS",nameS);
                roleS = (String) session.getAttribute("roleS");
                model.addAttribute("roleS",roleS);
                return "home";
            }
        }
        model.addAttribute("error","Wrong username or password!");
        return "login";
    }

    public static String getSHAHash(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] messageDigest = md.digest(input.getBytes());
            return convertByteToHex(messageDigest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String convertByteToHex(byte[] data) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < data.length; i++) {
            sb.append(Integer.toString((data[i] & 0xff) + 0x100, 16).substring(1));
        }
        return sb.toString();
    }

    @GetMapping( "/home")
    public String viewHome(Model model,
                           HttpSession session) {
        if (session.getAttribute("nameS")==null) {
            model.addAttribute("error","Please login!");
            return "login";
        }
        employeeList = employeeRepository.findAll();
        model.addAttribute("listUsers",employeeList);
        nameS = (String) session.getAttribute("nameS");
        model.addAttribute("nameS",nameS);
        roleS = (String) session.getAttribute("roleS");
        model.addAttribute("roleS",roleS);
        return "home";
    }

    @GetMapping("/register")
    public String viewRegister(Model model,
                               HttpSession session) {
        if (session.getAttribute("nameS")==null) {
            model.addAttribute("formName", "Register");
            return "register";
        } else {
            model.addAttribute("formName", "New Employee");
            return "register";
        }
    }

    // build create employee REST API
    @PostMapping("/register")
    public String createEmployee(@RequestParam(name = "name") String name,
                                 @RequestParam(name = "phone") String phone,
                                 @RequestParam(name = "email") String email,
                                 @RequestParam(name = "password") String password,
                                 @RequestParam(name = "role") String role,
                                 Model model,
                                 HttpSession session) {
        password = getSHAHash(password);
        Employee newEmployee = new Employee();
        newEmployee.setName(name);
        newEmployee.setPhone(phone);
        newEmployee.setEmail(email);
        newEmployee.setPassword(password);
        newEmployee.setRole(role);
        employeeRepository.save(newEmployee);
        if (session.getAttribute("nameS")==null) {
            model.addAttribute("confirm","Create account successfully!");
            return "login";
        }
        employeeList = employeeRepository.findAll();
        model.addAttribute("listUsers",employeeList);
        nameS = (String) session.getAttribute("nameS");
        model.addAttribute("nameS",nameS);
        roleS = (String) session.getAttribute("roleS");
        model.addAttribute("roleS",roleS);
        return "home";
    }

    // build get employee by id REST API
    @RequestMapping(value = "/searchEmployee", method = RequestMethod.POST)
    public String getEmployeeByName(@RequestParam ("nameSearch") String name,
                                    Model model,
                                    HttpSession session){
        if (session.getAttribute("nameS")==null) {
            model.addAttribute("error","Please login!");
            return "login";
        }
        if (name=="") {
            List<Employee> employeeList = employeeRepository.findAll();
            model.addAttribute("listUsers",employeeList);
            nameS = (String) session.getAttribute("nameS");
            model.addAttribute("nameS",nameS);
            roleS = (String) session.getAttribute("roleS");
            model.addAttribute("roleS",roleS);
            return "home";
        } else {
            List<Employee> employeeListSearch = new ArrayList<>();
            for (int i=0;i<employeeList.size();i++) {
                if (employeeList.get(i).getName().compareToIgnoreCase(name)==0) {
                    employeeListSearch.add(employeeList.get(i));
                }
            }
            model.addAttribute("listUsers",employeeListSearch);
            nameS = (String) session.getAttribute("nameS");
            model.addAttribute("nameS",nameS);
            roleS = (String) session.getAttribute("roleS");
            model.addAttribute("roleS",roleS);
            return "home";
        }
//        Employee employee = employeeRepository.findById(Long.parseLong(id))
//                .orElseThrow(() -> new ResourceNotFoundException("Employee not exist with id:" + id));
//        List<Employee> employeeListSearch = new ArrayList<>();
//        employeeListSearch.add(employee);
//        model.addAttribute("listUsers",employeeListSearch);
//        return "home";
    }

    @GetMapping("/edit/{id}")
    public String viewUpdate(@PathVariable long id,
                             Model model,
                             HttpSession session
    ) {
        if (session.getAttribute("nameS")==null) {
            model.addAttribute("error","Please login!");
            return "login";
        }
        Employee updateEmployee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not exist with id: " + id));
        String name = updateEmployee.getName();
        String phone = updateEmployee.getPhone();
        String email = updateEmployee.getEmail();
        String role = updateEmployee.getRole();
        model.addAttribute("name",name);
        model.addAttribute("phone",phone);
        model.addAttribute("email",email);
        model.addAttribute("role",role);
        return "update";
    }

    // build update employee REST API
    @PostMapping("/update/{id}")
    public String updateEmployee(@PathVariable long id,
                                 @RequestParam("name") String name,
                                 @RequestParam("phone") String phone,
                                 @RequestParam("email") String email,
                                 @RequestParam("role") String role,
                                 Model model,
                                 HttpSession session) {
        if (session.getAttribute("nameS")==null) {
            model.addAttribute("error","Please login!");
            return "login";
        }
        Employee updateEmployee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not exist with id: " + id));
        updateEmployee.setName(name);
        updateEmployee.setPhone(phone);
        updateEmployee.setEmail(email);
        updateEmployee.setRole(role);
        employeeRepository.save(updateEmployee);
        employeeList = employeeRepository.findAll();
        model.addAttribute("listUsers",employeeList);
        nameS = (String) session.getAttribute("nameS");
        model.addAttribute("nameS",nameS);
        roleS = (String) session.getAttribute("roleS");
        model.addAttribute("roleS",roleS);
        return "home";
    }

    // build delete employee REST API
    @GetMapping("/delete/{id}")
    public String deleteEmployee(@PathVariable long id,
                                 Model model,
                                 HttpSession session){
        if (session.getAttribute("nameS")==null) {
            model.addAttribute("error","Please login!");
            return "login";
        }
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not exist with id: " + id));
        employeeRepository.delete(employee);
        employeeList = employeeRepository.findAll();
        model.addAttribute("listUsers",employeeList);
        nameS = (String) session.getAttribute("nameS");
        model.addAttribute("nameS",nameS);
        roleS = (String) session.getAttribute("roleS");
        model.addAttribute("roleS",roleS);
        return "home";
    }

    // log out employee
    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        request.getSession().invalidate();
        return "login";
    }

}
