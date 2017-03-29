package net.kanstren.tcptunnel.capture;

import fi.iki.elonen.NanoHTTPD;

import java.util.Map;

/**
 * @author Teemu Kanstren.
 */
public class TestServer extends NanoHTTPD {
  private final String msg;

  public TestServer(int port, String msg) {
    super(port);
    this.msg = msg;
  }

  @Override
  public Response serve(IHTTPSession session) {
    System.out.println("received request...");
    return newFixedLengthResponse(msg);
  }

  public static void main(String[] args) throws Exception {
    int port = 6688;
    if (args.length > 0) {
      port = Integer.parseInt(args[0]);
    }
    TestServer server = new TestServer(port, "hello from test srv");
    server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
  }
}
