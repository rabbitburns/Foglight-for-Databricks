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
| Tier 7: Model Serving Endpoints + Served Models | 1.0.65 |
| Tier 8: Lakebase platform monitoring — project + branch inventory, endpoint state (3 projects, 5 branches confirmed) | 1.0.118 |
| Tier 11: AI Gateway Observability — endpoint metrics, daily token usage, per-requester activity (3 portlets, sub-nav page) | 1.0.119 |
| Instance Pool enhancements — pending idle/used counts, idle termination minutes, preloaded Spark versions | 1.0.124 |
| Cost & Usage dashboard — scroll, intra-day DBU accumulation time-plot, per-SKU 7-day trend table, bubble chart labels | 1.0.125 |
| All sub-nav dashboards scrollable | 1.0.125 |
| Query Volume Trend time-plot (`totalQueryCount` workspace metric, sum of per-warehouse counts each cycle) | 1.0.126 |
| Job Success Rate by Day table — per-job daily success %, 7d totals, trend vs prior 7 days | 1.0.126 |
| Tier 6: Cluster runtime metrics — CPU/memory utilisation from `system.compute.node_timeline`; cpuUtil/memUtil Metrics + percentage strings on DatabricksCluster | 1.0.140 |
| Cluster Utilization portlet (view 77) with CPU%/Mem% columns and sparklines | 1.0.140 |
| Job, Warehouse, Cluster sparkline portlets (views 74, 75, 76) | 1.0.136 |
| Cost columns added to Top Jobs by DBU (view 29), DLT Pipelines (view 17), AI Gateway Endpoints (view 59) | 1.0.143 |
| Cluster hygiene: Idle Clusters (view 78) + Untagged Clusters (view 79) | 1.0.143 |
| Dollar cost formatting fixed: `$%,.2f` ($ prefix, comma thousands, 2 decimal places) throughout | 1.0.147–1.0.148 |
| Cost by Product Treemap: treemap now shows dollar cost (not DBU) for cell area and hover | 1.0.148 |
| SQL Warehouse efficiency score: queryCount / sizeWeight, "Idle" if no queries | 1.0.149 |
| User Compute Spend: `DatabricksUserSpend` topology type; per-user per-product DBU+cost portlet (view 80) | 1.0.150 |
| Table Optimization History: `DatabricksOptimizationOp` type; Delta ANALYZE/COMPACTION ops, 7d (view 81) | 1.0.158 |
| Storage Costs: `DatabricksStorageCost` type; STORAGE_SPACE billing by product/SKU, 30d (view 82) | 1.0.158 |
| Cost MoM Slopegraph: prev vs current month cost by product, ranked, with ▲/▼/→ trend indicators (view 83) | 1.0.158 |
| Warehouse cold-start fix: SQL poll timeout increased from 30s to ~140s (15→60 poll attempts) | 1.0.154 |
| Tufte Unicode bars/sparklines: cluster util bars (▉░), job success sparklines (▁▂▃▄▅▆▇█), duration range, SKU spend shape, daily DBU bar, user cost bar | 1.0.159 |
| Cluster utilization fix: node_timeline lookback 5min→1hr; -1.0 sentinel for no-data clusters | 1.0.160 |
| Lakehouse Monitor Inventory: `DatabricksMonitor` type; `system.quality.monitor_run_timeline` query; Data Quality sub-nav (view 84) | 1.0.161 |
| Fix user spend SQL: `usage_metadata.run_as` → `identity_metadata.run_as_user`; add error detail logging to all FAILED SQL states | 1.0.162 |
| Fix monitor collection: `system.quality.monitor_run_timeline` does not exist; replaced SQL with REST API (`/api/2.1/lakehouse-monitoring/monitors` + `/refreshes`) | 1.0.163 |

---

## Backlog

### Near-term / Carry-forward

