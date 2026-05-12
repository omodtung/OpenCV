package com.portal.job.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate; // Thêm import cho LocalDate
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class JobSearchRequest {
    private String keyword ;
    private String location ;
    private Set<UUID> categories ; // thay doi thanh uuid cho categories Ids
    private List<String> employmentTypes ;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private LocalDate datePostedAfter ; // them truong loc ngay dang sau
    private int page = 0 ;
    private int size = 10 ;

    public String getKeyword() {
        return keyword;
    }

    public JobSearchRequest setKeyword(String keyword) {
        this.keyword = keyword;
        return this;
    }

    public String getLocation() {
        return location;
    }

    public JobSearchRequest setLocation(String location) {
        this.location = location;
        return this;
    }

    public Set<UUID> getCategories() {
        return categories;
    }

    public JobSearchRequest setCategories(Set<UUID> categories) {
        this.categories = categories;
        return this;
    }

    public List<String> getEmploymentTypes() {
        return employmentTypes;
    }

    public JobSearchRequest setEmploymentTypes(List<String> employmentTypes) {
        this.employmentTypes = employmentTypes;
        return this;
    }

    public BigDecimal getSalaryMin() {
        return salaryMin;
    }

    public JobSearchRequest setSalaryMin(BigDecimal salaryMin) {
        this.salaryMin = salaryMin;
        return this;
    }

    public BigDecimal getSalaryMax() {
        return salaryMax;
    }

    public JobSearchRequest setSalaryMax(BigDecimal salaryMax) {
        this.salaryMax = salaryMax;
        return this;
    }

    public LocalDate getDatePostedAfter() {
        return datePostedAfter;
    }

    public JobSearchRequest setDatePostedAfter(LocalDate datePostedAfter) {
        this.datePostedAfter = datePostedAfter;
        return this;
    }

    public int getPage() {
        return page;
    }

    public JobSearchRequest setPage(int page) {
        this.page = page;
        return this;
    }

    public int getSize() {
        return size;
    }

    public JobSearchRequest setSize(int size) {
        this.size = size;
        return this;
    }

    public boolean ket() {
    }
}
