package system._databricks.scripts;

// Cost by SKU — 60-day totals, sorted by dollar cost desc

def ts = server.get("TopologyService")
def skuData = [:]

def workspaces = ts.getObjectsOfType(ts.getType("DatabricksWorkspace"))
workspaces?.each { ws ->
    ws.get("usages")?.each { u ->
        def sku     = u.get("sku")                  ?: "UNKNOWN"
        def product = u.get("billingOriginProduct") ?: ""
        double dbu  = 0.0
        double cost = 0.0
        try { dbu  = Double.parseDouble(u.get("dbuConsumedStr")  ?: "0") } catch (Exception ignore) {}
        try { cost = Double.parseDouble(u.get("dollarCostStr")   ?: "0") } catch (Exception ignore) {}
        if (!skuData.containsKey(sku)) {
            skuData[sku] = [product: product, dbu: 0.0, cost: 0.0]
        }
        skuData[sku].dbu  += dbu
        skuData[sku].cost += cost
    }
}

def rawRows = []
skuData.each { sku, data ->
    rawRows << [
        sku    : sku,
        product: data.product,
        dbu    : data.dbu,
        dbuStr : String.format("%.2f", data.dbu),
        cost   : data.cost,
        costStr: String.format('$%.4f', data.cost)
    ]
}
rawRows.sort { a, b -> Double.compare(b.cost, a.cost) }

def rows = new java.util.ArrayList()
rawRows.each { r ->
    def row = functionHelper.createDataObject('databricks:DatabricksSkuCostRow', 'none', null)
    row.store('sku',        r.sku,     specificTimeRange)
    row.store('product',    r.product, specificTimeRange)
    row.store('totalDbu',   r.dbuStr,  specificTimeRange)
    row.store('dollarCost', r.costStr, specificTimeRange)
    rows.add(row)
}
return rows
