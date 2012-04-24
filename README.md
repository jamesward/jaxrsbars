Modern Web Apps with JAX-RS, MongoDB, JSON, and jQuery
======================================================

We are in the midst of a paradigm shift that will dramatically change how many of us build and deploy software.  In the late 90's software began moving to the web, and the prevalence of the web browser provided users with an easy way to interact with software without having to install thick client apps that were hard to upgrade and usually, not available cross-platform. We only used the browser as a dumb terminal with very limited capabilities. The 'logic of the application' happened on the server and each interaction required a roundtrip to and from the server which, as a result had to store tons of UI state information. In some ways this model is better than the thick-client approach but in the ensuing years we have uncovered many drawbacks to this approach and today, we are now working our way back towards client/server architecture - and this time the modern web browser 'is' the universal thick-client.

Over the past few years it has been common for web apps to use the browser as more than a HTML-capable dumb terminal. In the Ajax revolution many of us began doing partial page refreshes and client-side form validation. But to go further we needed more modern web browsers with their faster JavaScript execution engines, more powerful CSS capabilities, and more APIs. Today this is happening under the "HTML5" banner. While HTML5 technically refers to a collection of browser standards, to most developers it really means "the browser is a now universal think-client." The growth in mobile apps is also driving this great transition back to a client/server architecture. Ultimately what this all means for you and I is that web application architectures are changing.


The Web Client/Server Architecture
----------------------------------

Modern web application architecture moves the UI to the client, where all user interactions are handled on the client-side. All UI state, too, moves to the client-side. The client then just makes calls to the server when it needs to access shared data or communicate with other clients / systems.  The client talks to the server through HTTP using a RESTful pattern or possibly the newer WebSockets protocol which allows for bidirectional communication. This RESTful pattern is basically a way to map standard HTTP verbs (like GET, POST, PUT, DELETE) to actions the client wants to perform on the back-end / shared data. Today data is typically serialized into JSON (JavaScript Object Notation) because it is simple and universally supported. The RESTful services should also be stateless, and no data should be held in the web tier between requests (with the possible exception of cached data). Ultimately, this turns the web tier into a simple proxy layer that provides an endpoint for serialized data.

There are many different ways to build a browser-based client but for this example I will use jQuery and JavaScript since they are the most common. There are also many ways to expose the RESTful services, here I will use JAX-RS since it is the Java standard for RESTful services. For back-end data persistence this example will also use MongoDB, a NoSQL database.

The code for this example is available at:  
http://github.com/jamesward/jaxrsbars

To get a copy of this application locally, use git from the command-line or from your IDE and clone the following repository:

    git://github.com/jamesward/jaxrsbars.git


The JAX-RS & MongoDB Server
---------------------------

Let's start by setting up a server. Typically this means using Apache Tomcat, creating WAR files, etc.  In modern app development we now see Java devs gravitating towards a 'containerless' approach that makes it far easier to have dev/prod parity and works very well for quick development iterations. By taking the containerless approach, the HTTP handler is treated as just another library in your application.  Instead of deploying a partial application into a container, the application contains everything it needs and just "runs".

Here is the `pom.xml` file for a Maven build that includes all the dependencies needed to setup a containerless JAX-RS server using Jersey, Jackson, and the Grizzly HTTP handling library:

    <?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">

        <modelVersion>4.0.0</modelVersion>
        <groupId>com.jamesward</groupId>
        <artifactId>jaxrsbars</artifactId>
        <version>1.0-SNAPSHOT</version>

        <dependencies>
            <dependency>
                <groupId>org.glassfish.grizzly</groupId>
                <artifactId>grizzly-http-server</artifactId>
                <version>2.2.4</version>
            </dependency>
            <dependency>
                <groupId>org.glassfish.jersey.containers</groupId>
                <artifactId>jersey-container-grizzly2-http</artifactId>
                <version>2.0-m02</version>
            </dependency>
            <dependency>
                <groupId>org.glassfish.jersey.media</groupId>
                <artifactId>jersey-media-json</artifactId>
                <version>2.0-m02</version>
            </dependency>
            <dependency>
                <groupId>net.vz.mongodb.jackson</groupId>
                <artifactId>mongo-jackson-mapper</artifactId>
                <version>1.4.1</version>
            </dependency>
        </dependencies>

        <build>
            <plugins>
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


