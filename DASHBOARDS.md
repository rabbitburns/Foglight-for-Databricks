# Foglight for Databricks — Dashboard & Portlet Reference

## Built-In Navigation

The cartridge installs a **Databricks** top-level nav entry automatically. Clicking it opens the composite landing page with nine scrollable sub-pages:

| Sub-nav | Contents |
|---|---|
| **Overview** | Resource summary counts + DBU this month; cluster/warehouse trend; job/pipeline trend; DBU treemap; cost vs DBU bubble |
| **Clusters** | Cluster inventory; cluster sparklines; idle/underutilised cluster report; untagged cluster report; cluster utilization (CPU%/Mem%) |
| **SQL Warehouses** | Warehouse inventory with efficiency score; warehouse sparklines; query volume trend |
| **Jobs** | Jobs list; job runs; top jobs by DBU; job success rate by day; job sparklines |
| **Queries** | Query history; slow queries leaderboard; user activity (table + treemap + bubble); query volume trend |
| **DLT Pipelines** | Pipeline inventory; pipeline updates; pipeline data quality expectations |
| **Cost & Usage** | Cost by product treemap; cost vs DBU bubble; cost by SKU; MoM cost slopegraph; daily DBU accumulation; SKU trend; top jobs by DBU (with cost); daily DBU trend; DBU by product; DBU usage; SKU list prices; user compute spend; table optimization history; storage costs |
| **Lakebase** | Lakebase project and branch inventory |
| **AI Gateway** | AI endpoint metrics; daily token usage; per-requester activity |
| **Data Quality** | Lakehouse Monitor inventory: monitored tables, last run, row count, drifted column count |

No manual dashboard setup is required for normal use — all portlets are reachable through the nav.

---

## Adding Portlets to Custom Dashboards

All portlets are individually addable via **Actions → Add View** on any Foglight dashboard:

1. Open (or create) a dashboard via **Dashboards → My Dashboards**
2. Click **Actions → Add view...**
3. Search "Databricks" — all portlets appear
4. Select and click **Add**

After a cartridge upgrade, if a portlet's columns have changed, remove and re-add it to pick up the new schema.

---

## Full Portlet Reference

