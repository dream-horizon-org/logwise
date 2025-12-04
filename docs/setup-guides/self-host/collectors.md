## Log collectors with Vector

This stack is built around **Vector** as the central log router.  
Different **collectors/agents** can be used to read logs from applications and ship them into Vector.

This document shows:

- **How logs flow** into Vector
- **How to wire the OpenTelemetry Collector to Vector**
- **How to wire Fluent Bit to Vector**
- **How to wire three other agents (Fluentd, Logstash, syslog‑ng) to Vector**
- **What, if anything, needs to change on the Vector side**

---

## 1. High‑level flow

- **Apps / generators** write logs to files (for local demo: the `healthcheck-dummy` service writes to `/var/log/healthcheck/healthcheck.log`).
- A **collector/agent**:
  - tails those files, parses them, and adds metadata (e.g. `service_name`),
  - sends logs to **Vector** over a protocol that Vector supports.
- **Vector**:
  - receives logs (here via **OTLP**),
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

---

## 2. OpenTelemetry Collector → Vector

### 2.1. Collector configuration

The OTEL Collector can tail log files and export them to Vector over **OTLP HTTP**:

```8:40:deploy/healthcheck-dummy/otel-collector-config.yaml
receivers:
  filelog/healthcheck:
    include: 
      - /var/log/healthcheck/*.log
    start_at: end
    include_file_path: true
    include_file_name_resolved: true
    operators:
      - type: regex_parser
        id: parser-healthcheck
        regex: '^(?P<timestamp>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}) - (?P<message>.*)$'

exporters:
  otlphttp:
    endpoint: "http://<VECTOR_HOST>:4318"
```

Replace `<VECTOR_HOST>` with the hostname or IP where Vector is reachable.

### 2.2. Vector configuration impact

- If you keep the existing `otlp_logs` source as in `vector/vector.yaml`, **no changes are required** on the Vector side.
- You only need to ensure that:
  - the OTLP HTTP port in Vector (`4318` by default) is open and reachable from the collector, and
  - the OTEL collector’s `endpoint` points at that host:port.

---

## 3. Fluent Bit → Vector

**Fluent Bit** can also tail the same log files and send data to Vector over **OTLP HTTP**.

### 3.1. Collector configuration

Example configuration (taken from `deploy/healthcheck-dummy/fluent-bit.conf`):

```1:26:deploy/healthcheck-dummy/fluent-bit.conf
[SERVICE]
    Flush        1
    Daemon       off
    Log_Level    info
    Http_Server  On
    Http_Listen  0.0.0.0
    Http_Port    2020

[INPUT]
    Name              tail
    Path              /var/log/healthcheck/*.log
    Tag               healthcheck
    Mem_Buf_Limit     5MB
    Skip_Long_Lines   On
    Refresh_Interval  5

[FILTER]
    Name   modify
    Match  healthcheck
    Add    service_name  healthcheck-dummy

[OUTPUT]
    Name           opentelemetry
    Match          *
    Host           <VECTOR_HOST>
    Port           4318
    Logs_uri       /v1/logs
    Tls            Off
```

Key points:

- **Input**: tails `/var/log/healthcheck/*.log` (adjust for your app).
- **Filter**: adds a `service_name=healthcheck-dummy` field so downstream logic can key on it.
- **Output**: uses Fluent Bit’s `opentelemetry` output, targeting `http://<VECTOR_HOST>:4318/v1/logs`.

### 3.2. Vector configuration impact

- As with the OTEL Collector, if you keep the existing `otlp_logs` source, **no changes are required** on the Vector side.
- Ensure the OTLP HTTP port (`4318`) is reachable from where Fluent Bit runs.

---

## 4. Using other collectors

You can plug in other collectors as long as they can send to a protocol that Vector supports.

Common patterns:

- **OTLP (preferred)** – for OTEL‑aware agents:
  - Target `http://<VECTOR_HOST>:4317` (gRPC) or `http://<VECTOR_HOST>:4318/v1/logs` (HTTP).
- **TCP / HTTP** – for agents like Fluentd and Logstash:
  - Use a `socket` or `http` source in Vector.
- **Syslog** – for agents like syslog‑ng / rsyslog:
  - Use a `syslog` source in Vector.

### 4.1. General steps to add a new collector

1. **Choose a protocol** that both the collector and Vector support:
   - OTLP, HTTP, TCP, syslog, Kafka, etc.
