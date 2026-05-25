package system._databricks.scripts;
import java.awt.Color;

// Bubble chart data — Cost vs DBU by SKU (last 60 days)

def ts = server.get("TopologyService")
def skuData = [:]

(ts.getType("DatabricksModelRoot").findAll() ?: []).collectMany { _r -> (_r.get("accounts") ?: []).collectMany { _a -> (_a.get("workspaces") ?: []) as List } }?.each { ws ->
    ws.get("usages")?.each { u ->
        def sku = u.get("sku") ?: "UNKNOWN"
        double dbu = 0.0; double cost = 0.0
        try { dbu  = Double.parseDouble(u.get("dbuConsumedStr") ?: "0") } catch (Exception ignore) {}
        try { cost = Double.parseDouble(u.get("dollarCostStr")  ?: "0") } catch (Exception ignore) {}
        if (!skuData.containsKey(sku)) skuData[sku] = [dbu: 0.0, cost: 0.0]
        skuData[sku].dbu  += dbu
        skuData[sku].cost += cost
    }
}

def palette = [
    "#1f77b4", "#ff7f0e", "#2ca02c", "#d62728", "#9467bd",
    "#8c564b", "#e377c2", "#7f7f7f", "#bcbd22", "#17becf",
    "#aec7e8", "#ffbb78", "#98df8a", "#ff9896", "#c5b0d5"
]

def sorted = skuData.collect { k, v -> [sku: k, dbu: v.dbu, cost: v.cost] }
sorted.sort { a, b -> Double.compare(b.dbu, a.dbu) }

def nodes = new java.util.ArrayList()
sorted.eachWithIndex { r, i ->
    def node = functionHelper.createDataObject('databricks:DatabricksBubbleNode', 'none', null)
    node.set('xValue', r.dbu)
    node.set('yValue', r.cost)
    node.set('size',   r.dbu)
    node.set('color',  Color.decode(palette[i % palette.size()]))
    node.store('label', r.sku, specificTimeRange)
    nodes.add(node)
}
return nodes
