#!/bin/bash
set -e

# Tạo 4 database riêng biệt cho mỗi service (Database per Service pattern)
for db in identity_service_db candidate_service_db employer_service_db job_service_db; do
  psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    SELECT 'CREATE DATABASE $db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = '$db')\gexec
EOSQL
done

# ===== IDENTITY SERVICE DB =====
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "identity_service_db" <<-EOSQL
  CREATE TABLE IF NOT EXISTS users (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      email VARCHAR(255) UNIQUE NOT NULL,
      password_hash VARCHAR(255) NOT NULL,
      role VARCHAR(50) NOT NULL,
      is_active BOOLEAN DEFAULT TRUE,
      created_at TIMESTAMP DEFAULT NOW(),
      updated_at TIMESTAMP DEFAULT NOW()
  );

  CREATE TABLE IF NOT EXISTS roles (
      id SERIAL PRIMARY KEY,
      name VARCHAR(100) UNIQUE NOT NULL
  );

  CREATE TABLE IF NOT EXISTS permissions (
      id SERIAL PRIMARY KEY,
      name VARCHAR(100) UNIQUE NOT NULL
  );

  CREATE TABLE IF NOT EXISTS user_roles (
      user_id UUID REFERENCES users(id) ON DELETE CASCADE,
      role_id INT REFERENCES roles(id) ON DELETE CASCADE,
      PRIMARY KEY (user_id, role_id)
  );

  CREATE TABLE IF NOT EXISTS role_permissions (
      role_id INT REFERENCES roles(id) ON DELETE CASCADE,
      permission_id INT REFERENCES permissions(id) ON DELETE CASCADE,
      PRIMARY KEY (role_id, permission_id)
  );

  CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
  CREATE INDEX IF NOT EXISTS idx_users_is_active ON users(is_active);

  -- Seed default roles
  INSERT INTO roles (name) VALUES ('ROLE_CANDIDATE'), ('ROLE_EMPLOYER'), ('ROLE_ADMIN')
  ON CONFLICT (name) DO NOTHING;
EOSQL

# ===== CANDIDATE SERVICE DB =====
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "candidate_service_db" <<-EOSQL
  CREATE TABLE IF NOT EXISTS candidates (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      user_id UUID NOT NULL,
      first_name VARCHAR(100),
      last_name VARCHAR(100),
      email VARCHAR(255),
      phone_number VARCHAR(20),
      address TEXT,
      headline VARCHAR(255),
      avatar_url VARCHAR(500)
  );

  CREATE TABLE IF NOT EXISTS resumes (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      candidate_id UUID REFERENCES candidates(id) ON DELETE CASCADE,
      title VARCHAR(255),
      resume_data TEXT,
      file_url VARCHAR(500),
      is_default BOOLEAN DEFAULT FALSE
  );

  CREATE TABLE IF NOT EXISTS experiences (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      resume_id UUID REFERENCES resumes(id) ON DELETE CASCADE,
      job_title VARCHAR(255),
      company_name VARCHAR(255),
      start_date DATE,
      end_date DATE,
      description TEXT
  );

  CREATE TABLE IF NOT EXISTS educations (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      resume_id UUID REFERENCES resumes(id) ON DELETE CASCADE,
      institution VARCHAR(255),
      degree VARCHAR(100),
      field_of_study VARCHAR(255),
      start_date DATE,
      end_date DATE
  );

  CREATE TABLE IF NOT EXISTS skills (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      candidate_id UUID REFERENCES candidates(id) ON DELETE CASCADE,
      skill_name VARCHAR(100)
  );

  CREATE TABLE IF NOT EXISTS applied_jobs (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      candidate_id UUID REFERENCES candidates(id) ON DELETE CASCADE,
      job_id UUID NOT NULL,
      application_status VARCHAR(50) DEFAULT 'PENDING',
      applied_at TIMESTAMP DEFAULT NOW()
  );

  CREATE INDEX IF NOT EXISTS idx_candidates_user_id ON candidates(user_id);
  CREATE INDEX IF NOT EXISTS idx_applied_jobs_candidate ON applied_jobs(candidate_id);
  CREATE INDEX IF NOT EXISTS idx_applied_jobs_job ON applied_jobs(job_id);
  CREATE INDEX IF NOT EXISTS idx_skills_candidate ON skills(candidate_id);
EOSQL

# ===== EMPLOYER SERVICE DB =====
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "employer_service_db" <<-EOSQL
  CREATE TABLE IF NOT EXISTS companies (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      company_name VARCHAR(255) NOT NULL,
      description TEXT,
      logo_url VARCHAR(500),
      website VARCHAR(500),
      address TEXT
  );

  CREATE TABLE IF NOT EXISTS employers (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      user_id UUID NOT NULL,
      company_id UUID REFERENCES companies(id),
      first_name VARCHAR(100),
      last_name VARCHAR(100)
  );

  CREATE INDEX IF NOT EXISTS idx_employers_user_id ON employers(user_id);
  CREATE INDEX IF NOT EXISTS idx_employers_company ON employers(company_id);
EOSQL

# ===== JOB SERVICE DB =====
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "job_service_db" <<-EOSQL
  CREATE TABLE IF NOT EXISTS categories (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      name VARCHAR(100) UNIQUE NOT NULL
  );

  CREATE TABLE IF NOT EXISTS jobs (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      company_id UUID NOT NULL,
      company_name VARCHAR(255),
      title VARCHAR(255) NOT NULL,
      description TEXT,
      salary_min DECIMAL(15,2),
      salary_max DECIMAL(15,2),
      location VARCHAR(255),
      status VARCHAR(50) DEFAULT 'ACTIVE',
      created_at TIMESTAMP DEFAULT NOW()
  );

  CREATE TABLE IF NOT EXISTS job_applications (
      id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
      job_id UUID REFERENCES jobs(id) ON DELETE CASCADE,
      candidate_id UUID NOT NULL,
      candidate_name VARCHAR(200),
      resume_id UUID,
      status VARCHAR(50) DEFAULT 'PENDING',
      applied_at TIMESTAMP DEFAULT NOW()
  );

  CREATE INDEX IF NOT EXISTS idx_jobs_company ON jobs(company_id);
  CREATE INDEX IF NOT EXISTS idx_jobs_status ON jobs(status);
  CREATE INDEX IF NOT EXISTS idx_job_apps_job ON job_applications(job_id);
  CREATE INDEX IF NOT EXISTS idx_job_apps_candidate ON job_applications(candidate_id);
EOSQL

echo "=== All 4 databases initialized successfully ==="