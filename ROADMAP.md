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
| Cluster custom tags | 1.0.30 |
| Job Run timing breakdown (queue/setup/execution/cleanup durations) | 1.0.27 |
| Job Run retry tracking (attempt number, isRetry, originalAttemptRunId, stateMessage) | 1.0.27 |
| Job run stats per job (success rate %, avg/min/max duration, success/failure counts) | 1.0.29 |
| Job trigger type (FILE_ARRIVAL, TABLE, PERIODIC, etc.) | 1.0.30 |
| Job tags / custom metadata | 1.0.30 |
| Timestamp formatting (epoch → yyyy-MM-dd HH:mm:ss) on all time fields | 1.0.28 |
| Duration formatting (ms → "2m 34s") on all duration fields | 1.0.29 |
| Flat job runs table — Groovy script (text + WCF HTML) | 1.0.30 |
| WCF job runs portlet (`databricks_jobruns` module, `wcf.table.row-table`) | 1.0.33 |

---

## Backlog

### Tier 1 — Job & Cluster Depth (remaining)

| Gap | API Source | Effort | Notes |
|---|---|---|---|
| Job run task-level detail | `tasks[]` array in run response (`expand_tasks=true`) | Low | Deferred until drill-downs are built (Tier 7). `DatabricksJobTask` type ready to add. |
| Multi-workspace support | Config change + agent instance per workspace | Medium | Currently one workspace per agent instance. Config could support a list of workspace URLs. |

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
| Spark executor metrics | Spark REST API on running cluster (`/api/v1/applications`) | High | CPU, memory, GC, shuffle per executor. Requires network access to cluster driver. Not available via Databricks REST API. |
| Node-level CPU / memory / disk | Infrastructure agent via cluster init script | Very High | Out of scope for a Foglight agent approach — requires deploying an agent binary to every cluster node at startup. |
| Streaming metrics | Spark Streaming REST API | High | Input/processing rates, batch delay. Same access constraints as executor metrics. |

### Tier 6 — Model Serving

| Gap | API Source | Effort | Notes |
|---|---|---|---|
| Model serving endpoints | `GET /api/2.0/serving-endpoints` | Low | List of endpoints with state, creator, config. |
| Model serving metrics | Databricks metrics API (per-endpoint) | Medium | Request count, latency percentiles (p75/p90/p95/p99), 4xx/5xx counts, CPU/GPU/memory usage. Datadog covers this; New Relic does not. New type `DatabricksServingEndpoint`. |

### Tier 7 — Dashboards & Alerting

| Gap | Effort | Notes |
|---|---|---|
| WCF job runs / task flat table portlet | Medium | Groovy query written. Blocked on identifying correct WCF portlet type in Foglight 8.2 — investigating via existing cartridge inspection. |
| Drill-down dashboards (cluster, job, warehouse detail) | Medium | Click a row → full detail view with sparklines and recent history. Enables job run task detail from Tier 1. |
| Packaged dashboard in cartridge | Medium | Export from Foglight UI, embed XML in .car. Hold until dashboard design is stable. |
| Pre-built alert rules | Medium | Failed job alert, cluster stuck in PENDING, warehouse auto-stopped, long queue duration. Best practices research needed before implementation. |

---

## Priority Order (agreed)

1. **Tier 1** — Job & cluster depth ✓ (mostly complete — multi-workspace pending)
2. **Tier 2** — SQL Warehouse query metrics
3. **Tier 3** — DBU consumption & cost
4. **Tier 4** — DLT Pipeline depth
5. **Tier 7** — Dashboards & alerting (WCF portlet investigation in parallel)
6. **Tier 5/6** — Runtime metrics & model serving (lower priority, higher effort)

---

## Version History

| Version | Summary |
|---|---|
| 1.0.20 | Initial working topology — clusters, jobs, job runs |
| 1.0.24 | Added warehouses, pipelines, instance pools; clusterCountStr fix |
| 1.0.25 | Per-job run fetching (was bulk, starving most jobs of run history) |
| 1.0.26 | Debug logging for run fetch failures; removed expand_tasks |
| 1.0.27 | Cluster enrichment; job run timing breakdown; retry tracking |
| 1.0.28 | Timestamp formatting (epoch → readable date) |
| 1.0.29 | Job run stats (success rate, avg/min/max duration); runs limit 5→10 |
| 1.0.30 | Job trigger type; job tags; cluster custom tags |
| 1.0.31 | WCF Dashboard component added to cartridge build |
| 1.0.32 | Fixed WCF script filename (must match function id) |
| 1.0.33 | Fixed WCF module element order (views before script-functions per JiBX schema) |
