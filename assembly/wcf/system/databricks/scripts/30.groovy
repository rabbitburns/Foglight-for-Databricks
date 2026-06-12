package system._databricks.scripts;

// Top Jobs by DBU — last 30 days, sorted by DBU desc, enriched with job names

def ts = server.get("TopologyService")

def jobNames = [:]
def jobDurations = [:]
ts.getObjectsOfType(ts.getType("DatabricksJob"))?.each { j ->
    def id = j.get("jobId")
    jobNames[id] = j.get("jobName") ?: id
    def mn = j.get("minDurationStr") ?: ""
    def av = j.get("avgDurationStr") ?: ""
    def mx = j.get("maxDurationStr") ?: ""
    jobDurations[id] = (mn && av && mx) ? "${mn} – ${av} – ${mx}" : ""
}

def rawRows = []
def workspaces = ts.getObjectsOfType(ts.getType("DatabricksWorkspace"))
workspaces?.each { ws ->
    ws.get("jobDbus")?.each { jd ->
        def jobId = jd.get("jobId") ?: ""
        double dbu = 0.0
        try { dbu = Double.parseDouble(jd.get("dbuConsumedStr") ?: "0") } catch (Exception ignore) {}
        rawRows << [
            jobId    : jobId,
            jobName  : jobNames[jobId] ?: jobId,
            dbu      : dbu,
            dbuStr   : jd.get("dbuConsumedStr") ?: "",
            cost     : jd.get("dollarCostStr")  ?: "",
            durRange : jobDurations[jobId]       ?: ""
        ]
    }
}
rawRows.sort { a, b -> Double.compare(b.dbu, a.dbu) }

def rows = new java.util.ArrayList()
rawRows.each { r ->
    def row = functionHelper.createDataObject('databricks:DatabricksJobDbuRow', 'none', null)
    row.store('jobName',       r.jobName,  specificTimeRange)
    row.store('jobId',         r.jobId,    specificTimeRange)
    row.store('dbu',           r.dbuStr,   specificTimeRange)
    row.store('dollarCost',    r.cost,     specificTimeRange)
    row.store('durationRange', r.durRange, specificTimeRange)
    rows.add(row)
}
return rows
