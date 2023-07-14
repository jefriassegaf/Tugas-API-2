package com.je.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import io.github.cdimascio.dotenv.Dotenv;


import java.util.*;

public class Request {
    private final String[] requestMethodsAllowed = {
            "POST",
            "GET",
            "PUT",
            "DELETE"
    };

    public static class Handler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) {
            try {
                Dotenv dotenv = Dotenv.configure().directory(Main.getRootPath()).filename(".env").load();
                String envApiKey = dotenv.get("API_KEY");
                String reqApiKey = null;

                Response response = new Response(exchange);

                exchange.getResponseHeaders().put("Content-Type", Collections.singletonList("text/json"));
                int statusCode;

                try {
                    reqApiKey = exchange.getRequestHeaders().get("api-key").get(0);
                } catch (Exception e) {
                    e.printStackTrace();
                }


                if (!envApiKey.equals(reqApiKey)) {
                    response.send(statusCode = 401, "{" +
                            "\"status\": " + statusCode + "," +
                            "\"message\": \"API key not authorized\"" +
                            "}");
                    return;
                }

                String requestMethod = exchange.getRequestMethod();
                String requestQuery = exchange.getRequestURI().getQuery();
                String[] requestPath = Parser.splitString(exchange.getRequestURI().getPath(), "/");
                String requestBody = Parser.parseInputStream(exchange.getRequestBody());

                String tableMaster = null;
                String id = null;
                String tableDetail = null;

                try {
                    tableMaster = requestPath[0];
                    if (requestPath.length > 1) id = requestPath[1];
                    if (requestPath.length > 2) tableDetail = requestPath[2];
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (!Validate.isRequestMethodAllowed(requestMethod)) {
                    response.send(statusCode = 405, "{" +
                            "\"status\": " + statusCode + "," +
                            "\"message\": \"Request method " + requestMethod + " not allowed\"" +
                            "}");
                    return;
                }

                if (!Validate.isTableNameValid(tableMaster)) {
                    response.send(statusCode = 404, "{" +
                            "\"status\": " + statusCode + "," +
                            "\"message\": \"Table " + tableMaster + " was not found\"" +
                            "}");
                    return;
                }

                if (id != null && !Validate.isIdValid(id)) {
                    response.send(statusCode = 404, "{" +
                            "\"status\": " + statusCode + "," +
                            "\"message\": \"ID " + id + " was not found\"" +
                            "}");
                    return;
                }

                if (tableDetail != null && !Validate.isTableNameValid(tableDetail)) {
                    response.send(statusCode = 404, "{" +
                            "\"status\": " + statusCode + "," +
                            "\"message\": \"Table " + tableDetail + " was not found\"" +
                            "}");
                    return;
                }

                String condition = null;
                if (Validate.isRequestMethodAllowed(requestMethod) && !"POST".equals(requestMethod) && requestQuery != null) {
                    condition = Parser.parseRequestQuery(requestQuery);
                    if (condition == null) {
                        response.send(statusCode = 400, "{" +
                                "\"status\": " + statusCode + "," +
                                "\"message\": \"Query " + requestQuery + " is not valid\"" +
                                "}");
                        return;
                    }
                }

                if (!Validate.isRequestBodyValid(requestBody) && (requestMethod.equals("POST") || requestMethod.equals("PUT"))) {
                    response.send(statusCode = 400, "{" +
                            "\"status\": " + statusCode + "," +
                            "\"message\": \"Please input request body as JSON to inserting or updating data\"" +
                            "}");
                    return;
                }

                JsonNode jsonNode = null;

                if (requestMethod.equals("POST") || requestMethod.equals("PUT")) 
                    jsonNode = Parser.parseJson(requestBody);

                switch (requestMethod) {
                    case "GET":
                        // Do this when the user only routes to the table name without adding a query in the URL
                        if (requestPath.length == 1) {
                            response.handleGet(tableMaster, condition);
                        } else if (requestPath.length == 2 && requestQuery == null) {
                            assert id != null;
                            response.handleGet(tableMaster, Integer.parseInt(id), null);
                        } else if (requestPath.length == 3 && requestQuery == null) {
                            assert id != null;
                            response.handleGet(tableMaster, Integer.parseInt(id), tableDetail);
                        } else {
                            response.send(statusCode = 400, "{" +
                                    "\"status\": " + statusCode + "," +
                                    "\"message\": \"Please check your request\"" +
                                    "}");
                        }
                        return;
                    case "POST":
                        response.handlePost(tableMaster, jsonNode);
                        return;
                    case "PUT":
                        if (requestPath.length == 2) {
                            assert id != null;
                            response.handlePut(tableMaster, Integer.parseInt(id), jsonNode);
                        } else {
                            response.send(statusCode = 400, "{" +
                                    "\"status\": " + statusCode + "," +
                                    "\"message\": \"No ID detected, please check your request\"" +
                                    "}");
                        }
                        return;
                    case "DELETE":
                        if (requestPath.length == 2) {
                            assert id != null;
                            response.handleDelete(tableMaster, Integer.parseInt(id));
                        } else {
                            response.send(statusCode = 400, "{" +
                                    "\"status\": " + statusCode + "," +
                                    "\"message\": \"No ID detected, please check your request\"" +
                                    "}");
                        }
                        return;
                    default:
                        response.send(statusCode = 400, "{" +
                                "\"status\": " + statusCode + "," +
                                "\"message\": \"Please check your request\"" +
                                "}");
                }
            } catch (Exception e) {
                System.err.println(e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public String[] getRequestMethodsAllowed() {
        return requestMethodsAllowed;
    }
}
