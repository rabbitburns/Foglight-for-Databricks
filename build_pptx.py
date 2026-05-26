"""
Generate ENABLEMENT_DECK.pptx from slide content.
Run: python build_pptx.py
"""
from pptx import Presentation
from pptx.util import Inches, Pt, Emu
from pptx.dml.color import RGBColor
from pptx.enum.text import PP_ALIGN
from pptx.util import Inches, Pt
import pptx.oxml.ns as nsmap
from lxml import etree

# ── Colours ────────────────────────────────────────────────────────────────
NAVY       = RGBColor(0x0a, 0x16, 0x28)
BLUE       = RGBColor(0x00, 0x66, 0xCC)
CYAN       = RGBColor(0x00, 0xB4, 0xD8)
LTBLUE     = RGBColor(0x4D, 0x9D, 0xE0)
GREEN      = RGBColor(0x2D, 0xC6, 0x53)
ORANGE     = RGBColor(0xF4, 0xA2, 0x61)
RED        = RGBColor(0xE6, 0x39, 0x46)
WHITE      = RGBColor(0xF0, 0xF4, 0xFF)
GREY       = RGBColor(0xA0, 0xB4, 0xCC)
CARD_FILL  = RGBColor(0x11, 0x1E, 0x35)

W = Inches(13.33)
H = Inches(7.5)


def new_prs():
    prs = Presentation()
    prs.slide_width  = W
    prs.slide_height = H
    return prs


def blank_slide(prs):
    layout = prs.slide_layouts[6]   # completely blank
    slide = prs.slides.add_slide(layout)
    bg = slide.background.fill
    bg.solid()
    bg.fore_color.rgb = NAVY
    return slide


def add_text(slide, text, left, top, width, height,
             size=18, bold=False, color=WHITE, align=PP_ALIGN.LEFT,
             italic=False, wrap=True):
    txBox = slide.shapes.add_textbox(left, top, width, height)
    tf = txBox.text_frame
    tf.word_wrap = wrap
    p = tf.paragraphs[0]
    p.alignment = align
    run = p.add_run()
    run.text = text
    run.font.size = Pt(size)
    run.font.bold = bold
    run.font.italic = italic
    run.font.color.rgb = color
    return txBox


def add_rect(slide, left, top, width, height, fill=CARD_FILL, line=CYAN, line_width=Pt(0.75)):
    shape = slide.shapes.add_shape(1, left, top, width, height)
    shape.fill.solid()
    shape.fill.fore_color.rgb = fill
    shape.line.color.rgb = line
    shape.line.width = line_width
    return shape


def add_eyebrow(slide, text):
    add_text(slide, text.upper(), Inches(0.6), Inches(0.45), Inches(12), Inches(0.4),
             size=10, bold=True, color=CYAN)


def add_heading(slide, text, top=Inches(1.0), size=36):
    add_text(slide, text, Inches(0.6), top, Inches(12.1), Inches(1.2),
             size=size, bold=True, color=WHITE)


def add_divider(slide, top=Inches(0.82)):
    shape = slide.shapes.add_shape(1, Inches(0.6), top, Inches(0.7), Inches(0.05))
    shape.fill.solid()
    shape.fill.fore_color.rgb = CYAN
    shape.line.fill.background()


def card(slide, left, top, width, height, title, body, title_color=CYAN):
    add_rect(slide, left, top, width, height)
    add_text(slide, title, left+Inches(0.15), top+Inches(0.12), width-Inches(0.3), Inches(0.35),
             size=13, bold=True, color=title_color)
    add_text(slide, body, left+Inches(0.15), top+Inches(0.5), width-Inches(0.3), height-Inches(0.6),
             size=11, color=GREY)


def bullet_box(slide, left, top, width, height, title, items, title_color=CYAN):
    add_rect(slide, left, top, width, height)
    add_text(slide, title, left+Inches(0.15), top+Inches(0.1), width-Inches(0.3), Inches(0.35),
             size=13, bold=True, color=title_color)
    body = "\n".join(f"→  {i}" for i in items)
    add_text(slide, body, left+Inches(0.15), top+Inches(0.48), width-Inches(0.3), height-Inches(0.55),
             size=10.5, color=GREY)


