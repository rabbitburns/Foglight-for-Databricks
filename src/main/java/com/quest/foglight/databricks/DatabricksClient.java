package com.quest.foglight.databricks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

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