package net.kanstren.tcptunnel.capture.tcp;

import fi.iki.elonen.NanoHTTPD;

/**
 * @author Teemu Kanstren.
 */
public class TCPTestServer1 extends NanoHTTPD {
  private final String msg;

  public TCPTestServer1(int port, String msg) {
    super(port);
    this.msg = msg;
  }

  @Override
  public Response serve(IHTTPSession session) {
    //System.out.println("received request...");
    Response response = newFixedLengthResponse(msg);
    response.setGzipEncoding(true);
    return response;
  }

  public static void main(String[] args) throws Exception {
    int port = 6688;
    if (args.length > 0) {
      port = Integer.parseInt(args[0]);
    }
    TCPTestServer1 server = new TCPTestServer1(port, "hello from test srv");
    server.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
  }
}
