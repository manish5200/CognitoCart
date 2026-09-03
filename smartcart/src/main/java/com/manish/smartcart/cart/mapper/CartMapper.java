package com.manish.smartcart.cart.mapper;

import com.manish.smartcart.cart.dto.CartResponse;
import com.manish.smartcart.cart.model.Cart;
import com.manish.smartcart.cart.model.CartItem;
import com.manish.smartcart.infrastructure.returnpolicy.ReturnPolicyService;
import com.manish.smartcart.order.dto.PolicySnapshot;
import com.manish.smartcart.shared.enums.product.PolicyType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CartMapper {

    private ReturnPolicyService returnPolicyService;

    public CartResponse toCartResponse(Cart updatedCart) {
        List<CartResponse.ItemDTO> items = new ArrayList<>();
        for (CartItem item : updatedCart.getItems()) {
            String productName = (item.getVariant() != null && item.getVariant().getProduct() != null)
                    ? item.getVariant().getProduct().getProductName()
                    : "Unknown Product";
            String imageUrl = null;
            String variantInfo = "Standard";
            java.util.UUID variantPublicId = null;
            java.util.UUID productPublicId = null;
            
            PolicyType policyType = null;
            Integer returnWindowDays = null;

            if (item.getVariant() != null) {
                variantPublicId = item.getVariant().getPublicId();
                variantInfo = item.getVariant().getDisplayLabel();
                if (item.getVariant().getVariantImageUrl() != null) {
                    imageUrl = item.getVariant().getVariantImageUrl();
                } else if (item.getVariant().getProduct() != null && item.getVariant().getProduct().getPrimaryImageUrl() != null) {
                    imageUrl = item.getVariant().getProduct().getPrimaryImageUrl();
                }
                
                if (item.getVariant().getProduct() != null) {
                    productPublicId = item.getVariant().getProduct().getPublicId();
                    try {
                        PolicySnapshot snapshot = returnPolicyService.getPolicySnapshotForCheckout(item.getVariant().getProduct());
                        policyType = snapshot.getPolicyType();
                        returnWindowDays = snapshot.getReturnWindowDays();
                    } catch (Exception e) {
                        // fallback
                    }
                }
            }
            CartResponse.ItemDTO newItem = new CartResponse.ItemDTO(
                    productName,
                    item.getPriceAtAdding(),
                    item.getQuantity(),
                    item.getPriceAtAdding().multiply(new BigDecimal(item.getQuantity())),
                    variantPublicId,
                    productPublicId,
                    imageUrl,
                    variantInfo,
                    policyType,
                    returnWindowDays
            );
            items.add(newItem);
        }
        return new CartResponse(
                updatedCart.getId(),
                updatedCart.getTotalAmount(),
                updatedCart.getCouponCode(),
                updatedCart.getDiscountAmount(),
                updatedCart.getDeliveryFee(),
                items);
    }
}
