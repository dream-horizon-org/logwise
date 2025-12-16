FROM timberio/vector:0.45.0-alpine

# Copy the Vector configuration and protobuf descriptor from the repo
# so they are the single source of truth for both Docker Compose and k8s.
COPY vector.yaml /etc/vector/vector.yaml
COPY logwise-vector.desc /etc/vector/logwise-vector.desc


