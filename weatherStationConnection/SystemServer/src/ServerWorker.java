import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.Socket;
import java.util.List;

public class ServerWorker extends Thread
{
    public final Socket clientSocket;
    public Server server;
    private String login = null;
    private String publicName;
    private OutputStream outputStream;
    private BufferedReader reader;

    public ServerWorker(Server server, Socket clientSocket) throws IOException // Instantiates a server worker from a given server and socket
    {
        this.server = server;
        this.clientSocket = clientSocket;
    }

    @Override
    public void run() // Thread which handles the client socket
    {
        try
        {
            handleClientSocket();
        }
        catch (IOException e) { e.printStackTrace(); }
    }

    private void handleClientSocket() throws IOException
    {
        InputStream inputStream = clientSocket.getInputStream(); // Creates a bi directional communication stream with socket
        this.outputStream = clientSocket.getOutputStream(); // ^
        outputStream.write("\r".getBytes()); // Makes writing visible on console ? Don;t know why this happens

        this.reader = new BufferedReader(new InputStreamReader(inputStream)); // Take input
        String line;
        while((line = reader.readLine()) != null)
        {
            String[] tokens = StringUtils.split(line); // split line into tokens to read if it is a command
            if (tokens != null && tokens.length > 0)
            {
                String cmd = tokens[0]; // First token used as a command
                if ("quit".equalsIgnoreCase(cmd) || "logoff".equalsIgnoreCase(cmd))
                {
                    if(login != null) {
                        UserLogOff();
                    }
                    break;
                }
                else if ("login".equalsIgnoreCase(cmd))
                {
                    handleLogIn(outputStream, tokens);
                }
                else if("msg".equalsIgnoreCase(cmd)) // Used as a way to demonstrate messaging all online users #########################################
                {
                    String[] tokensMsg = StringUtils.split(line, null, 3);
                    handleUserMessage(tokensMsg);
                }
                else if("createaccount".equalsIgnoreCase(cmd)){
                    createAccount();
                }
                else if("download".equalsIgnoreCase(cmd)){
                    handleDownload(tokens);
                }
                else if("info".equalsIgnoreCase(cmd)){
                    handleInfo(tokens);
                }
                else if("help".equalsIgnoreCase(cmd)){
                    handleHelp();
                }
                else
                {
                    String msg = "unknown " + cmd + "\n\r";
                    outputStream.write(msg.getBytes());
                }
            }
        }
        clientSocket.close();
    }

    private void handleHelp() throws IOException {
        String msg;
        msg = "========== Help ==========\n\r" +
                "Commands:\n\r" +
                "Login: [login 'Username' 'Password']\n\r" +
                "# Allows a user to log in if credentials are correct.\n\n\r" +
                "Downland Station Data: [download 'Station Name']\n\r" +
                "#Download Station Data to your local Downloads folder.\n\n\r" +
                "Get Station Information: [info 'Station Name']\n\r" +
                "#Get crop and field area data about a Station.\n\n\r" +
                "Create Account: [createaccount]\n\r" +
                "#Create a new user or server account.\n\n\r" +
                "Exit Application: [logoff] or [quit]\n\n\r" ;

        outputStream.write(msg.getBytes());
    }

