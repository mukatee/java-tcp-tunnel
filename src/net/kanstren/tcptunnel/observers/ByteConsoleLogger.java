package net.kanstren.tcptunnel.observers;

import java.io.PrintStream;

/**
 * Observes a stream as a set of bytes.
 * Writes the bytes to console. 
 * Either as a list of integers (one per byte) or as a long string of hexadecimal values.
 * 
 * @author Teemu Kanstren.
 */
public class ByteConsoleLogger implements TCPObserver {
  /** Used to convert bytes to hex. */
  private final static char[] hexArray = "0123456789ABCDEF".toCharArray();
  /** If true, we log a hex stream. If false, a list of integers. */
  private final boolean hex;
  /** The stream to write to. Could be anything but generally here System.out. */
  private final PrintStream stream;
  /** To distinguish up/down stream log entries, we prefix each with own string. E.g., "up:" or "down:". */
  private final String prefix;
  /** System dependent line-feed / line-separator. */
  private static final String ln = System.getProperty("line.separator");

  /**
   * 
   * @param hex If true, print the observed stream as hex.
   * @param stream The stream where to print.
   * @param prefix Prefix every capture byte array with this when printing (to differentiate up/down).
   */
  public ByteConsoleLogger(boolean hex, PrintStream stream, String prefix) {
    this.hex = hex;
    this.stream = stream;
    this.prefix = prefix;
  }

  public boolean isHex() {
    return hex;
  }

  /**
   * Convert a given sub-array of the given byte-array to a hexadecimal string.
   * 
   * @param bytes The bytes to convert.
   * @param start Starting index in the byte array to convert.
   * @param count The number of bytes to convert.
   * @return A hexadecimal string representing the given parameters.
   */
  public String bytesToHex(byte[] bytes, int start, int count) {
    char[] hexed = new char[count * 2];
    int last = start + count-1;
    for (int j = start; j < last ; j++) {
      int v = bytes[j] & 0xFF;
      char c1 = hexArray[v >>> 4];
      hexed[j * 2] = c1;
      char c2 = hexArray[v & 0x0F];
      hexed[j * 2 + 1] = c2;
    }
    String result = prefix + ":" +ln + new String(hexed);
    return result;
  }

  /**
   * Convert a given sub-array of the given byte-array to a list of integers (-128 to +127).
   *
   * @param bytes The bytes to convert.
   * @param start Starting index in the byte array to convert.
   * @param count The number of bytes to convert.
   * @return A list of integers string representing the given parameters.
   */
  public String bytesToInt(byte[] bytes, int start, int count) {
//    Arrays.toString(bytes);
    int last = start+count-1;
    StringBuilder b = new StringBuilder();
    b.append(prefix);
    b.append(':');
    b.append(ln);
    b.append('[');
    for (int i = start ; i <= last ; i++) {
      b.append(bytes[i]);
      if (i == last)
        return b.append(']').toString();
      b.append(", ");
    }
    return prefix+":[]";
  }

  @Override
  public void observe(byte[] buffer, int start, int count) {
    if (hex) stream.println(bytesToHex(buffer, start, count));
    else stream.println(bytesToInt(buffer, start, count));
  }
}
