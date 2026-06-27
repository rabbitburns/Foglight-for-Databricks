# Foglight for Databricks

A [Quest Foglight](https://www.quest.com/products/foglight/) monitoring agent for [Databricks](https://www.databricks.com/) workspaces. Collects topology and metrics from the Databricks REST API and Unity Catalog system tables, and surfaces them in Foglight dashboards.

**Current version:** 1.0.185


## What It Monitors

| Object | Data Collected |
|---|---|
| **Clusters** | State, node types, Spark version, autoscale config, worker/core/memory counts, termination reason, creator, timestamps, custom tags; CPU/memory utilisation from `system.compute.node_timeline` |
| **Jobs** | Name, creator, schedule (cron + status), trigger type, tags, last run state/result/start/duration, success rate, avg/min/max duration, success/failure counts |
| **Job Runs** | Per-job run history (up to 10 most recent), lifecycle state, result, duration breakdown (queue/setup/execution/cleanup), task count, retry info |
| **SQL Warehouses** | State, type, size, cluster counts, Photon, auto-resume/stop settings, creator, query count |
| **SQL Queries** | Per-warehouse query history (up to 25 most recent), user, statement type, status, duration, compilation/execution/fetch times, bytes read, rows produced, cache hit, error message, query text |
| **DLT Pipelines** | State, name, creator, run-as user, update history (last 5), data quality expectations (pass/fail/dropped per update) |
| **Instance Pools** | State, node type, idle/used/pending counts, max capacity, idle termination minutes, preloaded Spark versions |
| **DBU & Cost** | 60-day rolling DBU usage by date/SKU/product/cloud/region; dollar cost via `system.billing.list_prices`; cost per job; cost per pipeline; cost per AI endpoint; user compute spend by product; warehouse efficiency score; top jobs by DBU; SKU price reference; daily per-SKU trend (7 days); MoM cost slopegraph (prev vs current month by product) |
| **Storage** | Table Optimization History (Delta ANALYZE/COMPACTION operations, 7 days, from `system.storage.predictive_optimization_operations_history`); Storage Costs by product/SKU (30 days, from `system.billing.usage`) |
| **Model Serving** | Serving endpoint inventory (state, creator, config update state); per-endpoint served model detail (version, workload size, traffic %, scale-to-zero); runtime metrics per endpoint (request count, 4xx/5xx errors, avg latency, CPU%, Mem%) from Prometheus API |
| **Lakebase** | Project and branch inventory, endpoint host, endpoint state |
| **AI Gateway** | Endpoint inventory (request count, total/input/output tokens, error rate, avg/p95 latency); daily token usage by endpoint and model; per-requester activity |
| **Workspace Metrics** | Time-series at each collection cycle: cluster counts, warehouse counts, job/pipeline counts, today's total DBU accumulation, total query volume |
| **Lakehouse Monitors** | Monitored Delta table inventory via REST API (`/api/2.1/lakehouse-monitoring/monitors`); per-table row count and drifted column count from `_profile_metrics` and `_drift_metrics` output tables |

## Requirements

- Quest Foglight Management Server 8.2.0+
- FglAM (Foglight Agent Manager) on the same host
- Databricks Personal Access Token with workspace read permissions
- A running SQL Warehouse (required for DBU/cost and AI Gateway features)
- Databricks Premium plan for `system.billing.*` system table access
- Unity AI Gateway V2 Preview enabled for AI Gateway monitoring

## Installation

Download the latest `.car` from the [Releases](../../releases) page and install it via Foglight UI → Administration → Cartridges.

## Quick Start

1. Install the `.car` file via Foglight UI → Administration → Cartridges
2. Configure the agent via Administration → Agents → Edit Properties (ASP):
   - `workspaceUrl` — your Databricks workspace URL
   - `accessToken` — a personal access token with read permissions
   - `billingWarehouseId` — a running SQL warehouse ID (for DBU/cost/AI Gateway)
3. Start the agent

## Dashboards

The cartridge installs a **Databricks** top-level navigation entry with eight sub-pages, each scrollable:

| Sub-nav | Contents |
|---|---|
| **Overview** | Resource summary counts + DBU this month; cluster/warehouse trend; job/pipeline trend; DBU treemap; cost vs DBU bubble |
| **Clusters** | Cluster inventory table; cluster sparklines; idle/underutilized cluster report; untagged cluster report |
| **SQL Warehouses** | Warehouse inventory table with efficiency score; query volume trend (time-plot) |
| **Jobs** | Jobs list; job runs list; top jobs by DBU; job success rate by day |
| **Queries** | Query history; slow queries leaderboard; user activity; query volume trend (time-plot) |
| **DLT Pipelines** | Pipeline inventory |
| **Cost & Usage** | Cost by product treemap; cost vs DBU bubble; cost by SKU; MoM growth; daily DBU accumulation (time-plot); SKU daily trend table; top jobs by DBU (with cost); daily DBU trend; DBU by product; DBU usage; SKU list prices; user compute spend |
| **Lakebase** | Lakebase project and branch inventory |
| **AI Gateway** | AI endpoint metrics; daily token usage; per-requester activity |
| **Data Quality** | Lakehouse Monitor inventory: monitored tables, last run timestamp, row count, column drift summary |

## Portlets

All portlets are available individually in the Add View picker:

| Portlet | Description |
|---|---|
| Databricks Overview | Summary counts by resource category + DBU this month |
| Databricks Active Resource Trend | Cluster and warehouse counts over time (time-plot) |
| Databricks Job and Pipeline Count Trend | Job and pipeline counts over time (time-plot) |
| Databricks Clusters | One row per cluster with state, config, and timing |
| Databricks SQL Warehouses | One row per warehouse with state, size, and query count |
| Databricks Query Volume Trend | Total queries across all warehouses over time (time-plot) |
| Databricks Jobs | One row per job with last run and historical stats |
| Databricks Job Runs | Flat cross-job run list sorted by start time |
| Databricks Job Success Rate by Day | Per-job daily success rate for last 7 days + 7d totals and trend |
| Databricks Query History | Cross-warehouse query list with text and timing |
| Databricks Slow Queries | Top 25 queries by duration |
| Databricks User Activity | Per-user query aggregates (table) |
| Databricks User Activity (Treemap) | Per-user query count as interactive treemap |
| Databricks User Activity (Bubble) | Query count vs avg duration scatter by user |
| Databricks DLT Pipelines | One row per pipeline with state and ownership |
| Databricks Pipeline Updates | Last 5 DLT update events per pipeline |
| Databricks Pipeline Data Quality | DLT expectation pass/fail counts per update |
| Databricks Instance Pools | One row per pool with capacity, usage, and configuration |
| Databricks DBU Usage | Raw DBU usage rows by date, SKU, product, cloud, region |
| Databricks DBU by Product | Current-month DBU grouped by billing product (table) |
| Databricks DBU by Product (Treemap) | Current-month DBU by product as interactive treemap |
| Databricks Daily DBU Trend | Day-by-day total DBU for the current month |
| Databricks Daily DBU Accumulation | Intra-day DBU accumulation at collection frequency (time-plot) |
| Databricks DBU Spend Trend by SKU | Per-SKU daily DBU for last 7 days with 7d totals and trend |
| Databricks MoM DBU Growth | Month-over-month DBU growth rate by product |
| Databricks Top Jobs by DBU | Top jobs by DBU consumed (current month) |
| Databricks Cost by SKU | Total DBU and estimated dollar cost grouped by SKU |
| Databricks Cost vs DBU by SKU (Bubble) | Cost vs DBU scatter bubble chart by SKU |
| Databricks SKU List Prices | Current price per DBU by SKU, cloud, and region |
| Databricks Model Serving Endpoints | Serving endpoint inventory — state, config, model count, requests, errors, latency, CPU%, Mem% |
| Databricks Served Models | Per-served-model detail — version, size, traffic % |
| Databricks Lakebase Projects | Lakebase project inventory with branch counts |
| Databricks Lakebase Branches | Lakebase branch inventory with endpoint state |
| Databricks AI Gateway Endpoints | AI Gateway endpoint metrics — tokens, errors, latency |
| Databricks AI Token Usage | Daily token usage by endpoint and model |
| Databricks AI User Activity | Per-requester token and request counts |
| Databricks Job Sparklines | Per-job success rate, avg duration, and last run duration as historical sparklines |
| Databricks Warehouse Sparklines | Per-warehouse query count and cluster count as historical sparklines |
| Databricks Cluster Sparklines | Per-cluster worker count as historical sparkline |
| Databricks Cluster Utilization | Per-cluster CPU% and memory% with trend sparklines |
| Databricks Idle Clusters | RUNNING clusters sorted by CPU utilisation (lowest first) — idle/underutilised cluster report |
| Databricks Untagged Clusters | Clusters with no custom tags — untagged resource report |
| Databricks User Compute Spend | Per-user, per-product DBU and dollar cost for last 30 days |
| Databricks Data Quality | One row per monitored table — last run time, row count, drifted column count |

## Topology

```
DatabricksModelRoot
└── DatabricksAccount
    └── DatabricksWorkspace
        ├── DatabricksCluster (many)
        ├── DatabricksJob (many)
        │   └── DatabricksJobRun (up to 10 per job)
        ├── DatabricksWarehouse (many)
        │   └── DatabricksQuery (up to 25 per warehouse)
        ├── DatabricksPipeline (many)
        │   └── DatabricksPipelineUpdate (up to 5 per pipeline)
        │       └── DatabricksPipelineExpectation (many)
        ├── DatabricksInstancePool (many)
        ├── DatabricksUsage (many)             ← billing/DBU data
        ├── DatabricksJobDbu (many)            ← per-job DBU
        ├── DatabricksSkuPrice (many)          ← list prices
        ├── DatabricksServingEndpoint (many)
        │   └── DatabricksServedModel (many)
        ├── DatabricksLakebaseProject (many)
        │   └── DatabricksLakebaseBranch (many)
        ├── DatabricksAiEndpoint (many)        ← AI Gateway endpoints
        ├── DatabricksAiUsage (many)           ← AI Gateway daily token usage
        ├── DatabricksAiUserActivity (many)    ← AI Gateway per-requester activity
        ├── DatabricksUserSpend (many)         ← per-user, per-product compute spend
        └── DatabricksMonitor (many)           ← Lakehouse Monitor inventory + DQ metrics
```

Workspace-level time-series metrics (sampled at every collection cycle):
- `clusterCount`, `activeClusterCount`
- `warehouseCount`, `activeWarehouseCount`
- `jobCount`, `pipelineCount`
- `totalDailyDbu` — running total of DBU consumed today (UTC)
- `totalQueryCount` — total queries across all warehouses this cycle

## Configuration

The agent is configured via Foglight's agent properties (Administration → Agents → Edit Properties). All properties can also be set in `databricks.properties` in the agent directory, though agent properties take precedence.

| Property | Required | Description |
|---|---|---|
| `workspaceUrl` | Yes | Your Databricks workspace URL, e.g. `https://adb-123.azuredatabricks.net/` |
| `accessToken` | Yes | Personal access token with workspace read permissions |
| `billingWarehouseId` | No* | A running SQL warehouse ID — required for DBU/cost and AI Gateway features |
| `collectionIntervalSeconds` | No | Collection interval in seconds (default: 60) |
| `accountId` | No | Account identifier string (default: `default`) |
| `accountName` | No | Account display name (default: `Databricks`) |

## Building from Source

Requirements: Foglight installed at `C:\Quest\Foglight\` (provides JRE), Python 3, Maven.

```powershell
$env:JAVA_HOME = "C:\Quest\Foglight\jre"
$env:PATH = "C:\Quest\Foglight\jre\bin;" + $env:PATH
python build_cartridge.py 1.0.126 --deploy
```

The `--deploy` flag copies the compiled JAR directly to the FglAM agent cache at `C:\Quest\Foglight\fglam\agents\DatabricksAgent\`. After deploying, a **full FglAM process restart** is required for the new JAR to load — FglAM caches agent classloaders, so agent stop/start alone is not sufficient. Kill the FglAM Java process; Quest Watchdog restarts it automatically. Then:

1. Install the `.car` via Administration → Cartridges
2. Start the agent in Administration → Agents

Each build also copies the `.car` to `cartridges/` for version tracking in git.

## Repository Structure

```
assembly/         Cartridge sources (WCF, topology, CDT, monitoring policy)
  topology/       Topology type definitions and CDT bindings
  wcf/system/
    databricks/           Main WCF module — all portlets, scripts, types
    databricks_clusters/  Clusters sub-nav composite
    databricks_cost/      Cost & Usage sub-nav composite
    databricks_jobs/      Jobs sub-nav composite
    databricks_queries/   Queries sub-nav composite
    databricks_warehouses/ SQL Warehouses sub-nav composite
    databricks_pipelines/ DLT Pipelines sub-nav composite
    databricks_lakebase/  Lakebase sub-nav composite
    databricks_ai_gateway/ AI Gateway sub-nav composite
    databricks_quality/    Data Quality sub-nav composite
src/              Java agent source (FglAM collectors)
docs/             Enablement deck (HTML + PPTX) and Lakebase executive summary
tools/
  lakebase-branch-discovery/   Python script — discovers Lakebase projects/branches/endpoints
                                and emits foglight_agents.csv + bootstrap.sql for Foglight
                                PostgreSQL agent onboarding
.github/workflows/
  lakebase-branch-discovery.yml   On-demand GitHub Actions workflow for the above
```

## Lakebase → Foglight PostgreSQL Onboarding

`tools/lakebase-branch-discovery/` contains a Python script that lists all Lakebase projects, branches, and endpoints and generates:

- `output/foglight_agents.csv` — one row per branch endpoint, pre-filled with host/port/database/SSL config ready to import into Foglight
- `output/bootstrap.sql` — idempotent SQL to enable `pg_stat_statements` on each branch

Run locally:
```bash
cd tools/lakebase-branch-discovery
# edit config.yaml with workspace URL + token
python branch_to_foglight.py
```

Or trigger on-demand via **Actions → Lakebase Branch to Foglight Discovery → Run workflow** (requires `DATABRICKS_HOST` and `DATABRICKS_TOKEN` repository secrets).

## Diagnostics

`dashboard.groovy` is a Script Console diagnostic that prints a full text summary of all collected topology. Run it in Foglight via Administration → Tooling → Script Console.

## Roadmap

See [ROADMAP.md](ROADMAP.md) for planned features including Tier 9 (Lakewatch SIEM) and the v2 AUI dashboard layer.

## License

This project is not officially supported by Quest Software. Use at your own risk.
