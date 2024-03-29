package hexlet.code.controllers;

import hexlet.code.domain.Url;
import hexlet.code.domain.UrlCheck;
import hexlet.code.domain.query.QUrl;
import hexlet.code.domain.query.QUrlCheck;
import io.ebean.PagedList;
import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class UrlController {

    private static final int ROWS_PER_PAGE = 10;
    public static Handler listUrls = ctx -> {
        String term = ctx.queryParamAsClass("term", String.class).getOrDefault("");
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
        int rowsPerPage = ROWS_PER_PAGE;
        int offset = (page - 1) * rowsPerPage;

        PagedList<Url> pagedUrls = new QUrl()
                .name.icontains(term)
                .setFirstRow(offset)
                .setMaxRows(rowsPerPage)
                .orderBy()
                .id.asc()
                .findPagedList();

        List<Url> urls = pagedUrls.getList();
        Map<String, UrlCheck> urlCheckChoice = new HashMap<>();

        for (var url: urls) {

            UrlCheck urlCheck = new QUrlCheck()
                    .url.id.equalTo(url.getId())
                    .orderBy()
                    .createdAt.desc()
                    .setMaxRows(1)
                    .findOne();

            urlCheckChoice.put(url.getName(), urlCheck);
        }

        int lastPage = pagedUrls.getTotalPageCount() + 1;
        int currentPage = pagedUrls.getPageIndex() + 1;
        List<Integer> pages = IntStream
                .range(1, lastPage)
                .boxed()
                .collect(Collectors.toList());

        ctx.attribute("urls", urls);
        ctx.attribute("urlCheckChoice", urlCheckChoice);
        ctx.attribute("term", term);
        ctx.attribute("pages", pages);
        ctx.attribute("currentPage", currentPage);
        ctx.render("urls/index.html");

    };

    public static Handler createUrl = ctx -> {
        String name = ctx.formParam("url");

        URL urlObject;

        try {

            urlObject = new URL(name);

        } catch (MalformedURLException ignored) {
            ctx.sessionAttribute("flash", "Некорректный URL");
            ctx.sessionAttribute("flash-type", "danger");
            ctx.redirect("/");
            return;
        }

        var newName = urlObject.getProtocol() + "://" + urlObject.getHost();

        String normalizedName = newName + (urlObject.getPort() == -1 ? "" :  ":" + urlObject.getPort());

        Url existingUrl = new QUrl()
                .name.equalTo(normalizedName)
                .findOne();

        if (existingUrl != null) {
            ctx.sessionAttribute("flash", "Страница уже существует");
            ctx.sessionAttribute("flash-type", "danger");
            ctx.redirect("/urls");
            return;
        }

        Url url = new Url(normalizedName);
        url.save();

        ctx.sessionAttribute("flash", "Страница успешно добавлена");
        ctx.sessionAttribute("flash-type", "success");
        ctx.redirect("/urls");
    };

    public static Handler showUrl = ctx -> {
        long id = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);

        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();

        if (url == null) {
            throw new NotFoundResponse();
        }

        List<UrlCheck> urlChecks = url.getUrlChecks();

        ctx.attribute("url", url);
        ctx.attribute("url_checks", urlChecks);
        ctx.render("urls/show.html");
    };

    public static Handler checkUrl = ctx -> {

        long id = ctx.pathParamAsClass("id", Long.class).getOrDefault(null);

        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();

        try {
            HttpResponse<String> response = Unirest
                .post(url.getName())
                .asString();

            Document body = Jsoup.parse(response.getBody());

            var checkStatusCode = response.getStatus();
            var checkTitle = body.title();
            var checkH1 = body.selectFirst("h1") != null ? body.selectFirst("h1").text() : null;
            var checkDescription = body.selectFirst("meta[name=description]") != null
                    ? body.selectFirst("meta[name=description]").attr("content") : null;

            UrlCheck urlCheck = new UrlCheck(checkStatusCode, checkTitle, checkH1, checkDescription, url);
            urlCheck.save();

            ctx.sessionAttribute("flash", "Страница успешно проверена");
            ctx.sessionAttribute("flash-type", "success");

        } catch (UnirestException e) {
            ctx.sessionAttribute("flash", "Не удалось проверить страницу");
            ctx.sessionAttribute("flash-type", "danger");
        }

        ctx.redirect("/urls/" + id);
    };
}
