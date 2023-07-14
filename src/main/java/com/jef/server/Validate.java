package com.bay.server;

import com.bay.data.Database;

import java.util.Arrays;

public class Validate {
    public static boolean isRequestMethodAllowed(String requestMethod) {
        return Arrays.asList(new Request().getRequestMethodsAllowed()).contains(requestMethod);
    }

    public static boolean isTableNameValid(String tableName) {
        Database database = new Database();
        return Arrays.asList(database.getTables()).contains(tableName);
    }

    public static boolean isIdValid(String id) {
        try {
            Integer.parseInt(id);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public static boolean isRequestBodyValid(String requestBody) {
        try {
            Parser.parseJson(requestBody);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            return false;
        }
        return true;
    }
}
