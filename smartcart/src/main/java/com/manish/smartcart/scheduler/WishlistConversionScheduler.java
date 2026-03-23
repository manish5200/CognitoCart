package com.manish.smartcart.scheduler;

import com.manish.smartcart.model.user.Users;
import com.manish.smartcart.model.user.Wishlist;
import com.manish.smartcart.repository.WishlistRepository;
import com.manish.smartcart.service.EmailService;
import com.manish.smartcart.service.email.EmailTemplateBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WishlistConversionScheduler {

    private final WishlistRepository wishlistRepository;
    private final EmailService emailService;
    private final EmailTemplateBuilder emailTemplateBuilder;

    // Run every 10 seconds for testing purposes
    //@Scheduled(fixedRate = 10000)

    //Runs at 2AM
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void processWishlistPriceDrops() {
        // Calculate the exact database time for 14 days in the past
        LocalDateTime cooldownCutoff = LocalDateTime.now().minusDays(14);

        // Trigger the Master Algorithm
        List<Wishlist> eligibleWishlists = wishlistRepository.findEligibleSalesForNotification(cooldownCutoff);

        log.info("Wishlist Scheduler heartbeat! Checking for sales... Found {} eligible Wishlist items.", eligibleWishlists.size());

        if (eligibleWishlists.isEmpty()) {
            return; // Exit after logging the heartbeat
        }

        // Group by User so we send ONE email per user containing all their specific sale products
        Map<Long, List<Wishlist>> wishlistsByUser = eligibleWishlists.stream()
                .collect(Collectors.groupingBy(w -> w.getUser().getId()));

        wishlistsByUser.forEach((userId, userWishlists) -> {
            try {
                // Snag the general user info from index 0
                Users user = userWishlists.get(0).getUser();
                // Map the inner products
                var products = userWishlists.stream().map(Wishlist::getProduct).toList();

                // Generate the premium FOMO HTML Native to CognitoCart!
                String body = emailTemplateBuilder.buildWishlistSaleEmail(user.getFullName(), products, "https://cognitocart.com/wishlist");

                // Blast the beautifully styled HTML email
                emailService.sendMail(user.getEmail(), "Massive Price Drop on your Wishlist! 🔥", body, "CognitoCart Marketing");

                // Safely update the timestamp lock so they are safely isolated for the next 14 days
                userWishlists.forEach(w -> w.setLastPriceDropNotifiedAt(LocalDateTime.now()));
                wishlistRepository.saveAll(userWishlists);

                log.info("Successfully sent Price Drop Alert to User {} for {} items.", user.getEmail(), products.size());
            } catch (Exception e) {
                log.error("Failed to send Wishlist alert to User ID {}", userId, e);
            }
        });
    }
}
