package net.kanstren.tcptunnel.observers;

import net.kanstren.tcptunnel.Params;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Observes a stream as a set of bytes.
 * Mirrors the up/downstream bytes to a second forwarding host.
 * The up/downstream configuration is simply defined by the configuration as to which stream this is attached to in the {@link net.kanstren.tcptunnel.Params} configuration.
 * This simply takes a stream of bytes and sends them to given address/host pair, discards whatever is received.
 * If you want to store/use what is returned, look into the run() method of the inner {@link Sucker} class.
 *
 * The point is to allow multiple point forwarding if you need to split traffic.
 * Really just two for now (at the time of writing this) but should work fine for more with minor mods.
 * May need a bit more testing in more complex network environments to make sure it works. But basic tests are OK so far.
 *
 * @author Teemu Kanstren.
 */
public class SocketForwardingObserver implements TCPObserver {
  /** Forward the observed bytes here. */
  private final String remoteHost;
  /** Port on remote host to connect to for forwarding. */
  private final int remotePort;
  /** Socket connection to the remote host. Redone if/when the connection drops.
   * Needs to be at class level as the observe() method gets called multiple times for big streams and we want to keep the mirror stream open as well in those cases. */
  private Socket socket = null;
  DatagramSocket udpSocket = null;

  /** The outputstream for the socket. Kept here since I am not sure how well the socket supports repeatedly calling getOutputStream() and tossing the reference. */
  private OutputStream os;
  /** The parameters this is set up with. Mainly to provide better logging. */
  private Params params;
  /** Actual stream source address, mostly for logging purposes. */
  private String sourceAddr;

  public SocketForwardingObserver(Params params, String remoteHost, int remotePort, String sourceAddr) {
    this.params = params;
    this.remoteHost = remoteHost;
    this.remotePort = remotePort;
    this.sourceAddr = sourceAddr;
  }

  /**
   * All the reader thread to call reset if the read function finishes or the socket has error.
   * Forces re-creation of mirror connection on next observation.
   */
  public synchronized void reset() {
    try {
      socket.close();
    } catch (Exception e) {
    }
    socket = null;
//    System.out.println("resetting mirror observer");
  }

  /**
   * This keeps getting over and over for the observed data.
   * So try to reconnect if the socket is closed or there is an error in reading/writing to it.
   * This should happen all the time for data streams that connect periodically to send some data, or generally stuff such as HTTP requests.
   *
   * @param buffer The byte array containing the bytes to observe.
   * @param start Starting index in the byte array to observe.
   * @param count The number of bytes to observe.
   * @throws IOException
   */
  @Override
  public synchronized void observe(byte[] buffer, int start, int count) throws IOException {
    if(params.isUdptun() || params.isDns()){
      if(udpSocket == null || udpSocket.isClosed()) udpSocket = new DatagramSocket();
      DatagramPacket packet = new DatagramPacket(buffer,start,count);
      packet.setAddress(InetAddress.getByName(remoteHost));
      packet.setPort(remotePort);
//      udpSocket.setSoTimeout(60000);
      udpSocket.send(packet);
      return;
    }

    Sucker sucker = null;
    if (socket == null) {
      System.out.println("Creating mirror stream to "+remoteHost+":"+remotePort+". Source = "+sourceAddr);
      socket = new Socket(remoteHost, remotePort);
      os = socket.getOutputStream();
      //create a separate thread to suck in all the input from the remote host receiving the data.
      //we can just discard the data if we like but the server may not like it if no-one reads its response
      InputStream is = socket.getInputStream();
      sucker = new Sucker(is, this);
      Thread t = new Thread(sucker);
      t.start();
    }
//    System.out.println("observed data:"+count);
    try {
      os.write(buffer, start, count);
      os.flush();
    } catch (IOException e) {
//      System.out.println("Stopped mirror stream due to exception in output. Waiting to create new. Error msg:"+e.getMessage());
      if (sucker != null) sucker.stop();
      //null the socket to force recreation of it and the reader thread when more data is observed next time
      reset();
    }
  }

  /**
   * Used for sucking in the response from server where the data is mirrored to.
   */
  private static final class Sucker implements Runnable {
    private final InputStream is;
    private final SocketForwardingObserver parent;
    private boolean stopped = false;

    public Sucker(InputStream is, SocketForwardingObserver parent) {
      this.is = is;
      this.parent = parent;
    }

    public void stop() {
      stopped = true;
    }

    public void run() {
      try {
        while (!stopped) {
          byte[] receiveBuffer = new byte[8192];
          int bytesRead = is.read(receiveBuffer);
          if (bytesRead == -1) break; // End of stream is reached --> exit
        }
      } catch (IOException e) {
//        System.out.println("Stopped mirror stream due to exception in input. Error msg:"+e.getMessage());
      }
//      System.out.println("Mirror stream has stopped due to socket close/error. Recreating when new data is observed.");
      parent.reset();
    }
  }

}
