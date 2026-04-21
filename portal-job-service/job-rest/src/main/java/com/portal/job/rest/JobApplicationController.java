package com.portal.job.rest;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.portal.job.app.service.JobApplicationService;
import com.portal.job.domain.JobApplication;

@RestController
@RequestMapping("/api/applications")
public class JobApplicationController {

    @Autowired
    private JobApplicationService jobApplicationService;

    @PutMapping("/{id}/status")
    public JobApplication updateStatus(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        return jobApplicationService.updateApplicationStatus(id, body.get("status"));
    }
}