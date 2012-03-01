package com.jamesward.jaxrsbars;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;

import java.io.IOException;

public class ContentServer {

    public static void main(String[] args) throws IOException {
        final HttpServer httpServer = HttpServer.createSimpleServer("src/main/webapp", 9090);
        httpServer.start();

        for (NetworkListener networkListener : httpServer.getListeners()) {
            networkListener.getFileCache().setEnabled(false);
        }

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
