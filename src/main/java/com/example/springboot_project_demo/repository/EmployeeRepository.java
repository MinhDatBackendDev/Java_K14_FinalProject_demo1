package com.example.springboot_project_demo.repository;

import com.example.springboot_project_demo.model.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {
}
