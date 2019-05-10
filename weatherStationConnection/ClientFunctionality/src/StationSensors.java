import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

public class StationSensors {

    private final StationClient stationClient; // Take in station[]
    public Random random = new Random();
    public Thread sensor;
    private double temperature;
    private double pressure;
    private double humidity;
    private double windSpeed;

    public StationSensors(StationClient stationClient){
        this.stationClient = stationClient;
    }

    public void startMessageReader(StationClient station) {
        this.sensor = new Thread(){
            @Override
            public void run()
            {
                try {
                    initialiseSensorValues();
                    startOrEndSession(station);
                    recordResults(station);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        sensor.start();
    }

    private void recordResults(StationClient station) throws InterruptedException, IOException {
        while(station.active) {

            generateResults();

            //Write to file
            String fileName = stationClient.stationName + "_SensorData.txt";
            // Add names ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
            String data = "{ Temperature: "+temperature+"C, Pressure: "+pressure+"hPa, Humidity "+ humidity +"% , Wind Speed: "+windSpeed+"mps }\n";

            writeDataToFile(fileName, data);

            Thread.sleep(1000 * 60);
        }
    }

    private void writeDataToFile(String fileName, String data ) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true));

        DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        String dateInfo = dateFormat.format(date);

        writer.append(dateInfo + ": " + data + System.getProperty("line.separator"));
        writer.close();
    }

    private void startOrEndSession(StationClient station) throws IOException {

        String fileName = stationClient.stationName + "_SensorData.txt";
        BufferedWriter writer = new BufferedWriter(new FileWriter(fileName, true));

        String linebreak = StringUtils.repeat("= ", 20);

        String msg =  "S E S S I O N    S T A R T  ";

        writer.append(linebreak + msg + linebreak + System.getProperty("line.separator"));
        writer.close();
    }

    private void generateResults(){
        this.temperature += (random.nextInt(50) / 100) * randomSign();
        this.humidity += (random.nextInt(10) / 100)  * randomSign();
        this.pressure += (random.nextInt(10) / 100)  * randomSign();
        this.windSpeed += (random.nextInt(100) / 100) * randomSign();
    }

    private void initialiseSensorValues(){
        this.temperature = random.nextInt(20) + 5;
        this.humidity = random.nextInt(10) + 68;
        this.pressure = random.nextInt(10) + 1017.5;
        this.windSpeed = random.nextInt(5) + 7;
    }

    private int randomSign(){
        int choice = random.nextInt(2);
        if(choice == 1){
            return 1;
        }
        return -1;
    }
}
