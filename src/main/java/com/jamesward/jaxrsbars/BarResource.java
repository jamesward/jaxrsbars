package com.jamesward.jaxrsbars;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Path("/")
public class BarResource {

    @Path("bar")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Bar addBar(Bar bar) {
        BarServer.datastore.save(bar);
        return bar;
    }

    @Path("bars")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Bar> listBars() {
        return BarServer.datastore.createQuery(Bar.class).asList();
    }

}