def diff_card(slide, left, top, width, height, title, body, vs_items):
    add_rect(slide, left, top, width, height, line=CYAN)
    # cyan left bar
    bar = slide.shapes.add_shape(1, left, top, Inches(0.05), height)
    bar.fill.solid(); bar.fill.fore_color.rgb = CYAN; bar.line.fill.background()
    add_text(slide, title, left+Inches(0.18), top+Inches(0.1), width-Inches(0.25), Inches(0.35),
             size=13, bold=True, color=WHITE)
    add_text(slide, body, left+Inches(0.18), top+Inches(0.48), width-Inches(0.25), Inches(0.55),
             size=10.5, color=GREY)
    y = top + Inches(1.05)
    for label, col in vs_items:
        t = slide.shapes.add_textbox(left+Inches(0.18), y, Inches(3.2), Inches(0.28))
        tf = t.text_frame; p = tf.paragraphs[0]
        run = p.add_run(); run.text = label
        run.font.size = Pt(9); run.font.color.rgb = col; run.font.bold = True
        y += Inches(0.3)


def table_slide(prs, eyebrow, heading, headers, rows, col_widths=None):
    slide = blank_slide(prs)
    add_eyebrow(slide, eyebrow)
    add_heading(slide, heading, top=Inches(0.85), size=30)
    n_cols = len(headers)
    if col_widths is None:
        col_widths = [Inches(12.1/n_cols)] * n_cols
    tbl_top  = Inches(1.85)
    tbl_left = Inches(0.6)
    tbl_h    = Inches(0.38 * (len(rows)+1))
    tbl = slide.shapes.add_table(len(rows)+1, n_cols, tbl_left, tbl_top,
                                  sum(col_widths), tbl_h).table
    for i, w in enumerate(col_widths):
        tbl.columns[i].width = w
    for ci, hdr in enumerate(headers):
        cell = tbl.cell(0, ci)
        cell.text = hdr
        cell.fill.solid(); cell.fill.fore_color.rgb = RGBColor(0x00, 0x44, 0x88)
        p = cell.text_frame.paragraphs[0]
        p.runs[0].font.size = Pt(10); p.runs[0].font.bold = True
        p.runs[0].font.color.rgb = LTBLUE
    for ri, row in enumerate(rows):
        for ci, val in enumerate(row):
            cell = tbl.cell(ri+1, ci)
            cell.text = str(val)
            cell.fill.solid()
            cell.fill.fore_color.rgb = CARD_FILL if ri % 2 == 0 else NAVY
            p = cell.text_frame.paragraphs[0]
            color = WHITE if ci == 0 else (GREEN if str(val).startswith("✓") else (RED if str(val).startswith("✗") else GREY))
            p.runs[0].font.size = Pt(10)
            p.runs[0].font.color.rgb = color
    return slide


# ═══════════════════════════════════════════════════════════════════════════
prs = new_prs()

# ── Slide 1: Title ──────────────────────────────────────────────────────────
s = blank_slide(prs)
# gradient feel — two semi-transparent rects
for ox, oy, c in [(0, H*0.2, BLUE), (W*0.6, 0, CYAN)]:
    sh = s.shapes.add_shape(1, int(ox-Inches(2)), int(oy-Inches(2)), Inches(6), Inches(6))
    sh.fill.solid(); sh.fill.fore_color.rgb = c
    sh.line.fill.background()
    # make semi-transparent via XML
    spPr = sh._element.spPr
    solidFill = spPr.find('.//{http://schemas.openxmlformats.org/drawingml/2006/main}solidFill')
    if solidFill is not None:
        srgb = solidFill.find('{http://schemas.openxmlformats.org/drawingml/2006/main}srgbClr')
        if srgb is not None:
            alpha = etree.SubElement(srgb, '{http://schemas.openxmlformats.org/drawingml/2006/main}alpha')
            alpha.set('val', '15000')

add_text(s, "QUEST SOFTWARE", Inches(0.7), Inches(0.6), Inches(6), Inches(0.4),
         size=11, bold=True, color=BLUE)
