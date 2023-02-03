package hexlet.code;

//import io.ebean.DB;
//import io.ebean.Database;
import hexlet.code.domain.Url;
import hexlet.code.domain.query.QUrl;
import io.ebean.Transaction;
import io.javalin.Javalin;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
//import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public final class AppTest {

    private static final int RESPONSE_NUMBER_200 = 200;
    private static final int RESPONSE_NUMBER_302 = 302;
    private static final int RESPONSE_NUMBER_422 = 422;

    private static Javalin app;
    private static String baseUrl;
    private static Transaction transaction;
    //private static Database database;

    @BeforeAll
    public static void beforeAll() {
        app = App.getApp();
        app.start(0);
        int port = app.port();
        baseUrl = "http://localhost:" + port;
        //database = DB.getDefault();
    }

    @AfterAll
    public static void afterAll() {
        app.stop();
    }

//    @BeforeEach
//    void beforeEach() {
//        transaction = DB.beginTransaction();
//    }

    @AfterEach
    void afterEach() {
        transaction.rollback();
    }

    @Test
    void testNewUrl() {
        HttpResponse<String> response = Unirest
                .get(baseUrl)
                .asString();

        assertThat(response.getStatus()).isEqualTo(RESPONSE_NUMBER_200);
    }
    @Test
    void testUrls() {

        HttpResponse<String> response = Unirest
                .get(baseUrl + "/urls")
                .asString();
        String content = response.getBody();

        assertThat(response.getStatus()).isEqualTo(RESPONSE_NUMBER_200);
        assertThat(content).contains("https://some-domain.org:8080");
        assertThat(content).contains("https://ruletka.ru");
    }
    @Test
    void testUrl() {

        HttpResponse<String> response = Unirest
                .get(baseUrl + "/users/20")
                .asString();
        String content = response.getBody();

        assertThat(response.getStatus()).isEqualTo(RESPONSE_NUMBER_200);
        assertThat(content).contains("https://some-domain.org:8080");
    }
    @Test
    void testCreateUrl() {

        String name = "https://les.ru";

        HttpResponse<String> responsePost = Unirest
                .post(baseUrl + "/urls")
                .field("firstName", name)
                .asEmpty();

        assertThat(responsePost.getStatus()).isEqualTo(RESPONSE_NUMBER_302);
        assertThat(responsePost.getHeaders().getFirst("Location")).isEqualTo("/urls");

        Url actualUrl = new QUrl()
                .name.equalTo(name)
                .findOne();

        assertThat(actualUrl).isNotNull();
        assertThat(actualUrl.getName()).isEqualTo(name);
    }
    @Test
    void testCreateUrlWithIncorrectName1() {

        String name = "";

        HttpResponse<String> responsePost = Unirest
                .post(baseUrl + "/urls")
                .field("name", name)
                .asString();

        assertThat(responsePost.getStatus()).isEqualTo(RESPONSE_NUMBER_422);
    }
    @Test
    void testCreateUrlAlreadyExistsName() {

        String name = "https://some-domain.org:8080";

        HttpResponse<String> responsePost = Unirest
                .post(baseUrl + "/urls")
                .field("name", name)
                .asString();

        assertThat(responsePost.getStatus()).isEqualTo(RESPONSE_NUMBER_422);
    }

}
