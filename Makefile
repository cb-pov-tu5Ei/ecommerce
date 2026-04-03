.PHONY: help clean build test test-selenium test-selenium-headed test-selenium-debug run kill start stop logs package install

# Default port from application.properties
PORT ?= 8080

# Colors for terminal output
BLUE := \033[0;34m
GREEN := \033[0;32m
RED := \033[0;31m
NC := \033[0m # No Color

help: ## Show this help message
	@echo '$(BLUE)Available targets:$(NC)'
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | sort | awk 'BEGIN {FS = ":.*?## "}; {printf "  $(GREEN)%-15s$(NC) %s\n", $$1, $$2}'

clean: ## Clean build artifacts
	@echo "$(BLUE)Cleaning build artifacts...$(NC)"
	./mvnw clean

build: ## Build the project
	@echo "$(BLUE)Building project...$(NC)"
	./mvnw compile

package: ## Package the application as JAR
	@echo "$(BLUE)Packaging application...$(NC)"
	./mvnw package -DskipTests

install: ## Install dependencies
	@echo "$(BLUE)Installing dependencies...$(NC)"
	./mvnw install -DskipTests

test: ## Run tests
	@echo "$(BLUE)Running tests...$(NC)"
	./mvnw test

test-fast: ## Run tests with delays disabled (faster local feedback)
	@echo "$(BLUE)Running tests (no delays)...$(NC)"
	./mvnw test -Dtest.delays.enabled=false

test-verbose: ## Run tests with verbose output
	@echo "$(BLUE)Running tests (verbose)...$(NC)"
	./mvnw test -X

test-selenium: ## Run Selenium UI tests (headless)
	@echo "$(BLUE)Running Selenium tests (headless)...$(NC)"
	./mvnw test -P selenium

test-selenium-headed: ## Run Selenium UI tests with visible browser
	@echo "$(BLUE)Running Selenium tests (visible browser)...$(NC)"
	./mvnw test -P selenium -Dselenium.headless=false

test-selenium-debug: ## Run a single Selenium test with visible browser (usage: make test-selenium-debug TEST=CheckoutFlowSeleniumTest#fullCheckoutFlow)
	@if [ -z "$(TEST)" ]; then \
		echo "$(RED)Usage: make test-selenium-debug TEST=ClassName#methodName$(NC)"; \
		echo "$(RED)Example: make test-selenium-debug TEST=CheckoutFlowSeleniumTest#fullCheckoutFlow$(NC)"; \
		exit 1; \
	fi
	@echo "$(BLUE)Running Selenium test: $(TEST) (visible browser)...$(NC)"
	./mvnw test -P selenium -Dselenium.headless=false -Dtest="$(TEST)"

run: ## Run the application
	@echo "$(BLUE)Starting application on port $(PORT)...$(NC)"
	./mvnw spring-boot:run

start: ## Start the application in the background
	@echo "$(BLUE)Starting application in background on port $(PORT)...$(NC)"
	@nohup ./mvnw spring-boot:run > app.log 2>&1 & echo $$! > .app.pid
	@echo "$(GREEN)Application started with PID $$(cat .app.pid)$(NC)"
	@echo "Logs are being written to app.log"

stop: kill ## Alias for kill

kill: ## Kill any process running on port 8080
	@echo "$(BLUE)Killing processes on port $(PORT)...$(NC)"
	@PIDS=$$(lsof -ti:$(PORT)); \
	if [ -z "$$PIDS" ]; then \
		echo "$(GREEN)No process found running on port $(PORT)$(NC)"; \
	else \
		echo "$(RED)Killing process(es): $$PIDS$(NC)"; \
		kill -9 $$PIDS; \
		echo "$(GREEN)Process(es) killed successfully$(NC)"; \
	fi
	@if [ -f .app.pid ]; then rm .app.pid; fi

logs: ## Tail application logs (if running in background)
	@if [ -f app.log ]; then \
		tail -f app.log; \
	else \
		echo "$(RED)No log file found. Start the app with 'make start' to enable logging.$(NC)"; \
	fi

status: ## Check if the application is running
	@PIDS=$$(lsof -ti:$(PORT)); \
	if [ -z "$$PIDS" ]; then \
		echo "$(RED)No process found running on port $(PORT)$(NC)"; \
	else \
		echo "$(GREEN)Process(es) running on port $(PORT): $$PIDS$(NC)"; \
		lsof -i:$(PORT); \
	fi

dev: kill run ## Kill any existing server and start fresh

rebuild: clean package ## Clean and rebuild the project

restart: kill start ## Restart the application (background mode)

h2-console: ## Open H2 console URL
	@echo "$(BLUE)H2 Console available at:$(NC) http://localhost:$(PORT)/h2-console"
	@echo "$(BLUE)JDBC URL:$(NC) jdbc:h2:mem:ecommerce"
	@echo "$(BLUE)Username:$(NC) sa"
	@echo "$(BLUE)Password:$(NC) (empty)"

open: ## Open the application in browser
	@echo "$(BLUE)Opening application...$(NC)"
	@open http://localhost:$(PORT) || xdg-open http://localhost:$(PORT) || echo "$(RED)Could not open browser. Visit http://localhost:$(PORT)$(NC)"
