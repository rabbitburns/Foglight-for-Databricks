package system._databricks.scripts;

// AI User Activity — per-requester token rollup, sorted by total tokens desc

def ts = server.get("TopologyService")
def rows = new java.util.ArrayList()

def rawRows = []
ts.getObjectsOfType(ts.getType("DatabricksAiUserActivity"))?.each { u ->
    long tot = 0L
    try { tot = Long.parseLong(u.get("totalTokensStr") ?: "0") } catch (Exception ignore) {}
    rawRows << [
        requester     : u.get("requester")       ?: "",
        requesterType : u.get("requesterType")   ?: "",
        requestCount  : u.get("requestCountStr") ?: "",
        totalTokens   : u.get("totalTokensStr")  ?: "",
        errorCount    : u.get("errorCountStr")   ?: "",
        totRaw        : tot
    ]
}

rawRows.sort { a, b -> Long.compare(b.totRaw, a.totRaw) }

rawRows.each { r ->
    def row = functionHelper.createDataObject('databricks:DatabricksAiUserActivityRow', 'none', null)
    row.store('requester',     r.requester,     specificTimeRange)
    row.store('requesterType', r.requesterType, specificTimeRange)
    row.store('requestCount',  r.requestCount,  specificTimeRange)
    row.store('totalTokens',   r.totalTokens,   specificTimeRange)
    row.store('errorCount',    r.errorCount,    specificTimeRange)
    rows.add(row)
}

return rows
