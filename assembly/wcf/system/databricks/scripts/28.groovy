package system._databricks.scripts;

// MoM DBU Growth by Product — current month vs previous month, sorted by growth rate desc

def ts = server.get("TopologyService")
def now = Calendar.getInstance()
def currentYear = now.get(Calendar.YEAR)
def currentMonth = now.get(Calendar.MONTH) + 1

def prevCal = Calendar.getInstance()
prevCal.add(Calendar.MONTH, -1)
def prevYear = prevCal.get(Calendar.YEAR)
def prevMonth = prevCal.get(Calendar.MONTH) + 1

def currentDbu = [:]
def previousDbu = [:]

def workspaces = (ts.getObjectsOfType(ts.getType("DatabricksModelRoot")) ?: []).collectMany { _r -> (_r.get("accounts") ?: []).collectMany { _a -> (_a.get("workspaces") ?: []) as List } }
workspaces?.each { ws ->
    ws.get("usages")?.each { u ->
        def dateStr = u.get("usageDate") ?: ""
        if (!dateStr) return
        def parts = dateStr.split("-")
        if (parts.length < 2) return
        int y = parts[0] as int
        int m = parts[1] as int
        def product = u.get("billingOriginProduct") ?: "UNKNOWN"
        double dbu = 0.0
        try { dbu = Double.parseDouble(u.get("dbuConsumedStr") ?: "0") } catch (Exception ignore) {}
        if (y == currentYear && m == currentMonth) {
            currentDbu[product] = (currentDbu[product] ?: 0.0) + dbu
        } else if (y == prevYear && m == prevMonth) {
            previousDbu[product] = (previousDbu[product] ?: 0.0) + dbu
        }
    }
}

def allProducts = (currentDbu.keySet() + previousDbu.keySet()) as Set
def rawRows = []
allProducts.each { product ->
    double curr = currentDbu[product] ?: 0.0
    double prev = previousDbu[product] ?: 0.0
    double growth = prev > 0 ? ((curr - prev) / prev * 100) : 0.0
    rawRows << [
        product  : product,
        curr     : curr,
        prev     : prev,
        growth   : growth,
        currStr  : String.format("%.2f", curr),
        prevStr  : String.format("%.2f", prev),
        growthStr: String.format("%.1f%%", growth)
    ]
}
rawRows.sort { a, b -> Double.compare(b.growth, a.growth) }

def rows = new java.util.ArrayList()
rawRows.each { r ->
    def row = functionHelper.createDataObject('databricks:DatabricksMomGrowthRow', 'none', null)
    row.store('product',          r.product,   specificTimeRange)
    row.store('currentMonthDbu',  r.currStr,   specificTimeRange)
    row.store('previousMonthDbu', r.prevStr,   specificTimeRange)
    row.store('growthRate',       r.growthStr, specificTimeRange)
    rows.add(row)
}
return rows
