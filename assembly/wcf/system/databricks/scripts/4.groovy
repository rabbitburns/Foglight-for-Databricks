package system._databricks.scripts;

// Query History — flat table across all warehouses, sorted by start time desc

def ts = server.get("TopologyService")

def dur = { ms ->
    if (!ms || ms == 0L) return ""
    long s = ms / 1000; long m = s / 60; long h = m / 60
    h > 0 ? "${h}h ${m % 60}m" : m > 0 ? "${m}m ${s % 60}s" : "${s}s"
}

def rawRows = []

def workspaces = (ts.getType("DatabricksModelRoot").findAll() ?: []).collectMany { _r -> (_r.get("accounts") ?: []).collectMany { _a -> (_a.get("workspaces") ?: []) as List } }
workspaces?.each { ws ->
    ws.get("warehouses")?.each { wh ->
        def whName = wh.get("warehouseName") ?: ""
        wh.get("queries")?.each { q ->
            rawRows << [
                warehouseName : whName,
                userName      : q.get("userName")      ?: "",
                statementType : q.get("statementType") ?: "",
                status        : q.get("status")        ?: "",
                startedAt     : q.get("startedAtStr")  ?: "",
                duration      : q.get("durationStr")   ?: "",
                compilation   : q.get("compilationTimeStr") ?: "",
                execution     : q.get("executionTimeStr")   ?: "",
                fetch         : q.get("fetchTimeStr")       ?: "",
                bytesRead     : q.get("bytesReadStr")   ?: "",
                rowsProduced  : q.get("rowsProducedStr") ?: "",
                fromCache     : q.get("fromResultCache") ?: "",
                errorMessage  : q.get("errorMessage")   ?: "",
                queryText     : q.get("queryText")      ?: "",
                durationMs    : (q.get("duration") ?: 0L) as long
            ]
        }
    }
}

rawRows.sort { a, b -> (b.startedAt ?: "").compareTo(a.startedAt ?: "") }

def rows = new java.util.ArrayList()
rawRows.each { r ->
    def row = functionHelper.createDataObject('databricks:DatabricksQueryRow', 'none', null)
    row.store('warehouseName', r.warehouseName, specificTimeRange)
    row.store('userName',      r.userName,      specificTimeRange)
    row.store('statementType', r.statementType, specificTimeRange)
    row.store('status',        r.status,        specificTimeRange)
    row.store('startedAt',     r.startedAt,     specificTimeRange)
    row.store('duration',      r.duration,      specificTimeRange)
    row.store('compilation',   r.compilation,   specificTimeRange)
    row.store('execution',     r.execution,     specificTimeRange)
    row.store('fetch',         r.fetch,         specificTimeRange)
    row.store('bytesRead',     r.bytesRead,     specificTimeRange)
    row.store('rowsProduced',  r.rowsProduced,  specificTimeRange)
    row.store('fromCache',     r.fromCache == "true" ? "Yes" : "", specificTimeRange)
    row.store('errorMessage',  r.errorMessage,  specificTimeRange)
    row.store('queryText',     r.queryText,     specificTimeRange)
    row.store('durationMs',    r.durationMs,    specificTimeRange)
    rows.add(row)
}

return rows
