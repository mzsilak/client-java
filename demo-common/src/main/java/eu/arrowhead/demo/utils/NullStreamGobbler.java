package eu.arrowhead.demo.utils;

import java.io.InputStream;

public class NullStreamGobbler extends StreamGobbler {


    public NullStreamGobbler(final InputStream inputStream) {
        super(inputStream, (s) -> {
            // empty
        });
    }
}