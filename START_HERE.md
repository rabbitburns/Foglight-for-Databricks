# Foglight for Databricks — Session Bootstrap

Read this file at the start of every new Claude Code window, then pick up from **Current Work** below.

---

## What This Project Is

Custom Quest Foglight monitoring cartridge (.car) + FglAM Java agent for Databricks workspaces.
Collects topology and metrics from the Databricks REST API and Unity Catalog system tables.
Surfaced as WCF portlets inside Foglight dashboards via a built-in top-level nav entry.

**Build:** `python build_cartridge.py X.X.X --deploy` → `target/DatabricksAgent-X.X.X.car` + deployed to FglAM agent cache + copied to `cartridges/`  
**Current version:** 1.0.181

**Codebase:** `C:\Users\mark_\OneDrive\Claude\foglight-databricks\`

---

## Repo

- `origin` → `https://github.com/rabbitburns/Foglight-for-Databricks.git`
- `work`   → `https://github.com/Mark-Gowdy_questsw/Foglight-for-Databricks.git`
  - work PAT expires July 26 2026 — needs `repo` + `workflow` scopes
- No GitHub Actions (hosted runners disabled in corp org) — `.car` committed to `cartridges/` after each build

---

## What's Been Built

| Tier | Content | Status |
|---|---|---|
| 1–3 | Clusters, Jobs, Job Runs, SQL Warehouses, Queries, DLT Pipelines, Instance Pools, Overview | ✓ Done |
| 4 | DBU consumption & cost (`system.billing.usage`), SKU list prices, user compute spend, treemap + bubble charts, cost columns on jobs/pipelines/AI, MoM slopegraph | ✓ Done |
| 5 | DLT Pipeline Updates + Data Quality Expectations | ✓ Built, untested (no DLT in dev workspace) |
| 6 | Cluster runtime metrics from `system.compute.node_timeline` — cpuUtil/memUtil Metrics; Cluster Utilization portlet (view 77) | ✓ Done |
| 7 | Model Serving Endpoints + Served Models | ✓ Done |
| 8 | Lakebase Projects + Branches (views 55, 57) | ✓ Done (3 projects, 5 branches confirmed) |
| 10 | Lakehouse Monitoring DQ portlet (view 84) — monitor inventory, row count, drifted column count | ✓ Done |
| 11 | AI Gateway Observability — Endpoints, Token Usage, User Activity (views 59, 61, 63) | ✓ Done |
| Sparklines | Job, Warehouse, Cluster sparklines (views 74, 75, 76) | ✓ Done |
| Hygiene | Idle Clusters (78), Untagged Clusters (79); Table Optimization History (81), Storage Costs (82) | ✓ Done |

**~45 WCF portlets across 9 sub-nav pages.** Next available view/script IDs: **85/85**.

---

## Architecture

```
FglAM Java Agent (ClusterCollector.java, 60s polling)
  → Databricks REST API + system.billing.* + system.compute.* + system.ai_gateway.* SQL
  → _profile_metrics / _drift_metrics SQL (Lakehouse Monitoring)
  → Foglight Topology Store (CDT transformation)
  → WCF Portlets (Groovy scripts)
```

**Topology root:** `DatabricksModelRoot → DatabricksAccount → DatabricksWorkspace → [children]`

**Key files:**
- `src/main/java/com/quest/foglight/databricks/ClusterCollector.java` — main Java collector (~1300 lines)
- `src/main/java/com/quest/foglight/databricks/DatabricksClient.java` — REST API client
- `src/main/java/com/quest/foglight/databricks/DatabricksAgent.java` — FglAM agent lifecycle
- `assembly/topology/topology-types.xml` — all topology type definitions
- `assembly/topology/cdt.xml` — CDT transformation (DOCTYPE line is REQUIRED — never remove it)
- `assembly/wcf/system/databricks/wcf.xml` — main WCF module (views 1–84)
- `assembly/wcf/system/databricks_*/wcf.xml` — sub-module nav entries (9 sub-navs)

---

## Hard-Won Rules (do not break these)

- **CDT DOCTYPE**: `cdt.xml` MUST keep `<!DOCTYPE topology-adapter ...>` — removing it silently kills all data
- **Never reinstall same version**: always increment version number or CDT unregisters and won't re-register
- **StringObservation**: state fields must be plain `String`, not `StringObservation` — CDT mapping fails at runtime
- **WCF align**: `align` in wcf.grid2 windows MUST be `stretch` — `top` causes nav module registration failure
- **JiBX ordering**: all `<view>` elements MUST precede all `<script-function>` elements in wcf.xml
- **xml:space**: `<value xml:space="preserve">` is REQUIRED on all `<value>` inside `<string-rv>` in time-plot views
- **Groovy traversal**: always traverse from `DatabricksWorkspace`, never `ts.getObjectsOfType("DatabricksCluster")` — containment children aren't returned by top-level queries
- **FglAM restart**: changing the JAR requires killing the FglAM Java process (not a Windows service — Quest Watchdog manages it). Agent stop/start alone does NOT reload the classloader.
- **CDT in deployed files**: CDT transforms must be in both `assembly/topology/cdt.xml` (source) AND the deployed copy at `C:\Quest\Foglight\state\cartridge.exploded\DatabricksAgent-1_0_{ver}\...\databricks-model-root-cdt.xml`. Installing a new `.car` updates the deployed copy. Direct patching only works until next install.

---

## Current Work

**1.0.181 deployed.** Data Quality portlet (view 84) now shows:
- ✓ **Last Run** — from `_profile_metrics` `MAX(window.start)`
- ✓ **Row Count** — from `_profile_metrics` `MAX(count)` where `column_name=':table'`
- ✓ **Drifted Cols** — from `_drift_metrics` chi-square + KS test `pvalue < 0.05` (fix: field is `pvalue` not `p_value`)
- ✗ **Change** — `rowCountDelta` hardcoded `""`, needs prev-run comparison logic

**Pending verification:** confirm Drifted Cols populates after 1.0.181 install + FglAM restart.

---

## Backlog (priority order)

1. **rowCountDelta (Change column)** — compare current vs previous `_profile_metrics` row count per table; needs in-memory prev-run state in collector
2. **Model serving metrics** — per-endpoint latency/throughput from Databricks metrics API
3. **Tier 9** — Lakewatch SIEM (blocked: Private Preview, no public API)
4. **AUI nav icon** — custom.svg workaround or await Quest platform team guidance
5. **Multi-workspace** — numbered config pairs (`workspace.1.url`, etc.)

---

## Full Memory

For complete WCF patterns, CDT format, sub-module nav rules, and version history:
`C:\Users\mark_\.claude\projects\C--Users-mark--OneDrive-Claude\memory\project_foglight_databricks.md`
