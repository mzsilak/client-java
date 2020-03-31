package eu.arrowhead.demo.utils;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import org.junit.Test;

public class ProcessTemplateTest {

    private final List<String> result = new ArrayList<>();

    @Test
    public void testProcess() throws IOException, InterruptedException {
        final ProcessTemplate template = new ProcessTemplate("cmd /c echo RFID TEST".split("\\s"));
        template.executor(Executors.newCachedThreadPool());
        template.setInputStreamConsumer(this::consumer);
        final Process process = template.execute();
        process.waitFor();

        assertEquals(1, result.size());
        assertEquals("RFID TEST", result.get(0));
    }

    private void consumer(String s) {
        result.add(s);
    }
}