package lt.viko.eif.subtrack.repository;

import lt.viko.eif.subtrack.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Spring Data JPA repository for {@link Category} entities.
 *
 * <p>Provides standard CRUD operations inherited from {@link JpaRepository}
 * plus a lookup and existence check keyed on the unique category name.</p>
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * Finds a category by its unique name.
     *
     * @param name the category name to search for
     * @return an {@link Optional} containing the matching category, or empty if none exists
     */
    Optional<Category> findByName(String name);

    /**
     * Checks whether a category with the given name already exists.
     *
     * @param name the category name to check
     * @return {@code true} if a category with that name exists, otherwise {@code false}
     */
    boolean existsByName(String name);
}