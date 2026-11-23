package com.bookit.employee.controller;

import com.bookit.employee.dto.PublicEmployeeResponse;
import com.bookit.employee.service.EmployeeService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Businesses")
@RestController
@RequestMapping("/businesses/{slug}/employees")
@RequiredArgsConstructor
public class PublicEmployeeController {

    private final EmployeeService employeeService;

    @GetMapping(version = "1")
    public List<PublicEmployeeResponse> getBusinessEmployees(@PathVariable String slug) {
        return employeeService.getPublic(slug);
    }
}
