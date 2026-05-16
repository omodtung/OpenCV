package com.portal.job.rest;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import com.portal.job.app.service.JobService;
import com.portal.job.domain.Category;
import com.portal.job.rest.dto.JobSearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.portal.job.domain.Job;
import com.portal.job.domain.JobRepository;
@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobService jobService ; // them JobService
    @GetMapping
    public List<Job> getAllJobs() {
        return jobRepository.findAll();
    }
    @PostMapping
    public Job createJob(@RequestBody Job job) {
        return jobRepository.save(job);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Job> getJob(@PathVariable UUID id) {
        return jobRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
    @GetMapping("/categories")
    public List<Category> getAllCategories(){
        return jobService.getAllCategories();
    }
    @PostMapping("/search")
    public Page<Job> searchJobs(@RequestBody JobSearchRequest request)
    {
        return jobService.searchJobs(request);
    }
}