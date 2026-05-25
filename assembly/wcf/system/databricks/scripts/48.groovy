package system._databricks.scripts;

// Model Serving Endpoints — one row per endpoint

def ts = server.get("TopologyService")
def rows = new java.util.ArrayList()

(ts.getType("DatabricksModelRoot").findAll() ?: []).collectMany { _r -> (_r.get("accounts") ?: []).collectMany { _a -> (_a.get("workspaces") ?: []) as List } }?.each { ws ->
    ws.get("servingEndpoints")?.each { ep ->
        def servedModels = ep.get("servedModels")
        def modelCount   = servedModels ? String.valueOf(servedModels.size()) : "0"

        def row = functionHelper.createDataObject('databricks:DatabricksServingEndpointRow', 'none', null)
        row.store('endpointName',      ep.get("endpointName")      ?: "", specificTimeRange)
        row.store('readyState',        ep.get("readyState")        ?: "", specificTimeRange)
        row.store('configUpdateState', ep.get("configUpdateState") ?: "", specificTimeRange)
        row.store('creator',           ep.get("creator")           ?: "", specificTimeRange)
        row.store('creationTime',      ep.get("creationTime")      ?: "", specificTimeRange)
        row.store('lastUpdatedTime',   ep.get("lastUpdatedTime")   ?: "", specificTimeRange)
        row.store('routeOptimized',    ep.get("routeOptimized")    ?: "false", specificTimeRange)
        row.store('servedModelCount',  modelCount,                          specificTimeRange)
        rows.add(row)
    }
}

rows.sort { a, b -> (a.get("endpointName") ?: "").compareTo(b.get("endpointName") ?: "") }
return rows
