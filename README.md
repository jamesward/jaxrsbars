Modern Web Apps with JAX-RS, MongoDB, JSON, and jQuery
======================================================

We are in the midst of a paradygm shift that will dramatically change how many of us build and deploy our software.  In the late 90's software began moving to the web.  The prevelence of the web browser provided our users an easy way to interact with our software without having to install thick client apps that were hard to upgrade and usually not cross-platform.  But we only used the browser as a dumb terminal with very limited capabilities.  All of the logic of the application happened on the server and each interaction required a roundtrip with the server which consequently had to store tons of UI state information.  In some ways this model is better than the thick-client approach but we have uncovered many of it's drawbacks and are now working our way back towards the client/server architecture - this time the modern web browser is the universal thick-client.

A few years ago we realized that the browser could be used for more than just rendering markup.  In the Ajax revolution many of us began doing partial page refreshes and client-side form validation.  But to go further we needed more modern web browsers with faster JavaScript execution engines, more powerful CSS capabilities, and more APIs.  This is exactly what is now happening under the "HTML5" banner.  HTML5 technically refers to a collection of browser standards, but to most developers it really means "the browser is a now universal think-client".  The splurge in mobile apps is also driving this great transition back to a client/server architecture.  Ultimately what this all means for you is that web application architectures are changing.


The Web Client/Server Architecture
----------------------------------

The modern web application architecure moves the User Interface to the client where all user interactions are handled on the client-side.  All UI state also moves to the client-side.  Then the client just makes calls to the server when it needs to access shared data or communicate with other clients / systems.  The client talks to the server through HTTP using a RESTful pattern or possibly the newer Web Sockets protocol which allows for bidirectional communication.  The RESTful pattern is basically a way to map the standard HTTP verbs (like GET, POST, PUT, DELETE) to actions the client wants to perform on the back-end / shared data.  Today the data is typically serialized into JSON (JavaScript Object Notation) because it is simple and universally supported.  The RESTful services should also be stateless, meaning no data should be held in the web tier between requests (with the exception of cached data).  Ultimately this turns the web tier into a simple proxy layer that provides an endpoint for serialized data.

There are many different ways to build a browser-based client but for this example I will use jQuery and JavaScript since they are the most common.  There are also many ways to expose the RESTful services.  For this example I will use JAX-RS since it is the Java standard for RESTful services.  For back-end data persistence this example will use MongoDB, a NoSQL database.

You can get all of the code for this example from: http://github.com/jamesward/jaxrsbars

The JAX-RS & MongoDB Server
---------------------------

Lets start by setting up a server.  Typically this means using Tomcat, creating WAR files, etc.  But most people are now gravitating towards a containerless approach that makes it easier to have dev/prod parity and works well for quick development iterations.  In the containerless approach the HTTP handler is just another library in your application.  Instead of deploying a partial application into a container, the application contains everything it needs and is "run".  Lets start with a Maven build that includes the dependencies needed to setup a containerless JAX-RS server using Jersey, Jackson, and the Grizzly HTTP handling library.  Here is the `pom.xml` file for the Maven build:

    <?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">

        <modelVersion>4.0.0</modelVersion>
        <groupId>com.jamesward</groupId>
        <artifactId>jaxrsbars</artifactId>
        <version>1.0-SNAPSHOT</version>

        <dependencies>
            <dependency>
                <groupId>org.glassfish.jersey.core</groupId>
                <artifactId>jersey-common</artifactId>
                <version>2.0-m01</version>
            </dependency>
            <dependency>
                <groupId>org.glassfish.jersey.core</groupId>
                <artifactId>jersey-server</artifactId>
                <version>2.0-m01</version>
            </dependency>
            <dependency>
                <groupId>org.glassfish.jersey.core</groupId>
                <artifactId>jersey-client</artifactId>
                <version>2.0-m01</version>
            </dependency>
            <dependency>
                <groupId>org.glassfish.jersey.containers</groupId>
                <artifactId>jersey-container-grizzly2-http</artifactId>
                <version>2.0-m01</version>
            </dependency>
            <dependency>
                <groupId>org.codehaus.jackson</groupId>
                <artifactId>jackson-jaxrs</artifactId>
                <version>1.9.5</version>
            </dependency>
            <dependency>
                <groupId>org.codehaus.jackson</groupId>
                <artifactId>jackson-xc</artifactId>
                <version>1.9.5</version>
            </dependency>
            <dependency>
                <groupId>org.mongodb</groupId>
                <artifactId>mongo-java-driver</artifactId>
                <version>2.7.3</version>
            </dependency>
            <dependency>
                <groupId>net.vz.mongodb.jackson</groupId>
                <artifactId>mongo-jackson-mapper</artifactId>
                <version>1.4.0</version>
            </dependency>
        </dependencies>

        <build>
            <plugins>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <version>2.3.2</version>
                    <configuration>
                        <source>1.6</source>
                        <target>1.6</target>
                    </configuration>
                </plugin>
                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-dependency-plugin</artifactId>
                    <version>2.4</version>
                    <executions>
                        <execution>
                            <id>copy-dependencies</id>
                            <phase>package</phase>
                            <goals><goal>copy-dependencies</goal></goals>
                        </execution>
                    </executions>
                </plugin>
            </plugins>
        </build>

    </project>

Notice that this project doesn't use WAR packaging.  Instead the compiled classes will go into a JAR file.  This means I need to provide a Java class with a good 'ole "static void main" method that starts the server.  Then assuming I have my Java classpath set to include the necessary dependencies, I can simply start the server with the `java` command.  Using the `maven-dependency-plugin` provides a simple way to copy the project dependencies into a single directory, thus making setting the classpath easy.

Here is the `com.jamesward.jaxrsbars.BarServer` class that starts the Grizzly server, sets up a MongoDB connection, and content URL (which we will find out more about shortly):

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
                networkListener.getFileCache().setEnabled(false);
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

Notice that the HTTP port, MongoDB connection URI, and the content URL have default values for local development but they can also be set via environment variables.  This provides a simple way to configure the application for different environments.  At the end of this article we will run this application on Heroku where those values will come from environment variables.  For this example I've also turned off the static file cache since we will use another mechanism for doing that and with caching on we wouldn't be able to do simply make changes to our client-side static files and then hit reload in the browser.

resources/META-INF/services


Bar.java


BarResource.java



jQuery Client
-------------

BarResource.java


index.js



Deploying on the Cloud
----------------------




Heroku

 - Procfile


CDN
