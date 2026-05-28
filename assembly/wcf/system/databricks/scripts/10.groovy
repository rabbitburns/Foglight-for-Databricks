package system._databricks.scripts;

def ts = server.get("TopologyService")

def mkRow = { cat, total, active, inactive, details ->
    def row = functionHelper.createDataObject('databricks:DatabricksSummaryRow', 'none', null)
    row.store('category', cat,                    specificTimeRange)
    row.store('total',    String.valueOf(total),   specificTimeRange)
    row.store('active',   active,                  specificTimeRange)
    row.store('inactive', inactive,                specificTimeRange)
    row.store('details',  details,                 specificTimeRange)
    row
}

def rows = new java.util.ArrayList()
def workspaces = ts.getObjectsOfType(ts.getType("DatabricksWorkspace"))

workspaces?.each { ws ->
    def clusters   = ws.get("clusters")
    def warehouses = ws.get("warehouses")
    def jobs       = ws.get("jobs")
    def pipelines  = ws.get("pipelines")
    def pools      = ws.get("instancePools")

    def totalClusters   = clusters?.size()   ?: 0
    def activeClusters  = clusters?.count   { it.get("state") == "RUNNING" } ?: 0

    def totalWH         = warehouses?.size() ?: 0
    def activeWH        = warehouses?.count  { it.get("state") == "RUNNING" } ?: 0

    def totalJobs       = jobs?.size()       ?: 0
    def activeJobs      = jobs?.count        { it.get("scheduleStatus") == "ACTIVE" } ?: 0

    def totalPipelines  = pipelines?.size()  ?: 0
    def activePipelines = pipelines?.count   { it.get("state") == "RUNNING" } ?: 0

    def totalPools      = pools?.size()      ?: 0

    def lakebaseProjects = ws.get("lakebaseProjects")
    def totalLakebase    = lakebaseProjects?.size() ?: 0
    def totalBranches    = lakebaseProjects?.collect { (it.get("branches")?.size() ?: 0) }?.sum() ?: 0

    rows.add(mkRow("Clusters",          totalClusters,   String.valueOf(activeClusters),  String.valueOf(totalClusters - activeClusters),  ""))
    rows.add(mkRow("SQL Warehouses",    totalWH,         String.valueOf(activeWH),         String.valueOf(totalWH - activeWH),              ""))
    rows.add(mkRow("Jobs",              totalJobs,       String.valueOf(activeJobs),        String.valueOf(totalJobs - activeJobs),          "scheduled"))
    rows.add(mkRow("Pipelines",         totalPipelines,  String.valueOf(activePipelines),   String.valueOf(totalPipelines - activePipelines), ""))
    rows.add(mkRow("Instance Pools",    totalPools,      "",                                "",                                              ""))
    rows.add(mkRow("Lakebase",          totalLakebase,   String.valueOf(totalBranches),     "",                                              "projects · branches"))
}

if (rows.isEmpty()) {
    rows.add(mkRow("No data", 0, "", "", "waiting for collection"))
}

return rows
