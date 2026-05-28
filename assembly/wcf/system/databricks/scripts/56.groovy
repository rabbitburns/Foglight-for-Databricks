package system._databricks.scripts;

// Lakebase Projects — one row per project

def ts = server.get("TopologyService")
def rows = new java.util.ArrayList()

ts.getObjectsOfType(ts.getType("DatabricksWorkspace"))?.each { ws ->
    ws.get("lakebaseProjects")?.each { proj ->
        def branches    = proj.get("branches")
        def branchCount = branches ? String.valueOf(branches.size()) : "0"

        def row = functionHelper.createDataObject('databricks:DatabricksLakebaseProjectRow', 'none', null)
        row.store('projectId',   proj.get("projectId")   ?: "", specificTimeRange)
        row.store('displayName', proj.get("displayName") ?: "", specificTimeRange)
        row.store('branchCount', branchCount,                    specificTimeRange)
        rows.add(row)
    }
}

rows.sort { a, b -> (a.get("projectId") ?: "").compareTo(b.get("projectId") ?: "") }
return rows
