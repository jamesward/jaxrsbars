package com.jamesward.jaxrsbars;

import java.io.IOException;
import java.net.URISyntaxException;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoURI;
import org.glassfish.grizzly.http.server.*;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainer;
import org.glassfish.jersey.media.json.JsonJacksonModule;
import org.glassfish.jersey.server.ContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;

public class BarServer {

    public static DB mongoDB;

    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {

        MongoURI mongolabUri = new MongoURI(System.getenv("MONGOLAB_URI") != null ? System.getenv("MONGOLAB_URI") : "mongodb://127.0.0.1:27017/hello");
        Mongo m = new Mongo(mongolabUri);
        mongoDB = m.getDB(mongolabUri.getDatabase());
        if ((mongolabUri.getUsername() != null) && (mongolabUri.getPassword() != null)) {
            mongoDB.authenticate(mongolabUri.getUsername(), mongolabUri.getPassword());
        }
        
        
        final int port = System.getenv("PORT") != null ? Integer.valueOf(System.getenv("PORT")) : 8080;
        
        final HttpServer server = HttpServer.createSimpleServer("src/main/webapp", port);
        
        final ResourceConfig resourceConfig = new ResourceConfig(BarResource.class);
        resourceConfig.addModules(new JsonJacksonModule());
        
        final GrizzlyHttpContainer jerseyHandler = ContainerFactory.createContainer(GrizzlyHttpContainer.class, resourceConfig);
        
        server.getServerConfiguration().addHttpHandler(jerseyHandler, "/api");
        
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                server.stop();
            }
        });
        
        Thread.currentThread().join();
    }
}