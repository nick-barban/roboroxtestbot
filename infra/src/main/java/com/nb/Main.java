package com.nb;

import software.amazon.awscdk.App;

public class Main {
    public static void main(final String[] args) {
        App app = new App();
        // TODO by nickbarban: 18/02/25 Should be renamed
        new AppStack(app, "MicronautAppStack");
        app.synth();
    }
}