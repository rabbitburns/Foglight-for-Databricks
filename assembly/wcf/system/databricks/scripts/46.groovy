package system._databricks.scripts;

// Pipeline Data Quality Expectations — all expectations across last 5 updates per pipeline

def ts = server.get("TopologyService")
def rows = new java.util.ArrayList()

(ts.getObjectsOfType(ts.getType("DatabricksModelRoot")) ?: []).collectMany { _r -> (_r.get("accounts") ?: []).collectMany { _a -> (_a.get("workspaces") ?: []) as List } }?.each { ws ->
    ws.get("pipelines")?.each { pl ->
        def pipelineName = pl.get("pipelineName") ?: pl.get("pipelineId") ?: "(unknown)"
        pl.get("updates")?.each { up ->
            up.get("expectations")?.each { ex ->
                def row = functionHelper.createDataObject('databricks:DatabricksPipelineExpectationRow', 'none', null)
                row.store('pipelineName',    pipelineName,                    specificTimeRange)
                row.store('updateId',        ex.get("updateId")        ?: "", specificTimeRange)
                row.store('flowName',        ex.get("flowName")        ?: "", specificTimeRange)
                row.store('expectationName', ex.get("expectationName") ?: "", specificTimeRange)
                row.store('dataset',         ex.get("dataset")         ?: "", specificTimeRange)
                row.store('passedRecords',   ex.get("passedRecords")   ?: "0", specificTimeRange)
                row.store('failedRecords',   ex.get("failedRecords")   ?: "0", specificTimeRange)
                row.store('droppedRecords',  ex.get("droppedRecords")  ?: "0", specificTimeRange)
                row.store('passRate',        ex.get("passRate")        ?: "N/A", specificTimeRange)
                rows.add(row)
            }
        }
    }
}

// Sort: failures first, then by pipeline name
rows.sort { a, b ->
    def aFailed = (a.get("failedRecords") ?: "0") as long
    def bFailed = (b.get("failedRecords") ?: "0") as long
    if (bFailed != aFailed) return Long.compare(bFailed, aFailed)
    return (a.get("pipelineName") ?: "").compareTo(b.get("pipelineName") ?: "")
}

return rows
