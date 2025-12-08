## Logstash

This page explains how to configure **Logstash** to ship logs into **Vector** for Logwise, and what you need to change on the Vector side for:

- the **Docker-based Logwise stack**, and
- a **manual/self-hosted Vector setup**.

Logstash sends JSON logs over **TCP**, and Vector receives them using a `socket` source with a JSON codec.

---

## 1. Logstash configuration

Example `logstash.conf` that tails a log file, enriches events, and forwards them to Vector:

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
- Port `24225` is an example; you can choose any free port as long as it matches the Vector configuration.

---

## 2. Vector configuration impact

To receive JSON logs from Logstash, Vector needs a **TCP socket source** with JSON decoding:

```40:48:vector/vector.yaml
  logstash_logs:
    type: "socket"
    address: "0.0.0.0:24225"
    mode: "tcp"
    decoding:
      codec: "json"
```

You can then route `logstash_logs` into your existing transforms/sinks, or add a dedicated transform if you want to treat these logs differently.

### 2.1 Docker-based Logwise stack

For Docker:

1. Edit `vector/vector.yaml` in the repository and add the `logstash_logs` source block.
2. Rebuild or restart the Vector container so it picks up the new configuration:
   - for example `docker compose restart vector` from the `deploy` directory, or
   - bring the stack down and up again (`make down && make up`) if you’re using the `Makefile`.
3. If Logstash runs outside the Docker network, ensure that the Vector container exposes port **24225/tcp** to the host.

### 2.2 Manual/self-hosted Vector

For a self-hosted Vector:

1. Follow the **Self-Host → Vector** guide to install Vector and place `vector.yaml` at `/etc/vector/vector.yaml`.
2. Add the `logstash_logs` source to `/etc/vector/vector.yaml`.
3. Open port **24225/tcp** (or your chosen port) on your firewall.
4. Restart Vector (for example: `sudo systemctl restart vector`).

---

## 3. Summary

- Use Logstash when it is already part of your stack or you need its filter plugins.
- Configure Logstash with:
  - a `file` input for your log files,
  - a `tcp` output with `json_lines` codec pointing to `VECTOR_HOST:24225`.
- Configure Vector with a `socket` source named `logstash_logs` listening on the same port and decoding JSON, then plug that source into your existing Logwise transforms/sinks.