add_text(s, "Foglight for", Inches(0.7), Inches(1.5), Inches(9), Inches(1.0),
         size=54, bold=True, color=WHITE)
add_text(s, "Databricks", Inches(0.7), Inches(2.4), Inches(9), Inches(1.0),
         size=54, bold=True, color=CYAN)
add_divider(s, top=Inches(3.5))
add_text(s, "Unified Lakehouse observability — compute, jobs, queries,\ncost intelligence, data quality, and model serving — natively inside Foglight.",
         Inches(0.7), Inches(3.7), Inches(8), Inches(1.2), size=18, color=GREY)
add_text(s, "Enablement Overview · v1.0.113  |  POC · May 2026",
         Inches(0.7), Inches(5.2), Inches(8), Inches(0.4), size=12, color=GREY)
add_text(s, "CONFIDENTIAL", Inches(11.5), Inches(7.1), Inches(1.6), Inches(0.3),
         size=9, color=RGBColor(0x44,0x55,0x66))

# ── Slide 2: The Problem ────────────────────────────────────────────────────
s = blank_slide(prs)
add_eyebrow(s, "The Problem")
add_heading(s, "Databricks is a black box\ninside your monitoring estate", size=30)
cw = Inches(3.9); ch = Inches(2.5); ct = Inches(2.2)
card(s, Inches(0.6), ct, cw, ch, "No single pane of glass",
     "Job failures, cluster anomalies, and slow queries are visible only in the Databricks UI — workspace-scoped, no cross-workspace view, no integration with the rest of your monitored infrastructure.")
card(s, Inches(4.72), ct, cw, ch, "Cost surprises at month end",
     "DBU consumption and estimated costs are buried in system tables that require SQL expertise to interrogate. Teams learn about cost overruns from their cloud bill, not from monitoring alerts.")
card(s, Inches(8.83), ct, cw, ch, "Silent data reliability failures",
     "DLT pipeline data quality expectations fail quietly. Data consumers find the problem before the engineering team does — from bad BI reports or failed ML model inputs.")
add_rect(s, Inches(0.6), Inches(5.0), Inches(12.1), Inches(0.9), line=ORANGE)
add_text(s, "The result:  Databricks teams operate reactively. Incidents are reported by downstream consumers, cost spikes discovered at billing time, data quality failures invisible until they cause business impact.",
         Inches(0.8), Inches(5.05), Inches(11.8), Inches(0.8), size=12, color=WHITE)
add_text(s, "CONFIDENTIAL", Inches(11.5), Inches(7.1), Inches(1.6), Inches(0.3), size=9, color=RGBColor(0x44,0x55,0x66))

# ── Slide 3: Why Existing Tools Fall Short ──────────────────────────────────
s = blank_slide(prs)
add_eyebrow(s, "Market Landscape")
add_heading(s, "Why existing tools fall short", size=32)
cw = Inches(3.9); ct = Inches(1.85); ch = Inches(3.5)
card(s, Inches(0.6), ct, cw, ch, "Datadog — Cost visibility = paid add-on",
     "DBU cost monitoring requires the Cloud Cost Management SKU — a separately priced product pulling from cloud provider billing APIs. Not included in standard Databricks monitoring.\n\nData quality: No DLT expectation monitoring.",
     title_color=RED)
card(s, Inches(4.72), ct, cw, ch, "New Relic — System table licence required",
     "Cost dashboards use system.billing.usage but require a Databricks system table licence to be enabled. Limited differentiation from our approach.\n\nModel serving: No endpoint monitoring.",
     title_color=ORANGE)
card(s, Inches(8.83), ct, cw, ch, "Databricks UI — Siloed and workspace-scoped",
     "The native UI provides rich detail per workspace but has no cross-workspace aggregation, no alert routing, and no RBAC alignment with the broader monitoring platform.\n\nMonitoring teams cannot monitor without a Databricks account.",
     title_color=LTBLUE)
add_rect(s, Inches(0.6), Inches(5.55), Inches(12.1), Inches(0.85), line=CYAN,
         fill=RGBColor(0x05, 0x1A, 0x30))
