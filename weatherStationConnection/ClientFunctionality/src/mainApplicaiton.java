import java.io.*;

public class mainApplicaiton {

    // Establish connection to server
    public static void main(String[] args) throws IOException {
        Runtime.getRuntime().exec("cmd /c start cmd.exe /K \"ping localhost && telnet localhost 4445\"");
    }
}



