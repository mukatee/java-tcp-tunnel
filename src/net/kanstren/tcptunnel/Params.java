package net.kanstren.tcptunnel;

import net.kanstren.tcptunnel.observers.ByteConsoleLogger;
import net.kanstren.tcptunnel.observers.ByteFileLogger;
import net.kanstren.tcptunnel.observers.InMemoryLogger;
import net.kanstren.tcptunnel.observers.SocketForwardingObserver;
import net.kanstren.tcptunnel.observers.StringConsoleLogger;
import net.kanstren.tcptunnel.observers.StringFileLogger;
import net.kanstren.tcptunnel.observers.TCPObserver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * The tunnel configuration parameters. Either parsed from command line or configured programmatically.
 * 
 * @author Teemu Kanstren.
 */
public class Params {
  /** Default size for the buffer used to read/write sockets. */
  public static final int DEFAULT_BUFFER_SIZE = 8192;
  /** Default path to the downstream log file(s). Suffix is added per file type separately. */
  public static final String DEFAULT_DOWN_PATH = "tcp_down";
  /** Default path to the upstream log file(s). Suffix is added per file type separately. */
  public static final String DEFAULT_UP_PATH = "tcp_up";
  /** Default string encoding when decoding streams for string logging. */
  public static final String DEFAULT_ENCONDING = "UTF8";

  /** Port to bind on localhost to wait for initial tunnel connections. */
  private int sourcePort = -1;
  /** Remote host where to forward all traffic from source port. */
  private String remoteHost = null;
  /** Port on the remote host where all traffic from source port is forwarded. */
  private int remotePort = -1;
  /** Remote host where to mirror all upstream traffic. */
  private String mirrorUpHost = null;
  /** Port on the upstream mirror host where to forward to. */
  private int mirrorUpPort = -1;
  /** Remote host where to mirror all downstream traffic. */
  private String mirrorDownHost = null;
  /** Port on the downstream mirror host where to forward to. */
  private int mirrorDownPort = -1;
  /** Actual buffer size used to read/write sockets. */
  private int bufferSize = DEFAULT_BUFFER_SIZE;
  /** Path to the base filename where downstream logs are to be written. */
  private String downFilePath = DEFAULT_DOWN_PATH;
  /** Path to the base filename where upstream logs are to be written. */
  private String upFilePath = DEFAULT_UP_PATH;
  /** The string encoding used to decode socket data for string logging. Defaults to UTF8. */
  private String encoding = DEFAULT_ENCONDING;
  /** If true, basic information is printed to console, such as open/close of tunneling sessions. */
  private boolean print = true;
  /** Set of errors observed in parsing configuration parameters. Empty string if none. */
  private String errors = "";
  /** Enables mirroring of upstream data to another IP/port combination besides target. */
  private boolean mirrorUpEnabled = false;
  /** Enables mirroring of downstream data to another IP/port combination besides target. */
  private boolean mirrorDownEnabled = false;
  /** Enables DNS forwarding tunnel. */
  private boolean dns = false;
  /** Enables UDP forwarding tunnel. */
  private boolean udp = false;
  /** Observers (typically loggers) for downstream data. */
  private List<TCPObserver> observersDown = new ArrayList<>();
  /** Observers (typically loggers) for upstream data. */
  private List<TCPObserver> observersUp = new ArrayList<>();
  /** 
   * Downstream in-memory logging. 
   * If in-memory logging is enabled, we provide direct access to the logger as it is most likely used for programmatic proxying 
   * (i.e., want to request the captured data from a Java API). 
   */
  private InMemoryLogger downMemoryLogger = null;
  /** 
   * Upstream in-memory logging. 
   * If in-memory logging is enabled, we provide direct access to the logger as it is most likely used for programmatic proxying 
   * (i.e., want to request the captured data from a Java API). 
   */
  private InMemoryLogger upMemoryLogger = null;
  /** Set to false to stop this tunnel (if running..). */
  private boolean shouldRun = true;
  /** Set to true to postfix console prints with an extra linefeed. Can help log readability if server does not return a final LF. */
  private boolean addLF = false;

  /**
   * @param sourcePort Source port where to wait for tunnel initiating connections on localhost.
   * @param remoteHost Remote host where to connect to forward traffic to/from source port.
   * @param remotePort Port on the remote host to connect to.
   */
  public Params(int sourcePort, String remoteHost, int remotePort) {
    this.sourcePort = sourcePort;
    this.remoteHost = remoteHost;
    this.remotePort = remotePort;
  }

  public Params() {
  }

  public boolean isDNS() {
    return dns;
  }

  public void setDNS(boolean dns) {
    this.dns = dns;
  }


  public boolean isUDP() {
    return udp;
  }

  public void setUDP(boolean udp) {
    this.udp = udp;
  }

