# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot 4.0.3 e-commerce application using Java 17, Maven, JPA/Hibernate, Spring Security, and Thymeleaf templates. Uses H2 in-memory database for development. CI/CD via CloudBees Unify.

Base package: `io.cb_demos.ecommerce`

## Build and Development Commands

### Running the application
```bash
./mvnw spring-boot:run
```
Application starts on http://localhost:8080
- H2 Console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:ecommerce`, user: `sa`, no password)

### Testing
```bash
# Run all tests (excludes Selenium tests by default)
./mvnw test

# Run a specific test class
./mvnw test -Dtest=OrderControllerTest

# Run a specific test method
./mvnw test -Dtest=OrderControllerTest#testPlaceOrder

# Run tests with detailed output
./mvnw test -X

# Run Selenium tests only (requires Chrome/ChromeDriver)
./mvnw test -P selenium

# Run specific Selenium test
./mvnw test -P selenium -Dtest=CheckoutFlowSeleniumTest
```

### Building
```bash
# Build without tests
./mvnw package -DskipTests

# Clean and build
./mvnw clean package

# Install to local Maven repository
./mvnw clean install
```

### Code Quality
```bash
# Compile only
./mvnw compile

# Validate project structure
./mvnw validate
```

## Architecture

### Layered Architecture
Standard Spring Boot MVC pattern with clear separation of concerns:

1. **Controllers** (`controller/`) - Handle HTTP requests, return Thymeleaf views or redirects
2. **Services** (`service/` + `service/impl/`) - Business logic layer, transactional operations
3. **Repositories** (`repository/`) - Spring Data JPA interfaces for database access
4. **Domain** (`domain/`) - JPA entities (Product, Order, User, Category, OrderItem, CartItem)
5. **DTOs** (`dto/`) - Data transfer objects (e.g., UserRegistrationDto)
6. **Security** (`security/`, `config/SecurityConfig.java`) - Authentication and authorization
7. **Exceptions** (`exception/`) - Custom exceptions and GlobalExceptionHandler
8. **Init** (`init/DataLoader.java`) - CommandLineRunner that seeds database with sample data

### Domain Model Relationships
- **User** (1) → (Many) **Order**
- **Order** (1) → (Many) **OrderItem** → (1) **Product**
- **Product** (Many) → (1) **Category**
- **CartItem** - Transient domain object managed by CartService (session-based)

### Security Configuration
- Public access: `/`, `/home`, `/products/**`, `/register`, `/cart/**`, static resources
- Authenticated: `/orders/**`, `/user/**`
- Admin only: `/admin/**`
- Form-based login at `/login`, logout at `/logout`
- CSRF enabled (except H2 console)

### Test users loaded by DataLoader
- Admin: `admin` / `admin123` (role: ADMIN)
- User: `testuser` / `test123` (role: USER)

## Key Implementation Patterns

### Service Layer
All services use interface + implementation pattern (e.g., `OrderService` → `OrderServiceImpl`). Services are transactional and handle business logic including validation and exception throwing.

### Repository Layer
Spring Data JPA repositories with custom query methods. Use method naming conventions or `@Query` annotations.

### Cart Management
Session-based cart using `CartService` and `CartServiceImpl`. Cart is stored in HTTP session, not persisted to database. On order creation, cart items are converted to OrderItems and cart is cleared.

### Order Processing
1. User adds items to session cart
2. At checkout, `OrderService.createOrder()` validates stock, creates Order and OrderItems
3. Stock is decremented atomically
4. Cart is cleared after successful order
5. Order gets unique `orderNumber` (format: ORD-{timestamp}-{userId})

### Entity Best Practices
- Use Lombok annotations (`@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`) except where custom logic needed
- Override `equals()` and `hashCode()` to compare by ID only for JPA entities
- Use `@CreationTimestamp` and `@UpdateTimestamp` for audit fields
- Lazy fetch for associations to avoid N+1 queries

## Testing Strategy

343 tests organized in:
- **Unit tests**: Service layer (mocked repositories), Domain objects (validation logic)
- **Controller tests**: `@WebMvcTest` with mocked services
- **Repository tests**: `@DataJpaTest` with test database
- **Integration tests**: `@SpringBootTest` for full workflows (checkout, order management, cart, registration)
- **Security tests**: Use Spring Security Test support (`@WithMockUser`, `@WithUserDetails`)
- **Selenium tests**: `@Tag("selenium")` for browser-based E2E tests (excluded from default test runs)

Test configuration: `src/test/java/io/cb_demos/ecommerce/config/TestSecurityConfig.java`

### Selenium Tests
Located in `src/test/java/io/cb_demos/ecommerce/selenium/`:
- **Base**: `BaseSeleniumTest` - Configures WebDriver (Chrome headless by default)
- **Pages**: Page Object Model classes for UI interactions
- **Tests**: E2E scenarios (auth flow, checkout, cart, product browsing, navigation, order management, user profile)

Selenium tests are excluded from default Maven runs via `<excludedGroups>selenium</excludedGroups>`. Run them explicitly with `-P selenium` profile.

### Test Delays for CI/CD Demo
Tests include artificial delays (via `TestDelayUtil`) to extend CI runtime from ~35s to ~2-3 minutes, making test execution more visible in CI/CD pipelines for demonstration purposes.

**Delay levels:**
- `smallDelay()` - 500ms for unit tests
- `mediumDelay()` - 1000ms for service/controller tests
- `largeDelay()` - 2000ms for integration tests
- `extraLargeDelay()` - 5000ms for key integration tests
- `massiveDelay()` - 10000ms for comprehensive integration tests

**Disable delays for local development:**
```bash
./mvnw test -Dtest.delays.enabled=false
```

Applied across 60+ tests in integration, service, controller, and repository layers.

## CloudBees Unify CI/CD

### Main Workflow
File: `.cloudbees/workflows/workflow.yml`
- **Triggers**: pushes and PRs to `main` branch
- **Smart Tests Integration**: Uses CloudBees Smart Tests for intelligent test selection
  - Records build and creates observation session
  - Compiles tests and filters out selenium/utility classes
  - Generates test subset targeting 50% coverage based on code changes
  - Records test results back to Smart Tests
- **Steps**: checkout → Smart Tests verify → record build → record session → compile tests → create subset → run tests → record results → publish test results → package → register artifact
- **Test reports**: `**/target/surefire-reports/*.xml`
- **Artifact registration**: JAR file registered with version info

### Nightly Selenium Workflow
File: `.cloudbees/workflows/nightly-selenium.yml`
- **Triggers**: Scheduled nightly at 2 AM UTC (`cron: 0 2 * * *`) or manual via `workflow_dispatch`
- **Steps**: checkout → install Chrome in container → run Selenium tests with `-P selenium` → publish test results
- **Purpose**: Full browser-based E2E testing separate from fast unit/integration tests

## Common Development Scenarios

### Adding a new feature
1. Create domain entity if needed (with JPA annotations)
2. Create repository interface extending `JpaRepository<Entity, ID>`
3. Create service interface and implementation with business logic
4. Create controller with request mappings
5. Add Thymeleaf templates in `src/main/resources/templates/`
6. Write tests at each layer
7. Update DataLoader if seed data needed

### Modifying security rules
Edit `SecurityConfig.java` `filterChain()` method. Use `requestMatchers()` for path patterns and combine with `.permitAll()`, `.authenticated()`, or `.hasRole()`.

### Debugging
- Application logs: DEBUG level for `io.cb_demos.ecommerce` and Spring Security
- SQL logs: Hibernate SQL DEBUG with parameter tracing
- Use H2 console for database inspection during development

### Running specific test suites
```bash
# All controller tests
./mvnw test -Dtest="*ControllerTest"

# All integration tests
./mvnw test -Dtest="*IntegrationTest"

# All service tests
./mvnw test -Dtest="*ServiceTest"

# All Selenium tests
./mvnw test -P selenium

# Specific Selenium test
./mvnw test -P selenium -Dtest="CheckoutFlowSeleniumTest"
```
