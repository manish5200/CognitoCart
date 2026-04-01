package com.manish.smartcart.controller;

import com.manish.smartcart.dto.cart.CartRequest;
import com.manish.smartcart.model.cart.GuestCart;
import com.manish.smartcart.service.GuestCartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/v1/guest-cart")
@RequiredArgsConstructor
@Tag(name = "Guest Cart Management", description = "Redis-backed temporary cart for anonymous users")
public class GuestCartController {

    private final GuestCartService guestCartService;


    @Operation(summary = "Get guest cart", description = "Retrieves the temporary Redis cart using the session ID")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved or initialized guest cart")
    @GetMapping("/{sessionId}")
    public ResponseEntity<GuestCart> getCart(
            @PathVariable String sessionId){
        return ResponseEntity.ok(guestCartService.getCart(sessionId));
    }

    @Operation(summary = "Add item to guest cart", description = "Adds or updates an item quantity in the Redis cart")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Item added to guest cart"),
        @ApiResponse(responseCode = "404", description = "Product not found"),
        @ApiResponse(responseCode = "400", description = "Invalid quantity")
    })
    @PostMapping("/{sessionId}/add")
    public ResponseEntity<GuestCart> addItem(
            @PathVariable String sessionId,
            @RequestBody @Valid CartRequest cartRequest) {

        GuestCart cart = guestCartService.addItem(sessionId, cartRequest.getProductId(), cartRequest.getQuantity());
        return ResponseEntity.ok(cart);
    }

    @Operation(summary = "Remove item from guest cart")
    @ApiResponse(responseCode = "200", description = "Item removed successfully")
    @DeleteMapping("/{sessionId}/item/{productId}")
    public ResponseEntity<GuestCart> removeItem(
            @PathVariable String sessionId,
            @PathVariable Long productId) {

        GuestCart cart = guestCartService.removeItem(sessionId, productId);
        return ResponseEntity.ok(cart);
    }


    @Operation(summary = "Clear the entire guest cart")
    @ApiResponse(responseCode = "200", description = "Cart cleared successfully")
    @DeleteMapping("/{sessionId}/clear")
    public ResponseEntity<String> clearCart(@PathVariable String sessionId) {
        guestCartService.deleteCart(sessionId);
        return ResponseEntity.ok("Guest cart cleared successfully");
    }

}
