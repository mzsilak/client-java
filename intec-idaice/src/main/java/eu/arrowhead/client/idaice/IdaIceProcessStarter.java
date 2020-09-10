package eu.arrowhead.client.idaice;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class IdaIceProcessStarter {

    private final Logger logger = LogManager.getLogger();

    private final ExecutorService executorService;
    private final PlcLookupService lookupService;
    private final IdaIceConfigParser configParser;
    private final String homeDirectory;

    public IdaIceProcessStarter(final ExecutorService executorService, final PlcLookupService lookupService,
                                final IdaIceConfigParser configParser,
                                @Value("${idaice.directory}") final String homeDirectory) {
        super();
        this.executorService = executorService;
        this.lookupService = lookupService;
        this.configParser = configParser;
        this.homeDirectory = homeDirectory;
    }

    public void startBuildingTracker() throws IOException {
        logger.info("Performing PLC address lookup");
        final String plcAddress = lookupService.getPlcAddress();

        logger.info("Writing script to temporary file");
        final String file = configParser.writeScriptWithAddress(plcAddress);

        logger.info("Preparing application start");
        executorService.submit(new ProcessWrapper(executorService, homeDirectory, file));
    }

    private static class ProcessWrapper implements Runnable {

        private final Logger btLogger = LogManager.getLogger("BUILDING-TRACKER");
        private ExecutorService executorService;
        private String homeDirectory;
        private String file;

        private ProcessWrapper(ExecutorService executorService, String homeDirectory, String file) {
            this.executorService = executorService;
            this.homeDirectory = homeDirectory;
            this.file = file;
        }

        @Override
        public void run() {
            final ProcessBuilder builder;
            final Process process;
            final String[] args = new String[]{"cmd", "/c", "ice.exe", "-X", file};
            btLogger.info("Starting Building Tracker: {}", (Object) args);

            try {
                builder = new ProcessBuilder(args);
                btLogger.debug("Setting home directory to {}", homeDirectory);
                builder.directory(new File(homeDirectory));
                process = builder.start();

                final StreamGobbler inGobbler = new StreamGobbler(process.getInputStream(), btLogger::debug);
                final StreamGobbler errGobbler = new StreamGobbler(process.getErrorStream(), btLogger::error);
                btLogger.debug("Redirecting streams ...");

                executorService.submit(inGobbler);
                executorService.submit(errGobbler);

                btLogger.debug("Waiting for exit and sleeping 3 more seconds and ...");
                final int code = process.waitFor();
                Thread.sleep(3_000L);

                btLogger.info("Application finished with exit code {}", code);
            } catch (IOException e) {
                btLogger.error("Unable to start ice.exe: {}", e.getMessage());
            } catch (InterruptedException e) {
                btLogger.error("Process interrupted", e);
            }
        }
    }
}
