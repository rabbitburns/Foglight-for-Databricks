# Foglight Databricks Agent — Installation Guide

## Prerequisites

- Foglight Management Server 8.2.0+
- FglAM (Foglight Agent Manager) installed and running on the same host
- A Databricks workspace URL and personal access token with at least read permissions

## Files

You should have received a zip file containing:

```
DatabricksAgent-1.0.24.car
agent-deploy/
  config/
    agent.manifest
    databricks.properties
  lib/
    databricks-agent.jar
    jackson-annotations-2.16.1.jar
    jackson-core-2.16.1.jar
    jackson-databind-2.16.1.jar
```

---

## Step 1 — Copy Agent Files to FglAM

Copy the `agent-deploy` folder contents into the FglAM agents directory, creating this structure:

```
C:\Quest\Foglight\fglam\agents\DatabricksAgent\1.0.6-1.0.6\
  config\
    agent.manifest
    databricks.properties
  lib\
    databricks-agent.jar
    jackson-annotations-2.16.1.jar
    jackson-core-2.16.1.jar
    jackson-databind-2.16.1.jar
```

> **Note:** Adjust the drive/path if Foglight is installed somewhere other than `C:\Quest\Foglight`.

---

## Step 2 — Configure the Agent

Edit `config\databricks.properties`:

```properties
workspaceUrl=https://<your-workspace>.azuredatabricks.net/
accessToken=dapi<your-token-here>
collectionIntervalSeconds=60
accountId=default
accountName=Databricks
```

- `workspaceUrl` — your Databricks workspace URL (include trailing slash)
- `accessToken` — a Databricks personal access token (User Settings > Developer > Access Tokens)
- `collectionIntervalSeconds` — how often to poll (60 recommended)
- `accountId` / `accountName` — display labels; can be any string

---

## Step 3 — Install the Cartridge

1. Log into the Foglight UI as an administrator
2. Go to **Administration > Cartridges**
3. Click **Install or Upgrade a Cartridge**
4. Upload `DatabricksAgent-1.0.24.car`
5. Confirm the install

---

## Step 4 — Restart FglAM

Restart the FglAM service so it picks up the new agent files:

```powershell
# From an elevated PowerShell prompt:
Restart-Service -Name "Quest Foglight Agent Manager"
```

Or use the Windows Services panel (`services.msc`) — look for **Quest Foglight Agent Manager**.

---

## Step 5 — Verify Collection

After FglAM restarts (allow ~1 minute for the first collection cycle):

1. In Foglight, go to **Administration > Agents**
2. You should see a `DatabricksAgent` instance listed as Active
3. Navigate to your dashboard — the following topology should be populated:
   - DatabricksAccount
   - DatabricksWorkspace
   - DatabricksCluster (one row per cluster)
   - DatabricksJob (one row per job)
   - DatabricksJobRun (recent runs per job)
   - DatabricksWarehouse (SQL warehouses)
   - DatabricksPipeline (DLT pipelines)
   - DatabricksInstancePool (if any exist)

---

## Troubleshooting

| Symptom | Check |
|---|---|
| No topology appears | FMS logs for "No CDT could be found" — cartridge may not have installed correctly |
| Agent shows as inactive | FglAM logs in `C:\Quest\Foglight\fglam\logs\` |
| Authentication errors | Verify `accessToken` in `databricks.properties` is valid and not expired |
| Missing warehouses/pipelines | Your Databricks token may lack permissions for those APIs |
| Metrics show `n/a` | Normal for TERMINATED clusters — metrics only populate for RUNNING resources |

---

## Uninstall

1. Remove the cartridge from **Administration > Cartridges**
2. Delete `C:\Quest\Foglight\fglam\agents\DatabricksAgent\`
3. Restart FglAM
