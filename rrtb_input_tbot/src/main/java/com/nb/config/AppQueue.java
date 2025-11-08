package com.nb.config;


import io.micronaut.context.annotation.ConfigurationProperties;

//@ConfigurationProperties("app.queue")
public class AppQueue {

    private String name;
    private String dlq;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDlq() {
        return dlq;
    }

    public void setDlq(String dlq) {
        this.dlq = dlq;
    }
}