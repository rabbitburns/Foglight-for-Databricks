# Foglight for Databricks — Product Management Specification

**Version:** 1.0.181  
**Status:** POC / Active Development  
**Owner:** Quest Software  
**Last Updated:** 2026-06-24

---

## 1. Executive Summary

Foglight for Databricks is a native Quest Foglight monitoring cartridge that provides comprehensive observability for Databricks Lakehouse environments. It collects topology, operational metrics, cost intelligence, and data quality signals from the Databricks REST API and Unity Catalog system tables, and surfaces them as Foglight portlets and dashboards.

The product targets organisations running Databricks on Azure, AWS, or GCP who need unified monitoring of their Lakehouse platform within their existing Foglight investment — without deploying a separate monitoring tool or paying for add-on modules from cloud-native observability vendors.

---

## 2. Problem Statement

### 2.1 The Observability Gap

Databricks environments generate rich operational data — job failures, query slowdowns, runaway clusters, cost spikes, data quality degradation — but this data is fragmented across:

- The Databricks UI (workspace-scoped, no org-level view)
- Individual SQL warehouse query logs
- Databricks billing system tables (requires SQL query expertise to interrogate)
- DLT pipeline event logs (requires navigating individual pipeline UIs)

Organisations running Databricks alongside other monitored infrastructure (databases, servers, applications) have no single pane of glass.

### 2.2 Cost of Alternatives

| Alternative | Problem |
|---|---|
| Datadog for Databricks | Cost visibility requires a separate Cloud Cost Management SKU (paid add-on). Pulls from cloud provider billing APIs (not Databricks-native). No data quality monitoring. |
| New Relic | Cost dashboards use `system.billing.usage` but require a Databricks system table licence. No competitive advantage over our approach. |
| Native Databricks UI | Workspace-scoped only. No cross-workspace, no alerting integration, no correlation with other monitored systems. |
| No monitoring | Common in practice. Teams learn about job failures from data consumers, not proactively. |

### 2.3 Target Customer Profile

- Organisations with an active Quest Foglight deployment
- Running Databricks on any cloud (Azure, AWS, GCP)
- Data engineering teams (5–50 engineers) with jobs, pipelines, SQL warehouses in production
- FinOps or platform engineering teams with accountability for Databricks spend
- Organisations with a data reliability or data quality requirement

---

## 3. Product Overview

### 3.1 Architecture

```
Databricks REST API + Unity Catalog System Tables
        │
        ▼
FglAM Java Agent (DatabricksAgent)
  ClusterCollector.java — 60s polling interval
  DatabricksClient.java — REST API abstraction
        │
        ▼
Foglight Topology Store
  DatabricksModelRoot
  └── DatabricksAccount
      └── DatabricksWorkspace
          ├── DatabricksCluster (n)
          ├── DatabricksJob (n)
          │   └── DatabricksJobRun (up to 10/job)
          ├── DatabricksWarehouse (n)
          │   └── DatabricksQuery (up to 25/warehouse)
          ├── DatabricksPipeline (n)
          │   └── DatabricksPipelineUpdate (up to 5/pipeline)
          │       └── DatabricksPipelineExpectation (n)
          ├── DatabricksInstancePool (n)
          ├── DatabricksUsage (n)             ← billing/DBU data
          ├── DatabricksJobDbu (n)            ← per-job DBU
          ├── DatabricksSkuPrice (n)          ← list prices
          ├── DatabricksServingEndpoint (n)
          │   └── DatabricksServedModel (n)
          ├── DatabricksLakebaseProject (n)
          │   └── DatabricksLakebaseBranch (n)
          ├── DatabricksAiEndpoint (n)        ← AI Gateway endpoints
          ├── DatabricksAiUsage (n)           ← daily token aggregates
          ├── DatabricksAiUserActivity (n)    ← per-requester token rollup
          ├── DatabricksUserSpend (n)         ← per-user compute spend
          └── DatabricksMonitor (n)           ← Lakehouse Monitor DQ
        │
        ▼
WCF Portlets (~45 views, Groovy scripts)
Built-in nav entry: Databricks → 10 sub-pages
```

### 3.2 Data Sources

