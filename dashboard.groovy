// Databricks strawman dashboard — run in Foglight Script Console
// Administration > Tooling > Script Console
// Output is the return value shown in the console result pane.

def ts   = server.get("TopologyService")
def sb   = new StringBuilder()
def line  = "-" * 72
def line2 = "=" * 72

def out = { s -> sb.append(s).append("\n") }

def metricVal = { m ->
    if (!m) return "n/a"
    try { def v = m.get("value"); return v != null ? v : "n/a" } catch (e) { return "?" }
}
def obsVal = { o ->
    if (!o) return "n/a"
    try { def v = o.get("value"); return v != null ? v : "n/a" } catch (e) { return "?" }
}
def dur = { ms ->
    if (!ms || ms == "n/a" || ms == 0) return "n/a"
    try {
        long s = (ms as long) / 1000; long m = s / 60; long h = m / 60
        h > 0 ? "${h}h ${m % 60}m" : m > 0 ? "${m}m ${s % 60}s" : "${s}s"
    } catch (e) { return "n/a" }
}
def fmtTs = { epochMs ->
    if (!epochMs || epochMs == "n/a" || epochMs == 0) return "n/a"
    try {
        new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(epochMs as long))
    } catch (e) { return "n/a" }
}

out(line2)
out("  DATABRICKS OVERVIEW")
out(line2)

def accounts = ts.getObjectsOfType(ts.getType("DatabricksAccount"))
if (!accounts || accounts.size() == 0) { out("No DatabricksAccount objects found."); return sb.toString() }

