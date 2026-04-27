# Foglight Databricks Agent — Installation Guide

## Prerequisites

- Foglight Management Server 8.2.0+
- FglAM (Foglight Agent Manager) installed and running on the same host
- A Databricks workspace URL and personal access token with at least read permissions

## Files

You only need the `.car` file:

```
DatabricksAgent-*.car
```

The agent jars and config template are bundled inside the cartridge and deployed automatically via the Foglight UI.

---

## Step 1 — Install the Cartridge

1. Log into the Foglight UI as an administrator
2. Go to **Administration > Cartridges**
3. Click **Install or Upgrade a Cartridge**
4. Upload the `.car` file
5. Confirm the install

---

## Step 2 — Deploy the Agent to FglAM

1. Go to **Administration > Agent Installers**
2. Find **DatabricksAgent-1.0.6.zip** in the list
3. Select your FglAM host and click **Deploy**
4. FglAM will receive the agent jars automatically — no manual file copying required

---

## Step 3 — Configure the Agent

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
