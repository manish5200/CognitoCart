package com.manish.smartcart.controller;

import com.manish.smartcart.model.product.Category;
import com.manish.smartcart.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Category Management", description = "Manage and browse product categories")
public class CategoryController {

    private final CategoryService categoryService;


    /**
     * POST: Only Admins can create new categories
     */
    @Operation(summary = "Add single category", description = "Admin only. Creates a new product category.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Category created successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addCategory(@RequestBody Category category) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.createCategory(category));
    }

    /**
     * POST: Only Admins can create new categories in bulk
     */
    @Operation(summary = "Add categories in bulk", description = "Admin only. Creates multiple categories in a single request.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Categories created successfully"),
        @ApiResponse(responseCode = "403", description = "Forbidden - Admin access required")
    })
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addCategoryBulk(@RequestBody List<Category> categories) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoryService.createCategoriesBulk(categories));
    }

    /**
     * GET: Public access so sellers/customers can see categories
     */
    @Operation(summary = "Get all categories", description = "Public access to view the full category hierarchy.")
    @ApiResponse(responseCode = "200", description = "Successfully retrieved all categories")
    @GetMapping
    public ResponseEntity<?> getCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }
}
