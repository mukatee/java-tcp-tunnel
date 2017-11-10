package net.kanstren.tcptunnel.forwarder;

import net.kanstren.tcptunnel.Params;
import net.kanstren.tcptunnel.observers.TCPObserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Created by Administrator on 2017/11/9.
 */
public class DNSForwarder extends Thread{
    private InetAddress clientAddr;
    private int clientPort;
    private DatagramSocket serverSocket;
    private DatagramSocket clientSocket;
    private DatagramPacket sendData;
    Params params;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MMM.dd HH:mm:ss");
    private List<TCPObserver> upObservers;
    private List<TCPObserver> downObservers;
    /** The observers to pass all data through. Logging the data etc. */


    /**
     * Construct for Symmetric UDP forwarder
     * @param packet
     * @param clientSocket
     * @param clientAddr
     * @param clientPort
     * @param params
     * @param tunnels
     */
    public DNSForwarder(DatagramPacket packet, DatagramSocket clientSocket, InetAddress clientAddr, int clientPort, Params params){
        try {
            this.clientAddr = InetAddress.getByAddress(clientAddr.getAddress());
            this.clientPort = clientPort;
            this.clientSocket = clientSocket;
            this.params = params;
            byte[] sendmsg = Arrays.copyOf(packet.getData(),packet.getLength());
            sendData = new DatagramPacket(sendmsg,packet.getLength(),InetAddress.getByName(params.getRemoteHost()),params.getRemotePort());
            this.upObservers = params.createUpObservers(clientAddr.getHostAddress()+":"+clientPort);
            this.downObservers = params.createDownObservers(params.getRemoteHost());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        //receiving the data from remote server
        DatagramPacket response = new DatagramPacket(new byte[12800], 12800);
        try {
            serverSocket = new DatagramSocket();
            serverSocket.setSoTimeout(2000);
            serverSocket.send(sendData);
            if(serverSocket.isClosed())return;
            serverSocket.receive(response);
            String dateStr = sdf.format(new Date());
            String result = new String(response.getData(), 0, response.getLength(), "ASCII");
            if (params.isPrint()) {
                System.out.println(dateStr+": UDP Receiving "
                        + response.getAddress().getHostAddress()+":"+response.getPort() + " <--> "
                        + clientAddr+":"+clientPort);
            }
            System.out.println("remote server response length="+response.getLength()+"\n"+result);
            System.out.println("\n\n");
            //send the response from remote server to client
            response.setAddress(clientAddr);
            response.setPort(clientPort);
            clientSocket.send(response);
            for (TCPObserver observer : upObservers) {
                observer.observe(sendData.getData(), 0, sendData.getLength());
            }
            for (TCPObserver observer : downObservers) {
                observer.observe(response.getData(), 0, response.getLength());
            }
            close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void close(){
        if(serverSocket.isClosed())return;
        serverSocket.close();
    }
}