    private void handleInfo(String[] tokens) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader("StationData.txt"));
        String fieldArea = "N/A";
        String fieldCrop = "N/A";
        String station = tokens[1];
        String stationID = pullStationID(station);

        String currentLine;

        while((currentLine = reader.readLine()) != null) {
            if (currentLine.length() > 0) {
                String[] dataTokens = StringUtils.split(currentLine, ",");
                if (stationID.equalsIgnoreCase(dataTokens[0])) {
                    fieldArea = dataTokens[1];
                    fieldCrop = dataTokens[2];
                    break;
                }
            }
        }
        reader.close();

        String infoMsg = station+" info: \n"+"Area: "+fieldArea+"\n"+"Crop: "+fieldCrop+"\n\r";
        outputStream.write(infoMsg.getBytes());
    }

    private String pullStationID(String station) throws IOException {

        BufferedReader reader = new BufferedReader(new FileReader("accounts.txt"));

        String currentLine;
        while((currentLine = reader.readLine()) != null) {
            if (currentLine.length() > 0) {
                String[] dataTokens = StringUtils.split(currentLine, ",");
                if (station.equalsIgnoreCase(dataTokens[1])) {
                    return dataTokens[0];
                }
            }
        }
        reader.close();
        return null;
    }


    private void handleDownload(String[] tokens) {
        if(tokens.length == 2) {
            String station = tokens[1];
            String fileName = station + "_SensorData.txt";
            File source = new File(fileName);

            if(source.exists()){

                String home = System.getProperty("user.home");
                File dest = new File(home+"/Downloads/" + fileName);

                try {
                    FileUtils.copyFile(source, dest);
                    System.out.println("Successful Download");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    // "msg" "user" "body"
    private void handleUserMessage(String[] tokens) throws IOException {
        String sendTo = tokens[1];
        String body = tokens[2];

        List<ServerWorker> workerList = server.getWorkersList();
        for(ServerWorker worker : workerList) {
            if (sendTo.equalsIgnoreCase(worker.getLogIn())) {
                String outMsg = "msg " + login + " " + body;
                worker.send(outMsg);
            }
        }

    }

    private void UserLogOff() throws IOException {
        String logOffMsg = "Offline: " + login;

        // Sends a log ogg message to all online users
        List<ServerWorker> workerList = server.getWorkersList();
        if(login.charAt(0) == 'S') {
            for (ServerWorker worker : workerList) {
                if (isStation(worker)) {
                    worker.send(logOffMsg);
                }
            }
        }
        System.out.println("Client: " + login + " has logged out");
        server.removeFromWorkersList(this);
        clientSocket.close();
    }

    public String getLogIn()
    {
        return login;
    }

    private void handleLogIn(OutputStream outputStream, String[] tokens) throws IOException {
        if(tokens.length == 3)
        {
            String username = tokens[1]; // Username as token 1
            String password = tokens[2]; // Password as token 2
            String msg;

            String loginID = tryLogin(username, password);

            if(loginID != null)
            {
                outputStream.write(("Good login"+"\n\r").getBytes());
                this.login = loginID;
                this.publicName = username.toLowerCase();
                if(login.charAt(0) == 's'){
                    System.out.println("Station: "+login+" has logged in");
                    StationClient stationClient = new StationClient(this);
                    stationClient.run();
                }
                else {
                    // Show current user all other online users
                    System.out.println("Client: " + login + " has logged in");
                    List<ServerWorker> workerList = server.getWorkersList();
                    outputStream.write("OnlineStations: \n\r".getBytes());
                    for (ServerWorker worker : workerList) {
                        if (isStation(worker)) {
                            String onlineUsersMsg = worker.getPublicName();
                            send(onlineUsersMsg);
                        }
                    }
                }
            }
            else
            {
                msg = "Failed login";
                outputStream.write((msg+"\n\r").getBytes());
                System.err.println("Login failed for user: "+login);
            }
        }
    }

    private String tryLogin(String username, String password) throws IOException { // Handle station logins better ##########

        FileReader fileReader = new FileReader("accounts.txt");
        BufferedReader bufferedReader = new BufferedReader(fileReader);

        String line;
        while((line = bufferedReader.readLine()) != null){
            String[] currentLine = StringUtils.split(line,',');
            if(currentLine[1].equalsIgnoreCase(username) && currentLine[2].equalsIgnoreCase(password)){
                String ID = currentLine[0];
                return ID;
            }
        }
        bufferedReader.close();

        return null;
    }

    private void createAccount() throws IOException { // Can have multiple of the same username, needs to be checked for unique

    char accountType;
    outputStream.write("Account type: \n1.User \n2.Station \n\nPlease enter a number: ".getBytes());
    while (true){
        String type = reader.readLine();
        if("1".equalsIgnoreCase(type)) {
            accountType = 'f';
            break;
        }
        else if("2".equalsIgnoreCase(type)){
            accountType = 's';
            break;
        }
        else {
            outputStream.write("Invalid choice, please try again \n\r".getBytes());
        }
    }
    outputStream.write("Set Username: \n\r".getBytes());
    String username = reader.readLine();

    outputStream.write("Set Password: \n\r".getBytes());
    String password = reader.readLine();

    String ID = getNewID(accountType);

    String data = (accountType + ID + "," + username + "," + password);
    data = data.toLowerCase();

    FileWriter writer = new FileWriter("accounts.txt",true);
    writer.write(data + System.getProperty("line.separator"));
    writer.close();

    String creationMsg = "Account "+username+" Created: Type "+accountType;
    System.out.println(creationMsg);
    outputStream.write((creationMsg+"\n\r").getBytes());
}

    private String getNewID(char IDType) throws IOException {
        String ID;
        String lastID = "";

        FileReader fileReader = new FileReader("accounts.txt");
        BufferedReader bufferedReader = new BufferedReader(fileReader);

        String line;
        while((line = bufferedReader.readLine()) != null){
            if(line.charAt(0) == IDType){
                lastID = line;
            }
        }
        bufferedReader.close();

        if(lastID != ""){
            String[] currentLine = StringUtils.split(lastID,',');
            lastID = currentLine[0].substring(1);
            int newID = Integer.parseInt(lastID)+1;
            ID = Integer.toString(newID);
        }
        else {
            ID = "1";
        }
        return ID;
    }

    public void send(String msg) throws IOException // Use outputStream to send message to current worker
    {
        if(login != null) {
            outputStream.write((msg + "\n\r").getBytes());
        }
    }

    public String getLogin(){
        return login;
    }

    public String getPublicName(){
        return publicName;
    }

    private boolean isStation(ServerWorker worker) {
        return (!login.equals(worker.getLogIn()));
    }

}
