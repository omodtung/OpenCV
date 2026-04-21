# OpenCV
Portal - Job Recruitment Microservices Platform (Complete Blueprint)

Tài liệu này chứa TOÀN BỘ source code, cấu hình, Docker, K8s của dự án. Một AI model đọc file này có thể tái tạo lại dự án 100% giống bản gốc.

Tổng quan

Kiến trúc: Microservices (4 services), Database per Service, Event-driven messaging

Tech stack: Java 17, Spring Boot 3.2.5, Spring Data JPA, Spring Security, PostgreSQL 16, ActiveMQ (JMS) / AWS SQS

Messaging: Profile-based switching (jms cho local, sqs cho AWS production)

Auth: JWT (jjwt 0.12.6), BCrypt password encoding

Build: Maven multi-module, Docker multi-stage build

Cấu trúc thư mục

Portal/
├── docker-compose.yml
├── docker/init-databases.sh
├── k8s/
│   ├── deploy.sh
│   ├── ingress.yaml
│   ├── base/
│   │   ├── namespace.yaml
│   │   ├── postgres.yaml
│   │   └── activemq.yaml
│   └── services/
│       ├── identity-service.yaml
│       ├── candidate-service.yaml
│       ├── employer-service.yaml
│       └── job-service.yaml
├── portal-identity-service/     (port 8080, context-path: /identity)
│   ├── Dockerfile
│   ├── pom.xml
│   ├── identity-domain/
│   ├── identity-app/
│   ├── identity-rest/
│   ├── identity-jms/
│   ├── identity-sqs/
│   └── identity-start/
├── portal-candidate-service/    (port 8081, context-path: /candidate)
│   ├── Dockerfile
│   ├── pom.xml
│   ├── candidate-domain/
│   ├── candidate-app/
│   ├── candidate-rest/
│   ├── candidate-jms/
│   ├── candidate-sqs/
│   └── candidate-start/
├── portal-employer-service/     (port 8082, context-path: /employer)
│   ├── Dockerfile
│   ├── pom.xml
│   ├── employer-domain/
│   ├── employer-app/
│   ├── employer-rest/
│   ├── employer-jms/
│   ├── employer-sqs/
│   └── employer-start/
└── portal-job-service/          (port 8083, context-path: /jobservice)
    ├── Dockerfile
    ├── pom.xml
    ├── job-domain/
    ├── job-app/
    ├── job-rest/
    ├── job-jms/
    ├── job-sqs/
    └── job-start/


Messaging Flow

Identity ──UserRegistered──► Candidate (tạo candidate profile nếu role=ROLE_CANDIDATE)
Identity ──UserRegistered──► Employer  (tạo employer profile nếu role=ROLE_EMPLOYER)
Candidate ──JobApplied─────► Job       (tạo job_application record)
Employer ──CompanyUpdated──► Job       (sync company_name trong bảng jobs)
Job ──ApplicationStatus───► Candidate  (cập nhật applied_jobs.application_status)


PHẦN 1: DOCKER & INFRASTRUCTURE

docker-compose.yml

version: "3.9"

services:

  # ===== PostgreSQL - 4 databases (Database per Service) =====
  postgres:
    image: postgres:16-alpine
    container_name: portal-postgres
    environment:
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
      POSTGRES_HOST_AUTH_METHOD: md5
      POSTGRES_INITDB_ARGS: "--auth-host=md5"
    ports:
      - "5432:5432"
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./docker/init-databases.sh:/docker-entrypoint-initdb.d/init-databases.sh
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 5s
      retries: 5

  # ===== ActiveMQ - Message Broker (JMS profile, local dev) =====
  activemq:
    image: apache/activemq-classic:5.18.4
    container_name: portal-activemq
    ports:
      - "61616:61616"   # JMS
      - "8161:8161"     # Web console (admin/admin)
    environment:
      ACTIVEMQ_USERNAME: admin
      ACTIVEMQ_PASSWORD: admin
    healthcheck:
      test: ["CMD-SHELL", "curl -s http://localhost:8161/ || exit 1"]
      interval: 10s
      timeout: 5s
      retries: 5

  # ===== Identity Service (port 8080) =====
  identity-service:
    build: ./portal-identity-service
    container_name: portal-identity
    depends_on:
      postgres:
        condition: service_healthy
      activemq:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: jms
      DATABASE_URL: jdbc:postgresql://postgres:5432/identity_service_db
      DATABASE_USERNAME: postgres
      DATABASE_PASSWORD: postgres
    ports:
      - "8080:8080"

  # ===== Candidate Service (port 8081) =====
  candidate-service:
    build: ./portal-candidate-service
    container_name: portal-candidate
    depends_on:
      postgres:
        condition: service_healthy
      activemq:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: jms
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/candidate_service_db
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: postgres
      SERVER_PORT: 8081
      SPRING_ACTIVEMQ_BROKER_URL: tcp://activemq:61616
    ports:
      - "8081:8081"

  # ===== Employer Service (port 8082) =====
  employer-service:
    build: ./portal-employer-service
    container_name: portal-employer
    depends_on:
      postgres:
        condition: service_healthy
      activemq:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: jms
      DB_HOST: postgres
      DB_NAME: employer_service_db
      DB_USER: postgres
      DB_PASSWORD: postgres
      SERVER_PORT: 8082
      SPRING_ACTIVEMQ_BROKER_URL: tcp://activemq:61616
    ports:
      - "8082:8082"

  # ===== Job Service (port 8083) =====
  job-service:
    build: ./portal-job-service
    container_name: portal-job
    depends_on:
      postgres:
        condition: service_healthy
      activemq:
        condition: service_healthy
    environment:
      SPRING_PROFILES_ACTIVE: jms
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/job_service_db
      DB_USER: postgres
      DB_PASSWORD: postgres
      SERVER_PORT: 8083
      SPRING_ACTIVEMQ_BROKER_URL: tcp://activemq:61616
    ports:
      - "8083:8083"

volumes:
  postgres_data:


docker/init-databases.sh

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


PHẦN 2: KUBERNETES

k8s/base/namespace.yaml

apiVersion: v1
kind: Namespace
metadata:
  name: portal


k8s/base/postgres.yaml

apiVersion: v1
kind: Secret
metadata:
  name: postgres-secret
  namespace: portal
type: Opaque
stringData:
  POSTGRES_USER: postgres
  POSTGRES_PASSWORD: postgres
---
apiVersion: v1
kind: Secret
metadata:
  name: jwt-secret
  namespace: portal
type: Opaque
stringData:
  JWT_SECRET: my-super-secret-key-that-is-at-least-256-bits-long!!
---
apiVersion: v1
kind: ConfigMap
metadata:
  name: postgres-init
  namespace: portal
data:
  init-databases.sh: |
    #!/bin/bash
    set -e
    for db in identity_service_db candidate_service_db employer_service_db job_service_db; do
      psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
        CREATE DATABASE $db;
    EOSQL
    done
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
  namespace: portal
spec:
  serviceName: postgres
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
        - name: postgres
          image: postgres:16-alpine
          ports:
            - containerPort: 5432
          envFrom:
            - secretRef:
                name: postgres-secret
          volumeMounts:
            - name: postgres-data
              mountPath: /var/lib/postgresql/data
            - name: init-script
              mountPath: /docker-entrypoint-initdb.d
          readinessProbe:
            exec:
              command: ["pg_isready", "-U", "postgres"]
            initialDelaySeconds: 5
            periodSeconds: 5
      volumes:
        - name: init-script
          configMap:
            name: postgres-init
  volumeClaimTemplates:
    - metadata:
        name: postgres-data
      spec:
        accessModes: ["ReadWriteOnce"]
        resources:
          requests:
            storage: 5Gi
---
apiVersion: v1
kind: Service
metadata:
  name: postgres
  namespace: portal
spec:
  selector:
    app: postgres
  ports:
    - port: 5432
  clusterIP: None


k8s/base/activemq.yaml

apiVersion: apps/v1
kind: Deployment
metadata:
  name: activemq
  namespace: portal
spec:
  replicas: 1
  selector:
    matchLabels:
      app: activemq
  template:
    metadata:
      labels:
        app: activemq
    spec:
      containers:
        - name: activemq
          image: apache/activemq-classic:5.18.4
          ports:
            - containerPort: 61616
            - containerPort: 8161
          env:
            - name: ACTIVEMQ_USERNAME
              value: admin
            - name: ACTIVEMQ_PASSWORD
              value: admin
          readinessProbe:
            httpGet:
              path: /
              port: 8161
            initialDelaySeconds: 10
            periodSeconds: 10
          resources:
            requests:
              memory: "256Mi"
              cpu: "100m"
            limits:
              memory: "512Mi"
              cpu: "500m"
---
apiVersion: v1
kind: Service
metadata:
  name: activemq
  namespace: portal
spec:
  selector:
    app: activemq
  ports:
    - name: jms
      port: 61616
    - name: web
      port: 8161


k8s/ingress.yaml

apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: portal-ingress
  namespace: portal
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /$2
spec:
  ingressClassName: nginx
  rules:
    - host: api.portal.local
      http:
        paths:
          - path: /api/auth(/|$)(.*)
            pathType: ImplementationSpecific
            backend:
              service:
                name: identity-service
                port:
                  number: 8080
          - path: /api/identity(/|$)(.*)
            pathType: ImplementationSpecific
            backend:
              service:
                name: identity-service
                port:
                  number: 8080
          - path: /api/candidates(/|$)(.*)
            pathType: ImplementationSpecific
            backend:
              service:
                name: candidate-service
                port:
                  number: 8081
          - path: /api/employers(/|$)(.*)
            pathType: ImplementationSpecific
            backend:
              service:
                name: employer-service
                port:
                  number: 8082
          - path: /api/jobs(/|$)(.*)
            pathType: ImplementationSpecific
            backend:
              service:
                name: job-service
                port:
                  number: 8083


