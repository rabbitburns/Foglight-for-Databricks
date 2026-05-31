package system._databricks.scripts;

// Returns all DatabricksCluster topology objects for sparkline table
def ts = server.get("TopologyService")
def clusters = ts.getObjectsOfType(ts.getType("DatabricksCluster"))
return clusters ? clusters.toList() : []