| Source | API / Table | Collection Interval |
|---|---|---|
| Clusters | `GET /api/2.0/clusters/list` | 60s |
| Cluster runtime metrics | `system.compute.node_timeline` via SQL warehouse | 60s |
| Jobs | `GET /api/2.1/jobs/list` | 60s |
| Job Runs | `GET /api/2.1/jobs/runs/list` (per job) | 60s |
| SQL Warehouses | `GET /api/2.0/sql/warehouses` | 60s |
| SQL Query History | `GET /api/2.0/sql/history/queries` (per warehouse) | 60s |
| DLT Pipelines | `GET /api/2.0/pipelines` | 60s |
| DLT Pipeline Events | `GET /api/2.0/pipelines/{id}/events` | 60s |
| Instance Pools | `GET /api/2.0/instance-pools/list` | 60s |
| DBU Usage | `system.billing.usage` via SQL warehouse | 60s |
| SKU List Prices | `system.billing.list_prices` via SQL warehouse | 60s |
| Table Optimization History | `system.storage.predictive_optimization_operations_history` | 60s |
| Storage Costs | `system.billing.usage` (STORAGE_SPACE SKUs) | 60s |
| Model Serving Endpoints | `GET /api/2.0/serving-endpoints` | 60s |
| Lakebase Projects | `GET /api/2.0/postgres/projects` | 60s |
| Lakebase Branches | `GET /api/2.0/postgres/projects/{id}/branches` | 60s |
| Lakebase Endpoints | `GET /api/2.0/postgres/projects/{id}/branches/{id}/endpoints` | 60s |
| AI Gateway Usage | `system.ai_gateway.usage` via SQL warehouse | 60s |
| Lakehouse Monitor Inventory | `GET /api/2.1/lakehouse-monitoring/monitors` + `/refreshes` | 60s |
| Lakehouse Monitor Profile Metrics | `_profile_metrics` output tables via SQL warehouse | 60s |
| Lakehouse Monitor Drift Metrics | `_drift_metrics` output tables via SQL warehouse | 60s |

### 3.3 Authentication

Databricks Personal Access Token (PAT) with read-only permissions. Configured via agent properties (ASP) in the Foglight UI. No write operations are performed against the Databricks API.

### 3.4 Deployment Requirements

| Component | Requirement |
|---|---|
| Quest Foglight Management Server | 8.2.0+ |
| FglAM (Foglight Agent Manager) | Co-located with FMS |
| JDK | 11+ (provided by FglAM) |
| Databricks workspace | Any cloud (Azure, AWS, GCP) |
| Databricks access token | Read permissions on workspace resources |
| Billing SQL warehouse | Required for DBU/cost, AI Gateway, and Data Quality features |
| Databricks plan | Standard for core features; Premium for system table access (billing, compute, quality) |

---

## 4. Feature Inventory

### 4.1 Compute Monitoring

**Clusters**
- State (RUNNING, TERMINATED, PENDING, etc.)
- Node types (driver + worker), worker count, core count
- Spark version, autoscale configuration (min/max workers)
- Creator, start time, last activity, terminated time
- Pinned-by user, custom tags, termination reason
- CPU and memory utilisation (%) from `system.compute.node_timeline` — sampled hourly lookback
- Idle cluster and untagged cluster hygiene reports

**SQL Warehouses**
- State (RUNNING, STOPPED, STARTING)
- Type (Classic, Pro, Serverless), size (2X-Small → 4X-Large)
- Cluster count (current, min, max), Photon enabled
- Auto-stop, auto-resume settings, creator
- Query count (derived from query history)
- Efficiency score: queryCount / sizeWeight; "Idle" if 0 queries

**Instance Pools**
- State, node type, idle/used/pending counts
- Max capacity, idle termination minutes, preloaded Spark versions

### 4.2 Job & Pipeline Monitoring

**Jobs**
- Name, creator, trigger type (PERIODIC, FILE_ARRIVAL, TABLE, etc.)
- Cron schedule and status
- Last run: state, result, start time, duration
- Historical stats: success rate %, avg/min/max duration, success/failure counts
- Custom tags; dollar cost (current month via billing join)

**Job Runs** (up to 10 per job)
- Lifecycle state, result, total duration
- Duration breakdown: queue / setup / execution / cleanup
- Task count, retry attempt number, state message

**DLT Pipelines**
- State, creator, run-as user, pipeline ID; dollar cost
- Update history (last 5 updates): state, start time
- Data quality expectations per update: pass rate %, passed/failed/dropped record counts

