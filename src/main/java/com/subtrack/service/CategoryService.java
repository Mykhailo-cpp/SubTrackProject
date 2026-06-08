package com.subtrack.service;

import com.subtrack.dto.CategoryRequest;
import com.subtrack.dto.CategoryResponse;

import java.util.List;

/**
 * Business operations for managing categories.
 *
 * <p>Categories are shared reference data across all users, so these
 * operations are not scoped to any particular user. Implementations are
 * responsible for enforcing name uniqueness and for translating missing or
 * conflicting resources into the appropriate exceptions.</p>
 */
public interface CategoryService {

    /**
     * Returns all categories.
     *
     * @return a list of all categories; empty if none exist
     */
    List<CategoryResponse> getAllCategories();

    /**
     * Returns a single category by its identifier.
     *
     * @param id the category id
     * @return the matching category
     * @throws com.subtrack.exception.ResourceNotFoundException if no category has the given id
     */
    CategoryResponse getCategoryById(Long id);

    /**
     * Creates a new category.
     *
     * @param request the category details
     * @return the created category, including its generated id
     * @throws com.subtrack.exception.DuplicateResourceException if a category with the same name already exists
     */
    CategoryResponse createCategory(CategoryRequest request);

    /**
     * Updates an existing category.
     *
     * @param id      the id of the category to update
     * @param request the new category details
     * @return the updated category
     * @throws com.subtrack.exception.ResourceNotFoundException  if no category has the given id
     * @throws com.subtrack.exception.DuplicateResourceException if renaming would collide with another category's name
     */
    CategoryResponse updateCategory(Long id, CategoryRequest request);

    /**
     * Deletes a category by its identifier.
     *
     * @param id the id of the category to delete
     * @throws com.subtrack.exception.ResourceNotFoundException if no category has the given id
     */
    void deleteCategory(Long id);
}