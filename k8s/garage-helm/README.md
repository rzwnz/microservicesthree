# Garage Helm Chart — Deployment Guide

Production-ready Helm chart for deploying [Garage](https://garagehq.deuxfleurs.fr/) (S3-compatible distributed object storage) on Kubernetes.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│ Kubernetes Cluster                                      │
│                                                         │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐    │
│  │  garage-0    │  │  garage-1    │  │  garage-2    │    │
│  │  ┌────────┐  │  │  ┌────────┐  │  │  ┌────────┐  │    │
│  │  │ S3 API │  │  │  │ S3 API │  │  │  │ S3 API │  │    │
│  │  │ :3900  │  │  │  │ :3900  │  │  │  │ :3900  │  │    │
│  │  ├────────┤  │  │  ├────────┤  │  │  ├────────┤  │    │
│  │  │ RPC    │  │  │  │ RPC    │  │  │  │ RPC    │  │    │
│  │  │ :3901  │◄─┼──┼─►│ :3901  │◄─┼──┼─►│ :3901  │  │    │
│  │  ├────────┤  │  │  ├────────┤  │  │  ├────────┤  │    │
│  │  │ Admin  │  │  │  │ Admin  │  │  │  │ Admin  │  │    │
│  │  │ :3903  │  │  │  │ :3903  │  │  │  │ :3903  │  │    │
│  │  └────────┘  │  │  └────────┘  │  │  └────────┘  │    │
│  │  PVC: data   │  │  PVC: data   │  │  PVC: data   │    │
│  │  PVC: meta   │  │  PVC: meta   │  │  PVC: meta   │    │
│  └─────────────┘  └─────────────┘  └─────────────┘    │
│         ▲                ▲                ▲             │
│         └────────────────┼────────────────┘             │
│                          │                              │
│                ┌─────────┴─────────┐                    │
│                │  Service (s3-api) │                    │
│                │  ClusterIP :3900  │                    │
│                └─────────┬─────────┘                    │
│                          │                              │
│                ┌─────────┴─────────┐                    │
│                │ Ingress (optional)│                    │
│                └───────────────────┘                    │
└─────────────────────────────────────────────────────────┘
```

## Prerequisites

| Tool    | Version |
|---------|---------|
| Helm    | >= 3.12 |
| kubectl | >= 1.27 |
| K8s     | >= 1.25 |

## Quick Start

```bash
# From the repository root
cd microservicesthree/k8s/garage-helm

# Install with default values (dev)
helm install garage . -n storage --create-namespace

# Install with custom values (production)
helm install garage . -n storage --create-namespace -f values-prod.yaml
```

## Configuration

### Core Values

| Parameter | Description | Default |
|-----------|-------------|---------|
| `replicaCount` | Number of Garage nodes | `3` |
| `image.repository` | Garage container image | `dxflrs/garage` |
| `image.tag` | Image tag | `v1.0.1` |
| `garage.s3Region` | S3 region identifier | `garage` |
| `garage.rpcSecret` | Shared RPC secret (change!) | `CHANGE_ME_...` |
| `garage.metadataReplicationFactor` | Metadata replication factor | `3` |

### Persistence

| Parameter | Description | Default |
|-----------|-------------|---------|
| `persistence.data.size` | Data volume size | `50Gi` |
| `persistence.data.storageClassName` | Storage class for data | `""` (default) |
| `persistence.metadata.size` | Metadata volume size | `1Gi` |
| `persistence.metadata.storageClassName` | Storage class for metadata | `""` (default) |

### Networking

| Parameter | Description | Default |
|-----------|-------------|---------|
| `ports.s3Api` | S3 API port | `3900` |
| `ports.rpc` | Inter-node RPC port | `3901` |
| `ports.web` | Web endpoint port | `3902` |
| `ports.admin` | Admin API port | `3903` |
| `ingress.enabled` | Enable Ingress resource | `false` |
| `ingress.className` | Ingress class name | `nginx` |
| `ingress.s3Host` | S3 API hostname | `s3.garage.local` |

### Resources

| Parameter | Description | Default |
|-----------|-------------|---------|
| `resources.requests.cpu` | CPU request | `250m` |
| `resources.requests.memory` | Memory request | `512Mi` |
| `resources.limits.cpu` | CPU limit | `1` |
| `resources.limits.memory` | Memory limit | `1Gi` |

### Bucket Initialization

| Parameter | Description | Default |
|-----------|-------------|---------|
| `buckets` | List of buckets to create on install | `[{name: file-service-dev-data}]` |
| `garage.s3Credentials.accessKeyId` | S3 access key for bucket init | `""` |
| `garage.s3Credentials.secretAccessKey` | S3 secret key for bucket init | `""` |

## Production Deployment

### 1. Generate an RPC Secret

```bash
openssl rand -hex 32
```

### 2. Create a production values file

```yaml
# values-prod.yaml
replicaCount: 3

garage:
  rpcSecret: "<generated-secret>"
  s3Region: "eu-central-1"
  metadataReplicationFactor: 3
  s3Credentials:
    accessKeyId: "<garage-admin-key>"
    secretAccessKey: "<garage-admin-secret>"

persistence:
  data:
    size: 200Gi
    storageClassName: "fast-ssd"
  metadata:
    size: 5Gi
    storageClassName: "fast-ssd"

resources:
  requests:
    cpu: 500m
    memory: 1Gi
  limits:
    cpu: 2
    memory: 4Gi

ingress:
  enabled: true
  className: nginx
  s3Host: s3.yourdomain.com
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/proxy-body-size: "500m"
  tls:
    - hosts:
        - s3.yourdomain.com
      secretName: garage-s3-tls

buckets:
  - name: file-service-prod-data
```

### 3. Deploy

```bash
helm install garage . \
  -n storage --create-namespace \
  -f values-prod.yaml
```

### 4. Verify cluster layout

```bash
# Port-forward the admin API
kubectl -n storage port-forward svc/garage-admin 3903:3903

# Check cluster status
curl http://localhost:3903/v0/status | jq .

# Check nodes are connected
curl http://localhost:3903/v0/status | jq '.knownNodes | length'
```

### 5. Assign node roles (required after first deploy)

Garage requires you to assign a "zone" and storage capacity to each node. After initial deployment:

```bash
# Get node IDs
NODE_IDS=$(curl -s http://localhost:3903/v0/status | jq -r '.knownNodes[].id')

# Assign each node (adjust zone and capacity as needed)
for NODE_ID in $NODE_IDS; do
  curl -X POST http://localhost:3903/v0/layout/assign \
    -H "Content-Type: application/json" \
    -d "[{\"id\": \"$NODE_ID\", \"zone\": \"dc1\", \"capacity\": 214748364800}]"
done

# Apply layout
VERSION=$(curl -s http://localhost:3903/v0/layout | jq '.version')
curl -X POST http://localhost:3903/v0/layout/apply \
  -H "Content-Type: application/json" \
  -d "{\"version\": $VERSION}"
```

### 6. Create API keys and bucket credentials

```bash
# Create an API key
curl -X POST http://localhost:3903/v0/key \
  -H "Content-Type: application/json" \
  -d '{"name": "file-service-key"}' | jq .

# Grant permissions on the data bucket
KEY_ID="<key-id-from-above>"
curl -X POST http://localhost:3903/v0/bucket/allow \
  -H "Content-Type: application/json" \
  -d "{\"bucketId\": \"file-service-dev-data\", \"accessKeyId\": \"$KEY_ID\", \"permissions\": {\"read\": true, \"write\": true, \"owner\": true}}"
```

## File Service Integration

Configure `application.yml` to point at the Garage S3 API:

```yaml
garage:
  endpoint: http://garage-s3.storage.svc.cluster.local:3900
  access-key: ${GARAGE_ACCESS_KEY}
  secret-key: ${GARAGE_SECRET_KEY}
  region: garage
  data-bucket: ${GARAGE_DATA_BUCKET:file-service-dev-data}
```

The file service stores objects under the unified key pattern:

```
entities/by-id/{entityId}/{category}/{objectId}.{ext}
artifacts/{type}/{id-or-date}/{objectId}.{ext}
system/{purpose}/{timestamp}/{filename}
tmp/{purpose}/{uuid}/{filename}
```

## Operations

### Scaling

```bash
# Scale up (data redistributes automatically)
helm upgrade garage . -n storage --set replicaCount=5

# After scaling, assign roles to new nodes (see step 5)
```

### Monitoring

Enable Prometheus metrics by setting:

```yaml
metrics:
  enabled: true
  serviceMonitor:
    enabled: true
```

### Backup

Metadata is stored on LMDB and replicated across nodes. For disaster recovery:

```bash
# Snapshot metadata from one node
kubectl -n storage exec garage-0 -- tar czf - /data/meta > meta-backup.tar.gz

# List all objects in a bucket (for data-level backup)
aws --endpoint-url http://localhost:3900 s3 ls s3://file-service-dev-data --recursive
```

### Upgrading Garage

```bash
# Update image tag
helm upgrade garage . -n storage --set image.tag=v1.1.0

# StatefulSet does a rolling restart automatically
```

## Troubleshooting

| Symptom | Cause | Solution |
|---------|-------|----------|
| Nodes not connecting | RPC secret mismatch | Verify all nodes share the same secret |
| `NoSuchBucket` | Bucket not created | Run init-buckets job or create manually |
| Slow uploads | Under-provisioned | Increase resources and storage class IOPS |
| Pod stuck `Pending` | No PV available | Check storage class and PV provisioner |
| `503 Service Unavailable` | Layout not applied | Assign node roles and apply layout |

## Uninstall

```bash
helm uninstall garage -n storage

# PVCs are NOT deleted automatically (data safety). To remove:
kubectl -n storage delete pvc -l app.kubernetes.io/instance=garage
```
