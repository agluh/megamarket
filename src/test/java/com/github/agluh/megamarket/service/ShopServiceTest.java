package com.github.agluh.megamarket.service;

import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.BDDMockito.given;

import com.github.agluh.megamarket.repository.ShopUnitRepository;
import com.github.agluh.megamarket.service.exceptions.ShopUnitNotFoundException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ShopServiceTest {

    @Mock
    private ShopUnitRepository shopUnitRepository;

    @InjectMocks
    private ShopService shopService;

    @Test
    void givenNonExistedNode_whenGetNode_thenWillThrowException() {
        // Given
        final UUID nonExistedNodeId = UUID.randomUUID();
        given(shopUnitRepository.getNodeWithSubtree(nonExistedNodeId))
            .willThrow(ShopUnitNotFoundException.class);

        // When
        final Throwable throwable = catchThrowable(() ->
            shopService.getNode(nonExistedNodeId));

        // Then
        then(throwable)
            .isInstanceOf(ShopUnitNotFoundException.class);
    }

    @Test
    void givenNonExistedNode_whenDeleteNode_thenWillThrowException() {
        // Given
        final UUID nonExistedNodeId = UUID.randomUUID();
        given(shopUnitRepository.getNode(nonExistedNodeId))
            .willThrow(ShopUnitNotFoundException.class);

        // When
        final Throwable throwable = catchThrowable(() ->
            shopService.deleteNode(nonExistedNodeId));

        // Then
        then(throwable)
            .isInstanceOf(ShopUnitNotFoundException.class);
    }
}