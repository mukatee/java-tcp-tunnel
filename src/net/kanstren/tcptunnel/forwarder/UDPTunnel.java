package net.kanstren.tcptunnel.forwarder;

import net.kanstren.tcptunnel.Params;
import net.kanstren.tcptunnel.Utils;
import net.kanstren.tcptunnel.observers.TCPObserver;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by AlexZhuo on 2017/11/12.
 *
 * Fixes, tests, conformance by Teemu Kanstren
 */
public class UDPTunnel extends Thread {
  /** Formats dates for logging. */
  private SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MMM.dd HH:mm:ss");
  /** Configuration parameters. */
  private final Params params;
  /** True if this tunnel is actively forwarding. False if stopped or not yet started. */
  private boolean active = false;
  /** For receiving data from tunnel source. */
  private DatagramSocket receiverSocket = null;
  /** For forwarding data to tunnel target. */
  private DatagramSocket fwdSocket = null;
  private List<TCPObserver> upObservers;
  private HashMap<String, UDPForwarder> forwarders = new HashMap<>();

  public UDPTunnel(Params params, DatagramSocket receiverSocket) {
    this.params = params;
    this.receiverSocket = receiverSocket;
    this.upObservers = params.createUpObservers("Unknown");
  }

  public void run() {
    InetAddress address = null;
    String dateStr = sdf.format(new Date());
    try {
      fwdSocket = new DatagramSocket();
      address = InetAddress.getByName(params.getRemoteHost());
    } catch (Throwable e) {
      fwdSocket.close();
      if (params.isPrint()) {
        String remoteAddr = params.getRemoteHost() + ":" + params.getRemotePort();
        String humanRemoteAddr = Utils.mapAddrToHumanReadable(remoteAddr);
        remoteAddr = remoteAddr + " (" + humanRemoteAddr + ")";
        System.err.println(dateStr + ": Failed to connect to remote host (" + remoteAddr + ")");
        e.printStackTrace();
      }
      return;
    }
    byte[] buffer = new byte[65536];
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    active = true;
    while (active) {
      try {
        packet.setData(buffer);
        packet.setLength(buffer.length);
        receiverSocket.receive(packet);
        InetAddress sourceAddress = packet.getAddress();
        int srcPort = packet.getPort();
        String strAddr = toStr(packet);
        if (params.isPrint()) {
          System.out.println(dateStr + ": UDP Forwarding " +packet.getLength()+ " bytes " + strAddr + " --> " + address.getHostAddress() + ":" + params.getRemotePort());
        }
        //send client request to server
        packet.setPort(params.getRemotePort());
        packet.setAddress(address);
        //for multiple clients
        UDPForwarder f = forwarders.get(strAddr);
        if (f == null || f.getReceiveSocket().isClosed() || !f.isAlive()) {
          if (params.isPrint()) System.out.println(dateStr+"new UDP session::" + strAddr);
          f = new UDPForwarder(this, fwdSocket, sourceAddress, srcPort, params);
          forwarders.put(strAddr, f);
          f.start();
        }
        f.getReceiveSocket().send(packet);
        for (TCPObserver observer : upObservers) {
          observer.observe(packet.getData(), 0, packet.getLength());
        }
      } catch (Throwable e) {
        if (params.isPrint()) {
          String remoteAddr = params.getRemoteHost() + ":" + params.getRemotePort();
          String humanRemoteAddr = Utils.mapAddrToHumanReadable(remoteAddr);
          remoteAddr = remoteAddr + " (" + humanRemoteAddr + ")";
          System.err.println(dateStr + ": Failed to connect to remote host (" + remoteAddr + ")");
          e.printStackTrace();
        }
        connectionBroken();
      }
    }
  }

  private String toStr(DatagramPacket packet) {
    String host = packet.getAddress().getHostAddress();
    int port = packet.getPort();
    return host + ":" + port;
  }

  private String toStr(DatagramSocket socket) {
    String host = socket.getInetAddress().getHostAddress();
    int port = socket.getPort();
    return host + ":" + port;
  }

  public String toStr(UDPForwarder f) {
    String host = f.getFwdAddr().getHostAddress();
    int port = f.getFwdPort();
    return host + ":" + port;
  }

  public void close() {
    connectionBroken();
  }

  public void close(UDPForwarder f) {
    String clientAddress = toStr(f);
    forwarders.remove(clientAddress);
    if (!f.getReceiveSocket().isClosed()) f.getReceiveSocket().close();
    System.out.println(clientAddress
            + "-->"
            + params.getRemoteHost() + ":" + params.getRemotePort()
            + " is Closed");
    System.out.println(forwarders.size() + " tunnels still alive");
  }


  public synchronized void connectionBroken() {
    try {
      fwdSocket.close();
    } catch (Exception e) {
    }
    try {
      receiverSocket.close();
    } catch (Exception e) {
    }

    if (active) {
      String dateStr = sdf.format(new Date());
      if (params.isPrint())
        System.out.println(dateStr + ": UDP Forwarding " + toStr(receiverSocket) + " <--> " + toStr(fwdSocket) + " stopped.");
      active = false;
    }
  }
}