| Item | Notes |
|---|---|
| **AUI nav icon** | Icons are keyed by WCF module ID in a hardcoded lookup map inside the AUI Angular bundle (`main.*.js`). No cartridge mechanism exists — workaround is replacing `custom.svg` at `C:\Quest\Foglight\state\tomcat\webapps\aui\assets\images\icons\custom.svg`. Seeking guidance from platform team. |
| **Sparkline verification** | New sparkline portlets (74–76) need at least 2+ collection cycles of history before the mini-charts populate. Verify after agent restart with 1.0.136+. |
| **Sparkline: workspace totalDailyDbu** | `DatabricksWorkspace.totalDailyDbu` is already a Metric sampled each cycle. Consider adding a single-row view or embedding it in the Overview composite rather than a table (only 1 workspace). |
| **Cluster runtime metrics (Tier 6)** | ✓ Done 1.0.140 — cpuUtil/memUtil Metrics on DatabricksCluster; Cluster Utilization portlet (view 77). |

---

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
| Cost per job / per run | Join `system.billing.usage` with job run data | High | ✓ Done 1.0.143 — dollar cost column in Top Jobs by DBU; also added to DLT Pipelines and AI Gateway Endpoints portlets. |
| SKU pricing table | `system.billing.list_prices` table | Low | ✓ Done 1.0.63 — DatabricksSkuPrice topology type, SKU List Prices WCF portlet (view 41/script 42). |

### Tier 5 — DLT Pipeline Depth

| Gap | API Source | Effort | Notes |
|---|---|---|---|
| Pipeline update history | `GET /api/2.0/pipelines/{id}/events` | Medium | ✓ Done 1.0.64 — DatabricksPipelineUpdate type, Pipeline Updates WCF portlet (view 43/script 44). **Untested: DLT not enabled in dev workspace.** |
| Data quality expectations | Same events endpoint — expectation results | Medium | ✓ Done 1.0.64 — DatabricksPipelineExpectation type, Pipeline Data Quality WCF portlet (view 45/script 46). **Untested: DLT not enabled in dev workspace.** |
| Pipeline flow metrics | Same events endpoint — per-flow stats | Medium | Deferred — backlog bytes, file counts, output rows per flow stage. Lower priority than expectations. |

### Tier 6 — Cluster Runtime Metrics ✓ Done 1.0.140

> **Revised approach:** The Databricks UI cluster metrics page (CPU, memory, network) is sourced from Ganglia on port 8652 inside the cluster — not reachable by an external FglAM agent. However, `system.compute.node_timeline` (Unity Catalog, GA) exposes the same per-node CPU/memory data via SQL warehouse at 1-minute granularity with 30-day retention. We already use this pattern for billing data. Unity Catalog is confirmed enabled.

| Gap | API Source | Effort | Notes |
|---|---|---|---|
| Per-node CPU & memory utilisation | `system.compute.node_timeline` via SQL warehouse | Medium | ✓ Done 1.0.140 — aggregated per cluster_id; cpuUtil/memUtil Metrics + percentage string on DatabricksCluster. |
| Cluster-level CPU/memory summary | Aggregated from `node_timeline` (AVG/MAX per cluster_id) | Low | ✓ Done 1.0.140 — Cluster Utilization portlet (view 77) with CPU%/Mem% columns and sparklines. |
| Cluster config & state history | `system.compute.clusters` via SQL warehouse | Low | Deferred — historical state transitions. Lower priority vs other work. |
| Spark executor / task metrics | Spark REST API on running cluster (`/api/v1/applications`) | Very High | Requires network access to cluster driver on port 4040. Out of scope — not reachable by FglAM agent in standard deployments. |
| Streaming metrics | Spark Streaming REST API | Very High | Same access constraints as executor metrics. Deferred. |

### Tier 7 — Model Serving

