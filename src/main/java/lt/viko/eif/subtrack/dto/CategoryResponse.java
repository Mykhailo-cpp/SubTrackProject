package lt.viko.eif.subtrack.dto;

/**
 * Response payload representing a category returned to the client.
 *
 * <p>Exposes only the safe, presentational fields of a category and
 * deliberately omits the {@code subscriptions} association to avoid leaking
 * unrelated data and to prevent serialisation cycles. Mapping from the entity
 * is performed by {@code com.subtrack.mapper.CategoryMapper}.</p>
 *
 * @param id          the category's unique identifier
 * @param name        the category name
 * @param description the optional description, possibly {@code null}
 */
public record CategoryResponse(Long id, String name, String description) {}