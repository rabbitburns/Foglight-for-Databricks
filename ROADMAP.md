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
| Databricks nav module — top-level nav entry, Job Runs as landing page | 1.0.39 |
| SQL Warehouse query history, slow query leaderboard, user activity portlets | 1.0.40–1.0.42 |

---

## Backlog

### Tier 1 — Job & Cluster Depth (remaining)

| Gap | API Source | Effort | Notes |
|---|---|---|---|
| Job run task-level detail | `tasks[]` array in run response (`expand_tasks=true`) | Low | **Deferred** — waiting on drill-down dashboards (Tier 2). `DatabricksJobTask` type ready to add. |
| Multi-workspace support | Config change + agent instance per workspace | Medium | **Deferred** — low priority for now; single workspace covers most use cases. Numbered config pairs (`workspace.1.url`, etc.) is the planned approach. |

### Tier 2 — Dashboards & Packaging (remaining)

| Gap | Effort | Notes |
|---|---|---|
| Composite landing page for Databricks nav entry | Medium | **Next** — package a multi-portlet landing page (Job Runs + Query History + User Activity) as the module main-view. Replicates Databricks #1/2 dashboards in the cartridge. |
| Drill-down dashboards (cluster, job, warehouse detail) | Medium | **Deferred** — hold until more data sources (Tier 3+) are collected. |
| Packaged dashboard in cartridge | Medium | **Deferred** — hold until dashboard design is stable. |
| Pre-built alert rules | Medium | **Deferred** — best practices research needed before implementation. |

### Tier 3 — SQL Warehouse Query Metrics ✓

| Gap | API Source | Effort | Notes |
|---|---|---|---|
| Query history per warehouse | `GET /api/2.0/sql/history/queries` | Medium | ✓ Done 1.0.40 — query ID, user, status, duration, bytes read, rows, cache hit. |
| Query execution breakdown | Same response — `compilation_time`, `execution_time`, `fetch_time` | Low | ✓ Done 1.0.40 — included in query history fetch. |
| Warehouse query volume (count per interval) | Derived from query history | Low | ✓ Done 1.0.40 — `queryCountStr` on each warehouse. |
| Query History portlet | WCF | Low | ✓ Done 1.0.41 — flat cross-warehouse query table. |
| Slow Query leaderboard portlet | WCF | Low | ✓ Done 1.0.41 — top 25 by duration. |
| User Activity summary portlet | WCF | Low | ✓ Done 1.0.41 — per-user query count, avg duration, bytes, cache hits, errors. |

### Tier 4 — DBU Consumption & Cost

> **Competitive note:** Datadog's cost visibility for Databricks is part of their separate Cloud Cost Management product, which pulls from cloud provider billing APIs (AWS Cost Explorer, Azure Cost Management) — it is not included in standard Databricks monitoring and requires an additional paid SKU. Our approach via `system.billing.usage` is Databricks-native, more granular (DBU-denominated rather than dollar-estimated), and included in the base cartridge. New Relic has cost dashboards using the same source but requires a Databricks system table license. This is a meaningful differentiator for the Foglight value proposition — worth developing for the sales narrative.

| Gap | API Source | Effort | Notes |
|---|---|---|---|
| DBU usage by SKU | `system.billing.usage` table via SQL warehouse query | Medium | ✓ Done 1.0.48 — 30-day window, grouped by date/SKU/product/cloud/region. DatabricksUsage topology type. DBU Usage portlet. |
| DBU by product, daily trend, MoM growth, cost by SKU | Derived from DatabricksUsage topology | Low | ✓ Done 1.0.54 — 5 table portlets; DBU summary row in Overview (1.0.58). |
| Graphical DBU widgets | WCF treemap + bubble components | Low | ✓ Done 1.0.62 — DBU by Product treemap, Cost vs DBU bubble, User Activity treemap + bubble. |
| Cost per job / per run | Join `system.billing.usage` with job run data | High | Requires matching cluster IDs to billing records. Very high value for FinOps use cases but complex to implement correctly. |
| SKU pricing table | `system.billing.list_prices` table | Low | ✓ Done 1.0.63 — DatabricksSkuPrice topology type, SKU List Prices WCF portlet (view 41/script 42). |

