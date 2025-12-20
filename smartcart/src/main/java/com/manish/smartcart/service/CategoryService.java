package com.manish.smartcart.service;

import com.manish.smartcart.model.product.Category;
import com.manish.smartcart.repository.CategoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;



    @Transactional
    public Category createCategory(Category category){
        // If a parent ID is provided in the JSON, we link it here
        if (categoryRepository.findBySlug(category.getSlug()).isPresent()) {
            throw new RuntimeException("Category slug already exists!");
        }
        if(category.getParentCategory() != null && category.getParentCategory().getId() != null){
               Category parent = categoryRepository.findById(category.getParentCategory().getId())
                       .orElseThrow(()-> new RuntimeException("Parent Category Not Found"));
               category.setParentCategory(parent);
        }
        return categoryRepository.save(category);
    }
  //Add in bulk
    @Transactional
    public List<Category> createCategoriesBulk(List<Category> categories) {
        List<Category> savedCategories = new ArrayList<>();
        for (Category category : categories) {
            // Resolve parent if ID is provided
            if (category.getParentCategory() != null && category.getParentCategory().getId() != null) {
                Category parent = categoryRepository.findById(category.getParentCategory().getId())
                        .orElseThrow(() -> new RuntimeException("Parent ID " + category.getParentCategory().getId() + " not found"));
                category.setParentCategory(parent);
            }
            savedCategories.add(categoryRepository.save(category));
        }
        return savedCategories;
    }

    //List all categories

    public List<Category> getAllCategories(){
        return categoryRepository.findAll();
    }

    // Recursive method to get all child IDs
    public List<Long> getAllChildCategoryIds(Long parentId){
        // Start recursion with an empty 'visited' set to prevent loops
         return getAllChildCategoryIdsRecursive(parentId,new HashSet<>());
    }

    //Recursive helper

    public List<Long>getAllChildCategoryIdsRecursive(Long currentId,Set<Long>visited){
        List<Long>allIds=new ArrayList<>();

        // 1. BASE CASE / SAFETY CHECK
        // If we have already processed this ID in the current chain, STOP.
        if (visited.contains(currentId)) {
            return allIds;
        }
        // 2. MARK AS VISITED
        visited.add(currentId);
        allIds.add(currentId);

        //3. RECURSIVE STEP
        // Fetch direct children
        List<Category> children = categoryRepository.findByParentCategoryId(currentId);
        for (Category child : children) {
            // Pass the 'visited' set down to child calls
            allIds.addAll(getAllChildCategoryIdsRecursive(child.getId(), visited));
        }
        return allIds;

    }

}
