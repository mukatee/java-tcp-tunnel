package net.kanstren.tcptunnel;

import net.kanstren.tcptunnel.observers.ByteConsoleLogger;
import net.kanstren.tcptunnel.observers.ByteFileLogger;
import net.kanstren.tcptunnel.observers.StringConsoleLogger;
import net.kanstren.tcptunnel.observers.StringFileLogger;
import net.kanstren.tcptunnel.observers.TCPObserver;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import osmo.common.TestUtils;

import java.util.List;

import static org.testng.Assert.*;

/**
 * @author Teemu Kanstren.
 */
public class ParserTests {
  @Test
  public void helpText() {
    String help = ArgumentParser.help();
    help = TestUtils.unifyLineSeparators(help, "\n");
    String expected = TestUtils.getResource(ParserTests.class, "expected_help.txt");
    expected = TestUtils.unifyLineSeparators(expected, "\n");
    assertEquals(help, expected, "Help text");
  }

  @DataProvider
  public Object[][] invalidDataProvider() {
    return new Object[][] {
            {new String[] {}, "", "No args should print help and not give any errors."},
            {new String[] {"localhost", "1911"}, "Too few arguments. Need 3, got 2: [localhost, 1911].", "Error for missing arguments."},
            {new String[] {"localhost"}, "Too few arguments. Need 3, got 1: [localhost].", "Error for missing arguments."},
            {new String[] {"2222", "localhost", "1911", "extra"}, "Too many arguments. Need 3, got 4: [2222, localhost, 1911, extra].", "Error for missing arguments."},
            {new String[] {"wrong", "localhost", "1911"}, "Unable to parse source port from: 'wrong'.", "Error for non-numeric source port."},
            {new String[] {"1911", "localhost", "wrong"}, "Unable to parse remote port from: 'wrong'.", "Error for non-numeric destination port."},
            {new String[] {"--bob", "builder", "2222", "localhost", "1911"}, "Invalid option '--bob'.", "Error for invalid option name."},
            {new String[] {"2222", "localhost", "1911", "--buffersize"}, "No value given for option --buffersize. Please provide one.", "Error for last option with no value."},
            {new String[] {"--buffersize", "-1", "2222", "localhost", "1911"}, "Buffer size has to be > 0, was: -1.", "Error for negative buffer size."},
            {new String[] {"--buffersize", "0", "2222", "localhost", "1911"}, "Buffer size has to be > 0, was: 0.", "Error for negative buffer size."},
            {new String[] {"--encoding", "TEEMU", "2222", "localhost", "1911"}, "Unsupported encoding: 'TEEMU'.", "Error for unknown encoding."},
            {new String[] {"--logger", "invalid", "2222", "localhost", "1911"}, "Unknown logger type: 'invalid'.", "Error for invalid logger value."},
            {new String[] {"0", "localhost", "0"}, "Port numbers have to be in range 1-65535, source port was: 0.\nPort numbers have to be in range 1-65535, remote port was: 0.", "Error for out of bounds port numbers."},
    };
  }

