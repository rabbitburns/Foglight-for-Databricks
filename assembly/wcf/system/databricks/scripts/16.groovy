package system._databricks.scripts;

// Jobs — one row per job, failures first then by most recent run

def ts = server.get("TopologyService")

def rawRows = []
def workspaces = (ts.getType("DatabricksModelRoot").findAll() ?: []).collectMany { _r -> (_r.get("accounts") ?: []).collectMany { _a -> (_a.get("workspaces") ?: []) as List } }
workspaces?.each { ws ->
    ws.get("jobs")?.each { j ->
        def lastResult = j.get("lastRunResult") ?: ""
        rawRows << [
            jobName      : j.get("jobName")            ?: "",
            creator      : j.get("creatorUserName")    ?: "",
            triggerType  : j.get("triggerType")        ?: "",
            schedule     : j.get("scheduleStatus")     ?: "",
            lastState    : j.get("lastRunState")       ?: "",
            lastResult   : lastResult,
            lastStart    : j.get("lastRunStartStr")    ?: "",
            lastDuration : j.get("lastRunDurationStr") ?: "",
            successRate  : j.get("successRateStr")     ?: "",
            avgDuration  : j.get("avgDurationStr")     ?: "",
            minDuration  : j.get("minDurationStr")     ?: "",
            maxDuration  : j.get("maxDurationStr")     ?: "",
            successes    : j.get("successCountStr")    ?: "",
            failures     : j.get("failureCountStr")    ?: "",
            tags         : j.get("tagsStr")            ?: "",
            failed       : (lastResult == "FAILED" || lastResult == "TIMEDOUT" || lastResult == "ERROR")
        ]
    }
}

rawRows.sort { a, b ->
    if (a.failed && !b.failed) return -1
    if (b.failed && !a.failed) return 1
    (b.lastStart ?: "").compareTo(a.lastStart ?: "")
}

def rows = new java.util.ArrayList()
rawRows.each { r ->
    def row = functionHelper.createDataObject('databricks:DatabricksJobRow', 'none', null)
    row.store('jobName',      r.jobName,      specificTimeRange)
    row.store('creator',      r.creator,      specificTimeRange)
    row.store('triggerType',  r.triggerType,  specificTimeRange)
    row.store('schedule',     r.schedule,     specificTimeRange)
    row.store('lastState',    r.lastState,    specificTimeRange)
    row.store('lastResult',   r.lastResult,   specificTimeRange)
    row.store('lastStart',    r.lastStart,    specificTimeRange)
    row.store('lastDuration', r.lastDuration, specificTimeRange)
    row.store('successRate',  r.successRate,  specificTimeRange)
    row.store('avgDuration',  r.avgDuration,  specificTimeRange)
    row.store('minDuration',  r.minDuration,  specificTimeRange)
    row.store('maxDuration',  r.maxDuration,  specificTimeRange)
    row.store('successes',    r.successes,    specificTimeRange)
    row.store('failures',     r.failures,     specificTimeRange)
    row.store('tags',         r.tags,         specificTimeRange)
    rows.add(row)
}
return rows
