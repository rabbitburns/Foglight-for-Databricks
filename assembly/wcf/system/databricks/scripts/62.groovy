package system._databricks.scripts;

// AI Token Usage — daily by endpoint + model, sorted by date desc then tokens desc

def ts = server.get("TopologyService")
def rows = new java.util.ArrayList()

def rawRows = []
ts.getObjectsOfType(ts.getType("DatabricksAiUsage"))?.each { u ->
    long tot = 0L
    try { tot = Long.parseLong(u.get("totalTokensStr") ?: "0") } catch (Exception ignore) {}
    rawRows << [
        usageDate    : u.get("usageDate")       ?: "",
        endpointName : u.get("endpointName")    ?: "",
        modelName    : u.get("modelName")       ?: "",
        requestCount : u.get("requestCountStr") ?: "",
        totalTokens  : u.get("totalTokensStr")  ?: "",
        inputTokens  : u.get("inputTokensStr")  ?: "",
        outputTokens : u.get("outputTokensStr") ?: "",
        totRaw       : tot
    ]
}

rawRows.sort { a, b ->
    int dateCmp = b.usageDate.compareTo(a.usageDate)
    dateCmp != 0 ? dateCmp : Long.compare(b.totRaw, a.totRaw)
}

rawRows.each { r ->
    def row = functionHelper.createDataObject('databricks:DatabricksAiUsageRow', 'none', null)
    row.store('usageDate',    r.usageDate,    specificTimeRange)
    row.store('endpointName', r.endpointName, specificTimeRange)
    row.store('modelName',    r.modelName,    specificTimeRange)
    row.store('requestCount', r.requestCount, specificTimeRange)
    row.store('totalTokens',  r.totalTokens,  specificTimeRange)
    row.store('inputTokens',  r.inputTokens,  specificTimeRange)
    row.store('outputTokens', r.outputTokens, specificTimeRange)
    rows.add(row)
}

return rows
