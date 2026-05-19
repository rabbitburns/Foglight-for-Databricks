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

The landing page is built into the nav entry automatically — no dashboard to create. Clicking **Databricks** in the left nav opens the composite landing page (view id=51), which stacks full-width:

1. Databricks Overview (summary table)
2. Databricks Cost vs DBU by SKU (bubble chart)
3. Databricks DBU by Product (treemap)
4. Databricks Active Resource Trend (cluster/warehouse counts over time)
5. Databricks Job and Pipeline Count Trend (job/pipeline counts over time)

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
| Databricks Pipeline Updates | Full width, middle |
| Databricks Pipeline Data Quality | Full width, below |
| Databricks Instance Pools | Full width, bottom |

### Dashboard 7 — Databricks - Model Serving

| Portlet | Layout suggestion |
|---|---|
| Databricks Model Serving Endpoints | Full width, top |
| Databricks Served Models | Full width, below |

### Dashboard 6 — Databricks - DBU & Cost

| Portlet | Layout suggestion |
|---|---|
| Databricks DBU by Product (Treemap) | Half width, top-left |
| Databricks Cost vs DBU by SKU (Bubble) | Half width, top-right |
| Databricks Cost by SKU | Full width, middle |
| Databricks MoM DBU Growth | Half width, bottom-left |
| Databricks Top Jobs by DBU | Half width, bottom-right |
| Databricks SKU List Prices | Full width, bottom |

### Notes

- If a portlet doesn't appear in the Add View picker, check that your role is listed under the portlet's **Relevant Roles** in the Definitions editor
- After adding a portlet to a dashboard, removing and re-adding it picks up any column changes from a cartridge upgrade
- The **Databricks** nav entry landing page is managed by the cartridge — do not manually create a module with the same name as it will conflict

---

## Module

WCF module name: `system:databricks`
Nav entry: **Databricks** (top-level, appears alongside Administration / Alarms / Infrastructure)
Landing page: **Databricks** composite (view id=51, wcf.grid2) — five views stacked full-width: overview table, Cost vs DBU bubble, DBU by Product treemap, Active Resource Trend, Job & Pipeline Count Trend

---

## Views

All views carry these purposes unless noted:
- `page` — usable as a full dashboard page
- `pagelet` — embeddable in a composite page
- `portlet` — addable via Add View picker to any dashboard
- `reportlet` — includable in Foglight reports

All views are visible to: Administrator, Operator, Advanced Operator, Dashboard Designer, Dashboard User, General Access.

| View ID | Display Name | Script ID | WCF Component | Sort Order | Notes |
|---|---|---|---|---|---|
| 9 | Databricks Overview | 10 | wcf.table.row-table | Fixed (category order) | **Main-view / landing page** |
| 1 | Databricks Job Runs | 2 | wcf.table.row-table | Started desc | Cross-job flat run list |
| 3 | Databricks Query History | 4 | wcf.table.row-table | Started desc | Cross-warehouse query list |
| 5 | Databricks Slow Queries | 6 | wcf.table.row-table | Duration desc, top 25 | Slow query leaderboard |
| 7 | Databricks User Activity | 8 | wcf.table.row-table | Query count desc | Per-user query summary |
| 11 | Databricks Clusters | 12 | wcf.table.row-table | RUNNING first, then name | |
| 13 | Databricks SQL Warehouses | 14 | wcf.table.row-table | RUNNING first, then name | |
| 15 | Databricks Jobs | 16 | wcf.table.row-table | Failures first, then last start desc | |
| 17 | Databricks DLT Pipelines | 18 | wcf.table.row-table | RUNNING first, then name | |
| 19 | Databricks Instance Pools | 20 | wcf.table.row-table | Pool name asc | |
| 21 | Databricks DBU Usage | 22 | wcf.table.row-table | Date desc | Raw usage rows: date, SKU, product, cloud, region, DBU |
| 23 | Databricks DBU by Product | 24 | wcf.table.row-table | DBU desc | Current-month DBU aggregated by billing product |
| 25 | Databricks Daily DBU Trend | 26 | wcf.table.row-table | Date asc | Day-by-day total DBU for current month |
| 27 | Databricks MoM DBU Growth | 28 | wcf.table.row-table | Growth rate desc | Month-over-month DBU change by product |
| 29 | Databricks Top Jobs by DBU | 30 | wcf.table.row-table | DBU desc | Top 10 jobs by DBU consumed this month |
| 31 | Databricks Cost by SKU | 32 | wcf.table.row-table | Dollar cost desc | Total DBU and estimated cost by SKU |
| 33 | Databricks DBU by Product (Treemap) | 34 | wcf.treemap | By DBU size | Current-month DBU by product — interactive treemap |
| 35 | Databricks Cost vs DBU by SKU (Bubble) | 36 | wcf.html-chart.scatter.bubble | By DBU | Cost (Y) vs DBU (X) scatter bubble, sized by DBU, coloured by SKU |
| 37 | Databricks User Activity (Treemap) | 38 | wcf.treemap | By query count size | Per-user query count — interactive treemap |
| 39 | Databricks User Activity (Bubble) | 40 | wcf.html-chart.scatter.bubble | By query count | Query count (X) vs avg duration (Y), coloured by error rate |
| 41 | Databricks SKU List Prices | 42 | wcf.table.row-table | SKU name, cloud asc | `system.billing.list_prices` — current prices per SKU/cloud/region |
| 43 | Databricks Pipeline Updates | 44 | wcf.table.row-table | Start time desc | Last 5 DLT pipeline update events per pipeline (state, startTime, updateId) |
| 45 | Databricks Pipeline Data Quality | 46 | wcf.table.row-table | Failures first | DLT data quality expectations — pass rate, passed/failed/dropped row counts |
| 47 | Databricks Model Serving Endpoints | 48 | wcf.table.row-table | Endpoint name asc | Serving endpoint inventory — state, config update state, creator, model count |
| 49 | Databricks Served Models | 50 | wcf.table.row-table | Endpoint name, model name asc | Per-served-model detail — model name, version, workload size, traffic %, deployment state |
| 53 | Databricks Active Resource Trend | — | wcf.chart.time-plot | Time asc | Historical time-plot of activeClusterCount, clusterCount, activeWarehouseCount, warehouseCount |
| 54 | Databricks Job and Pipeline Count Trend | — | wcf.chart.time-plot | Time asc | Historical time-plot of jobCount, pipelineCount |

