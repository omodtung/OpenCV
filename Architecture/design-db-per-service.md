# Database Schemas per Microservice

This document provides the detailed database schema for each microservice, designed based on our updated architecture. Each service owns its database to ensure loose coupling.

---

### 1. IdentityService (`identity_db` - PostgreSQL)
**Responsibility:** Manages user authentication, authorization, and core user accounts.

**`users` table**
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Primary Key | Unique identifier for the user. This ID is referenced by other services (e.g., Candidate, Employer). |
| `email` | `VARCHAR(255)` | Unique, Not Null | User's email address, used for login and unique identification. |
| `password_hash` | `VARCHAR(255)` | Not Null | Hashed password for secure authentication. |

| `is_enabled` | `BOOLEAN` | Not Null, Default TRUE | Account status (e.g., enabled, disabled). |
| `last_login_at` | `TIMESTAMP` | | Timestamp of the last successful login. |
| `created_at` | `TIMESTAMP` | Not Null, Default NOW | Timestamp of account creation. |
| `updated_at` | `TIMESTAMP` | Not Null, Default NOW | Timestamp of last profile update or password change. |

**`roles` table**
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `SERIAL` | Primary Key | Unique identifier for the role. |
| `name` | `VARCHAR(50)` | Unique, Not Null | Name of the role (e.g., 'ROLE_CANDIDATE', 'ROLE_EMPLOYER_ADMIN'). |

**`permissions` table**
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `SERIAL` | Primary Key | Unique identifier for the permission. |
| `name` | `VARCHAR(100)` | Unique, Not Null | Name of the permission (e.g., 'job:create', 'profile:read'). |

**`user_roles` table**
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `user_id` | `UUID` | Primary Key, FK to users | Links to the user. |
| `role_id` | `INTEGER` | Primary Key, FK to roles | Links to the role. |

**`role_permissions` table**
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `role_id` | `INTEGER` | Primary Key, FK to roles | Links to the role. |
| `permission_id` | `INTEGER` | Primary Key, FK to permissions | Links to the permission. |

**`audit_logs` table**
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Primary Key | Unique identifier for the log entry. |
| `user_id` | `UUID` | Not Null, FK to users | The user who performed the action. |
| `action` | `VARCHAR(100)` | Not Null | Description of the action performed (e.g., 'LOGIN_SUCCESS', 'PASSWORD_RESET'). |
| `timestamp` | `TIMESTAMP` | Not Null, Default NOW | When the action occurred. |
| `ip_address` | `VARCHAR(45)` | | IP address from which the action was initiated. |

---

### 2. JobService (`job_db` - PostgreSQL)
**Responsibility:** Manages job postings, categories, and required skills.

**`jobs` table**
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Primary Key | Unique identifier for the job. |
| `employer_id` | `UUID` | Not Null | Foreign reference to an employer in `EmployerService`. (Not a direct foreign key) |
| `title` | `VARCHAR(255)` | Not Null | Job title. |
| `description` | `TEXT` | Not Null | Detailed job description. |
| `location` | `VARCHAR(255)` | | Job location (e.g., city, state, remote). |
| `employment_type` | `VARCHAR(50)` | | e.g., 'Full-time', 'Part-time', 'Contract', 'Internship'. |
| `salary_min` | `DECIMAL(10, 2)` | | Minimum salary offered. |
| `salary_max` | `DECIMAL(10, 2)` | | Maximum salary offered. |
| `currency` | `VARCHAR(3)` | | e.g., 'USD', 'EUR'. |
| `experience_level` | `VARCHAR(50)` | | e.g., 'Entry-level', 'Mid-level', 'Senior', 'Director'. |
| `application_deadline` | `DATE` | | Deadline for applications. |
| `external_application_url` | `VARCHAR(500)` | | URL if applications are handled externally. |
| `status` | `VARCHAR(50)` | Not Null, Default 'ACTIVE' | e.g., 'ACTIVE', 'INACTIVE', 'CLOSED', 'EXPIRED'. |
| `posted_at` | `TIMESTAMP` | Not Null, Default NOW | Timestamp of job posting. |
| `updated_at` | `TIMESTAMP` | Not Null, Default NOW | Timestamp of last job update. |
| `views_count` | `INT` | Default 0 | Number of times the job has been viewed. |

