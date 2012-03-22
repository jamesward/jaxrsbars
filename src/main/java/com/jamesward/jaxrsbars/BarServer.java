package com.jamesward.jaxrsbars;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;
import org.glassfish.grizzly.http.server.*;
import org.glassfish.jersey.grizzly2.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.Application;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.core.UriBuilder;

public class BarServer {

    public static DB mongoDB;
    
    public static String contentUrl;

    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
        final int port = System.getenv("PORT") != null ? Integer.valueOf(System.getenv("PORT")) : 8080;
        final URI baseUri = UriBuilder.fromUri("http://0.0.0.0/").port(port).build();
        final Application application = Application.builder(ResourceConfig.builder().packages(BarServer.class.getPackage().getName()).build()).build();
        final HttpServer httpServer = GrizzlyHttpServerFactory.createHttpServer(baseUri, application);
        httpServer.getServerConfiguration().addHttpHandler(new StaticHttpHandler("src/main/webapp"), "/content");
        
        for (NetworkListener networkListener : httpServer.getListeners()) {
            if (System.getenv("PROD") == null) {
                // disable the file cache unless we are in PROD mode
                networkListener.getFileCache().setEnabled(false);
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                httpServer.stop();
            }
        });

        MongoURI mongolabUri = new MongoURI(System.getenv("MONGOLAB_URI") != null ? System.getenv("MONGOLAB_URI") : "mongodb://127.0.0.1:27017/hello");
        Mongo m = new Mongo(mongolabUri);
        mongoDB = m.getDB(mongolabUri.getDatabase());
        if ((mongolabUri.getUsername() != null) && (mongolabUri.getPassword() != null)) {
            mongoDB.authenticate(mongolabUri.getUsername(), mongolabUri.getPassword());
        }

        contentUrl = System.getenv("CONTENT_URL") != null ? System.getenv("CONTENT_URL") : "/content/";

        Thread.currentThread().join();
    }
}