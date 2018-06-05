package com.nosoft.district;

import io.vertx.core.Vertx;

public class App {

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(MainVerticle.class.getName());
    }
}
