# Foglight for Databricks — Product Management Specification

**Version:** 1.0.78  
**Status:** POC / Active Development  
**Owner:** Quest Software  
**Last Updated:** 2026-05-19

---

## 1. Executive Summary

Foglight for Databricks is a native Quest Foglight monitoring cartridge that provides comprehensive observability for Databricks Lakehouse environments. It collects topology, operational metrics, cost intelligence, and data quality signals from the Databricks REST API and surfaces them as Foglight portlets and dashboards.

The product targets organisations running Databricks on Azure, AWS, or GCP who need unified monitoring of their Lakehouse platform within their existing Foglight investment — without deploying a separate monitoring tool or paying for add-on modules from cloud-native observability vendors.

---

## 2. Problem Statement

### 2.1 The Observability Gap

Databricks environments generate rich operational data — job failures, query slowdowns, runaway clusters, cost spikes, data quality degradation — but this data is fragmented across:

- The Databricks UI (workspace-scoped, no org-level view)
- Individual SQL warehouse query logs
- Databricks billing system tables (requires SQL query expertise to interrogate)
- DLT pipeline event logs (requires navigating individual pipeline UIs)

Organisations running Databricks alongside other monitored infrastructure (databases, servers, applications) have no single pane of glass. They are forced to context-switch between Foglight and the Databricks UI, or pay for a separate Databricks-specific monitoring product.

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
Databricks REST API
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
          ├── DatabricksUsage (n)          ← billing data
          ├── DatabricksJobDbu (n)         ← per-job DBU
          ├── DatabricksSkuPrice (n)       ← list prices
          └── DatabricksServingEndpoint (n)
              └── DatabricksServedModel (n)
        │
        ▼
