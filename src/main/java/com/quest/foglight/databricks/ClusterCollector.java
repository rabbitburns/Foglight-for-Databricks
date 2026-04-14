package com.quest.foglight.databricks;

import com.fasterxml.jackson.databind.JsonNode;
import com.quest.glue.api.services.LogService;
import com.quest.glue.api.services.TopologyDataSubmissionService3;
import com.quest.glue.api.services.TopologyNode;
import com.quest.glue.api.services.TopologyValue;

import java.util.ArrayList;
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

    public ClusterCollector(DatabricksClient client,
                            TopologyDataSubmissionService3 topologyService,
                            LogService.Logger log,
                            String workspaceUrl,
                            String accountId,
                            String accountName) {
        this.client = client;
        this.topologyService = topologyService;
        this.log = log;
        this.workspaceUrl = workspaceUrl;
        this.accountId = accountId;
        this.accountName = accountName;
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

            log.log("ClusterCollector: creating top-level node type=DatabricksModelRoot"
                    + ", accountId=" + accountId
                    + ", workspace=" + workspaceUrl
                    + ", now=" + now);

            TopologyNode root = submitter.createTopLevelNode("DatabricksModelRoot", now);
            root.setId("DatabricksModelRoot");

            log.log("ClusterCollector: created top-level node type=DatabricksModelRoot"
                    + ", accountId=" + accountId
                    + ", workspace=" + workspaceUrl);

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
            int jobCount = 0;
            int runCount = 0;
            int warehouseCount = 0;
            int pipelineCount = 0;
            int poolCount = 0;

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
                    setValue(clusterNode, "pinnedByUserName", cluster.path("pinned_by_user_name").asText(""), false);
                    setValue(clusterNode, "startTimeStr", String.valueOf(cluster.path("start_time").asLong(0)), false);
                    setValue(clusterNode, "lastActivityTimeStr", String.valueOf(cluster.path("last_activity_time").asLong(0)), false);
                    setValue(clusterNode, "terminatedTimeStr", String.valueOf(cluster.path("terminated_time").asLong(0)), false);

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
                            setValue(jobNode, "lastRunStartStr",    String.valueOf(lastStart), false);
                            setValue(jobNode, "lastRunDurationStr", String.valueOf(Math.max(0, lastEnd - lastStart)), false);

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
                                setValue(runNode, "startTimeStr", String.valueOf(startTime), false);
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

                    TopologyNode whNode = warehousesNode.createNode(whId);
                    whNode.setId(whId);

                    setValue(whNode, "warehouseId", whId, true);
                    setValue(whNode, "warehouseName", wh.path("name").asText(whId), false);
                    setValue(whNode, "state", wh.path("state").asText(""), false);
                    setValue(whNode, "stateStr", wh.path("state").asText(""), false);
                    setValue(whNode, "warehouseType", wh.path("warehouse_type").asText(""), false);
                    setValue(whNode, "size", wh.path("cluster_size").asText(""), false);
                    setValue(whNode, "creatorName", wh.path("creator_name").asText(""), false);
                    setValue(whNode, "enablePhoton", String.valueOf(wh.path("enable_photon").asBoolean(false)), false);
                    setValue(whNode, "autoResume", String.valueOf(wh.path("auto_resume").asBoolean(false)), false);
                    long numClusters = wh.path("num_clusters").asLong(0);
                    whNode.createValue("numClusters").setSampleValue(numClusters);
                    setValue(whNode, "numClustersStr", String.valueOf(numClusters), false);
                    whNode.createValue("minClusters").setSampleValue(wh.path("min_num_clusters").asLong(1));
                    whNode.createValue("maxClusters").setSampleValue(wh.path("max_num_clusters").asLong(1));
                    whNode.createValue("autoStopMins").setSampleValue(wh.path("auto_stop_mins").asLong(0));
                }
            }

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
                }
            }

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

            log.log("ClusterCollector: topology summary rootType=DatabricksModelRoot"
                    + ", accountId=" + accountId
                    + ", accountName=" + accountName
                    + ", workspace=" + workspaceUrl
                    + ", clusterCount=" + clusterCount
                    + ", jobCount=" + jobCount
                    + ", runCount=" + runCount
                    + ", warehouseCount=" + warehouseCount
                    + ", pipelineCount=" + pipelineCount
                    + ", poolCount=" + poolCount);

            System.out.println("=== ClusterCollector summary === "
                    + "accountId=" + accountId
                    + ", workspace=" + workspaceUrl
                    + ", clusterCount=" + clusterCount
                    + ", jobCount=" + jobCount
                    + ", warehouseCount=" + warehouseCount
                    + ", pipelineCount=" + pipelineCount
                    + ", poolCount=" + poolCount
                    + ", runCount=" + runCount);

            submitter.submit(now);

            System.out.println("=== ClusterCollector submit complete === "
                    + "accountId=" + accountId
                    + ", workspace=" + workspaceUrl);

        } catch (Exception e) {
            log.errorUnexpected("ClusterCollector failed", e);
        }
    }

    private void setValue(TopologyNode node, String name, String value, boolean isIdentity) {
        TopologyValue valueNode = node.createValue(name);
        valueNode.setSampleValue(value == null ? "" : value);
        if (isIdentity) {
            valueNode.setIsIdentity(true);
        }
    }
}