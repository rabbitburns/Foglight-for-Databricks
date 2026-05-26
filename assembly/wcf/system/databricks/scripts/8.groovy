package system._databricks.scripts;

// User Activity Summary — query count, avg duration, total bytes, cache hits, errors per user

def ts = server.get("TopologyService")

def dur = { ms ->
    if (!ms || ms == 0L) return ""
    long s = ms / 1000; long m = s / 60; long h = m / 60
    h > 0 ? "${h}h ${m % 60}m" : m > 0 ? "${m}m ${s % 60}s" : "${s}s"
}

def userMap = [:]

def workspaces = ts.getObjectsOfType(ts.getType("DatabricksWorkspace"))
workspaces?.each { ws ->
    ws.get("warehouses")?.each { wh ->
        wh.get("queries")?.each { q ->
            def user = q.get("userName") ?: "(unknown)"
            if (!userMap.containsKey(user)) {
                userMap[user] = [queryCount: 0, totalMs: 0L, totalBytes: 0L, cacheHits: 0, errors: 0]
            }
            def u = userMap[user]
            u.queryCount++
            u.totalMs    += (q.get("duration") ?: 0L) as long
            u.totalBytes += (q.get("bytesRead") ?: 0L) as long
            if (q.get("fromResultCache") == "true") u.cacheHits++
            if (q.get("errorMessage") && q.get("errorMessage") != "") u.errors++
        }
    }
}

def rawRows = userMap.collect { user, u -> [user: user] + u }
rawRows.sort { a, b -> b.queryCount <=> a.queryCount }

def rows = new java.util.ArrayList()
rawRows.each { r ->
    def row = functionHelper.createDataObject('databricks:DatabricksUserActivityRow', 'none', null)
    row.store('userName',    r.user,                                     specificTimeRange)
    row.store('queryCount',  String.valueOf(r.queryCount),               specificTimeRange)
    row.store('avgDuration', r.queryCount > 0 ? dur(r.totalMs / r.queryCount) : "", specificTimeRange)
    row.store('totalBytes',  r.totalBytes > 0 ? String.valueOf(r.totalBytes) : "", specificTimeRange)
    row.store('cacheHits',   String.valueOf(r.cacheHits),                specificTimeRange)
    row.store('errorCount',  r.errors > 0 ? String.valueOf(r.errors) : "", specificTimeRange)
    rows.add(row)
}

return rows
