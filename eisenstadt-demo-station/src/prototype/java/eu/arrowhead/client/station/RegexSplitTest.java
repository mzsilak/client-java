package eu.arrowhead.client.station;


import org.junit.Assert;
import org.junit.Test;


public class RegexSplitTest {

    @Test
    public void splitTest() {
        /*
command.rfid=python /home/pi/MFRC522-python/Read.py
command.power.on=/home/pi/PowerPlug/sem-6000.exp V1 --on
command.power.off=/home/pi/PowerPlug/sem-6000.exp V1 --off
command.power.status=/home/pi/PowerPlug/sem-6000.exp V1 --measure header --measure 15
         */

        String[] split = "python /home/pi/MFRC522-python/Read.py".split("\\s");
        Assert.assertEquals("python", split[0]);
        Assert.assertEquals("/home/pi/MFRC522-python/Read.py", split[1]);

        split = "/home/pi/PowerPlug/sem-6000.exp V1 --measure header --measure 15".split("\\s");
        Assert.assertEquals("/home/pi/PowerPlug/sem-6000.exp", split[0]);
        Assert.assertEquals("V1", split[1]);
        Assert.assertEquals("--measure", split[2]);
        Assert.assertEquals("header", split[3]);
        Assert.assertEquals("--measure", split[4]);
        Assert.assertEquals("15", split[5]);
    }
}