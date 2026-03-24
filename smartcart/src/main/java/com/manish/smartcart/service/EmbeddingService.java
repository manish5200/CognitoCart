package com.manish.smartcart.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

/**
 * CONCEPT: This service converts text into a mathematical vector (embedding)
 * using the Hugging Face free Inference API.
 *
 * FREE MODEL: sentence-transformers/all-MiniLM-L6-v2
 *   - Output: 384-dimensional vector (vs OpenAI's 1536)
 *   - Speed: Very fast (~100ms)
 *   - Cost: 100% FREE — 1000 requests/day on HuggingFace free tier
 *   - Quality: Excellent for semantic similarity tasks (e-commerce search, FAQs)
 *
 * HOW IT WORKS:
 *   Text → HuggingFace API → float[384] (meaning as math)
 *   "Blue wireless headphones" → [0.021, -0.455, 0.891, ... 384 numbers]
 *   "Earphones for noisy cafe" → [0.019, -0.448, 0.877, ... 384 similar numbers]
 *   These two are mathematically CLOSE → cosine similarity returns both!
 */
@Slf4j
@Service
public class EmbeddingService {

    // Injected from application.yml at startup — never hardcoded in source code
    @Value("${huggingface.api-key}")
    private String apiKey;

    @Value("${huggingface.model}")
    private String model;

    // CONCEPT: We configure RestTemplate with a permissive message converter.
    // The new HuggingFace router endpoint returns a Content-Type header like:
    //   "application/json, application/yaml, application/*+json"
    // Spring's default strict MIME parser rejects this multi-value format.
    // By setting MediaType.ALL as a supported media type, we tell RestTemplate:
    // "Accept any Content-Type in the response — don't reject based on headers."
    private final RestTemplate restTemplate = buildPermissiveRestTemplate();

    private static RestTemplate buildPermissiveRestTemplate() {
        org.springframework.http.converter.json.MappingJackson2HttpMessageConverter converter =
                new org.springframework.http.converter.json.MappingJackson2HttpMessageConverter();
        // Accept ALL media types — bypasses the strict comma-separated MIME type validation
        converter.setSupportedMediaTypes(java.util.List.of(MediaType.ALL));
        RestTemplate template = new RestTemplate();
        template.setMessageConverters(java.util.List.of(converter));
        return template;
    }

    // HuggingFace Inference Router URL (new endpoint as of 2025 — api-inference.huggingface.co is deprecated)
    // The {model} placeholder is substituted at runtime by RestTemplate's exchange() method
    private static final String HF_API_URL =
            "https://router.huggingface.co/hf-inference/models/{model}/pipeline/feature-extraction";

    /**
     * Converts any text string into a 384-dimensional float vector.
     *
     * CONCEPT: This is the "translator" between human language and math.
     * The HuggingFace model reads the text and produces a fixed-size array
     * of numbers that encode the SEMANTIC MEANING of the text.
     *
     * Semantically similar texts → numerically close vectors (cosine distance ≈ 0)
     * Unrelated texts → numerically far vectors (cosine distance ≈ 1)
     *
     * @param text  Product name + description + tags concatenated together
     * @return      float[] of exactly 384 numbers (the model's output dimensionality)
     */
    public float[] generateEmbedding(String text) {

        // STEP 1: Build the HTTP request headers
        // HuggingFace also uses "Authorization: Bearer hf_xxx" — same OAuth2 pattern as OpenAI
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        // STEP 2: Build the request body
        // HuggingFace feature-extraction pipeline expects: { "inputs": "text here" }
        // This is different from OpenAI which used "input" (without the 's')
        Map<String, Object> requestBody = Map.of(
                "inputs", text,
                // Wait for model to load if it was sleeping (cold start can take 20s)
                // This prevents a 503 error on the first call after inactivity
                "options", Map.of("wait_for_model", true)
        );

        log.info("Calling HuggingFace Embeddings API for text: '{}'",
                text.substring(0, Math.min(60, text.length())));

        // STEP 3: Make the HTTP POST call
        // We substitute {model} with our model name from application.yml
        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

        // CONCEPT: HuggingFace returns a LIST of embeddings (one per input sentence).
        // Since we send just one string, the response is:
        //   [[0.021, -0.455, 0.891, ...]]   ← outer list = batch, inner list = vector
        // We use List.class and cast ourselves — simple and dependency-free.
        @SuppressWarnings("unchecked")
        ResponseEntity<List> response = restTemplate.exchange(
                HF_API_URL,
                HttpMethod.POST,
                requestEntity,
                List.class,
                model  // substituted into {model} in the URL
        );

        if (response.getBody() == null || response.getBody().isEmpty()) {
            throw new RuntimeException("Empty response from HuggingFace Embeddings API");
        }

        // STEP 4: Parse the nested list response
        // The outer list is the batch → get first (index 0) element = our vector
        Object firstElement = response.getBody().get(0);

        List<Double> embeddingValues;

        // CONCEPT: HuggingFace can return either:
        // - List<List<Double>> for sentence-transformers models (batch mode)
        // - List<Double> if the model returns a flat vector directly
        if (firstElement instanceof List) {
            // sentence-transformers format: [[0.021, -0.455, ...]]
            embeddingValues = (List<Double>) firstElement;
        } else {
            // flat format: [0.021, -0.455, ...]
            embeddingValues = (List<Double>) response.getBody();
        }

        // STEP 5: Convert List<Double> → float[]
        // We need float[] (4 bytes each) not double[] (8 bytes each).
        // 384 floats = 1.5KB per embedding — very efficient.
        float[] floatVector = new float[embeddingValues.size()];
        for (int i = 0; i < embeddingValues.size(); i++) {
            floatVector[i] = embeddingValues.get(i).floatValue();
        }

        log.info("✅ Generated embedding vector of size {} successfully", floatVector.length);
        return floatVector;
    }
}
