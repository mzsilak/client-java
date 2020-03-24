package eu.arrowhead.client.station;


import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;


public class RfidReadTest {

    private final static String CARD_UID = "Card read UID: ";
    private int i = 0;

    @Test
    public void scannerTest() {
        testData().forEach(this::scanner);
        Assert.assertEquals(4, i);
    }

    public void scanner(final String string) {
        if (StringUtils.startsWith(string, CARD_UID)) {
            count(StringUtils.remove(string, CARD_UID));
        }
    }

    private void count(final String next) {
        Assert.assertEquals("118,229,45,31", next);
        i++;
    }

    /*
Welcome to the MFRC522 data read example
Press Ctrl-C to stop.


Card detected
Card read UID: 118,229,45,31
Size: 8
Sector 8 [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
Card detected
Card read UID: 118,229,45,31
Size: 8
Sector 8 [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
Card detected
Card read UID: 118,229,45,31
Size: 8
Sector 8 [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
Card detected
Card read UID: 118,229,45,31
AUTH ERROR!!
AUTH ERROR(status2reg & 0x08) != 0
Authentication error
Card detected
Card read UID: 118,229,45,31
Size: 8
Sector 8 [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
     */
    private Stream<String> testData() {
        final List<String> strings = new ArrayList<>();
        strings.add("Welcome to the MFRC522 data read example");
        strings.add("Press Ctrl-C to stop.");
        strings.add("");
        strings.add("");
        strings.addAll(getCardDetected());
        strings.addAll(getCardDetected());
        strings.addAll(getCardDetected());
        strings.add("AUTH ERROR!!");
        strings.add("AUTH ERROR(status2reg & 0x08) != 0");
        strings.add("Authentication error");
        strings.addAll(getCardDetected());

        return strings.stream();
    }

    private List<String> getCardDetected() {
        return List
            .of("Card read UID: 118,229,45,31", "Size: 8", "Sector 8 [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]");
    }


}