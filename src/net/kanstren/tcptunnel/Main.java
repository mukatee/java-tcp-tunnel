package net.kanstren.tcptunnel;

import net.kanstren.tcptunnel.forwarder.TCPTunnel;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

/**
 * Main starting point for the tunnel application. Either directly from command line or programmatically.
 * When running programmatically, create the configuration with the {@link Params} object, create an instance of this class and invoke the start() method.
 */
public class Main implements Runnable {
  /** The configuration for the tunnel. */
  private final Params params;
  /** As long as this is true, we wait for new connections for the tunnel. */
  private boolean shouldRun = true;
  /** The main thread running this tunnel. */
  private Thread thread = null;
  /** List of active tunnels. */
  private final List<TCPTunnel> tunnels = new ArrayList<>();
  private ServerSocket serverSocket;

  public Main(Params params) {
    this.params = params;
  }

  public static void main(String[] args) {
    Params params = ArgumentParser.parseArgs(args);
    if (params.getErrors().length() > 0) {
      System.err.println(params.getErrors());
    }
    //if the parametesr do not parse correctly or something required is missing, exit
    if (!params.shouldRun()) {
      return;
    }
    Main main = new Main(params);
    main.run();
  }

  /**
   * Use this to start the actual tunneling.
   */
  public void start() {
    thread = new Thread(this);
    thread.start();
  }

  @Override
  public void run() {
    try {
      serverSocket =  new ServerSocket(params.getSourcePort());
      while (shouldRun) {
        Socket clientSocket = serverSocket.accept();
        TCPTunnel tunnel = new TCPTunnel(params, clientSocket, this);
        tunnel.start();
        tunnels.add(tunnel);
      }
    } catch (IOException e) {
      throw new RuntimeException("Error while trying to forward TCP with params:"+params, e);
    }
  }

  /**
   * Called when a tunnel is closed to remove it from active list.
   * 
   * @param tunnel The closed tunnel.
   */
  public void closed(TCPTunnel tunnel) {
    tunnels.remove(tunnel);
  }

  /**
   * Stops the tunneling application (no more waiting for new connections to open tunnels).
   */
  public void stop() {
    shouldRun = false;
    try {
      if (serverSocket != null) serverSocket.close();
    } catch (IOException e) {
      if (params.isPrint()) {
        System.err.println("Error closing server socket");
        e.printStackTrace();
      }
    }
    for (TCPTunnel tunnel : tunnels) {
      tunnel.close();
    }
  }
}

