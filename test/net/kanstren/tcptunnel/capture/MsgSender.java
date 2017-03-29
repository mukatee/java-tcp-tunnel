package net.kanstren.tcptunnel.capture;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;

import static net.kanstren.tcptunnel.observers.StringConsoleLogger.ln;

/**
 * @author Teemu Kanstren.
 */
public class MsgSender {
  public static String send(String to, String msg) throws Exception {
    HttpURLConnection conn = (HttpURLConnection) new URL(to).openConnection();
    conn.setRequestMethod("POST");

    conn.setRequestProperty("User-Agent", "tcptunnel-tester");

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
    System.out.println(ln+"response:" + response.toString());
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
    send("http://localhost:6677", "hello1");
    send("http://localhost:6677", "hello2");
  }
}
