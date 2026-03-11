package com.manish.smartcart.repository;

import com.manish.smartcart.model.product.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {
        // JpaSpecificationExecutor adds the .findAll(Specification, Pageable) method!

        List<Product> findByCategory_Id(Long categoryId);

        Optional<Product> findBySlug(String slug);

        // FIX: Use 'category.id' instead of 'categoryId'
        // -> because its transient in category
        @Query("SELECT p FROM Product p WHERE p.category.id IN :categoryIds")
        List<Product> findByCategoryIdIn(@Param("categoryIds") List<Long> categoryIds);

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
}
