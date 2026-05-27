# Foglight for Databricks

A [Quest Foglight](https://www.quest.com/products/foglight/) monitoring agent for [Databricks](https://www.databricks.com/) workspaces. Collects topology and metrics from the Databricks REST API and Unity Catalog system tables, and surfaces them in Foglight dashboards.

**Current version:** 1.0.113

## What It Monitors

| Object | Data Collected |
|---|---|
| **Clusters** | State, node types, Spark version, autoscale config, worker/core/memory counts, termination reason, creator, timestamps |
| **Jobs** | Name, creator, schedule (cron + status), trigger type, tags, last run state/result/start/duration, success rate, avg/min/max duration, success/failure counts |
| **Job Runs** | Per-job run history (up to 10 most recent), lifecycle state, result, duration breakdown (queue/setup/execution/cleanup), task count, retry info |
| **SQL Warehouses** | State, type, size, cluster counts, Photon, auto-resume/stop settings, creator, query count |
| **SQL Queries** | Per-warehouse query history (up to 25 most recent), user, statement type, status, duration, compilation/execution/fetch times, bytes read, rows produced, cache hit, error message, query text |
| **DLT Pipelines** | State, name, creator, run-as user, update history (last 5), data quality expectations (pass/fail/dropped per update) |
| **Instance Pools** | State, node type, idle/used/max counts |
| **DBU & Cost** | 60-day rolling DBU usage by date/SKU/product/cloud/region; estimated dollar cost via `system.billing.list_prices`; top jobs by DBU; SKU price reference |
| **Model Serving** | Serving endpoint inventory (state, creator, config update state); per-endpoint served model detail (version, workload size, traffic %, scale-to-zero) |

## Requirements

- Quest Foglight Management Server 8.2.0+
- FglAM (Foglight Agent Manager) on the same host
- Databricks Personal Access Token with read permissions
- A running SQL Warehouse (required for DBU/cost features)
- Databricks Premium plan for `system.billing.*` system table access

## Installation

Download the latest release zip from the [Releases](../../releases) page and follow the steps in [INSTALL.md](INSTALL.md).

## Quick Start

1. Extract the release zip
2. Copy `agent-deploy/` contents to `C:\Quest\Foglight\fglam\agents\DatabricksAgent\1.0.6-1.0.6\`
3. Edit `config\databricks.properties` with your workspace URL, access token, and (optional) billing warehouse ID
4. Install the `.car` file via Foglight UI → Administration → Cartridges
5. Restart FglAM

## Dashboards & Portlets

The cartridge includes a **Databricks** top-level nav entry with a composite landing page and 27 portlets, all available in the Add View picker:

| Portlet | Description |
|---|---|
| Databricks Overview | Summary counts by resource category + DBU this month |
| Databricks Clusters | One row per cluster with state, config, and timing |
| Databricks SQL Warehouses | One row per warehouse with state, size, and query count |
| Databricks Jobs | One row per job with last run and historical stats |
| Databricks Job Runs | Flat cross-job run list sorted by start time |
| Databricks Query History | Cross-warehouse query list with text and timing |
| Databricks Slow Queries | Top 25 queries by duration |
| Databricks User Activity | Per-user query aggregates (table) |
| Databricks User Activity (Treemap) | Per-user query count as interactive treemap |
| Databricks User Activity (Bubble) | Query count vs avg duration scatter bubble by user |
| Databricks DLT Pipelines | One row per pipeline with state and ownership |
| Databricks Pipeline Updates | Last 5 DLT update events per pipeline |
| Databricks Pipeline Data Quality | DLT expectation pass/fail counts per update |
| Databricks Instance Pools | One row per pool with capacity and usage |
| Databricks DBU Usage | Raw DBU usage rows by date, SKU, product, cloud, region |
| Databricks DBU by Product | Current-month DBU grouped by billing product (table) |
| Databricks DBU by Product (Treemap) | Current-month DBU by product as interactive treemap |
| Databricks Daily DBU Trend | Day-by-day total DBU for the current month |
| Databricks MoM DBU Growth | Month-over-month DBU growth rate by product |
| Databricks Top Jobs by DBU | Top jobs by DBU consumed (current month) |
| Databricks Cost by SKU | Total DBU and estimated dollar cost grouped by SKU |
| Databricks Cost vs DBU by SKU (Bubble) | Cost vs DBU scatter bubble chart by SKU |
| Databricks SKU List Prices | Current price per DBU by SKU, cloud, and region |
| Databricks Model Serving Endpoints | Serving endpoint inventory — state, config, model count |
| Databricks Served Models | Per-served-model detail — version, size, traffic % |
| Databricks Active Resource Trend | Cluster and warehouse counts over time (time-plot) |
| Databricks Job and Pipeline Count Trend | Job and pipeline counts over time (time-plot) |

The **Databricks** nav landing page is a composite view stacking the Overview table, Cost vs DBU bubble chart, DBU by Product treemap, Active Resource Trend chart, and Job & Pipeline Count Trend chart — full-width, with titles.

See [DASHBOARDS.md](DASHBOARDS.md) for full portlet and script reference.

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
        ├── DatabricksUsage (many)          ← billing/DBU data
        ├── DatabricksJobDbu (many)         ← per-job DBU
        ├── DatabricksSkuPrice (many)       ← list prices
        └── DatabricksServingEndpoint (many)
            └── DatabricksServedModel (many)
```

## Configuration

Edit `config\databricks.properties` in the deployed agent directory:

```properties
# Required
workspaceUrl=https://<your-workspace>.azuredatabricks.net/
accessToken=<your-token>

# Optional — defaults shown
collectionIntervalSeconds=60
accountId=default
accountName=Databricks

# Optional — required for DBU/cost features
billingWarehouseId=<warehouse-id>
```

## Building from Source

Requirements: JDK 11+, Python 3, Maven dependencies in local `.m2` cache, Foglight installed at `C:\Quest\Foglight\`.

```powershell
.\build.ps1 -Version 1.0.113
```

Outputs:
- `target\DatabricksAgent-{VERSION}.car` — Foglight cartridge
- `target\DatabricksAgent-{VERSION}-dist.zip` — distributable package with all files

## Repository Structure

```
assembly/         Cartridge sources (WCF, topology, CDT, monitoring policy)
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

See [ROADMAP.md](ROADMAP.md) for planned features including Tier 8 (Lakebase platform monitoring), Tier 11 (AI Gateway token observability), Tier 6 (cluster runtime metrics via `system.compute.node_timeline`), and the v2 AUI dashboard layer.

## License

This project is not officially supported by Quest Software. Use at your own risk.
