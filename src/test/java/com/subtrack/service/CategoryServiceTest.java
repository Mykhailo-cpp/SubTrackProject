package com.subtrack.service;

import com.subtrack.dto.CategoryRequest;
import com.subtrack.dto.CategoryResponse;
import com.subtrack.entity.Category;
import com.subtrack.exception.DuplicateResourceException;
import com.subtrack.exception.ResourceNotFoundException;
import com.subtrack.mapper.CategoryMapper;
import com.subtrack.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CategoryServiceImpl}. All collaborators
 * ({@link CategoryRepository}, {@link CategoryMapper}) are mocked with Mockito;
 * no Spring context or database is involved.
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category category;
    private CategoryRequest request;
    private CategoryResponse response;

    @BeforeEach
    void setUp() {
        category = new Category("Streaming", "Video and music streaming");
        category.setId(1L);

        request = new CategoryRequest("Streaming", "Video and music streaming");

        response = new CategoryResponse();
        response.setId(1L);
        response.setName("Streaming");
        response.setDescription("Video and music streaming");
    }

    @Nested
    class GetTests {

        @Test
        void getAllCategories_ReturnsMappedList() {
            // Arrange
            List<Category> entities = List.of(category);
            List<CategoryResponse> responses = List.of(response);
            when(categoryRepository.findAll()).thenReturn(entities);
            when(categoryMapper.toResponseList(entities)).thenReturn(responses);

            // Act
            List<CategoryResponse> result = categoryService.getAllCategories();

            // Assert
            assertEquals(1, result.size());
            assertSame(response, result.get(0));
            verify(categoryRepository).findAll();
        }

        @Test
        void getCategoryById_Found_ReturnsResponse() {
            // Arrange
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryMapper.toResponse(category)).thenReturn(response);

            // Act
            CategoryResponse result = categoryService.getCategoryById(1L);

            // Assert
            assertSame(response, result);
        }

        @Test
        void getCategoryById_NotFound_Throws() {
            // Arrange
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class,
                    () -> categoryService.getCategoryById(99L));
        }
    }

    @Nested
    class CreateTests {

        @Test
        void createCategory_Success_ReturnsResponse() {
            // Arrange
            when(categoryRepository.existsByName("Streaming")).thenReturn(false);
            when(categoryMapper.toEntity(request)).thenReturn(category);
            when(categoryRepository.save(category)).thenReturn(category);
            when(categoryMapper.toResponse(category)).thenReturn(response);

            // Act
            CategoryResponse result = categoryService.createCategory(request);

            // Assert
            assertSame(response, result);
            verify(categoryRepository).save(category);
        }

        @Test
        void createCategory_DuplicateName_ThrowsAndDoesNotSave() {
            // Arrange
            when(categoryRepository.existsByName("Streaming")).thenReturn(true);

            // Act & Assert
            assertThrows(DuplicateResourceException.class,
                    () -> categoryService.createCategory(request));
            verify(categoryRepository, never()).save(any(Category.class));
        }
    }

    @Nested
    class UpdateTests {

        @Test
        void updateCategory_NewName_Success() {
            // Arrange
            CategoryRequest renameRequest = new CategoryRequest("Music", "Music only");
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryRepository.existsByName("Music")).thenReturn(false);
            when(categoryRepository.save(category)).thenReturn(category);
            when(categoryMapper.toResponse(category)).thenReturn(response);

            // Act
            CategoryResponse result = categoryService.updateCategory(1L, renameRequest);

            // Assert
            assertSame(response, result);
            verify(categoryMapper).updateEntity(renameRequest, category);
            verify(categoryRepository).save(category);
        }

        @Test
        void updateCategory_SameName_SkipsDuplicateCheck() {
            // Arrange: name unchanged, so existsByName must not be consulted
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryRepository.save(category)).thenReturn(category);
            when(categoryMapper.toResponse(category)).thenReturn(response);

            // Act
            CategoryResponse result = categoryService.updateCategory(1L, request);

            // Assert
            assertSame(response, result);
            verify(categoryRepository, never()).existsByName(any());
        }

        @Test
        void updateCategory_DuplicateName_Throws() {
            // Arrange
            CategoryRequest renameRequest = new CategoryRequest("Music", "Music only");
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
            when(categoryRepository.existsByName("Music")).thenReturn(true);

            // Act & Assert
            assertThrows(DuplicateResourceException.class,
                    () -> categoryService.updateCategory(1L, renameRequest));
            verify(categoryRepository, never()).save(any(Category.class));
        }

        @Test
        void updateCategory_NotFound_Throws() {
            // Arrange
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class,
                    () -> categoryService.updateCategory(99L, request));
        }
    }

    @Nested
    class DeleteTests {

        @Test
        void deleteCategory_Success_Deletes() {
            // Arrange
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

            // Act
            categoryService.deleteCategory(1L);

            // Assert
            verify(categoryRepository).delete(category);
        }

        @Test
        void deleteCategory_NotFound_Throws() {
            // Arrange
            when(categoryRepository.findById(99L)).thenReturn(Optional.empty());

            // Act & Assert
            assertThrows(ResourceNotFoundException.class,
                    () -> categoryService.deleteCategory(99L));
            verify(categoryRepository, never()).delete(any(Category.class));
        }
    }
}