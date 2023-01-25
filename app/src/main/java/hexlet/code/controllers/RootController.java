package hexlet.code.controllers;

import hexlet.code.domain.Url;
import io.javalin.http.Handler;

public class RootController {

    public static Handler newUrl = ctx -> {
        Url url = new Url();

        ctx.attribute("url", url);
        ctx.render("/index.html");
    };

}
