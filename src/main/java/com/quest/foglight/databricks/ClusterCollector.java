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
                    setValue(clusterNode, "startTimeStr",        fmtTs(cluster.path("start_time").asLong(0)), false);
                    setValue(clusterNode, "lastActivityTimeStr", fmtTs(cluster.path("last_activity_time").asLong(0)), false);
                    setValue(clusterNode, "terminatedTimeStr",   fmtTs(cluster.path("terminated_time").asLong(0)), false);

                    JsonNode autoscale = cluster.path("autoscale");
                    if (autoscale.isObject()) {
                        clusterNode.createValue("autoscaleEnabled").setSampleValue(true);
                        clusterNode.createValue("autoscaleMinWorkers")
                                .setSampleValue(autoscale.path("min_workers").asInt(0));
                        clusterNode.createValue("autoscaleMaxWorkers")
                                .setSampleValue(autoscale.path("max_workers").asInt(0));
                        clusterNode.createValue("autoscaleTargetWorkers")
                                .setSampleValue(autoscale.path("target_workers").asInt(0));
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
                    } catch (Exception e) {
                        log.log("ClusterCollector: failed to fetch queries for warehouse " + whId + ": " + e.getMessage());
                    }
                }
            }

            workspaceNode.createValue("warehouseCount").setSampleValue((long) warehouseCount);
            workspaceNode.createValue("activeWarehouseCount").setSampleValue((long) activeWarehouseCount);

            // ---------------------------------------------------------------------
            // pipelines -> DatabricksPipeline
            // ---------------------------------------------------------------------
            TopologyNode pipelinesNode = workspaceNode.createNode("pipelines");

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
            // usages -> DatabricksUsage (system.billing.usage via SQL warehouse)
            // ---------------------------------------------------------------------
            TopologyNode usagesNode = workspaceNode.createNode("usages");

            if (billingWarehouseId != null && !billingWarehouseId.isBlank()) {
                try {
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
                                setValue(usageNode, "dollarCostStr",   String.format("%.4f", cost), false);
                            }
                        }
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
                    String jobSql = "SELECT usage_metadata.job_id, "
                            + "CAST(SUM(usage_quantity) AS DOUBLE) AS dbu_total "
                            + "FROM system.billing.usage "
                            + "WHERE usage_metadata.job_id IS NOT NULL "
                            + "AND usage_date >= DATE_ADD(CURRENT_DATE, -30) "
                            + "GROUP BY usage_metadata.job_id "
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
                                double dbu = row.path(1).asDouble(0.0);

                                jobDbuCount++;
                                TopologyNode jobDbuNode = jobDbusNode.createNode(jobId);
                                jobDbuNode.setId(jobId);
                                setValue(jobDbuNode, "jobId", jobId, true);
                                jobDbuNode.createValue("dbuConsumed").setSampleValue((long)(dbu * 1000));
                                setValue(jobDbuNode, "dbuConsumedStr", String.format("%.2f", dbu), false);
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

            } else {
                log.log("ClusterCollector: no billing warehouse configured, skipping billing collection");
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

    private static String fmtTs(long epochMs) {
        if (epochMs <= 0) return "";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(epochMs));
    }

    private void setValue(TopologyNode node, String name, String value, boolean isIdentity) {
        TopologyValue valueNode = node.createValue(name);
        valueNode.setSampleValue(value == null ? "" : value);
        if (isIdentity) {
            valueNode.setIsIdentity(true);
        }
    }
}