k8s/services/identity-service.yaml

apiVersion: apps/v1
kind: Deployment
metadata:
  name: identity-service
  namespace: portal
spec:
  replicas: 2
  selector:
    matchLabels:
      app: identity-service
  template:
    metadata:
      labels:
        app: identity-service
    spec:
      containers:
        - name: identity-service
          image: portal/identity-service:latest
          ports:
            - containerPort: 8080
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: jms
            - name: DATABASE_URL
              value: jdbc:postgresql://postgres:5432/identity_service_db
            - name: DATABASE_USERNAME
              valueFrom:
                secretKeyRef:
                  name: postgres-secret
                  key: POSTGRES_USER
            - name: DATABASE_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: postgres-secret
                  key: POSTGRES_PASSWORD
            - name: SPRING_ACTIVEMQ_BROKER_URL
              value: tcp://activemq:61616
            - name: JWT_SECRET
              valueFrom:
                secretKeyRef:
                  name: jwt-secret
                  key: JWT_SECRET
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
          resources:
            requests:
              memory: "256Mi"
              cpu: "200m"
            limits:
              memory: "512Mi"
              cpu: "500m"
---
apiVersion: v1
kind: Service
metadata:
  name: identity-service
  namespace: portal
spec:
  selector:
    app: identity-service
  ports:
    - port: 8080


k8s/services/candidate-service.yaml

apiVersion: apps/v1
kind: Deployment
metadata:
  name: candidate-service
  namespace: portal
spec:
  replicas: 2
  selector:
    matchLabels:
      app: candidate-service
  template:
    metadata:
      labels:
        app: candidate-service
    spec:
      containers:
        - name: candidate-service
          image: portal/candidate-service:latest
          ports:
            - containerPort: 8081
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: jms
            - name: SPRING_DATASOURCE_URL
              value: jdbc:postgresql://postgres:5432/candidate_service_db
            - name: SPRING_DATASOURCE_USERNAME
              valueFrom:
                secretKeyRef:
                  name: postgres-secret
                  key: POSTGRES_USER
            - name: SPRING_DATASOURCE_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: postgres-secret
                  key: POSTGRES_PASSWORD
            - name: SERVER_PORT
              value: "8081"
            - name: SPRING_ACTIVEMQ_BROKER_URL
              value: tcp://activemq:61616
            - name: IDENTITY_SERVICE_URL
              value: http://identity-service:8080
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8081
            initialDelaySeconds: 30
            periodSeconds: 10
          resources:
            requests:
              memory: "256Mi"
              cpu: "200m"
            limits:
              memory: "512Mi"
              cpu: "500m"
---
apiVersion: v1
kind: Service
metadata:
  name: candidate-service
  namespace: portal
spec:
  selector:
    app: candidate-service
  ports:
    - port: 8081


k8s/services/employer-service.yaml

apiVersion: apps/v1
kind: Deployment
metadata:
  name: employer-service
  namespace: portal
spec:
  replicas: 2
  selector:
    matchLabels:
      app: employer-service
  template:
    metadata:
      labels:
        app: employer-service
    spec:
      containers:
        - name: employer-service
          image: portal/employer-service:latest
          ports:
            - containerPort: 8082
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: jms
            - name: DB_HOST
              value: postgres
            - name: DB_NAME
              value: employer_service_db
            - name: DB_USER
              valueFrom:
                secretKeyRef:
                  name: postgres-secret
                  key: POSTGRES_USER
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: postgres-secret
                  key: POSTGRES_PASSWORD
            - name: SERVER_PORT
              value: "8082"
            - name: SPRING_ACTIVEMQ_BROKER_URL
              value: tcp://activemq:61616
            - name: IDENTITY_SERVICE_URL
              value: http://identity-service:8080
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8082
            initialDelaySeconds: 30
            periodSeconds: 10
          resources:
            requests:
              memory: "256Mi"
              cpu: "200m"
            limits:
              memory: "512Mi"
              cpu: "500m"
---
apiVersion: v1
kind: Service
metadata:
  name: employer-service
  namespace: portal
spec:
  selector:
    app: employer-service
  ports:
    - port: 8082


k8s/services/job-service.yaml

apiVersion: apps/v1
kind: Deployment
metadata:
  name: job-service
  namespace: portal
spec:
  replicas: 2
  selector:
    matchLabels:
      app: job-service
  template:
    metadata:
      labels:
        app: job-service
    spec:
      containers:
        - name: job-service
          image: portal/job-service:latest
          ports:
            - containerPort: 8083
          env:
            - name: SPRING_PROFILES_ACTIVE
              value: jms
            - name: SPRING_DATASOURCE_URL
              value: jdbc:postgresql://postgres:5432/job_service_db
            - name: DB_USER
              valueFrom:
                secretKeyRef:
                  name: postgres-secret
                  key: POSTGRES_USER
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: postgres-secret
                  key: POSTGRES_PASSWORD
            - name: SERVER_PORT
              value: "8083"
            - name: SPRING_ACTIVEMQ_BROKER_URL
              value: tcp://activemq:61616
            - name: IDENTITY_SERVICE_URL
              value: http://identity-service:8080
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8083
            initialDelaySeconds: 30
            periodSeconds: 10
          resources:
            requests:
              memory: "256Mi"
              cpu: "200m"
            limits:
              memory: "512Mi"
              cpu: "500m"
---
apiVersion: v1
kind: Service
metadata:
  name: job-service
  namespace: portal
spec:
  selector:
    app: job-service
  ports:
    - port: 8083


k8s/deploy.sh

#!/bin/bash
set -e

NAMESPACE="portal"

echo "=== Building Docker images ==="
docker build -t portal/identity-service:latest ./portal-identity-service
docker build -t portal/candidate-service:latest ./portal-candidate-service
docker build -t portal/employer-service:latest ./portal-employer-service
docker build -t portal/job-service:latest ./portal-job-service

echo "=== Applying K8s manifests ==="
kubectl apply -f k8s/base/namespace.yaml
kubectl apply -f k8s/base/postgres.yaml
kubectl apply -f k8s/base/activemq.yaml

echo "=== Waiting for infra to be ready ==="
kubectl -n $NAMESPACE wait --for=condition=ready pod -l app=postgres --timeout=120s
kubectl -n $NAMESPACE wait --for=condition=ready pod -l app=activemq --timeout=120s

echo "=== Deploying services ==="
kubectl apply -f k8s/services/
kubectl apply -f k8s/ingress.yaml

echo "=== Waiting for services ==="
kubectl -n $NAMESPACE wait --for=condition=ready pod -l app=identity-service --timeout=180s
kubectl -n $NAMESPACE wait --for=condition=ready pod -l app=candidate-service --timeout=180s
kubectl -n $NAMESPACE wait --for=condition=ready pod -l app=employer-service --timeout=180s
kubectl -n $NAMESPACE wait --for=condition=ready pod -l app=job-service --timeout=180s

echo "=== Done! ==="
kubectl -n $NAMESPACE get pods
kubectl -n $NAMESPACE get ingress


PHẦN 3: IDENTITY SERVICE (port 8080)

portal-identity-service/Dockerfile

FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY identity-domain/pom.xml identity-domain/
COPY identity-app/pom.xml identity-app/
COPY identity-rest/pom.xml identity-rest/
COPY identity-jms/pom.xml identity-jms/
COPY identity-sqs/pom.xml identity-sqs/
COPY identity-start/pom.xml identity-start/
RUN mvn dependency:go-offline -B
COPY . .
RUN mvn package -DskipTests -B -pl identity-start -am

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/identity-start/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]


portal-identity-service/pom.xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>com.portal.identity</groupId>
    <artifactId>identity-service</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <modules>
        <module>identity-domain</module>
        <module>identity-app</module>
        <module>identity-rest</module>
        <module>identity-jms</module>
        <module>identity-sqs</module>
        <module>identity-start</module>
    </modules>

    <properties>
        <java.version>17</java.version>
        <spring-cloud-aws.version>3.1.1</spring-cloud-aws.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.6</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.6</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.12.6</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.portal.identity</groupId>
                <artifactId>identity-domain</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.portal.identity</groupId>
                <artifactId>identity-app</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.portal.identity</groupId>
                <artifactId>identity-rest</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.portal.identity</groupId>
                <artifactId>identity-jms</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.portal.identity</groupId>
                <artifactId>identity-sqs</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.portal.identity</groupId>
                <artifactId>identity-start</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>io.awspring.cloud</groupId>
                <artifactId>spring-cloud-aws-dependencies</artifactId>
                <version>${spring-cloud-aws.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>


identity-domain/pom.xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.portal.identity</groupId>
        <artifactId>identity-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>identity-domain</artifactId>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
    </dependencies>
</project>


identity-app/pom.xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.portal.identity</groupId>
        <artifactId>identity-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>identity-app</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.portal.identity</groupId>
            <artifactId>identity-domain</artifactId>
        </dependency>
    </dependencies>
</project>


identity-rest/pom.xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.portal.identity</groupId>
        <artifactId>identity-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>identity-rest</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.portal.identity</groupId>
            <artifactId>identity-app</artifactId>
        </dependency>
    </dependencies>
</project>


identity-jms/pom.xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.portal.identity</groupId>
        <artifactId>identity-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>identity-jms</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.portal.identity</groupId>
            <artifactId>identity-app</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-activemq</artifactId>
        </dependency>
    </dependencies>
</project>


identity-sqs/pom.xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.portal.identity</groupId>
        <artifactId>identity-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>identity-sqs</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.portal.identity</groupId>
            <artifactId>identity-app</artifactId>
        </dependency>
        <dependency>
            <groupId>io.awspring.cloud</groupId>
            <artifactId>spring-cloud-aws-starter-sqs</artifactId>
        </dependency>
        <dependency>
            <groupId>io.awspring.cloud</groupId>
            <artifactId>spring-cloud-aws-starter</artifactId>
        </dependency>
    </dependencies>
</project>


identity-start/pom.xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.portal.identity</groupId>
        <artifactId>identity-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>identity-start</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.portal.identity</groupId>
            <artifactId>identity-rest</artifactId>
        </dependency>
        <dependency>
            <groupId>com.portal.identity</groupId>
            <artifactId>identity-jms</artifactId>
        </dependency>
        <dependency>
            <groupId>com.portal.identity</groupId>
            <artifactId>identity-sqs</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>


identity-start/src/main/resources/application.properties

server.servlet.context-path=/identity
spring.profiles.include=jms
spring.autoconfigure.exclude=io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration

spring.datasource.url=${DATABASE_URL:jdbc:postgresql://localhost:5432/identity_service_db}
spring.datasource.username=${DATABASE_USERNAME:postgres}
spring.datasource.password=${DATABASE_PASSWORD:postgres}
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=false

# JWT
jwt.secret=${JWT_SECRET:my-super-secret-key-that-is-at-least-256-bits-long!!}
jwt.expiration-ms=3600000


identity-start/src/main/resources/application-jms.properties

spring.activemq.broker-url=${ACTIVEMQ_BROKER_URL:tcp://localhost:61616}
spring.activemq.user=${ACTIVEMQ_USER:admin}
spring.activemq.password=${ACTIVEMQ_PASSWORD:admin}

queue.user.registered=portal.user.registered
queue.user.deactivated=portal.user.deactivated


identity-start/src/main/resources/application-sqs.properties

queue.user.registered=${SQS_USER_REGISTERED_QUEUE_ARN}
queue.user.deactivated=${SQS_USER_DEACTIVATED_QUEUE_ARN}

spring.cloud.aws.region.static=${AWS_REGION:us-east-1}


Identity Service - Java Source Files

identity-domain: com.portal.identity.domain

Permission.java

package com.portal.identity.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}


Role.java

package com.portal.identity.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    private String name;

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}


RoleRepository.java

package com.portal.identity.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Integer> {
    Optional<Role> findByName(String name);
}


User.java

package com.portal.identity.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private String email;

    private String passwordHash;

    private String role;

    private boolean isActive;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}


