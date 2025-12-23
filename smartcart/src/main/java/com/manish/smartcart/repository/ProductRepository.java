package com.manish.smartcart.repository;

import com.manish.smartcart.model.product.Product;
import org.hibernate.query.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Optional;

@Repository
public interface   ProductRepository extends JpaRepository<Product,Long> {

    List<Product> findByCategoryId(Long categoryId);

    Optional<Product> findBySlug(String slug);

    // FIX: Use 'category.id' instead of 'categoryId' -> because its transient in category
    @Query("SELECT p FROM Product p WHERE p.category.id IN :categoryIds")
    List<Product> findByCategoryIdIn(@Param("categoryIds") List<Long> categoryIds);


    // Returns the full list of products that need restocking
    List<Product> findByStockQuantityLessThan(int threshold);

    // Get Top Selling Products (Custom JPQL)
    @Query("SELECT i.product, SUM(i.quantity) as totalSold " +
            "from OrderItem i " +
            "join i.order o "+ // We 'Join' the Order table to check its status
            "WHERE o.orderStatus = 'DELIVERED' "+ // The strictest, most accurate filter
            "GROUP BY i.product " +
            "ORDER BY totalSold DESC")
    List<Object[]>findToSellingProducts(Pageable pageable);


}
