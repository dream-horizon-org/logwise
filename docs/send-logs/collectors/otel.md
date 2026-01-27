## OpenTelemetry Collector

This page explains how to configure **OpenTelemetry Collector** to ship logs into **Vector** for Logwise, and what (if anything) you need to change on the Vector side for:

- the **Docker-based Logwise stack**, and
- a **manual/self-hosted Vector setup**.

---

## 1. What the OTEL Collector does

The OTEL Collector:

- tails log files from your applications,
- parses and enriches them (for example: adds `service_name`),
- sends them to **Vector** using **OTLP HTTP**.

Vector’s OTLP listener is already configured in `vector/vector.yaml`:

```yaml
sources:
  otlp_logs:
    type: "opentelemetry"
    grpc:
      address: "0.0.0.0:4317"
    http:
      address: "0.0.0.0:4318"
```

Any OTEL Collector can send logs to:

- gRPC: `http://<VECTOR_HOST>:4317`
- HTTP: `http://<VECTOR_HOST>:4318/v1/logs`

For Logwise we recommend **OTLP HTTP**.

---

## 2. Example OTEL Collector configuration

Below is a minimal example that tails log files and exports them to Vector over OTLP HTTP:

```yaml
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

Key points:

- **Receiver**: `filelog/healthcheck` tails `/var/log/healthcheck/*.log`.
- **Parser**: a `regex_parser` extracts `timestamp` and `message`.
- **Exporter**: `otlphttp` points at Vector’s OTLP HTTP endpoint.

Replace `<VECTOR_HOST>` with the hostname or IP where Vector is reachable.

### Multiline log support

If your logs span multiple lines, add the `multiline` configuration section to your receiver. This ensures that multi-line log entries are properly grouped together before parsing.

Add the following to your receiver configuration (before the `operators` section):

```yaml
receivers:
  filelog/healthcheck:
    include: 
      - /var/log/healthcheck/*.log
    start_at: end
    include_file_path: true
    include_file_name_resolved: true

    multiline:
      line_start_pattern: '<PATTERN>'

    operators:
      - type: regex_parser
        id: parser-healthcheck
        regex: '^(?P<timestamp>\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}) - (?P<message>.*)$'
```

The `line_start_pattern` should match the beginning of each log entry. Adjust the pattern to match your log format.

**Example:** If your logs start with a timestamp like `2024-01-15 10:30:45`, use:

```yaml
multiline:
  line_start_pattern: '^\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}'
```

This pattern will correctly group multi-line log entries. For example, a log entry like:

```
2024-01-15 10:30:45 - Error occurred
  Stack trace:
    at com.example.Class.method(Class.java:123)
    at com.example.Other.method(Other.java:456)
2024-01-15 10:30:46 - Next log entry
```

Will be grouped into two separate log entries, with the first entry containing all lines from the timestamp until the next timestamp.

For a more complete OTEL Collector installation and configuration walkthrough, also see the [OpenTelemetry – EC2 guide](/send-logs/collectors/otel/ec2/opentelemetry).

---

## 3. Vector configuration impact

### 3.1 Docker-based Logwise stack

In the Docker-based Logwise setup:

- Vector is already configured with an `otlp_logs` source that listens on port **4318** for OTLP HTTP logs.
- No additional changes are required in `vector.yaml` for the OTEL Collector.

You only need to ensure that:

- the Vector container is reachable at port **4318** from where the OTEL Collector runs, and
- the OTEL Collector’s `endpoint` points at that host:port (for example `http://vector:4318` inside the Docker network).

### 3.2 Manual/self-hosted Vector

In a self-hosted setup, follow the **Self-Host → Vector** guide to:

- install Vector on your host,
- copy `vector/vector.yaml` to `/etc/vector/vector.yaml`,
- start Vector as a service.

As long as you keep the `otlp_logs` source as shown above:

- you do **not** need extra changes in Vector for the OTEL Collector,
- you only need to make sure that port **4318** is open in your firewall and reachable from the OTEL Collector host.

---

## 4. Summary

- Use the OTEL Collector when you want a rich, OTEL-native log pipeline.
- Point the OTEL Collector’s **OTLP HTTP exporter** at `http://<VECTOR_HOST>:4318/v1/logs` (or `http://<VECTOR_HOST>:4318` depending on your OTEL version/config style).
- In Logwise, Vector’s OTLP source is already wired for both Docker and self-hosted setups; you mainly need to:
  - configure the **filelog receiver** for your log paths, and
  - point the **OTLP exporter** at your Vector instance.


