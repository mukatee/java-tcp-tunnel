package net.kanstren.tcptunnel.capture.tcp;

import net.kanstren.tcptunnel.Main;
import net.kanstren.tcptunnel.Params;
import net.kanstren.tcptunnel.PortManager;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

/**
 * @author Teemu Kanstren.
 */
public class MirrorLoggingTests {
  @BeforeMethod
  public void before() {
  }

  @AfterMethod
  public void after() {
  }

  @Test
  public void mirrorUpStream() throws Exception {
    //create a test server to give us a page to request
    int serverPort = PortManager.port();
    int proxyPort = PortManager.port();
    int mirrorPort = PortManager.port();
    TCPTestServer2 server1 = new TCPTestServer2(serverPort, "mirror test1");
    server1.start();
    TCPTestServer2 server2 = new TCPTestServer2(mirrorPort, "mirror test2");
    server2.start();
    //configure the tunnel to accept connections on proxyport and forward them to localhost:serverport
    Params params = new Params(proxyPort, "localhost", serverPort);
    params.enableMirrorUpStreamLogger("localhost", mirrorPort);
    //this is how we actually start the tunnel
    Main main = new Main(params);
    main.start();
    Thread.sleep(50);
    //send a test request to get some data in the tunnel
    String response = TCPMsgSender.send2("localhost", proxyPort, "hi there");
    //check we got the correct response from the server
    assertEquals(response, "mirror test1", "Response content");

    //short sleep in hopes the tunnel is closed
    Thread.sleep(100);
    String receiveString = server1.getReceiveString();
    assertEquals(receiveString, "hi there", "Received stream for regular forward stream");
    String receiveString2 = server2.getReceiveString();
    assertEquals(receiveString2, "hi there", "Received stream for mirrored forward stream");

    server1 = new TCPTestServer2(serverPort, "mirror test1");
    server1.start();
    server2 = new TCPTestServer2(mirrorPort, "mirror test2");
    server2.start();

    String response2 = TCPMsgSender.send2("localhost", proxyPort, "hi there again");
    //check we got the correct response from the server for a second time
    assertEquals(response2, "mirror test1", "Response content");

    //short sleep in hopes the tunnel is closed
    Thread.sleep(100);
    receiveString = server1.getReceiveString();
    assertEquals(receiveString, "hi there again", "Received stream for mirrored forward stream");
    receiveString2 = server2.getReceiveString();
    assertEquals(receiveString2, "hi there again", "Received stream for mirrored forward stream");
  }

  @Test
  public void mirrorDownStream() throws Exception {
    //create a test server to give us a page to request
    int serverPort = PortManager.port();
    int proxyPort = PortManager.port();
    int mirrorPort = PortManager.port();
    TCPTestServer2 server1 = new TCPTestServer2(serverPort, "mirror test1");
    server1.start();
    TCPTestServer2 server2 = new TCPTestServer2(mirrorPort, "mirror test2");
    server2.start();
    //configure the tunnel to accept connections on proxyport and forward them to localhost:serverport
    Params params = new Params(proxyPort, "localhost", serverPort);
    params.enableMirrorDownStreamLogger("localhost", mirrorPort);
    //this is how we actually start the tunnel
    Main main = new Main(params);
    main.start();
    Thread.sleep(50);
    //send a test request to get some data in the tunnel
    String response = TCPMsgSender.send2("localhost", proxyPort, "hi there");
    //check we got the correct response from the server
    assertEquals(response, "mirror test1", "Response content");

    //short sleep in hopes the tunnel is closed
    Thread.sleep(100);
    String receiveString = server1.getReceiveString();
    assertEquals(receiveString, "hi there", "Received stream for regular forward stream");
    String receiveString2 = server2.getReceiveString();
    assertEquals(receiveString2, "mirror test1", "Received stream for mirrored forward stream");

    server1 = new TCPTestServer2(serverPort, "mirror test1");
    server1.start();
    server2 = new TCPTestServer2(mirrorPort, "mirror test2");
    server2.start();

    String response2 = TCPMsgSender.send2("localhost", proxyPort, "hi there again");
    //check we got the correct response from the server for a second time
    assertEquals(response2, "mirror test1", "Response content");

    //short sleep in hopes the tunnel is closed
    Thread.sleep(100);
    receiveString = server1.getReceiveString();
    assertEquals(receiveString, "hi there again", "Received stream for mirrored forward stream");
    receiveString2 = server2.getReceiveString();
    assertEquals(receiveString2, "mirror test1", "Received stream for mirrored forward stream");
  }
}
