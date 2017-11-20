package net.kanstren.tcptunnel.capture.udp;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

/**
 * @author Teemu Kanstren.
 */
public class UDPTestServer implements Runnable {
  private final int port;
  private final byte[] receiveBuffer = new byte[8192];
  private int bytesReceived = 0;
  private boolean started = false;

  public UDPTestServer(int port) {
    this.port = port;
  }

  public static void main(String[] args) throws Exception {
    UDPTestServer server = new UDPTestServer(Integer.parseInt(args[0]));
    server.start();
  }

  public void start() throws Exception {
    new Thread(this).start();
  }

  public boolean isStarted() {
    return started;
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
    DatagramSocket server = new DatagramSocket(port);
    DatagramPacket pkt = new DatagramPacket(receiveBuffer, receiveBuffer.length);
    System.out.println("UDP test server listening on port "+port);
    started = true; //still could race but rather small chance and its just for testing
    server.receive(pkt);
    bytesReceived = pkt.getLength();
    System.out.println("UDP test server received "+bytesReceived+" bytes, exiting.");
    server.close();
  }

  public byte[] getReceiveBytes() {
    byte[] slice = Arrays.copyOfRange(receiveBuffer, 0, bytesReceived);;
    return slice;
  }
}
