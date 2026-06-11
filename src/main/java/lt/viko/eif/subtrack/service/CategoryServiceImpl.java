package lt.viko.eif.subtrack.service;

import lt.viko.eif.subtrack.dto.CategoryRequest;
import lt.viko.eif.subtrack.dto.CategoryResponse;
import lt.viko.eif.subtrack.entity.Category;
import lt.viko.eif.subtrack.exception.DuplicateResourceException;
import lt.viko.eif.subtrack.exception.ResourceNotFoundException;
import lt.viko.eif.subtrack.mapper.CategoryMapper;
import lt.viko.eif.subtrack.repository.CategoryRepository;
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
 * missing categories raise a {@link ResourceNotFoundException} (HTTP 404).
 * Entity/DTO conversion is delegated to {@link CategoryMapper}.</p>
 */
@Service
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    /** Repository providing persistence operations for categories. */
    private final CategoryRepository categoryRepository;

    /** Mapper converting between category entities and DTOs. */
    private final CategoryMapper categoryMapper;

    /**
     * Creates the service with its required collaborators.
     *
     * @param categoryRepository the category repository
     * @param categoryMapper     the category mapper
     */
    public CategoryServiceImpl(CategoryRepository categoryRepository,
                               CategoryMapper categoryMapper) {
        this.categoryRepository = categoryRepository;
        this.categoryMapper = categoryMapper;
    }

    /** {@inheritDoc} */
    @Override
    public List<CategoryResponse> getAllCategories() {
        return categoryMapper.toResponseList(categoryRepository.findAll());
    }

    /** {@inheritDoc} */
    @Override
    public CategoryResponse getCategoryById(Long id) {
        return categoryMapper.toResponse(findCategoryOrThrow(id));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new DuplicateResourceException(
                    "Category already exists with name: " + request.getName());
        }
        Category category = categoryMapper.toEntity(request);
        Category saved = categoryRepository.save(category);
        return categoryMapper.toResponse(saved);
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

        categoryMapper.updateEntity(request, category);
        Category saved = categoryRepository.save(category);
        return categoryMapper.toResponse(saved);
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