package lt.viko.eif.subtrack.mapper;

import lt.viko.eif.subtrack.dto.SubscriptionRequest;
import lt.viko.eif.subtrack.dto.SubscriptionResponse;
import lt.viko.eif.subtrack.entity.Subscription;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * MapStruct mapper between {@link Subscription} entities and their DTOs.
 *
 * Generated as a Spring bean ({@code componentModel = "spring"}). The owning
 * {@code user} and {@code category} associations are resolved by the service
 * layer (from the authenticated principal and the referenced category id) and
 * are therefore ignored on the inbound mappings; the service sets them
 * explicitly after mapping. On the outbound mapping, the category is flattened
 * to {@code categoryId}/{@code categoryName}.
 *
 * Note on {@code active}: the request field is a nullable {@link Boolean}
 * while the entity field is a primitive {@code boolean} that defaults to
 * {@code true}. MapStruct null-guards the assignment, so omitting {@code active}
 * on create leaves the entity default ({@code true}) and on update leaves the
 * stored value unchanged.
 */
@Mapper(componentModel = "spring")
public interface SubscriptionMapper {

    /**
     * Maps a subscription entity to its response DTO, flattening the category.
     *
     * <p>Must be invoked within an active persistence context, since it reads
     * the lazily-loaded category.</p>
     *
     * @param subscription the entity to convert
     * @return the response DTO
     */
    @Mapping(target = "categoryId", source = "category.id")
    @Mapping(target = "categoryName", source = "category.name")
    SubscriptionResponse toResponse(Subscription subscription);

    /**
     * Maps a list of subscription entities to response DTOs.
     *
     * @param subscriptions the entities to convert
     * @return the response DTOs
     */
    List<SubscriptionResponse> toResponseList(List<Subscription> subscriptions);

    /**
     * Builds a new subscription entity from a create request, leaving the
     * {@code user} and {@code category} associations for the service to set.
     *
     * @param request the request payload
     * @return a new, unpersisted {@link Subscription}
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "category", ignore = true)
    Subscription toEntity(SubscriptionRequest request);

    /**
     * Copies request fields onto an existing subscription entity in place,
     * leaving {@code user} and {@code category} for the service to manage.
     *
     * @param request      the request payload
     * @param subscription the managed entity to update
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "category", ignore = true)
    void updateEntity(SubscriptionRequest request, @MappingTarget Subscription subscription);
}