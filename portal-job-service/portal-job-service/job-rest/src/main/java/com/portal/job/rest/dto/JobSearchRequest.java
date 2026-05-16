package com.portal.job.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class JobSearchRequest {
    private String keyword;
    private String location;
    private Set<UUID> categories;
    private List<String> employmentTypes;
    private BigDecimal salaryMin;
    private BigDecimal salaryMax;
    private LocalDate datePostedAfter;
    private int page = 0;
    private int size = 10;

    // Getters and Setters
    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public Set<UUID> getCategories() {
        return categories;
    }

    public void setCategories(Set<UUID> categories) {
        this.categories = categories;
    }

    public List<String> getEmploymentTypes() {
        return employmentTypes;
    }

    public void setEmploymentTypes(List<String> employmentTypes) {
        this.employmentTypes = employmentTypes;
    }

    public BigDecimal getSalaryMin() {
        return salaryMin;
    }

    public void setSalaryMin(BigDecimal salaryMin) {
        this.salaryMin = salaryMin;
    }

    public BigDecimal getSalaryMax() {
        return salaryMax;
    }

    public void setSalaryMax(BigDecimal salaryMax) {
        this.salaryMax = salaryMax;
    }

    public LocalDate getDatePostedAfter() {
        return datePostedAfter;
    }

    public void setDatePostedAfter(LocalDate datePostedAfter) {
        this.datePostedAfter = datePostedAfter;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }
}
