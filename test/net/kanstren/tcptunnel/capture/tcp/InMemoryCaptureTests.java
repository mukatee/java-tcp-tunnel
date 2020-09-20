package net.kanstren.tcptunnel.capture.tcp;

import net.kanstren.tcptunnel.Main;
import net.kanstren.tcptunnel.Params;
import net.kanstren.tcptunnel.PortManager;
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
    TCPTestServer2 server = new TCPTestServer2(serverPort, "test1");
    server.start();
    String response = TCPMsgSender.send2("localhost", serverPort, "hi there");
    assertEquals(response, "test1", "Response content");
  }

  @Test
  public void sendRequestMITM() throws Exception {
    int serverPort = PortManager.port();
    int proxyPort = PortManager.port();
    //create a test server to give us a page to request
    TCPTestServer2 server = new TCPTestServer2(serverPort, "test1");
    server.start();
    //configure the tunnel to accept connections on port 5598 and forward them to localhost:5599
    Params params = new Params(proxyPort, "localhost", serverPort);
    //we want to use the captured data in testing, so enable logging the tunnel data in memory with buffer size 8092 bytes
    params.enableInMemoryLogging(8092);
    //this gives us access to the data passed from client connected to port 5598 -> localhost:5599 (client to server)
    InMemoryLogger upLogger = params.getUpMemoryLogger();
    //this gives us access to the data passed from localhost:5599 -> client connected to port 5598 (server to client)
    InMemoryLogger downLogger = params.getDownMemoryLogger();
    //this is how we actually start the tunnel
    Main main = new Main(params);
    main.start();
    Thread.sleep(50);
    //send a test request to get some data in the tunnel
    String response = TCPMsgSender.send2("localhost", proxyPort, "hi there");
    //check we got the correct response from the server
    assertEquals(response, "test1", "Response content");
    //assert the HTTP protocol data passed through the tunnel both ways
    assertTcpStream(upLogger, "expected_up1.txt");
    assertTcpStream(downLogger, "expected_down1.txt");
  }

  private void assertTcpStream(InMemoryLogger logger, String filename) throws Exception {
    //here we get the actual data that was passed through the tunnel in one direction (depending if we get passed the upstream memorylogger or downstream)
    String actual = logger.getString("UTF8");
    //the rest of this is just making sure the test should run the same over different platforms and with varying date-times in HTTP headers
    actual = TestUtils.unifyLineSeparators(actual, "\n");
    String expected = TestUtils.getResource(InMemoryCaptureTests.class, filename);
    expected = TestUtils.unifyLineSeparators(expected, "\n");
    assertEquals(actual, expected, "Request content");
  }
}
