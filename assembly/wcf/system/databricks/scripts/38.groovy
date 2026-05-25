package system._databricks.scripts;

// Treemap — User Activity sized by query count

def ts = server.get("TopologyService")
def userMap = [:]

(ts.getType("DatabricksModelRoot").findAll() ?: []).collectMany { _r -> (_r.get("accounts") ?: []).collectMany { _a -> (_a.get("workspaces") ?: []) as List } }?.each { ws ->
    ws.get("warehouses")?.each { wh ->
        wh.get("queries")?.each { q ->
            def user = q.get("userName") ?: "(unknown)"
            if (!userMap.containsKey(user)) userMap[user] = 0
            userMap[user]++
        }
    }
}

def palette = [
    "#1f77b4", "#ff7f0e", "#2ca02c", "#d62728", "#9467bd",
    "#8c564b", "#e377c2", "#7f7f7f", "#bcbd22", "#17becf",
    "#aec7e8", "#ffbb78", "#98df8a", "#ff9896", "#c5b0d5"
]

def sorted = userMap.collect { k, v -> [user: k, count: v] }
sorted.sort { a, b -> b.count <=> a.count }

def nodes = new java.util.ArrayList()
sorted.eachWithIndex { r, i ->
    def node = functionHelper.createDataObject('databricks:DatabricksTreeMapNode', 'none', null)
    node.store('id',        r.user,                          specificTimeRange)
    node.store('name',      r.user,                          specificTimeRange)
    node.store('count',     String.valueOf(r.count),         specificTimeRange)
    node.store('fillColor', palette[i % palette.size()],     specificTimeRange)
    nodes.add(node)
}
return nodes
