package com.portal.job.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "jobs")
public class Job {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "employer_id", nullable = false)
  private UUID employerId;

  @Column(name = "company_name")
  private String companyName;

  @Column(nullable = false)
  private String title;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String description;

  private String location;

  @Column(name = "employment_type")
  private String employmentType;

  @Column(name = "salary_min")
  private BigDecimal salaryMin;

  @Column(name = "salary_max")
  private BigDecimal salaryMax;

  private String currency;

  @Column(name = "experience_level")
  private String experienceLevel;

  @Column(name = "application_deadline")
  private java.time.LocalDate applicationDeadline;

  @Column(name = "external_application_url", length = 500)
  private String externalApplicationUrl;

  @Column(nullable = false)
  private String status = "ACTIVE";

  @Column(name = "posted_at", nullable = false)
  private LocalDateTime postedAt = LocalDateTime.now();

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt = LocalDateTime.now();

  @Column(name = "views_count")
  private Integer viewsCount = 0;

  @ManyToMany
  @JoinTable(
    name = "job_job_categories",
    joinColumns = @JoinColumn(name = "job_id"),
    inverseJoinColumns = @JoinColumn(name = "category_id")
  )
  private Set<Category> categories = new HashSet<>();

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getEmployerId() {
    return employerId;
  }

  public void setEmployerId(UUID employerId) {
    this.employerId = employerId;
  }

  public String getCompanyName() {
    return companyName;
  }

  public void setCompanyName(String companyName) {
    this.companyName = companyName;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getLocation() {
    return location;
  }

  public void setLocation(String location) {
    this.location = location;
  }

  public String getEmploymentType() {
    return employmentType;
  }

  public void setEmploymentType(String employmentType) {
    this.employmentType = employmentType;
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

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public String getExperienceLevel() {
    return experienceLevel;
  }

  public void setExperienceLevel(String experienceLevel) {
    this.experienceLevel = experienceLevel;
  }

  public java.time.LocalDate getApplicationDeadline() {
    return applicationDeadline;
  }

  public void setApplicationDeadline(java.time.LocalDate applicationDeadline) {
    this.applicationDeadline = applicationDeadline;
  }

  public String getExternalApplicationUrl() {
    return externalApplicationUrl;
  }

  public void setExternalApplicationUrl(String externalApplicationUrl) {
    this.externalApplicationUrl = externalApplicationUrl;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public LocalDateTime getPostedAt() {
    return postedAt;
  }

  public void setPostedAt(LocalDateTime postedAt) {
    this.postedAt = postedAt;
  }

  public LocalDateTime getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(LocalDateTime updatedAt) {
    this.updatedAt = updatedAt;
  }

  public Integer getViewsCount() {
    return viewsCount;
  }

  public void setViewsCount(Integer viewsCount) {
    this.viewsCount = viewsCount;
  }

  public Set<Category> getCategories() {
    return categories;
  }

  public void setCategories(Set<Category> categories) {
    this.categories = categories;
  }
}
