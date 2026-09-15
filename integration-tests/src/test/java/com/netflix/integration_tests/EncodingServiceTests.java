package com.netflix.integration_tests;

import org.junit.jupiter.api.*;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;


@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class EncodingServiceTests {
    public static final String VALID_MOVIE_ID = "cbfc6799-12aa-4090-9d2a-dd1bea6e28ab";
    public static final String S3_BUCKET_NAME = "netflix-streaming-videos-440744257191-us-east-1-an";

    @Test
    @Order(1)
    void getMovieById_shouldReturn200WithVideoStatusEncoded() {
        given()
                .baseUri("http://localhost")
                .port(8081)
                .basePath("/api/v1/movies")
                .when()
                .get("/{movieId}", VALID_MOVIE_ID)
                .then()
                .statusCode(200)
                .body("videoStatus", equalTo("ENCODED"))
                .body("videoKey", notNullValue())
                .body("videoKey", startsWith("raw/" + VALID_MOVIE_ID))
                .body("videoKey", containsString("_test_file_3mb.mp4"))
                .body("hlsUrl", notNullValue())
                .body("hlsUrl", equalTo("https://" + S3_BUCKET_NAME + ".s3.us-east-1.amazonaws.com/encoded/" + VALID_MOVIE_ID + "/master.m3u8"));
    }
}
