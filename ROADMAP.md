# Foglight for Databricks — Feature Roadmap

## Completed

| Feature | Version |
|---|---|
| Clusters (state, node types, Spark version, autoscale, workers, memory, cores, termination) | 1.0.20 |
| Jobs + last run summary (state, result, start, duration) | 1.0.24 |
| Job Runs (per-job, up to 10 most recent) | 1.0.25 |
| SQL Warehouses (state, type, size, cluster counts, Photon, auto-stop, creator) | 1.0.24 |
| DLT Pipelines (state, name, creator, run-as user) | 1.0.24 |
| Instance Pools (state, node type, idle/used/max counts) | 1.0.24 |
| Cluster enrichment (creator, start time, last activity, terminated time, pinned-by) | 1.0.27 |
| Job Run timing breakdown (queue/setup/execution/cleanup durations) | 1.0.27 |
| Job Run retry tracking (attempt number, isRetry, originalAttemptRunId, stateMessage) | 1.0.27 |

---

## Backlog

### Tier 1 — Job & Cluster Depth

| Gap | API Source | Effort | Notes |
|---|---|---|---|
| Job run task-level detail | `tasks[]` array already in run response (when `expand_tasks=true`) | Low | Each task has state, duration, cluster used, error message. Already fetched — just not extracted. Would add `DatabricksJobTask` type under each run. |
| Job tags / custom metadata | `job.settings.tags` in jobs list response | Low | Key-value pairs; useful for cost attribution and filtering by team/project. |
| Cluster custom tags | `cluster.custom_tags` in clusters list response | Low | Same use case — cost attribution, ownership. |
| Multi-workspace support | Config change + agent instance per workspace | Medium | Currently one workspace per agent instance. Config could support a list of workspace URLs, or deploy multiple agent instances. |
| Job trigger type on job (not run) | `job.settings.trigger` for file-arrival / table triggers | Low | Currently only `schedule` (cron) is captured. New trigger types introduced in Databricks 2023+. |

### Tier 2 — SQL Warehouse Query Metrics

| Gap | API Source | Effort | Notes |
|---|---|---|---|
| Query history per warehouse | `GET /api/2.0/sql/history/queries` | Medium | Returns query ID, user, status, duration, bytes read, rows, cache hit, queue wait. New type `DatabricksQuery` under each warehouse. High-value for performance monitoring. |
| Query execution breakdown | Same response — `compilation_time`, `execution_time`, `fetch_time` | Low | Comes free with query history fetch. |
| Warehouse query volume (count per interval) | Derived from query history | Low | Count of queries in collection window; good metric for warehouse utilization trending. |

### Tier 3 — DBU Consumption & Cost

| Gap | API Source | Effort | Notes |
|---|---|---|---|
| DBU usage by SKU | `system.billing.usage` table via SQL warehouse query | Medium | Requires an active SQL warehouse to execute the query. Returns SKU, quantity, cloud, region, custom tags. New Relic uses this for cost dashboards. Strong differentiator. |
| Cost per job / per run | Join `system.billing.usage` with job run data | High | Requires matching cluster IDs to billing records. Very high value for FinOps use cases but complex to implement correctly. |
| SKU pricing table | `system.billing.list_prices` table | Low | Static reference data; needed to convert DBU counts to dollar amounts. |

### Tier 4 — DLT Pipeline Depth

| Gap | API Source | Effort | Notes |
|---|---|---|---|
| Pipeline update history | `GET /api/2.0/pipelines/{id}/events` | Medium | Each pipeline update: status, duration, wait time, run time. New Relic covers this well. New type `DatabricksPipelineUpdate`. |
| Pipeline flow metrics | Same events endpoint — per-flow stats | Medium | Backlog bytes, file counts, output rows, dropped records per flow within an update. |
| Data quality expectations | Same events endpoint — expectation results | Medium | Pass/fail counts per expectation. Useful for data reliability monitoring — differentiator vs. Datadog. |

### Tier 5 — Cluster Runtime Metrics

| Gap | API Source | Effort | Notes |
|---|---|---|---|
| Spark executor metrics | Spark REST API on running cluster (`/api/v1/applications`) | High | CPU, memory, GC, shuffle per executor. Requires network access to the cluster driver. Not available via Databricks REST API. |
| Node-level CPU / memory / disk | Infrastructure agent installed via cluster init script | Very High | How New Relic and Datadog do it. Requires deploying an agent binary to every cluster node at startup. Out of scope for a Foglight agent approach. |
| Streaming metrics | Spark Streaming REST API | High | Input/processing rates, batch delay. Same access constraints as executor metrics. |

### Tier 6 — Model Serving

| Gap | API Source | Effort | Notes |
|---|---|---|---|
| Model serving endpoints | `GET /api/2.0/serving-endpoints` | Low | List of endpoints with state, creator, config. |
| Model serving metrics | Databricks metrics API (per-endpoint) | Medium | Request count, latency percentiles (p75/p90/p95/p99), 4xx/5xx counts, CPU/GPU/memory usage. Datadog covers this; New Relic does not. New type `DatabricksServingEndpoint`. |

### Tier 7 — Dashboards & Alerting

| Gap | Effort | Notes |
|---|---|---|
| Drill-down dashboards (cluster detail, job detail, warehouse detail) | Medium | Click a row → full detail view with sparklines and recent history. |
| Packaged dashboard in cartridge | Medium | Export from Foglight UI, embed XML in .car. Hold until dashboard design is stable. |
| Pre-built alert rules | Medium | Failed job alert, cluster stuck in PENDING, warehouse auto-stopped, long queue duration. Best practices research needed before implementation. |

---

## Priority Order (agreed)

1. **Tier 1** — Job & cluster depth (in progress)
2. **Tier 2** — SQL Warehouse query metrics
3. **Tier 3** — DBU consumption & cost
4. **Tier 4** — DLT Pipeline depth
5. **Tier 7** — Dashboards & alerting
6. **Tier 5/6** — Runtime metrics & model serving (lower priority, higher effort)