accounts.each { acct ->
    out("")
    out("ACCOUNT  ${acct.get('name')}  (id: ${acct.get('accountId')})")
    out(line)

    def workspaces = acct.get("workspaces")
    out("  Workspaces: ${workspaces?.size() ?: 0}")

    workspaces?.each { ws ->
        out("")
        out("  WORKSPACE  ${ws.get('workspaceUrl')}")
        out("  ${"─" * 68}")

        // ── Clusters ──────────────────────────────────────────────────────
        def clusters = ws.get("clusters")
        out("  Clusters: ${clusters?.size() ?: 0}  (clusterCount metric: ${metricVal(ws.get('clusterCount'))})")
        out("")

        clusters?.each { c ->
            def autoscale = c.get("autoscaleEnabled")
            def workerInfo
            if (autoscale?.toString() == "true") {
                def mn = metricVal(c.get("autoscaleMinWorkers"))
                def mx = metricVal(c.get("autoscaleMaxWorkers"))
                def tg = metricVal(c.get("autoscaleTargetWorkers"))
                workerInfo = "autoscale ${mn}-${mx}  (current: ${tg})"
            } else {
                workerInfo = "workers: ${metricVal(c.get('numWorkers'))}"
            }

            out("    [${c.get('stateStr') ?: 'n/a'}]  ${c.get('clusterName')}")
            out("      id:       ${c.get('clusterId')}")
            out("      source:   ${c.get('clusterSource')}")
            out("      spark:    ${c.get('sparkVersion')}")
            out("      nodes:    ${c.get('nodeTypeId')} / driver: ${c.get('driverNodeTypeId')}")
            out("      workers:  ${workerInfo}")
            out("      memory:   ${metricVal(c.get('clusterMemoryMb'))} MB")
            out("      cores:    ${metricVal(c.get('clusterCores'))}")
            def termCode = c.get("terminationCode")
            if (termCode) out("      term:     ${termCode} / ${c.get('terminationType')}")
            out("")
        }

        // ── Warehouses ────────────────────────────────────────────────────
        def warehouses = ws.get("warehouses")
        out("  SQL Warehouses: ${warehouses?.size() ?: 0}")
        out("")

        warehouses?.each { wh ->
            out("    [${wh.get('stateStr') ?: 'n/a'}]  ${wh.get('warehouseName')}")
            out("      id:       ${wh.get('warehouseId')}")
            out("      type:     ${wh.get('warehouseType')}  size: ${wh.get('size')}")
            out("      clusters: ${wh.get('numClustersStr') ?: 'n/a'} active  (min: ${metricVal(wh.get('minClusters'))}  max: ${metricVal(wh.get('maxClusters'))})")
            out("      photon:   ${wh.get('enablePhoton')}  auto-resume: ${wh.get('autoResume')}  auto-stop: ${metricVal(wh.get('autoStopMins'))} min")
            out("      creator:  ${wh.get('creatorName')}")
            out("")
        }

        // ── Pipelines (DLT) ───────────────────────────────────────────────
        def pipelines = ws.get("pipelines")
        out("  DLT Pipelines: ${pipelines?.size() ?: 0}")
        out("")

        pipelines?.each { pl ->
            out("    [${pl.get('stateStr') ?: 'n/a'}]  ${pl.get('pipelineName')}")
            out("      id:       ${pl.get('pipelineId')}")
            out("      creator:  ${pl.get('creatorUserName')}")
            out("      run as:   ${pl.get('runAsUserName')}")

            def updates = pl.get("updates")
            out("      updates:  ${updates?.size() ?: 0}")
            updates?.each { up ->
                out("        [${up.get('state') ?: 'n/a'}]  ${up.get('startTime') ?: ''}  id=${up.get('updateId') ?: ''}")
                def exps = up.get("expectations")
                if (exps?.size() > 0) {
                    out("          expectations: ${exps.size()}")
                    exps?.each { ex ->
                        out("            ${ex.get('expectationName')}  flow=${ex.get('flowName')}  passRate=${ex.get('passRate')}  passed=${ex.get('passedRecords')}  failed=${ex.get('failedRecords')}")
                    }
                } else {
                    out("          expectations: 0 (none defined or no flow_progress events yet)")
                }
            }
            out("")
        }

        // ── Instance Pools ────────────────────────────────────────────────
        def pools = ws.get("instancePools")
        if (pools && pools.size() > 0) {
            out("  Instance Pools: ${pools.size()}")
            out("")
            pools.each { pool ->
                out("    [${pool.get('stateStr') ?: 'n/a'}]  ${pool.get('instancePoolName')}")
                out("      id:       ${pool.get('instancePoolId')}")
                out("      node:     ${pool.get('nodeTypeId')}")
                out("      idle:     ${metricVal(pool.get('idleCount'))}  used: ${metricVal(pool.get('usedCount'))}  max: ${metricVal(pool.get('maxCapacity'))}")
                out("")
            }
        }

        // ── Jobs ──────────────────────────────────────────────────────────
        def jobs = ws.get("jobs")
        out("  Jobs: ${jobs?.size() ?: 0}")
        out("")

        jobs?.each { job ->
            def sched = job.get("scheduleCron")
            def schedInfo = (sched && sched != "") \
                ? "${sched}  [${job.get('scheduleStatus')}]" \
                : "manual / event-triggered"

            def lastResult = job.get("lastRunResult") ?: ""
            def lastState  = job.get("lastRunState")  ?: ""
            def lastStatus = lastResult ? "${lastState} / ${lastResult}" : lastState ?: "no runs"
            out("    [${lastStatus}]  ${job.get('jobName')}  (id: ${job.get('jobId')})")
            out("      creator:  ${job.get('creatorUserName')}")
            out("      schedule: ${schedInfo}")
            out("      last run: ${fmtTs(job.get('lastRunStartStr'))}  duration: ${dur(job.get('lastRunDurationStr'))}")

            def runs = job.get("runs")
            if (runs && runs.size() > 0) {
                out("      recent runs (${runs.size()}):")
                runs.each { run ->
                    def lc  = run.get("lifeCycleStateStr") ?: ""
                    def rs  = run.get("resultStateStr") ?: ""
                    def status = rs ? "${lc} / ${rs}" : lc
                    out("        run ${run.get('runId')}  [${status}]  trigger: ${run.get('triggerType')}")
                    out("          started:  ${fmtTs(run.get('startTimeStr'))}")
                    out("          duration: ${dur(run.get('durationMsStr'))}")
                    out("          tasks:    ${run.get('taskCountStr') ?: 'n/a'}")
                }
            } else {
                out("      no recent runs")
            }
            out("")
        }
    }
}
out(line2)
out("Done.")

return sb.toString()
