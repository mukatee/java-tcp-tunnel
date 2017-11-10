package net.kanstren.tcptunnel.forwarder;

import net.kanstren.tcptunnel.Params;
import net.kanstren.tcptunnel.Utils;
import net.kanstren.tcptunnel.observers.TCPObserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by Administrator on 2017/11/9.
 */
public class UDPTunnel {

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MMM.dd HH:mm:ss");
    /** Configuration parameters. */
    private final Params params;
    private boolean active = false;
    private DatagramSocket clientSocket;
    DatagramSocket serverSocket = null;
    private InetAddress sourceAddress;
    private int srcPort;
    InetAddress remoteAddress = null;
    private List<TCPObserver> upObservers;
    private List<TCPObserver> downObservers;

    public UDPTunnel(Params params, DatagramSocket clientSocket)
    {
        this.params = params;
        this.clientSocket = clientSocket;
        this.upObservers = params.createUpObservers("Unknown");
        this.downObservers = params.createDownObservers(params.getRemoteHost()+":"+params.getRemotePort());
    }

    public void start() {
        try {
            serverSocket = new DatagramSocket();
            remoteAddress = InetAddress.getByName(params.getRemoteHost());
        } catch (Exception e1) {
            e1.printStackTrace();
            System.out.println("Cannot connect to UDP Destination on " + params.getRemoteHost());
            return;
        }
        byte[] buffer = new byte[65536];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        String dateStr = sdf.format(new Date());
        packet.setData(buffer);
        packet.setLength(buffer.length);
        //forward request to server
        new Thread(){
            @Override
            public void run() {
                try {
                    while (true) {
                        clientSocket.receive(packet);
                        System.out.println("new UDP request,length==" + packet.getLength());
                        active = true;
                        String result1 = new String(packet.getData(), 0, packet.getLength(), "ASCII");
                        sourceAddress = packet.getAddress();
                        srcPort = packet.getPort();
                        if (params.isPrint()) {
                            System.out.println("\n"+dateStr + ": UDP Forwarding " + sourceAddress + ":" + srcPort + " <--> " + remoteAddress.getHostAddress() + ":" + params.getRemotePort());
                        }
                        System.out.println("request content is ：：" + result1);
                        //下面是把该代理服务收到的客户端数据转发给远程服务器
                        packet.setPort(params.getRemotePort());
                        packet.setAddress(remoteAddress);
                        serverSocket.send(packet);
                        for (TCPObserver observer : upObservers) {
                            observer.observe(packet.getData(), 0, packet.getLength());
                        }
                    }
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
        }.start();
        DatagramPacket response = new DatagramPacket(new byte[12800], 12800);
        //forward response to client
        new Thread(){
            @Override
            public void run() {
                while (true) {
                    try {
                        serverSocket.receive(response);
                        String dateStr = sdf.format(new Date());
                        String result = new String(response.getData(), 0, response.getLength(), "ASCII");
                        if (params.isPrint()) {
                            System.out.println(dateStr + ": UDP Receiving "
                                    + response.getAddress().getHostAddress() + ":" + response.getPort() + " <--> "
                                    + sourceAddress + ":" + srcPort);
                        }
                        System.out.println("remote server response length=" + response.getLength() + "\n" + result);
                        System.out.println("\n\n");
                        //send the response from remote server to client
                        response.setAddress(sourceAddress);
                        response.setPort(srcPort);
                        clientSocket.send(response);
                        for (TCPObserver observer : downObservers) {
                            observer.observe(response.getData(), 0, response.getLength());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }.start();



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
            if (params.isPrint()) System.out.println(dateStr+": UDP Forwarding " + toStr(clientSocket) + " <--> " + toStr(serverSocket) + " stopped.");
            active = false;
        }
    }
}
