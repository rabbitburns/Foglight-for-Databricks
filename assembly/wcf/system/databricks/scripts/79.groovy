package system._databricks.scripts;

// Untagged clusters — clusters with no custom tags set
// Sorted by state (RUNNING first) then by cluster name

def ts = server.get("TopologyService")
def rows = new java.util.ArrayList()

ts.getObjectsOfType(ts.getType("DatabricksCluster"))?.each { c ->
    String tags = c.get("customTagsStr") ?: ""
    if (tags.trim().isEmpty()) {
        rows.add(c)
    }
}

rows.sort { a, b ->
    boolean aRunning = (a.get("state") == "RUNNING")
    boolean bRunning = (b.get("state") == "RUNNING")
    if (aRunning && !bRunning) return -1
    if (bRunning && !aRunning) return 1
    (a.get("clusterName") ?: "").compareTo(b.get("clusterName") ?: "")
}

return rows
