package com.netflix.integration_tests;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MovieServiceTests {

    private static String createdMovieId;
    private static final String wrongMovieId = "ffffffff-ffff-ffff-ffff-ffffffffffff";

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8081;
    }

    private String createMovieJson(String title, int durationMinutes, String genre) {
        return """
                {
                    "title": "%s",
                    "durationMinutes": %d,
                    "genre": "%s"
                }
                """.formatted(title, durationMinutes, genre);
    }

    @Test
    @Order(1)
    void createMovie_shouldReturn201AndPersistedMovie() {
        String requestBody = createMovieJson("Inception", 148, "SCI_FI");

        Response response = given()
                .contentType(ContentType.JSON)
                .body(requestBody)
        .when()
                .post("/api/v1/movies")
        .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("title", equalTo("Inception"))
                .body("durationMinutes", equalTo(148))
                .body("genre", equalTo("SCI_FI"))
                .extract().response();

        createdMovieId = response.jsonPath().getString("id");
    }

    @Test
    @Order(2)
    void getMovieById_shouldReturn200WithCorrectId() {
        given()
        .when()
                .get("/api/v1/movies/{id}", createdMovieId)
        .then()
                .statusCode(200);
    }

    @Test
    @Order(3)
    void getMovieById_shouldReturn404WithWrongId() {
        given()
        .when()
                .get("/api/v1/movies/{movieId}", wrongMovieId)
        .then()
                .statusCode(404);
    }

    @Test
    @Order(4)
    void updateMovie_shouldReturn200AndReflectUpdatedDurationMinutes() {
        String updateBody = createMovieJson("Inception", 200, "SCI_FI");

        given()
                .contentType(ContentType.JSON)
                .body(updateBody)
        .when()
                .put("/api/v1/movies/{movieId}", createdMovieId)
        .then()
                .statusCode(200)
                .body("durationMinutes", equalTo(200));

        given()
        .when()
                .get("/api/v1/movies/{movieId}", createdMovieId)
        .then()
                .statusCode(200)
                .body("durationMinutes", equalTo(200));
    }

    @Test
    @Order(5)
    void updateMovie_shouldReturn404WithWrongId() {
        String updateBody = createMovieJson("Inception", 200, "SCI_FI");

        given()
                .contentType(ContentType.JSON)
                .body(updateBody)
        .when()
                .put("/api/v1/movies/{movieId}", wrongMovieId)
        .then()
                .statusCode(404);
    }

    @Test
    @Order(6)
    void deleteMovie_shouldReturn200WithCorrectId() {
        given()
        .when()
                .delete("/api/v1/movies/{movieId}", createdMovieId)
        .then()
                .statusCode(200);
    }

    @Test
    @Order(7)
    void deleteMovie_shouldReturn404WhenAlreadyDeleted() {
        given()
        .when()
                .delete("/api/v1/movies/{movieId}", createdMovieId)
        .then()
                .statusCode(404);
    }

    @Test
    @Order(8)
    void searchByGenre_shouldReturn200AndListOfSize3() {
        String id1 = given().contentType(ContentType.JSON).body(createMovieJson("The Dark Knight", 152, "ACTION")).when().post("/api/v1/movies").jsonPath().getString("id");
        String id2 = given().contentType(ContentType.JSON).body(createMovieJson("Mad Max: Fury Road", 120, "ACTION")).when().post("/api/v1/movies").jsonPath().getString("id");
        String id3 = given().contentType(ContentType.JSON).body(createMovieJson("John Wick", 101, "ACTION")).when().post("/api/v1/movies").jsonPath().getString("id");

        given()
                .queryParam("genre", "ACTION")
        .when()
                .get("/api/v1/movies/search")
        .then()
                .statusCode(200)
                .body("$", hasSize(3));

        given().when().delete("/api/v1/movies/{movieId}", id1);
        given().when().delete("/api/v1/movies/{movieId}", id2);
        given().when().delete("/api/v1/movies/{movieId}", id3);
    }
}
