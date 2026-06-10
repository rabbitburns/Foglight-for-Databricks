package system._databricks.scripts;

// User Compute Spend — per user per billing product, last 30 days, sorted by cost desc

def ts = server.get("TopologyService")
def rawRows = []

def workspaces = ts.getObjectsOfType(ts.getType("DatabricksWorkspace"))
workspaces?.each { ws ->
    ws.get("userSpends")?.each { us ->
        double cost = 0.0
        try { cost = Double.parseDouble((us.get("dollarCostStr") ?: "0").replace('$','').replace(',','')) } catch (Exception ignore) {}
        rawRows << [
            runAsUser      : us.get("runAsUser")      ?: "",
            billingProduct : us.get("billingProduct") ?: "",
            dbu            : us.get("dbuConsumedStr") ?: "",
            dollarCost     : us.get("dollarCostStr")  ?: "",
            costRaw        : cost
        ]
    }
}

rawRows.sort { a, b -> Double.compare(b.costRaw, a.costRaw) }

def rows = new java.util.ArrayList()
rawRows.each { r ->
    def row = functionHelper.createDataObject('databricks:DatabricksUserSpendRow', 'none', null)
    row.store('runAsUser',      r.runAsUser,      specificTimeRange)
    row.store('billingProduct', r.billingProduct, specificTimeRange)
    row.store('dbu',            r.dbu,            specificTimeRange)
    row.store('dollarCost',     r.dollarCost,     specificTimeRange)
    rows.add(row)
}
return rows
