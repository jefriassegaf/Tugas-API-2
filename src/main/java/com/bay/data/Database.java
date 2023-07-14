package com.bay.data;

import com.bay.server.Main;
import com.bay.server.Parser;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.*;
import java.util.*;

public class Database {
    private final String[] tables = {
            "addresses",
            "orderDetails",
            "orders",
            "products",
            "reviews",
            "users"
    };
    public Connection connect() {
        Connection connection = null;
        try {
            String path = "jdbc:sqlite:" + Main.getRootPath() + "/ecommerce.db";
            connection = DriverManager.getConnection(path);
        } catch (SQLException e) {

            System.err.println(e.getMessage());
        }

        return connection;
    }

    public Result select(String tableName, String condition) {
        List<String> rows = new ArrayList<>();
        try {
            String query = "SELECT * FROM " + tableName +
                    (condition != null ? " WHERE " + condition : "");
            Connection connection = this.connect();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(query);

            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            int columnCount = resultSetMetaData.getColumnCount();

            while (resultSet.next()) {
                // Format to json
                StringBuilder row = new StringBuilder();
                row.append("{");
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = resultSetMetaData.getColumnName(i);
                    Object columnValue = resultSet.getObject(i);
                    row.append("\"").append(columnName).append("\":\"").append(columnValue).append("\",");
                }
                row.deleteCharAt(row.length() - 1);
                row.append("}");
                // End of formatting

                ObjectMapper objectMapper = new ObjectMapper();
                Object object = objectMapper.readValue(row.toString(), Class.forName("com.bay.data." + getClassName(tableName)));
                rows.add(toJson(object));
            }

            if (rows.size() == 0) return new Result(null, "No matching data found, please check your request", 404, false);
            else if (rows.size() == 1) return new Result(rows.get(0), "Select success", 200, true);
            return new Result(rows, "Select success", 200, true);
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(null, e.getMessage(), 400, false);
        }
    }

    public Result customSelect(String customQuery) {
        List<String> rows = new ArrayList<>();
        try {
            Connection connection = this.connect();
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(customQuery);

            ResultSetMetaData resultSetMetaData = resultSet.getMetaData();
            int columnCount = resultSetMetaData.getColumnCount();

            while (resultSet.next()) {
                // Format to json
                StringBuilder row = new StringBuilder();
                row.append("{");
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = resultSetMetaData.getColumnName(i);
                    Object columnValue = resultSet.getObject(i);
                    if (columnValue instanceof Double || columnValue instanceof Integer) row.append("\"").append(columnName).append("\":").append(columnValue).append(",");
                    else row.append("\"").append(columnName).append("\":\"").append(columnValue).append("\",");
                }
                row.deleteCharAt(row.length() - 1);
                row.append("}");
                // End of formatting

                rows.add(row.toString());
            }

            if (rows.size() == 0) return new Result(null, "No matching data found, please check your request", 404, false);
            else if (rows.size() == 1) return new Result(rows.get(0), "Select success", 200, true);
            return new Result(rows, "Select success", 200, true);
        } catch (Exception e) {
            e.printStackTrace();
            return new Result(null, e.getMessage(), 400, false);
        }
    }

    public Result insert(String tableName, String fieldKeys, String fieldValues) {
        Result result;
        try {
            String query = "INSERT INTO " + tableName + " (" + fieldKeys + ") " + "VALUES (" + fieldValues + ") ";
            Connection connection = this.connect();
            Statement statement = connection.createStatement();
            result = new Result(statement.executeUpdate(query), "Insert success", 200,true);
        } catch (Exception e) {
            e.printStackTrace();
            result = new Result(null, e.getMessage(), 404,false);
        }

        return result;
    }

    public Result update(String tableName, int id, String fieldKeys, String fieldValues) {
        if (!this.select(tableName, "id=" + id).isSuccess()) {
            return new Result(null, "No matching data found, please check your request", 404, false);
        }

        Result result;
        try {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("UPDATE ").append(tableName).append(" SET ");

            String[] fieldKeysParsed = Parser.splitString(fieldKeys, ",");
            String[] fieldValuesParsed = Parser.splitString(fieldValues, ",");

            for (int i = 0; i < fieldKeysParsed.length; i++) {
                stringBuilder.append(fieldKeysParsed[i]).append("=").append(fieldValuesParsed[i]).append(",");
            }

            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
            stringBuilder.append(" WHERE id=").append(id);

            String query = stringBuilder.toString();

            Connection connection = this.connect();
            Statement statement = connection.createStatement();
            result = new Result(statement.executeUpdate(query), "Update success", 200,true);
        } catch (Exception e) {
            e.printStackTrace();
            result = new Result(null, e.getMessage(), 400,false);
        }

        return result;
    }

    public Result delete(String tableName, int id) {
        if (!this.select(tableName, "id=" + id).isSuccess()) return new Result(null, "No matching data found, please check your request", 404, false);

        Result result;
        try {
            String query = "DELETE FROM " + tableName + " WHERE id=" + id;
            Connection connection = this.connect();
            Statement statement = connection.createStatement();
            result = new Result(statement.executeUpdate(query), "Delete success", 200,true);
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            result = new Result(null, e.getMessage(), 400,false);
            return result;
        }
    }

    public String getClassName(String tableName) {
        StringBuilder stringBuilder = new StringBuilder(tableName);
        stringBuilder.deleteCharAt(0);
        stringBuilder.insert(0, tableName.toUpperCase().charAt(0));

        stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        if (stringBuilder.charAt(stringBuilder.length() - 1) == 'e') {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }
        return stringBuilder.toString();
    }

    public String toJson(Object object) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String joinJson(String firstJson, String tableName, String secondJson) {
        StringBuilder stringBuilder = new StringBuilder(firstJson);

        // Array
        if (stringBuilder.charAt(0) == '[' && stringBuilder.charAt(stringBuilder.length() - 1) == ']') {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1); // ]
            stringBuilder.deleteCharAt(stringBuilder.length() - 1); // }
            stringBuilder.append(",");
            stringBuilder.append("\"").append(tableName).append("\"").append(":");
            stringBuilder.append(secondJson);
            stringBuilder.append("}]");
        }

        // Object
        else if (stringBuilder.charAt(0) == '{' && stringBuilder.charAt(stringBuilder.length() - 1) == '}') {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1); // }
            stringBuilder.append(",");
            stringBuilder.append("\"").append(tableName).append("\"").append(":");
            stringBuilder.append(secondJson);
            stringBuilder.append("}");
        }

        return stringBuilder.toString();
    }

    public String[] getTables() {
        return tables;
    }
}
