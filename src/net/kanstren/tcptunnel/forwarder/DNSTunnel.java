package net.kanstren.tcptunnel.forwarder;

import net.kanstren.tcptunnel.Params;
import net.kanstren.tcptunnel.Utils;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Handles some basic DNS query tunneling..
 *
 * Created by AlexZhuo on 2017/11/9.
 */
public class DNSTunnel extends Thread {
  private SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MMM.dd HH:mm:ss");
  /** Configuration parameters. */
  private final Params params;
  private boolean active = false;
  /** Socket to receive packets from source. */
  private DatagramSocket sourceSocket;

  public DNSTunnel(Params params, DatagramSocket sourceSocket) {
    this.params = params;
    this.sourceSocket = sourceSocket;
  }

  public void run() {
    String dateStr = sdf.format(new Date());
    byte[] buffer = new byte[65536];
    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
    while (true) {
      try {
        packet.setData(buffer);
        packet.setLength(buffer.length);
        sourceSocket.receive(packet);
        active = true;
        InetAddress sourceAddress = packet.getAddress();
        int srcPort = packet.getPort();
        InetAddress address = InetAddress.getByName(params.getRemoteHost());
        String strAddr = toStr(packet);
        if (params.isPrint()) {
          System.out.println(dateStr + ": DNS Forwarding " +packet.getLength()+ " bytes " + strAddr + " --> " + address.getHostAddress() + ":" + params.getRemotePort());
        }
        //send client request to server
        packet.setPort(params.getRemotePort());
        packet.setAddress(address);
        Thread tunnel = new DNSForwarder(packet, sourceSocket, sourceAddress, srcPort, params);
        tunnel.start();

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

  public String toStr(DNSForwarder f) {
    String host = f.getFwdAddr().getHostAddress();
    int port = f.getFwdPort();
    return host + ":" + port;
  }

  public void close() {
    connectionBroken();
  }

  public synchronized void connectionBroken() {
    try {
      sourceSocket.close();
    } catch (Exception e) {
    }

    if (active) {
      String dateStr = sdf.format(new Date());
      if (params.isPrint())
        System.out.println(dateStr + ": DNS Forwarding " + toStr(sourceSocket) + " <--> " + params.getRemoteHost() + ":" + params.getRemotePort() + " stopped.");
      active = false;
    }
  }
}
