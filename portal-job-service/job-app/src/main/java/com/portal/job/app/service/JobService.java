package com.portal.job.app.service;

import com.portal.job.domain.Category;
import com.portal.job.domain.CategoryRepository;
import com.portal.job.domain.Job;
import com.portal.job.domain.JobRepository;
import com.portal.job.rest.dto.JobSearchRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;

import java.util.List;

@Service
public class JobService {

    @Autowired
    private JobRepository jobRepository ;

    @Autowired
    private CategoryRepository categoryRepository;

    public Page<Job> searchJobs (JobSearchRequest request)
    {
        Specification<Job> spec =  JobSpecification.searchJobs(request);
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return jobRepository.findAll(spec,pageable);

    }
    public List<Category> getAllCategories(){
        return categoryRepository.findAll();
    }
}
