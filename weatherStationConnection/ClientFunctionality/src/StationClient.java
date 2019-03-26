import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.List;

public class StationClient extends Thread {


    private final ServerWorker stationWorker;
    private String login;
    private int[] stationSensors;
    public String stationName;
    private String landArea = "N/A";
    private String fieldCrop = "N/A";

    public StationClient(ServerWorker worker) {
        this.stationWorker = worker;
        this.login = stationWorker.getLogin();
        this.stationName = stationWorker.getPublicName();
    }

    @Override
    public void run()  // Thread which handles the client socket
    {
        StationSensors stationSensors = new StationSensors(this);

        try {
            displayOnline();
            pullStationData();
            stationSensors.startMessageReader();
            handleStationClient();
        } catch (IOException e) {
            e.printStackTrace();
        }
        stationSensors.startMessageReader();
    }

    private void handleStationClient() throws IOException{
        BufferedReader reader = new BufferedReader(new InputStreamReader(stationWorker.clientSocket.getInputStream())); // Take input
        String line;
        while((line = reader.readLine()) != null)
        {
            String[] tokens = StringUtils.split(line); // split line into tokens to read if it is a command
            if (tokens != null && tokens.length > 0)
            {
                String cmd = tokens[0]; // First token used as a command
                if("logoff".equalsIgnoreCase(cmd)){
                    break;
                }
                else if("update".equalsIgnoreCase(cmd)){
                    handleUpdate(tokens);
                }
            }
        }
    }

    private void handleUpdate(String[] tokens) throws IOException {
        if(tokens.length == 3){
            String field = tokens[1];

            if("area".equalsIgnoreCase(field)){
                double value = Double.parseDouble(tokens[2]);
                landArea = Double.toString(value);
                updateFields();
            }
            else if("crop".equalsIgnoreCase(field)){
                fieldCrop = tokens[2];
                updateFields();
            }
            else {
                System.out.println("Failed to update");
            }
        }
    }

    private void updateFields() throws IOException {

        File inputFile = new File("StationData.txt");
        File tempFile = new File("StationDataTemp.txt");

        BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

        String currentLine;

        while((currentLine = reader.readLine()) != null) {
            if (currentLine.length() > 0) {
                String[] tokens = StringUtils.split(currentLine, ",");
                if (!login.equalsIgnoreCase(tokens[0])) {
                    writer.write(currentLine + System.getProperty("line.separator"));
                }
            }
        }
        writer.write(login + "," + landArea + "," + fieldCrop + System.getProperty("line.separator"));
        writer.close();
        reader.close();

        inputFile.delete();
        tempFile.renameTo(inputFile);

        System.out.println("Update successful");
    }

    private void pullStationData() throws IOException {

        BufferedReader reader = new BufferedReader(new FileReader("StationData.txt"));

        String currentLine;

        while((currentLine = reader.readLine()) != null) {
            if (currentLine.length() > 0) {
                String[] tokens = StringUtils.split(currentLine, ",");
                if (login.equalsIgnoreCase(tokens[0])) {
                    landArea = tokens[1];
                    fieldCrop = tokens[2];
                    break;
                }
            }
        }
        reader.close();
    }

    private void displayOnline() throws IOException {
        // Send online notification for current user
        String onlineMsg = "online: " + login;
        List<ServerWorker> workerList = stationWorker.server.getWorkersList();

        for (ServerWorker worker : workerList) {
            if (!login.equals(worker.getLogIn()) && worker.getLogin().charAt(0) != 'S') {
                worker.send(onlineMsg);
            }
        }
    }

    public int[] getStationSensors(){
        return stationSensors;
    }

}
