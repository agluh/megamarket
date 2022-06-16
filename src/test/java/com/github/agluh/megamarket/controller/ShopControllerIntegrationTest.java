package com.github.agluh.megamarket.controller;

import static org.assertj.core.api.BDDAssertions.then;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.github.agluh.megamarket.service.ShopService;
import java.net.URI;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.UriComponentsBuilder;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ShopControllerIntegrationTest {

    @MockBean
    private ShopService shopService;

    @Autowired
    private TestRestTemplate restTemplate;

    @LocalServerPort
    private int randomServerPort;

    @ParameterizedTest(name = "{1}")
    @MethodSource("provideBadDataForImporting")
    public void givenIncorrectImportData_whenPost_thenResponseBadRequest(String data, String message) {
        // Given
        final String baseUrl = "http://localhost:" + randomServerPort;

        // When
        ResponseEntity<Object> responseEntity = doJsonPost(baseUrl + "/imports", data);

        // Then
        then(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        then(responseEntity.getBody()).extracting("code").isEqualTo(400);
        then(responseEntity.getBody()).extracting("message").isEqualTo("Bad Request");
    }

    private static Stream<Arguments> provideBadDataForImporting() {
        return Stream.of(
            Arguments.of("", "empty request"),
            Arguments.of("{}", "empty object as request"),
            Arguments.of("{bad}", "malformed request"),
            Arguments.of("{\"items\":[]}", "updateDate is missed"),
            Arguments.of("{\"items\":[], \"updateDate\":null}", "updateDate is null"),
            Arguments.of("{\"updateDate\":\"2022-05-28T21:12:01.000Z\"}", "items is missed"),
            Arguments.of("{\"items\":null, \"updateDate\":\"2022-05-28T21:12:01.000Z\"}", "items is null"),
            Arguments.of("{\"items\":4, \"updateDate\":\"2022-05-28T21:12:01.000Z\"}", "items is not an array"),
            Arguments.of("{\"extra\":null, \"items\":[], \"updateDate\":\"2022-05-28T21:12:01.000Z\"}", "extra fields"),
            Arguments.of("""
                {
                  "items": [
                    {
                      "name": "Offer",
                      "parentId": "3fa85f64-5717-4562-b3fc-2c963f66a333",
                      "price": 123,
                      "type": "OFFER"
                    }
                  ],
                  "updateDate": "2022-05-28T21:12:01.000Z"
                }
                """, "id is missing"),
            Arguments.of("""
                {
                  "items": [
                    {
                      "id": null,
                      "name": "Offer",
                      "parentId": "3fa85f64-5717-4562-b3fc-2c963f66a333",
                      "price": 234,
                      "type": "OFFER"
                    }
                  ],
                  "updateDate": "2022-05-28T21:12:01.000Z"
                }
                """, "id is null"),
            Arguments.of("""
                {
                  "items": [
                    {
                      "id": "",
                      "name": "Offer",
                      "parentId": "3fa85f64-5717-4562-b3fc-2c963f66a333",
                      "price": 234,
                      "type": "OFFER"
                    }
                  ],
                  "updateDate": "2022-05-28T21:12:01.000Z"
                }
                """, "id is empty"),
            Arguments.of("""
                {
                  "items": [
                    {
                      "id": "121212",
                      "name": "Offer",
                      "parentId": "3fa85f64-5717-4562-b3fc-2c963f66a333",
                      "price": 234,
                      "type": "OFFER"
                    }
                  ],
                  "updateDate": "2022-05-28T21:12:01.000Z"
                }
                """, "id is not valid"),
            Arguments.of("""
                {
                  "items": [
                    {
                      "id": "3fa85f64-5717-4562-b3fc-2c963f66a444",
                      "parentId": "3fa85f64-5717-4562-b3fc-2c963f66a333",
                      "price": 123,
                      "type": "OFFER"
                    }
                  ],
                  "updateDate": "2022-05-28T21:12:01.000Z"
                }
                """, "name is missing"),
            Arguments.of("""
                {
                  "items": [
                    {
                      "id": "3fa85f64-5717-4562-b3fc-2c963f66a444",
                      "name": null,
                      "parentId": "3fa85f64-5717-4562-b3fc-2c963f66a333",
                      "price": 234,
                      "type": "OFFER"
                    }
                  ],
                  "updateDate": "2022-05-28T21:12:01.000Z"
                }
                """, "name is null"),
            Arguments.of("""
                {
                  "items": [
                    {
                      "id": "3fa85f64-5717-4562-b3fc-2c963f66a444",
                      "name": "Offer",
                      "parentId": "3fa85f64-5717-4562-b3fc-2c963f66a333",
                      "price": 123
                    }
                  ],
                  "updateDate": "2022-05-28T21:12:01.000Z"
                }
                """, "type is missing"),
            Arguments.of("""
                {
                  "items": [
                    {
                      "id": "3fa85f64-5717-4562-b3fc-2c963f66a444",
                      "name": "Offer",
                      "parentId": "3fa85f64-5717-4562-b3fc-2c963f66a333",
                      "price": 123,
                      "type": "Bad"
                    }
                  ],
                  "updateDate": "2022-05-28T21:12:01.000Z"
                }
                """, "type is unknown"),
            Arguments.of("""
                {
                  "items": [
                    {
                      "id": "3fa85f64-5717-4562-b3fc-2c963f66a444",
                      "name": "Offer",
                      "parentId": "3fa85f64-5717-4562-b3fc-2c963f66a333",
                      "type": "OFFER"
                    }
                  ],
                  "updateDate": "2022-05-28T21:12:01.000Z"
                }
                """, "offer price is missing"),
            Arguments.of("""
                {
                  "items": [
                    {
                      "id": "3fa85f64-5717-4562-b3fc-2c963f66a444",
                      "name": "Offer",
                      "parentId": "3fa85f64-5717-4562-b3fc-2c963f66a333",
                      "price": null,
                      "type": "OFFER"
                    }
                  ],
                  "updateDate": "2022-05-28T21:12:01.000Z"
                }
                """, "offer price is null"),
            Arguments.of("""
                {
                  "items": [
                    {
                      "id": "3fa85f64-5717-4562-b3fc-2c963f66a444",
                      "name": "Offer",
                      "parentId": "3fa85f64-5717-4562-b3fc-2c963f66a333",
                      "price": -1,
                      "type": "OFFER"
                    }
                  ],
                  "updateDate": "2022-05-28T21:12:01.000Z"
                }
                """, "offer price is negative"),
            Arguments.of("""
                {
                  "items": [
                    {
                      "id": "3fa85f64-5717-4562-b3fc-2c963f66a444",
                      "name": "Category",
                      "parentId": "3fa85f64-5717-4562-b3fc-2c963f66a333",
                      "price": 123,
                      "type": "CATEGORY"
                    }
                  ],
                  "updateDate": "2022-05-28T21:12:01.000Z"
                }
                """, "category price is not null"),
            Arguments.of("""
                {
                  "items": [
                    {
                      "id": "3fa85f64-5717-4562-b3fc-2c963f66a333",
                      "name": "Offer",
                      "parentId": "121212",
                      "price": 234,
                      "type": "OFFER"
                    }
                  ],
                  "updateDate": "2022-05-28T21:12:01.000Z"
                }
                """, "parent id is not valid")
        );
    }

    @ParameterizedTest(name = "{1}")
    @MethodSource("provideCorrectDataForImporting")
    public void givenCorrectImportData_whenPost_thenResponseOk(String data, String message) {
        // Given
        final String baseUrl = "http://localhost:" + randomServerPort;

        // When
        ResponseEntity<Object> responseEntity = doJsonPost(baseUrl + "/imports", data);

        // Then
        then(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private static Stream<Arguments> provideCorrectDataForImporting() {
        return Stream.of(
            Arguments.of("""
                {
                  "items": [
                    {
                        "id": "3fa85f64-5717-4562-b3fc-2c963f66a444",
                        "name": "Category",
                        "parentId": "3fa85f64-5717-4562-b3fc-2c963f66a333",
                        "type": "CATEGORY"
                    }
                  ],
                  "updateDate": "2022-05-28T21:12:01.000Z"
                }
                """, "category price is missing"),
            Arguments.of("""
                {
                  "items": [
                    {
                        "id": "3fa85f64-5717-4562-b3fc-2c963f66a444",
                        "name": "Category",
                        "parentId": "3fa85f64-5717-4562-b3fc-2c963f66a333",
                        "price": null,
                        "type": "CATEGORY"
                    }
                  ],
                  "updateDate": "2022-05-28T21:12:01.000Z"
                }
                """, "category price is null"),
            Arguments.of("""
                {
                  "items": [
                    {
                        "id": "3fa85f64-5717-4562-b3fc-2c963f66a444",
                        "name": "Offer",
                        "price": 123,
                        "type": "OFFER"
                    }
                  ],
                  "updateDate": "2022-05-28T21:12:01.000Z"
                }
                """, "offer parent id is missing"),
            Arguments.of("""
                {
                  "items": [
                    {
                        "id": "3fa85f64-5717-4562-b3fc-2c963f66a444",
                        "name": "Offer",
                        "parentId": null,
                        "price": 123,
                        "type": "OFFER"
                    }
                  ],
                  "updateDate": "2022-05-28T21:12:01.000Z"
                }
                """, "offer parent id is null")
        );
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/iso8601_dates.csv")
    public void givenCorrectImportDate_whenPost_thenResponseOk(String date) {
        // Given
        final String baseUrl = "http://localhost:" + randomServerPort;
        final String json = """
            {
                "items": [],
                "updateDate": "%s"
            }
        """;

        // When
        ResponseEntity<Object> responseEntity = doJsonPost(baseUrl + "/imports", String.format(json, date));

        // Then
        then(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/iso8601_invalid_dates.csv")
    public void givenInvalidImportDate_whenPost_thenResponseBadRequest(String date) {
        // Given
        final String baseUrl = "http://localhost:" + randomServerPort;
        final String json = """
            {
                "items": [],
                "updateDate": "%s"
            }
        """;

        // When
        ResponseEntity<Object> responseEntity = doJsonPost(baseUrl + "/imports", String.format(json, date));

        // Then
        then(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        then(responseEntity.getBody()).extracting("code").isEqualTo(400);
        then(responseEntity.getBody()).extracting("message").isEqualTo("Bad Request");
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/iso8601_dates.csv")
    public void givenCorrectDate_whenGetSales_thenResponseOk(String date) {
        // Given: date from argument
        final String baseUrl = "http://localhost:" + randomServerPort;
        URI uri = UriComponentsBuilder.fromUriString(baseUrl + "/sales?date={date}").build(date);

        // When
        ResponseEntity<Object> responseEntity =
            restTemplate.getForEntity(uri, Object.class);

        // Then
        then(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/iso8601_invalid_dates.csv")
    public void givenInvalidDate_whenGetSales_thenResponseBadRequest(String date) {
        // Given: date from argument
        final String baseUrl = "http://localhost:" + randomServerPort;
        URI uri = UriComponentsBuilder.fromUriString(baseUrl + "/sales?date={date}").build(date);

        // When
        ResponseEntity<Object> responseEntity =
            restTemplate.getForEntity(uri, Object.class);

        // Then
        then(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        then(responseEntity.getBody()).extracting("code").isEqualTo(400);
        then(responseEntity.getBody()).extracting("message").isEqualTo("Bad Request");
    }

    @Test
    public void givenMissedDateParam_whenGetSales_thenResponseBadRequest() {
        // Given
        final String baseUrl = "http://localhost:" + randomServerPort;

        // When
        ResponseEntity<Object> responseEntity =
            restTemplate.getForEntity(baseUrl + "/sales", Object.class);

        // Then
        then(responseEntity.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        then(responseEntity.getBody()).extracting("code").isEqualTo(400);
        then(responseEntity.getBody()).extracting("message").isEqualTo("Bad Request");
    }

    @Test
    public void givenNotExistedEndpoint_whenGet_thenResponseNotFound() {
        // Given
        final String baseUrl = "http://localhost:" + randomServerPort;

        // When
        ResponseEntity<Object> responseEntity =
            restTemplate.getForEntity(baseUrl + "/foo", Object.class);

        // Then
        then(responseEntity.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        then(responseEntity.getBody()).extracting("code").isEqualTo(404);
        then(responseEntity.getBody()).extracting("message").isEqualTo("Not Found");
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/iso8601_dates.csv")
    public void givenCorrectDate_whenGetNodeStatistics_thenResponseOk(String date) {
        // Given
        final UUID nodeId = UUID.randomUUID();
        given(shopService.getNodeStatistics(eq(nodeId), any(), any()))
            .willReturn(Collections.emptyList());

        final String baseUrl = "http://localhost:" + randomServerPort;
        URI uri = UriComponentsBuilder.fromUriString(baseUrl +
                "/node/{id}/statistic?dateStart={from}&dateEnd={to}")
            .build(nodeId, date, date);

        // When
        ResponseEntity<Object> responseEntity =
            restTemplate.getForEntity(uri, Object.class);

        // Then
        then(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    private ResponseEntity<Object> doJsonPost(String url, String json) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> request = new HttpEntity<>(json, headers);

        return restTemplate.postForEntity(url, request, Object.class);
    }
}