package system._databricks.scripts;

// Table Optimization History — predictive optimization ops, last 7 days, most recent first

def ts = server.get("TopologyService")
def rows = new java.util.ArrayList()

def workspaces = ts.getObjectsOfType(ts.getType("DatabricksWorkspace"))
workspaces?.each { ws ->
    ws.get("optimizationOps")?.each { op ->
        def row = functionHelper.createDataObject('databricks:DatabricksOptimizationOpRow', 'none', null)
        row.store('tableCatalog',    op.get("tableCatalog")    ?: "", specificTimeRange)
        row.store('tableSchema',     op.get("tableSchema")     ?: "", specificTimeRange)
        row.store('tableName',       op.get("tableName")       ?: "", specificTimeRange)
        row.store('operationType',   op.get("operationType")   ?: "", specificTimeRange)
        row.store('operationStatus', op.get("operationStatus") ?: "", specificTimeRange)
        row.store('startTime',       op.get("startTime")       ?: "", specificTimeRange)
        row.store('usageStr',        op.get("usageStr")        ?: "", specificTimeRange)
        rows.add(row)
    }
}
return rows
