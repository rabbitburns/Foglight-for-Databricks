# Foglight Databricks Agent — Installation Guide

## Prerequisites

- Foglight Management Server 8.2.0+
- FglAM (Foglight Agent Manager) installed and running on the same host
- A Databricks workspace URL and personal access token with at least read permissions
- A running SQL Warehouse (required for DBU/cost, AI Gateway, and Lakehouse Monitoring features)

## Files

You only need the `.car` file from the `cartridges/` folder:

```
DatabricksAgent-*.car
```

The agent jars and all configuration are bundled inside the cartridge and deployed automatically.

---

## Step 1 — Install the Cartridge

1. Log into the Foglight UI as an administrator
2. Go to **Administration → Cartridges**
3. Click **Install or Upgrade a Cartridge**
4. Upload the `.car` file
5. Confirm the install

> **Upgrading:** always install a higher version number. Reinstalling the same version number causes the CDT to unregister and not re-register — use a new version.

---

## Step 2 — Deploy the Agent to FglAM

FglAM downloads the agent package automatically after cartridge install.

1. Go to **Administration → Agent Installers**
2. Find **DatabricksAgent** in the list
3. Select your FglAM host and click **Deploy**

FglAM downloads and extracts the agent jars automatically — no manual file copying required.

---

## Step 3 — Create and Configure the Agent

1. Go to **Administration → Agents → Create Agent**
2. Select **DatabricksAgent** as the type
3. Give it a name (e.g. `DatabricksAgent-prod`)
4. Click the agent row → **Edit Properties** (ASP) and set:

| Property | Required | Description |
|---|---|---|
| `workspaceUrl` | Yes | Databricks workspace URL, e.g. `https://adb-123.azuredatabricks.net/` |
| `accessToken` | Yes | Personal access token (User Settings → Developer → Access Tokens) |
| `billingWarehouseId` | No* | A running SQL Warehouse ID — required for DBU/cost, AI Gateway, and Data Quality features |
| `collectionIntervalSeconds` | No | Poll interval in seconds (default: 60) |

> Agent properties (ASP) take priority over `databricks.properties`. The properties file is a fallback for environments where ASP is not available.

---

## Step 4 — Restart FglAM

**FglAM is not a Windows service** — it is managed by Quest Watchdog. To restart after installing a new agent version:

1. Open Task Manager → Details tab
2. Find the FglAM Java process (look for `java.exe` with FglAM in the command line, or the process owned by Quest Watchdog)
3. End the process
4. Quest Watchdog restarts FglAM automatically within ~30 seconds

> Agent stop/start alone does **not** reload the agent JAR from disk — FglAM caches the classloader. A full process restart is required when installing a new `.car` version for the first time.

---

## Step 5 — Start the Agent

1. Go to **Administration → Agents**
2. Find your `DatabricksAgent` instance
3. Click **Start**

Allow one collection cycle (~60 seconds) for data to appear.

---

## Step 6 — Verify Collection

After the first collection cycle, navigate to **Databricks** in the top-level nav. The following should be populated:

- DatabricksWorkspace (one entry)
- DatabricksCluster (one row per cluster)
- DatabricksJob (one row per job)
- DatabricksJobRun (recent runs per job)
- DatabricksWarehouse (SQL warehouses)
- DatabricksPipeline (DLT pipelines, if any)
- DatabricksInstancePool (instance pools, if any)
- DatabricksUsage (billing rows, if `billingWarehouseId` is configured)
- DatabricksMonitor (Lakehouse monitors, if any exist)

---

## Troubleshooting

| Symptom | Check |
|---|---|
| Nav entry doesn't appear | Cartridge install may have failed — check **Administration → Cartridges** for errors |
| Agent fails to activate | FMS log at `C:\Quest\Foglight\logs\ManagementServer_*.log` for CDT errors |
| No topology after activation | FglAM log at `C:\Quest\Foglight\fglam\state\default\logs\FglAM_*.log` for collection errors |
| Authentication errors | Verify `accessToken` is valid and not expired; test with `curl -H "Authorization: Bearer <token>" <workspaceUrl>/api/2.0/clusters/list` |
| DBU / cost columns blank | `billingWarehouseId` not set, or warehouse is stopped |
| Row Count / Drifted Cols blank | Requires `billingWarehouseId`; also requires Lakehouse Monitoring to be configured on at least one Delta table in the workspace |
| Agent shows old code after upgrade | FglAM classloader cached old JAR — kill the FglAM Java process, let Watchdog restart it |
| "No CDT could be found" in FMS log | Reinstalling same version — always increment the version number |

---

## Uninstall

1. Stop the agent in **Administration → Agents**
2. Remove the cartridge from **Administration → Cartridges**
3. Delete `C:\Quest\Foglight\fglam\agents\DatabricksAgent\`
4. Restart FglAM (kill process, Watchdog restarts it)
