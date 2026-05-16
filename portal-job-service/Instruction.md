Tôi hiểu rằng bạn muốn hướng dẫn chi tiết về các bước tiếp theo để triển khai chức năng Tìm kiếm & Lọc Job nâng cao. Dưới đây là kế hoạch từng bước cho giai đoạn "Quick
Win" sử dụng Job Service và PostgreSQL:

Giai đoạn 1: "Quick Win" với Job Service (PostgreSQL)

Bước 1: Tạo DTO JobSearchRequest

File: portal-job-service/job-rest/src/main/java/com/portal/job/rest/dto/JobSearchRequest.java
Mục đích: Định nghĩa cấu trúc dữ liệu cho các tiêu chí tìm kiếm và lọc từ frontend.

Nội dung file JobSearchRequest.java:

    1 package com.portal.job.rest.dto;
    2
    3 import java.math.BigDecimal;
    4 import java.time.LocalDate; // Thêm import cho LocalDate
    5 import java.util.List;
    6 import java.util.Set;
    7
    8 public class JobSearchRequest {
    9     private String keyword;
10     private String location;
11     private Set<UUID> categories; // Thay đổi thành UUID cho category IDs
12     private List<String> employmentTypes; // Đổi tên từ jobTypes cho phù hợp với entity Job
13     private BigDecimal salaryMin;
14     private BigDecimal salaryMax;
15     private LocalDate datePostedAfter; // Thêm trường lọc theo ngày đăng sau
16     private int page = 0;
17     private int size = 10;
18
19     // Getters and Setters
20     public String getKeyword() {
21         return keyword;
22     }
23
24     public void setKeyword(String keyword) {
25         this.keyword = keyword;
26     }
27
28     public String getLocation() {
29         return location;
30     }
31
32     public void setLocation(String location) {
33         this.location = location;
34     }
35
36     public Set<UUID> getCategories() {
37         return categories;
38     }
39
40     public void setCategories(Set<UUID> categories) {
41         this.categories = categories;
42     }
43
44     public List<String> getEmploymentTypes() {
45         return employmentTypes;
46     }
47
48     public void setEmploymentTypes(List<String> employmentTypes) {
49         this.employmentTypes = employmentTypes;
50     }
51
52     public BigDecimal getSalaryMin() {
53         return salaryMin;
54     }
55
56     public void setSalaryMin(BigDecimal salaryMin) {
57         this.salaryMin = salaryMin;
58     }
59
60     public BigDecimal getSalaryMax() {
61         return salaryMax;
62     }
63
64     public void setSalaryMax(BigDecimal salaryMax) {
65         this.salaryMax = salaryMax;
66     }
67
68     public LocalDate getDatePostedAfter() {
69         return datePostedAfter;
70     }
71
72     public void setDatePostedAfter(LocalDate datePostedAfter) {
73         this.datePostedAfter = datePostedAfter;
74     }
75
76     public int getPage() {
77         return page;
78     }
79
80     public void setPage(int page) {
81         this.page = page;
82     }
83
84     public int getSize() {
85         return size;
86     }
87
88     public void setSize(int size) {
89         this.size = size;
90     }
91 }

Bước 2: Mở rộng JobRepository với JpaSpecificationExecutor

File: portal-job-service/job-domain/src/main/java/com/portal/job/domain/JobRepository.java
Mục đích: Cho phép JobRepository sử dụng Spring Data JPA Specification để xây dựng các truy vấn động.

    1 package com.portal.job.domain;
    2
    3 import java.util.List;
    4 import java.util.UUID;
    5 import org.springframework.data.jpa.repository.JpaRepository;
    6 import org.springframework.data.jpa.repository.JpaSpecificationExecutor; // Thêm import này
    7 import org.springframework.data.jpa.repository.Modifying;
    8 import org.springframework.data.jpa.repository.Query;
    9 import org.springframework.data.repository.query.Param;
