package net.kanstren.tcptunnel;

import java.io.InputStream;
import java.util.Scanner;

/**
 * @author Teemu Kanstren.
 */
public class Utils {
  /** System specific line separator. */
  public static String ln = System.getProperty("line.separator");

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
}
