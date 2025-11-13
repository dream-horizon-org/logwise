SHELL := /bin/bash

PROJECT := log-central
COMPOSE := docker compose

.PHONY: bootstrap setup up down logs ps topics list-topics spark-submit spark-rest-submit init-db teardown reset-kafka

bootstrap:
	@chmod +x ./start.sh || true
	./start.sh

# One-click setup: bootstrap, create .env, start services, create topics
setup:
	@chmod +x ./setup.sh || true
	./setup.sh

up:
	$(COMPOSE) up -d --build

down:
	$(COMPOSE) down

logs:
	$(COMPOSE) logs -f --tail=200

ps:
	$(COMPOSE) ps

# Create Kafka topic if not exists
topics:
	$(COMPOSE) exec -T kafka bash -lc "kafka-topics --bootstrap-server kafka:9092 --create --if-not-exists --topic $$KAFKA_TOPIC --partitions 3 --replication-factor 1"

# List all Kafka topics
list-topics:
	$(COMPOSE) exec kafka bash -lc "kafka-topics --bootstrap-server kafka:9092 --list"

# Placeholder for DB init/migrations if needed
init-db:
	@echo "DB migrations handled by the app on startup (Spring Boot)."

# Submit Spark job on-demand
spark-submit:
	$(COMPOSE) exec spark-client bash -lc \
		"/opt/submit.sh"

spark-rest-submit:
	$(COMPOSE) exec spark-client bash -lc \
		"/opt/rest-submit.sh"

.PHONY: spark-build spark-run-jar


teardown:
	$(COMPOSE) down -v

