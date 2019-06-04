package com.example.myapplication;

public class ResultObject {
    private String success;
    public ResultObject(String success) {
        this.success = success;
    }
    public String getSuccess() {
        return success;
    }

    @Override
    public String toString() {
        return "ResultObject{" +
                "success='" + success + '\'' +
                '}';
    }
}