**`job_applicants` table**
Tracks candidates who have applied for a specific job. This table is owned by the Job Service to manage applications related to its jobs. It's distinct from the `applied_jobs` table in the Candidate Service.

    | Column Name        | Data Type   | Constraints                      | Description                                                  |
    | :----------------- | :---------- | :------------------------------- | :----------------------------------------------------------- |
    | id                 | `UUID`        | Primary Key                      |                                                              |
    | job_id             | `UUID`        | Not Null, FK to jobs             | The job the candidate applied for                            |
    | candidate_id       | `UUID`        | Not Null                         | Reference to a candidate in the Candidate Service. Not a foreign key. |
    | application_status | `VARCHAR(50)` | Not Null, Default 'APPLIED'      | e.g., 'APPLIED', 'REVIEWED', 'REJECTED', 'INTERVIEWED'       |
    | applied_at         | `TIMESTAMP`   | Not Null, Default NOW            |                                                              |
    | reviewed_at        | `TIMESTAMP`   |                                  | Timestamp when the application was reviewed                  |

**`job_categories` table**
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Primary Key | Unique identifier for the category. |
| `name` | `VARCHAR(100)` | Unique, Not Null | Name of the job category (e.g., 'Software Development', 'Marketing'). |
| `description` | `TEXT` | | Description of the category. |

**`job_job_categories` table (Junction table for many-to-many relationship)**
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `job_id` | `UUID` | Primary Key, FK to jobs | Links to the job posting. |
| `category_id` | `UUID` | Primary Key, FK to job_categories | Links to the job category. |

**`job_skills_required` table**
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Primary Key | Unique identifier for the skill requirement. |
| `job_id` | `UUID` | Not Null, FK to jobs | Links to the job posting. |
| `skill_name` | `VARCHAR(100)` | Not Null | Name of the required skill (e.g., 'Java', 'SQL', 'Cloud Computing'). |
| `min_proficiency` | `VARCHAR(50)` | | Minimum proficiency level required (e.g., 'BASIC', 'INTERMEDIATE'). |
| `is_mandatory` | `BOOLEAN` | Not Null, Default TRUE | Whether the skill is mandatory. |

---

### 3. ApplicationService (`application_db` - PostgreSQL)
**Responsibility:** Manages job applications.

**`applications` table**
| Column | Type | Constraints | Description |
| :--- | :--- | :--- | :--- |
| `id` | `UUID` | Primary Key | Unique identifier for the application. |
| `job_id` | `UUID` | Not Null | Foreign reference to a job in `JobService`. |
| `candidate_id` | `UUID` | Not Null | Foreign reference to a user in `IdentityService`. |
| `resume_id` | `UUID` | Not Null | Foreign reference to a resume in `ResumeService`. |
| `status` | `VARCHAR(50)` | Not Null | e.g., 'APPLIED', 'REVIEWED', 'REJECTED'. |
| `applied_at` | `TIMESTAMP` | Not Null | Timestamp of when the application was submitted. |

---

### 4. ResumeService (`resume_db` - MongoDB)
**Responsibility:** Manages candidate resumes. A document-based approach is used for flexibility.

**`resumes` collection**
```json
{
  "_id": "ObjectId('...')",
  "candidate_id": "UUID('...')", // Foreign reference to a user in IdentityService
  "file_url": "https://storage.service/path/to/resume.pdf",
  "file_hash": "sha256_hash_of_the_file",
  "parsed_content": {
    "name": "John Doe",
    "email": "john.doe@example.com",
    "phone": "123-456-7890",
    "skills": ["Java", "Spring Boot", "Docker"],
    "experience": [
      {
        "title": "Software Engineer",
        "company": "Tech Corp",
        "duration": "2020-Present"
      }
    ],
    "education": [
      {
        "degree": "B.S. in Computer Science",
        "university": "State University"
      }
    ]
  },
  "created_at": "ISODate('...')",
  "updated_at": "ISODate('...')",
  "educations": [
    {
      "id": "UUID",
      "resume_id": "UUID",
      "institution": "VARCHAR(255)",
      "degree": "VARCHAR(255)",
      "field_of_study": "VARCHAR(255)",
      "start_date": "DATE",
      "end_date": "DATE"
    }
  ],
  "experiences": [
    {
      "id": "UUID",
      "resume_id": "UUID",
      "title": "VARCHAR(255)",
      "company": "VARCHAR(255)",
      "location": "VARCHAR(255)",
      "start_date": "DATE",
      "end_date": "DATE",
      "description": "TEXT"
    }
  ],
  "skills": [
    {
      "id": "UUID",
      "resume_id": "UUID",
      "skill_name": "VARCHAR(100)",
      "proficiency": "VARCHAR(50)"
    }
  ]
}
```
---

