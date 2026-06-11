package lt.viko.eif.subtrack.controller;

import lt.viko.eif.subtrack.dto.CategoryResponse;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

/**
 * Converts a {@link CategoryResponse} into an {@link EntityModel} enriched
 * with HATEOAS links, satisfying Richardson Maturity Model Level 3.
 *
 * <p>Each model carries a {@code self} link to its own resource and a
 * {@code categories} link back to the collection. Links are derived from the
 * {@link CategoryController} mappings via {@code linkTo(methodOn(...))} so that
 * they stay correct if the URL structure ever changes.</p>
 */
@Component
public class CategoryModelAssembler
        implements RepresentationModelAssembler<CategoryResponse, EntityModel<CategoryResponse>> {

    /**
     * Wraps the given response in an {@link EntityModel} with self and
     * collection links.
     *
     * @param category the response DTO to wrap
     * @return the link-enriched model
     */
    @Override
    public EntityModel<CategoryResponse> toModel(CategoryResponse category) {
        return EntityModel.of(category,
                linkTo(methodOn(CategoryController.class)
                        .getCategoryById(category.id())).withSelfRel(),
                linkTo(methodOn(CategoryController.class)
                        .getAllCategories()).withRel("categories"));
    }
}