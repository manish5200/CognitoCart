package com.manish.smartcart.controller;

import com.manish.smartcart.model.product.Category;
import com.manish.smartcart.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

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
          try{
              return ResponseEntity.status(HttpStatus.CREATED)
                      .body(categoryService.createCategory(category));
          }catch(Exception e){
              return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
          }
    }
    /**
     * POST: Only Admins can create new categories in bulk
     */
    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?>addCategoryBulk(@RequestBody List<Category> categories) {
        try{
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(categoryService.createCategoriesBulk(categories));
        }catch(Exception e){
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * GET: Public access so sellers/customers can see categories
     */

    @GetMapping
    public ResponseEntity<?> getCategories() {
         try{
             return ResponseEntity.ok(categoryService.getAllCategories());
         } catch (Exception e) {
             return ResponseEntity.badRequest().body(Map.of("error",e.getMessage()));
         }
    }
}