add_text(s, "The gap:  No existing tool provides Databricks-native cost intelligence, DLT data quality monitoring, and model serving observability in a single platform — inside the monitoring tool organisations already own.",
         Inches(0.8), Inches(5.6), Inches(11.8), Inches(0.75), size=12, color=WHITE)
add_text(s, "CONFIDENTIAL", Inches(11.5), Inches(7.1), Inches(1.6), Inches(0.3), size=9, color=RGBColor(0x44,0x55,0x66))

# ── Slide 4: Solution ───────────────────────────────────────────────────────
s = blank_slide(prs)
add_eyebrow(s, "Solution")
add_heading(s, "Introducing Foglight for Databricks", size=32)
# arch flow
boxes = [
    ("☁", "Databricks", "REST API"),
    ("⚙", "FglAM Agent", "Java · 60s polling"),
    ("🗄", "Topology Store", "Foglight FMS"),
    ("📊", "34 Portlets", "WCF Dashboards"),
]
bw = Inches(2.4); bh = Inches(1.1); by = Inches(1.9)
bx = Inches(0.7)
for i, (icon, lbl, sub) in enumerate(boxes):
    hl = (i in (1, 2))
    add_rect(s, bx, by, bw, bh, line=CYAN, fill=RGBColor(0x05,0x20,0x3A) if hl else CARD_FILL)
    add_text(s, lbl, bx+Inches(0.1), by+Inches(0.15), bw-Inches(0.2), Inches(0.38),
             size=13, bold=True, color=CYAN if hl else WHITE, align=PP_ALIGN.CENTER)
    add_text(s, sub, bx+Inches(0.1), by+Inches(0.55), bw-Inches(0.2), Inches(0.35),
             size=10, color=GREY, align=PP_ALIGN.CENTER)
    bx += bw
    if i < 3:
        add_text(s, "→", bx, by+Inches(0.35), Inches(0.4), Inches(0.4),
                 size=18, color=CYAN, align=PP_ALIGN.CENTER)
        bx += Inches(0.4)
# stats
sw = Inches(2.9); sh2 = Inches(1.4); sy = Inches(3.3)
stats = [("34", "WCF Portlets\nacross 7 monitoring categories"),
         ("60s", "Collection interval\nnear real-time visibility"),
         ("0", "Write operations\nagainst Databricks — read-only"),
         ("1", "PAT token required\nno agents inside Databricks")]
sx = Inches(0.6)
for num, lbl in stats:
    add_rect(s, sx, sy, sw, sh2)
    add_text(s, num, sx, sy+Inches(0.1), sw, Inches(0.7),
             size=40, bold=True, color=CYAN, align=PP_ALIGN.CENTER)
    add_text(s, lbl, sx+Inches(0.1), sy+Inches(0.78), sw-Inches(0.2), Inches(0.55),
             size=10, color=GREY, align=PP_ALIGN.CENTER)
    sx += sw + Inches(0.1)
add_rect(s, Inches(0.6), Inches(5.0), Inches(12.1), Inches(0.8))
add_text(s, "A native Foglight cartridge — installs via the standard .car file mechanism. No new infrastructure, no SaaS subscription, no separate product to buy.",
         Inches(0.8), Inches(5.05), Inches(11.8), Inches(0.7), size=12, color=GREY)
add_text(s, "CONFIDENTIAL", Inches(11.5), Inches(7.1), Inches(1.6), Inches(0.3), size=9, color=RGBColor(0x44,0x55,0x66))

