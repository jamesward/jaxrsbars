package com.jamesward.jaxrsbars;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/")
public class Index {

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String getClichedMessage() {
        return "Hello World";
    }

}