package com.jamesward.jaxrsbars;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;
import org.glassfish.grizzly.http.server.*;
import org.glassfish.jersey.grizzly2.GrizzlyHttpServerFactory;
import org.glassfish.jersey.media.json.JsonJacksonModule;
import org.glassfish.jersey.server.Application;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.core.UriBuilder;

public class BarServer {

    public static DB mongoDB;

    public static String contentUrl;

    private static final String CONTENT_PATH = "/content";

    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
        final int port = System.getenv("PORT") != null ? Integer.parseInt(System.getenv("PORT")) : 8080;
        final URI baseUri = UriBuilder.fromUri("http://0.0.0.0/").port(port).build();
        final Application application = Application.builder(ResourceConfig.builder().packages(BarServer.class.getPackage().getName()).build()).build();
        application.addModules(new JsonJacksonModule());
        final HttpServer httpServer = GrizzlyHttpServerFactory.createHttpServer(baseUri, application);
        httpServer.getServerConfiguration().addHttpHandler(new StaticHttpHandler("src/main/webapp"), CONTENT_PATH);

        for (NetworkListener networkListener : httpServer.getListeners()) {
            if (System.getenv("FILE_CACHE_ENABLED") == null) {
                networkListener.getFileCache().setEnabled(false);
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                httpServer.stop();
            }
        });

        MongoURI mongoUri = new MongoURI(System.getenv("ORMONGO_URL") != null ? System.getenv("ORMONGO_URL") : "mongodb://127.0.0.1:27017/hello");
        Mongo m = new Mongo(mongoUri);

        String mongoDb = System.getenv("ORMONGO_DATABASE") != null ? System.getenv("ORMONGO_DATABASE") : mongoUri.getDatabase();
        mongoDB = m.getDB(mongoDb);

        String mongoUsername = System.getenv("ORMONGO_USERNAME") != null ? System.getenv("ORMONGO_USERNAME") : mongoUri.getUsername();
        char[] mongoPassword = System.getenv("ORMONGO_PASSWORD") != null ? System.getenv("ORMONGO_PASSWORD").toCharArray() : mongoUri.getPassword();

        if ((mongoUsername != null) && (mongoPassword != null)) {
            mongoDB.authenticate(mongoUsername, mongoPassword);
        }

        contentUrl = System.getenv("CONTENT_URL") != null ? System.getenv("CONTENT_URL") : CONTENT_PATH;

        Thread.currentThread().join();
    }
}
