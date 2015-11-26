package net.kanstren.tcptunnel.observers;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Observes a stream as a set of bytes.
 * Writes the bytes to a file.
 * The file is given a ".bytes" ending.
 * 
 * @author Teemu Kanstren.
 */
public class ByteFileLogger implements TCPObserver {
  /** For writing the bytes to a file. */
  private final OutputStream out;

  /**
   * @param path Path to the file to write.
   * @throws IOException If there is a problem writing the file.
   */
  public ByteFileLogger(String path) throws IOException {
    out = new BufferedOutputStream(new FileOutputStream(path+".bytes"));
  }

  @Override
  public void observe(byte[] buffer, int start, int count) throws IOException {
    out.write(buffer, start, count);
    out.flush();
  }
}
