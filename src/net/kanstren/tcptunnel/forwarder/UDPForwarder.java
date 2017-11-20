package net.kanstren.tcptunnel.forwarder;

import net.kanstren.tcptunnel.Params;
import net.kanstren.tcptunnel.observers.TCPObserver;

import java.io.IOException;
import java.net.*;
import java.util.List;

/**
 * Created by AlexZhuo on 2017/11/12.
 * Fixes, tests, conformance by Teemu Kanstren
 */
public class UDPForwarder extends Thread {
  private InetAddress clientAddr;
  private int clientPort;
  private DatagramSocket receiveSocket;
  private DatagramSocket fwdSocket;
  private UDPTunnel parent;
  /** The observers to pass all data through. Logging the data etc. */
  private final List<TCPObserver> observers;

  public UDPForwarder(UDPTunnel parent, DatagramSocket fwdSocket, InetAddress clientAddr, int clientPort, Params params) {
    this.parent = parent;
    //UDP is one way only, no replies so no downstream..
    this.observers = params.createUpObservers(params.getRemoteHost());
    this.clientPort = clientPort;
    this.fwdSocket = fwdSocket;
    try {
      this.clientAddr = InetAddress.getByAddress(clientAddr.getAddress());
      receiveSocket = new DatagramSocket();
      receiveSocket.setSoTimeout(30000);//30s UDP tunnel TimeOut
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public DatagramSocket getReceiveSocket() {
    return this.receiveSocket;
  }

  public InetAddress getClientAddr() {
    return clientAddr;
  }

  public int getClientPort() {
    return clientPort;
  }

  @Override
  public void run() {
    //receiving the data to be forwarded
    DatagramPacket packet = new DatagramPacket(new byte[65535], 65535);
    try {
      while (true) {
        if (receiveSocket.isClosed()) return;
        receiveSocket.receive(packet);

        //send the copied packet from remote server to client
        packet.setAddress(clientAddr);
        packet.setPort(clientPort);
        fwdSocket.send(packet);
        for (TCPObserver observer : observers) {
          observer.observe(packet.getData(), 0, packet.getLength());
        }
      }
    } catch (Exception e) {
      parent.close(this);
      close();
    }
  }

  public void close() {
    if (receiveSocket.isClosed()) return;
    receiveSocket.close();
    fwdSocket.close();
  }
}
