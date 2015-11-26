package net.kanstren.tcptunnel.observers;

import java.io.IOException;

/**
 * Top level interface for all components that observe a TCP stream.
 * Called by the threads to log data when something is observed on the TCP stream.
 * 
 * @author Teemu Kanstren.
 */
public interface TCPObserver {
  /**
   * Called on observers when a set of bytes has been captured on an observed steam.
   * Typically the observer logs the data in different format somewhere (console, file, ...).
   *
   * @param buffer The byte array containing the bytes to observe.
   * @param start Starting index in the byte array to observe.
   * @param count The number of bytes to observe.
   */
  void observe(byte[] buffer, int start, int count) throws IOException;
}
