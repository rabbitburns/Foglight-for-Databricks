package system._databricks.scripts;
import java.awt.Color;

// Bubble chart — User Activity: query count (x) vs avg duration (y), sized by query count
// Colour: blue (no errors) → orange (some) → red (high error rate)

def ts = server.get("TopologyService")
def userMap = [:]

(ts.getObjectsOfType(ts.getType("DatabricksModelRoot")) ?: []).collectMany { _r -> (_r.get("accounts") ?: []).collectMany { _a -> (_a.get("workspaces") ?: []) as List } }?.each { ws ->
    ws.get("warehouses")?.each { wh ->
        wh.get("queries")?.each { q ->
            def user = q.get("userName") ?: "(unknown)"
            if (!userMap.containsKey(user)) userMap[user] = [count: 0, totalMs: 0L, errors: 0]
            def u = userMap[user]
            u.count++
            u.totalMs += (q.get("duration") ?: 0L) as long
            if (q.get("errorMessage") && q.get("errorMessage") != "") u.errors++
        }
    }
}

def sorted = userMap.collect { k, v -> [user: k] + v }
sorted.sort { a, b -> b.count <=> a.count }

def nodes = new java.util.ArrayList()
sorted.each { r ->
    double avgSec = r.count > 0 ? (r.totalMs / r.count / 1000.0) : 0.0
    double errorRate = r.count > 0 ? (r.errors / (double) r.count) : 0.0
    def colorStr = errorRate >= 0.1 ? "#d62728" : errorRate > 0 ? "#ff7f0e" : "#1f77b4"

    def node = functionHelper.createDataObject('databricks:DatabricksBubbleNode', 'none', null)
    node.set('xValue', (double) r.count)
    node.set('yValue', avgSec)
    node.set('size',   (double) r.count)
    node.set('color',  Color.decode(colorStr))
    node.store('label', r.user, specificTimeRange)
    nodes.add(node)
}
return nodes
