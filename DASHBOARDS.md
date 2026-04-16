# Foglight for Databricks — Dashboard & Portlet Reference

## Recommended Dashboard Setup

The cartridge ships portlets but does not automatically create dashboards. After installing, build the following 5 dashboards manually via the Foglight UI.

### How to create a dashboard and add portlets

1. In the left nav under **Dashboards → My Dashboards**, click the **+** icon to create a new dashboard
2. Give it a name (e.g. "Databricks - Compute")
3. Click **Actions → Add view...** in the top-right of the dashboard
4. Search for "Databricks" — all portlets should appear
5. Select the portlet and click **Add**
6. Repeat for each portlet on the dashboard
7. Use the resize/drag handles to arrange the layout

### Dashboard 1 — Databricks (Overview)

The landing page is built into the nav entry automatically — no dashboard to create. Clicking **Databricks** in the left nav goes directly to the Overview summary table.

### Dashboard 2 — Databricks - Compute

| Portlet | Layout suggestion |
|---|---|
| Databricks Clusters | Full width, top |
| Databricks SQL Warehouses | Full width, below |

### Dashboard 3 — Databricks - Jobs

| Portlet | Layout suggestion |
|---|---|
| Databricks Jobs | Full width, top — shows per-job stats |
| Databricks Job Runs | Full width, below — shows individual run history |

### Dashboard 4 — Databricks - Queries

| Portlet | Layout suggestion |
|---|---|
| Databricks Query History | Full width, top |
| Databricks Slow Queries | Full width, middle |
| Databricks User Activity | Full width, bottom |

### Dashboard 5 — Databricks - Pipelines & Pool

| Portlet | Layout suggestion |
|---|---|
| Databricks DLT Pipelines | Full width, top |
| Databricks Instance Pools | Full width, below |

### Notes

- If a portlet doesn't appear in the Add View picker, check that your role is listed under the portlet's **Relevant Roles** in the Definitions editor
- After adding a portlet to a dashboard, removing and re-adding it picks up any column changes from a cartridge upgrade
- The **Databricks** nav entry landing page is managed by the cartridge — do not manually create a module with the same name as it will conflict

---

## Module

WCF module name: `system:databricks`
Nav entry: **Databricks** (top-level, appears alongside Administration / Alarms / Infrastructure)
Landing page: **Databricks Overview** (view id=9)

---

## Views

All views carry these purposes unless noted:
- `page` — usable as a full dashboard page
- `pagelet` — embeddable in a composite page
- `portlet` — addable via Add View picker to any dashboard
- `reportlet` — includable in Foglight reports

All views are visible to: Administrator, Operator, Advanced Operator, Dashboard Designer, Dashboard User, General Access.

| View ID | Display Name | Script ID | WCF Type | Sort Order | Notes |
|---|---|---|---|---|---|
| 9 | Databricks Overview | 10 | DatabricksSummaryRow | Fixed (category order) | **Main-view / landing page** |
| 1 | Databricks Job Runs | 2 | DatabricksRunRow | Started desc | Cross-job flat run list |
| 3 | Databricks Query History | 4 | DatabricksQueryRow | Started desc | Cross-warehouse query list |
| 5 | Databricks Slow Queries | 6 | DatabricksQueryRow | Duration desc, top 25 | Slow query leaderboard |
| 7 | Databricks User Activity | 8 | DatabricksUserActivityRow | Query count desc | Per-user query summary |
| 11 | Databricks Clusters | 12 | DatabricksClusterRow | RUNNING first, then name | |
| 13 | Databricks SQL Warehouses | 14 | DatabricksWarehouseRow | RUNNING first, then name | |
| 15 | Databricks Jobs | 16 | DatabricksJobRow | Failures first, then last start desc | |
| 17 | Databricks DLT Pipelines | 18 | DatabricksPipelineRow | RUNNING first, then name | |
| 19 | Databricks Instance Pools | 20 | DatabricksInstancePoolRow | Pool name asc | |

---

## WCF Data Types (`types.xml`)