# ── Slide 5: Six Monitoring Pillars ─────────────────────────────────────────
s = blank_slide(prs)
add_eyebrow(s, "Feature Coverage")
add_heading(s, "Seven monitoring pillars", size=32)
pillars = [
    ("Compute", ["Cluster state, node types, Spark version", "Autoscale config, worker/core counts", "SQL Warehouses — type, size, Photon", "Instance Pools — idle/used/max capacity"]),
    ("Jobs & Pipelines", ["Job inventory, trigger type, cron schedule", "Last run state, result, duration breakdown", "Success rate %, avg/min/max duration", "DLT pipeline state + update history", "Data quality expectations — pass/fail/dropped"]),
    ("Query Intelligence", ["Query history — up to 25 per warehouse", "Compilation / execution / fetch breakdown", "Bytes read, rows produced, cache hit", "Full query text, error message", "Slow query leaderboard (top 25)"]),
    ("Cost Intelligence ★", ["DBU usage — 60-day rolling by SKU/product", "Estimated dollar cost joined with list prices", "MoM growth, top jobs by DBU", "DBU treemap + Cost vs DBU bubble chart"]),
    ("Data Quality ★", ["DLT pipeline update history (last 5)", "Per-expectation pass rate %", "Passed, failed, dropped record counts", "Per-flow, per-dataset breakdown"]),
    ("Model Serving", ["Endpoint inventory — state, creator", "Served model name, version, deployment", "Traffic %, scale-to-zero, workload size"]),
    ("AI Gateway ★", ["Token consumption — input/output/cache/reasoning", "Per-requester activity + treemap", "Endpoint latency p50/p90/p95/p99", "Tag-based attribution (team/project)"]),
]
pw = Inches(1.82); ph = Inches(2.2); py = Inches(1.85); px = Inches(0.55)
for i, (title, items) in enumerate(pillars):
    hl = title.endswith("★")
    fill = RGBColor(0x03, 0x1E, 0x38) if hl else CARD_FILL
    lc   = CYAN if hl else RGBColor(0x22, 0x44, 0x66)
    tc   = CYAN if hl else WHITE
    add_rect(s, px, py, pw, ph, fill=fill, line=lc)
    t = title.replace(" ★", "")
    add_text(s, t, px+Inches(0.08), py+Inches(0.08), pw-Inches(0.16), Inches(0.35),
             size=11, bold=True, color=tc)
    body = "\n".join(f"· {it}" for it in items)
    add_text(s, body, px+Inches(0.08), py+Inches(0.45), pw-Inches(0.16), ph-Inches(0.5),
             size=9, color=GREY)
    px += pw + Inches(0.07)
add_text(s, "CONFIDENTIAL", Inches(11.5), Inches(7.1), Inches(1.6), Inches(0.3), size=9, color=RGBColor(0x44,0x55,0x66))

# ── Slide 6: Where We Win ────────────────────────────────────────────────────
s = blank_slide(prs)
add_eyebrow(s, "Competitive Positioning")
add_heading(s, "Where we win", size=32)
diffs = [
    ("DBU Cost Intelligence — Included, Not Add-On",
     "Queries system.billing.usage and system.billing.list_prices directly — Databricks-native, DBU-denominated, 60-day granularity, included at no additional cost.",
     [("Datadog: Paid add-on SKU required", RED), ("New Relic: Requires system table licence", ORANGE), ("Foglight: Included", GREEN)]),
    ("DLT Data Quality Expectations — Industry First",
     "Surfaces DLT expect() rule pass/fail/dropped counts as a native monitoring signal. No other monitoring platform collects this data automatically.",
     [("Datadog: No DLT expectation monitoring", RED), ("New Relic: No DLT expectation monitoring", ORANGE), ("Foglight: Full expectation detail per update", GREEN)]),
    ("AI Gateway Token Observability — GenAI-Native",
     "Token consumption, latency, and error rates from system.ai_gateway.usage — same collection pattern as billing, zero new infrastructure. Neither Datadog nor New Relic surfaces this natively.",
     [("Datadog: No AI Gateway token observability", RED), ("New Relic: No AI Gateway token observability", ORANGE), ("Foglight: Included (Tier 11)", GREEN)]),
]
dy = Inches(1.85); dh = Inches(1.5)
for title, body, vs in diffs:
    diff_card(s, Inches(0.6), dy, Inches(12.1), dh, title, body, vs)
    dy += dh + Inches(0.18)
add_text(s, "CONFIDENTIAL", Inches(11.5), Inches(7.1), Inches(1.6), Inches(0.3), size=9, color=RGBColor(0x44,0x55,0x66))

