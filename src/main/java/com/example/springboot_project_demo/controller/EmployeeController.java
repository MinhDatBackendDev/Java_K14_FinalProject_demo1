package com.example.springboot_project_demo.controller;

import com.example.springboot_project_demo.exception.ResourceNotFoundException;
import com.example.springboot_project_demo.model.Employee;
import com.example.springboot_project_demo.repository.EmployeeRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    private List<Employee> employeeList = new ArrayList<>();
    private String nameS;
    private String roleS;
    private int check;

    // view Login page and check session and cookie API
    @GetMapping("/login")
    public String viewLogin(
            HttpSession session,
            HttpServletRequest request,
            Model model
    ) {
        // check session
        if (session.getAttribute("nameS")!=null) {
            model.addAttribute("listEmployees",employeeList);
            model.addAttribute("nameS",nameS);
            model.addAttribute("roleS",roleS);
            return "home";
        }

        // get cookie
        Cookie[] cookies = request.getCookies();
        for (int i=0;i<cookies.length;i++) {
            if (cookies[i].getName().compareTo("usernameCookie")==0) {
                model.addAttribute("usernameCookie",cookies[i].getValue());
            }
            if (cookies[i].getName().compareTo("passwordCookie")==0) {
                model.addAttribute("passwordCookie",cookies[i].getValue());
            }
            if (cookies[i].getName().compareTo("rememberCookie")==0) {
                model.addAttribute("rememberCookie",cookies[i].getValue());
            }
        }
        return "login";
    }

    // check Login API
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

                /** Remember me
                 * Username and Password of an Employee with checkbox will be saved
                 */
                Cookie usernameCookie = new Cookie("usernameCookie",name);
                Cookie passwordCookie = new Cookie("passwordCookie",password);
                Cookie rememberCookie = new Cookie("rememberCookie",remember);
                if (remember.compareTo("remember")==0) {
                    usernameCookie.setMaxAge(60*60);
                    passwordCookie.setMaxAge(60*60);
                    rememberCookie.setMaxAge(60*60);
                } else if (remember.compareTo("noRemember")==0) {
                    usernameCookie.setMaxAge(0);
                    passwordCookie.setMaxAge(0);
                    rememberCookie.setMaxAge(0);
                }
                response.addCookie(usernameCookie);
                response.addCookie(passwordCookie);
                response.addCookie(rememberCookie);

                /** Khi login thanh cong thi employeeList, nameS va roleS da duoc luu vao session
                 * Nen nhung thao tac tra ve trang home thi chi can goi ra ngoai tru delete
                 * Vi da set san variable public o tren
                 */
                session.setAttribute("nameS",employeeList.get(i).getName());
                session.setAttribute("roleS",employeeList.get(i).getRole());
                model.addAttribute("listEmployees",employeeList);
                nameS = (String) session.getAttribute("nameS");
                model.addAttribute("nameS",nameS);
                roleS = (String) session.getAttribute("roleS");
                model.addAttribute("roleS",roleS);
                return "home";
            }
        }

        model.addAttribute("usernameCookie",name);
        model.addAttribute("passwordCookie",password);
        model.addAttribute("rememberCookie",remember);
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
        model.addAttribute("listEmployees",employeeList);
        model.addAttribute("nameS",nameS);
        model.addAttribute("roleS",roleS);
        return "home";
    }

    @GetMapping("/register")
    public String viewRegister(Model model,
                               HttpSession session) {
        if (session.getAttribute("nameS")==null) {
            model.addAttribute("formName", "Registration");
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

        // check employee is existed or not non logged in account
        employeeList = employeeRepository.findAll(); //get the list because of non logged in account
        check = 0;
        for (int i=0; i<employeeList.size(); i++) {
            if (employeeList.get(i).getPhone().compareTo(phone)==0) {
                check++;
                model.addAttribute("invalidPhone",phone+" is already in used!");
            }
            if (employeeList.get(i).getEmail().compareTo(email)==0) {
                check++;
                model.addAttribute("invalidEmail",email+" is already in used!");
            }
        }
        if (check > 0) {
            model.addAttribute("name",name);
            model.addAttribute("phone",phone);
            model.addAttribute("email",email);
            model.addAttribute("password",password);
            model.addAttribute("role",role);
            if (session.getAttribute("nameS")==null) {
                model.addAttribute("formName", "Registration");
            } else {
                model.addAttribute("formName", "New Employee");
            }
            return "register";
        }

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
        model.addAttribute("confirmCreate","Create new employee with name is "+name+" successfully!");
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
            model.addAttribute("listEmployees",employeeListSearch);
            model.addAttribute("nameS",nameS);
            model.addAttribute("roleS",roleS);
            return "home";
        }
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

        // check employee is existed or not with logged in account
        check = 0;
        for (int i=0; i<employeeList.size(); i++) {
            if (
                    employeeList.get(i).getId()!=id&&
                    employeeList.get(i).getPhone().compareTo(phone)==0
            ) {
                model.addAttribute("invalidPhone",phone+" is already in used!");
                check++;
            }
            if (
                    employeeList.get(i).getId()!=id&&
                    employeeList.get(i).getEmail().compareTo(email)==0
            ) {
                model.addAttribute("invalidEmail",email+" is already in used!");
                check++;
            }
        }
        if (check > 0) {
            model.addAttribute("name",name);
            model.addAttribute("phone",phone);
            model.addAttribute("email",email);
            model.addAttribute("role",role);
            return "update";
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
        model.addAttribute(
                "confirmEdit",
                "Edit employee with name is "+name+" successfully!"
        );
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
        String name = employee.getName();

        // Delete with role
        if (
                name.compareTo(nameS)==0
        ) {
            model.addAttribute("errorDelete","errorDelete");
            return "home";
        } else if (roleS.compareTo("Manager") == 0 && employee.getRole().compareTo("Staff") != 0) {
            model.addAttribute("errorDelete","errorDelete");
            return "home";
        }

        employeeRepository.delete(employee);

        // Luu y delete xong thi phai tim lai
        employeeList = employeeRepository.findAll();
        model.addAttribute("listEmployees",employeeList);
        model.addAttribute("nameS",nameS);
        model.addAttribute("roleS",roleS);
        model.addAttribute(
                "confirmDelete",
                "Delete employee with name is "+name+" successfully!"
        );
        return "home";
    }

    // log out employee
    @GetMapping("/logout")
    public String logout(
            HttpServletRequest request
    ) {
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
        model.addAttribute("name",name);
        model.addAttribute("phone",phone);
        model.addAttribute("email",email);
        model.addAttribute("role",role);
        model.addAttribute("errorEmployee","fakeEmployee");
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
            model.addAttribute("errorEmployee","noEmployee");
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
