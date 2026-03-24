package com.manish.smartcart.scheduler;

import com.manish.smartcart.model.feedback.Review;
import com.manish.smartcart.model.product.Product;
import com.manish.smartcart.model.product.ProductInsights;
import com.manish.smartcart.repository.ProductInsightsRepository;
import com.manish.smartcart.repository.ProductRepository;
import com.manish.smartcart.service.AiSummarizationService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewSummarizationScheduler{

    private final ProductRepository productRepository;
    private final AiSummarizationService aiSummarizationService;
    private final ProductInsightsRepository productInsightsRepository;


    @Transactional
    @Scheduled(fixedRate = 10000) // for testing purpose
    //@Scheduled(cron = "0 0 3 * * ?")  // every night at 3 AM
    public void generateSummaries(){
        log.info("=== AI Review Summarization Job STARTED ===");
        long start =  System.currentTimeMillis();
        int success = 0 , skipped = 0, failed = 0;

        for(Product product : productRepository.findAllWithReviews()){
            try{
                // findAllWithReviews() uses JOIN FETCH — loads all products AND their reviews
                // in ONE single SQL query. No N+1. Reviews are in memory before the loop starts.
                List<Review> reviews = product.getReviews();
                if(reviews.size() < 3){
                    skipped++;
                    continue;
                }
                String summary = aiSummarizationService.generateSummary(reviews);
                if(summary == null){
                    log.warn("No summary for Product ID {} — retaining previous", product.getId());
                    failed++;
                    continue;
                }
                // Upsert: update existing row, or create a new one
                ProductInsights insights = productInsightsRepository
                        .findByProductId(product.getId())
                        .orElse(ProductInsights.builder().product(product).build());

                insights.setAiSummary(summary);

                // Mirror the denormalized count already maintained by ReviewService
                insights.setTotalReviews((long)product.getTotalReviews());
                insights.setLastGenerated(LocalDateTime.now());
                productInsightsRepository.save(insights);
                log.info("✅ '{}' (ID {}) — insights updated", product.getProductName(), product.getId());
                success++;
            }catch (Exception e){
                // Isolate per-product: one failure should NOT abort the whole batch
                log.error("Failed for Product ID {}: {}", product.getId(), e.getMessage(), e);
                failed++;
            }
        }
        log.info("=== Job FINISHED === [Success={}, Skipped={}, Failed={}, Duration={}ms]",
                success, skipped, failed, System.currentTimeMillis() - start);
    }
}
