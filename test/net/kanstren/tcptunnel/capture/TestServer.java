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
    return newFixedLengthResponse(msg);
  }
}
