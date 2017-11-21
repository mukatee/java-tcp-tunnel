package net.kanstren.tcptunnel.forwarder;

import net.kanstren.tcptunnel.Params;
import net.kanstren.tcptunnel.observers.TCPObserver;

import java.net.*;
import java.util.List;

/**
 * Receives UDP packets and forwards them again to specified UDP endpoints.
 *
 * Created by AlexZhuo on 2017/11/12.
 * Fixes, docs, tests, conformance by Teemu Kanstren
 */
public class UDPForwarder extends Thread {
  /** Address to forward packets to. */
  private InetAddress fwdAddr;
  /** Port to forward the packets to. */
  private int fwdPort;
  /** Socket to receive packets to forward. */
  private DatagramSocket receiveSocket;
  /** Socket to send packets to forward. */
  private DatagramSocket fwdSocket;
  private UDPTunnel parent;
  /** The observers to pass all data through. Logging the data etc. */
  private final List<TCPObserver> observers;

  public UDPForwarder(UDPTunnel parent, DatagramSocket fwdSocket, InetAddress fwdAddr, int fwdPort, Params params) throws Exception {
    this.parent = parent;
    //UDP is one way only, no replies so no downstream..
    this.observers = params.createUpObservers(params.getRemoteHost());
    this.fwdPort = fwdPort;
    this.fwdSocket = fwdSocket;
    this.fwdAddr = InetAddress.getByAddress(fwdAddr.getAddress());
    receiveSocket = new DatagramSocket();
    receiveSocket.setSoTimeout(30000);//30s UDP tunnel TimeOut
  }

  public DatagramSocket getReceiveSocket() {
    return this.receiveSocket;
  }

  public InetAddress getFwdAddr() {
    return fwdAddr;
  }

  public int getFwdPort() {
    return fwdPort;
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
        packet.setAddress(fwdAddr);
        packet.setPort(fwdPort);
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