| Type | Fields | Used By |
|---|---|---|
| DatabricksSummaryRow | category, total, active, inactive, details | Overview |
| DatabricksRunRow | jobName, creator, runId, state, result, started, total, queue, setup, execution, cleanup, tasks, retry, message | Job Runs |
| DatabricksQueryRow | warehouseName, userName, statementType, status, startedAt, duration, compilation, execution, fetch, bytesRead, rowsProduced, fromCache, errorMessage, durationMs | Query History, Slow Queries |
| DatabricksUserActivityRow | userName, queryCount, avgDuration, totalBytes, cacheHits, errorCount | User Activity |
| DatabricksClusterRow | clusterName, state, source, driverNode, workers, minWorkers, maxWorkers, cores, sparkVersion, creator, started, lastActivity, terminated, pinnedBy, tags | Clusters |
| DatabricksWarehouseRow | warehouseName, type, state, size, numClusters, minClusters, maxClusters, photon, autoStop, autoResume, creator, queryCount | SQL Warehouses |
| DatabricksJobRow | jobName, creator, triggerType, schedule, lastState, lastResult, lastStart, lastDuration, successRate, avgDuration, minDuration, maxDuration, successes, failures, tags | Jobs |
| DatabricksPipelineRow | pipelineName, state, creator, runAs, pipelineId | DLT Pipelines |
| DatabricksInstancePoolRow | poolName, state, nodeType, minIdle, maxCapacity, idle, used | Instance Pools |

---

## Scripts (`scripts/{id}.groovy`)

All scripts use the pattern:
1. `ts.getObjectsOfType(ts.getType("DatabricksWorkspace"))` — workspace traversal
2. Collect raw map list from topology
3. Sort raw list
4. Create `DataObject` rows via `functionHelper.createDataObject('databricks:TypeName', 'none', null)`
5. `row.store('field', value, specificTimeRange)`
6. Return `ArrayList`

| Script | Purpose |
|---|---|
| 2.groovy | Job Runs — traverses workspace → jobs → runs |
| 4.groovy | Query History — traverses workspace → warehouses → queries |
| 6.groovy | Slow Queries — same as 4, sorted by durationMs desc, capped at 25 |
| 8.groovy | User Activity — aggregates per-user stats from query data |
| 10.groovy | Overview Summary — counts clusters/warehouses/jobs/pipelines/pools/queries |
| 12.groovy | Clusters — one row per DatabricksCluster |
| 14.groovy | SQL Warehouses — one row per DatabricksWarehouse |
| 16.groovy | Jobs — one row per DatabricksJob, failures sorted first |
| 18.groovy | DLT Pipelines — one row per DatabricksPipeline |
| 20.groovy | Instance Pools — one row per DatabricksInstancePool |

---

## Topology Source

All data comes from the topology collected by `ClusterCollector.java` (Java agent, runs every 60s):

```
DatabricksModelRoot
  └── DatabricksAccount
        └── DatabricksWorkspace
              ├── DatabricksCluster (n)
              ├── DatabricksJob (n)
              │     └── DatabricksJobRun (up to 10 per job)
              ├── DatabricksWarehouse (n)
              │     └── DatabricksQuery (up to 25 per warehouse)
              ├── DatabricksPipeline (n)
              └── DatabricksInstancePool (n)
```

No new collection is needed to support any of the current views — all fields exist in topology as of v1.0.43.

---

## Version History

| Version | Views Added |
|---|---|
| 1.0.33–1.0.39 | Job Runs (id=1) — nav entry, landing page |
| 1.0.41 | Query History (id=3), Slow Queries (id=5), User Activity (id=7) |
| 1.0.43 | Overview (id=9) — new landing page |
| 1.0.44 | Clusters (id=11), SQL Warehouses (id=13), Jobs (id=15), DLT Pipelines (id=17), Instance Pools (id=19); reportlet purpose on all views |
| 1.0.45 | Query text collection (Java + CDT); queryText column added to Query History and Slow Queries |
| 1.0.46–1.0.47 | Fix: queryText column missing from Slow Queries view |
