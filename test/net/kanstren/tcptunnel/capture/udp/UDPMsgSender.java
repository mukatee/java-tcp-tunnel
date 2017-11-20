package net.kanstren.tcptunnel.capture.udp;

import java.io.*;
import java.net.*;

import static net.kanstren.tcptunnel.observers.StringConsoleLogger.ln;

/**
 * @author Teemu Kanstren.
 */
public class UDPMsgSender {
  public static void send2(String host, int port, String msg) throws Exception {
    send2(host, port, msg.getBytes());
  }

  public static void send2(String host, int port, byte[] bytes) throws Exception {
    DatagramSocket socket = new DatagramSocket();
    InetAddress ip = InetAddress.getByName(host);
    DatagramPacket pkt = new DatagramPacket(bytes, bytes.length, ip, port);
    socket.send(pkt);
  }

  public static void main(String[] args) throws Exception {
    int port = Integer.parseInt(args[0]);
    send2("localhost", port, "hello1");
//    send2("localhost", 6666, "hello2");
  }
}
