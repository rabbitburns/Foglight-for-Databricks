package system._databricks.scripts;

// DBU by Product — current calendar month, sorted by DBU desc

def ts = server.get("TopologyService")
def now = Calendar.getInstance()
def currentYear = now.get(Calendar.YEAR)
def currentMonth = now.get(Calendar.MONTH) + 1

def productDbu = [:]
def workspaces = (ts.getType("DatabricksModelRoot").findAll() ?: []).collectMany { _r -> (_r.get("accounts") ?: []).collectMany { _a -> (_a.get("workspaces") ?: []) as List } }
workspaces?.each { ws ->
    ws.get("usages")?.each { u ->
        def dateStr = u.get("usageDate") ?: ""
        if (!dateStr) return
        def parts = dateStr.split("-")
        if (parts.length < 2) return
        int y = parts[0] as int
        int m = parts[1] as int
        if (y != currentYear || m != currentMonth) return
        def product = u.get("billingOriginProduct") ?: "UNKNOWN"
        double dbu = 0.0
        try { dbu = Double.parseDouble(u.get("dbuConsumedStr") ?: "0") } catch (Exception ignore) {}
        productDbu[product] = (productDbu[product] ?: 0.0) + dbu
    }
}

def rawRows = []
productDbu.each { product, dbu ->
    rawRows << [product: product, dbu: dbu, dbuStr: String.format("%.2f", dbu)]
}
rawRows.sort { a, b -> Double.compare(b.dbu, a.dbu) }

def rows = new java.util.ArrayList()
rawRows.each { r ->
    def row = functionHelper.createDataObject('databricks:DatabricksProductDbuRow', 'none', null)
    row.store('product', r.product, specificTimeRange)
    row.store('dbu',     r.dbuStr,  specificTimeRange)
    rows.add(row)
}
return rows
