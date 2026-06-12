package system._databricks.scripts;

// MoM Cost Slopegraph — previous month vs current month cost by product

def ts = server.get("TopologyService")

def now = Calendar.getInstance()
int cy = now.get(Calendar.YEAR)
int cm = now.get(Calendar.MONTH) + 1

def prevCal = Calendar.getInstance()
prevCal.add(Calendar.MONTH, -1)
int py = prevCal.get(Calendar.YEAR)
int pm = prevCal.get(Calendar.MONTH) + 1

def currCost = [:]
def prevCost = [:]

ts.getObjectsOfType(ts.getType("DatabricksWorkspace"))?.each { ws ->
    ws.get("usages")?.each { u ->
        def dateStr = u.get("usageDate") ?: ""
        if (!dateStr) return
        def parts = dateStr.split("-")
        if (parts.length < 2) return
        int y = parts[0] as int
        int m = parts[1] as int
        def product = u.get("billingOriginProduct") ?: "UNKNOWN"
        double cost = 0.0
        try {
            cost = Double.parseDouble((u.get("dollarCostStr") ?: "0").replaceAll(/[\$,]/, ""))
        } catch (Exception ignore) {}
        if (y == cy && m == cm) currCost[product] = (currCost[product] ?: 0.0) + cost
        else if (y == py && m == pm) prevCost[product] = (prevCost[product] ?: 0.0) + cost
    }
}

def allP = ((currCost.keySet() + prevCost.keySet()) as Set).toList()
allP = allP.findAll { (currCost[it] ?: 0.0) + (prevCost[it] ?: 0.0) > 0.005 }
allP.sort { a, b -> Double.compare(currCost[b] ?: 0.0, currCost[a] ?: 0.0) }

def fmt = { double v ->
    if (v < 0.005) return '-'
    if (v >= 1000) return '$' + String.format('%,.0f', v)
    return '$' + String.format('%.2f', v)
}

def rows = new java.util.ArrayList()
allP.each { prod ->
    double curr = currCost[prod] ?: 0.0
    double prev = prevCost[prod] ?: 0.0
    double pct = prev > 0.005 ? ((curr - prev) / prev * 100.0) : (curr > 0.005 ? 9999.0 : 0.0)

    String trend
    if (pct > 9000)       trend = "new"
    else if (pct > 15)    trend = String.format("▲ +%.0f%%", pct)
    else if (pct < -15)   trend = String.format("▼ %.0f%%", pct)
    else if (pct >= 0)    trend = String.format("→ +%.0f%%", pct)
    else                  trend = String.format("→ %.0f%%", pct)

    def row = functionHelper.createDataObject('databricks:DatabricksSlopegraphRow', 'none', null)
    row.store('product',   prod,        specificTimeRange)
    row.store('prevCost',  fmt(prev),   specificTimeRange)
    row.store('trend',     trend,       specificTimeRange)
    row.store('currCost',  fmt(curr),   specificTimeRange)
    row.store('changePct', trend,       specificTimeRange)
    rows.add(row)
}
return rows