WCF Portlets (27 views, Groovy scripts)
Foglight Dashboards (user-configured)
```

### 3.2 Data Sources

| Source | API | Collection Interval |
|---|---|---|
| Clusters | `GET /api/2.0/clusters/list` | 60s |
| Jobs | `GET /api/2.1/jobs/list` | 60s |
| Job Runs | `GET /api/2.1/jobs/runs/list` (per job) | 60s |
| SQL Warehouses | `GET /api/2.0/sql/warehouses` | 60s |
| SQL Query History | `GET /api/2.0/sql/history/queries` (per warehouse) | 60s |
| DLT Pipelines | `GET /api/2.0/pipelines` | 60s |
| DLT Pipeline Events | `GET /api/2.0/pipelines/{id}/events` | 60s |
| Instance Pools | `GET /api/2.0/instance-pools/list` | 60s |
| DBU Usage | `system.billing.usage` via SQL warehouse | 60s |
| SKU List Prices | `system.billing.list_prices` via SQL warehouse | 60s |
| Model Serving Endpoints | `GET /api/2.0/serving-endpoints` | 60s |

### 3.3 Authentication

Databricks Personal Access Token (PAT) with read-only permissions. Token stored in `databricks.properties` on the FglAM host. No write operations are performed against the Databricks API.

### 3.4 Deployment Requirements

| Component | Requirement |
|---|---|
| Quest Foglight Management Server | 8.2.0+ |
| FglAM (Foglight Agent Manager) | Co-located with FMS |
| JDK | 11+ (provided by FglAM) |
| Databricks workspace | Any cloud (Azure, AWS, GCP) |
| Databricks access token | Read permissions on workspace resources |
| Billing SQL warehouse | Required for DBU/cost features (any running warehouse) |
| Databricks plan | Standard for core features; Premium for system table access (billing) |

---

## 4. Feature Inventory

### 4.1 Compute Monitoring

**Clusters**
- State (RUNNING, TERMINATED, PENDING, etc.)
- Node types (driver + worker), worker count, core count
- Spark version, autoscale configuration (min/max workers)
- Creator, start time, last activity, terminated time
- Pinned-by user, custom tags
- Termination reason

**SQL Warehouses**
- State (RUNNING, STOPPED, STARTING)
- Type (Classic, Pro, Serverless), size (2X-Small → 4X-Large)
- Cluster count (current, min, max), Photon enabled
- Auto-stop, auto-resume settings, creator
- Query count (derived from query history)

**Instance Pools**
- State, node type, idle/used/max capacity counts

### 4.2 Job & Pipeline Monitoring

**Jobs**
- Name, creator, trigger type (PERIODIC, FILE_ARRIVAL, TABLE, etc.)
- Cron schedule and status
- Last run: state, result, start time, duration
- Historical stats: success rate %, avg/min/max duration, success/failure counts over collected history
- Custom tags

**Job Runs** (up to 10 per job)
- Lifecycle state (RUNNING, TERMINATED, etc.), result (SUCCESS, FAILED, etc.)
- Total duration, plus breakdown: queue / setup / execution / cleanup
- Task count, retry attempt number, state message

**DLT Pipelines**
- State, creator, run-as user, pipeline ID
- Update history (last 5 updates): state, start time
- Data quality expectations per update: expectation name, flow, dataset, passed/failed/dropped record counts, pass rate %

### 4.3 SQL & Query Intelligence

**Query History** (up to 25 per warehouse)
- User, statement type, status
- Total duration with breakdown: compilation / execution / fetch
- Bytes read, rows produced, cache hit flag
- Full query text
- Error message (if failed)

**Slow Query Leaderboard**
- Top 25 queries by duration, cross-warehouse

**User Activity**
- Per-user aggregates: query count, avg duration, total bytes read, cache hit count, error count
- Interactive treemap: query volume by user
- Bubble chart: query count vs avg duration, coloured by error rate

### 4.4 DBU Consumption & Cost Intelligence

**DBU Usage** (60-day rolling window)
- Raw usage by date, SKU, billing product, cloud, region
- Derived aggregations: DBU by product (current month), daily DBU trend, month-over-month growth by product

**Cost Intelligence**
- Estimated dollar cost per SKU (DBU × list price, joined from `system.billing.list_prices`)
- Top 10 jobs by DBU consumed (current month)
- Cost by SKU table
- SKU list price reference (price per DBU, effective date, currency)
- Graphical: DBU by Product treemap, Cost vs DBU bubble chart by SKU
- Overview summary: DBU this month, top product, estimated total cost

### 4.5 Model Serving

**Serving Endpoints**
- Name, ready state (READY, NOT_READY, UPDATING), config update state
- Creator, creation time, last updated time
- Route optimised flag, served model count

**Served Models** (per endpoint)
- Model name and version
- Deployment state, workload size (Small/Medium/Large)
- Traffic percentage, scale-to-zero enabled

### 4.6 Portlet Reference

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
| 26 | Databricks Active Resource Trend | Compute |
| 27 | Databricks Job and Pipeline Count Trend | Jobs |

---

## 5. Competitive Positioning

### 5.1 Feature Comparison

| Capability | Foglight for Databricks | Datadog | New Relic |
|---|---|---|---|
| Cluster inventory & state | ✓ | ✓ | ✓ |
| Job run history & success rate | ✓ | ✓ | ✓ |
| SQL warehouse monitoring | ✓ | ✓ | ✓ |
| Query history with query text | ✓ | Partial | Partial |
| DBU cost monitoring | ✓ Included, Databricks-native | Paid add-on (Cloud Cost Mgmt SKU) | Requires system table licence |
| Cost by SKU with list price | ✓ | ✗ | ✗ |
| DLT pipeline update history | ✓ | ✗ | ✓ |
| DLT data quality expectations | ✓ | ✗ | ✗ |
| Model serving endpoint monitoring | ✓ | ✓ | ✗ |
| Graphical widgets (treemap/bubble) | ✓ | ✓ | ✓ |
| Integrated with broader IT monitoring | ✓ (Foglight platform) | Partial | Partial |
| On-premises deployment option | ✓ (FglAM) | ✗ | ✗ |

### 5.2 Key Differentiators

**1. DBU Cost Intelligence — Included, Not Add-On**

Datadog's Databricks cost visibility is part of their Cloud Cost Management product — a separately priced SKU that pulls from cloud provider billing APIs (AWS Cost Explorer, Azure Cost Management). It is not included in standard Databricks monitoring and requires additional commercial negotiation.

Foglight for Databricks queries `system.billing.usage` and `system.billing.list_prices` directly — Databricks-native, more granular (DBU-denominated rather than cloud-dollar-estimated), and included in the base cartridge at no additional cost.

**2. DLT Data Quality Expectations**

No other monitoring platform surfaces DLT data quality expectation pass/fail counts as a native monitoring signal. When a pipeline's `expect()` rules start failing, it means bad data is entering or passing through the Lakehouse. This is currently invisible to operations teams unless they manually inspect the DLT UI per pipeline.

Foglight for Databricks collects expectation results from pipeline events and surfaces them in a dedicated portlet — failures sorted first — giving data engineering teams an immediate view of data reliability across all pipelines.

**3. Unified Monitoring — Foglight Platform Integration**

Datadog and New Relic are standalone SaaS products. For organisations running Foglight for databases, servers, and applications, adding Databricks monitoring to the same platform means:
- Single alert console
- Single dashboard environment
- Single RBAC model
- No additional SaaS contract or per-host pricing

**4. On-Premises / Private Cloud Deployment**

FglAM runs on-premises or in a private cloud. For organisations with data sovereignty requirements or private Databricks deployments (BYOC/VPC), this is a meaningful advantage over SaaS-only monitoring tools.

---

## 6. Roadmap

### 6.1 In Progress / Complete (v1.0.78)

All items in Tiers 1–5 and Tier 7 are complete. Time-plot trend views (Active Resource Trend, Job & Pipeline Count Trend) added in 1.0.74. Landing page composite-view finalized in 1.0.78 — 5 views stacked full-width with titles. See ROADMAP.md for full version history.

### 6.2 Next — Tier 8: Lakebase Platform Monitoring

Databricks Lakebase is a serverless managed PostgreSQL offering (GA 2025). The Foglight PostgreSQL cartridge handles per-branch query-level monitoring. Tier 8 adds platform-level visibility via the Lakebase REST API:

- Project and branch inventory (name, state, parent, created time)
- Endpoint provisioning status (running, stopped, provisioning)
- In-flight operation monitoring (create/clone/restore — detect stuck operations)

**Value:** Gives platform teams visibility into the lifecycle of Lakebase resources without navigating the Databricks UI. Complements, rather than replaces, the existing PostgreSQL cartridge.

### 6.3 Tier 9: Lakewatch Security SIEM

Databricks Lakewatch is an agentic SIEM platform (Private Preview, March 2026). No public API available yet. Blocked pending GA.

**Planned coverage (post-GA):** Security event ingestion status, detection inventory, incident summary (MTTD/MTTR).

### 6.4 Tier 10: Lakehouse Monitoring (Data Quality)

Requires Unity Catalog (confirmed enabled: `azure:eastus`). Two phases:

**Phase 1** — Monitor inventory: which tables are monitored, monitor type, last refresh status, stale monitor detection. REST API: `/api/2.1/lakehouse-monitoring/monitors`.

**Phase 2** — Job → data quality correlation: cross-reference job run failures with downstream table drift metrics. If Job X failed and Table Y shows drift shortly after, surface both in a single view. **This capability does not exist in Datadog or New Relic** and is the primary differentiator for this tier.

### 6.5 v2: AUI Dashboard Layer

WCF portlets are the v1 foundation. v2 replaces or supplements them with Foglight's Angular UI (AUI) framework:

| v1 (WCF) | v2 (AUI) |
|---|---|
| Static tables | Sortable, filterable, paginated tables |
| Treemap / bubble (limited) | Line, bar, heatmap, Gantt, sparklines |
| No drill-down navigation | Context-aware drill-down |
| Fixed portlet grid | Flexible responsive layout |

AUI dependency: internal platform team documentation required before implementation begins.

---

## 7. Installation & Configuration

### 7.1 Installation Steps

1. Download `DatabricksAgent-{VERSION}-dist.zip` from the GitHub releases page
2. Extract and copy `agent-deploy/` contents to the FglAM agents directory
3. Edit `config/databricks.properties` with workspace URL, access token, and (optional) billing warehouse ID
4. Install `DatabricksAgent-{VERSION}.car` via Foglight UI → Administration → Cartridges
5. Restart FglAM
6. Navigate to the Databricks nav entry in Foglight — Overview portlet loads automatically

### 7.2 Configuration Reference

```properties
# Required
workspaceUrl=https://<workspace>.azuredatabricks.net/
accessToken=<personal-access-token>

