package system._databricks.scripts;

// SQL Warehouses — one row per warehouse, RUNNING first then by name

def ts = server.get("TopologyService")

def rawRows = []
def workspaces = ts.getObjectsOfType(ts.getType("DatabricksWorkspace"))
workspaces?.each { ws ->
    ws.get("warehouses")?.each { wh ->
        def autoStopVal = (wh.get("autoStopMins") ?: 0L) as long
        rawRows << [
            warehouseName : wh.get("warehouseName") ?: "",
            type          : wh.get("warehouseType") ?: "",
            state         : wh.get("state")         ?: "",
            size          : wh.get("size")          ?: "",
            numClusters   : String.valueOf((wh.get("numClusters") ?: 0L) as long),
            minClusters   : String.valueOf((wh.get("minClusters") ?: 0L) as long),
            maxClusters   : String.valueOf((wh.get("maxClusters") ?: 0L) as long),
            photon        : wh.get("enablePhoton")  ?: "",
            autoStop      : autoStopVal > 0 ? "${autoStopVal} min" : "",
            autoResume    : wh.get("autoResume")    ?: "",
            creator       : wh.get("creatorName")   ?: "",
            queryCount    : wh.get("queryCountStr") ?: "0"
        ]
    }
}

rawRows.sort { a, b ->
    if (a.state == "RUNNING" && b.state != "RUNNING") return -1
    if (b.state == "RUNNING" && a.state != "RUNNING") return 1
    a.warehouseName.compareTo(b.warehouseName)
}

def rows = new java.util.ArrayList()
rawRows.each { r ->
    def row = functionHelper.createDataObject('databricks:DatabricksWarehouseRow', 'none', null)
    row.store('warehouseName', r.warehouseName, specificTimeRange)
    row.store('type',          r.type,          specificTimeRange)
    row.store('state',         r.state,         specificTimeRange)
    row.store('size',          r.size,          specificTimeRange)
    row.store('numClusters',   r.numClusters,   specificTimeRange)
    row.store('minClusters',   r.minClusters,   specificTimeRange)
    row.store('maxClusters',   r.maxClusters,   specificTimeRange)
    row.store('photon',        r.photon,        specificTimeRange)
    row.store('autoStop',      r.autoStop,      specificTimeRange)
    row.store('autoResume',    r.autoResume,    specificTimeRange)
    row.store('creator',       r.creator,       specificTimeRange)
    row.store('queryCount',    r.queryCount,    specificTimeRange)
    rows.add(row)
}
return rows
