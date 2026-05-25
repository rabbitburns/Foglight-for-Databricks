package system._databricks.scripts;

// DIAGNOSTIC: Clusters — testing topology access patterns

def ts = server.get("TopologyService")

def rows = new java.util.ArrayList()

def addRow = { String msg ->
    def row = functionHelper.createDataObject('databricks:DatabricksClusterRow', 'none', null)
    row.store('clusterName',  msg,  specificTimeRange)
    row.store('state',        '',   specificTimeRange)
    row.store('source',       '',   specificTimeRange)
    row.store('driverNode',   '',   specificTimeRange)
    row.store('workers',      '',   specificTimeRange)
    row.store('minWorkers',   '',   specificTimeRange)
    row.store('maxWorkers',   '',   specificTimeRange)
    row.store('cores',        '',   specificTimeRange)
    row.store('sparkVersion', '',   specificTimeRange)
    row.store('creator',      '',   specificTimeRange)
    row.store('started',      '',   specificTimeRange)
    row.store('lastActivity', '',   specificTimeRange)
    row.store('terminated',   '',   specificTimeRange)
    row.store('pinnedBy',     '',   specificTimeRange)
    row.store('tags',         '',   specificTimeRange)
    rows.add(row)
}

// Test 1: getType("DatabricksModelRoot")
def rootType = null
try {
    rootType = ts.getType("DatabricksModelRoot")
    addRow("[1] getType(DatabricksModelRoot) = " + (rootType == null ? "NULL" : rootType.toString()))
} catch (e) {
    addRow("[1] getType THREW: " + e.toString())
}

// Test 2: getObjectsOfType(DatabricksModelRoot)
try {
    def roots = rootType ? ts.getObjectsOfType(rootType) : null
    def cnt = roots == null ? "null-result" : String.valueOf(roots.size())
    addRow("[2] getObjectsOfType(DatabricksModelRoot) count=" + cnt)
    roots?.each { r ->
        def name = ""
        try { name = r.get("name") } catch (ignored) {}
        addRow("[2]   root name=" + name)
        def accts = null
        try { accts = r.get("accounts") } catch (ignored) {}
        addRow("[2]   root.accounts=" + (accts == null ? "null" : String.valueOf(accts.size())))
        accts?.each { a ->
            def wsList = null
            try { wsList = a.get("workspaces") } catch (ignored) {}
            addRow("[2]     account.workspaces=" + (wsList == null ? "null" : String.valueOf(wsList.size())))
        }
    }
} catch (e) {
    addRow("[2] getObjectsOfType THREW: " + e.toString())
}

// Test 3: findAll()
try {
    def roots2 = rootType?.findAll()
    addRow("[3] findAll(DatabricksModelRoot) count=" + (roots2 == null ? "null" : String.valueOf(roots2.size())))
} catch (e) {
    addRow("[3] findAll THREW: " + e.toString())
}

// Test 4: getObjectsOfType(DatabricksWorkspace) — containment, expect empty
try {
    def wsType = ts.getType("DatabricksWorkspace")
    def wsList = wsType ? ts.getObjectsOfType(wsType) : null
    addRow("[4] getObjectsOfType(DatabricksWorkspace) count=" + (wsList == null ? "null" : String.valueOf(wsList.size())))
} catch (e) {
    addRow("[4] DatabricksWorkspace THREW: " + e.toString())
}

// Test 5: getObjectsOfType(DatabricksCluster) — containment, expect empty
try {
    def cType = ts.getType("DatabricksCluster")
    def cList = cType ? ts.getObjectsOfType(cType) : null
    addRow("[5] getObjectsOfType(DatabricksCluster) count=" + (cList == null ? "null" : String.valueOf(cList.size())))
} catch (e) {
    addRow("[5] DatabricksCluster THREW: " + e.toString())
}

// Test 6: getObjectsOfType(DatabricksAccount) — containment, expect empty
try {
    def aType = ts.getType("DatabricksAccount")
    def aList = aType ? ts.getObjectsOfType(aType) : null
    addRow("[6] getObjectsOfType(DatabricksAccount) count=" + (aList == null ? "null" : String.valueOf(aList.size())))
} catch (e) {
    addRow("[6] DatabricksAccount THREW: " + e.toString())
}

// Test 7: try getObjectsOfType(ModelRoot)
try {
    def mrType = ts.getType("ModelRoot")
    def mrList = mrType ? ts.getObjectsOfType(mrType) : null
    def mrCnt = mrList == null ? "null" : String.valueOf(mrList.size())
    addRow("[7] getObjectsOfType(ModelRoot) count=" + mrCnt)
    mrList?.each { mr ->
        def typeName = ""
        try { typeName = mr.getTopologyType()?.getName() } catch (ignored) {}
        addRow("[7]   ModelRoot type=" + typeName)
    }
} catch (e) {
    addRow("[7] ModelRoot THREW: " + e.toString())
}

return rows
