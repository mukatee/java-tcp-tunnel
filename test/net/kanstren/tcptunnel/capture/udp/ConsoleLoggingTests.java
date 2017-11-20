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

import static org.testng.Assert.assertEquals;

/**
 * @author Teemu Kanstren.
 */
public class ConsoleLoggingTests {
  private Params params = null;
  private UDPTestServer server = null;

  @BeforeMethod
  public void before() throws Exception {
    //create a test server to capture the data at the other end of the tunnel
    int serverPort = PortManager.port();
    int proxyPort = PortManager.port();
    server = new UDPTestServer(serverPort);
    server.start();
    while (!server.isStarted()) {
      //first wait for server to start up before sending it something
      Thread.sleep(100);
    }
    //configure the tunnel to accept connections on proxyport and forward them to localhost:serverport
    params = new Params(proxyPort, "localhost", serverPort);
    params.setUDP(true);
    params.setPrint(false);

    TestUtils.startOutputCapture();
  }

  @AfterMethod
  public void after() {
    TestUtils.endOutputCapture();
  }

  private void sendAndReadResponse(String msg, String expectedFileName) throws Exception {
    //send test data in the tunnel
    UDPMsgSender.send2("localhost", params.getSourcePort(), msg);
    //wait for UDP server to receive the sent data
    Thread.sleep(100);
    //check the server received the data
    assertEquals(server.getReceiveString(), msg, "UDP Server received data");

    TestUtils.endOutputCapture();
    String actual = TestUtils.getOutput();
    //first line prints opening tunnel, last one closing it. ports on those lines are random so better to remove them from assert
    actual = stripResponseForTest(actual);
    String expected = TestUtils.getResource(ConsoleLoggingTests.class, expectedFileName)+"\n";
    assertEquals(actual, expected);
  }

  @Test
  public void captureIntList() throws Exception {
    params.enableByteConsoleLogger(false);
    //this is how we actually start the tunnel
    Main main = new Main(params);
    main.start();
    Thread.sleep(100);
    sendAndReadResponse("hi there", "expected_console1.txt");
  }

  @Test
  public void captureHexString() throws Exception {
    params.enableByteConsoleLogger(true);
    //this is how we actually start the tunnel
    Main main = new Main(params);
    main.start();
    Thread.sleep(100);
    sendAndReadResponse("hi there hex boy", "expected_console2.txt");
  }

  @Test
  public void captureString() throws Exception {
    params.enableStringConsoleLogger();
    //this is how we actually start the tunnel
    Main main = new Main(params);
    main.start();
    Thread.sleep(100);
    sendAndReadResponse("hi there\n", "expected_console3.txt");
  }

  //http://stackoverflow.com/questions/8624195/how-to-remove-first-line-from-a-string-containing-xml
  public String stripResponseForTest(String from) {
    String s = TestUtils.unifyLineSeparators(from, "\n");
    String[] lines = s.split("\n");
    StringBuilder sb = new StringBuilder(from.length());
    for (String line : lines) {
      //remove tunnel status lines. ports on those lines are random so better to remove them from assert
      if (line.startsWith("UDP Forwarding")) continue;
      //remove also output from UDP test server
      if (line.startsWith("UDP test")) continue;
      //trim the line since looking at the expected files in editor stripts trailing whitespace
      sb.append(line.trim());
      sb.append("\n");
    }
    return sb.toString();
  }
}
