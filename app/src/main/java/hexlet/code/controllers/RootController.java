package hexlet.code.controllers;

import io.javalin.http.Handler;

import java.util.Map;

public class RootController {

    public static Handler newUrl = ctx -> {

        ctx.attribute("url", Map.of());
        ctx.render("index.html");
    };

}
