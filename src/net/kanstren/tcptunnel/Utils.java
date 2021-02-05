package net.kanstren.tcptunnel;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Scanner;

/**
 * @author Teemu Kanstren.
 */
public class Utils {
  /** System specific line separator. */
  public static String ln = System.getProperty("line.separator");
  private static final Properties addrNameProps = new Properties();

  static {
    try {
      addrNameProps.load(new FileInputStream("tcptunnel_namemap.properties"));
    } catch (IOException e) {
      String msg = "Unable to load name mapping properties from tcptunnel_namemap.properties. No mappings used."+ln;
      msg += "You can write IP address mappings ";
      System.err.println(msg);
    }
  }

  public static String getResource(InputStream in) {
    StringBuilder text = new StringBuilder();
    try (Scanner scanner = new Scanner(in, "UTF-8")) {
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine();
        text.append(line);
        if (scanner.hasNextLine()) {
          text.append("\n");
        }
      }
    }

    return text.toString();
  }

  /**
   * Map an IP:port address to a human readable name as given in the tcptunnel_namemap.properties file.
   * For example, with a mapping of 8.8.8.8=Google DNS, the addr 8.8.8.8:2222 would map to "Google DNS".
   *
   * @param addr The address to map. Must be of format ip:port.
   * @return Human readable address as specified in properties file or "unknown" if no matching entry found.
   */
  public static String mapAddrToHumanReadable(String addr) {
    String ip = addr.substring(0, addr.indexOf(':'));
    String name = addrNameProps.getProperty(ip);
    if (name == null) {
      name = "unknown";
    }
    return name;
  }

  /**
   * Read lines in http request
   * Souce: trilead/ssh2 ClientServerHello.java
   */
  public static int readLineRN(InputStream is, byte[] buffer) throws IOException
  {
    int pos = 0;
    boolean need10 = false;
    int len = 0;
    while (true)
    {
      int c = is.read();
      if (c == -1)
        throw new IOException("Premature connection close");

      buffer[pos++] = (byte) c;

      if (c == 13)
      {
        need10 = true;
        continue;
      }

      if (c == 10)
        break;

      if (need10)
        throw new IOException("Malformed line sent by the server, the line does not end correctly.");

      len++;
      if (pos >= buffer.length)
        throw new IOException("The server sent a too long line: "+new String(buffer, StandardCharsets.ISO_8859_1));
    }

    return len;
  }
}
