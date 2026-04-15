package system._databricks.scripts;

// Instance Pools — one row per pool, sorted by name

def ts = server.get("TopologyService")

def rawRows = []
def workspaces = ts.getObjectsOfType(ts.getType("DatabricksWorkspace"))
workspaces?.each { ws ->
    ws.get("instancePools")?.each { p ->
        rawRows << [
            poolName    : p.get("instancePoolName")  ?: "",
            state       : p.get("state")             ?: "",
            nodeType    : p.get("nodeTypeId")        ?: "",
            minIdle     : String.valueOf((p.get("minIdleInstances") ?: 0L) as long),
            maxCapacity : String.valueOf((p.get("maxCapacity") ?: 0L) as long),
            idle        : String.valueOf((p.get("idleCount") ?: 0L) as long),
            used        : String.valueOf((p.get("usedCount") ?: 0L) as long)
        ]
    }
}

rawRows.sort { a, b -> a.poolName.compareTo(b.poolName) }

def rows = new java.util.ArrayList()
rawRows.each { r ->
    def row = functionHelper.createDataObject('databricks:DatabricksInstancePoolRow', 'none', null)
    row.store('poolName',    r.poolName,    specificTimeRange)
    row.store('state',       r.state,       specificTimeRange)
    row.store('nodeType',    r.nodeType,    specificTimeRange)
    row.store('minIdle',     r.minIdle,     specificTimeRange)
    row.store('maxCapacity', r.maxCapacity, specificTimeRange)
    row.store('idle',        r.idle,        specificTimeRange)
    row.store('used',        r.used,        specificTimeRange)
    rows.add(row)
}
return rows
