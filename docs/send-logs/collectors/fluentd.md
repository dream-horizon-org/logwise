## Fluentd

This page explains how to configure **Fluentd** to ship logs into **Vector** for Logwise, and what you need to change on the Vector side for:

- the **Docker-based Logwise stack**, and
- a **manual/self-hosted Vector setup**.

Unlike OTEL Collector or Fluent Bit (which use OTLP), Fluentd will send logs over **TCP** using its `forward` output plugin, and Vector will receive them using a `socket` source.

---

## 1. Fluentd configuration

Example Fluentd config that tails a log file and forwards records to Vector:

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
- Port `24224` is an example; you can use any free port, as long as it matches the Vector configuration.

---

## 2. Vector configuration impact

To receive logs from Fluentd, Vector needs a **TCP socket source**:

```30:40:vector/vector.yaml
  fluentd_logs:
    type: "socket"
    address: "0.0.0.0:24224"
    mode: "tcp"
```

You can then:

- reference `fluentd_logs` in existing transforms, or
- create a dedicated transform that consumes `fluentd_logs` and routes to the same sinks as `otlp_logs`.

### 2.1 Docker-based Logwise stack

For Docker:

1. Edit `vector/vector.yaml` in the repository and add the `fluentd_logs` source shown above.
2. Rebuild or restart the Vector container so it picks up the new configuration, for example:
   - `docker compose restart vector` (from the `deploy` directory), or
   - bring the stack down and up again (`make down && make up`) if you’re using the `Makefile`.
3. Ensure the container port mapped for Vector exposes `24224/tcp` if Fluentd runs outside the Docker network.

### 2.2 Manual/self-hosted Vector

For a self-hosted Vector:

1. Follow the **Self-Host → Vector** guide to install Vector and copy `vector.yaml` to `/etc/vector/vector.yaml`.
2. Add the `fluentd_logs` source block to `/etc/vector/vector.yaml`.
3. Open port **24224/tcp** on your firewall (or whichever port you chose).
4. Restart Vector (for example: `sudo systemctl restart vector`).

---

## 3. Summary

- Use Fluentd when you already have it deployed or need its plugin ecosystem.
- Configure Fluentd with:
  - a `tail` input for your log files,
  - a `forward` output pointing to `VECTOR_HOST:24224`.
- Configure Vector with a `socket` source named `fluentd_logs` listening on the same port, and wire that source into your existing transforms/sinks so Fluentd logs follow the same pipeline as OTEL-based logs.


