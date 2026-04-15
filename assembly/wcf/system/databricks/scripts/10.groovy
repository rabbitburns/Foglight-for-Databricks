package system._databricks.scripts;

// Databricks Overview — one summary row per resource category

def ts = server.get("TopologyService")

def clusters      = 0; def clustersRunning = 0; def clustersStopped = 0
def warehouses    = 0; def whRunning = 0;       def whStopped = 0
def jobs          = 0; def jobsFailed = 0
def pipelines     = 0; def pipelinesRunning = 0; def pipelinesStopped = 0
def pools         = 0
def totalQueries  = 0; def queryErrors = 0; def totalQueryMs = 0L

def workspaces = ts.getObjectsOfType(ts.getType("DatabricksWorkspace"))
workspaces?.each { ws ->

    ws.get("clusters")?.each { c ->
        clusters++
        def s = c.get("state") ?: ""
        if (s == "RUNNING") clustersRunning++ else clustersStopped++
    }

    ws.get("warehouses")?.each { wh ->
        warehouses++
        def s = wh.get("state") ?: ""
        if (s == "RUNNING") whRunning++ else whStopped++
        wh.get("queries")?.each { q ->
            totalQueries++
            totalQueryMs += (q.get("duration") ?: 0L) as long
            if (q.get("errorMessage") && q.get("errorMessage") != "") queryErrors++
        }
    }

    ws.get("jobs")?.each { j ->
        jobs++
        def lastResult = j.get("lastRunResult") ?: ""
        if (lastResult == "FAILED" || lastResult == "TIMEDOUT" || lastResult == "ERROR") jobsFailed++
    }

    ws.get("pipelines")?.each { p ->
        pipelines++
        def s = p.get("state") ?: ""
        if (s == "RUNNING") pipelinesRunning++ else pipelinesStopped++
    }

    ws.get("instancePools")?.each { pools++ }
}

def avgQueryDur = ""
if (totalQueries > 0) {
    long avgMs = totalQueryMs / totalQueries
    long s = avgMs / 1000; long m = s / 60
    avgQueryDur = m > 0 ? "${m}m ${s % 60}s avg" : "${s}s avg"
}

def mkRow = { cat, total, active, inactive, details ->
    def row = functionHelper.createDataObject('databricks:DatabricksSummaryRow', 'none', null)
    row.store('category', cat,                    specificTimeRange)
    row.store('total',    String.valueOf(total),   specificTimeRange)
    row.store('active',   active,                  specificTimeRange)
    row.store('inactive', inactive,                specificTimeRange)
    row.store('details',  details,                 specificTimeRange)
    row
}

def rows = new java.util.ArrayList()
rows.add(mkRow("Clusters",       clusters,   "${clustersRunning} RUNNING",  "${clustersStopped} TERMINATED", ""))
rows.add(mkRow("SQL Warehouses", warehouses, "${whRunning} RUNNING",        "${whStopped} STOPPED",          ""))
rows.add(mkRow("Jobs",           jobs,       "",                            "",                              jobsFailed > 0 ? "${jobsFailed} with recent failures" : "All jobs healthy"))
rows.add(mkRow("DLT Pipelines",  pipelines,  "${pipelinesRunning} RUNNING", "${pipelinesStopped} IDLE",      ""))
rows.add(mkRow("Instance Pools", pools,      "",                            "",                              ""))
rows.add(mkRow("Queries",        totalQueries, "",                          "",                              queryErrors > 0 ? "${avgQueryDur}, ${queryErrors} errors" : avgQueryDur))
return rows
