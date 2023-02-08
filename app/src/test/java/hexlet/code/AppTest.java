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
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public final class AppTest {

    private static final int RESPONSE_NUMBER_200 = 200;
    private static final int RESPONSE_NUMBER_302 = 302;
    private static final String FIXTURES_DIRECTORY = "src/test/resources/fixtures";
    private static final int PAGE = 10;

    private static Javalin app;
    private static String baseUrl;
    private static Database database;

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

            String name = "https://www.ebean.io";

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
                    .field("name", name)
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
            String samplePage = Files.readString(Paths.get(FIXTURES_DIRECTORY, "sample.html"));

            MockWebServer mockServer = new MockWebServer();
            String samplePageUrl = mockServer.url("/").toString();
            mockServer.enqueue(new MockResponse().setBody(samplePage));

            HttpResponse<String> response = Unirest
                    .post(baseUrl + "/urls/")
                    .field("url", samplePageUrl)
                    .asEmpty();

            Url url = new QUrl()
                    .name.equalTo(samplePageUrl.substring(0, samplePageUrl.length() - 1))
                    .findOne();

            assertThat(url).isNotNull();

            HttpResponse<String> response1 = Unirest
                    .post(baseUrl + "/urls/" + url.getId() + "/checks")
                    .asEmpty();

            HttpResponse<String> response2 = Unirest
                    .get(baseUrl + "/urls/" + url.getId())
                    .asString();

            assertThat(response2.getStatus()).isEqualTo(RESPONSE_NUMBER_200);

            UrlCheck check = new QUrlCheck()
                    .findList().get(0);

            assertThat(check).isNotNull();
            assertThat(check.getUrl().getId()).isEqualTo(url.getId());

            assertThat(response2.getBody()).contains("Sample title");
            assertThat(response2.getBody()).contains("Sample description");
            assertThat(response2.getBody()).contains("Sample header");

            mockServer.shutdown();
        }
    }

    @Nested
    class PaginationTest {
        @Test
        void testPagination() {
            for (int i = 1; i <= PAGE + 1; i++) {
                String testWebsite = String.format("http://localhost:%d", i);
                Url url = new Url(testWebsite);
                url.save();
            }
            HttpResponse<String> response1 = Unirest
                    .get(baseUrl + "/urls")
                    .asString();

            String content1 = response1.getBody();
            assertThat(content1).contains("http://localhost:1");
            assertThat(content1).contains("http://localhost:10");
            assertThat(content1.contains("http://localhost:11")).isFalse();

            HttpResponse<String> response2 = Unirest
                    .get(baseUrl + "/urls?page=2")
                    .asString();
            String content2 = response2.getBody();
            assertThat(content2).contains("http://localhost:11");
        }
    }
}
