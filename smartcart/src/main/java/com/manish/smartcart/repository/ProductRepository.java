package com.manish.smartcart.repository;

import com.manish.smartcart.model.product.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
        // JpaSpecificationExecutor adds the .findAll(Specification, Pageable) method!

        List<Product> findByCategory_Id(Long categoryId);

        Optional<Product> findBySlug(String slug);

        /**
         * Fetches a product row with a PESSIMISTIC_WRITE lock (SELECT ... FOR UPDATE).
         * Used exclusively during checkout to prevent double-selling when two customers
         * attempt to buy the last unit simultaneously. The second concurrent transaction
         * BLOCKS at this line until the first commits its stock decrement.
         *
         * ⚠️ Only call from within a @Transactional method — the lock is released on commit.
         */
        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("SELECT p FROM Product p WHERE p.id = :id")
        Optional<Product> findByIdForUpdate(@Param("id") Long id);


        // FIX: Use 'category.id' instead of 'categoryId'
        // -> because its transient in category
        @Query("SELECT p FROM Product p WHERE p.category.id IN :categoryIds")
        Page<Product> findByCategoryIdIn(@Param("categoryIds") List<Long> categoryIds, Pageable pageable);

        // Returns the full list of products that need restocking
        List<Product> findByStockQuantityLessThan(int threshold);

        // Get Top Selling Products (Custom JPQL) — Admin, global
        @Query("SELECT i.product, SUM(i.quantity) as totalSold " +
                        "from OrderItem i " +
                        "join i.order o " + // We 'Join' the Order table to check its status
                        "WHERE o.orderStatus = 'DELIVERED' " + // The strictest, most accurate filter
                        "GROUP BY i.product " +
                        "ORDER BY totalSold DESC")
        List<Object[]> findToSellingProducts(Pageable pageable);

        // ── Seller Dashboard Queries ─────────────────────────────────────────

        /**
         * Total active products owned by this seller (@SoftDelete auto-filters deleted
         * ones)
         */
        @Query("SELECT COUNT(p) FROM Product p WHERE p.sellerId = :sellerId")
        long countBySellerIdAndNotDeleted(@Param("sellerId") Long sellerId);

        /** Products that are live (available = true) */
        @Query("SELECT COUNT(p) FROM Product p WHERE p.sellerId = :sellerId AND p.isAvailable = true")
        long countAvailableBySellerIdAndNotDeleted(@Param("sellerId") Long sellerId);

        /** Products that are out of stock */
        @Query("SELECT COUNT(p) FROM Product p WHERE p.sellerId = :sellerId AND p.stockQuantity = 0")
        long countOutOfStockBySellerId(@Param("sellerId") Long sellerId);

        /** Top N products for this seller by units sold (DELIVERED orders only) */
        @Query("SELECT i.product, SUM(i.quantity) as totalSold, SUM(i.priceAtPurchase * i.quantity) as revenue " +
                        "FROM OrderItem i " +
                        "JOIN i.order o " +
                        "WHERE i.product.sellerId = :sellerId AND o.orderStatus = 'DELIVERED' " +
                        "GROUP BY i.product " +
                        "ORDER BY totalSold DESC")
        List<Object[]> findTopProductsBySellerId(@Param("sellerId") Long sellerId, Pageable pageable);

        /**
         * Semantic Vector Search using PostgreSQL's cosine distance operator (<=>).
         *
         * CONCEPT: This is NOT a normal JPA query. It is a @NativeQuery because
         * pgvector's <=> operator is PostgreSQL-specific syntax that JPQL doesn't know.
         *
         * How it works:
         *   embedding <=> CAST(:queryVector AS vector)
         *   This calculates the COSINE DISTANCE between each stored product vector
         *   and our search query vector. Think of it like measuring the "angle"
         *   between two arrows in 1536-dimensional space.
         *
         * Distance 0.0 = identical meaning (perfect match)
         * Distance 1.0 = completely unrelated
         * Distance 2.0 = exact opposites
         *
         * ORDER BY distance ASC = most similar products come first.
         * WHERE embedding IS NOT NULL = skip products not yet indexed.
         *
         * @param queryVector  The search query converted to float[] by EmbeddingService
         * @param limit        How many results to return (e.g., top 10)
         */
        @Query(
                value = "SELECT * FROM products " +
                        "WHERE embedding IS NOT NULL AND is_deleted = false " +
                        "ORDER BY embedding <=> CAST(:queryVector AS vector) " +
                        "LIMIT :limit",
                nativeQuery = true
        )
        List<Product> findBySimilarity(
                @Param("queryVector") String queryVector,
                @Param("limit") int limit
        );

        /**
         * Updates the embedding column for a product using a native SQL CAST.
         *
         * CONCEPT: We CANNOT set a vector column via normal JPA save() because Hibernate
         * binds String parameters as VARCHAR, and PostgreSQL rejects VARCHAR → vector.
         * The only reliable way is a native UPDATE with an explicit CAST(:value AS vector),
         * which tells PostgreSQL: "trust me, this string IS a valid vector literal".
         *
         * @Modifying means this query mutates data (not a SELECT).
         * @Transactional is required for any @Modifying query.
         *
         * The vectorString format must be: "[0.021,-0.455,0.891,...]"
         * Our VectorAttributeConverter.convertToDatabaseColumn(float[]) produces exactly that.
         */
        @jakarta.transaction.Transactional
        @org.springframework.data.jpa.repository.Modifying
        @Query(
                value = "UPDATE products SET embedding = CAST(:vectorString AS vector) WHERE id = :productId",
                nativeQuery = true
        )
        void updateEmbedding(@Param("productId") Long productId, @Param("vectorString") String vectorString);

        /**
         * Fetches ALL products with their reviews in a SINGLE SQL JOIN query.
         *
         * CONCEPT: JOIN FETCH tells Hibernate: "load the reviews collection eagerly
         * as part of THIS query" — no second SQL per product (eliminates N+1).
         *
         * DISTINCT is required because a JOIN on a one-to-many relationship produces
         * duplicate Product rows in the result set (one per review). DISTINCT
         * de-duplicates them at the Hibernate level before returning to Java.
         *
         * Used exclusively by the AI Review Summarization scheduler.
         */
        @Query("SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.reviews")
        List<Product> findAllWithReviews();
}
