package system._databricks.scripts;

// Instance Pools — one row per pool with utilization metrics

def ts = server.get("TopologyService")

def rawRows = []
def workspaces = ts.getObjectsOfType(ts.getType("DatabricksWorkspace"))
workspaces?.each { ws ->

    // Build map of poolName -> cluster count for clusters using that pool
    def poolClusterCount = [:]
    ws.get("clusters")?.each { c ->
        def pn = c.get("instancePoolName") ?: ""
        if (pn) {
            poolClusterCount[pn] = (poolClusterCount[pn] ?: 0) + 1
        }
    }

    ws.get("instancePools")?.each { p ->
        def poolName    = p.get("instancePoolName") ?: p.get("instancePoolId") ?: ""
        def idle        = (p.get("idleCount")         ?: 0L) as long
        def used        = (p.get("usedCount")         ?: 0L) as long
        def total       = idle + used
        def utilPct     = total > 0 ? (long)((used * 100.0) / total) : 0L
        def clusterCnt  = poolClusterCount[poolName] ?: 0

        rawRows << [
            poolName     : poolName,
            state        : p.get("stateStr")          ?: "",
            nodeType     : p.get("nodeTypeId")         ?: "",
            minIdle      : String.valueOf((p.get("minIdleInstances") ?: 0L) as long),
            maxCapacity  : String.valueOf((p.get("maxCapacity")      ?: 0L) as long),
            idle         : String.valueOf(idle),
            used         : String.valueOf(used),
            total        : String.valueOf(total),
            utilization  : utilPct + "%",
            clusterCount : String.valueOf(clusterCnt)
        ]
    }
}

rawRows.sort { it.poolName }

def rows = new java.util.ArrayList()
rawRows.each { r ->
    def row = functionHelper.createDataObject('databricks:DatabricksInstancePoolRow', 'none', null)
    row.store('poolName',     r.poolName,     specificTimeRange)
    row.store('state',        r.state,        specificTimeRange)
    row.store('nodeType',     r.nodeType,     specificTimeRange)
    row.store('minIdle',      r.minIdle,      specificTimeRange)
    row.store('maxCapacity',  r.maxCapacity,  specificTimeRange)
    row.store('idle',         r.idle,         specificTimeRange)
    row.store('used',         r.used,         specificTimeRange)
    row.store('total',        r.total,        specificTimeRange)
    row.store('utilization',  r.utilization,  specificTimeRange)
    row.store('clusterCount', r.clusterCount, specificTimeRange)
    rows.add(row)
}
return rows
