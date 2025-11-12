SHELL := /bin/bash

PROJECT := log-central
COMPOSE := docker compose

.PHONY: bootstrap up down logs ps topics spark-submit spark-rest-submit init-db teardown reset-kafka

bootstrap:
	@chmod +x ./start.sh || true
	./start.sh

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
	$(COMPOSE) exec -T kafka bash -lc "kafka-topics.sh --bootstrap-server kafka:9092 --create --if-not-exists --topic $$KAFKA_TOPIC --partitions 3 --replication-factor 1"

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

# Build the Java Spark app JAR
spark-build:
	cd d11-log-management-spark && \
	mvn -B -T 4 clean package -Dmaven.test.skip -Dmaven.artifact.threads=10 -Paws

# Submit the built JAR to Spark master
# Usage: make spark-run-jar MAIN_CLASS=com.example.Main APP_JAR=app.jar
spark-run-jar:
	@if [ -z "$(MAIN_CLASS)" ]; then echo "MAIN_CLASS is required"; exit 1; fi
	@if [ -z "$(APP_JAR)" ]; then echo "APP_JAR is required (file under d11-log-managament-spark/target)"; exit 1; fi
	$(COMPOSE) exec \
	  -e MAIN_CLASS=$(MAIN_CLASS) \
	  -e APP_JAR=/opt/app/$(APP_JAR) \
	  spark-client bash -lc "/opt/submit.sh"

teardown:
	$(COMPOSE) down -v

# Reset Kafka and ZooKeeper volumes to fix cluster ID mismatch issues
reset-kafka:
	@echo "Stopping Kafka and ZooKeeper services..."
	$(COMPOSE) stop kafka zookeeper || true
	@echo "Removing Kafka and ZooKeeper containers and volumes..."
	$(COMPOSE) rm -f -v kafka zookeeper || true
	docker volume rm log-central_kafka_data log-central_zookeeper_data 2>/dev/null || true
	@echo "Kafka and ZooKeeper volumes removed. Run 'make up' to start fresh."


