package com.bay.server;

import com.bay.data.Database;
import com.bay.data.Result;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

public class Response {
    private final HttpExchange exchange;
    private final Database database = new Database();

    public Response(HttpExchange exchange) {
        this.exchange = exchange;
    }

    public void handleGet(String tableName, String condition) throws IOException {
        Result result = database.select(tableName, condition);
        int statusCode = result.getStatusCode();

        if (result.isSuccess()) {
            this.send(statusCode, "{" +
                    "\"status\": " + statusCode + "," +
                    "\"message\": " + result.getMessage() + "," +
                    "\"data\": " + result.getData() +
                    "}"
            );
        } else {
            this.send(statusCode, "{" +
                    "\"status\": " + statusCode + "," +
                    "\"message\": " + result.getMessage() +
                    "}"
            );
        }
    }

    public void handleGet(String tableMaster, int id, String tableDetail) throws IOException, SQLException {
        Result resultParent = database.select(tableMaster, "id=" + id);
        String jsonResult = resultParent.getData();

        int statusCode = resultParent.getStatusCode();
        boolean isSuccess = resultParent.isSuccess();
        String message = resultParent.getMessage();

        if (!isSuccess) {
            this.send(statusCode, "{" +
                    "\"status\": " + statusCode + "," +
                    "\"message\": " + message +
                    "}");
            return;
        }

        if (tableMaster.equals("users")) {
            if (tableDetail == null) {
                Result addresses = database.select("addresses", "user=" + id);
                isSuccess = addresses.isSuccess();
                if (!isSuccess) {
                    statusCode = addresses.getStatusCode();
                    message = addresses.getMessage();
                } else {
                    jsonResult = database.joinJson(jsonResult,
                            "addresses", addresses.getData());
                }
            } else if (tableDetail.equals("products")) {
                Result products = database.select("products", "seller=" + id);
                isSuccess = products.isSuccess();
                if (!isSuccess) {
                    statusCode = products.getStatusCode();
                    message = products.getMessage();
                } else {
                    jsonResult = database.joinJson(jsonResult,
                            "products", products.getData());
                }
            } else if (tableDetail.equals("orders")) {
                Result orders = database.select("orders", "buyer=" + id);
                isSuccess = orders.isSuccess();
                if (!isSuccess) {
                    statusCode = orders.getStatusCode();
                    message = orders.getMessage();
                } else {
                    jsonResult = database.joinJson(jsonResult,
                            "orders", orders.getData());
                }
            } else if (tableDetail.equals("reviews")) {
                String query = "SELECT id FROM orders WHERE buyer=" + id;
                Connection connection = database.connect();
                Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(query);

                ArrayList<Integer> idOrders = new ArrayList<>();
                while (resultSet.next()) {
                    idOrders.add(resultSet.getInt("id"));
                }

                Result reviews = new Result();
                if (idOrders.size() == 1) {
                    reviews = database.select("reviews", "`order`=" + idOrders.get(0));
                } else {
                    ArrayList<Object> tempReviews = new ArrayList<>();
                    for (Integer idOrder : idOrders) {
                        Result review = database.select("reviews", "`order`=" + idOrder);
                        if (review.getData() != null) {
                            tempReviews.add(review.getData());
                        }
                    }

                    if (tempReviews.contains(null)) {
                        System.out.println("seriosuly");
                        reviews.setData(null);
                        reviews.setStatusCode(400);
                        reviews.setMessage("No matching data found");
                        reviews.setSuccess(false);
                    } else {
                        reviews.setData(tempReviews);
                        reviews.setStatusCode(200);
                        reviews.setMessage("Select success");
                        reviews.setSuccess(true);
                    }
                }

                System.out.println(reviews.isSuccess());
                isSuccess = reviews.isSuccess();
                if (!isSuccess) {
                    statusCode = reviews.getStatusCode();
                    message = reviews.getMessage();
                } else  {
                    jsonResult = database.joinJson(jsonResult,
                            "reviews", reviews.getData());
                }
            }
        } else if (tableMaster.equals("orders") && tableDetail == null) {
            // Select ID Buyer
            String query = "SELECT buyer FROM orders WHERE id=" + id;
            Connection connection = database.connect();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);
            int idBuyer = resultSet.getInt("buyer");

            // Select a user who has a Buyer ID above
            Result buyer = database.select("users", "id=" + idBuyer);

            isSuccess = buyer.isSuccess();
            if (!isSuccess) {
                statusCode = buyer.getStatusCode();
                message = buyer.getMessage();
            } else {
                jsonResult = database.joinJson(jsonResult,
                        "buyerDetail", buyer.getData());
            }

            // Select all order detail columns in the orderDetails
            // and the product title in the products when the order id matches
            Result orderDetails = database.customSelect("SELECT products.title, orderDetails.* FROM orderDetails " +
                    "JOIN products ON orderDetails.product = products.id " +
                    "WHERE orderDetails.`order`=" + id);

            isSuccess = orderDetails.isSuccess();
            if (!isSuccess) {
                statusCode = orderDetails.getStatusCode();
                message = orderDetails.getMessage();
            } else {
                jsonResult = database.joinJson(jsonResult,
                        "orderDetails", orderDetails.getData());
            }

            // Select buyer reviews
            Result review = database.select("reviews", "`order`=" + id);
            isSuccess = orderDetails.isSuccess();
            if (!isSuccess) {
                statusCode = orderDetails.getStatusCode();
                message = orderDetails.getMessage();
            } else {
                jsonResult = database.joinJson(jsonResult,
                        "review", review.getData());
            }

        } else if (tableMaster.equals("products") && tableDetail == null) {
            String query = "SELECT seller FROM products WHERE id=" + id;
            Connection connection = database.connect();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);

