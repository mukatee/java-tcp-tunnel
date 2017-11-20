package net.kanstren.tcptunnel.capture.tcp;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * @author Teemu Kanstren.
 */
public class TCPTestServer2 implements Runnable {
  private final int port;
  private final byte[] bytesToSend;
  private final byte[] receiveBuffer = new byte[8192];
  private int bytesReceived = 0;

  public TCPTestServer2(int port, String msg) {
    this.port = port;
    this.bytesToSend = msg.getBytes();
  }

  public TCPTestServer2(int port, byte[] bytes) {
    this.port = port;
    this.bytesToSend = bytes;
  }

  public static void main(String[] args) throws Exception {
    byte[] dltestBytes = {0x02, 0x00, 0x01};
    TCPTestServer2 server = new TCPTestServer2(Integer.parseInt(args[0]), dltestBytes);
    server.start();
  }

  public void start() throws Exception {
    new Thread(this).start();
  }

  public String getReceiveString() {
    return new String(receiveBuffer, 0, bytesReceived);
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
//    byte[] buffer = new byte[8192];
    bytesReceived = is.read(receiveBuffer);

    os.write(bytesToSend, 0, bytesToSend.length);
    os.flush();
    serverSocket.close();
    server.close();
  }
}
