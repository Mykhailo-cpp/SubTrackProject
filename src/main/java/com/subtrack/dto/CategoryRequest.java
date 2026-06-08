package com.subtrack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for creating or updating a category.
 *
 * <p>Used as the {@code @RequestBody} for {@code POST} and {@code PUT}
 * requests on {@code /api/categories}. The {@code id} is never accepted from
 * the client; it is assigned by the database on creation and taken from the
 * path on update.</p>
 */
public class CategoryRequest {

    /** Unique, human-readable category name. Must not be blank. */
    @NotBlank(message = "Category name is required")
    @Size(max = 255, message = "Category name must not exceed 255 characters")
    private String name;

    /** Optional free-text description of the category. */
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    /** Default constructor required for JSON deserialisation. */
    public CategoryRequest() {
    }

    /**
     * Creates a request with the given values.
     *
     * @param name        the category name
     * @param description the optional description
     */
    public CategoryRequest(String name, String description) {
        this.name = name;
        this.description = description;
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