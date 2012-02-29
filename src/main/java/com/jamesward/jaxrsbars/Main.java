package com.jamesward.jaxrsbars;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.Application;
import org.glassfish.jersey.server.ResourceConfig;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import javax.ws.rs.core.UriBuilder;

public class Main {
    public static void main(String[] args) throws IOException, URISyntaxException {
        final int port = System.getenv("PORT") != null ? Integer.valueOf(System.getenv("PORT")) : 8080;
        final URI baseUri = UriBuilder.fromUri("http://0.0.0.0/").port(port).build();
        final Application application = Application.builder(ResourceConfig.builder().packages(Main.class.getPackage().getName()).build()).build();
        final HttpServer httpServer = GrizzlyHttpServerFactory.createHttpServer(baseUri, application);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                httpServer.stop();
            }
        });

        while (true) {
            System.in.read();
        }
    }
}