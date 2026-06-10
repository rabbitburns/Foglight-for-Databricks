package system._databricks.scripts;

// DLT Pipelines — one row per pipeline, RUNNING first then by name

def ts = server.get("TopologyService")

def rawRows = []
def workspaces = ts.getObjectsOfType(ts.getType("DatabricksWorkspace"))
workspaces?.each { ws ->
    ws.get("pipelines")?.each { p ->
        rawRows << [
            pipelineName : p.get("pipelineName")    ?: "",
            state        : p.get("state")           ?: "",
            creator      : p.get("creatorUserName") ?: "",
            runAs        : p.get("runAsUserName")   ?: "",
            pipelineId   : p.get("pipelineId")      ?: "",
            dollarCost   : p.get("dollarCostStr")   ?: ""
        ]
    }
}

rawRows.sort { a, b ->
    if (a.state == "RUNNING" && b.state != "RUNNING") return -1
    if (b.state == "RUNNING" && a.state != "RUNNING") return 1
    a.pipelineName.compareTo(b.pipelineName)
}

def rows = new java.util.ArrayList()
rawRows.each { r ->
    def row = functionHelper.createDataObject('databricks:DatabricksPipelineRow', 'none', null)
    row.store('pipelineName', r.pipelineName, specificTimeRange)
    row.store('state',        r.state,        specificTimeRange)
    row.store('creator',      r.creator,      specificTimeRange)
    row.store('runAs',        r.runAs,        specificTimeRange)
    row.store('pipelineId',   r.pipelineId,   specificTimeRange)
    row.store('dollarCost',   r.dollarCost,   specificTimeRange)
    rows.add(row)
}
return rows
