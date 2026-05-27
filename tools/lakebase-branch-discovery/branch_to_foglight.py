#!/usr/bin/env python3
"""
Lakebase Branch → Foglight discovery job.

- Lists Lakebase projects → branches → endpoints via the Databricks Lakebase Postgres API
  and emits:
    * output/foglight_agents.csv
    * output/bootstrap.sql

- Reads configuration from config.yaml (in the same directory) and allows the Actions
  workflow to overwrite `workspace_url` / `access_token` by editing that file before execution.

- Supports `--dry-run` to validate access and print a small summary without writing files.
"""

from __future__ import annotations

import argparse
import csv
import json
import os
import sys
from typing import Dict, List, Any

import requests
import yaml

API_BASE = "/api/2.0/postgres"


# ---------------------------
# Utilities
# ---------------------------

def _bare_id(val: str | None) -> str | None:
    """
    Normalize a Lakebase resource 'name', removing known prefixes and returning only the bare ID.
    Examples:
      'projects/foo'        -> 'foo'
      'branches/dev'        -> 'dev'
      'endpoints/primary'   -> 'primary'
      'projects/projects/x' -> 'x'
      'foo'                 -> 'foo'
    """
    if not isinstance(val, str) or not val:
        return val
    # strip any known prefix repeatedly
    while True:
        if val.startswith("projects/"):
            val = val.split("/", 1)[1]
            continue
        if val.startswith("branches/"):
            val = val.split("/", 1)[1]
            continue
        if val.startswith("endpoints/"):
            val = val.split("/", 1)[1]
            continue
        break
    # finally, if anything remains like 'a/b/c', keep the last segment
    if "/" in val:
        val = val.rsplit("/", 1)[-1]
    return val


def _get_cfg_path() -> str:
    here = os.path.dirname(os.path.abspath(__file__))
    return os.path.join(here, "config.yaml")


def load_config() -> Dict[str, Any]:
    cfg_path = _get_cfg_path()
    if not os.path.isfile(cfg_path):
        raise FileNotFoundError(f"Config file not found: {cfg_path}")
    with open(cfg_path, "r", encoding="utf-8") as f:
        cfg = yaml.safe_load(f) or {}
    cfg.setdefault("workspace_url", "")
    cfg.setdefault("access_token", "")
    cfg.setdefault("project_id_filter", [])
    cfg.setdefault("include_tags", [])
    cfg.setdefault("output_dir", "output")
    cfg.setdefault("postgres", {})
    cfg["postgres"].setdefault("database", "databricks_postgres")
    cfg["postgres"].setdefault("port", 5432)
    cfg["postgres"].setdefault("sslmode", "require")
    cfg["postgres"].setdefault("monitoring_user", "foglight_monitor")
    return cfg


def api_get(url: str, headers: Dict[str, str]) -> Dict[str, Any]:
    r = requests.get(url, headers=headers, timeout=30)
    try:
        r.raise_for_status()
    except requests.HTTPError as e:
        try:
            body = r.json()
        except Exception:
            body = r.text[:1000]
        raise requests.HTTPError(f"GET {url} → HTTP {r.status_code}\n{body}") from e
    try:
        return r.json()
    except Exception as e:
        raise ValueError(f"GET {url} returned non-JSON body") from e


def ensure_outdir(path: str) -> None:
    os.makedirs(path, exist_ok=True)


# ---------------------------
# Discovery
# ---------------------------

