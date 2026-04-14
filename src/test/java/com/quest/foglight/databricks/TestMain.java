package com.quest.foglight.databricks;

import com.quest.glue.api.services.*;
import com.quest.glue.api.services.TopologyDataSubmissionService3.TopologySubmitter3;

import java.util.*;

/**
 * Standalone test: runs ClusterCollector.collect() against the real Databricks API
 * using stub implementations of the FglAM topology interfaces. No FglAM runtime needed.
 *
 * Usage: see test.ps1 in project root
 */
public class TestMain {

    public static void main(String[] args) throws Exception {
        String workspaceUrl = args.length > 0 ? args[0] : "https://adb-5019866263455093.13.azuredatabricks.net/";
        String accessToken  = args.length > 1 ? args[1] : "dapi6d8e372a03a1cff3f5d78b368437af54-3";

        System.out.println("Testing ClusterCollector against: " + workspaceUrl);

        DatabricksClient client = new DatabricksClient(workspaceUrl, accessToken);
        LogService.Logger log = new StubLogger();
        TopologyDataSubmissionService3 topologyService = new StubTopologyService();

        // ClusterCollector collector = new ClusterCollector(client, topologyService, log, workspaceUrl);
        ClusterCollector collector = new ClusterCollector(client, topologyService, log, workspaceUrl,"dummy-account-id","dummy-account-name");
        collector.collect();
    }

    // --- Stubs ---

    static class StubLogger implements LogService.Logger {
        @Override public void log(String msg) { System.out.println("[LOG] " + msg); }
        @Override public void log(String msg, Throwable t) { System.out.println("[LOG] " + msg); t.printStackTrace(); }
        @Override public void log(String msg, String... args) { System.out.println("[LOG] " + msg + " " + Arrays.toString(args)); }
        @Override public void log(String msg, Throwable t, String... args) { System.out.println("[LOG] " + msg); t.printStackTrace(); }
        @Override public void logOnce(String msg) { log(msg); }
        @Override public void logOnce(String msg, Throwable t) { log(msg, t); }
        @Override public void logOnce(String msg, String... args) { log(msg, args); }
        @Override public void logOnce(String msg, Throwable t, String... args) { log(msg, t, args); }
        @Override public void logRaw(long ts, String level, String logger, String thread, String msg, Throwable t) { System.out.println("[RAW] " + msg); }
        @Override public void debug(String msg) { System.out.println("[DEBUG] " + msg); }
        @Override public void debug(String msg, Throwable t) { System.out.println("[DEBUG] " + msg); }
        @Override public void debug2(String msg) { System.out.println("[DEBUG2] " + msg); }
        @Override public void debug2(String msg, Throwable t) { System.out.println("[DEBUG2] " + msg); }
        @Override public void errorUnexpected(String msg) { System.out.println("[ERROR] " + msg); }
        @Override public void errorUnexpected(String msg, Throwable t) { System.out.println("[ERROR] " + msg); t.printStackTrace(); }
        @Override public void ignoreException(String msg, Throwable t) { System.out.println("[IGNORE] " + msg); }
        @Override public int getDebugLevel() { return 2; }
    }

    static class StubTopologyService implements TopologyDataSubmissionService3 {
        @Override public TopologySubmitter3 getTopologySubmitter() { return new StubSubmitter(); }
        @Override public boolean isServerOverloaded() { return false; }
        @Override public boolean isSampleTooOld(Date d) { return false; }
    }

    static class StubSubmitter implements TopologySubmitter3 {
        private final List<StubNode> roots = new ArrayList<>();