### 5. CandidateService (`candidate_db` - PostgreSQL)
**Responsibility:** Manages detailed candidate profiles and their associated data.

1.  `candidates`
    Stores the main profile information for each candidate.

    | Column Name  | Data Type    | Constraints           | Description                             |
        | :----------- | :----------- | :-------------------- | :-------------------------------------- |
    | id           | `UUID`         | Primary Key           | Unique identifier for the candidate     |
    | user_id      | `UUID`         | Not Null, Unique      | Links to the Identity Service's user    |
    | first_name   | `VARCHAR(100)` | Not Null              | Candidate's first name                  |
    | last_name    | `VARCHAR(100)` | Not Null              | Candidate's last name                   |
    | email        | `VARCHAR(255)` | Not Null, Unique      | Candidate's primary email               |
    | phone_number | `VARCHAR(50)`  |                       | Candidate's phone number                |
    | headline     | `VARCHAR(255)` |                       | A short professional headline           |
    | bio          | `TEXT`         |                       | A brief biography or summary            |
    | photo_url    | `VARCHAR(255)` |                       | URL to the candidate's profile photo    |
    | resume_url   | `VARCHAR(255)` |                       | URL to the candidate's full resume file |
    | linkedin_url | `VARCHAR(255)` |                       | LinkedIn profile URL                    |
    | github_url   | `VARCHAR(255)` |                       | GitHub profile URL                      |
    | portfolio_url| `VARCHAR(255)` |                       | Online portfolio URL                    |
    | experience_years | `INT`          |                       | Total years of experience               |
    | current_salary | `DECIMAL(10, 2)` |                       | Current or desired salary               |
    | desired_job_title | `VARCHAR(255)` |                       |                                         |
    | location     | `VARCHAR(255)` |                       | Candidate's preferred location          |
    | created_at   | `TIMESTAMP`    | Not Null, Default NOW |                                         |
    | updated_at   | `TIMESTAMP`    | Default NOW           |                                         |

2.  `skills`
    Lists skills possessed by a candidate.

    | Column Name | Data Type    | Constraints             | Description                                     |
        | :---------- | :----------- | :---------------------- | :---------------------------------------------- |
    | id          | `UUID`         | Primary Key             |                                                 |
    | candidate_id| `UUID`         | Not Null, FK to candidates | Links to the candidate this skill belongs to    |
    | skill_name  | `VARCHAR(100)` | Not Null                | e.g., "Java", "Python", "React"                 |
    | proficiency | `VARCHAR(50)`  |                         | e.g., 'BEGINNER', 'INTERMEDIATE', 'EXPERT'      |

3.  `experiences`
    Details of a candidate's work experience.

    | Column Name | Data Type    | Constraints             | Description                                   |
        | :---------- | :----------- | :---------------------- | :-------------------------------------------- |
    | id          | `UUID`         | Primary Key             |                                               |
    | candidate_id| `UUID`         | Not Null, FK to candidates | Links to the candidate this experience belongs to |
    | title       | `VARCHAR(255)` | Not Null                | Job title                                     |
    | company     | `VARCHAR(255)` | Not Null                | Company name                                  |
    | location    | `VARCHAR(255)` |                         | Job location                                  |
    | start_date  | `DATE`         | Not Null                |                                               |
    | end_date    | `DATE`         |                         | Null if current job                           |
    | description | `TEXT`         |                         | Job responsibilities and achievements         |

