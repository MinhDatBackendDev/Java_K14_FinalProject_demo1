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
    private long idS; // idS for keeping the page position
    private int limit = 5; // This limit must match the limit in the pagination js

    // View Login page and check session and cookie API
    @GetMapping("/login")
    public String viewLogin(
            HttpSession session,
            HttpServletRequest request,
            Model model
    ) {
        // Check session when logged-in user try to get back to the login page
        if (session.getAttribute("nameS")!=null) {
            // Keep page position that has the user
            model.addAttribute("pagePosition",keepPagePosition(idS,employeeList));
            // Return all information for the home page
            model.addAttribute("employeeList",employeeList);
            model.addAttribute("nameS",nameS);
            model.addAttribute("roleS",roleS);
            return "home";
        }
        // Get cookie to fill the form
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

    // Check Login API
    @PostMapping("/login")
    public String checkLogin(
            @RequestParam(name = "name") String name,
            @RequestParam(name = "password") String password,
            @RequestParam(name = "remember") String remember,
            HttpSession session,
            HttpServletResponse response,
            Model model
    ) {
        // Get the employee list to check login
        employeeList = employeeRepository.findAll();
        for (int i=0; i<employeeList.size(); i++) {
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

                // Set session attributes
                session.setAttribute("nameS",employeeList.get(i).getName());
                session.setAttribute("roleS",employeeList.get(i).getRole());
                session.setAttribute("idS",employeeList.get(i).getId());
                nameS = (String) session.getAttribute("nameS");
                idS = (long) session.getAttribute("idS");
                roleS = (String) session.getAttribute("roleS");

                // Keep pagePosition for login and search
                model.addAttribute("pagePosition",keepPagePosition(idS,employeeList));
                model.addAttribute("employeeList",employeeList);
                model.addAttribute("nameS",nameS);
                model.addAttribute("roleS",roleS);
                return "home";
            }
        }
        // Return all inputs that user entered
        model.addAttribute("error","Wrong username or password!");
        model.addAttribute("usernameCookie",name);
        model.addAttribute("passwordCookie",password);
        model.addAttribute("rememberCookie",remember);
        return "login";
    }

    // View Forgot password page API
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
        // Verify user is a real employee or not
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
        // Return all inputs that user entered
        model.addAttribute("errorEmployee","fakeEmployee");
        model.addAttribute("name",name);
        model.addAttribute("phone",phone);
        model.addAttribute("email",email);
        model.addAttribute("role",role);
        return "confirmEmployee";
    }

    // Renew password API
    @PostMapping("/renewPassword")
    public String confirmEmployee(
            @RequestParam("newPassword") String newPassword,
            Model model,
            HttpSession session,
            HttpServletRequest request
    ){
        // Prevent user from skipping the verification
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

    // View home page API
    @GetMapping( "/home")
    public String viewHome(Model model,
                           HttpSession session) {
        // Prevent user from skipping login
        if (session.getAttribute("nameS")==null) {
            model.addAttribute("error","Please login!");
            return "login";
        }
        // Keep page position that has the user
        model.addAttribute("pagePosition",keepPagePosition(idS,employeeList));
        // Return all information for the home page
        model.addAttribute("employeeList",employeeList);
        model.addAttribute("nameS",nameS);
        model.addAttribute("roleS",roleS);
        return "home";
    }

    // View register page API
    @GetMapping("/register")
    public String viewRegister(Model model,
                               HttpSession session) {
        // Show the name of the form depending on session
        if (session.getAttribute("nameS")==null) {
            model.addAttribute("formName", "Registration");
            return "register";
        } else {
            model.addAttribute("formName", "New Employee");
            return "register";
        }
    }

    // Create new employee API
    @PostMapping("/register")
    public String createEmployee(@RequestParam(name = "name") String name,
                                 @RequestParam(name = "phone") String phone,
                                 @RequestParam(name = "email") String email,
                                 @RequestParam(name = "password") String password,
                                 @RequestParam(name = "role") String role,
                                 Model model,
                                 HttpSession session) {
        // Check employee is existed or not whether user logged in or not
        employeeList = employeeRepository.findAll(); // Get the list to check whether user logged-in or not
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
            // Return all inputs that user entered and the errors above
            model.addAttribute("name",name);
            model.addAttribute("phone",phone);
            model.addAttribute("email",email);
            model.addAttribute("password",password);
            model.addAttribute("role",role);
            // Return the name of the form depending on session
            if (session.getAttribute("nameS")==null) {
                model.addAttribute("formName", "Registration");
            } else {
                model.addAttribute("formName", "New Employee");
            }
            return "register";
        }
        // Save the new employee
        Employee newEmployee = new Employee();
        newEmployee.setName(name);
        newEmployee.setPhone(phone);
        newEmployee.setEmail(email);
        newEmployee.setPassword(getSHAHash(password));
        newEmployee.setRole(role);
        employeeRepository.save(newEmployee);
        // Return to login page with non logged-in user
        if (session.getAttribute("nameS")==null) {
            model.addAttribute("confirm","Create account successfully!");
            return "login";
        }
        // Refresh the list after change
        employeeList = employeeRepository.findAll();
        // Return the page position that has the new employee
        long pagePosition = (long) Math.ceil((float)employeeList.size()/limit);
        model.addAttribute("pagePosition",pagePosition);
        // Return all information for the home page and the confirmation of creating new employee
        model.addAttribute("nameS",nameS);
        model.addAttribute("roleS",roleS);
        model.addAttribute("employeeList",employeeList);
        model.addAttribute("confirmCreate","Create new employee with name is "+name+" successfully!");
        return "home";
    }

    // Search employees by name API
    @RequestMapping(value = "/searchEmployee", method = RequestMethod.POST)
    public String getEmployeeByName(@RequestParam ("nameSearch") String name,
                                    Model model,
                                    HttpSession session){
        // Prevent user from skipping login
        if (session.getAttribute("nameS")==null) {
            model.addAttribute("error","Please login!");
            return "login";
        }
        // Click the button "Search" with no input
        if (name.compareTo("")==0) {
            // Return all information for the home page
            model.addAttribute("nameS",nameS);
            model.addAttribute("roleS",roleS);
            model.addAttribute("employeeList",employeeList);
            // Keep page position that has the user
            model.addAttribute("pagePosition",keepPagePosition(idS,employeeList));
            return "home";
        } else {
            // Return the list of employees with the same name (ignore case)
            List<Employee> employeeListSearch = new ArrayList<>();
            for (int i=0;i<employeeList.size();i++) {
                if (employeeList.get(i).getName().compareToIgnoreCase(name)==0) {
                    employeeListSearch.add(employeeList.get(i));
                }
            }
            // If the employee is not found, return the error in the home page
            if (employeeListSearch.size()==0) {
                model.addAttribute("errorSearch","Employee with name is "+name+" is not found!");
            }
            // Return all searches in the first page
            model.addAttribute("pagePosition",1);
            // Return all information for the home page
            model.addAttribute("employeeList",employeeListSearch);
            model.addAttribute("nameS",nameS);
            model.addAttribute("roleS",roleS);
            return "home";
        }
    }

    // View update page API
    @GetMapping("/edit/{id}")
    public String viewUpdate(@PathVariable long id,
                             Model model,
                             HttpSession session
    ) {
        // Prevent user from skipping login
        if (session.getAttribute("nameS")==null) {
            model.addAttribute("error","Please login!");
            return "login";
        }
        // Find the employee by id and get all information
        Employee updateEmployee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not exist with id: " + id));
        String name = updateEmployee.getName();
        String phone = updateEmployee.getPhone();
        String email = updateEmployee.getEmail();
        String role = updateEmployee.getRole();
        // Return all information for the home page if error occurs
        model.addAttribute("employeeList",employeeList);
        model.addAttribute("nameS",nameS);
        model.addAttribute("roleS",roleS);
        // Check the role and the name of the user in order to prevent user from editing himself and higher role employees
        if (
                name.compareTo(nameS)==0
        ) {
            model.addAttribute("errorEdit","You can not edit this employee!");
            // Keep page position that user is doing editing
            model.addAttribute("pagePosition",keepPagePosition(id,employeeList));
            return "home";
        } else if (roleS.compareTo("Manager") == 0 && role.compareTo("Staff") != 0) {
            model.addAttribute("errorEdit","You can not edit this employee!");
            // Keep page position that user is doing editing
            model.addAttribute("pagePosition",keepPagePosition(id,employeeList));
            return "home";
        }
        // Send all employee's information for updating
        model.addAttribute("name",name);
        model.addAttribute("phone",phone);
        model.addAttribute("email",email);
        model.addAttribute("role",role);
        return "update";
    }

    // Update employee API
    @PostMapping("/update/{id}")
    public String updateEmployee(@PathVariable long id,
                                 @RequestParam("name") String name,
                                 @RequestParam("phone") String phone,
                                 @RequestParam("email") String email,
                                 @RequestParam("role") String role,
                                 Model model,
                                 HttpSession session) {
        // Prevent user from skipping login
        if (session.getAttribute("nameS")==null) {
            model.addAttribute("error","Please login!");
            return "login";
        }
        // Keep page position that user is doing editing
        model.addAttribute("pagePosition",keepPagePosition(id,employeeList));
        // check employee is existed or not with logged-in user
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
            // Return all inputs that user entered in the form if error occurs
            model.addAttribute("name",name);
            model.addAttribute("phone",phone);
            model.addAttribute("email",email);
            model.addAttribute("role",role);
            return "update";
        }
        // Update new employee
        Employee updateEmployee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not exist with id: " + id));
        updateEmployee.setName(name);
        updateEmployee.setPhone(phone);
        updateEmployee.setEmail(email);
        updateEmployee.setRole(role);
        employeeRepository.save(updateEmployee);
        // Refresh the list after editing
        employeeList = employeeRepository.findAll();
        // Return all information for the home page and the confirmation
        model.addAttribute("employeeList",employeeList);
        model.addAttribute("nameS",nameS);
        model.addAttribute("roleS",roleS);
        model.addAttribute(
                "confirmEdit",
                "Edit employee with name is "+name+" successfully!"
        );
        return "home";
    }

    // Delete employee API
    @GetMapping("/delete/{id}")
    public String deleteEmployee(@PathVariable long id,
                                 Model model,
                                 HttpSession session){
        // Prevent user from skipping login
        if (session.getAttribute("nameS")==null) {
            model.addAttribute("error","Please login!");
            return "login";
        }
        // Find the employee by id for checking and deleting
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not exist with id: " + id));
        String name = employee.getName();
        // Keep page position that user is doing deleting
        model.addAttribute("pagePosition",keepPagePosition(id,employeeList));
        // Check the role and the name of the user in order to prevent user from deleting himself and higher role employees
        if (
                name.compareTo(nameS)==0
        ) {
            // Return all information for the home page and the error
            model.addAttribute("errorDelete","You can not delete this employee!");
            model.addAttribute("employeeList",employeeList);
            model.addAttribute("nameS",nameS);
            model.addAttribute("roleS",roleS);
            return "home";
        } else if (roleS.compareTo("Manager") == 0 && employee.getRole().compareTo("Staff") != 0) {
            // Return all information for the home page and the error
            model.addAttribute("errorDelete","You can not delete this employee!");
            model.addAttribute("employeeList",employeeList);
            model.addAttribute("nameS",nameS);
            model.addAttribute("roleS",roleS);
            return "home";
        }
        // Delete the employee
        employeeRepository.delete(employee);
        // Refresh the list after deleting
        employeeList = employeeRepository.findAll();
        // Return all information for the home page and the confirmation
        model.addAttribute("employeeList",employeeList);
        model.addAttribute("nameS",nameS);
        model.addAttribute("roleS",roleS);
        model.addAttribute(
                "confirmDelete",
                "Delete employee with name is "+name+" successfully!"
        );
        return "home";
    }

    // Log out API
    @GetMapping("/logout")
    public String logout(
            HttpServletRequest request
    ) {
        request.getSession().invalidate();
        return "login";
    }

    // Keep the page position that has the logged in user
    // Keep the page position that user is doing the actions (view home, search, edit, delete)
    public static long keepPagePosition(
            long employeeId,
            List<Employee> employeeList
    ) {
        long pagePosition = 1;
        int limit = 5;
        for (int i=0; i<employeeList.size(); i++) {
            if (employeeList.get(i).getId()==employeeId) {
                pagePosition = (long) Math.ceil((float)(i+1)/limit);
            }
        }
        return pagePosition;
    }

    // Get SHA Hash code for the password
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

}
