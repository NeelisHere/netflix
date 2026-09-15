package com.netflix.integration_tests;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VideoServiceTests {
    public static final String SMALL_VIDEO_FILE = "test_file_3mb.mp4";
    public static final String LARGE_VIDEO_FILE = "test_file_8.5mb.mp4";
    public static final String VALID_MOVIE_ID = "479ca164-fb4c-49ee-8112-e09d776be9ea";

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = 8082;
        RestAssured.basePath = "/api/v1/video";
    }

    private File getResourceFile(String fileName) {
        return new File("C:\\Users\\NeelabhPaul\\Desktop\\netflix\\test-files\\" + fileName);
    }

    @Test
    @Order(1)
    void shouldUploadVideoSuccessfully() {

        File file = getResourceFile(SMALL_VIDEO_FILE);

        given()
                .multiPart("file", file)
                .contentType(ContentType.MULTIPART)
                .when()
                .post("/upload/{movieId}", VALID_MOVIE_ID)
                .then()
                .log().all()
                .statusCode(200)
                .body("message", containsString("Video uploaded successfully"))
                .body("message", containsString("key:"));
    }

    @Test
    @Order(2)
    void getMovieById_shouldReturn200WithVideoStatusUploaded() throws InterruptedException {
        Thread.sleep(20_000);

        given()
                .baseUri("http://localhost")
                .port(8081)
                .basePath("/api/v1/movies")
        .when()
                .get("/{movieId}", VALID_MOVIE_ID)
        .then()
                .statusCode(200)
                .body("videoStatus", equalTo("UPLOADED"));
    }

    @Test
    @Order(3)
    void shouldRejectVideoLargerThan3MB() throws IOException {

        File file = getResourceFile(LARGE_VIDEO_FILE);

        given()
                .multiPart("file", file)
                .contentType(ContentType.MULTIPART)
                .when()
                .post("/upload/{movieId}", VALID_MOVIE_ID)
                .then()
                .log().all()
                .statusCode(413)
                .body("message", equalTo("Maximum upload size exceeded"))
                .body("httpStatus", equalTo("413 CONTENT_TOO_LARGE"))
                .body("timestamp", notNullValue());
    }
}
