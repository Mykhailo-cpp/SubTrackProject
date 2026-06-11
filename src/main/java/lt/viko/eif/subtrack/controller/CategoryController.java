package lt.viko.eif.subtrack.controller;

import lt.viko.eif.subtrack.dto.CategoryRequest;
import lt.viko.eif.subtrack.dto.CategoryResponse;
import lt.viko.eif.subtrack.service.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.IanaLinkRelations;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * REST controller exposing CRUD endpoints for categories under
 * {@code /api/categories}.
 *
 * <p>Categories are shared across all users, so these endpoints are not
 * user-scoped. Business rules — name uniqueness and existence checks — are
 * delegated to {@link CategoryService}, which raises the exceptions that map
 * to HTTP 409 (Conflict) and 404 (Not Found) respectively. Responses are
 * wrapped in HATEOAS models carrying {@code self} and {@code categories}
 * links, satisfying Richardson Maturity Model Level 3. Link construction is
 * delegated to {@link CategoryModelAssembler}.</p>
 */
@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categories", description = "Manage subscription categories (shared across all users)")
public class CategoryController {

    /** Service encapsulating category business logic. */
    private final CategoryService categoryService;

    /** Assembler that attaches HATEOAS links to responses. */
    private final CategoryModelAssembler assembler;

    /**
     * Creates the controller with its required collaborators.
     *
     * @param categoryService the category service
     * @param assembler       the HATEOAS model assembler
     */
    public CategoryController(CategoryService categoryService,
                              CategoryModelAssembler assembler) {
        this.categoryService = categoryService;
        this.assembler = assembler;
    }

    /**
     * Returns all categories.
     *
     * @return HTTP 200 with a collection model of link-enriched categories
     */
    @GetMapping
    @Operation(
            summary = "Get all categories",
            description = "Returns every category available in the system. " +
                    "Categories are shared across all users and are not paginated. " +
                    "Each item is enriched with a `self` link and a `categories` collection link (HATEOAS)."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "List of categories returned (may be empty)",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            examples = @ExampleObject(value = """
                                    {
                                      "_embedded": {
                                        "categoryResponseList": [
                                          {
                                            "id": 1,
                                            "name": "Streaming",
                                            "description": "Video and audio streaming",
                                            "_links": {
                                              "self":       { "href": "http://localhost:8080/api/categories/1" },
                                              "categories": { "href": "http://localhost:8080/api/categories" }
                                            }
                                          }
                                        ]
                                      },
                                      "_links": {
                                        "self": { "href": "http://localhost:8080/api/categories" }
                                      }
                                    }""")
                    )
            )
    })
    public CollectionModel<EntityModel<CategoryResponse>> getAllCategories() {
        List<EntityModel<CategoryResponse>> models =
                categoryService.getAllCategories().stream()
                        .map(assembler::toModel)
                        .toList();
        return CollectionModel.of(models,
                linkTo(methodOn(CategoryController.class).getAllCategories()).withSelfRel());
    }

    /**
     * Returns a single category by id.
     *
     * @param id the category id
     * @return HTTP 200 with the link-enriched category; HTTP 404 if it does not exist
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
                                    {
                                      "id": 1,
                                      "name": "Streaming",
                                      "description": "Video and audio streaming",
                                      "_links": {
                                        "self":       { "href": "http://localhost:8080/api/categories/1" },
                                        "categories": { "href": "http://localhost:8080/api/categories" }
                                      }
                                    }""")
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
    public ResponseEntity<EntityModel<CategoryResponse>> getCategoryById(
            @Parameter(description = "ID of the category to retrieve", required = true, example = "1")
            @PathVariable Long id) {
        return ResponseEntity.ok(assembler.toModel(categoryService.getCategoryById(id)));
    }

    /**
     * Creates a new category.
     *
     * @param request the validated category details
     * @return HTTP 201 with the created category, a {@code Location} header,
     *         and HATEOAS links; HTTP 409 if a category with the same name already exists
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
                            { "name": "Gaming", "description": "Gaming subscriptions" }""")
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
                                    {
                                      "id": 4,
                                      "name": "Gaming",
                                      "description": "Gaming subscriptions",
                                      "_links": {
                                        "self":       { "href": "http://localhost:8080/api/categories/4" },
                                        "categories": { "href": "http://localhost:8080/api/categories" }
                                      }
                                    }""")
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
    public ResponseEntity<EntityModel<CategoryResponse>> createCategory(
            @Valid @org.springframework.web.bind.annotation.RequestBody CategoryRequest request) {
        EntityModel<CategoryResponse> model =
                assembler.toModel(categoryService.createCategory(request));
        return ResponseEntity
                .created(model.getRequiredLink(IanaLinkRelations.SELF).toUri())
                .body(model);
    }

    /**
     * Updates an existing category.
     *
     * @param id      the id of the category to update
     * @param request the validated new category details
     * @return HTTP 200 with the updated, link-enriched category; HTTP 404 if it does not exist;
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
                                    {
                                      "id": 1,
                                      "name": "Entertainment",
                                      "description": null,
                                      "_links": {
                                        "self":       { "href": "http://localhost:8080/api/categories/1" },
                                        "categories": { "href": "http://localhost:8080/api/categories" }
                                      }
                                    }""")
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
    public ResponseEntity<EntityModel<CategoryResponse>> updateCategory(
            @Parameter(description = "ID of the category to update", required = true, example = "1")
            @PathVariable Long id,
            @Valid @org.springframework.web.bind.annotation.RequestBody CategoryRequest request) {
        return ResponseEntity.ok(assembler.toModel(categoryService.updateCategory(id, request)));
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