Notice that this project doesn't use WAR packaging.  Instead the compiled classes will go into a JAR file.  This means I need to provide a Java class with a good 'ole "static void main" method that starts the server.  Then assuming the Java classpath is set to include the necessary dependencies, the server can be started using the `java` command.  The `maven-dependency-plugin` provides a simple way to copy the project dependencies into a single directory, thus making it easy to set the classpath.

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
    import org.glassfish.jersey.media.json.JsonJacksonModule;
    import org.glassfish.jersey.server.Application;
    import org.glassfish.jersey.server.ResourceConfig;

    import javax.ws.rs.core.UriBuilder;

    public class BarServer {

        public static DB mongoDB;

        public static String contentUrl;

        private static final String CONTENT_PATH = "/content";

        public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
            final int port = System.getenv("PORT") != null ? Integer.valueOf(System.getenv("PORT")) : 8080;
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

            MongoURI mongolabUri = new MongoURI(System.getenv("MONGOLAB_URI") != null ? System.getenv("MONGOLAB_URI") : "mongodb://127.0.0.1:27017/hello");
            Mongo m = new Mongo(mongolabUri);
            mongoDB = m.getDB(mongolabUri.getDatabase());
            if ((mongolabUri.getUsername() != null) && (mongolabUri.getPassword() != null)) {
                mongoDB.authenticate(mongolabUri.getUsername(), mongolabUri.getPassword());
            }

            contentUrl = System.getenv("CONTENT_URL") != null ? System.getenv("CONTENT_URL") : CONTENT_PATH;

            Thread.currentThread().join();
        }
    }

Notice that the HTTP port, MongoDB connection URI, and the content URL have default values for local development but they can also be set via environment variables.  This provides a simple way to configure the application for different environments.  At the end of this article we will run this application on Heroku where those values will come from the environment.  There is also an environment variable named `FILE_CACHE_ENABLED` that allows the static file cache to be turned on and off.  Turning the file cache off provides a simple way to shorten the change / test loop in development while turning it ok provides better performance in production.

Since this example uses MongoDB for data persistence, if you want to do local development on this project you will need to install the MongoDB server on your system.

To start the `BarServer` locally you will first need to do a Maven build:

    mvn package

