package com.portal.job.app.service;

import com.portal.job.domain.Category;
import com.portal.job.domain.Job;
import com.portal.job.rest.dto.JobSearchRequest;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class JobSpecification {
    public static Specification<Job> searchJobs(JobSearchRequest request) {
        return (root , query , criteriaBuilder )->
        {
            List<Predicate> predicates = new ArrayList<>() ;
            if  (request.getKeyword() != null  && !request.getKeyword().isEmpty()){
                String likePattern ='%'+ request.getKeyword().toLowerCase() + '%';
                Predicate titlePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), likePattern);
                Predicate descriptionPredicate =  criteriaBuilder.like(criteriaBuilder.lower(root.get("description")),likePattern );
                Predicate companyNamePredicate =criteriaBuilder.like(criteriaBuilder.lower(root.get("company")),likePattern );
                predicates.add(criteriaBuilder.or(titlePredicate,descriptionPredicate,companyNamePredicate)) ;
            }
            if (request.getLocation() != null && !request.getLocation().isEmpty())
            {
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("location")),"%" + request.getLocation().toLowerCase()+ "%"));
            }
            if (request.getCategories()!= null && !request.getCategories().isEmpty() )
            {
                Join<Job, Category> jobCategryJoin = root.join("categories");
                predicates.add(jobCategryJoin.get("id").in(request.getCategories()));
            }
            if(request.getEmploymentTypes()!= null && !request.getEmploymentTypes().isEmpty() )
            {
                predicates.add(root.get("employmentType").in(request.getEmploymentTypes())) ;

            }


            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));


        };
    }
}
