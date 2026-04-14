// Job Runs Table — flat view across all jobs and runs
// Run in Foglight Script Console (Administration > Tooling > Script Console)
// Designed to be adapted into a WCF portlet

def ts  = server.get("TopologyService")
def sb  = new StringBuilder()

def dur = { ms ->
    if (!ms || ms == "" || ms == "0") return ""
    try {
        long s = (ms as long) / 1000; long m = s / 60; long h = m / 60
        h > 0 ? "${h}h ${m % 60}m" : m > 0 ? "${m}m ${s % 60}s" : "${s}s"
    } catch (e) { return ms }
}

// ── header ────────────────────────────────────────────────────────────────────
def cols = ["Job Name", "Creator", "Run ID", "State", "Result",
            "Started", "Total", "Queue", "Setup", "Execution", "Cleanup",
            "Tasks", "Retry?", "Message"]
def widths = [40, 28, 14, 12, 10, 20, 10, 8, 8, 10, 9, 6, 6, 30]

def hdr = cols.withIndex().collect { c, i -> c.padRight(widths[i]) }.join(" | ")
def sep = widths.collect { "-" * it }.join("-+-")

sb.append(hdr).append("\n")
sb.append(sep).append("\n")

// ── data ──────────────────────────────────────────────────────────────────────
def rows = []

def workspaces = ts.getObjectsOfType(ts.getType("DatabricksWorkspace"))
workspaces?.each { ws ->
    ws.get("jobs")?.each { job ->
        def jobName    = job.get("jobName")    ?: ""
        def creator    = job.get("creatorUserName") ?: ""
        def runs       = job.get("runs")

        if (!runs || runs.size() == 0) {
            rows << [jobName, creator, "(no runs)", "", "", "", "", "", "", "", "", "", "", ""]
        } else {
            runs.each { run ->
                rows << [
                    jobName,
                    creator,
                    run.get("runId")             ?: "",
                    run.get("lifeCycleStateStr") ?: "",
                    run.get("resultStateStr")    ?: "",
                    run.get("startTimeStr")      ?: "",
                    dur(run.get("durationMsStr")),
                    dur(run.get("queueDurationStr")),
                    dur(run.get("setupDurationStr")),
                    dur(run.get("executionDurationStr")),
                    dur(run.get("cleanupDurationStr")),
                    run.get("taskCountStr")      ?: "",
                    run.get("isRetry")           ?: "",
                    run.get("stateMessage")      ?: ""
                ]
            }
        }
    }
}

// sort by started desc (col index 5)
rows.sort { a, b -> (b[5] ?: "").compareTo(a[5] ?: "") }

rows.each { row ->
    sb.append(row.withIndex().collect { v, i ->
        v.toString().take(widths[i]).padRight(widths[i])
    }.join(" | ")).append("\n")
}

sb.append("\n${rows.size()} rows")
return sb.toString()
