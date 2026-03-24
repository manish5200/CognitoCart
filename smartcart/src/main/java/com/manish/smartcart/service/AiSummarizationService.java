package com.manish.smartcart.service;

import com.manish.smartcart.model.feedback.Review;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;


@Slf4j
@Service
public class AiSummarizationService{

    @Value("${huggingface.api-key}")
    private String apiKey;

    private final RestTemplate restTemplate = buildPermissiveRestTemplate();

    // facebook/bart-large-cnn is a free, production-quality summarization model
    // It is trained on CNN/DailyMail articles and produces clean paragraph summaries
    private static final String HF_SUMMARIZATION_URL =
            "https://api-inference.huggingface.co/models/facebook/bart-large-cnn";

    private static RestTemplate buildPermissiveRestTemplate(){
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setSupportedMediaTypes(List.of(MediaType.ALL));
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setMessageConverters(List.of(converter));
        return restTemplate;
    }

    /**
     * Accepts Review entities, extracts their comments, calls HuggingFace BART
     * and returns a single paragraph summary. Returns null if the API call fails.
     */
    public String generateSummary(List<Review>reviews){
        List<String>comments = reviews.stream()
                .map(Review::getComment)
                .filter(c -> c != null && !c.isBlank())
                .toList();

        if(comments.isEmpty()){
            log.debug("No valid comments to summarize.😔");
            return null;
        }

        String prompt = "Summarize these product reviews highlighting pros and cons: "
                + String.join(".", comments);

        log.info("Calling HuggingFace BART to summarize {} review comments", comments.size());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey); // same pattern as EmbeddingService

        Map<String, Object> requestBody = Map.of(
                "inputs",prompt,
                "parameters",Map.of(
                        "max_length",130,
                        "min_length",30,
                        "do_sample",false
                ),
                "options", Map.of("wait_for_model",true) // prevents 503 on HuggingFace cold-start
        );
        try{
            // HuggingFace BART returns: [ { "summary_text": "..." } ]
            List<Map<String, String>>response = restTemplate.postForObject(
                    HF_SUMMARIZATION_URL, new HttpEntity<>(requestBody, headers), List.class
            );

            if(response != null && !response.isEmpty()){
                String summary = response.get(0).get("summary_text");
                log.info("Summary generated ({} chars)", summary.length());
                return summary;
            }
            log.warn("HuggingFace returned an empty response.");
        }catch (HttpClientErrorException e) {
            log.error("HuggingFace client error [{}]: {}", e.getStatusCode(), e.getMessage());
        } catch (HttpServerErrorException e) {
            log.error("HuggingFace server error [{}]: {}", e.getStatusCode(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected summarization error: {}", e.getMessage(), e);
        }
        return null;
    }
}
