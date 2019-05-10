import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ServerWorker extends Thread
{
    public final Socket clientSocket;
    public Server server;
    private String login = null;
    private String publicName;
    private OutputStream outputStream;
    private BufferedReader reader;

    public ServerWorker(Server server, Socket clientSocket) // Instantiates a server worker from a given server and socket
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
        entryMessage();
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
                else if ("start".equalsIgnoreCase((cmd))){
                    handleStationStart(outputStream, tokens);
                }
                else if ("stop".equalsIgnoreCase(cmd)){
                    handleStationStop(outputStream, tokens);
                }
                else if("createaccount".equalsIgnoreCase(cmd)){
                    createAccount();
                }
                else if("info".equalsIgnoreCase(cmd)){
                    handleInfo(tokens);
                }
                else if("help".equalsIgnoreCase(cmd)){
                    handleHelp();
                }
                else if("clear".equalsIgnoreCase(cmd)){
                    moveScreen();
                }
                else if(login != null){
                    if("update".equalsIgnoreCase(cmd)){
                        handleUpdate(tokens);
                    }
                    else if ("refresh".equalsIgnoreCase(cmd)){
                        handleRefresh(tokens);
                    }
                    else if("download".equalsIgnoreCase(cmd)){
                        handleDownload(tokens);
                    }
                    else if("users".equalsIgnoreCase(cmd)){
                        showOnlineUsers();
                    }
                    else if("stations".equalsIgnoreCase(cmd)){
                        showOnlineStations();
                    }
                }
                else
                {
                    String msg = "Unknown command:  " + cmd + "\n\r";
                    outputStream.write(msg.getBytes());
                }
            }
        }
        clientSocket.close();
    }

    private void handleRefresh(String[] tokens) throws IOException {
        if(tokens.length == 2){
            String stationName = tokens[1];

            ArrayList<String> stationData = new ArrayList<>();
            Boolean found = false;

            List<StationClient> stationList = server.getStationList();
            for(StationClient station : stationList){
                if(station.stationName.equalsIgnoreCase(stationName)){
                    stationData = station.getDataBrief();
                    found = true;
                }
            }
            if(!found) outputStream.write(("Could not refresh station: "+stationName+" data.").getBytes());
            else{
                outputStream.write(("Most recent updates for station: "+stationName+", please download the station data to store this. \n\r").getBytes());
                System.out.println("Station "+ stationName + " data refreshed.");
                for(String line : stationData){
                    outputStream.write((line+ System.getProperty("line.separator")).getBytes());
                }
            }
        }
    }

    private void handleUpdate(String[] tokens) throws IOException {
        if(tokens.length == 4){
            String stationName = tokens[1];
            String field = tokens[2];
            String value = tokens[3];

            List<StationClient> stationList = server.getStationList();
            for(StationClient station : stationList){
                if(station.stationName.equalsIgnoreCase(stationName)){
                    station.updateFields(field, value);
                }
            }
        }
    }

    private void entryMessage() throws IOException {
        String msg = "+ - - - - - - - - - - - - - +\n\r" +
                     "|   Weather Station System  |\n\r" +
                     "+ - - - - - - - - - - - - - +\n\r";
        msg += "\nPlease type help for any assistance\n\n\r";
        moveScreen();
        outputStream.write(msg.getBytes());
    }

    private void handleHelp() throws IOException {
        String msg;
        msg = "========== Help ==========\n\r" +
                "Commands: \n\r" +
                "Login: [login 'Username' 'Password']\n\r" +
                "# Allows a user to log in if credentials are correct.\n\n\r" +
                "* View active users: [users]\n\r" +
                "#Show a list of all current active users.\n\n\r" +
                "* View all active Stations : [stations]\n\r" +
                "#Show a list of all active Stations.\n\n\r" +
                "Activate Station : [start 'Station Name']\n\r" +
                "#Activate a station and start gathering data.\n\n\r" +
                "Deactivate Station : [stop 'Station Name']\n\r" +
                "#Deactivate a station and stop gathering data.\n\n\r" +
                "* Update Station Info: [update 'Station Name' 'Field To Change' 'New Value']\n\r" +
                "#Update Station info to a specified input.\n\n\r" +
                "* View recorded Station Data: [refresh 'Station Name']\n\r" +
                "#View the most recent Station Data recorded.\n\n\r" +
                "* Downland Station Data: [download 'Station Name']\n\r" +
                "#Download Station Data to your local Downloads folder.\n\n\r" +
                "* Get Station Information: [info 'Station Name']\n\r" +
                "#Get crop and field area data about a Station.\n\n\r" +
                "Create Account: [createaccount]\n\r" +
                "#Create a new user or station.\n\n\r" +
                "Clear screen: [clear]\n\r" +
                "#Clears the screen of all data.\n\n\r" +
                "Exit Application: [logoff] or [quit]\n\n\r" +
                "All commands with an '*' character require a valid login.\n\n\r" ;
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

        BufferedReader reader = new BufferedReader(new FileReader("StationData.txt"));

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


    private void handleDownload(String[] tokens) throws IOException {
        if(tokens.length == 2) {

            String stationName = tokens[2];
            boolean success = false;
            String msg = "Station " + stationName + " download status: ";

            List<StationClient> stationList = server.getStationList();
            for(StationClient station : stationList){
                if(station.stationName.equalsIgnoreCase(stationName)){
                   success = station.downloadData();
                }
            }
            outputStream.write((msg + success + "\n\r").getBytes());
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

    private void handleStationStart(OutputStream outputStream, String[] tokens) throws IOException {
        if(tokens.length == 2)
        {
            String stationName = tokens[1]; // Station name as token 1
            String msg;

            String stationID = tryStationStart(stationName);

            if(stationID != null)
            {
                outputStream.write(("Station "+ stationName +" activated. "+"\n\r").getBytes());
                System.out.println("Station: "+ stationName +" has been activated.");
                StationClient stationClient = new StationClient(this, stationID, stationName.toLowerCase());
                server.addStation(stationClient);
                stationClient.run();

                List<ServerWorker> workerList = server.getWorkersList();
                for (ServerWorker worker : workerList) {
                    String onlineMessage = "# Station "+stationName+" is now online #";
                    worker.send(onlineMessage);
                }
            }
            else
            {
                msg = "Failed to start station: " + stationName;
                outputStream.write((msg+"\n\r").getBytes());
                System.err.println("Station "+ stationName+" failed to start.");
            }
        }
    }

    private void handleStationStop(OutputStream outputStream, String[] tokens) throws IOException {
        if (tokens.length == 2) {
            String stationName = tokens[1]; // Station name as token 1

            Boolean success = false;

            List<StationClient> stationList = server.getStationList();
            for (StationClient station : stationList) {
                if (station.stationName.equalsIgnoreCase(stationName)) {
                    station.stopSensor();
                    server.removeStation(station);
                    success = true;
                    break;
                }
            }

            if (success) {
                outputStream.write(("Station " + stationName + " stopped successfully.\n\r").getBytes());
                System.out.println("Station" + stationName + " has been deactivated. ");

                List<ServerWorker> workerList = server.getWorkersList();
                for (ServerWorker worker : workerList) {
                    if(!isStation(worker)){
                        String offlineMessage = "# Station "+stationName+" is now offline #";
                        worker.send(offlineMessage);
                    }
                }

            } else outputStream.write(("Station " + stationName + " was not found.\n\r").getBytes());

        }
    }

    private String tryStationStart(String stationName) throws IOException {
        FileReader fileReader = new FileReader("StationData.txt");
        BufferedReader bufferedReader = new BufferedReader(fileReader);

        String line;
        while((line = bufferedReader.readLine()) != null){
            String[] currentLine = StringUtils.split(line,',');

            if(currentLine[1].equalsIgnoreCase(stationName)){
                String ID = currentLine[0];
                return ID;
            }
        }
        bufferedReader.close();
        return null;
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
                moveScreen();
                outputStream.write(("Login successful"+"\n\r").getBytes());
                this.login = loginID;
                this.publicName = username.toLowerCase();

                // Show current user all online stations
                System.out.println("Client: " + login + " has logged in");
                showOnlineStations();
                showOnlineUsers();
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
            if(line.length() > 0) {
                String[] currentLine = StringUtils.split(line, ',');
                if (currentLine[1].equalsIgnoreCase(username) && currentLine[2].equalsIgnoreCase(password)) {
                    String ID = currentLine[0];
                    return ID;
                }
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

    String data;
    String ID = getNewID(accountType);

    if(accountType == 's'){

        outputStream.write("Set Station name: \n\r".getBytes());
        String stationName = reader.readLine();

        outputStream.write("Set land area: \n\r".getBytes());
        String area = reader.readLine();

        outputStream.write("Set crop: \n\r".getBytes());
        String crop = reader.readLine();

        data = (accountType + ID + "," + stationName + "," + area + "," + crop);
    }
    else{
        outputStream.write("Set Username: \n\r".getBytes());
        String username = reader.readLine();

        outputStream.write("Set Password: \n\r".getBytes());
        String password = reader.readLine();

        data = (accountType + ID + "," + username + "," + password);
    }

    data = data.toLowerCase();

    String fileName = (accountType == 's') ?  "StationData" : "accounts";

    FileWriter writer = new FileWriter(fileName+".txt",true);
    writer.write(data);
    writer.write(System.getProperty("line.separator"));
    writer.close();

    String creationMsg = "Account "+accountType + ID + " created.";
    System.out.println(creationMsg);
    outputStream.write((creationMsg+"\n\r").getBytes());
}

    private String getNewID(char IDType) throws IOException {
        String ID;
        String lastID = "";

        String fileName = (IDType == 's') ?  "StationData" : "accounts";

        FileReader fileReader = new FileReader(fileName+".txt");
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

    private boolean isStation(ServerWorker worker) {
        return (!login.equals(worker.getLogIn()));
    }

    private void moveScreen() throws IOException {
        String linebreak = "\n".repeat(50);
        outputStream.write(linebreak.getBytes());
    }

    private void showOnlineStations() throws IOException {
        List<StationClient> stationList = server.getStationList();
        outputStream.write("= = = = = Online Stations = = = = =  \n\r".getBytes());
        for (StationClient station : stationList) {
            String stationOnlineMsg = station.stationName;
            send(stationOnlineMsg);
        }
        outputStream.write("= = = = = = = = = = = = = = = = = =  \n\n\r".getBytes());
    }

    private void showOnlineUsers() throws IOException {
        List<ServerWorker> workerList = server.getWorkersList();
        outputStream.write(" = = = = = Online Users = = = = = =  \n\r".getBytes());
        for (ServerWorker worker : workerList) {
            if(worker.publicName != null) {
                if (!worker.publicName.equalsIgnoreCase(publicName)) {
                    String userOnlineMsg = worker.publicName;
                    send(userOnlineMsg);
                }
            }
        }
        outputStream.write(" = = = = = = = = = = = = = = = = =  \n\n\r".getBytes());
    }

}
