package eu.arrowhead.client.idaice;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

@Component
public class IdaIceConfigParser {

    private final Logger logger = LogManager.getLogger();

    private static final String $IDM_FILE$ = "$IDM_FILE$";
    private static final String $OPC_UA_URL$ = "$OPC_UA_URL$";

    private final String template;
    private final String idmFile;


    public IdaIceConfigParser(@Value("${idaice.template}") final Resource templateFile,
                              @Value("${idaice.idm-file}") final String idmFile) throws IOException {
        super();
        Assert.isTrue(templateFile.exists(), "Template file not found: " + templateFile.getFilename());
        this.template = asString(templateFile);
        this.idmFile = Objects.requireNonNull(idmFile);
    }

    private String asString(Resource resource) throws IOException {
        try (Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        }
    }

    public String writeScriptWithAddress(final String plcAddress) throws IOException {
        String content = StringUtils.replace(template, $IDM_FILE$, idmFile);
        content = StringUtils.replace(content, $OPC_UA_URL$, plcAddress);

        final File file = new File("building-tracker-start-script.tmp").getAbsoluteFile();
        if (file.createNewFile()) {
            logger.debug("Created new file: {}", file.toString());
        }

        logger.info("Writing script to file: {}", file.toString());
        try (final Writer writer = new FileWriter(file)) {
            FileCopyUtils.copy(content, writer);
        }

        logger.debug("Script content: \n{}", content);
        // file.deleteOnExit();
        return file.toString();
    }
}
