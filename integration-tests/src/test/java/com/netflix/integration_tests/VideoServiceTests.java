package com.netflix.integration_tests;


import io.restassured.RestAssured;
import io.restassured.config.HttpClientConfig;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class VideoServiceTests {
    public static final String SMALL_VIDEO_FILE = "1.5mb.mp4";
    public static final String LARGE_VIDEO_FILE = "3mb.mp4";
    public static final String VALID_USER_ID = "f379b400-9271-4c41-a337-147a9e3fdf3b";

    @BeforeAll
    static void setUp() {
        RestAssured.baseURI = "http://localhost/api/v1/video";
        RestAssured.port = 8082;
    }

    private File getResourceFile(String fileName) {
        return new File("D:\\IdeaProjects\\netflix\\test_mp4_files\\" + fileName);
    }

    @Test
    @Order(1)
    void shouldUploadVideoSuccessfully() {

        File file = getResourceFile(SMALL_VIDEO_FILE);

        given()
                .multiPart("file", file)
                .contentType(ContentType.MULTIPART)
                .when()
                .post("/upload/{movieId}", VALID_USER_ID)
                .then()
                .log().all()
                .statusCode(200)
                .body("message", containsString("Video uploaded successfully"))
                .body("message", containsString("key:"));
    }

    @Test
    @Order(2)
    void shouldRejectVideoLargerThan2MB() throws IOException {

        File file = getResourceFile(LARGE_VIDEO_FILE);

        given()
                .multiPart(
                        "file",
                        file.getName(),
                        Files.readAllBytes(file.toPath()),
                        "video/mp4"
                )
                .contentType(ContentType.MULTIPART)
                .when()
                .post("/upload/{movieId}", VALID_USER_ID)
                .then()
                .log().all()
                .statusCode(413)
                .body("message", equalTo("Maximum upload size exceeded"))
                .body("httpStatus", equalTo("413 CONTENT_TOO_LARGE"))
                .body("timestamp", notNullValue());
    }
}
