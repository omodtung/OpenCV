package com.portal.employer.app.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portal.employer.app.messaging.EmployerMessageSender;
import com.portal.employer.domain.Company;
import com.portal.employer.domain.CompanyRepository;
import com.portal.employer.domain.Employer;
import com.portal.employer.domain.EmployerRepository;

@Service
public class EmployerProfileService {

    @Autowired
    private EmployerRepository employerRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private EmployerMessageSender employerMessageSender;

    @Transactional
    public Employer createProfile(UUID userId, String companyName) {
        Employer employer = new Employer();
        employer.setUserId(userId);
        employer.setCompanyName(companyName);
        return employerRepository.save(employer);
    }

    @Transactional
    public Company createCompany(String companyName, String description, String website, String address) {
        Company company = new Company();
        company.setCompanyName(companyName);
        company.setDescription(description);
        company.setWebsite(website);
        company.setAddress(address);
        Company saved = companyRepository.save(company);

        Map<String, String> headers = employerMessageSender.getDefaultMessageHeaders(saved.getId().toString());
        headers.put("companyId", saved.getId().toString());
        headers.put("companyName", saved.getCompanyName());
        employerMessageSender.sendCompanyCreated(null, headers);

        return saved;
    }

    @Transactional
    public Company updateCompany(UUID companyId, String companyName, String description) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found: " + companyId));
        company.setCompanyName(companyName);
        company.setDescription(description);
        Company saved = companyRepository.save(company);

        Map<String, String> headers = employerMessageSender.getDefaultMessageHeaders(saved.getId().toString());
        headers.put("companyId", saved.getId().toString());
        headers.put("companyName", saved.getCompanyName());
        employerMessageSender.sendCompanyUpdated(null, headers);

        return saved;
    }
}