### Tier 5 — DLT Pipeline Depth

| Gap | API Source | Effort | Notes |
|---|---|---|---|
| Pipeline update history | `GET /api/2.0/pipelines/{id}/events` | Medium | ✓ Done 1.0.64 — DatabricksPipelineUpdate type, Pipeline Updates WCF portlet (view 43/script 44). **Untested: DLT not enabled in dev workspace.** |
| Data quality expectations | Same events endpoint — expectation results | Medium | ✓ Done 1.0.64 — DatabricksPipelineExpectation type, Pipeline Data Quality WCF portlet (view 45/script 46). **Untested: DLT not enabled in dev workspace.** |
| Pipeline flow metrics | Same events endpoint — per-flow stats | Medium | Deferred — backlog bytes, file counts, output rows per flow stage. Lower priority than expectations. |

### Tier 6 — Cluster Runtime Metrics

> **Revised approach:** The Databricks UI cluster metrics page (CPU, memory, network) is sourced from Ganglia on port 8652 inside the cluster — not reachable by an external FglAM agent. However, `system.compute.node_timeline` (Unity Catalog, GA) exposes the same per-node CPU/memory data via SQL warehouse at 1-minute granularity with 30-day retention. We already use this pattern for billing data. Unity Catalog is confirmed enabled.

| Gap | API Source | Effort | Notes |
|---|---|---|---|
| Per-node CPU & memory utilisation | `system.compute.node_timeline` via SQL warehouse | Medium | 1-min granularity, 30-day retention. Queryable via same warehouse we use for billing. New topology type `DatabricksNodeMetric`. Driver vs worker breakdown available. |
| Cluster-level CPU/memory summary | Aggregated from `node_timeline` (AVG/MAX per cluster_id) | Low | Rolled up from per-node data — avg CPU %, peak memory % per cluster over configurable window. |
| Cluster config & state history | `system.compute.clusters` via SQL warehouse | Low | Historical record of cluster state transitions, config changes, creator, cloud provider attrs. Complements current snapshot-only cluster topology. |
| Spark executor / task metrics | Spark REST API on running cluster (`/api/v1/applications`) | Very High | Requires network access to cluster driver on port 4040. Out of scope — not reachable by FglAM agent in standard deployments. |
| Streaming metrics | Spark Streaming REST API | Very High | Same access constraints as executor metrics. Deferred. |

### Tier 7 — Model Serving

| Gap | API Source | Effort | Notes |
|---|---|---|---|
| Model serving endpoints | `GET /api/2.0/serving-endpoints` | Low | ✓ Done 1.0.65 — DatabricksServingEndpoint topology type, Model Serving Endpoints portlet (view 47/script 48). |
| Served model detail | `config.served_models` / `config.served_entities` per endpoint | Low | ✓ Done 1.0.65 — DatabricksServedModel topology type, Served Models portlet (view 49/script 50). |
| Model serving metrics | Databricks metrics API (per-endpoint) | Medium | Request count, latency percentiles (p75/p90/p95/p99), 4xx/5xx counts, CPU/GPU/memory usage. Deferred — requires separate metrics API call per endpoint. |

### Tier 8 — Lakebase (Managed PostgreSQL)

> Lakebase is Databricks' serverless managed PostgreSQL (announced 2025). The Foglight PostgreSQL cartridge handles per-branch query-level monitoring (one agent per branch, by design). This tier covers the **platform layer** — what Lakebase resources exist and their provisioning health — via the Databricks REST API at `/api/2.0/postgres/`.

| Gap | API Source | Effort | Notes |
|---|---|---|---|
| Project inventory | `GET /api/2.0/postgres/projects` | Low | Project name, state, created/modified timestamps. New type `DatabricksLakebaseProject`. |
| Branch inventory | `GET /api/2.0/postgres/projects/{id}/branches` | Low | Branch name, state, parent branch, created time. New type `DatabricksLakebaseBranch`. |
| Endpoint status | `GET /api/2.0/postgres/projects/{id}/branches/{id}/endpoints` | Low | Endpoint state (provisioning/running/stopped), size, endpoint URL. |
| In-flight operations | `GET /api/2.0/postgres/projects/{id}/operations` | Medium | Async op type (create/clone/restore), state, duration. Surfaces stuck or failed provisioning. |