---

## WCF Data Types (`types.xml`)

| Type | Fields | Used By |
|---|---|---|
| DatabricksSummaryRow | category, total, active, inactive, details | Overview |
| DatabricksRunRow | jobName, creator, runId, state, result, started, total, queue, setup, execution, cleanup, tasks, retry, message | Job Runs |
| DatabricksQueryRow | warehouseName, userName, statementType, status, startedAt, duration, compilation, execution, fetch, bytesRead, rowsProduced, fromCache, errorMessage, queryText, durationMs | Query History, Slow Queries |
| DatabricksUserActivityRow | userName, queryCount, avgDuration, totalBytes, cacheHits, errorCount | User Activity |
| DatabricksClusterRow | clusterName, state, source, driverNode, workers, minWorkers, maxWorkers, cores, sparkVersion, creator, started, lastActivity, terminated, pinnedBy, tags | Clusters |
| DatabricksWarehouseRow | warehouseName, type, state, size, numClusters, minClusters, maxClusters, photon, autoStop, autoResume, creator, queryCount | SQL Warehouses |
| DatabricksJobRow | jobName, creator, triggerType, schedule, lastState, lastResult, lastStart, lastDuration, successRate, avgDuration, minDuration, maxDuration, successes, failures, tags | Jobs |
| DatabricksPipelineRow | pipelineName, state, creator, runAs, pipelineId | DLT Pipelines |
| DatabricksInstancePoolRow | poolName, state, nodeType, minIdle, maxCapacity, idle, used | Instance Pools |
| DatabricksUsageRow | usageDate, sku, billingOriginProduct, cloud, region, dbuConsumed | DBU Usage |
| DatabricksProductDbuRow | product, dbu | DBU by Product |
| DatabricksDailyDbuRow | usageDate, totalDbu | Daily DBU Trend |
| DatabricksMomGrowthRow | product, currentMonthDbu, previousMonthDbu, growthRate | MoM DBU Growth |
| DatabricksJobDbuRow | jobName, jobId, dbu | Top Jobs by DBU |
| DatabricksSkuCostRow | sku, product, totalDbu, dollarCost | Cost by SKU |
| DatabricksTreeMapNode | id, name, count, fillColor | DBU by Product (Treemap), User Activity (Treemap) |
| DatabricksBubbleNode | xValue (wcf:Number), yValue (wcf:Number), size (wcf:Number), color (wcf:Color), label (wcf:String) | Cost vs DBU (Bubble), User Activity (Bubble) |
| DatabricksSkuPriceRow | skuName, cloud, region, pricingUnit, dbuPrice, currency, effectiveFrom | SKU List Prices |
| DatabricksPipelineUpdateRow | pipelineName, pipelineId, updateId, state, startTime | Pipeline Updates |
| DatabricksPipelineExpectationRow | pipelineName, updateId, expectationName, flowName, passRate, passed, failed, dropped | Pipeline Data Quality |
| DatabricksServingEndpointRow | endpointName, readyState, configUpdateState, creator, creationTime, lastUpdatedTime, routeOptimized, servedModelCount | Model Serving Endpoints |
| DatabricksServedModelRow | endpointName, servedModelName, modelName, modelVersion, workloadSize, scaleToZero, trafficPercentage, deploymentState | Served Models |

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
| 10.groovy | Overview Summary — counts clusters/warehouses/jobs/pipelines/pools/queries; includes current-month DBU + top product |
| 12.groovy | Clusters — one row per DatabricksCluster |
| 14.groovy | SQL Warehouses — one row per DatabricksWarehouse |
| 16.groovy | Jobs — one row per DatabricksJob, failures sorted first |
| 18.groovy | DLT Pipelines — one row per DatabricksPipeline |
| 20.groovy | Instance Pools — one row per DatabricksInstancePool |
| 22.groovy | DBU Usage — one row per DatabricksUsage object (date/SKU/product/cloud/region/DBU) |
| 24.groovy | DBU by Product — current-month DBU aggregated by billingOriginProduct |
| 26.groovy | Daily DBU Trend — total DBU per day for the current calendar month |
| 28.groovy | MoM DBU Growth — current vs previous month DBU and growth rate by product |
| 30.groovy | Top Jobs by DBU — top 10 jobs by DBU consumed in the current month |
| 32.groovy | Cost by SKU — total DBU and estimated dollar cost per SKU |
| 34.groovy | DBU Treemap Nodes — DatabricksTreeMapNode list for DBU by Product treemap |
| 36.groovy | Cost vs DBU Bubble Nodes — DatabricksBubbleNode list; x=DBU, y=cost, coloured by palette per SKU |
| 38.groovy | User Activity Treemap Nodes — DatabricksTreeMapNode list sized by per-user query count |
| 40.groovy | User Activity Bubble Nodes — DatabricksBubbleNode list; x=query count, y=avg duration (sec), coloured by error rate |
| 42.groovy | SKU List Prices — one row per DatabricksSkuPrice, sorted by skuName + cloud |
| 44.groovy | Pipeline Updates — traverses workspace → pipelines → updates; sorted by startTime desc |
| 46.groovy | Pipeline Data Quality — traverses workspace → pipelines → updates → expectations; failures sorted first |
| 48.groovy | Model Serving Endpoints — one row per DatabricksServingEndpoint, sorted by endpointName |
| 50.groovy | Served Models — traverses servingEndpoints → servedModels; sorted by endpointName + servedModelName |

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
              │     └── DatabricksPipelineUpdate (up to 5 per pipeline)
              │           └── DatabricksPipelineExpectation (n per update)
              ├── DatabricksInstancePool (n)
              ├── DatabricksUsage (n) — system.billing.usage rows
              ├── DatabricksSkuPrice (n) — system.billing.list_prices rows
              └── DatabricksServingEndpoint (n)
                    └── DatabricksServedModel (n per endpoint)
