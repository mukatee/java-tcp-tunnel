package net.kanstren.tcptunnel.capture;

import net.kanstren.tcptunnel.Main;
import net.kanstren.tcptunnel.Params;
import net.kanstren.tcptunnel.observers.InMemoryLogger;
import org.testng.annotations.Test;
import osmo.common.TestUtils;

import static org.testng.Assert.assertEquals;

/**
 * @author Teemu Kanstren.
 */
public class CaptureTests {
  @Test
  public void sendRequestNoMITM() throws Exception {
    TestServer server = new TestServer(5599, "test1");
    server.start();
    String response = MsgSender.send("http://localhost:5599", "hi there");
    assertEquals(response, "test1", "Response content");
    Thread.sleep(1000);
    server.stop();
  }

  @Test
  public void sendRequestMITM() throws Exception {
    //create a test server to give us a page to request
    TestServer server = new TestServer(5599, "test1");
    server.start();
    //configure the tunnel to accept connections on port 5598 and forward them to localhost:5599
    Params params = new Params(5598, "localhost", 5599);
    //we want to use the captured data in testing, so enable logging the tunnel data in memory with buffer size 8092 bytes
    params.enableInMemoryLogging(8092);
    //this gives us access to the data passed from client connected to port 5598 -> localhost:5599 (client to server)
    InMemoryLogger upLogger = params.getUpMemoryLogger();
    //this gives us access to the data passed from localhost:5599 -> client connected to port 5598 (server to client)
    InMemoryLogger downLogger = params.getDownMemoryLogger();
    //this is how we actually start the tunnel
    Main main = new Main(params);
    main.start();
    //send a test request to get some data in the tunnel
    String response = MsgSender.send("http://localhost:5598", "hi there");
    //check we got the correct response from the server
    assertEquals(response, "test1", "Response content");
    //assert the HTTP protocol data passed through the tunnel both ways
    assertTcpStream(upLogger, "expected_up1.txt");
    assertTcpStream(downLogger, "expected_down1.txt");
    //the test server sometimes seems cranky if we stop it too soon
    Thread.sleep(1000);
    server.stop();
  }

  private void assertTcpStream(InMemoryLogger logger, String filename) throws Exception {
    //here we get the actual data that was passed through the tunnel in one direction (depending if we get passed the upstream memorylogger or downstream)
    String actual = logger.getString("UTF8");
    //the rest of this is just making sure the test should run the same over different platforms and with varying date-times in HTTP headers
    actual = TestUtils.unifyLineSeparators(actual, "\n");
    String expected = TestUtils.getResource(CaptureTests.class, filename);

    String[] replaced = TestUtils.replace("##", expected, actual);
    actual = replaced[0];
    expected = replaced[1];

    expected = TestUtils.unifyLineSeparators(expected, "\n");
    assertEquals(actual, expected, "Request full content");
  }
}
