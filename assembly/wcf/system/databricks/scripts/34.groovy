package system._databricks.scripts;

// Treemap data — DBU by Product, current month, colour-coded

def ts = server.get("TopologyService")
def now = Calendar.getInstance()
def currentYear  = now.get(Calendar.YEAR)
def currentMonth = now.get(Calendar.MONTH) + 1

def productDbu = [:]
ts.getObjectsOfType(ts.getType("DatabricksWorkspace"))?.each { ws ->
    ws.get("usages")?.each { u ->
        def dateStr = u.get("usageDate") ?: ""
        if (!dateStr) return
        def parts = dateStr.split("-")
        if (parts.length < 2) return
        if ((parts[0] as int) != currentYear || (parts[1] as int) != currentMonth) return
        def product = u.get("billingOriginProduct") ?: "UNKNOWN"
        double dbu = 0.0
        try { dbu = Double.parseDouble(u.get("dbuConsumedStr") ?: "0") } catch (Exception ignore) {}
        productDbu[product] = (productDbu[product] ?: 0.0) + dbu
    }
}

def palette = [
    "#1f77b4", "#ff7f0e", "#2ca02c", "#d62728", "#9467bd",
    "#8c564b", "#e377c2", "#7f7f7f", "#bcbd22", "#17becf",
    "#aec7e8", "#ffbb78", "#98df8a", "#ff9896", "#c5b0d5"
]

def sorted = productDbu.collect { k, v -> [product: k, dbu: v] }
sorted.sort { a, b -> Double.compare(b.dbu, a.dbu) }

def nodes = new java.util.ArrayList()
sorted.eachWithIndex { r, i ->
    def node = functionHelper.createDataObject('databricks:DatabricksTreeMapNode', 'none', null)
    node.store('id',        r.product,                       specificTimeRange)
    node.store('name',      r.product,                       specificTimeRange)
    node.store('count',     String.format("%.1f", r.dbu),    specificTimeRange)
    node.store('fillColor', palette[i % palette.size()],     specificTimeRange)
    nodes.add(node)
}
return nodes
