package system._databricks.scripts;

def ts = server.get("TopologyService")

def dur = { ms ->
    if (!ms || ms == "" || ms == "0") return ""
    try {
        long s = (ms as long) / 1000; long m = s / 60; long h = m / 60
        h > 0 ? "${h}h ${m % 60}m" : m > 0 ? "${m}m ${s % 60}s" : "${s}s"
    } catch (e) { return "" }
}

def rawRows = []

def workspaces = ts.getObjectsOfType(ts.getType("DatabricksWorkspace"))
workspaces?.each { ws ->
    ws.get("jobs")?.each { job ->
        def jobName = job.get("jobName")         ?: ""
        def creator = job.get("creatorUserName") ?: ""
        def runs    = job.get("runs")

        if (!runs || runs.size() == 0) {
            rawRows << [jobName: jobName, creator: creator, runId: "", state: "",
                        result: "", started: "", total: "", queue: "", setup: "",
                        execution: "", cleanup: "", tasks: "", retry: "", message: "no runs"]
        } else {
            runs.each { run ->
                rawRows << [
                    jobName  : jobName,
                    creator  : creator,
                    runId    : run.get("runId")              ?: "",
                    state    : run.get("lifeCycleStateStr")  ?: "",
                    result   : run.get("resultStateStr")     ?: "",
                    started  : run.get("startTimeStr")       ?: "",
                    total    : dur(run.get("durationMsStr")),
                    queue    : dur(run.get("queueDurationStr")),
                    setup    : dur(run.get("setupDurationStr")),
                    execution: dur(run.get("executionDurationStr")),
                    cleanup  : dur(run.get("cleanupDurationStr")),
                    tasks    : run.get("taskCountStr")       ?: "",
                    retry    : run.get("isRetry") == "true" ? "Yes" : "",
                    message  : run.get("stateMessage")       ?: ""
                ]
            }
        }
    }
}

rawRows.sort { a, b -> (b.started ?: "").compareTo(a.started ?: "") }

def rows = new java.util.ArrayList()
rawRows.each { r ->
    def row = functionHelper.createDataObject('databricks:DatabricksRunRow', 'none', null)
    row.store('jobName',   r.jobName,   specificTimeRange)
    row.store('creator',   r.creator,   specificTimeRange)
    row.store('runId',     r.runId,     specificTimeRange)
    row.store('state',     r.state,     specificTimeRange)
    row.store('result',    r.result,    specificTimeRange)
    row.store('started',   r.started,   specificTimeRange)
    row.store('total',     r.total,     specificTimeRange)
    row.store('queue',     r.queue,     specificTimeRange)
    row.store('setup',     r.setup,     specificTimeRange)
    row.store('execution', r.execution, specificTimeRange)
    row.store('cleanup',   r.cleanup,   specificTimeRange)
    row.store('tasks',     r.tasks,     specificTimeRange)
    row.store('retry',     r.retry,     specificTimeRange)
    row.store('message',   r.message,   specificTimeRange)
    rows.add(row)
}

return rows
