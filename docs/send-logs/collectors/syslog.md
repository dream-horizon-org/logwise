## syslog-ng / rsyslog

This page explains how to configure traditional syslog daemons like **syslog-ng** or **rsyslog** to ship logs into **Vector** for Logwise, and what you need to change on the Vector side for:

- the **Docker-based Logwise stack**, and
- a **manual/self-hosted Vector setup**.

Syslog daemons forward logs over **TCP** or **UDP**, and Vector ingests them using its `syslog` source.

---

## 1. syslog-ng configuration example

Example syslog-ng snippet that reads a local log file and forwards logs to Vector over TCP:

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
- Port `5514` is an example; you can use a different port as long as it matches the Vector configuration.

For rsyslog, the equivalent configuration would use an `action` with `omfwd` (or similar) to forward to the same host and port.

---

## 2. Vector configuration impact

To receive syslog traffic, Vector needs a **syslog source**:

```48:60:vector/vector.yaml
  syslog_logs:
    type: "syslog"
    address: "0.0.0.0:5514"
    mode: "tcp"
    max_length: 102400
```

You can then route `syslog_logs` into your existing transforms/sinks, or keep them isolated with a dedicated transform if you prefer.

### 2.1 Docker-based Logwise stack

For Docker:

1. Edit `vector/vector.yaml` in the repository and add the `syslog_logs` source.
2. Rebuild or restart the Vector container so it picks up the new configuration:
   - for example `docker compose restart vector` from the `deploy` directory, or
   - bring the stack down and up again (`make down && make up`) if you’re using the `Makefile`.
3. If syslog-ng/rsyslog runs outside the Docker network, ensure that the Vector container exposes port **5514/tcp** (or your chosen port) to the host.

### 2.2 Manual/self-hosted Vector

For a self-hosted Vector:

1. Follow the **Self-Host → Vector** guide to install Vector and place `vector.yaml` at `/etc/vector/vector.yaml`.
2. Add the `syslog_logs` source block to `/etc/vector/vector.yaml`.
3. Open port **5514/tcp** (or your chosen port) on your firewall.
4. Restart Vector (for example: `sudo systemctl restart vector`).

---

## 3. Summary

- Use syslog-ng or rsyslog when you want to integrate existing syslog-based infrastructure with Logwise.
- Configure your syslog daemon to forward to `VECTOR_HOST:5514` (TCP) or another agreed port.
- Configure Vector with a `syslog` source named `syslog_logs` listening on the same port, and plug that source into your existing Logwise transforms/sinks.


