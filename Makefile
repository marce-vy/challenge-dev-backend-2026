.PHONY: help format fmt-check lint test coverage verify ci build run config-check docker-build docker-up docker-down docker-logs clean

MVN := ./mvnw

help:
	@printf '%s\n' \
		'Targets:' \
		'  help          Show available commands' \
		'  format        Format Java and Maven files with Spotless' \
		'  fmt-check     Check Java and Maven formatting' \
		'  lint          Run static analysis checks' \
		'  test          Run automated tests' \
		'  coverage      Run tests and generate JaCoCo coverage report' \
		'  verify        Run Maven verification' \
		'  ci            Run the full local validation suite' \
		'  build         Build the application artifact' \
		'  run           Run the Spring Boot application locally' \
		'  config-check  Validate Docker Compose configuration' \
		'  docker-build  Build the API Docker image' \
		'  docker-up     Start API and PostgreSQL with Docker Compose' \
		'  docker-down   Stop Docker Compose services' \
		'  docker-logs   Follow Docker Compose logs' \
		'  clean         Remove Maven build output'

format:
	$(MVN) spotless:apply

fmt-check:
	$(MVN) spotless:check

lint:
	$(MVN) checkstyle:check pmd:check pmd:cpd-check spotbugs:check

test:
	$(MVN) test

coverage:
	$(MVN) verify

verify:
	$(MVN) clean verify
	sh build-tools/checks/check-architecture-boundaries.sh
	sh build-tools/checks/check-private-artifacts.sh
	sh build-tools/checks/check-readme-commands.sh

ci: verify

build:
	$(MVN) package

run:
	$(MVN) spring-boot:run

config-check:
	docker compose config

docker-build:
	docker compose build api

docker-up:
	docker compose up --build

docker-down:
	docker compose down

docker-logs:
	docker compose logs -f

clean:
	$(MVN) clean
