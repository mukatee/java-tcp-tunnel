package net.kanstren.tcptunnel.capture.udp;

import net.kanstren.tcptunnel.Main;
import net.kanstren.tcptunnel.Params;
import net.kanstren.tcptunnel.PortManager;
import net.kanstren.tcptunnel.capture.tcp.TCPMsgSender;
import net.kanstren.tcptunnel.capture.tcp.TCPTestServer2;
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
    //create a test server to send data to
    int serverPort = PortManager.port();
    int proxyPort = PortManager.port();
    int mirrorPort = PortManager.port();
    UDPTestServer server1 = new UDPTestServer(serverPort);
    server1.start();
    UDPTestServer server2 = new UDPTestServer(mirrorPort);
    server2.start();

    while (!server1.isStarted() || !server2.isStarted()) {
      //first wait for server to start up before sending it something
      Thread.sleep(100);
    }

    //configure the tunnel to accept connections on proxyport and forward them to localhost:serverport
    Params params = new Params(proxyPort, "localhost", serverPort);
    params.enableMirrorUpStreamLogger("localhost", mirrorPort);
    params.setUDP(true);
    //this is how we actually start the tunnel
    Main main = new Main(params);
    main.start();
    Thread.sleep(100);

    //send a test request to get some data in the tunnel
    UDPMsgSender.send2("localhost", proxyPort, "hi there");
    Thread.sleep(300);
    //check we got the correct data at test servers
    assertEquals(server1.getReceiveString(), "hi there", "Data received by UDP test server");
    assertEquals(server2.getReceiveString(), "hi there", "Data received by UDP test server mirror");

    //now re-test with a new connection as real systems keep opening new connections all the time

    server1 = new UDPTestServer(serverPort);
    server1.start();
    server2 = new UDPTestServer(mirrorPort);
    server2.start();

    //send a test request to get some data in the tunnel
    UDPMsgSender.send2("localhost", proxyPort, "hi there again");
    Thread.sleep(100);
    //check we got the correct data at test servers
    assertEquals(server1.getReceiveString(), "hi there again", "Data received by UDP test server");
    assertEquals(server2.getReceiveString(), "hi there again", "Data received by UDP test server mirror");
  }

  //TODO: configuring downstream logger for UDP should actually fail..

}
