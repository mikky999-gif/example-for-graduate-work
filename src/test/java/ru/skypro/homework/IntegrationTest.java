package ru.skypro.homework;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.*;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

    //  Integration-тесты для проверки работы всех эндпоинтов с H2 базой

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
    "spring.datasource.driverClassName=org.h2.Driver",
    "spring.datasource.username=sa",
    "spring.datasource.password=",
    "spring.jpa.hibernate.ddl-auto=update",
    "spring.jpa.show-sql=false",
    "app.images.root-path=/tmp/test-uploads",
    "app.images.url-prefix=/api/images/"
})
class IntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port;
    }

    @Test
    void contextLoads() {
        // Проверка, что контекст Spring загружен
        assertNotNull(restTemplate);
    }

    // Тесты для AuthController
    @Test
    void registerEndpoint_shouldReturnCreated() {
        String registerUrl = baseUrl + "/register";
        
        String requestBody = "{\n" +
                "    \"username\": \"testuser\",\n" +
                "    \"password\": \"1q2w3e4r\",\n" +
                "    \"firstName\": \"Иван\",\n" +
                "    \"lastName\": \"Иванов\",\n" +
                "    \"phone\": \"+79000000000\",\n" +
                "    \"role\": \"USER\"\n" +
                "}";

        HttpEntity<String> request = new HttpEntity<>(requestBody, headers());
        ResponseEntity<Void> response = restTemplate.postForEntity(registerUrl, request, Void.class);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
    }

    @Test
    void loginEndpoint_shouldReturnOk() {
        // Сначала зарегистрируем пользователя
        String registerUrl = baseUrl + "/register";
        String registerBody = "{\n" +
                "    \"username\": \"loginuser\",\n" +
                "    \"password\": \"1q2w3e4r\",\n" +
                "    \"firstName\": \"Петр\",\n" +
                "    \"lastName\": \"Петров\",\n" +
                "    \"phone\": \"+79000000000\",\n" +
                "    \"role\": \"USER\"\n" +
                "}";
        restTemplate.postForEntity(registerUrl, new HttpEntity<>(registerBody, headers()), Void.class);

        // Затем логинимся
        String loginUrl = baseUrl + "/login";
        String loginBody = "{\n" +
                "    \"username\": \"loginuser\",\n" +
                "    \"password\": \"1q2w3e4r\"\n" +
                "}";

        HttpEntity<String> request = new HttpEntity<>(loginBody, headers());
        ResponseEntity<Void> response = restTemplate.postForEntity(loginUrl, request, Void.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // Тесты для AdController
    @Test
    void getAllAdsEndpoint_shouldReturnOk() {
        String url = baseUrl + "/ads?offset=0&limit=10";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("\"count\""));
    }

    @Test
    void getAdByIdEndpoint_shouldReturnOk() {
        // Сначала создадим объявление
        String createUrl = baseUrl + "/ads";
        String createBody = "{\n" +
                "    \"title\": \"Тестовый товар\",\n" +
                "    \"description\": \"Описание товара\",\n" +
                "    \"price\": 1000\n" +
                "}";
        
        HttpEntity<String> createRequest = new HttpEntity<>(createBody, headers());
        restTemplate.postForEntity(createUrl, createRequest, Void.class);

        // Затем получим его по ID
        ResponseEntity<String> response = restTemplate.getForEntity(baseUrl + "/ads/1", String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // Тесты для CommentController
    @Test
    void getCommentsEndpoint_shouldReturnOk() {
        // Сначала создадим объявление и комментарий
        String createAdUrl = baseUrl + "/ads";
        String createAdBody = "{\n" +
                "    \"title\": \"Товар для комментариев\",\n" +
                "    \"description\": \"Описание\",\n" +
                "    \"price\": 500\n" +
                "}";
        restTemplate.postForEntity(createAdUrl, new HttpEntity<>(createAdBody, headers()), Void.class);

        String commentsUrl = baseUrl + "/ads/1/comments";
        ResponseEntity<String> response = restTemplate.getForEntity(commentsUrl, String.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // Тесты для UserController
    @Test
    void getCurrentUserEndpoint_shouldReturnUnauthorizedWithoutAuth() {
        String url = baseUrl + "/users/me";
        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    private HttpHeaders headers() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}