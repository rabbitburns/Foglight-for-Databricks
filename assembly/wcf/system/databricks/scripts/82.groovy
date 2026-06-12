package system._databricks.scripts;

// Storage Costs — STORAGE_SPACE billing, last 30 days, sorted by cost desc

def ts = server.get("TopologyService")
def rows = new java.util.ArrayList()

def workspaces = ts.getObjectsOfType(ts.getType("DatabricksWorkspace"))
workspaces?.each { ws ->
    ws.get("storageCosts")?.each { sc ->
        def row = functionHelper.createDataObject('databricks:DatabricksStorageCostRow', 'none', null)
        row.store('storageProduct', sc.get("storageProduct") ?: "", specificTimeRange)
        row.store('skuName',        sc.get("skuName")        ?: "", specificTimeRange)
        row.store('usageUnit',      sc.get("usageUnit")      ?: "", specificTimeRange)
        row.store('usageStr',       sc.get("usageStr")       ?: "", specificTimeRange)
        row.store('dollarCost',     sc.get("dollarCostStr")  ?: "", specificTimeRange)
        rows.add(row)
    }
}
return rows
