package com.example.httpapigenerator;

public class HttpApiData {

    private String path;
    private String method;
    private String desc;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    @Override
    public String toString() {
        return "HttpApiData{" +
                "path='" + path + '\'' +
                ", method='" + method + '\'' +
                ", desc='" + desc + '\'' +
                '}';
    }
}
