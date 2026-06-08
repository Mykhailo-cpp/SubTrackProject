package com.subtrack.dto;

import com.subtrack.entity.Category;

/**
 * Response payload representing a category returned to the client.
 *
 * <p>Exposes only the safe, presentational fields of a {@link Category} and
 * deliberately omits the {@code subscriptions} association to avoid leaking
 * unrelated data and to prevent serialisation cycles.</p>
 */
public class CategoryResponse {

    /** The category's unique identifier. */
    private Long id;

    /** The category name. */
    private String name;

    /** The category description, possibly {@code null}. */
    private String description;

    /** Default constructor. */
    public CategoryResponse() {
    }

    /**
     * Creates a response with the given values.
     *
     * @param id          the category id
     * @param name        the category name
     * @param description the description
     */
    public CategoryResponse(Long id, String name, String description) {
        this.id = id;
        this.name = name;
        this.description = description;
    }

    /**
     * Maps a {@link Category} entity to a response DTO.
     *
     * @param category the entity to convert
     * @return a populated {@link CategoryResponse}
     */
    public static CategoryResponse fromEntity(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription()
        );
    }

    /**
     * Returns the category id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the category id.
     *
     * @param id the id
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Returns the category name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the category name.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns the description.
     *
     * @return the description, possibly {@code null}
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets the description.
     *
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }
}