package com.manish.smartcart.controller;

import com.manish.smartcart.model.product.Category;
import com.manish.smartcart.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController{

    @Autowired
    private CategoryService  categoryService;

    /**
     * POST: Only Admins can create new categories
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?>addCategory(@RequestBody Category category) {
        return ResponseEntity.status(HttpStatus.CREATED)
                      .body(categoryService.createCategory(category));
    }
    /**
     * POST: Only Admins can create new categories in bulk
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?>addCategoryBulk(@RequestBody List<Category> categories) {
        return ResponseEntity.status(HttpStatus.CREATED)
                    .body(categoryService.createCategoriesBulk(categories));
    }

    /**
     * GET: Public access so sellers/customers can see categories
     */

    @GetMapping
    public ResponseEntity<?> getCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }
}
