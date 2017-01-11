package com.jamesward.jaxrsbars;

import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;


@Entity("bars")
public class Bar {

    @Id
    public String id;

    public String name;

}