  @DataProvider
  public Object[][] validDataProvider() {
    return new Object[][] {
            {new String[] {"2222", "localhost", "1911"}, 2222, "localhost", 1911,
                    Params.DEFAULT_BUFFER_SIZE, Params.DEFAULT_DOWN_PATH, Params.DEFAULT_UP_PATH, Params.DEFAULT_ENCONDING,
                    false, StringConsoleLogger.class, null},
            {new String[] {"2222", "localhost", "1911", "--buffersize", "1"}, 2222, "localhost", 1911,
                    1, Params.DEFAULT_DOWN_PATH, Params.DEFAULT_UP_PATH, Params.DEFAULT_ENCONDING,
                    false, StringConsoleLogger.class, null},
            {new String[] {"2222", "localhost", "1911", "--encoding", "UTF16"}, 2222, "localhost", 1911,
                    Params.DEFAULT_BUFFER_SIZE, Params.DEFAULT_DOWN_PATH, Params.DEFAULT_UP_PATH, "UTF16",
                    false, StringConsoleLogger.class, null},
            {new String[] {"2222", "localhost", "1911", "--down", "hello.txt"}, 2222, "localhost", 1911,
                    Params.DEFAULT_BUFFER_SIZE, "hello.txt", Params.DEFAULT_UP_PATH, Params.DEFAULT_ENCONDING,
                    false, StringConsoleLogger.class, null},
            {new String[] {"2222", "localhost", "1911", "--up", "hi.txt"}, 2222, "localhost", 1911,
                    Params.DEFAULT_BUFFER_SIZE, Params.DEFAULT_DOWN_PATH, "hi.txt", Params.DEFAULT_ENCONDING,
                    false, StringConsoleLogger.class, null},
            {new String[] {"5555", "localhost", "777", "--buffersize", "10000", "--down", "hello.txt", "--up", "hi.txt", "--encoding", "UTF16"},
                    5555, "localhost", 777, 10000, "hello.txt", "hi.txt", "UTF16",
                    false, StringConsoleLogger.class, null},
            {new String[] {"--buffersize", "10000", "5555", "localhost", "777", "--down", "hello.txt", "--up", "hi.txt", "--encoding", "UTF16"},
                    5555, "localhost", 777, 10000, "hello.txt", "hi.txt", "UTF16",
                    false, StringConsoleLogger.class, null},
            {new String[] {"--buffersize", "10000", "--down", "hello.txt", "--up", "hi.txt", "--encoding", "UTF16", "5555", "localhost", "777"},
                    5555, "localhost", 777, 10000, "hello.txt", "hi.txt", "UTF16",
                    false, StringConsoleLogger.class, null},
            {new String[] {"--hex", "--logger", "console-bytes", "--logger", "file-bytes", "--encoding", "UTF16", "5555", "localhost", "777"},
                    5555, "localhost", 777, 8192, Params.DEFAULT_DOWN_PATH, Params.DEFAULT_UP_PATH, "UTF16",
                    true, ByteConsoleLogger.class, null},
            {new String[] {"--hex", "--logger", "file-bytes", "--logger", "file-string", "--encoding", "UTF16", "5555", "localhost", "777"},
                    5555, "localhost", 777, 8192, Params.DEFAULT_DOWN_PATH, Params.DEFAULT_UP_PATH, "UTF16",
                    true, ByteFileLogger.class, null},
            {new String[] {"--logger", "file-string", "--encoding", "UTF16", "5555", "localhost", "777"},
                    5555, "localhost", 777, 8192, Params.DEFAULT_DOWN_PATH, Params.DEFAULT_UP_PATH, "UTF16",
                    false, StringFileLogger.class, null},
            {new String[] {"--logger", "console-bytes", "--encoding", "UTF16", "5555", "localhost", "777"},
                    5555, "localhost", 777, 8192, Params.DEFAULT_DOWN_PATH, Params.DEFAULT_UP_PATH, "UTF16",
                    false, ByteConsoleLogger.class, null},
            {new String[] {"--buffersize", "10000", "--down", "hello.txt", "5555", "--up", "hi.txt", "localhost", "--encoding", "UTF16", "777"},
                    5555, "localhost", 777, 10000, "hello.txt", "hi.txt", "UTF16",
                    false, StringConsoleLogger.class, null},
    };
  }

  @Test(dataProvider = "invalidDataProvider")
  public void invalidTests(String[] args, String expectedError, String failMsg) {
    Params params = ArgumentParser.parseArgs(args);
    String errors = params.getErrors();
    errors = TestUtils.unifyLineSeparators(errors, "\n");
    assertEquals(errors, expectedError, failMsg);
  }

  @Test(dataProvider = "validDataProvider")
  public void validTests(String[] args, int expectedSourcePort, String expectedRemoteHost, int expectedRemotePort,
          int expectedBufferSize, String expectedInPath, String expectedOutPath, String expectedEncoding, 
          boolean hex, Class observer1, Class observer2) {
    Params params = ArgumentParser.parseArgs(args);
    assertEquals(params.getErrors(), "", "Valid input should provide no errors.");
    assertEquals(params.getSourcePort(), expectedSourcePort, "Parsed source port.");
    assertEquals(params.getRemoteHost(), expectedRemoteHost, "Parsed remote host.");
    assertEquals(params.getRemotePort(), expectedRemotePort, "Parsed remote port.");
    assertEquals(params.getBufferSize(), expectedBufferSize, "Parsed buffer size.");
    assertEquals(params.getDownFilePath(), expectedInPath, "Parsed path for remote->local data stream.");
    assertEquals(params.getUpFilePath(), expectedOutPath, "Parsed path for local->remote data stream.");
    assertEquals(params.getEncoding(), expectedEncoding, "Parsed encoding.");
    List<TCPObserver> downers = params.createDownObservers();
    assertObserverType(downers, observer1, observer2, hex);
    List<TCPObserver> uppers = params.createUpObservers();
    assertObserverType(uppers, observer1, observer2, hex);
  }
  
  private void assertObserverType(List<TCPObserver> observers, Class expected1, Class expected2, boolean hex) {
    boolean found1 = false;
    boolean found2 = expected2 == null;
    for (TCPObserver observer : observers) {
      if (observer.getClass().equals(expected1)) found1 = true;
      if (observer.getClass().equals(expected2)) found2 = true;
      if (observer instanceof ByteConsoleLogger) {
        ByteConsoleLogger bcl = (ByteConsoleLogger) observer;
        assertEquals(bcl.isHex(), hex, "BCL hex value");
      }
    }
    assertTrue(found1, "Could not find expected observer type: "+expected1+" in "+observers);
    assertTrue(found2, "Could not find expected observer type: "+expected2+" in "+observers);
  }
}
