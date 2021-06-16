package net.kanstren.tcptunnel;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses command line argument and options.
 * 
 * @author Teemu Kanstren.
 */
public class ArgumentParser {
  /** System specific line separator. */
  public static String ln = System.getProperty("line.separator");

  /**
   * Parses the given arguments and options.
   * 
   * @param args The arguments and options to parse. An option is expected to start with "--".
   * @return The configuration parsed from the arguments/options.
   */
  public static Params parseArgs(String[] args) {
    //in case the program is invoked with no arguments, we print the help
    if (args.length == 0) args = new String[] {"--help"};
    Params params = new Params();
    //we collect all errors and if any are found, report them all in the end
    String errors = "";
    //collect all options (starting with "--") here
    List<Option> options = new ArrayList<>();
    //collect all non-option parameters here
    List<String> paramStrs = new ArrayList<>();
    for (int i = 0 ; i < args.length ; i++) {
      String arg = args[i];
      if (arg.startsWith("--")) {
        if (arg.equals("--hex")) {
          //hex is a standalone option, so give it a value and move straight to next one
          options.add(new Option("--hex", "true"));
          continue;
        }
        if (arg.equals("--help")) {
          //help is a standalone option, so give it a value and move straight to next one
          options.add(new Option("--help", "true"));
          continue;
        }
        if (arg.equals("--trailing-lf")) {
          options.add(new Option("--trailing-lf", "true"));
          continue;
        }
        if (arg.equals("--udp-dns")) {
          options.add(new Option("--udp-dns", "true"));
          continue;
        }
        if (arg.equals("--udp-tun")) {
          options.add(new Option("--udp-tun", "true"));
          continue;
        }
        if (arg.equals("--gzip")) {
          options.add(new Option("--gzip", "true"));
          continue;
        }
        if (arg.equals("--https-tun")) {
          options.add(new Option("--https-tun", "true"));
          continue;
        }
        if (args.length <= i + 1) {
          //all options coming this far should have a value. otherwise it is an error.
          errors += "No value given for option " + arg + ". Please provide one." + ln;
          break;
        }
        Option option = new Option(arg, args[i + 1]);
        options.add(option);
        i++;
      } else {
        paramStrs.add(arg);
      }
    }
    //now that we collected all the options, parse the actual content from their values and check they are valid
    errors = parseOptions(options, params, errors);

    //if the options were ok, move to parsing and checking the non-option parameters
    if (params.shouldRun()) {
      boolean paramsOK = true;
      if (paramStrs.size() < 3) {
        errors += "Too few arguments. Need 3, got " + paramStrs.size() + ": " + paramStrs + "." + ln;
        paramsOK = false;
      }
      if (paramStrs.size() > 3) {
        errors += "Too many arguments. Need 3, got " + paramStrs.size() + ": " + paramStrs+ "." + ln;
        paramsOK = false;
      }
      if (paramsOK) errors = parseParams(paramStrs, params, errors);
    }
    
    //every error msg ends with a linefeed, so remove the last one
    if (errors.length() > 0) errors = errors.substring(0, errors.length() - ln.length());
    params.setErrors(errors);
    return params;
  }

  /**
   * Parse the actual content from the previously captured option definitions.
   * 
   * @param options The options from which to parse the content.
   * @param params This is where we store the parsed option content.
   * @param errors The errors previously observed.
   * @return Updated errors with the old one intact and new ones added.
   */
  private static String parseOptions(List<Option> options, Params params, String errors) {
    //we set this to true if the --hex option is given
    boolean hex = false;
    //this is to trace if any loggers are defined. if not, we add the default later.
    int loggers = 0;
    StringBuilder errorsBuilder = new StringBuilder(errors);
    for (Option option : options) {
      String name = option.name;
      switch (name) {
        case "--buffersize":
          //here we have the buffersize to use for capturing the observed stream and for writing to files etc.
          try {
            int bufferSize = Integer.parseInt(option.value);
            params.setBufferSize(bufferSize);
            if (bufferSize <= 0) errorsBuilder.append("Buffer size has to be > 0, was: ").append(bufferSize).append(".").append(ln);
          } catch (NumberFormatException e) {
            errorsBuilder.append("Invalid number for 'buffersize':").append(option.value).append(".").append(ln);
          }
          break;
        case "--encoding":
          //here we have the character encoding used to decode strings from raw bytes
          params.setEncoding(option.value);
          try {
            boolean supported = Charset.isSupported(option.value);
            if (!supported) errorsBuilder.append("Unsupported encoding: '").append(option.value).append("'.").append(ln);
          } catch (Exception e) {
            errorsBuilder.append("Unsupported encoding: '").append(option.value).append("'.").append(ln);
          }
          break;
        case "--down":
          //this is the path for storing the downstream traffic (from remote host to local port connection)
          params.setDownFilePath(option.value);
          break;
        case "--up":
          //this is the path for storing the upstream traffic (from local port to remote host)
          params.setUpFilePath(option.value);
          break;
        case "--mirror-down-host":
          params.setMirrorDownHost(option.value);
          break;
        case "--mirror-down-port":
          try {
            int iValue = Integer.parseInt(option.value);
            params.setMirrorDownPort(iValue);
            if (iValue <= 1 || iValue > 65535) errorsBuilder.append("Invalid mirror-down port value. Should be between 1-65535, was: ").append(iValue).append(".").append(ln);
          } catch (NumberFormatException e) {
            errorsBuilder.append("Invalid number for 'mirrordownport':").append(option.value).append(".").append(ln);
          }
          break;
        case "--mirror-up-host":
          params.setMirrorUpHost(option.value);
          break;
        case "--mirror-up-port":
          try {
            int iValue = Integer.parseInt(option.value);
            params.setMirrorUpPort(iValue);
            if (iValue <= 1 || iValue > 65535) errorsBuilder.append("Invalid mirror-up port value. Should be between 1-65535, was: ").append(iValue).append(".").append(ln);
          } catch (NumberFormatException e) {
            errorsBuilder.append("Invalid number for 'mirrorupport':").append(option.value).append(".").append(ln);
          }
          break;
        case "--hex":
          //if using byte console logger, this setting changes printing the bytes from integer list to set of hex characters
          hex = true;
          break;
        case "--help":
          //obviously, print out the help
          System.out.println(help());
          params.setShouldRun(false);
          return "";
        case "--logger":
          //increase the number of loggers found so we know later if we need to create the default one or not
          loggers++;
          //handle creation in round 2
          break;
        case "--udp-dns":
          //increase the number of loggers found so we know later if we need to create the default one or not
          params.setDNS(true);
          break;
        case "--udp-tun":
          //increase the number of loggers found so we know later if we need to create the default one or not
          params.setUDP(true);
          break;
        case "--trailing-lf":
          //enable adding a trailing LF to every console print
          params.setAddLF(true);
          break;
        case "--gzip":
          //try to decompress gzip encoding in HTTP requests for string logs
          params.setGzip(true);
          break;
        case "--https-tun":
          //enable https proxy tunnel
          params.setHttps(true);
          break;
        default:
          //anything not processed above is invalid..
          errorsBuilder.append("Invalid option '").append(name).append("'.").append(ln);
          break;
      }
    }
    errors = errorsBuilder.toString();
    //add the default logger if none found before
    if (loggers == 0) options.add(new Option("--logger", "console-string"));
    for (Option option : options) {
      //we create loggers in this separate iteration to have access to affecting parameters (e.g., --hex) and default logger if none specified by user
      String name = option.name;
      switch (name) {
        case "--logger":
          errors = addLogger(params, option.value, hex, errors);
          break;
      }
    }
    return errors;
  }

