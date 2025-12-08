---
title: Log collectors in Logwise
next:
  text: OpenTelemetry – EC2
  link: /send-logs/ec2/opentelemetry
---

## Log collectors in Logwise

Logwise is built around **Vector** as the central log router.  
Different **collectors/agents** can be used to read logs from applications and ship them into Vector.

This section explains:

- **Which log collectors we support today**
- **How logs flow into Vector**
- **Where to find per-collector setup guides**
- **What (if anything) you need to change in Vector for Docker vs manual setups**

---

## 1. High‑level flow

- **Apps** write logs to files (for local demo: the `healthcheck-dummy` service writes to `/var/log/healthcheck/healthcheck.log`).
- A **collector/agent**:
  - tails those files, parses them, and adds metadata (for example: `service_name`),
  - sends logs to **Vector** over a protocol that Vector supports.
- **Vector**:
  - receives logs (for OTEL-based agents, this is via **OTLP**),
  - applies transforms,
  - ships them to Kafka.

Vector’s OTLP listener is defined in `vector/vector.yaml`:

```1:9:vector/vector.yaml
sources:
  otlp_logs:
    type: "opentelemetry"
    grpc:
      address: "0.0.0.0:4317"
    http:
      address: "0.0.0.0:4318"
```

Any collector that can speak OTLP can send logs to:

- gRPC: `http://<VECTOR_HOST>:4317`
- HTTP: `http://<VECTOR_HOST>:4318/v1/logs`

> **Docker setup**: In the Docker-based Logwise stack, this OTLP source is already configured in the Vector container; you typically do **not** need to change Vector for OTEL-based collectors (OpenTelemetry Collector, Fluent Bit OTEL output).  
> **Manual setup**: In a self-hosted Vector deployment, you install and configure Vector as described in the **Self-Host → Vector** guide; the same OTLP source applies there.

---

## 2. Supported collectors

Logwise supports (or provides examples for) the following collectors:

- **OpenTelemetry Collector** – OTEL-native agent, preferred when you want full OTEL processing.
- **Fluent Bit** – lightweight agent, great for container-heavy environments.
- **Fluentd** – full-featured log processor with a rich plugin ecosystem.
- **Logstash** – part of the Elastic stack, often used where it is already deployed.
- **syslog-ng / rsyslog** – traditional syslog daemons, forwarding logs over TCP/UDP.


## 3. Choosing a collector

- **OpenTelemetry Collector**
  - Best when you already speak OTLP and want OTEL-native processors.
  - Rich ecosystem of receivers/exporters and a standard telemetry model.
- **Fluent Bit**
  - Lightweight, very efficient, great for container-heavy environments.
  - Good choice when you just need to tail logs and ship to Vector.
- **Fluentd / Logstash / Syslog**
  - Use when you already have them deployed or need their specific features.
  - Just make sure they can output to a protocol/source that Vector supports.

For concrete setup steps, pick one of the collectors from the sidebar and follow its dedicated guide.


