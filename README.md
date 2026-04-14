# Foglight for Databricks

A [Quest Foglight](https://www.quest.com/products/foglight/) monitoring agent for [Databricks](https://www.databricks.com/) workspaces. Collects topology and metrics from the Databricks REST API and surfaces them in Foglight dashboards.

## What It Monitors

| Object | Data Collected |
|---|---|
| **Clusters** | State, node types, Spark version, autoscale config, worker/core/memory counts, termination reason |
| **Jobs** | Name, creator, schedule (cron + status), last run state/result/start/duration |
| **Job Runs** | Per-job run history (up to 10 most recent), lifecycle state, result, duration, task count |
| **SQL Warehouses** | State, type, size, cluster counts, Photon, auto-resume/stop settings, creator |
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

## Topology

```
DatabricksModelRoot
└── DatabricksAccount
    └── DatabricksWorkspace
        ├── DatabricksCluster (many)
        ├── DatabricksJob (many)
        │   └── DatabricksJobRun (many)
        ├── DatabricksWarehouse (many)
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
.\build.ps1 -Version 1.0.25
```

Outputs:
- `target\DatabricksAgent-{VERSION}.car` — Foglight cartridge
- `target\DatabricksAgent-{VERSION}-dist.zip` — distributable package with all files

## Diagnostics

`dashboard.groovy` is a Script Console diagnostic that prints a full text summary of all collected topology. Run it in Foglight via Administration → Tooling → Script Console.

## License

This project is not officially supported by Quest Software. Use at your own risk.
