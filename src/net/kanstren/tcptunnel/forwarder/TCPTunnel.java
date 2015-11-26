package net.kanstren.tcptunnel.forwarder;

import net.kanstren.tcptunnel.Main;
import net.kanstren.tcptunnel.Params;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

/**
 * Creates a TCP tunnel between two endpoints via two Forwarder instances.
 * Data is forwarded in both directions using separate sockets.
 * Any error on either socket causes the whole tunnel (both sockets) to be closed.
 */
public class TCPTunnel extends Thread {
  /** Configuration parameters. */
  private final Params params;
  /** Local endpoint for the tunnel. */
  private Socket localSocket;
  /** Remote endpoint for the tunnel. */
  private Socket serverSocket;
  /** True if this tunnel is actively forwarding. False if stopped or not yet started. */
  private boolean active = false;
  /** Parent to notify when connection is broken. */
  private final Main parent;

  /**
   * @param params Configuration parameters.
   * @param localSocket Socket for the local port (endpoint 1 for tunnel).
   */
  public TCPTunnel(Params params, Socket localSocket, Main parent) {
    this.params = params;
    this.localSocket = localSocket;
    this.parent = parent;
  }

  /**
   * Connects to the remote host and starts bidirectional forwarding (the tunnel).
   */
  public void run() {
    try {
      // Connect to the destination server
      serverSocket = new Socket(params.getRemoteHost(), params.getRemotePort());

      // Turn on keep-alive for both the sockets
      serverSocket.setKeepAlive(true);
      localSocket.setKeepAlive(true);

      // Obtain client & server input & output streams
      InputStream clientIn = localSocket.getInputStream();
      OutputStream clientOut = localSocket.getOutputStream();
      InputStream serverIn = serverSocket.getInputStream();
      OutputStream serverOut = serverSocket.getOutputStream();

      // Start forwarding data between server and client
      active = true;
      Forwarder clientForward = new Forwarder(this, clientIn, serverOut, params, true);
      clientForward.start();
      Forwarder serverForward = new Forwarder(this, serverIn, clientOut, params, false);
      serverForward.start();

      if (params.isPrint()) System.out.println("TCP Forwarding " + toStr(localSocket) + " <--> " + toStr(serverSocket));
    } catch (IOException ioe) {
      if (params.isPrint()) System.err.println("Failed to connect to remote host (" + params.getRemoteHost() + ":" + params.getRemotePort()+")");
      connectionBroken();
    }
  }

  /**
   * @param socket The socket to describe.
   * @return A string representation of a socket (ip+port).
   */
  private String toStr(Socket socket) {
    String host = socket.getInetAddress().getHostAddress();
    int port = socket.getPort();
    return host + ":" + port;
  }

  /**
   * Closes the tunnel (the forwarding sockets..).
   */
  public void close() {
    connectionBroken();
  }

  /**
   * Called when an error is observed on one of the sockets making up the tunnel.
   * Terminates the tunnel by closing both sockets.
   */
  public synchronized void connectionBroken() {
    try {
      serverSocket.close();
    } catch (Exception e) {}
    try {
      localSocket.close();
    } catch (Exception e) {}

    if (active) {
      if (params.isPrint()) System.out.println("TCP Forwarding " + toStr(localSocket) + " <--> " + toStr(serverSocket) + " stopped.");
      active = false;
    }
    parent.closed(this);
  }
}