4.  `education`
    Details of a candidate's educational background.

    | Column Name    | Data Type    | Constraints             | Description                                   |
        | :------------- | :----------- | :---------------------- | :-------------------------------------------- |
    | id             | `UUID`         | Primary Key             |                                               |
    | candidate_id   | `UUID`         | Not Null, FK to candidates | Links to the candidate this education belongs to |
    | institution    | `VARCHAR(255)` | Not Null                | Name of the school/university                 |
    | degree         | `VARCHAR(255)` | Not Null                | e.g., "Bachelor of Science"                   |
    | field_of_study | `VARCHAR(255)` |                         | e.g., "Computer Science"                      |
    | start_date     | `DATE`         |                         |                                               |
    | end_date       | `DATE`         |                         |                                               |

5.  `applied_jobs`
    Tracks which jobs a candidate has applied for. This is a crucial table for understanding service boundaries.

    | Column Name        | Data Type   | Constraints                | Description                                               |
        | :----------------- | :---------- | :------------------------- | :-------------------------------------------------------- |
    | id                 | `UUID`        | Primary Key                |                                                           |
    | candidate_id       | `UUID`        | Not Null, FK to candidates | The candidate who applied                                 |
    | job_id             | `UUID`        | Not Null                   | Reference to a job in the Job Service. Not a foreign key. |
    | application_status | `VARCHAR(50)` | Not Null                   | e.g., 'APPLIED', 'REVIEWED', 'REJECTED'                   |
    | applied_at         | `TIMESTAMP`   | Not Null                   |                                                           |

---

### 6. EmployerService (`employer_db` - PostgreSQL)
**Responsibility:** Manages detailed employer profiles, company information, and job postings.

1.  `employers`
    Stores the main profile information for each employer.

    | Column Name      | Data Type    | Constraints           | Description                                       |
        | :--------------- | :----------- | :-------------------- | :------------------------------------------------ |
    | id               | `UUID`         | Primary Key           | Unique identifier for the employer                |
    | user_id          | `UUID`         | Not Null, Unique      | Links to the Identity Service's user              |
    | company_name     | `VARCHAR(255)` | Not Null              | Name of the company                               |
    | industry         | `VARCHAR(255)` |                       | Industry the company belongs to                   |
    | description      | `TEXT`         |                       | Company description                               |
    | website          | `VARCHAR(255)` |                       | Company website URL                               |
    | logo_url         | `VARCHAR(255)` |                       | URL to the company logo                           |
    | headquarters     | `VARCHAR(255)` |                       | Company headquarters location                     |
    | founded_year     | `INT`          |                       | Year the company was founded                      |
    | company_size     | `VARCHAR(50)`  |                       | e.g., "1-10", "11-50", "51-200", "201-500", "500+" |
    | contact_email    | `VARCHAR(255)` |                       | Primary contact email for the company             |
    | phone_number     | `VARCHAR(50)`  |                       | Company phone number                              |
    | created_at       | `TIMESTAMP`    | Not Null, Default NOW |                                                   |
    | updated_at       | `TIMESTAMP`    | Default NOW           |                                                   |



4.  `employer_locations`
    Stores multiple office locations for a company.

    | Column Name | Data Type    | Constraints               | Description                             |
        | :---------- | :----------- | :------------------------ | :-------------------------------------- |
    | id          | `UUID`         | Primary Key               |                                         |
    | employer_id | `UUID`         | Not Null, FK to employers | Links to the employer                   |
    | address_line1 | `VARCHAR(255)` | Not Null                  |                                         |
    | address_line2 | `VARCHAR(255)` |                           |                                         |
    | city        | `VARCHAR(100)` | Not Null                  |                                         |
    | state       | `VARCHAR(100)` |                           |                                         |
    | postal_code | `VARCHAR(20)`  |                           |                                         |
    | country     | `VARCHAR(100)` | Not Null                  |                                         |
    | is_headquarters | `BOOLEAN`      | Default FALSE             | Indicates if this is the main office    |

5.  `employer_benefits`
    Stores benefits offered by the employer for their jobs.

    | Column Name | Data Type    | Constraints               | Description                         |
        | :---------- | :----------- | :------------------------ | :---------------------------------- |
    | id          | `UUID`         | Primary Key               |                                     |
    | employer_id | `UUID`         | Not Null, FK to employers | Links to the employer               |
    | benefit_name | `VARCHAR(255)` | Not Null                  | e.g., "Health Insurance", "401K"    |
    | description | `TEXT`         |                           | Details about the benefit           |
