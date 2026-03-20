package com.manish.smartcart.scheduler;

import com.manish.smartcart.model.cart.Cart;
import com.manish.smartcart.repository.CartRepository;
import com.manish.smartcart.service.EmailService;
import com.manish.smartcart.service.email.EmailTemplateBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CartAbandonmentJob {

    private final CartRepository  cartRepository;
    private final EmailService emailService;
    private final EmailTemplateBuilder  emailTemplateBuilder;

    /*
     * For testing right now, this runs every 30 seconds!
     * In production, you would change this to "0 0 2 * * *" (2:00 AM daily)
     */

    //For Testing - 30 sec == "*/30 * * * * *"
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional(readOnly = true)
    public void scanAndEmailAbandonedCarts(){
        log.info("⏰ Starting Cart Abandonment Job...");

        // Calculate the threshold: 24 hours ago
        LocalDateTime threshold = LocalDateTime.now().minusHours(24);

        // Fetch carts
        List<Cart>abandonedCarts = cartRepository.findAbandonedCarts(threshold);
        log.info("🔍 Found {} abandoned carts.", abandonedCarts.size());

        for(Cart cart : abandonedCarts){
            String userEmail = cart.getUser().getEmail();
            String userName = cart.getUser().getFullName();
            int itemCount = cart.getItems().size();

            // Your frontend URL where they go to pay
            String checkoutUrl = "https://cognitocart.com/cart";

            try{

                // 1. Build the HTML String
                String htmlBody = emailTemplateBuilder.
                        buildCartAbandonmentEmail(userName, itemCount, checkoutUrl);

                // 2. Dispatch via JavaMailSender asynchronously
                emailService.sendMail(
                        userEmail,
                        "Did you forget something in your cart? 🛒",
                        htmlBody,
                        "CognitoCart"
                );

                log.info("📧 Cart Abandonment email successfully dispatched to: {}", userEmail);

            }catch(Exception e){
                log.error("❌ Failed to send cart abandonment email to {}", userEmail, e);
            }
        }
    }
}
