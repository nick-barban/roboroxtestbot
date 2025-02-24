package com.nb.config;

import io.micronaut.context.annotation.ConfigurationBuilder;
import io.micronaut.context.annotation.ConfigurationProperties;

@ConfigurationProperties("app")
public class AppConfig {

    @ConfigurationBuilder(configurationPrefix = "queue.input")
    private AppQueue input = new AppQueue();
    @ConfigurationBuilder(configurationPrefix = "queue.output")
    private AppQueue output = new AppQueue();

    public AppQueue getInput() {
        return input;
    }

    public void setInput(AppQueue input) {
        this.input = input;
    }

    public AppQueue getOutput() {
        return output;
    }

    public void setOutput(AppQueue output) {
        this.output = output;
    }

    //    @ConfigurationProperties("app.queue")
    public static class AppQueue {

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
}