package com.quest.foglight.databricks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class DatabricksClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String baseUrl;
    private final String token;
    private final HttpClient http;

    public DatabricksClient(String baseUrl, String token) {
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.token = token;
        this.http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public JsonNode listClusters() throws Exception {
        return get("/api/2.0/clusters/list");
    }

    public JsonNode listJobs() throws Exception {
        return get("/api/2.1/jobs/list?limit=100");
    }

    public JsonNode listRunsForJob(String jobId) throws Exception {
        return get("/api/2.1/jobs/runs/list?job_id=" + jobId + "&limit=10");
    }

    public JsonNode listWarehouses() throws Exception {
        return get("/api/2.0/sql/warehouses");
    }

    public JsonNode listPipelines() throws Exception {
        return get("/api/2.0/pipelines?max_results=100");
    }

    public JsonNode listInstancePools() throws Exception {
        return get("/api/2.0/instance-pools/list");
    }

    public JsonNode listQueriesForWarehouse(String warehouseId) throws Exception {
        return get("/api/2.0/sql/history/queries?max_results=25&filter_by.warehouse_ids=" + warehouseId);
    }

    public JsonNode executeSqlStatement(String warehouseId, String sql) throws Exception {
        ObjectNode body = MAPPER.createObjectNode();
        body.put("warehouse_id", warehouseId);
        body.put("statement", sql);
        body.put("wait_timeout", "20s");
        body.put("on_wait_timeout", "CONTINUE");

        JsonNode result = post("/api/2.0/sql/statements", MAPPER.writeValueAsString(body));

        String state = result.path("status").path("state").asText("");
        String statementId = result.path("statement_id").asText("");
        int attempts = 0;
        while (!state.equals("SUCCEEDED") && !state.equals("FAILED") && !state.equals("CANCELED") && attempts < 15) {
            Thread.sleep(2000);
            result = get("/api/2.0/sql/statements/" + statementId);
            state = result.path("status").path("state").asText("");
            attempts++;
        }
        return result;
    }

    private JsonNode post(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(30))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("=== Databricks API call ===");
        System.out.println("POST " + baseUrl + path);
        System.out.println("Status: " + response.statusCode());

        if (response.statusCode() != 200) {
            System.out.println("Response body:");
            System.out.println(response.body());
            throw new RuntimeException("Databricks API " + response.statusCode()
                    + " for " + path + ": " + response.body());
        }

        JsonNode json = MAPPER.readTree(response.body());
        System.out.println("Response body:");
        System.out.println(json.toPrettyString());
        return json;
    }

    private JsonNode get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();

        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("=== Databricks API call ===");
        System.out.println("GET " + baseUrl + path);
        System.out.println("Status: " + response.statusCode());

        if (response.statusCode() != 200) {
            System.out.println("Response body:");
            System.out.println(response.body());
            throw new RuntimeException("Databricks API " + response.statusCode()
                    + " for " + path + ": " + response.body());
        }

        JsonNode json = MAPPER.readTree(response.body());

        System.out.println("Response body:");
        System.out.println(json.toPrettyString());

        return json;
    }
}