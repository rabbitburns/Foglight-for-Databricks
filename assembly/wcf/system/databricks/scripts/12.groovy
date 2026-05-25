package system._databricks.scripts;

// Clusters — one row per cluster, RUNNING first then by name

def ts = server.get("TopologyService")

def rawRows = []
def workspaces = (ts.getObjectsOfType(ts.getType("DatabricksModelRoot")) ?: []).collectMany { _r -> (_r.get("accounts") ?: []).collectMany { _a -> (_a.get("workspaces") ?: []) as List } }
workspaces?.each { ws ->
    ws.get("clusters")?.each { c ->
        def autoscale = c.get("autoscaleEnabled")
        rawRows << [
            clusterName  : c.get("clusterName")        ?: "",
            state        : c.get("state")              ?: "",
            source       : c.get("clusterSource")      ?: "",
            driverNode   : c.get("driverNodeTypeId")   ?: "",
            workers      : String.valueOf((c.get("numWorkers") ?: 0L) as long),
            minWorkers   : autoscale ? String.valueOf((c.get("autoscaleMinWorkers") ?: 0L) as long) : "",
            maxWorkers   : autoscale ? String.valueOf((c.get("autoscaleMaxWorkers") ?: 0L) as long) : "",
            cores        : String.valueOf((c.get("clusterCores") ?: 0L) as long),
            sparkVersion : c.get("sparkVersion")       ?: "",
            creator      : c.get("creatorUserName")    ?: "",
            started      : c.get("startTimeStr")       ?: "",
            lastActivity : c.get("lastActivityTimeStr") ?: "",
            terminated   : c.get("terminatedTimeStr")  ?: "",
            pinnedBy     : c.get("pinnedByUserName")   ?: "",
            tags         : c.get("customTagsStr")      ?: ""
        ]
    }
}

rawRows.sort { a, b ->
    if (a.state == "RUNNING" && b.state != "RUNNING") return -1
    if (b.state == "RUNNING" && a.state != "RUNNING") return 1
    a.clusterName.compareTo(b.clusterName)
}

def rows = new java.util.ArrayList()
rawRows.each { r ->
    def row = functionHelper.createDataObject('databricks:DatabricksClusterRow', 'none', null)
    row.store('clusterName',  r.clusterName,  specificTimeRange)
    row.store('state',        r.state,        specificTimeRange)
    row.store('source',       r.source,       specificTimeRange)
    row.store('driverNode',   r.driverNode,   specificTimeRange)
    row.store('workers',      r.workers,      specificTimeRange)
    row.store('minWorkers',   r.minWorkers,   specificTimeRange)
    row.store('maxWorkers',   r.maxWorkers,   specificTimeRange)
    row.store('cores',        r.cores,        specificTimeRange)
    row.store('sparkVersion', r.sparkVersion, specificTimeRange)
    row.store('creator',      r.creator,      specificTimeRange)
    row.store('started',      r.started,      specificTimeRange)
    row.store('lastActivity', r.lastActivity, specificTimeRange)
    row.store('terminated',   r.terminated,   specificTimeRange)
    row.store('pinnedBy',     r.pinnedBy,     specificTimeRange)
    row.store('tags',         r.tags,         specificTimeRange)
    rows.add(row)
}
return rows
