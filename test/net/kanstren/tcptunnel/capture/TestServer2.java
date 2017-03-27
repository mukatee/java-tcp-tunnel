package net.kanstren.tcptunnel.capture;

import net.kanstren.tcptunnel.observers.TCPObserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author Teemu Kanstren.
 */
public class TestServer2 implements Runnable {
  private final int port;
  private final byte[] bytes;

  public TestServer2(int port, String msg) {
    this.port = port;
    this.bytes = msg.getBytes();
  }

  public TestServer2(int port, byte[] bytes) {
    this.port = port;
    this.bytes = bytes;
  }

  public void start() throws Exception {
    new Thread(this).start();
  }

  @Override
  public void run() {
    try {
      runrun();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void runrun() throws Exception {
    ServerSocket server = new ServerSocket(port);
    Socket serverSocket = server.accept();

    InputStream is = serverSocket.getInputStream();
    OutputStream os = serverSocket.getOutputStream();
    byte[] buffer = new byte[8192];
    int bytesRead = is.read(buffer);

    os.write(bytes, 0, bytes.length);
    os.flush();
    serverSocket.close();
    server.close();
  }
}
