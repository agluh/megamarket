package com.github.agluh.megamarket.controller;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.agluh.megamarket.service.ShopService;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(ShopController.class)
class ShopControllerTest {

    @MockBean
    private ShopService shopService;

    @Autowired
    private MockMvc mvc;

    @ParameterizedTest(name = "{1}")
    @MethodSource("provideBadDataForImporting")
    public void givenIncorrectImportData_whenPost_thenResponseBadRequest(String data, String message) throws Exception {
        // Given: data from argument

        // When
        ResultActions result = mvc.perform(
            post("/imports")
                .content(data)
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isBadRequest());
        result.andExpect(jsonPath("$.code", isA(Integer.class)));
        result.andExpect(jsonPath("$.code", is(400)));
        result.andExpect(jsonPath("$.message", isA(String.class)));
        result.andExpect(jsonPath("$.message", is("Validation failed")));
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
    public void givenCorrectImportData_whenPost_thenResponseOk(String data, String message) throws Exception {
        // Given: data from argument

        // When
        ResultActions result = mvc.perform(
            post("/imports")
                .content(data)
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk());
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
    public void givenCorrectImportDate_whenPost_thenResponseOk(String date) throws Exception {
        // Given: date from argument
        final String json = """
            {
                "items": [],
                "updateDate": "%s"
            }
        """;

        // When
        ResultActions result = mvc.perform(
            post("/imports")
                .content(String.format(json, date))
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk());
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/iso8601_invalid_dates.csv")
    public void givenInvalidImportDate_whenPost_thenResponseBadRequest(String date) throws Exception {
        // Given: date from argument
        final String json = """
            {
                "items": [],
                "updateDate": "%s"
            }
        """;

        // When
        ResultActions result = mvc.perform(
            post("/imports")
                .content(String.format(json, date))
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isBadRequest());
        result.andExpect(jsonPath("$.code", isA(Integer.class)));
        result.andExpect(jsonPath("$.code", is(400)));
        result.andExpect(jsonPath("$.message", isA(String.class)));
        result.andExpect(jsonPath("$.message", is("Validation failed")));
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/iso8601_dates.csv")
    public void givenCorrectDate_whenGetSales_thenResponseOk(String date) throws Exception {
        // Given: date from argument

        // When
        ResultActions result = mvc.perform(
            get("/sales")
                .param("date", date)
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk());
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/iso8601_invalid_dates.csv")
    public void givenInvalidDate_whenGetSales_thenResponseBadRequest(String date) throws Exception {
        // Given: date from argument

        // When
        ResultActions result = mvc.perform(
            get("/sales")
                .param("date", date)
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isBadRequest());
        result.andExpect(jsonPath("$.code", isA(Integer.class)));
        result.andExpect(jsonPath("$.code", is(400)));
        result.andExpect(jsonPath("$.message", isA(String.class)));
        result.andExpect(jsonPath("$.message", is("Validation failed")));
    }

    @Test
    public void givenMissedDateParam_whenGetSales_thenResponseBadRequest() throws Exception {
        // Given: no date param

        // When
        ResultActions result = mvc.perform(
            get("/sales")
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isBadRequest());
        result.andExpect(jsonPath("$.code", isA(Integer.class)));
        result.andExpect(jsonPath("$.code", is(400)));
        result.andExpect(jsonPath("$.message", isA(String.class)));
        result.andExpect(jsonPath("$.message", is("Validation failed")));
    }

    @Test
    public void givenNotExistedEndpoint_whenGet_thenResponseNotFound() throws Exception {
        // Given: no date param

        // When
        ResultActions result = mvc.perform(
            get("/foo")
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isNotFound());
        result.andExpect(jsonPath("$.code", isA(Integer.class)));
        result.andExpect(jsonPath("$.code", is(404)));
        result.andExpect(jsonPath("$.message", isA(String.class)));
        result.andExpect(jsonPath("$.message", is("Item not found")));
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/iso8601_dates.csv")
    public void givenCorrectDate_whenGetNodeStatistics_thenResponseOk(String date) throws Exception {
        // Given: date from argument
        final UUID nodeId = UUID.randomUUID();
        given(shopService.getNodeStatistics(eq(nodeId), any(), any()))
            .willReturn(Collections.emptyList());

        // When
        ResultActions result = mvc.perform(
            get("/node/" + nodeId + "/statistic")
                .param("from", date)
                .param("to", date)
                .contentType(MediaType.APPLICATION_JSON));

        // Then
        result.andExpect(status().isOk());
    }
}