UserRepository.java

package com.portal.identity.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
}


identity-app: com.portal.identity.app

messaging/IdentityMessageSender.java

package com.portal.identity.app.messaging;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public interface IdentityMessageSender {

    void sendUserRegistered(String messageBody, Map<String, String> messageHeaders);

    void sendUserDeactivated(String messageBody, Map<String, String> messageHeaders);

    default Map<String, String> getDefaultMessageHeaders(String groupId) {
        Map<String, String> headers = new HashMap<>();
        headers.put("message-group-id", groupId);
        headers.put("message-deduplication-id", UUID.randomUUID().toString());
        return headers;
    }
}


messaging/DefaultIdentityMessageSender.java

package com.portal.identity.app.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

public class DefaultIdentityMessageSender implements IdentityMessageSender {

    private static final Logger log = LoggerFactory.getLogger(DefaultIdentityMessageSender.class);

    @Override
    public void sendUserRegistered(String messageBody, Map<String, String> messageHeaders) {
        log.info("No-op sendUserRegistered: {}", messageBody);
    }

    @Override
    public void sendUserDeactivated(String messageBody, Map<String, String> messageHeaders) {
        log.info("No-op sendUserDeactivated: {}", messageBody);
    }
}


messaging/IdentityMessageSenderConfigurer.java

package com.portal.identity.app.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IdentityMessageSenderConfigurer {

    @Bean
    @ConditionalOnMissingBean(IdentityMessageSender.class)
    public IdentityMessageSender identityMessageSender() {
        return new DefaultIdentityMessageSender();
    }
}


service/JwtProvider.java

package com.portal.identity.app.service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Component
public class JwtProvider {

    private final SecretKey key;
    private final long expirationMs;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration-ms:3600000}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(String userId, String email, String role) {
        return Jwts.builder()
                .subject(userId)
                .claims(Map.of("email", email, "role", role))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(key)
                .compact();
    }

    public Map<String, Object> validateToken(String token) {
        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
        return Map.of(
                "userId", claims.getSubject(),
                "email", claims.get("email", String.class),
                "role", claims.get("role", String.class)
        );
    }
}


service/AuthService.java

package com.portal.identity.app.service;

import com.portal.identity.domain.User;
import com.portal.identity.domain.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtProvider jwtProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
    }

    public String login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!user.isActive()) {
            throw new RuntimeException("Account is deactivated");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        return jwtProvider.generateToken(user.getId().toString(), user.getEmail(), user.getRole());
    }
}


service/UserRegistrationService.java

package com.portal.identity.app.service;

import com.portal.identity.app.messaging.IdentityMessageSender;
import com.portal.identity.domain.User;
import com.portal.identity.domain.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class UserRegistrationService {

    private final UserRepository userRepository;
    private final IdentityMessageSender identityMessageSender;
    private final PasswordEncoder passwordEncoder;

    public UserRegistrationService(UserRepository userRepository, IdentityMessageSender identityMessageSender, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.identityMessageSender = identityMessageSender;
        this.passwordEncoder = passwordEncoder;
    }

    public User registerUser(String email, String password, String roleName) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(roleName);
        user.setActive(true);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);

        Map<String, String> headers = identityMessageSender.getDefaultMessageHeaders(saved.getId().toString());
        headers.put("userId", saved.getId().toString());
        headers.put("email", saved.getEmail());
        headers.put("role", saved.getRole());

        identityMessageSender.sendUserRegistered(saved.getId().toString(), headers);

        return saved;
    }
}


identity-rest: com.portal.identity.rest

AuthController.java

package com.portal.identity.rest;

import com.portal.identity.app.service.AuthService;
import com.portal.identity.app.service.JwtProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwtProvider;

    public AuthController(AuthService authService, JwtProvider jwtProvider) {
        this.authService = authService;
        this.jwtProvider = jwtProvider;
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody Map<String, String> request) {
        String token = authService.login(request.get("email"), request.get("password"));
        return ResponseEntity.ok(Map.of("token", token));
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Object>> validate(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        Map<String, Object> claims = jwtProvider.validateToken(token);
        return ResponseEntity.ok(claims);
    }
}


UserController.java

package com.portal.identity.rest;

import com.portal.identity.app.service.UserRegistrationService;
import com.portal.identity.domain.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserRegistrationService userRegistrationService;

    public UserController(UserRegistrationService userRegistrationService) {
        this.userRegistrationService = userRegistrationService;
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody Map<String, String> request) {
        User user = userRegistrationService.registerUser(
                request.get("email"),
                request.get("passwordHash"),
                request.get("roleName")
        );
        return ResponseEntity.ok(user);
    }
}


config/SecurityConfig.java

package com.portal.identity.rest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/api/users/register", "/actuator/health").permitAll()
                .anyRequest().authenticated()
            );
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}


identity-jms: com.portal.identity.jms

sender/JmsIdentityMessageSender.java

package com.portal.identity.jms.sender;

import com.portal.identity.app.messaging.IdentityMessageSender;
import jakarta.jms.TextMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("identityMessageSender")
@Profile("jms")
public class JmsIdentityMessageSender implements IdentityMessageSender {

    private final JmsTemplate jmsTemplate;

    @Value("${queue.user.registered}")
    private String userRegisteredQueue;

    @Value("${queue.user.deactivated}")
    private String userDeactivatedQueue;

    public JmsIdentityMessageSender(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    @Override
    public void sendUserRegistered(String messageBody, Map<String, String> messageHeaders) {
        sendMessage(userRegisteredQueue, messageBody, messageHeaders);
    }

    @Override
    public void sendUserDeactivated(String messageBody, Map<String, String> messageHeaders) {
        sendMessage(userDeactivatedQueue, messageBody, messageHeaders);
    }

    private void sendMessage(String queue, String messageBody, Map<String, String> messageHeaders) {
        jmsTemplate.send(queue, session -> {
            TextMessage message = session.createTextMessage(messageBody);
            for (Map.Entry<String, String> entry : messageHeaders.entrySet()) {
                message.setStringProperty(entry.getKey(), entry.getValue());
            }
            return message;
        });
    }
}


identity-sqs: com.portal.identity.sqs

sender/SqsIdentityMessageSender.java

package com.portal.identity.sqs.sender;

import com.portal.identity.app.messaging.IdentityMessageSender;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("identityMessageSender")
@Profile("sqs")
public class SqsIdentityMessageSender implements IdentityMessageSender {

    private final SqsTemplate sqsTemplate;

    @Value("${queue.user.registered}")
    private String userRegisteredQueueArn;

    @Value("${queue.user.deactivated}")
    private String userDeactivatedQueueArn;

    public SqsIdentityMessageSender(SqsTemplate sqsTemplate) {
        this.sqsTemplate = sqsTemplate;
    }

    @Override
    public void sendUserRegistered(String messageBody, Map<String, String> messageHeaders) {
        sendMessage(userRegisteredQueueArn, messageBody, messageHeaders);
    }

    @Override
    public void sendUserDeactivated(String messageBody, Map<String, String> messageHeaders) {
        sendMessage(userDeactivatedQueueArn, messageBody, messageHeaders);
    }

    private void sendMessage(String queueArn, String messageBody, Map<String, String> messageHeaders) {
        String queueName = extractQueueName(queueArn);
        sqsTemplate.send(to -> to
                .queue(queueName)
                .payload(messageBody)
                .headers(new java.util.HashMap<>(messageHeaders))
        );
    }

    private String extractQueueName(String arn) {
        if (arn != null && arn.contains(":")) {
            return arn.substring(arn.lastIndexOf(":") + 1);
        }
        return arn;
    }
}


identity-start: com.portal.identity

IdentityServiceApplication.java

package com.portal.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.portal.identity")
public class IdentityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}


PHẦN 4: CANDIDATE SERVICE (port 8081)

