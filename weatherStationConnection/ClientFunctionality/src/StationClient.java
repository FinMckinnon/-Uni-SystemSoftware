import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.sql.Array;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

public class StationClient extends Thread {

    private final ServerWorker stationWorker;
    private String login;
    public String stationName;
    private String landArea;
    private String fieldCrop;
    private StationSensors stationSensors;
    public Boolean active;

    // Initialise values for the current session
    public StationClient(ServerWorker worker, String login, String stationName) {
        this.stationWorker = worker;
        this.login = login;
        this.stationName = stationName;
        active = true;
    }

    @Override
    public void run()  // Thread which handles the client socket
    {
        try {
            this.stationSensors = new StationSensors(this);
            pullStationData();
            stationSensors.startMessageReader(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Update a specified field with a given value
    boolean updateFields(String fieldToUpdate, String value) throws IOException {
        if("area".equalsIgnoreCase(fieldToUpdate)){
            this.landArea = value;
        }
        else if("crop".equalsIgnoreCase(fieldToUpdate)){
            this.fieldCrop = value;
        }
        else if("name".equalsIgnoreCase(fieldToUpdate)){
            this.stationName = value;
        }
        else {
            System.out.println("Update on "+stationName+" unsuccessful");
            return false;
        }

        // Store current file data, excluding updated value
        ArrayList<String> fileData = new ArrayList<>();
        String path = System.getProperty("user.dir") + "/";
        File inputFile = new File(path+"StationData.txt");
        BufferedReader reader = new BufferedReader(new FileReader(inputFile));

        String currentLine;
        while((currentLine = reader.readLine()) != null) {
            if (currentLine.length() > 0) {
                String[] tokens = StringUtils.split(currentLine, ",");
                if (!login.equalsIgnoreCase(tokens[0])) {
                    fileData.add(currentLine);
                }
            }
        }

        // Overwrite file with updated value
        BufferedWriter clearWriter = new BufferedWriter(new FileWriter(inputFile, false));
        clearWriter.write(login + "," + stationName + "," + landArea + "," + fieldCrop + System.getProperty("line.separator"));
        clearWriter.close();

        // Re-write all other data
        String line;
        BufferedWriter writer = new BufferedWriter(new FileWriter(inputFile, true));
        for(int i = 0; i < fileData.size(); i++ ){
            if(i == fileData.size()-1)line = fileData.get(i);
            else line = fileData.get(i) +System.getProperty("line.separator");
            writer.append(line);
        }
        writer.close();
        reader.close();

        System.out.println("Update on "+stationName+" successful");
        return true;
    }

    // Retrieve data of current station from database
    private void pullStationData() throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader("StationData.txt"));

        String currentLine;
        while((currentLine = reader.readLine()) != null) {
            if (currentLine.length() > 0) {
                String[] tokens = StringUtils.split(currentLine, ",");
                if (login.equalsIgnoreCase(tokens[0])) {
                    landArea = tokens[2];
                    fieldCrop = tokens[3];
                    break;
                }
            }
        }
        reader.close();
    }

    // Stops station sensor
    public void stopSensor(){
        this.active = false;
    }

    // Pull data from database file and return it as an array
    public ArrayList<String> getDataBrief() throws IOException {
        ArrayList<String> data = new ArrayList<>();
        String fileName = stationName + "_SensorData.txt";

        FileReader fileReader = new FileReader(fileName);
        BufferedReader bufferedReader = new BufferedReader(fileReader);

        String line;
        while((line = bufferedReader.readLine()) != null){
            data.add(line);
        }
        bufferedReader.close();
        return data;
    }

    // Download specified database file to users downloads folder
    public boolean downloadData(){
        String fileName = stationName + "_SensorData.txt";
        File source = new File(fileName);

        if(source.exists()){
            String home = System.getProperty("user.home");
            File dest = new File(home+"/Downloads/" + fileName);

            try {
                FileUtils.copyFile(source, dest);
                System.out.println(stationName + " data downloaded.");
                return true;
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }
}
