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
 */
public class UDPTunnel extends Thread{

    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MMM.dd HH:mm:ss");
    /** Configuration parameters. */
    private final Params params;
    private boolean active = false;
    private DatagramSocket clientSocket;
    DatagramSocket serverSocket = null;
    private List<TCPObserver> upObservers;

    private HashMap<String,UDPForwarder> forwarders = new HashMap<>();
    public UDPTunnel(Params params, DatagramSocket clientSocket)
    {
        this.params = params;
        this.clientSocket = clientSocket;
        this.upObservers = params.createUpObservers("Unknown");
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
                    System.out.println(dateStr+": UDP Forwarding " + sourceAddress+":"+srcPort + " <--> " + address.getHostAddress()+":"+params.getRemotePort());
                    String request = new String(packet.getData(), 0, packet.getLength(), "ASCII");
                    System.out.println("\n\n\n\nget new UDP request,length=="+packet.getLength());
                    System.out.println("request content is ：："+request);
                }
                //send client request to server
                packet.setPort(params.getRemotePort());
                packet.setAddress(address);
                //for multiple clients
                String key = sourceAddress.getHostAddress()+srcPort;
                UDPForwarder f = forwarders.get(key);
                if(f == null || f.getServerSocket().isClosed() || !f.isAlive()){
                    if(params.isPrint()) System.out.println("\nnew UDP session::"+key);
                    f = new UDPForwarder(this,clientSocket,sourceAddress,srcPort,params);
                    forwarders.put(key,f);
                    f.start();
                }else {
                    if(params.isPrint()) System.out.println("old client session");
                }
                f.getServerSocket().send(packet);
                for (TCPObserver observer : upObservers) {
                    observer.observe(packet.getData(), 0, packet.getLength());
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


    }
    private String toStr(DatagramSocket socket) {
        String host = socket.getInetAddress().getHostAddress();
        int port = socket.getPort();
        return host + ":" + port;
    }
    public void close() {
        connectionBroken();
    }
    public void close(UDPForwarder f){
        forwarders.remove(f.getClientAddr().getHostAddress()+f.getClientPort());
        if(!f.getServerSocket().isClosed())f.getServerSocket().close();
        System.out.println(f.getClientAddr().getHostAddress()+":"+f.getClientPort()
                +"<-->"
                +params.getRemoteHost()+":"+params.getRemotePort()
                +" is Closed");
        System.out.println(forwarders.size()+" tunnels still alive");
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
