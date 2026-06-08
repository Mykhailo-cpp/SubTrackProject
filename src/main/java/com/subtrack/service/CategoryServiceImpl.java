package com.subtrack.service;

import com.subtrack.dto.CategoryRequest;
import com.subtrack.dto.CategoryResponse;
import com.subtrack.entity.Category;
import com.subtrack.exception.DuplicateResourceException;
import com.subtrack.exception.ResourceNotFoundException;
import com.subtrack.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Default {@link CategoryService} implementation backed by
 * {@link CategoryRepository}.
 *
 * <p>Read operations run in read-only transactions; mutating operations run
 * in read-write transactions. Name uniqueness is enforced before persisting,
 * raising a {@link DuplicateResourceException} (HTTP 409) on conflict, and
 * missing categories raise a {@link ResourceNotFoundException} (HTTP 404).</p>
 */
@Service
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    /** Repository providing persistence operations for categories. */
    private final CategoryRepository categoryRepository;

    /**
     * Creates the service with its required repository.
     *
     * @param categoryRepository the category repository
     */
    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    /** {@inheritDoc} */
    @Override
    public List<CategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(CategoryResponse::fromEntity)
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    public CategoryResponse getCategoryById(Long id) {
        Category category = findCategoryOrThrow(id);
        return CategoryResponse.fromEntity(category);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException(
                    "Category already exists with name: " + request.getName());
        }
        Category category = new Category(request.getName(), request.getDescription());
        Category saved = categoryRepository.save(category);
        return CategoryResponse.fromEntity(saved);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CategoryRequest request) {
        Category category = findCategoryOrThrow(id);

        // Only check for a name clash when the name is actually changing,
        // so that re-saving a category under its own name is allowed.
        if (!category.getName().equals(request.getName())
                && categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException(
                    "Category already exists with name: " + request.getName());
        }

        category.setName(request.getName());
        category.setDescription(request.getDescription());
        Category saved = categoryRepository.save(category);
        return CategoryResponse.fromEntity(saved);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = findCategoryOrThrow(id);
        categoryRepository.delete(category);
    }

    /**
     * Loads a category by id or throws if it does not exist.
     *
     * @param id the category id
     * @return the found category
     * @throws ResourceNotFoundException if no category has the given id
     */
    private Category findCategoryOrThrow(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Category", id));
    }
}