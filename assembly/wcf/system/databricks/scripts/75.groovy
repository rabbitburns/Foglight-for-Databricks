package system._databricks.scripts;

// Returns all DatabricksWarehouse topology objects for sparkline table
def ts = server.get("TopologyService")
def warehouses = ts.getObjectsOfType(ts.getType("DatabricksWarehouse"))
return warehouses ? warehouses.toList() : []
