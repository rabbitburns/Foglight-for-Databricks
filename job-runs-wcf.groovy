// Job Runs — WCF HTML table
// Use as a WCF Script Function, or paste into Script Console to verify output

def ts = server.get("TopologyService")

def dur = { ms ->
    if (!ms || ms == "" || ms == "0") return ""
    try {
        long s = (ms as long) / 1000; long m = s / 60; long h = m / 60
        h > 0 ? "${h}h ${m % 60}m" : m > 0 ? "${m}m ${s % 60}s" : "${s}s"
    } catch (e) { return "" }
}

def esc = { v -> v ? v.toString().replace("&","&amp;").replace("<","&lt;").replace(">","&gt;") : "" }

// ── collect rows ──────────────────────────────────────────────────────────────
def rows = []

def workspaces = ts.getObjectsOfType(ts.getType("DatabricksWorkspace"))
workspaces?.each { ws ->
    ws.get("jobs")?.each { job ->
        def jobName  = job.get("jobName")          ?: ""
        def creator  = job.get("creatorUserName")  ?: ""
        def runs     = job.get("runs")

        if (!runs || runs.size() == 0) {
            rows << [jobName: jobName, creator: creator, runId: "", state: "",
                     result: "", started: "", total: "", queue: "", setup: "",
                     execution: "", cleanup: "", tasks: "", retry: "", message: "no runs"]
        } else {
            runs.each { run ->
                rows << [
                    jobName  : jobName,
                    creator  : creator,
                    runId    : run.get("runId")             ?: "",
                    state    : run.get("lifeCycleStateStr") ?: "",
                    result   : run.get("resultStateStr")    ?: "",
                    started  : run.get("startTimeStr")      ?: "",
                    total    : dur(run.get("durationMsStr")),
                    queue    : dur(run.get("queueDurationStr")),
                    setup    : dur(run.get("setupDurationStr")),
                    execution: dur(run.get("executionDurationStr")),
                    cleanup  : dur(run.get("cleanupDurationStr")),
                    tasks    : run.get("taskCountStr")      ?: "",
                    retry    : run.get("isRetry")           ?: "",
                    message  : run.get("stateMessage")      ?: ""
                ]
            }
        }
    }
}

// sort by started desc
rows.sort { a, b -> (b.started ?: "").compareTo(a.started ?: "") }

// ── render HTML ───────────────────────────────────────────────────────────────
def html = new StringBuilder()

html << """
<style>
  .db-table { border-collapse: collapse; width: 100%; font-size: 12px; font-family: Arial, sans-serif; }
  .db-table th { background: #2c5f8a; color: white; padding: 6px 8px; text-align: left; white-space: nowrap; }
  .db-table td { padding: 4px 8px; border-bottom: 1px solid #ddd; white-space: nowrap; }
  .db-table tr:hover td { background: #f0f4f8; }
  .state-success  { color: #2e7d32; font-weight: bold; }
  .state-failed   { color: #c62828; font-weight: bold; }
  .state-running  { color: #1565c0; font-weight: bold; }
  .state-norun    { color: #999; font-style: italic; }
</style>
<table class="db-table">
<tr>
  <th>Job Name</th>
  <th>Creator</th>
  <th>Run ID</th>
  <th>State</th>
  <th>Result</th>
  <th>Started</th>
  <th>Total</th>
  <th>Queue</th>
  <th>Setup</th>
  <th>Execution</th>
  <th>Cleanup</th>
  <th>Tasks</th>
  <th>Retry</th>
  <th>Message</th>
</tr>
"""

rows.each { r ->
    def stateClass = ""
    if (r.result == "SUCCESS")          stateClass = "state-success"
    else if (r.result == "FAILED" || r.result == "TIMEDOUT") stateClass = "state-failed"
    else if (r.state == "RUNNING")      stateClass = "state-running"
    else if (r.runId == "")             stateClass = "state-norun"

    html << "<tr>"
    html << "<td>${esc(r.jobName)}</td>"
    html << "<td>${esc(r.creator)}</td>"
    html << "<td>${esc(r.runId)}</td>"
    html << "<td class='${stateClass}'>${esc(r.state)}</td>"
    html << "<td class='${stateClass}'>${esc(r.result)}</td>"
    html << "<td>${esc(r.started)}</td>"
    html << "<td>${esc(r.total)}</td>"
    html << "<td>${esc(r.queue)}</td>"
    html << "<td>${esc(r.setup)}</td>"
    html << "<td>${esc(r.execution)}</td>"
    html << "<td>${esc(r.cleanup)}</td>"
    html << "<td>${esc(r.tasks)}</td>"
    html << "<td>${r.retry == 'true' ? 'Yes' : ''}</td>"
    html << "<td>${esc(r.message)}</td>"
    html << "</tr>\n"
}

html << "</table>\n<p style='font-size:11px;color:#666'>${rows.size()} runs</p>"

return html.toString()
