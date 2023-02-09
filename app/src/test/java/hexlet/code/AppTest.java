package hexlet.code;

import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrlCheck;
import io.ebean.DB;
import io.ebean.Database;
import hexlet.code.domain.Url;
import hexlet.code.domain.query.QUrl;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public final class AppTest {

    @Test
    void testInit() {
        assertThat(true).isEqualTo(true);
    }
    private static final int RESPONSE_NUMBER_200 = 200;
    private static final int RESPONSE_NUMBER_302 = 302;

    private static Javalin app;
    private static String baseUrl;
    private static Database database;
    private static MockWebServer mockServer;

    private static Path getFixturePath() {
        return Paths.get("src", "test", "resources", "fixtures", "siteTest.html")
                .toAbsolutePath().normalize();
    }
    private static String readFixture() throws IOException {
        Path filePath = getFixturePath();
        return Files.readString(filePath).trim();
    }

    @BeforeAll
    public static void beforeAll() {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
        database = DB.getDefault();
    }

    @AfterAll
    public static void afterAll() {
        database.script().run("/truncate.sql");
        app.stop();
    }

    @Nested
    class RootTest {
        @Test
        void testNewUrl() {
            HttpResponse<String> response = Unirest
                    .get(baseUrl)
                    .asString();

            assertThat(response.getStatus()).isEqualTo(RESPONSE_NUMBER_200);
            assertThat(response.getBody()).contains("Анализатор страниц");
        }
    }
    @Nested
    class UrlTest {
        private final String testWebsite = "https://www.example.com";

        @BeforeEach
        void addTestWebsite() {
            database.script().run("/truncate.sql");
            Url testUrl = new Url(testWebsite);
            testUrl.save();
        }
        @Test
        void testUrls() {

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String content = response.getBody();

            assertThat(response.getStatus()).isEqualTo(RESPONSE_NUMBER_200);
            assertThat(content).contains(testWebsite);
        }
        @Test
        void testUrl() {

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls/1")
                    .asString();
            String content = response.getBody();

            assertThat(response.getStatus()).isEqualTo(RESPONSE_NUMBER_200);
            assertThat(content).contains(testWebsite);
        }

        @Test
        void testCreateUrl() {

            String name = "https://www.google.com";

            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("name", name)
                    .asString();

            assertThat(responsePost.getStatus()).isEqualTo(RESPONSE_NUMBER_302);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String content = response.getBody();

            assertThat(response.getStatus()).isEqualTo(RESPONSE_NUMBER_200);
            assertThat(content).contains(name);
            assertThat(content).contains("Страница успешно добавлена");

            Url actualUrl = new QUrl()
                    .name.equalTo(name)
                    .findOne();

            assertThat(actualUrl).isNotNull();
            assertThat(actualUrl.getName()).isEqualTo(name);
        }

        @Test
        void testCreateUrlWithIncorrectName1() {

            String name = "test";

            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("url", name)
                    .asString();

            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/");

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/")
                    .asString();
            String content = response.getBody();

            assertThat(content).contains("Некорректный URL");
        }

        @Test
        void testCreateUrlAlreadyExistsName() {

            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("name", testWebsite)
                    .asString();

            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

            HttpResponse<String> response = Unirest
                    .get(baseUrl + "/urls")
                    .asString();
            String content = response.getBody();

            assertThat(content).contains(testWebsite);
            assertThat(content).contains("Страница уже существует");
        }
        @Test
        void testCheckUrl() throws IOException {

            mockServer = new MockWebServer();
            MockResponse mockedResponse = new MockResponse()
                    .setBody(readFixture());
            mockServer.enqueue(mockedResponse);
            mockServer.start();

            String websiteAddress = mockServer.url("/").toString().replaceAll("/$", "");
            System.out.println(websiteAddress);

            Unirest
                    .post(baseUrl + "/urls")
                    .field("url", websiteAddress)
                    .asEmpty();

            HttpResponse<String> responsePost = Unirest
                    .post(baseUrl + "/urls")
                    .field("name", websiteAddress)
                    .asString();

            assertThat(responsePost.getStatus()).isEqualTo(RESPONSE_NUMBER_302);
            assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

            HttpResponse<String> response = Unirest
                    .post(baseUrl + "/urls/1/checks")
                    .asString();

            HttpResponse<String> response2 = Unirest
                    .get(baseUrl + "/urls/1")
                    .asString();

            UrlCheck urlCheck = new QUrlCheck()
                    .findList()
                    .get(0);

            assertThat(response.getStatus()).isEqualTo(RESPONSE_NUMBER_302);
            assertThat(response2.getStatus()).isEqualTo(RESPONSE_NUMBER_200);
            assertThat(response2.getBody()).contains("Страница успешно проверена");

            assertThat(urlCheck).isNotNull();
            assertThat(urlCheck.getUrl().getId()).isEqualTo(1);

            Url actualUrl = new QUrl()
                    .name.equalTo(websiteAddress)
                    .findOne();

            assertThat(actualUrl).isNotNull();
            assertThat(actualUrl.getName()).isEqualTo(websiteAddress);

            mockServer.shutdown();
        }
    }
}
