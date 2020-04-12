package net.kanstren.tcptunnel.capture.tcp;

import net.kanstren.tcptunnel.Main;
import net.kanstren.tcptunnel.Params;
import net.kanstren.tcptunnel.PortManager;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import osmo.common.TestUtils;

import static org.testng.Assert.assertEquals;

/**
 * @author Teemu Kanstren.
 */
public class ConsoleLoggingTests {
  @BeforeMethod
  public void before() {
    TestUtils.startOutputCapture();
  }

  @AfterMethod
  public void after() {
    TestUtils.endOutputCapture();
  }

  @Test
  public void captureIntList() throws Exception {
    //create a test server to give us a page to request
    int serverPort = PortManager.port();
    int proxyPort = PortManager.port();
    TCPTestServer2 server = new TCPTestServer2(serverPort, "console test1");
    server.start();
    //configure the tunnel to accept connections on port 5598 and forward them to localhost:5599
    Params params = new Params(proxyPort, "localhost", serverPort);
    params.enableByteConsoleLogger(false);
    //this is how we actually start the tunnel
    Main main = new Main(params);
    main.start();
    //send a test request to get some data in the tunnel
    String response = TCPMsgSender.send2("localhost", proxyPort, "hi there");
    //check we got the correct response from the server
    assertEquals(response, "console test1", "Response content");

    //short sleep in hopes the tunnel is closed
    Thread.sleep(100);
    TestUtils.endOutputCapture();
    String actual = TestUtils.getOutput();
    //first line prints opening tunnel, last one closing it. ports on those lines are random so better to remove them from assert
    actual = stripResponseForTest(actual);
    String expected = TestUtils.getResource(ConsoleLoggingTests.class, "expected_console1.txt")+"\n";
    assertEquals(actual, expected);
  }

  @Test
  public void captureHexString() throws Exception {
    //create a test server to give us a page to request
    int serverPort = PortManager.port();
    int proxyPort = PortManager.port();
    TCPTestServer2 server = new TCPTestServer2(serverPort, "hex console test");
    server.start();
    //configure the tunnel to accept connections on port 5598 and forward them to localhost:5599
    Params params = new Params(proxyPort, "localhost", serverPort);
    params.enableByteConsoleLogger(true);
    //this is how we actually start the tunnel
    Main main = new Main(params);
    main.start();
    //send a test request to get some data in the tunnel
    String response = TCPMsgSender.send2("localhost", proxyPort, "hi there hex boy");
    //check we got the correct response from the server
    assertEquals(response, "hex console test", "Response content");

    //short sleep in hopes the tunnel is closed
    Thread.sleep(100);
    TestUtils.endOutputCapture();
    String actual = TestUtils.getOutput();
    //first line prints opening tunnel, last one closing it. ports on those lines are random so better to remove them from assert
    actual = stripResponseForTest(actual);
    String expected = TestUtils.getResource(ConsoleLoggingTests.class, "expected_console3.txt")+"\n";
    assertEquals(actual, expected);
  }

  @Test
  public void captureString() throws Exception {
    int serverPort = PortManager.port();
    int proxyPort = PortManager.port();
    //create a test server to give us a page to request
    TCPTestServer2 server = new TCPTestServer2(serverPort, "console string test\n");
    server.start();
    //configure the tunnel to accept connections on port 5598 and forward them to localhost:5599
    Params params = new Params(proxyPort, "localhost", serverPort);
    params.enableStringConsoleLogger();
    //this is how we actually start the tunnel
    Main main = new Main(params);
    main.start();
    //send a test request to get some data in the tunnel
    String response = TCPMsgSender.send2("localhost", proxyPort, "hi there");
    //check we got the correct response from the server
    assertEquals(response, "console string test\n", "Response content");

    //short sleep in hopes the tunnel is closed
    Thread.sleep(100);
    TestUtils.endOutputCapture();
    String actual = TestUtils.getOutput();
    actual = stripResponseForTest(actual);
    String expected = TestUtils.getResource(ConsoleLoggingTests.class, "expected_console2.txt")+"\n";
    assertEquals(actual, expected);
  }

  @Test
  public void captureStringExtraLF() throws Exception {
    int serverPort = PortManager.port();
    int proxyPort = PortManager.port();
    //create a test server to give us a page to request
    TCPTestServer2 server = new TCPTestServer2(serverPort, "console string test\n");
    server.start();
    //configure the tunnel to accept connections on port 5598 and forward them to localhost:5599
    Params params = new Params(proxyPort, "localhost", serverPort);
    //this has to be set before enabling logger or it will not pass through
    params.setAddLF(true);
    params.enableStringConsoleLogger();
    //this is how we actually start the tunnel
    Main main = new Main(params);
    main.start();
    //send a test request to get some data in the tunnel
    String response = TCPMsgSender.send2("localhost", proxyPort, "hi there");
    //check we got the correct response from the server
    assertEquals(response, "console string test\n", "Response content");

    //short sleep in hopes the tunnel is closed
    Thread.sleep(100);
    TestUtils.endOutputCapture();
    String actual = TestUtils.getOutput();
    actual = stripResponseForTest(actual);
    String expected = TestUtils.getResource(ConsoleLoggingTests.class, "expected_console4.txt")+"\n";
    assertEquals(actual, expected);
  }

  //http://stackoverflow.com/questions/8624195/how-to-remove-first-line-from-a-string-containing-xml
  public String stripResponseForTest(String from) {
    String s = TestUtils.unifyLineSeparators(from, "\n");
    String[] lines = s.split("\n");
    StringBuilder sb = new StringBuilder(from.length());
    for (String line : lines) {
      //remove tunnel status lines. ports on those lines are random so better to remove them from assert
      if (line.contains(": TCP Forwarding")) continue;
      //remove date lines as datetimes vary across executions
//      if (line.startsWith("Date")) continue;
      //trim the line since looking at the expected files in editor stripts trailing whitespace
      sb.append(line.trim());
      sb.append("\n");
    }
    return sb.toString();
  }
}
