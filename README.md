# Foglight for Databricks

A [Quest Foglight](https://www.quest.com/products/foglight/) monitoring agent for [Databricks](https://www.databricks.com/) workspaces. Collects topology and metrics from the Databricks REST API and surfaces them in Foglight dashboards.

## What It Monitors

| Object | Data Collected |
|---|---|
| **Clusters** | State, node types, Spark version, autoscale config, worker/core/memory counts, termination reason, creator, timestamps |
| **Jobs** | Name, creator, schedule (cron + status), trigger type, tags, last run state/result/start/duration, success rate, avg/min/max duration, success/failure counts |
| **Job Runs** | Per-job run history (up to 10 most recent), lifecycle state, result, duration breakdown (queue/setup/execution/cleanup), task count, retry info |
| **SQL Warehouses** | State, type, size, cluster counts, Photon, auto-resume/stop settings, creator, query count |
| **SQL Queries** | Per-warehouse query history (up to 25 most recent), user, statement type, status, duration, compilation/execution/fetch times, bytes read, rows produced, cache hit, error message, query text |
| **DLT Pipelines** | State, name, creator, run-as user |
| **Instance Pools** | State, node type, idle/used/max counts |

## Requirements

- Quest Foglight Management Server 8.2.0+
- FglAM (Foglight Agent Manager) on the same host
- Databricks personal access token with read permissions

## Installation

Download the latest release zip from the [Releases](../../releases) page and follow the steps in [INSTALL.md](INSTALL.md).

## Quick Start

1. Extract the release zip
2. Copy `agent-deploy/` contents to `C:\Quest\Foglight\fglam\agents\DatabricksAgent\1.0.6-1.0.6\`
3. Edit `config\databricks.properties` with your workspace URL and access token
4. Install the `.car` file via Foglight UI → Administration → Cartridges
5. Restart FglAM

## Dashboards & Portlets

The cartridge includes a **Databricks** top-level nav entry with a landing page and 20 portlets, all available in the Add View picker:

| Portlet | Description |
|---|---|
| Databricks Overview | Summary counts by resource category — landing page |
| Databricks Clusters | One row per cluster with state, config, and timing |
| Databricks SQL Warehouses | One row per warehouse with state, size, and query count |
| Databricks Jobs | One row per job with last run and historical stats |
| Databricks Job Runs | Flat cross-job run list sorted by start time |
| Databricks Query History | Cross-warehouse query list with text and timing |
| Databricks Slow Queries | Top 25 queries by duration |
| Databricks User Activity | Per-user query aggregates (table) |
| Databricks User Activity (Treemap) | Per-user query count as interactive treemap |
| Databricks DLT Pipelines | One row per pipeline with state and ownership |
| Databricks Instance Pools | One row per pool with capacity and usage |
| Databricks DBU Usage | Raw DBU usage rows by date, SKU, product, cloud, region |
| Databricks DBU by Product | Current-month DBU grouped by billing product (table) |
| Databricks DBU by Product (Treemap) | Current-month DBU by product as interactive treemap |
| Databricks Daily DBU Trend | Day-by-day total DBU for the current month |
| Databricks MoM DBU Growth | Month-over-month DBU growth rate by product |
| Databricks Top Jobs by DBU | Top jobs by DBU consumed (current month) |
| Databricks Cost by SKU | Total DBU and estimated dollar cost grouped by SKU |
| Databricks Cost vs DBU by SKU (Bubble) | Cost vs DBU scatter bubble chart by SKU |
| Databricks User Activity (Bubble) | Query count vs avg duration scatter bubble by user |

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
        └── DatabricksInstancePool (many)
```

## Configuration

Edit `config\databricks.properties` in the deployed agent directory:

```properties
workspaceUrl=https://<your-workspace>.azuredatabricks.net/
accessToken=<your-token>
collectionIntervalSeconds=60
accountId=default
accountName=Databricks
```

## Building from Source

Requirements: JDK 11+, Python 3, Maven dependencies in local `.m2` cache, Foglight installed at `C:\Quest\Foglight\`.

```powershell
.\build.ps1 -Version 1.0.47
```

Outputs:
- `target\DatabricksAgent-{VERSION}.car` — Foglight cartridge
- `target\DatabricksAgent-{VERSION}-dist.zip` — distributable package with all files

## Diagnostics

`dashboard.groovy` is a Script Console diagnostic that prints a full text summary of all collected topology. Run it in Foglight via Administration → Tooling → Script Console.

## Roadmap

See [ROADMAP.md](ROADMAP.md) for planned features including DBU cost monitoring, DLT pipeline depth, and the v2 AUI dashboard layer.

## License

This project is not officially supported by Quest Software. Use at your own risk.
