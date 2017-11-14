package net.kanstren.tcptunnel.forwarder;

import net.kanstren.tcptunnel.Params;
import net.kanstren.tcptunnel.observers.TCPObserver;

import java.io.IOException;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created by AlexZhuo on 2017/11/12.
 */
public class UDPForwarder extends Thread{
    private InetAddress clientAddr;
    private int clientPort;
    private DatagramSocket serverSocket;
    private DatagramSocket clientSocket;
    private DatagramPacket sendData;
    Params params;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy.MMM.dd HH:mm:ss");
    private List<TCPObserver> downObservers;
    UDPTunnel parent;
    /** The observers to pass all data through. Logging the data etc. */


    /**
     * Construct for Symmetric UDP forwarder
     * @param clientSocket
     * @param clientAddr
     * @param clientPort
     * @param params
     */
    public UDPForwarder(UDPTunnel parent,DatagramSocket clientSocket, InetAddress clientAddr, int clientPort, Params params){
        try {
            this.clientAddr = InetAddress.getByAddress(clientAddr.getAddress());
            this.clientPort = clientPort;
            this.clientSocket = clientSocket;
            this.params = params;
            this.downObservers = params.createDownObservers(params.getRemoteHost());
            serverSocket = new DatagramSocket();
            serverSocket.setSoTimeout(30000);//30s UDP tunnel TimeOut
            this.parent = parent;
        }  catch (SocketException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public DatagramSocket getServerSocket(){
        return this.serverSocket;
    }
    public InetAddress getClientAddr(){
        return clientAddr;
    }
    public int getClientPort(){
        return clientPort;
    }

    @Override
    public void run() {
        //receiving the data from remote server
        DatagramPacket response = new DatagramPacket(new byte[12800], 12800);
        try {
            while (true) {
                if (serverSocket.isClosed()) return;
                serverSocket.receive(response);
                if (params.isPrint()) {
                    String dateStr = sdf.format(new Date());
                    System.out.println("\n"+dateStr + ": UDP Receiving "
                            + response.getAddress().getHostAddress() + ":" + response.getPort() + " <--> "
                            + clientAddr + ":" + clientPort);
                    String result = new String(response.getData(), 0, response.getLength(), "ASCII");
                    System.out.println("remote server response length=" + response.getLength() + "\n" + result);
                }

                //send the response from remote server to client
                response.setAddress(clientAddr);
                response.setPort(clientPort);
                clientSocket.send(response);
                for (TCPObserver observer : downObservers) {
                    observer.observe(response.getData(), 0, response.getLength());
                }
            }
        } catch (SocketTimeoutException e){
            if(params.isPrint())System.out.println("this thread is dead"+clientAddr+":"+clientPort);
            parent.close(this);
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