### 4.3 SQL & Query Intelligence

**Query History** (up to 25 per warehouse)
- User, statement type, status, total duration with breakdown
- Bytes read, rows produced, cache hit flag, error message, query text

**Slow Query Leaderboard** — top 25 queries by duration, cross-warehouse

**User Activity** — per-user aggregates: query count, avg duration, bytes, cache hits, errors; treemap and bubble chart

**Query Volume Trend** — time-series of total queries at each collection cycle

### 4.4 DBU Consumption & Cost Intelligence

**DBU Usage** (60-day rolling window) — raw usage by date, SKU, product, cloud, region

**Cost Intelligence**
- Estimated dollar cost per SKU (DBU × `system.billing.list_prices`)
- Top jobs by DBU with dollar cost
- Cost by SKU; cost by product treemap; cost vs DBU bubble chart by SKU
- Month-over-month cost slopegraph (▲/▼/→ trend per product)
- Daily DBU trend; daily DBU accumulation time-plot; SKU 7-day trend
- User compute spend: per-user, per-product DBU and dollar cost (30 days)
- Table Optimization History: Delta ANALYZE/COMPACTION ops (7 days)
- Storage Costs by product/SKU (30 days)

### 4.5 AI Gateway Observability

**AI Gateway Endpoints** — per-endpoint current-window rollup: request count, total tokens, error rate, p95 latency

**Token Usage** (daily aggregates) — total, input, output tokens by date × endpoint × model

**Per-Requester Activity** — request count, total tokens, error count per requester/requester type

**Prerequisites:** Unity AI Gateway V2 Preview enabled; account-admin access required for `system.ai_gateway.usage`.

### 4.6 Model Serving

**Serving Endpoints** — name, ready state, config update state, creator, served model count

**Served Models** (per endpoint) — model name and version, workload size, traffic %, scale-to-zero, deployment state

### 4.7 Lakebase Platform Monitoring

**Projects** — project ID/name, state, endpoint URL

**Branches** — branch ID/name, state, parent project, endpoint state/URL

### 4.8 Data Quality (Lakehouse Monitoring)

**Monitor Inventory** (via REST API)
- Which Delta tables are monitored; last refresh time; run count

**Profile Metrics** (via `_profile_metrics` SQL)
- Row count at last monitor run (`MAX(count)` where `column_name=':table'`)
- Last run timestamp (`MAX(window.start)`)

**Drift Metrics** (via `_drift_metrics` SQL)
- Drifted column count: columns where chi-square test or KS test p-value < 0.05
- Total monitored columns

**Backlog:** Row count delta (Change column) — requires prev-run comparison logic.

### 4.9 Portlet Reference

| # | Portlet | Category |
|---|---|---|
| 1 | Databricks Overview | Summary |
| 2 | Databricks Clusters | Compute |
| 3 | Databricks SQL Warehouses | Compute |
| 4 | Databricks Instance Pools | Compute |
| 5 | Databricks Jobs | Jobs |
| 6 | Databricks Job Runs | Jobs |
| 7 | Databricks DLT Pipelines | Pipelines |
| 8 | Databricks Pipeline Updates | Pipelines |
| 9 | Databricks Pipeline Data Quality | Pipelines |
| 10 | Databricks Query History | Queries |
| 11 | Databricks Slow Queries | Queries |
| 12 | Databricks User Activity | Queries |
| 13 | Databricks User Activity (Treemap) | Queries |
| 14 | Databricks User Activity (Bubble) | Queries |
| 15 | Databricks DBU Usage | Cost |
| 16 | Databricks DBU by Product | Cost |
| 17 | Databricks DBU by Product (Treemap) | Cost |
| 18 | Databricks Daily DBU Trend | Cost |
| 19 | Databricks MoM DBU Growth | Cost |
| 20 | Databricks Top Jobs by DBU | Cost |
| 21 | Databricks Cost by SKU | Cost |
| 22 | Databricks Cost vs DBU by SKU (Bubble) | Cost |
| 23 | Databricks SKU List Prices | Cost |
| 24 | Databricks Model Serving Endpoints | Model Serving |
| 25 | Databricks Served Models | Model Serving |
| 26 | Databricks Active Resource Trend | Summary |
| 27 | Databricks Job and Pipeline Count Trend | Jobs |
| 28 | Databricks Lakebase Projects | Lakebase |
| 29 | Databricks Lakebase Branches | Lakebase |
| 30 | Databricks AI Gateway Endpoints | AI Gateway |
| 31 | Databricks AI Token Usage | AI Gateway |
| 32 | Databricks AI User Activity | AI Gateway |
| 33 | Databricks Job Sparklines | Jobs |
| 34 | Databricks Warehouse Sparklines | Compute |
| 35 | Databricks Cluster Sparklines | Compute |
| 36 | Databricks Cluster Utilization | Compute |
| 37 | Databricks Idle Clusters | Compute |
| 38 | Databricks Untagged Clusters | Compute |
| 39 | Databricks User Compute Spend | Cost |
| 40 | Databricks Table Optimization History | Storage |
| 41 | Databricks Storage Costs | Storage |
| 42 | Databricks Cost MoM Slopegraph | Cost |
| 43 | Databricks Daily DBU Accumulation | Cost |
| 44 | Databricks DBU Spend Trend by SKU | Cost |
| 45 | Databricks Query Volume Trend | Queries |
| 46 | Databricks Job Success Rate by Day | Jobs |
| 47 | Databricks Data Quality | Data Quality |

