package system._databricks.scripts;

// Lakehouse Monitor Inventory — all monitored tables, sorted by most recent run desc

def ts = server.get("TopologyService")
def rows = new java.util.ArrayList()

ts.getObjectsOfType(ts.getType("DatabricksWorkspace"))?.each { ws ->
    ws.get("monitors")?.each { m ->
        def catalog = m.get("catalogName") ?: ""
        def schema  = m.get("schemaName")  ?: ""
        def table   = m.get("tableName")   ?: ""
        def fqn     = catalog ? "${catalog}.${schema}.${table}" : table

        def row = functionHelper.createDataObject('databricks:DatabricksMonitorRow', 'none', null)
        row.store('monitorKey',    m.get("monitorKey")     ?: "", specificTimeRange)
        row.store('tableName',     fqn,                          specificTimeRange)
        row.store('monitorType',   m.get("monitorType")    ?: "", specificTimeRange)
        row.store('lastRunStatus', m.get("lastRunStatus")  ?: "", specificTimeRange)
        row.store('lastRunTime',   m.get("lastRunTimeStr") ?: "", specificTimeRange)
        row.store('runCount30d',   m.get("runCount30d")    ?: "", specificTimeRange)
        row.store('failCount30d',  m.get("failCount30d")   ?: "", specificTimeRange)
        row.store('rowCount',          m.get("rowCount")          ?: "", specificTimeRange)
        row.store('rowCountDelta',     m.get("rowCountDelta")     ?: "", specificTimeRange)
        row.store('driftColumnCount',  m.get("driftColumnCount")  ?: "", specificTimeRange)
        rows.add(row)
    }
}
return rows
