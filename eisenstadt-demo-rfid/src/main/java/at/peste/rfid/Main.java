package at.peste.rfid;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {

    private static final Logger logger = LogManager.getLogger();

    public Main() {
        logger.info("Main created");
    }

    /*
    public static void main(String[] args) throws InterruptedException, IOException {
        final ProcessTemplate template;
        if (args.length == 1) {
            template = new ProcessTemplate(args[0].split("\\s"));
        } else if (args.length > 1) {
            template = new ProcessTemplate(args);
        } else {
            System.err.println("At least one argument expected");
            System.exit(1);
            return;
        }
        template.executor(Executors.newCachedThreadPool());
        template.setInputStreamConsumer(Main::log);

        logger.info("Ctx creation finished");

        final Process process = template.execute();

        Thread.sleep(15 * 1000L);

        process.waitFor();
    }
     */


    public static void main(String[] args) throws InterruptedException, IOException {
        final Process exec;
        if (args.length == 1) {
            exec = Runtime.getRuntime().exec(args[0].split("\\s"));
        } else if (args.length > 1) {
            exec = Runtime.getRuntime().exec(args);
        } else {
            System.err.println("At least one argument expected");
            System.exit(1);
            return;
        }

        logger.info("setting up reader");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(exec.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log(line);
            }
        }
        logger.info("reader loop finished");
        Thread.sleep(5 * 1000L);
        exec.waitFor();
    }

    public static void log(final String msg) {
        System.out.println("OUTPUT: " + msg);
    }
}

