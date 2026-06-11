package lt.viko.eif.subtrack.controller;

import lt.viko.eif.subtrack.dto.CategoryRequest;
import lt.viko.eif.subtrack.dto.CategoryResponse;
import lt.viko.eif.subtrack.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
@Tag(name = "Categories", description = "Manage subscription categories (shared across all users)")
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
    @Operation(
            summary = "Get all categories",
            description = "Returns every category available in the system. " +
                    "Categories are shared across all users and are not paginated."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "List of categories returned (may be empty)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            array = @ArraySchema(schema = @Schema(implementation = CategoryResponse.class)),
                            examples = @ExampleObject(value = """
                                    [
                                      { "id": 1, "name": "Streaming" },
                                      { "id": 2, "name": "Software" },
                                      { "id": 3, "name": "Cloud Storage" }
                                    ]""")
                    )
            )
    })
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
    @Operation(
            summary = "Get a category by ID",
            description = "Returns the category with the given `id`."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Category found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CategoryResponse.class),
                            examples = @ExampleObject(value = """
                                    { "id": 1, "name": "Streaming" }""")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "No category with the given ID exists",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    { "error": "Category with id 99 not found" }""")
                    )
            )
    })
    public ResponseEntity<CategoryResponse> getCategoryById(
            @Parameter(description = "ID of the category to retrieve", required = true, example = "1")
            @PathVariable Long id) {
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
    @Operation(
            summary = "Create a new category",
            description = "Creates a category with a unique name. " +
                    "Returns HTTP 201 with a `Location` header pointing to the new resource."
    )
    @RequestBody(
            description = "Category to create. Name must be unique (case-insensitive).",
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CategoryRequest.class),
                    examples = @ExampleObject(value = """
                            { "name": "Gaming" }""")
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Category created — `Location` header points to the new resource",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CategoryResponse.class),
                            examples = @ExampleObject(value = """
                                    { "id": 4, "name": "Gaming" }""")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed — name is blank or missing",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    { "error": "name: must not be blank" }""")
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "A category with this name already exists",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    { "error": "Category 'Gaming' already exists" }""")
                    )
            )
    })
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @org.springframework.web.bind.annotation.RequestBody CategoryRequest request) {
        CategoryResponse created = categoryService.createCategory(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
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
    @Operation(
            summary = "Update a category",
            description = "Replaces the name of the category identified by `id`. " +
                    "The new name must be unique."
    )
    @RequestBody(
            description = "New category name.",
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = CategoryRequest.class),
                    examples = @ExampleObject(value = """
                            { "name": "Entertainment" }""")
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Category updated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CategoryResponse.class),
                            examples = @ExampleObject(value = """
                                    { "id": 1, "name": "Entertainment" }""")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Validation failed",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    { "error": "name: must not be blank" }""")
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Category not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    { "error": "Category with id 99 not found" }""")
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Another category already has this name",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    { "error": "Category 'Entertainment' already exists" }""")
                    )
            )
    })
    public ResponseEntity<CategoryResponse> updateCategory(
            @Parameter(description = "ID of the category to update", required = true, example = "1")
            @PathVariable Long id,
            @Valid @org.springframework.web.bind.annotation.RequestBody CategoryRequest request) {
        return ResponseEntity.ok(categoryService.updateCategory(id, request));
    }

    /**
     * Deletes a category by id.
     *
     * @param id the id of the category to delete
     * @return HTTP 204 (No Content); HTTP 404 if it does not exist
     */
    @DeleteMapping("/{id}")
    @Operation(
            summary = "Delete a category",
            description = "Permanently removes the category identified by `id`. " +
                    "Subscriptions that reference this category may be affected — " +
                    "check your cascade/constraint configuration."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Category deleted successfully"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Category not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    { "error": "Category with id 99 not found" }""")
                    )
            )
    })
    public ResponseEntity<Void> deleteCategory(
            @Parameter(description = "ID of the category to delete", required = true, example = "1")
            @PathVariable Long id) {
        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}