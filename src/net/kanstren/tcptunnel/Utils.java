package net.kanstren.tcptunnel;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
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
}
