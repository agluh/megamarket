package com.github.agluh.megamarket.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.github.agluh.megamarket.model.ShopUnit;
import com.github.agluh.megamarket.repository.ShopUnitRepository;
import java.util.Collections;
import java.util.UUID;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@Disabled
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ShopControllerIntegrationTest {

    @MockBean
    private ShopUnitRepository shopUnitRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void canRetrieveByIdWhenExists() {
        // given
        final UUID nonExistingId = UUID.randomUUID();
        given(shopUnitRepository.getNodeWithSubtree(nonExistingId))
            .willReturn(Collections.emptyList());

        // when
        ResponseEntity<ShopUnit> superHeroResponse =
            restTemplate.getForEntity("/nodes/" + nonExistingId, ShopUnit.class);

        // then
        assertThat(superHeroResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

}