| Gap | API Source | Effort | Notes |
|---|---|---|---|
| Model serving endpoints | `GET /api/2.0/serving-endpoints` | Low | ✓ Done 1.0.65 — DatabricksServingEndpoint topology type, Model Serving Endpoints portlet (view 47/script 48). |
| Served model detail | `config.served_models` / `config.served_entities` per endpoint | Low | ✓ Done 1.0.65 — DatabricksServedModel topology type, Served Models portlet (view 49/script 50). |
| Model serving metrics | Databricks metrics API (per-endpoint) | Medium | Request count, latency percentiles (p75/p90/p95/p99), 4xx/5xx counts, CPU/GPU/memory usage. Deferred — requires separate metrics API call per endpoint. |

### Tier 8 — Lakebase (Managed PostgreSQL) ✓ Done 1.0.118

> Lakebase is Databricks' serverless managed PostgreSQL (announced 2025). The Foglight PostgreSQL cartridge handles per-branch query-level monitoring (one agent per branch, by design). This tier covers the **platform layer** — what Lakebase resources exist and their provisioning health — via the Databricks REST API at `/api/2.0/postgres/`.
>
> **API note:** The JSON response from `/api/2.0/postgres/projects` (and `/branches`) nests the fields `project_id`, `display_name`, and `branch_id` under a `status` sub-object — not at the top level. The `name` field at the top level is a resource path (e.g. `projects/abc123`), not the display name.

| Gap | API Source | Effort | Notes |
|---|---|---|---|
| Project inventory | `GET /api/2.0/postgres/projects` | Low | ✓ Done 1.0.118 — project ID/name, state, endpoint URL stored. New type `DatabricksLakebaseProject`. |
| Branch inventory | `GET /api/2.0/postgres/projects/{id}/branches` | Low | ✓ Done 1.0.118 — branch ID/name, state, parent project, endpoint state/URL. New type `DatabricksLakebaseBranch`. |
| Endpoint status | `GET /api/2.0/postgres/projects/{id}/branches/{id}/endpoints` | Low | ✓ Done 1.0.118 — first endpoint per branch; endpoint state + URL stored on branch. |
| In-flight operations | `GET /api/2.0/postgres/projects/{id}/operations` | Medium | Deferred — async op type (create/clone/restore), state, duration. Surfaces stuck or failed provisioning. |

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
| Monitor inventory | `GET /api/2.1/lakehouse-monitoring/monitors` | Low | ✓ Done 1.0.163 — replaced broken SQL approach; REST API `listMonitors()` + `listMonitorRefreshes()` per monitor. New type `DatabricksMonitor`. |
| Monitor refresh status | Same API — `status` field per monitor | Low | ✓ Done 1.0.163 — last refresh time and run count stored on `DatabricksMonitor`. |
| Profile metrics (row count) | SQL against `_profile_metrics` output table | Medium | ✓ Done 1.0.180 — `MAX(window.start)` per table, `MAX(count)` where `column_name=':table'`; `rowCount` field on `DatabricksMonitor`; CDT transform added. |
| Drift metrics (column drift) | SQL against `_drift_metrics` output table | Medium | ✓ Done 1.0.181 — chi-square + KS test (`pvalue < 0.05`); `driftColumnCount` field on `DatabricksMonitor`; fix: field name is `pvalue` not `p_value` in Databricks schema. |
| Row count delta (change) | Derived — compare consecutive `_profile_metrics` runs | Low | ✓ Done 1.0.184 — `prevRowCounts` map on collector instance; delta shown as `+N`/`-N`/`0`; blank on first cycle after agent start. |
| Job → data quality correlation | Cross-reference `DatabricksJob` with monitored table output | High | **Deferred** — correlate job run failures with downstream drift detection. Differentiator vs Datadog/New Relic. |

### Tier 11 — AI Gateway Observability (Token & GenAI Usage) ✓ Done 1.0.119

> **Competitive note:** Token-level GenAI observability from the Databricks-native `system.ai_gateway.usage` system table — surfaced inside the same Foglight platform as DBU cost, query, pipeline, and Lakebase monitoring — is differentiated. Neither Datadog nor New Relic surfaces Databricks AI Gateway token economics natively. This extends the "Databricks-native, not cloud-billing-estimated" wedge into the fastest-growing workload on the platform. Tag-based attribution (project / team / cost-center) gives FinOps and platform buyers a per-team GenAI consumption view that complements existing DBU rollups.

