# Foglight for Databricks — Launch Considerations

## Versioning & Branding

### Current State

The internal build version is `1.0.183`. This number is used by FglAM (Foglight Agent Manager) to detect when a new agent package needs to be downloaded. It must always increase — FglAM will refuse to install a lower version without a full uninstall first.

### The Question

Should the product be rebranded as a beta (`v0.x`) before going to `v1.0`, rather than shipping what is currently labelled `1.0.x`?

### Options

**Option A — Decouple product version from build number (recommended)**

Keep the Foglight internal build version incrementing (`1.0.184`, `1.0.185`...) — FglAM requires this. Separately, adopt a **product label** (`v0.9 Beta`, eventually `v1.0`) in all docs, README, and customer-facing material. The two are independent.

- No risk to upgrade path — existing installs continue to receive updates
- README and docs say e.g. "v0.9 Beta — build 1.0.183"
- When the product is declared generally available, docs say `v1.0`; build counter continues as `1.0.200` or whatever it reaches
- Zero code changes required

**Option B — Fresh cartridge identity**

Rename the cartridge from `DatabricksAgent` to something like `FoglightForDatabricks`, start versioning at `0.183.0`. Because the cartridge name changes, FglAM treats it as a new product — no downgrade conflict. The old `DatabricksAgent` cartridge must be manually uninstalled first.

- Clean break with proper semver (`0.x` → `1.0`)
- One-time manual migration required for any existing installs
- Significant effort: rename in `manifest.xml`, `agent.manifest`, topology type names (`DatabricksAgent` → new name), WCF module IDs, `build_cartridge.py`, all docs
- Topology objects created under the old cartridge name would be orphaned

**Recommendation:** Option A. The internal build number is an implementation detail — customers don't need to see `1.0.183`. Docs and README carry the product version label independently.

---

## What "v1.0" Should Mean

Before declaring v1.0, consider defining the GA bar. Suggested criteria:

| Criterion | Status |
|---|---|
| Core monitoring (clusters, jobs, warehouses, queries, pipelines) | ✓ Done |
| Cost & DBU intelligence | ✓ Done |
| Cluster runtime metrics (CPU/memory) | ✓ Done |
| AI Gateway observability | ✓ Done |
| Lakehouse Monitor data quality (row count + drift) | ✓ Done |
| Lakebase platform monitoring | ✓ Done |
| DLT data quality expectations tested in live DLT environment | Not yet — DLT not in dev workspace |
| Row count delta (Change column) | ✓ Done (1.0.183) |
| Second FMS install validated (186w862a) | Pending |
| Multi-workspace support | Deferred |
| Packaged dashboards (no manual setup required) | Deferred to v2 |
| AUI dashboard layer | Deferred to v2 |

---

## Naming

Current internal name: `DatabricksAgent` / cartridge display: `Foglight for Databricks`

Options for the product name:
- **Foglight for Databricks** — consistent with Quest naming conventions (Foglight for SQL Server, Foglight for Oracle, etc.)
- **Quest Databricks Monitor** — more descriptive, less platform-specific
- **Foglight Databricks Cartridge** — accurate but not marketing-friendly

**Recommendation:** Keep `Foglight for Databricks`. It follows the established Quest product naming pattern and is already used in all docs.

---

## Release Packaging

Currently the `.car` file is committed to `cartridges/` in git and distributed from there (GitHub Actions hosted runners are disabled in the corporate org).

For a v1.0 release, consider:
- **GitHub Releases** — attach the `.car` as a release asset; update README download link from `cartridges/` to the Releases page
- **Quest software distribution** — determine if the cartridge needs to go through Quest's internal software delivery pipeline, signing, or approval process
- **Versioned release notes** — ROADMAP.md serves this purpose today; a formal CHANGELOG.md or GitHub Release description may be needed for customer-facing releases

---

## Open Pre-Launch Items

| Item | Priority | Notes |
|---|---|---|
| Validate on second FMS (186w862a) | High | Install 1.0.183.car and confirm all portlets render |
| DLT data quality expectations | Medium | Code complete; untested — DLT not in dev workspace |
| rowCountDelta (Change) validation | Medium | Just implemented (1.0.183); needs a cycle where row count actually changes |
| Credential rotation | High | `Access Key.txt`, `github_PAT.txt`, `Claude Code - Java Agent.txt` were in git history — those tokens should be rotated |
| Quest internal review / approval | TBD | Determine if there is a Quest PM or legal review gate before external release |
| Pricing / licensing model | TBD | Is this included in Foglight base, a paid add-on, or free/community? |
| Support model | TBD | Who handles customer issues? Quest support queue or GitHub issues only? |
