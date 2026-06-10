package system._databricks.scripts;

// Idle / underutilized clusters — RUNNING state, sorted by CPU util ascending
// Shows clusters that are running but doing little work (candidates for termination)

def ts = server.get("TopologyService")
def rows = new java.util.ArrayList()

ts.getObjectsOfType(ts.getType("DatabricksCluster"))?.each { c ->
    if (c.get("state") == "RUNNING") {
        rows.add(c)
    }
}

rows.sort { a, b ->
    long aCpu = 0L
    long bCpu = 0L
    try { aCpu = a.get("cpuUtil")?.getValue() ?: 0L } catch (Exception ignore) {}
    try { bCpu = b.get("cpuUtil")?.getValue() ?: 0L } catch (Exception ignore) {}
    Long.compare(aCpu, bCpu)
}

return rows
