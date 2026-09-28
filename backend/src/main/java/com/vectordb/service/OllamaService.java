// src/main/java/com/vectordb/service/OllamaService.java
package com.vectordb.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class OllamaService {

    private final RestTemplate embedClient;
    private final RestTemplate generateClient;
    private final RestTemplate pingClient;

    @Value("${ollama.embed-model}")
    private String embedModel;

    @Value("${ollama.gen-model}")
    private String genModel;

    @Value("${ollama.base-url}")
    private String baseUrl;

    // Three separate RestTemplate instances with different timeouts —
    // embed: 30s, generate: 180s, ping: 2s
    public OllamaService(RestTemplateBuilder builder,
                         @Value("${ollama.timeout.connect-ms:2000}") int connectMs,
                         @Value("${ollama.timeout.embed-ms:30000}") int embedMs,
                         @Value("${ollama.timeout.generate-ms:180000}") int generateMs) {

        this.pingClient = builder
        .setConnectTimeout(Duration.ofMillis(connectMs))
        .setReadTimeout(Duration.ofMillis(connectMs))
        .build();

        this.embedClient = builder
        .setConnectTimeout(Duration.ofMillis(connectMs))
        .setReadTimeout(Duration.ofMillis(embedMs))
        .build();

        this.generateClient = builder
        .setConnectTimeout(Duration.ofMillis(connectMs))
        .setReadTimeout(Duration.ofMillis(generateMs))
        .build();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns true if Ollama is reachable. Fast check — uses 2s timeout.
     */
    public boolean isAvailable() {
        // Step 2 & Quick Test: Print the URL and verify the base URL
        log.info("Checking Ollama at {}", baseUrl + "/api/tags");
        log.info("Base URL: {}", baseUrl);

        try {
            ResponseEntity<String> res = pingClient.getForEntity(
                    baseUrl + "/api/tags", String.class);
            
            // Quick Test: Print the status code
            log.info("Status: {}", res.getStatusCode());
            
            return res.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            // Step 1: Don't hide the exception (Log the full stack trace)
            log.error("Ollama availability check failed", e);
            return false;
        }
    }

    /**
     * Embeds text using nomic-embed-text.
     * Returns empty array on failure (Ollama offline or model missing).
     */
    public double[] embed(String text) {
        try {
            Map<String, String> body = Map.of(
                    "model", embedModel,
                    "prompt", text
            );
            Map<?, ?> response = embedClient.postForObject(
                    baseUrl + "/api/embeddings", body, Map.class);

            if (response == null || !response.containsKey("embedding")) {
                log.warn("embed: no 'embedding' field in Ollama response");
                return new double[0];
            }

            @SuppressWarnings("unchecked")
            List<Number> raw = (List<Number>) response.get("embedding");
            return toDoubleArray(raw);

        } catch (ResourceAccessException e) {
            log.warn("embed: Ollama unreachable — {}", e.getMessage());
            return new double[0];
        } catch (Exception e) {
            log.error("embed: unexpected error", e);
            return new double[0];
        }
    }

    /**
     * Generates text using llama3.2, stream=false.
     * Returns error string on failure (matches C++ original behaviour).
     */
    public String generate(String prompt) {
        try {
            Map<String, Object> body = Map.of(
                    "model", genModel,
                    "prompt", prompt,
                    "stream", false
            );
            Map<?, ?> response = generateClient.postForObject(
                    baseUrl + "/api/generate", body, Map.class);

            if (response == null || !response.containsKey("response")) {
                log.warn("generate: no 'response' field in Ollama response");
                return "ERROR: unexpected response from Ollama";
            }

            return (String) response.get("response");

        } catch (ResourceAccessException e) {
            log.warn("generate: Ollama unreachable — {}", e.getMessage());
            return "ERROR: Ollama unavailable. Run: ollama serve";
        } catch (Exception e) {
            log.error("generate: unexpected error", e);
            return "ERROR: " + e.getMessage();
        }
    }

    // ── Accessors (used by StatusController before VectorStoreService exists) ─

    public String getEmbedModel() {
        return embedModel;
    }

    public String getGenModel() {
        return genModel;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private double[] toDoubleArray(List<Number> list) {
        double[] arr = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            arr[i] = list.get(i).doubleValue();
        }
        return arr;
    }
}