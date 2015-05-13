import java.lang.System;
import java.io.IOException;
import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;

//
// This is an implementation of a simplified version of a command 
// line ftp client. The program takes no arguments.
//


public class CSftp {

  static final int MAX_LEN = 255;
  static int port = 21;
  static Socket socket = null;
  static Socket tSocket;
  static BufferedReader reader;
  static String command;
  static final String emptystring = "";
  static final String[] commandlist = {
    "close", "get", "put", "cd", "dir"
  };

  // MAIN FUNCTION
  public static void main(String[] args) {

    byte cmdString[] = new byte[MAX_LEN];

    try {

      for (int len = 1; len > 0;) {
        cmdString = new byte[MAX_LEN]; // added by jon to clear cmdString for each command *CHANGE - may cause memory leak
        System.out.print("csftp> ");
        len = System. in .read(cmdString);
        command = new String(cmdString); // byte to String
        command = command.replaceAll("\\s+", " ").trim(); // gets rid of all double spaces and replaces with single spaces
        String[] commandparts = command.split(" "); // splits string and put into array
        int arraylength = commandparts.length;
        if (len <= 0) {
          break;
        } else {
          if (socket == null || socket.isClosed() == true) {
            functionSwitch(commandparts[0], commandparts, false);
          } else if (socket.isConnected()) {
            functionSwitch(commandparts[0], commandparts, socket.isConnected());
          }
        }
      }
    } catch (IOException exception) {
      errorOutput(898, emptystring, emptystring, 0);
    } catch (NullPointerException e) {
      socket = null;
      errorOutput(825, emptystring, emptystring, 0);
      String[] tempString = new String[1];
      main(tempString);
    }
  };

  // FUNCTION SWITCH FOR COMMANDS
  public static void functionSwitch(String field, String[] commandparts, boolean socketstatus) {
    if (socketstatus == true) {
      switch (field) {
        case "open":
          errorOutput(803, emptystring, emptystring, 0);
          break;
        case "user":
          userCommand(commandparts);
          break;
        case "close":
          if (commandparts.length == 1) closeCommand();
          else errorOutput(802, emptystring, emptystring, 0);
          break;
        case "quit":
          if (commandparts.length == 1) quitCommand();
          else errorOutput(802, emptystring, emptystring, 0);
          break;
        case "get":
          getCommand(commandparts);
          break;
        case "put":
          putCommand(commandparts);
          break;
        case "cd":
          cdCommand(commandparts);
          break;
        case "dir":
          dirCommand(commandparts);
          break;
        case "#":
          return;
        case "":
          return;
        default:
          errorOutput(800, emptystring, emptystring, 0);
          break;
      }
    } else if (socketstatus != true) {
      if (Arrays.asList(commandlist).contains(field)) {
        errorOutput(803, emptystring, emptystring, 0);
      } else if (field.equals("open")) {
        openCommand(commandparts);
      } else if (field.equals("quit")) {
        quitCommand();
      } else if (field.equals("#") || field.equals("") || field.equals(" ")) {
        return;
      } else {
        errorOutput(800, emptystring, emptystring, 0);
        return;
      }
    }
  }

  // OUTPUT SWITCH FOR ERRORS
  public static void errorOutput(int errorcode, String filename, String hostname, int port) {
    String errorstatement;
    switch (errorcode) {
      case 800:
        errorstatement = "800 Invalid command.";
        break;
      case 801:
        errorstatement = "801 Incorrect number of arguments.";
        break;
      case 802:
        errorstatement = "802 Invalid argument.";
        break;
      case 803:
        errorstatement = "803 Supplied command not expected at this time.";
        break;
      case 810:
        errorstatement = "810 Access to local file " + hostname + " denied.";
        break;
      case 820:
        errorstatement = "820 Control connection to " + hostname + " on port " + port + " failed to open";
        break;
      case 825:
        errorstatement = "825 Control connection I/O error, closing control connection.";
        break;
      case 830:
        errorstatement = "830 Data transfer connection to " + hostname + " on port " + port + " failed to open.";
        break;
      case 835:
        errorstatement = "835 Data transfer connection I/O error, closing data connection.";
        break;
      case 898:
        errorstatement = "898 Input error while reading commands, terminating.";
        break;
      default:
        errorstatement = "899 Processing error. " + filename + ".";
        break;
    }
    System.err.println(errorstatement);
    return;
  }