---

## 5. Competitive Positioning

### 5.1 Feature Comparison

| Capability | Foglight for Databricks | Datadog | New Relic |
|---|---|---|---|
| Cluster inventory & state | ✓ | ✓ | ✓ |
| Cluster CPU/memory utilisation | ✓ (system.compute.node_timeline) | ✓ | ✓ |
| Job run history & success rate | ✓ | ✓ | ✓ |
| SQL warehouse monitoring | ✓ | ✓ | ✓ |
| Query history with query text | ✓ | Partial | Partial |
| DBU cost monitoring | ✓ Included, Databricks-native | Paid add-on (Cloud Cost Mgmt SKU) | Requires system table licence |
| Cost by SKU with list price | ✓ | ✗ | ✗ |
| DLT pipeline update history | ✓ | ✗ | ✓ |
| DLT data quality expectations | ✓ | ✗ | ✗ |
| Lakehouse Monitor DQ (row count, drift) | ✓ | ✗ | ✗ |
| Model serving endpoint monitoring | ✓ | ✓ | ✗ |
| AI Gateway token & latency observability | ✓ | ✗ | ✗ |
| Lakebase platform monitoring | ✓ | ✗ | ✗ |
| Graphical widgets (treemap/bubble) | ✓ | ✓ | ✓ |
| Integrated with broader IT monitoring | ✓ (Foglight platform) | Partial | Partial |
| On-premises deployment option | ✓ (FglAM) | ✗ | ✗ |

### 5.2 Key Differentiators

**1. DBU Cost Intelligence — Included, Not Add-On**

Datadog's Databricks cost visibility is part of their Cloud Cost Management product — a separately priced SKU. Foglight for Databricks queries `system.billing.usage` and `system.billing.list_prices` directly — Databricks-native, more granular, included in the base cartridge.

**2. DLT Data Quality Expectations**

No other monitoring platform surfaces DLT data quality expectation pass/fail counts as a native monitoring signal. Foglight for Databricks collects expectation results from pipeline events — failures sorted first.

**3. Lakehouse Monitoring Data Quality**

Row count trend and statistical drift (chi-square + KS test) for all Lakehouse-monitored Delta tables, surfaced in a dedicated portlet. Neither Datadog nor New Relic surfaces this data natively.

**4. Unified Monitoring — Foglight Platform Integration**

Single alert console, single dashboard environment, single RBAC model, no additional SaaS contract.

**5. On-Premises / Private Cloud Deployment**

FglAM runs on-premises or in a private cloud — advantage for data sovereignty or private Databricks deployments.

---

## 6. Roadmap

See [ROADMAP.md](ROADMAP.md) for full tier-by-tier status and version history.

### Summary

