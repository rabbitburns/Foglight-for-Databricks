package system._databricks.scripts;

// SKU List Price reference table — current prices from system.billing.list_prices

def ts = server.get("TopologyService")
def rows = new java.util.ArrayList()

ts.getObjectsOfType(ts.getType("DatabricksWorkspace"))?.each { ws ->
    ws.get("skuPrices")?.each { sp ->
        def row = functionHelper.createDataObject('databricks:DatabricksSkuPriceRow', 'none', null)
        row.store('skuName',         sp.get("skuName")         ?: "", specificTimeRange)
        row.store('cloud',           sp.get("cloud")           ?: "", specificTimeRange)
        row.store('currencyCode',    sp.get("currencyCode")    ?: "USD", specificTimeRange)
        row.store('listPricePerDbu', sp.get("listPricePerDbu") ?: "0.0000", specificTimeRange)
        row.store('priceStartTime',  sp.get("priceStartTime")  ?: "", specificTimeRange)
        rows.add(row)
    }
}

rows.sort { a, b ->
    def skuCmp = (a.get("skuName") ?: "").compareTo(b.get("skuName") ?: "")
    if (skuCmp != 0) return skuCmp
    return (a.get("cloud") ?: "").compareTo(b.get("cloud") ?: "")
}

return rows
