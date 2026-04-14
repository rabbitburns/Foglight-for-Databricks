package com.quest.foglight.databricks;

import com.quest.glue.api.agent.Agent;
import com.quest.glue.api.services.ASPService;
import com.quest.glue.api.services.LogService;
import com.quest.glue.api.services.ServiceFactory;
import com.quest.glue.api.services.TopologyDataSubmissionService3;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DatabricksAgent implements Agent {

    private final ServiceFactory serviceFactory;
    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> collectionTask;

    public DatabricksAgent(ServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
    }

    @Override
    public void startDataCollection() throws Exception {
        LogService.Logger log = null;
        try {
            LogService logService = serviceFactory.getService(LogService.class);
            log = logService.getLogger(DatabricksAgent.class);
            log.log("DatabricksAgent: startDataCollection entered");

            Properties fileProps = loadConfigFile(log);
            ASPService aspService = serviceFactory.getService(ASPService.class);

            String workspaceUrl = firstNonBlank(
                fileProps.getProperty("workspaceUrl"),
                aspToString(aspService.getPrimaryASP("workspaceUrl"))
            );
            String accessToken = firstNonBlank(
                fileProps.getProperty("accessToken"),
                aspToString(aspService.getPrimaryASP("accessToken"))
            );

            if (workspaceUrl == null || workspaceUrl.isBlank()) {
                log.log("DatabricksAgent: workspaceUrl not configured, collection disabled");
                return;
            }

            if (accessToken == null || accessToken.isBlank()) {
                log.log("DatabricksAgent: accessToken not configured, collection disabled");
                return;
            }

            String intervalStr = firstNonBlank(
                fileProps.getProperty("collectionIntervalSeconds"),
                aspToString(aspService.getPrimaryASP("collectionIntervalSeconds"))
            );
            long intervalSeconds = intervalStr != null ? Long.parseLong(intervalStr) : 60L;

            String accountId = firstNonBlank(fileProps.getProperty("accountId"), "default");
            String accountName = firstNonBlank(fileProps.getProperty("accountName"), "Databricks");

            DatabricksClient client = new DatabricksClient(workspaceUrl, accessToken);
            TopologyDataSubmissionService3 topologyService =
                serviceFactory.getService(TopologyDataSubmissionService3.class);

            ClusterCollector collector = new ClusterCollector(
                client,
                topologyService,
                log,
                workspaceUrl,
                accountId,
                accountName
            );

            log.log("DatabricksAgent: running initial collection");
            collector.collect();

            scheduler = Executors.newSingleThreadScheduledExecutor();
            collectionTask = scheduler.scheduleWithFixedDelay(
                collector::collect,
                intervalSeconds,
                intervalSeconds,
                TimeUnit.SECONDS
            );

            log.log("DatabricksAgent started: workspace=" + workspaceUrl
                + ", accountId=" + accountId
                + ", interval=" + intervalSeconds + "s");
        } catch (Exception e) {
            System.err.println("DatabricksAgent.startDataCollection failed: " + e);
            e.printStackTrace(System.err);
            if (log != null) {
                log.log("DatabricksAgent: startDataCollection failed: " + e);
            }
            throw e;
        }
    }

    private Properties loadConfigFile(LogService.Logger log) {
        Properties props = new Properties();

        File jarDir = getJarDir();
        log.log("DatabricksAgent: jar dir=" + jarDir);

        List<File> candidates = new ArrayList<>();
        if (jarDir != null && jarDir.getParentFile() != null) {
            candidates.add(new File(jarDir.getParentFile(), "config/databricks.properties"));
        }
        candidates.add(new File(System.getProperty("glue.agent.home", "."), "config/databricks.properties"));
        candidates.add(new File("config/databricks.properties"));

        for (File configFile : candidates) {
            log.log("DatabricksAgent: trying " + configFile.getAbsolutePath() + " exists=" + configFile.exists());
            if (configFile.exists()) {
                try (FileInputStream fis = new FileInputStream(configFile)) {
                    props.load(fis);
                    log.log("DatabricksAgent: loaded config from " + configFile.getAbsolutePath());
                    return props;
                } catch (Exception e) {
                    log.log("DatabricksAgent: failed to load " + configFile.getAbsolutePath() + ": " + e.getMessage());
                }
            }
        }

        return props;
    }

    private File getJarDir() {
        try {
            java.security.CodeSource codeSource = DatabricksAgent.class.getProtectionDomain().getCodeSource();
            if (codeSource == null || codeSource.getLocation() == null) {
                return null;
            }

            File location = new File(codeSource.getLocation().toURI());
            if (location.isFile()) {
                return location.getParentFile();
            }
            if (location.isDirectory()) {
                return location;
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String aspToString(Object asp) {
        return asp != null ? asp.toString() : null;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        if (second != null && !second.isBlank()) {
            return second;
        }
        return null;
    }

    @Override
    public void stopDataCollection() throws Exception {
        if (collectionTask != null) {
            collectionTask.cancel(false);
            collectionTask = null;
        }

        if (scheduler != null) {
            scheduler.shutdown();
            scheduler = null;
        }
    }

    @Override
    public void destroy() throws Exception {
        stopDataCollection();
    }
}