| Tier | Description | Status |
|---|---|---|
| 1–3 | Clusters, Jobs, Warehouses, Queries, Pipelines, Pools | ✓ Complete |
| 4 | DBU consumption & cost intelligence | ✓ Complete |
| 5 | DLT pipeline depth (updates + expectations) | ✓ Built; untested (no DLT in dev workspace) |
| 6 | Cluster runtime metrics (CPU/memory) | ✓ Complete — 1.0.140 |
| 7 | Model Serving endpoints + served models | ✓ Complete — 1.0.65 |
| 8 | Lakebase platform monitoring | ✓ Complete — 1.0.118 |
| 10 Phase 1 | Lakehouse Monitoring DQ (inventory + row count + drift) | ✓ Complete — 1.0.181 |
| 10 Phase 2 | Job → data quality correlation | Deferred |
| 11 | AI Gateway token & GenAI observability | ✓ Complete — 1.0.119 |
| 9 | Lakewatch SIEM | Blocked — Private Preview, no public API |
| v2 | AUI dashboard layer | Planned |

### Near-term Backlog

- **rowCountDelta (Change column)**: compare consecutive `_profile_metrics` row counts per table
- **Model serving metrics**: per-endpoint latency/throughput from Databricks metrics API
- **Multi-workspace support**: numbered config pairs (`workspace.1.url`, etc.)

---

## 7. Installation & Configuration

See [INSTALL.md](INSTALL.md) for step-by-step installation instructions.

### Configuration Reference

| Property | Required | Description |
|---|---|---|
| `workspaceUrl` | Yes | Databricks workspace URL, e.g. `https://adb-123.azuredatabricks.net/` |
| `accessToken` | Yes | Personal access token with workspace read permissions |
| `billingWarehouseId` | No* | Running SQL Warehouse ID — required for DBU/cost, AI Gateway, Data Quality |
| `collectionIntervalSeconds` | No | Poll interval in seconds (default: 60) |
| `accountId` | No | Account identifier string (default: `default`) |
| `accountName` | No | Account display name (default: `Databricks`) |

### Access Token Permissions

The token requires read access to:
- Clusters, Jobs, Warehouses, Pipelines, Instance Pools, Serving Endpoints (standard workspace read)
- `system.billing.*`, `system.compute.*`, `system.storage.*` tables (Databricks Premium)
- `system.ai_gateway.usage` — **requires account-admin access** (stricter than billing tables)
- Lakebase REST API endpoints (if Lakebase is deployed)
- Lakehouse Monitoring REST API + `_profile_metrics`/`_drift_metrics` output tables

No write permissions are required or used.

---

## 8. Known Limitations (POC)

| Limitation | Notes |
|---|---|
| Single workspace per agent instance | Multi-workspace support planned (numbered config pairs). One agent per workspace for now. |
| DLT pipeline data quality untested | DLT not enabled in dev workspace. Code complete; needs validation in a DLT-enabled environment. |
| Model serving metrics | Endpoint inventory only. Per-endpoint latency/throughput metrics deferred. |
| DBU cost estimation | Based on list prices from `system.billing.list_prices`. Does not account for committed use discounts or negotiated rates. |
| Lakehouse Monitor row count delta | `rowCountDelta` (Change column) always blank — requires prev-run comparison logic. |
| No packaged dashboards | Dashboards must be built manually by the Foglight administrator. Packaged dashboard export deferred to v2. |
| AUI layer | All portlets use WCF tables (v1). Sortable/filterable tables, Gantt charts, and rich drill-down deferred to v2 AUI layer. |
| AI Gateway — account-admin required | `system.ai_gateway.usage` accessible only to account admins. Production should use a dedicated service principal rather than a personal PAT. |

---

## 9. Open Questions

| Question | Owner | Status |
|---|---|---|
| DLT pipelines with `expect()` rules in use? | Customer | DLT not enabled in dev workspace — needs validation in customer environment |
| AUI component library documentation? | Quest Dev Team | Requested — blocks v2 work |
| Target GA version and release process | Quest PM | TBC |
| Account-admin grant for monitoring principal vs. personal PAT? | Quest Dev / Customer | Production should use a dedicated account-admin-scoped service principal |

---

## 10. Success Metrics (POC)

- Agent collects and displays data for all configured object types within one collection cycle (60s)
- All ~47 portlets render without errors in Foglight 8.2.0
- DBU cost data matches values visible in the Databricks billing UI (within rounding)
- Dashboard setup can be completed by a Foglight administrator in under 30 minutes
- No write operations performed against the Databricks workspace
