SHELL := /bin/bash

PROJECT := log-central
COMPOSE := docker compose

.PHONY: bootstrap setup up down logs ps topics list-topics spark-submit spark-rest-submit init-db teardown reset-kafka

bootstrap:
	@chmod +x ./scripts/start.sh || true
	./scripts/start.sh

# One-click setup: bootstrap, create .env, start services, create topics
setup:
	@chmod +x ./scripts/setup.sh || true
	./scripts/setup.sh

up:
	$(COMPOSE) -f docker-compose.yml up -d --build

down:
	$(COMPOSE) -f docker-compose.yml down

logs:
	$(COMPOSE) -f docker-compose.yml logs -f --tail=200

ps:
	$(COMPOSE) -f docker-compose.yml ps
# List all Kafka topics
list-topics:
	$(COMPOSE) -f docker-compose.yml exec kafka bash -lc "kafka-topics --bootstrap-server kafka:9092 --list"

# Placeholder for DB init/migrations if needed
init-db:
	@echo "DB migrations handled by the app on startup (Spring Boot)."

# Submit Spark job on-demand
spark-submit:
	$(COMPOSE) -f docker-compose.yml exec spark-client bash -lc \
		"/opt/submit.sh"

spark-rest-submit:
	$(COMPOSE) -f docker-compose.yml exec spark-client bash -lc \
		"/opt/rest-submit.sh"

.PHONY: spark-build spark-run-jar


teardown:
	$(COMPOSE) -f docker-compose.yml down --volumes --remove-orphans

