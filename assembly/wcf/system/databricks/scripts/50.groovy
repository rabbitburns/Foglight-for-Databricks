package system._databricks.scripts;

// Model Serving — Served Models detail, one row per served model across all endpoints

def ts = server.get("TopologyService")
def rows = new java.util.ArrayList()

(ts.getObjectsOfType(ts.getType("DatabricksModelRoot")) ?: []).collectMany { _r -> (_r.get("accounts") ?: []).collectMany { _a -> (_a.get("workspaces") ?: []) as List } }?.each { ws ->
    ws.get("servingEndpoints")?.each { ep ->
        ep.get("servedModels")?.each { sm ->
            def row = functionHelper.createDataObject('databricks:DatabricksServedModelRow', 'none', null)
            row.store('endpointName',      sm.get("endpointName")      ?: "", specificTimeRange)
            row.store('servedModelName',   sm.get("servedModelName")   ?: "", specificTimeRange)
            row.store('modelName',         sm.get("modelName")         ?: "", specificTimeRange)
            row.store('modelVersion',      sm.get("modelVersion")      ?: "", specificTimeRange)
            row.store('workloadSize',      sm.get("workloadSize")      ?: "", specificTimeRange)
            row.store('scaleToZero',       sm.get("scaleToZero")       ?: "", specificTimeRange)
            row.store('trafficPercentage', sm.get("trafficPercentage") ?: "", specificTimeRange)
            row.store('deploymentState',   sm.get("deploymentState")   ?: "", specificTimeRange)
            rows.add(row)
        }
    }
}

rows.sort { a, b ->
    def epCmp = (a.get("endpointName") ?: "").compareTo(b.get("endpointName") ?: "")
    if (epCmp != 0) return epCmp
    return (a.get("servedModelName") ?: "").compareTo(b.get("servedModelName") ?: "")
}
return rows