  public boolean isAddLF() {
    return addLF;
  }

  public void setAddLF(boolean addLF) {
    this.addLF = addLF;
  }

  /**
   * Used to stop running if command line parameters are invalid.
   * 
   * @return True if the tunnel should continue to run. If parameters are parsed OK, and stopping has not been requested.
   */
  public boolean shouldRun() {
    if (errors.length() > 0) return false;
    return shouldRun;
  }

  /**
   * Used to stop running if command line parameters are invalid. If running programmatically, this is not used.
   *
   * @param shouldRun If set to true, program will not continue after parsing command line parameters.
   */
  public void setShouldRun(boolean shouldRun) {
    this.shouldRun = shouldRun;
  }

  /**
   * @return The port to bind on localhost and wait for tunnel initialization connections.
   */
  public int getSourcePort() {
    return sourcePort;
  }

  /**
   * @param sourcePort The port to bind on localhost and wait for tunnel initialization connections.
   */
  public void setSourcePort(int sourcePort) {
    this.sourcePort = sourcePort;
  }

  /**
   * @return The remote host where the tunnel should be formed when someone connects to the source port.
   */
  public String getRemoteHost() {
    return remoteHost;
  }

  /**
   * @param remoteHost The remote host where the tunnel should be formed when someone connects to the source port.
   */
  public void setRemoteHost(String remoteHost) {
    this.remoteHost = remoteHost;
  }

  /**
   * @return The port on remote host to connect to.
   */
  public int getRemotePort() {
    return remotePort;
  }

  /**
   * @param remotePort The port on remote host to connect to.
   */
  public void setRemotePort(int remotePort) {
    this.remotePort = remotePort;
  }

  /**
   * @return Size of byte buffer used to store network data.
   */
  public int getBufferSize() {
    return bufferSize;
  }

  /**
   * @param bufferSize Size of byte buffer used to store network data.
   */
  public void setBufferSize(int bufferSize) {
    this.bufferSize = bufferSize;
  }

  /**
   * @return Base filename path for downstream log files. Suffixes added separately.
   */
  public String getDownFilePath() {
    return downFilePath;
  }

  /**
   * @param downFilePath Base filename path for downstream log files. Suffixes added separately.
   */
  public void setDownFilePath(String downFilePath) {
    this.downFilePath = downFilePath;
  }

  /**
   * @return Base filename path for upstream log files. Suffixes added separately.
   */
  public String getUpFilePath() {
    return upFilePath;
  }

  /**
   * @param upFilePath Base filename path for downstream log files. Suffixes added separately.
   */
  public void setUpFilePath(String upFilePath) {
    this.upFilePath = upFilePath;
  }

  /**
   * @return The character encoding id used to decode network data. Defaults to UTF8.
   */
  public String getEncoding() {
    return encoding;
  }

  /**
   * @param encoding The character encoding id used to decode network data. Defaults to UTF8.
   */
  public void setEncoding(String encoding) {
    this.encoding = encoding;
  }

  /**
   * @return The host to mirror upstream traffic to.
   */
  public String getMirrorUpHost() {
    return mirrorUpHost;
  }

  /**
   * @param mirrorUpHost The host to mirror upstream traffic to.
   */
  public void setMirrorUpHost(String mirrorUpHost) {
    this.mirrorUpHost = mirrorUpHost;
  }

  /**
   * @return The port on remote host to mirror upstream traffic to.
   */
  public int getMirrorUpPort() {
    return mirrorUpPort;
  }

  /**
   * @param mirrorUpPort The port on remote host to mirror upstream traffic to.
   */
  public void setMirrorUpPort(int mirrorUpPort) {
    this.mirrorUpPort = mirrorUpPort;
  }

  /**
   * @return The host to mirror downstream traffic to.
   */
  public String getMirrorDownHost() {
    return mirrorDownHost;
  }

  /**
   * @param mirrorDownHost The host to mirror downstream traffic to.
   */
  public void setMirrorDownHost(String mirrorDownHost) {
    this.mirrorDownHost = mirrorDownHost;
  }

  /**
   * @return The port on remote host to mirror downstream traffic to.
   */
  public int getMirrorDownPort() {
    return mirrorDownPort;
  }

  /**
   * @param mirrorDownPort The port on remote host to mirror downstream traffic to.
   */
  public void setMirrorDownPort(int mirrorDownPort) {
    this.mirrorDownPort = mirrorDownPort;
  }

  /**
   * @return Whether to print some messages to console. Generally used for command-line interface.
   */
  public boolean isPrint() {
    return print;
  }

  /**
   * @param print Whether to print some messages to console. Generally used for command-line interface.
   */
  public void setPrint(boolean print) {
    this.print = print;
  }

  /**
   * @return Errors when parsing parameters/options.
   */
  public String getErrors() {
    return errors;
  }

