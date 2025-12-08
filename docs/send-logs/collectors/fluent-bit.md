## Fluent Bit

This page explains how to configure **Fluent Bit** to ship logs into **Vector** for Logwise, and what (if anything) you need to change on the Vector side for:

- the **Docker-based Logwise stack**, and
- a **manual/self-hosted Vector setup**.

---

## 1. What Fluent Bit does in this setup

Fluent Bit:

- tails log files from your applications,
- adds useful metadata (for example: `service_name`),
- sends logs to **Vector** using **OTLP HTTP** via Fluent Bit’s `opentelemetry` output.

Vector receives these logs through the same OTLP source used by the OpenTelemetry Collector.

---

## 2. Example Fluent Bit configuration

Below is an example configuration (adapt it to your environment):

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

- **Input**: tails `/var/log/healthcheck/*.log` (change to your app’s path).
- **Filter**: adds `service_name=healthcheck-dummy` so downstream logic can key on it.
- **Output**: uses Fluent Bit’s `opentelemetry` output, targeting `http://<VECTOR_HOST>:4318/v1/logs`.

Replace `<VECTOR_HOST>` with the hostname or IP where Vector is reachable.

---

## 3. Vector configuration impact

### 3.1 Docker-based Logwise stack

In the Docker-based Logwise setup:

- Vector is already configured with an `otlp_logs` source that listens on port **4318** for OTLP HTTP logs.
- No additional changes are required in `vector.yaml` to support Fluent Bit’s OTLP output.

You only need to ensure that:

- the Vector container is reachable from Fluent Bit at port **4318**, and
- the Fluent Bit `Host` points at that host (for example `vector` if you are inside the same Docker network).

### 3.2 Manual/self-hosted Vector

In a self-hosted setup, follow the **Self-Host → Vector** guide to install and configure Vector with the `otlp_logs` source enabled:

```1:9:vector/vector.yaml
sources:
  otlp_logs:
    type: "opentelemetry"
    grpc:
      address: "0.0.0.0:4317"
    http:
      address: "0.0.0.0:4318"
```

As long as this source is present and listening on port **4318**:

- you do **not** need further changes in Vector for Fluent Bit,
- you only need to open port **4318** on your firewall and point Fluent Bit at that address.

---

## 4. Summary

- Use Fluent Bit when you need a **lightweight log agent** that can still speak OTLP to Vector.
- Configure Fluent Bit with:
  - a `tail` input for your log files,
  - a `modify` filter to add metadata (for example `service_name`),
  - an `opentelemetry` output pointing to `http://<VECTOR_HOST>:4318/v1/logs`.
- In Logwise, Vector’s OTLP source is the same for both the OTEL Collector and Fluent Bit, and requires no extra changes for either Docker or manual setups.