            int idSeller = resultSet.getInt("seller");

            Result users = database.select("users", "`id`=" + idSeller);
            isSuccess = users.isSuccess();
            if (!isSuccess) {
                statusCode = users.getStatusCode();
                message = users.getMessage();
            } else {
                jsonResult = database.joinJson(jsonResult,
                        "sellerDetail", users.getData());
            }
        } else {
            this.send(statusCode = 400, "{" +
                    "\"status\": " + statusCode + "," +
                    "\"message\": " + "\"No matching data found, please check your request\"" +
                    "}"
            );
            return;
        }

        if (!isSuccess) {
            this.send(statusCode, "{" +
                    "\"status\": " + statusCode + "," +
                    "\"message\": " + message +
                    "}");
        } else {
            this.send(statusCode, "{" +
                    "\"status\": " + statusCode + "," +
                    "\"message\": " + message + "," +
                    "\"data\": " + jsonResult +
                    "}"
            );
        }
    }

    public void handlePost(String tableName, JsonNode jsonNode) throws IOException {
        StringBuilder fieldKeys = new StringBuilder();
        StringBuilder fieldValues = new StringBuilder();

        Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            fieldKeys.append(field.getKey());
            fieldKeys.append(",");

            fieldValues.append(field.getValue());
            fieldValues.append(",");
        }

        // Remove the comma (,) character at the end of the string
        fieldKeys.deleteCharAt(fieldKeys.length() - 1);
        fieldValues.deleteCharAt(fieldValues.length() - 1);

        Result result = database.insert(tableName, fieldKeys.toString(), fieldValues.toString());
        int statusCode = result.getStatusCode();

        if (result.isSuccess()) {
            this.send(statusCode, "{" +
                    "\"status\": " + statusCode + "," +
                    "\"message\": " + result.getMessage() + "," +
                    "\"data\": " + result.getData() +
                    "}");
        } else {
            this.send(statusCode, "{" +
                    "\"status\": " + statusCode + "," +
                    "\"message\": " + result.getMessage()  +
                    "}");
        }
    }

    public void handlePut(String tableName, int id, JsonNode jsonNode) throws IOException {
        StringBuilder fieldKeys = new StringBuilder();
        StringBuilder fieldValues = new StringBuilder();

        Iterator<Map.Entry<String, JsonNode>> fields = jsonNode.fields();

        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            fieldKeys.append(field.getKey());
            fieldKeys.append(",");

            fieldValues.append(field.getValue());
            fieldValues.append(",");
        }

        // Remove the comma (,) character at the end of the string
        fieldKeys.deleteCharAt(fieldKeys.length() - 1);
        fieldValues.deleteCharAt(fieldValues.length() - 1);

        Result result = database.update(tableName, id, fieldKeys.toString(), fieldValues.toString());
        int statusCode = result.getStatusCode();

        if (result.isSuccess()) {
            this.send(statusCode, "{" +
                    "\"status\": " + statusCode + "," +
                    "\"message\": " + result.getMessage() + "," +
                    "\"data\": " + result.getData() +
                    "}");
        } else {
            this.send(statusCode, "{" +
                    "\"status\": " + statusCode + "," +
                    "\"message\": " + result.getMessage()  +
                    "}");
        }
    }

    public void handleDelete(String tableName, int id) throws IOException {
        Result result = database.delete(tableName, id);
        int statusCode = result.getStatusCode();

        if (result.isSuccess()) {
            this.send(statusCode, "{" +
                    "\"status\": " + statusCode + "," +
                    "\"message\": " + result.getMessage() + "," +
                    "\"data\": " + result.getData() +
                    "}");
        } else {
            this.send(statusCode, "{" +
                    "\"status\": " + statusCode + "," +
                    "\"message\": " + result.getMessage()  +
                    "}");
        }
    }

    public void send(int statusCode, String jsonMessage) throws IOException {
        OutputStream outputStream = exchange.getResponseBody();
        exchange.sendResponseHeaders(statusCode, jsonMessage.length());
        outputStream.write(jsonMessage.getBytes());
        outputStream.flush();
        outputStream.close();
    }
}