### Tier 9 — Lakewatch (Security SIEM)

> Lakewatch is Databricks' open, agentic SIEM platform (announced March 2026, currently Private Preview). It ingests 100% of security telemetry (AWS, Okta, Zscaler, etc.), normalises to OCSF, stores in Delta Lake / Iceberg, and uses an AI agent ("Genie") for detection authoring and triage. No public REST API is available yet — the product is driven by SQL queries against ingested security tables rather than traditional metrics endpoints.
>
> **Status: Blocked — waiting for GA and public API documentation.**

| Gap | API Source | Effort | Notes |
|---|---|---|---|
| Security event ingestion status | TBD — no public API in Private Preview | High | Would surface connector health, ingest lag, OCSF normalisation errors. |
| Detection / alert inventory | TBD | Medium | List active detections, last fired, severity distribution. |
| Incident summary | TBD | Medium | Open incidents, MTTD/MTTR metrics. High value for a SOC-facing Foglight dashboard. |

> **Note:** Lakehouse Monitoring (data quality observability for Delta tables, GA today) is a separate product — see Tier 10.

### Tier 10 — Lakehouse Monitoring (Data Quality Observability)

> Lakehouse Monitoring attaches quality monitors to Delta tables and writes metric results to output Delta tables. Two data sources are available: the REST API for monitor inventory and status, and SQL queries against the output tables for actual quality and drift metrics. Potential to correlate with existing job and warehouse topology — e.g. job failure → downstream data quality degradation.

| Gap | API Source | Effort | Notes |
|---|---|---|---|
| Monitor inventory | `GET /api/2.1/lakehouse-monitoring/monitors` | Low | Which tables are monitored, monitor type (snapshot/timeseries/inference), schedule, last refresh time, output table locations. New type `DatabricksLakehouseMonitor`. |
| Monitor refresh status | Same API — `status` field per monitor | Low | Surfaces monitors that are failing or have stale refreshes. |
| Profile metrics | SQL query against `profile_metrics` output table via warehouse | Medium | Column-level stats: null %, distinct count, min/max/mean. Surfaces tables with data quality issues. |
| Drift metrics | SQL query against `drift_metrics` output table via warehouse | Medium | Statistical drift vs baseline (JS divergence, distribution % change). Surfaces columns where data has shifted unexpectedly. |
| Job → data quality correlation | Cross-reference `DatabricksJob` with monitored table output | High | Correlate job run failures or anomalies with downstream drift detection. Differentiator vs Datadog/New Relic. |

---

## Priority Order (agreed)

1. **Tier 1** — Job & cluster depth ✓ (mostly complete — multi-workspace deferred)
2. **Tier 2** — Dashboards & packaging (nav module ✓, landing page ✓, portlets ✓)
3. **Tier 3** — SQL Warehouse query metrics ✓
4. **Tier 4** — DBU consumption & cost ✓ (mostly complete — cost-per-job deferred)
5. **Tier 5** — DLT Pipeline depth ✓ (built 1.0.64, untested — DLT not in dev workspace)
6. **Tier 7** — Model Serving ✓ (1.0.65)
7. **Tier 8** — Lakebase platform monitoring — **next** (unblocked)
8. **Tier 6** — Cluster runtime metrics via `system.compute.node_timeline` — **unblocked** (UC confirmed; revised from Ganglia approach)
9. **Tier 10 Phase 1** — Lakehouse Monitoring: monitor inventory + drift metrics (**on hold**)
10. **Tier 10 Phase 2** — Lakehouse Monitoring: job → data quality correlation (**on hold**)
11. **Tier 9** — Lakewatch SIEM (**blocked: Private Preview, no public API**)

---

## v2 — AUI Dashboard Layer

> WCF portlets are the v1 foundation. v2 replaces or supplements them with Foglight's AUI (Angular UI) framework, which is the current platform standard and offers significantly richer visualization options. All topology data collected by the Java agent carries forward unchanged — AUI is purely a UI layer change.

### Why AUI

