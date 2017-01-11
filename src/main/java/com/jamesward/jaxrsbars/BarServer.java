package com.jamesward.jaxrsbars;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import org.glassfish.grizzly.http.server.CLStaticHttpHandler;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;

public class BarServer {

    static Datastore datastore;

    public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
        MongoClientURI mongoClientURI = new MongoClientURI(System.getenv("MONGOLAB_URI") != null ? System.getenv("MONGOLAB_URI") : "mongodb://127.0.0.1:27017/jaxrsbars");
        MongoClient mongoClient = new MongoClient(mongoClientURI);
        Morphia morphia = new Morphia();
        morphia.map(Bar.class);
        datastore = morphia.createDatastore(mongoClient, mongoClientURI.getDatabase());

        final String port = System.getenv("PORT")!=null ? System.getenv("PORT") : "8080";
        final URI apiUri = URI.create(String.format("http://0.0.0.0:%s/api", port));

        final ResourceConfig resourceConfig = new ResourceConfig(BarResource.class);
        HttpHandler httpHandler = new CLStaticHttpHandler(HttpServer.class.getClassLoader(), "/META-INF/resources/");

        final HttpServer server = GrizzlyHttpServerFactory.createHttpServer(apiUri, resourceConfig);
        server.getServerConfiguration().addHttpHandler(httpHandler, "/");

        System.out.println(String.format("Starting Grizzly at: http://localhost:%s", port));
    }
}
