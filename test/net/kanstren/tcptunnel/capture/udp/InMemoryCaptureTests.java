package net.kanstren.tcptunnel.capture.udp;

import net.kanstren.tcptunnel.Main;
import net.kanstren.tcptunnel.Params;
import net.kanstren.tcptunnel.PortManager;
import net.kanstren.tcptunnel.capture.tcp.TCPMsgSender;
import net.kanstren.tcptunnel.capture.tcp.TCPTestServer2;
import net.kanstren.tcptunnel.observers.InMemoryLogger;
import org.testng.annotations.Test;
import osmo.common.TestUtils;

import static org.testng.Assert.assertEquals;

/**
 * @author Teemu Kanstren.
 */
public class InMemoryCaptureTests {
  @Test
  public void sendRequestNoMITM() throws Exception {
    int serverPort = PortManager.port();
    UDPTestServer server = new UDPTestServer(serverPort);
    server.start();
    Thread.sleep(100);
    UDPMsgSender.send2("localhost", serverPort, "hi there");
    Thread.sleep(100);
    assertEquals(server.getReceiveString(), "hi there", "data received by UDP test server");
  }

  @Test
  public void sendRequestMITM() throws Exception {
    int serverPort = PortManager.port();
    int proxyPort = PortManager.port();
    //create a test server to capture data sent
    UDPTestServer server = new UDPTestServer(serverPort);
    server.start();
    while (!server.isStarted()) {
      //first wait for server to start up before sending it something
      Thread.sleep(100);
    }
    //configure the tunnel to accept connections on proxyport and forward them to localhost:serverport
    Params params = new Params(proxyPort, "localhost", serverPort);
    params.setUDP(true);
    //we want to use the captured data in testing, so enable logging the tunnel data in memory with buffer size 8092 bytes
    params.enableInMemoryLogging(8092);
    //this gives us access to the data passed from client connected to port 5598 -> localhost:5599 (client to server)
    InMemoryLogger upLogger = params.getUpMemoryLogger();
    //TODO: check that UDP forwarding fails to start if downstream logging is attempted
    //TODO: check what to do if many different loggers attempted at the same time
    //this is how we actually start the tunnel
    Main main = new Main(params);
    main.start();
    Thread.sleep(10);
    //send a test request to get some data in the tunnel
    UDPMsgSender.send2("localhost", proxyPort, "hi there");
    Thread.sleep(500);
    //check the data was passed through to the server
    assertEquals(server.getReceiveString(), "hi there", "data received by UDP test server");
    assertUDPStream(upLogger, "expected_up1.txt");
  }

  private void assertUDPStream(InMemoryLogger logger, String filename) throws Exception {
    //here we get the actual data that was passed through the tunnel (UDP only does one-way so this has to be upstream)
    String actual = logger.getString("UTF8");
    //the rest of this is just making sure the test should run the same over different platforms and with varying date-times in HTTP headers
    actual = TestUtils.unifyLineSeparators(actual, "\n");
    String expected = TestUtils.getResource(InMemoryCaptureTests.class, filename);
    expected = TestUtils.unifyLineSeparators(expected, "\n");
    assertEquals(actual, expected, "Request content");
  }
}