2. **Configure or add a Vector source** for that protocol in `vector/vector.yaml` (if not already present).
3. **Configure the collector**:
   - Tail the appropriate log files or read from the desired input.
   - Add any metadata (e.g. `service_name`) the rest of your pipeline expects.
   - Point the collector output to the Vector endpoint you configured (host, port, and protocol).
4. **Deploy and verify** by checking Vector’s metrics/logs and downstream sinks (e.g. Kafka).

The following subsections show concrete examples for three other popular agents.

---

### 4.2. Fluentd → Vector

**Fluentd** can tail log files and forward them using its `forward` output plugin.

#### 4.2.1. Fluentd configuration

Example Fluentd config:

```xml
<source>
  @type tail
  path /var/log/healthcheck/healthcheck.log
  pos_file /var/log/fluentd-healthcheck.pos
  tag healthcheck
  <parse>
    @type none
  </parse>
</source>

<match healthcheck>
  @type forward
  <server>
    host VECTOR_HOST
    port 24224
  </server>
</match>
```

- Replace `VECTOR_HOST` with the host where Vector is running.
- Port `24224` is an arbitrary example; you can use any free port.

#### 4.2.2. Vector configuration impact

Add a `socket` source (or `fluent` / `fluent_source` depending on Vector version) to `vector/vector.yaml`:

```30:40:vector/vector.yaml
  fluentd_logs:
    type: "socket"
    address: "0.0.0.0:24224"
    mode: "tcp"
```

Then point your transforms/sinks at `fluentd_logs` (or add a new transform that consumes it).  
This is **additional** to the existing `otlp_logs` source; you can run both in parallel.

---

### 4.3. Logstash → Vector

**Logstash** can send JSON logs over TCP, which Vector can receive via a `socket` source.

#### 4.3.1. Logstash configuration

Example `logstash.conf`:

```ruby
input {
  file {
    path => "/var/log/healthcheck/healthcheck.log"
    start_position => "beginning"
  }
}

filter {
  mutate {
    add_field => { "service_name" => "healthcheck-dummy" }
  }
}

output {
  tcp {
    host  => "VECTOR_HOST"
    port  => 24225
    codec => json_lines
  }
}
```

- Replace `VECTOR_HOST` with the Vector host.
- Port `24225` is an example; choose any free port.

#### 4.3.2. Vector configuration impact

Add a `socket` source for Logstash:

```40:48:vector/vector.yaml
  logstash_logs:
    type: "socket"
    address: "0.0.0.0:24225"
    mode: "tcp"
    decoding:
      codec: "json"
```

Again, point any transforms/sinks at `logstash_logs` as needed.

---

### 4.4. syslog‑ng (or rsyslog) → Vector

Traditional syslog daemons like **syslog‑ng** or **rsyslog** can forward logs over TCP/UDP.  
Vector can ingest these via its `syslog` source.

#### 4.4.1. syslog‑ng configuration

Example snippet:

```conf
source s_healthcheck {
  file("/var/log/healthcheck/healthcheck.log");
};

destination d_vector {
  network("VECTOR_HOST" port(5514) transport("tcp"));
};

log {
  source(s_healthcheck);
  destination(d_vector);
};
```

- Replace `VECTOR_HOST` with the host where Vector is reachable.
- Port `5514` is an example (separate from any system‑wide syslog port you may already use).

#### 4.4.2. Vector configuration impact

Add a `syslog` source in `vector/vector.yaml`:

```48:60:vector/vector.yaml
  syslog_logs:
    type: "syslog"
    address: "0.0.0.0:5514"
    mode: "tcp"
    max_length: 102400
```

You can then route `syslog_logs` into your existing transforms/sinks, or add a dedicated transform if you want to treat these logs differently.

---

## 5. When to choose which collector

- **OpenTelemetry Collector**
  - Best when you already speak OTLP and want OTEL‑native processors.
  - Rich ecosystem of receivers/exporters and standard telemetry model.

- **Fluent Bit**
  - Lightweight, very efficient, great for container‑heavy environments.
  - Good choice when you just need to tail logs and ship to Vector.

- **Other agents (Filebeat, Logstash, syslog, etc.)**
  - Use when you already have them deployed or need their specific features.
  - Just make sure they can output to a protocol/source that Vector supports.


