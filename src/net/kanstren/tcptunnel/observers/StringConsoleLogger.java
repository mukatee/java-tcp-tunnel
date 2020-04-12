package net.kanstren.tcptunnel.observers;

import java.io.IOException;
import java.io.PrintStream;

/**
 * Logs the observed data stream to console as strings decoded with the given character encoding id.
 * Default is UTF8.
 * 
 * @author Teemu Kanstren.
 */
public class StringConsoleLogger implements TCPObserver {
  /** System specific line separator. */
  public static String ln = System.getProperty("line.separator");
  /** For writing the logged data to console. */
  private final PrintStream stream;
  /** Prefix for printing. Allows separating up/down stream data when printing to same console. */
  private final String prefix;
  /** Character encoding id to use for decoding. Default is UTF8. */
  private final String encoding;
  /** If enabled, adds a linefeed/newline every time something is printed. To keep timestamps messing with data printouts without final linefeed. */
  private final boolean addLF;

  /**
   * @param stream Write the collected trace (decoded strings) here.
   * @param prefix Prefix every captured data set (e.g., message) with this.
   * @param encoding The character encoding id to use for decoding the bytes to string.
   * @param addLF If true, add linefeed to end of each print.
   */
  public StringConsoleLogger(PrintStream stream, String prefix, String encoding, boolean addLF) {
    this.stream = stream;
    this.prefix = prefix;
    this.encoding = encoding;
    this.addLF = addLF;
  }

  @Override
  public void observe(byte[] buffer, int start, int count) throws IOException {
    String add = new String(buffer, start, count, encoding);
    stream.print(ln+prefix+":"+ln+add);
    if (addLF) {
      stream.print(ln);
    }
  }
}
