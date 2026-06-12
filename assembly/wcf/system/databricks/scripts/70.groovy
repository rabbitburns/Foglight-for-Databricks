package system._databricks.scripts;

// Job Success Rate by Day — per-job daily success rate for last 7 days

def ts = server.get("TopologyService")

// jobName -> date -> [total, success]
def jobDateMap = [:]

ts.getObjectsOfType(ts.getType("DatabricksWorkspace"))?.each { ws ->
    ws.get("jobs")?.each { job ->
        def jobName = job.get("jobName") ?: job.get("jobId") ?: "Unknown"
        if (!jobDateMap.containsKey(jobName)) jobDateMap[jobName] = [:]

        job.get("runs")?.each { run ->
            def startStr = run.get("startTimeStr") ?: ""
            if (!startStr || startStr.length() < 10) return
            def dateStr = startStr[0..9]   // "YYYY-MM-DD"

            def lc  = run.get("lifeCycleState") ?: ""
            def res = run.get("resultState")    ?: ""
            if (!("TERMINATED".equals(lc) || "SKIPPED".equals(lc))) return

            if (!jobDateMap[jobName].containsKey(dateStr))
                jobDateMap[jobName][dateStr] = [0, 0]
            jobDateMap[jobName][dateStr][0]++
            if ("SUCCESS".equals(res)) jobDateMap[jobName][dateStr][1]++
        }
    }
}

def allDates = new java.util.TreeSet<String>()
jobDateMap.values().each { allDates.addAll(it.keySet()) }
def sortedDates = allDates.toList().sort().reverse()
def last7  = sortedDates.take(7)
def prior7 = sortedDates.drop(7).take(7)

def fmtRate = { int succ, int total ->
    total > 0 ? "${(succ * 100 / total)}%" : "-"
}

def SPARKS = "▁▂▃▄▅▆▇█"
def toSpark = { List<String> days ->
    days.collect { s ->
        if (!s || s == "-") return '·'
        double v = 0.0
        try { v = Double.parseDouble(s.replace('%','')) } catch (Exception ignore) {}
        int idx = (int)(v / 100.0 * 7.99)
        SPARKS[Math.max(0, Math.min(7, idx))]
    }.join('')
}

def rawRows = []
jobDateMap.each { jobName, dateMap ->
    int wk1Total = 0, wk1Succ = 0, wk2Total = 0, wk2Succ = 0
    def dayStrs = []

    last7.each { d ->
        def pair = dateMap[d]
        if (pair) {
            wk1Total += (pair[0] as int)
            wk1Succ  += (pair[1] as int)
            dayStrs << fmtRate(pair[1] as int, pair[0] as int)
        } else {
            dayStrs << "-"
        }
    }
    prior7.each { d ->
        def pair = dateMap[d]
        if (pair) {
            wk2Total += (pair[0] as int)
            wk2Succ  += (pair[1] as int)
        }
    }

    def wk1Rate = wk1Total > 0 ? (wk1Succ * 100.0 / wk1Total) : -1.0
    def wk2Rate = wk2Total > 0 ? (wk2Succ * 100.0 / wk2Total) : -1.0
    String trendStr = wk2Rate >= 0
        ? (wk1Rate >= wk2Rate ? "+" : "") + String.format("%.1f", wk1Rate - wk2Rate) + "pp"
        : (wk1Rate >= 0 ? "New" : "-")

    def days7 = (0..6).collect { i -> dayStrs.size() > i ? dayStrs[i] : "-" }
    rawRows << [
        jobName  : jobName,
        d1       : days7[0], d2: days7[1], d3: days7[2], d4: days7[3],
        d5       : days7[4], d6: days7[5], d7: days7[6],
        spark    : toSpark(days7.reverse()),  // oldest→newest left to right
        weekRuns : String.valueOf(wk1Total),
        weekSucc : String.valueOf(wk1Succ),
        weekRate : wk1Total > 0 ? fmtRate(wk1Succ, wk1Total) : "-",
        trend    : trendStr,
        _rate    : wk1Rate
    ]
}

// Sort by 7-day success rate ascending (worst jobs first); jobs with no data go to bottom
rawRows.sort { a, b ->
    def ra = (a._rate ?: -1.0) as double
    def rb = (b._rate ?: -1.0) as double
    if (ra < 0 && rb < 0) return (a.jobName ?: "").compareTo(b.jobName ?: "")
    if (ra < 0) return 1
    if (rb < 0) return -1
    Double.compare(ra, rb)
}

def rows = new java.util.ArrayList()
rawRows.each { r ->
    def row = functionHelper.createDataObject('databricks:DatabricksJobTrendRow', 'none', null)
    row.store('jobName',  r.jobName,  specificTimeRange)
    row.store('d1',       r.d1,       specificTimeRange)
    row.store('d2',       r.d2,       specificTimeRange)
    row.store('d3',       r.d3,       specificTimeRange)
    row.store('d4',       r.d4,       specificTimeRange)
    row.store('d5',       r.d5,       specificTimeRange)
    row.store('d6',       r.d6,       specificTimeRange)
    row.store('d7',       r.d7,       specificTimeRange)
    row.store('spark',    r.spark,    specificTimeRange)
    row.store('weekRuns', r.weekRuns, specificTimeRange)
    row.store('weekSucc', r.weekSucc, specificTimeRange)
    row.store('weekRate', r.weekRate, specificTimeRange)
    row.store('trend',    r.trend,    specificTimeRange)
    rows.add(row)
}
return rows
