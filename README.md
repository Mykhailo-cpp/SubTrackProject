# SubTrack

A RESTful Web Service for tracking paid subscriptions (Spotify, Netflix, YouTube Premium, etc.).

Built with **Java 17**, **Spring Boot 3.5.14**, **PostgreSQL**, **JWT authentication**, and **Swagger/OpenAPI** documentation.

---

## Tech Stack

**Backend**
- Java 17
- Spring Boot 3.5.14
- Spring Data JPA + PostgreSQL 16
- Spring Security + JWT (jjwt 0.12.5)
- Spring HATEOAS
- Spring Cache
- Springdoc OpenAPI 2.8.9 (Swagger UI)
- Liquibase (schema migrations)
- MapStruct 1.6.3 (DTO mapping)
- Lombok
- JavaMail (Gmail SMTP)

**Testing**
- JUnit 5 + Mockito
- Cucumber 7.15.0 (acceptance tests)
- H2 (in-memory test database)

**Frontend** (static pages)
- HTML, CSS, JavaScript
- Bootstrap
- Chart.js

---

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- Docker (recommended) **or** PostgreSQL 16+ installed locally

### 1. Configure environment variables

Copy `.env` and fill in your values:

```dotenv
# JWT
JWT_SECRET=your_256_bit_secret

# Database
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password

# ExchangeRate API — get a free key at https://www.exchangerate-api.com
EXCHANGERATE_API_KEY=your_exchangerate_api_key

# Gmail SMTP (use an App Password, not your account password)
MAIL_USERNAME=your-gmail@gmail.com
MAIL_PASSWORD=your_gmail_app_password
```

### 2. Start the database

**With Docker (recommended):**

```bash
docker-compose up -d
```

This starts PostgreSQL 16 on port **5433** and persists data in a named volume.

**Without Docker:**

Create the database manually:

```sql
CREATE DATABASE subtrack;
```

Then update `spring.datasource.url` in `src/main/resources/application.properties` if needed.

### 3. Run the application

```bash
mvn spring-boot:run
```

Liquibase runs automatically on startup and applies all pending migrations.

### 4. Access Swagger UI

```
http://localhost:8080/swagger-ui.html
```

To call protected endpoints, first authenticate via `POST /api/auth/login`, copy the returned JWT, and click **Authorize** in Swagger UI.

---

## API Endpoints

All endpoints except `/api/auth/**` require a `Bearer <token>` JWT header.

### Authentication

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Register a new user |
| POST | `/api/auth/login` | Login and receive a JWT token |
| POST | `/api/auth/forgot-password` | Request a password reset email |
| POST | `/api/auth/reset-password` | Reset password using the emailed token |

### Categories

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/categories` | List all categories |
| GET | `/api/categories/{id}` | Get a category by ID |
| POST | `/api/categories` | Create a category |
| PUT | `/api/categories/{id}` | Update a category |
| DELETE | `/api/categories/{id}` | Delete a category |

### Subscriptions

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/subscriptions` | List all subscriptions for the current user |
| GET | `/api/subscriptions/{id}` | Get a subscription by ID |
| POST | `/api/subscriptions` | Create a subscription |
| PUT | `/api/subscriptions/{id}` | Update a subscription |
| DELETE | `/api/subscriptions/{id}` | Delete a subscription |
| GET | `/api/subscriptions/summary` | Monthly spending summary across all subscriptions |
| GET | `/api/subscriptions/{id}/convert?targetCurrency=EUR` | Convert a subscription's price to another currency |
| GET | `/api/subscriptions/test-reminder` | Manually trigger renewal reminder emails (dev/test) |

Subscription responses include HATEOAS `_links` (`self` and `subscriptions`).

---

## Running Tests

```bash
# Unit tests + Cucumber acceptance tests (all in one command)
mvn test
```

Tests run against an **H2 in-memory database** (PostgreSQL-compatibility mode) — no external database required.

**Test coverage includes:**
- `AuthServiceImplTest` — registration, login, password reset flow
- `CategoryServiceTest` — category CRUD
- `SubscriptionServiceTest` — subscription lifecycle
- `CurrencyServiceTest` — exchange rate conversion
- `SpendingServiceTest` — monthly spending summary logic
- `RenewalReminderServiceImplTest` — scheduled reminder emails
- `EmailServiceImplTest` — email sending
- `UserDetailsServiceImplTest` — Spring Security user loading
- Cucumber scenarios: `auth.feature`, `categories.feature`, `subscriptions.feature`, `spending.feature`

---

## Project Structure

```
src/main/java/lt/viko/eif/subtrack/
├── config/          # Spring configuration (Security, OpenAPI, AppConfig)
├── controller/      # REST controllers (Auth, Category, Subscription)
├── dto/             # Request / Response DTOs
├── entity/          # JPA entities (User, Subscription, Category, BillingCycle, PasswordResetToken)
├── exception/       # Custom exceptions + GlobalExceptionHandler
├── mapper/          # MapStruct mappers (Category, Subscription, Spending)
├── repository/      # Spring Data JPA repositories
├── scheduler/       # RenewalReminderScheduler (daily cron job)
├── security/        # JWT utilities, filters, and CurrentUserProvider
└── service/         # Business logic interfaces + implementations

src/main/resources/
├── application.properties
├── db/changelog/    # Liquibase changesets (001–005)
└── static/          # Frontend (HTML, CSS, JS — Bootstrap + Chart.js)

src/test/
├── java/.../service/       # JUnit 5 / Mockito unit tests
├── java/.../cucumber/      # Cucumber acceptance test runner + step definitions
└── resources/features/     # Gherkin feature files
```

---

## Database Migrations

Schema is managed by **Liquibase**. Changesets live in `src/main/resources/db/changelog/changes/`:

| File | Description |
|------|-------------|
| `001-create-users-table.xml` | Users table |
| `002-create-categories-table.xml` | Categories table |
| `003-create-subscriptions-table.xml` | Subscriptions table |
| `004-add-renewal-reminder-enabled.xml` | `renewal_reminder_enabled` column |
| `005-create-password-reset-tokens.xml` | Password reset tokens table |

---

## Branching Strategy

- `main` — stable, production-ready code
- `feature/xyz` — individual feature branches merged into `develop` via PR

---

## Team

| Name | Role |
|------|------|
| Mykhailo Osadchuk | Backend Developer + Analyst |
| Karyna Yatsenko | Full-stack Developer + QA |
