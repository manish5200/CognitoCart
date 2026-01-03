package com.manish.smartcart.util;

public class AppConstants {
    // --- Pagination Defaults ---
    public static final String DEFAULT_PAGE_NUMBER = "0";
    public static final String DEFAULT_PAGE_SIZE = "10";
    public static final String DEFAULT_SORT_BY = "id";
    public static final String DEFAULT_SORT_DIRECTION = "asc";

    // --- Security & JWT ---
    public static final String AUTH_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    // --- Business Logic Constants ---
    public static final int LOW_STOCK_THRESHOLD = 5;
    public static final double MAXIMUM_COUPON_DISCOUNT = 100.0;
    public static final double MINIMUM_COUPON_DISCOUNT = 0.0;

    // --- Review & Rating ---
    public static final double INITIAL_RATING = 0.0;
    public static final int INITIAL_REVIEW_COUNT = 0;

    // --- File Storage (If you add image uploads later) ---
    public static final String PRODUCT_IMAGE_PATH = "uploads/products/";

    // --- Wishlist & Cart Management ---
    public static final String PRODUCT_QUANTITY = "1";

    private AppConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