  // OPEN COMMAND 
  public static void openCommand(String[] commandparts) {
    int arraylength = commandparts.length;
    String hostName = "";

    if (arraylength == 2) {
      hostName = commandparts[1];
    } else if (arraylength == 3) {
      hostName = commandparts[1];
      try {
        port = Integer.parseInt(commandparts[2]);
      } catch (NumberFormatException e) {
        errorOutput(802, emptystring, emptystring, 0);
        return;
      }
    } else {
      errorOutput(801, emptystring, emptystring, 0);
      return;
    };

    try {
      socket = new Socket();
      socket.connect(new InetSocketAddress(InetAddress.getByName(hostName), port), 30000);
      reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      System.out.println(reader.readLine());
    } catch (IOException e) {
      socket = null;
      errorOutput(820, emptystring, hostName, port);
      return;
    } catch (IllegalArgumentException e) {
      errorOutput(802, emptystring, emptystring, 0);
      return;
    }
  };

  // USER COMMAND
  public static void userCommand(String[] commandparts) {
    int arraylength = commandparts.length;
    String password;
    String userName;
    String serverOut;
    BufferedReader cmdlReader = new BufferedReader(new InputStreamReader(System. in ));
    if (arraylength == 2) {
      userName = commandparts[1];
      try {
        PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
        printWriter.println("USER " + userName);
        System.out.println("--> " + command);
        System.out.println("<-- " + reader.readLine());
        System.out.print("Password: ");
        password = cmdlReader.readLine();
        printWriter.println("PASS  " + password);
        System.out.println("--> " + password);
        serverOut = reader.readLine();
        if ((serverOut != "null")) System.out.println("<-- " + serverOut);
        else {
          System.out.println("<-- " + serverOut);
          System.out.println("--> Incorrect username. Connection will now close");
          closeCommand();
        }
      } catch (IOException e) {
        errorOutput(825, emptystring, emptystring, 0);
        try {
          socket.close();
        } catch (IOException ex) {
          errorOutput(825, emptystring, emptystring, 0);
        };
      }
    } else {
      errorOutput(801, emptystring, emptystring, 0);
    }
  }

  // CLOSE COMMAND
  public static void closeCommand() {
    try {
      if (socket != null && socket.isConnected()) {
        PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
        printWriter.println("QUIT");
        System.out.println(reader.readLine());
        socket.close();
      } else {
        errorOutput(803, emptystring, emptystring, 0);
      };
    } catch (IOException e) {
      errorOutput(820, emptystring, emptystring, 0);
      return;
    }
  }

  // QUIT COMMAND
  public static void quitCommand() {
    if (socket.isClosed()) {
      System.exit(0);
    } else if (socket != null) {
      closeCommand();
      System.exit(0);
    } else System.exit(0);

  }

  // GET COMMAND
  public static void getCommand(String[] commandparts) {
    int arraylength = commandparts.length;
    String filename;
    String streamsize;
    ByteArrayInputStream tReader;
    String pasv;
    FileOutputStream fileOutput;
    int fileByte;
    String regex = "\\((\\d+)\\s\\bbytes\\b";
    Pattern pattern = Pattern.compile(regex);
    Matcher matcher;
    int counter = 0;
    int filesize = 0;
    int stringsize;
    String symbols = "____________________";
    String line;

    if (arraylength == 2) {
      filename = commandparts[1];
      try {
        System.out.println("--> " + command);
        PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
        printWriter.println("PASV");
        pasv = reader.readLine();

        System.out.println("<-- " + pasv);
        printWriter.println("TYPE I");

        System.out.println("<-- " + reader.readLine());

        createTSocket(pasv); // parse server input of format: "227 Entering Passive Mode (h1,h2,h3,h4,p1,p2)" 
        //and create new data transfer socket

        printWriter.println("RETR " + filename);
        streamsize = reader.readLine();
        String[] errorcode = streamsize.split(" ");
        if (errorcode[0].equals("550")) throw new FileNotFoundException();
        System.out.println("<-- " + streamsize);
        matcher = pattern.matcher(streamsize);
        if (matcher.find()) filesize = Integer.parseInt(matcher.group(1));
        fileOutput = new FileOutputStream(filename);

        while ((fileByte = tSocket.getInputStream().read()) != -1) { // read bytes from input and write to files
          counter++;
          int percent = (counter * 100) / filesize;
          symbols = "____________________";
          for (int i = 0; i < (percent / 10); i++) {
            symbols = symbols.replaceFirst("(__)", "**");
          }
          System.out.print("\r<-- [" + symbols + "] " + percent + " %");
          fileOutput.write(fileByte);
        }
        System.out.print("\n");
        tSocket.close();
        System.out.println("<-- " + reader.readLine());



      } catch (UnknownHostException e) {
        errorOutput(899, e.getMessage(), emptystring, 0);
        return;
      } catch (FileNotFoundException e) {
        errorOutput(899, e.getMessage(), emptystring, 0);
        return;
      } catch (IOException e) {
        errorOutput(835, e.getMessage(), emptystring, 0);
        return;
      }
    } else {
      errorOutput(801, emptystring, emptystring, 0);
    }
  }