  /**
   * @param errors Errors when parsing parameters/options.
   */
  public void setErrors(String errors) {
    this.errors = errors;
  }

  /**
   * Turn on logging to a file using decoded strings (from raw socket data using configured encoding).
   * 
   * @param downPath Path to the basename for storing downstream data. Suffix separately added based on file type.
   * @param upPath Path to the basename for storing upstream data. Suffix separately added based on file type.
   * @throws IOException
   */
  public void enableStringFileLogger(String downPath, String upPath) throws IOException {
    observersDown.add(new StringFileLogger(downPath));
    observersUp.add(new StringFileLogger(upPath));
  }

  /**
   * Turn on logging raw socket data to a file in binary format.
   *
   * @param downPath Path to the basename for storing downstream data. Suffix separately added based on file type.
   * @param upPath Path to the basename for storing upstream data. Suffix separately added based on file type.
   * @throws IOException
   */
  public void enableByteFileLogger(String downPath, String upPath) throws IOException {
    observersDown.add(new ByteFileLogger(downPath));
    observersUp.add(new ByteFileLogger(upPath));
  }

  /**
   * Enable logging decoded strings to console.
   * Decoding based on defined encoding setting.
   */
  public void enableStringConsoleLogger() {
    observersDown.add(new StringConsoleLogger(System.out, "down", encoding, addLF));
    observersUp.add(new StringConsoleLogger(System.out, "up", encoding, addLF));
  }

  /**
   * Enable logging by mirroring the uplink data to another IP address.
   */
  public void enableMirrorUpStreamLogger(String remoteHost, int remotePort) {
    mirrorUpEnabled = true;
    mirrorUpHost = remoteHost;
    mirrorUpPort = remotePort;
//    observersUp.add(new SocketForwardingObserver(remoteHost, remotePort));
  }

  /**
   * Enable logging by mirroring the downlink data to another IP address.
   */
  public void enableMirrorDownStreamLogger(String remoteHost, int remotePort) {
    mirrorDownEnabled = true;
    mirrorDownHost = remoteHost;
    mirrorDownPort = remotePort;
//    observersDown.add(new SocketForwardingObserver(remoteHost, remotePort));
  }

  /**
   * Enable logging of byte values to console.
   * 
   * @param hex If true, strings of hexadecimal values are printed. Otherwise integer lists.
   */
  public void enableByteConsoleLogger(boolean hex) {
    observersDown.add(new ByteConsoleLogger(hex, System.out, "down"));
    observersUp.add(new ByteConsoleLogger(hex, System.out, "up"));
  }

  /**
   * Enable collecting raw byte data to memory.
   * 
   * @param initialCapacity Initial capacity of byte buffer to hold the data. Will grow at rate of double when filled (see ByteBuffer docs).
   */
  public void enableInMemoryLogging(int initialCapacity) {
    downMemoryLogger = new InMemoryLogger(initialCapacity);
    observersDown.add(downMemoryLogger);
    upMemoryLogger = new InMemoryLogger(initialCapacity);
    observersUp.add(upMemoryLogger);
  }

  /**
   * @param sourceAddr Stream actual source address, for logging purposes mainly.
   *
   * @return The list of configured downstream data observers (loggers).
   */
  public List<TCPObserver> createDownObservers(String sourceAddr) {
    List<TCPObserver> result = new ArrayList<>();
    result.addAll(observersDown);
    if (mirrorDownEnabled) result.add(new SocketForwardingObserver(this, mirrorDownHost, mirrorDownPort, sourceAddr));
    return result;
  }

  /**
   * @param sourceAddr Stream actual source address, for logging purposes mainly.
   *
   * @return The list of configured upstream data observers (loggers).
   */
  public List<TCPObserver> createUpObservers(String sourceAddr) {
    List<TCPObserver> result = new ArrayList<>();
    result.addAll(observersUp);
    if (mirrorUpEnabled) result.add(new SocketForwardingObserver(this, mirrorUpHost, mirrorUpPort, sourceAddr));
    return result;
  }

  /**
   * @return The configured logger used to store in-memory data for downstream (if configured).
   */
  public InMemoryLogger getDownMemoryLogger() {
    return downMemoryLogger;
  }

  /**
   * @return The configured logger used to store in-memory data for upstream (if configured).
   */
  public InMemoryLogger getUpMemoryLogger() {
    return upMemoryLogger;
  }

  @Override
  public String toString() {
    return "Params{" +
            "encoding='" + encoding + '\'' +
            ", sourcePort=" + sourcePort +
            ", remoteHost='" + remoteHost + '\'' +
            ", remotePort=" + remotePort +
            ", bufferSize=" + bufferSize +
            ", inFilePath='" + downFilePath + '\'' +
            ", outFilePath='" + upFilePath + '\'' +
            '}';
  }
}
