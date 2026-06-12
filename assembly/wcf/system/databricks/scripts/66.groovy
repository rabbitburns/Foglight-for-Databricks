package system._databricks.scripts;

// DBU Spend Trend by SKU — per-SKU daily DBU for last 7 days
// with 7-day totals and trend vs prior 7 days

def ts = server.get("TopologyService")

def skuDateMap = [:]   // sku -> (date -> dbu)
def skuProduct  = [:]  // sku -> product

ts.getObjectsOfType(ts.getType("DatabricksWorkspace"))?.each { ws ->
    ws.get("usages")?.each { u ->
        def sku     = u.get("sku") ?: "UNKNOWN"
        def dateStr = u.get("usageDate") ?: ""
        if (!dateStr) return

        if (!skuProduct.containsKey(sku))
            skuProduct[sku] = u.get("billingOriginProduct") ?: ""

        double dbu = 0.0
        try { dbu = Double.parseDouble(u.get("dbuConsumedStr") ?: "0") } catch (Exception ignore) {}

        if (!skuDateMap.containsKey(sku)) skuDateMap[sku] = [:]
        skuDateMap[sku][dateStr] = (skuDateMap[sku][dateStr] ?: 0.0) + dbu
    }
}

// 14 most recent dates across all SKUs (newest first)
def allDates = new java.util.TreeSet<String>()
skuDateMap.values().each { allDates.addAll(it.keySet()) }
def sortedDates = allDates.toList().sort().reverse()
def last7  = sortedDates.take(7)
def prior7 = sortedDates.drop(7).take(7)

def fmt = { double v -> v > 0 ? String.format("%.1f", v) : "-" }

def SPARKS = "▁▂▃▄▅▆▇█"
def dbuSpark = { List<Double> vals ->
    double mx = vals.max() ?: 0.0
    if (mx <= 0) return "·······"
    vals.collect { v ->
        if (v <= 0) return '·'
        int idx = (int)(v / mx * 7.99)
        SPARKS[Math.max(0, Math.min(7, idx))]
    }.join('')
}

def rawRows = []
skuDateMap.each { sku, dateMap ->
    def dayVals = last7.collect { d -> (dateMap[d] ?: 0.0) as double }
    double wk1  = dayVals.sum() ?: 0.0
    double wk2  = prior7.collect { d -> (dateMap[d] ?: 0.0) as double }.sum() ?: 0.0

    String trendStr = wk2 > 0
        ? (wk1 >= wk2 ? "+" : "") + String.format("%.1f", ((wk1 - wk2) / wk2) * 100.0) + "%"
        : (wk1 > 0 ? "New" : "-")

    def paddedVals = (0..6).collect { i -> i < dayVals.size() ? dayVals[i] : 0.0 }
    rawRows << [
        sku          : sku,
        product      : skuProduct[sku] ?: "",
        d1           : fmt(paddedVals[0]), d2: fmt(paddedVals[1]), d3: fmt(paddedVals[2]),
        d4           : fmt(paddedVals[3]), d5: fmt(paddedVals[4]), d6: fmt(paddedVals[5]),
        d7           : fmt(paddedVals[6]),
        spark        : dbuSpark(paddedVals.reverse()),  // oldest→newest
        weekTotal    : fmt(wk1),
        prevWeekTotal: fmt(wk2),
        trend        : trendStr,
        _wk1         : wk1
    ]
}

rawRows.sort { a, b -> Double.compare((b._wk1 ?: 0.0) as double, (a._wk1 ?: 0.0) as double) }

def rows = new java.util.ArrayList()
rawRows.each { r ->
    def row = functionHelper.createDataObject('databricks:DatabricksSkuSparkRow', 'none', null)
    row.set('sku',           r.sku)
    row.set('product',       r.product)
    row.set('d1',            r.d1)
    row.set('d2',            r.d2)
    row.set('d3',            r.d3)
    row.set('d4',            r.d4)
    row.set('d5',            r.d5)
    row.set('d6',            r.d6)
    row.set('d7',            r.d7)
    row.set('spark',         r.spark)
    row.set('weekTotal',     r.weekTotal)
    row.set('prevWeekTotal', r.prevWeekTotal)
    row.set('trend',         r.trend)
    rows.add(row)
}
return rows