| Gap | API Source | Effort | Notes |
|---|---|---|---|
| AI Gateway endpoint inventory | Aggregate `endpoint_name` from `system.ai_gateway.usage` | Low | ✓ Done 1.0.119 — `DatabricksAiEndpoint` type; current-window rollup: request count, total tokens, error rate, p95 latency. |
| Token consumption rollup | `SUM(total/input/output tokens)` by date × endpoint × model | Low | ✓ Done 1.0.119 — `DatabricksAiUsage` type; daily aggregated rows. Direct analogue of `DatabricksUsage`. |
| Per-requester activity | `GROUP BY requester, requester_type` | Low | ✓ Done 1.0.119 — `DatabricksAiUserActivity` type; request count, total tokens, error count per requester. |
| Endpoint performance metrics | `approx_percentile(latency_ms, …)`, status-code rollup in SQL | Medium | Deferred — p50/p90/p95/p99 latency, TTFB, 4xx/5xx error counts per endpoint. |
| Tag-based usage attribution | `request_tags['…']`, `endpoint_tags['…']` columns | Low | Deferred — per-team/project/cost-center token rollups. |
| Overview summary row | Derived from `DatabricksAiUsage` | Low | Deferred — "AI Gateway (This Month)" row in Overview: total tokens, top endpoint/model, request count. |
| Token → dollar cost attribution | Join to `system.billing.usage` model-serving/foundation-model SKU records | High | **Deferred.** Same join complexity as Tier 4 cost-per-job. |

**Prerequisites:** Unity AI Gateway V2 Preview enabled (account Previews toggle). **Account-admin access required** for `system.ai_gateway.usage` — stricter than `system.billing.*`. Collector degrades gracefully if preview is disabled or access is revoked.

---

## Priority Order (agreed)

1. **Tier 1** — Job & cluster depth ✓ (mostly complete — multi-workspace deferred)
2. **Tier 2** — Dashboards & packaging (nav module ✓, landing page ✓, portlets ✓)
3. **Tier 3** — SQL Warehouse query metrics ✓
4. **Tier 4** — DBU consumption & cost ✓ (mostly complete — cost-per-job deferred)
5. **Tier 5** — DLT Pipeline depth ✓ (built 1.0.64, untested — DLT not in dev workspace)
6. **Tier 7** — Model Serving ✓ (1.0.65)
7. **Tier 8** — Lakebase platform monitoring ✓ (1.0.118, CONFIRMED: 3 projects, 5 branches)
8. **Tier 11** — AI Gateway Observability ✓ Done 1.0.119 (3 portlets, sub-nav page; deferred items: tag attribution, overview row, dollar cost join)
9. **Tier 6** — Cluster runtime metrics via `system.compute.node_timeline` ✓ Done 1.0.140
10. **Tier 10 Phase 1** — Lakehouse Monitoring: monitor inventory ✓ 1.0.163; DQ portlet row count ✓ 1.0.180; drift column count ✓ 1.0.181; row count delta (Change column) backlog
11. **Tier 10 Phase 2** — Lakehouse Monitoring: job → data quality correlation (**deferred**)
12. **Tier 9** — Lakewatch SIEM (**blocked: Private Preview, no public API**)

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

