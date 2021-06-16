package net.kanstren.tcptunnel.forwarder;

import net.kanstren.tcptunnel.Main;
import net.kanstren.tcptunnel.Params;
import net.kanstren.tcptunnel.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Creates a TCP tunnel between two endpoints via two Forwarder instances.
 * Data is forwarded in both directions using separate sockets.
 * Any error on either socket causes the whole tunnel (both sockets) to be closed.
 */
public class TCPTunnel extends Thread {
  private SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MMM.dd HH:mm:ss");
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
   * @param parent Parent to notify when connection is broken
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
    String dateStr = sdf.format(new Date());
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

      // Send http 200 ok to client and start forwarding
      if (params.isHttps()) {
        // read request client and ignore
        byte[] buffer = new byte[8024/*1024*/];
        int len = clientIn.read(buffer);

        // send 200 ok
        clientOut.write("HTTP/1.1 200 OK\r\n\r\n".getBytes());
        clientOut.flush();
      }

      // Start forwarding data between server and client
      active = true;
      String clientAddr = toStr(localSocket);
      String serverAddr = toStr(serverSocket);
      String hummanClientAddr = Utils.mapAddrToHumanReadable(clientAddr);
      String hummanServerAddr = Utils.mapAddrToHumanReadable(serverAddr);
      clientAddr = clientAddr+" ("+hummanClientAddr+")";
      serverAddr = serverAddr+" ("+hummanServerAddr+")";
      TCPForwarder clientForward = new TCPForwarder(this, clientIn, serverOut, params, true, clientAddr);
      clientForward.start();
      TCPForwarder serverForward = new TCPForwarder(this, serverIn, clientOut, params, false, serverAddr);
      serverForward.start();

      if (params.isPrint()) {
        System.out.println(dateStr+": TCP Forwarding " + clientAddr + " <--> " + serverAddr);
      }
    } catch (IOException ioe) {
      if (params.isPrint()) {
        String remoteAddr = params.getRemoteHost() + ":" + params.getRemotePort();
        String humanRemoteAddr = Utils.mapAddrToHumanReadable(remoteAddr);
        remoteAddr = remoteAddr + " ("+humanRemoteAddr+")";
        System.err.println(dateStr + ": Failed to connect to remote host (" + remoteAddr + ")");
      }
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
    } catch (Exception ignored) {}
    try {
      localSocket.close();
    } catch (Exception ignored) {}

    if (active) {
      String dateStr = sdf.format(new Date());
      if (params.isPrint()) System.out.println(dateStr+": TCP Forwarding " + toStr(localSocket) + " <--> " + toStr(serverSocket) + " stopped.");
      active = false;
    }
    parent.closed(this);
  }
}

