\# RentalHub



A full-stack property rental management platform for owners, tenants and admins.



\## Tech Stack

\- \*\*Backend:\*\* Java 21, Spring Boot 3.2, Spring Security + JWT, Spring Data JPA, MySQL

\- \*\*Frontend:\*\* Angular 17 (standalone components), Angular Material, Chart.js



\## Features

\- JWT authentication with refresh tokens and role-based access (Admin, Owner, Tenant)

\- Property management with image upload

\- Tenant assignment and move-out lifecycle

\- Rental agreements with PDF upload/download and expiry alerts

\- Monthly rent tracking with overdue detection

\- Maintenance requests with a status workflow

\- Email and in-app notifications

\- Role-specific dashboards with charts

\- Excel and PDF reports

\- Forgot/reset password and profile settings



\## Getting Started



\### Backend

1\. Create a MySQL database named `rentalhub\_db`.

2\. Copy `rentalhub-backend/src/main/resources/application-dev.yml.example`

&#x20;  to `application-dev.yml` in the same folder and fill in your credentials.

3\. Run:

&#x20;  ```bash

&#x20;  cd rentalhub-backend

&#x20;  mvn spring-boot:run

&#x20;  ```

&#x20;  The API runs at `http://localhost:8080/api`.



\### Frontend

```bash

cd rentalhub-frontend

npm install

ng serve

```

The app runs at `http://localhost:4200`.

