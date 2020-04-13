package net.kanstren.tcptunnel.observers;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Logs the observed data stream to console as strings decoded with the given character encoding id.
 * Default is UTF8.
 * 
 * @author Teemu Kanstren.
 */
public class GZipStringConsoleLogger implements TCPObserver {
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
  public GZipStringConsoleLogger(PrintStream stream, String prefix, String encoding, boolean addLF) {
    this.stream = stream;
    this.prefix = prefix;
    this.encoding = encoding;
    this.addLF = addLF;
  }

  @Override
  public void observe(byte[] buffer, int start, int count) throws IOException {
    byte[] content = buffer;
    byte[] gzipMagic = new byte[]{0x1f, (byte)0x8b, 0x08};
    int gzStart = indexOf(buffer, gzipMagic, start);
    if (gzStart > 0) {
      int to = start + count;
      byte[] gzip = Arrays.copyOfRange(buffer, gzStart, to);
      gzip = unzip(gzip);
      byte[] newContent = new byte[gzStart + gzip.length];
      System.arraycopy(content, 0, newContent, 0, gzStart);
      System.arraycopy(gzip, 0, newContent, gzStart, gzip.length);
      content = newContent;
      start = 0;
      count = content.length;
    }
    String add = new String(content, start, count, encoding);
    stream.print(ln+prefix+":"+ln+add);
    if (addLF) {
      stream.print(ln);
    }
  }

  public static int bodyIndex(byte[] from, int start) {
    return indexOf(from, "\r\n\r\n".getBytes(StandardCharsets.UTF_8), start)+4; //+4 is for the \r\n\r\n
  }

  public static byte[] extractHeader(byte[] from, int start, int count) {
    int bodyStart = bodyIndex(from, start);
    byte[] header = Arrays.copyOfRange(from, start, bodyStart);
    return header;
  }

  public static byte[] unzip(byte[] gzip) throws IOException {
    byte[] gzipMagic = new byte[]{0x1f, (byte)0x8b, 0x08};
    int gzStart = indexOf(gzip, gzipMagic, 0);
    if (gzStart > 0) {
      gzip = Arrays.copyOfRange(gzip, gzStart, gzip.length);
    }
    try (java.io.ByteArrayInputStream bytein = new java.io.ByteArrayInputStream(gzip);
         java.util.zip.GZIPInputStream gzin = new java.util.zip.GZIPInputStream(bytein);
         java.io.ByteArrayOutputStream byteout = new java.io.ByteArrayOutputStream()) {

      int res = 0;
      byte[] buf = new byte[1024];
      while (res >= 0) {
        res = gzin.read(buf, 0, buf.length);
        if (res > 0) {
          byteout.write(buf, 0, res);
        }
      }
      byte[] uncompressed = byteout.toByteArray();
      return uncompressed;
    } catch (Exception e) {
      throw new IOException("Failed to decompress GZIP stream", e);
    }

  }

  public static int indexOf(byte[] outerArray, byte[] smallerArray, int start) {
    for(int i = start ; i < outerArray.length - smallerArray.length+1 ; ++i) {
      boolean found = true;
      for(int j = 0 ; j < smallerArray.length ; ++j) {
        if (outerArray[i+j] != smallerArray[j]) {
          found = false;
          break;
        }
      }
      if (found) return i;
    }
    return -1;
  }
}
