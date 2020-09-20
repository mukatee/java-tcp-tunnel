package net.kanstren.tcptunnel.capture.gzip;

import net.kanstren.tcptunnel.Main;
import net.kanstren.tcptunnel.Params;
import net.kanstren.tcptunnel.PortManager;
import net.kanstren.tcptunnel.capture.tcp.ConsoleLoggingTests;
import net.kanstren.tcptunnel.capture.tcp.TCPMsgSender;
import net.kanstren.tcptunnel.capture.tcp.TCPTestServer1;
import net.kanstren.tcptunnel.observers.GZipStringConsoleLogger;
import org.testng.annotations.Test;
import osmo.common.TestUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.testng.Assert.assertEquals;

/**
 * @author Teemu Kanstren
 */
public class GZipTests {

  @Test
  public void networkGzip() throws Exception {
    int proxyPort = PortManager.port();
    //configure the tunnel to accept connections on port 5598 and forward them to localhost:5599
    Params params = new Params(proxyPort, "httpbin.org", 80);
    //this has to be set before enabling logger or it will not pass through
    params.setGzip(true);
    params.enableByteFileLogger("down.bytes", "up.bytes");
    TestUtils.startOutputCapture();
    params.enableStringConsoleLogger();
    //this is how we actually start the tunnel
    Main main = new Main(params);
    main.start();
    Thread.sleep(50);
    //send a test request to get some data in the tunnel
    byte[] response = TCPMsgSender.sendGZGet("http://localhost:"+proxyPort+"/gzip");
    //check we got the a GZIP encoded response
    byte[] gzipMagic = new byte[]{0x1f, (byte)0x8b, 0x08};
    for (int i = 0 ; i < gzipMagic.length ; i++) {
      assertEquals(gzipMagic[i], response[i], "GZIP header magic byte "+i);
    }

    //short sleep in hopes the tunnel is closed
    Thread.sleep(100);
    TestUtils.endOutputCapture();
    String actual = TestUtils.getOutput();
    actual = ConsoleLoggingTests.stripResponseForTest(actual, true);
    actual = stripResponseForTest(actual);
    String expected = TestUtils.getResource(GZipTests.class, "expected_console1.txt")+"\n";
    //note, this test can be a little flaky since sometimes the server chunks the response.
    //the observer should still work but it will add an extra "down" to the print, since the response comes downstream in two chunks
    //maybe fix it someday but it is not a real issue..
    assertEquals(actual, expected);
  }

  @Test(enabled = false)
  //"This was chunked, not supporting chunked atm"
  public void restGzip() throws Exception{
    InputStream stream = GZipTests.class.getResourceAsStream("gzip_test_down1.bytes");
    byte[] bytes = readStream(stream);
    //StringConsoleLogger logger = new StringConsoleLogger(System.out, "test", "UTF8", false, true);
    byte[] unzipped = GZipStringConsoleLogger.unzip(bytes);
    String uzStr = new String(unzipped, "UTF8");
    uzStr = stripResponseForTest(uzStr);
    assertEquals(uzStr, "");
  }

  @Test
  public void bareGZReq() throws Exception {
    InputStream stream = GZipTests.class.getResourceAsStream("bare_gz.bytes");
    byte[] bytes = readStream(stream);
    TestUtils.startOutputCapture();
    GZipStringConsoleLogger logger = new GZipStringConsoleLogger(System.out, "test", "UTF8", false);
    logger.observe(bytes, 0, bytes.length);
    //short sleep in hopes the tunnel is closed
    Thread.sleep(100);
    TestUtils.endOutputCapture();
    String actual = TestUtils.getOutput();
    actual = stripResponseForTest(actual);
    String expected = TestUtils.getResource(GZipTests.class, "expected_console2.txt")+"\n";
    assertEquals(actual, expected);
  }

  @Test
  public void bareGZ() throws Exception {
    InputStream stream = GZipTests.class.getResourceAsStream("bare_gz.bytes");
    byte[] bytes = readStream(stream);
    byte[] unzipped = GZipStringConsoleLogger.unzip(bytes);
    String uzStr = new String(unzipped, "UTF8");
    uzStr = stripResponseForTest(uzStr);
    String expected = TestUtils.getResource(GZipTests.class, "expected_response1.txt")+"\n";
    assertEquals(uzStr, expected);
  }

