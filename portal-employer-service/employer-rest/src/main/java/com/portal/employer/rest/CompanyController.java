package com.portal.employer.rest;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.portal.employer.app.service.EmployerProfileService;
import com.portal.employer.domain.Company;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    @Autowired
    private EmployerProfileService employerProfileService;

    @PostMapping
    public ResponseEntity<Company> create(@RequestBody Map<String, String> body) {
        Company company = employerProfileService.createCompany(
                body.get("companyName"), body.get("description"),
                body.get("website"), body.get("address"));
        return ResponseEntity.ok(company);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Company> update(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        Company company = employerProfileService.updateCompany(id, body.get("companyName"), body.get("description"));
        return ResponseEntity.ok(company);
    }
}