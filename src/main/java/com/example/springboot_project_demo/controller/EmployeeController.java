package com.example.springboot_project_demo.controller;

import com.example.springboot_project_demo.exception.ResourceNotFoundException;
import com.example.springboot_project_demo.model.Employee;
import com.example.springboot_project_demo.repository.EmployeeRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.net.http.HttpResponse;
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
    public String checkLogin(
            @RequestParam(name = "name") String name,
            @RequestParam(name = "password") String password,
            @RequestParam(name = "remember") String remember,
            HttpSession session,
            HttpServletResponse response,
            Model model
    ) {
        employeeList = employeeRepository.findAll();
        for (int i=0;i<employeeList.size();i++) {
            if (employeeList.get(i).getName().compareToIgnoreCase(name)==0&&
            employeeList.get(i).getPassword().compareTo(getSHAHash(password))==0) {
                session.setAttribute("nameS",employeeList.get(i).getName());
                session.setAttribute("roleS",employeeList.get(i).getRole());

                //Remember me
                /** Username and Password of an Employee with checkbox will be saved
                 *
                 */
                Cookie usernameCookie = new Cookie("usernameCookie",name);
                Cookie passwordCookie = new Cookie("passwordCookie",password);
                Cookie rememberCookie = new Cookie("rememberCookie",remember);
                if (remember.compareTo("remember")==0) {
                    response.addCookie(usernameCookie);
                    response.addCookie(passwordCookie);
                    response.addCookie(rememberCookie);
                    usernameCookie.setMaxAge(60*60);
                    passwordCookie.setMaxAge(60*60);
                    rememberCookie.setMaxAge(60*60);
                    session.setAttribute("usernameCookie",usernameCookie.getValue());
                    session.setAttribute("passwordCookie",passwordCookie.getValue());
                    session.setAttribute("rememberCookie",rememberCookie.getValue());
                } else if (remember.compareTo("noRemember")==0) {
                    response.addCookie(usernameCookie);
                    response.addCookie(passwordCookie);
                    response.addCookie(rememberCookie);
                    usernameCookie.setMaxAge(0);
                    passwordCookie.setMaxAge(0);
                    rememberCookie.setMaxAge(0);
                }

                model.addAttribute("listEmployees",employeeList);
                /** Khi login thanh cong thi nameS va roleS da duoc luu vao session
                 * Nen nhung thao tac tra ve trang thi chi can goi ra
                 * Vi da set san bien public o tren
                 */
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

    // SHA Hash code
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
        model.addAttribute("listEmployees",employeeList);
        model.addAttribute("nameS",nameS);
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

        Employee newEmployee = new Employee();
        newEmployee.setName(name);
        newEmployee.setPhone(phone);
        newEmployee.setEmail(email);
        newEmployee.setPassword(getSHAHash(password));
        newEmployee.setRole(role);
        employeeRepository.save(newEmployee);
        if (session.getAttribute("nameS")==null) {
            model.addAttribute("confirm","Create account successfully!");
            return "login";
        }

        employeeList = employeeRepository.findAll();
        model.addAttribute("nameS",nameS);
        model.addAttribute("roleS",roleS);
        model.addAttribute("listEmployees",employeeList);
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
            model.addAttribute("nameS",nameS);
            model.addAttribute("roleS",roleS);
            model.addAttribute("listEmployees",employeeList);
            return "home";
        } else {
            List<Employee> employeeListSearch = new ArrayList<>();
            for (int i=0;i<employeeList.size();i++) {
                if (employeeList.get(i).getName().compareToIgnoreCase(name)==0) {
                    employeeListSearch.add(employeeList.get(i));
                }
            }
            model.addAttribute("listUsers",employeeListSearch);
            model.addAttribute("nameS",nameS);
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

        employeeList = employeeRepository.findAll();
        model.addAttribute("listEmployees",employeeList);
        model.addAttribute("nameS",nameS);
        model.addAttribute("roleS",roleS);

        // Edit with role
        if (
                name.compareTo(nameS)==0
        ) {
            model.addAttribute("errorEdit","errorEdit");
            return "home";
        } else if (roleS.compareTo("Manager") == 0 && role.compareTo("Staff") != 0) {
            model.addAttribute("errorEdit","errorEdit");
            return "home";
        }

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
        model.addAttribute("listEmployees",employeeList);
        model.addAttribute("nameS",nameS);
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

        // Delete with role
        if (
                employee.getName().compareTo(nameS)==0
        ) {
            model.addAttribute("errorDelete","errorDelete");
            return "home";
        } else if (roleS.compareTo("Manager") == 0 && employee.getRole().compareTo("Staff") != 0) {
            model.addAttribute("errorDelete","errorDelete");
            return "home";
        }

        employeeRepository.delete(employee);

        employeeList = employeeRepository.findAll();
        model.addAttribute("listEmployees",employeeList);
        model.addAttribute("nameS",nameS);
        model.addAttribute("roleS",roleS);
        return "home";
    }

    // log out employee
    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        request.getSession().invalidate();
        return "login";
    }

    // view confirmEmployee Page
    @GetMapping("/forgot-Password")
    public String viewConfirmEmployee() {
        return "confirmEmployee";
    }

    // check Employee forgot password existed
    @PostMapping("/confirmEmployee")
    public String confirmEmployee(
            @RequestParam("name") String name,
            @RequestParam("phone") String phone,
            @RequestParam("email") String email,
            @RequestParam("role") String role,
            Model model,
            HttpSession session
    ) {
        employeeList = employeeRepository.findAll();
        for (int i=0;i<employeeList.size();i++) {
            if (
                    employeeList.get(i).getName().compareToIgnoreCase(name)==0&&
                            employeeList.get(i).getPhone().compareToIgnoreCase(phone)==0&&
                            employeeList.get(i).getEmail().compareTo(email)==0&&
                            employeeList.get(i).getRole().compareTo(role)==0
            ) {
                Long realEmployeeId = employeeList.get(i).getId();
                session.setAttribute("realEmployeeId",realEmployeeId);
                return "renewPassword";
            }
        }
        model.addAttribute("fakeEmployee","fakeEmployee");
        return "confirmEmployee";
    }

    // Renew password
    @PostMapping("/renewPassword")
    public String confirmEmployee(
            @RequestParam("newPassword") String newPassword,
            Model model,
            HttpSession session,
            HttpServletRequest request
    ){
        if (session.getAttribute("realEmployeeId")==null) {
            model.addAttribute("fakeEmployee","noEmployee");
            return "confirmEmployee";
        }
        Long id = (Long) session.getAttribute("realEmployeeId");
        Employee renewPasswordEmployee = employeeRepository.findById(id).orElseThrow(() ->
                new ResourceNotFoundException("Employee not exist with id: " + id));
        renewPasswordEmployee.setPassword(getSHAHash(newPassword));
        employeeRepository.save(renewPasswordEmployee);
        request.getSession().invalidate();
        model.addAttribute("confirm","Renew Password Successfully!");
        return "login";
    }

}