Then simply start the `BarServer` by running:

    java -cp target/classes:target/dependency/* com.jamesward.jaxrsbars.BarServer

This application will store a list of "bars" so there is a simple Java object named `com.jamesward.jaxrsbars.Bar` containing:

    package com.jamesward.jaxrsbars;

    import javax.persistence.Id;

    public class Bar {

        @Id
        public String id;

        public String name;
    }

The `@Id` annotation will be used by the `mongo-jackson-mapper` library to determine which property will store the primary key.  Each `Bar` also has a `name` property.  For this example I'm using simple direct field access instead of the more verbose getters & setters.

To configure the RESTful endpoints that will provide access to create and fetch the `Bar` objects there is a Java object named `com.jamesward.jaxrsbars.BarResource` containing the following.  (Note: I've ommitted the `index` method you will see in the source, but will cover it shortly.)

    package com.jamesward.jaxrsbars;

    import net.vz.mongodb.jackson.JacksonDBCollection;
    import net.vz.mongodb.jackson.WriteResult;

    import javax.ws.rs.*;
    import javax.ws.rs.core.MediaType;
    import java.util.List;

    @Path("/")
    public class BarResource {

        private JacksonDBCollection<Bar, String> getJacksonDBCollection() {
            return JacksonDBCollection.wrap(BarServer.mongoDB.getCollection(Bar.class.getSimpleName().toLowerCase()), Bar.class, String.class);
        }

        @Path("bar")
        @POST
        @Consumes(MediaType.APPLICATION_JSON)
        @Produces(MediaType.APPLICATION_JSON)
        public Bar addBar(Bar bar) {
            WriteResult<Bar, String> result = getJacksonDBCollection().insert(bar);
            return result.getSavedObject();
        }

        @Path("bars")
        @GET
        @Produces(MediaType.APPLICATION_JSON)
        public List<Bar> listBars() {
            return getJacksonDBCollection().find().toArray();
        }

    }

The `getJacksonDBCollection` method uses the MongoDB connection that was setup in `BarServer` to provide access to the MongoDB collection that stores the `Bar` objects.  The `addBar` method has a `Path` of `bar` meaning it will be accessible via HTTP requests to the `/bar` URL.  It also only handles `POST` requests and consumes & produces `application/json` content.  If you start the `BarServer` locally you can test this method using the `curl` command:

    curl -d '{"name":"foo"}' -H "Content-type:application/json" http://localhost:8080/bar

The JSON data that is being sent to the server is automatically deserialized into a `Bar` instance.  That object is added to the MongoDB collection and the resulting `Bar` now containing a primary key is returned.  You should see something like:

    {"id":"4f83185e986c2baad17bbf8b","name":"foo"}

The `listBars` method simply fetches all of the `Bar` objects from the MongoDB collection and returns the `List` of `Bar` objects.  To test `listBars` locally with `curl` simply run:

    curl -H "Content-type:application/json" http://localhost:8080/bars

You will see a JSON array containing the list of serialized `Bar` objects like:

    [{"id":"4f83185e986c2baad17bbf8b","name":"foo"}]

Great!  The RESTful services for creating and fetching the `Bar` object works.  Now lets take a look at the client-side of the application.


The JavaScript & jQuery Client
------------------------------

You may have noticed that `BarServer` sets up a `StaticHttpHandler` that looks for resources in the `src/main/webapp` directory and handles requests to those resources via requests to the `/content` URL.  This project contains a copy of the minified jQuery JavaScript library in the `src/main/webapp/jquery-1.7.min.js` file.  That library is available via requests to the `/content/jquery-1.7.min.js` URL.  There is also a `src/main/webapp/index.js` file which is the entire client-side / UI of this application.  It renders, creates, and fetches the `Bar` objects from the RESTful services using jQuery and renders the HTML form and list of `Bar` objects.  Here is the contents of the `index.js` file:

    function loadbars() {
        $.ajax("/bars", {
            contentType: "application/json",
            success: function(data) {
                $("#bars").children().remove();
                $.each(data, function(index, item) {
                    $("#bars").append($("<li>").text(item.name));
                });
            }
        });
    }

    function addbar() {
        $.ajax({
            url: "/bar",
            type: 'post',
            dataType: 'json',
            contentType: 'application/json',
            data: JSON.stringify({name:$("#bar").val()}),
            success: loadbars
        });
    }

    $(function() {
        $("body").append('<h4>Bars:</h4>');
        $("body").append('<ul id="bars"></ul>');

        loadbars();

        $("body").append('<input id="bar"/>');
        $("body").append('<button id="submit">GO!</button>');


        $("#submit").click(addbar);
        $("#bar").keyup(function(key) {
            if (key.which == 13) {
                addbar();
            }
        });

    });

The `loadbars` function makes an `ajax` request (using jQuery) to `/bars` and then updates the web page with the list.  The `addbar` function makes an `ajax` request to `/bar`, passing it the JSON string for a `Bar` object containing the `name` specified in an input field.  The anonymous function `function() {` gets called when the page ready.  This function adds the unordered list that will contain the "bars" to the page, calls the `loadbars` function, adds the form elements to create new bars, and adds event handlers for clicking on the "GO!" button / pressing the `Enter` key in the input field.  Both of those event handlers call the `addbar` function.

This final thing this application needs is a simple HTML page that will bootstrap the application in the browser by loading the jQuery library and the `index.js` JavaScript.  This could potentially also be a static file.  However, shortly we will load the client-side of the app (`index.js` and `jquery-1.7.min.js`) from a Content Delivery Network (CDN) which will require our bootstrapping HTML file to change based on the environment it's running in (thus the reason for the `contentUrl` property in the `BarServer` class).  This file could be served using a server-side templating library but for this example we will keep things simple and just add a `GET` request handler in `BarResource` that handles requests to the `/` URL and produces `text/html` content:

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String index() {
        return "<!DOCTYPE html>\n" +
                "<html>\n" +
                "<head>\n" +
                "<script type='text/javascript' src='" + BarServer.contentUrl + "/jquery-1.7.min.js'></script>\n" +
                "<script type='text/javascript' src='" + BarServer.contentUrl + "/index.js'></script>\n" +
                "</head>\n" +
                "</html>";
    }

This very simple HTML page simple loads jQuery and the `index.js` JavaScript file using `BarServer.contentUrl` as the basis.  Since `BarServer.contentUrl` can be changed through the `CONTENT_URL` environment variable it is now very easy to fetch those files from an alternative location.  However they are always available from this server via the `/content` URL.

If you are running the application locally you should be able to add new "bars" and see the list by opening [http://localhost:8080](http://localhost:8080) in your browser.  If you inspect the requests made to render the page (with a tool like Firebug) you should see four requests:  
![](https://github.com/jamesward/jaxrsbars/raw/master/img/requests.png)

Adding a new "Bar" from the form should do a `POST` request and then refresh the list of "bars" via a `GET` request.  Now that we have a fully functional Client/Server web app lets deploy it on the cloud and then load the client-side from a CDN.


Deploying on the Cloud with Heroku
----------------------------------

To deploy the application on the cloud you can use Heroku, a Java-capable, Polyglot Platform-as-a-Service (PaaS) provider.  For this example we will upload the code via git to Heroku.  This method supports "Continuous Delivery" by making incremental changes very easy to deploy.  Every time new code is received by Heroku (through a git push), the Maven build will be run and the new version deployed. Follow these steps to deploy your copy of this app on Heroku:

1) [Signup for a Heroku account](https://heroku.com/signup).  You will be able to deploy and run this application for free on one [dyno](https://devcenter.heroku.com/articles/dynos).

2) [Verify your Heroku account](https://heroku.com/verify). (Required to use the free tier of the MongoLab add-on.)

3) [Install the Heroku Toolbelt](http://toolbelt.heroku.com).

4) Login to Heroku from the command line:

        heroku login

This will also help you setup your SSH key and associate it with your Heroku account.  The SSH key will be used for authenticating git pushes (uploads).

5) Provision a new app on Heroku (using the [cedar stack](https://devcenter.heroku.com/articles/cedar)) with the [MongoLab add-on](https://addons.heroku.com/mongolab) (run this command in the root of your project):

        heroku create --stack cedar --addons mongolab

The git remote endpoint for the newly created app will be named "heroku" and will be automatically added to your git configuration.

6) Using git, upload your application to Heroku:

        git push heroku master

You can also do this from within your IDE.  This will copy the source files, Maven build descriptor, and the `Procfile` to Heroku.  Heroku will then run the Maven build on the project, deploy the app onto a dyno, and launch the web process defined in the `Procfile`.  The `Procfile` is just a simple way to map process names to commands.  Here is the `Procfile` for this project:

        web:    java -cp target/classes:target/dependency/* com.jamesward.jaxrsbars.BarServer

Notice that the launch command on Heroku is the same as the one that you used to start `BarServer` locally.

Once the git push is complete you should be able to open your app in the browser using either the URL from the `git push` output or by running:

        heroku open

7) By default caching and 304 handling was turned off for local development.  Turn that on by setting the `FILE_CACHE_ENABLED` environment variable on Heroku:

        heroku config:add FILE_CACHE_ENABLED=true

You can see a full list of environment variables for your application (including `MONGOLAB_URI` that was set by the MongoLab add-on) by running:

        heroku config

Congrats!  Your application is now running on the cloud!

Serving the Client-side from the AWS CloudFront CDN
---------------------------------------------------

At this point everything should be working wonderfully on the cloud.  But we can take things one step further to increase performance of the app.  CDNs will edge catch static assets around the world, meaning that a copy of the content is placed very near the consumer of the content.  This process really only works for static assets but since the client-side of this application is now static assets (`index.js` and `jquery-1.7.min.js`) those files can be loaded from a CDN instead of from the more centralized server on Heroku.  The HTML page that bootstraps the app will still be loaded directly from Heroku because it's not fully static and because we want want to avoid cross-origin browser restrictions.  To avoid the cross-origin browser restictions the page the user loads in their browser must be on the same domain as the RESTful services.

For this example we will use the Amazon CloudFront CDN service.  Amazon uses a purely usage based pricing model so this part won't be free, but for this small example app it shouldn't cost more than a dollar to try it out.  CloudFront provides a way to retreive the static assets that it will cache, from an origin server.  This makes it very easy to configure and switch to using CloudFront for static assets.  If CloudFront does not have the correct version of the static asset it will go back to the origin server to get it.

Follow these steps to setup CloudFront:

1) Create an account on AWS:

    https://aws-portal.amazon.com/gp/aws/developer/registration/index.html

3) Open: https://console.aws.amazon.com/cloudfront/home

4) Click "Create Distribution"

5) Select "Custom Origin"

6) Enter the domain name of your application on Heroku

7) Select "Continue"

8) Select "Continue" again

9) Select "Create Distribution"

10) Wait a few minutes while the CloudFront Distribution is provisioned

11) Using the newly created CloudFront distribution, configure your Heroku app to use CloudFront for static content:

        heroku config:add CONTENT_URL=http://yourdomainname.cloudfront.net/content

12) Reload your app in your browser


You now have a Client/Server web app in the cloud with the client-side edge cached on a CDN!  You can now expand on this example to build out much more complex applications that have a distinct Client/Server separation.  As you explore this new way of building applications you will begin to discover a vast and emerging variety of tools and libraries to help you build the client-side.  I recommend that you check out [Backbone.js](http://backbonejs.org/) for client-side MVC and [Underscore.js](http://documentcloud.github.com/underscore/) for client-side templating.  For beautifying the UI check out [Twitter Bootstrap](http://twitter.github.com/bootstrap).  There are many different choices that you should evaluate but those are good libraries to start with.

If you'd like to see a live demo of this project check out:
[http://jaxrsbars.herokuapp.com](http://jaxrsbars.herokuapp.com)
