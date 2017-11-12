package net.kanstren.tcptunnel.forwarder;

import net.kanstren.tcptunnel.Params;
import net.kanstren.tcptunnel.Utils;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by AlexZhuo on 2017/11/9.
 */
public class DNSTunnel extends Thread{

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MMM.dd HH:mm:ss");
    /** Configuration parameters. */
    private final Params params;
    private boolean active = false;
    private DatagramSocket clientSocket;
    DatagramSocket serverSocket = null;
    public DNSTunnel(Params params, DatagramSocket clientSocket)
    {
        this.params = params;
        this.clientSocket = clientSocket;
    }

    public void run() {
        InetAddress address = null;
        try {
            serverSocket = new DatagramSocket();
            address = InetAddress.getByName(params.getRemoteHost());
        } catch (Exception e1) {
            e1.printStackTrace();
            System.out.println("Cannot connect to UDP Destination on " + params.getRemoteHost());
            return;
        }
        byte[] buffer = new byte[65536];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        String dateStr = sdf.format(new Date());
        while (true) {
            try {
                packet.setData(buffer);
                packet.setLength(buffer.length);
                clientSocket.receive(packet);
                active = true;
                InetAddress sourceAddress = packet.getAddress();
                int srcPort = packet.getPort();
                if (params.isPrint()) {
                    System.out.println("\n\n\nget new UDP request,length=="+packet.getLength());
                    String result1 = new String(packet.getData(), 0, packet.getLength(), "ASCII");
                    System.out.println(dateStr+": UDP Forwarding " + sourceAddress+":"+srcPort + " <--> " + address.getHostAddress()+":"+params.getRemotePort());
                    System.out.println("request content is ：："+result1);
                }
                //send client request to server
                packet.setPort(params.getRemotePort());
                packet.setAddress(address);
                //Symmetric mode
                Thread tunnel = new DNSForwarder(packet,clientSocket,sourceAddress,srcPort,params);
                tunnel.start();

            } catch (Throwable e) {
                e.printStackTrace();
                if (params.isPrint()) {
                    String remoteAddr = params.getRemoteHost() + ":" + params.getRemotePort();
                    String humanRemoteAddr = Utils.mapAddrToHumanReadable(remoteAddr);
                    remoteAddr = remoteAddr + " ("+humanRemoteAddr+")";
                    System.err.println(dateStr + ": Failed to connect to remote host (" + remoteAddr + ")");
                }
                connectionBroken();
            }
        }


}
    private String toStr(DatagramSocket socket) {
        String host = socket.getInetAddress().getHostAddress();
        int port = socket.getPort();
        return host + ":" + port;
    }
    public void close() {
        connectionBroken();
    }

    public synchronized void connectionBroken() {
        try {
            serverSocket.close();
        } catch (Exception e) {}
        try {
            clientSocket.close();
        } catch (Exception e) {}

        if (active) {
            String dateStr = sdf.format(new Date());
            if (params.isPrint()) System.out.println(dateStr+": DNS Forwarding " + toStr(clientSocket) + " <--> " + toStr(serverSocket) + " stopped.");
            active = false;
        }
    }
}
