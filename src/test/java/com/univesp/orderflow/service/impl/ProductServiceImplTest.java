package com.univesp.orderflow.service.impl;

import com.univesp.orderflow.dto.ProductDTO;
import com.univesp.orderflow.dto.filters.ProductFilterDTO;
import com.univesp.orderflow.exceptions.ApiErrorException;
import com.univesp.orderflow.mapper.ProductMapper;
import com.univesp.orderflow.model.Product;
import com.univesp.orderflow.repository.ProductRepository;
import com.univesp.orderflow.service.ProductImageStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {
    @Mock
    private ProductRepository productRepository;
    @Mock
    private ProductMapper productMapper;
    @Mock
    private ProductImageStorage imageStorage;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(productRepository, productMapper, imageStorage);
    }

    @Test
    void saveProductMapsAndPersistsProduct() {
        ProductDTO data = productDTO();
        Product product = new Product();
        ProductDTO expected = productDTO();
        when(productMapper.toEntity(data)).thenReturn(product);
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toDTO(product)).thenReturn(expected);

        assertSame(expected, productService.saveProduct(data));
        verify(productRepository).save(product);
    }

    @Test
    void getAllAppliesFilterAndMapsResults() {
        ProductFilterDTO filter = new ProductFilterDTO();
        filter.setName("coffee");
        filter.setCode("C-1");
        Product product = new Product();
        ProductDTO dto = productDTO();
        var pageable = PageRequest.of(0, 10);
        when(productRepository.findByFilters("coffee", "C-1", pageable))
                .thenReturn(new PageImpl<>(List.of(product)));
        when(productMapper.toDTO(product)).thenReturn(dto);

        var result = productService.getAll(filter, pageable);

        assertEquals(List.of(dto), result.getContent());
    }

    @Test
    void updateProductChangesOnlyProvidedFields() {
        UUID id = UUID.randomUUID();
        Product product = new Product();
        product.setName("Old");
        product.setCode("OLD");
        product.setPrice(new BigDecimal("4.50"));
        product.setQuantity(8);
        ProductDTO changes = new ProductDTO(null, "New", null, null, 3, null);
        ProductDTO expected = productDTO();
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toDTO(product)).thenReturn(expected);

        ProductDTO result = productService.updateProduct(id, changes);

        assertSame(expected, result);
        assertEquals("New", product.getName());
        assertEquals("OLD", product.getCode());
        assertEquals(new BigDecimal("4.50"), product.getPrice());
        assertEquals(3, product.getQuantity());
    }

    @Test
    void updateProductReturnsNotFoundForUnknownId() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        ApiErrorException exception = assertThrows(
                ApiErrorException.class,
                () -> productService.updateProduct(id, new ProductDTO())
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(productRepository, never()).save(any());
    }

    @Test
    void uploadImageStoresReplacementBeforeDeletingOldImage() {
        UUID id = UUID.randomUUID();
        Product product = new Product();
        product.setImageFileName("old.png");
        ProductDTO expected = productDTO();
        var file = new MockMultipartFile("file", "new.png", "image/png", new byte[]{1, 2});
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(imageStorage.store(file)).thenReturn("new.png");
        when(productRepository.save(product)).thenReturn(product);
        when(productMapper.toDTO(product)).thenReturn(expected);

        assertSame(expected, productService.uploadImage(id, file));
        assertEquals("new.png", product.getImageFileName());
        InOrder order = inOrder(productRepository, imageStorage);
        order.verify(imageStorage).store(file);
        order.verify(productRepository).save(product);
        order.verify(imageStorage).delete("old.png");
    }

    @Test
    void getImageReturnsStoredResource() {
        UUID id = UUID.randomUUID();
        Product product = new Product();
        product.setImageFileName("photo.png");
        var resource = new ByteArrayResource(new byte[]{1});
        when(productRepository.findById(id)).thenReturn(Optional.of(product));
        when(imageStorage.load("photo.png")).thenReturn(resource);

        assertSame(resource, productService.getImage(id));
    }

    @Test
    void getImageReturnsNotFoundWhenProductHasNoImage() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.of(new Product()));

        ApiErrorException exception = assertThrows(ApiErrorException.class, () -> productService.getImage(id));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        verify(imageStorage, never()).load(any());
    }

    private ProductDTO productDTO() {
        return new ProductDTO(UUID.randomUUID(), "Coffee", "C-1", new BigDecimal("4.50"), 8, null);
    }
}