# ── Slide 7: Competitive Comparison Table ────────────────────────────────────
headers = ["Capability", "Foglight for Databricks", "Datadog", "New Relic", "Databricks UI"]
rows = [
    ["Cluster inventory & state",          "✓ Included", "✓ Yes", "✓ Yes", "✓ Yes"],
    ["Job run history & success rate",     "✓ Included", "✓ Yes", "✓ Yes", "✓ Yes"],
    ["SQL warehouse monitoring",           "✓ Included", "✓ Yes", "✓ Yes", "✓ Yes"],
    ["Query history with full text",       "✓ Included", "Partial", "Partial", "✓ Yes"],
    ["DBU cost monitoring",                "✓ Included · Databricks-native", "Paid add-on SKU", "Requires system table lic.", "Manual SQL only"],
    ["Cost by SKU with list price",        "✓ Included", "✗ No", "✗ No", "✗ No"],
    ["DLT data quality expectations",      "✓ Included · pass/fail/dropped", "✗ No", "✗ No", "✓ Yes (per pipeline)"],
    ["Model serving endpoint monitoring",  "✓ Included", "✓ Yes + metrics", "✗ No", "✓ Yes"],
    ["AI Gateway token observability",     "✓ Planned — Tier 11", "✗ No", "✗ No", "✗ No"],
    ["Integrated with broader IT monitoring","✓ Yes — same Foglight platform","Partial","Partial","✗ No"],
    ["On-premises deployment",             "✓ Yes — FglAM", "✗ SaaS only", "✗ SaaS only", "✗ No"],
]
col_widths = [Inches(3.1), Inches(3.0), Inches(2.0), Inches(2.2), Inches(2.0)]
s = table_slide(prs, "Competitive Comparison", "Feature comparison",
                headers, rows, col_widths)
add_text(s, "CONFIDENTIAL", Inches(11.5), Inches(7.1), Inches(1.6), Inches(0.3), size=9, color=RGBColor(0x44,0x55,0x66))

# ── Slide 8: Business Case ───────────────────────────────────────────────────
s = blank_slide(prs)
add_eyebrow(s, "Value Selling Framework")
add_heading(s, "The business case", size=32)
cols = [
    ("Business Issue", "Databricks environments generate cost, risk, and data quality events invisible to ops. Platform engineers spend hours per week context-switching between Foglight and the Databricks UI.",
     RED, RGBColor(0x1A,0x05,0x08)),
    ("Problem", "No Foglight cartridge exists for Databricks. Monitoring is manual and reactive. Cost overruns discovered at month end. Data quality failures surface via downstream complaints.",
     ORANGE, RGBColor(0x1A,0x10,0x05)),
    ("Solution", "Foglight for Databricks — native cartridge collecting compute, job, query, cost, pipeline, and model serving data every 60s. 34 portlets, no new infrastructure, installs in under 30 minutes.",
     CYAN, RGBColor(0x03,0x18,0x28)),
    ("Value", "Operational: proactive alerting on failures and anomalies.\nFinancial: DBU cost visibility before the cloud bill arrives.\nData reliability: DLT failures surfaced before consumers are impacted.",
     GREEN, RGBColor(0x04,0x14,0x09)),
    ("Why Quest", "Databricks-native cost intelligence included. First platform to surface DLT data quality expectations. Extends existing Foglight investment — no new vendor, no new contract.",
     LTBLUE, RGBColor(0x05,0x12,0x25)),
]
cw2 = Inches(2.44); cy = Inches(1.85); ch2 = Inches(4.5); cx = Inches(0.55)
for title, body, tc, fill in cols:
    add_rect(s, cx, cy, cw2, ch2, fill=fill, line=tc)
    add_text(s, title, cx+Inches(0.12), cy+Inches(0.1), cw2-Inches(0.24), Inches(0.4),
             size=13, bold=True, color=tc)
    add_text(s, body, cx+Inches(0.12), cy+Inches(0.55), cw2-Inches(0.24), ch2-Inches(0.65),
             size=10.5, color=GREY)
    cx += cw2 + Inches(0.05)
add_text(s, "CONFIDENTIAL", Inches(11.5), Inches(7.1), Inches(1.6), Inches(0.3), size=9, color=RGBColor(0x44,0x55,0x66))