        @Override public TopologyNode createTopLevelNode(String type, long ts) {
            StubNode n = new StubNode(type); roots.add(n); return n;
        }
        @Override public List<? extends TopologyNode> getTopLevelNodes() { return roots; }
        @Override public void submit(long ts) {
            System.out.println("\n=== TOPOLOGY SUBMISSION ===");
            for (StubNode r : roots) r.print("");
            System.out.println("===========================");
        }
        @Override public void submit() { submit(System.currentTimeMillis()); }
        @Override public TopologyDataSubmissionService3.VerifiedSubmission3 submitVerified() { submit(); return null; }
        @Override public VerifiedSubmissionWaiter submitVerifiedWaiter() { return null; }
        @Override public TopologyDataSubmissionService3.VerifiedSubmission3 submitVerified(long timeout) { return submitVerified(); }
        @Override public VerifiedSubmissionWaiter submitVerifiedWaiter(long timeout) { return null; }
        @Override public void flushPendingMessages() {}
        @Override public void setTopologyFormat(String f) {}
        @Override public void setPriority(TopologySubmissionPriority p) {}
        @Override public TopologySubmissionPriority getPriority() { return null; }
        @Override public void setHistoricalData(boolean h) {}
        @Override public boolean isHistorical() { return false; }
        @Override public void assumeNextSubmissionAfter(Long ts) {}
        @Override public Long getNextAssumedSubmission() { return null; }
        @Override public void setMetaData(String k, String v) {}
        @Override public String getMetaData(String k) { return null; }
        @Override public void setInvalidDataAction(TopologySubmitter3.InvalidDataAction a) {}
    }

    static class StubNode implements TopologyNode {
        final String type;
        String id;
        boolean identity;
        String typeHint;
        final Map<String, Object> values = new LinkedHashMap<>();
        final List<StubNode> children = new ArrayList<>();

        StubNode(String type) { this.type = type; }

        @Override public String getName() { return type; }
        @Override public void setId(String id) { this.id = id; }
        @Override public void setRefId(String id) {}
        @Override public void setIsIdentity(boolean v) { this.identity = v; }
        @Override public boolean isIdentity() { return identity; }
        @Override public void setTypeHint(String hint) { this.typeHint = hint; }
        @Override public void setReplace(boolean v) {}
        @Override public boolean isReplace() { return false; }
        @Override public void setNodeTimestamp(long ts) {}
        @Override public long getNodeTimestamp() { return 0; }
        @Override public void setNodeFrequency(long f) {}
        @Override public long getNodeFrequency() { return 0; }

        @Override
        public <N extends TopologyNode> N createNode(String relation) {
            StubNode child = new StubNode(typeHint != null ? typeHint : relation);
            typeHint = null; // consume it
            children.add(child);
            @SuppressWarnings("unchecked") N n = (N) child;
            return n;
        }

        @Override
        public <V extends TopologyValue> V createValue(String name) {
            StubValue v = new StubValue(name, values);
            @SuppressWarnings("unchecked") V vv = (V) v;
            return vv;
        }

        void print(String indent) {
            System.out.println(indent + "[" + type + "]  id=" + id + "  identity=" + identity);
            for (Map.Entry<String, Object> e : values.entrySet()) {
                System.out.println(indent + "  " + e.getKey() + " = " + e.getValue());
            }
            for (StubNode child : children) child.print(indent + "  ");
        }
    }

    static class StubValue implements TopologyValue {
        final String name;
        final Map<String, Object> store;

        StubValue(String name, Map<String, Object> store) { this.name = name; this.store = store; }

        @Override public String getName() { return name; }
        @Override public void setSampleValue(String v) { store.put(name, v); }
        @Override public void setSampleValue(String v, UnitService.Units u) { store.put(name, v); }
        @Override public void setSampleValue(Number v) { store.put(name, v); }
        @Override public void setSampleValue(Number v, UnitService.Units u) { store.put(name, v); }
        @Override public void setSampleValue(Date v) { store.put(name, v); }
        @Override public void setSampleValue(Boolean v) { store.put(name, v); }
        @Override public void setIsIdentity(boolean v) { store.put(name + "*", "(identity)"); }
        @Override public boolean isIdentity() { return false; }
        @Override public void setReplace(boolean v) {}
        @Override public boolean isReplace() { return false; }
        @Override public void setValueTimestamp(long ts) {}
        @Override public long getValueTimestamp() { return 0; }
        @Override public void setValueFrequency(long f) {}
        @Override public long getValueFrequency() { return 0; }
        @Override public Number getValueNumber() { return null; }
        @Override public String getValueText() { return null; }
        @Override public Date getValueDate() { return null; }
        @Override public Boolean getValueBoolean() { return null; }
    }
}
