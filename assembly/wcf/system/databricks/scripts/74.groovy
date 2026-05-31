package system._databricks.scripts;

// Returns all DatabricksJob topology objects for sparkline table
def ts = server.get("TopologyService")
def jobs = ts.getObjectsOfType(ts.getType("DatabricksJob"))
return jobs ? jobs.toList() : []
