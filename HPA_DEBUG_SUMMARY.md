# HPA Debug Summary

## What is HPA?

**HPA** stands for **Horizontal Pod Autoscaler**. It's a Kubernetes component that automatically scales the number of pods in a deployment based on observed metrics like:
- CPU utilization
- Memory utilization  
- Custom metrics

In your case, the `vector-logs` HPA is configured to scale between 1-5 replicas based on:
- CPU utilization target: 70%
- Memory utilization target: 75%

## Problem: HPA Showing `<unknown>` Metrics

Your HPA is showing:
```
cpu: <unknown>/70%, memory: <unknown>/75%
```

This means the HPA cannot retrieve CPU and memory metrics to make scaling decisions.

## Root Cause

The issue is that **metrics-server** cannot scrape metrics from the kubelet. Looking at the metrics-server logs:

```
E0113 12:19:32.284369       1 scraper.go:140] "Failed to scrape node" err="request failed, status: \"403 Forbidden\"" node="logwise-local-control-plane"
```

The metrics-server is getting **403 Forbidden** errors when trying to access the kubelet's metrics endpoint. This is a common issue in **Kind (Kubernetes in Docker)** clusters where the kubelet's authorization mode blocks the metrics-server from accessing node metrics.

## Diagnosis Steps Performed

1. ✅ Verified HPA configuration is correct
2. ✅ Verified vector deployment has resource requests/limits set
3. ✅ Confirmed metrics-server pod is running but not ready (0/1)
4. ✅ Found 403 Forbidden errors in metrics-server logs
5. ✅ Confirmed APIService shows "MissingEndpoints" status

## Solution

The kubelet on the Kind node is blocking metrics-server access. You can fix this by modifying the kubelet configuration directly on the running node. Here are the manual commands to run:

### Fix the Running Cluster (No File Changes Required)

**Step 1: Backup the current kubelet config**
```bash
docker exec logwise-local-control-plane cp /var/lib/kubelet/config.yaml /var/lib/kubelet/config.yaml.backup
```

**Step 2: Modify the kubelet config to allow anonymous access**
```bash
docker exec logwise-local-control-plane sed -i 's/enabled: false/enabled: true/' /var/lib/kubelet/config.yaml
docker exec logwise-local-control-plane sed -i 's/mode: Webhook/mode: AlwaysAllow/' /var/lib/kubelet/config.yaml
```

**Step 3: Restart the kubelet service**
```bash
docker exec logwise-local-control-plane systemctl restart kubelet
```

**Step 4: Wait a few seconds for kubelet to restart, then verify**
```bash
# Wait 10-15 seconds, then check metrics-server
kubectl get pods -n kube-system | grep metrics-server
# Should eventually show: 1/1 Running
```

**Alternative: If the above doesn't work, you can edit the config file directly:**

```bash
# Get the node name (adjust if different)
NODE_NAME=$(kubectl get nodes -o name | head -1 | cut -d/ -f2)

# Edit the config file interactively
docker exec -it logwise-local-control-plane vi /var/lib/kubelet/config.yaml
# Change:
#   authentication.anonymous.enabled: false  →  true
#   authorization.mode: Webhook  →  AlwaysAllow
# Then save and restart kubelet:
docker exec logwise-local-control-plane systemctl restart kubelet
```

**Note:** This fix will persist until the node is restarted. If you restart the Kind cluster, you'll need to reapply these changes.

### Verify the Fix

After recreating the cluster and redeploying, verify metrics-server is working:

```bash
# Check metrics-server is ready
kubectl get pods -n kube-system | grep metrics-server
# Should show: 1/1 Running

# Test metrics API
kubectl top nodes
kubectl top pods -n logwise

# Check HPA status (should show actual metrics, not <unknown>)
kubectl get hpa vector-logs -n logwise
kubectl describe hpa vector-logs -n logwise
```

The HPA should now show actual CPU and memory percentages instead of `<unknown>`.

## Additional Notes

- The kubelet configuration added is **only for local development** (Kind clusters)
- For production clusters (EKS, GKE, AKS), metrics-server should work without these changes
- The configuration enables anonymous authentication on the kubelet, which is acceptable for local dev but should NOT be used in production

## References

- [Kubernetes HPA Documentation](https://kubernetes.io/docs/tasks/run-application/horizontal-pod-autoscale/)
- [Metrics Server Documentation](https://github.com/kubernetes-sigs/metrics-server)
- [Kind Configuration Documentation](https://kind.sigs.k8s.io/docs/user/configuration/)

