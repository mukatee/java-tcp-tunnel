package net.kanstren.tcptunnel.capture.udp;

import net.kanstren.tcptunnel.Main;
import net.kanstren.tcptunnel.Params;
import net.kanstren.tcptunnel.PortManager;
import net.kanstren.tcptunnel.capture.tcp.TCPMsgSender;
import net.kanstren.tcptunnel.capture.tcp.TCPTestServer2;
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
    UDPTestServer server = new UDPTestServer(serverPort);
    server.start();
    while (!server.isStarted()) {
      //first wait for server to start up before sending it something
      Thread.sleep(100);
    }
    //configure the tunnel to accept connections on proxyport and forward them to localhost:serverpot
    Params params = new Params(proxyPort, "localhost", serverPort);
    params.enableByteFileLogger(null, UPLOADLOGFILE); //TODO: check if UDP logging enabled downstream logger definition should give error
    params.setUDP(true);
    //this is how we actually start the tunnel
    Main main = new Main(params);
    main.start();
    Thread.sleep(50);
    //send a test request to get some data in the tunnel
    UDPMsgSender.send2("localhost", proxyPort, "hi there");
    Thread.sleep(300);
    //check we got the correct data at test server
    assertEquals(server.getReceiveString(), "hi there", "Data received by UDP test server");

    String up = TestUtils.readFile(UPLOADLOGFILE + ".bytes", "UTF8");
    up = TestUtils.unifyLineSeparators(up, "\n");
    assertEquals(up, "hi there");
  }

  @Test
  public void captureBinaryBytes() throws Exception {
    //create a test server to give us a page to request
    int serverPort = PortManager.port();
    int proxyPort = PortManager.port();
    byte[] ultestBytes = {0x05, 0x06, 0x07};
    UDPTestServer server = new UDPTestServer(serverPort);
    server.start();
    while (!server.isStarted()) {
      //first wait for server to start up before sending it something
      Thread.sleep(100);
    }
    //configure the tunnel to accept connections on proxyport and forward them to localhost:serverpot
    Params params = new Params(proxyPort, "localhost", serverPort);
    params.enableByteFileLogger(null, UPLOADLOGFILE); //TODO: check if UDP logging enabled downstream logger definition should give error
    params.setUDP(true);
    //this is how we actually start the tunnel
    Main main = new Main(params);
    main.start();
    Thread.sleep(50);
    //send a test request to get some data in the tunnel
    UDPMsgSender.send2("localhost", proxyPort, ultestBytes);
    Thread.sleep(300);

    //check we got the correct response from the server
    assertBytes(server.getReceiveBytes(), ultestBytes);

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