  @Test
  public void curlGzipHttpCurl() throws Exception {
    InputStream stream = GZipTests.class.getResourceAsStream("gzip_nanohttpd_curl.bytes");
    byte[] bytes = readStream(stream);
    byte[] unzipped = GZipStringConsoleLogger.unzip(bytes);
    String uzStr = new String(unzipped, "UTF8");
    uzStr = stripResponseForTest(uzStr);
    assertEquals(uzStr, "hello from test srv\n");
  }

  @Test
  public void curlGzipHttp() throws Exception {
    InputStream stream = GZipTests.class.getResourceAsStream("gzip_curl.bytes");
    byte[] bytes = readStream(stream);
    byte[] unzipped = GZipStringConsoleLogger.unzip(bytes);
    String uzStr = new String(unzipped, "UTF8");
    uzStr = stripResponseForTest(uzStr);
    String expected = TestUtils.getResource(GZipTests.class, "expected_response2.txt")+"\n";
    assertEquals(uzStr, expected);
  }

  @Test
  public void reuseBuffer() throws Exception {
    //test for reused buffer where anot GZIP magic appears after actual data, as from previous request
    //such data should be ignored, since parse index should not reach that far
    InputStream stream = GZipTests.class.getResourceAsStream("bare_gz.bytes");
    byte[] bytes = readStream(stream);
    byte[] doubleBytes = new byte[bytes.length*2];
    System.arraycopy(bytes, 0, doubleBytes, 0, bytes.length);
    System.arraycopy(bytes, 0, doubleBytes, bytes.length, bytes.length);
    GZipStringConsoleLogger logger = new GZipStringConsoleLogger(System.out, "test", "UTF8", false);
    //this should not throw, although the magic bytes appear also later in the buffer but outside given range
    //note that it skips first byte, so the first magic number is not found either
    logger.observe(doubleBytes, 1, bytes.length-10);
    //just to be sure, check that the latter part still works
    logger.observe(doubleBytes, bytes.length, bytes.length);
  }

  @Test
  public void curlGzipBody() throws Exception {
    InputStream stream = GZipTests.class.getResourceAsStream("gzip_curl.bytes");
    byte[] bytes = readStream(stream);
    byte[] unzipped = GZipStringConsoleLogger.unzip(bytes);
    String uzStr = new String(unzipped, "UTF8");
    uzStr = stripResponseForTest(uzStr);
    String expected = TestUtils.getResource(GZipTests.class, "expected_response2.txt")+"\n";
    assertEquals(uzStr, expected);
  }

  @Test
  public void rawGzip() throws Exception{
    InputStream stream = GZipTests.class.getResourceAsStream("gzip_only.bytes");
    byte[] bytes = readStream(stream);
    byte[] unzipped = GZipStringConsoleLogger.unzip(bytes);
    String uzStr = new String(unzipped, "UTF8");
    uzStr = stripResponseForTest(uzStr);
    String expected = TestUtils.getResource(GZipTests.class, "expected_response2.txt")+"\n";
    assertEquals(uzStr, expected);
  }

  private byte[] readStream(InputStream is) throws IOException {
    try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
      int nRead = 0;
      byte[] data = new byte[16384];

      while ((nRead = is.read(data, 0, data.length)) != -1) {
        buffer.write(data, 0, nRead);
      }

      return buffer.toByteArray();
    }
  }

  //http://stackoverflow.com/questions/8624195/how-to-remove-first-line-from-a-string-containing-xml
  public static String stripResponseForTest(String from) {
    String s = TestUtils.unifyLineSeparators(from, "\n");
    String[] lines = s.split("\n");
    StringBuilder sb = new StringBuilder(from.length());
    for (String line : lines) {
      //remove lines that change by request
      if (line.contains("Content-Length:")) continue;
      if (line.contains("\"origin\":")) continue;
      if (line.contains("\"X-Amzn-Trace-Id\":")) continue;
      sb.append(line.trim());
      sb.append("\n");
    }
    return sb.toString();
  }
}