| Portlet | Category | Description |
|---|---|---|
| Databricks Overview | Summary | Resource counts by category + DBU this month |
| Databricks Active Resource Trend | Summary | Cluster and warehouse counts over time (time-plot) |
| Databricks Job and Pipeline Count Trend | Summary | Job and pipeline counts over time (time-plot) |
| Databricks Clusters | Compute | One row per cluster — state, config, timing, tags |
| Databricks Cluster Utilization | Compute | Per-cluster CPU% and memory% with sparklines |
| Databricks Cluster Sparklines | Compute | Per-cluster worker count as historical sparkline |
| Databricks Idle Clusters | Compute | RUNNING clusters sorted by CPU% (lowest first) |
| Databricks Untagged Clusters | Compute | Clusters with no custom tags |
| Databricks SQL Warehouses | Compute | One row per warehouse — state, size, efficiency score |
| Databricks Query Volume Trend | Compute | Total queries across all warehouses over time (time-plot) |
| Databricks Warehouse Sparklines | Compute | Per-warehouse query count and cluster count as sparklines |
| Databricks Jobs | Jobs | One row per job — last run + 30d historical stats |
| Databricks Job Runs | Jobs | Flat cross-job run list sorted by start time |
| Databricks Job Success Rate by Day | Jobs | Per-job daily success rate for last 7 days + trend |
| Databricks Job Sparklines | Jobs | Per-job success rate, avg duration, last run duration sparklines |
| Databricks Query History | Queries | Cross-warehouse query list with text and timing |
| Databricks Slow Queries | Queries | Top 25 queries by duration |
| Databricks User Activity | Queries | Per-user query aggregates |
| Databricks User Activity (Treemap) | Queries | Per-user query count as treemap |
| Databricks User Activity (Bubble) | Queries | Query count vs avg duration scatter by user |
| Databricks DLT Pipelines | Pipelines | One row per pipeline — state and ownership |
| Databricks Pipeline Updates | Pipelines | Last 5 DLT update events per pipeline |
| Databricks Pipeline Data Quality | Pipelines | DLT expectation pass/fail counts per update |
| Databricks Instance Pools | Pools | One row per pool — capacity, usage, config |
| Databricks DBU Usage | Cost | Raw DBU rows by date, SKU, product, cloud, region |
| Databricks DBU by Product | Cost | Current-month DBU grouped by billing product |
| Databricks DBU by Product (Treemap) | Cost | Current-month DBU by product as treemap |
| Databricks Daily DBU Trend | Cost | Day-by-day total DBU for the current month |
| Databricks Daily DBU Accumulation | Cost | Intra-day DBU accumulation (time-plot) |
| Databricks DBU Spend Trend by SKU | Cost | Per-SKU daily DBU for last 7 days with totals and trend |
| Databricks MoM DBU Growth | Cost | Month-over-month DBU growth rate by product |
| Databricks Top Jobs by DBU | Cost | Top jobs by DBU consumed (current month) with dollar cost |
| Databricks Cost by SKU | Cost | Total DBU and dollar cost grouped by SKU |
| Databricks Cost vs DBU by SKU (Bubble) | Cost | Cost vs DBU scatter bubble by SKU |
| Databricks SKU List Prices | Cost | Current price per DBU by SKU, cloud, region |
| Databricks Cost MoM Slopegraph | Cost | Prev vs current month cost by product with ▲/▼/→ trend |
| Databricks User Compute Spend | Cost | Per-user, per-product DBU and dollar cost (30 days) |
| Databricks Table Optimization History | Storage | Delta ANALYZE/COMPACTION ops (7 days) |
| Databricks Storage Costs | Storage | Storage billing by product/SKU (30 days) |
| Databricks Model Serving Endpoints | Model Serving | Endpoint inventory — state, config, model count |
| Databricks Served Models | Model Serving | Per-served-model detail — version, size, traffic % |
| Databricks Lakebase Projects | Lakebase | Lakebase project inventory |
| Databricks Lakebase Branches | Lakebase | Branch inventory with endpoint state |
| Databricks AI Gateway Endpoints | AI Gateway | Endpoint metrics — tokens, errors, latency |
| Databricks AI Token Usage | AI Gateway | Daily token usage by endpoint and model |
| Databricks AI User Activity | AI Gateway | Per-requester token and request counts |
| Databricks Data Quality | Data Quality | Monitored table inventory — last run, row count, drifted cols |

---

## Module Details

**WCF module:** `system:databricks`  
**Nav entry:** Databricks (top-level, alongside Administration / Alarms / Infrastructure)  
**Landing page:** composite view (id=51, wcf.grid2) — overview table, cost vs DBU bubble, DBU by product treemap, active resource trend, job & pipeline count trend, stacked full-width

**Sub-modules:**

| Module ID | Nav Label | Path |
|---|---|---|
| `system:databricks` | Databricks | Main module — all portlets |
| `system:databricks_clusters` | Clusters | Clusters sub-nav |
| `system:databricks_warehouses` | SQL Warehouses | Warehouses sub-nav |
| `system:databricks_jobs` | Jobs | Jobs sub-nav |
| `system:databricks_queries` | Queries | Queries sub-nav |
| `system:databricks_pipelines` | DLT Pipelines | Pipelines sub-nav |
| `system:databricks_cost` | Cost & Usage | Cost sub-nav |
| `system:databricks_lakebase` | Lakebase | Lakebase sub-nav |
| `system:databricks_ai_gateway` | AI Gateway | AI Gateway sub-nav |
| `system:databricks_quality` | Data Quality | Data Quality sub-nav |

---

## Notes

- If a portlet doesn't appear in the Add View picker, check that your role is listed under the portlet's **Relevant Roles** in the Definitions editor. All portlets are configured for Administrator, Operator, Advanced Operator, Dashboard Designer, Dashboard User, and General Access.
- The **Databricks** nav entry is managed by the cartridge — do not manually create a module with the same name.
- Time-plot portlets (Active Resource Trend, Query Volume Trend, Daily DBU Accumulation) require at least two collection cycles before the chart renders.
- Sparkline portlets require multiple cycles of history before the mini-charts populate.
