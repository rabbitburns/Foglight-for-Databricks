package system._databricks.scripts;

// Lakebase Branches — one row per branch across all projects

def ts = server.get("TopologyService")
def rows = new java.util.ArrayList()

ts.getObjectsOfType(ts.getType("DatabricksWorkspace"))?.each { ws ->
    ws.get("lakebaseProjects")?.each { proj ->
        proj.get("branches")?.each { branch ->
            def row = functionHelper.createDataObject('databricks:DatabricksLakebaseBranchRow', 'none', null)
            row.store('projectId',     branch.get("projectId")     ?: "", specificTimeRange)
            row.store('branchId',      branch.get("branchId")      ?: "", specificTimeRange)
            row.store('displayName',   branch.get("displayName")   ?: "", specificTimeRange)
            row.store('endpointHost',  branch.get("endpointHost")  ?: "", specificTimeRange)
            row.store('endpointState', branch.get("endpointState") ?: "", specificTimeRange)
            rows.add(row)
        }
    }
}

rows.sort { a, b ->
    def cmp = (a.get("projectId") ?: "").compareTo(b.get("projectId") ?: "")
    cmp != 0 ? cmp : (a.get("branchId") ?: "").compareTo(b.get("branchId") ?: "")
}
return rows