  /**
   * Adds a given type of logger to parser results.
   * 
   * @param params For storing parsed parameters (logger instances).
   * @param type The type identifier for the logger.
   * @param hex Whether to print bytes as ints or hex in relevant loggers.
   * @param errors Errors so far.
   * @return Previous and new errors.
   */
  private static String addLogger(Params params, String type, boolean hex, String errors) {
    switch(type) {
      case "console-string":
        //print decoded strings to console
        params.enableStringConsoleLogger();
        break;
      case "console-bytes":
        //print bytes to console, hex or int
        params.enableByteConsoleLogger(hex);
        break;
      case "file-string":
        //write decoded strings to file
        try {
          params.enableStringFileLogger(params.getDownFilePath(), params.getUpFilePath());
        } catch (IOException e) {
          errors += "Unable to create string file logger:"+e.getMessage();
          e.printStackTrace();
        }
        break;
      case "file-bytes":
        //write raw bytes as binary to file
        try {
          params.enableByteFileLogger(params.getDownFilePath(), params.getUpFilePath());
        } catch (IOException e) {
          errors += "Unable to create byte file logger:"+e.getMessage();
        }
        break;
      case "mirror-up":
        //write raw bytes as binary to another socket for upstream traffic
        params.enableMirrorUpStreamLogger(params.getMirrorUpHost(), params.getMirrorUpPort());
        break;
      case "mirror-down":
        //write raw bytes as binary to another socket for downstream traffic
        params.enableMirrorDownStreamLogger(params.getMirrorDownHost(), params.getMirrorDownPort());
        break;
      default:
        //anything not processed above is considerd invalid
        errors += "Unknown logger type: '"+type+"'."+ln;
    }
    return errors;
  }
  /**
   * Parse the actual parameters, meaning source (local) port, remote host and remote port.
   * Parameters are the ones not starting with '-'.
   * 
   * @param paramStrs The strings for parameters, filtered from options.
   * @param params For storing the parsing results.
   * @param errors Previous parsing errors so far.
   * @return Previous and new parsing errors.
   */
  private static String parseParams(List<String> paramStrs, Params params, String errors) {
    try {
      int sourcePort = Integer.parseInt(paramStrs.get(0));
      params.setSourcePort(sourcePort);
      if (sourcePort < 1 || sourcePort > 65535) errors += "Port numbers have to be in range 1-65535, source port was: " + sourcePort + "." + ln;
    } catch (NumberFormatException e) {
      errors += "Unable to parse source port from: '" + paramStrs.get(0) + "'." + ln;
    }
    params.setRemoteHost(paramStrs.get(1));
    try {
      int remotePort = Integer.parseInt(paramStrs.get(2));
      params.setRemotePort(remotePort);
      if (remotePort < 1 || remotePort > 65535) errors += "Port numbers have to be in range 1-65535, remote port was: " + remotePort + "." + ln;
    } catch (NumberFormatException e) {
      errors += "Unable to parse remote port from: '" + paramStrs.get(2) + "'." + ln;
    }
    return errors;
  }

  /**
   * @return The help text to show to user.
   */
  public static String help() {
    InputStream is = ArgumentParser.class.getResourceAsStream("helptext.txt");
    String template = Utils.getResource(is);
    return String.format(template, Params.DEFAULT_BUFFER_SIZE, Params.DEFAULT_ENCONDING, Params.DEFAULT_DOWN_PATH, Params.DEFAULT_UP_PATH);
  }

  /**
   * A class for holding a name-value pair as a configuration option.
   */
  private static class Option {
    /** The option name. */
    public final String name;
    /** The unparsed option value.*/
    public final String value;

    /**
     * @param name The option name.
     * @param value The unparsed option value.
     */
    public Option(String name, String value) {
      this.name = name;
      this.value = value;
    }
  }
}
