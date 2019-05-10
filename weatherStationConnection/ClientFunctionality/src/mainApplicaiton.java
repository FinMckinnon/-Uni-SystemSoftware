import java.io.*;

public class mainApplicaiton {

    public static void main(String[] args) throws IOException {
        Runtime.getRuntime().exec("cmd /c start cmd.exe /K \"ping localhost && telnet localhost 4445\"");
    }
}