10
11 // Kế thừa JpaSpecificationExecutor<Job>
12 public interface JobRepository extends JpaRepository<Job, UUID>, JpaSpecificationExecutor<Job> {
13   List<Job> findByEmployerId(UUID employerId);
14
15   List<Job> findByStatus(String status);
16
17   @Modifying
18   @Query(
19     "UPDATE Job j SET j.companyName = :newName WHERE j.employerId = :companyId"
20   )
21   int updateCompanyName(
22     @Param("companyId") UUID companyId,
23     @Param("newName") String newName
24   );
25 }

Bước 3: Tạo JobSpecification class

File: portal-job-service/job-app/src/main/java/com/portal/job/app/service/JobSpecification.java
Mục đích: Xây dựng các điều kiện Predicate động dựa trên JobSearchRequest.

Nội dung file JobSpecification.java:

    1 package com.portal.job.app.service;
    2
    3 import com.portal.job.domain.Job;
    4 import com.portal.job.domain.Category;
    5 import com.portal.job.rest.dto.JobSearchRequest;
    6 import jakarta.persistence.criteria.Join;
    7 import jakarta.persistence.criteria.Predicate;
    8 import org.springframework.data.jpa.domain.Specification;
    9
10 import java.util.ArrayList;
11 import java.util.List;
12 import java.util.Objects;
13
14 public class JobSpecification {
15
16     public static Specification<Job> searchJobs(JobSearchRequest request) {
17         return (root, query, criteriaBuilder) -> {
18             List<Predicate> predicates = new ArrayList<>();
19
20             if (request.getKeyword() != null && !request.getKeyword().isEmpty()) {
21                 String likePattern = "%" + request.getKeyword().toLowerCase() + "%";
22                 Predicate titlePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), likePattern);
23                 Predicate descriptionPredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), likePattern);
24                 Predicate companyNamePredicate = criteriaBuilder.like(criteriaBuilder.lower(root.get("companyName")), likePattern);
25                 predicates.add(criteriaBuilder.or(titlePredicate, descriptionPredicate, companyNamePredicate));
26             }
27
28             if (request.getLocation() != null && !request.getLocation().isEmpty()) {
29                 predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("location")), "%" + request.getLocation().toLowerCase() + "%"));
30             }
31
32             if (request.getCategories() != null && !request.getCategories().isEmpty()) {
33                 Join<Job, Category> jobCategoryJoin = root.join("categories");
34                 predicates.add(jobCategoryJoin.get("id").in(request.getCategories()));
35             }
36
37             if (request.getEmploymentTypes() != null && !request.getEmploymentTypes().isEmpty()) {
38                 predicates.add(root.get("employmentType").in(request.getEmploymentTypes()));
39             }
40
41             if (request.getSalaryMin() != null) {
42                 predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("salaryMax"), request.getSalaryMin()));
43             }
44
45             if (request.getSalaryMax() != null) {
46                 predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("salaryMin"), request.getSalaryMax()));
47             }
48
49             if (request.getDatePostedAfter() != null) {
50                 predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("postedAt"), request.getDatePostedAfter().atStartOfDay()));
51             }
52
53             return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
54         };
55     }
56 }

Bước 4: Tạo CategoryRepository

File: portal-job-service/job-domain/src/main/java/com/portal/job/domain/CategoryRepository.java
Mục đích: Để truy xuất danh sách các Category.

Nội dung file CategoryRepository.java:

1 package com.portal.job.domain;
2
3 import java.util.UUID;
4 import org.springframework.data.jpa.repository.JpaRepository;
5
6 public interface CategoryRepository extends JpaRepository<Category, UUID> {
7     // Có thể thêm các phương thức tùy chỉnh nếu cần
8 }

Bước 5: Tạo JobService

File: portal-job-service/job-app/src/main/java/com/portal/job/app/service/JobService.java
Mục đích: Đóng gói logic nghiệp vụ tìm kiếm và lọc job, cũng như lấy danh sách category.

