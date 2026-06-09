package com.subtrack.mapper;

import com.subtrack.dto.CategoryRequest;
import com.subtrack.dto.CategoryResponse;
import com.subtrack.entity.Category;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * MapStruct mapper between {@link Category} entities and their DTOs.
 *
 * Generated as a Spring bean ({@code componentModel = "spring"}) so it can
 * be injected into services. Identifier and relationship fields are never
 * populated from client input: {@code id} is database-assigned and the
 * {@code subscriptions} back-reference is managed by JPA, so both are ignored
 * on the inbound mappings.
 */
@Mapper(componentModel = "spring")
public interface CategoryMapper {

    /**
     * Maps a category entity to its response DTO.
     *
     * @param category the entity to convert
     * @return the response DTO
     */
    CategoryResponse toResponse(Category category);

    /**
     * Maps a list of category entities to response DTOs.
     *
     * @param categories the entities to convert
     * @return the response DTOs
     */
    List<CategoryResponse> toResponseList(List<Category> categories);

    /**
     * Builds a new category entity from a create/update request.
     *
     * @param request the request payload
     * @return a new, unpersisted {@link Category}
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subscriptions", ignore = true)
    Category toEntity(CategoryRequest request);

    /**
     * Copies request fields onto an existing category entity in place.
     *
     * @param request  the request payload
     * @param category the managed entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subscriptions", ignore = true)
    void updateEntity(CategoryRequest request, @MappingTarget Category category);
}