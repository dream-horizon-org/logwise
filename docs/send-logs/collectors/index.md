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

<div class="collector-grid">

<div class="collector-card">
  <div class="collector-card-header">
    <h3>OpenTelemetry Collector</h3>
  </div>
  <div class="collector-card-body">
    <p>OTEL-native agent, preferred when you want full OTEL processing.</p>
  </div>
  <div class="collector-card-footer">
    <a href="/send-logs/collectors/otel/index">View Guide →</a>
  </div>
</div>

<div class="collector-card">
  <div class="collector-card-header">
    <h3>Fluent Bit</h3>
  </div>
  <div class="collector-card-body">
    <p>Lightweight agent, great for container-heavy environments.</p>
  </div>
  <div class="collector-card-footer">
    <a href="/send-logs/collectors/fluent-bit">View Guide →</a>
  </div>
</div>

<div class="collector-card">
  <div class="collector-card-header">
    <h3>Fluentd</h3>
  </div>
  <div class="collector-card-body">
    <p>Full-featured log processor with a rich plugin ecosystem.</p>
  </div>
  <div class="collector-card-footer">
    <a href="/send-logs/collectors/fluentd">View Guide →</a>
  </div>
</div>

<div class="collector-card">
  <div class="collector-card-header">
    <h3>Logstash</h3>
  </div>
  <div class="collector-card-body">
    <p>Part of the Elastic stack, often used where it is already deployed.</p>
  </div>
  <div class="collector-card-footer">
    <a href="/send-logs/collectors/logstash">View Guide →</a>
  </div>
</div>

<div class="collector-card">
  <div class="collector-card-header">
    <h3>Syslog</h3>
  </div>
  <div class="collector-card-body">
    <p>Traditional syslog daemons (syslog-ng / rsyslog), forwarding logs over TCP/UDP.</p>
  </div>
  <div class="collector-card-footer">
    <a href="/send-logs/collectors/syslog">View Guide →</a>
  </div>
</div>

</div>

<style>
.collector-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(280px, 1fr));
  gap: 1.5rem;
  margin: 2rem 0;
}

.collector-card {
  background: var(--vp-c-bg-soft);
  border: 1px solid var(--vp-c-divider);
  border-radius: 8px;
  padding: 1.5rem;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.collector-card:hover {
  border-color: var(--vp-c-brand-1);
  transform: translateY(-4px);
  box-shadow: 0 8px 16px rgba(95, 211, 224, 0.15);
}

.collector-card-header h3 {
  margin: 0 0 1rem 0;
  font-size: 1.25rem;
  font-weight: 600;
  color: var(--vp-c-brand-1);
}

.collector-card-body {
  flex: 1;
  margin-bottom: 1rem;
}

.collector-card-body p {
  margin: 0;
  color: var(--vp-c-text-2);
  line-height: 1.6;
}

.collector-card-footer {
  margin-top: auto;
  padding-top: 1rem;
  border-top: 1px solid var(--vp-c-divider);
}

.collector-card-footer a {
  color: var(--vp-c-brand-1);
  text-decoration: none;
  font-weight: 500;
  transition: color 0.2s ease;
}

.collector-card-footer a:hover {
  color: var(--vp-c-brand-2);
  text-decoration: underline;
}

@media (max-width: 640px) {
  .collector-grid {
    grid-template-columns: 1fr;
  }
}
</style>


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


