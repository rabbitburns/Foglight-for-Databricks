package system._databricks.scripts;

// Daily DBU Trend — total DBUs per day across all products (last 60 days), sorted by date desc

def ts = server.get("TopologyService")
def dailyDbu = [:]

def workspaces = ts.getObjectsOfType(ts.getType("DatabricksWorkspace"))
workspaces?.each { ws ->
    ws.get("usages")?.each { u ->
        def date = u.get("usageDate") ?: ""
        if (!date) return
        double dbu = 0.0
        try { dbu = Double.parseDouble(u.get("dbuConsumedStr") ?: "0") } catch (Exception ignore) {}
        dailyDbu[date] = (dailyDbu[date] ?: 0.0) + dbu
    }
}

def rawRows = []
dailyDbu.each { date, dbu ->
    rawRows << [date: date, dbu: dbu, dbuStr: String.format("%.1f", dbu)]
}
rawRows.sort { a, b -> b.date.compareTo(a.date) }

double maxDbu = rawRows.collect { it.dbu as double }.max() ?: 1.0
def dbuBar = { double v ->
    if (v <= 0) return ""
    int filled = (int)Math.round(v / maxDbu * 8)
    filled = Math.max(1, Math.min(8, filled))
    "████████".substring(0, filled) + "░░░░░░░░".substring(0, 8 - filled)
}

def rows = new java.util.ArrayList()
rawRows.each { r ->
    def row = functionHelper.createDataObject('databricks:DatabricksDailyDbuRow', 'none', null)
    row.store('usageDate', r.date,          specificTimeRange)
    row.store('totalDbu',  r.dbuStr,        specificTimeRange)
    row.store('dbuBar',    dbuBar(r.dbu),   specificTimeRange)
    rows.add(row)
}
return rows