Nội dung file JobService.java:

    1 package com.portal.job.app.service;
    2
    3 import com.portal.job.domain.Category;
    4 import com.portal.job.domain.CategoryRepository;
    5 import com.portal.job.domain.Job;
    6 import com.portal.job.domain.JobRepository;
    7 import com.portal.job.rest.dto.JobSearchRequest;
    8 import org.springframework.beans.factory.annotation.Autowired;
    9 import org.springframework.data.domain.Page;
10 import org.springframework.data.domain.PageRequest;
11 import org.springframework.data.domain.Pageable;
12 import org.springframework.data.jpa.domain.Specification;
13 import org.springframework.stereotype.Service;
14
15 import java.util.List;
16
17 @Service
18 public class JobService {
19
20     @Autowired
21     private JobRepository jobRepository;
22
23     @Autowired
24     private CategoryRepository categoryRepository;
25
26     public Page<Job> searchJobs(JobSearchRequest request) {
27         Specification<Job> spec = JobSpecification.searchJobs(request);
28         Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
29         return jobRepository.findAll(spec, pageable);
30     }
31
32     public List<Category> getAllCategories() {
33         return categoryRepository.findAll();
34     }
35 }

Bước 6: Cập nhật JobController

File: portal-job-service/job-rest/src/main/java/com/portal/job/rest/JobController.java
Mục đích: Thêm các endpoint API mới để lấy danh mục và tìm kiếm job.

Nội dung file JobController.java:

    1 package com.portal.job.rest;
    2
    3 import java.util.List;
    4 import java.util.UUID;
    5
    6 import com.portal.job.app.service.JobService; // Thêm import này
    7 import com.portal.job.domain.Category; // Thêm import này
    8 import com.portal.job.rest.dto.JobSearchRequest; // Thêm import này
    9 import org.springframework.beans.factory.annotation.Autowired;
10 import org.springframework.data.domain.Page; // Thêm import này
11 import org.springframework.http.ResponseEntity;
12 import org.springframework.web.bind.annotation.GetMapping;
13 import org.springframework.web.bind.annotation.PathVariable;
14 import org.springframework.web.bind.annotation.PostMapping;
15 import org.springframework.web.bind.annotation.RequestBody;
16 import org.springframework.web.bind.annotation.RequestMapping;
17 import org.springframework.web.bind.annotation.RestController;
18
19 import com.portal.job.domain.Job;
20 import com.portal.job.domain.JobRepository;
21
22 @RestController
23 @RequestMapping("/api/v1/jobs") // Cập nhật RequestMapping để có /api/v1
24 public class JobController {
25
26     @Autowired
27     private JobRepository jobRepository;
28
29     @Autowired
30     private JobService jobService; // Thêm JobService
31
32     @GetMapping
33     public List<Job> getAllJobs() {
34         return jobRepository.findAll();
35     }
36
37     @PostMapping
38     public Job createJob(@RequestBody Job job) {
39         return jobRepository.save(job);
40     }
41
42     @GetMapping("/{id}")
43     public ResponseEntity<Job> getJob(@PathVariable UUID id) {
44         return jobRepository.findById(id)
45                 .map(ResponseEntity::ok)
46                 .orElse(ResponseEntity.notFound().build());
47     }
48
49     @GetMapping("/categories") // Endpoint mới cho danh mục
50     public List<Category> getAllCategories() {
51         return jobService.getAllCategories();
52     }
53
54     @PostMapping("/search") // Endpoint mới cho tìm kiếm nâng cao
55     public Page<Job> searchJobs(@RequestBody JobSearchRequest request) {
56         return jobService.searchJobs(request);
57     }
58 }

Hãy làm theo các bước này. Nếu bạn muốn tôi tự động tạo các file hoặc chỉnh sửa, hãy cho tôi biết.



─────────────────────────────────────────────────────────────────────