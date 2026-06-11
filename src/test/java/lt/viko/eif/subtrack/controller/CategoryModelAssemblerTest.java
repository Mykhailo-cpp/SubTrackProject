package lt.viko.eif.subtrack.controller;

import lt.viko.eif.subtrack.dto.CategoryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link CategoryModelAssembler}.
 *
 * <p>Verifies that {@link CategoryModelAssembler#toModel(CategoryResponse)}
 * attaches the correct {@code self} and {@code categories} HATEOAS links,
 * and that the original payload is preserved unchanged inside the model.</p>
 */
class CategoryModelAssemblerTest {

    private CategoryModelAssembler assembler;
    private CategoryResponse response;

    @BeforeEach
    void setUp() {
        assembler = new CategoryModelAssembler();
        response = new CategoryResponse(7L, "Streaming", "Video and audio streaming");
    }

    @Test
    void toModel_WrapsPayloadUnchanged() {
        EntityModel<CategoryResponse> model = assembler.toModel(response);

        assertNotNull(model.getContent(), "EntityModel content must not be null");
        assertEquals(response, model.getContent(), "EntityModel must wrap the original response");
    }

    @Test
    void toModel_HasSelfLink() {
        EntityModel<CategoryResponse> model = assembler.toModel(response);

        Link selfLink = model.getLink("self").orElse(null);
        assertNotNull(selfLink, "Model must contain a 'self' link");
        assertTrue(selfLink.getHref().endsWith("/api/categories/7"),
                "Self link href must end with /api/categories/7 but was: " + selfLink.getHref());
    }

    @Test
    void toModel_HasCategoriesCollectionLink() {
        EntityModel<CategoryResponse> model = assembler.toModel(response);

        Link collectionLink = model.getLink("categories").orElse(null);
        assertNotNull(collectionLink, "Model must contain a 'categories' link");
        assertTrue(collectionLink.getHref().endsWith("/api/categories"),
                "Collection link href must end with /api/categories but was: " + collectionLink.getHref());
    }

    @Test
    void toModel_SelfLinkReflectsCategoryId() {
        CategoryResponse other = new CategoryResponse(3L, "Gaming", null);

        EntityModel<CategoryResponse> model = assembler.toModel(other);

        Link selfLink = model.getLink("self").orElseThrow();
        assertTrue(selfLink.getHref().endsWith("/api/categories/3"),
                "Self link must use the response's own id (3) but was: " + selfLink.getHref());
    }
}