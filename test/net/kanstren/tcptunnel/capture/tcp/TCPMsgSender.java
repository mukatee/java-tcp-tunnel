package net.kanstren.tcptunnel.capture.tcp;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;

import static net.kanstren.tcptunnel.observers.StringConsoleLogger.ln;

/**
 * @author Teemu Kanstren.
 */
public class TCPMsgSender {

  public static byte[] readBinaryStream(InputStream is) throws IOException {
    try (java.io.ByteArrayOutputStream byteout = new java.io.ByteArrayOutputStream()) {

      int res = 0;
      byte[] buf = new byte[1024];
      while (res >= 0) {
        res = is.read(buf, 0, buf.length);
        if (res > 0) {
          byteout.write(buf, 0, res);
        }
      }
      byte[] data = byteout.toByteArray();
      return data;
    } catch (Exception e) {
      throw new IOException("Failed to read byte stream", e);
    }
  }

  public static byte[] sendGZGet(String to) throws Exception {
    HttpURLConnection conn = (HttpURLConnection) new URL(to).openConnection();
    conn.setRequestMethod("GET");

    conn.setRequestProperty("User-Agent", "tcptunnel-tester");
    conn.setRequestProperty("Accept-Encoding", "gzip");

    conn.setUseCaches(false);
    conn.setAllowUserInteraction(false);

    int responseCode = conn.getResponseCode();
    String responseMsg = conn.getResponseMessage();

    InputStream in = conn.getInputStream();
    byte[] bytes = readBinaryStream(in);
    conn.disconnect();
    //System.out.println(ln+"response:" + response.toString());
    return bytes;
  }

  public static String sendGet(String to) throws Exception {
    HttpURLConnection conn = (HttpURLConnection) new URL(to).openConnection();
    conn.setRequestMethod("GET");

    conn.setRequestProperty("User-Agent", "tcptunnel-tester");

    conn.setUseCaches(false);
    conn.setAllowUserInteraction(false);

    int responseCode = conn.getResponseCode();
    String responseMsg = conn.getResponseMessage();

    StringBuilder response = new StringBuilder();
    InputStream in = conn.getInputStream();
    BufferedReader bin = new BufferedReader(new InputStreamReader(in));
    String line = "";
    while ((line = bin.readLine()) != null) {
      response.append(line);
    }
    conn.disconnect();
    //System.out.println(ln+"response:" + response.toString());
    return response.toString();
  }

  public static String sendPost(String to, String msg) throws Exception {
    HttpURLConnection conn = (HttpURLConnection) new URL(to).openConnection();
//    conn.setRequestMethod("GET");
    conn.setRequestMethod("POST");

    conn.setRequestProperty("User-Agent", "tcptunnel-tester-----");
    if (false)
      conn.setRequestProperty("Accept-Encoding", "gzip");

    conn.setDoOutput(true);
    OutputStream out = conn.getOutputStream();
    DataOutputStream wr = new DataOutputStream(out);
    wr.writeBytes(msg);
    wr.flush();
    wr.close();

    int responseCode = conn.getResponseCode();

    StringBuilder response = new StringBuilder();
    InputStream in = conn.getInputStream();
    BufferedReader bin = new BufferedReader(new InputStreamReader(in));
    String line = "";
    while ((line = bin.readLine()) != null) {
      response.append(line);
    }
    conn.disconnect();
    //System.out.println(ln+"response:" + response.toString());
    return response.toString();
  }

  public static String send2(String host, int port, String msg) throws Exception {
    return new String(send2(host, port, msg.getBytes()));
  }

  public static byte[] send2(String host, int port, byte[] bytes) throws Exception {
    Socket socket = new Socket(host, port);
    InputStream is = socket.getInputStream();
    OutputStream os = socket.getOutputStream();
    os.write(bytes);
    byte[] buf = new byte[8092];
    int read = is.read(buf);
    byte[] result = new byte[read];
    System.arraycopy(buf, 0, result, 0, read);
    return result;
  }

  public static void main(String[] args) throws Exception {
    send2("localhost", 6666, "hello1");
//    send2("localhost", 6666, "hello2");
  }
}
