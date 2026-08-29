package com.manish.smartcart.product.dto;

import lombok.*;

import java.util.List;

/**
 * Wraps the AI search results with extra metadata.
 * Instead of just returning a raw list, we now return:
 *  - The products list
 *  - The query that was searched
 *  - Total count found
 *  - Each product's relevance score (how closely it matched the query)
 */
@Setter
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemanticSearchResponse {

    // The original query the user typed (e.g. "wireless headphones for gym")
    private String query;

    // How many products were found
    private int totalFound;

    // The ranked list of products with their relevance scores
    private List<RankedProduct> results;


    /**
     * A product + its AI relevance score bundled together.
     * relevanceScore ranges from 0.0 (no match) to 1.0 (perfect match).
     * We convert cosine DISTANCE → similarity: similarity = 1 - distance
     */
    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RankedProduct {

        // The full product details (same as normal product response)
        private ProductResponse product;

        // How relevant this product is (0.0 = irrelevant, 1.0 = perfect match)
        // Formula: 1 - cosineDistance (cosine distance is 0=identical, 2=opposite)
        private double relevanceScore;

        // Human-readable percentage, e.g. "94%"
        private String relevanceLabel;

        // Position in the ranked list (1 = best match)
        private int rank;
    }
}