# ── Slide 9: Roadmap ─────────────────────────────────────────────────────────
s = blank_slide(prs)
add_eyebrow(s, "Roadmap")
add_heading(s, "What's built · What's next", size=32)
items = [
    ("✓ Done",    GREEN,   "Tiers 1–4: Core Platform Monitoring + Cost Intelligence",
     "Clusters, jobs, warehouses, queries, DBU/cost, graphical widgets, SKU pricing — v1.0.20 through v1.0.65"),
    ("✓ Done",    GREEN,   "Tier 5: DLT Pipeline Depth — Update History + Data Quality Expectations",
     "Per-expectation pass/fail/dropped counts from pipeline events — v1.0.64. Untested pending DLT-enabled environment."),
    ("✓ Done",    GREEN,   "Tier 7: Model Serving Endpoint Inventory",
     "Serving endpoint state, served model config, traffic distribution — v1.0.65"),
    ("→ Next",    CYAN,    "Tier 8: Lakebase Platform Monitoring",
     "Project, branch, and endpoint inventory via Lakebase REST API. Complements the Foglight PostgreSQL cartridge."),
    ("→ Next",    CYAN,    "Tier 11: AI Gateway Observability (Token & GenAI Usage)",
     "Token consumption, latency, per-requester activity from system.ai_gateway.usage. 7 new portlets. Preview enabled."),
    ("◷ Planned", GREY,    "v2: AUI Dashboard Layer",
     "Angular UI framework — sortable tables, charts, drill-down. Requires AUI component library docs from platform team."),
    ("◷ Blocked", GREY,    "Tier 9: Lakewatch Security SIEM",
     "Blocked pending GA and public API. Announced March 2026, currently Private Preview."),
]
ry = Inches(1.85); rh = Inches(0.72)
for status, sc, title, desc in items:
    add_text(s, status, Inches(0.6), ry+Inches(0.05), Inches(1.1), Inches(0.35),
             size=10, bold=True, color=sc)
    add_text(s, title, Inches(1.75), ry+Inches(0.05), Inches(10.9), Inches(0.35),
             size=12, bold=True, color=WHITE)
    add_text(s, desc, Inches(1.75), ry+Inches(0.38), Inches(10.9), Inches(0.3),
             size=10, color=GREY)
    line = s.shapes.add_shape(1, Inches(0.6), ry+rh-Inches(0.02), Inches(12.1), Inches(0.01))
    line.fill.solid(); line.fill.fore_color.rgb = RGBColor(0x18,0x28,0x40); line.line.fill.background()
    ry += rh
add_text(s, "CONFIDENTIAL", Inches(11.5), Inches(7.1), Inches(1.6), Inches(0.3), size=9, color=RGBColor(0x44,0x55,0x66))

# ── Slide 10: AI Gateway Observability ──────────────────────────────────────
s = blank_slide(prs)
add_eyebrow(s, "Tier 11 — Coming Next")
add_heading(s, "AI Gateway Observability", size=32)
bullet_box(s, Inches(0.6), Inches(1.85), Inches(4.05), Inches(2.35), "Token & Usage Metrics",
           ["Total, input, output tokens by endpoint × model × day",
            "Cache token breakdown (read, creation, reasoning)",
            "Request counts and error rates per endpoint",
            "Per-requester aggregates — user / service",
            "Tag-based rollups — project, team, cost-center"])
bullet_box(s, Inches(0.6), Inches(4.3), Inches(4.05), Inches(1.8), "Endpoint Performance",
           ["p50 / p90 / p95 / p99 latency per endpoint",
            "Average time-to-first-byte (TTFB)",
            "4xx / 5xx error counts and error rate %",
            "Pre-aggregated in SQL — no raw per-request pull"])
diff_card(s, Inches(4.85), Inches(1.85), Inches(7.9), Inches(1.6),
          "AI Gateway — Databricks-Native, Same Platform",
          "Queries system.ai_gateway.usage via the same SQL warehouse as system.billing.usage. No new collection mechanism. 7 new portlets under Databricks - AI Gateway dashboard.",
          [("Datadog: No AI Gateway token observability", RED),
           ("New Relic: No AI Gateway token observability", ORANGE),
           ("Foglight: Included (Tier 11)", GREEN)])
