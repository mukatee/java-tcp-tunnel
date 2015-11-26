package net.kanstren.tcptunnel.observers;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

/**
 * Logs the observed stream as a byte array in memory.
 * This can then be queried as a string or raw bytes.
 * The stored byte array can also be reset to allow, for example, separating request/response tracing per message.
 * 
 * @author Teemu Kanstren.
 */
public class InMemoryLogger implements TCPObserver {
  /** Initial capasity of the byte array used to store the observed data in memory. This is expanded automatically as needed by the JDK. */
  private final int initialCapacity;
  /** This is where the data is stored in memory. */
  private ByteArrayOutputStream bout;

  /**
   * @param initialCapacity The initial size of the byte array holding the observed data. Expanded as needed.
   */
  public InMemoryLogger(int initialCapacity) {
    this.initialCapacity = initialCapacity;
    reset();
  }

  @Override
  public void observe(byte[] buffer, int start, int count) {
    bout.write(buffer, start, count);
  }

  /**
   * @return The set of captured raw bytes (since last reset).
   */
  public byte[] getBytes() {
    return bout.toByteArray();
  }

  /**
   * @param encoding String encoding id used to decode the bytes as string (e.g., UTF8).
   * @return A string decoded from captured bytes using given encoding id.
   * @throws UnsupportedEncodingException If the given encoding is not supported.
   */
  public String getString(String encoding) throws UnsupportedEncodingException {
    return new String(getBytes(), encoding);
  }

  /**
   * Resets the data stream. Useful to separate traces per message, or just to avoid eating all memory over long term.
   */
  public void reset() {
    bout = new ByteArrayOutputStream(initialCapacity);
  }
}
