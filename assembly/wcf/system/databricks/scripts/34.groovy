package system._databricks.scripts;
import java.awt.Color;

// Treemap data — Cost by Product, current month, colour-coded

def ts = server.get("TopologyService")
def now = Calendar.getInstance()
def currentYear  = now.get(Calendar.YEAR)
def currentMonth = now.get(Calendar.MONTH) + 1

def productCost = [:]
ts.getObjectsOfType(ts.getType("DatabricksWorkspace"))?.each { ws ->
    ws.get("usages")?.each { u ->
        def dateStr = u.get("usageDate") ?: ""
        if (!dateStr) return
        def parts = dateStr.split("-")
        if (parts.length < 2) return
        if ((parts[0] as int) != currentYear || (parts[1] as int) != currentMonth) return
        def product = u.get("billingOriginProduct") ?: "UNKNOWN"
        double cost = 0.0
        try { cost = Double.parseDouble((u.get("dollarCostStr") ?: "0").replace('$','').replace(',','')) } catch (Exception ignore) {}
        productCost[product] = (productCost[product] ?: 0.0) + cost
    }
}

def palette = [
    "#1f77b4", "#ff7f0e", "#2ca02c", "#d62728", "#9467bd",
    "#8c564b", "#e377c2", "#7f7f7f", "#bcbd22", "#17becf",
    "#aec7e8", "#ffbb78", "#98df8a", "#ff9896", "#c5b0d5"
]

def sorted = productCost.collect { k, v -> [product: k, cost: v] }
sorted.sort { a, b -> Double.compare(b.cost, a.cost) }

def nodes = new java.util.ArrayList()
sorted.eachWithIndex { r, i ->
    def node = functionHelper.createDataObject('databricks:DatabricksTreeMapNode', 'none', null)
    node.set('id',        r.product)
    node.set('name',      r.product)
    node.set('count',     r.cost)
    node.set('fillColor', Color.decode(palette[i % palette.size()]))
    nodes.add(node)
}
return nodes
