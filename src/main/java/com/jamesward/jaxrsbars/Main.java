package com.jamesward.jaxrsbars;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.Application;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.core.UriBuilder;

public class Main {
    public static void main(String[] args) throws IOException, URISyntaxException {
        final int port = System.getenv("PORT") != null ? Integer.valueOf(System.getenv("PORT")) : 8080;
        final URI baseUri = UriBuilder.fromUri("http://localhost/").port(port).build();
        final Application application = Application.builder(ResourceConfig.builder().packages(Main.class.getPackage().getName()).build()).build();
        final HttpServer httpServer = GrizzlyHttpServerFactory.createHttpServer(baseUri, application);
        System.in.read();
        httpServer.stop();
    }
}