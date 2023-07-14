package com.bay.server;

import java.io.IOException;

public class Main {
    private static final String nim = "2205551070";
    private static final int port = Integer.parseInt("8" + nim.substring(nim.length() - 3));
    private static final String rootPath = System.getProperty("user.dir");

    public static void main(String[] args) throws IOException {
        new Server(port);
    }

    public static String getRootPath() {
        return rootPath;
    }

}