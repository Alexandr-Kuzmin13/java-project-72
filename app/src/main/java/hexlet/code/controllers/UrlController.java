package hexlet.code.controllers;

import hexlet.code.domain.Url;
import hexlet.code.domain.query.QUrl;
import io.javalin.http.Handler;
import io.javalin.http.NotFoundResponse;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

public class UrlController {

    //private static final int ROWS_PER_PAGE = 10;

    public static Handler newUrl = ctx -> {

        ctx.attribute("url", Map.of());
        ctx.render("index.html");
    };

    /*public static Handler listUrls = ctx -> {
        //String term = ctx.queryParamAsClass("term", String.class).getOrDefault("");
        int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1) - 1;
        int rowsPerPage = 10;

        PagedList<Url> pagedUrls = new QUrl()
                //.name.icontains(term)
                .setFirstRow(page * rowsPerPage)
                .setMaxRows(rowsPerPage)
                .orderBy()
                .id.asc()
                .findPagedList();

        List<Url> urls = pagedUrls.getList();

        int lastPage = pagedUrls.getTotalPageCount() + 1;
        int currentPage = pagedUrls.getPageIndex() + 1;
        List<Integer> pages = IntStream
                .range(1, lastPage)
                .boxed()
                .collect(Collectors.toList());

        ctx.attribute("urls", urls);
        //ctx.attribute("term", term);
        ctx.attribute("pages", pages);
        ctx.attribute("currentPage", currentPage);
        ctx.render("urls/index.html");
    };*/

    public static Handler listUrls = ctx -> {
        /*int page = ctx.queryParamAsClass("page", Integer.class).getOrDefault(1);
        int rowsPerPage = ROWS_PER_PAGE;
        int offset = (page - 1) * rowsPerPage;

        PagedList<Url> pagedUrls = new QUrl()
                .setFirstRow(offset)
                .setMaxRows(rowsPerPage)
                .orderBy()
                .id.asc()
                .findPagedList();

        List<Url> urls = pagedUrls.getList();

        ctx.attribute("urls", urls);
        ctx.attribute("page", page);
        ctx.render("urls/index.html");*/

        List<Url> urls = new QUrl()
                .orderBy()
                    .id.asc()
                .findList();

        ctx.attribute("urls", urls);
        ctx.render("urls/index.html");
    };

    public static Handler createUrl = ctx -> {
        String name = ctx.formParam("name");

        String newName = "";

        try {
            var urlName = new URL(name);

            if (urlName.getPort() == -1) {
                newName = urlName.getProtocol() + "://" + urlName.getHost();
            } else {
                newName = urlName.getProtocol() + "://" + urlName.getHost() + ":" + urlName.getPort();
            }
        } catch (MalformedURLException ignored) {
            Url url = new Url(name);
            ctx.sessionAttribute("flash", "Некорректный URL");
            ctx.sessionAttribute("flash-type", "danger");
            ctx.attribute("url", url);
            ctx.render("index.html");
            return;
        }

        Url errorUrl = new QUrl()
                .name.equalTo(newName)
                .findOne();

        if (errorUrl != null) {
            ctx.sessionAttribute("flash", "Страница уже существует");
            ctx.sessionAttribute("flash-type", "danger");
            ctx.attribute("url", errorUrl);
            ctx.render("index.html");
            return;
        }

        Url url = new Url(newName);
        url.save();

        ctx.sessionAttribute("flash", "Страница успешно добавлена");
        ctx.sessionAttribute("flash-type", "success");
        ctx.redirect("/urls");
    };

    public static Handler showUrl = ctx -> {
        int id = ctx.pathParamAsClass("id", Integer.class).getOrDefault(null);

        Url url = new QUrl()
                .id.equalTo(id)
                .findOne();

        if (url == null) {
            throw new NotFoundResponse();
        }

        ctx.attribute("url", url);
        ctx.render("urls/show.html");
    };
}
