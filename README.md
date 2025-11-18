# Bank Cards Management System

REST API backend application for managing bank cards with JWT authentication, role-based access control, and secure money transfers between cards.

## 🚀 Tech Stack

**Backend:** Java 17, Spring Boot 3.2, Spring Security, Spring Data JPA  
**Security:** JWT (JSON Web Tokens), BCrypt password encryption, AES card number encryption  
**Database:** PostgreSQL 15, Liquibase migrations  
**Tools:** Docker, Maven, Lombok  
**Documentation:** SpringDoc OpenAPI (Swagger UI)  
**Testing:** JUnit 5, Mockito

## ✨ Features

### Authentication & Authorization
- JWT-based authentication
- Role-based access control (USER, ADMIN)
- Secure password hashing with BCrypt

### User Management
- User registration and authentication
- Admin panel for user management
- Profile information management

### Card Operations
- Create new bank cards
- View cards with pagination
- Block/unblock cards
- Card number encryption (AES)
- Masked card display (`**** **** **** 1234`)
- Automatic expiry date validation

### Money Transfers
- Transfer money between own cards
- Transaction history with pagination
- Atomic transactions (rollback on failure)
- Balance validation
- Card status validation

### Security Features
- Encrypted card numbers in database
- Role-based endpoint protection
- Authorization checks (users can only access own cards)
- Global exception handling

## 📋 Prerequisites

- **Java 17+** ([Download](https://adoptium.net/))
- **Docker Desktop** ([Download](https://www.docker.com/products/docker-desktop/))
- **Maven 3.8+** (included in project wrapper)

## 🎯 Quick Start

### 1. Clone Repository
```bash
git clone <repository-url>
cd bank_rest
```

### 2. Start PostgreSQL Database
```bash
docker-compose up -d
```

This will start PostgreSQL on `localhost:5432` with:
- Database: `bankcards_db`
- User: `bankuser`
- Password: `bankpass123`

### 3. Run Application
```bash
./mvnw spring-boot:run
```

Or build and run JAR:
```bash
./mvnw clean package
java -jar target/bankcards-1.0.0.jar
```

Application will start on `http://localhost:8080`

### 4. Access Swagger UI
Open browser: `http://localhost:8080/swagger-ui.html`

## 🔐 Test Credentials

Default users are created automatically via Liquibase seed data:

**Admin Account:**
```
Username: admin
Password: password
```

**User Account:**
```
Username: testuser
Password: password
```

## 📡 API Quick Start

### 1. Register New User
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "newuser",
    "email": "newuser@example.com",
    "password": "password123",
    "firstName": "John",
    "lastName": "Doe"
  }'
```

### 2. Login
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "testuser",
    "password": "password"
  }'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "user": {
    "id": 2,
    "username": "testuser",
    "email": "user@bankcards.com"
  }
}
```

### 3. Use Token in Requests
```bash
curl -X GET http://localhost:8080/api/cards \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

For complete API documentation with all endpoints, see Swagger UI.

## 🧪 Running Tests

### Run All Tests
```bash
./mvnw test
```

### Run Specific Test Class
```bash
./mvnw test -Dtest=UserServiceTest
```

### Test Coverage
- **UserService:** 8 tests (registration, CRUD, validation)
- **CardService:** 10 tests (card operations, encryption, authorization)
- **TransferService:** 7 tests (transfers, validation, transaction history)
- **Total:** 25 unit tests with ~70% coverage of critical business logic

## 📂 Project Structure
```
bank_rest/
├── src/
│   ├── main/
│   │   ├── java/com/example/bankcards/
│   │   │   ├── config/          # Security, JWT, App configuration
│   │   │   ├── controller/      # REST API endpoints
│   │   │   ├── dto/             # Request/Response DTOs
│   │   │   ├── entity/          # JPA entities
│   │   │   ├── exception/       # Custom exceptions & global handler
│   │   │   ├── repository/      # Spring Data JPA repositories
│   │   │   ├── security/        # JWT provider, filters, UserDetails
│   │   │   ├── service/         # Business logic
│   │   │   └── util/            # Encryption utilities
│   │   └── resources/
│   │       ├── application.yml           # Application configuration
│   │       └── db/migration/changelog/   # Liquibase migrations
│   └── test/
│       └── java/com/example/bankcards/
│           └── service/         # Service layer unit tests
├── docker-compose.yml           # PostgreSQL setup
├── pom.xml                      # Maven dependencies
└── README.md
```

## 🗄️ Database Schema

### Tables
- **users** - User accounts with roles
- **cards** - Bank cards (encrypted card numbers)
- **transactions** - Money transfer history

### Migrations
Database schema is managed by Liquibase. Migrations run automatically on application startup.

Location: `src/main/resources/db/migration/changelog/changes/`

## 🔒 Security Configuration

### JWT Settings
Configure in `application.yml`:
```yaml
jwt:
  secret: ${JWT_SECRET:your-secret-key}
  expiration: 86400000  # 24 hours
```

### Card Encryption
```yaml
encryption:
  password: ${ENCRYPTION_PASSWORD:your-encryption-password}
  salt: ${ENCRYPTION_SALT:your-salt}
```

**Production:** Use environment variables instead of default values!

## 🛠️ Development

### Database Console
```bash
# Connect to PostgreSQL
docker exec -it bank-postgres psql -U bankuser -d bankcards_db

# View tables
\dt

# Query users
SELECT * FROM users;
```

### Reset Database
```bash
docker-compose down -v
docker-compose up -d
./mvnw spring-boot:run  # Liquibase will recreate schema
```

## 📊 API Endpoints Summary

### Public Endpoints
- `POST /api/auth/register` - Register new user
- `POST /api/auth/login` - Login and get JWT token

### User Endpoints (Requires Authentication)
- `GET /api/cards` - Get own cards (paginated)
- `GET /api/cards/{id}` - Get specific card
- `POST /api/cards` - Create new card
- `PUT /api/cards/{id}/block` - Block own card
- `POST /api/transfers` - Transfer money between own cards
- `GET /api/transfers/history` - Get transaction history

### Admin Endpoints (Requires ADMIN Role)
- `GET /api/cards/all` - Get all cards (all users)
- `PUT /api/cards/{id}/activate` - Activate any card
- `DELETE /api/cards/{id}` - Delete any card
- `GET /api/users` - Get all users
- `GET /api/users/{id}` - Get user by ID
- `DELETE /api/users/{id}` - Delete user

## 🎓 Architecture Highlights

### Layered Architecture
- **Controller Layer** - REST endpoints, request validation
- **Service Layer** - Business logic, transactions
- **Repository Layer** - Data access (Spring Data JPA)
- **Security Layer** - JWT, authentication, authorization

### Design Patterns
- **DTO Pattern** - Separation of API contracts and entities
- **Repository Pattern** - Data access abstraction
- **Service Pattern** - Business logic encapsulation
- **Builder Pattern** - Entity construction (Lombok @Builder)

### Best Practices
- Records for immutable DTOs
- Bean Validation (Jakarta)
- Global exception handling
- Transaction management (@Transactional)
- Role-based method security (@PreAuthorize)

## 📝 License

This project is developed as a technical assessment for a Junior Java Developer position.

---

**Author:** Anton Dernovskiy  
**Java Version:** 17  
**Spring Boot Version:** 3.2.0


