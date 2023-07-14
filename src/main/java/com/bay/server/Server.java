package com.bay.server;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class Server {
    private final int port;
    public Server(int port) throws IOException {
        this.port = port;

        HttpServer httpServer = HttpServer.create(new InetSocketAddress(this.port), 0);
        httpServer.setExecutor(Executors.newSingleThreadExecutor());
        httpServer.createContext("/", new Request.Handler());
        httpServer.start();
        System.out.println(this);
    }

    @Override
    public String toString() {
        return "Server running on port " + this.port + "...";
    }
}
