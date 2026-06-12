# Foglight for Databricks — Session Bootstrap

Read this file at the start of every new Claude Code window, then pick up from **Current Work** below.

---

## What This Project Is

Custom Quest Foglight monitoring cartridge (.car) + FglAM Java agent for Databricks workspaces.
Collects topology and metrics from the Databricks REST API and Unity Catalog system tables.
Surfaced as WCF portlets inside Foglight dashboards.

**Build:** `python build_cartridge.py X.X.X` → `target/DatabricksAgent-X.X.X.car`
**Current version:** 1.0.158 (confirmed working and deployed)

**Codebase:** `C:\Users\mark_\OneDrive\Claude\foglight-databricks\`

---

## Repo

- `origin` → `https://github.com/rabbitburns/Foglight-for-Databricks.git`
- `work`   → `https://github.com/Mark-Gowdy_questsw/Foglight-for-Databricks.git`
  - work PAT expires July 26 2026 — needs `repo` + `workflow` scopes

---

## What's Been Built

| Tier | Content | Status |
|---|---|---|
| 1–3 | Clusters, Jobs, Job Runs, SQL Warehouses, Queries, DLT Pipelines, Instance Pools, Overview | ✓ Done |
| 4 | DBU consumption & cost (`system.billing.usage`), SKU list prices, treemap + bubble charts | ✓ Done |
| 5 | DLT Pipeline Updates + Data Quality Expectations | ✓ Built, untested (no DLT in dev workspace) |
| 7 | Model Serving Endpoints + Served Models | ✓ Done |
| 8 | Lakebase Projects + Branches (2 portlets — views 55, 57) | ✓ Done |
| 11 | AI Gateway Observability — Endpoints, Token Usage, User Activity (3 portlets — views 59, 61, 63) | ✓ Done |
| Sparklines | Job, Warehouse, Cluster sparklines (views 74, 75, 76) | ✓ Done |
| 6 | Cluster runtime metrics from `system.compute.node_timeline` — cpuUtil/memUtil Metrics; Cluster Utilization portlet (view 77) | ✓ Done |
| FinOps | Cost columns on Jobs, Pipelines, AI Endpoints; Idle + Untagged cluster reports (78, 79); User Compute Spend (80); Warehouse Efficiency; Cost treemap; `$%,.2f` formatting | ✓ Done |
| Storage | Table Optimization History (view 81); Storage Costs by product/SKU (view 82) | ✓ Done |
| Tufte | Cost MoM Slopegraph — prev vs current month cost, ▲/▼/→ trend (view 83) | ✓ Done |

38 WCF portlets. Next available view/script IDs: **84/84** (`last-entity-id="83"` on main module).

---

## Architecture

```
FglAM Java Agent (ClusterCollector.java, 60s polling)
  → Databricks REST API + system.billing.* + system.compute.* SQL
  → Foglight Topology Store (CDT transformation)
  → WCF Portlets (Groovy scripts)
```

**Topology root:** `DatabricksModelRoot → DatabricksAccount → DatabricksWorkspace → [children]`

**Key files:**
- `src/main/java/com/quest/foglight/databricks/ClusterCollector.java` — main Java collector
- `src/main/java/com/quest/foglight/databricks/DatabricksClient.java` — REST API client
- `assembly/topology/topology-types.xml` — all topology type definitions
- `assembly/topology/cdt.xml` — CDT transformation (DOCTYPE line is REQUIRED — never remove it)
- `assembly/wcf/system/databricks/wcf.xml` — main WCF module (views 1–83)
- `assembly/wcf/system/databricks_*/wcf.xml` — sub-module nav entries

---

## Hard-Won Rules (do not break these)

- **CDT DOCTYPE**: `cdt.xml` MUST keep `<!DOCTYPE topology-adapter ...>` — removing it silently kills all data
- **Never reinstall same version**: always increment version number or CDT unregisters and won't re-register
- **StringObservation**: state fields must be plain `String`, not `StringObservation` — CDT mapping fails at runtime
- **WCF align**: `align` in wcf.grid2 windows MUST be `stretch` — `top` causes nav module registration failure
- **JiBX ordering**: all `<view>` elements MUST precede all `<script-function>` elements in wcf.xml
- **xml:space**: `<value xml:space="preserve">` is REQUIRED on all `<value>` inside `<string-rv>` in time-plot views
- **Groovy traversal**: always traverse from `DatabricksWorkspace`, never `ts.getObjectsOfType("DatabricksCluster")` — containment children aren't returned by top-level queries

---

## Current Work — Tufte visualization improvements

Views 81–83 landed in 1.0.158. Next options:
- **Tufte table upgrades** — see "What's next / Tufte" section below
- **Tier 10** — Lakehouse Monitoring
- **Model serving metrics** — latency/throughput from Databricks metrics API

---

## Backlog (priority order)

1. **Tufte upgrades** — Job Duration range plot (min/avg/max per job); User Spend dot plot; small multiples daily cost by product
2. **Tier 10** — Lakehouse Monitoring (monitor inventory + drift metrics)
3. **Model serving metrics** — per-endpoint latency/throughput from Databricks metrics API
4. **Tier 9** — Lakewatch SIEM (blocked: Private Preview, no public API)
5. **AUI nav icon** — verify custom.svg approach or await Quest platform team guidance

---

## Full Memory

For complete WCF patterns, CDT format, sub-module nav rules, and version history:
`C:\Users\mark_\.claude\projects\C--Users-mark--OneDrive-Claude\memory\project_foglight_databricks.md`
