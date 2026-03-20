package com.manish.smartcart.model.cart;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

// 1. @RedisHash tells Spring to store this object as a Redis Hash.
// 2. The key in Redis will be "GuestCart:<sessionId>"

@Data
@NoArgsConstructor
@AllArgsConstructor
@RedisHash("GuestCart")
public class GuestCart implements Serializable {

    @Id // The frontend generated UUID (e.g., "guest-1234-abcd")
    private String sessionId;

    private List<GuestCartItem> items = new ArrayList<>();

    // Extremely important: This deletes the cart from Redis automatically after 7 days!
    // Real companies do exactly this to prevent memory leaks from millions of anonymous shoppers.
    @TimeToLive(unit = TimeUnit.DAYS)
    private Long timeToLive = 7L;

}
