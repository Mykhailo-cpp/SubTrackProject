# SubTrack

A RESTful Web Service for tracking paid subscriptions (Spotify, Netflix, YouTube Premium, etc.).

Built with Java 17, Spring Boot, PostgreSQL, JWT authentication, and Swagger/OpenAPI documentation.

---

## Tech Stack

**Backend**
- Java 17
- Spring Boot 4.0.6
- Spring Data JPA + PostgreSQL
- Spring Security + JWT
- Spring HATEOAS
- Springdoc OpenAPI (Swagger UI)
- JavaMail
- JUnit 5 + Mockito
- Cucumber (acceptance tests)
- Maven

**Frontend**
- HTML, CSS, JavaScript
- Bootstrap
- Chart.js

---

## Getting Started

### Prerequisites
- Java 17+
- Maven 3.8+
- PostgreSQL 14+

### 1. Create the database

```sql
CREATE DATABASE subtrack;
```

### 2. Configure application.properties

Edit `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/subtrack
spring.datasource.username=YOUR_USERNAME
spring.datasource.password=YOUR_PASSWORD
jwt.secret=YOUR_256_BIT_SECRET
```

### 3. Run the application

```bash
mvn spring-boot:run
```

### 4. Access Swagger UI

Open your browser and go to:
```
http://localhost:8080/swagger-ui.html
```

---

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/auth/register | Register a new user |
| POST | /api/auth/login | Login and get JWT token |
| GET | /api/subscriptions | Get all subscriptions |
| POST | /api/subscriptions | Create a subscription |
| GET | /api/subscriptions/{id} | Get subscription by ID |
| PUT | /api/subscriptions/{id} | Update a subscription |
| DELETE | /api/subscriptions/{id} | Delete a subscription |
| GET | /api/categories | Get all categories |
| POST | /api/categories | Create a category |
| GET | /api/subscriptions/summary | Get monthly spending summary |

---

## Running Tests

```bash
# Unit tests
mvn test

# Generate JavaDoc
mvn javadoc:javadoc
```

---

## Project Structure

```
src/main/java/com/subtrack/
├── config/          # Spring configuration (Security, OpenAPI, Cache)
├── controller/      # REST controllers
├── dto/             # Request/Response DTOs
├── entity/          # JPA entities (User, Subscription, Category)
├── exception/       # Custom exceptions + global handler
├── repository/      # Spring Data JPA repositories
├── security/        # JWT utilities and filters
└── service/         # Business logic interfaces + implementations
```

---

## Branching Strategy

- `main` — stable, production-ready code
- `feature/xyz` — individual feature branches (merged into develop via PR)

---

## Team

| Name | Role |
|------|------|
| [Mykhailo Osadchuk] | Backend Developer + Analyst |
| [Karyna Yatsenko] | Full-stack Developer + QA |
