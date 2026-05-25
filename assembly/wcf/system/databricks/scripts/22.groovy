package system._databricks.scripts;

// DBU Usage — one row per usage record, sorted by date desc then DBU desc

def ts = server.get("TopologyService")

def rawRows = []
def workspaces = (ts.getType("DatabricksModelRoot").findAll() ?: []).collectMany { _r -> (_r.get("accounts") ?: []).collectMany { _a -> (_a.get("workspaces") ?: []) as List } }
workspaces?.each { ws ->
    ws.get("usages")?.each { u ->
        double dbu = 0.0
        try { dbu = Double.parseDouble(u.get("dbuConsumedStr") ?: "0") } catch (Exception ignore) {}
        rawRows << [
            usageDate            : u.get("usageDate")            ?: "",
            sku                  : u.get("sku")                  ?: "",
            billingOriginProduct : u.get("billingOriginProduct") ?: "",
            cloud                : u.get("cloud")                ?: "",
            region               : u.get("region")               ?: "",
            dbuConsumed          : u.get("dbuConsumedStr")        ?: "",
            dbuRaw               : dbu
        ]
    }
}

rawRows.sort { a, b ->
    int dateCmp = b.usageDate.compareTo(a.usageDate)
    dateCmp != 0 ? dateCmp : Double.compare(b.dbuRaw, a.dbuRaw)
}

def rows = new java.util.ArrayList()
rawRows.each { r ->
    def row = functionHelper.createDataObject('databricks:DatabricksUsageRow', 'none', null)
    row.store('usageDate',            r.usageDate,            specificTimeRange)
    row.store('sku',                  r.sku,                  specificTimeRange)
    row.store('billingOriginProduct', r.billingOriginProduct, specificTimeRange)
    row.store('cloud',                r.cloud,                specificTimeRange)
    row.store('region',               r.region,               specificTimeRange)
    row.store('dbuConsumed',          r.dbuConsumed,          specificTimeRange)
    rows.add(row)
}
return rows
