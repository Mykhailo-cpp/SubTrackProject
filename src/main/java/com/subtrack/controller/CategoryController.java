package com.subtrack.controller;

import com.subtrack.dto.CategoryRequest;
import com.subtrack.dto.CategoryResponse;
import com.subtrack.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * REST controller exposing CRUD endpoints for categories under
 * {@code /api/categories}.
 *
 * <p>Categories are shared across all users, so these endpoints are not
 * user-scoped. Business rules — name uniqueness and existence checks — are
 * delegated to {@link CategoryService}, which raises the exceptions that map
 * to HTTP 409 (Conflict) and 404 (Not Found) respectively.</p>
 */
@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    /** Service encapsulating category business logic. */
    private final CategoryService categoryService;

    /**
     * Creates the controller with its required service.
     *
     * @param categoryService the category service
     */
    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Returns all categories.
     *
     * @return HTTP 200 with the list of categories
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {
        return ResponseEntity.ok(categoryService.getAllCategories());
    }

    /**
     * Returns a single category by id.
     *
     * @param id the category id
     * @return HTTP 200 with the category; HTTP 404 if it does not exist
     */
    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {
        return ResponseEntity.ok(categoryService.getCategoryById(id));
    }

    /**
     * Creates a new category.
     *
     * @param request the validated category details
     * @return HTTP 201 with the created category and a {@code Location} header;
     *         HTTP 409 if a category with the same name already exists
     */
    @PostMapping
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CategoryRequest request) {
        CategoryResponse created = categoryService.createCategory(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    /**
     * Updates an existing category.
     *
     * @param id      the id of the category to update
     * @param request the validated new category details
     * @return HTTP 200 with the updated category; HTTP 404 if it does not exist;
     *         HTTP 409 if the new name collides with another category
     */
    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    /**
     * Deletes a category by id.
     *
     * @param id the id of the category to delete
     * @return HTTP 204 (No Content); HTTP 404 if it does not exist
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}