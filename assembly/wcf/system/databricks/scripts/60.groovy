package system._databricks.scripts;

// AI Gateway Endpoints — per-endpoint rollup sorted by total tokens desc

def ts = server.get("TopologyService")
def rows = new java.util.ArrayList()

def rawRows = []
ts.getObjectsOfType(ts.getType("DatabricksAiEndpoint"))?.each { ep ->
    long tot = 0L
    try { tot = Long.parseLong(ep.get("totalTokensStr") ?: "0") } catch (Exception ignore) {}
    rawRows << [
        endpointName : ep.get("endpointName")  ?: "",
        endpointType : ep.get("endpointType")  ?: "",
        requestCount : ep.get("requestCountStr") ?: "",
        totalTokens  : ep.get("totalTokensStr")  ?: "",
        inputTokens  : ep.get("inputTokensStr")  ?: "",
        outputTokens : ep.get("outputTokensStr") ?: "",
        errorCount   : ep.get("errorCountStr")   ?: "",
        errorRate    : ep.get("errorRateStr")    ?: "",
        avgLatency   : ep.get("avgLatencyStr")   ?: "",
        p95Latency   : ep.get("p95LatencyStr")   ?: "",
        dollarCost   : ep.get("dollarCostStr")   ?: "",
        totRaw       : tot
    ]
}

rawRows.sort { a, b -> Long.compare(b.totRaw, a.totRaw) }

rawRows.each { r ->
    def row = functionHelper.createDataObject('databricks:DatabricksAiEndpointRow', 'none', null)
    row.store('endpointName',  r.endpointName,  specificTimeRange)
    row.store('endpointType',  r.endpointType,  specificTimeRange)
    row.store('requestCount',  r.requestCount,  specificTimeRange)
    row.store('totalTokens',   r.totalTokens,   specificTimeRange)
    row.store('inputTokens',   r.inputTokens,   specificTimeRange)
    row.store('outputTokens',  r.outputTokens,  specificTimeRange)
    row.store('errorCount',    r.errorCount,    specificTimeRange)
    row.store('errorRate',     r.errorRate,     specificTimeRange)
    row.store('avgLatency',    r.avgLatency,    specificTimeRange)
    row.store('p95Latency',    r.p95Latency,    specificTimeRange)
    row.store('dollarCost',    r.dollarCost,    specificTimeRange)
    rows.add(row)
}

return rows
