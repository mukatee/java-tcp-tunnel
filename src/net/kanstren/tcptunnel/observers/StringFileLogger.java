package net.kanstren.tcptunnel.observers;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * Logs the observed data stream to a file as strings decoded with the given character encoding id.
 * Default is UTF8.
 *
 * @author Teemu Kanstren.
 */
public class StringFileLogger implements TCPObserver {
  /** For writing to the file. */
  private final BufferedWriter writer;

  /**
   * @param path Path to the file to write to. Postfix ".txt" is added to the path.
   * @throws IOException If there is a problem with accessing the file with given path.
   */
  public StringFileLogger(String path) throws IOException {
    writer = Files.newBufferedWriter(Paths.get(path+".txt"), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
  }

  @Override
  public void observe(byte[] buffer, int start, int count) throws IOException {
    String add = new String(buffer, start, count);
    writer.write(add);
    writer.flush();
  }
}
