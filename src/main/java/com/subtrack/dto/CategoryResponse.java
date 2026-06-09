package com.subtrack.dto;

/**
 * Response payload representing a category returned to the client.
 *
 * Exposes only the safe, presentational fields of a category and
 * deliberately omits the {@code subscriptions} association to avoid leaking
 * unrelated data and to prevent serialisation cycles. Mapping from the entity
 * is performed by {@code com.subtrack.mapper.CategoryMapper}.
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