package com.quest.foglight.databricks;

import com.fasterxml.jackson.databind.JsonNode;
import com.quest.glue.api.services.LogService;
import com.quest.glue.api.services.TopologyDataSubmissionService3;
import com.quest.glue.api.services.TopologyNode;
import com.quest.glue.api.services.TopologyValue;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClusterCollector {

    private final DatabricksClient client;
    private final TopologyDataSubmissionService3 topologyService;
    private final LogService.Logger log;
    private final String workspaceUrl;
    private final String accountId;
    private final String accountName;
    private final String configuredBillingWarehouseId;
    private final String workspaceRegion;

    public ClusterCollector(DatabricksClient client,
                            TopologyDataSubmissionService3 topologyService,
                            LogService.Logger log,
                            String workspaceUrl,
                            String accountId,
                            String accountName,
                            String configuredBillingWarehouseId,
                            String workspaceRegion) {
        this.client = client;
        this.topologyService = topologyService;
        this.log = log;
        this.workspaceUrl = workspaceUrl;
        this.accountId = accountId;
        this.accountName = accountName;
        this.configuredBillingWarehouseId = configuredBillingWarehouseId;
        this.workspaceRegion = workspaceRegion;
    }

    public void collect() {
        try {
            System.out.println("=== ClusterCollector collect() ENTER === "
                    + "accountId=" + accountId
                    + ", workspace=" + workspaceUrl);

            long now = System.currentTimeMillis();

            JsonNode clustersResponse = client.listClusters();
            JsonNode jobsResponse = client.listJobs();
            JsonNode warehousesResponse = client.listWarehouses();
            JsonNode pipelinesResponse = client.listPipelines();
            JsonNode poolsResponse = client.listInstancePools();

            TopologyDataSubmissionService3.TopologySubmitter3 submitter =
                    topologyService.getTopologySubmitter();

            System.out.println("ClusterCollector: creating top-level node"
                    + " accountId=" + accountId
                    + " workspace=" + workspaceUrl
                    + " now=" + now);

            TopologyNode root = submitter.createTopLevelNode("DatabricksModelRoot", now);
            root.setId("DatabricksModelRoot");

            // ---------------------------------------------------------------------
            // Direct DatabricksAccount under root
            // ---------------------------------------------------------------------
            TopologyNode accountNode = root.createNode("DatabricksAccount");
            accountNode.setId(accountId);
            setValue(accountNode, "name", accountName, false);
            setValue(accountNode, "accountId", accountId, true);

            // ---------------------------------------------------------------------
            // workspaces -> DatabricksWorkspace
            // ---------------------------------------------------------------------
            TopologyNode workspacesNode = accountNode.createNode("workspaces");
            TopologyNode workspaceNode = workspacesNode.createNode(workspaceUrl);
            workspaceNode.setId(workspaceUrl);
            setValue(workspaceNode, "workspaceUrl", workspaceUrl, true);

            int clusterCount = 0;
            int activeClusterCount = 0;
            int jobCount = 0;
            int runCount = 0;
            int warehouseCount = 0;
            int activeWarehouseCount = 0;
            int pipelineCount = 0;
            int poolCount = 0;
            int usageCount = 0;
            int jobDbuCount = 0;
            String billingWarehouseId = configuredBillingWarehouseId;

            // Determine billing warehouse early so node_timeline query can run before cluster loop
            if ((billingWarehouseId == null || billingWarehouseId.isBlank())
                    && warehousesResponse != null && warehousesResponse.has("warehouses")
                    && warehousesResponse.get("warehouses").isArray()) {
                for (JsonNode wh : warehousesResponse.get("warehouses")) {
                    String whId = wh.path("id").asText("");
                    if (!whId.isBlank()) { billingWarehouseId = whId; break; }
                }
            }

            // Query node_timeline for per-cluster CPU/mem utilization (last 5 minutes)
            java.util.Map<String, double[]> clusterUtils = new java.util.HashMap<>();
            if (billingWarehouseId != null && !billingWarehouseId.isBlank()) {
                try {
                    String utilSql = "SELECT cluster_id, "
                            + "AVG(COALESCE(worker_cpu_util, driver_cpu_util)) AS avg_cpu, "
                            + "AVG(COALESCE(worker_mem_util, driver_mem_util)) AS avg_mem "
                            + "FROM system.compute.node_timeline "
                            + "WHERE timestamp >= CURRENT_TIMESTAMP - INTERVAL 1 HOUR "
                            + "GROUP BY cluster_id "
                            + "LIMIT 500";
                    JsonNode utilResult = client.executeSqlStatement(billingWarehouseId, utilSql);
                    if ("SUCCEEDED".equals(utilResult.path("status").path("state").asText(""))) {
                        JsonNode rows = utilResult.path("result").path("data_array");
                        if (rows.isArray()) {
                            for (JsonNode row : rows) {
                                String cid = row.path(0).asText("");
                                if (!cid.isBlank()) {
                                    clusterUtils.put(cid, new double[]{
                                        row.path(1).asDouble(0.0),
                                        row.path(2).asDouble(0.0)
                                    });
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.log("ClusterCollector: node_timeline query failed: " + e.getMessage());
                }
            }

            // Build poolId -> poolName lookup from pools response
            java.util.Map<String, String> poolIdToName = new java.util.HashMap<>();
            if (poolsResponse != null
                    && poolsResponse.has("instance_pools")
                    && poolsResponse.get("instance_pools").isArray()) {
                for (JsonNode pool : poolsResponse.get("instance_pools")) {
                    String pid = pool.path("instance_pool_id").asText("");
                    String pname = pool.path("instance_pool_name").asText(pid);
                    if (!pid.isBlank()) poolIdToName.put(pid, pname);
                }
            }

            // ---------------------------------------------------------------------
            // clusters -> DatabricksCluster
            // ---------------------------------------------------------------------
            TopologyNode clustersNode = workspaceNode.createNode("clusters");

            if (clustersResponse != null
                    && clustersResponse.has("clusters")
                    && clustersResponse.get("clusters").isArray()) {

                for (JsonNode cluster : clustersResponse.get("clusters")) {
                    String clusterId = cluster.path("cluster_id").asText();
                    if (clusterId == null || clusterId.isBlank()) {
                        continue;
                    }

                    clusterCount++;

                    String clusterName = cluster.path("cluster_name").asText(clusterId);
                    String state = cluster.path("state").asText("UNKNOWN");
                    if ("RUNNING".equals(state)) activeClusterCount++;
                    String clusterSource = cluster.path("cluster_source").asText("");
                    String nodeTypeId = cluster.path("node_type_id").asText("");
                    String driverNodeTypeId = cluster.path("driver_node_type_id").asText("");
                    String sparkVersion = cluster.path("effective_spark_version").asText("");

                    TopologyNode clusterNode = clustersNode.createNode(clusterId);
                    clusterNode.setId(clusterId);

                    setValue(clusterNode, "clusterId", clusterId, true);
                    setValue(clusterNode, "clusterName", clusterName, false);
                    setValue(clusterNode, "state", state, false);
                    setValue(clusterNode, "stateStr", state, false);
                    setValue(clusterNode, "clusterSource", clusterSource, false);
                    setValue(clusterNode, "nodeTypeId", nodeTypeId, false);
                    setValue(clusterNode, "driverNodeTypeId", driverNodeTypeId, false);
                    setValue(clusterNode, "sparkVersion", sparkVersion, false);
                    setValue(clusterNode, "creatorUserName", cluster.path("creator_user_name").asText(""), false);
                    setValue(clusterNode, "customTagsStr", serializeTags(cluster.path("custom_tags")), false);
                    setValue(clusterNode, "pinnedByUserName", cluster.path("pinned_by_user_name").asText(""), false);
                    String poolId = cluster.path("instance_pool_id").asText("");
                    setValue(clusterNode, "instancePoolName", poolId.isBlank() ? "" : poolIdToName.getOrDefault(poolId, poolId), false);
                    setValue(clusterNode, "startTimeStr",        fmtTs(cluster.path("start_time").asLong(0)), false);
                    setValue(clusterNode, "lastActivityTimeStr", fmtTs(cluster.path("last_activity_time").asLong(0)), false);
                    setValue(clusterNode, "terminatedTimeStr",   fmtTs(cluster.path("terminated_time").asLong(0)), false);

                    JsonNode autoscale = cluster.path("autoscale");
                    if (autoscale.isObject()) {
                        clusterNode.createValue("autoscaleEnabled").setSampleValue(true);
                        int minW   = autoscale.path("min_workers").asInt(0);
                        int maxW   = autoscale.path("max_workers").asInt(0);
                        int targetW = autoscale.path("target_workers").asInt(0);
                        clusterNode.createValue("autoscaleMinWorkers").setSampleValue(minW);
                        clusterNode.createValue("autoscaleMaxWorkers").setSampleValue(maxW);
                        clusterNode.createValue("autoscaleTargetWorkers").setSampleValue(targetW);
                        clusterNode.createValue("numWorkers").setSampleValue(targetW > 0 ? targetW : minW);
                    } else {
                        clusterNode.createValue("autoscaleEnabled").setSampleValue(false);
                        clusterNode.createValue("numWorkers")
                                .setSampleValue(cluster.path("num_workers").asInt(0));
                    }

                    clusterNode.createValue("clusterMemoryMb")
                            .setSampleValue(cluster.path("cluster_memory_mb").asLong(0));
                    clusterNode.createValue("clusterCores")
                            .setSampleValue((long) cluster.path("cluster_cores").asDouble(0.0));

                    setValue(clusterNode, "terminationCode",
                            cluster.path("termination_reason").path("code").asText(""), false);
                    setValue(clusterNode, "terminationType",
                            cluster.path("termination_reason").path("type").asText(""), false);
                    clusterNode.createValue("terminationInactivityMinutes")
                            .setSampleValue(cluster.path("autotermination_minutes").asInt(0));

                    double[] utils = clusterUtils.getOrDefault(clusterId, new double[]{-1.0, -1.0});
                    clusterNode.createValue("cpuUtil").setSampleValue(utils[0] >= 0 ? (long)(utils[0] * 100) : 0L);
                    clusterNode.createValue("memUtil").setSampleValue(utils[1] >= 0 ? (long)(utils[1] * 100) : 0L);
                    setValue(clusterNode, "cpuUtilStr", utilBar(utils[0]), false);
                    setValue(clusterNode, "memUtilStr", utilBar(utils[1]), false);
                }
            }

            workspaceNode.createValue("clusterCount").setSampleValue((long) clusterCount);
            workspaceNode.createValue("activeClusterCount").setSampleValue((long) activeClusterCount);
            setValue(workspaceNode, "clusterCountStr", String.valueOf(clusterCount), false);

            // ---------------------------------------------------------------------
            // jobs -> DatabricksJob
            // ---------------------------------------------------------------------
            TopologyNode jobsNode = workspaceNode.createNode("jobs");
            Map<String, TopologyNode> jobNodes = new HashMap<>();

            if (jobsResponse != null
                    && jobsResponse.has("jobs")
                    && jobsResponse.get("jobs").isArray()) {

                for (JsonNode job : jobsResponse.get("jobs")) {
                    String jobId = job.path("job_id").asText();
                    if (jobId == null || jobId.isBlank()) {
                        continue;
                    }

                    jobCount++;

                    String jobName = job.path("settings").path("name").asText(jobId);
                    String creatorUserName = job.path("creator_user_name").asText("");
                    String scheduleCron = job.path("schedule").path("quartz_cron_expression").asText("");
                    String scheduleStatus = job.path("schedule").path("pause_status").asText("");

                    TopologyNode jobNode = jobsNode.createNode(jobId);
                    jobNode.setId(jobId);

                    setValue(jobNode, "jobId", jobId, true);
                    setValue(jobNode, "jobName", jobName, false);
                    setValue(jobNode, "creatorUserName", creatorUserName, false);
                    setValue(jobNode, "scheduleCron", scheduleCron, false);
                    setValue(jobNode, "scheduleStatus", scheduleStatus, false);

                    JsonNode triggerNode = job.path("settings").path("trigger");
                    String jobTriggerType = triggerNode.isMissingNode()
                            ? ""
                            : triggerNode.path("trigger_type").asText(triggerNode.path("pause_status").asText(""));
                    setValue(jobNode, "triggerType", jobTriggerType, false);
                    setValue(jobNode, "tagsStr", serializeTags(job.path("settings").path("tags")), false);

                    // fetch runs for this job
                    try {
                        JsonNode runsResponse = client.listRunsForJob(jobId);
                        if (runsResponse != null
                                && runsResponse.has("runs")
                                && runsResponse.get("runs").isArray()
                                && runsResponse.get("runs").size() > 0) {

                            List<JsonNode> jobRuns = new ArrayList<>();
                            for (JsonNode run : runsResponse.get("runs")) {
                                jobRuns.add(run);
                            }

                            // last run summary (API returns newest-first)
                            JsonNode lastRun = jobRuns.get(0);
                            long lastStart = lastRun.path("start_time").asLong(0L);
                            long lastEnd   = lastRun.path("end_time").asLong(lastStart);
                            setValue(jobNode, "lastRunState",       lastRun.path("state").path("life_cycle_state").asText(""), false);
                            setValue(jobNode, "lastRunResult",      lastRun.path("state").path("result_state").asText(""), false);
                            setValue(jobNode, "lastRunStartStr",    fmtTs(lastStart), false);
                            setValue(jobNode, "lastRunDurationStr", fmtDur(Math.max(0, lastEnd - lastStart)), false);
                            jobNode.createValue("lastRunDuration").setSampleValue(Math.max(0, lastEnd - lastStart));

                            // --- run stats across all fetched runs ---
                            int successCount = 0, failureCount = 0, durationCount = 0;
                            long totalDuration = 0, minDuration = Long.MAX_VALUE, maxDuration = 0;
                            for (JsonNode r : jobRuns) {
                                String lc  = r.path("state").path("life_cycle_state").asText("");
                                String res = r.path("state").path("result_state").asText("");
                                if ("TERMINATED".equals(lc) || "SKIPPED".equals(lc)) {
                                    if ("SUCCESS".equals(res)) successCount++;
                                    else if (!res.isEmpty()) failureCount++;
                                }
                                long rs = r.path("start_time").asLong(0);
                                long re = r.path("end_time").asLong(rs);
                                if (rs > 0 && re > rs) {
                                    long dur = re - rs;
                                    totalDuration += dur;
                                    minDuration = Math.min(minDuration, dur);
                                    maxDuration = Math.max(maxDuration, dur);
                                    durationCount++;
                                }
                            }
                            int rated = successCount + failureCount;
                            setValue(jobNode, "successCountStr", String.valueOf(successCount), false);
                            setValue(jobNode, "failureCountStr", String.valueOf(failureCount), false);
                            setValue(jobNode, "successRateStr",  rated > 0 ? (successCount * 100 / rated) + "%" : "", false);
                            setValue(jobNode, "avgDurationStr",  durationCount > 0 ? fmtDur(totalDuration / durationCount) : "", false);
                            setValue(jobNode, "minDurationStr",  durationCount > 0 ? fmtDur(minDuration) : "", false);
                            setValue(jobNode, "maxDurationStr",  durationCount > 0 ? fmtDur(maxDuration) : "", false);
                            jobNode.createValue("successCount").setSampleValue((long) successCount);
                            jobNode.createValue("failureCount").setSampleValue((long) failureCount);
                            jobNode.createValue("successRate").setSampleValue(rated > 0 ? (long)(successCount * 100 / rated) : 0L);
                            jobNode.createValue("avgDurationMs").setSampleValue(durationCount > 0 ? totalDuration / durationCount : 0L);
                            jobNode.createValue("minDurationMs").setSampleValue(durationCount > 0 && minDuration != Long.MAX_VALUE ? minDuration : 0L);
                            jobNode.createValue("maxDurationMs").setSampleValue(durationCount > 0 ? maxDuration : 0L);

                            TopologyNode runsNode = jobNode.createNode("runs");

                            for (JsonNode run : jobRuns) {
                                String runId = run.path("run_id").asText();
                                if (runId == null || runId.isBlank()) continue;

                                runCount++;

                                String runName = run.path("run_name").asText(runId);
                                String lifeCycleState = run.path("state").path("life_cycle_state").asText("");
                                String resultState = run.path("state").path("result_state").asText("");
                                String runType = run.path("run_type").asText("");
                                String triggerType = run.path("trigger").isTextual()
                                        ? run.path("trigger").asText("")
                                        : run.path("trigger").path("trigger_type").asText("");

                                long startTime    = run.path("start_time").asLong(0L);
                                long endTime      = run.path("end_time").asLong(startTime);
                                long durationMs   = Math.max(0, endTime - startTime);
                                long queueDur     = run.path("queue_duration").asLong(0);
                                long setupDur     = run.path("setup_duration").asLong(0);
                                long executionDur = run.path("execution_duration").asLong(0);
                                long cleanupDur   = run.path("cleanup_duration").asLong(0);
                                int  attemptNum   = run.path("attempt_number").asInt(0);
                                int taskCount = run.path("tasks").isArray()
                                        ? run.path("tasks").size()
                                        : run.path("number_of_tasks").asInt(0);

                                TopologyNode runNode = runsNode.createNode(runId);
                                runNode.setId(runId);

                                setValue(runNode, "runId", runId, true);
                                setValue(runNode, "runName", runName, false);
                                setValue(runNode, "lifeCycleState", lifeCycleState, false);
                                setValue(runNode, "lifeCycleStateStr", lifeCycleState, false);
                                setValue(runNode, "resultState", resultState, false);
                                setValue(runNode, "resultStateStr", resultState, false);
                                setValue(runNode, "runType", runType, false);
                                setValue(runNode, "triggerType", triggerType, false);
                                setValue(runNode, "attemptNumber", String.valueOf(attemptNum), false);
                                setValue(runNode, "isRetry", attemptNum > 0 ? "true" : "false", false);
                                setValue(runNode, "originalAttemptRunId", run.path("original_attempt_run_id").asText(""), false);
                                setValue(runNode, "stateMessage", run.path("state").path("state_message").asText(""), false);

                                runNode.createValue("startTime").setSampleValue(startTime);
                                setValue(runNode, "startTimeStr", fmtTs(startTime), false);
                                runNode.createValue("durationMs").setSampleValue(durationMs);
                                setValue(runNode, "durationMsStr", String.valueOf(durationMs), false);
                                runNode.createValue("queueDuration").setSampleValue(queueDur);
                                setValue(runNode, "queueDurationStr", String.valueOf(queueDur), false);
                                runNode.createValue("setupDuration").setSampleValue(setupDur);
                                setValue(runNode, "setupDurationStr", String.valueOf(setupDur), false);
                                runNode.createValue("executionDuration").setSampleValue(executionDur);
                                setValue(runNode, "executionDurationStr", String.valueOf(executionDur), false);
                                runNode.createValue("cleanupDuration").setSampleValue(cleanupDur);
                                setValue(runNode, "cleanupDurationStr", String.valueOf(cleanupDur), false);
                                runNode.createValue("taskCount").setSampleValue(taskCount);
                                setValue(runNode, "taskCountStr", String.valueOf(taskCount), false);
                            }
                        }
                    } catch (Exception e) {
                        log.log("ClusterCollector: failed to fetch runs for job " + jobId + ": " + e.getMessage());
                        System.out.println("=== runs fetch FAILED for job " + jobId + ": " + e.getMessage());
                    }

                    jobNodes.put(jobId, jobNode);
                }
            }
            workspaceNode.createValue("jobCount").setSampleValue((long) jobCount);

            // ---------------------------------------------------------------------
            // warehouses -> DatabricksWarehouse
            // ---------------------------------------------------------------------
            TopologyNode warehousesNode = workspaceNode.createNode("warehouses");

            int totalQueryCount = 0;
            if (warehousesResponse != null
                    && warehousesResponse.has("warehouses")
                    && warehousesResponse.get("warehouses").isArray()) {

                for (JsonNode wh : warehousesResponse.get("warehouses")) {
                    String whId = wh.path("id").asText();
                    if (whId == null || whId.isBlank()) continue;

                    warehouseCount++;
                    String whState = wh.path("state").asText("");
                    if ("RUNNING".equals(whState) || "STARTING".equals(whState)) activeWarehouseCount++;

                    TopologyNode whNode = warehousesNode.createNode(whId);
                    whNode.setId(whId);

                    setValue(whNode, "warehouseId", whId, true);
                    setValue(whNode, "warehouseName", wh.path("name").asText(whId), false);
                    setValue(whNode, "state", whState, false);
                    setValue(whNode, "stateStr", whState, false);
                    setValue(whNode, "warehouseType", wh.path("warehouse_type").asText(""), false);
                    setValue(whNode, "size", wh.path("cluster_size").asText(""), false);
                    setValue(whNode, "creatorName", wh.path("creator_name").asText(""), false);
                    setValue(whNode, "enablePhoton", String.valueOf(wh.path("enable_photon").asBoolean(false)), false);
                    setValue(whNode, "autoResume", String.valueOf(wh.path("auto_resume").asBoolean(false)), false);
                    if (billingWarehouseId == null || billingWarehouseId.isBlank()) {
                        billingWarehouseId = whId;
                    }

                    long numClusters = wh.path("num_clusters").asLong(0);
                    whNode.createValue("numClusters").setSampleValue(numClusters);
                    setValue(whNode, "numClustersStr", String.valueOf(numClusters), false);
                    whNode.createValue("minClusters").setSampleValue(wh.path("min_num_clusters").asLong(1));
                    whNode.createValue("maxClusters").setSampleValue(wh.path("max_num_clusters").asLong(1));
                    whNode.createValue("autoStopMins").setSampleValue(wh.path("auto_stop_mins").asLong(0));

                    // queries
                    try {
                        JsonNode queriesResponse = client.listQueriesForWarehouse(whId);
                        JsonNode queryList = queriesResponse != null ? queriesResponse.path("res") : null;
                        int queryCount = 0;
                        if (queryList != null && queryList.isArray() && queryList.size() > 0) {
                            TopologyNode queriesNode = whNode.createNode("queries");
                            for (JsonNode q : queryList) {
                                String queryId = q.path("query_id").asText("");
                                if (queryId.isBlank()) continue;
                                queryCount++;
                                TopologyNode qNode = queriesNode.createNode(queryId);
                                qNode.setId(queryId);
                                setValue(qNode, "queryId",       queryId, true);
                                setValue(qNode, "status",        q.path("status").asText(""), false);
                                setValue(qNode, "userName",      q.path("user_name").asText(""), false);
                                setValue(qNode, "statementType", q.path("statement_type").asText(""), false);
                                setValue(qNode, "startedAtStr",  fmtTs(q.path("query_start_time_ms").asLong(0)), false);
                                setValue(qNode, "errorMessage",  q.path("error_message").asText(""), false);
                                String queryText = q.path("query_text").asText("");
                                if (queryText.length() > 500) queryText = queryText.substring(0, 500) + "...";
                                setValue(qNode, "queryText", queryText, false);

                                long dur = q.path("duration").asLong(0);
                                qNode.createValue("duration").setSampleValue(dur);
                                setValue(qNode, "durationStr", fmtDur(dur), false);

                                JsonNode m = q.path("metrics");
                                long bytesRead       = m.path("read_bytes").asLong(0);
                                long rowsProduced    = m.path("rows_produced_count").asLong(0);
                                long compilationTime = m.path("compilation_time_ms").asLong(0);
                                long executionTime   = m.path("execution_time_ms").asLong(0);
                                long fetchTime       = m.path("result_fetch_time_ms").asLong(0);
                                boolean fromCache    = m.path("result_from_cache").asBoolean(false);

                                qNode.createValue("bytesRead").setSampleValue(bytesRead);
                                setValue(qNode, "bytesReadStr", bytesRead > 0 ? String.valueOf(bytesRead) : "", false);
                                qNode.createValue("rowsProduced").setSampleValue(rowsProduced);
                                setValue(qNode, "rowsProducedStr", rowsProduced > 0 ? String.valueOf(rowsProduced) : "", false);
                                qNode.createValue("compilationTime").setSampleValue(compilationTime);
                                setValue(qNode, "compilationTimeStr", fmtDur(compilationTime), false);
                                qNode.createValue("executionTime").setSampleValue(executionTime);
                                setValue(qNode, "executionTimeStr", fmtDur(executionTime), false);
                                qNode.createValue("fetchTime").setSampleValue(fetchTime);
                                setValue(qNode, "fetchTimeStr", fmtDur(fetchTime), false);
                                setValue(qNode, "fromResultCache", fromCache ? "true" : "", false);
                            }
                        }
                        whNode.createValue("queryCount").setSampleValue((long) queryCount);
                        setValue(whNode, "queryCountStr", String.valueOf(queryCount), false);
                        totalQueryCount += queryCount;
                    } catch (Exception e) {
                        log.log("ClusterCollector: failed to fetch queries for warehouse " + whId + ": " + e.getMessage());
                    }
                }
            }

            workspaceNode.createValue("warehouseCount").setSampleValue((long) warehouseCount);
            workspaceNode.createValue("activeWarehouseCount").setSampleValue((long) activeWarehouseCount);
            workspaceNode.createValue("totalQueryCount").setSampleValue((long) totalQueryCount);

            // ---------------------------------------------------------------------
            // pipelines -> DatabricksPipeline
            // ---------------------------------------------------------------------
            TopologyNode pipelinesNode = workspaceNode.createNode("pipelines");

            // Pre-query pipeline dollar cost from billing (requires billingWarehouseId)
            java.util.Map<String, Double> pipelineDollarCost = new java.util.HashMap<>();
            if (billingWarehouseId != null && !billingWarehouseId.isBlank()) {
                try {
                    String plCostSql = "SELECT u.usage_metadata.pipeline_id, "
                            + "CAST(SUM(u.usage_quantity * COALESCE(lp.pricing.effective_list.default, 0)) AS DOUBLE) AS dollar_cost "
                            + "FROM system.billing.usage u "
                            + "LEFT JOIN system.billing.list_prices lp "
                            + "  ON lp.sku_name = u.sku_name "
                            + "  AND u.usage_end_time >= lp.price_start_time "
                            + "  AND (lp.price_end_time IS NULL OR u.usage_end_time < lp.price_end_time) "
                            + "WHERE u.usage_metadata.pipeline_id IS NOT NULL "
                            + "  AND u.usage_date >= DATE_ADD(CURRENT_DATE, -30) "
                            + "GROUP BY u.usage_metadata.pipeline_id "
                            + "ORDER BY dollar_cost DESC "
                            + "LIMIT 200";
                    JsonNode plCostResult = client.executeSqlStatement(billingWarehouseId, plCostSql);
                    if ("SUCCEEDED".equals(plCostResult.path("status").path("state").asText(""))) {
                        JsonNode plCostData = plCostResult.path("result").path("data_array");
                        if (plCostData.isArray()) {
                            for (JsonNode row : plCostData) {
                                String pid = row.path(0).asText("");
                                if (!pid.isBlank()) pipelineDollarCost.put(pid, row.path(1).asDouble(0.0));
                            }
                        }
                    }
                } catch (Exception e) {
                    log.log("ClusterCollector: pipeline cost query failed: " + e.getMessage());
                }
            }

            if (pipelinesResponse != null
                    && pipelinesResponse.has("statuses")
                    && pipelinesResponse.get("statuses").isArray()) {

                for (JsonNode pl : pipelinesResponse.get("statuses")) {
                    String plId = pl.path("pipeline_id").asText();
                    if (plId == null || plId.isBlank()) continue;

                    pipelineCount++;

                    TopologyNode plNode = pipelinesNode.createNode(plId);
                    plNode.setId(plId);

                    setValue(plNode, "pipelineId", plId, true);
                    setValue(plNode, "pipelineName", pl.path("name").asText(plId), false);
                    setValue(plNode, "state", pl.path("state").asText(""), false);
                    setValue(plNode, "stateStr", pl.path("state").asText(""), false);
                    setValue(plNode, "creatorUserName", pl.path("creator_user_name").asText(""), false);
                    setValue(plNode, "runAsUserName", pl.path("run_as_user_name").asText(""), false);
                    double plCost = pipelineDollarCost.getOrDefault(plId, 0.0);
                    setValue(plNode, "dollarCostStr",
                            plCost > 0 ? String.format("$%,.2f",plCost) : "", false);

                    // Fetch update history + expectations (last 5 updates)
                    try {
                        JsonNode eventsResp = client.getPipelineEvents(plId);
                        JsonNode events = eventsResp.path("events");
                        if (!events.isArray()) continue;

                        TopologyNode updatesNode = plNode.createNode("updates");

                        // Track updates seen — cap at 5
                        java.util.LinkedHashMap<String, TopologyNode> updateNodes = new java.util.LinkedHashMap<>();
                        // Expectations keyed by updateId -> list
                        java.util.Map<String, java.util.List<JsonNode>> expectationsByUpdate = new java.util.LinkedHashMap<>();

                        for (JsonNode ev : events) {
                            String evType = ev.path("event_type").asText("");

                            if ("update_progress".equals(evType)) {
                                String updateId = ev.path("origin").path("update_id").asText("");
                                if (updateId.isBlank() || updateNodes.containsKey(updateId)) continue;
                                if (updateNodes.size() >= 5) continue;

                                String state     = ev.path("details").path("update_progress").path("state").asText("");
                                String timestamp = ev.path("timestamp").asText("");
                                String cause     = ev.path("origin").path("request_id").asText("");

                                // Format timestamp
                                String startTime = timestamp;
                                if (!timestamp.isBlank()) {
                                    try {
                                        long epochMs = java.time.Instant.parse(timestamp).toEpochMilli();
                                        startTime = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                                                .format(new java.util.Date(epochMs));
                                    } catch (Exception ignore) {}
                                }

                                TopologyNode upNode = updatesNode.createNode(updateId);
                                upNode.setId(updateId);
                                setValue(upNode, "updateId",    updateId,  true);
                                setValue(upNode, "state",       state,     false);
                                setValue(upNode, "startTime",   startTime, false);
                                setValue(upNode, "durationStr", "",        false);
                                setValue(upNode, "cause",       cause,     false);
                                updateNodes.put(updateId, upNode);
                                expectationsByUpdate.put(updateId, new java.util.ArrayList<>());
                            }
                        }

                        // Second pass — flow_progress events for expectations
                        for (JsonNode ev : events) {
                            if (!"flow_progress".equals(ev.path("event_type").asText(""))) continue;
                            String updateId = ev.path("origin").path("update_id").asText("");
                            if (!updateNodes.containsKey(updateId)) continue;

                            String flowName = ev.path("origin").path("flow_name").asText("");
                            JsonNode dq = ev.path("details").path("flow_progress").path("data_quality");
                            if (!dq.has("expectations")) continue;

                            for (JsonNode exp : dq.path("expectations")) {
                                String expName  = exp.path("name").asText("");
                                String dataset  = exp.path("dataset").asText("");
                                long passed  = exp.path("passed_records").asLong(0);
                                long failed  = exp.path("failed_records").asLong(0);
                                long dropped = exp.path("dropped_records").asLong(0);
                                long total   = passed + failed;
                                String passRate = total > 0
                                        ? String.format("%.1f%%", 100.0 * passed / total)
                                        : "N/A";

                                String expKey = updateId + "|" + flowName + "|" + expName;
                                TopologyNode upNode = updateNodes.get(updateId);
                                TopologyNode expsNode = upNode.createNode("expectations");
                                TopologyNode expNode  = expsNode.createNode(expKey);
                                expNode.setId(expKey);
                                setValue(expNode, "expectationKey",  expKey,                    true);
                                setValue(expNode, "updateId",        updateId,                  false);
                                setValue(expNode, "flowName",        flowName,                  false);
                                setValue(expNode, "expectationName", expName,                   false);
                                setValue(expNode, "dataset",         dataset,                   false);
                                setValue(expNode, "passedRecords",   String.valueOf(passed),    false);
                                setValue(expNode, "failedRecords",   String.valueOf(failed),    false);
                                setValue(expNode, "droppedRecords",  String.valueOf(dropped),   false);
                                setValue(expNode, "passRate",        passRate,                  false);
                                expNode.createValue("passedCount").setSampleValue(passed);
                                expNode.createValue("failedCount").setSampleValue(failed);
                                expNode.createValue("droppedCount").setSampleValue(dropped);
                                expNode.createValue("passRatePct").setSampleValue(total > 0 ? (long)(100.0 * passed / total) : 0L);
                            }
                        }
                    } catch (Exception e) {
                        log.log("ClusterCollector: pipeline events fetch failed for " + plId + ": " + e.getMessage());
                    }
                }
            }

            workspaceNode.createValue("pipelineCount").setSampleValue((long) pipelineCount);

            // ---------------------------------------------------------------------
            // instancePools -> DatabricksInstancePool
            // ---------------------------------------------------------------------
            TopologyNode poolsNode = workspaceNode.createNode("instancePools");

            if (poolsResponse != null
                    && poolsResponse.has("instance_pools")
                    && poolsResponse.get("instance_pools").isArray()) {

                for (JsonNode pool : poolsResponse.get("instance_pools")) {
                    String poolId = pool.path("instance_pool_id").asText();
                    if (poolId == null || poolId.isBlank()) continue;

                    poolCount++;

                    TopologyNode poolNode = poolsNode.createNode(poolId);
                    poolNode.setId(poolId);

                    setValue(poolNode, "instancePoolId", poolId, true);
                    setValue(poolNode, "instancePoolName", pool.path("instance_pool_name").asText(poolId), false);
                    setValue(poolNode, "state", pool.path("state").asText(""), false);
                    setValue(poolNode, "stateStr", pool.path("state").asText(""), false);
                    setValue(poolNode, "nodeTypeId", pool.path("node_type_id").asText(""), false);
                    poolNode.createValue("minIdleInstances").setSampleValue(pool.path("min_idle_instances").asLong(0));
                    poolNode.createValue("maxCapacity").setSampleValue(pool.path("max_capacity").asLong(0));
                    poolNode.createValue("idleCount").setSampleValue(pool.path("stats").path("idle_count").asLong(0));
                    poolNode.createValue("usedCount").setSampleValue(pool.path("stats").path("used_count").asLong(0));
                    poolNode.createValue("pendingIdleCount").setSampleValue(pool.path("stats").path("pending_idle_count").asLong(0));
                    poolNode.createValue("pendingUsedCount").setSampleValue(pool.path("stats").path("pending_used_count").asLong(0));
                    poolNode.createValue("idleTerminationMinutes").setSampleValue(pool.path("idle_instance_autotermination_minutes").asLong(0));
                    JsonNode sparkVersions = pool.path("preloaded_spark_versions");
                    String versionsStr = "";
                    if (sparkVersions.isArray()) {
                        java.util.List<String> vlist = new java.util.ArrayList<>();
                        for (JsonNode v : sparkVersions) vlist.add(v.asText(""));
                        versionsStr = String.join(", ", vlist);
                    }
                    setValue(poolNode, "preloadedSparkVersions", versionsStr, false);
                }
            }

            // ---------------------------------------------------------------------
            // servingEndpoints -> DatabricksServingEndpoint + DatabricksServedModel
            // ---------------------------------------------------------------------
            TopologyNode endpointsNode = workspaceNode.createNode("servingEndpoints");
            int endpointCount = 0;
            try {
                JsonNode epResponse = client.listServingEndpoints();
                JsonNode endpoints = epResponse.path("endpoints");
                if (endpoints.isArray()) {
                    for (JsonNode ep : endpoints) {
                        String epName = ep.path("name").asText("");
                        if (epName.isBlank()) continue;
                        endpointCount++;

                        String readyState        = ep.path("state").path("ready").asText("");
                        String configUpdateState = ep.path("state").path("config_update").asText("");
                        String creator           = ep.path("creator").asText("");
                        String routeOptimized    = String.valueOf(ep.path("route_optimized").asBoolean(false));

                        String creationTime  = fmtTs(ep.path("creation_timestamp").asLong(0));
                        String lastUpdated   = fmtTs(ep.path("last_updated_timestamp").asLong(0));

                        TopologyNode epNode = endpointsNode.createNode(epName);
                        epNode.setId(epName);
                        setValue(epNode, "endpointName",      epName,            true);
                        setValue(epNode, "creator",           creator,           false);
                        setValue(epNode, "readyState",        readyState,        false);
                        setValue(epNode, "configUpdateState", configUpdateState, false);
                        setValue(epNode, "creationTime",      creationTime,      false);
                        setValue(epNode, "lastUpdatedTime",   lastUpdated,       false);
                        setValue(epNode, "routeOptimized",    routeOptimized,    false);

                        // Served models — check both served_models and served_entities (newer API)
                        TopologyNode servedModelsNode = epNode.createNode("servedModels");
                        JsonNode servedModels = ep.path("config").path("served_models");
                        if (!servedModels.isArray() || servedModels.size() == 0)
                            servedModels = ep.path("config").path("served_entities");

                        if (servedModels.isArray()) {
                            for (JsonNode sm : servedModels) {
                                String smName      = sm.path("name").asText("");
                                String modelName   = sm.path("model_name").asText(sm.path("entity_name").asText(""));
                                String modelVer    = sm.path("model_version").asText("");
                                String workload    = sm.path("workload_size").asText("");
                                String scaleToZero = String.valueOf(sm.path("scale_to_zero_enabled").asBoolean(false));
                                String traffic     = String.valueOf(sm.path("traffic_percentage").asInt(0)) + "%";
                                String depState    = sm.path("state").path("deployment").asText("");

                                String smKey = epName + "|" + smName;
                                TopologyNode smNode = servedModelsNode.createNode(smKey);
                                smNode.setId(smKey);
                                setValue(smNode, "servedModelKey",    smKey,      true);
                                setValue(smNode, "endpointName",      epName,     false);
                                setValue(smNode, "servedModelName",   smName,     false);
                                setValue(smNode, "modelName",         modelName,  false);
                                setValue(smNode, "modelVersion",      modelVer,   false);
                                setValue(smNode, "workloadSize",      workload,   false);
                                setValue(smNode, "scaleToZero",       scaleToZero, false);
                                setValue(smNode, "trafficPercentage", traffic,    false);
                                setValue(smNode, "deploymentState",   depState,   false);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.log("ClusterCollector: serving endpoint collection failed: " + e.getMessage());
            }

            // ---------------------------------------------------------------------
            // lakebaseProjects -> DatabricksLakebaseProject + DatabricksLakebaseBranch
            // ---------------------------------------------------------------------
            TopologyNode lakebaseProjectsNode = workspaceNode.createNode("lakebaseProjects");
            try {
                JsonNode projResponse = client.listLakebaseProjects();
                JsonNode projects = projResponse.path("projects");
                if (projects.isArray()) {
                    for (JsonNode proj : projects) {
                        String projectId   = proj.path("status").path("project_id")
                                                     .asText(bareId(proj.path("name").asText("")));
                        String displayName = proj.path("status").path("display_name").asText(projectId);
                        if (projectId.isBlank()) continue;

                        TopologyNode projNode = lakebaseProjectsNode.createNode(projectId);
                        projNode.setId(projectId);
                        setValue(projNode, "projectId",   projectId,   true);
                        setValue(projNode, "displayName", displayName, false);

                        TopologyNode branchesNode = projNode.createNode("branches");
                        int branchCount = 0;
                        try {
                            JsonNode branchResp = client.listLakebaseBranches(projectId);
                            JsonNode branches = branchResp.path("branches");
                            if (branches.isArray()) {
                                for (JsonNode branch : branches) {
                                    String branchId    = branch.path("status").path("branch_id")
                                                             .asText(bareId(branch.path("name").asText("")));
                                    String branchName  = branch.path("status").path("display_name").asText(branchId);
                                    if (branchId.isBlank()) continue;
                                    branchCount++;

                                    String endpointHost  = "";
                                    String endpointState = "";
                                    try {
                                        JsonNode epResp = client.listLakebaseEndpoints(projectId, branchId);
                                        JsonNode eps = epResp.path("endpoints");
                                        if (eps.isArray() && eps.size() > 0) {
                                            JsonNode ep = eps.get(0);
                                            endpointHost  = ep.path("status").path("hosts").path("host").asText("");
                                            endpointState = ep.path("status").path("state").asText("");
                                        }
                                    } catch (Exception ignored) {}

                                    String branchKey = projectId + "|" + branchId;
                                    TopologyNode branchNode = branchesNode.createNode(branchKey);
                                    branchNode.setId(branchKey);
                                    setValue(branchNode, "branchKey",     branchKey,     true);
                                    setValue(branchNode, "projectId",     projectId,     false);
                                    setValue(branchNode, "branchId",      branchId,      false);
                                    setValue(branchNode, "displayName",   branchName,    false);
                                    setValue(branchNode, "endpointHost",  endpointHost,  false);
                                    setValue(branchNode, "endpointState", endpointState, false);
                                }
                            }
                        } catch (Exception e) {
                            log.log("ClusterCollector: lakebase branch list failed for " + projectId + ": " + e.getMessage());
                        }
                        projNode.createValue("branchCount").setSampleValue(branchCount);
                    }
                }
            } catch (Exception e) {
                log.log("ClusterCollector: lakebase project collection failed: " + e.getMessage());
            }

            // ---------------------------------------------------------------------
            // usages -> DatabricksUsage (system.billing.usage via SQL warehouse)
            // ---------------------------------------------------------------------
            TopologyNode usagesNode = workspaceNode.createNode("usages");

            if (billingWarehouseId != null && !billingWarehouseId.isBlank()) {
                try {
                    String todayStr = java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString();
                    double todayDbuTotal = 0.0;
                    String sql = "SELECT u.usage_date, u.sku_name, u.cloud, u.billing_origin_product, "
                            + "CAST(SUM(u.usage_quantity) AS DOUBLE) AS dbu_total, "
                            + "CAST(SUM(u.usage_quantity * COALESCE(lp.pricing.effective_list.default, 0)) AS DOUBLE) AS dollar_cost "
                            + "FROM system.billing.usage u "
                            + "LEFT JOIN system.billing.list_prices lp "
                            + "  ON lp.sku_name = u.sku_name "
                            + "  AND u.usage_end_time >= lp.price_start_time "
                            + "  AND (lp.price_end_time IS NULL OR u.usage_end_time < lp.price_end_time) "
                            + "WHERE u.usage_date >= DATE_ADD(CURRENT_DATE, -60) "
                            + "GROUP BY u.usage_date, u.sku_name, u.cloud, u.billing_origin_product "
                            + "ORDER BY u.usage_date DESC, dbu_total DESC "
                            + "LIMIT 1000";

                    JsonNode stmtResult = client.executeSqlStatement(billingWarehouseId, sql);
                    String stmtState = stmtResult.path("status").path("state").asText("");

                    if ("SUCCEEDED".equals(stmtState)) {
                        JsonNode dataArray = stmtResult.path("result").path("data_array");
                        if (dataArray.isArray()) {
                            for (JsonNode row : dataArray) {
                                String usageDate      = row.path(0).asText("");
                                String sku            = row.path(1).asText("");
                                String cloud          = row.path(2).asText("");
                                String billingProduct = row.path(3).asText("");
                                double dbu            = row.path(4).asDouble(0.0);
                                double cost           = row.path(5).asDouble(0.0);

                                String usageKey = usageDate + "|" + sku + "|" + billingProduct + "|" + cloud;
                                usageCount++;

                                TopologyNode usageNode = usagesNode.createNode(usageKey);
                                usageNode.setId(usageKey);

                                setValue(usageNode, "usageKey",             usageKey,        true);
                                setValue(usageNode, "usageDate",            usageDate,       false);
                                setValue(usageNode, "sku",                  sku,             false);
                                setValue(usageNode, "billingOriginProduct", billingProduct,  false);
                                setValue(usageNode, "cloud",                cloud,           false);
                                setValue(usageNode, "region",               workspaceRegion, false);
                                usageNode.createValue("dbuConsumed").setSampleValue((long)(dbu * 1000));
                                setValue(usageNode, "dbuConsumedStr",  String.format("%.2f", dbu),  false);
                                setValue(usageNode, "dollarCostStr",   String.format("$%,.2f", cost), false);
                                if (usageDate.equals(todayStr)) todayDbuTotal += dbu;
                            }
                        }
                        workspaceNode.createValue("totalDailyDbu").setSampleValue((long)(todayDbuTotal * 1000));
                    } else {
                        log.log("ClusterCollector: billing query did not succeed, state=" + stmtState
                                + ", error=" + stmtResult.path("status").path("error").path("message").asText(""));
                    }
                } catch (Exception e) {
                    log.log("ClusterCollector: billing collection failed: " + e.getMessage());
                    System.out.println("=== billing collection FAILED: " + e.getMessage());
                }

                // -----------------------------------------------------------------
                // jobDbus -> DatabricksJobDbu (usage_metadata.job_id)
                // -----------------------------------------------------------------
                TopologyNode jobDbusNode = workspaceNode.createNode("jobDbus");
                try {
                    String jobSql = "SELECT u.usage_metadata.job_id, "
                            + "CAST(SUM(u.usage_quantity) AS DOUBLE) AS dbu_total, "
                            + "CAST(SUM(u.usage_quantity * COALESCE(lp.pricing.effective_list.default, 0)) AS DOUBLE) AS dollar_cost "
                            + "FROM system.billing.usage u "
                            + "LEFT JOIN system.billing.list_prices lp "
                            + "  ON lp.sku_name = u.sku_name "
                            + "  AND u.usage_end_time >= lp.price_start_time "
                            + "  AND (lp.price_end_time IS NULL OR u.usage_end_time < lp.price_end_time) "
                            + "WHERE u.usage_metadata.job_id IS NOT NULL "
                            + "AND u.usage_date >= DATE_ADD(CURRENT_DATE, -30) "
                            + "GROUP BY u.usage_metadata.job_id "
                            + "ORDER BY dbu_total DESC "
                            + "LIMIT 100";

                    JsonNode jobResult = client.executeSqlStatement(billingWarehouseId, jobSql);
                    String jobState = jobResult.path("status").path("state").asText("");

                    if ("SUCCEEDED".equals(jobState)) {
                        JsonNode jobData = jobResult.path("result").path("data_array");
                        if (jobData.isArray()) {
                            for (JsonNode row : jobData) {
                                String jobId = row.path(0).asText("");
                                if (jobId.isBlank()) continue;
                                double dbu  = row.path(1).asDouble(0.0);
                                double cost = row.path(2).asDouble(0.0);

                                jobDbuCount++;
                                TopologyNode jobDbuNode = jobDbusNode.createNode(jobId);
                                jobDbuNode.setId(jobId);
                                setValue(jobDbuNode, "jobId", jobId, true);
                                jobDbuNode.createValue("dbuConsumed").setSampleValue((long)(dbu * 1000));
                                setValue(jobDbuNode, "dbuConsumedStr", String.format("%.2f", dbu), false);
                                setValue(jobDbuNode, "dollarCostStr",
                                        cost > 0 ? String.format("$%,.2f",cost) : "", false);
                            }
                        }
                    } else {
                        log.log("ClusterCollector: job DBU query did not succeed, state=" + jobState
                                + ", error=" + jobResult.path("status").path("error").path("message").asText(""));
                    }
                } catch (Exception e) {
                    log.log("ClusterCollector: job DBU collection failed: " + e.getMessage());
                }
                // skuPrices -> DatabricksSkuPrice (system.billing.list_prices, current only)
                // -----------------------------------------------------------------
                TopologyNode skuPricesNode = workspaceNode.createNode("skuPrices");
                try {
                    String priceSql = "SELECT sku_name, cloud, currency_code, "
                            + "pricing.effective_list.default AS list_price_per_dbu, "
                            + "CAST(price_start_time AS STRING) AS price_start_time "
                            + "FROM system.billing.list_prices "
                            + "WHERE price_end_time IS NULL "
                            + "ORDER BY sku_name, cloud";

                    JsonNode priceResult = client.executeSqlStatement(billingWarehouseId, priceSql);
                    String priceState = priceResult.path("status").path("state").asText("");

                    if ("SUCCEEDED".equals(priceState)) {
                        JsonNode priceData = priceResult.path("result").path("data_array");
                        if (priceData.isArray()) {
                            for (JsonNode row : priceData) {
                                String skuName       = row.path(0).asText("");
                                String cloud         = row.path(1).asText("");
                                String currencyCode  = row.path(2).asText("USD");
                                double listPrice     = row.path(3).asDouble(0.0);
                                String startTime     = row.path(4).asText("");

                                String priceKey = skuName + "|" + cloud;
                                TopologyNode priceNode = skuPricesNode.createNode(priceKey);
                                priceNode.setId(priceKey);
                                setValue(priceNode, "skuName",         skuName,                          true);
                                setValue(priceNode, "cloud",           cloud,                            false);
                                setValue(priceNode, "currencyCode",    currencyCode,                     false);
                                setValue(priceNode, "listPricePerDbu", String.format("%.4f", listPrice), false);
                                setValue(priceNode, "priceStartTime",  startTime,                        false);
                            }
                        }
                    } else {
                        log.log("ClusterCollector: SKU price query did not succeed, state=" + priceState
                                + ", error=" + priceResult.path("status").path("error").path("message").asText(""));
                    }
                } catch (Exception e) {
                    log.log("ClusterCollector: SKU price collection failed: " + e.getMessage());
                }

                // userSpends -> DatabricksUserSpend (spend by run_as user + product, last 30 days)
                // -----------------------------------------------------------------
                TopologyNode userSpendsNode = workspaceNode.createNode("userSpends");
                try {
                    String userSpendSql = "SELECT u.identity_metadata.run_as, "
                            + "u.billing_origin_product, "
                            + "CAST(SUM(u.usage_quantity) AS DOUBLE) AS dbu_total, "
                            + "CAST(SUM(u.usage_quantity * COALESCE(lp.pricing.effective_list.default, 0)) AS DOUBLE) AS dollar_cost "
                            + "FROM system.billing.usage u "
                            + "LEFT JOIN system.billing.list_prices lp "
                            + "  ON lp.sku_name = u.sku_name "
                            + "  AND u.usage_end_time >= lp.price_start_time "
                            + "  AND (lp.price_end_time IS NULL OR u.usage_end_time < lp.price_end_time) "
                            + "WHERE u.identity_metadata.run_as IS NOT NULL "
                            + "  AND u.usage_date >= DATE_ADD(CURRENT_DATE, -30) "
                            + "GROUP BY u.identity_metadata.run_as, u.billing_origin_product "
                            + "ORDER BY dollar_cost DESC "
                            + "LIMIT 500";

                    JsonNode usResult = client.executeSqlStatement(billingWarehouseId, userSpendSql);
                    if ("SUCCEEDED".equals(usResult.path("status").path("state").asText(""))) {
                        JsonNode usData = usResult.path("result").path("data_array");
                        if (usData.isArray()) {
                            for (JsonNode row : usData) {
                                String user    = row.path(0).asText("");
                                String product = row.path(1).asText("");
                                if (user.isBlank()) continue;
                                double dbu  = row.path(2).asDouble(0.0);
                                double cost = row.path(3).asDouble(0.0);
                                String key  = user + "|" + product;

                                TopologyNode usNode = userSpendsNode.createNode(key);
                                usNode.setId(key);
                                setValue(usNode, "userSpendKey",   key,                               true);
                                setValue(usNode, "runAsUser",      user,                              false);
                                setValue(usNode, "billingProduct", product,                           false);
                                setValue(usNode, "dbuConsumedStr", String.format("%.2f", dbu),        false);
                                setValue(usNode, "dollarCostStr",
                                        cost > 0 ? String.format("$%,.2f", cost) : "",               false);
                                usNode.createValue("dbuConsumed").setSampleValue((long)(dbu * 1000));
                            }
                        }
                    } else {
                        log.log("ClusterCollector: user spend query state="
                                + usResult.path("status").path("state").asText("")
                                + ", error=" + usResult.path("status").path("error").path("message").asText(""));
                    }
                } catch (Exception e) {
                    log.log("ClusterCollector: user spend collection failed: " + e.getMessage());
                }

                // optimizationOps -> DatabricksOptimizationOp (predictive optimization history, last 7 days)
                // -----------------------------------------------------------------
                TopologyNode optimizationOpsNode = workspaceNode.createNode("optimizationOps");
                try {
                    String optSql = "SELECT operation_id, catalog_name, schema_name, table_name, "
                            + "operation_type, operation_status, "
                            + "DATE_FORMAT(start_time, 'yyyy-MM-dd HH:mm') AS start_fmt, "
                            + "CAST(usage_quantity AS DOUBLE) AS qty, usage_unit "
                            + "FROM system.storage.predictive_optimization_operations_history "
                            + "WHERE start_time >= CURRENT_TIMESTAMP - INTERVAL 7 DAYS "
                            + "ORDER BY start_time DESC "
                            + "LIMIT 500";

                    JsonNode optResult = client.executeSqlStatement(billingWarehouseId, optSql);
                    if ("SUCCEEDED".equals(optResult.path("status").path("state").asText(""))) {
                        JsonNode optData = optResult.path("result").path("data_array");
                        if (optData.isArray()) {
                            for (JsonNode row : optData) {
                                String opId     = row.path(0).asText("");
                                String catalog  = row.path(1).asText("");
                                String schema   = row.path(2).asText("");
                                String table    = row.path(3).asText("");
                                String opType   = row.path(4).asText("");
                                String opStatus = row.path(5).asText("");
                                String start    = row.path(6).asText("");
                                double qty      = row.path(7).asDouble(0.0);
                                String unit     = row.path(8).asText("");
                                String usage    = qty > 0 ? String.format("%.3f %s", qty, unit) : "";
                                if (opId.isBlank()) continue;

                                TopologyNode opNode = optimizationOpsNode.createNode(opId);
                                opNode.setId(opId);
                                setValue(opNode, "operationId",     opId,     true);
                                setValue(opNode, "tableCatalog",    catalog,  false);
                                setValue(opNode, "tableSchema",     schema,   false);
                                setValue(opNode, "tableName",       table,    false);
                                setValue(opNode, "operationType",   opType,   false);
                                setValue(opNode, "operationStatus", opStatus, false);
                                setValue(opNode, "startTime",       start,    false);
                                setValue(opNode, "usageStr",        usage,    false);
                            }
                        }
                    } else {
                        log.log("ClusterCollector: optimization ops query state="
                                + optResult.path("status").path("state").asText("")
                                + ", error=" + optResult.path("status").path("error").path("message").asText(""));
                    }
                } catch (Exception e) {
                    log.log("ClusterCollector: optimization ops collection failed: " + e.getMessage());
                }

                // storageCosts -> DatabricksStorageCost (storage spend from billing, last 30 days)
                // -----------------------------------------------------------------
                TopologyNode storageCostsNode = workspaceNode.createNode("storageCosts");
                try {
                    String storageCostSql = "SELECT u.billing_origin_product, u.sku_name, u.usage_unit, "
                            + "CAST(SUM(u.usage_quantity) AS DOUBLE) AS total_usage, "
                            + "CAST(SUM(u.usage_quantity * COALESCE(lp.pricing.effective_list.default, 0)) AS DOUBLE) AS dollar_cost "
                            + "FROM system.billing.usage u "
                            + "LEFT JOIN system.billing.list_prices lp "
                            + "  ON lp.sku_name = u.sku_name "
                            + "  AND u.usage_end_time >= lp.price_start_time "
                            + "  AND (lp.price_end_time IS NULL OR u.usage_end_time < lp.price_end_time) "
                            + "WHERE u.usage_type = 'STORAGE_SPACE' "
                            + "  AND u.usage_date >= DATE_ADD(CURRENT_DATE, -30) "
                            + "GROUP BY u.billing_origin_product, u.sku_name, u.usage_unit "
                            + "ORDER BY dollar_cost DESC "
                            + "LIMIT 200";

                    JsonNode scResult = client.executeSqlStatement(billingWarehouseId, storageCostSql);
                    if ("SUCCEEDED".equals(scResult.path("status").path("state").asText(""))) {
                        JsonNode scData = scResult.path("result").path("data_array");
                        if (scData.isArray()) {
                            for (JsonNode row : scData) {
                                String product = row.path(0).asText("");
                                String sku     = row.path(1).asText("");
                                String unit    = row.path(2).asText("");
                                double usage   = row.path(3).asDouble(0.0);
                                double cost    = row.path(4).asDouble(0.0);
                                String key     = product + "|" + sku;

                                TopologyNode scNode = storageCostsNode.createNode(key);
                                scNode.setId(key);
                                setValue(scNode, "storageCostKey", key,                                  true);
                                setValue(scNode, "storageProduct", product,                              false);
                                setValue(scNode, "skuName",        sku,                                  false);
                                setValue(scNode, "usageUnit",      unit,                                 false);
                                setValue(scNode, "usageStr",       String.format("%.4f %s", usage, unit), false);
                                setValue(scNode, "dollarCostStr",
                                        cost > 0 ? String.format("$%,.2f", cost) : "",                  false);
                            }
                        }
                    } else {
                        log.log("ClusterCollector: storage cost query state="
                                + scResult.path("status").path("state").asText("")
                                + ", error=" + scResult.path("status").path("error").path("message").asText(""));
                    }
                } catch (Exception e) {
                    log.log("ClusterCollector: storage cost collection failed: " + e.getMessage());
                }

            } else {
                log.log("ClusterCollector: no billing warehouse configured, skipping billing collection");
            }

            // ---------------------------------------------------------------------
            // Lakehouse Monitoring — pure SQL via _profile_metrics tables
            // Avoids REST API permission issues; agent token only needs SQL warehouse access
            // ---------------------------------------------------------------------
            TopologyNode monitorsNode = workspaceNode.createNode("monitors");
            if (billingWarehouseId != null && !billingWarehouseId.isBlank()) {
            try {
                // Step 1: discover both _profile_metrics and _drift_metrics tables
                String discoverSql = "SELECT table_catalog, table_schema, table_name "
                        + "FROM system.information_schema.tables "
                        + "WHERE (table_name LIKE '%_profile_metrics' OR table_name LIKE '%_drift_metrics') "
                        + "  AND LEFT(table_schema, 2) <> '__' "
                        + "ORDER BY table_catalog, table_schema, table_name LIMIT 400";
                JsonNode discoverResult = client.executeSqlStatement(billingWarehouseId, discoverSql);
                String discoverState = discoverResult.path("status").path("state").asText("");
                System.out.println("ClusterCollector: monitor discovery state=" + discoverState);
                if (!"SUCCEEDED".equals(discoverState)) {
                    System.out.println("ClusterCollector: monitor discovery query state="
                            + discoverState
                            + ", error=" + discoverResult.path("status").path("error").path("message").asText(""));
                } else {
                    JsonNode discoverRows = discoverResult.path("result").path("data_array");
                    System.out.println("ClusterCollector: monitor discovery rows isArray=" + discoverRows.isArray()
                            + " size=" + (discoverRows.isArray() ? discoverRows.size() : "n/a"));
                    if (discoverRows.isArray() && discoverRows.size() > 0) {
                        // Split into profile and drift table sets
                        java.util.Set<String> driftKeys = new java.util.HashSet<>();
                        java.util.List<String[]> profileTables = new java.util.ArrayList<>();
                        for (JsonNode dr : discoverRows) {
                            String cat  = dr.path(0).asText("");
                            String sch  = dr.path(1).asText("");
                            String name = dr.path(2).asText("");
                            if (name.endsWith("_drift_metrics")) {
                                String base = cat + "." + sch + "." + name.replaceAll("_drift_metrics$", "");
                                driftKeys.add(base);
                            } else if (name.endsWith("_profile_metrics")) {
                                String tbl  = name.replaceAll("_profile_metrics$", "");
                                String full = cat + "." + sch + "." + tbl;
                                profileTables.add(new String[]{cat, sch, tbl, full,
                                        cat + "." + sch + "." + name,
                                        cat + "." + sch + "." + tbl + "_drift_metrics"});
                            }
                        }

                        // Step 2: profile UNION ALL
                        StringBuilder unionSql = new StringBuilder();
                        for (String[] t : profileTables) {
                            String cat = t[0], sch = t[1], tbl = t[2], full = t[3], fqm = t[4];
                            String fullQ = full.replace("'", "''");
                            String catQ  = cat.replace("'", "''");
                            String schQ  = sch.replace("'", "''");
                            String tblQ  = tbl.replace("'", "''");
                            if (unionSql.length() > 0) unionSql.append(" UNION ALL ");
                            unionSql.append("SELECT '").append(fullQ).append("' AS full_name, ")
                                    .append("'").append(catQ).append("' AS catalog_name, ")
                                    .append("'").append(schQ).append("' AS schema_name, ")
                                    .append("'").append(tblQ).append("' AS table_name, ")
                                    .append("MAX(window.start) AS last_run, COUNT(*) AS run_count, ")
                                    .append("MAX(count) AS last_count ")
                                    .append("FROM ").append(fqm)
                                    .append(" WHERE column_name=':table' ")
                                    .append("AND window.start >= DATE_ADD(CURRENT_DATE,-30) ")
                                    .append("HAVING MAX(window.start) IS NOT NULL");
                        }

                        // Step 3: drift UNION ALL (only tables that have a _drift_metrics table)
                        StringBuilder driftSql = new StringBuilder();
                        for (String[] t : profileTables) {
                            String full = t[3], fqd = t[5];
                            if (!driftKeys.contains(full)) continue;
                            String fullQ = full.replace("'", "''");
                            if (driftSql.length() > 0) driftSql.append(" UNION ALL ");
                            driftSql.append("SELECT '").append(fullQ).append("' AS full_name, ")
                                    .append("COUNT(DISTINCT CASE WHEN (chi_square_test.pvalue < 0.05 OR ks_test.pvalue < 0.05) THEN column_name END) AS drifted_cols, ")
                                    .append("COUNT(DISTINCT column_name) AS total_cols ")
                                    .append("FROM ").append(fqd)
                                    .append(" WHERE column_name <> ':table' ")
                                    .append("AND window.start >= DATE_ADD(CURRENT_DATE,-14) ")
                                    .append("HAVING COUNT(DISTINCT column_name) > 0");
                        }

                        // Execute profile query
                        System.out.println("ClusterCollector: monitor SQL=" + unionSql.toString().substring(0, Math.min(500, unionSql.length())));
                        java.util.Map<String, String> driftMap = new java.util.HashMap<>();
                        JsonNode monResult = client.executeSqlStatement(billingWarehouseId, unionSql.toString());
                        String monState = monResult.path("status").path("state").asText("");
                        System.out.println("ClusterCollector: monitor union state=" + monState);

                        // Execute drift query if any drift tables exist
                        if (driftSql.length() > 0) {
                            String driftSqlStr = driftSql.toString();
                            JsonNode driftResult = client.executeSqlStatement(billingWarehouseId, driftSqlStr);
                            String driftState = driftResult.path("status").path("state").asText("");
                            String driftError = driftResult.path("status").path("error").path("message").asText("");
                            // chi_square_test column only exists for categorical columns; retry with ks_test only if absent
                            if ("FAILED".equals(driftState) && driftError.contains("chi_square_test")) {
                                driftSqlStr = driftSqlStr.replace("chi_square_test.pvalue < 0.05 OR ", "");
                                driftResult = client.executeSqlStatement(billingWarehouseId, driftSqlStr);
                                driftState = driftResult.path("status").path("state").asText("");
                                driftError = driftResult.path("status").path("error").path("message").asText("");
                            }
                            System.out.println("ClusterCollector: drift state=" + driftState
                                    + ("FAILED".equals(driftState) ? " error=" + driftError : ""));
                            if ("SUCCEEDED".equals(driftState)) {
                                JsonNode driftRows = driftResult.path("result").path("data_array");
                                if (driftRows.isArray()) {
                                    for (JsonNode dr : driftRows) {
                                        String drifted = dr.path(1).asText("");
                                        String total   = dr.path(2).asText("");
                                        String label   = (!drifted.isEmpty() && !total.isEmpty())
                                                ? drifted + " / " + total : "";
                                        driftMap.put(dr.path(0).asText(""), label);
                                    }
                                }
                            }
                            System.out.println("ClusterCollector: drift map size=" + driftMap.size());
                        }

                        if ("SUCCEEDED".equals(monState)) {
                            JsonNode monRows = monResult.path("result").path("data_array");
                            System.out.println("ClusterCollector: monitor union rows isArray=" + monRows.isArray()
                                    + " size=" + (monRows.isArray() ? monRows.size() : "n/a"));
                            if (monRows.isArray()) {
                                if (monRows.size() > 0) System.out.println("ClusterCollector: monitor row[0]=" + monRows.get(0).toString());
                                for (JsonNode row : monRows) {
                                    String fullName  = row.path(0).asText("");
                                    String catalog   = row.path(1).asText("");
                                    String schema    = row.path(2).asText("");
                                    String table     = row.path(3).asText("");
                                    String lastRun   = row.path(4).asText("").replace("T", " ").replaceAll("\\.\\d+Z?$", "");
                                    long   runs      = row.path(5).asLong(0);
                                    String lastCount = row.path(6).asText("");
                                    TopologyNode mNode = monitorsNode.createNode(fullName);
                                    setValue(mNode, "monitorKey",        fullName,                             true);
                                    setValue(mNode, "catalogName",       catalog,                              false);
                                    setValue(mNode, "schemaName",        schema,                               false);
                                    setValue(mNode, "tableName",         table,                                false);
                                    setValue(mNode, "monitorType",       "SNAPSHOT",                           false);
                                    setValue(mNode, "lastRunStatus",     "SUCCESSFUL",                         false);
                                    setValue(mNode, "lastRunTimeStr",    lastRun,                              false);
                                    setValue(mNode, "runCount30d",       runs > 0 ? String.valueOf(runs) : "", false);
                                    setValue(mNode, "failCount30d",      "",                                   false);
                                    setValue(mNode, "rowCount",          lastCount,                            false);
                                    setValue(mNode, "rowCountDelta",     "",                                   false);
                                    setValue(mNode, "driftColumnCount",  driftMap.getOrDefault(fullName, ""),  false);
                                }
                                System.out.println("ClusterCollector: monitor collection succeeded, count=" + monRows.size());
                            }
                        } else {
                            System.out.println("ClusterCollector: monitor union query state="
                                    + monState
                                    + ", error=" + monResult.path("status").path("error").path("message").asText(""));
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("ClusterCollector: monitor collection failed: " + e.getMessage());
                e.printStackTrace(System.out);
            }
            } // end if billingWarehouseId

            // ---------------------------------------------------------------------
            // AI Gateway Observability (system.ai_gateway.usage)
            // Requires account-admin; degrades gracefully if not available
            // ---------------------------------------------------------------------
            if (billingWarehouseId != null && !billingWarehouseId.isBlank()) {
                TopologyNode aiEndpointsNode      = workspaceNode.createNode("aiEndpoints");
                TopologyNode aiUsagesNode         = workspaceNode.createNode("aiUsages");
                TopologyNode aiUserActivitiesNode = workspaceNode.createNode("aiUserActivities");
                try {
                    // --- billing cost by endpoint (30-day, join to list_prices) ---
                    java.util.Map<String, Double> epDollarCost = new java.util.HashMap<>();
                    try {
                        String costSql = "SELECT b.usage_metadata.endpoint_name, "
                                + "CAST(SUM(b.usage_quantity * COALESCE(lp.pricing.effective_list.default, 0)) AS DOUBLE) AS dollar_cost "
                                + "FROM system.billing.usage b "
                                + "LEFT JOIN system.billing.list_prices lp "
                                + "  ON lp.sku_name = b.sku_name "
                                + "  AND b.usage_end_time >= lp.price_start_time "
                                + "  AND (lp.price_end_time IS NULL OR b.usage_end_time < lp.price_end_time) "
                                + "WHERE b.billing_origin_product = 'FOUNDATION_MODEL_API' "
                                + "  AND b.usage_date >= DATE_ADD(CURRENT_DATE, -30) "
                                + "  AND b.usage_metadata.endpoint_name IS NOT NULL "
                                + "GROUP BY b.usage_metadata.endpoint_name "
                                + "ORDER BY dollar_cost DESC "
                                + "LIMIT 200";
                        JsonNode costResult = client.executeSqlStatement(billingWarehouseId, costSql);
                        if ("SUCCEEDED".equals(costResult.path("status").path("state").asText(""))) {
                            JsonNode costData = costResult.path("result").path("data_array");
                            if (costData.isArray()) {
                                for (JsonNode row : costData) {
                                    String ep = row.path(0).asText("");
                                    if (!ep.isBlank()) epDollarCost.put(ep, row.path(1).asDouble(0.0));
                                }
                            }
                        }
                    } catch (Exception e) {
                        log.log("ClusterCollector: AI Gateway cost query failed: " + e.getMessage());
                    }

                    // --- endpoint rollup (30-day window) ---
                    String epSql = "SELECT endpoint_name, MIN(api_type) AS api_type, "
                            + "COUNT(*) AS request_count, "
                            + "COALESCE(SUM(total_tokens), 0) AS total_tokens, "
                            + "COALESCE(SUM(input_tokens), 0) AS input_tokens, "
                            + "COALESCE(SUM(output_tokens), 0) AS output_tokens, "
                            + "SUM(CASE WHEN status_code >= 400 THEN 1 ELSE 0 END) AS error_count, "
                            + "COALESCE(CAST(ROUND(AVG(latency_ms)) AS BIGINT), 0) AS avg_latency_ms, "
                            + "COALESCE(CAST(ROUND(approx_percentile(latency_ms, 0.95)) AS BIGINT), 0) AS p95_latency_ms "
                            + "FROM system.ai_gateway.usage "
                            + "WHERE event_time >= CURRENT_TIMESTAMP - INTERVAL 30 DAYS "
                            + "GROUP BY endpoint_name "
                            + "ORDER BY total_tokens DESC "
                            + "LIMIT 200";

                    JsonNode epResult = client.executeSqlStatement(billingWarehouseId, epSql);
                    if ("SUCCEEDED".equals(epResult.path("status").path("state").asText(""))) {
                        JsonNode epData = epResult.path("result").path("data_array");
                        if (epData.isArray()) {
                            for (JsonNode row : epData) {
                                String epName    = row.path(0).asText("");
                                if (epName.isBlank()) continue;
                                String epType    = row.path(1).asText("");
                                long   reqCount  = row.path(2).asLong(0);
                                long   totTok    = row.path(3).asLong(0);
                                long   inTok     = row.path(4).asLong(0);
                                long   outTok    = row.path(5).asLong(0);
                                long   errCount  = row.path(6).asLong(0);
                                long   avgLat    = row.path(7).asLong(0);
                                long   p95Lat    = row.path(8).asLong(0);
                                String errRate   = reqCount > 0
                                        ? String.format("%.1f%%", 100.0 * errCount / reqCount)
                                        : "0.0%";

                                TopologyNode epNode = aiEndpointsNode.createNode(epName);
                                epNode.setId(epName);
                                setValue(epNode, "endpointName",    epName,                    true);
                                setValue(epNode, "endpointType",    epType,                    false);
                                setValue(epNode, "requestCountStr", String.valueOf(reqCount),  false);
                                setValue(epNode, "totalTokensStr",  String.valueOf(totTok),    false);
                                setValue(epNode, "inputTokensStr",  String.valueOf(inTok),     false);
                                setValue(epNode, "outputTokensStr", String.valueOf(outTok),    false);
                                setValue(epNode, "errorCountStr",   String.valueOf(errCount),  false);
                                setValue(epNode, "errorRateStr",    errRate,                   false);
                                setValue(epNode, "avgLatencyStr",   avgLat + " ms",            false);
                                setValue(epNode, "p95LatencyStr",   p95Lat + " ms",            false);
                                double cost = epDollarCost.getOrDefault(epName, 0.0);
                                setValue(epNode, "dollarCostStr",
                                        cost > 0 ? String.format("$%,.2f",cost) : "", false);
                                epNode.createValue("requestCount").setSampleValue(reqCount);
                                epNode.createValue("totalTokens").setSampleValue(totTok);
                                epNode.createValue("inputTokens").setSampleValue(inTok);
                                epNode.createValue("outputTokens").setSampleValue(outTok);
                                epNode.createValue("errorCount").setSampleValue(errCount);
                                epNode.createValue("avgLatencyMs").setSampleValue(avgLat);
                                epNode.createValue("p95LatencyMs").setSampleValue(p95Lat);
                            }
                        }
                    } else {
                        log.log("ClusterCollector: AI Gateway endpoint query state="
                                + epResult.path("status").path("state").asText("")
                                + " error=" + epResult.path("status").path("error").path("message").asText(""));
                    }

                    // --- daily usage by endpoint + model ---
                    String dailySql = "SELECT CAST(event_time AS DATE) AS usage_date, "
                            + "endpoint_name, destination_model, COUNT(*) AS request_count, "
                            + "COALESCE(SUM(total_tokens), 0) AS total_tokens, "
                            + "COALESCE(SUM(input_tokens), 0) AS input_tokens, "
                            + "COALESCE(SUM(output_tokens), 0) AS output_tokens "
                            + "FROM system.ai_gateway.usage "
                            + "WHERE event_time >= CURRENT_TIMESTAMP - INTERVAL 30 DAYS "
                            + "GROUP BY CAST(event_time AS DATE), endpoint_name, destination_model "
                            + "ORDER BY usage_date DESC, total_tokens DESC "
                            + "LIMIT 500";

                    JsonNode dailyResult = client.executeSqlStatement(billingWarehouseId, dailySql);
                    if ("SUCCEEDED".equals(dailyResult.path("status").path("state").asText(""))) {
                        JsonNode dailyData = dailyResult.path("result").path("data_array");
                        if (dailyData.isArray()) {
                            for (JsonNode row : dailyData) {
                                String date    = row.path(0).asText("");
                                String epName  = row.path(1).asText("");
                                String model   = row.path(2).asText("");
                                long   req     = row.path(3).asLong(0);
                                long   tot     = row.path(4).asLong(0);
                                long   inp     = row.path(5).asLong(0);
                                long   out     = row.path(6).asLong(0);
                                String key     = date + "|" + epName + "|" + model;

                                TopologyNode uNode = aiUsagesNode.createNode(key);
                                uNode.setId(key);
                                setValue(uNode, "aiUsageKey",      key,                   true);
                                setValue(uNode, "usageDate",       date,                  false);
                                setValue(uNode, "endpointName",    epName,                false);
                                setValue(uNode, "modelName",       model,                 false);
                                setValue(uNode, "requestCountStr", String.valueOf(req),   false);
                                setValue(uNode, "totalTokensStr",  String.valueOf(tot),   false);
                                setValue(uNode, "inputTokensStr",  String.valueOf(inp),   false);
                                setValue(uNode, "outputTokensStr", String.valueOf(out),   false);
                                uNode.createValue("requestCount").setSampleValue(req);
                                uNode.createValue("totalTokens").setSampleValue(tot);
                                uNode.createValue("inputTokens").setSampleValue(inp);
                                uNode.createValue("outputTokens").setSampleValue(out);
                            }
                        }
                    } else {
                        log.log("ClusterCollector: AI Gateway daily usage query state="
                                + dailyResult.path("status").path("state").asText(""));
                    }

                    // --- per-requester activity ---
                    String userSql = "SELECT requester, MIN(requester_type) AS requester_type, "
                            + "COUNT(*) AS request_count, "
                            + "COALESCE(SUM(total_tokens), 0) AS total_tokens, "
                            + "SUM(CASE WHEN status_code >= 400 THEN 1 ELSE 0 END) AS error_count "
                            + "FROM system.ai_gateway.usage "
                            + "WHERE event_time >= CURRENT_TIMESTAMP - INTERVAL 30 DAYS "
                            + "GROUP BY requester "
                            + "ORDER BY total_tokens DESC "
                            + "LIMIT 200";

                    JsonNode userResult = client.executeSqlStatement(billingWarehouseId, userSql);
                    if ("SUCCEEDED".equals(userResult.path("status").path("state").asText(""))) {
                        JsonNode userData = userResult.path("result").path("data_array");
                        if (userData.isArray()) {
                            for (JsonNode row : userData) {
                                String requester = row.path(0).asText("");
                                if (requester.isBlank()) continue;
                                String reqType   = row.path(1).asText("");
                                long   req       = row.path(2).asLong(0);
                                long   tot       = row.path(3).asLong(0);
                                long   err       = row.path(4).asLong(0);

                                TopologyNode uaNode = aiUserActivitiesNode.createNode(requester);
                                uaNode.setId(requester);
                                setValue(uaNode, "requester",       requester,            true);
                                setValue(uaNode, "requesterType",   reqType,              false);
                                setValue(uaNode, "requestCountStr", String.valueOf(req),  false);
                                setValue(uaNode, "totalTokensStr",  String.valueOf(tot),  false);
                                setValue(uaNode, "errorCountStr",   String.valueOf(err),  false);
                                uaNode.createValue("requestCount").setSampleValue(req);
                                uaNode.createValue("totalTokens").setSampleValue(tot);
                                uaNode.createValue("errorCount").setSampleValue(err);
                            }
                        }
                    } else {
                        log.log("ClusterCollector: AI Gateway user activity query state="
                                + userResult.path("status").path("state").asText("")
                                + ", error=" + userResult.path("status").path("error").path("message").asText(""));
                    }

                } catch (Exception e) {
                    log.log("ClusterCollector: AI Gateway collection failed (account-admin required): " + e.getMessage());
                }
            }

            System.out.println("ClusterCollector: topology summary rootType=DatabricksModelRoot"
                    + ", accountId=" + accountId
                    + ", accountName=" + accountName
                    + ", workspace=" + workspaceUrl
                    + ", clusterCount=" + clusterCount
                    + ", jobCount=" + jobCount
                    + ", runCount=" + runCount
                    + ", warehouseCount=" + warehouseCount
                    + ", pipelineCount=" + pipelineCount
                    + ", poolCount=" + poolCount
                    + ", usageCount=" + usageCount
                    + ", jobDbuCount=" + jobDbuCount);

            System.out.println("=== ClusterCollector summary === "
                    + "accountId=" + accountId
                    + ", workspace=" + workspaceUrl
                    + ", clusterCount=" + clusterCount
                    + ", jobCount=" + jobCount
                    + ", warehouseCount=" + warehouseCount
                    + ", pipelineCount=" + pipelineCount
                    + ", poolCount=" + poolCount
                    + ", runCount=" + runCount
                    + ", usageCount=" + usageCount
                    + ", jobDbuCount=" + jobDbuCount);

            submitter.submit(now);

            System.out.println("=== ClusterCollector submit complete === "
                    + "accountId=" + accountId
                    + ", workspace=" + workspaceUrl);

        } catch (Exception e) {
            log.errorUnexpected("ClusterCollector failed", e);
        }
    }

    private static String serializeTags(JsonNode tagsNode) {
        if (tagsNode == null || !tagsNode.isObject() || tagsNode.size() == 0) return "";
        StringBuilder sb = new StringBuilder();
        tagsNode.fields().forEachRemaining(e -> {
            if (sb.length() > 0) sb.append(", ");
            sb.append(e.getKey()).append("=").append(e.getValue().asText());
        });
        return sb.toString();
    }

    private static String fmtDur(long ms) {
        if (ms <= 0) return "";
        long s = ms / 1000;
        long m = s / 60;
        long h = m / 60;
        if (h > 0) return h + "h " + (m % 60) + "m";
        if (m > 0) return m + "m " + (s % 60) + "s";
        return s + "s";
    }

    private static String bareId(String val) {
        if (val == null || val.isEmpty()) return val == null ? "" : val;
        for (String prefix : new String[]{"projects/", "branches/", "endpoints/"}) {
            while (val.startsWith(prefix)) val = val.substring(prefix.length());
        }
        int slash = val.lastIndexOf('/');
        return slash >= 0 ? val.substring(slash + 1) : val;
    }

    private static String fmtTs(long epochMs) {
        if (epochMs <= 0) return "";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(epochMs));
    }

    private static String utilBar(double ratio) {
        if (ratio < 0) return "";  // no data available for this cluster
        int filled = (int) Math.round(ratio * 8);
        filled = Math.max(0, Math.min(8, filled));
        String bar = "████████".substring(0, filled) + "░░░░░░░░".substring(0, 8 - filled);
        return bar + " " + (int) Math.round(ratio * 100) + "%";
    }

    private static String formatBytes(long bytes) {
        if (bytes < 1024L) return bytes + " B";
        if (bytes < 1024L * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024));
        if (bytes < 1024L * 1024 * 1024 * 1024) return String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024));
        return String.format("%.2f TB", bytes / (1024.0 * 1024 * 1024 * 1024));
    }

    private void setValue(TopologyNode node, String name, String value, boolean isIdentity) {
        TopologyValue valueNode = node.createValue(name);
        valueNode.setSampleValue(value == null ? "" : value);
        if (isIdentity) {
            valueNode.setIsIdentity(true);
        }
    }
}