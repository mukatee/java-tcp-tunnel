package net.kanstren.tcptunnel.forwarder;

import net.kanstren.tcptunnel.Params;
import net.kanstren.tcptunnel.observers.TCPObserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;

/**
 * Created by AlexZhuo on 2017/11/9.
 */
public class DNSForwarder extends Thread {
  /** Address to forward packets to. */
  private InetAddress fwdAddr;
  /** Port to forward packets to. */
  private int fwdPort;
  /** Socket to receive packets to forward. */
  private DatagramSocket fwdSocket;
  /** Socket to send packets to forward. */
  private DatagramSocket responseSocket;
  private DatagramPacket sendData;
  private List<TCPObserver> upObservers;
  private List<TCPObserver> downObservers;

  public DNSForwarder(DatagramPacket packet, DatagramSocket responseSocket, InetAddress fwdAddr, int fwdPort, Params params) throws Exception {
    this.fwdAddr = InetAddress.getByAddress(fwdAddr.getAddress());
    this.fwdPort = fwdPort;
    this.responseSocket = responseSocket;
    byte[] sendmsg = Arrays.copyOf(packet.getData(), packet.getLength());
    sendData = new DatagramPacket(sendmsg, packet.getLength(), InetAddress.getByName(params.getRemoteHost()), params.getRemotePort());
    this.upObservers = params.createUpObservers(fwdAddr.getHostAddress() + ":" + fwdPort);
    this.downObservers = params.createDownObservers(params.getRemoteHost());
  }

  public InetAddress getFwdAddr() {
    return fwdAddr;
  }

  public int getFwdPort() {
    return fwdPort;
  }

  @Override
  public void run() {
    //receiving the data from remote server
    DatagramPacket packet = new DatagramPacket(new byte[12800], 12800);
    try {
      fwdSocket = new DatagramSocket();
      fwdSocket.setSoTimeout(2000);
      fwdSocket.send(sendData);
      if (fwdSocket.isClosed()) return;
      fwdSocket.receive(packet);
      //send the packet to forward target
      packet.setAddress(fwdAddr);
      packet.setPort(fwdPort);
      //send received packet back to the source that did the query
      responseSocket.send(packet);
      for (TCPObserver observer : upObservers) {
        observer.observe(sendData.getData(), 0, sendData.getLength());
      }
      for (TCPObserver observer : downObservers) {
        observer.observe(packet.getData(), 0, packet.getLength());
      }
      close();
    } catch (IOException e) {
      e.printStackTrace();
      close();
    }
  }

  public void close() {
    if (fwdSocket.isClosed()) return;
    fwdSocket.close();
  }
}