portal-candidate-service/Dockerfile

FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY candidate-domain/pom.xml candidate-domain/
COPY candidate-app/pom.xml candidate-app/
COPY candidate-rest/pom.xml candidate-rest/
COPY candidate-jms/pom.xml candidate-jms/
COPY candidate-sqs/pom.xml candidate-sqs/
COPY candidate-start/pom.xml candidate-start/
RUN mvn dependency:go-offline -B
COPY . .
RUN mvn package -DskipTests -B -pl candidate-start -am

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/candidate-start/target/*.jar app.jar
EXPOSE 8081
ENTRYPOINT ["java", "-jar", "app.jar"]


portal-candidate-service/pom.xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>com.portal.candidate</groupId>
    <artifactId>candidate-service</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <properties>
        <java.version>17</java.version>
        <spring-cloud-aws.version>3.1.1</spring-cloud-aws.version>
    </properties>

    <modules>
        <module>candidate-domain</module>
        <module>candidate-app</module>
        <module>candidate-rest</module>
        <module>candidate-jms</module>
        <module>candidate-sqs</module>
        <module>candidate-start</module>
    </modules>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>io.awspring.cloud</groupId>
                <artifactId>spring-cloud-aws-dependencies</artifactId>
                <version>${spring-cloud-aws.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>


candidate-domain/pom.xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.portal.candidate</groupId>
        <artifactId>candidate-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>candidate-domain</artifactId>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
    </dependencies>
</project>


candidate-app/pom.xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.portal.candidate</groupId>
        <artifactId>candidate-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>candidate-app</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.portal.candidate</groupId>
            <artifactId>candidate-domain</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
    </dependencies>
</project>


candidate-rest/pom.xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.portal.candidate</groupId>
        <artifactId>candidate-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>candidate-rest</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.portal.candidate</groupId>
            <artifactId>candidate-app</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
    </dependencies>
</project>


candidate-jms/pom.xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.portal.candidate</groupId>
        <artifactId>candidate-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>candidate-jms</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.portal.candidate</groupId>
            <artifactId>candidate-app</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-activemq</artifactId>
        </dependency>
    </dependencies>
</project>


candidate-sqs/pom.xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.portal.candidate</groupId>
        <artifactId>candidate-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>candidate-sqs</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.portal.candidate</groupId>
            <artifactId>candidate-app</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>io.awspring.cloud</groupId>
            <artifactId>spring-cloud-aws-starter-sqs</artifactId>
        </dependency>
    </dependencies>
</project>


candidate-start/pom.xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.portal.candidate</groupId>
        <artifactId>candidate-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>candidate-start</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.portal.candidate</groupId>
            <artifactId>candidate-rest</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.portal.candidate</groupId>
            <artifactId>candidate-jms</artifactId>
            <version>${project.version}</version>
        </dependency>
        <dependency>
            <groupId>com.portal.candidate</groupId>
            <artifactId>candidate-sqs</artifactId>
            <version>${project.version}</version>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>


candidate-start/src/main/resources/application.properties

spring.application.name=candidate-service
server.port=${SERVER_PORT:8081}
server.servlet.context-path=/candidate
spring.profiles.include=jms
spring.autoconfigure.exclude=io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration

spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/candidate_service_db}
spring.datasource.username=${SPRING_DATASOURCE_USERNAME:postgres}
spring.datasource.password=${SPRING_DATASOURCE_PASSWORD:postgres}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.hibernate.ddl-auto=update
spring.jpa.open-in-view=false


candidate-start/src/main/resources/application-jms.properties

spring.activemq.broker-url=tcp://localhost:61616
spring.activemq.user=admin
spring.activemq.password=admin

queue.job.applied=portal.job.applied
queue.user.registered=portal.user.registered
queue.application.status-changed=portal.application.status


candidate-start/src/main/resources/application-sqs.properties

spring.cloud.aws.region.static=ap-southeast-1
spring.cloud.aws.credentials.access-key=${AWS_ACCESS_KEY:}
spring.cloud.aws.credentials.secret-key=${AWS_SECRET_KEY:}

queue.job.applied=${AWS_JOB_APPLIED_QUEUE:CHANGEIT}
queue.user.registered=${AWS_USER_REGISTERED_QUEUE:CHANGEIT}
queue.application.status-changed=${AWS_APPLICATION_STATUS_CHANGED_QUEUE:CHANGEIT}


Candidate Service - Java Source Files

candidate-domain: com.portal.candidate.domain

Candidate.java

package com.portal.candidate.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "candidates")
public class Candidate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    private String firstName;
    private String lastName;
    private String email;
    private String phoneNumber;

    @Column(columnDefinition = "TEXT")
    private String address;

    private String headline;
    private String avatarUrl;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getHeadline() { return headline; }
    public void setHeadline(String headline) { this.headline = headline; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}


CandidateRepository.java

package com.portal.candidate.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface CandidateRepository extends JpaRepository<Candidate, UUID> {
    Optional<Candidate> findByUserId(UUID userId);
}


Resume.java

package com.portal.candidate.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "resumes")
public class Resume {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id")
    private Candidate candidate;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String resumeData;

    private String fileUrl;
    private boolean isDefault;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Candidate getCandidate() { return candidate; }
    public void setCandidate(Candidate candidate) { this.candidate = candidate; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getResumeData() { return resumeData; }
    public void setResumeData(String resumeData) { this.resumeData = resumeData; }
    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
}


ResumeRepository.java

package com.portal.candidate.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {
    List<Resume> findByCandidateId(UUID candidateId);
}


Experience.java

package com.portal.candidate.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "experiences")
public class Experience {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id")
    private Resume resume;

    private String jobTitle;
    private String companyName;
    private LocalDate startDate;
    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Resume getResume() { return resume; }
    public void setResume(Resume resume) { this.resume = resume; }
    public String getJobTitle() { return jobTitle; }
    public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}


Education.java

package com.portal.candidate.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "educations")
public class Education {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id")
    private Resume resume;

    private String institution;
    private String degree;
    private String fieldOfStudy;
    private LocalDate startDate;
    private LocalDate endDate;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Resume getResume() { return resume; }
    public void setResume(Resume resume) { this.resume = resume; }
    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }
    public String getDegree() { return degree; }
    public void setDegree(String degree) { this.degree = degree; }
    public String getFieldOfStudy() { return fieldOfStudy; }
    public void setFieldOfStudy(String fieldOfStudy) { this.fieldOfStudy = fieldOfStudy; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
}


Skill.java

package com.portal.candidate.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "skills")
public class Skill {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id")
    private Candidate candidate;

    private String skillName;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Candidate getCandidate() { return candidate; }
    public void setCandidate(Candidate candidate) { this.candidate = candidate; }
    public String getSkillName() { return skillName; }
    public void setSkillName(String skillName) { this.skillName = skillName; }
}


AppliedJob.java

package com.portal.candidate.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "applied_jobs")
public class AppliedJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidate_id")
    private Candidate candidate;

    @Column(name = "job_id", nullable = false)
    private UUID jobId;

    @Column(name = "application_status")
    private String applicationStatus = "PENDING";

    @Column(name = "applied_at")
    private LocalDateTime appliedAt = LocalDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Candidate getCandidate() { return candidate; }
    public void setCandidate(Candidate candidate) { this.candidate = candidate; }
    public UUID getJobId() { return jobId; }
    public void setJobId(UUID jobId) { this.jobId = jobId; }
    public String getApplicationStatus() { return applicationStatus; }
    public void setApplicationStatus(String applicationStatus) { this.applicationStatus = applicationStatus; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
}


AppliedJobRepository.java

package com.portal.candidate.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AppliedJobRepository extends JpaRepository<AppliedJob, UUID> {
    List<AppliedJob> findByCandidateId(UUID candidateId);
    List<AppliedJob> findByJobId(UUID jobId);
}


candidate-app: com.portal.candidate.app

messaging/CandidateMessageSender.java

package com.portal.candidate.app.messaging;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public interface CandidateMessageSender {

    String GROUP_ID_HEADER = "message-group-id";
    String DEDUPLICATION_ID_HEADER = "message-deduplication-id";

    void sendJobApplied(String messageBody, Map<String, String> messageHeaders);

    void sendApplicationWithdrawn(String messageBody, Map<String, String> messageHeaders);

    default Map<String, String> getDefaultMessageHeaders(String groupId) {
        Map<String, String> headers = new HashMap<>();
        headers.put(GROUP_ID_HEADER, groupId);
        headers.put(DEDUPLICATION_ID_HEADER, UUID.randomUUID().toString());
        return headers;
    }
}


messaging/DefaultCandidateMessageSender.java

package com.portal.candidate.app.messaging;

import java.util.Map;

public class DefaultCandidateMessageSender implements CandidateMessageSender {

    @Override
    public void sendJobApplied(String messageBody, Map<String, String> messageHeaders) {}

    @Override
    public void sendApplicationWithdrawn(String messageBody, Map<String, String> messageHeaders) {}
}


messaging/CandidateMessageSenderConfigurer.java

package com.portal.candidate.app.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CandidateMessageSenderConfigurer {

    @Bean
    @ConditionalOnMissingBean(CandidateMessageSender.class)
    public CandidateMessageSender candidateMessageSender() {
        return new DefaultCandidateMessageSender();
    }
}


service/CandidateProfileService.java

package com.portal.candidate.app.service;

import com.portal.candidate.app.messaging.CandidateMessageSender;
import com.portal.candidate.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class CandidateProfileService {

    private final CandidateRepository candidateRepository;
    private final AppliedJobRepository appliedJobRepository;
    private final CandidateMessageSender messageSender;

    public CandidateProfileService(CandidateRepository candidateRepository,
                                   AppliedJobRepository appliedJobRepository,
                                   CandidateMessageSender messageSender) {
        this.candidateRepository = candidateRepository;
        this.appliedJobRepository = appliedJobRepository;
        this.messageSender = messageSender;
    }

    @Transactional
    public Candidate createProfile(UUID userId, String email) {
        Candidate candidate = new Candidate();
        candidate.setUserId(userId);
        candidate.setEmail(email);
        return candidateRepository.save(candidate);
    }

    @Transactional
    public AppliedJob applyJob(UUID candidateId, UUID jobId) {
        Candidate candidate = candidateRepository.findById(candidateId)
                .orElseThrow(() -> new RuntimeException("Candidate not found"));

        AppliedJob appliedJob = new AppliedJob();
        appliedJob.setCandidate(candidate);
        appliedJob.setJobId(jobId);
        AppliedJob saved = appliedJobRepository.save(appliedJob);

        Map<String, String> headers = messageSender.getDefaultMessageHeaders(candidateId.toString());
        headers.put("candidateId", candidateId.toString());
        headers.put("jobId", jobId.toString());
        messageSender.sendJobApplied("", headers);

        return saved;
    }
}


candidate-rest: com.portal.candidate.rest

CandidateController.java

package com.portal.candidate.rest;

import com.portal.candidate.app.service.CandidateProfileService;
import com.portal.candidate.domain.AppliedJob;
import com.portal.candidate.domain.Candidate;
import com.portal.candidate.domain.CandidateRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/candidates")
public class CandidateController {

    private final CandidateRepository candidateRepository;
    private final CandidateProfileService candidateProfileService;

    public CandidateController(CandidateRepository candidateRepository,
                               CandidateProfileService candidateProfileService) {
        this.candidateRepository = candidateRepository;
        this.candidateProfileService = candidateProfileService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<Candidate> getCandidate(@PathVariable UUID id) {
        return candidateRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/apply")
    public ResponseEntity<AppliedJob> applyJob(@RequestBody Map<String, String> request) {
        UUID candidateId = UUID.fromString(request.get("candidateId"));
        UUID jobId = UUID.fromString(request.get("jobId"));
        return ResponseEntity.ok(candidateProfileService.applyJob(candidateId, jobId));
    }
}


config/JwtAuthFilter.java

package com.portal.candidate.rest.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final RestTemplate restTemplate;
    private final String identityServiceUrl;

    public JwtAuthFilter(@Value("${identity-service.url:http://identity-service:8080}") String identityServiceUrl) {
        this.restTemplate = new RestTemplate();
        this.identityServiceUrl = identityServiceUrl;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Missing or invalid Authorization header\"}");
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authHeader);
            ResponseEntity<Map> result = restTemplate.exchange(
                    identityServiceUrl + "/identity/api/auth/validate",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            Map body = result.getBody();
            request.setAttribute("userId", body.get("userId"));
            request.setAttribute("email", body.get("email"));
            request.setAttribute("role", body.get("role"));

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Invalid token\"}");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/actuator/health");
    }
}


config/SecurityConfig.java

package com.portal.candidate.rest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}


candidate-jms: com.portal.candidate.jms

listener/JmsCandidateMessageReceiver.java

package com.portal.candidate.jms.listener;

import com.portal.candidate.app.service.CandidateProfileService;
import com.portal.candidate.domain.AppliedJob;
import com.portal.candidate.domain.AppliedJobRepository;
import jakarta.jms.Message;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@Profile("jms")
public class JmsCandidateMessageReceiver {

    private final CandidateProfileService candidateProfileService;
    private final AppliedJobRepository appliedJobRepository;

    public JmsCandidateMessageReceiver(CandidateProfileService candidateProfileService,
                                       AppliedJobRepository appliedJobRepository) {
        this.candidateProfileService = candidateProfileService;
        this.appliedJobRepository = appliedJobRepository;
    }

    @JmsListener(destination = "${queue.user.registered}")
    public void receiveUserRegistered(Message message) throws Exception {
        String role = message.getStringProperty("role");
        if ("ROLE_CANDIDATE".equals(role)) {
            UUID userId = UUID.fromString(message.getStringProperty("userId"));
            String email = message.getStringProperty("email");
            candidateProfileService.createProfile(userId, email);
        }
    }

    @JmsListener(destination = "${queue.application.status-changed}")
    @Transactional
    public void receiveApplicationStatusChanged(Message message) throws Exception {
        UUID jobId = UUID.fromString(message.getStringProperty("jobId"));
        String status = message.getStringProperty("status");
        List<AppliedJob> jobs = appliedJobRepository.findByJobId(jobId);
        jobs.forEach(j -> j.setApplicationStatus(status));
        appliedJobRepository.saveAll(jobs);
    }
}


sender/JmsCandidateMessageSender.java

package com.portal.candidate.jms.sender;

import com.portal.candidate.app.messaging.CandidateMessageSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import jakarta.jms.JMSException;
import java.util.Map;

@Component("candidateMessageSender")
@Profile("jms")
public class JmsCandidateMessageSender implements CandidateMessageSender {

    private final JmsTemplate jmsTemplate;

    @Value("${queue.job.applied}")
    private String jobAppliedQueue;

    @Value("${queue.application.withdrawn:portal-application-withdrawn}")
    private String applicationWithdrawnQueue;

    public JmsCandidateMessageSender(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    @Override
    public void sendJobApplied(String messageBody, Map<String, String> messageHeaders) {
        jmsTemplate.send(jobAppliedQueue, session -> {
            var msg = session.createTextMessage(messageBody);
            messageHeaders.forEach((k, v) -> {
                try { msg.setStringProperty(k, v); } catch (JMSException e) { throw new RuntimeException(e); }
            });
            return msg;
        });
    }

    @Override
    public void sendApplicationWithdrawn(String messageBody, Map<String, String> messageHeaders) {
        jmsTemplate.send(applicationWithdrawnQueue, session -> {
            var msg = session.createTextMessage(messageBody);
            messageHeaders.forEach((k, v) -> {
                try { msg.setStringProperty(k, v); } catch (JMSException e) { throw new RuntimeException(e); }
            });
            return msg;
        });
    }
}


candidate-sqs: com.portal.candidate.sqs

listener/SqsCandidateMessageReceiver.java

package com.portal.candidate.sqs.listener;

import com.portal.candidate.app.service.CandidateProfileService;
import com.portal.candidate.domain.AppliedJob;
import com.portal.candidate.domain.AppliedJobRepository;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
@Profile("sqs")
public class SqsCandidateMessageReceiver {

    private final CandidateProfileService candidateProfileService;
    private final AppliedJobRepository appliedJobRepository;

    public SqsCandidateMessageReceiver(CandidateProfileService candidateProfileService,
                                       AppliedJobRepository appliedJobRepository) {
        this.candidateProfileService = candidateProfileService;
        this.appliedJobRepository = appliedJobRepository;
    }

    @SqsListener("${queue.user.registered}")
    public void receiveUserRegistered(@Header("role") String role,
                                      @Header("userId") String userId,
                                      @Header("email") String email) {
        if ("ROLE_CANDIDATE".equals(role)) {
            candidateProfileService.createProfile(UUID.fromString(userId), email);
        }
    }

    @SqsListener("${queue.application.status-changed}")
    @Transactional
    public void receiveApplicationStatusChanged(@Header("jobId") String jobId,
                                                @Header("status") String status) {
        List<AppliedJob> jobs = appliedJobRepository.findByJobId(UUID.fromString(jobId));
        jobs.forEach(j -> j.setApplicationStatus(status));
        appliedJobRepository.saveAll(jobs);
    }
}


sender/SqsCandidateMessageSender.java

package com.portal.candidate.sqs.sender;

import com.portal.candidate.app.messaging.CandidateMessageSender;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("candidateMessageSender")
@Profile("sqs")
public class SqsCandidateMessageSender implements CandidateMessageSender {

    private final SqsTemplate sqsTemplate;

    @Value("${queue.job.applied}")
    private String jobAppliedQueue;

    @Value("${queue.application.withdrawn:portal-application-withdrawn}")
    private String applicationWithdrawnQueue;

    public SqsCandidateMessageSender(SqsTemplate sqsTemplate) {
        this.sqsTemplate = sqsTemplate;
    }

    @Override
    public void sendJobApplied(String messageBody, Map<String, String> messageHeaders) {
        sqsTemplate.send(jobAppliedQueue,
                MessageBuilder.withPayload(messageBody).copyHeaders(messageHeaders).build());
    }

    @Override
    public void sendApplicationWithdrawn(String messageBody, Map<String, String> messageHeaders) {
        sqsTemplate.send(applicationWithdrawnQueue,
                MessageBuilder.withPayload(messageBody).copyHeaders(messageHeaders).build());
    }
}


candidate-start: com.portal.candidate

CandidateServiceApplication.java

package com.portal.candidate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CandidateServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CandidateServiceApplication.class, args);
    }
}


PHẦN 5: EMPLOYER SERVICE (port 8082)

portal-employer-service/Dockerfile

FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY employer-domain/pom.xml employer-domain/
COPY employer-app/pom.xml employer-app/
COPY employer-rest/pom.xml employer-rest/
COPY employer-jms/pom.xml employer-jms/
COPY employer-sqs/pom.xml employer-sqs/
COPY employer-start/pom.xml employer-start/
RUN mvn dependency:go-offline -B
COPY . .
RUN mvn package -DskipTests -B -pl employer-start -am

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/employer-start/target/*.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]


portal-employer-service/pom.xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <groupId>com.portal.employer</groupId>
    <artifactId>employer-service</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <modules>
        <module>employer-domain</module>
        <module>employer-app</module>
        <module>employer-rest</module>
        <module>employer-jms</module>
        <module>employer-sqs</module>
        <module>employer-start</module>
    </modules>

    <properties>
        <java.version>17</java.version>
        <spring-cloud-aws.version>3.1.1</spring-cloud-aws.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.portal.employer</groupId>
                <artifactId>employer-domain</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.portal.employer</groupId>
                <artifactId>employer-app</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.portal.employer</groupId>
                <artifactId>employer-rest</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.portal.employer</groupId>
                <artifactId>employer-jms</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.portal.employer</groupId>
                <artifactId>employer-sqs</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.portal.employer</groupId>
                <artifactId>employer-start</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>io.awspring.cloud</groupId>
                <artifactId>spring-cloud-aws-dependencies</artifactId>
                <version>${spring-cloud-aws.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>


employer-domain/pom.xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.portal.employer</groupId>
        <artifactId>employer-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>employer-domain</artifactId>
</project>


employer-app/pom.xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.portal.employer</groupId>
        <artifactId>employer-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>employer-app</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.portal.employer</groupId>
            <artifactId>employer-domain</artifactId>
        </dependency>
    </dependencies>
</project>


employer-rest/pom.xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.portal.employer</groupId>
        <artifactId>employer-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>employer-rest</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.portal.employer</groupId>
            <artifactId>employer-app</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
    </dependencies>
</project>


employer-jms/pom.xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.portal.employer</groupId>
        <artifactId>employer-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>employer-jms</artifactId>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-activemq</artifactId>
        </dependency>
        <dependency>
            <groupId>com.portal.employer</groupId>
            <artifactId>employer-app</artifactId>
        </dependency>
    </dependencies>
</project>


employer-sqs/pom.xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.portal.employer</groupId>
        <artifactId>employer-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>employer-sqs</artifactId>
    <dependencies>
        <dependency>
            <groupId>io.awspring.cloud</groupId>
            <artifactId>spring-cloud-aws-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>io.awspring.cloud</groupId>
            <artifactId>spring-cloud-aws-starter-sqs</artifactId>
        </dependency>
        <dependency>
            <groupId>com.portal.employer</groupId>
            <artifactId>employer-app</artifactId>
        </dependency>
    </dependencies>
</project>


employer-start/pom.xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.portal.employer</groupId>
        <artifactId>employer-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <artifactId>employer-start</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.portal.employer</groupId>
            <artifactId>employer-rest</artifactId>
        </dependency>
        <dependency>
            <groupId>com.portal.employer</groupId>
            <artifactId>employer-jms</artifactId>
        </dependency>
        <dependency>
            <groupId>com.portal.employer</groupId>
            <artifactId>employer-sqs</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>


employer-start/src/main/resources/application.properties

spring.profiles.include=jms
spring.autoconfigure.exclude=io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5432}/${DB_NAME:employer_service_db}
spring.datasource.username=${DB_USER:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}
spring.datasource.hikari.maximum-pool-size=5
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation=true
server.port=${SERVER_PORT:8082}
server.servlet.context-path=/employer


employer-start/src/main/resources/application-jms.properties

spring.activemq.broker-url=tcp://localhost:61616
spring.activemq.user=admin
spring.activemq.password=admin

queue.user.registered=portal.user.registered
queue.company.created=portal.company.created
queue.company.updated=portal.company.updated


employer-start/src/main/resources/application-sqs.properties

spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration

queue.user.registered=${AWS_USER_REGISTERED_QUEUE:CHANGEIT}
queue.company.created=${AWS_COMPANY_CREATED_QUEUE:CHANGEIT}
queue.company.updated=${AWS_COMPANY_UPDATED_QUEUE:CHANGEIT}


Employer Service - Java Source Files

employer-domain: com.portal.employer.domain

Company.java

package com.portal.employer.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "companies")
public class Company {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "logo_url")
    private String logoUrl;

    private String website;

    @Column(columnDefinition = "TEXT")
    private String address;

    public Company() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
}


CompanyRepository.java

package com.portal.employer.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {
}


Employer.java

package com.portal.employer.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "employers")
public class Employer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    public Employer() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
}


EmployerRepository.java

package com.portal.employer.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface EmployerRepository extends JpaRepository<Employer, UUID> {
    Optional<Employer> findByUserId(UUID userId);
}


employer-app: com.portal.employer.app

messaging/EmployerMessageSender.java

package com.portal.employer.app.messaging;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public interface EmployerMessageSender {

    void sendCompanyCreated(String messageBody, Map<String, String> messageHeaders);

    void sendCompanyUpdated(String messageBody, Map<String, String> messageHeaders);

    default Map<String, String> getDefaultMessageHeaders(String groupId) {
        Map<String, String> headers = new HashMap<>();
        headers.put("message-group-id", groupId);
        headers.put("message-deduplication-id", UUID.randomUUID().toString());
        return headers;
    }
}


messaging/DefaultEmployerMessageSender.java

package com.portal.employer.app.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

public class DefaultEmployerMessageSender implements EmployerMessageSender {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultEmployerMessageSender.class);

    @Override
    public void sendCompanyCreated(String messageBody, Map<String, String> messageHeaders) {
        LOG.warn("No messaging provider configured - sendCompanyCreated not sent");
    }

    @Override
    public void sendCompanyUpdated(String messageBody, Map<String, String> messageHeaders) {
        LOG.warn("No messaging provider configured - sendCompanyUpdated not sent");
    }
}


messaging/EmployerMessageSenderConfigurer.java

package com.portal.employer.app.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmployerMessageSenderConfigurer {

    @Bean
    @ConditionalOnMissingBean(EmployerMessageSender.class)
    public EmployerMessageSender employerMessageSender() {
        return new DefaultEmployerMessageSender();
    }
}


service/EmployerProfileService.java

package com.portal.employer.app.service;

import com.portal.employer.app.messaging.EmployerMessageSender;
import com.portal.employer.domain.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class EmployerProfileService {

    @Autowired
    private EmployerRepository employerRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private EmployerMessageSender employerMessageSender;

    @Transactional
    public Employer createProfile(UUID userId, String firstName, String lastName) {
        Employer employer = new Employer();
        employer.setUserId(userId);
        employer.setFirstName(firstName);
        employer.setLastName(lastName);
        return employerRepository.save(employer);
    }

    @Transactional
    public Company createCompany(String companyName, String description, String website, String address) {
        Company company = new Company();
        company.setCompanyName(companyName);
        company.setDescription(description);
        company.setWebsite(website);
        company.setAddress(address);
        Company saved = companyRepository.save(company);

        Map<String, String> headers = employerMessageSender.getDefaultMessageHeaders(saved.getId().toString());
        headers.put("companyId", saved.getId().toString());
        headers.put("companyName", saved.getCompanyName());
        employerMessageSender.sendCompanyCreated(null, headers);

        return saved;
    }

    @Transactional
    public Company updateCompany(UUID companyId, String companyName, String description) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found: " + companyId));
        company.setCompanyName(companyName);
        company.setDescription(description);
        Company saved = companyRepository.save(company);

        Map<String, String> headers = employerMessageSender.getDefaultMessageHeaders(saved.getId().toString());
        headers.put("companyId", saved.getId().toString());
        headers.put("companyName", saved.getCompanyName());
        employerMessageSender.sendCompanyUpdated(null, headers);

        return saved;
    }
}


employer-rest: com.portal.employer.rest

CompanyController.java

package com.portal.employer.rest;

import com.portal.employer.app.service.EmployerProfileService;
import com.portal.employer.domain.Company;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/companies")
public class CompanyController {

    @Autowired
    private EmployerProfileService employerProfileService;

    @PostMapping
    public ResponseEntity<Company> create(@RequestBody Map<String, String> body) {
        Company company = employerProfileService.createCompany(
                body.get("companyName"), body.get("description"),
                body.get("website"), body.get("address"));
        return ResponseEntity.ok(company);
    }

    @PutMapping("/{id}")
    public ResponseEntity<Company> update(@PathVariable UUID id, @RequestBody Map<String, String> body) {
        Company company = employerProfileService.updateCompany(id, body.get("companyName"), body.get("description"));
        return ResponseEntity.ok(company);
    }
}


config/JwtAuthFilter.java

package com.portal.employer.rest.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final RestTemplate restTemplate;
    private final String identityServiceUrl;

    public JwtAuthFilter(@Value("${identity-service.url:http://identity-service:8080}") String identityServiceUrl) {
        this.restTemplate = new RestTemplate();
        this.identityServiceUrl = identityServiceUrl;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Missing or invalid Authorization header\"}");
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authHeader);
            ResponseEntity<Map> result = restTemplate.exchange(
                    identityServiceUrl + "/identity/api/auth/validate",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            Map body = result.getBody();
            request.setAttribute("userId", body.get("userId"));
            request.setAttribute("email", body.get("email"));
            request.setAttribute("role", body.get("role"));

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Invalid token\"}");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/actuator/health");
    }
}


config/SecurityConfig.java

package com.portal.employer.rest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}


employer-jms: com.portal.employer.jms

listener/JmsEmployerMessageReceiver.java

package com.portal.employer.jms.listener;

import com.portal.employer.app.service.EmployerProfileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("jms")
public class JmsEmployerMessageReceiver {

    @Autowired
    private EmployerProfileService employerProfileService;

    @JmsListener(destination = "${queue.user.registered}")
    public void receiveUserRegistered(
            @Header("userId") String userId,
            @Header("email") String email,
            @Header("role") String role) {
        if ("ROLE_EMPLOYER".equals(role)) {
            employerProfileService.createProfile(UUID.fromString(userId), null, null);
        }
    }
}


sender/JmsEmployerMessageSender.java

package com.portal.employer.jms.sender;

import com.portal.employer.app.messaging.EmployerMessageSender;
import jakarta.jms.TextMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("employerMessageSender")
@Profile("jms")
public class JmsEmployerMessageSender implements EmployerMessageSender {

    private final JmsTemplate jmsTemplate;

    @Value("${queue.company.created}")
    private String companyCreatedQueue;

    @Value("${queue.company.updated}")
    private String companyUpdatedQueue;

    public JmsEmployerMessageSender(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    @Override
    public void sendCompanyCreated(String messageBody, Map<String, String> messageHeaders) {
        sendMessage(companyCreatedQueue, messageBody, messageHeaders);
    }

    @Override
    public void sendCompanyUpdated(String messageBody, Map<String, String> messageHeaders) {
        sendMessage(companyUpdatedQueue, messageBody, messageHeaders);
    }

    private void sendMessage(String queue, String messageBody, Map<String, String> messageHeaders) {
        jmsTemplate.send(queue, session -> {
            TextMessage message = session.createTextMessage(messageBody);
            for (Map.Entry<String, String> entry : messageHeaders.entrySet()) {
                message.setStringProperty(entry.getKey(), entry.getValue());
            }
            return message;
        });
    }
}


employer-sqs: com.portal.employer.sqs

listener/SqsEmployerMessageReceiver.java

package com.portal.employer.sqs.listener;

import com.portal.employer.app.service.EmployerProfileService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("sqs")
public class SqsEmployerMessageReceiver {

    @Autowired
    private EmployerProfileService employerProfileService;

    @SqsListener(value = "#{'${queue.user.registered}'.contains('arn:') ? '${queue.user.registered}'.substring('${queue.user.registered}'.lastIndexOf(':') + 1) : '${queue.user.registered}'}")
    public void receiveUserRegistered(
            @Header("userId") String userId,
            @Header("email") String email,
            @Header("role") String role) {
        if ("ROLE_EMPLOYER".equals(role)) {
            employerProfileService.createProfile(UUID.fromString(userId), null, null);
        }
    }
}


sender/SqsEmployerMessageSender.java

package com.portal.employer.sqs.sender;

import com.portal.employer.app.messaging.EmployerMessageSender;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("employerMessageSender")
@Profile("sqs")
public class SqsEmployerMessageSender implements EmployerMessageSender {

    private final SqsTemplate sqsTemplate;

    @Value("${queue.company.created}")
    private String companyCreatedQueueArn;

    @Value("${queue.company.updated}")
    private String companyUpdatedQueueArn;

    public SqsEmployerMessageSender(SqsTemplate sqsTemplate) {
        this.sqsTemplate = sqsTemplate;
    }

    @Override
    public void sendCompanyCreated(String messageBody, Map<String, String> messageHeaders) {
        sendMessage(companyCreatedQueueArn, messageBody, messageHeaders);
    }

    @Override
    public void sendCompanyUpdated(String messageBody, Map<String, String> messageHeaders) {
        sendMessage(companyUpdatedQueueArn, messageBody, messageHeaders);
    }

    private void sendMessage(String queueArn, String messageBody, Map<String, String> messageHeaders) {
        String queueName = queueArn.contains(":") ? queueArn.substring(queueArn.lastIndexOf(":") + 1) : queueArn;
        sqsTemplate.send(to -> to.queue(queueName).payload(messageBody).headers(new java.util.HashMap<>(messageHeaders)));
    }
}


employer-start: com.portal.employer

EmployerServiceApplication.java

package com.portal.employer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.portal.employer")
public class EmployerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EmployerServiceApplication.class, args);
    }
}


PHẦN 6: JOB SERVICE (port 8083)

portal-job-service/Dockerfile

FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY job-domain/pom.xml job-domain/
COPY job-app/pom.xml job-app/
COPY job-rest/pom.xml job-rest/
COPY job-jms/pom.xml job-jms/
COPY job-sqs/pom.xml job-sqs/
COPY job-start/pom.xml job-start/
RUN mvn dependency:go-offline -B
COPY . .
RUN mvn package -DskipTests -B -pl job-start -am

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/job-start/target/*.jar app.jar
EXPOSE 8083
ENTRYPOINT ["java", "-jar", "app.jar"]


portal-job-service/pom.xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.portal.job</groupId>
    <artifactId>job-service</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>
    <name>Portal Job Service</name>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>

    <modules>
        <module>job-domain</module>
        <module>job-app</module>
        <module>job-rest</module>
        <module>job-jms</module>
        <module>job-sqs</module>
        <module>job-start</module>
    </modules>

    <properties>
        <java.version>17</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <spring-cloud-aws.version>3.1.1</spring-cloud-aws.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>com.portal.job</groupId>
                <artifactId>job-domain</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.portal.job</groupId>
                <artifactId>job-app</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.portal.job</groupId>
                <artifactId>job-rest</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.portal.job</groupId>
                <artifactId>job-jms</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>com.portal.job</groupId>
                <artifactId>job-sqs</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>io.awspring.cloud</groupId>
                <artifactId>spring-cloud-aws-dependencies</artifactId>
                <version>${spring-cloud-aws.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>


job-domain/pom.xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>com.portal.job</groupId>
        <artifactId>job-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>job-domain</artifactId>
</project>


job-app/pom.xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>com.portal.job</groupId>
        <artifactId>job-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>job-app</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.portal.job</groupId>
            <artifactId>job-domain</artifactId>
        </dependency>
    </dependencies>
</project>


job-rest/pom.xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>com.portal.job</groupId>
        <artifactId>job-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>job-rest</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.portal.job</groupId>
            <artifactId>job-app</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
    </dependencies>
</project>


job-jms/pom.xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>com.portal.job</groupId>
        <artifactId>job-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>job-jms</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.portal.job</groupId>
            <artifactId>job-app</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-activemq</artifactId>
        </dependency>
    </dependencies>
</project>


job-sqs/pom.xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>com.portal.job</groupId>
        <artifactId>job-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>job-sqs</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.portal.job</groupId>
            <artifactId>job-app</artifactId>
        </dependency>
        <dependency>
            <groupId>io.awspring.cloud</groupId>
            <artifactId>spring-cloud-aws-starter</artifactId>
        </dependency>
        <dependency>
            <groupId>io.awspring.cloud</groupId>
            <artifactId>spring-cloud-aws-starter-sqs</artifactId>
        </dependency>
    </dependencies>
</project>


job-start/pom.xml

<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <parent>
        <groupId>com.portal.job</groupId>
        <artifactId>job-service</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>
    <modelVersion>4.0.0</modelVersion>
    <artifactId>job-start</artifactId>
    <dependencies>
        <dependency>
            <groupId>com.portal.job</groupId>
            <artifactId>job-rest</artifactId>
        </dependency>
        <dependency>
            <groupId>com.portal.job</groupId>
            <artifactId>job-jms</artifactId>
        </dependency>
        <dependency>
            <groupId>com.portal.job</groupId>
            <artifactId>job-sqs</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>


job-start/src/main/resources/application.properties

spring.application.name=job-service
server.port=${SERVER_PORT:8083}
server.servlet.context-path=/jobservice
spring.profiles.include=jms
spring.autoconfigure.exclude=io.awspring.cloud.autoconfigure.sqs.SqsAutoConfiguration

spring.datasource.url=${SPRING_DATASOURCE_URL:jdbc:postgresql://localhost:5432/job_service_db}
spring.datasource.username=${DB_USER:postgres}
spring.datasource.password=${DB_PASSWORD:postgres}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.datasource.hikari.maximum-pool-size=5
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.jdbc.lob.non_contextual_creation=true


job-start/src/main/resources/application-jms.properties

spring.activemq.broker-url=tcp://localhost:61616
spring.activemq.user=admin
spring.activemq.password=admin

queue.job.applied=portal.job.applied
queue.company.updated=portal.company.updated
queue.application.status.out=portal.application.status


job-start/src/main/resources/application-sqs.properties

spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jms.activemq.ActiveMQAutoConfiguration

queue.job.applied=${AWS_JOB_APPLIED_QUEUE:CHANGEIT}
queue.company.updated=${AWS_COMPANY_UPDATED_QUEUE:CHANGEIT}
queue.application.status.out=${AWS_APPLICATION_STATUS_QUEUE:CHANGEIT}


Job Service - Java Source Files

job-domain: com.portal.job.domain

Category.java

package com.portal.job.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String name;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}


CategoryRepository.java

package com.portal.job.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CategoryRepository extends JpaRepository<Category, UUID> {
}


Job.java

package com.portal.job.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "job")
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "company_id", nullable = false)
    private UUID companyId;

    @Column(name = "company_name")
    private String companyName;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "salary_min")
    private BigDecimal salaryMin;

    @Column(name = "salary_max")
    private BigDecimal salaryMax;

    private String location;

    @Column(nullable = false)
    private String status = "ACTIVE";

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getCompanyId() { return companyId; }
    public void setCompanyId(UUID companyId) { this.companyId = companyId; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getSalaryMin() { return salaryMin; }
    public void setSalaryMin(BigDecimal salaryMin) { this.salaryMin = salaryMin; }
    public BigDecimal getSalaryMax() { return salaryMax; }
    public void setSalaryMax(BigDecimal salaryMax) { this.salaryMax = salaryMax; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}


JobRepository.java

package com.portal.job.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    List<Job> findByCompanyId(UUID companyId);

    List<Job> findByStatus(String status);

    @Modifying
    @Query("UPDATE Job j SET j.companyName = :newName WHERE j.companyId = :companyId")
    int updateCompanyName(@Param("companyId") UUID companyId, @Param("newName") String newName);
}


JobApplication.java

package com.portal.job.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "job_application")
public class JobApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "candidate_name")
    private String candidateName;

    @Column(name = "resume_id")
    private UUID resumeId;

    @Column(nullable = false)
    private String status = "PENDING";

    @Column(name = "applied_at", nullable = false)
    private LocalDateTime appliedAt = LocalDateTime.now();

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Job getJob() { return job; }
    public void setJob(Job job) { this.job = job; }
    public UUID getCandidateId() { return candidateId; }
    public void setCandidateId(UUID candidateId) { this.candidateId = candidateId; }
    public String getCandidateName() { return candidateName; }
    public void setCandidateName(String candidateName) { this.candidateName = candidateName; }
    public UUID getResumeId() { return resumeId; }
    public void setResumeId(UUID resumeId) { this.resumeId = resumeId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
}


JobApplicationRepository.java

package com.portal.job.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {

    List<JobApplication> findByJobId(UUID jobId);

    List<JobApplication> findByCandidateId(UUID candidateId);
}


job-app: com.portal.job.app

messaging/JobMessageSender.java

package com.portal.job.app.messaging;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public interface JobMessageSender {

    String GROUP_ID_HEADER = "message-group-id";
    String DEDUPLICATION_ID_HEADER = "message-deduplication-id";

    void sendApplicationStatusChanged(String body, Map<String, String> headers);

    default Map<String, String> getDefaultMessageHeaders(String groupId) {
        Map<String, String> headers = new HashMap<>();
        headers.put(GROUP_ID_HEADER, groupId);
        headers.put(DEDUPLICATION_ID_HEADER, UUID.randomUUID().toString());
        return headers;
    }
}


messaging/DefaultJobMessageSender.java

package com.portal.job.app.messaging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Map;

public class DefaultJobMessageSender implements JobMessageSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultJobMessageSender.class);

    @Override
    public void sendApplicationStatusChanged(String body, Map<String, String> headers) {
        LOGGER.warn("Message is not sent... Body:{}, Headers:{}", body, headers);
    }
}


messaging/JobMessageSenderConfigurer.java

package com.portal.job.app.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JobMessageSenderConfigurer {

    @Bean
    @ConditionalOnMissingBean(JobMessageSender.class)
    public JobMessageSender jobMessageSender() {
        return new DefaultJobMessageSender();
    }
}


service/CompanySyncService.java

package com.portal.job.app.service;

import com.portal.job.domain.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

@Service
public class CompanySyncService {

    @Autowired
    private JobRepository jobRepository;

    @Transactional
    public void syncCompanyName(UUID companyId, String newName) {
        jobRepository.updateCompanyName(companyId, newName);
    }
}


service/JobApplicationService.java

package com.portal.job.app.service;

import com.portal.job.app.messaging.JobMessageSender;
import com.portal.job.domain.Job;
import com.portal.job.domain.JobApplication;
import com.portal.job.domain.JobApplicationRepository;
import com.portal.job.domain.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.UUID;

@Service
public class JobApplicationService {

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private JobApplicationRepository jobApplicationRepository;

    @Autowired
    private JobMessageSender jobMessageSender;

    @Transactional
    public JobApplication createApplicationFromEvent(UUID jobId, UUID candidateId, String candidateName, UUID resumeId) {
        Job job = jobRepository.findById(jobId).orElseThrow();
        JobApplication application = new JobApplication();
        application.setJob(job);
        application.setCandidateId(candidateId);
        application.setCandidateName(candidateName);
        application.setResumeId(resumeId);
        return jobApplicationRepository.save(application);
    }

    @Transactional
    public JobApplication updateApplicationStatus(UUID applicationId, String status) {
        JobApplication application = jobApplicationRepository.findById(applicationId).orElseThrow();
        application.setStatus(status);
        application = jobApplicationRepository.save(application);

        Map<String, String> headers = jobMessageSender.getDefaultMessageHeaders(applicationId.toString());
        jobMessageSender.sendApplicationStatusChanged(
                "{\"applicationId\":\"" + applicationId + "\",\"status\":\"" + status + "\"}",
                headers
        );
        return application;
    }
}


job-rest: com.portal.job.rest

JobController.java

package com.portal.job.rest;

import com.portal.job.domain.Job;
import com.portal.job.domain.JobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    @Autowired
    private JobRepository jobRepository;

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
}


JobApplicationController.java

package com.portal.job.rest;

import com.portal.job.app.service.JobApplicationService;
import com.portal.job.domain.JobApplication;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import java.util.UUID;

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


config/JwtAuthFilter.java

package com.portal.job.rest.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final RestTemplate restTemplate;
    private final String identityServiceUrl;

    public JwtAuthFilter(@Value("${identity-service.url:http://identity-service:8080}") String identityServiceUrl) {
        this.restTemplate = new RestTemplate();
        this.identityServiceUrl = identityServiceUrl;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Missing or invalid Authorization header\"}");
            return;
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", authHeader);
            ResponseEntity<Map> result = restTemplate.exchange(
                    identityServiceUrl + "/identity/api/auth/validate",
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class
            );

            Map body = result.getBody();
            request.setAttribute("userId", body.get("userId"));
            request.setAttribute("email", body.get("email"));
            request.setAttribute("role", body.get("role"));

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\":\"Invalid token\"}");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.contains("/actuator/health");
    }
}


config/SecurityConfig.java

package com.portal.job.rest.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}


job-jms: com.portal.job.jms

listener/JmsJobMessageReceiver.java

package com.portal.job.jms.listener;

import com.portal.job.app.service.CompanySyncService;
import com.portal.job.app.service.JobApplicationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
@Profile("jms")
public class JmsJobMessageReceiver {

    @Autowired
    private JobApplicationService jobApplicationService;

    @Autowired
    private CompanySyncService companySyncService;

    @JmsListener(destination = "${queue.job.applied}")
    public void receiveJobApplied(
            @Header("jobId") String jobId,
            @Header("candidateId") String candidateId,
            @Header("candidateName") String candidateName,
            @Header("resumeId") String resumeId) {
        jobApplicationService.createApplicationFromEvent(
                UUID.fromString(jobId), UUID.fromString(candidateId), candidateName, UUID.fromString(resumeId));
    }

    @JmsListener(destination = "${queue.company.updated}")
    public void receiveCompanyUpdated(
            @Header("companyId") String companyId,
            @Header("companyName") String companyName) {
        companySyncService.syncCompanyName(UUID.fromString(companyId), companyName);
    }
}


sender/JmsJobMessageSender.java

package com.portal.job.jms.sender;

import com.portal.job.app.messaging.JobMessageSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;
import java.util.Map;

@Component("jobMessageSender")
@Profile("jms")
public class JmsJobMessageSender implements JobMessageSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(JmsJobMessageSender.class);

    @Autowired
    private JmsTemplate jmsTemplate;

    @Value("${queue.application.status.out}")
    private String applicationStatusQueue;

    @Override
    public void sendApplicationStatusChanged(String body, Map<String, String> headers) {
        LOGGER.info("Sending application status changed message");
        jmsTemplate.send(applicationStatusQueue, session -> {
            var message = session.createTextMessage(body);
            for (var entry : headers.entrySet()) {
                message.setStringProperty(entry.getKey(), entry.getValue());
            }
            return message;
        });
    }
}


job-sqs: com.portal.job.sqs

listener/SqsJobMessageReceiver.java

package com.portal.job.sqs.listener;

import com.portal.job.app.service.CompanySyncService;
import com.portal.job.app.service.JobApplicationService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
@Profile("sqs")
public class SqsJobMessageReceiver {

    @Autowired
    private JobApplicationService jobApplicationService;

    @Autowired
    private CompanySyncService companySyncService;

    @SqsListener(value = "${queue.job.applied}")
    public void receiveJobApplied(
            @Header("jobId") String jobId,
            @Header("candidateId") String candidateId,
            @Header("candidateName") String candidateName,
            @Header("resumeId") String resumeId) {
        jobApplicationService.createApplicationFromEvent(
                UUID.fromString(jobId), UUID.fromString(candidateId), candidateName, UUID.fromString(resumeId));
    }

    @SqsListener(value = "${queue.company.updated}")
    public void receiveCompanyUpdated(
            @Header("companyId") String companyId,
            @Header("companyName") String companyName) {
        companySyncService.syncCompanyName(UUID.fromString(companyId), companyName);
    }
}


sender/SqsJobMessageSender.java

package com.portal.job.sqs.sender;

import com.portal.job.app.messaging.JobMessageSender;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Component("jobMessageSender")
@Profile("sqs")
public class SqsJobMessageSender implements JobMessageSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqsJobMessageSender.class);

    @Autowired
    private SqsTemplate sqsTemplate;

    @Value("${queue.application.status.out}")
    private String applicationStatusQueue;

    @Override
    public void sendApplicationStatusChanged(String body, Map<String, String> headers) {
        LOGGER.info("Sending application status changed message via SQS");
        sqsTemplate.send(applicationStatusQueue, MessageBuilder.withPayload(body).copyHeaders(headers).build());
    }

    @Override
    public Map<String, String> getDefaultMessageHeaders(String groupId) {
        Map<String, String> headers = new HashMap<>();
        headers.put(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_GROUP_ID_HEADER, groupId);
        headers.put(SqsHeaders.MessageSystemAttributes.SQS_MESSAGE_DEDUPLICATION_ID_HEADER, UUID.randomUUID().toString());
        return headers;
    }
}


job-start: com.portal.job

JobServiceApplication.java

package com.portal.job;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {"com.portal.job"})
@EntityScan(basePackages = "com.portal.job")
@EnableJpaRepositories(basePackages = "com.portal.job")
public class JobServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(JobServiceApplication.class, args);
    }
}


PHẦN 7: HƯỚNG DẪN TRIỂN KHAI

Chạy local với Docker Compose

# Clone/tạo toàn bộ file theo cấu trúc ở trên
# Từ thư mục gốc Portal/:
docker-compose up --build


Sau khi chạy: - Identity Service: http://localhost:8080/identity/api/auth/login - Candidate Service: http://localhost:8081/candidate/api/candidates - Employer Service: http://localhost:8082/employer/api/companies - Job Service: http://localhost:8083/jobservice/api/jobs

Chạy trên Kubernetes

chmod +x k8s/deploy.sh
./k8s/deploy.sh


Test API

# 1. Đăng ký user
curl -X POST http://localhost:8080/identity/api/users/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","passwordHash":"password123","roleName":"ROLE_CANDIDATE"}'

# 2. Login
curl -X POST http://localhost:8080/identity/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"password123"}'

# 3. Dùng token để gọi các service khác
curl http://localhost:8083/jobservice/api/jobs \
  -H "Authorization: Bearer <token>"