| Capability | WCF (current) | AUI (v2) |
|---|---|---|
| Tables | ✓ row-table only | ✓ sortable, filterable, paginated |
| Charts | ✗ | ✓ line, bar, heatmap, sparklines |
| Timeline / Gantt | ✗ | ✓ job run timeline visualization |
| Drill-down navigation | ✗ manual dashboard links | ✓ native context passing |
| Layout | fixed portlet grid | flexible, responsive |
| Competitive parity | Datadog-style tables | Datadog/NR-style rich dashboards |

### v2 Target Pages

| Page | Key additions over WCF |
|---|---|
| Overview | Sparklines for query volume, job success rate trend |
| Clusters | State history timeline, memory/core utilization charts |
| Jobs | Job run Gantt timeline, success rate trend chart |
| SQL Warehouses | Query volume over time, warehouse utilization heatmap |
| Queries | Query duration distribution, per-user trend charts |
| DBU / Cost | Spend trend charts, cost-by-job bar chart (requires Tier 4) |

### Dependencies

- Requires familiarity with Foglight AUI component library (internal platform team resource recommended)
- Packaged dashboard export/import mechanism (currently deferred in Tier 2) becomes the right delivery vehicle for AUI dashboards in the cartridge
- Multi-workspace support (Tier 1 deferred) becomes more important at v2 — AUI org-level views would span workspaces

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
| 1.0.37 | WCF portlet (`databricks_jobruns`) — flat cross-job runs table; page + portlet purposes; wcf_support parent |
| 1.0.38 | Renamed WCF module to `databricks`; top-level nav entry with main-view; Job Runs table as landing page |
| 1.0.39 | Fixed WCF module load failure (composite-view not valid in module); Job Runs table is now the main-view directly |
| 1.0.40 | SQL Warehouse query history collection — DatabricksQuery type, per-warehouse query fetch |
| 1.0.41 | Query History, Slow Queries, User Activity WCF portlets |
| 1.0.42 | Role visibility fix — all portlets now show for Operator/Dashboard roles |
| 1.0.43 | Databricks Overview landing page (summary by resource category); Job Runs demoted from main-view |
| 1.0.44 | Five new portlets: Clusters, SQL Warehouses, Jobs, DLT Pipelines, Instance Pools; reportlet purpose on all views |
| 1.0.45 | Query text collection (Java collector + CDT + topology type); queryText column in Query History and Slow Queries |
| 1.0.46–1.0.47 | Fix: queryText column missing from Slow Queries view (WCF build timing issue) |
| 1.0.48 | Tier 4: DBU usage collection — DatabricksUsage topology type, system.billing.usage SQL query via warehouse, DBU Usage WCF portlet |
| 1.0.49–1.0.54 | Tier 4 WCF portlets: DBU by Product, Daily DBU Trend, MoM DBU Growth, Top Jobs by DBU, Cost by SKU |
| 1.0.55–1.0.57 | Fix: WCF structure and column path errors in Tier 4 portlets |
| 1.0.58 | Overview: DBU (This Month) summary row added — total DBU, top product, and estimated cost |
| 1.0.59 | Graphical: DBU by Product treemap (wcf.treemap) — DatabricksTreeMapNode type |
| 1.0.60 | Graphical: Cost vs DBU by SKU bubble chart (wcf.html-chart.scatter.bubble) — DatabricksBubbleNode type |
| 1.0.61 | Graphical: User Activity treemap and bubble chart |
| 1.0.62 | Fix: treemap layout bug — added component-sizing to treemap views |
| 1.0.63 | Tier 4: SKU List Prices — DatabricksSkuPrice topology type + WCF portlet (view 41, script 42) |
| 1.0.64 | Tier 5: DLT Pipeline Updates (view 43/44) + Pipeline Data Quality / Expectations (view 45/46) |
| 1.0.65 | Tier 7: Model Serving Endpoints (view 47/48) + Served Models (view 49/50) — DatabricksServingEndpoint, DatabricksServedModel topology types |
| 1.0.66 | Nav main-view wired as composite-view (wcf.grid2 id=51): overview table top + DBU treemap + Cost vs DBU bubble side-by-side |
