package net.kanstren.tcptunnel.capture.tcp;

import net.kanstren.tcptunnel.Main;
import net.kanstren.tcptunnel.Params;
import net.kanstren.tcptunnel.PortManager;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import osmo.common.TestUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.testng.Assert.assertEquals;

/**
 * @author Teemu Kanstren.
 */
public class FileLoggingTests {
  private static final String OUTPUTDIR = "testoutputdirthatshouldbedeletedaftertestsarerun";
  private static final String DOWNLOADLOGFILE = OUTPUTDIR+"/down";
  private static final String UPLOADLOGFILE = OUTPUTDIR+"/up";

  @BeforeMethod
  public void before() {
    File path = new File(OUTPUTDIR);
    path.mkdirs();
  }

  @AfterMethod
  public void after() {
    TestUtils.recursiveDelete(OUTPUTDIR);
  }

  @Test
  public void captureTextBytes() throws Exception {
    //create a test server to give us a page to request
    int serverPort = PortManager.port();
    int proxyPort = PortManager.port();
    TCPTestServer2 server = new TCPTestServer2(serverPort, "console test1");
    server.start();
    //configure the tunnel to accept connections on port 5598 and forward them to localhost:5599
    Params params = new Params(proxyPort, "localhost", serverPort);
    params.enableByteFileLogger(DOWNLOADLOGFILE, UPLOADLOGFILE);
    //this is how we actually start the tunnel
    Main main = new Main(params);
    main.start();
    Thread.sleep(10);
    //send a test request to get some data in the tunnel
    String response = TCPMsgSender.send2("localhost", proxyPort, "hi there");
    //check we got the correct response from the server
    assertEquals(response, "console test1", "Response content");

    //short sleep in hopes the tunnel is closed
    Thread.sleep(100);
    String down = TestUtils.readFile(DOWNLOADLOGFILE + ".bytes", "UTF8");
    down = TestUtils.unifyLineSeparators(down, "\n");
    String up = TestUtils.readFile(UPLOADLOGFILE + ".bytes", "UTF8");
    up = TestUtils.unifyLineSeparators(up, "\n");
    assertEquals(down, "console test1");
    assertEquals(up, "hi there");
  }

  @Test
  public void captureBinaryBytes() throws Exception {
    //create a test server to give us a page to request
    int serverPort = PortManager.port();
    int proxyPort = PortManager.port();
    byte[] dltestBytes = {0x02, 0x00, 0x01};
    byte[] ultestBytes = {0x05, 0x06, 0x07};
    TCPTestServer2 server = new TCPTestServer2(serverPort, dltestBytes);
    server.start();
    //configure the tunnel to accept connections on port 5598 and forward them to localhost:5599
    Params params = new Params(proxyPort, "localhost", serverPort);
    params.enableByteFileLogger(DOWNLOADLOGFILE, UPLOADLOGFILE);
    //this is how we actually start the tunnel
    Main main = new Main(params);
    main.start();
    Thread.sleep(10);
    //send a test request to get some data in the tunnel
    byte[] response = TCPMsgSender.send2("localhost", proxyPort, ultestBytes);
    //check we got the correct response from the server
    assertBytes(dltestBytes, response);

    //short sleep in hopes the tunnel is closed
    Thread.sleep(100);
    assertBytes(dltestBytes, DOWNLOADLOGFILE+".bytes");
    assertBytes(ultestBytes, UPLOADLOGFILE+".bytes");
  }

  private static void assertBytes(byte[] expected, String pathToActual) throws Exception {
    Path path = Paths.get(pathToActual);
    byte[] actualBytes = Files.readAllBytes(path);
    assertBytes(expected, actualBytes);
  }

  private static void assertBytes(byte[] expected, byte[] actual) throws Exception {
    assertEquals(actual.length, expected.length, "Number of bytes captured");
    for (int i = 0 ; i < expected.length ; i++) {
      assertEquals(actual[i], expected[i], "Byte logged at index: "+i);
    }
  }
}