```

---

## Version History

| Version | Views Added / Changed |
|---|---|
| 1.0.33–1.0.39 | Job Runs (id=1) — nav entry, landing page |
| 1.0.41 | Query History (id=3), Slow Queries (id=5), User Activity (id=7) |
| 1.0.43 | Overview (id=9) — new landing page |
| 1.0.44 | Clusters (id=11), SQL Warehouses (id=13), Jobs (id=15), DLT Pipelines (id=17), Instance Pools (id=19); reportlet purpose on all views |
| 1.0.45 | Query text collection (Java + CDT); queryText column added to Query History and Slow Queries |
| 1.0.46–1.0.47 | Fix: queryText column missing from Slow Queries view |
| 1.0.48 | DBU Usage (id=21) — DatabricksUsage topology type, system.billing.usage collection |
| 1.0.49–1.0.54 | DBU by Product (id=23), Daily DBU Trend (id=25), MoM DBU Growth (id=27), Top Jobs by DBU (id=29), Cost by SKU (id=31) |
| 1.0.55–1.0.57 | Fix: WCF structure and column path errors in views 23–31 |
| 1.0.58 | Overview updated: DBU (This Month) summary row added to view 9 |
| 1.0.59 | DBU by Product Treemap (id=33); DatabricksTreeMapNode type; wcf.treemap component |
| 1.0.60 | Cost vs DBU by SKU Bubble (id=35); DatabricksBubbleNode type; wcf.html-chart.scatter.bubble component |
| 1.0.61 | User Activity Treemap (id=37) and User Activity Bubble (id=39) |
| 1.0.62 | Fix: treemap sizing — added component-sizing to views 33 and 37 |
| 1.0.63 | SKU List Prices (id=41) — DatabricksSkuPrice topology type; system.billing.list_prices source |
| 1.0.64 | Pipeline Updates (id=43), Pipeline Data Quality (id=45) — DLT event history and expectation results |
| 1.0.65 | Model Serving Endpoints (id=47), Served Models (id=49) — DatabricksServingEndpoint and DatabricksServedModel topology types |
| 1.0.66 | Nav main-view changed from standalone Overview table (id=9) to composite-view (id=51, wcf.grid2): overview table full-width top + DBU treemap + Cost vs DBU bubble side-by-side below |
| 1.0.67–1.0.74 | Active Resource Trend (id=53, wcf.chart.time-plot): activeClusterCount, clusterCount, activeWarehouseCount, warehouseCount; Job & Pipeline Count Trend (id=54): jobCount, pipelineCount; both use query id=52 (DatabricksWorkspace datasource) |
| 1.0.75–1.0.78 | Landing page layout overhaul: single-column wcf.grid2 with 5 stacked full-width views (row 0–4); `align=stretch` + `hweight=1` per window; `showTitle=true` for view title headers; `<width preferred="0"/>` on composite-view sizing |