# Optional — defaults shown
collectionIntervalSeconds=60
accountId=default
accountName=Databricks

# Optional — required for DBU/cost features
billingWarehouseId=<warehouse-id>
```

### 7.3 Access Token Permissions

The token requires read access to:
- Clusters, Jobs, Warehouses, Pipelines, Instance Pools, Serving Endpoints (standard workspace read)
- `system.billing.usage` and `system.billing.list_prices` tables (requires Databricks Premium or system table access)

No write permissions are required or used.

### 7.4 Dashboard Setup

The cartridge provides portlets but does not automatically create dashboards. Recommended setup (see DASHBOARDS.md):

| Dashboard | Portlets |
|---|---|
| Databricks - Compute | Clusters, SQL Warehouses, Instance Pools |
| Databricks - Jobs | Jobs, Job Runs |
| Databricks - Queries | Query History, Slow Queries, User Activity |
| Databricks - Pipelines | DLT Pipelines, Pipeline Updates, Pipeline Data Quality |
| Databricks - DBU & Cost | DBU by Product (Treemap), Cost vs DBU (Bubble), Cost by SKU, MoM Growth, Top Jobs by DBU |
| Databricks - Model Serving | Model Serving Endpoints, Served Models |

---

## 8. Known Limitations (POC)

| Limitation | Notes |
|---|---|
| Single workspace per agent instance | Multi-workspace support is planned (numbered config pairs). Currently requires one FglAM agent deployment per workspace. |
| DLT pipeline data quality untested | DLT not enabled in dev workspace. Code is complete; requires a DLT-enabled environment for validation. |
| Model serving metrics | Endpoint inventory only (v1.0.65). Per-endpoint latency/throughput metrics deferred. |
| DBU cost estimation | Based on list prices from `system.billing.list_prices`. Does not account for committed use discounts or negotiated rates. |
| Query text retention | Query text stored in topology for up to 25 queries per warehouse per collection cycle. No historical retention beyond what Foglight retains in topology. |
| No packaged dashboards | Dashboards must be built manually by the Foglight administrator. Packaged dashboard export deferred to v2. |
| AUI layer | Most portlets use WCF tables. Time-plot charts (wcf.chart.time-plot) added for resource and job/pipeline trends. No line/bar/Gantt until v2 AUI layer is implemented. |

---

## 9. Open Questions

| Question | Owner | Status |
|---|---|---|
| Is Unity Catalog fully enabled and accessible to the service token? | Customer | UC metastore confirmed: `azure:eastus`. Token access to monitoring tables TBC. |
| Are DLT pipelines with `expect()` rules in use? | Customer | DLT not enabled in dev workspace — needs validation in a customer environment. |
| Is Lakebase deployed? | Customer | TBC — determines Tier 8 priority. |
| AUI component library documentation available? | Quest Dev Team | Requested. Blocks v2 work. |
| Target GA version and release process | Quest PM | TBC |

---

## 10. Success Metrics (POC)

- Agent collects and displays data for all configured object types within one collection cycle (60s)
- All 25 portlets render without errors in Foglight 8.2.0
- DBU cost data matches values visible in the Databricks billing UI (within rounding)
- Dashboard setup can be completed by a Foglight administrator in under 30 minutes
- No write operations performed against the Databricks workspace
