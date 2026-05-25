package system._databricks.scripts;

// Pipeline Update History — last 5 updates per pipeline, most recent first

def ts = server.get("TopologyService")
def rows = new java.util.ArrayList()

(ts.getType("DatabricksModelRoot").findAll() ?: []).collectMany { _r -> (_r.get("accounts") ?: []).collectMany { _a -> (_a.get("workspaces") ?: []) as List } }?.each { ws ->
    ws.get("pipelines")?.each { pl ->
        def pipelineName = pl.get("pipelineName") ?: pl.get("pipelineId") ?: "(unknown)"
        pl.get("updates")?.each { up ->
            def row = functionHelper.createDataObject('databricks:DatabricksPipelineUpdateRow', 'none', null)
            row.store('pipelineName', pipelineName,              specificTimeRange)
            row.store('updateId',     up.get("updateId") ?: "", specificTimeRange)
            row.store('state',        up.get("state")    ?: "", specificTimeRange)
            row.store('startTime',    up.get("startTime") ?: "", specificTimeRange)
            row.store('durationStr',  up.get("durationStr") ?: "", specificTimeRange)
            row.store('cause',        up.get("cause")    ?: "", specificTimeRange)
            rows.add(row)
        }
    }
}

rows.sort { a, b ->
    def timeCmp = (b.get("startTime") ?: "").compareTo(a.get("startTime") ?: "")
    if (timeCmp != 0) return timeCmp
    return (a.get("pipelineName") ?: "").compareTo(b.get("pipelineName") ?: "")
}

return rows
