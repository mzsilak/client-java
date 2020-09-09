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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

@Component
public class IdaIceConfigParser {

    private static final String $IDM_FILE$ = "$IDM_FILE$";
    private static final String $IMPORT$ = "$IMPORT$";
    private static final String $OPC_UA_URL$ = "$OPC_UA_URL$";

    private final String template;
    private final String idmFile;
    private final String importStatement;


    public IdaIceConfigParser(@Value("${idaice.template}") final Resource templateFile,
                              @Value("${idaice.idm-file}") final String idmFile,
                              @Value("${idaice.import}") final String importStatement) throws IOException {
        super();
        Assert.isTrue(templateFile.exists(), "Template file not found: " + templateFile.getFilename());
        this.template = asString(templateFile);
        this.idmFile = Objects.requireNonNull(idmFile);
        this.importStatement = Objects.requireNonNull(importStatement);
    }

    private String asString(Resource resource) throws IOException {
        try (Reader reader = new InputStreamReader(resource.getInputStream(), UTF_8)) {
            return FileCopyUtils.copyToString(reader);
        }
    }

    public String writeScriptWithAddress(final String plcAddress) throws IOException {
        String content = template.replace($IDM_FILE$, idmFile);
        content = content.replace($IMPORT$, importStatement);
        content = content.replace($OPC_UA_URL$, plcAddress);

        final Path tempFile = Files.createTempFile("building-tracker-start-script-", ".tmp");

        try (final Writer writer = new FileWriter(tempFile.toFile())) {
            FileCopyUtils.copy(content, writer);
        }

        final File file = tempFile.toAbsolutePath().toFile();
        file.deleteOnExit();
        return file.toString();
    }
}
