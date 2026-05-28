# Foglight for Databricks — Session Bootstrap

Read this file at the start of every new Claude Code window, then pick up from **Current Work** below.

---

## What This Project Is

Custom Quest Foglight monitoring cartridge (.car) + FglAM Java agent for Databricks workspaces.
Collects topology and metrics from the Databricks REST API and Unity Catalog system tables.
Surfaced as WCF portlets inside Foglight dashboards.

**Build:** `python build_cartridge.py X.X.X` → `target/DatabricksAgent-X.X.X.car`
**Current version:** 1.0.113 (confirmed working and deployed)

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

27 WCF portlets. Next available view/script IDs: **55/55** (`last-entity-id="54"` on main module).

---

## Architecture

```
FglAM Java Agent (ClusterCollector.java, 60s polling)
  → Databricks REST API + system.billing.* SQL
  → Foglight Topology Store (CDT transformation)
  → WCF Portlets (Groovy scripts)
```

**Topology root:** `DatabricksModelRoot → DatabricksAccount → DatabricksWorkspace → [children]`

**Key files:**
- `src/main/java/com/quest/foglight/databricks/ClusterCollector.java` — main Java collector
- `assembly/topology/topology-types.xml` — all topology type definitions
- `assembly/topology/cdt.xml` — CDT transformation (DOCTYPE line is REQUIRED — never remove it)
- `assembly/wcf/system/databricks/wcf.xml` — main WCF module (views 1–54)
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

## Current Work — Tier 8: Lakebase Platform Monitoring

**Goal:** Surface Lakebase project/branch/endpoint inventory as Foglight topology. Complements the existing Foglight PostgreSQL cartridge (which handles per-branch query monitoring).

**API endpoints:**
- `GET /api/2.0/postgres/projects` → project list
- `GET /api/2.0/postgres/projects/{id}/branches` → branch list per project
- `GET /api/2.0/postgres/projects/{id}/branches/{id}/endpoints` → endpoints per branch
- `GET /api/2.0/postgres/projects/{id}/operations` → in-flight ops (create/clone/restore)

**New topology types to add** (in `topology-types.xml`):
- `DatabricksLakebaseProject` — under `DatabricksWorkspace.lakebaseProjects`
- `DatabricksLakebaseBranch` — under `DatabricksLakebaseProject.branches`

**New portlets** (~4, views 55+):
- Lakebase Projects (project name, state, timestamps)
- Lakebase Branches (branch name, state, parent branch, created time)
- Lakebase Endpoints (endpoint state, size, URL)
- Lakebase Operations (in-flight op type, state, duration — surfaces stuck provisioning)

**Note:** `tools/lakebase-branch-discovery/branch_to_foglight.py` already exists — it's a standalone agent-onboarding script (writes foglight_agents.csv). Tier 8 is the FMS topology layer that complements it.

---

## Next After Tier 8

- **Tier 11** — AI Gateway Observability (`system.ai_gateway.usage`); new types `DatabricksAiEndpoint`, `DatabricksAiUsage`, `DatabricksAiUserActivity`; 7 portlets; requires account-admin access
- **Tier 6** — Cluster runtime metrics (`system.compute.node_timeline`)

---

## Full Memory

For complete WCF patterns, CDT format, sub-module nav rules, and version history:
`C:\Users\mark_\.claude\projects\C--Users-mark--OneDrive-Claude\memory\project_foglight_databricks.md`
