Java TCP/UDP Tunnel
===================

A simple tool for capturing and inspecting data sent over a socket.
In a basic use case, you put this between two HTTP endpoints and watch what really is passed in between.
Or take a stream of protocol buffers data and split it to two destinations, while saving also to disk.
Or whatever else you like..

Can be used either from command line or as a Java library.

Why?
----

Pretty much every software I write/work with these days seems to be networked to some extent.
Trying to test networked software often is just more complicated than it needs to be.

And always it seems people end up using all kinds of libraries, frameworks, whatever.
Pretty much everything then seems to fail in mysterious ways under all those layers of libraries, factories, dependency injections, black boxes, or whatever.

Too many times I have tried to look for some solutions to get insight into this on the internet.
Too many times have I ended up on the website of Fiddler or some complex (for me) solution requiring installing too many dependencies (Mono etc).
Or with complex user interfaces when I just wanted to see what really went on (both ways client<->server) when making those network requests.

This is an attempt to make debugging and testing the networked stuff easier (for me).
I am a simple kind of a guy (who often likes the programmatic approach), so this is an attempt to make something simple enough for me.
So that's why.

Options
-------

For the options

```shell
java -jar tcptunnel-1.2.0.jar --help
```

Note that you can also just use this from your IDE by cloning the Github project and setting this up as a Java project.
For example, I use this from IntellIJ by setting the main class and using parameters such as "8079 localhost 8080".
I leave it up to you, dear reader, to figure out what that does based on notes on this page.

Example use from command line (from the scripts directory):
-----------------------------------------------------------

Forward local port 5566 to www.github.com port 80, default options will print decoded (UTF8) strings to console:

```shell
java -jar tcptunnel-1.2.0.jar 5566 www.github.com 80
```

To request the site now:

```shell
curl localhost:5566 --header 'Host: www.github.com'
```

Note the need to fix the "Host" header for a regular HTTP website request.
Else those pesky webservers will feel bad. For your own HTTP/REST/whatever server there is likely no such restriction.

Same as above but log to a file.

```shell
java -jar tcptunnel-1.2.0.jar 5566 www.github.com 80 --logger file-string
```

Log raw bytes to a file:

```shell
java -jar tcptunnel-1.2.0.jar 5566 www.github.com 80 --logger file-bytes
```

Log bytes as list of integers to console:

```shell
java -jar tcptunnel-1.2.0.jar 5566 www.github.com 80 --logger console-bytes
```

Log bytes as hexadecimal string to console:

```shell
java -jar tcptunnel-1.2.0.jar 5566 www.github.com 80 --logger console-bytes --hex
```

Mirror upstream data to another host/port in addition to forwarding through the tunnel:

```shell
java -jar tcptunnel-1.2.0.jar 6677 localhost 6667 --logger mirror-up --mirror-up-host localhost --mirror-up-port 6668
```

So the above listens for connections on port 6677, tunnels the bytes received on that port to "localhost:6667", and at the same time mirrors the same data also to "localhost:6668".
The data received from the other end ("localhost:6667") is pushed back to the client connection that connected to 6677.
The data received from the mirror server at "localhost:6668" is simply discarded.

Same but for downstream mirroring:

```shell
java -jar tcptunnel-1.2.0.jar 6677 localhost 6667 --logger mirror-down --mirror-down-host localhost --mirror-down-port 6668
```

In this downstream mirror example the difference is that the data sent to "localhost:6668" is the data received back from forwarding target at "localhost:6667".

UDP / DNS
---------

Forward local UDP port 53 to 8.8.8.8 port 53 , to resolve some DNS on local, use symmetric UDP protocal

```shell
java -jar tcptunnel-1.2.0.jar 53 8.8.8.8 53 --udp-dns
```


Test the DNS forwarder like this:

```shell
nslookup www.google.com 127.0.0.1
```

Notice: Using --udp-dns mode you need to make sure when you send a packet to server you will soon receive a packet from it, like the DNS request, or the socket will be out-of-time or closed.

Forward local UDP port 7000 to a remote IP with port 9999, to make a P2P tunnel on UDP protocal, like OpenVPN on UDP:

```shell
java -jar tcptunnel-1.2.0.jar 7000 xxx.xxx.xxx.xxx 9999 --udp-tun
```

Notice: Default UDP timeout is 30s.

Mirror datagram socket (UDP), support upstream and downstream

```shell
java -jar tcptunnel-1.2.0.jar 53 8.8.8.8 53 --udp-dns --logger mirror-up --mirror-up-host localhost --mirror-up-port 6668
```

HTTP/GZIP
---------
There is some support for trying to look for gzip compression inside HTTP requests and decompressing those for string logging.
I made this to help decode some HTTP REST framework requests, where the data is always GZIP'd and the log is less usefull without decompression.

This support does not work for all types of combinations. For example, chunked transfers may have some issues, although it should work also if the data is plain GZIP without headers (like a continuation chunk).
It just expects to see a single HTTP request, with the GZIP magic header starting the GZIP's content,
which then is expected to continue until the end of the response.

Example:
```shell
java -jar tcptunnel-1.2.0.jar 6688 httpbin.org 80 --gzip

curl -sH 'Accept-encoding: gzip' http://localhost:6688/gzip
```


Example use from Java (from the tests directory):
-------------------------------------------------

Example use in unit test code:

```java
public class CaptureTests {
  @Test
  public void sendRequestMITM() throws Exception {
    //create a test server to give us a page to request
    TestServer server = new TestServer(5599, "test1");
    server.start();
    //configure the tunnel to accept connections on port 5598 and forward them to localhost:5599
    Params params = new Params(5598, "localhost", 5599);
    //we want to use the captured data in testing, so enable logging the tunnel data in memory with buffer size 8092 bytes
    params.enableInMemoryLogging(8092);
    //this gives us access to the data passed from client connected to port 5598 -> localhost:5599 (client to server)
    InMemoryLogger upLogger = params.getUpMemoryLogger();
    //this gives us access to the data passed from localhost:5599 -> client connected to port 5598 (server to client)
    InMemoryLogger downLogger = params.getDownMemoryLogger();
    //this is how we actually start the tunnel
    Main main = new Main(params);
    main.start();
    //send a test request to get some data in the tunnel
    String response = MsgSender.send("http://localhost:5598", "hi there");
    //check we got the correct response from the server
    assertEquals(response, "test1", "Response content");
    //assert the HTTP protocol data passed through the tunnel both ways
    assertTcpStream(upLogger, "expected_up1.txt");
    assertTcpStream(downLogger, "expected_down1.txt");
    //the test server sometimes seems cranky if we stop it too soon
    Thread.sleep(1000);
    server.stop();
  }

  private void assertTcpStream(InMemoryLogger logger, String filename) throws Exception {
    //here we get the actual data that was passed through the tunnel in one direction (depending if we get passed the upstream memorylogger or downstream)
    String actual = logger.getString("UTF8");
    //the rest of this is just making sure the test should run the same over different platforms and with varying date-times in HTTP headers
    actual = TestUtils.unifyLineSeparators(actual, "\n");
    String expected = TestUtils.getResource(CaptureTests.class, filename);

    String[] replaced = TestUtils.replace("##", expected, actual);
    actual = replaced[0];
    expected = replaced[1];

    expected = TestUtils.unifyLineSeparators(expected, "\n");
    assertEquals(actual, expected, "Request full content");
  }
}
```

Installing
----------

Either use Maven dependencies or download the jar directly.

```xml
<dependency>
	<groupId>net.kanstren.tcptunnel</groupId>
	<artifactId>tcptunnel</artifactId>
	<version>1.0.0</version>
</dependency>
```

or direct [link](http://central.maven.org/maven2/net/kanstren/tcptunnel/tcptunnel/1.0.0/tcptunnel-1.0.0.jar)

Go Cousin
---------
There is also the Golang version I wrote: [Go-Forward](https://github.com/mukatee/go-forward).
It may not have 100% the same functionality at this time, but it compiles to a single binary if thats your preference.
And if you want to modify the tunnel but prefer Go, that could be an option..

License
-------

MIT License

