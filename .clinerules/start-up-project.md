# Hướng dẫn khởi động dự án

Dự án này là một nền tảng microservices tuyển dụng, sử dụng Java 17, Spring Boot 3.2.5, PostgreSQL và ActiveMQ.

## Yêu cầu tiên quyết

Đảm bảo rằng bạn đã cài đặt các công cụ sau trên hệ thống của mình:

*   **Docker và Docker Compose:** Để chạy các dịch vụ dưới dạng container.
*   **Java Development Kit (JDK) 17:** Để build các ứng dụng Spring Boot.
*   **Maven 3.x:** Để quản lý các dependencies và build dự án.

## Các bước khởi động dự án

Thực hiện theo các bước sau để khởi động và chạy tất cả các microservices trong môi trường cục bộ bằng Docker Compose:

1.  **Mở terminal và điều hướng đến thư mục `Portal/`**

    ```bash
    cd Portal/
    ```

2.  **Khởi động các dịch vụ bằng Docker Compose**

    Lệnh này sẽ build Docker images cho từng microservice (nếu chưa có), khởi tạo các cơ sở dữ liệu PostgreSQL và ActiveMQ, sau đó chạy tất cả các dịch vụ được định nghĩa trong `docker-compose.yml`.

    ```bash
    docker-compose up --build
    ```

    *   `--build`: Tùy chọn này đảm bảo rằng Docker images sẽ được build lại (hoặc build lần đầu) trước khi khởi động các container. Điều này hữu ích khi có sự thay đổi trong source code của các dịch vụ.

3.  **Kiểm tra trạng thái các dịch vụ**

    Sau khi chạy lệnh `docker-compose up`, bạn có thể kiểm tra trạng thái của các container bằng lệnh:

    ```bash
    docker-compose ps
    ```

    Đảm bảo rằng tất cả các dịch vụ đều đang ở trạng thái `Up` và `healthy` (nếu có healthcheck).

## Các cổng dịch vụ mặc định

Dưới đây là các cổng mà mỗi dịch vụ sẽ lắng nghe:

*   **Identity Service:** `http://localhost:8080/identity`
*   **Candidate Service:** `http://localhost:8081/candidate`
*   **Employer Service:** `http://localhost:8082/employer`
*   **Job Service:** `http://localhost:8083/jobservice`
*   **PostgreSQL:** `localhost:5432`
*   **ActiveMQ Console:** `http://localhost:8161` (admin/admin)

Bạn có thể truy cập các API của từng dịch vụ thông qua các cổng này hoặc thông qua Ingress nếu triển khai trên Kubernetes.


Start project

cd /home/dothetung/Projects/OPEN-CV/Portal && docker-compose down -v && docker-compose up