  // PUT COMMAND
  public static void putCommand(String[] commandparts) {
    int arraylength = commandparts.length;
    String filename;
    String pasv;
    String line;
    byte[] fileBytes;
    Path filePath;
    ByteArrayOutputStream tWriter = new ByteArrayOutputStream();
    BufferedReader fileReader;
    BufferedReader fReader;
    ByteArrayInputStream byteReader;
    int fileByte;
    if (arraylength == 2) {
      filename = commandparts[1];

      try {
        fileReader = new BufferedReader(new FileReader(System.getProperty("user.dir") + "/" + filename));
        String fileString;

        System.out.println("--> " + command);
        PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
        printWriter.println("PASV");
        pasv = reader.readLine();
        System.out.println("<-- " + pasv);
        createTSocket(pasv); // parse server input of format: "227 Entering Passive Mode (h1,h2,h3,h4,p1,p2)"
        //and create new data transfer socket
        printWriter.println("TYPE I");
        System.out.println("<-- " + reader.readLine());
        printWriter.println("STOR " + filename);

        String serverOut = reader.readLine();
        System.out.println("<-- " + serverOut);

        filePath = Paths.get(System.getProperty("user.dir") + "/" + filename);
        fileBytes = Files.readAllBytes(filePath); // store file bytes in array

        tWriter.write(fileBytes, 0, fileBytes.length); // store fileBytes in buffer
        tWriter.writeTo(tSocket.getOutputStream()); // write buffered bytes to socket output stream

        tWriter.close();
        tSocket.close();

        String[] outputSplit = serverOut.split(" ");


        if (outputSplit[0].equals("150")) {
          System.out.println("<-- " + reader.readLine());
        } 
      } catch (UnknownHostException e) {
        errorOutput(899, e.getMessage(), emptystring, 0);
        return;
      } catch (FileNotFoundException e) {
        errorOutput(899, e.getMessage(), emptystring, 0);
        return;
      } catch (IOException e) {
        errorOutput(835, e.getMessage(), emptystring, 0);
        return;
      }
    } else {
      errorOutput(801, emptystring, emptystring, 0);
    }
  }

  // CD COMMAND
  public static void cdCommand(String[] commandparts) {
    int arraylength = commandparts.length;
    String foldername;
    BufferedReader tReader;
    if (arraylength == 2) {
      foldername = commandparts[1];
      try {
        System.out.println("--> " + command);
        PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
        printWriter.println("CWD " + foldername);
        System.out.println("<-- " + reader.readLine());
      } catch (IOException e) {
        errorOutput(899, e.getMessage(), emptystring, 0);
        return;
      }
    } else {
      errorOutput(801, emptystring, emptystring, 0);
    }
  }

  // DIR COMMAND
  public static void dirCommand(String[] commandparts) {
    String pasv;
    BufferedReader tReader;
    String linereader;
    try {
      System.out.println("--> " + command);
      PrintWriter printWriter = new PrintWriter(socket.getOutputStream(), true);
      printWriter.println("PASV");
      pasv = reader.readLine();
      if (pasv.contains("530")) throw new IOException();
      System.out.println("<-- " + pasv);
      createTSocket(pasv); // parse server input of format: "227 Entering Passive Mode (h1,h2,h3,h4,p1,p2)""
      tReader = new BufferedReader(new InputStreamReader(tSocket.getInputStream()));
      printWriter.println("LIST");
      System.out.println("<-- " + reader.readLine());
      while ((linereader = tReader.readLine()) != null) System.out.println("<-- " + linereader);
      System.out.println("<-- " + reader.readLine());
      if (tSocket != null) tSocket.close();
    } catch (IOException e) {
      errorOutput(803, emptystring, emptystring, 0);
    }

  }

  public static void createTSocket(String pasv) {

    String[] parts;
    String hostName;
    int port;
    int p1;
    int p2;

    ByteArrayOutputStream tWriter = null;

    parts = pasv.split(" ");

    if (parts[0].equals("227")) {
      parts[4] = parts[4].replaceAll("[()]", "").replace(".", ""); // remove "(", ")", and "."
      parts = parts[4].split(","); // split into address and port components
      hostName = parts[0] + "." + parts[1] + "." + parts[2] + "." + parts[3];
      p1 = Integer.parseInt(parts[4]);
      p2 = Integer.parseInt(parts[5]);
      port = (p1 * 256) + p2;
      try {
        tSocket = new Socket();
        tSocket.connect(new InetSocketAddress(InetAddress.getByName(hostName), port), 30000);
      } catch (UnknownHostException e) {
        errorOutput(899, e.getMessage(), emptystring, 0);
        return;
      } catch (IOException e) {
        errorOutput(835, e.getMessage(), hostName, port);
        tSocket = null;
        return;

      }
    }
  }


}