add_rect(s, Inches(4.85), Inches(3.65), Inches(7.9), Inches(1.45), line=ORANGE)
add_text(s, "Prerequisites", Inches(5.0), Inches(3.72), Inches(7.6), Inches(0.3),
         size=11, bold=True, color=ORANGE)
add_text(s, "· Unity AI Gateway V2 Preview enabled (account Previews toggle)\n· Account-admin access required for monitoring identity (stricter than billing tables)\n· Existing SQL warehouse — reuses billingWarehouseId config\n· Collector degrades gracefully if preview is disabled",
         Inches(5.0), Inches(4.05), Inches(7.6), Inches(1.0), size=10, color=GREY)
add_rect(s, Inches(4.85), Inches(5.25), Inches(7.9), Inches(0.85), line=CYAN,
         fill=RGBColor(0x03,0x18,0x28))
add_text(s, "Fast-follow (deferred): Token → dollar cost attribution via join to system.billing.usage model-serving SKU records — same approach as DBU cost, same complexity class as Tier 4 cost-per-job.",
         Inches(5.0), Inches(5.3), Inches(7.6), Inches(0.75), size=10, color=GREY)
add_text(s, "CONFIDENTIAL", Inches(11.5), Inches(7.1), Inches(1.6), Inches(0.3), size=9, color=RGBColor(0x44,0x55,0x66))

# ── Slide 11: Getting Started ────────────────────────────────────────────────
s = blank_slide(prs)
add_eyebrow(s, "Next Steps")
add_heading(s, "Getting started", size=32)
bullet_box(s, Inches(0.6), Inches(1.85), Inches(5.9), Inches(2.1), "Requirements",
           ["Quest Foglight Management Server 8.2.0+",
            "FglAM co-located with FMS (existing deployment)",
            "Databricks Personal Access Token — read only",
            "Any running SQL Warehouse (for cost features)",
            "Databricks Premium + system tables for DBU/cost"])
bullet_box(s, Inches(0.6), Inches(4.1), Inches(5.9), Inches(2.2), "Installation — under 30 minutes",
           ["Download release zip from GitHub",
            "Copy agent files to FglAM agents directory",
            "Edit databricks.properties: workspace URL + token",
            "Install .car via Foglight UI → Administration → Cartridges",
            "Restart FglAM — data appears within 60 seconds"])
add_rect(s, Inches(6.75), Inches(1.85), Inches(6.0), Inches(4.5))
add_text(s, "Recommended Dashboard Setup", Inches(6.9), Inches(1.95), Inches(5.7), Inches(0.35),
         size=13, bold=True, color=CYAN)
dashboards = [
    ("Databricks - Compute",      "Clusters, Warehouses, Pools"),
    ("Databricks - Jobs",         "Jobs, Job Runs"),
    ("Databricks - Queries",      "Query History, Slow Queries, User Activity"),
    ("Databricks - Pipelines",    "DLT Pipelines, Updates, Data Quality"),
    ("Databricks - DBU & Cost",   "Treemap, Bubble, Cost by SKU, MoM Growth"),
    ("Databricks - Model Serving","Endpoints, Served Models"),
    ("Databricks - AI Gateway",   "AI Endpoints, Token Usage, Performance, Trend"),
]
dy = Inches(2.38)
for name, portlets in dashboards:
    add_text(s, name, Inches(6.9), dy, Inches(2.9), Inches(0.3), size=11, bold=True, color=WHITE)
    add_text(s, portlets, Inches(9.85), dy, Inches(2.8), Inches(0.3), size=11, color=GREY)
    dy += Inches(0.38)
add_text(s, "CONFIDENTIAL", Inches(11.5), Inches(7.1), Inches(1.6), Inches(0.3), size=9, color=RGBColor(0x44,0x55,0x66))

# ── Save ─────────────────────────────────────────────────────────────────────
out = r"C:\Users\mark_\OneDrive\Claude\foglight-databricks\ENABLEMENT_DECK.pptx"
prs.save(out)
print(f"Saved: {out}")
print(f"Slides: {len(prs.slides)}")
