package com.manish.smartcart.service;

import com.manish.smartcart.dto.feedback.ReviewRequestDTO;
import com.manish.smartcart.enums.OrderStatus;
import com.manish.smartcart.mapper.ReviewMapper;
import com.manish.smartcart.model.feedback.Review;
import com.manish.smartcart.model.product.Product;
import com.manish.smartcart.model.user.Users;
import com.manish.smartcart.repository.OrderRepository;
import com.manish.smartcart.repository.ProductRepository;
import com.manish.smartcart.repository.ReviewRepository;
import com.manish.smartcart.repository.UsersRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
@AllArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final UsersRepository usersRepository;
    private final ReviewMapper reviewMapper;

    @Transactional
    public Map<String,Object> addOrUpdateReview(Long userId, Long productId, ReviewRequestDTO  reviewRequestDTO) {
        // 1. Verify the product exists
        Product product = productRepository
                .findById(productId)
                .orElseThrow(()-> new RuntimeException("Product not found"));

        // 2. VERIFIED PURCHASE CHECK (Real-World Rule)
        // Ensure user has an order with this product that is 'DELIVERED'
        boolean hasPurchased = orderRepository
                .existsByUserIdAndOrderItems_Product_IdAndOrderStatus(
                        userId, productId, OrderStatus.DELIVERED) || orderRepository
                .existsByUserIdAndOrderItems_Product_IdAndOrderStatus(
                        userId, productId, OrderStatus.RETURNED);

        if(!hasPurchased){
            throw new RuntimeException("You can only review products you have ordered.");
        }

        // 3. THE SMART CHECK: Find existing review or create new
        //Upsert Logic
        Optional<Review>existingReview = reviewRepository.findByProductIdAndUserId(productId, userId);
        String statusMessage;
        Review finalReview;
        if(existingReview.isPresent()){
            Review review = existingReview.get();
            // SUBTRACT the old rating from the average before adding the new one
            updateProductRatingOnEdit(product, review.getRating(), reviewRequestDTO.getRating());
            review.setRating(reviewRequestDTO.getRating());
            review.setComment(reviewRequestDTO.getComment());
            finalReview = reviewRepository.save(review);
            statusMessage = "Review has been updated successfully";
        }else{
            Review review = new Review();
            review.setProduct(product);

            // Link the user! (Using getReferenceById avoids an extra DB Select)
            Users user = usersRepository.getReferenceById(userId);
            review.setUser(user);

            review.setRating(reviewRequestDTO.getRating());
            review.setComment(reviewRequestDTO.getComment());
            finalReview = reviewRepository.save(review);
            updateProductRatingOnNew(product, reviewRequestDTO.getRating());
            statusMessage = "Review added successfully";
        }
        return Map.of("message",statusMessage,"review",reviewMapper.toReviewResponseDTO(finalReview));
    }

    private void updateProductRatingOnEdit(Product product, Integer oldRating, Integer newRating) {
        if (product.getTotalReviews() == 0) return; // Defensive check

        double currentTotalScore = product.getAverageRating()*product.getTotalReviews();
        double newAverageRating = (currentTotalScore + newRating - oldRating) / product.getTotalReviews();
        // Consistent rounding for the UI
        product.setAverageRating(Math.round(newAverageRating * 10.0) / 10.0);
        productRepository.save(product);
    }

    private void updateProductRatingOnNew(Product product, Integer newRating) {
          int newTotalReview = product.getTotalReviews() + 1;
          double currentTotalScore = product.getAverageRating()*product.getTotalReviews();
          double newAverageRating = (currentTotalScore + newRating )/newTotalReview;

          product.setTotalReviews(newTotalReview);
        // Apply rounding here as well
         product.setAverageRating(Math.round(newAverageRating * 10.0) / 10.0);
         productRepository.save(product);
    }

}
