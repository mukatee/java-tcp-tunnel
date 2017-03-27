package net.kanstren.tcptunnel;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Teemu Kanstren.
 */
public class PortManager {
  private static AtomicInteger port = new AtomicInteger(5555);

  public static int port() {
    return port.getAndIncrement();
  }
}