| Page | Key additions over WCF | WCF approximation |
|---|---|---|
| Overview | Sparklines for query volume, job success rate trend | Query volume time-plot (view 69) + Job success rate table (view 71) added to sub-navs in 1.0.126 |
| Clusters | State history timeline, memory/core utilization charts | Requires Tier 6 (node_timeline data) — no WCF approximation |
| Jobs | Job run Gantt timeline, success rate trend chart | Job Success Rate by Day table (view 71) in 1.0.126 |
| SQL Warehouses | Query volume over time, warehouse utilization heatmap | Query Volume Trend time-plot (view 69) in 1.0.126 |
| Queries | Query duration distribution, per-user trend charts | Query Volume Trend time-plot (view 69) in 1.0.126 |
| DBU / Cost | Spend trend charts, cost-by-job bar chart | SKU 7-day trend table (view 65) + intra-day accumulation time-plot (view 67) in 1.0.125 |

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
| 1.0.67–1.0.74 | Time-plot trend views: Active Resource Trend (id=53) — activeClusterCount, clusterCount, activeWarehouseCount, warehouseCount; Job & Pipeline Count Trend (id=54) — jobCount, pipelineCount; query id=52 selects DatabricksWorkspace |
| 1.0.75–1.0.78 | Landing page layout: single-column wcf.grid2, 5 views stacked full-width (Overview → Bubble → Treemap → Trend 53 → Trend 54); `align=stretch`, `showTitle=true`, `<width preferred="0"/>` sizing |
| 1.0.79–1.0.113 | CDT diagnostics and stability fixes: DOCTYPE restoration in cdt.xml (parsing failure); StringObservation → plain String for all state fields (runtime type mismatch); same-version reinstall CDT skip documented. No feature changes. |
| 1.0.114 | Tier 8 Lakebase: DatabricksLakebaseProject + DatabricksLakebaseBranch topology types; REST collector calling /api/2.0/postgres/projects → /branches → /endpoints; WCF sub-module databricks_lakebase with Projects (view 55) and Branches (view 57) portlets; Lakebase row added to Overview. **Build process bugs present — agent code did not execute.** |
| 1.0.115–1.0.116 | Debug iterations: confirmed FglAM was loading cached April-compiled JAR (not new code). Root causes identified: (1) Maven compile not run before packaging; (2) AGENT_VER hardcoded "1.0.6" so FglAM never fetched new package; (3) --deploy targeted deploy/deployed/ staging area, not actual agent cache at agents/DatabricksAgent/<ver>/lib/. |
| 1.0.117 | Build process fully fixed: auto Maven compile in build_cartridge.py; AGENT_VER synced to VERSION; agent.manifest ver/build-id injected at build time; --deploy now targets correct FglAM agent cache dir. JSON parsing fix: project_id/display_name/branch_id are nested under status sub-object in API response. Lakebase collection running. |
| 1.0.118 | Clean production build: debug System.out.println removed; Lakebase CONFIRMED working (3 projects, 5 branches, all API calls succeeding in ~150ms). |
| 1.0.119 | Tier 11 AI Gateway Observability: `DatabricksAiEndpoint`, `DatabricksAiUsage`, `DatabricksAiUserActivity` topology types; SQL queries against `system.ai_gateway.usage`; 3 WCF portlets (AI Endpoints, Token Usage, User Activity); `databricks_ai_gateway` sub-nav module. |
| 1.0.121 | Fix sub-module nav registration (height preferred="0" required for wcf.grid2 sub-nav composite-views); fix AI Gateway SQL column names. |
| 1.0.122 | Agent properties (ASP / Edit Properties) now take priority over `databricks.properties` file for all config keys. |
| 1.0.123 | Instance pool improvements; clusters view cleanup. |
| 1.0.124 | Instance Pool API fields: `pendingIdleCount`, `pendingUsedCount`, `idleTerminationMinutes` (Metric), `preloadedSparkVersions` (String). New columns in Instance Pools portlet. |
| 1.0.125 | Cost & Usage dashboard redesign: `scroll:true` on all composite-views; intra-day DBU accumulation time-plot (`totalDailyDbu` Metric sampled each cycle, view 67); SKU 7-day spend trend table (view 65, script 66, `DatabricksSkuSparkRow` type); bubble chart `showLabels:true`. All 8 sub-nav dashboards made scrollable. |
| 1.0.126 | `totalQueryCount` workspace Metric (sum of per-warehouse queryCount each collection cycle); Query Volume Trend time-plot (view 69) added to SQL Warehouses and Queries sub-navs; Job Success Rate by Day table (view 71, script 70, `DatabricksJobTrendRow` type) added to Jobs sub-nav — per-job daily success rate for last 7 days sorted worst-first. |
| 1.0.127–1.0.132 | WCF structural fixes and validate_wcf tooling; AUI nav icon investigation (hardcoded in Angular bundle keyed by module ID — no cartridge mechanism; fallback is `custom.svg`). |
| 1.0.133 | Fix bubble chart labels: all `store()` calls on `wcf:String`/`wcf:Number`/`wcf:Color` DataObject properties changed to `set()` — `store()` is only valid for `wcf:Metric` time-series. Affected: 34.groovy (treemap), 36.groovy (cost bubble), 40.groovy (user activity bubble), 66.groovy (SKU trend). |
| 1.0.134 | Fix treemap `fillColor`: `DatabricksTreeMapNode.fillColor` changed from `wcf:String` → `wcf:Color`; 34.groovy now uses `Color.decode()`. Fix bubble chart mouseover: added `<on action="dwell">` popup handler (view 73) to both bubble views (35, 39) — `wcf.html-chart.scatter.bubble` tooltips require a dwell popup, not `label` property. DBU Spend Trend table made full-width. |
| 1.0.135 | Fix treemap blank: `DatabricksTreeMapNode.count` changed from `wcf:String` → `wcf:Number` — `wcf.treemap`'s `displayNumber` must be numeric for cell sizing. 34.groovy passes raw double. |
| 1.0.136 | Metric-backed sparkline portlets: `DatabricksJob.lastRunDuration` Metric added to topology; autoscaling clusters now write `numWorkers` metric; 3 new portlets using `system:oscommon.86` sparkline renderer — Job Sparklines (successRate, avgDurationMs, lastRunDuration), Warehouse Sparklines (queryCount, numClusters), Cluster Sparklines (numWorkers). |
| 1.0.137 | Fix Cost & Usage treemap: `wcf.treemap` requires full-width container to compute cell sizes; separated treemap and bubble from shared two-column row into their own full-width rows in `databricks_cost` composite. |
| 1.0.138–1.0.139 | Fix treemap blank on Cost & Usage page; full-width layout fix. |
| 1.0.140 | Tier 6: Cluster runtime metrics — `system.compute.node_timeline` SQL query; cpuUtil/memUtil Metrics + cpuUtilStr/memUtilStr percentage strings on DatabricksCluster; Cluster Utilization portlet (view 77) with CPU%/Mem% columns and sparklines. |
| 1.0.141–1.0.142 | Cluster Sparklines (view 76) added to Clusters sub-nav; wcf.grid2 row property fix (`<property name="row">` in config, not XML attribute). |
| 1.0.143 | Cost attribution fast-follows: dollar cost column on Top Jobs by DBU (view 29), DLT Pipelines (view 17), AI Gateway Endpoints (view 59); Idle Clusters report (view 78); Untagged Clusters report (view 79); DatabricksUserSpend topology type; cluster hygiene views added to Clusters sub-nav. |
| 1.0.144–1.0.146 | Dollar cost formatting: all costs now `$%,.2f` ($ prefix, comma thousands, 2 decimal places). Fix ClusterCollector line 885 (`$%.4f` → `$%,.2f`). |
| 1.0.147–1.0.148 | Fix Cost by SKU view (script 32): parse with `.replace('$','').replace(',','')` before `parseDouble`; format with `$%,.2f`. Cost by Product Treemap (script 34): switch from DBU to dollar cost for cell area + hover value; renamed "Databricks Cost by Product (Treemap)". |
| 1.0.149 | SQL Warehouse efficiency column: `queryCount / sizeWeight` (2X-Small=1 … 4X-Large=256); "Idle" if 0 queries. |
| 1.0.150 | User Compute Spend: `DatabricksUserSpend` topology type + CDT pattern; per-user per-product DBU+cost query in ClusterCollector; User Compute Spend portlet (view 80, script 80) added to Cost & Usage sub-nav. |
| 1.0.151–1.0.157 | Table Optimization History (view 81) + Storage Costs (view 82) added to Cost & Usage; Cost MoM Slopegraph (view 83) with ▲/▼/→ trend indicators. Bug fixes: wcf.html SVG escaping (reverted to row-table); view ordering violation; missing `<flow/>` and role entries causing menu collapse. |
| 1.0.158 | Stable commit: storage portlets (81, 82), slopegraph (83), poll timeout fix. |
| 1.0.159 | Tufte Unicode visualization pass: cluster utilization bars (████░░░░ + %), job success rate sparklines (▁▂▃▄▅▆▇█), duration range column (min – avg – max), SKU spend shape sparkline, daily DBU proportional bar, user cost proportional bar. View column headers cleaned up. |
| 1.0.160 | Cluster utilization data fix: `node_timeline` lookback widened 5min → 1hr; -1.0 sentinel distinguishes no-data from 0% utilization; metric guard prevents negative long cast. |
| 1.0.161 | Tier 10 Lakehouse Monitoring: `DatabricksMonitor` topology type + CDT pattern; `system.quality.monitor_run_timeline` collection in ClusterCollector; Data Quality sub-nav module (`databricks_quality`); Monitor Inventory portlet (view 84, script 84). |
| 1.0.162 | Fix user spend SQL: `u.usage_metadata.run_as` → `u.identity_metadata.run_as_user` (field was failing every cycle); add error detail (`.status.error.message`) to all FAILED-state log lines; add failure logging to monitor query non-SUCCEEDED path. |
| 1.0.163 | Fix monitor collection: `system.quality.monitor_run_timeline` does not exist in Databricks. Replaced SQL approach with REST API — `listMonitors()` + `listMonitorRefreshes(tableName)` per monitor. Collection no longer requires a billing warehouse. |
| 1.0.164–1.0.178 | Iterative fixes: `MAX(run_time)` → `MAX(window.start)` in profile UNION SQL; async agent activation (`scheduleWithFixedDelay` with `initialDelay=0`, removes synchronous `collect()` call that caused activation timeout); CDT missing transforms for `rowCount`/`rowCountDelta`/`driftColumnCount` (root cause of blank DQ columns); drift window widened 7→14 days. |
| 1.0.179 | CDT transform fix confirmed working — Row Count column now populates in Data Quality portlet (view 84). |
| 1.0.180 | Drift diagnostic logging: `drift state=` logged after SQL poll to diagnose 0-row result. `cartridges/` folder added to repo — `.car` committed to git after each build for distribution without GitHub-hosted runners. |
| 1.0.181 | Fix drift metrics field name: `chi_square_test.p_value` / `ks_test.p_value` → `chi_square_test.pvalue` / `ks_test.pvalue`. Databricks `_drift_metrics` schema uses `pvalue` (no underscore); query was failing BAD_REQUEST every cycle, silently leaving Drifted Cols blank. |
| 1.0.182 | Fix drift query for numeric-only monitors: `chi_square_test` column is absent from `_drift_metrics` tables where all monitored columns are numeric (only `ks_test` present). On UNRESOLVED_COLUMN failure mentioning `chi_square_test`, retry with `ks_test.pvalue` only. Drifted Cols confirmed populating. |
| 1.0.183 | Add row count delta (Change column): `prevRowCounts` map on `ClusterCollector` instance tracks last seen row count per table; delta computed each cycle as `+N`/`-N`/`0`. Blank on first cycle after agent start (seeding). |
| 1.0.184 | Fix delta parsing: replace `asLong(-1)` with `Long.parseLong(lastCount)` — Jackson's `asLong()` on a TextNode (all Databricks SQL results are returned as strings) does not reliably parse the string value. Change column confirmed showing `0` for stable tables. |
