package com.portal.job.app.service;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.portal.job.domain.JobRepository;

@Service
public class CompanySyncService {

    @Autowired
    private JobRepository jobRepository;

    @Transactional
    public void syncCompanyName(UUID companyId, String newName) {
        jobRepository.updateCompanyName(companyId, newName);
    }
}