def discover(cfg: Dict[str, Any], *, dry_run: bool = False) -> Dict[str, Any]:
    host = (cfg["workspace_url"] or "").rstrip("/")
    token = cfg["access_token"] or ""
    if not host or not token:
        raise ValueError("workspace_url and access_token must both be provided in config.yaml")

    headers = {"Authorization": f"Bearer {token}"}
    out = {
        "agents": [],
        "bootstrap_lines": [],
        "projects_seen": [],
        "branches_seen": [],
    }

    # Optional filter
    filter_set = set(str(x).strip() for x in cfg.get("project_id_filter", []) if str(x).strip())

    # List projects
    projects_url = f"{host}{API_BASE}/projects"
    data = api_get(projects_url, headers)
    projects = data.get("projects", []) or []

    for p in projects:
        pid = _bare_id(p.get("project_id") or p.get("name"))
        if not pid:
            continue
        if filter_set and pid not in filter_set:
            continue

        out["projects_seen"].append(pid)

        # List branches for project
        branches_url = f"{host}{API_BASE}/projects/{pid}/branches"
        b_resp = api_get(branches_url, headers)
        branches = b_resp.get("branches", []) or []

        for b in branches:
            bid = _bare_id(b.get("branch_id") or b.get("name") or "production")
            if not bid:
                continue
            out["branches_seen"].append((pid, bid))

            # List endpoints for branch
            eps_url = f"{host}{API_BASE}/projects/{pid}/branches/{bid}/endpoints"
            e_resp = api_get(eps_url, headers)
            endpoints = e_resp.get("endpoints", []) or []

            for ep in endpoints:
                endpoint_label = _bare_id(ep.get("endpoint_id") or ep.get("name") or "")
                ep_host = (ep.get("status") or {}).get("hosts", {}).get("host") or ""

                out["agents"].append({
                    "project": pid,
                    "branch": bid,
                    "endpoint": endpoint_label,
                    "host": ep_host,
                    "port": cfg["postgres"]["port"],
                    "database": cfg["postgres"]["database"],
                    "sslmode": cfg["postgres"]["sslmode"],
                    "monitoring_user": cfg["postgres"]["monitoring_user"],
                })

            # Per-database bootstrap (idempotent)
            out["bootstrap_lines"].append(f"-- {pid}/{bid}")
            out["bootstrap_lines"].append("CREATE EXTENSION IF NOT EXISTS pg_stat_statements;")
            out["bootstrap_lines"].append("SELECT set_config('track_io_timing','on', false);")
            out["bootstrap_lines"].append("")

    if dry_run:
        print("== DRY RUN SUMMARY ==")
        print("Projects:", out["projects_seen"])
        print("Branches:", out["branches_seen"])
        print(f"Agents to write: {len(out['agents'])}")
        return out

    # Write outputs
    outdir = cfg.get("output_dir") or "output"
    ensure_outdir(outdir)

    csv_path = os.path.join(outdir, "foglight_agents.csv")
    with open(csv_path, "w", newline="", encoding="utf-8") as f:
        fieldnames = [
            "project", "branch", "endpoint",
            "host", "port", "database", "sslmode", "monitoring_user"
        ]
        w = csv.DictWriter(f, fieldnames=fieldnames)
        w.writeheader()
        w.writerows(out["agents"])

    sql_path = os.path.join(outdir, "bootstrap.sql")
    with open(sql_path, "w", encoding="utf-8") as f:
        f.write("\n".join(out["bootstrap_lines"]))

    print(f"Wrote: {csv_path}")
    print(f"Wrote: {sql_path}")
    return out


# ---------------------------
# CLI
# ---------------------------

def parse_args(argv: List[str]) -> argparse.Namespace:
    p = argparse.ArgumentParser(description="Lakebase → Foglight discovery")
    p.add_argument("--dry-run", action="store_true", help="Enumerate & print summary without writing files")
    p.add_argument("--debug", action="store_true", help="Print effective config and minor diagnostics")
    return p.parse_args(argv)


def main(argv: List[str] | None = None) -> int:
    args = parse_args(argv or sys.argv[1:])
    cfg = load_config()

    if args.debug or args.dry_run:
        print("Effective config:", json.dumps({
            "workspace_url": cfg.get("workspace_url", "****"),
            "access_token": "****",
            "project_id_filter": cfg.get("project_id_filter", []),
            "include_tags": cfg.get("include_tags", []),
            "output_dir": cfg.get("output_dir", "output"),
            "postgres": cfg.get("postgres", {}),
        }, indent=2))

    try:
        discover(cfg, dry_run=args.dry_run)
    except Exception as e:
        print(f"[ERROR] {e}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
